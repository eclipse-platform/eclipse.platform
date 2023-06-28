/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
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
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate2;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;

/**
 * A launch delegate for testing which does nothing or delegate method
 * invocations to a delegate if a delegate was set to delegate.
 */
public class TestLaunchDelegate extends LaunchConfigurationDelegate {

	private ILaunchConfigurationDelegate2 delegate = null;

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		if (delegate != null) {
			delegate.launch(configuration, mode, launch, monitor);
		}
	}

	@Override
	public boolean buildForLaunch(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
		if (delegate != null) {
			return delegate.buildForLaunch(configuration, mode, monitor);
		}
		return super.buildForLaunch(configuration, mode, monitor);
	}

	@Override
	public boolean preLaunchCheck(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
		if (delegate != null) {
			return delegate.preLaunchCheck(configuration, mode, monitor);
		}
		return super.preLaunchCheck(configuration, mode, monitor);
	}

	@Override
	public boolean finalLaunchCheck(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
		if (delegate != null) {
			delegate.finalLaunchCheck(configuration, mode, monitor);
		}
		return super.finalLaunchCheck(configuration, mode, monitor);
	}

	public ILaunchConfigurationDelegate2 getDelegate() {
		return delegate;
	}

	public void setDelegate(ILaunchConfigurationDelegate2 delegate) {
		this.delegate = delegate;
	}
}
