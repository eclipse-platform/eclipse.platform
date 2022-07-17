/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
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

package org.eclipse.ant.tests.ui.performance;

import org.eclipse.ant.tests.ui.AbstractAntUIBuildPerformanceTest;
import org.eclipse.core.externaltools.internal.IExternalToolConstants;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.IDebugUIConstants;

@SuppressWarnings("restriction")
public class SeparateVMTests extends AbstractAntUIBuildPerformanceTest {

	public SeparateVMTests(String name) {
		super(name);
	}

	/**
	 * Performance test for launching Ant in a separate vm.
	 */
	public void testBuild() throws CoreException {
		// tagAsSummary("Separate JRE Build", Dimension.ELAPSED_PROCESS);
		ILaunchConfiguration config = getLaunchConfiguration("echoingSepVM"); //$NON-NLS-1$
		// possible first time hit of the SWT pieces getting written from the JAR to the
		// metadata area
		launchAndTerminate(config, 20000);
		for (int i = 0; i < 10; i++) {
			launch(config, 10);
		}
		commitMeasurements();
		assertPerformance();
	}

	/**
	 * Performance test for launching Ant in a separate vm with no console output.
	 */
	public void testBuildNoConsole() throws CoreException {
		// tagAsSummary("Separate JRE Build; capture output off", Dimension.ELAPSED_PROCESS);
		ILaunchConfiguration config = getLaunchConfiguration("echoingSepVM"); //$NON-NLS-1$
		assertNotNull("Could not locate launch configuration for " + "echoingSepVM", config); //$NON-NLS-1$ //$NON-NLS-2$
		ILaunchConfigurationWorkingCopy copy = config.getWorkingCopy();
		copy.setAttribute(IDebugUIConstants.ATTR_CAPTURE_IN_CONSOLE, false);
		copy.setAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, false);
		// possible first time hit of the SWT pieces getting written from the JAR to the
		// metadata area
		launchAndTerminate(copy, 20000);
		for (int i = 0; i < 10; i++) {
			launch(copy, 10);
		}
		commitMeasurements();
		assertPerformance();
	}

	/**
	 * Performance test for launching Ant in a separate vm with debug information.
	 */
	public void testBuildMinusDebug() throws CoreException {
		// tagAsSummary("Separate JRE Build; -debug", Dimension.ELAPSED_PROCESS);
		ILaunchConfiguration config = getLaunchConfiguration("echoingSepVM"); //$NON-NLS-1$
		assertNotNull("Could not locate launch configuration for " + "echoingSepVM", config); //$NON-NLS-1$ //$NON-NLS-2$
		ILaunchConfigurationWorkingCopy copy = config.getWorkingCopy();
		copy.setAttribute(IExternalToolConstants.ATTR_TOOL_ARGUMENTS, "-debug"); //$NON-NLS-1$
		// possible first time hit of the SWT pieces getting written from the JAR to the
		// metadata area
		launchAndTerminate(copy, 20000);
		for (int i = 0; i < 10; i++) {
			launch(copy, 10);
		}
		commitMeasurements();
		assertPerformance();
	}

	/**
	 * Performance test for launching Ant in a separate vm with lots of links
	 */
	public void testBuildWithLotsOfLinks() throws CoreException {
		// tagAsSummary("Separate JRE Build; links", Dimension.ELAPSED_PROCESS);
		ILaunchConfiguration config = getLaunchConfiguration("echoPropertiesSepVM"); //$NON-NLS-1$
		// possible first time hit of the SWT pieces getting written from the JAR to the
		// metadata area
		launchAndTerminate(config, 20000);
		for (int i = 0; i < 10; i++) {
			launch(config, 10);
		}
		commitMeasurements();
		assertPerformance();
	}
}
