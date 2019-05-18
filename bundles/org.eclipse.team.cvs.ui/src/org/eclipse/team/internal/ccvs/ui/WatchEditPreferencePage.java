/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.preference.*;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;

/**
 * This page contains preferences related to the cvs watch/edit commands
 */
public class WatchEditPreferencePage extends CVSFieldEditorPreferencePage {
	
	private RadioGroupFieldEditor promptEditor;
	private RadioGroupFieldEditor updateEditor;
	private RadioGroupFieldEditor actionEditor;
	private IPreferenceStore store;

	@Override
	protected String getPageHelpContextId() {
		return IHelpContextIds.WATCH_EDIT_PREFERENCE_PAGE;
	}
	
	@Override
	protected String getPageDescription() {
		return CVSUIMessages.WatchEditPreferencePage_description; //;
	}

	@Override
	protected void createFieldEditors() {
		addField(new BooleanFieldEditor(
			ICVSUIConstants.PREF_CHECKOUT_READ_ONLY, 
			CVSUIMessages.WatchEditPreferencePage_checkoutReadOnly,  
			BooleanFieldEditor.DEFAULT, 
			getFieldEditorParent()));
		addField(new BooleanFieldEditor(
				ICVSUIConstants.PREF_ENABLE_WATCH_ON_EDIT, 
				CVSUIMessages.WatchEditPreferencePage_0,  
				BooleanFieldEditor.DEFAULT, 
				getFieldEditorParent()));
		
		actionEditor = new RadioGroupFieldEditor(
			ICVSUIConstants.PREF_EDIT_ACTION,
			CVSUIMessages.WatchEditPreferencePage_validateEditSaveAction, 
			1,
			new String[][] {{CVSUIMessages.WatchEditPreferencePage_edit, ICVSUIConstants.PREF_EDIT_PROMPT_EDIT},
							{CVSUIMessages.WatchEditPreferencePage_editInBackground, ICVSUIConstants.PREF_EDIT_IN_BACKGROUND},
							{CVSUIMessages.WatchEditPreferencePage_highjack, ICVSUIConstants.PREF_EDIT_PROMPT_HIGHJACK},
							}, 	// 
			getFieldEditorParent(), true);
		addField(actionEditor);
		
		promptEditor = new RadioGroupFieldEditor(
			ICVSUIConstants.PREF_EDIT_PROMPT,
			CVSUIMessages.WatchEditPreferencePage_editPrompt, 
			1,
			new String[][] {{CVSUIMessages.WatchEditPreferencePage_alwaysPrompt, ICVSUIConstants.PREF_EDIT_PROMPT_ALWAYS}, 
							{CVSUIMessages.WatchEditPreferencePage_onlyPrompt, ICVSUIConstants.PREF_EDIT_PROMPT_IF_EDITORS}, 
							{CVSUIMessages.WatchEditPreferencePage_neverPrompt, ICVSUIConstants.PREF_EDIT_PROMPT_NEVER}, 
							},	// 
			getFieldEditorParent(), true);
		
		updateEditor = new RadioGroupFieldEditor(
				ICVSUIConstants.PREF_UPDATE_PROMPT,
				CVSUIMessages.WatchEditPreferencePage_updatePrompt, 
				1,
				new String[][] {{CVSUIMessages.WatchEditPreferencePage_autoUpdate, ICVSUIConstants.PREF_UPDATE_PROMPT_AUTO}, 
								{CVSUIMessages.WatchEditPreferencePage_promptUpdate, ICVSUIConstants.PREF_UPDATE_PROMPT_IF_OUTDATED}, 
								{CVSUIMessages.WatchEditPreferencePage_neverUpdate, ICVSUIConstants.PREF_UPDATE_PROMPT_NEVER}, 
								},  // 
				getFieldEditorParent(), true);
		
		store = getCVSPreferenceStore();
		addField(promptEditor);
		addField(updateEditor);
	}

	private boolean isEditEnabled() {
		return store.getString(ICVSUIConstants.PREF_EDIT_ACTION).equals(ICVSUIConstants.PREF_EDIT_PROMPT_EDIT);
	}

	@Override
	protected void pushPreferences() {
		store = getCVSPreferenceStore();
		Preferences target = CVSProviderPlugin.getPlugin().getPluginPreferences();
		target.setValue(
			CVSProviderPlugin.READ_ONLY,
			store.getBoolean(ICVSUIConstants.PREF_CHECKOUT_READ_ONLY));
		target.setValue(
				CVSProviderPlugin.ENABLE_WATCH_ON_EDIT,
				store.getBoolean(ICVSUIConstants.PREF_ENABLE_WATCH_ON_EDIT));
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getSource() == actionEditor) {
			boolean enabled = event.getNewValue().equals(ICVSUIConstants.PREF_EDIT_PROMPT_EDIT);
			promptEditor.setEnabled(enabled, getFieldEditorParent());
			updateEditor.setEnabled(enabled, getFieldEditorParent());
		}
		super.propertyChange(event);
	}

	@Override
	protected void initialize() {
		super.initialize();
		promptEditor.setEnabled(isEditEnabled(), getFieldEditorParent());
		updateEditor.setEnabled(isEditEnabled(), getFieldEditorParent());
	}
}
