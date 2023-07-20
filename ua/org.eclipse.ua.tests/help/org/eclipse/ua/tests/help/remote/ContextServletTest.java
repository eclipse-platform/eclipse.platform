/*******************************************************************************
 * Copyright (c) 2009, 2016 IBM Corporation and others.
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
 *     Snehasish Paul <snehpaul@in.ibm.com> - Eclipse help public API services
 *******************************************************************************/
package org.eclipse.ua.tests.help.remote;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.entityresolver.LocalEntityResolver;
import org.eclipse.help.internal.server.WebappManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ContextServletTest {

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
	public void testRemoteContextFound() throws Exception {
		Element[] topics = getContextsFromServlet("org.eclipse.ua.tests.test_cheatsheets");
		assertEquals(1, topics.length);
		assertEquals("abcdefg", topics[0].getAttribute("label"));
	}

	@Test
	public void testRemoteContextFoundDe() throws Exception {
		Element[] topics = getContextsUsingLocale
			("org.eclipse.ua.tests.test_cheatsheets", "de");
		assertEquals(1, topics.length);
		assertEquals("German Context", topics[0].getAttribute("label"));
	}

	@Test(expected = IOException.class)
	public void testRemoteContextNotFound() throws Exception {
		getContextsFromServlet("org.eclipse.ua.tests.no_such_context");
	}

	protected Element[] getContextsFromServlet(String phrase)
			throws Exception {
		int port = WebappManager.getPort();
		URL url = new URL("http", "localhost", port,
				"/help/context?id=" + URLEncoder.encode(phrase, StandardCharsets.UTF_8));
		return makeServletCall(url);
	}

	protected Element[] getContextsUsingLocale(String phrase, String locale)
			throws Exception {
		int port = WebappManager.getPort();
		URL url = new URL("http", "localhost", port, "/help/context?id="
				+ URLEncoder.encode(phrase, StandardCharsets.UTF_8) + "&lang=" + locale);
		return makeServletCall(url);
	}

	protected Element[] makeServletCall(URL url) throws IOException,
			ParserConfigurationException, FactoryConfigurationError,
			SAXException {
		try (InputStream is = url.openStream()) {
			InputSource inputSource = new InputSource(is);
			Document document = LocalEntityResolver.parse(inputSource);
			Node root = document.getFirstChild();
			assertEquals("context", root.getNodeName());
			NodeList children = root.getChildNodes();
			List<Node> topics = new ArrayList<>();
			int length = children.getLength();
			for (int i = 0; i < length; i++) {
				Node next = children.item(i);
				if ("topic".equals(next.getNodeName())) {
					topics.add(next);
				}
			}
			return topics.toArray(new Element[topics.size()]);
		}
	}

}
