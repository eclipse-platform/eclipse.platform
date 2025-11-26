/*******************************************************************************
 *  Copyright (c) 2025 IBM Corporation.
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
package org.eclipse.debug.internal.ui.launchConfigurations;

import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.importexport.launchconfigurations.ImportLaunchConfigurationsWizard;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;

public class ImportLaunchConfigurationAction extends AbstractLaunchConfigurationAction {
	/**
	 * Action identifier for IDebugView#getAction(String)
	 */
	public static final String ID_IMPORT_ACTION = DebugUIPlugin.getUniqueIdentifier() + ".ID_IMPORT_ACTION"; //$NON-NLS-1$

	/**
	 * Constructs an action to import launch configuration(s)
	 */
	public ImportLaunchConfigurationAction(Viewer viewer, String mode) {
		super(LaunchConfigurationsMessages.ImportLaunchConfigurationAction, viewer, mode);
	}

	@Override
	protected void performAction() {
		ImportLaunchConfigurationsWizard wizard = new ImportLaunchConfigurationsWizard();
		wizard.init(PlatformUI.getWorkbench(), null);
		WizardDialog dialog = new WizardDialog(getShell(), wizard);
		dialog.open();
	}

	@Override
	public ImageDescriptor getDisabledImageDescriptor() {
		return DebugUITools.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_IMPORT_CONFIG);
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return DebugUITools.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_IMPORT_CONFIG);
	}

	@Override
	public String getToolTipText() {
		return LaunchConfigurationsMessages.LaunchConfigurationImportDialog;
	}

}
