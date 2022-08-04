/*******************************************************************************
 * Copyright (c) 2012-2022 Mihai Nita and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.ui.internal.console.ansi.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.handlers.HandlerUtil;

public class ShowQuickPrefsHandler extends AbstractHandler {
	private static final String PREF_PAGE_NAME =
			org.eclipse.ui.internal.console.ansi.preferences.AnsiConsolePreferencePage.class.getName();

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Shell shell = HandlerUtil.getActiveShell(event);
		if (shell != null) {
			final PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(
					shell, PREF_PAGE_NAME, /* show all pages */ null, null);
			dialog.open();
		}
		return null;
	}
}
