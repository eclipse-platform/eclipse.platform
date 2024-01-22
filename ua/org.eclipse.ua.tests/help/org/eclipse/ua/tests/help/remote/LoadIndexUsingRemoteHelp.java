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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.help.IIndex;
import org.eclipse.help.IIndexEntry;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LoadIndexUsingRemoteHelp {

	private int mode;

	@Before
	public void setUp() throws Exception {
		RemotePreferenceStore.savePreferences();
		mode = BaseHelpSystem.getMode();
		BaseHelpSystem.setMode(BaseHelpSystem.MODE_INFOCENTER);
	}

	@After
	public void tearDown() throws Exception {
		RemotePreferenceStore.restorePreferences();
		BaseHelpSystem.setMode(mode);
	}

	@Test
	public void testIndexContribution() throws Exception {
		String locale = "en";
		HelpPlugin.getIndexManager().clearCache();
		IIndex index = HelpPlugin.getIndexManager().getIndex(locale);
		assertThat(matchingEntries(index, "entry1_" + locale)).isEmpty();
		assertThat(matchingEntries(index, "entry2_" + locale)).isEmpty();
		RemotePreferenceStore.setMockRemoteServer();
		HelpPlugin.getIndexManager().clearCache();
		index = HelpPlugin.getIndexManager().getIndex(locale);
		assertThat(matchingEntries(index, "entry1_" + locale)).hasSize(1);
		assertThat(matchingEntries(index, "entry2_" + locale)).hasSize(1);
	}

	@Test
	public void testIndexWithTwoRemoteServers() throws Exception {
		String locale = "en";
		HelpPlugin.getIndexManager().clearCache();
		IIndex index = HelpPlugin.getIndexManager().getIndex(locale);
		IIndexEntry[] entry1 = matchingEntries(index, "entry1_" + locale);
		assertThat(entry1).isEmpty();
		IIndexEntry[] entry2 = matchingEntries(index, "entry2_" + locale);
		assertThat(entry2).isEmpty();
		RemotePreferenceStore.setTwoMockRemoteServers();
		HelpPlugin.getIndexManager().clearCache();
		index = HelpPlugin.getIndexManager().getIndex(locale);
		// Entry 1 has the same child on each remote server, Entry 2 has different children
		entry1 = matchingEntries(index, "entry1_" + locale);
		entry2 = matchingEntries(index, "entry2_" + locale);
		assertThat(entry1).hasSize(1);
		assertThat(entry1[0].getTopics()).hasSize(1);
		assertThat(entry2).hasSize(1);
		assertThat(entry2[0].getTopics()).hasSize(2);
	}

	private IIndexEntry[] matchingEntries(IIndex index, String keyword) {
		List<IIndexEntry> matches = new ArrayList<>();
		IIndexEntry[] entries = index.getEntries();
		for (IIndexEntry entrie : entries) {
			if (keyword.equals(entrie.getKeyword())) {
				matches.add(entrie);
			}
		}
		return matches.toArray(new IIndexEntry[matches.size()]);
	}

}
