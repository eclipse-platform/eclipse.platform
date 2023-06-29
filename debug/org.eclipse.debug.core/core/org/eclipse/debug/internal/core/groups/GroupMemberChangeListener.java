/*******************************************************************************
 *  Copyright (c) 2016, 2017 SSI Schaefer IT Solutions GmbH and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      SSI Schaefer IT Solutions GmbH
 *      Axel Richard (Obeo) - Bug 41353 - Launch configurations prototypes
 *******************************************************************************/
package org.eclipse.debug.internal.core.groups;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationListener;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;

/**
 * Manages renames of launch configurations that are members of group launches
 *
 * @since 3.11
 */
public class GroupMemberChangeListener implements ILaunchConfigurationListener {

	private static final String GROUP_TYPE_ID = "org.eclipse.debug.core.groups.GroupLaunchConfigurationType"; //$NON-NLS-1$

	@Override
	public void launchConfigurationAdded(ILaunchConfiguration configuration) {
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfiguration original = launchManager.getMovedFrom(configuration);
		if (original != null) {
			ILaunchConfigurationType type = launchManager.getLaunchConfigurationType(GROUP_TYPE_ID);
			if (type == null) {
				DebugPlugin.logMessage("cannot find group launch configuration type", null); //$NON-NLS-1$
				return;
			}
			try {
				for (ILaunchConfiguration c : DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(type, ILaunchConfiguration.CONFIGURATION | ILaunchConfiguration.PROTOTYPE)) {
					List<GroupLaunchElement> elements = GroupLaunchConfigurationDelegate.createLaunchElements(c);
					boolean updated = false;
					for (GroupLaunchElement e : elements) {
						if (e.name.equals(original.getName())) {
							updated = true;
							e.name = configuration.getName();
						}
					}

					if (updated) {
						ILaunchConfigurationWorkingCopy workingCopy = c.getWorkingCopy();
						GroupLaunchConfigurationDelegate.storeLaunchElements(workingCopy, elements);
						workingCopy.doSave();
					}
				}
			} catch (CoreException e) {
				DebugPlugin.log(e);
			}
		}
	}

	@Override
	public void launchConfigurationChanged(ILaunchConfiguration configuration) {
	}

	@Override
	public void launchConfigurationRemoved(ILaunchConfiguration configuration) {
	}

}
