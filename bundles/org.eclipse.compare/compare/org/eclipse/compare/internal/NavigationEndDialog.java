/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
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
import org.eclipse.jface.preference.RadioGroupFieldEditor;
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
