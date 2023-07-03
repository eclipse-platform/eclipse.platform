/*******************************************************************************
 * Copyright (c) 2010, 2020 IBM Corporation and others.
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
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 *******************************************************************************/
package org.eclipse.help.search;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.Stack;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.base.util.ProxyUtil;
import org.eclipse.help.internal.dynamic.DocumentReader;
import org.eclipse.help.internal.dynamic.ExtensionHandler;
import org.eclipse.help.internal.dynamic.IncludeHandler;
import org.eclipse.help.internal.dynamic.ProcessorHandler;
import org.eclipse.help.internal.dynamic.XMLProcessor;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * An abstract search participants for adding XML documents to the search index. Subclass it
 * and implement or override protected methods to handle parsing of the document.
 *
 * @since 3.5
 */
public abstract class SearchParticipantXML extends SearchParticipant {
	private Stack<String> stack = new Stack<>();
	private SAXParser parser;
	private XMLProcessor processor;
	private boolean hasFilters;

	/**
	 * Class that implements this interface is used to store data obtained during the parsing phase.
	 */
	protected interface IParsedXMLContent {

		/**
		 * Returns the locale of the index.
		 *
		 * @return the locale string
		 */
		String getLocale();

		/**
		 * Sets the title of the parsed document for indexing.
		 *
		 * @param title
		 *            the document title
		 */
		void setTitle(String title);

		/**
		 * Sets the optional summary of the parsed document that can be later rendered for the
		 * search hits.
		 *
		 * @param summary
		 *            the short document summary
		 */
		void addToSummary(String summary);

		/**
		 * Adds the text to the content buffer for indexing.
		 *
		 * @param text
		 *            the text to add to the document content buffer
		 */
		void addText(String text);
	}

	private static class ParsedXMLContent implements IParsedXMLContent {
		private StringBuilder buffer = new StringBuilder();
		private StringBuilder summary = new StringBuilder();
		private String title;
		private String locale;
		private static int SUMMARY_LENGTH = 200;

		public ParsedXMLContent(String locale) {
			this.locale = locale;
		}

		@Override
		public String getLocale() {
			return locale;
		}

		@Override
		public void setTitle(String title) {
			this.title = title;
		}

		@Override
		public void addToSummary(String text) {
			if (summary.length() >= SUMMARY_LENGTH)
				return;
			if (summary.length() > 0)
				summary.append(" "); //$NON-NLS-1$
			summary.append(text);
			if (summary.length() > SUMMARY_LENGTH)
				summary.delete(SUMMARY_LENGTH, summary.length());
		}

		@Override
		public void addText(String text) {
			if (buffer.length() > 0)
				buffer.append(" "); //$NON-NLS-1$
			buffer.append(text);
		}

		public String getContent() {
			return buffer.toString();
		}

		public String getSummary() {
			// if the summary starts with the title, trim that part off.
			String summaryStr = summary.toString();
			if (title != null && summaryStr.length() >= title.length()) {
				String header = summaryStr.substring(0, title.length());
				if (header.equalsIgnoreCase(title)) {
					return summaryStr.substring(title.length()).trim();
				}
			}
			return summaryStr;
		}

		public String getTitle() {
			return title;
		}
	}

	private class XMLHandler extends DefaultHandler {

		public ParsedXMLContent data;

		public XMLHandler(ParsedXMLContent data) {
			this.data = data;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {
			stack.push(qName);
			handleStartElement(qName, attributes, data);
			if (attributes.getValue("filter") != null || qName.equalsIgnoreCase("filter")) { //$NON-NLS-1$ //$NON-NLS-2$
				hasFilters = true;
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			handleEndElement(qName, data);
			String top = stack.peek();
			if (top != null && top.equals(qName))
				stack.pop();
		}

		@Override
		public void startDocument() throws SAXException {
			SearchParticipantXML.this.handleStartDocument(data);
		}

		@Override
		public void endDocument() throws SAXException {
			SearchParticipantXML.this.handleEndDocument(data);
		}

		@Override
		public void processingInstruction(String target, String pidata) throws SAXException {
			handleProcessingInstruction(target, data);
		}

		@Override
		public void characters(char[] characters, int start, int length) throws SAXException {
			if (length == 0)
				return;
			StringBuilder buff = new StringBuilder();
			for (int i = 0; i < length; i++) {
				buff.append(characters[start + i]);
			}
			String text = buff.toString();
			if (text.trim().length() > 0)
				handleText(text, data);
		}

		/*
		 * Note: throws clause does not declare IOException due to a bug in
		 * sun jdk: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6327149
		 *
		 * @see org.xml.sax.helpers.DefaultHandler#resolveEntity(java.lang.String, java.lang.String)
		 */
		@Override
		public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
			return new InputSource(new StringReader("")); //$NON-NLS-1$
		}
	}

