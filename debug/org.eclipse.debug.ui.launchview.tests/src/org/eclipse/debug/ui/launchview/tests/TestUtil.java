/*******************************************************************************
 * Copyright (c) 2017 Google, Inc and others.
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
 *   Andrey Loskutov (loskutov@gmx.de) - many different extensions
 *******************************************************************************/
package org.eclipse.debug.ui.launchview.tests;

import static org.junit.Assert.fail;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.junit.Assert;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;


public class TestUtil {

	/**
	 * Call this in the tearDown method of every test to clean up state that can
	 * otherwise leak through SWT between tests.
	 */
	public static void cleanUp(String owner) {
		// Ensure that the Thread.interrupted() flag didn't leak.
		Assert.assertFalse("The main thread should not be interrupted at the end of a test", Thread.interrupted());

		// Wait for any outstanding jobs to finish. Protect against deadlock by
		// terminating the wait after a timeout.
		boolean timedOut = waitForJobs(owner, 5, 5000);
		if (timedOut) {
			// We don't expect any extra jobs run during the test: try to cancel them
			log(IStatus.INFO, owner, "Trying to cancel running jobs: " + getRunningOrWaitingJobs(null));
			getRunningOrWaitingJobs(null).forEach(Job::cancel);
			waitForJobs(owner, 5, 1000);
		}

		// Ensure that the Thread.interrupted() flag didn't leak.
		Assert.assertFalse("The main thread should not be interrupted at the end of a test", Thread.interrupted());
	}

	public static void log(int severity, String owner, String message, Throwable... optionalError) {
		Throwable error = null;
		if (optionalError != null && optionalError.length > 0) {
			error = optionalError[0];
		}
		Bundle testBundle = FrameworkUtil.getBundle(TestUtil.class);
		Status status = new Status(severity, testBundle.getSymbolicName(), "[" + owner + "] " + message, error);
		ILog.of(testBundle).log(status);
	}

	/**
	 * Process all queued UI events. If called from background thread, does
	 * nothing.
	 */
	public static void processUIEvents() {
		Display display = Display.getCurrent();
		if (display != null && !display.isDisposed()) {
			while (display.readAndDispatch()) {
				// Keep pumping events until the queue is empty
			}
		}
	}

