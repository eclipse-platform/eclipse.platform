/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
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
package org.eclipse.core.internal.content;

import java.io.IOException;
import java.io.StringReader;
import javax.xml.parsers.*;
import org.eclipse.core.runtime.ServiceCaller;
import org.xml.sax.*;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A content describer for detecting the name of the top-level element of the
 * DTD system identifier in an XML file. This supports two parameters:
 * <code>DTD_TO_FIND</code> and <code>ELEMENT_TO_FIND</code>. This is done
 * using the <code>IExecutableExtension</code> mechanism. If the
 * <code>":-"</code> method is used, then the value is treated as the
 * <code>ELEMENT_TO_FIND</code>.
 *
 * @since 3.0
 */
public final class XMLRootHandler extends DefaultHandler implements LexicalHandler {
	/**
	 * An exception indicating that the parsing should stop. This is usually
	 * triggered when the top-level element has been found.
	 *
	 * @since 3.0
	 */
	private static class StopParsingException extends SAXException {
		/**
		 * All serializable objects should have a stable serialVersionUID
		 */
		private static final long serialVersionUID = 1L;

		/**
		 * Constructs an instance of <code>StopParsingException</code> with a
		 * <code>null</code> detail message.
		 */
		public StopParsingException() {
			super((String) null);
		}
	}

	/**
	 * Should we check the root element?
	 */
	private boolean checkRoot;
	/**
	 * The system identifier for the DTD that was found while parsing the XML.
	 * This member variable is <code>null</code> unless the file has been
	 * parsed successful to the point of finding the DTD's system identifier.
	 */
	private String dtdFound = null;
	/**
	 * This is the name of the top-level element found in the XML file. This
	 * member variable is <code>null</code> unless the file has been parsed
	 * successful to the point of finding the top-level element.
	 */
	private String elementFound = null;

	/**
	 * This is the namespace of the top-level element found in the XML file. This
	 * member variable is <code>null</code> unless the file has been parsed
	 * successful to the point of finding the top-level element.
	 */
	private String namespaceFound = null;

	public XMLRootHandler(boolean checkRoot) {
		this.checkRoot = checkRoot;
	}

	@Override
	public void comment(final char[] ch, final int start, final int length) {
		// Not interested.
	}

	/**
	 * Creates a new SAX parser for use within this instance.
	 *
	 * @return The newly created parser.
	 *
	 * @throws ParserConfigurationException
	 *             If a parser of the given configuration cannot be created.
	 * @throws SAXException
	 *             If something in general goes wrong when creating the parser.
	 * @throws SAXNotRecognizedException
	 *             If the <code>XMLReader</code> does not recognize the
	 *             lexical handler configuration option.
	 * @throws SAXNotSupportedException
	 *             If the <code>XMLReader</code> does not support the lexical
	 *             handler configuration option.
	 */
	private SAXParser createParser(SAXParserFactory parserFactory) throws ParserConfigurationException, SAXException, SAXNotRecognizedException, SAXNotSupportedException {
		// Initialize the parser.
		final SAXParser parser = parserFactory.newSAXParser();
		final XMLReader reader = parser.getXMLReader();
		reader.setProperty("http://xml.org/sax/properties/lexical-handler", this); //$NON-NLS-1$
		// disable DTD validation (bug 63625)
		try {
			//	be sure validation is "off" or the feature to ignore DTD's will not apply
			reader.setFeature("http://xml.org/sax/features/validation", false); //$NON-NLS-1$
			reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false); //$NON-NLS-1$
		} catch (SAXNotRecognizedException | SAXNotSupportedException e) {
			// not a big deal if the parser does not support the features
		}
		return parser;
	}

	@Override
	public void endCDATA() {
		// Not interested.
	}

	@Override
	public void endDTD() {
		// Not interested.
	}

	@Override
	public void endEntity(final String name) {
		// Not interested.
	}

	public String getDTD() {
		return dtdFound;
	}

	public String getRootName() {
		return elementFound;
	}

	/**
	 * @since org.eclipse.core.contenttype 3.3
	 */
	public String getRootNamespace() {
		return namespaceFound;
	}

	@SuppressWarnings("unchecked")
	static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
		throw (E) e;
	}

	public boolean parseContents(InputSource contents) throws IOException, ParserConfigurationException, SAXException {
		// Parse the file into we have what we need (or an error occurs).
		return ServiceCaller.callOnce(getClass(), SAXParserFactory.class, factory -> {
			try {
				factory.setNamespaceAware(true);
				final SAXParser parser = createParser(factory);
				// to support external entities specified as relative URIs (see bug 63298)
				contents.setSystemId("/"); //$NON-NLS-1$
				parser.parse(contents, this);
			} catch (StopParsingException e) {
				// Abort the parsing normally. Fall through...
			} catch (SAXException | IOException | ParserConfigurationException e) {
				sneakyThrow(e);
			}
		});
	}

	/*
	 * Resolve external entity definitions to an empty string.  This is to speed
	 * up processing of files with external DTDs.  Not resolving the contents
	 * of the DTD is ok, as only the System ID of the DTD declaration is used.
	 * @see org.xml.sax.helpers.DefaultHandler#resolveEntity(java.lang.String, java.lang.String)
	 */
	@Override
	public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
		return new InputSource(new StringReader("")); //$NON-NLS-1$
	}

	@Override
	public void startCDATA() {
		// Not interested.
	}

	@Override
	public void startDTD(final String name, final String publicId, final String systemId) throws SAXException {
		dtdFound = systemId;
		// If we don't care about the top-level element, we can stop here.
		if (!checkRoot)
			throw new StopParsingException();
	}

	@Override
	public void startElement(final String uri, final String elementName, final String qualifiedName, final Attributes attributes) throws SAXException {
		elementFound = elementName;
		namespaceFound = uri;
		throw new StopParsingException();
	}

	@Override
	public void startEntity(final String name) {
		// Not interested.
	}
}
