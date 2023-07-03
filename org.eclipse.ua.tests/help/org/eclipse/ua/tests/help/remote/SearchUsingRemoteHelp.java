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

package org.eclipse.ua.tests.help.remote;

import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.ua.tests.help.search.SearchTestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SearchUsingRemoteHelp {

	private int mode;

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
	public void testSearchDefaultLocale() throws Exception {
		BaseHelpSystem.setMode(BaseHelpSystem.MODE_INFOCENTER);
		RemotePreferenceStore.setMockRemoteServer();
		SearchTestUtils.searchOneLocale("endfdsadsads", new String[] { "http://www.eclipse.org" },
				"en");
		RemotePreferenceStore.disableRemoteHelp();
		SearchTestUtils.searchOneLocale("endfdsadsads", new String[0], "en");
	}

	@Test
	public void testSearchDefaultLocaleTwoServers() throws Exception {
		BaseHelpSystem.setMode(BaseHelpSystem.MODE_INFOCENTER);
		RemotePreferenceStore.setTwoMockRemoteServers();
		SearchTestUtils.searchOneLocale("endfdsadsads", new String[] { "http://www.eclipse.org" },
				"en");
		RemotePreferenceStore.disableRemoteHelp();
		SearchTestUtils.searchOneLocale("endfdsadsads", new String[0], "en");
	}

	@Test
	public void testSearchDeWordInDeLocale() throws Exception {
		BaseHelpSystem.setMode(BaseHelpSystem.MODE_INFOCENTER);
		RemotePreferenceStore.setMockRemoteServer();
		SearchTestUtils.searchOneLocale("dedfdsadsads", new String[] { "http://www.eclipse.org" }, "de");
		RemotePreferenceStore.disableRemoteHelp();
		SearchTestUtils.searchOneLocale("dedfdsadsads", new String[0], "de");
	}

	@Test
	public void testSearchEnWordInDeLocale() throws Exception {
		BaseHelpSystem.setMode(BaseHelpSystem.MODE_INFOCENTER);
		RemotePreferenceStore.setMockRemoteServer();
		SearchTestUtils.searchOneLocale("endfdsadsads", new String[0], "de");
		RemotePreferenceStore.disableRemoteHelp();
	}

	@Test
	public void testSearchDeWordInEnLocale() throws Exception {
		BaseHelpSystem.setMode(BaseHelpSystem.MODE_INFOCENTER);
		RemotePreferenceStore.setMockRemoteServer();
		SearchTestUtils.searchOneLocale("dedfdsadsads", new String[0], "en");
		RemotePreferenceStore.disableRemoteHelp();
	}

}
