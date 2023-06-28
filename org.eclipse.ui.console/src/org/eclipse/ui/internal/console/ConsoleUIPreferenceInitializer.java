/*******************************************************************************
 * Copyright (c) 2018, 2020 Andrey Loskutov <loskutov@gmx.de> and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov <loskutov@gmx.de> - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.console;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsoleConstants;

public class ConsoleUIPreferenceInitializer extends AbstractPreferenceInitializer {

	public ConsoleUIPreferenceInitializer() {
		super();
	}

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore prefs = ConsolePlugin.getDefault().getPreferenceStore();
		prefs.setDefault(IConsoleConstants.P_CONSOLE_AUTO_SCROLL_LOCK, true);
		prefs.setDefault(IConsoleConstants.P_CONSOLE_WORD_WRAP, false);
	}

}
