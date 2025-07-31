/*******************************************************************************
 * Copyright (c) 2011, 2025 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 * Dirk Fauth <dirk.fauth@googlemail.com> - Bug 460496
 * Alexander Fedorov (ArSysOp) - further evolution
 *******************************************************************************/
package org.eclipse.terminal.view.ui.internal.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.terminal.view.ui.IPreferenceKeys;
import org.eclipse.terminal.view.ui.internal.UIPlugin;

/**
 * Terminal default preferences initializer.
 */
public class PreferencesInitializer extends AbstractPreferenceInitializer {

	public PreferencesInitializer() {
	}

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore prefs = UIPlugin.getScopedPreferences();
		prefs.setDefault(IPreferenceKeys.PREF_REMOVE_TERMINATED_TERMINALS, true);
		prefs.setDefault(IPreferenceKeys.PREF_LOCAL_TERMINAL_INITIAL_CWD, IPreferenceKeys.PREF_INITIAL_CWD_USER_HOME);
		prefs.setDefault(IPreferenceKeys.PREF_LOCAL_TERMINAL_DEFAULT_SHELL_UNIX, ""); //$NON-NLS-1$
	}
}
