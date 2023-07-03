/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
package org.eclipse.help.internal.xhtml;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.core.runtime.content.IContentDescriber;
import org.eclipse.help.internal.base.HelpEvaluationContext;
import org.eclipse.help.internal.dynamic.DocumentReader;
import org.eclipse.help.internal.dynamic.ExtensionHandler;
import org.eclipse.help.internal.dynamic.FilterHandler;
import org.eclipse.help.internal.dynamic.IncludeHandler;
import org.eclipse.help.internal.dynamic.ProcessorHandler;
import org.eclipse.help.internal.dynamic.XMLProcessor;
import org.eclipse.help.internal.search.HTMLDocParser;
import org.xml.sax.SAXException;

/*
 * Performs any needed XHTML processing on the given input stream. If the input
 * is not XHTML, simply forwards the stream.
 */
public class DynamicXHTMLProcessor {

	private static IContentDescriber xhtmlDescriber;
	private static XMLProcessor xmlProcessor;
	private static String xmlProcessorLocale;
	private static XMLProcessor xmlProcessorNoFilter;
	private static String xmlProcessorNoFilterLocale;

	/*
	 * Performs any needed processing. Does nothing if not XHTML.
	 */
	public static InputStream process(String href, InputStream in, String locale, boolean filter) throws IOException, SAXException, ParserConfigurationException, TransformerConfigurationException, TransformerException {
		BufferedInputStream buf = new BufferedInputStream(in, XHTMLContentDescriber.BUFFER_SIZE);
		int bufferSize = Math.max(XHTMLContentDescriber.BUFFER_SIZE, HTMLDocParser.MAX_OFFSET);
		byte[] buffer = new byte[bufferSize];
		buf.mark(Math.max(XHTMLContentDescriber.BUFFER_SIZE, HTMLDocParser.MAX_OFFSET));
		buf.read(buffer);
		buf.reset();
		boolean isXHTML = isXHTML(new ByteArrayInputStream(buffer));
		if (isXHTML) {
			String charset = HTMLDocParser.getCharsetFromHTML(new ByteArrayInputStream(buffer));
			if (filter) {
				return getXmlProcessor(locale).process(buf, href, charset);
			}
			return getXmlProcessorNoFilter(locale).process(buf, href, charset);
		}
		return buf;
	}

	private static synchronized XMLProcessor getXmlProcessorNoFilter(String locale) {
		if (!Objects.equals(xmlProcessorNoFilterLocale, locale)) {
			xmlProcessorNoFilterLocale = locale;
			xmlProcessorNoFilter = null;
		}
		if (xmlProcessorNoFilter == null) {
			DocumentReader reader = new DocumentReader();
			xmlProcessorNoFilter = new XMLProcessor(new ProcessorHandler[] { new IncludeHandler(reader, locale),
					new ExtensionHandler(reader, locale), new XHTMLCharsetHandler() });
		}
		return xmlProcessorNoFilter;
	}

	private static synchronized XMLProcessor getXmlProcessor(String locale) {
		if (!Objects.equals(xmlProcessorLocale, locale)) {
			xmlProcessorLocale = locale;
			xmlProcessor = null;
		}
		if (xmlProcessor == null) {
			DocumentReader reader = new DocumentReader();
			xmlProcessor = new XMLProcessor(
					new ProcessorHandler[] { new IncludeHandler(reader, locale), new ExtensionHandler(reader, locale),
							new XHTMLCharsetHandler(), new FilterHandler(HelpEvaluationContext.getContext()) });
		}
		return xmlProcessor;
	}

	/*
	 * Returns whether or not the given input stream is XHTML content.
	 */
	private static synchronized boolean isXHTML(InputStream in) {
		if (xhtmlDescriber == null) {
			xhtmlDescriber = new XHTMLContentDescriber();
		}
		if (in != null) {
			try {
				return (xhtmlDescriber.describe(in, null) == IContentDescriber.VALID);
			}
			catch (IOException e) {
			}
		}
		return false;
	}
}
