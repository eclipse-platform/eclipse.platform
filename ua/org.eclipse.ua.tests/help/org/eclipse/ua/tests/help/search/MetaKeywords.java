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
 *******************************************************************************/
package org.eclipse.ua.tests.help.search;


import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.search.ISearchQuery;
import org.eclipse.help.internal.search.SearchHit;
import org.eclipse.help.internal.search.SearchQuery;
import org.eclipse.help.internal.search.SearchResults;
import org.junit.Test;


public class MetaKeywords {
	@Test
	public void testKeywordInHtml() {
		SearchTestUtils.searchAllLocales("ydhaedrsc", new String[] { "/org.eclipse.ua.tests/data/help/search/testMeta.htm" });
	}

	@Test
	public void testKeywordInXhtml() {
		SearchTestUtils.searchAllLocales("olfrgkjrifjd", new String[] { "/org.eclipse.ua.tests/data/help/search/testMeta.xhtml" });
	}

	@Test
	public void testDescriptionInHtml() {
		SearchHit[] hits = getResultDescriptions("ydhaedrsc", "en");
		assertEquals(hits.length, 1);
		assertEquals("HTML Meta description", hits[0].getDescription());
	}

	@Test
	public void testDescriptionInXhtml() {
		SearchHit[] hits = getResultDescriptions("olfrgkjrifjd", "en");
		assertEquals(hits.length, 1);
		assertEquals("XHTML Meta description", hits[0].getDescription());
	}

	@Test
	public void testSearchDescriptionInHtml() {
		SearchTestUtils.searchAllLocales("basbanba", new String[] { "/org.eclipse.ua.tests/data/help/search/extraDir/testMeta2.htm" });
	}

	@Test
	public void testSearchDescriptionInXhtml() {
		SearchTestUtils.searchAllLocales("mfjrudjesm", new String[] { "/org.eclipse.ua.tests/data/help/search/extraDir/testMeta2.xhtml" });
	}

	private SearchHit[] getResultDescriptions(String searchWord, String nl) {
		ISearchQuery query = new SearchQuery(searchWord, false, new ArrayList<>(), nl);
		SearchResults collector = new SearchResults(null, 500, nl);
		BaseHelpSystem.getSearchManager().search(query, collector, new NullProgressMonitor());
		return collector.getSearchHits();
	}


}