	/**
	 * Process all queued UI events. If called from background thread, just
	 * waits
	 *
	 * @param millis max wait time to process events
	 */
	public static void processUIEvents(final long millis) throws Exception {
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start < millis) {
			Display display = Display.getCurrent();
			if (display != null && !display.isDisposed()) {
				while (display.readAndDispatch()) {
					// loop until the queue is empty
				}
			} else {
				Thread.sleep(10);
			}
		}
	}

	/**
	 * Waits while given condition is {@code true} for a given amount of
	 * milliseconds. If the actual wait time exceeds given timeout and condition
	 * will be still {@code true}, throws {@link AssertionFailedError} with
	 * given message.
	 * <p>
	 * Will process UI events while waiting in UI thread, if called from
	 * background thread, just waits.
	 *
	 * @param <T> type of the context
	 * @param condition function which will be evaluated while waiting
	 * @param context test context
	 * @param timeout max wait time in milliseconds to wait on given condition
	 * @param errorMessage message which will be used to construct the failure
	 *            exception in case the condition will still return {@code true}
	 *            after given timeout
	 */
	public static <T> void waitWhile(Function<T, Boolean> condition, T context, long timeout, Function<T, String> errorMessage) throws Exception {
		long start = System.currentTimeMillis();
		Display display = Display.getCurrent();
		while (System.currentTimeMillis() - start < timeout && condition.apply(context)) {
			if (display != null && !display.isDisposed()) {
				if (!display.readAndDispatch()) {
					Thread.sleep(0);
				}
			} else {
				Thread.sleep(5);
			}
		}
		Boolean stillTrue = condition.apply(context);
		if (stillTrue) {
			fail(errorMessage.apply(context));
		}
	}

	/**
	 * A simplified variant of
	 * {@link #waitWhile(Function, Object, long, Function)}.
	 * <p>
	 * Waits while given condition is {@code true} for a given amount of
	 * milliseconds.
	 * <p>
	 * Will process UI events while waiting in UI thread, if called from
	 * background thread, just waits.
	 *
	 * @param condition function which will be evaluated while waiting
	 * @param timeout max wait time in milliseconds to wait on given condition
	 * @return value of condition when method returned
	 */
	public static boolean waitWhile(Supplier<Boolean> condition, long timeout) throws Exception {
		if (condition == null) {
			condition = () -> true;
		}
		long start = System.currentTimeMillis();
		Display display = Display.getCurrent();
		while (System.currentTimeMillis() - start < timeout && condition.get()) {
			Thread.yield();
			if (display != null && !display.isDisposed()) {
				if (!display.readAndDispatch()) {
					Thread.sleep(1);
				}
			} else {
				Thread.sleep(5);
			}
		}
		return condition.get();
	}

	/**
	 * Utility for waiting until the execution of jobs of any family has
	 * finished or timeout is reached. If no jobs are running, the method waits
	 * given minimum wait time. While this method is waiting for jobs, UI events
	 * are processed.
	 *
	 * @param owner name of the caller which will be logged as prefix if the
	 *            wait times out
	 * @param minTimeMs minimum wait time in milliseconds
	 * @param maxTimeMs maximum wait time in milliseconds
	 * @return true if the method timed out, false if all the jobs terminated
	 *         before the timeout
	 */
	public static boolean waitForJobs(String owner, long minTimeMs, long maxTimeMs) {
		return waitForJobs(owner, minTimeMs, maxTimeMs, (Object[]) null);
	}

	/**
	 * Utility for waiting until the execution of jobs of any family has finished or timeout is reached. If no jobs are running, the method waits
	 * given minimum wait time. While this method is waiting for jobs, UI events are processed.
	 *
	 * @param owner
	 *            name of the caller which will be logged as prefix if the wait times out
	 * @param minTimeMs
	 *            minimum wait time in milliseconds
	 * @param maxTimeMs
	 *            maximum wait time in milliseconds
	 * @param excludedFamilies
	 *            optional list of job families to NOT wait for
	 *
	 * @return true if the method timed out, false if all the jobs terminated before the timeout
	 */
	public static boolean waitForJobs(String owner, long minTimeMs, long maxTimeMs, Object... excludedFamilies) {
		if (maxTimeMs < minTimeMs) {
			throw new IllegalArgumentException("Max time is smaller as min time!");
		}
		final long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start < minTimeMs) {
			processUIEvents();
			try {
				Thread.sleep(Math.min(10, minTimeMs));
			} catch (InterruptedException e) {
				// Uninterruptable
			}
		}
		while (!Job.getJobManager().isIdle()) {
			List<Job> jobs = getRunningOrWaitingJobs(null, excludedFamilies);
			if (jobs.isEmpty()) {
				// only uninteresting jobs running
				break;
			}

			if (!Collections.disjoint(runningJobs, jobs)) {
				// There is a job which runs already quite some time, don't wait for it to avoid test timeouts
				dumpRunningOrWaitingJobs(owner, jobs);
				return true;
			}

			if (System.currentTimeMillis() - start >= maxTimeMs) {
				dumpRunningOrWaitingJobs(owner, jobs);
				return true;
			}
			processUIEvents();
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// Uninterruptable
			}
		}
		runningJobs.clear();
		return false;
	}

	static Set<Job> runningJobs = new LinkedHashSet<>();

	private static void dumpRunningOrWaitingJobs(String owner, List<Job> jobs) {
		String message = "Some job is still running or waiting to run: " + dumpRunningOrWaitingJobs(jobs);
		log(IStatus.ERROR, owner, message);
	}

	private static String dumpRunningOrWaitingJobs(List<Job> jobs) {
		if (jobs.isEmpty()) {
			return "";
		}
		// clear "old" running jobs, we only remember most recent
		runningJobs.clear();
		StringBuilder sb = new StringBuilder();
		for (Job job : jobs) {
			runningJobs.add(job);
			sb.append("\n'").append(job.toString()).append("'/");
			sb.append(job.getClass().getName());
			Thread thread = job.getThread();
			if (thread != null) {
				ThreadInfo[] threadInfos = ManagementFactory.getThreadMXBean().getThreadInfo(new long[] { thread.getId() }, true, true);
				if (threadInfos[0] != null) {
					sb.append("\nthread info: ").append(threadInfos[0]);
				}
			}
			sb.append(", ");
		}
		sb.setLength(sb.length() - 2);
		return sb.toString();
	}

	public static List<Job> getRunningOrWaitingJobs(Object jobFamily, Object... excludedFamilies) {
		List<Job> running = new ArrayList<>();
		Job[] jobs = Job.getJobManager().find(jobFamily);
		for (Job job : jobs) {
			if (isRunningOrWaitingJob(job) && !belongsToFamilies(job, excludedFamilies)) {
				running.add(job);
			}
		}
		return running;
	}

	private static boolean isRunningOrWaitingJob(Job job) {
		int state = job.getState();
		return (state == Job.RUNNING || state == Job.WAITING);
	}

	private static boolean belongsToFamilies(Job job, Object... excludedFamilies) {
		if (excludedFamilies == null || excludedFamilies.length == 0) {
			return false;
		}
		for (Object family : excludedFamilies) {
			if (job.belongsTo(family)) {
				return true;
			}
		}
		return false;
	}

}
