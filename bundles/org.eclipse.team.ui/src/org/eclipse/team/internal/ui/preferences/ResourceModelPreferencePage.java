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
package org.eclipse.team.internal.ui.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.internal.ui.IHelpContextIds;
import org.eclipse.team.internal.ui.IPreferenceIds;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

public class ResourceModelPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage, IPreferenceIds {

	private RadioGroupFieldEditor defaultLayout;

	public ResourceModelPreferencePage() {
		super(GRID);
		setTitle(TeamUIMessages.SynchronizationCompareAdapter_0);
		setDescription(TeamUIMessages.ResourceModelPreferencePage_0);
		setPreferenceStore(TeamUIPlugin.getPlugin().getPreferenceStore());
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.RESOURCE_MODEL_PREFERENCE_PAGE);
	}

	@Override
	protected void createFieldEditors() {
		defaultLayout = new RadioGroupFieldEditor(SYNCVIEW_DEFAULT_LAYOUT,
				TeamUIMessages.SyncViewerPreferencePage_0, 3,
				new String[][] {
					{TeamUIMessages.SyncViewerPreferencePage_1, FLAT_LAYOUT},
					{TeamUIMessages.SyncViewerPreferencePage_2, TREE_LAYOUT},
					{TeamUIMessages.SyncViewerPreferencePage_3, COMPRESSED_LAYOUT}
				},
				getFieldEditorParent(), true /* use a group */);
		addField(defaultLayout);
	}

	@Override
	public void init(IWorkbench workbench) {
		// Nothing to do
	}

	@Override
	public boolean performOk() {
		TeamUIPlugin.getPlugin().savePluginPreferences();
		return super.performOk();
	}

}
