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
package org.eclipse.help.internal.dynamic;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.help.internal.UAElement;
import org.eclipse.help.internal.UAElementFactory;
import org.eclipse.help.internal.entityresolver.LocalEntityResolver;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This class manages reuse of DOM parsers. It will keep reusing the same DocumentBuilder unless it is being used
 * elsewhere in which case a new one is allocated.
 */

public class DocumentReader {

	private static class ManagedBuilder {
		public DocumentBuilder builder;
		public boolean inUse;
	}

	private ManagedBuilder cachedBuilder;

	public UAElement read(InputStream in) throws IOException, SAXException, ParserConfigurationException {
		return read(in, null);
	}

	public UAElement read(InputStream in, String charset) throws IOException, SAXException, ParserConfigurationException {
		ManagedBuilder managedBuilder = getManagedBuilder();
		InputSource input = null;
		if (charset != null) {
			input = new InputSource(new InputStreamReader(in, charset));
		}
		else {
			input = new InputSource(in);
		}
		Document document = managedBuilder.builder.parse(input);
		managedBuilder.inUse = false;
		prepareDocument(document);
		return UAElementFactory.newElement(document.getDocumentElement());
	}

	/**
	 * Allows subclasses to process the DOM before creating a UA element
	 */
	protected void prepareDocument(Document document) {
	}

	private synchronized ManagedBuilder getManagedBuilder() throws FactoryConfigurationError, ParserConfigurationException {
		if (cachedBuilder == null || cachedBuilder.inUse) {
			cachedBuilder = createManagedBuilder();
		}
		cachedBuilder.inUse = true;
		return cachedBuilder;
	}

	private ManagedBuilder createManagedBuilder() throws FactoryConfigurationError, ParserConfigurationException {
		ManagedBuilder managedBuilder = new ManagedBuilder();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(false);
		managedBuilder.builder= factory.newDocumentBuilder();
		managedBuilder.builder.setEntityResolver(new LocalEntityResolver());
		return managedBuilder;
	}
}
