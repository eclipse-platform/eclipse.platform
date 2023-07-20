/*******************************************************************************
 *  Copyright (c) 2023 Joerg Kubitz and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.core.internal.runtime;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * XML processing which prohibits external entities.
 *
 * @see <a href="https://rules.sonarsource.com/java/RSPEC-2755/">RSPEC-2755</a>
 */
public class XmlProcessorFactory {
	private XmlProcessorFactory() {
		// static Utility only
	}

	// using these factories is synchronized with creating & configuring them
	// potentially concurrently in another thread:
	private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY_ERROR_ON_DOCTYPE = createDocumentBuilderFactoryWithErrorOnDOCTYPE();
	private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY_IGNORING_DOCTYPE = createDocumentBuilderFactoryIgnoringDOCTYPE();
	private static final SAXParserFactory SAX_FACTORY_ERROR_ON_DOCTYPE = createSAXFactoryWithErrorOnDOCTYPE(false);
	private static final SAXParserFactory SAX_FACTORY_ERROR_ON_DOCTYPE_NS = createSAXFactoryWithErrorOnDOCTYPE(true);
	private static final SAXParserFactory SAX_FACTORY_IGNORING_DOCTYPE = createSAXFactoryIgnoringDOCTYPE();
	private static final SAXParserFactory SAX_FACTORY_PURE = createSAXFactory(false);
	private static final SAXParserFactory SAX_FACTORY_PURE_NS = createSAXFactory(true);

