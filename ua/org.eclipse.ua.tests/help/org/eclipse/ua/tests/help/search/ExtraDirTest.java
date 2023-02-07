/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.search.ISearchQuery;
import org.eclipse.help.internal.search.SearchHit;
import org.eclipse.help.internal.search.SearchQuery;
import org.eclipse.help.internal.search.SearchResults;
import org.eclipse.help.internal.workingset.AdaptableHelpResource;
import org.eclipse.help.internal.workingset.AdaptableToc;
import org.eclipse.help.internal.workingset.WorkingSet;
import org.eclipse.help.internal.workingset.WorkingSetManager;
import org.junit.Assert;
import org.junit.Test;

public class ExtraDirTest {

	// try without search scope
	@Test
	public void testNoScope()
	{
		performSearch("iusazemhdv", null, "/org.eclipse.ua.tests/data/help/search/test8.htm");
	}

	// try with containing toc's search scope
	@Test
	public void testContainingScope()
	{
		performSearch("iusazemhdv", "/org.eclipse.ua.tests/data/help/toc/root.xml", "/org.eclipse.ua.tests/data/help/search/test8.htm" );
	}

	// try with another toc's search scope; shouldn't find it
	@Test
	public void testNonContainingScope()
	{
		performSearch("iusazemhdv", "/org.eclipse.platform.doc.user/toc.xml", null );
	}

	// extradir doc, no search scope
	@Test
	public void testExtradirNoScope()
	{
		performSearch("xzopsujjae", null, "/org.eclipse.ua.tests/data/help/search/extraDir/extraDoc1.htm" );
	}

	// extradir doc, with correct search scope
	@Test
	public void testExtradirCorrectScope()
	{
		performSearch("xzopsujjae", "/org.eclipse.ua.tests/data/help/toc/root.xml", "/org.eclipse.ua.tests/data/help/search/extraDir/extraDoc1.htm" );
	}

	// extradir doc, with incorrect search scope
	@Test
	public void testExtradirIncorrectScope()
	{
		performSearch("xzopsujjae", "/org.eclipse.platform.doc.user/toc.xml", null );
	}

	// extradir doc (in subdir), no search scope
	@Test
	public void testExtradirSubdirNoScope()
	{
		performSearch("mrendiqwja", null, "/org.eclipse.ua.tests/data/help/search/extraDir/subDir/extraDoc2.htm" );
	}

	// extradir doc (in subdir), with correct search scope
	@Test
	public void testExtradirSubDirInScope()
	{
		performSearch("mrendiqwja", "/org.eclipse.ua.tests/data/help/toc/root.xml", "/org.eclipse.ua.tests/data/help/search/extraDir/subDir/extraDoc2.htm" );
	}

	// extradir doc (in subdir), with incorrect search scope
	@Test
	public void testExtradirSubDirOutOfScope()
	{
		performSearch("mrendiqwja", "/org.eclipse.platform.doc.user/toc.xml", null );
	}

	// extradir doc link_to'ed from another toc (toc3.xml), with incorrect scope
	@Test
	public void testExtradirLinkedOutOfScope()
	{
		performSearch("kleoiujfpn", "/org.eclipse.platform.doc.user/toc.xml", null );
	}

	// extradir doc linked into toc2.xml from toc4.xml, no scope
	@Test
	public void testExtradirLinkedNoScope()
	{
		performSearch("fuqnejwmfh", null, "/org.eclipse.ua.tests/data/help/search/extraDir3/extraDoc.htm" );
	}

	// extradir doc linked into toc2.xml from toc4.xml, correct scope
	@Test
	public void testExtradirLinkedInScope()
	{
		performSearch("fuqnejwmfh", "/org.eclipse.ua.tests/data/help/toc/root.xml", "/org.eclipse.ua.tests/data/help/search/extraDir3/extraDoc.htm" );
	}

	// extradir doc linked into toc2.xml from toc4.xml, incorrect scope
	@Test
	public void testExtradirLinkedWrongScope()
	{
		performSearch("fuqnejwmfh", "/org.eclipse.platform.doc.user/toc.xml", null );
	}

	private void performSearch(String searchWord, String scope,
			String expectedResults) {
		WorkingSet[] workingSets = null;
		if (scope != null) {
			WorkingSetManager wsm = BaseHelpSystem.getWorkingSetManager();
			List<AdaptableToc> tocs = new ArrayList<>();
			StringTokenizer tok = new StringTokenizer(scope, ", \t\n");
			while (tok.hasMoreTokens()) {
				tocs.add(wsm.getAdaptableToc(tok.nextToken()));
			}
			workingSets = new WorkingSet[] { wsm.createWorkingSet("testWorkingSet", tocs.toArray(new AdaptableHelpResource[tocs.size()])) };
		}

		// expected hits
		final Set<String> hrefsToFind = new HashSet<>();
		if (expectedResults != null) {
			StringTokenizer tok = new StringTokenizer(expectedResults, ", \t\n");
			while (tok.hasMoreTokens()) {
				hrefsToFind.add(tok.nextToken());
			}
		}

		// run test
		final Set<String> unexpectedHrefs = new HashSet<>();
		ISearchQuery query = new SearchQuery(searchWord, false, new ArrayList<>(), Platform.getNL());
		SearchResults collector = new SearchResults(workingSets, 500, Platform.getNL());
		BaseHelpSystem.getSearchManager().search(query, collector, new NullProgressMonitor());
		SearchHit[] hits = collector.getSearchHits();
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
			Assert.fail(buf.toString());
		}
	}
}
