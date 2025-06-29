/*******************************************************************************
 * Copyright (c) 2019 Paul Pazderski and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Paul Pazderski - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.tests.console;

import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.core.LaunchManager;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.views.console.ConsoleRemoveAllTerminatedAction;
import org.eclipse.debug.internal.ui.views.console.ProcessConsole;
import org.eclipse.debug.internal.ui.views.console.ProcessConsoleManager;
import org.eclipse.debug.tests.AbstractDebugTest;
import org.eclipse.debug.tests.TestUtil;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.junit.Test;

/**
 * Tests the ProcessConsoleManager.
 */
public class ProcessConsoleManagerTests extends AbstractDebugTest {

	/**
	 * Test addition and removal of a ProcessConsole. It also kind of tests
	 * {@link LaunchManager} because the ProcessConsoleManager primary works
	 * through an {@link ILaunchListener} which is honored by this test.
	 */
	@Test
	public void testProcessConsoleLifecycle() throws Exception {
		// ensure debug UI plugin is started before adding first launch
		DebugUIPlugin.getDefault();
		final ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		final IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
		final int existingNumConsoles = consoleManager.getConsoles().length;
		if (existingNumConsoles > 0) {
			// existing consoles must not harm this test but it may be
			// interesting in case the test fails
			TestUtil.log(IStatus.INFO, name.getMethodName(), "Found " + existingNumConsoles + " existing consoles on test start.");
		}

		ILaunch launch = null;
		final MockProcess mockProcess = new MockProcess(MockProcess.RUN_FOREVER);
		try {
			final IProcess process = mockProcess.toRuntimeProcess();
			launch = process.getLaunch();
			launchManager.addLaunch(launch);
			// do not wait on input read job
			TestUtil.waitForJobs(name.getMethodName(), ProcessConsoleManager.class, 0, 10000, ProcessConsole.class);
			assertThat(consoleManager.getConsoles()).as("console has been added").hasSize(1);
		} finally {
			mockProcess.destroy();
		}

		if (launch != null) {
			launchManager.removeLaunch(launch);
			TestUtil.waitForJobs(name.getMethodName(), ProcessConsoleManager.class, 0, 10000);
			assertThat(consoleManager.getConsoles()).as("console has been removed").isEmpty();
		}
	}

	/**
	 * Test problematic scenario where launch is already removed before console
	 * is created. see https://bugs.eclipse.org/bugs/show_bug.cgi?id=546710#c13
	 */
	@Test
	public void testBug546710_ConsoleCreationRaceCondition() throws Exception {
		final ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		for (ILaunch existingLaunch : launchManager.getLaunches()) {
			assertTrue("Found existing not terminated launch. This should not happen and can interfere this test. Check for leakages in previous run tests.", existingLaunch.isTerminated());
			launchManager.removeLaunch(existingLaunch);
		}

		final MockProcess mockProcess1 = new MockProcess(0);
		final IProcess process1 = mockProcess1.toRuntimeProcess("FirstMockProcess");
		final MockProcess mockProcess2 = new MockProcess(0);
		final IProcess process2 = mockProcess2.toRuntimeProcess("SecondMockProcess");
		try {
			setPreference(DebugUIPlugin.getDefault().getPreferenceStore(), IDebugUIConstants.PREF_AUTO_REMOVE_OLD_LAUNCHES, true);
			// Stop the JobManager to reliable trigger the tested race
			// condition.
			TestUtil.waitForJobs(name.getMethodName(), ProcessConsoleManager.class, 0, 10000);
			Job.getJobManager().suspend();
			launchManager.addLaunch(process1.getLaunch());
			launchManager.addLaunch(process2.getLaunch());
		} finally {
			Job.getJobManager().resume();
		}

		TestUtil.waitForJobs(name.getMethodName(), ProcessConsoleManager.class, 0, 10000);
		ProcessConsoleManager processConsoleManager = DebugUIPlugin.getDefault().getProcessConsoleManager();
		ILaunch[] launches = launchManager.getLaunches();
		Set<IConsole> openConsoles = new HashSet<>();
		if (processConsoleManager.getConsole(process1) != null) {
			openConsoles.add(processConsoleManager.getConsole(process1));
		}
		if (processConsoleManager.getConsole(process2) != null) {
			openConsoles.add(processConsoleManager.getConsole(process2));
		}

		String launchesString = Stream.of(launches).map(launch -> Stream.of(launch.getProcesses()).map(IProcess::getLabel).collect(joining(",", "[", "]"))).collect(joining());
		String consolesString = openConsoles.stream().map(IConsole::getName).collect(joining());
		String failureMessage = String.format("ProcessConsoleManager and LaunchManager got out of sync.\nLaunches: %s\nConsoles: %s", launchesString, consolesString);
		assertThat(openConsoles).as(failureMessage).hasSameSizeAs(launches);

		final ConsoleRemoveAllTerminatedAction removeAction = new ConsoleRemoveAllTerminatedAction();
		if (launchManager.getLaunches().length != 0) {
			assertThat(removeAction).matches(ConsoleRemoveAllTerminatedAction::isEnabled, "is enabled");
		}
		removeAction.run();
		TestUtil.waitForJobs(name.getMethodName(), ProcessConsoleManager.class, 0, 10000);
		assertNull("First console not removed.", processConsoleManager.getConsole(process1));
		assertNull("Second console not removed.", processConsoleManager.getConsole(process1));
	}
}
