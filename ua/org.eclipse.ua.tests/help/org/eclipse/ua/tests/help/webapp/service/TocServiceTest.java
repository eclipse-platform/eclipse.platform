/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
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
package org.eclipse.ua.tests.help.webapp.service;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.net.URL;

import org.eclipse.help.internal.entityresolver.LocalEntityResolver;
import org.eclipse.help.internal.server.WebappManager;
import org.eclipse.ua.tests.help.remote.TocServletTest;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class TocServiceTest extends TocServletTest {

	@Override
	protected Node getTocContributions( String locale)
			throws Exception {
		int port = WebappManager.getPort();
		URL url = new URL("http", "localhost", port, "/help/vs/service/toc?lang=" + locale);
		try (InputStream is = url.openStream()) {
			InputSource inputSource = new InputSource(is);
			Document document = LocalEntityResolver.parse(inputSource);
			Node root = document.getFirstChild();
			assertEquals("tocContributions", root.getNodeName());
			return root;
		}
	}

	/*
	 * Disabled, see Bug 339274
	public void testTocServiceXMLSchema()
			throws Exception {
		int port = WebappManager.getPort();
		URL url = new URL("http", "localhost", port, "/help/vs/service/toc?lang=en");
		URL schemaUrl = new URL("http", "localhost", port, "/help/test/schema/xml/toc.xsd");
		String schema = schemaUrl.toString();
		String uri = url.toString();
		String result = SchemaValidator.testXMLSchema(uri, schema);

		assertEquals("URL: \"" + uri + "\" is ", "valid", result);
	}
	*/
	@Test
	public void testTocServiceJSONSchema()
			throws Exception {
		/*int port = WebappManager.getPort();
		URL url = new URL("http", "localhost", port, "/help/vs/service/toc?lang=en");
		URL schemaUrl = new URL("http", "localhost", port, "/help/test/schema/json/toc.json");
		String schema = schemaUrl.toString();
		String uri = url.toString();
		String result = SchemaValidator.testJSONSchema(uri, schema);

		assertEquals("URL: \"" + uri + "\" is ", "valid", result);*/
//		fail("Not yet implemented.");
	}

}
