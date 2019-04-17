/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.*;

/**
 * This class acts as an abstract superclass for CVS preference pages that use
 * field editors.
 */
public abstract class CVSFieldEditorPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public static IPreferenceStore getCVSPreferenceStore() {
		return CVSUIPlugin.getPlugin().getPreferenceStore();
	}

	/**
	 * Constructor for CVSFieldEditorPreferencePage.
	 */
	public CVSFieldEditorPreferencePage() {
		super(GRID);
		setPreferenceStore(getCVSPreferenceStore());
		String description = getPageDescription();
		if (description != null)
			setDescription(description);
	}

	@Override
	protected Control createContents(Composite parent) {
		Control control = super.createContents(parent);
		String id = getPageHelpContextId();
		if (id != null)
			PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), id);
		Dialog.applyDialogFont(control);
		return control;
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	/**
	 * Method getPageHelpContextId must be overridden by subclasses to provide
	 * the help context ID of the page. Return null for no page F1 help.
	 *
	 * @return String
	 */
	protected abstract String getPageHelpContextId();

	/**
	 * Method getPageDescription must be overridden by subclasses to provide the
	 * description of the page. Return null for no description.
	 * @return String
	 */
	protected abstract String getPageDescription();

	@Override
	public boolean performOk() {
		if (!super.performOk()) return false;
		pushPreferences();
		return true;
	}

	/**
	 * Push the preferences to the Core plugin as required
	 */
	protected void pushPreferences() {
		// Do nothing by default
	}

}
