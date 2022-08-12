/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.team.examples.pessimistic.ui;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.team.examples.pessimistic.IPessimisticFilesystemConstants;
import org.eclipse.team.examples.pessimistic.PessimisticFilesystemProviderPlugin;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * A preference page for the <code>PessimisticFilesystemProviderPlugin</code>.
 */
public class PessimisticPreferencesPage
	extends PreferencePage
	implements IWorkbenchPreferencePage {

	/*
	 * Widget for the files are edited preference
	 */
	private Combo filesAreEditedCombo;
	/*
	 * Widget for the files are edited without a context preference
	 */
	private Combo filesAreEditedNoPromptCombo;
	/*
	 * Widget for the files are saved preference
	 */
	private Combo filesAreSavedCombo;
	/*
	 * Widget for the files are edited preference
	 */
	private Combo addToControlCombo;
	/*
	 * Widget for the change file contents preference
	 */
	private Button changeFileContents;
	/*
	 * Widget for the fail validate edit preference
	 */
	private Button failValidateEdit;

	/*
	 * Option strings for the files are edited preference.
	 */
	private static final String[] EDIT_OPTION_STRINGS=
		new String[] {
			"Prompt to checkout",
			"Checkout",
			"Do nothing", };
	/*
	 * Option values for the files are edited preference.
	 */
	private static final int[] EDIT_OPTION_KEYS=
		new int[] {
			IPessimisticFilesystemConstants.OPTION_PROMPT,
			IPessimisticFilesystemConstants.OPTION_AUTOMATIC,
			IPessimisticFilesystemConstants.OPTION_DO_NOTHING, };

	/*
	 * Option strings for the files are edited without a context preference.
	 */
	private static final String[] EDIT_NO_PROMPT_OPTION_STRINGS=
		new String[] {
			"Checkout",
			"Do nothing", };

	/*
	 * Option strings for the files are saved preference.
	 */
	private static final String[] SAVE_OPTION_STRINGS=
		new String[] {
			"Checkout",
			"Do nothing", };
	/*
	 * Option values for the files are saved preference.
	 */
	private static final int[] SAVE_OPTION_KEYS=
		new int[] {
			IPessimisticFilesystemConstants.OPTION_AUTOMATIC,
			IPessimisticFilesystemConstants.OPTION_DO_NOTHING, };

	/*
	 * Option strings for the add to control preference.
	 */
	private static final String[] ADD_TO_CONTROL_OPTION_STRINGS=
		new String[] {
			"Prompt to add to control",
			"Add to control",
			"Do nothing", };
	/*
	 * Option values for the add to control preference.
	 */
	private static final int[] ADD_TO_CONTROL_OPTION_KEYS=
		new int[] {
			IPessimisticFilesystemConstants.OPTION_PROMPT,
			IPessimisticFilesystemConstants.OPTION_AUTOMATIC,
			IPessimisticFilesystemConstants.OPTION_DO_NOTHING, };


	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return PessimisticFilesystemProviderPlugin.getInstance().getPreferenceStore();
	}


	/*
	 * Sets the layout to be a grid layout with the given number of columns.
	 */
	protected void setDefaultLayout(Composite group, int columns) {
		GridLayout layout = new GridLayout();
		group.setLayout(layout);

		GridData data =
			new GridData(
				GridData.VERTICAL_ALIGN_FILL
					| GridData.HORIZONTAL_ALIGN_FILL
					| GridData.GRAB_HORIZONTAL);

		layout.numColumns = columns;
		group.setLayoutData(data);
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		setDefaultLayout(composite, 1);

		Composite options = new Composite(composite, SWT.NULL);
		setDefaultLayout(options, 2);

		Label label = new Label(options, SWT.NONE);
		label.setText("File handling:");
		GridData gridData = new GridData();
		gridData.horizontalSpan = 2;
		gridData.horizontalAlignment = GridData.FILL;
		label.setLayoutData(gridData);

		label = new Label(options, SWT.NONE);
		label.setText("When checked in files are edited:");
		gridData = new GridData();
		gridData.horizontalSpan = 1;
		gridData.horizontalAlignment = GridData.FILL;
		label.setLayoutData(gridData);

		filesAreEditedCombo= new Combo(options, SWT.BORDER | SWT.READ_ONLY);
		gridData = new GridData();
		gridData.horizontalSpan= 1;
		gridData.horizontalAlignment = GridData.FILL;
		filesAreEditedCombo.setLayoutData(gridData);
		filesAreEditedCombo.setItems(EDIT_OPTION_STRINGS);

		label = new Label(options, SWT.NONE);
		label.setText("When checked in files are edited programmatically:");
		gridData = new GridData();
		gridData.horizontalSpan = 1;
		gridData.horizontalAlignment = GridData.FILL;
		label.setLayoutData(gridData);

		filesAreEditedNoPromptCombo= new Combo(options, SWT.BORDER | SWT.READ_ONLY);
		gridData = new GridData();
		gridData.horizontalSpan= 1;
		gridData.horizontalAlignment = GridData.FILL;
		filesAreEditedNoPromptCombo.setLayoutData(gridData);
		filesAreEditedNoPromptCombo.setItems(EDIT_NO_PROMPT_OPTION_STRINGS);

		label = new Label(options, SWT.NONE);
		label.setText("When checked in files are saved:");
		gridData = new GridData();
		gridData.horizontalSpan = 1;
		gridData.horizontalAlignment = GridData.FILL;
		label.setLayoutData(gridData);

		filesAreSavedCombo= new Combo(options, SWT.BORDER | SWT.READ_ONLY);
		gridData = new GridData();
		gridData.horizontalSpan = 1;
		gridData.horizontalAlignment = GridData.FILL;
		filesAreSavedCombo.setLayoutData(gridData);
		filesAreSavedCombo.setItems(SAVE_OPTION_STRINGS);

		label = new Label(options, SWT.NONE);
		label.setText("When files are created:");
		gridData = new GridData();
		gridData.horizontalSpan = 1;
		gridData.horizontalAlignment = GridData.FILL;
		label.setLayoutData(gridData);

		addToControlCombo= new Combo(options, SWT.BORDER | SWT.READ_ONLY);
		gridData = new GridData();
		gridData.horizontalSpan = 1;
		gridData.horizontalAlignment = GridData.FILL;
		addToControlCombo.setLayoutData(gridData);
		addToControlCombo.setItems(ADD_TO_CONTROL_OPTION_STRINGS);

		options = new Composite(composite, SWT.NULL);
		setDefaultLayout(options, 1);

		label = new Label(options, SWT.NONE);
		label.setText("Error cases:");
		gridData = new GridData();
		gridData.horizontalSpan = 1;
		gridData.horizontalAlignment = GridData.FILL;
		label.setLayoutData(gridData);

		failValidateEdit= new Button(options, SWT.CHECK | SWT.LEFT);
		failValidateEdit.setText("Fail validate edit");
		gridData = new GridData();
		failValidateEdit.setLayoutData(gridData);

		changeFileContents= new Button(options, SWT.CHECK | SWT.LEFT);
		changeFileContents.setText("Touch files during validate edit");
		gridData = new GridData();
		changeFileContents.setLayoutData(gridData);

		updatePreferencePage();

		return composite;
	}

	@Override
	public void init(IWorkbench workbench) {
		// do nothing
	}

	@Override
	protected void performDefaults() {
		IPreferenceStore store = getPreferenceStore();

		filesAreEditedCombo.select(
			getEditOptionIndex(store.getDefaultInt(IPessimisticFilesystemConstants.PREF_CHECKED_IN_FILES_EDITED)));
		filesAreEditedNoPromptCombo.select(
			getEditNoPromptOptionIndex(store.getDefaultInt(IPessimisticFilesystemConstants.PREF_CHECKED_IN_FILES_EDITED_NOPROMPT)));
		filesAreSavedCombo.select(
			getSaveOptionIndex(store.getDefaultInt(IPessimisticFilesystemConstants.PREF_CHECKED_IN_FILES_SAVED)));
		addToControlCombo.select(
			getAddToControlOptionIndex(store.getDefaultInt(IPessimisticFilesystemConstants.PREF_ADD_TO_CONTROL)));
		failValidateEdit.setSelection(
			store.getDefaultBoolean(IPessimisticFilesystemConstants.PREF_FAIL_VALIDATE_EDIT));
		changeFileContents.setSelection(
			store.getDefaultBoolean(IPessimisticFilesystemConstants.PREF_TOUCH_DURING_VALIDATE_EDIT));
		super.performDefaults();
	}

	@Override
	public boolean performOk() {
		IPreferenceStore store = getPreferenceStore();

		int selectionIndex = filesAreEditedCombo.getSelectionIndex();
		if (selectionIndex != -1)
			store.setValue(
				IPessimisticFilesystemConstants.PREF_CHECKED_IN_FILES_EDITED,
				EDIT_OPTION_KEYS[selectionIndex]);
		selectionIndex = filesAreEditedNoPromptCombo.getSelectionIndex();
		if (selectionIndex != -1)
			store.setValue(
				IPessimisticFilesystemConstants.PREF_CHECKED_IN_FILES_EDITED_NOPROMPT,
				SAVE_OPTION_KEYS[selectionIndex]);
		selectionIndex = filesAreSavedCombo.getSelectionIndex();
		if (selectionIndex != -1)
			store.setValue(
				IPessimisticFilesystemConstants.PREF_CHECKED_IN_FILES_SAVED,
				SAVE_OPTION_KEYS[selectionIndex]);
		selectionIndex = addToControlCombo.getSelectionIndex();
		if (selectionIndex != -1)
			store.setValue(
				IPessimisticFilesystemConstants.PREF_ADD_TO_CONTROL,
				ADD_TO_CONTROL_OPTION_KEYS[selectionIndex]);
		store.setValue(
			IPessimisticFilesystemConstants.PREF_FAIL_VALIDATE_EDIT,
			failValidateEdit.getSelection());
		store.setValue(
			IPessimisticFilesystemConstants.PREF_TOUCH_DURING_VALIDATE_EDIT,
			changeFileContents.getSelection());
		return true;
	}

	/*
	 * Sets the widgets to have the state stored in the preferences.
	 */
	protected void updatePreferencePage() {
		IPreferenceStore store = getPreferenceStore();

		filesAreEditedCombo.select(
			getEditOptionIndex(store.getInt(IPessimisticFilesystemConstants.PREF_CHECKED_IN_FILES_EDITED)));
		filesAreEditedNoPromptCombo.select(
			getEditNoPromptOptionIndex(store.getInt(IPessimisticFilesystemConstants.PREF_CHECKED_IN_FILES_EDITED_NOPROMPT)));
		filesAreSavedCombo.select(
			getSaveOptionIndex(store.getInt(IPessimisticFilesystemConstants.PREF_CHECKED_IN_FILES_SAVED)));
		addToControlCombo.select(
			getAddToControlOptionIndex(store.getInt(IPessimisticFilesystemConstants.PREF_ADD_TO_CONTROL)));
		failValidateEdit.setSelection(
			store.getBoolean(IPessimisticFilesystemConstants.PREF_FAIL_VALIDATE_EDIT));
		changeFileContents.setSelection(
			store.getBoolean(IPessimisticFilesystemConstants.PREF_TOUCH_DURING_VALIDATE_EDIT));
	}

	/*
	 * Answers the index of the given key.
	 */
	protected int getEditOptionIndex(int key) {
		for(int i= 0; i < EDIT_OPTION_KEYS.length; i++) {
			if (EDIT_OPTION_KEYS[i] == key)
				return i;
		}
		return -1;
	}

	/*
	 * Answers the index of the given key.
	 */
	protected int getSaveOptionIndex(int key) {
		for(int i= 0; i < SAVE_OPTION_KEYS.length; i++) {
			if (SAVE_OPTION_KEYS[i] == key)
				return i;
		}
		return -1;
	}

	/*
	 * Answers the index of the given key.
	 */
	protected int getEditNoPromptOptionIndex(int key) {
		for(int i= 0; i < SAVE_OPTION_KEYS.length; i++) {
			if (SAVE_OPTION_KEYS[i] == key)
				return i;
		}
		return -1;
	}

	/*
	 * Answers the index of the given key.
	 */
	protected int getAddToControlOptionIndex(int key) {
		for(int i= 0; i < ADD_TO_CONTROL_OPTION_KEYS.length; i++) {
			if (ADD_TO_CONTROL_OPTION_KEYS[i] == key)
				return i;
		}
		return -1;
	}

}