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

import org.eclipse.compare.CompareUI;
import org.eclipse.jface.preference.*;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Event;
import org.eclipse.team.internal.ui.SWTUtils;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

/**
 * Preference page for configuring CVS comparisons
 */
public class ComparePreferencePage extends CVSFieldEditorPreferencePage {
	private BooleanFieldEditor contents;
	private StringFieldEditor regex;

	@Override
	protected String getPageHelpContextId() {
		return IHelpContextIds.COMPARE_PREFERENCE_PAGE;
	}

	@Override
	protected String getPageDescription() {
		return CVSUIMessages.ComparePreferencePage_0;
	}

	@Override
	protected void createFieldEditors() {
		IPreferenceStore store = getPreferenceStore();

		contents = new BooleanFieldEditor(ICVSUIConstants.PREF_CONSIDER_CONTENTS,
				CVSUIMessages.ComparePreferencePage_4,
				BooleanFieldEditor.DEFAULT, getFieldEditorParent()) {
			private Event selectionEvent = createSelectionEvent();
			private Event createSelectionEvent() {
				Event event = new Event();
				event.type = SWT.Selection;
				return event;
			}
			// invert the UI
			@Override
			protected void doLoad() {
				super.doLoad();
				getChangeControl(getFieldEditorParent()).setSelection(!getBooleanValue());
				getChangeControl(getFieldEditorParent()).notifyListeners(SWT.Selection,
						selectionEvent);
			}
			@Override
			protected void doLoadDefault() {
				super.doLoadDefault();
				getChangeControl(getFieldEditorParent()).setSelection(!getBooleanValue());
				getChangeControl(getFieldEditorParent()).notifyListeners(SWT.Selection,
						selectionEvent);
			}
			@Override
			protected void doStore() {
				getPreferenceStore().setValue(getPreferenceName(), !getBooleanValue());
			}
		};
		addField(contents);
		regex = new StringFieldEditor(ICVSUIConstants.PREF_SYNCVIEW_REGEX_FILTER_PATTERN,
				CVSUIMessages.ComparePreferencePage_5,
				getFieldEditorParent());
		addField(regex);
		GridData data = new GridData();
		data.horizontalIndent = 20;
		regex.getLabelControl(getFieldEditorParent()).setLayoutData(data);
		regex.setEnabled(store.getBoolean(ICVSUIConstants.PREF_CONSIDER_CONTENTS), getFieldEditorParent());
		addField(new BooleanFieldEditor(
				ICVSUIConstants.PREF_SHOW_COMPARE_REVISION_IN_DIALOG,
				CVSUIMessages.ComparePreferencePage_3,
				BooleanFieldEditor.DEFAULT,
				getFieldEditorParent()));
		addField(new BooleanFieldEditor(
				ICVSUIConstants.PREF_COMMIT_SET_DEFAULT_ENABLEMENT,
				CVSUIMessages.ComparePreferencePage_2,
				BooleanFieldEditor.DEFAULT,
				getFieldEditorParent()));
		addField(new BooleanFieldEditor(
				ICVSUIConstants.PREF_ENABLE_MODEL_SYNC,
				CVSUIMessages.ComparePreferencePage_7,
				BooleanFieldEditor.DEFAULT,
				getFieldEditorParent()));
		addField(new BooleanFieldEditor(
				ICVSUIConstants.PREF_OPEN_COMPARE_EDITOR_FOR_SINGLE_FILE,
				CVSUIMessages.ComparePreferencePage_8,
				BooleanFieldEditor.DEFAULT,
				getFieldEditorParent()));

		IPreferencePageContainer container = getContainer();
		if (container instanceof IWorkbenchPreferenceContainer) {
			IWorkbenchPreferenceContainer workbenchContainer = (IWorkbenchPreferenceContainer) container;
			SWTUtils.createPreferenceLink(workbenchContainer, getFieldEditorParent(),
					CompareUI.PREFERENCE_PAGE_ID, CVSUIMessages.ComparePreferencePage_6);
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		super.propertyChange(event);
		regex.setEnabled(!contents.getBooleanValue(), getFieldEditorParent());
	}
}
