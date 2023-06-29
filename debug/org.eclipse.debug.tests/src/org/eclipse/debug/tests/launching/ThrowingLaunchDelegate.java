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
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate2;
import org.eclipse.debug.core.model.ISourceLocator;

public class ThrowingLaunchDelegate implements ILaunchConfigurationDelegate2 {

	class ThrowingLaunch extends Launch {
		public ThrowingLaunch(ILaunchConfiguration launchConfiguration, String mode, ISourceLocator locator) {
			super(launchConfiguration, mode, locator);
		}
	}

	enum ThrowingEnum {
		launch, buildForLaunch, finalLaunchCheck, preLaunchCheck;
	}

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		if (configuration.getAttribute("throw.launch", true)) { //$NON-NLS-1$
			throw new CoreException(Status.error(ThrowingEnum.launch.toString()));
		}
	}

	@Override
	public ILaunch getLaunch(ILaunchConfiguration configuration, String mode) throws CoreException {
		return new ThrowingLaunch(configuration, "run", null); //$NON-NLS-1$
	}

	@Override
	public boolean buildForLaunch(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
		if (configuration.getAttribute("throw.buildForLaunch", true)) { //$NON-NLS-1$
			throw new CoreException(Status.error(ThrowingEnum.buildForLaunch.toString()));
		}
		return true;
	}

	@Override
	public boolean finalLaunchCheck(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
		if (configuration.getAttribute("throw.finalLaunchCheck", true)) { //$NON-NLS-1$
			throw new CoreException(Status.error(ThrowingEnum.finalLaunchCheck.toString()));
		}
		return true;
	}

	@Override
	public boolean preLaunchCheck(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
		if (configuration.getAttribute("throw.preLaunchCheck", true)) { //$NON-NLS-1$
			throw new CoreException(Status.error(ThrowingEnum.preLaunchCheck.toString()));
		}
		return true;
	}
}
