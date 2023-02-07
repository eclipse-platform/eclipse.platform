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

import static org.junit.Assert.assertEquals;

import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.toc.Toc;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LoadTocUsingRemoteHelp {

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
	public void testTocContribution() throws Exception {
		BaseHelpSystem.setMode(BaseHelpSystem.MODE_INFOCENTER);
		RemotePreferenceStore.setMockRemoteServer();
		HelpPlugin.getTocManager().clearCache();
		Toc[] tocs = HelpPlugin.getTocManager().getTocs("en");
		int enTocs = matchingTocs(tocs, "Mock Toc en");
		assertEquals(1, enTocs);
		enTocs = matchingTocs(tocs, "Mock Toc 2 en");
		assertEquals(1, enTocs);
		int deTocs = matchingTocs(tocs, "Mock Toc de");
		assertEquals(0, deTocs);
		deTocs = matchingTocs(tocs, "Mock Toc 2 de");
		assertEquals(0, deTocs);
		RemotePreferenceStore.disableRemoteHelp();
	}

	@Test
	public void testTocContributionDe() throws Exception {
		BaseHelpSystem.setMode(BaseHelpSystem.MODE_INFOCENTER);
		RemotePreferenceStore.setMockRemoteServer();
		HelpPlugin.getTocManager().clearCache();
		Toc[] tocs = HelpPlugin.getTocManager().getTocs("de");
		int enTocs = matchingTocs(tocs, "Mock Toc en");
		assertEquals(0, enTocs);
		enTocs = matchingTocs(tocs, "Mock Toc 2 en");
		assertEquals(0, enTocs);
		int deTocs = matchingTocs(tocs, "Mock Toc de");
		assertEquals(1, deTocs);
		deTocs = matchingTocs(tocs, "Mock Toc 2 de");
		assertEquals(1, deTocs);
		RemotePreferenceStore.disableRemoteHelp();
	}

	@Test
	public void testTocContributionFromTwoServers() throws Exception {
		BaseHelpSystem.setMode(BaseHelpSystem.MODE_INFOCENTER);
		RemotePreferenceStore.setTwoMockRemoteServers();
		HelpPlugin.getTocManager().clearCache();
		Toc[] tocs = HelpPlugin.getTocManager().getTocs("en");
		int enTocs = matchingTocs(tocs, "Mock Toc en");
		assertEquals(1, enTocs);
		int deTocs = matchingTocs(tocs, "Mock Toc de");
		assertEquals(0, deTocs);
		RemotePreferenceStore.disableRemoteHelp();
	}

	/*
	 * Return the number of tocs with this label
	 */
	private int matchingTocs(Toc[] tocs, String label) {
		int result = 0;
		for (Toc toc : tocs) {
			if (label.equals(toc.getLabel())) {
				result += 1;
			}
		}
		return result;
	}

}
