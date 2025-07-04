/*******************************************************************************
 * Copyright (c) 2000, 2025 IBM Corporation and others.
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
 *     Alexander Fedorov (ArSysOp) - API to process launch configuration attributes
 *******************************************************************************/
package org.eclipse.ant.internal.ui.launchConfigurations;

import java.util.Optional;

import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.AntUtil;
import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.ant.internal.ui.IAntUIHelpContextIds;
import org.eclipse.ant.launching.IAntLaunchConstants;
import org.eclipse.core.externaltools.internal.IExternalToolConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.externaltools.internal.launchConfigurations.ExternalToolsMainTab;
import org.eclipse.ui.externaltools.internal.ui.FileSelectionDialog;

public class AntMainTab extends ExternalToolsMainTab {

	private Optional<String> fCurrentLocation;
	private Button fSetInputHandlerButton;
	private IFile fNewFile;

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		super.initializeFrom(configuration);
		fCurrentLocation = IExternalToolConstants.LAUNCH_ATTRIBUTE_LOCATION.probe(configuration);
		updateCheckButtons(configuration);
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		super.performApply(configuration);
		// has the location changed
		Optional<String> newLocation = IExternalToolConstants.LAUNCH_ATTRIBUTE_LOCATION.probe(configuration);
		if (!newLocation.equals(fCurrentLocation)) {
			updateTargetsTab();
			fCurrentLocation = newLocation;
			updateProjectName(configuration);
		}
		setMappedResources(configuration);
		setAttribute(IAntUIConstants.SET_INPUTHANDLER, configuration, fSetInputHandlerButton.getSelection(), true);
	}

	private void setMappedResources(ILaunchConfigurationWorkingCopy configuration) {
		// Don't map resources for external tool builders - they don't show up in the launch history
		try {
			if (!IAntLaunchConstants.ID_ANT_BUILDER_LAUNCH_CONFIGURATION_TYPE.equals(configuration.getType().getIdentifier())) {
				IFile file = getIFile(configuration);
				configuration.setMappedResources(new IResource[] { file });
			}
		}
		catch (CoreException e) {
			// do nothing
		}
	}

	private void updateProjectName(ILaunchConfigurationWorkingCopy configuration) {
		IFile file = getIFile(configuration);
		String projectName = IAntCoreConstants.EMPTY_STRING;
		if (file != null) {
			projectName = file.getProject().getName();
		}
		configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectName);
	}

	private IFile getIFile(ILaunchConfigurationWorkingCopy configuration) {
		if (fNewFile != null) {
			IFile file = fNewFile;
			fNewFile = null;
			return file;
		}
		return IExternalToolConstants.LAUNCH_ATTRIBUTE_LOCATION.probe(configuration).map(exp -> AntUtil.getFileForLocation(exp, null)).orElse(null);
	}

	@Override
	public void createControl(Composite parent) {
		Composite mainComposite = new Composite(parent, SWT.NONE);
		setControl(mainComposite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(mainComposite, IAntUIHelpContextIds.ANT_MAIN_TAB);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		mainComposite.setLayout(layout);
		mainComposite.setLayoutData(gridData);
		mainComposite.setFont(parent.getFont());
		createLocationComponent(mainComposite);
		createWorkDirectoryComponent(mainComposite);
		createArgumentComponent(mainComposite);
		createVerticalSpacer(mainComposite, 2);
		createSetInputHandlerComponent(mainComposite);
		Dialog.applyDialogFont(parent);
	}

	/**
	 * Creates the controls needed to edit the set input handler attribute of an Ant build
	 *
	 * @param parent
	 *            the composite to create the controls in
	 */
	private void createSetInputHandlerComponent(Composite parent) {
		fSetInputHandlerButton = createCheckButton(parent, AntLaunchConfigurationMessages.AntMainTab_0);
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		data.horizontalSpan = 2;
		fSetInputHandlerButton.setLayoutData(data);
		fSetInputHandlerButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();
			}
		});
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		super.setDefaults(configuration);
		// prevent a new blank configuration from being dirty when first created and not yet edited
		setMappedResources(configuration);
	}

	private void updateCheckButtons(ILaunchConfiguration configuration) {
		boolean setInputHandler = true;
		try {
			setInputHandler = configuration.getAttribute(IAntUIConstants.SET_INPUTHANDLER, true);
		}
		catch (CoreException ce) {
			AntUIPlugin.log(AntLaunchConfigurationMessages.AntMainTab_1, ce);
		}
		fSetInputHandlerButton.setSelection(setInputHandler);
	}

	@Override
	protected void handleWorkspaceLocationButtonSelected() {
		FileSelectionDialog dialog;
		dialog = new FileSelectionDialog(getShell(), ResourcesPlugin.getWorkspace().getRoot(), AntLaunchConfigurationMessages.AntMainTab__Select_a_build_file__1);
		dialog.open();
		IStructuredSelection result = dialog.getResult();
		if (result == null) {
			return;
		}
		Object file = result.getFirstElement();
		if (file instanceof IFile) {
			fNewFile = (IFile) file;
			locationField.setText(VariablesPlugin.getDefault().getStringVariableManager().generateVariableExpression("workspace_loc", fNewFile.getFullPath().toString())); //$NON-NLS-1$
		}
	}

	@Override
	protected String getWorkingDirectoryLabel() {
		return AntLaunchConfigurationMessages.AntMainTab_3;
	}

	private void updateTargetsTab() {
		// the location has changed...set the targets tab to
		// need to be recomputed
		for (ILaunchConfigurationTab tab : getLaunchConfigurationDialog().getTabs()) {
			if (tab instanceof AntTargetsTab) {
				((AntTargetsTab) tab).setDirty(true);
				break;
			}
		}
	}

	@Override
	protected String getLocationLabel() {
		return AntLaunchConfigurationMessages.AntMainTab_6;
	}
}
