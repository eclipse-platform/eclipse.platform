/*******************************************************************************
 * Copyright (c) 2003, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *     Thirumala Reddy Mutchukota - Bug 432049, JobGroup API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.runtime.jobs;

import java.util.*;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Base class for tests using IJobManager
 */
public class AbstractJobManagerTest extends AbstractJobTest {
	protected IJobManager manager;
	private FussyProgressProvider progressProvider;

	public AbstractJobManagerTest() {
		super();
	}

	public AbstractJobManagerTest(String name) {
		super(name);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		manager = Job.getJobManager();
		progressProvider = new FussyProgressProvider();
		manager.setProgressProvider(progressProvider);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		progressProvider.sanityCheck();
		manager.setProgressProvider(null);
	}

	/**
	 * Will return a list (snapshot) of all jobs that are currently running, at
	 * *this* exact moment in time, excluding the calling thread's Job (if it is
	 * within one)
	 *
	 * @param family
	 *            The job family to search for, or null for all jobs.
	 */
	protected List<Job> getRunningJobs(Object family) {
		List<Job> jobs = new ArrayList<>();
		jobs.addAll(Arrays.asList(Job.getJobManager().find(family)));
		for (Iterator<Job> iterator = jobs.iterator(); iterator.hasNext();) {
			Job job = iterator.next();
			if (job.getState() != Job.RUNNING || job.getThread() == Thread.currentThread()) {
				iterator.remove();
			}
		}
		return jobs;
	}

	protected List<Job> getJobs(String[] families) {
		if (families == null) {
			return getRunningJobs(null);
		}
		List<Job> j = new ArrayList<>();
		for (String family : families)
			j.addAll(getRunningJobs(family));
		return j;
	}

	protected List<Job> getFinishedJobs(Job[] jobs) {
		List<Job> joblist = new ArrayList<>(Arrays.asList(jobs));
		for (Iterator<Job> iterator = joblist.iterator(); iterator.hasNext();) {
			Job job = iterator.next();
			if (job.getState() != Job.NONE) {
				iterator.remove();
			}
		}
		return joblist;
	}

	protected void waitForJobsCompletion(Job[] jobs, int waitTime) {
		List<Job> jobList = new ArrayList<>(Arrays.asList(jobs));
		int i = 0;
		int tickLength = 10;
		int ticks = waitTime / tickLength;
		while (!jobList.isEmpty()) {
			sleep(tickLength);
			//sanity test to avoid hanging tests
			if (i++ > ticks) {
				dumpState();
				assertTrue("Timeout waiting for job to complete", false);
			}
			for (Iterator<Job> iterator = jobList.iterator(); iterator.hasNext();) {
				if (iterator.next().getState() == Job.NONE)
					iterator.remove();
			}
		}
	}

	protected void waitForJobsCompletion(String[] families, int waitTime) {
		List<Job> jobs = getJobs(families);
		int i = 0;
		int tickLength = 10;
		int ticks = waitTime / tickLength;

		while (!jobs.isEmpty()) {
			sleep(tickLength);
			//sanity test to avoid hanging tests
			if (i++ > ticks) {
				dumpState();
				assertTrue("Timeout waiting for job to complete", false);
			}
			jobs = getJobs(families);
		}
	}
}
