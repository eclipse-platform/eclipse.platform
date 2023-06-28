/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
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

import org.eclipse.debug.internal.ui.LaunchConfigurationTabExtension;
import org.eclipse.ui.IPluginContribution;

/**
 * This class provides a wrapper for a launch tab contribution so that it can be filtered from the UI
 * via the use of capabilities
 *
 * @since 3.3
 */
public class LaunchTabContribution implements IPluginContribution {

	LaunchConfigurationTabExtension fTab = null;

	public LaunchTabContribution(LaunchConfigurationTabExtension tab) {
		fTab = tab;
	}

	/**
	 * @see org.eclipse.ui.IPluginContribution#getLocalId()
	 */
	@Override
	public String getLocalId() {
		return fTab.getIdentifier();
	}

	/**
	 * @see org.eclipse.ui.IPluginContribution#getPluginId()
	 */
	@Override
	public String getPluginId() {
		return fTab.getPluginIdentifier();
	}

}
