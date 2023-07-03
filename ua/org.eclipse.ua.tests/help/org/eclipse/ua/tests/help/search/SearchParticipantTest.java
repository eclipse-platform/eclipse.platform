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

import org.eclipse.help.internal.search.SearchHit;
import org.junit.Test;


public class SearchParticipantTest {
	@Test
	public void testSearchFirstWordInFirstDoc() {
		SearchTestUtils.searchAllLocales("jkijkijkk", new String[] { "/org.eclipse.ua.tests/participant1.xml" });
	}

	@Test
	public void testSearchLastWordInFirstDoc() {
		SearchTestUtils.searchAllLocales("frgeded", new String[] { "/org.eclipse.ua.tests/participant1.xml" });
	}

	@Test
	public void testSearchUsingAndInFirstDoc() {
		SearchTestUtils.searchAllLocales("jkijkijkk AND frgeded", new String[] { "/org.eclipse.ua.tests/participant1.xml" });
	}

	@Test
	public void testSearchUsingAndInSeparateDocs() {
		SearchTestUtils.searchAllLocales("jduehdye and olhoykk", new String[0]);
	}

	@Test
	public void testSearchExactMatch() {
		SearchTestUtils.searchAllLocales("\"jkijkijkk frgeded\"", new String[] { "/org.eclipse.ua.tests/participant1.xml" });
	}

	@Test
	public void testSearchExactMatchNotFound() {
		SearchTestUtils.searchAllLocales("\"frgeded jkijkijkk\"", new String[0]);
	}

	@Test
	public void testSearchWordInSecondDoc() {
		SearchTestUtils.searchAllLocales("olhoykk", new String[] { "/org.eclipse.ua.tests/participant2.xml" });
	}

	@Test
	public void testReturnedTitle() {
		SearchHit[] hits = SearchTestUtils.getSearchHits("jkijkijkk", "en");
		assertEquals(hits.length,1);
		assertEquals("Title1", hits[0].getLabel());
	}

	@Test
	public void testReturnedSummary() {
		SearchHit[] hits = SearchTestUtils.getSearchHits("jkijkijkk", "en");
		assertEquals(hits.length,1);
		assertEquals("Summary1", hits[0].getSummary());
	}

}
