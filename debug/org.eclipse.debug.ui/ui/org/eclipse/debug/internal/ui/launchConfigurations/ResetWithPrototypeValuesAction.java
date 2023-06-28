/*******************************************************************************
 * Copyright (c) 2017 Obeo.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.launchConfigurations;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;

/**
 * Reset selected launch configuration(s) attributes with values from associated
 * prototype(s).
 *
 * @since 3.13
 */
public class ResetWithPrototypeValuesAction extends AbstractLaunchConfigurationAction {

	/**
	 * Action identifier for IDebugView#getAction(String)
	 */
	public static final String ID_RESET_WITH_PROTOTYPE_VALUES_ACTION = DebugUIPlugin.getUniqueIdentifier() + ".ID_RESET_WITH_PROTOTYPE_VALUES_ACTION"; //$NON-NLS-1$

	/**
	 * Constructs an action to reset a launch configuration with prototype
	 * values
	 *
	 * @param viewer the viewer
	 * @param mode the mode the action applies to
	 */
	public ResetWithPrototypeValuesAction(Viewer viewer, String mode) {
		super(LaunchConfigurationsMessages.ResetWithPrototypeValuesAction_Reset_with_prototype_values_1, viewer, mode);
	}

	/**
	 * @see AbstractLaunchConfigurationAction#performAction()
	 */
	@Override
	protected void performAction() {
		try {
			for (Object launchConfiguration : getStructuredSelection().toList()) {
				if (launchConfiguration instanceof ILaunchConfiguration) {
					ILaunchConfigurationWorkingCopy workingCopy = ((ILaunchConfiguration) launchConfiguration).getWorkingCopy();
					workingCopy.setPrototype(((ILaunchConfiguration) launchConfiguration).getPrototype(), true);
					workingCopy.doSave();
					// if only one configuration is selected, refresh the
					// tabs to display visible attributes values from the
					// prototype
					if (getStructuredSelection().size() == 1) {
						ILaunchConfigurationDialog dialog = LaunchConfigurationsDialog.getCurrentlyVisibleLaunchConfigurationDialog();
						if (dialog instanceof LaunchConfigurationsDialog) {
							((LaunchConfigurationsDialog) dialog).getTabViewer().setInput(workingCopy);
						}
					}
				}
			}
		} catch (CoreException e) {
			errorDialog(e);
		}
	}

	/**
	 * @see org.eclipse.ui.actions.SelectionListenerAction#updateSelection(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		boolean onlyLaunchConfigurationWithPrototypeSelected = false;
		for (Object object : selection.toList()) {
			if (object instanceof ILaunchConfiguration) {
				if (((ILaunchConfiguration) object).isPrototype()) {
					return false;
				} else {
					try {
						if (((ILaunchConfiguration) object).getPrototype() != null) {
							onlyLaunchConfigurationWithPrototypeSelected = true;
						} else {
							return false;
						}
					} catch (CoreException e) {
						DebugUIPlugin.log(e.getStatus());
					}
				}
			} else {
				return false;
			}
		}
		return onlyLaunchConfigurationWithPrototypeSelected;
	}

	@Override
	public ImageDescriptor getDisabledImageDescriptor() {
		return DebugUITools.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_RESET_PROTO);
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return DebugUITools.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_RESET_PROTO);
	}

	@Override
	public String getToolTipText() {
		return LaunchConfigurationsMessages.LaunchConfigurationsDialog_9;
	}
}
