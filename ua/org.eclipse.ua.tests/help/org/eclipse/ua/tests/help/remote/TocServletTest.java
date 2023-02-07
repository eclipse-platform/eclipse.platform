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

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

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

public class TocServletTest {

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
	public void testTocServletContainsUAToc() throws Exception {
		Node root = getTocContributions("en");
		Element[] UARoot = findContributionById(root, "/org.eclipse.ua.tests/data/help/toc/root.xml");
		assertEquals(1, UARoot.length);
	}

	@Test
	public void testTocServletContainsFilteredToc() throws Exception {
		Node root = getTocContributions("en");
		Element[] UARoot = findContributionById(root, "/org.eclipse.ua.tests/data/help/toc/filteredToc/toc.xml");
		assertEquals(1, UARoot.length);
	}

	@Test
	public void testTocServletContainsUnlinkedToc() throws Exception {
		Node root = getTocContributions("en");
		Element[] UARoot = findContributionById(root, "/org.eclipse.ua.tests/data/help/toc/filteredToc/nonPrimaryToc.xml");
		assertEquals(1, UARoot.length);
	}

	@Test
	public void testReadEnToc() throws Exception {
		Node root = getTocContributions("en");
		Element[] uaRoot = findContributionById(root, "/org.eclipse.ua.tests/data/help/search/toc.xml");
		assertEquals(1, uaRoot.length);
		Element[] toc = findChildren(uaRoot[0], "toc", "label", "search");
		assertEquals(1, toc.length);
		Element[] topicSearch = findChildren(toc[0], "topic", "label", "search");
		assertEquals(1, topicSearch.length);
		Element[] topicEn = findChildren(topicSearch[0], "topic", "label", "testen.html");
		assertEquals(1, topicEn.length);
		Element[] topicDe = findChildren(topicSearch[0], "topic", "label", "testde.html");
		assertEquals(0, topicDe.length);
	}

	@Test
	public void testReadDeToc() throws Exception {
		Node root = getTocContributions("de");
		Element[] uaRoot = findContributionById(root, "/org.eclipse.ua.tests/data/help/search/toc.xml");
		assertEquals(1, uaRoot.length);
		Element[] toc = findChildren(uaRoot[0], "toc", "label", "search");
		assertEquals(1, toc.length);
		Element[] topicSearch = findChildren(toc[0], "topic", "label", "search");
		assertEquals(1, topicSearch.length);
		Element[] topicEn = findChildren(topicSearch[0], "topic", "label", "testen.html");
		assertEquals(0, topicEn.length);
		Element[] topicDe = findChildren(topicSearch[0], "topic", "label", "testde.html");
		assertEquals(1, topicDe.length);
	}

	private Element[] findContributionById(Node root, String id) {
		return findChildren(root, "tocContribution", "id", id);
	}

	private Element[] findChildren(Node parent, String childKind, String attributeName, String attributeValue) {
		NodeList contributions = parent.getChildNodes();
		List<Node> results = new ArrayList<>();
		for (int i = 0; i < contributions.getLength(); i++) {
			Node next = contributions.item(i);
			if (next instanceof Element) {
				Element nextElement = (Element)next;
				if ( childKind.equals(nextElement.getTagName()) && attributeValue.equals(nextElement.getAttribute(attributeName))) {

					results.add(next);
				}
			}
		}
		return results.toArray(new Element[results.size()]);
	}


	protected Node getTocContributions( String locale)
			throws Exception {
		int port = WebappManager.getPort();
		URL url = new URL("http", "localhost", port, "/help/toc?lang=" + locale);
		try (InputStream is = url.openStream()) {
			InputSource inputSource = new InputSource(is);
			DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			documentBuilder.setEntityResolver(new LocalEntityResolver());
			Document document = documentBuilder.parse(inputSource);
			Node root = document.getFirstChild();
			assertEquals("tocContributions", root.getNodeName());
			return root;
		}
	}

}
