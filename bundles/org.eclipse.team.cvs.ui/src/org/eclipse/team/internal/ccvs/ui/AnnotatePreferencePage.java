/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.*;

public class AnnotatePreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private RadioGroupFieldEditor binaryPrompt = null;

	public AnnotatePreferencePage() {
		super(GRID);
		setTitle(CVSUIMessages.AnnotatePreferencePage_AnnotatePrefPageTitle);
		setDescription(CVSUIMessages.AnnotatePreferencePage_AnnotatePrefPageMessage);
		setPreferenceStore(CVSUIPlugin.getPlugin().getPreferenceStore());
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.ANNOTATE_VIEW);
	}

	@Override
	public void createFieldEditors() {
		binaryPrompt = new RadioGroupFieldEditor(ICVSUIConstants.PREF_ANNOTATE_PROMPTFORBINARY, CVSUIMessages.AnnotatePreferencePage_AnnotatePrefPageBinaryFileMessage, 3, new String[][] { {CVSUIMessages.CVSPreferencesPage_11, MessageDialogWithToggle.ALWAYS}, {CVSUIMessages.CVSPreferencesPage_12, MessageDialogWithToggle.NEVER}, {CVSUIMessages.CVSPreferencesPage_13, MessageDialogWithToggle.PROMPT}}, getFieldEditorParent(), true /* use a group */);
		addField(binaryPrompt);

		Dialog.applyDialogFont(getFieldEditorParent());
		getFieldEditorParent().layout(true);
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	public boolean performOk() {
		CVSUIPlugin.getPlugin().savePluginPreferences();
		return super.performOk();
	}
}
