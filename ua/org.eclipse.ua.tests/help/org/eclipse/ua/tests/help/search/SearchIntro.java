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

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.search.ISearchQuery;
import org.eclipse.help.internal.search.SearchHit;
import org.eclipse.help.internal.search.SearchQuery;
import org.eclipse.help.internal.search.SearchResults;
import org.junit.Test;

public class SearchIntro {

	public SearchHit[] findHits(String searchWord) {
		ISearchQuery query = new SearchQuery(searchWord, false, new ArrayList<>(), Platform.getNL());
		SearchResults collector = new SearchResults(null, 10, Platform.getNL());
		BaseHelpSystem.getSearchManager().search(query, collector, new NullProgressMonitor());
		return collector.getSearchHits();
	}

	@Test
	public void testSearchIntroGroupLabel() {
		SearchHit[] hits = SearchTestUtils.getSearchHits("ifirifjrnfj", "en");
		assertEquals(1, hits.length);
	}

	@Test
	public void testSearchIntroGroupText() {
		SearchHit[] hits = SearchTestUtils.getSearchHits("nenfhhdhhed", "en");
		assertEquals(1, hits.length);
	}

	@Test
	public void testSearchIntroLinkLabel() {
		SearchHit[] hits = SearchTestUtils.getSearchHits("hydefefed", "en");
		assertEquals(1, hits.length);
	}

	@Test
	public void testSearchIntroLinkText() {
		SearchHit[] hits = SearchTestUtils.getSearchHits("hfuejfujduj", "en");
		assertEquals(1, hits.length);
	}

}
