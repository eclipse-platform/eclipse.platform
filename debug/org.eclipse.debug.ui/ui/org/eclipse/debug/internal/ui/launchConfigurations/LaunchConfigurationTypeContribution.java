/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
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

import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.ui.IPluginContribution;

/**
 * A plug-in contribution (UI element) which contains a launch configuration
 * type (Core element). Plug-in contributions are passed to the workbench
 * activity support to filter elements from the UI.
 */
class LaunchConfigurationTypeContribution implements IPluginContribution {

	protected ILaunchConfigurationType type;

	/**
	 * Creates a new plug-in contribution for the given type
	 *
	 * @param type the launch configuration type
	 */
	public LaunchConfigurationTypeContribution(ILaunchConfigurationType type) {
		this.type= type;
	}

	@Override
	public String getLocalId() {
		return type.getIdentifier();
	}

	@Override
	public String getPluginId() {
		return type.getPluginIdentifier();
	}

}
