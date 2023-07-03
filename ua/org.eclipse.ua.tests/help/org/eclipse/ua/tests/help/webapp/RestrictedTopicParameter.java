/*******************************************************************************
 *  Copyright (c) 2008, 2016 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ua.tests.help.webapp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.webapp.data.UrlUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for function which determines whether a topic path can be passed to the content frame
 */

public class RestrictedTopicParameter {

	private static final String RESTRICT_TOPIC = "restrictTopicParameter";
	private boolean restrictTopic;
	private int helpMode;

	@Before
	public void setUp() throws Exception {
		restrictTopic = Platform.getPreferencesService().getBoolean
		 (HelpBasePlugin.PLUGIN_ID, RESTRICT_TOPIC,
				  false, null);
		helpMode = BaseHelpSystem.getMode();
	}

	@After
	public void tearDown() throws Exception {
		setRestrictTopic(restrictTopic);
		BaseHelpSystem.setMode(helpMode);
	}

	private void setRestrictTopic(boolean isRestrict) {
		IEclipsePreferences pref = InstanceScope.INSTANCE.getNode(HelpBasePlugin.PLUGIN_ID);
		pref.putBoolean(RESTRICT_TOPIC, isRestrict);
	}

	@Test
	public void testWorkbenchMode() {
		BaseHelpSystem.setMode(BaseHelpSystem.MODE_WORKBENCH);
		setRestrictTopic(true);
		assertFalse(UrlUtil.isValidTopicParamOrWasOpenedFromHelpDisplay("http://www.eclipse.org"));
		assertFalse(UrlUtil.isValidTopicParamOrWasOpenedFromHelpDisplay("https://www.eclipse.org"));
		setRestrictTopic(false);
		assertTrue(UrlUtil.isValidTopicParamOrWasOpenedFromHelpDisplay("http://www.eclipse.org"));
		assertTrue(UrlUtil.isValidTopicParamOrWasOpenedFromHelpDisplay("https://www.eclipse.org"));
	}

	@Test
	public void testStandaloneMode() {
		BaseHelpSystem.setMode(BaseHelpSystem.MODE_STANDALONE);
		setRestrictTopic(true);
		assertFalse(UrlUtil.isValidTopicParamOrWasOpenedFromHelpDisplay("http://www.eclipse.org"));
		assertFalse(UrlUtil.isValidTopicParamOrWasOpenedFromHelpDisplay("https://www.eclipse.org"));
		setRestrictTopic(false);
		assertTrue(UrlUtil.isValidTopicParamOrWasOpenedFromHelpDisplay("http://www.eclipse.org"));
		assertTrue(UrlUtil.isValidTopicParamOrWasOpenedFromHelpDisplay("https://www.eclipse.org"));
	}

	@Test
	public void testInfocenterUnrestricted() {
		BaseHelpSystem.setMode(BaseHelpSystem.MODE_INFOCENTER);
		setRestrictTopic(false);
		assertTrue(UrlUtil.isValidTopicParamOrWasOpenedFromHelpDisplay("http://www.eclipse.org"));
		assertTrue(UrlUtil.isValidTopicParamOrWasOpenedFromHelpDisplay("https://www.eclipse.org"));
		assertTrue(UrlUtil.isValidTopicParamOrWasOpenedFromHelpDisplay("org.eclipse.platform.doc.user/reference/ref-43.htm"));
	}

	@Test
	public void testInfocenterResestricted() {
		BaseHelpSystem.setMode(BaseHelpSystem.MODE_INFOCENTER);
		setRestrictTopic(true);
		assertFalse(UrlUtil.isValidTopicParamOrWasOpenedFromHelpDisplay("http://www.eclipse.org"));
		assertFalse(UrlUtil.isValidTopicParamOrWasOpenedFromHelpDisplay("https://www.eclipse.org"));
		assertFalse(UrlUtil.isValidTopicParamOrWasOpenedFromHelpDisplay("HTTP://www.eclipse.org"));
		assertFalse(UrlUtil.isValidTopicParamOrWasOpenedFromHelpDisplay("file://somepath.html"));
		assertTrue(UrlUtil.isValidTopicParamOrWasOpenedFromHelpDisplay("org.eclipse.platform.doc.user/reference/ref-43.htm"));
	}

}
