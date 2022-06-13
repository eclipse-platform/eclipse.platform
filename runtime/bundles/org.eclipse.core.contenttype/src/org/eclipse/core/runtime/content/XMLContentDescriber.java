/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.content;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.internal.content.TextContentDescriber;
import org.eclipse.core.internal.content.Util;
import org.eclipse.core.runtime.QualifiedName;

/**
 * A content describer for XML files. This class provides basis for XML-based
 * content describers.
 * <p>
 * The document is detected by the describer as <code>VALID</code>, if it
 * contains an xml declaration with <code>&lt;?xml</code> prefix and the
 * encoding in the declaration is correct.
 * </p>
 * Below are sample declarations recognized by the describer as
 * <code>VALID</code>
 * <ul>
 * <li>&lt;?xml version="1.0"?&gt;</li>
 * <li>&lt;?xml version="1.0"</li>
 * <li>&lt;?xml version="1.0" encoding="utf-16"?&gt;</li>
 * <li>&lt;?xml version="1.0" encoding="utf-16?&gt;</li>
 * </ul>
 *
 * @noinstantiate This class is not intended to be instantiated by clients.
 *                Clients should use it to provide their own XML-based
 *                describers that can be referenced by the "describer"
 *                configuration element in extensions to the
 *                <code>org.eclipse.core.runtime.contentTypes</code> extension
 *                point.
 * @see org.eclipse.core.runtime.content.IContentDescriber
 * @see org.eclipse.core.runtime.content.XMLRootElementContentDescriber2
 * @see "http://www.w3.org/TR/REC-xml *"
 * @since org.eclipse.core.contenttype 3.4
 */
public class XMLContentDescriber extends TextContentDescriber {
	private static final QualifiedName[] SUPPORTED_OPTIONS = new QualifiedName[] {IContentDescription.CHARSET, IContentDescription.BYTE_ORDER_MARK};
	private static final String XML_PREFIX = "<?xml "; //$NON-NLS-1$
	private static final String XML_DECL_END = "?>"; //$NON-NLS-1$
	private static final String BOM = "org.eclipse.core.runtime.content.XMLContentDescriber.bom"; //$NON-NLS-1$
	private static final String CHARSET = "org.eclipse.core.runtime.content.XMLContentDescriber.charset"; //$NON-NLS-1$
	private static final String FULL_XML_DECL = "org.eclipse.core.runtime.content.XMLContentDescriber.fullXMLDecl"; //$NON-NLS-1$
	private static final String RESULT = "org.eclipse.core.runtime.content.XMLContentDescriber.processed"; //$NON-NLS-1$

	@Override
	public int describe(InputStream input, IContentDescription description) throws IOException {
		return describe2(input, description, new HashMap<>());
	}

	int describe2(InputStream input, IContentDescription description, Map<String, Object> properties) throws IOException {
		if (!isProcessed(properties))
			fillContentProperties(input, description, properties);
		return internalDescribe(description, properties);
	}

	@Override
	public int describe(Reader input, IContentDescription description) throws IOException {
		return describe2(input, description, new HashMap<>());
	}

	int describe2(Reader input, IContentDescription description, Map<String, Object> properties) throws IOException {
		if (!isProcessed(properties))
			fillContentProperties(readXMLDecl(input), description, properties);
		return internalDescribe(description, properties);
	}

	private boolean isProcessed(Map<String, Object> properties) {
		Boolean result = (Boolean) properties.get(RESULT);
		if (result != null)
			return true;
		return false;
	}

	private void fillContentProperties(InputStream input, IContentDescription description, Map<String, Object> properties) throws IOException {
		byte[] bom = Util.getByteOrderMark(input);
		String xmlDeclEncoding = "UTF-8"; //$NON-NLS-1$
		input.reset();
		if (bom != null) {
			if (bom == IContentDescription.BOM_UTF_16BE)
				xmlDeclEncoding = "UTF-16BE"; //$NON-NLS-1$
			else if (bom == IContentDescription.BOM_UTF_16LE)
				xmlDeclEncoding = "UTF-16LE"; //$NON-NLS-1$
			// skip BOM to make comparison simpler
			input.skip(bom.length);
			properties.put(BOM, bom);
		}
		fillContentProperties(readXMLDecl(input, xmlDeclEncoding), description, properties);
	}

	private void fillContentProperties(String line, IContentDescription description, Map<String, Object> properties) throws IOException {
		// XMLDecl should be the first string (no blanks allowed)
		if (line != null && line.startsWith(XML_PREFIX))
			properties.put(FULL_XML_DECL, Boolean.TRUE);
		String charset = getCharset(line);
		if (charset != null)
			properties.put(CHARSET, charset);
		properties.put(RESULT, Boolean.TRUE);
	}

