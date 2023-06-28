/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
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
package org.eclipse.unittest.internal.ui;

import org.osgi.service.prefs.BackingStoreException;

import org.eclipse.unittest.internal.UnitTestPlugin;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

/**
 * Defines constants which are used to refer to values in the plugin's
 * preference store.
 */
public class UnitTestUIPreferencesConstants {
	/**
	 * Boolean preference controlling whether newly launched Unit tests should be
	 * shown in all Unit Test views (in all windows).
	 */
	public static final String SHOW_IN_ALL_VIEWS = UnitTestPlugin.PLUGIN_ID + ".show_in_all_views"; //$NON-NLS-1$

	/**
	 * A default value for SHOW_IN_ALL_VIEWS preference
	 */
	public static final boolean SHOW_IN_ALL_VIEWS_DEFAULT = false; // would need a PreferenceInitializer if this was
																	// changed to true!

	private UnitTestUIPreferencesConstants() {
		// no instance
	}

	/**
	 * Returns a value of SHOW_IN_ALL_VIEWS preference
	 *
	 * @return boolean value of SHOW_IN_ALL_VIEWS preference
	 */
	public static boolean getShowInAllViews() {
		return Platform.getPreferencesService().getBoolean(UnitTestPlugin.PLUGIN_ID, SHOW_IN_ALL_VIEWS,
				SHOW_IN_ALL_VIEWS_DEFAULT, null);
	}

	/**
	 * Sets a value of SHOW_IN_ALL_VIEWS preference
	 *
	 * @param show <code>true</code> if the results view is to be shown in all
	 *             views, otherwise <code>false</code>
	 */
	public static void setShowInAllViews(boolean show) {
		IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(UnitTestPlugin.PLUGIN_ID);
		preferences.putBoolean(SHOW_IN_ALL_VIEWS, show);
		try {
			preferences.flush();
		} catch (BackingStoreException e) {
			UnitTestPlugin.log(e);
		}
	}
}
