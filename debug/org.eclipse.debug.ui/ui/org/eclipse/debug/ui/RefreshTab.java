/*******************************************************************************
 *  Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.debug.ui;


import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.RefreshUtil;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationsDialog;
import org.eclipse.debug.internal.ui.stringsubstitution.StringSubstitutionMessages;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetEditWizard;

/**
 * A launch configuration tab which allows the user to specify
 * which resources should be refreshed when the launch
 * terminates.
 * <p>
 * Clients may call {@link #setHelpContextId(String)} on this tab prior to control
 * creation to alter the default context help associated with this tab.
 * </p>
 * <p>
 * This class may be instantiate.
 * </p>
 * @since 3.0
 * @noextend This class is not intended to be sub-classed by clients.
 */
public class RefreshTab extends AbstractLaunchConfigurationTab {

	/**
	 * Boolean attribute indicating if a refresh scope is recursive. Default
	 * value is <code>true</code>.
	 */
	public static final String ATTR_REFRESH_RECURSIVE = RefreshUtil.ATTR_REFRESH_RECURSIVE;

	/**
	 * String attribute identifying the scope of resources that should be
	 * refreshed after an external tool is run. The value is either a refresh
	 * variable or the default value, <code>null</code>, indicating no refresh.
	 */
	public static final String ATTR_REFRESH_SCOPE = RefreshUtil.ATTR_REFRESH_SCOPE;

	// Check Buttons
	private Button fRefreshButton;
	private Button fRecursiveButton;

	// Group box
	private Group fGroup;

	// Radio Buttons
	private Button fContainerButton;
	private Button fProjectButton;
	private Button fResourceButton;
	private Button fWorkingSetButton;
	private Button fWorkspaceButton;

	// Push Button
	private Button fSelectButton;

	// Working set
	private IWorkingSet fWorkingSet;

	/**
	 * Constructor
	 */
	public RefreshTab() {
		setHelpContextId(IDebugHelpContextIds.LAUNCH_CONFIGURATION_DIALOG_REFRESH_TAB);
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite mainComposite = new Composite(parent, SWT.NONE);
		setControl(mainComposite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), getHelpContextId());

		GridLayout layout = new GridLayout();
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		mainComposite.setLayout(layout);
		mainComposite.setLayoutData(gd);
		mainComposite.setFont(parent.getFont());