	private int internalDescribe(IContentDescription description, Map<String, Object> properties) {
		if (description != null) {
			byte[] bom = (byte[]) properties.get(BOM);
			if (bom != null && description.isRequested(IContentDescription.BYTE_ORDER_MARK))
				description.setProperty(IContentDescription.BYTE_ORDER_MARK, bom);
		}
		Boolean fullXMLDecl = (Boolean) properties.get(FULL_XML_DECL);
		if (fullXMLDecl == null || !fullXMLDecl.booleanValue())
			return INDETERMINATE;
		if (description == null)
			return VALID;
		String charset = (String) properties.get(CHARSET);
		if (description.isRequested(IContentDescription.CHARSET)) {
			if (charset != null && !isCharsetValid(charset))
				return INVALID;
			if (isNonDefaultCharset(charset))
				description.setProperty(IContentDescription.CHARSET, charset);
		}
		return VALID;
	}

	private boolean isNonDefaultCharset(String charset) {
		if (charset == null)
			return false;
		if ("utf8".equalsIgnoreCase(charset) || "utf-8".equalsIgnoreCase(charset)) //$NON-NLS-1$ //$NON-NLS-2$
			return false;
		return true;
	}

	private boolean isFullXMLDecl(String xmlDecl) {
		return xmlDecl.endsWith(XML_DECL_END);
	}

	private String readXMLDecl(InputStream input, String encoding) throws IOException {
		byte[] xmlDeclEndBytes = XML_DECL_END.getBytes(encoding);

		// allocate an array for the input
		int xmlDeclSize = 100 * xmlDeclEndBytes.length / 2;
		byte[] xmlDecl = new byte[xmlDeclSize];

		// looks for XMLDecl end (?>)
		int c = 0;
		int read = 0;

		// count is incremented when subsequent read characters match the xmlDeclEnd bytes,
		// the end of xmlDecl is reached, when count equals the xmlDeclEnd length
		int count = 0;

		while (read < xmlDecl.length && (c = input.read()) != -1) {
			if (c == xmlDeclEndBytes[count])
				count++;
			else
				count = 0;
			xmlDecl[read++] = (byte) c;
			if (count == xmlDeclEndBytes.length)
				break;
		}
		return new String(xmlDecl, 0, read, encoding);
	}

	private String readXMLDecl(Reader input) throws IOException {
		BufferedReader reader = new BufferedReader(input);
		String line = null;

		StringBuilder stringBuilder = new StringBuilder(100);
		while (stringBuilder.length() < 100 && ((line = reader.readLine()) != null)) {
			stringBuilder.append(line);
			if (line.contains(XML_DECL_END)) {
				String resultString = stringBuilder.toString();
				return resultString.substring(0, resultString.indexOf(XML_DECL_END) + XML_DECL_END.length());
			}
		}
		return stringBuilder.toString();
	}

	private String getCharset(String firstLine) {
		int encodingPos = findEncodingPosition(firstLine);
		if (encodingPos == -1)
			return null;
		char quoteChar = '"';
		int firstQuote = firstLine.indexOf('"', encodingPos);
		int firstApostrophe = firstLine.indexOf('\'', encodingPos);
		//use apostrophe if there is no quote, or an apostrophe comes first
		if (firstQuote == -1 || (firstApostrophe != -1 && firstApostrophe < firstQuote)) {
			quoteChar = '\'';
			firstQuote = firstApostrophe;
		}
		if (firstQuote == -1 || firstLine.length() == firstQuote + 1)
			return null;
		int secondQuote = firstLine.indexOf(quoteChar, firstQuote + 1);
		if (secondQuote == -1)
			return isFullXMLDecl(firstLine) ? firstLine.substring(firstQuote + 1, firstLine.lastIndexOf(XML_DECL_END)).trim() : null;
		return firstLine.substring(firstQuote + 1, secondQuote);
	}

	private int findEncodingPosition(String line) {
		String encoding = "encoding"; //$NON-NLS-1$
		int fromIndex = 0;
		int position = 0;
		while ((position = line.indexOf(encoding, fromIndex)) != -1) {
			boolean equals = false;
			fromIndex = position + encoding.length();
			for (int i = fromIndex; i < line.length(); i++) {
				char c = line.charAt(i);
				if (c == '=' && !equals) {
					equals = true;
				} else if (c == 0x20 || c == 0x09 || c == 0x0D || c == 0x0A) {
					// white space characters to ignore
				} else if ((c == '"' || c == '\'') && equals) {
						return position;
				} else {
					break;
				}
			}
		}
		return -1;
	}

	private boolean isCharsetValid(String charset) {
		if (charset.isEmpty())
			return false;

		char c = charset.charAt(0);
		if (!(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z'))
			return false;

		for (int i = 1; i < charset.length(); i++) {
			c = charset.charAt(i);
			if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-' || c == '_' || c == '.')
				continue;
			return false;
		}
		return true;
	}

	@Override
	public QualifiedName[] getSupportedOptions() {
		return SUPPORTED_OPTIONS;
	}
}
