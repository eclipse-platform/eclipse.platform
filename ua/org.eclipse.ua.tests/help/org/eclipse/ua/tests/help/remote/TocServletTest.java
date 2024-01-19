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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.entityresolver.LocalEntityResolver;
import org.eclipse.help.internal.server.WebappManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class TocServletTest {

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
	public void testTocServletContainsUAToc() throws Exception {
		Node root = getTocContributions("en");
		Element[] UARoot = findContributionById(root, "/org.eclipse.ua.tests/data/help/toc/root.xml");
		assertThat(UARoot).hasSize(1);
	}

	@Test
	public void testTocServletContainsFilteredToc() throws Exception {
		Node root = getTocContributions("en");
		Element[] UARoot = findContributionById(root, "/org.eclipse.ua.tests/data/help/toc/filteredToc/toc.xml");
		assertThat(UARoot).hasSize(1);
	}

	@Test
	public void testTocServletContainsUnlinkedToc() throws Exception {
		Node root = getTocContributions("en");
		Element[] UARoot = findContributionById(root, "/org.eclipse.ua.tests/data/help/toc/filteredToc/nonPrimaryToc.xml");
		assertThat(UARoot).hasSize(1);
	}

	@Test
	public void testReadEnToc() throws Exception {
		Node root = getTocContributions("en");
		Element[] uaRoot = findContributionById(root, "/org.eclipse.ua.tests/data/help/search/toc.xml");
		assertThat(uaRoot).hasSize(1);
		Element[] toc = findChildren(uaRoot[0], "toc", "label", "search");
		assertThat(toc).hasSize(1);
		Element[] topicSearch = findChildren(toc[0], "topic", "label", "search");
		assertThat(topicSearch).hasSize(1);
		Element[] topicEn = findChildren(topicSearch[0], "topic", "label", "testen.html");
		assertThat(topicEn).hasSize(1);
		Element[] topicDe = findChildren(topicSearch[0], "topic", "label", "testde.html");
		assertThat(topicDe).isEmpty();
	}

	@Test
	public void testReadDeToc() throws Exception {
		Node root = getTocContributions("de");
		Element[] uaRoot = findContributionById(root, "/org.eclipse.ua.tests/data/help/search/toc.xml");
		assertThat(uaRoot).hasSize(1);
		Element[] toc = findChildren(uaRoot[0], "toc", "label", "search");
		assertThat(toc).hasSize(1);
		Element[] topicSearch = findChildren(toc[0], "topic", "label", "search");
		assertThat(topicSearch).hasSize(1);
		Element[] topicEn = findChildren(topicSearch[0], "topic", "label", "testen.html");
		assertThat(topicEn).isEmpty();
		Element[] topicDe = findChildren(topicSearch[0], "topic", "label", "testde.html");
		assertThat(topicDe).hasSize(1);
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
			Document document = LocalEntityResolver.parse(inputSource);
			Node root = document.getFirstChild();
			assertEquals("tocContributions", root.getNodeName());
			return root;
		}
	}

}
