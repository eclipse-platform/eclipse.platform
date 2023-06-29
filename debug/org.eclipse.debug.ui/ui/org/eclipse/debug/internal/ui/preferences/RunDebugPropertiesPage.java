/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.core.LaunchManager;
import org.eclipse.debug.internal.ui.AbstractDebugListSelectionDialog;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.DefaultLabelProvider;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationComparator;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationManager;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationsDialog;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.dialogs.SelectionDialog;

/**
 * Displays default launch configuration settings for a selected resource - associated launch configurations.
 *
 * @see PropertyPage
 * @see ILaunchConfiguration
 * @see LaunchConfigurationsDialog
 *
 * CONTEXTLAUNCHING
 *
 * @since 3.3.0
 */
public class RunDebugPropertiesPage extends PropertyPage {
	/**
	 * Set of configurations to be deleted
	 */
	private Set<ILaunchConfigurationWorkingCopy> fDeletedConfigurations = new HashSet<>();

	/**
	 * Set of original default candidates for the resource
	 */
	private Set<ILaunchConfiguration> fOriginalCandidates;

	/**
	 * Holds configurations that need to be saved when the page closes
	 */
	private Set<ILaunchConfigurationWorkingCopy> fChangedConfigurations = new HashSet<>();

	/**
	 * List of the applicable launch config types for the backing resource
	 */
	private List<ILaunchConfigurationType> fTypeCandidates = null;

	//widgets
	private TableViewer fViewer;
	private Button fNewButton = null;
	private Button fEditButton = null;
	private Button fDuplicateButton = null;
	private Button fDeleteButton = null;