		fRefreshButton = createCheckButton(mainComposite, StringSubstitutionMessages.RefreshTab_31);
		fRefreshButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateEnabledState();
				updateLaunchConfigurationDialog();
			}
		});

		fGroup = new Group(mainComposite, SWT.NONE);
		fGroup.setFont(mainComposite.getFont());
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = false;
		fGroup.setLayout(layout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fGroup.setLayoutData(gd);

		SelectionAdapter adapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (((Button)e.getSource()).getSelection()) {
					updateEnabledState();
					updateLaunchConfigurationDialog();
				}
			}
		};

		fWorkspaceButton = createRadioButton(fGroup, StringSubstitutionMessages.RefreshTab_32);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fWorkspaceButton.setLayoutData(gd);
		fWorkspaceButton.addSelectionListener(adapter);

		fResourceButton = createRadioButton(fGroup, StringSubstitutionMessages.RefreshTab_33);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fResourceButton.setLayoutData(gd);
		fResourceButton.addSelectionListener(adapter);

		fProjectButton = createRadioButton(fGroup, StringSubstitutionMessages.RefreshTab_34);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fProjectButton.setLayoutData(gd);
		fProjectButton.addSelectionListener(adapter);

		fContainerButton = createRadioButton(fGroup, StringSubstitutionMessages.RefreshTab_35);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fContainerButton.setLayoutData(gd);
		fContainerButton.addSelectionListener(adapter);

		fWorkingSetButton = createRadioButton(fGroup, StringSubstitutionMessages.RefreshTab_36);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 1;
		fWorkingSetButton.setLayoutData(gd);
		fWorkingSetButton.addSelectionListener(adapter);

		fSelectButton = createPushButton(fGroup, StringSubstitutionMessages.RefreshTab_37, null);
		gd = (GridData)fSelectButton.getLayoutData();
		gd.horizontalAlignment = GridData.HORIZONTAL_ALIGN_END;
		fSelectButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectResources();
			}
		});

		createVerticalSpacer(fGroup, 2);
		createRecursiveComponent(fGroup);
	}

	/**
	 * Prompts the user to select the resources to refresh.
	 */
	private void selectResources() {
		IWorkingSetManager workingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();

		if (fWorkingSet == null){
			fWorkingSet = workingSetManager.createWorkingSet(StringSubstitutionMessages.RefreshTab_40, new IAdaptable[0]);
		}
		IWorkingSetEditWizard wizard = workingSetManager.createWorkingSetEditWizard(fWorkingSet);
		WizardDialog dialog = new WizardDialog(((LaunchConfigurationsDialog)LaunchConfigurationsDialog.getCurrentlyVisibleLaunchConfigurationDialog()).getShell(), wizard);
		dialog.create();

		if (dialog.open() == Window.CANCEL) {
			return;
		}
		fWorkingSet = wizard.getSelection();
		updateLaunchConfigurationDialog();
	}

	/**
	 * Creates the controls needed to edit the refresh recursive
	 * attribute of a launch configuration
	 *
	 * @param parent the composite to create the controls in
	 */
	private void createRecursiveComponent(Composite parent) {
		fRecursiveButton = createCheckButton(parent, StringSubstitutionMessages.RefreshTab_0);
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		data.horizontalSpan = 2;
		fRecursiveButton.setLayoutData(data);
		fRecursiveButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();
			}
		});
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		updateRefresh(configuration);
		updateRecursive(configuration);
		updateScope(configuration);
		updateEnabledState();
	}

	/**
	 * Updates the tab to display the refresh scope specified by the launch config
	 * @param configuration the configuration to update scope information on
	 */
	private void updateScope(ILaunchConfiguration configuration) {
		String scope = null;
		try {
			scope= configuration.getAttribute(ATTR_REFRESH_SCOPE, (String)null);
		} catch (CoreException ce) {
			DebugUIPlugin.log(DebugUIPlugin.newErrorStatus("Exception reading launch configuration", ce)); //$NON-NLS-1$
		}
		fWorkspaceButton.setSelection(false);
		fResourceButton.setSelection(false);
		fContainerButton.setSelection(false);
		fProjectButton.setSelection(false);
		fWorkingSetButton.setSelection(false);
		if (scope == null) {
			// select the workspace by default
			fWorkspaceButton.setSelection(true);
		} else {
			if (scope.equals(RefreshUtil.MEMENTO_WORKSPACE)) {
				fWorkspaceButton.setSelection(true);
			} else if (scope.equals(RefreshUtil.MEMENTO_SELECTED_RESOURCE)) {
				fResourceButton.setSelection(true);
			} else if (scope.equals(RefreshUtil.MEMENTO_SELECTED_CONTAINER)) {
				fContainerButton.setSelection(true);
			} else if (scope.equals(RefreshUtil.MEMENTO_SELECTED_PROJECT)) {
				fProjectButton.setSelection(true);
			} else if (scope.startsWith("${resource:")) { //$NON-NLS-1$
				fWorkingSetButton.setSelection(true);
				try {
					IResource[] resources = getRefreshResources(scope);
					IWorkingSetManager workingSetManager= PlatformUI.getWorkbench().getWorkingSetManager();
					fWorkingSet = workingSetManager.createWorkingSet(StringSubstitutionMessages.RefreshTab_40, resources);
				} catch (CoreException e) {
					fWorkingSet = null;
				}
			} else if (scope.startsWith("${working_set:")) { //$NON-NLS-1$
				fWorkingSetButton.setSelection(true);
				fWorkingSet = getWorkingSet(scope);
			}
		}
	}
	/**
	 * Method updateRecursive.
	 * @param configuration the launch configuration to update the refresh button from
	 */
	private void updateRecursive(ILaunchConfiguration configuration) {
		boolean recursive= true;
		try {
			recursive= configuration.getAttribute(ATTR_REFRESH_RECURSIVE, true);
		} catch (CoreException ce) {
			DebugUIPlugin.log(DebugUIPlugin.newErrorStatus("Exception reading launch configuration", ce)); //$NON-NLS-1$
		}
		fRecursiveButton.setSelection(recursive);
	}
	/**
	 * Method updateRefresh.
	 * @param configuration the configuration to update the refresh scope button from
	 */
	private void updateRefresh(ILaunchConfiguration configuration) {
		String scope= null;
		try {
			scope= configuration.getAttribute(ATTR_REFRESH_SCOPE, (String)null);
		} catch (CoreException ce) {
			DebugUIPlugin.log(DebugUIPlugin.newErrorStatus("Exception reading launch configuration", ce)); //$NON-NLS-1$
		}
		fRefreshButton.setSelection(scope != null);
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		if (fRefreshButton.getSelection()) {
			String scope = generateScopeMemento();
			configuration.setAttribute(ATTR_REFRESH_SCOPE, scope);
			setAttribute(ATTR_REFRESH_RECURSIVE, configuration, fRecursiveButton.getSelection(), true);
		} else {
			//clear the refresh attributes
			configuration.setAttribute(ATTR_REFRESH_SCOPE, (String)null);
			setAttribute(ATTR_REFRESH_RECURSIVE, configuration, true, true);
		}
	}

	/**
	 * Generates a memento for the refresh scope. This is based on old refresh
	 * variables.
	 *
	 * @return a memento
	 */
	private String generateScopeMemento() {
		if (fWorkspaceButton.getSelection()) {
			return RefreshUtil.MEMENTO_WORKSPACE;
		}
		if (fResourceButton.getSelection()) {
			return RefreshUtil.MEMENTO_SELECTED_RESOURCE;
		}
		if (fContainerButton.getSelection()) {
			return RefreshUtil.MEMENTO_SELECTED_CONTAINER;
		}
		if (fProjectButton.getSelection()) {
			return RefreshUtil.MEMENTO_SELECTED_PROJECT;
		}
		if (fWorkingSetButton.getSelection()) {
			return getRefreshAttribute(fWorkingSet);
		}
		return null;
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	@Override
	public String getName() {
		return StringSubstitutionMessages.RefreshTab_6;
	}

	/**
	 * Updates the enablement state of the fields.
	 */
	private void updateEnabledState() {
		boolean enabled= fRefreshButton.getSelection();
		fRecursiveButton.setEnabled(enabled);
		fGroup.setEnabled(enabled);
		fWorkspaceButton.setEnabled(enabled);
		fResourceButton.setEnabled(enabled);
		fContainerButton.setEnabled(enabled);
		fProjectButton.setEnabled(enabled);
		fWorkingSetButton.setEnabled(enabled);
		fSelectButton.setEnabled(enabled && fWorkingSetButton.getSelection());
		if (!enabled) {
			super.setErrorMessage(null);
		}
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
	 */
	@Override
	public Image getImage() {
		return DebugPluginImages.getImage(IInternalDebugUIConstants.IMG_OBJS_REFRESH_TAB);
	}

	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		setErrorMessage(null);
		setMessage(null);
		if (fRefreshButton.getSelection() && (fWorkingSetButton.getSelection() && (fWorkingSet == null || fWorkingSet.getElements().length == 0))) {
			setErrorMessage(StringSubstitutionMessages.RefreshTab_42);
			return false;
		}
		return true;
	}

	/**
	 * Refreshes the resources as specified by the given launch configuration.
	 *
	 * @param configuration launch configuration
	 * @param monitor progress monitor which may be <code>null</code>
	 * @throws CoreException if an exception occurs while refreshing resources
	 */
	public static void refreshResources(ILaunchConfiguration configuration, IProgressMonitor monitor) throws CoreException {
		RefreshUtil.refreshResources(configuration, monitor);
	}

	/**
	 * Returns a collection of resources referred to by a refresh scope attribute.
	 *
	 * @param scope refresh scope attribute (<code>ATTR_REFRESH_SCOPE</code>)
	 * @return collection of resources referred to by the refresh scope attribute
	 * @throws CoreException if unable to resolve a set of resources
	 */
	public static IResource[] getRefreshResources(String scope) throws CoreException {
		return RefreshUtil.toResources(scope);
	}

	/**
	 * Returns the refresh scope attribute specified by the given launch configuration
	 * or <code>null</code> if none.
	 *
	 * @param configuration launch configuration
	 * @return refresh scope attribute (<code>ATTR_REFRESH_SCOPE</code>)
	 * @throws CoreException if unable to access the associated attribute
	 */
	public static String getRefreshScope(ILaunchConfiguration configuration) throws CoreException {
		return configuration.getAttribute(ATTR_REFRESH_SCOPE, (String) null);
	}

	/**
	 * Returns whether the refresh scope specified by the given launch
	 * configuration is recursive.
	 *
	 * @param configuration the configuration to check for recursive refresh being set
	 * @return whether the refresh scope is recursive
	 * @throws CoreException if unable to access the associated attribute
	 */
	public static boolean isRefreshRecursive(ILaunchConfiguration configuration) throws CoreException {
		return configuration.getAttribute(ATTR_REFRESH_RECURSIVE, true);
	}

	/**
	 * Creates and returns a memento for the given working set, to be used as a
	 * refresh attribute.
	 *
	 * @param workingSet a working set, or <code>null</code>
	 * @return an equivalent refresh attribute
	 */
	public static String getRefreshAttribute(IWorkingSet workingSet) {
		if (workingSet == null || workingSet.getElements().length == 0) {
			return RefreshUtil.toMemento(new IResource[0]);
		} else {
			IAdaptable[] elements = workingSet.getElements();
			IResource[] resources = new IResource[elements.length];
			for (int i = 0; i < resources.length; i++) {
				resources[i]= elements[i].getAdapter(IResource.class);
			}
			return RefreshUtil.toMemento(resources);
		}
	}

	/**
	 * Creates and returns a working set from the given refresh attribute created by
	 * the method <code>getRefreshAttribute(IWorkingSet)</code>, or <code>null</code>
	 * if none.
	 *
	 * @param refreshAttribute a refresh attribute that represents a working set
	 * @return equivalent working set, or <code>null</code>
	 */
	public static IWorkingSet getWorkingSet(String refreshAttribute) {
		if (refreshAttribute.startsWith("${working_set:")) { //$NON-NLS-1$
			try {
				IResource[] resources = RefreshUtil.toResources(refreshAttribute);
				IWorkingSetManager workingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();
				IWorkingSet workingSet = workingSetManager.createWorkingSet(StringSubstitutionMessages.RefreshTab_1, resources);
				return workingSet;
			} catch (CoreException e) {
				DebugUIPlugin.log(e);
			}
		}
		return null;
	}

	@Override
	public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
		// do nothing on activation
	}

	@Override
	public void deactivated(ILaunchConfigurationWorkingCopy workingCopy) {
		// do nothing on deactivation
	}

	/**
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getId()
	 *
	 * @since 3.5
	 */
	@Override
	public String getId() {
		return "org.eclipse.debug.ui.refreshTab"; //$NON-NLS-1$
	}
}
