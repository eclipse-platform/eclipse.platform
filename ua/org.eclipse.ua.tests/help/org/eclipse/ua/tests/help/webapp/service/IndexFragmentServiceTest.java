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

import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.entityresolver.LocalEntityResolver;
import org.eclipse.help.internal.server.WebappManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class IndexFragmentServiceTest {

	private int mode;

	@Before
	public void setUp() throws Exception {
		BaseHelpSystem.ensureWebappRunning();
		mode = BaseHelpSystem.getMode();
		BaseHelpSystem.setMode(BaseHelpSystem.MODE_INFOCENTER);
	}

	@After
	public void tearDown() throws Exception {
		BaseHelpSystem.setMode(mode);
	}

	@Test
	public void testIndexServletEn() throws Exception {
		getTreeData("en");
	}

	@Test
	public void testIndexServletDe() throws Exception {
		getTreeData("de");
	}

	private void getTreeData(String locale)
			throws Exception {
		int port = WebappManager.getPort();
		URL url = new URL("http", "localhost", port, "/help/vs/service/indexfragment?lang=" + locale);
		try (InputStream is = url.openStream()) {
			InputSource inputSource = new InputSource(is);
			Document document = LocalEntityResolver.parse(inputSource);
			Node root = document.getFirstChild();
			assertEquals("tree_data", root.getNodeName());
		}
	}

	@Test
	public void testIndexFragmentServiceXMLSchema()
			throws Exception {
		int port = WebappManager.getPort();
		URL url = new URL("http", "localhost", port, "/help/vs/service/indexfragment?lang=en");
		URL schemaUrl = new URL("http", "localhost", port, "/help/test/schema/xml/indexfragment.xsd");
		String schema = schemaUrl.toString();
		String uri = url.toString();
		String result = SchemaValidator.testXMLSchema(uri, schema);

		assertEquals("URL: \"" + uri + "\" is ", "valid", result);
	}

	@Test
	public void testIndexFragmentServiceJSONSchema()
			throws Exception {
//		fail("Not yet implemented.");
	}

}
