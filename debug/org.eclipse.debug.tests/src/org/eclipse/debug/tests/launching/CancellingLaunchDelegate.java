/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
package org.eclipse.debug.tests.launching;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate2;
import org.eclipse.debug.core.model.ISourceLocator;

/**
 * Test launch delegate that will always cancel the launch
 *
 * @since 3.9.100
 */
public class CancellingLaunchDelegate implements ILaunchConfigurationDelegate2 {

	class CancellingLaunch extends Launch {

		public CancellingLaunch(ILaunchConfiguration launchConfiguration, String mode, ISourceLocator locator) {
			super(launchConfiguration, mode, locator);
			setAttribute("cancel.launch", Boolean.toString(true)); //$NON-NLS-1$
		}
	}

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		// do nothing, this thing does not launch
	}

	@Override
	public ILaunch getLaunch(ILaunchConfiguration configuration, String mode) throws CoreException {
		return new CancellingLaunch(configuration, "run", null); //$NON-NLS-1$
	}

	@Override
	public boolean buildForLaunch(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
		return configuration.getAttribute("cancel.buildForLaunch", true); //$NON-NLS-1$
	}

	@Override
	public boolean finalLaunchCheck(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
		return configuration.getAttribute("cancel.finalLaunchCheck", true); //$NON-NLS-1$
	}

	@Override
	public boolean preLaunchCheck(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
		return configuration.getAttribute("cancel.preLaunchCheck", true); //$NON-NLS-1$
	}
}
