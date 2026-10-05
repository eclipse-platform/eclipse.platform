/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.terminal.view.ui.internal.view;

import org.eclipse.jface.action.Action;
import org.eclipse.terminal.view.ui.internal.Messages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * Action that opens the Terminal preferences dialog.
 */
public class TerminalShowPreferencesAction extends Action {

	/**
	 * The id of the primary terminal preference page to open.
	 */
	private static final String TERMINAL_PREF_PAGE = "org.eclipse.terminal.TerminalPreferencePage"; //$NON-NLS-1$

	/**
	 * The set of preference pages shown in the dialog.
	 */
	private static final String[] PAGES_TO_SHOW = { TERMINAL_PREF_PAGE, "org.eclipse.terminal.view.ui.preferences" };//$NON-NLS-1$

	public TerminalShowPreferencesAction() {
		super(Messages.TerminalShowPreferencesAction_label);
		setToolTipText(Messages.TerminalShowPreferencesAction_tooltip);
	}

	@Override
	public void run() {
		PreferencesUtil.createPreferenceDialogOn(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
				TERMINAL_PREF_PAGE, PAGES_TO_SHOW, null).open();
	}
}