	/**
	 * Creates TransformerFactory which throws TransformerException when detecting
	 * external entities.
	 *
	 * @return javax.xml.transform.TransformerFactory
	 */
	public static TransformerFactory createTransformerFactoryWithErrorOnDOCTYPE() {
		TransformerFactory factory = TransformerFactory.newInstance();
		// prohibit the use of all protocols by external entities:
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); //$NON-NLS-1$
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, ""); //$NON-NLS-1$
		return factory;
	}

	/**
	 * Creates DocumentBuilderFactory which throws SAXParseException when detecting
	 * external entities. It's magnitudes faster to call
	 * {@link #createDocumentBuilderWithErrorOnDOCTYPE()}.
	 *
	 * @return javax.xml.parsers.DocumentBuilderFactory
	 */
	public static synchronized DocumentBuilderFactory createDocumentBuilderFactoryWithErrorOnDOCTYPE() {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		// completely disable DOCTYPE declaration:
		try {
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); //$NON-NLS-1$
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		return factory;
	}

	/**
	 * Creates DocumentBuilderFactory which ignores external entities. It's
	 * magnitudes faster to call {@link #createDocumentBuilderIgnoringDOCTYPE()}.
	 *
	 * @return javax.xml.parsers.DocumentBuilderFactory
	 */
	public static synchronized DocumentBuilderFactory createDocumentBuilderFactoryIgnoringDOCTYPE() {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			// completely disable external entities declarations:
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false); //$NON-NLS-1$
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false); //$NON-NLS-1$
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		return factory;
	}

	/**
	 * Creates DocumentBuilder which throws SAXParseException when detecting
	 * external entities. The builder is not thread safe.
	 *
	 * @return javax.xml.parsers.DocumentBuilder
	 * @throws ParserConfigurationException
	 */
	public static synchronized DocumentBuilder createDocumentBuilderWithErrorOnDOCTYPE()
			throws ParserConfigurationException {
		return DOCUMENT_BUILDER_FACTORY_ERROR_ON_DOCTYPE.newDocumentBuilder();
	}

	/**
	 * Creates DocumentBuilder which ignores external entities. The builder is not
	 * thread safe.
	 *
	 * @return javax.xml.parsers.DocumentBuilder
	 * @throws ParserConfigurationException
	 */
	public static synchronized DocumentBuilder createDocumentBuilderIgnoringDOCTYPE()
			throws ParserConfigurationException {
		return DOCUMENT_BUILDER_FACTORY_IGNORING_DOCTYPE.newDocumentBuilder();
	}

	/**
	 * Creates DocumentBuilderFactory which throws SAXParseException when detecting
	 * external entities.
	 *
	 * @return javax.xml.parsers.DocumentBuilderFactory
	 */
	public static SAXParserFactory createSAXFactoryWithErrorOnDOCTYPE() {
		return createSAXFactoryWithErrorOnDOCTYPE(false);
	}

	/**
	 * Creates DocumentBuilderFactory which throws SAXParseException when detecting
	 * external entities.
	 *
	 * @param awareness true if the parser produced by this code will provide
	 *                  support for XML namespaces; false otherwise.
	 * @return javax.xml.parsers.DocumentBuilderFactory
	 */
	public static synchronized SAXParserFactory createSAXFactoryWithErrorOnDOCTYPE(boolean awareness) {
		SAXParserFactory f = SAXParserFactory.newInstance();
		if (awareness) {
			f.setNamespaceAware(true);
		}
		try {
			// force org.xml.sax.SAXParseException for any DOCTYPE:
			f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); //$NON-NLS-1$
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return f;
	}

	private static synchronized SAXParserFactory createSAXFactoryIgnoringDOCTYPE() {
		SAXParserFactory f = SAXParserFactory.newInstance();
		try {
			// ignore DOCTYPE:
			f.setFeature("http://xml.org/sax/features/external-general-entities", false); //$NON-NLS-1$
			f.setFeature("http://xml.org/sax/features/external-parameter-entities", false); //$NON-NLS-1$
			f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false); //$NON-NLS-1$
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return f;
	}

	/**
	 * Creates SAXParser which throws SAXParseException when detecting external
	 * entities.
	 *
	 * @return javax.xml.parsers.SAXParser
	 */

	public static SAXParser createSAXParserWithErrorOnDOCTYPE() throws ParserConfigurationException, SAXException {
		return createSAXParserWithErrorOnDOCTYPE(false);
	}

	/**
	 * Creates SAXParser which throws SAXParseException when detecting external
	 * entities.
	 *
	 * @param namespaceAware parameter for SAXParserFactory
	 *
	 * @return javax.xml.parsers.SAXParser
	 */
	public static synchronized SAXParser createSAXParserWithErrorOnDOCTYPE(boolean namespaceAware)
			throws ParserConfigurationException, SAXException {
		if (namespaceAware) {
			return SAX_FACTORY_ERROR_ON_DOCTYPE_NS.newSAXParser();
		}
		return SAX_FACTORY_ERROR_ON_DOCTYPE.newSAXParser();
	}

	/**
	 * Creates SAXParser which does not throw Exception when detecting external
	 * entities but ignores them.
	 *
	 * @return javax.xml.parsers.SAXParser
	 */
	public static synchronized SAXParser createSAXParserIgnoringDOCTYPE()
			throws ParserConfigurationException, SAXException {
		SAXParser parser = SAX_FACTORY_IGNORING_DOCTYPE.newSAXParser();
		parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, ""); //$NON-NLS-1$
		parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, ""); //$NON-NLS-1$
		return parser;
	}

	private static synchronized SAXParserFactory createSAXFactory(boolean awareness) {
		SAXParserFactory f = SAXParserFactory.newInstance();
		if (awareness) {
			f.setNamespaceAware(true);
		}
		return f;
	}

	public static synchronized SAXParser createSAXParserNoExternal(boolean namespaceAware)
			throws ParserConfigurationException, SAXException {

		SAXParser parser = namespaceAware ? SAX_FACTORY_PURE_NS.newSAXParser() : SAX_FACTORY_PURE.newSAXParser();
		// prohibit the use of all protocols by external entities:
		parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, ""); //$NON-NLS-1$
		parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, ""); //$NON-NLS-1$
		return parser;
	}

	public static synchronized SAXParser createSAXParserNoExternal() throws ParserConfigurationException, SAXException {
		return createSAXParserNoExternal(false);
	}

	/**
	 * Parse the content of the given input source as an XML document and return a
	 * new DOM {@link Document} object.
	 *
	 * @see javax.xml.parsers.DocumentBuilder#parse(InputSource)
	 */
	public static Document parseWithErrorOnDOCTYPE(InputSource is)
			throws SAXException, IOException, ParserConfigurationException {
		return createDocumentBuilderWithErrorOnDOCTYPE().parse(is);
	}

	/**
	 * Parse the content of the given input source as an XML document and return a
	 * new DOM {@link Document} object.
	 *
	 * @see javax.xml.parsers.DocumentBuilder#parse(InputStream)
	 */
	public static Document parseWithErrorOnDOCTYPE(InputStream is)
			throws SAXException, IOException, ParserConfigurationException {
		return createDocumentBuilderWithErrorOnDOCTYPE().parse(is);
	}

	/**
	 * Parse the content of the given input source as an XML document and return a
	 * new DOM {@link Document} object.
	 *
	 * @see javax.xml.parsers.DocumentBuilder#parse(File)
	 */
	public static Document parseWithErrorOnDOCTYPE(File file)
			throws SAXException, IOException, ParserConfigurationException {
		return createDocumentBuilderWithErrorOnDOCTYPE().parse(file);
	}

	/**
	 * Obtain a new instance of a DOM {@link Document} object to build a DOM tree
	 * with.
	 *
	 * @return A new instance of a DOM Document object.
	 * @see javax.xml.parsers.DocumentBuilder#newDocument()
	 */
	public static Document newDocumentWithErrorOnDOCTYPE() throws ParserConfigurationException {
		return createDocumentBuilderWithErrorOnDOCTYPE().newDocument();
	}

}