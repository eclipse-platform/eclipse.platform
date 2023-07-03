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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;

import org.eclipse.help.AbstractTocProvider;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.base.remote.RemoteTocProvider;
import org.eclipse.help.internal.toc.TocContribution;
import org.eclipse.help.internal.toc.TocFileProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TocManagerTest {

	private int mode;

	@Before
	public void setUp() throws Exception {
		BaseHelpSystem.ensureWebappRunning();
		mode = BaseHelpSystem.getMode();
		RemotePreferenceStore.savePreferences();
		BaseHelpSystem.setMode(BaseHelpSystem.MODE_INFOCENTER);
	}

	@After
	public void tearDown() throws Exception {
		BaseHelpSystem.setMode(mode);
		RemotePreferenceStore.restorePreferences();
	}

	@Test
	public void testDuplicatesOneRemote() throws Exception {

		RemotePreferenceStore.setMockRemoteServer();
		HelpPlugin.getTocManager().clearCache();
		boolean hasDuplicates=hasDuplicateContributions(HelpPlugin.getTocManager().getTocContributions("en"));
		assertFalse(hasDuplicates);
	}

	@Test
	public void testDuplicatesTwoRemote() throws Exception {

		RemotePreferenceStore.setTwoMockRemoteServers();
		HelpPlugin.getTocManager().clearCache();
		boolean hasDuplicates=hasDuplicateContributions(HelpPlugin.getTocManager().getTocContributions("en"));
		assertFalse(hasDuplicates);
	}

	@Test
	public void testLocalProviderPriority() throws Exception {

		int localPriority=0,remotePriority=0;
		RemotePreferenceStore.setMockRemoteServer();
		RemotePreferenceStore.setMockLocalPriority();
		HelpPlugin.getTocManager().clearCache();
		AbstractTocProvider [] tocProviders = HelpPlugin.getTocManager().getTocProviders();
		for (AbstractTocProvider tocProvider : tocProviders) {
			if(tocProvider instanceof TocFileProvider)
				localPriority = tocProvider.getPriority();

			if(tocProvider instanceof RemoteTocProvider)
				remotePriority = tocProvider.getPriority();
		}

		assertTrue(localPriority<remotePriority);
	}

	@Test
	public void testRemoteProviderPriority() throws Exception {

		RemotePreferenceStore.setMockRemoteServer();
		RemotePreferenceStore.setMockRemotePriority();
		HelpPlugin.getTocManager().clearCache();
		int localPriority=0,remotePriority=0;

		AbstractTocProvider [] tocProviders = HelpPlugin.getTocManager().getTocProviders();
		for (AbstractTocProvider tocProvider : tocProviders) {
			if(tocProvider instanceof TocFileProvider)
				localPriority = tocProvider.getPriority();

			if(tocProvider instanceof RemoteTocProvider)
				remotePriority = tocProvider.getPriority();
		}

		assertTrue(remotePriority<localPriority);
	}

	public static boolean hasDuplicateContributions(TocContribution[] tocContributions)
	{
		HashSet<String> contributionsFound = new HashSet<>();

		for (TocContribution tocContribution : tocContributions) {
			if(contributionsFound.contains(tocContribution.getId()))
				return true;
			else
				contributionsFound.add(tocContribution.getId());
		}

		return false;
	}
}
