/*******************************************************************************
 * Copyright (c) 2008, 2016 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.search.ISearchQuery;
import org.eclipse.help.internal.search.SearchHit;
import org.eclipse.help.internal.search.SearchQuery;
import org.eclipse.help.internal.search.SearchResults;
import org.junit.Assert;

public class SearchTestUtils {

	public static void searchAllLocales(String searchWord, String[] hrefs) {
		searchOneLocale(searchWord, hrefs, "en");
		searchOneLocale(searchWord, hrefs, "de");
	}

	public static void searchOneLocale(String searchWord, String[] hrefs, String nl) {
		String unexpected = searchForExpectedResults(searchWord, hrefs, nl);
		if (unexpected != null) {
			Assert.fail(unexpected);
		}
	}

	/**
	 * @return null if the expected results are returned, otherwise a string describing
	 * any discrepancies
	 */
	public static String searchForExpectedResults(String searchWord,
			String[] hrefs, String nl) {
		final Set<String> hrefsToFind = new HashSet<>();
		final Set<String> unexpectedHrefs = new HashSet<>();
		hrefsToFind.addAll(Arrays.asList(hrefs));

		SearchHit[] hits;
		hits = getSearchHits(searchWord, nl);
		for (SearchHit hit : hits) {
			String href = hit.getHref();
			// ignore query params
			int index = href.indexOf('?');
			if (index != -1) {
				href = href.substring(0, index);
			}
			if (hrefsToFind.contains(href)) {
				hrefsToFind.remove(href);
			}
			else {
				unexpectedHrefs.add(href);
			}
		}

		if (!hrefsToFind.isEmpty() || !unexpectedHrefs.isEmpty()) {
			StringBuilder buf = new StringBuilder();
			buf.append("While searching for: " + searchWord + ",\n");
			if (!hrefsToFind.isEmpty()) {
				buf.append("Some of the expected results were not found:\n");
				Iterator<String> iter = hrefsToFind.iterator();
				while (iter.hasNext()) {
					String missedHref = iter.next();
					buf.append(missedHref + "\n");
				}
			}
			if (!unexpectedHrefs.isEmpty()) {
				if (!hrefsToFind.isEmpty()) {
					buf.append("\nAlso,\n");
				}
				buf.append("Found some unexpected search results:\n");
				Iterator<String> iter = unexpectedHrefs.iterator();
				while (iter.hasNext()) {
					String unexpectedHref = iter.next();
					buf.append(unexpectedHref + "\n");
				}
			}
			return buf.toString();
		}
		return null;
	}

	public static SearchHit[] getSearchHits(String searchWord, String nl) {
		SearchHit[] hits;
		ISearchQuery query = new SearchQuery(searchWord, false, new ArrayList<>(), nl);
		SearchResults collector = new SearchResults(null, 500, nl);
		BaseHelpSystem.getSearchManager().search(query, collector, new NullProgressMonitor());
		hits = collector.getSearchHits();
		return hits;
	}

}
