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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.URL;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.search.AnalyzerDescriptor;
import org.eclipse.help.internal.search.SearchIndex;
import org.eclipse.help.internal.search.SearchIndexWithIndexingProgress;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SearchIndexCreation {

	private int mode;
	private AnalyzerDescriptor analyzerDesc;

	@Before
	public void setUp() throws Exception {
		BaseHelpSystem.ensureWebappRunning();
		mode = BaseHelpSystem.getMode();
		RemotePreferenceStore.savePreferences();
		RemotePreferenceStore.setMockRemoteServer();
		RemotePreferenceStore.disableErrorPage();
		BaseHelpSystem.setMode(BaseHelpSystem.MODE_INFOCENTER);
		HelpPlugin.getTocManager().getTocs("en");
		analyzerDesc = new AnalyzerDescriptor("en-us");
	}

	@After
	public void tearDown() throws Exception {
		RemotePreferenceStore.restorePreferences();
		BaseHelpSystem.setMode(mode);
	}

	@Test
	public void testSearchIndexMakesNoRemoteCalls() throws Throwable {
		int initialCallCount = MockContentServlet.getCallcount();
		SearchIndexWithIndexingProgress index = new SearchIndexWithIndexingProgress("en-us", analyzerDesc, HelpPlugin
				.getTocManager());
		index.beginAddBatch(true);
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/search/test7.html", true);
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/search/test8.htm", true);
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/search/test77.htm", false); // Does not exist
		index.endAddBatch(false, true);
		int finalCallCount = MockContentServlet.getCallcount();
		assertEquals("Remote server called", 0, finalCallCount - initialCallCount);
	}

	@Test
	public void testSearchIndexMakesNoRemoteCalls2() throws Throwable {
		int initialCallCount = MockContentServlet.getCallcount();
		SearchIndexWithIndexingProgress index = new SearchIndexWithIndexingProgress("en-us", analyzerDesc, HelpPlugin
				.getTocManager());
		index.beginAddBatch(true);
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/search/test7.html", true);
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/search/test8.htm", true);
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/search/test77.htm", false); // Does not exist
		index.endAddBatch(false, true);
		int finalCallCount = MockContentServlet.getCallcount();
		assertEquals("Remote server called", 0, finalCallCount - initialCallCount);
	}

	@Test
	public void testSearchIndexMakesNoRemoteCallsRemotePriority() throws Throwable {
		RemotePreferenceStore.setMockRemotePriority();
		int initialCallCount = MockContentServlet.getCallcount();
		SearchIndexWithIndexingProgress index = new SearchIndexWithIndexingProgress("en-us", analyzerDesc, HelpPlugin
				.getTocManager());
		index.beginAddBatch(true);
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/search/test7.html", true);
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/search/test8.htm", true);
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/search/test77.htm", false); // Does not exist
		index.endAddBatch(false, true);
		int finalCallCount = MockContentServlet.getCallcount();
		assertEquals("Remote server called", 0, finalCallCount - initialCallCount);
	}

	private void addHrefToIndex(SearchIndexWithIndexingProgress index,
			String doc, boolean exists) throws Throwable {
		URL url = SearchIndex.getIndexableURL(index.getLocale(), doc);
		IStatus status = index.addDocument(url.getFile(), url);
		if (exists && !status.isOK()) {
			if (status.getException() != null) {
				throw status.getException();
			}
			fail(doc + " status = " + status.getMessage());
		}
	}

}
