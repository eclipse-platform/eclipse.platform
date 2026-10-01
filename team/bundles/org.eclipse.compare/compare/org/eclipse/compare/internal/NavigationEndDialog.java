/*******************************************************************************
 * Copyright (c) 2006, 2026 IBM Corporation and others.
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
package org.eclipse.compare.internal;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class NavigationEndDialog extends MessageDialogWithToggle {

	private final String[][] labelsAndValues;
	private RadioGroupFieldEditor editor;

	public NavigationEndDialog(Shell parentShell, String dialogTitle,
			Image dialogTitleImage, String dialogMessage, String[][] labelsAndValues) {
		super(parentShell, dialogTitle, dialogTitleImage, dialogMessage,
				QUESTION, new String[] { IDialogConstants.OK_LABEL , IDialogConstants.CANCEL_LABEL}, 0,
				CompareMessages.NavigationEndDialog_0, false);
		this.labelsAndValues = labelsAndValues;
	}

	/**
	 * Returns the navigation end action for running past the end or beginning of
	 * the displayed element, asking the user when the preference says so, or
	 * <code>null</code> when the user canceled.
	 */
	public static String chooseEndAction(Shell shell, boolean next) {
		IPreferenceStore store = CompareUIPlugin.getDefault().getPreferenceStore();
		String value = store.getString(ICompareUIConstants.PREF_NAVIGATION_END_ACTION);
		if (!value.equals(ICompareUIConstants.PREF_VALUE_PROMPT)) {
			return value;
		}
		shell.getDisplay().beep();
		String title;
		String message;
		String loopMessage;
		String nextMessage;
		if (next) {
			title = CompareMessages.TextMergeViewer_0;
			message = CompareMessages.TextMergeViewer_1;
			loopMessage = CompareMessages.TextMergeViewer_2;
			nextMessage = CompareMessages.TextMergeViewer_3;
		} else {
			title = CompareMessages.TextMergeViewer_4;
			message = CompareMessages.TextMergeViewer_5;
			loopMessage = CompareMessages.TextMergeViewer_6;
			nextMessage = CompareMessages.TextMergeViewer_7;
		}
		NavigationEndDialog dialog = new NavigationEndDialog(shell, title, null, message, new String[][] {
				{ loopMessage, ICompareUIConstants.PREF_VALUE_LOOP },
				{ nextMessage, ICompareUIConstants.PREF_VALUE_NEXT },
				{ CompareMessages.TextMergeViewer_17, ICompareUIConstants.PREF_VALUE_DO_NOTHING } });
		if (dialog.open() != Window.OK) {
			return null;
		}
		String chosen = store.getString(ICompareUIConstants.PREF_NAVIGATION_END_ACTION_LOCAL);
		if (dialog.getToggleState()) {
			store.putValue(ICompareUIConstants.PREF_NAVIGATION_END_ACTION, chosen);
			store.firePropertyChangeEvent(ICompareUIConstants.PREF_NAVIGATION_END_ACTION, value, chosen);
		}
		return chosen;
	}

	@Override
	protected Control createCustomArea(Composite parent) {
		editor = new RadioGroupFieldEditor(ICompareUIConstants.PREF_NAVIGATION_END_ACTION_LOCAL, CompareMessages.NavigationEndDialog_1, 1,
				labelsAndValues,
				parent, true);
		editor.setPreferenceStore(CompareUIPlugin.getDefault().getPreferenceStore());
		editor.fillIntoGrid(parent, 1);
		editor.load();
		return parent;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID) {
			editor.store();
		}
		super.buttonPressed(buttonId);
	}

}
