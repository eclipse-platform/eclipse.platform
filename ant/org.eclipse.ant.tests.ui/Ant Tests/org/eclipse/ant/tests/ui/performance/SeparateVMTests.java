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

import static org.eclipse.ant.tests.ui.testplugin.AntUITestUtil.assertProject;
import static org.eclipse.ant.tests.ui.testplugin.AntUITestUtil.getLaunchConfiguration;
import static org.eclipse.ant.tests.ui.testplugin.AntUITestUtil.launchAndTerminate;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.ant.tests.ui.testplugin.AntUITestUtil;
import org.eclipse.ant.tests.ui.testplugin.CloseWelcomeScreenExtension;
import org.eclipse.core.externaltools.internal.IExternalToolConstants;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.test.performance.PerformanceTestCaseJunit5;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@SuppressWarnings("restriction")
@ExtendWith(CloseWelcomeScreenExtension.class)
public class SeparateVMTests extends PerformanceTestCaseJunit5 {

	@BeforeEach
	void setup() throws Exception {
		assertProject();
	}

	/**
	 * Performance test for launching Ant in a separate vm.
	 */
	@Test
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
	@Test
	public void testBuildNoConsole() throws CoreException {
		// tagAsSummary("Separate JRE Build; capture output off", Dimension.ELAPSED_PROCESS);
		ILaunchConfiguration config = getLaunchConfiguration("echoingSepVM"); //$NON-NLS-1$
		assertNotNull(config, "Could not locate launch configuration for " + "echoingSepVM"); //$NON-NLS-1$ //$NON-NLS-2$
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
	@Test
	public void testBuildMinusDebug() throws CoreException {
		// tagAsSummary("Separate JRE Build; -debug", Dimension.ELAPSED_PROCESS);
		ILaunchConfiguration config = getLaunchConfiguration("echoingSepVM"); //$NON-NLS-1$
		assertNotNull(config, "Could not locate launch configuration for " + "echoingSepVM"); //$NON-NLS-1$ //$NON-NLS-2$
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
	@Test
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

	/**
	 * Launches the Ant build for this config. Waits for all of the lines to be
	 * appended to the console.
	 *
	 * @param config the launch configuration to execute
	 * @param i      the number of times to perform the launch
	 */
	private void launch(ILaunchConfiguration config, int i) throws CoreException {
		startMeasuring();
		for (int j = 0; j < i; j++) {
			AntUITestUtil.launch(config);
		}
		stopMeasuring();
	}
}
