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

import org.eclipse.debug.core.ILaunchDelegate;
import org.eclipse.ui.IPluginContribution;

/**
 * This class provides a wrapper for a launch delegate so tht it can be filtered from UI and launching choices
 * @since 3.3
 */
public class LaunchDelegateContribution implements IPluginContribution {

	private ILaunchDelegate fDelegate = null;

	/**
	 * Constructor
	 * @param delegate
	 */
	public LaunchDelegateContribution(ILaunchDelegate delegate) {
		fDelegate = delegate;
	}

	/**
	 * @see org.eclipse.ui.IPluginContribution#getLocalId()
	 */
	@Override
	public String getLocalId() {
		return fDelegate.getId();
	}

	/**
	 * @see org.eclipse.ui.IPluginContribution#getPluginId()
	 */
	@Override
	public String getPluginId() {
		return fDelegate.getPluginIdentifier();
	}

}
