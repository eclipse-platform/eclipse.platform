/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;

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
	
	protected Control createCustomArea(Composite parent) {
		editor = new RadioGroupFieldEditor(ICompareUIConstants.PREF_NAVIGATION_END_ACTION_LOCAL, CompareMessages.NavigationEndDialog_1, 1,
				labelsAndValues,
				parent, true);
		editor.setPreferenceStore(CompareUIPlugin.getDefault().getPreferenceStore());
		editor.fillIntoGrid(parent, 1);
		editor.load();
		return parent;
	}
	
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID) {
			editor.store();
		}
		super.buttonPressed(buttonId);
	}

}
