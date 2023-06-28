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
package org.eclipse.debug.internal.ui.launchConfigurations;


import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationTabGroup;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;

/**
 * Creates a new launch configuration based on the selection.
 */
public class CreateLaunchConfigurationAction extends AbstractLaunchConfigurationAction {

	/**
	 * Action identifier for IDebugView#getAction(String)
	 */
	public static final String ID_CREATE_ACTION = DebugUIPlugin.getUniqueIdentifier() + ".ID_CREATE_ACTION"; //$NON-NLS-1$

	/**
	 * Constructs an action to create a launch configuration
	 * @param viewer the viewer
	 * @param mode the mode the action applies to
	 */
	public CreateLaunchConfigurationAction(Viewer viewer, String mode) {
		super(LaunchConfigurationsMessages.CreateLaunchConfigurationAction_Ne_w_1, viewer, mode);
	}

	/**
	 * @see AbstractLaunchConfigurationAction#performAction()
	 */
	@Override
	protected void performAction() {
		Object object = getStructuredSelection().getFirstElement();
		//double click with Ctrl key mask results in empty selection: bug 156087
		//do no work if the selection is null
		if(object != null) {
			ILaunchConfigurationType type= null;
			// Construct a new config of the selected type
			if (object instanceof ILaunchConfiguration) {
				ILaunchConfiguration config= (ILaunchConfiguration) object;
				try {
					type = config.getType();
				} catch (CoreException e) {
					errorDialog(e);
					return;
				}
			} else {
				type = (ILaunchConfigurationType) object;
			}
			try {
				ILaunchConfigurationWorkingCopy wc = type.newInstance(null, DebugPlugin.getDefault().getLaunchManager().generateLaunchConfigurationName(LaunchConfigurationsMessages.CreateLaunchConfigurationAction_New_configuration_2));
				ILaunchConfigurationTabGroup tabGroup = LaunchConfigurationPresentationManager.getDefault().getTabGroup(wc, getMode());
				// this only works because this action is only present when the dialog is open
				ILaunchConfigurationDialog dialog = LaunchConfigurationsDialog.getCurrentlyVisibleLaunchConfigurationDialog();
				tabGroup.createTabs(dialog, dialog.getMode());
				ILaunchConfigurationTab[] tabs = tabGroup.getTabs();
				for (ILaunchConfigurationTab tab : tabs) {
					tab.setLaunchConfigurationDialog(dialog);
				}
				tabGroup.setDefaults(wc);
				tabGroup.dispose();
				wc.doSave();
			} catch (CoreException e) {
				errorDialog(e);
				return;
			}
		}
	}

	/**
	 * @see org.eclipse.ui.actions.SelectionListenerAction#updateSelection(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		return selection.size() == 1;
	}

	@Override
	public ImageDescriptor getDisabledImageDescriptor() {
		return DebugUITools.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_NEW_CONFIG);
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return DebugUITools.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_NEW_CONFIG);
	}

	@Override
	public String getToolTipText() {
		return LaunchConfigurationsMessages.LaunchConfigurationsDialog_0;
	}

}
