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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.help.ITopic;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.entityresolver.LocalEntityResolver;
import org.eclipse.help.internal.server.WebappManager;
import org.eclipse.help.internal.toc.Toc;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class TocFragmentServiceTest {

	private int mode;

	@BeforeEach
	public void setUp() throws Exception {
		BaseHelpSystem.ensureWebappRunning();
		mode = BaseHelpSystem.getMode();
		BaseHelpSystem.setMode(BaseHelpSystem.MODE_INFOCENTER);
	}

	@AfterEach
	public void tearDown() throws Exception {
		BaseHelpSystem.setMode(mode);
	}

	@Test
	public void testTocFragmentServiceContainsUAToc() throws Exception {
		int port = WebappManager.getPort();
		URL url = new URL("http", "localhost", port,
				"/help/vs/service/tocfragment?lang=en");
		Node root = getTreeData(url);
		Element[] UARoot = findNodeById(root,
				"/org.eclipse.ua.tests/data/help/toc/root.xml");
		assertThat(UARoot).hasSize(1);
	}

	@Test
	public void testTocFragmentServiceContainsFilteredToc() throws Exception {
		int port = WebappManager.getPort();
		URL url = new URL("http", "localhost", port,
				"/help/vs/service/tocfragment?lang=en&toc=/org.eclipse.ua.tests/data/help/toc/root.xml&path=2");
		Node root = getTreeData(url);
		Element[] UARoot = findNodeById(root,
				"/org.eclipse.ua.tests/data/help/toc/root.xml");
		assertThat(UARoot).hasSize(1);
		Element[] filterNode = findNodeById(UARoot[0], "2");
		assertThat(filterNode).hasSize(1);
		Element[] results = findHref(filterNode[0], "node",
				"../topic/org.eclipse.ua.tests/data/help/toc/filteredToc/simple_page.html");
		assertThat(results).hasSize(24);

		results = findHref(filterNode[0], "node",
		"../topic/org.eclipse.ua.tests/data/help/toc/filteredToc/helpInstalled.html");
		assertThat(results).hasSize(1);
	}

	@Test
	public void testTocFragmentServiceReadEnToc() throws Exception {
		int uaSearch = findUATopicIndex("search", "en");
		assertTrue(uaSearch >= 0);
		int port = WebappManager.getPort();
		URL url = new URL("http", "localhost", port,
				"/help/vs/service/tocfragment?lang=en&toc=/org.eclipse.ua.tests/data/help/toc/root.xml&path=" + uaSearch);
		Node root = getTreeData(url);
		Element[] UARoot = findNodeById(root,
				"/org.eclipse.ua.tests/data/help/toc/root.xml");
		assertThat(UARoot).hasSize(1);
		Element[] searchNode = findChildren(UARoot[0], "node", "title", "search");
		assertThat(searchNode).hasSize(1);
		Element[] topicEn = findHref(searchNode[0], "node",
				"../topic/org.eclipse.ua.tests/data/help/search/test_en.html");
		assertThat(topicEn).hasSize(1);
		Element[] topicDe = findHref(searchNode[0], "node",
				"../topic/org.eclipse.ua.tests/data/help/search/test_de.html");
		assertThat(topicDe).isEmpty();
	}

	private int findUATopicIndex(String title, String locale) {
		int index = -1;
		Toc[] tocs = HelpPlugin.getTocManager().getTocs(locale);
		for (Toc toc : tocs) {
			if ("/org.eclipse.ua.tests/data/help/toc/root.xml".equals(toc.getHref())) {
				ITopic[] topics = toc.getTopics();
				for (int j = 0; j < topics.length; j++) {
					if (title.equals(topics[j].getLabel())) {
						index = j;
					}
				}
			}
		}
		return index;
	}

	@Test
	public void testTocFragmentServiceReadDeToc() throws Exception {
		int helpMode = BaseHelpSystem.getMode();
		BaseHelpSystem.setMode(BaseHelpSystem.MODE_INFOCENTER);
		int uaSearch = findUATopicIndex("search", "de");
		assertTrue(uaSearch >= 0);
		int port = WebappManager.getPort();
		URL url = new URL("http", "localhost", port,
				"/help/vs/service/tocfragment?lang=de&toc=/org.eclipse.ua.tests/data/help/toc/root.xml&path=" + uaSearch);
		Node root = getTreeData(url);
		Element[] UARoot = findNodeById(root,
				"/org.eclipse.ua.tests/data/help/toc/root.xml");
		assertThat(UARoot).hasSize(1);
		Element[] searchNode = findChildren(UARoot[0], "node", "title", "search");
		Element[] topicEn = findHref(searchNode[0], "node",
				"../topic/org.eclipse.ua.tests/data/help/search/test_en.html");
		assertThat(topicEn).isEmpty();
		Element[] topicDe = findHref(searchNode[0], "node",
				"../topic/org.eclipse.ua.tests/data/help/search/test_de.html");
		assertThat(topicDe).hasSize(1);
		BaseHelpSystem.setMode(helpMode);
	}

	private Element[] findNodeById(Node root, String id) {
		return findChildren(root, "node", "id", id);
	}

	private Element[] findChildren(Node parent, String childKind,
			String attributeName, String attributeValue) {
		NodeList nodes = parent.getChildNodes();
		List<Node> results = new ArrayList<>();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node next = nodes.item(i);
			if (next instanceof Element) {
				Element nextElement = (Element)next;
				if ( childKind.equals(nextElement.getTagName())
						&& attributeValue.equals(nextElement.getAttribute(attributeName))) {

					results.add(next);
				}
			}
		}
		return results.toArray(new Element[results.size()]);
	}

	/*
	 * Look for a matching href, the query part of the href is not compared
	 */
	private Element[] findHref(Node parent, String childKind,
			 String attributeValue) {
		NodeList nodes = parent.getChildNodes();
		List<Node> results = new ArrayList<>();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node next = nodes.item(i);
			if (next instanceof Element) {
				Element nextElement = (Element)next;
				if ( childKind.equals(nextElement.getTagName()) ) {
					String href = nextElement.getAttribute("href");
					if (href != null) {
						int query = href.indexOf('?');
						if (query >= 0) {
							href = href.substring(0, query);
						}
						if (href.equals (attributeValue)) {
							results.add(next);
						}
					}
				}
			}
		}
		return results.toArray(new Element[results.size()]);
	}


	private Node getTreeData(URL url)
			throws Exception {
		try (InputStream is = url.openStream()) {
			InputSource inputSource = new InputSource(is);
			Document document = LocalEntityResolver.parse(inputSource);
			Node root = document.getFirstChild();
			assertEquals("tree_data", root.getNodeName());
			return root;
		}
	}

	@Test
	public void testTocFragmentServiceXMLSchema()
			throws Exception {
		int port = WebappManager.getPort();
		URL url = new URL("http", "localhost", port, "/help/vs/service/tocfragment?lang=en");
		URL schemaUrl = new URL("http", "localhost", port, "/help/test/schema/xml/tocfragment.xsd");
		String schema = schemaUrl.toString();
		String uri = url.toString();
		String result = SchemaValidator.testXMLSchema(uri, schema);

		assertThat(result).as("URL: " + uri).isEqualTo("valid");
	}

	/*
	public void testTocFragmentServiceJSONSchema()
			throws Exception {
		fail("Not yet implemented.");
	}
	*/

}
