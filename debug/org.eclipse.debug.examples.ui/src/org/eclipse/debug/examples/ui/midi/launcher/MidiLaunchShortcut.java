/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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
package org.eclipse.debug.examples.ui.midi.launcher;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.examples.core.midi.launcher.MidiLaunchDelegate;
import org.eclipse.debug.examples.ui.pda.DebugUIPlugin;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

/**
 * Launch shortcut for a MIDI file.
 *
 * @since 1.0
 */
public class MidiLaunchShortcut implements ILaunchShortcut {

	@Override
	public void launch(ISelection selection, String mode) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) selection;
			if (ss.size() == 1) {
				Object element = ss.getFirstElement();
				if (element instanceof IFile) {
					IFile file = (IFile) element;
					ILaunchConfiguration configuration = getConfiguration(file);
					if (configuration != null) {
						DebugUITools.launch(configuration, mode);
					}
				}
			}
		}
	}

	/**
	 * Returns a MIDI configuration to use for the given file or
	 * <code>null</code> to cancel. Creates a new configuration
	 * if required.
	 *
	 * @param file file
	 * @return associated launch configuration or <code>null</code>
	 */
	private ILaunchConfiguration getConfiguration(IFile file) {
		List<ILaunchConfiguration> candiates = new ArrayList<>();
		try {
			ILaunchConfiguration[] configurations = getLaunchManager().getLaunchConfigurations(getLaunchType());
			for (ILaunchConfiguration configuration : configurations) {
				IResource[] resources = configuration.getMappedResources();
				if (resources != null && resources.length == 1 &&
						resources[0].equals(file)) {
					candiates.add(configuration);
				}
			}
		} catch (CoreException e) {
		}
		if (!candiates.isEmpty()) {
			return chooseConfiguration(candiates);
		}
		return newConfiguration(file);
	}

	/**
	 * Returns the MIDI launch configuration type.
	 *
	 * @return the MIDI launch configuration type
	 */
	private ILaunchConfigurationType getLaunchType() {
		ILaunchManager manager = getLaunchManager();
		ILaunchConfigurationType type = manager.getLaunchConfigurationType(MidiLaunchDelegate.ID_MIDI_LAUNCH_CONFIGURATION_TYPE);
		return type;
	}

	/**
	 * Returns the launch manager.
	 *
	 * @return launch manager
	 */
	private ILaunchManager getLaunchManager() {
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		return manager;
	}

	/**
	 * Returns a configuration from the given collection of configurations that should be launched,
	 * or <code>null</code> to cancel.
	 *
	 * @param configList list of configurations to choose from
	 * @return configuration to launch or <code>null</code> to cancel
	 */
	private ILaunchConfiguration chooseConfiguration(List<ILaunchConfiguration> configList) {
		if (configList.size() == 1) {
			return configList.get(0);
		}
		IDebugModelPresentation labelProvider = DebugUITools.newDebugModelPresentation();
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(DebugUIPlugin.getActiveWorkbenchShell(), labelProvider);
		dialog.setElements(configList.toArray());
		dialog.setTitle("Select Configuraiton"); //$NON-NLS-1$
		dialog.setMessage("&Select an existing configuration:"); //$NON-NLS-1$
		dialog.setMultipleSelection(false);
		int result = dialog.open();
		labelProvider.dispose();
		if (result == Window.OK) {
			return (ILaunchConfiguration) dialog.getFirstResult();
		}
		return null;
	}

	/**
	 * Creates and returns a new MIDI launch configuration for the
	 * given file.
	 *
	 * @param file MIDI file
	 * @return new launch configuration
	 */
	private ILaunchConfiguration newConfiguration(IFile file) {
		ILaunchConfigurationType type = getLaunchType();
		try {
			ILaunchConfigurationWorkingCopy workingCopy = type.newInstance(null, getLaunchManager().
					generateLaunchConfigurationName(
"[" + file.getProject().getName() + "] " + file.getName())); //$NON-NLS-1$ //$NON-NLS-2$
			workingCopy.setAttribute(MidiLaunchDelegate.ATTR_MIDI_FILE, file.getFullPath().toString());
			workingCopy.setMappedResources(new IResource[]{file});
			return workingCopy.doSave();
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void launch(IEditorPart editor, String mode) {
		// nothing - currently no editor
	}
}
