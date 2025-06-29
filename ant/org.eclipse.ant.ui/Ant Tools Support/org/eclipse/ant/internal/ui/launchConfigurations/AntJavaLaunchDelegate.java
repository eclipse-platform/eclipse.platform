/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.ant.internal.ui.launchConfigurations;

import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaLaunchDelegate;

/**
 * Used by the AntLaunchDelegate for Ant builds in a separate VM The sub-classing is needed to be able to launch an Ant build from a non-Java project
 */
public class AntJavaLaunchDelegate extends JavaLaunchDelegate {
	@Override
	public boolean preLaunchCheck(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
		try {
			return super.preLaunchCheck(configuration, mode, monitor);
		}
		catch (CoreException ce) {
			// likely dealing with a non-Java project
		}
		// no need to check for breakpoints as always in run mode
		return true;
	}

	@Override
	public String getProgramArguments(ILaunchConfiguration configuration) throws CoreException {
		try {
			return super.getProgramArguments(configuration);
		}
		catch (CoreException ce) {
			// do nothing
		}
		return configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, IAntCoreConstants.EMPTY_STRING);
	}
}
