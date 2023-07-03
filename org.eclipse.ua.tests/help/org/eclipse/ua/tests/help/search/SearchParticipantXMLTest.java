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


public class SearchParticipantXMLTest {
	@Test
	public void testSearchFirstWordInFirstDoc() {
		SearchTestUtils.searchAllLocales("jfplepdl", new String[] { MockSearchParticipantXML.DOC_1 });
	}

	@Test
	public void testSearchLastWordInFirstDoc() {
		SearchTestUtils.searchAllLocales("memdjkemd", new String[] { MockSearchParticipantXML.DOC_1 });
	}

	@Test
	public void testSearchWordFromOuterElement() {
		SearchTestUtils.searchAllLocales("odoeofoedo", new String[] { MockSearchParticipantXML.DOC_2 });
	}

	@Test
	public void testSearchWordFromNestedElement() {
		SearchTestUtils.searchAllLocales("odkeofkeks", new String[] { MockSearchParticipantXML.DOC_2 });
	}

	@Test
	public void testReturnedTitle() {
		SearchHit[] hits = SearchTestUtils.getSearchHits("jfplepdl", "en");
		assertEquals(hits.length,1);
		assertEquals("Participant XML 1", hits[0].getLabel());
	}

	@Test
	public void testReturnedSummary() {
		SearchHit[] hits = SearchTestUtils.getSearchHits("jfplepdl", "en");
		assertEquals(hits.length,1);
		assertEquals("Summary for file Participant XML1", hits[0].getSummary());
	}

	@Test
	public void testReturnedTitleNestedCase() {
		SearchHit[] hits = SearchTestUtils.getSearchHits("odoeofoedo", "en");
		assertEquals(hits.length,1);
		assertEquals("Participant XML 2 - tests nesting", hits[0].getLabel());
	}

	@Test
	public void testReturnedSummaryNestedCase() {
		SearchHit[] hits = SearchTestUtils.getSearchHits("odoeofoedo", "en");
		assertEquals(hits.length,1);
		assertEquals("Summary for file Participant XML2", hits[0].getSummary());
	}

}
