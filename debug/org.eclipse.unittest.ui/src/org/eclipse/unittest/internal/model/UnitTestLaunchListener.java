/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package org.eclipse.unittest.internal.model;

import java.util.HashSet;

import org.eclipse.unittest.internal.UnitTestPlugin;
import org.eclipse.unittest.internal.launcher.TestListenerRegistry;
import org.eclipse.unittest.internal.launcher.TestRunListener;
import org.eclipse.unittest.internal.launcher.TestViewSupportRegistry;
import org.eclipse.unittest.internal.launcher.UnitTestLaunchConfigurationConstants;
import org.eclipse.unittest.ui.ITestViewSupport;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchListener;

/**
 * Used to track new launches. We need to do this so that we only attach a
 * TestRunner once to a launch. Once a test runner is connected, it is removed
 * from the set.
 */
public class UnitTestLaunchListener implements ILaunchListener {

	/**
	 * Used to track new launches. We need to do this so that we only attach a
	 * TestRunner once to a launch. Once a test runner is connected, it is removed
	 * from the set.
	 */
	private HashSet<ILaunch> fTrackedLaunches = new HashSet<>(20);

	@Override
	public void launchAdded(ILaunch launch) {
		ILaunchConfiguration config = launch.getLaunchConfiguration();
		if (config == null)
			return;

		try {
			if (!config.hasAttribute(UnitTestLaunchConfigurationConstants.ATTR_UNIT_TEST_VIEW_SUPPORT))
				return;
		} catch (CoreException e1) {
			UnitTestPlugin.log(e1);
			return;
		}

		ITestViewSupport testRunnerViewSupport = TestViewSupportRegistry.newTestRunnerViewSupport(config).orElse(null);
		if (testRunnerViewSupport == null)
			return;

		fTrackedLaunches.add(launch);
	}

	@Override
	public void launchRemoved(final ILaunch launch) {
		fTrackedLaunches.remove(launch);
	}

	@Override
	public void launchChanged(final ILaunch launch) {
		if (!fTrackedLaunches.contains(launch))
			return;

		// Load session on 1st change (usually 1st process added), although it's not
		// much reliable. Each TestRunnerClient should take care of listening to the
		// launch to get the right IProcess or stream or whatever else i useful
		if (UnitTestModel.getInstance().getTestRunSessions().stream()
				.noneMatch(session -> launch.equals(session.getLaunch()))) {
			TestRunSession testRunSession = new TestRunSession(launch);
			UnitTestModel.getInstance().addTestRunSession(testRunSession);
			for (TestRunListener listener : TestListenerRegistry.getDefault().getUnitTestRunListeners()) {
				listener.sessionLaunched(testRunSession);
			}
		}
	}
}
