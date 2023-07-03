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

package org.eclipse.ua.tests.help.search;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.search.ISearchQuery;
import org.eclipse.help.internal.search.SearchHit;
import org.eclipse.help.internal.search.SearchQuery;
import org.eclipse.help.internal.search.SearchResults;
import org.junit.Test;

public class SearchRanking {

	public SearchHit[] findHits(String searchWord) {
		ISearchQuery query = new SearchQuery(searchWord, false, new ArrayList<>(), Platform.getNL());
		SearchResults collector = new SearchResults(null, 10, Platform.getNL());
		BaseHelpSystem.getSearchManager().search(query, collector, new NullProgressMonitor());
		return collector.getSearchHits();
	}

	/**
	 * Verify that a match in a title has more weight than a match in the body
	 */
	@Test
	public void testTitleBoost1() {
		SearchHit[] hits = SearchTestUtils.getSearchHits("mjuhgt", "en");
		Arrays.sort(hits);
		assertEquals(2, hits.length);
		assertEquals("/org.eclipse.ua.tests/data/help/search/extraDir/ranking/ranktest1b.htm", getPath(hits[0]));
		assertEquals("/org.eclipse.ua.tests/data/help/search/extraDir/ranking/ranktest1a.htm", getPath(hits[1]));
	}

	@Test
	public void testTitleBoost2() {
		SearchHit[] hits = SearchTestUtils.getSearchHits("odrgtb", "en");
		Arrays.sort(hits);
		assertEquals(2, hits.length);
		assertEquals("/org.eclipse.ua.tests/data/help/search/extraDir/ranking/ranktest1a.htm", getPath(hits[0]));
		assertEquals("/org.eclipse.ua.tests/data/help/search/extraDir/ranking/ranktest1b.htm", getPath(hits[1]));
	}

	/**
	 * Verify that consecutive words raise the weight
	 */
	@Test
	public void testConsecutiveWords1() {
		SearchHit[] hits = SearchTestUtils.getSearchHits("iduhnf xaqsdab", "en");
		Arrays.sort(hits);
		assertEquals(2, hits.length);
		assertEquals("/org.eclipse.ua.tests/data/help/search/extraDir/ranking/ranktest2b.htm",
				getPath(hits[0]));
		assertEquals("/org.eclipse.ua.tests/data/help/search/extraDir/ranking/ranktest2a.htm",
				getPath(hits[1]));
	}

	@Test
	public void testConsecutiveWords2() {
		SearchHit[] hits = SearchTestUtils.getSearchHits("xaqsdab iduhnf", "en");
		Arrays.sort(hits);
		assertEquals(2, hits.length);
		assertEquals("/org.eclipse.ua.tests/data/help/search/extraDir/ranking/ranktest2a.htm",
				getPath(hits[0]));
		assertEquals("/org.eclipse.ua.tests/data/help/search/extraDir/ranking/ranktest2b.htm",
				getPath(hits[1]));
	}


	private String getPath(SearchHit hit) {
		String href = hit.getHref();
		int query = href.indexOf('?');
		return query < 0 ? href : href.substring(0, query);
	}

}
