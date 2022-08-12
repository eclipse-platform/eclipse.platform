/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.preferences;

import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.team.internal.ui.IHelpContextIds;
import org.eclipse.team.internal.ui.IPreferenceIds;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

/**
 * This preference page allows to configure various aspects of the Synchronize View.
 */
public class SyncViewerPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage, IPreferenceIds {

	private BooleanFieldEditor showSyncInLabels = null;
	private RadioGroupFieldEditor synchronizePerspectiveSwitch = null;
	private RadioGroupFieldEditor defaultLayout = null;
	private boolean includeDefaultLayout = true;

	private static class PerspectiveDescriptorComparator implements Comparator {
		@Override
		public int compare(Object o1, Object o2) {
			if (o1 instanceof IPerspectiveDescriptor && o2 instanceof IPerspectiveDescriptor) {
				String id1= ((IPerspectiveDescriptor)o1).getLabel();
				String id2= ((IPerspectiveDescriptor)o2).getLabel();
				return Collator.getInstance().compare(id1, id2);
			}
			return 0;
		}
	}

	public SyncViewerPreferencePage() {
		super(GRID);
		setTitle(TeamUIMessages.SyncViewerPreferencePage_6);
		setDescription(TeamUIMessages.SyncViewerPreferencePage_7);
		setPreferenceStore(TeamUIPlugin.getPlugin().getPreferenceStore());
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.SYNC_PREFERENCE_PAGE);
	}

	@Override
	public void createFieldEditors() {
		showSyncInLabels = new BooleanFieldEditor(SYNCVIEW_VIEW_SYNCINFO_IN_LABEL, TeamUIMessages.SyncViewerPreferencePage_19, SWT.NONE, getFieldEditorParent());
		addField(showSyncInLabels);

		addField(new BooleanFieldEditor(
				SHOW_AUTHOR_IN_COMPARE_EDITOR,
				TeamUIMessages.SyncViewerPreferencePage_43,
				BooleanFieldEditor.DEFAULT,
				getFieldEditorParent()));

		addField(new BooleanFieldEditor(
				MAKE_FILE_WRITTABLE_IF_CONTEXT_MISSING,
				TeamUIMessages.SyncViewerPreferencePage_44,
				BooleanFieldEditor.DEFAULT,
				getFieldEditorParent()));

		addField(new BooleanFieldEditor(
				REUSE_OPEN_COMPARE_EDITOR,
				TeamUIMessages.SyncViewerPreferencePage_45,
				BooleanFieldEditor.DEFAULT,
				getFieldEditorParent()));

		addField(new BooleanFieldEditor(
				RUN_IMPORT_IN_BACKGROUND,
				TeamUIMessages.SyncViewerPreferencePage_46,
				BooleanFieldEditor.DEFAULT,
				getFieldEditorParent()));

		addField(new BooleanFieldEditor(
				APPLY_PATCH_IN_SYNCHRONIZE_VIEW,
				TeamUIMessages.SyncViewerPreferencePage_47,
				BooleanFieldEditor.DEFAULT,
				getFieldEditorParent()));

		if (isIncludeDefaultLayout()) {
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

		synchronizePerspectiveSwitch= new RadioGroupFieldEditor(SYNCHRONIZING_COMPLETE_PERSPECTIVE, TeamUIMessages.SyncViewerPreferencePage_13, 3,
				new String[][] {
								{TeamUIMessages.SyncViewerPreferencePage_14, MessageDialogWithToggle.ALWAYS},
								{TeamUIMessages.SyncViewerPreferencePage_42, MessageDialogWithToggle.NEVER},
								{TeamUIMessages.SyncViewerPreferencePage_16, MessageDialogWithToggle.PROMPT}
							},
							getFieldEditorParent(), true);
		addField(synchronizePerspectiveSwitch);

		Group perspectiveGroup = createGroup(getFieldEditorParent(), TeamUIMessages.SyncViewerPreferencePage_15);

		createLabel(perspectiveGroup, TeamUIMessages.SynchronizationViewPreference_defaultPerspectiveDescription, 1);

		handleDeletedPerspectives();
		String[][] perspectiveNamesAndIds = getPerspectiveNamesAndIds();
		ComboFieldEditor comboEditor= new ComboFieldEditor(
			SYNCVIEW_DEFAULT_PERSPECTIVE,
			TeamUIMessages.SynchronizationViewPreference_defaultPerspectiveLabel,
			perspectiveNamesAndIds,
			perspectiveGroup);
		addField(comboEditor);

		Dialog.applyDialogFont(getFieldEditorParent());
		updateLayout(perspectiveGroup);
		getFieldEditorParent().layout(true);
	}

	private Label createLabel(Composite parent, String title, int spacer) {
		GridData data;
		Label l = new Label(parent, SWT.WRAP);
		l.setFont(parent.getFont());
		data = new GridData();
		data.horizontalSpan = 2;
		if(spacer != 0) {
			data.verticalSpan = spacer;
		}
		data.horizontalAlignment = GridData.FILL;
		l.setLayoutData(data);
		if(title != null) {
			l.setText(title);
		}
		return l;
	}

	private Group createGroup(Composite parent, String title) {
		Group display = new Group(parent, SWT.NONE);
		updateLayout(display);
		GridData data = new GridData();
		data.horizontalSpan = 2;
		data.horizontalAlignment = GridData.FILL;
		display.setLayoutData(data);
		display.setText(title);
		return display;
	}

	private void updateLayout(Composite composite) {
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 5;
		layout.marginHeight =5;
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 5;
		composite.setLayout(layout);
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		//if(event.getSource() == bkgScheduledRefresh || event.getSource() == scheduledDelay) {
	//		updateEnablements();
	//	}
		super.propertyChange(event);
	}

	@Override
	public boolean performOk() {
		TeamUIPlugin.getPlugin().savePluginPreferences();
		return super.performOk();
	}

	@Override
	protected void initialize() {
		super.initialize();
		updateEnablements();
	}

	protected void updateEnablements() {
		//boolean enabled = bkgScheduledRefresh.getBooleanValue();
		//scheduledDelay.setEnabled(enabled, refreshGroup);
		//scheduledDelay.refreshValidState();
	}

	/**
	 * Return a 2-dimensional array of perspective names and ids.
	 */
	private String[][] getPerspectiveNamesAndIds() {

		IPerspectiveRegistry registry= PlatformUI.getWorkbench().getPerspectiveRegistry();
		IPerspectiveDescriptor[] perspectiveDescriptors= registry.getPerspectives();

		Arrays.sort(perspectiveDescriptors, new PerspectiveDescriptorComparator());

		String[][] table = new String[perspectiveDescriptors.length + 1][2];
		table[0][0] = TeamUIMessages.SynchronizationViewPreference_defaultPerspectiveNone; //;
		table[0][1] = SYNCVIEW_DEFAULT_PERSPECTIVE_NONE;
		for (int i = 0; i < perspectiveDescriptors.length; i++) {
			table[i + 1][0] = perspectiveDescriptors[i].getLabel();
			table[i + 1][1] = perspectiveDescriptors[i].getId();
		}
		return table;
	}

	private static void handleDeletedPerspectives() {
		IPreferenceStore store= TeamUIPlugin.getPlugin().getPreferenceStore();
		String id= store.getString(SYNCVIEW_DEFAULT_PERSPECTIVE);
		if (PlatformUI.getWorkbench().getPerspectiveRegistry().findPerspectiveWithId(id) == null) {
			store.putValue(SYNCVIEW_DEFAULT_PERSPECTIVE, SYNCVIEW_DEFAULT_PERSPECTIVE_NONE);
		}
	}

	public boolean isIncludeDefaultLayout() {
		return includeDefaultLayout;
	}

	public void setIncludeDefaultLayout(boolean includeDefaultLayout) {
		this.includeDefaultLayout = includeDefaultLayout;
	}
}