	@Override
	protected Control createContents(Composite parent) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IDebugHelpContextIds.RUN_DEBUG_RESOURCE_PROPERTY_PAGE);
		collectConfigCandidates(getResource());
		Composite topComposite = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_BOTH);

		SWTFactory.createWrapLabel(topComposite, DebugPreferencesMessages.DefaultLaunchConfigurationsPropertiesPage_0, 2, 300);
		SWTFactory.createVerticalSpacer(topComposite, 2);
		SWTFactory.createWrapLabel(topComposite, MessageFormat.format(DebugPreferencesMessages.DefaultLaunchConfigurationsPropertiesPage_1, new Object[] { getResource().getName() }), 2, 300);
		fViewer = createViewer(topComposite);

		Composite buttonComp = SWTFactory.createComposite(topComposite, 1, 1, GridData.FILL_VERTICAL);
		GridLayout layout = (GridLayout) buttonComp.getLayout();
		layout.marginHeight = 0;
		fNewButton = SWTFactory.createPushButton(buttonComp, DebugPreferencesMessages.DefaultLaunchConfigurationsPropertiesPage_2, null);
		fNewButton.setToolTipText(DebugPreferencesMessages.DefaultLaunchConfigurationsPropertiesPage_3);
		fNewButton.setEnabled(collectTypeCandidates().length > 0);
		fNewButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleNew();
			}
		});

		fDuplicateButton = SWTFactory.createPushButton(buttonComp, DebugPreferencesMessages.DefaultLaunchConfigurationsPropertiesPage_4, null);
		fDuplicateButton.setToolTipText(DebugPreferencesMessages.DefaultLaunchConfigurationsPropertiesPage_5);
		fDuplicateButton.setEnabled(false);
		fDuplicateButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleCopy();
			}
		});
		fEditButton = SWTFactory.createPushButton(buttonComp, DebugPreferencesMessages.DefaultLaunchConfigurationsPropertiesPage_6, null);
		fEditButton.setToolTipText(DebugPreferencesMessages.DefaultLaunchConfigurationsPropertiesPage_7);
		fEditButton.setEnabled(false);
		fEditButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleEdit();
			}
		});
		fDeleteButton = SWTFactory.createPushButton(buttonComp, DebugPreferencesMessages.DefaultLaunchConfigurationsPropertiesPage_8, null);
		fDeleteButton.setToolTipText(DebugPreferencesMessages.DefaultLaunchConfigurationsPropertiesPage_9);
		fDeleteButton.setEnabled(false);
		fDeleteButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleDelete();
			}
		});

		fViewer.setSelection(new StructuredSelection());
		applyDialogFont(topComposite);
		return topComposite;
	}

	/**
	 * Creates and returns the viewer that will display the possible default configurations.
	 *
	 * @param parent parent composite to create the viewer in
	 * @return viewer viewer that will display possible default configurations
	 */
	protected TableViewer createViewer(Composite parent){
		TableViewer viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		viewer.setLabelProvider(new DefaultLabelProvider());
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setComparator(new LaunchConfigurationComparator());
		Table builderTable = viewer.getTable();
		GridData tableGridData = new GridData(GridData.FILL_BOTH);
		tableGridData.heightHint = 300;
		tableGridData.widthHint = 300;
		builderTable.setLayoutData(tableGridData);
		IResource resource = getResource();
		viewer.setInput(collectConfigCandidates(resource));
		viewer.addSelectionChangedListener(event -> {
			ISelection sel = event.getSelection();
			if(sel instanceof IStructuredSelection) {
				IStructuredSelection ss = (IStructuredSelection) sel;
				boolean empty = ss.isEmpty();
				int size = ss.size();
				fEditButton.setEnabled(!empty && size == 1);
				fDuplicateButton.setEnabled(!empty && size == 1);
				fDeleteButton.setEnabled(!empty);
				setErrorMessage(null);
			}
		});
		viewer.addDoubleClickListener(arg0 -> handleEdit());
		return viewer;
	}

	@Override
	public void dispose() {
		if(fOriginalCandidates != null) {
			fOriginalCandidates.clear();
			fOriginalCandidates = null;
		}
		if(fChangedConfigurations != null) {
			fChangedConfigurations.clear();
			fChangedConfigurations = null;
		}
		super.dispose();
	}

	/**
	 * Returns the viewer displaying possible default configurations.
	 *
	 * @return viewer
	 */
	protected TableViewer getViewer() {
		return fViewer;
	}

	/**
	 * Returns the launch manager
	 * @return the launch manager
	 */
	protected LaunchManager getLaunchManager() {
		return (LaunchManager) DebugPlugin.getDefault().getLaunchManager();
	}

	/**
	 * Collects the applicable launch configuration types for the backing resource.
	 * Default implementation uses the launch shortcut evaluation expressions and leverages the
	 * mapping of launch shortcut to config type id to derive the applicable types.
	 * @return the listing of applicable launch configuration types for the backing resource
	 */
	protected ILaunchConfigurationType[] collectTypeCandidates() {
		if(fTypeCandidates == null) {
			String[] types = DebugUIPlugin.getDefault().getLaunchConfigurationManager().getApplicableConfigurationTypes(getResource());
			fTypeCandidates = new ArrayList<>(types.length);
			for (String type : types) {
				fTypeCandidates.add(DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(type));
			}

			Collections.sort(fTypeCandidates, (o1, o2) -> {
				ILaunchConfigurationType t1 = o1;
				ILaunchConfigurationType t2 = o2;
				return t1.getName().compareTo(t2.getName());
			});
		}
		return fTypeCandidates.toArray(new ILaunchConfigurationType[fTypeCandidates.size()]);
	}

	/**
	 * Returns a set of potential default configurations candidates for the given
	 * resource. The configurations are working copies.
	 *
	 * @param resource resource
	 * @return list of default candidates
	 */
	protected Set<ILaunchConfiguration> collectConfigCandidates(IResource resource) {
		if(fOriginalCandidates == null) {
			fOriginalCandidates = new HashSet<>();
			try {
				ILaunchConfiguration[] configs = DebugUIPlugin.getDefault().getLaunchConfigurationManager().getApplicableLaunchConfigurations(null, resource);
				for (ILaunchConfiguration config : configs) {
					fOriginalCandidates.add(config.getWorkingCopy());
				}
			}
			catch(CoreException ce) {DebugUIPlugin.log(ce);}
		}
		return fOriginalCandidates;
	}



	/**
	 * Returns the resource this property page is open on.
	 *
	 * @return resource
	 */
	protected IResource getResource() {
		IAdaptable element = getElement();
		IResource resource = null;
		if (element instanceof IResource) {
			resource = (IResource) element;
		} else if (element != null) {
			resource = Adapters.adapt(element, IResource.class);
		}
		if (resource == null && element != null) {
			return Adapters.adapt(element, IProject.class);
		}
		return resource;
	}

	@Override
	public boolean performOk() {
	//delete
		for (ILaunchConfigurationWorkingCopy currentConfig : fDeletedConfigurations) {
			try{
				if (currentConfig.getOriginal() != null){
					currentConfig.getOriginal().delete();
				}
			} catch (CoreException e) {
				DebugPlugin.logMessage("Problem deleting configuration " + currentConfig.getName(), e); //$NON-NLS-1$
			}
		}
	//add
		for (ILaunchConfigurationWorkingCopy currentConfig : fChangedConfigurations) {
			try{
				currentConfig.doSave();
			} catch (CoreException e) {
				DebugPlugin.logMessage("Problem saving changes to configuration " + currentConfig.getName(), e); //$NON-NLS-1$
			}
		}

		return super.performOk();
	}

	@Override
	protected void performDefaults() {
		setErrorMessage(null);
		setValid(true);
		fOriginalCandidates.clear();
		fOriginalCandidates = null;
		getViewer().setInput(collectConfigCandidates(getResource()));
		fChangedConfigurations.clear();
		fDeletedConfigurations.clear();
		fViewer.refresh(true, true);
		super.performDefaults();
	}

	/**
	 * Returns the names of the launch configurations passed in as original input to the tree viewer
	 * @return the names of the original launch configurations
	 */
	private Set<String> getConfigurationNames() {
		Set<String> names = new HashSet<>();
		for(ILaunchConfiguration config : fOriginalCandidates) {
			names.add(config.getName());
		}

		for(ILaunchConfigurationWorkingCopy config : fChangedConfigurations) {
			names.add(config.getName());
		}
		return names;
	}

	/**
	 * Returns selected configurations.
	 *
	 * @return selected configurations
	 */
	private ILaunchConfigurationWorkingCopy[] getSelectedConfigurations() {
		IStructuredSelection ss = fViewer.getStructuredSelection();
		return (ILaunchConfigurationWorkingCopy[]) ss.toList().toArray(new ILaunchConfigurationWorkingCopy[ss.size()]);
	}

	/**
	 * Copy the selection
	 */
	private void handleCopy() {
		ILaunchConfigurationWorkingCopy configuration = getSelectedConfigurations()[0];
		try {
			ILaunchConfigurationWorkingCopy copy = configuration.copy(
					((LaunchManager)DebugPlugin.getDefault().getLaunchManager()).generateUniqueLaunchConfigurationNameFrom(configuration.getName(), getConfigurationNames()));
			copy.setAttributes(configuration.getAttributes());
			fChangedConfigurations.add(copy);
			fViewer.add(copy);
			fViewer.setSelection(new StructuredSelection(copy));
		} catch (CoreException e) {
			setErrorMessage(e.getMessage());
		}
	}

	/**
	 * Delete the selection
	 */
	private void handleDelete() {
		Table table = fViewer.getTable();
		int[] indices = table.getSelectionIndices();
		Arrays.sort(indices);
		ILaunchConfigurationWorkingCopy[] configurations = getSelectedConfigurations();
		for (ILaunchConfigurationWorkingCopy configuration : configurations) {
			fDeletedConfigurations.add(configuration);
			fChangedConfigurations.remove(configuration);
			fViewer.remove(configuration);
		}
		if (indices[0] < table.getItemCount()) {
			fViewer.setSelection(new StructuredSelection(table.getItem(indices[0]).getData()));
		} else if (table.getItemCount() > 0) {
			fViewer.setSelection(new StructuredSelection(table.getItem(table.getItemCount() - 1).getData()));
		}
	}

	/**
	 * Edit the selection
	 */
	private void handleEdit() {
		ILaunchConfigurationWorkingCopy config = getSelectedConfigurations()[0];
		int ret = edit(config, false);
		if(ret == IDialogConstants.OK_ID) {
			fChangedConfigurations.add(config);
			fViewer.refresh(config, true, true);
		}
		else if(ret == IDialogConstants.ABORT_ID) {
			setErrorMessage(MessageFormat.format(DebugPreferencesMessages.RunDebugPropertiesPage_0, new Object[] { config.getName() }));
		}
	}

	/**
	 * Edits the given configuration as a nested working copy.
	 * Returns the code from the dialog used to edit the configuration.
	 *
	 * @param configuration the configuration working copy to editor
	 * @param setDefaults whether to set default values in the config
	 * @return dialog return code - OK or CANCEL
	 */
	private int edit(ILaunchConfigurationWorkingCopy configuration, boolean setDefaults) {
		try {
			LaunchConfigurationManager lcm = DebugUIPlugin.getDefault().getLaunchConfigurationManager();
			ILaunchGroup group = null;
			// bug 208034, we should try modes we know about first then guess
			ILaunchConfigurationType type = configuration.getType();
			if(type.supportsMode(ILaunchManager.RUN_MODE)) {
				group = lcm.getLaunchGroup(type, ILaunchManager.RUN_MODE);
			}
			else if(type.supportsMode(ILaunchManager.DEBUG_MODE)) {
				group = lcm.getLaunchGroup(type, ILaunchManager.DEBUG_MODE);
			}
			else if(type.supportsMode(ILaunchManager.PROFILE_MODE)) {
				group = lcm.getLaunchGroup(type, ILaunchManager.PROFILE_MODE);
			}
			else {
				for (Set<String> modes : type.getSupportedModeCombinations()) {
					group = DebugUIPlugin.getDefault().getLaunchConfigurationManager().getLaunchGroup(type, modes);
					if(group != null) {
						break;
					}
				}
			}
			if(group != null) {
				return DebugUIPlugin.openLaunchConfigurationPropertiesDialog(getShell(), configuration, group.getIdentifier(), getConfigurationNames(), null, setDefaults);
			}
		}
		catch(CoreException ce) {}
		return IDialogConstants.ABORT_ID;
	}

	/**
	 * Create a new configuration
	 */
	private void handleNew() {

		final ILaunchConfigurationType[] typeCandidates = collectTypeCandidates();

		SelectionDialog dialog = new AbstractDebugListSelectionDialog(getShell()){

			@Override
			protected String getDialogSettingsId() {
				return DebugUIPlugin.getUniqueIdentifier() + ".SELECT_CONFIGURATION_TYPE_DIALOG"; //$NON-NLS-1$
			}

			@Override
			protected Object getViewerInput() {
				return typeCandidates;
			}

			@Override
			protected String getHelpContextId() {
				return IDebugHelpContextIds.SELECT_CONFIGURATION_TYPE_DIALOG;
			}

			@Override
			protected String getViewerLabel() {
				return DebugPreferencesMessages.DefaultLaunchConfigurationsPropertiesPage_12;
			}

		};
		dialog.setTitle(DebugPreferencesMessages.DefaultLaunchConfigurationsPropertiesPage_11);

		if (dialog.open() == Window.OK) {
			Object[] result = dialog.getResult();
			if (result.length == 1) {
				ILaunchConfigurationType type = (ILaunchConfigurationType) result[0];
				try {
					ILaunchConfigurationWorkingCopy wc = type.newInstance(null,
							((LaunchManager)DebugPlugin.getDefault().getLaunchManager()).
							generateUniqueLaunchConfigurationNameFrom("New_configuration", getConfigurationNames())); //$NON-NLS-1$
					int ret = edit(wc, true);
					if (ret == Window.OK) {
						fChangedConfigurations.add(wc);
						fViewer.add(wc);
						fViewer.setSelection(new StructuredSelection(wc));
					}
					else if(ret == IDialogConstants.ABORT_ID) {
						setErrorMessage(MessageFormat.format(DebugPreferencesMessages.RunDebugPropertiesPage_0, new Object[] { wc.getName() }));
					}
				} catch (CoreException e) {
					setErrorMessage(e.getMessage());
				}
			}
		}
	}
}
