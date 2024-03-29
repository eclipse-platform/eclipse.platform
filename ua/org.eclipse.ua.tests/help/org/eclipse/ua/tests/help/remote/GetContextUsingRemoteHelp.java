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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.eclipse.help.IContext;
import org.eclipse.help.IContext3;
import org.eclipse.help.IHelpResource;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GetContextUsingRemoteHelp {

	private int mode;

	@BeforeEach
	public void setUp() throws Exception {
		RemotePreferenceStore.savePreferences();
		mode = BaseHelpSystem.getMode();
		RemotePreferenceStore.setMockRemoteServer();
		BaseHelpSystem.setMode(BaseHelpSystem.MODE_INFOCENTER);
	}

	@AfterEach
	public void tearDown() throws Exception {
		RemotePreferenceStore.restorePreferences();
		BaseHelpSystem.setMode(mode);
	}

	@Test
	public void testContextDefaultLocale() throws Exception {
		IContext context = HelpPlugin.getContextManager().getContext("org.eclipse.ua.tests.test_one", "en");
		assertNotNull(context);
		IHelpResource[] relatedTopics = context.getRelatedTopics();
		assertThat(relatedTopics).hasSize(1);
		String topicLabel = relatedTopics[0].getLabel();
		assertEquals("context_one_en", topicLabel);
		String title = ((IContext3)context).getTitle();
		assertEquals("context_one_en", title);
		RemotePreferenceStore.disableRemoteHelp();
		context = HelpPlugin.getContextManager().getContext("org.eclipse.ua.tests.test_one", "en");
		assertNull(context);
	}

	@Test
	public void testLocalContextBeatsRemote() throws Exception {
		IContext context = HelpPlugin.getContextManager().getContext("org.eclipse.ua.tests.test_cheatsheets", "en");
		assertNotNull(context);
		IHelpResource[] relatedTopics = context.getRelatedTopics();
		assertThat(relatedTopics).hasSize(1);
		String topicLabel = relatedTopics[0].getLabel();
		assertEquals("abcdefg", topicLabel);
	}

	@Test
	public void testContextDeLocale() throws Exception {
		IContext context = HelpPlugin.getContextManager().getContext("org.eclipse.ua.tests.test_one", "de");
		assertEquals("context_one_de", ((IContext3)context).getTitle());
	}

	@Test
	public void testContextNotFound() throws Exception {
		IContext context = HelpPlugin.getContextManager().getContext("org.eclipse.ua.tests.no_such_ctx", "en");
		assertNull(context);
	}

}
