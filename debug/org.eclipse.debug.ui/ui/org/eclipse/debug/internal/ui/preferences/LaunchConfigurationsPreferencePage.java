/*******************************************************************************
 * Copyright (c) 2004, 2017 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.core.Preferences;
import org.eclipse.debug.internal.ui.AbstractDebugCheckboxSelectionDialog;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchCategoryFilter;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.ProgressMonitorPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.AdaptableList;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchViewerComparator;

/**
 * Provides the Launch Configuration preference page to the Run/Debug preferences
 *
 * This page allows users to set filtering options as well as perform migration tasks.
 * This class is not intended to be sub-classed
 * @since 3.2
 */
public class LaunchConfigurationsPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	/**
	 * Creates a dialog that allows users to select one or more projects to migrate.
	 * @since 3.2
	 */
	class LaunchConfigurationMigrationSelectionDialog extends AbstractDebugCheckboxSelectionDialog {

		private Object fInput;

		public LaunchConfigurationMigrationSelectionDialog(Shell parentShell, Object input) {
			super(parentShell);
			fInput = input;
			setShellStyle(getShellStyle() | SWT.RESIZE);
		}

		@Override
		protected String getDialogSettingsId() {
			return IDebugUIConstants.PLUGIN_ID + ".MIGRATION_SELECTION_DIALOG"; //$NON-NLS-1$
		}

		@Override
		protected String getHelpContextId() {
			return IDebugHelpContextIds.SELECT_LAUNCH_CONFIGURATION_MIGRATION_DIALOG;
		}

		@Override
		protected Object getViewerInput() {
			return fInput;
		}

		@Override
		protected String getViewerLabel() {
			return DebugPreferencesMessages.LaunchingPreferencePage_0;
		}

		@Override
		protected IContentProvider getContentProvider() {
			return new WorkbenchContentProvider();
		}

		@Override
		protected IBaseLabelProvider getLabelProvider() {
			return DebugUITools.newDebugModelPresentation();
		}
	}

	/**
	 * Content provider for the launch configuration type table
	 */
	class TableContentProvider implements IStructuredContentProvider {

		@Override
		public Object[] getElements(Object inputElement) {
			return getLaunchConfigurationTypes();
		}

		@Override
		public void dispose() {}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
	}

	/**
	 * to monitor the progress of the migration process
	 */
	private ProgressMonitorPart fMonitor;

	/**
	 * the migrate now button
	 */
	private Button fMigrateNow;

	/**
	 * a list of the field editors
	 */
	private List<FieldEditor> fFieldEditors;

	/**
	 * Boolean editor for debug core plug-in preference
	 */
	private Button fDeleteConfigs;

	/**
	 * The table for the launch configuration types
	 */
	private Table fTable;

	/**
	 * Constructor
	 */
	public LaunchConfigurationsPreferencePage() {
		super();
		setPreferenceStore(DebugUIPlugin.getDefault().getPreferenceStore());
		setTitle(DebugPreferencesMessages.LaunchConfigurationsPreferencePage_1);
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IDebugHelpContextIds.LAUNCH_CONFIGURATION_PREFERENCE_PAGE);
	}

	@Override
	protected Control createContents(Composite parent) {
		fFieldEditors = new ArrayList<>();
		Composite comp = SWTFactory.createComposite(parent, parent.getFont(), 1, 1, GridData.FILL_HORIZONTAL);
		//filtering options
		Group group = SWTFactory.createGroup(comp, DebugPreferencesMessages.LaunchingPreferencePage_32, 1, 1, GridData.FILL_HORIZONTAL);
		Composite spacer = SWTFactory.createComposite(group, group.getFont(), 1, 1, GridData.FILL_HORIZONTAL);
		FieldEditor edit = new BooleanFieldEditor(IInternalDebugUIConstants.PREF_FILTER_LAUNCH_CLOSED, DebugPreferencesMessages.LaunchingPreferencePage_33, SWT.NONE, spacer);
		fFieldEditors.add(edit);
		edit = new BooleanFieldEditor(IInternalDebugUIConstants.PREF_FILTER_LAUNCH_DELETED, DebugPreferencesMessages.LaunchingPreferencePage_34, SWT.NONE, spacer);
		fFieldEditors.add(edit);
		edit = new BooleanFieldEditor(IInternalDebugUIConstants.PREF_FILTER_WORKING_SETS, DebugPreferencesMessages.LaunchConfigurationsPreferencePage_3, SWT.NONE, spacer);
		fFieldEditors.add(edit);
		fDeleteConfigs = SWTFactory.createCheckButton(comp, DebugPreferencesMessages.LaunchConfigurationsPreferencePage_2, null, false, 3);

		//add table options
		createTypeFiltering(group);

		//migration
		group = SWTFactory.createGroup(comp, DebugPreferencesMessages.LaunchingPreferencePage_35, 1, 1, GridData.FILL_HORIZONTAL);
		Label label = new Label(group, SWT.LEFT | SWT.WRAP);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 350;
		label.setLayoutData(gd);
		label.setText(DebugPreferencesMessages.LaunchingPreferencePage_26);
		label.setFont(parent.getFont());
		fMigrateNow = SWTFactory.createPushButton(group, DebugPreferencesMessages.LaunchingPreferencePage_27, null);
		gd = new GridData(SWT.BEGINNING);

		fMigrateNow.setLayoutData(gd);
		fMigrateNow.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleMigrateNowSelected();
			}
		});

		//init field editors
		initFieldEditors();
		fTable.setEnabled(getPreferenceStore().getBoolean(IInternalDebugUIConstants.PREF_FILTER_LAUNCH_TYPES));
		return comp;
	}

	/**
	 * @param parent the parent to add this composite to
	 * @return the new composite with the type selection table in it
	 */
	private Composite createTypeFiltering(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, parent.getFont(), 1, 1, GridData.FILL_HORIZONTAL);
		BooleanFieldEditor2 editor = new BooleanFieldEditor2(IInternalDebugUIConstants.PREF_FILTER_LAUNCH_TYPES, DebugPreferencesMessages.LaunchConfigurationsPreferencePage_0, SWT.NONE, comp);
		editor.setPropertyChangeListener(event -> {
			boolean newvalue = false;
			if(event.getNewValue() instanceof Boolean) {
				newvalue = ((Boolean)event.getNewValue()).booleanValue();
			}
			else {
				newvalue = Boolean.parseBoolean(event.getNewValue().toString());
			}
			if(newvalue) {
				fTable.setEnabled(true);
			}
			else {
				fTable.setEnabled(false);
			}
		});
		fFieldEditors.add(editor);
		fTable = new Table(comp, SWT.CHECK | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 155;
		fTable.setLayoutData(gd);
		CheckboxTableViewer tviewer = new CheckboxTableViewer(fTable);
		tviewer.setLabelProvider(DebugUITools.newDebugModelPresentation());
		tviewer.setContentProvider(new TableContentProvider());
		tviewer.setComparator(new WorkbenchViewerComparator());
		// filter external tool builders
		tviewer.addFilter(new LaunchCategoryFilter(IInternalDebugUIConstants.ID_EXTERNAL_TOOL_BUILDER_LAUNCH_CATEGORY));
		tviewer.setInput(getLaunchConfigurationTypes());
		fTable.setFont(parent.getFont());
		return comp;
	}

	/**
	 * returns the launch configuration types
	 * @return the launch configuration types
	 */
	private ILaunchConfigurationType[] getLaunchConfigurationTypes() {
		return DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationTypes();
	}

	/**
	 * handles the Migrate button being clicked
	 *
	 * @since 3.2
	 */
	private void handleMigrateNowSelected() {
		try {
			ILaunchManager lmanager = DebugPlugin.getDefault().getLaunchManager();
			ILaunchConfiguration[] configurations = lmanager.getMigrationCandidates();
			//separate the private from the public
			List<ILaunchConfiguration> pub = new ArrayList<>();
			for (ILaunchConfiguration configuration : configurations) {
				if (DebugUITools.isPrivate(configuration)) {
					//auto-migrate private ones
					configuration.migrate();
				} else {
					pub.add(configuration);
				}
			}
			if(pub.isEmpty()) {
				MessageDialog.openInformation(getShell(), DebugPreferencesMessages.LaunchingPreferencePage_29, DebugPreferencesMessages.LaunchingPreferencePage_30);
				return;
			}
			LaunchConfigurationMigrationSelectionDialog listd = new LaunchConfigurationMigrationSelectionDialog(getShell(),new AdaptableList(pub));
			listd.setTitle(DebugPreferencesMessages.LaunchingPreferencePage_28);
			listd.setInitialSelections((Object[]) configurations);
			if(listd.open() == IDialogConstants.OK_ID) {
				fMonitor = new ProgressMonitorPart(fMigrateNow.getParent(), new GridLayout());
				Object[] objs = listd.getResult();
				fMonitor.beginTask(DebugPreferencesMessages.LaunchingPreferencePage_31, objs.length);
				for (Object obj : objs) {
					if (obj instanceof ILaunchConfiguration) {
						((ILaunchConfiguration) obj).migrate();
					}
					fMonitor.worked(1);
				}
				fMonitor.done();
				fMonitor.dispose();
			}
		}
		catch (CoreException e) {DebugUIPlugin.log(e);}
	}

	@Override
	public void init(IWorkbench workbench) {}

	/**
	 * Initializes the field editors to their values
	 * @since 3.2
	 */
	private void initFieldEditors() {
		for (FieldEditor editor : fFieldEditors) {
			editor.setPreferenceStore(getPreferenceStore());
			editor.load();
		}
		fDeleteConfigs.setSelection(
				Platform.getPreferencesService().getBoolean(DebugPlugin.getUniqueIdentifier(),
				DebugPlugin.PREF_DELETE_CONFIGS_ON_PROJECT_DELETE, true, null));
		//restore the tables' checked state
		TableItem[] items = fTable.getItems();
		ILaunchConfigurationType type;
		for (String t : getPreferenceStore().getString(IInternalDebugUIConstants.PREF_FILTER_TYPE_LIST).split("\\,")) { //$NON-NLS-1$
			for (TableItem item : items) {
				type = (ILaunchConfigurationType) item.getData();
				if (type.getIdentifier().equals(t)) {
					item.setChecked(true);
				}
			}
		}
	}

	@Override
	protected void performDefaults() {
		fDeleteConfigs.setSelection(Preferences.getDefaultBoolean(DebugPlugin.getUniqueIdentifier(), DebugPlugin.PREF_DELETE_CONFIGS_ON_PROJECT_DELETE, true));
		for (FieldEditor editor : fFieldEditors) {
			editor.loadDefault();
			if(editor instanceof BooleanFieldEditor2) {
				fTable.setEnabled(((BooleanFieldEditor2)editor).getBooleanValue());
			}
		}
	}

	@Override
	public boolean performOk() {
		//save field editors
		for (FieldEditor editor : fFieldEditors) {
			editor.store();
		}
		Preferences.setBoolean(DebugPlugin.getUniqueIdentifier(), DebugPlugin.PREF_DELETE_CONFIGS_ON_PROJECT_DELETE, fDeleteConfigs.getSelection(), null);
		//save table
		String types = IInternalDebugCoreConstants.EMPTY_STRING;
		ILaunchConfigurationType type;
		for (TableItem item : fTable.getItems()) {
			if (item.getChecked()) {
				type = (ILaunchConfigurationType) item.getData();
				types += type.getIdentifier()+","; //$NON-NLS-1$
			}
		}
		getPreferenceStore().setValue(IInternalDebugUIConstants.PREF_FILTER_TYPE_LIST, types);
		return super.performOk();
	}
}
