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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.entityresolver.LocalEntityResolver;
import org.eclipse.help.internal.server.WebappManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class SearchServletTest {

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
	public void testRemoteSearchNotFound() throws Exception {
		Node[] hits = getSearchHitsFromServlet("duernfryehd");
		assertEquals(0, hits.length);
	}

	@Test
	public void testRemoteSearchFound() throws Exception {
		Node[] hits = getSearchHitsFromServlet("jehcyqpfjs");
		assertEquals(1, hits.length);
	}

	@Test
	public void testRemoteSearchOrFound() throws Exception {
		Node[] hits = getSearchHitsFromServlet("jehcyqpfjs OR duernfryehd");
		assertEquals(1, hits.length);
	}

	@Test
	public void testRemoteSearchAndFound() throws Exception {
		Node[] hits = getSearchHitsFromServlet("jehcyqpfjs AND vkrhjewiwh");
		assertEquals(1, hits.length);
	}

	@Test
	public void testRemoteSearchAndNotFound() throws Exception {
		Node[] hits = getSearchHitsFromServlet("jehcyqpfjs AND duernfryehd");
		assertEquals(0, hits.length);
	}

	@Test
	public void testRemoteSearchExactMatchFound() throws Exception {
		Node[] hits = getSearchHitsFromServlet("\"jehcyqpfjs vkrhjewiwh\"");
		assertEquals(1, hits.length);
	}

	@Test
	public void testRemoteSearchExactMatchNotFound() throws Exception {
		Node[] hits = getSearchHitsFromServlet("\"vkrhjewiwh jehcyqpfjs\"");
		assertEquals(0, hits.length);
	}

	@Test
	public void testRemoteSearchWordNotInDefaultLocale() throws Exception {
		Node[] hits = getSearchHitsFromServlet("deuejwuid");
		assertEquals(0, hits.length);
	}

	@Test
	public void testRemoteSearchUsingDeLocale() throws Exception {
		Node[] hits = getSearchHitsUsingLocale("deuejwuid", "de");
		assertEquals(1, hits.length);
	}

	@Test
	public void testRemoteSearchUsingEnLocale() throws Exception {
		Node[] hits = getSearchHitsUsingLocale("deuejwuid", "en");
		assertEquals(0, hits.length);
	}

	protected Node[] getSearchHitsFromServlet(String phrase)
			throws Exception {
		int port = WebappManager.getPort();
		URL url = new URL("http", "localhost", port,
				"/help/search?phrase=" + URLEncoder.encode(phrase, StandardCharsets.UTF_8));
		return makeServletCall(url);
	}

	protected Node[] getSearchHitsUsingLocale(String phrase, String locale)
			throws Exception {
		int port = WebappManager.getPort();
		URL url = new URL("http", "localhost", port, "/help/search?phrase="
				+ URLEncoder.encode(phrase, StandardCharsets.UTF_8) + "&lang=" + locale);
		return makeServletCall(url);
	}

	protected Node[] makeServletCall(URL url) throws IOException,
			ParserConfigurationException, FactoryConfigurationError,
			SAXException {
		try (InputStream is = url.openStream()) {
			InputSource inputSource = new InputSource(is);
			DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			documentBuilder.setEntityResolver(new LocalEntityResolver());
			Document document = documentBuilder.parse(inputSource);
			Node root = document.getFirstChild();
			is.close();
			assertEquals("searchHits", root.getNodeName());
			NodeList children = root.getChildNodes();
			List<Node> hits = new ArrayList<>();
			int length = children.getLength();
			for (int i = 0; i < length; i++) {
				Node next = children.item(i);
				if ("hit".equals(next.getNodeName())) {
					hits.add(next);
				}
			}
			return hits.toArray(new Node[hits.size()]);
		}
	}

}
