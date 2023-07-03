/*******************************************************************************
 * Copyright (c) 2010, 2016 IBM Corporation and others.
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
import org.eclipse.ua.tests.help.util.ParallelTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ParallelSearchServletTest {

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

	private class SearchServletTester implements ParallelTestSupport.ITestCase {
		private String phrase;
		private int expectedHits;

		public SearchServletTester(String phrase, int expectedHits) {
			this.phrase = phrase;
			this.expectedHits = expectedHits;
		}

		@Override
		public String runTest() throws Exception {
			Node[] hits = getSearchHitsFromServlet(phrase);
			if (hits.length != expectedHits) {
				return "Searching for " + phrase + " got "
						+ hits.length + " hits, expected " + expectedHits;
			}
			return null;
		}
	}

	@Test
	public void testNotFoundNonParallel() {
		ParallelTestSupport.testSingleCase(new SearchServletTester("duernfryehd", 0), 100);
	}

	@Test
	public void testFoundNonParallel() {
		ParallelTestSupport.testSingleCase(new SearchServletTester("jehcyqpfjs", 1), 100);
	}

	@Test
	public void testNotFoundInParallel() {
		ParallelTestSupport.testInParallel(new SearchServletTester[]
			 { new SearchServletTester("duernfryehd", 0),
			   new SearchServletTester("duernfryehd", 0)}, 100);
	}

	@Test
	public void testFoundInParallel() {
		ParallelTestSupport.testInParallel(new SearchServletTester[]
			 { new SearchServletTester("jehcyqpfjs", 1),
			   new SearchServletTester("jehcyqpfjs", 1)}, 100);
	}

	@Test
	public void testMixedParallelSearches() {
		ParallelTestSupport.testInParallel(new SearchServletTester[]
			 { new SearchServletTester("jehcyqpfjs", 1),
			   new SearchServletTester("duernfryehd", 0),
			   new SearchServletTester("jehcyqpfjs", 1),
			   new SearchServletTester("duernfryehd", 0)}, 100);
	}

	private Node[] getSearchHitsFromServlet(String phrase)
			throws Exception {
		int port = WebappManager.getPort();
		URL url = new URL("http", "localhost", port,
				"/help/search?phrase=" + URLEncoder.encode(phrase, StandardCharsets.UTF_8));
		return makeServletCall(url);
	}

	private Node[] makeServletCall(URL url) throws IOException,
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
