/*******************************************************************************
 * Copyright (c) 2004, 2016 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.preferences;

import java.text.MessageFormat;

import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

/**
 * A preference page for configuring launching preferences.
 *
 * @since 3.0.0
 */
public class LaunchingPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private Button fUseContextLaunching;
	private Button fUseOldLaunching;
	private Button fLaunchLastIfNotLaunchable;
	private Button fCheckParent;
	private Button fTerminatAndRelaunch;

	/**
	 * The default constructor
	 */
	public LaunchingPreferencePage() {
		super();
		setPreferenceStore(DebugUIPlugin.getDefault().getPreferenceStore());
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IDebugHelpContextIds.LAUNCHING_PREFERENCE_PAGE);
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_HORIZONTAL);
		//save dirty editors
		FieldEditor edit = new RadioGroupFieldEditor(IInternalDebugUIConstants.PREF_SAVE_DIRTY_EDITORS_BEFORE_LAUNCH, DebugPreferencesMessages.LaunchingPreferencePage_2, 3,
				 new String[][] {{DebugPreferencesMessages.LaunchingPreferencePage_3, MessageDialogWithToggle.ALWAYS},
				 {DebugPreferencesMessages.LaunchingPreferencePage_4, MessageDialogWithToggle.NEVER},
				 {DebugPreferencesMessages.LaunchingPreferencePage_5, MessageDialogWithToggle.PROMPT}},
				 comp,
				 true);
		addField(edit);

		//wait for build
		edit = new RadioGroupFieldEditor(IInternalDebugUIConstants.PREF_WAIT_FOR_BUILD,
				 DebugPreferencesMessages.LaunchingPreferencePage_6, 3,
				 new String[][] {{DebugPreferencesMessages.LaunchingPreferencePage_7, MessageDialogWithToggle.ALWAYS},
				 {DebugPreferencesMessages.LaunchingPreferencePage_8, MessageDialogWithToggle.NEVER},
				 {DebugPreferencesMessages.LaunchingPreferencePage_9, MessageDialogWithToggle.PROMPT}},
				 comp,
				 true);
		addField(edit);

		//re-launch in debug mode
		edit = new RadioGroupFieldEditor(IInternalDebugUIConstants.PREF_RELAUNCH_IN_DEBUG_MODE,
				 DebugPreferencesMessages.LaunchingPreferencePage_15, 3,
				 new String[][] {{DebugPreferencesMessages.LaunchingPreferencePage_16, MessageDialogWithToggle.ALWAYS},
				 {DebugPreferencesMessages.LaunchingPreferencePage_17, MessageDialogWithToggle.NEVER},
				 {DebugPreferencesMessages.LaunchingPreferencePage_18, MessageDialogWithToggle.PROMPT}},
				 comp,
				 true);
		addField(edit);

		//continue with compile errors
		edit = new RadioGroupFieldEditor(IInternalDebugUIConstants.PREF_CONTINUE_WITH_COMPILE_ERROR,
				 DebugPreferencesMessages.LaunchingPreferencePage_21, 2,
				 new String[][] {{DebugPreferencesMessages.LaunchingPreferencePage_22, MessageDialogWithToggle.ALWAYS},
				 {DebugPreferencesMessages.LaunchingPreferencePage_23, MessageDialogWithToggle.PROMPT}},
				 comp,
				 true);
		addField(edit);

		//filtering options
		Group group = SWTFactory.createGroup(comp, DebugPreferencesMessages.LaunchingPreferencePage_36, 1, 1, GridData.FILL_HORIZONTAL);
		Composite spacer = SWTFactory.createComposite(group, 1, 1, GridData.FILL_HORIZONTAL);
		edit = new BooleanFieldEditor(IDebugUIConstants.PREF_BUILD_BEFORE_LAUNCH, DebugPreferencesMessages.LaunchingPreferencePage_1, SWT.NONE, spacer);
		edit.fillIntoGrid(spacer, 2);
		addField(edit);
		edit = new BooleanFieldEditor(IDebugUIConstants.PREF_AUTO_REMOVE_OLD_LAUNCHES, DebugPreferencesMessages.LaunchingPreferencePage_10, SWT.NONE, spacer);
		edit.fillIntoGrid(spacer, 2);
		addField(edit);

		edit = new BooleanFieldEditor(IInternalDebugUIConstants.PREF_REMOVE_FROM_LAUNCH_HISTORY, DebugPreferencesMessages.LaunchingPreferencePage_confirm_0, spacer);
		edit.fillIntoGrid(spacer, 2);
		addField(edit);

		//history list size preference
		IntegerFieldEditor editor = new IntegerFieldEditor(IDebugUIConstants.PREF_MAX_HISTORY_SIZE, DebugPreferencesMessages.DebugPreferencePage_10, spacer);
		editor.fillIntoGrid(spacer, 2);
		addField(editor);
		int historyMax = IDebugPreferenceConstants.MAX_LAUNCH_HISTORY_SIZE;
		editor.setTextLimit(Integer.toString(historyMax).length());
		editor.setErrorMessage(MessageFormat.format(DebugPreferencesMessages.DebugPreferencePage_11, new Object[] { Integer.valueOf(1), Integer.valueOf(historyMax)}));
		editor.setValidateStrategy(StringFieldEditor.VALIDATE_ON_KEY_STROKE);
		editor.setValidRange(1, historyMax);
		editor.setEmptyStringAllowed(false);

		//CONTEXTLAUNCHING
		createContextLaunchingControls(comp);
		initialize();
		checkState();
		return comp;
	}

	/**
	 * Creates the context launching portion of the page, which includes two radio buttons and
	 * a nested check box
	 * @param parent the parent to add this control to
	 *
	 * @since 3.3.0
	 * CONTEXTLAUNCHING
	 */
	private void createContextLaunchingControls(Composite parent) {
		Group group = SWTFactory.createGroup(parent, DebugPreferencesMessages.LaunchingPreferencePage_40, 1, 1, GridData.FILL_HORIZONTAL);
		fUseOldLaunching = SWTFactory.createRadioButton(group, DebugPreferencesMessages.LaunchingPreferencePage_37);
		fUseContextLaunching = SWTFactory.createRadioButton(group, DebugPreferencesMessages.LaunchingPreferencePage_38);
		fUseContextLaunching.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean enabled = ((Button)e.widget).getSelection();
				fCheckParent.setEnabled(enabled);
				fLaunchLastIfNotLaunchable.setEnabled(enabled);
			}
		});
		Composite space = SWTFactory.createComposite(group, 1, 1, GridData.FILL_HORIZONTAL);
		GridData gd = (GridData) space.getLayoutData();
		gd.horizontalIndent = 10;
		GridLayout layout = (GridLayout) space.getLayout();
		layout.marginHeight = 0;
		fCheckParent = SWTFactory.createRadioButton(space, DebugPreferencesMessages.LaunchingPreferencePage_39);
		fLaunchLastIfNotLaunchable = SWTFactory.createRadioButton(space, DebugPreferencesMessages.LaunchingPreferencePage_41);

		fTerminatAndRelaunch = SWTFactory.createCheckButton(group, DebugPreferencesMessages.LaunchingPreferencePage_42, null, false, 1);

		// initialize the buttons
		boolean value = getPreferenceStore().getBoolean(IInternalDebugUIConstants.PREF_USE_CONTEXTUAL_LAUNCH);
		fUseOldLaunching.setSelection(!value);
		fUseContextLaunching.setSelection(value);
		boolean enable = getPreferenceStore().getBoolean(IInternalDebugUIConstants.PREF_LAUNCH_PARENT_PROJECT);
		fCheckParent.setSelection(enable);
		fCheckParent.setEnabled(value);
		fLaunchLastIfNotLaunchable.setSelection(!enable);
		fLaunchLastIfNotLaunchable.setEnabled(value);

		value = getPreferenceStore().getBoolean(IInternalDebugUIConstants.PREF_TERMINATE_AND_RELAUNCH_LAUNCH_ACTION);
		fTerminatAndRelaunch.setSelection(value);
	}

	@Override
	public void init(IWorkbench workbench) {}

	@Override
	protected void performDefaults() {
		boolean value = getPreferenceStore().getDefaultBoolean(IInternalDebugUIConstants.PREF_USE_CONTEXTUAL_LAUNCH);
		fUseOldLaunching.setSelection(!value);
		fUseContextLaunching.setSelection(value);
		boolean parent = getPreferenceStore().getDefaultBoolean(IInternalDebugUIConstants.PREF_LAUNCH_PARENT_PROJECT);
		fCheckParent.setSelection(parent);
		fCheckParent.setEnabled(value);
		fLaunchLastIfNotLaunchable.setSelection(!parent);
		fLaunchLastIfNotLaunchable.setEnabled(value);
		value = getPreferenceStore().getDefaultBoolean(IInternalDebugUIConstants.PREF_TERMINATE_AND_RELAUNCH_LAUNCH_ACTION);
		fTerminatAndRelaunch.setSelection(value);
		super.performDefaults();
	}

	@Override
	public boolean performOk() {
		getPreferenceStore().setValue(IInternalDebugUIConstants.PREF_USE_CONTEXTUAL_LAUNCH, fUseContextLaunching.getSelection());
		getPreferenceStore().setValue(IInternalDebugUIConstants.PREF_LAUNCH_PARENT_PROJECT, fCheckParent.getSelection());
		getPreferenceStore().setValue(IInternalDebugUIConstants.PREF_LAUNCH_LAST_IF_NOT_LAUNCHABLE, fLaunchLastIfNotLaunchable.getSelection());
		getPreferenceStore().setValue(IInternalDebugUIConstants.PREF_TERMINATE_AND_RELAUNCH_LAUNCH_ACTION, fTerminatAndRelaunch.getSelection());
		return super.performOk();
	}

	@Override
	protected void createFieldEditors() {
		//do nothing we overload the create contents method
	}
}
