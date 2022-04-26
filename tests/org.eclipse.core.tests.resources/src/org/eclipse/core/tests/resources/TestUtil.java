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
 *   Andrey Loskutov <loskutov@gmx.de> - more logging
 *******************************************************************************/
package org.eclipse.core.tests.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.core.internal.runtime.InternalPlatform;
import org.eclipse.core.internal.utils.StringPoolJob;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.Assert;

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
		message = "[" + owner + "] " + message;
		Throwable error = null;
		if (optionalError != null && optionalError.length > 0) {
			error = optionalError[0];
		}
		Status status = new Status(severity, "org.eclipse.core.tests.resources", message, error);
		InternalPlatform.getDefault().getLog(Platform.getBundle("org.eclipse.core.tests.resources")).log(status);
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
	 * @return true if the method timed out, false if all the jobs terminated before the timeout
	 */
	public static boolean waitForJobs(String owner, long minTimeMs, long maxTimeMs) {
		return waitForJobs(owner, minTimeMs, maxTimeMs, null);
	}

	static boolean ignoreJob(Job job) {
		if (job instanceof StringPoolJob) {
			// reschedules itself
			return true;
		}
		Class<?> clazz = job.getClass();
		while ((clazz = clazz.getSuperclass()) != null) {
			if (clazz.getSimpleName().equals("UIJob")) {
				// can not be run while waiting in UI thread
				return true;
			}
		}
		if (job.belongsTo(FreezeMonitor.class)) {
			// sleeps for a good reason
			// and shouldn't be touched during test
			return true;
		}
		return false;
	}

	private static void wakeUpSleepingJobs(Object family) {
		List<Job> sleepingJobs = getSleepingJobs(family);
		for (Job job : sleepingJobs) {
			if (!ignoreJob(job)) {
				// System.out.println("wakeup job " + asString(job));
				job.wakeUp();
			}
		}
	}

	/**
	 * Utility for waiting until the execution of jobs of given family has finished
	 * or timeout is reached. If no jobs are running, the method waits given minimum
	 * wait time.
	 *
	 * @param owner
	 *            name of the caller which will be logged as prefix if the wait
	 *            times out
	 * @param minTimeMs
	 *            minimum wait time in milliseconds
	 * @param maxTimeMs
	 *            maximum wait time in milliseconds
	 * @param family
	 *            job family to wait for
	 *
	 * @return true if the method timed out, false if all the jobs terminated before
	 *         the timeout
	 */
	public static boolean waitForJobs(String owner, long minTimeMs, long maxTimeMs, Object family) {
		if (maxTimeMs < minTimeMs) {
			throw new IllegalArgumentException("Max time is smaller as min time!");
		}
		wakeUpSleepingJobs(family);
		final long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start < minTimeMs) {
			try {
				Thread.sleep(Math.min(10, minTimeMs));
			} catch (InterruptedException e) {
				// Uninterruptable
			}
		}
		while (!Job.getJobManager().isIdle()) {
			List<Job> jobs = getRunningOrWaitingJobs(family);
			if (jobs.isEmpty()) {
				// only uninteresting jobs running
				break;
			}

			long millisGone = System.currentTimeMillis() - start;
			if (millisGone >= maxTimeMs) {
				dumpRunningOrWaitingJobs(owner, jobs, millisGone);
				return true;
			}
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// Uninterruptable
			}
			wakeUpSleepingJobs(family);
		}
		return false;
	}

	public static void dumpRunnigOrWaitingJobs(String owner) {
		List<Job> jobs = getRunningOrWaitingJobs(null);
		if (!jobs.isEmpty()) {
			String message = "Some job is still running or waiting to run: " + asString(jobs);
			log(IStatus.INFO, owner, message, new RuntimeException(message));
		}
	}

	private static void dumpRunningOrWaitingJobs(String owner, List<Job> jobs, long millisGone) {
		String message = "Some job is still running or waiting to run after " + millisGone + "ms: "
				+ dumpRunningOrWaitingJobs(jobs);
		log(IStatus.ERROR, owner, message, new RuntimeException(message));
	}

	private static String dumpRunningOrWaitingJobs(List<Job> jobs) {
		if (jobs.isEmpty()) {
			return "";
		}
		return asString(jobs);
	}

	private static String asString(Job job) {
		StringBuilder sb = new StringBuilder();
		sb.append("'").append(job.getName()).append("'/");
		sb.append(job.getClass().getName());
		sb.append(" state=" + job.getState());
		Thread thread = job.getThread();
		if (thread != null) {
			sb.append(" thread=" + thread.getName());
		}
		return sb.toString();
	}

	private static String asString(List<Job> jobs) {
		return jobs.stream().map(TestUtil::asString).collect(Collectors.joining(", "));
	}

	private static List<Job> getRunningOrWaitingJobs(Object jobFamily) {
		List<Job> running = new ArrayList<>();
		Job[] jobs = Job.getJobManager().find(jobFamily);
		for (Job job : jobs) {
			if (isRunningOrWaitingJob(job) && !ignoreJob(job)) {
				running.add(job);
			}
		}
		return running;
	}

	private static List<Job> getSleepingJobs(Object family) {
		List<Job> running = new ArrayList<>();
		Job[] jobs = Job.getJobManager().find(family);
		for (Job job : jobs) {
			if (job.getState() == Job.SLEEPING) {
				running.add(job);
			}
		}
		return running;
	}

	private static boolean isRunningOrWaitingJob(Job job) {
		int state = job.getState();
		return (state == Job.RUNNING || state == Job.WAITING);
	}

}
