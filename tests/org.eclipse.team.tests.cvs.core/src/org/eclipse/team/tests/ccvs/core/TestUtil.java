/*******************************************************************************
 * Copyright (c) 2016 Google, Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Stefan Xenos (Google) - Initial implementation
 *******************************************************************************/
package org.eclipse.team.tests.ccvs.core;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.junit.Assert;

public class TestUtil {
	/**
	 * Call this in the tearDown method of every test to clean up state that can
	 * otherwise leak through SWT between tests.
	 */
	public static void cleanUp() {
		// Ensure that the Thread.interrupted() flag didn't leak.
		Assert.assertFalse("The main thread should not be interrupted at the end of a test", Thread.interrupted());
		// Wait for any outstanding jobs to finish. Protect against deadlock by
		// terminating the wait after a timeout.
		Job[] unfinishedJobs = waitForJobs(0, TimeUnit.MINUTES.toMillis(3));
		if (unfinishedJobs != null && unfinishedJobs.length != 0) {
			Assert.fail(
					"The following jobs did not terminate at the end of the test: " + Arrays.toString(unfinishedJobs));
		}
		// Wait for any pending *syncExec calls to finish
		runEventLoop();
		// Ensure that the Thread.interrupted() flag didn't leak.
		Assert.assertFalse("The main thread should not be interrupted at the end of a test", Thread.interrupted());
	}

	public static void runEventLoop() {
		Display display = Display.getCurrent();
		if (display != null && !display.isDisposed()) {
			while (display.readAndDispatch()) {
				// Keep pumping events until the queue is empty
			}
		}
	}

	/**
	 * Utility for waiting until the execution of jobs of any family has
	 * finished or timeout is reached. If no jobs are running, the method waits
	 * given minimum wait time. While this method is waiting for jobs, UI events
	 * are processed.
	 *
	 * @param minTimeMs
	 *            minimum wait time in milliseconds
	 * @param maxTimeMs
	 *            maximum wait time in milliseconds
	 * @return {@code null} if all jobs have completed, or an array of jobs that were still running after
	 * 			  {@code maxTimeMs}
	 */
	public static Job[] waitForJobs(long minTimeMs, long maxTimeMs) {
		if (maxTimeMs < minTimeMs) {
			throw new IllegalArgumentException("Max time is smaller as min time!");
		}
		final long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start < minTimeMs) {
			runEventLoop();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// Uninterruptable
			}
		}
		while (!Job.getJobManager().isIdle()) {
			if (System.currentTimeMillis() - start >= maxTimeMs) {
				return Job.getJobManager().find(null);
			}
			runEventLoop();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// Uninterruptable
			}
		}
		return null;
	}
}