	/**
	 * Called when the element has been started.
	 *
	 * @param name
	 *            the element name
	 * @param attributes
	 *            the element attributes
	 * @param data
	 *            data the parser content data to update
	 */
	protected abstract void handleStartElement(String name, Attributes attributes, IParsedXMLContent data);

	/**
	 * Called when the element has been ended.
	 *
	 * @param name
	 *            the name of the XML element
	 * @param data
	 *            data the parser content data to update
	 */
	protected abstract void handleEndElement(String name, IParsedXMLContent data);

	/**
	 * Called when the XML document has been started.
	 *
	 * @param data
	 *            data the parser content data to update
	 */
	protected void handleStartDocument(IParsedXMLContent data) {
	}

	/**
	 * Called when the XML document has been ended.
	 *
	 * @param data
	 *            data the parser content data to update
	 */
	protected void handleEndDocument(IParsedXMLContent data) {
	}

	/**
	 * Called when a processing instruction has been encountered.
	 *
	 * @param type
	 *            the instruction data
	 * @param data
	 *            the parser content data to update
	 */
	protected void handleProcessingInstruction(String type, IParsedXMLContent data) {
	}

	/**
	 * Called when element body text has been encountered. Use 'getElementStackPath()' to determine
	 * the element in question.
	 *
	 * @param text
	 *            the body text
	 * @param data
	 *            the parser content data to update
	 */
	protected abstract void handleText(String text, IParsedXMLContent data);


	@Override
	public IStatus addDocument(IHelpSearchIndex index, String pluginId,
			String name, URL url, String id, ISearchDocument doc) {
		InputStream stream = null;
		try {
			if (parser == null) {
				parser = SAXParserFactory.newInstance().newSAXParser();
			}
			stack.clear();
			hasFilters = false;
			ParsedXMLContent parsed = new ParsedXMLContent(index.getLocale());
			XMLHandler handler = new XMLHandler(parsed);
			stream = ProxyUtil.getStream(url);
			stream = preprocess(stream, name, index.getLocale());
			parser.parse(stream, handler);
			doc.addContents(parsed.getContent());
			String title = parsed.getTitle();
			if (title != null)
				addTitle(title, doc);
			String summary = parsed.getSummary();
			if (summary != null)
				doc.setSummary(summary);
			if (hasFilters) {
				doc.setHasFilters(true);
			}
			return Status.OK_STATUS;
		} catch (Exception e) {
			return new Status(IStatus.ERROR, HelpBasePlugin.PLUGIN_ID, IStatus.ERROR,
					"Exception occurred while adding document " + name //$NON-NLS-1$
							+ " to index.", //$NON-NLS-1$
					e);
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
				}
				stream = null;
			}
		}
	}

	/**
	 * Returns the name of the element that is currently at the top of the element stack.
	 *
	 * @return the name of the element that is currently at the top of the element stack
	 */

	protected String getTopElement() {
		return stack.peek();
	}

	/**
	 * Returns the full path of the current element in the stack separated by the '/' character.
	 *
	 * @return the path to the current element in the stack.
	 */
	protected String getElementStackPath() {
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < stack.size(); i++) {
			if (i > 0)
				buf.append("/"); //$NON-NLS-1$
			buf.append(stack.get(i));
		}
		return buf.toString();
	}

	/**
	 * <p>
	 * Pre-processes the given document input stream for the given document name and locale.
	 * This implementation will resolve dynamic content that is applicable to searching,
	 * e.g. includes and extensions, but not filters. Subclasses may override to do their
	 * own pre-processing.
	 * </p>
	 * <p>
	 * For performance, implementations that handle documents that do not support dynamic
	 * content should subclass and return the original stream.
	 * </p>
	 *
	 * @param in the input stream for the document content
	 * @param name the name of the document as it appears in the index
	 * @param locale the locale code, e.g. "en_US"
	 * @return the processed content
	 * @since 3.3
	 */
	protected InputStream preprocess(InputStream in, String name, String locale) {
		if (processor == null) {
			DocumentReader reader = new DocumentReader();
			processor = new XMLProcessor(new ProcessorHandler[] {
				new IncludeHandler(reader, locale),
				new ExtensionHandler(reader, locale)
			});
		}
		try {
			return processor.process(in, name, null);
		}
		catch (Throwable t) {
			String msg = "An error occured while pre-processing user assistance document \"" + name + "\" for search indexing"; //$NON-NLS-1$ //$NON-NLS-2$
			ILog.of(getClass()).error(msg, t);
			return in;
		}
	}
}
