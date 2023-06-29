/*******************************************************************************
 *  Copyright (c) 2017 Red Hat Inc. and others.
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
 *     Ian Pun & Lucas Bullen - Export Functionality based off of AbstractLaunchConfigurationAction
 *******************************************************************************/
package org.eclipse.debug.internal.ui.launchConfigurations;

import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.importexport.launchconfigurations.ExportLaunchConfigurationsWizard;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;

public class ExportLaunchConfigurationAction extends AbstractLaunchConfigurationAction {
	/**
	 * Action identifier for IDebugView#getAction(String)
	 */
	public static final String ID_EXPORT_ACTION = DebugUIPlugin.getUniqueIdentifier() + ".ID_EXPORT_ACTION"; //$NON-NLS-1$

	/**
	 * Constructs an action to export launch configuration(s)
	 */
	public ExportLaunchConfigurationAction(Viewer viewer, String mode) {
		super(LaunchConfigurationsMessages.ExportLaunchConfigurationAction_Export_1, viewer, mode);
	}

	@Override
	protected void performAction() {
		IStructuredSelection selection = getStructuredSelection();
		ExportLaunchConfigurationsWizard wizard;
		if (selection == null) {
			wizard = new ExportLaunchConfigurationsWizard();
		} else {
			wizard = new ExportLaunchConfigurationsWizard(selection);
		}
		wizard.init(PlatformUI.getWorkbench(), null);
		WizardDialog dialog = new WizardDialog(getShell(), wizard);
		dialog.open();
	}

	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		return selection.size() > 0;
	}

	@Override
	public ImageDescriptor getDisabledImageDescriptor() {
		return DebugUITools.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_EXPORT_CONFIG);
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return DebugUITools.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_EXPORT_CONFIG);
	}

	@Override
	public String getToolTipText() {
		return LaunchConfigurationsMessages.LaunchConfigurationsDialog_6;
	}

}
