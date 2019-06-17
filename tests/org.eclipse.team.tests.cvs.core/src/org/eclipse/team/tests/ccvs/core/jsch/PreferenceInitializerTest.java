/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - ongoing maintenance
 *******************************************************************************/
package org.eclipse.team.tests.ccvs.core.jsch;

import java.io.File;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jsch.internal.core.IConstants;
import org.eclipse.jsch.internal.core.JSchCorePlugin;
import org.eclipse.jsch.internal.core.PreferenceInitializer;
import org.eclipse.team.tests.ccvs.core.EclipseTest;

public class PreferenceInitializerTest extends EclipseTest {

	private boolean PREF_HAS_CHANGED_DEFAULT_WIN32_SSH_HOME_value = false;
	private boolean PREF_HAS_MIGRATED_SSH2_PREFS_value = false;
	private String KEY_SSH2HOME_value = null;
	private String KEY_PRIVATEKEY_value = null;

	@Override
	protected void setUp() throws Exception {
		if (!Platform.getOS().equals(Platform.OS_WIN32))
			return;

		super.setUp();
		// remembering preferences changed in test
		IEclipsePreferences preferences = InstanceScope.INSTANCE
				.getNode(JSchCorePlugin.ID);
		PREF_HAS_CHANGED_DEFAULT_WIN32_SSH_HOME_value = preferences.getBoolean(
				IConstants.PREF_HAS_CHANGED_DEFAULT_WIN32_SSH_HOME, false);
		PREF_HAS_MIGRATED_SSH2_PREFS_value = preferences.getBoolean(
				IConstants.PREF_HAS_MIGRATED_SSH2_PREFS, false);
		KEY_SSH2HOME_value = preferences.get(IConstants.KEY_SSH2HOME, null);
		KEY_PRIVATEKEY_value = preferences.get(IConstants.KEY_PRIVATEKEY, null);
	}

	@Override
	protected void tearDown() throws Exception {
		if (!Platform.getOS().equals(Platform.OS_WIN32))
			return;

		super.tearDown();
		// restoring preferences changed in test
		IEclipsePreferences preferences = InstanceScope.INSTANCE
				.getNode(JSchCorePlugin.ID);
		preferences.putBoolean(
				IConstants.PREF_HAS_CHANGED_DEFAULT_WIN32_SSH_HOME,
				PREF_HAS_CHANGED_DEFAULT_WIN32_SSH_HOME_value);
		preferences.putBoolean(IConstants.PREF_HAS_MIGRATED_SSH2_PREFS,
				PREF_HAS_MIGRATED_SSH2_PREFS_value);
		if (KEY_SSH2HOME_value != null)
			preferences.put(IConstants.KEY_SSH2HOME, KEY_SSH2HOME_value);
		else
			preferences.remove(IConstants.KEY_SSH2HOME);
		if (KEY_PRIVATEKEY_value != null)
			preferences.put(IConstants.KEY_PRIVATEKEY, KEY_PRIVATEKEY_value);
		else
			preferences.remove(IConstants.KEY_PRIVATEKEY);
	}

	public void testChangeDefaultWin32SshHomeOldWorkspace() {
		if (!Platform.getOS().equals(Platform.OS_WIN32))
			return;

		if (PreferenceInitializer.SSH_OLD_WIN32_HOME_DEFAULT != null) {
			File file = new File(
					PreferenceInitializer.SSH_OLD_WIN32_HOME_DEFAULT);
			if (!file.exists()) {
				file.mkdir();
			}
		}

		IEclipsePreferences preferences = InstanceScope.INSTANCE
				.getNode(JSchCorePlugin.ID);
		preferences.remove(IConstants.PREF_HAS_CHANGED_DEFAULT_WIN32_SSH_HOME);
		// this value indicates that this is an old workspace
		preferences.putBoolean(IConstants.PREF_HAS_MIGRATED_SSH2_PREFS, true);
		preferences.remove(IConstants.KEY_SSH2HOME);

		// verify that the preference is not set
		assertFalse(preferences.getBoolean(
				IConstants.PREF_HAS_CHANGED_DEFAULT_WIN32_SSH_HOME, false));
		// verify if this is an old workspace
		assertTrue(preferences.getBoolean(
				IConstants.PREF_HAS_MIGRATED_SSH2_PREFS, false));

		PreferenceInitializer preferenceInitializer = new PreferenceInitializer();
		preferenceInitializer.initializeDefaultPreferences();

		// verify that the preference is set now
		assertTrue(preferences.getBoolean(
				IConstants.PREF_HAS_CHANGED_DEFAULT_WIN32_SSH_HOME, false));
		assertEquals(PreferenceInitializer.SSH_OLD_WIN32_HOME_DEFAULT,
				preferences.get(IConstants.KEY_SSH2HOME, null));
	}

	public void _testDontChangeDefaultWin32SshHomeNewWorkspace() {
		if (!Platform.getOS().equals(Platform.OS_WIN32))
			return;

		if (PreferenceInitializer.SSH_OLD_WIN32_HOME_DEFAULT != null) {
			File file = new File(
					PreferenceInitializer.SSH_OLD_WIN32_HOME_DEFAULT);
			if (!file.exists()) {
				file.mkdir();
			}
		}

		IEclipsePreferences preferences = InstanceScope.INSTANCE
				.getNode(JSchCorePlugin.ID);
		preferences.remove(IConstants.PREF_HAS_CHANGED_DEFAULT_WIN32_SSH_HOME);
		// this value indicates that this is a new workspace
		preferences.remove(IConstants.PREF_HAS_MIGRATED_SSH2_PREFS);
		preferences.remove(IConstants.KEY_SSH2HOME);

		// verify that the preference is not set
		assertFalse(preferences.getBoolean(
				IConstants.PREF_HAS_CHANGED_DEFAULT_WIN32_SSH_HOME, false));
		// verify if this is a new workspace
		assertFalse(preferences.getBoolean(
				IConstants.PREF_HAS_MIGRATED_SSH2_PREFS, false));

		PreferenceInitializer preferenceInitializer = new PreferenceInitializer();
		preferenceInitializer.initializeDefaultPreferences();

		// verify that the preference is set now
		assertTrue(preferences.getBoolean(
				IConstants.PREF_HAS_CHANGED_DEFAULT_WIN32_SSH_HOME, false));
		assertNull(preferences.get(IConstants.KEY_SSH2HOME, null));
	}

	public static Test suite() {
		return new TestSuite(PreferenceInitializerTest.class);
	}
}
