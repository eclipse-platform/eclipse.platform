/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;

/**
 * Duplicates the selected launch configuration.
 */
public class DuplicateLaunchConfigurationAction extends AbstractLaunchConfigurationAction {

	/**
	 * Action identifier for IDebugView#getAction(String)
	 */
	public static final String ID_DUPLICATE_ACTION = DebugUIPlugin.getUniqueIdentifier() + ".ID_DUPLICATE_ACTION"; //$NON-NLS-1$

	/**
	 * Constructs an action to duplicate a launch configuration
	 */
	public DuplicateLaunchConfigurationAction(Viewer viewer, String mode) {
		super(LaunchConfigurationsMessages.DuplicateLaunchConfigurationAction__Duplicate_1, viewer, mode);
	}

	/**
	 * @see AbstractLaunchConfigurationAction#performAction()
	 */
	@Override
	protected void performAction() {
		ILaunchConfiguration original = (ILaunchConfiguration)getStructuredSelection().getFirstElement();
		String newName = DebugPlugin.getDefault().getLaunchManager().generateLaunchConfigurationName(original.getName());
		try {
			ILaunchConfigurationWorkingCopy newWorkingCopy = original.copy(newName);
			newWorkingCopy.doSave();
		} catch (CoreException e) {
			errorDialog(e);
		}
	}

	/**
	 * @see org.eclipse.ui.actions.SelectionListenerAction#updateSelection(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		return selection.size() == 1 && selection.getFirstElement() instanceof ILaunchConfiguration;
	}

	@Override
	public ImageDescriptor getDisabledImageDescriptor() {
		return DebugUITools.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_DUPLICATE_CONFIG);
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return DebugUITools.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_DUPLICATE_CONFIG);
	}

	@Override
	public String getToolTipText() {
		return LaunchConfigurationsMessages.LaunchConfigurationsDialog_5;
	}

}
