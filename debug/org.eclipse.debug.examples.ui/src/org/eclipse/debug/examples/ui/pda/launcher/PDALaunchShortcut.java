/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
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
 *     Bjorn Freeman-Benson - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.examples.ui.pda.launcher;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.examples.core.pda.DebugCorePlugin;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;


/**
 * Launches a PDA file
 */
public class PDALaunchShortcut implements ILaunchShortcut {

	@Override
	public void launch(ISelection selection, String mode) {
		// must be a structured selection with one file selected
		IFile file = (IFile) ((IStructuredSelection)selection).getFirstElement();

		// check for an existing launch config for the pda file
		String path = file.getFullPath().toString();
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type = launchManager.getLaunchConfigurationType(DebugCorePlugin.ID_PDA_LAUNCH_CONFIGURATION_TYPE);
		try {
			ILaunchConfiguration[] configurations = launchManager.getLaunchConfigurations(type);
			for (int i = 0; i < configurations.length; i++) {
				ILaunchConfiguration configuration = configurations[i];
				String attribute = configuration.getAttribute(DebugCorePlugin.ATTR_PDA_PROGRAM, (String)null);
				if (path.equals(attribute)) {
					DebugUITools.launch(configuration, mode);
					return;
				}
			}
		} catch (CoreException e) {
			return;
		}

		try {
			// create a new configuration for the pda file
			ILaunchConfigurationWorkingCopy workingCopy = type.newInstance(null, file.getName());
			workingCopy.setAttribute(DebugCorePlugin.ATTR_PDA_PROGRAM, path);
			workingCopy.setMappedResources(new IResource[]{file});
			ILaunchConfiguration configuration = workingCopy.doSave();
			DebugUITools.launch(configuration, mode);
		} catch (CoreException e1) {
		}
	}

	@Override
	public void launch(IEditorPart editor, String mode) {
	}

}
