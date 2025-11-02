/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.ui.internal.console;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleFactory;

public class ConsoleViewConsoleFactory implements IConsoleFactory {

	int counter = 1;
	private ConsoleView currentConsoleView;

	@Override
	public void openConsole() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window == null) {
			return;
		}
		IWorkbenchPage page = window.getActivePage();
		if (page == null) {
			return;
		}
		boolean shouldPin = handleAutoPin();
		try {
			String secondaryId = "Console View #" + counter; //$NON-NLS-1$
			IViewPart view = page.showView(IConsoleConstants.ID_CONSOLE_VIEW, secondaryId, 1);
			if (view instanceof ConsoleView newConsoleView) {
				newConsoleView.setPinned(shouldPin);
			}
			counter++;
		} catch (PartInitException e) {
			ConsolePlugin.log(e);
		}
	}

	/**
	 * This handler checks if the remember auto-pin decision state <b>not true</b>
	 * and asks the user if auto pin of the view content should be enabled.
	 * Afterwards it checks if remember auto-pin decision was checked and sets the
	 * preference according to that
	 *
	 * If the remember auto-pin decision state is <b>true</b> it gathers the auto
	 * pin preference value and sets this to the current view.
	 */
	private boolean handleAutoPin() {
		if (currentConsoleView == null) {
			return false;
		}
		IPreferenceStore store = ConsolePlugin.getDefault().getPreferenceStore();
		if (!store.getBoolean(IConsoleConstants.REMEMBER_AUTO_PIN_DECISION_PREF_NAME)) {
			Shell shell = Display.getDefault().getActiveShell();
			MessageDialogWithToggle toggleDialog = MessageDialogWithToggle.openOkCancelConfirm(shell,
					ConsoleMessages.TurnOnAutoPinDialogTitle, ConsoleMessages.TurnOnAutoPinDialogMessage,
					ConsoleMessages.TurnOnAutoPinRememberDecision, false, null, null);

			store.setValue(IConsoleConstants.AUTO_PIN_ENABLED_PREF_NAME,
					toggleDialog.getReturnCode() == IDialogConstants.OK_ID);

			store.setValue(IConsoleConstants.REMEMBER_AUTO_PIN_DECISION_PREF_NAME, toggleDialog.getToggleState());
		}

		if (store.getBoolean(IConsoleConstants.AUTO_PIN_ENABLED_PREF_NAME)) {
			// To avoid if pinned manually and unpin due to preference..
			currentConsoleView.setPinned(true);
			return true;
		}
		return false;
	}

	/**
	 * Sets the console view, on which the open new console action was called.
	 */
	void setConsoleView(ConsoleView consoleView) {
		this.currentConsoleView = consoleView;
	}

}
