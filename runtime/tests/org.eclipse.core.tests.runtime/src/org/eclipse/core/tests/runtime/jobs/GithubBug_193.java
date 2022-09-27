/*******************************************************************************
 * Copyright (c) 2022 Andrey Loskutov <loskutov@gmx.de> and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov <loskutov@gmx.de> - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.runtime.jobs;

import static org.junit.Assert.assertEquals;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import junit.framework.TestCase;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * Test for bug https://github.com/eclipse-platform/eclipse.platform/issues/193
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GithubBug_193 extends TestCase {

	final static Object JOB_FAMILY1 = " family 1";
	final static Object JOB_FAMILY2 = " family 2";
	final static Object JOB_FAMILY3 = " family 3";

	/**
	 * This test relies on proper job event order reported to job listeners to
	 * succeed. Listener expects that for any job, "scheduled" event is always sent
	 * before "done". If that is not the case, test will fail.
	 *
	 * @throws Exception
	 */
	@Test
	public void testDoneEventReceivedBeforeSchedule() throws Exception {
		int jobCount = 1000;
		JobWatcher watcher = JobWatcher.startWatchingFor(JOB_FAMILY1, JOB_FAMILY2, JOB_FAMILY3);
		List<Job> fewJobs = startFewJobs(jobCount);
		watcher.waitUntilJobsAreDone();
		assertEquals(Collections.emptyList(), watcher.getJobsToWaitFor());
		assertEquals(watcher.getScheduled(), watcher.getDone());
		assertEquals(fewJobs.size(), watcher.getDone());

		System.out.println("Scheduled: " + watcher.getScheduled());
		System.out.println("Done: " + watcher.getDone());
	}

	private List<Job> startFewJobs(int jobCount) {
		List<Job> jobs = new ArrayList<>();
		for (int i = 0; i < jobCount; i++) {
			final int jobId = i;
			Job job = new MyJob("job " + jobId, jobId);
			job.schedule();
			jobs.add(job);
		}
		return jobs;
	}

	private static Object getFamily(int jobId) {
		int id = jobId % 13;
		switch (id) {
		case 0:
			return JOB_FAMILY3;
		default:
			return jobId % 2 == 0 ? JOB_FAMILY2 : JOB_FAMILY1;
		}
	}

	private final class MyJob extends Job {
		private final int jobId;
		Object myFamily;

		private MyJob(String name, int jobId) {
			super(name + getFamily(jobId));
			this.jobId = jobId;
			myFamily = getFamily(jobId);
		}


		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				if (myFamily != JOB_FAMILY3) {
					if (myFamily == JOB_FAMILY2 && jobId % 3 == 0) {
						Job.getJobManager().join(JOB_FAMILY3, null);
					} else {
						Thread.sleep(0, jobId % 10);
					}
				}
			} catch (InterruptedException e) {
				//
			}
			return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
		}

		@Override
		public boolean belongsTo(Object family) {
			return myFamily == family || this == family;
		}

		@Override
		public String toString() {
			return getName();
		}
	}

}

class JobWatcher {

	private final Object[] families;
	private final AtomicLong sheduled;
	private final AtomicLong done;

	/**
	 * Start watching for jobs that belong to one of the given families.
	 *
	 * @param families list of families
	 * @return the newly created job watcher
	 */
	public static JobWatcher startWatchingFor(Object... families) {
		JobWatcher watcher = new JobWatcher(families);
		watcher.startWatchingJobs();
		return watcher;
	}

	private JobWatcher(Object... families) {
		this.families = families;
		sheduled = new AtomicLong();
		done = new AtomicLong();
	}

	/*
	 * It can happen that same job is scheduled few times. We want track *all*
	 * start/done events for a job, so it is not a set.
	 */
	private final List<Job> jobsToWaitFor = new LinkedList<>();

	private CountDownLatch testDoneSignal;
	Collection<String> errors = new ConcurrentLinkedQueue<>();

	private final JobChangeAdapter jobListener = new JobChangeAdapter() {

		@Override
		public void scheduled(IJobChangeEvent event) {
			Job scheduledJob = event.getJob();
			if (jobNeedsToBeWatched(scheduledJob)) {
				synchronized (jobsToWaitFor) {
					rememberScheduled(scheduledJob);
					boolean wasEmpty = jobsToWaitFor.isEmpty();
					jobsToWaitFor.add(scheduledJob);
					if (wasEmpty) {
						testDoneSignal = new CountDownLatch(1);
					}
				}
			}
		}

		private void rememberScheduled(Job job) {
			sheduled.addAndGet(1);
			// System.out.println("-> " + job.getName());
		}

		private void rememberDone(Job job) {
			done.addAndGet(1);
			// System.out.println("OK " + job.getName());
		}

		@Override
		public void done(IJobChangeEvent event) {
			synchronized (jobsToWaitFor) {
				Job job = event.getJob();
				rememberDone(job);
				boolean removed = jobsToWaitFor.remove(job);
				if (!removed) {
					if (testDoneSignal != null && jobNeedsToBeWatched(job)) {
						errors.add("received 'done' before 'schedule' for " + job);
						testDoneSignal.countDown(); // fail
					}
				}
				if (!jobsToWaitFor.isEmpty()) {
					return;
				}

				if (testDoneSignal != null) {
					testDoneSignal.countDown();
					testDoneSignal = null;
				}
			}
		}
	};

	private void startWatchingJobs() {
		synchronized (this) {
			Job.getJobManager().addJobChangeListener(jobListener);
		}
	}

	private boolean jobNeedsToBeWatched(Job job) {
		for (Object family : families) {
			if (job.belongsTo(family)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Stops watching for jobs. Threads blocked in {@code waitUntilJobsAreDone} are
	 * notified and will return.
	 */
	public void stopWatching() {
		synchronized (this) {
			Job.getJobManager().removeJobChangeListener(jobListener);
		}

		synchronized (jobsToWaitFor) {
			if (testDoneSignal != null) {
				testDoneSignal.countDown();
			}
		}
	}

	/**
	 * Waits until no jobs belonging to the given set of families are running or
	 * scheduled.
	 */
	public void waitUntilJobsAreDone() {
		CountDownLatch latchToWaitFor;
		synchronized (jobsToWaitFor) {
			if (jobsToWaitFor.isEmpty()) {
				return;
			}
			latchToWaitFor = testDoneSignal;
		}

		if (latchToWaitFor != null) {
			try {
				latchToWaitFor.await(1, TimeUnit.MINUTES);
			} catch (InterruptedException e) {
				// ignore
			}
			errors.forEach(e -> {
				throw new AssertionError(e);
			});
			assertEquals("Jobs delivered in wrong order for " + getJobsToWaitFor(), 0, latchToWaitFor.getCount());
		}
	}

	public List<Job> getJobsToWaitFor() {
		synchronized (jobsToWaitFor) {
			return new ArrayList<>(jobsToWaitFor);
		}
	}

	public long getScheduled() {
		return sheduled.get();
	}

	public long getDone() {
		return done.get();
	}

}

