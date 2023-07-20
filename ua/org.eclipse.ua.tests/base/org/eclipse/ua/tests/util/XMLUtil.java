/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
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
package org.eclipse.ua.tests.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;

import org.eclipse.help.internal.entityresolver.LocalEntityResolver;
import org.junit.Assert;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A utility class for working with XML.
 */
public class XMLUtil extends Assert {

	public static void assertXMLEquals(String msg, String s1, String s2) throws Exception {
		InputStream in1 = new ByteArrayInputStream(s1.getBytes(StandardCharsets.UTF_8));
		InputStream in2 = new ByteArrayInputStream(s2.getBytes(StandardCharsets.UTF_8));
		assertXMLEquals(msg, in1, in2);
	}

	public static void assertXMLEquals(String msg, InputStream in1, InputStream in2) throws Exception {
		String s1 = process(in1);
		String s2 = process(in2);
		assertEquals(msg, s1, s2);
	}

	public static void assertParseableXML(String s)  {
		try {
			InputStream in1 = new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
			process(in1);
		} catch (Exception e) {
			fail("Unable to parse source: " + s);
		}
	}

	private static String process(InputStream in) throws Exception {
		@SuppressWarnings("restriction")
		SAXParser parser = org.eclipse.core.internal.runtime.XmlProcessorFactory
				.createSAXParserNoExternal();
		Handler handler = new Handler();
		parser.parse(in, handler);
		return handler.toString();
	}

	private static class Handler extends DefaultHandler {

		private StringBuilder buf = new StringBuilder();

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			buf.append('<');
			buf.append(qName);

			List<String> list = new ArrayList<>();
			for (int i=0;i<attributes.getLength();++i) {
				list.add(attributes.getQName(i));
			}
			list.sort(null);
			for (String name : list) {
				buf.append(' ');
				buf.append(name);
				buf.append('=');
				buf.append('"');
				buf.append(attributes.getValue(name));
				buf.append('"');
			}
			buf.append('>');
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			buf.append('<');
			buf.append('/');
			buf.append(qName);
			buf.append('>');
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			buf.append(ch, start, length);
		}

		/*
		 * Note: throws clause does not declare IOException due to a bug in
		 * sun jdk: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6327149
		 *
		 * @see org.xml.sax.helpers.DefaultHandler#resolveEntity(java.lang.String, java.lang.String)
		 */
		@Override
		public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
			try {
				return LocalEntityResolver.resolve(publicId, systemId);
			} catch (IOException e) {
				return new InputSource(new StringReader("")); //$NON-NLS-1$
			}
		}

		@Override
		public String toString() {
			return buf.toString();
		}
	}
}
