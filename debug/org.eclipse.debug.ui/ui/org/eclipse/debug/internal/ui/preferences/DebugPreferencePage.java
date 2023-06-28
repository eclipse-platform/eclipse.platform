/*******************************************************************************
 *  Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.preferences;

import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

/**
 * The page for setting debugger preferences.  Built on the 'field editor' infrastructure.
 */
public class DebugPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage, IDebugPreferenceConstants {

	public DebugPreferencePage() {
		super(GRID);

		IPreferenceStore store= DebugUIPlugin.getDefault().getPreferenceStore();
		setPreferenceStore(store);
		setDescription(DebugPreferencesMessages.DebugPreferencePage_1);
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IDebugHelpContextIds.DEBUG_PREFERENCE_PAGE);
	}

	@Override
	protected void createFieldEditors() {
		addField(new BooleanFieldEditor(IDebugUIConstants.PREF_REUSE_EDITOR, DebugPreferencesMessages.DebugPreferencePage_2, SWT.NONE, getFieldEditorParent()));
		addField(new BooleanFieldEditor(IDebugUIConstants.PREF_SHOW_VARIABLES_INLINE,
				DebugPreferencesMessages.DebugPreferencePage_showValuesInline, SWT.NONE, getFieldEditorParent()));

		SWTFactory.createHorizontalSpacer(getFieldEditorParent(), 2);

		addField(new BooleanFieldEditor(IDebugUIConstants.PREF_ACTIVATE_WORKBENCH, DebugPreferencesMessages.DebugPreferencePage_3, SWT.NONE, getFieldEditorParent()));
		addField(new BooleanFieldEditor(IInternalDebugUIConstants.PREF_ACTIVATE_DEBUG_VIEW, DebugPreferencesMessages.DebugPreferencePage_26, SWT.NONE, getFieldEditorParent()));

		SWTFactory.createHorizontalSpacer(getFieldEditorParent(), 2);

		addField(new BooleanFieldEditor(IDebugUIConstants.PREF_SKIP_BREAKPOINTS_DURING_RUN_TO_LINE, DebugPreferencesMessages.DebugPreferencePage_25, SWT.NONE, getFieldEditorParent()));
		addField(new BooleanFieldEditor(IDebugPreferenceConstants.PREF_PROMPT_REMOVE_ALL_BREAKPOINTS, DebugPreferencesMessages.DebugPreferencePage_29, SWT.NONE, getFieldEditorParent()));
		addField(new BooleanFieldEditor(IDebugPreferenceConstants.PREF_PROMPT_REMOVE_BREAKPOINTS_FROM_CONTAINER, DebugPreferencesMessages.DebugPreferencePage_30, SWT.NONE, getFieldEditorParent()));
		addField(new BooleanFieldEditor(IDebugPreferenceConstants.PREF_PROMPT_REMOVE_ALL_EXPRESSIONS, DebugPreferencesMessages.DebugPreferencePage_5, SWT.NONE, getFieldEditorParent()));
		addField(new BooleanFieldEditor(IDebugPreferenceConstants.PREF_PROMPT_REMOVE_ALL_TRIGGER_BREAKPOINTS, DebugPreferencesMessages.DebugPreferencePage_31, SWT.NONE, getFieldEditorParent()));
		addField(new BooleanFieldEditor(IDebugPreferenceConstants.PREF_PROMPT_DISABLE_ALL_BREAKPOINTS,
				DebugPreferencesMessages.DebugPreferencePage_32, SWT.NONE, getFieldEditorParent()));

		SWTFactory.createHorizontalSpacer(getFieldEditorParent(), 2);
		ColorFieldEditor mem= new ColorFieldEditor(IDebugUIConstants.PREF_CHANGED_DEBUG_ELEMENT_COLOR, DebugPreferencesMessages.DebugPreferencePage_4, getFieldEditorParent());
		addField(mem);
		mem = new ColorFieldEditor(IDebugUIConstants.PREF_CHANGED_VALUE_BACKGROUND, DebugPreferencesMessages.DebugPreferencePage_28, getFieldEditorParent());
		addField(mem);
		mem= new ColorFieldEditor(IDebugUIConstants.PREF_MEMORY_HISTORY_UNKNOWN_COLOR, DebugPreferencesMessages.DebugPreferencePage_0, getFieldEditorParent());
		addField(mem);
		mem= new ColorFieldEditor(IDebugUIConstants.PREF_MEMORY_HISTORY_KNOWN_COLOR, DebugPreferencesMessages.DebugPreferencePage_27, getFieldEditorParent());
		addField(mem);
	}

	@Override
	public void init(IWorkbench workbench) {}

}
