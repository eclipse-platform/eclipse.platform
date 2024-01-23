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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.search.ISearchQuery;
import org.eclipse.help.internal.search.SearchHit;
import org.eclipse.help.internal.search.SearchQuery;
import org.eclipse.help.internal.search.SearchResults;
import org.junit.jupiter.api.Test;

public class SearchIntro {

	public SearchHit[] findHits(String searchWord) {
		ISearchQuery query = new SearchQuery(searchWord, false, new ArrayList<>(), Platform.getNL());
		SearchResults collector = new SearchResults(null, 10, Platform.getNL());
		BaseHelpSystem.getSearchManager().search(query, collector, new NullProgressMonitor());
		return collector.getSearchHits();
	}

	@Test
	public void testSearchIntroGroupLabel() {
		assertThat(SearchTestUtils.getSearchHits("ifirifjrnfj", "en")).hasSize(1);
	}

	@Test
	public void testSearchIntroGroupText() {
		assertThat(SearchTestUtils.getSearchHits("nenfhhdhhed", "en")).hasSize(1);
	}

	@Test
	public void testSearchIntroLinkLabel() {
		assertThat(SearchTestUtils.getSearchHits("hydefefed", "en")).hasSize(1);
	}

	@Test
	public void testSearchIntroLinkText() {
		assertThat(SearchTestUtils.getSearchHits("hfuejfujduj", "en")).hasSize(1);
	}

}
