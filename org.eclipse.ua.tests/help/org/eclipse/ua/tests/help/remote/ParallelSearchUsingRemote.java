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

import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.ua.tests.help.search.SearchTestUtils;
import org.eclipse.ua.tests.help.util.ParallelTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ParallelSearchUsingRemote {

	private class Searcher implements ParallelTestSupport.ITestCase {

	private int count = 0;
		@Override
		public String runTest() throws Exception {
			count++;
			return SearchTestUtils.searchForExpectedResults
					(searchWords[count%3], expectedResults[count%3], "en");
		}
	}

	private int mode;
	private String[] searchWords = new String[] {"endfdsadsads", "dedfdsadsads", "jehcyqpfjs" };
	private String[][] expectedResults = new String[][] {
			new String[] { "http://www.eclipse.org" },
			new String[0],
			new String[] { "/org.eclipse.ua.tests/data/help/search/test1.xhtml" }
	};

	@Before
	public void setUp() throws Exception {
		RemotePreferenceStore.savePreferences();
		mode = BaseHelpSystem.getMode();
	}

	@After
	public void tearDown() throws Exception {
		RemotePreferenceStore.restorePreferences();
		BaseHelpSystem.setMode(mode);
	}

	@Test
	public void testSearchOnOneThreadWithRemote() throws Exception {
		BaseHelpSystem.setMode(BaseHelpSystem.MODE_INFOCENTER);
		RemotePreferenceStore.setMockRemoteServer();
		ParallelTestSupport.testSingleCase(new Searcher(), 100);
		RemotePreferenceStore.disableRemoteHelp();
	}

	@Test
	public void testSearchInParallelWithRemote() throws Exception {
		BaseHelpSystem.setMode(BaseHelpSystem.MODE_INFOCENTER);
		RemotePreferenceStore.setMockRemoteServer();
		ParallelTestSupport.ITestCase[] testCases = new Searcher[3];
		for (int i = 0; i < 3; i++) {
			testCases[i] = new Searcher();
		}
		ParallelTestSupport.testInParallel(testCases, 100);
		RemotePreferenceStore.disableRemoteHelp();
	}

}
