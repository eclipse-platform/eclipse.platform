/*******************************************************************************
 * Copyright (c) 2025 Holger Voormann and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.ua.tests.help.search;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Collections;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.help.IHelpResource;
import org.eclipse.help.internal.search.SearchQuery;
import org.eclipse.help.internal.search.SearchResult;
import org.eclipse.help.internal.search.federated.LocalHelp;
import org.eclipse.help.internal.search.federated.LocalHelpScope;
import org.eclipse.help.search.AbstractSearchProcessor;
import org.eclipse.help.search.ISearchEngineResult;
import org.eclipse.help.search.ISearchEngineResultCollector;
import org.eclipse.help.search.ISearchResult;
import org.eclipse.help.search.SearchProcessorInfo;
import org.junit.jupiter.api.Test;

public class SearchProcessorTest {

	@Test
	void testPreSearch() {
		var processor = new AbstractSearchProcessor() {

			@Override
			public SearchProcessorInfo preSearch(String query) {
				SearchProcessorInfo info = new SearchProcessorInfo();
				info.setQuery("jkijkijkk");
				return info;
			}

			@Override
			public ISearchResult[] postSearch(String query, ISearchResult[] results) {
				return null;
			}

		};
		test(processor, new String[] { "/org.eclipse.ua.tests/participant1.xml" });
	}

	@Test
	void testPostSearch() {
		var processor = new AbstractSearchProcessor() {

			@Override
			public SearchProcessorInfo preSearch(String query) {
				SearchProcessorInfo info = new SearchProcessorInfo();
				info.setQuery("jkijkijkk");
				return info;
			}

			@Override
			public ISearchResult[] postSearch(String query, ISearchResult[] results) {
				assertEquals("jkijkijkk", query);
				assertEquals(1, results.length);
				assertEquals("/org.eclipse.ua.tests/participant1.xml", withoutQueryPart(results[0].getHref()));
				var addedResult = new SearchResult();
				addedResult.setHref("/org.eclipse.ua.tests/added");
				return new ISearchResult[] { addedResult };
			}

		};
		test(processor, new String[] { "/org.eclipse.ua.tests/added" });
	}

	@Test
	void testExtendedPostSearch() {
		var processor = new AbstractSearchProcessor() {

			@Override
			public SearchProcessorInfo preSearch(String query) {
				SearchProcessorInfo info = new SearchProcessorInfo();
				info.setQuery("olhoykk");
				return info;
			}

			@Override
			public ISearchResult[] postSearch(String query, ISearchResult[] results) {
				return null;
			}

			@Override
			public ISearchResult[] postSearch(String query, String originalQuery, ISearchResult[] results,
					String locale, IHelpResource[] scopes) {
				assertEquals("olhoykk", query);
				assertEquals(WrappedSearchProcessor.QUERY, originalQuery);
				assertEquals(1, results.length);
				assertEquals("/org.eclipse.ua.tests/participant2.xml", withoutQueryPart(results[0].getHref()));
				assertEquals(new SearchQuery().getLocale(), locale);
				assertNull(scopes);
				var addedResult = new SearchResult();
				addedResult.setHref("/org.eclipse.ua.tests/added2");
				return new ISearchResult[] { addedResult };
			}

		};
		test(processor, new String[] { "/org.eclipse.ua.tests/added2" });
	}

	@Test
	void testNullProcessor() {
		var processor = new AbstractSearchProcessor() {

			@Override
			public SearchProcessorInfo preSearch(String query) {
				assertEquals(WrappedSearchProcessor.QUERY, query);
				return null;
			}

			@Override
			public ISearchResult[] postSearch(String query, ISearchResult[] results) {
				return null;
			}

		};
		test(processor, new String[0]);
	}

	private void test(AbstractSearchProcessor processor, String[] expected) {
		try (var autoClosable = WrappedSearchProcessor.set(processor)) {
			String[] hits = search(WrappedSearchProcessor.QUERY);
			assertArrayEquals(expected, hits);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e);
		}
	}

	private static String[] search(String query) throws Exception {

		var foundHrefs = new ArrayList<String>();
		ISearchEngineResultCollector collector = new ISearchEngineResultCollector() {

			@Override
			public void accept(ISearchEngineResult searchResult) {
				foundHrefs.add(withoutQueryPart(searchResult.getHref()));
			}

			@Override
			public void accept(ISearchEngineResult[] searchResults) {
				for (ISearchEngineResult searchResult : searchResults) {
					foundHrefs.add(withoutQueryPart(searchResult.getHref()));
				}
			}

			@Override
			public void error(IStatus status) {
				fail(new RuntimeException(status.getMessage(), status.getException()));
			}

		};
		new LocalHelp().run(query, new LocalHelpScope(null, false), collector, new NullProgressMonitor());
		Collections.sort(foundHrefs);
		return foundHrefs.toArray(String[]::new);
	}

	private static String withoutQueryPart(String href) {
		return href.indexOf('?') < 0 ? href : href.substring(0, href.indexOf('?'));
	}

}
