/*******************************************************************************
 *  Copyright (c) 2007, 2014 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Thirumala Reddy Mutchukota - Bug 432049, JobGroup API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.runtime.jobs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.internal.jobs.JobListeners;
import org.eclipse.core.internal.jobs.JobManager;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.After;
import org.junit.Before;

/**
 * Common superclass for all tests of the org.eclipse.core.runtime.jobs API. Provides
 * convenience methods useful for testing jobs.
 */
@SuppressWarnings("restriction")
public class AbstractJobTest  {
	protected IJobManager manager;
	private FussyProgressProvider progressProvider;

	protected void sleep(long duration) {
		try {
			Thread.sleep(duration);
		} catch (InterruptedException e) {
			//ignore
		}
	}

	/**
	 * Ensures job completes within the given time.
	 * @param waitTime time in milliseconds
	 */
	protected void waitForCompletion(Job job, int waitTime) {
		int i = 0;
		int tickLength = 1;
		int ticks = waitTime / tickLength;
		long start = now();
		while (job.getState() != Job.NONE && now() - start < waitTime) {
			sleep(tickLength);
			// sanity test to avoid hanging tests
			if (i++ > ticks && now() - start > waitTime) {
				dumpState();
				assertTrue("Timeout waiting for job to complete", false);
			}
		}
	}

	/**
	 * Ensures given job completes within a second.
	 */
	protected void waitForCompletion(Job job) {
		waitForCompletion(job, 1000);
	}

	/**
	 * Extra debugging for bug 109898
	 */
	protected void dumpState() {
		System.out.println("**** BEGIN DUMP JOB MANAGER INFORMATION ****");
		Job[] jobs = Job.getJobManager().find(null);
		for (Job job : jobs) {
			System.out.println("" + job + " state: " + JobManager.printState(job));
		}
		System.out.println("**** END DUMP JOB MANAGER INFORMATION ****");
	}

	public static long now() {
		return ((JobManager) (Job.getJobManager())).now();
	}

	@Before
	public void setProgressProvider() throws Exception {
		assertNoTimeoutOccured();
		manager = Job.getJobManager();
		progressProvider = new FussyProgressProvider();
		manager.setProgressProvider(progressProvider);
	}

	@After
	public void resetProgressProvider() throws Exception {
		progressProvider.sanityCheck();
		Job.getJobManager().setProgressProvider(null);
		assertNoTimeoutOccured();
	}

	protected final void assertNoTimeoutOccured() throws Exception {
		int jobListenerTimeout = JobListeners.getJobListenerTimeout();
		JobListeners.resetJobListenerTimeout();
		int defaultTimeout = JobListeners.getJobListenerTimeout();
		assertEquals("See logfile for TimeoutException to get details.", defaultTimeout, jobListenerTimeout);
	}
}
