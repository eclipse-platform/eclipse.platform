/*******************************************************************************
 * Copyright (c) 2008, 2015 IBM Corporation and others.
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

package org.eclipse.help.internal.entityresolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.help.internal.HelpPlugin;
import org.osgi.framework.Bundle;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Utility Class to parse XML that can contain references to internally known
 * DTDs
 */
public final class LocalEntityResolver implements EntityResolver {
	private LocalEntityResolver() {
		// static Utility only - so that nothing can change behavior
	}
	public static InputSource resolve(String publicId, String systemId) throws IOException {
		int index = systemId.lastIndexOf("/"); //$NON-NLS-1$
		if (index >= 0) {
			Bundle helpBundle = HelpPlugin.getDefault().getBundle();
			String dtdPath = "dtds/internal" + systemId.substring(index); //$NON-NLS-1$
			URL dtdURL = FileLocator.find(helpBundle, IPath.fromOSString(dtdPath), null);
			if (dtdURL != null) {
				InputStream stream = dtdURL.openStream();
				if (stream != null) {
					InputSource is = new InputSource(stream);
					is.setSystemId(systemId);
					is.setPublicId(publicId);
					return is;
				}
			}
		}
		return new InputSource(new StringReader("")); //$NON-NLS-1$
	}

	@Override
	public final InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
		return resolve(publicId, systemId);
	}

	private static DocumentBuilder createDocumentBuilder() throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		// see https://rules.sonarsource.com/java/RSPEC-2755/
		// disable some external entities declarations:
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false); //$NON-NLS-1$
		// prohibit the use of all protocols by external entities:
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); //$NON-NLS-1$
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, ""); //$NON-NLS-1$
		DocumentBuilder documentBuilder = factory.newDocumentBuilder();
		documentBuilder.setEntityResolver(new LocalEntityResolver());
		return documentBuilder;
	}

	/**
	 * Parse the content of the given input source as an XML document and return
	 * a new DOM {@link Document} object.
	 *
	 * @see javax.xml.parsers.DocumentBuilder#parse(InputSource)
	 */
	public static Document parse(InputSource is) throws SAXException, IOException, ParserConfigurationException {
		return createDocumentBuilder().parse(is);
	}

	/**
	 * Parse the content of the given input source as an XML document and return
	 * a new DOM {@link Document} object.
	 *
	 * @see javax.xml.parsers.DocumentBuilder#parse(InputStream)
	 */
	public static Document parse(InputStream is) throws SAXException, IOException, ParserConfigurationException {
		return createDocumentBuilder().parse(is);
	}

	/**
	 * Parse the content of the given input source as an XML document and return
	 * a new DOM {@link Document} object.
	 *
	 * @see javax.xml.parsers.DocumentBuilder#parse(String)
	 */
	public static Document parse(String content) throws SAXException, IOException, ParserConfigurationException {
		StringReader reader = new StringReader(content);
		return createDocumentBuilder().parse(new InputSource(reader));
	}

	/**
	 * Obtain a new instance of a DOM {@link Document} object to build a DOM
	 * tree with.
	 *
	 * @return A new instance of a DOM Document object.
	 * @see javax.xml.parsers.DocumentBuilder#newDocument()
	 */
	public static Document newDocument() throws ParserConfigurationException {
		return createDocumentBuilder().newDocument();
	}

}
