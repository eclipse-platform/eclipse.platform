/*******************************************************************************
 * Copyright (c) 2014, 2022 Google Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Thirumala Reddy Mutchukota - initial API and implementation
 *******************************************************************************/

package org.eclipse.core.tests.runtime.jobs;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import junit.framework.AssertionFailedError;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.core.tests.harness.*;

/**
 * Tests for {@link JobGroup}.
 */
@SuppressWarnings("restriction")
public class JobGroupTest extends AbstractJobTest {
	private IJobManager manager;
	private FussyProgressProvider progressProvider;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		manager = Job.getJobManager();
		progressProvider = new FussyProgressProvider();
		manager.setProgressProvider(progressProvider);
	}

	@Override
	public void tearDown() throws Exception {
		super.tearDown();
		progressProvider.sanityCheck();
		manager.setProgressProvider(null);
	}

	public void testThrottlingWhenAllJobsAreKnown() {
		final int NUM_JOBS = 100;
		final int MAX_THREADS = 10;
		TestJob[] jobs = new TestJob[NUM_JOBS];
		final JobGroup jobGroup = new JobGroup("JobGroup", MAX_THREADS, NUM_JOBS);
		final int[] maxThreadsUsed = new int[1];
		final TestBarrier2 barrier = new TestBarrier2();

		// Create and schedule the long running test jobs.
		for (int i = 0; i < NUM_JOBS; i++) {
			jobs[i] = new TestJob("TestJob", 1000000, 1);
			jobs[i].setJobGroup(jobGroup);
			jobs[i].schedule();
		}

		maxThreadsUsed[0] = 0;
		// Use a thread to record the maximum number of running jobs and
		// cancel the running jobs so that the waiting jobs will be scheduled.
		final Thread t = new Thread(() -> {
			barrier.setStatus(TestBarrier2.STATUS_RUNNING);
			while (jobGroup.getState() != JobGroup.NONE) {
				List<TestJob> runningJobs = new ArrayList<>();
				for (Job activeJob : jobGroup.getActiveJobs()) {
					if (activeJob.getState() == Job.RUNNING) {
						runningJobs.add((TestJob) activeJob);
					}
				}
				int runningJobsSize = runningJobs.size();
				if (runningJobsSize > maxThreadsUsed[0]) {
					maxThreadsUsed[0] = runningJobsSize;
				}
				for (Job runningJob : runningJobs) {
					runningJob.cancel();
					waitForCompletion(runningJob);
				}
			}
			barrier.setStatus(TestBarrier2.STATUS_DONE);
		});

		assertEquals("1.0", JobGroup.ACTIVE, jobGroup.getState());
		// Start the thread and wait for it to complete.
		t.start();
		barrier.waitForStatus(TestBarrier2.STATUS_RUNNING);
		barrier.waitForStatus(TestBarrier2.STATUS_DONE);

		assertEquals("2.0", JobGroup.NONE, jobGroup.getState());
		assertTrue("3.0", maxThreadsUsed[0] > 0);
		assertTrue("4.0", maxThreadsUsed[0] <= MAX_THREADS);
	}

	public void testSeedJobsWhenAllJobsAreKnown() {
		final int NUM_SEED_JOBS = 3;
		final JobGroup jobGroup = new JobGroup("JobGroup", 1, NUM_SEED_JOBS);

		for (int i = 1; i <= NUM_SEED_JOBS; i++) {
			// Create and schedule a long running test job.
			TestJob job = new TestJob("TestJob", 1000000, 1);
			job.setJobGroup(jobGroup);
			job.schedule();
			waitForStart(job);
			// Job group should be in the ACTIVE state with one active job.
			assertEquals("1." + i, 1, jobGroup.getActiveJobs().size());
			assertEquals("2." + i, JobGroup.ACTIVE, jobGroup.getState());
			// Cancel the test job and wait for it to finish.
			job.cancel();
			waitForCompletion(job);
			// Verify that the group does not contain any active jobs.
			assertEquals("3." + i, 0, jobGroup.getActiveJobs().size());
			// Verify that the group will be in the ACTIVE state even when there are no active jobs
			// and transitions to NONE state only after all the seed jobs are completed.
			if (i < NUM_SEED_JOBS) {
				assertEquals("4." + i, JobGroup.ACTIVE, jobGroup.getState());
			} else {
				waitForCompletion(jobGroup);
				assertEquals("4." + i, JobGroup.NONE, jobGroup.getState());
			}
		}
	}

	public void testSeedJobsWhenSeedJobsAddNewJobs() {
		final int NUM_SEED_JOBS = 10;
		final int NUM_CHILD_JOBS = 10;
		final JobGroup jobGroup = new JobGroup("JobGroup", 10, NUM_SEED_JOBS);

		for (int i = 1; i <= NUM_SEED_JOBS; i++) {
			// Create and schedule a seed job, which creates and schedules the
			// long running child jobs belonging to the same group.
			// An example usage would be a directory digger that starts
			// with a set of root directories.
			Job job = new Job("SeedJob") {
				@Override
				public IStatus run(IProgressMonitor monitor) {
					for (int j = 0; j < NUM_CHILD_JOBS; j++) {
						TestJob childJob = new TestJob("ChildTestJob", 1000000, 1);
						childJob.setJobGroup(getJobGroup());
						childJob.schedule();
					}
					return Status.OK_STATUS;
				}
			};
			job.setJobGroup(jobGroup);
			job.schedule();
			waitForCompletion(job);
			// Job group should be in the ACTIVE state with the active child jobs.
			assertEquals("1." + i, NUM_CHILD_JOBS, jobGroup.getActiveJobs().size());
			assertEquals("2." + i, JobGroup.ACTIVE, jobGroup.getState());
			// Cancel all the active child jobs and wait for them to finish.
			for (Job activeJob : jobGroup.getActiveJobs()) {
				activeJob.cancel();
				waitForCompletion(activeJob);
			}
			// Verify that the group does not contain any active jobs.
			assertEquals("3." + i, 0, jobGroup.getActiveJobs().size());
			// Verify that the group will be in the ACTIVE state even when there are no active jobs
			// and transitions to NONE state only after all the seed jobs are completed.
			if (i < NUM_SEED_JOBS) {
				assertEquals("4." + i, JobGroup.ACTIVE, jobGroup.getState());
			} else {
				waitForCompletion(jobGroup);
				assertEquals("4." + i, JobGroup.NONE, jobGroup.getState());
			}
		}
	}

	public void testSeedJobsWithRepeatingJobs() {
		final int NUM_SEED_JOBS = 10;
		final int REPEATING_COUNT = 5;
		final JobGroup jobGroup = new JobGroup("JobGroup", 1, NUM_SEED_JOBS);

		RepeatingJob[] jobs = new RepeatingJob[NUM_SEED_JOBS];
		for (int i = 0; i < NUM_SEED_JOBS; i++) {
			RepeatingJob job = new RepeatingJob("RepeatingJob", REPEATING_COUNT);
			jobs[i] = job;
			job.setJobGroup(jobGroup);
			job.schedule();
		}
		waitForCompletion(jobGroup);
		for (int i = 0; i < NUM_SEED_JOBS; i++) {
			// Verify that all the repeating jobs have run the expected number of times,
			// which only happen when the job group treats the multiple executions of
			// a repeating job as a single seed job.
			assertEquals("1." + i, REPEATING_COUNT, jobs[i].getRunCount());
		}
	}

	public void testCancel() {
		final int NUM_JOBS = 20;
		TestJob[] jobs = new TestJob[NUM_JOBS];
		// Create two different job groups. The cancel operation is performed and tested on the
		// firstJobGroup. The secondJobGroup is used to make sure that the presence of a job group
		// will not affect the working of another job group.
		final JobGroup firstJobGroup = new JobGroup("FirstJobGroup", 1, NUM_JOBS / 2);
		final JobGroup secondJobGroup = new JobGroup("SecondJobGroup", 1, NUM_JOBS / 2);

		for (int i = 0; i < NUM_JOBS; i++) {
			// Assign half the jobs to the first group, the other half to the second group.
			if (i % 2 == 0) {
				jobs[i] = new TestJob("TestFirstJobGroup", 1000000, 10);
				jobs[i].setJobGroup(firstJobGroup);
			} else {
				jobs[i] = new TestJob("TestSecondJobGroup", 1000000, 10);
				jobs[i].setJobGroup(secondJobGroup);
			}
			jobs[i].schedule();
		}

		waitForStart(jobs[0]);
		assertState("1.0", jobs[0], Job.RUNNING);

		waitForStart(jobs[1]);
		assertState("2.0", jobs[1], Job.RUNNING);

		// Verify that the first two jobs are running and the rest are waiting.
		for (int i = 2; i < NUM_JOBS; i++) {
			assertState("1." + i, jobs[i], Job.WAITING);
		}

		// Cancel the first group of jobs.
		firstJobGroup.cancel();
		waitForCompletion(firstJobGroup);

		// Verify that the the previously running job is canceled and moved to NONE state.
		assertState("2.0", jobs[0], Job.NONE);

		for (int i = 2; i < NUM_JOBS; i++) {
			// Verify that all other jobs in the first group are also canceled and moved to NONE state.
			if (jobs[i].getJobGroup() == firstJobGroup) {
				assertState("2." + i, jobs[i], Job.NONE);
				jobs[i].wakeUp();
				assertState("2." + i, jobs[i], Job.NONE);
				jobs[i].sleep();
				assertState("2." + i, jobs[i], Job.NONE);
			} else { // Verify that all other jobs in the second groups are in the waiting state.
				assertState("3." + i, jobs[i], Job.WAITING);
			}
		}

		// Cancel the second group of jobs.
		secondJobGroup.cancel();
		waitForCompletion(secondJobGroup);

		// Verify that the running job from the second group is canceled to moved to NONE state.
		assertState("7.0", jobs[1], Job.NONE);

		for (int i = 0; i < NUM_JOBS; i++) {
			// Verify that all the jobs are canceled and moved to NONE state.
			assertState("8." + i, jobs[i], Job.NONE);
		}
	}

	public void testGetActiveJobs() {
		final int NUM_JOBS = 20;
		final int JOBS_PER_GROUP = NUM_JOBS / 5;
		TestJob[] jobs = new TestJob[NUM_JOBS];
		// Create five different job groups.
		final JobGroup firstJobGroup = new JobGroup("FirstJobGroup", 1, JOBS_PER_GROUP);
		final JobGroup secondJobGroup = new JobGroup("SecondJobGroup", 1, JOBS_PER_GROUP);
		final JobGroup thirdJobGroup = new JobGroup("ThirdJobGroup", 1, JOBS_PER_GROUP);
		final JobGroup fourthJobGroup = new JobGroup("FourthJobGrroup", 1, JOBS_PER_GROUP);
		final JobGroup fifthJobGroup = new JobGroup("FifthJobGroup", 1, JOBS_PER_GROUP);

		for (int i = 0; i < NUM_JOBS; i++) {
			switch (i % 5) {
			case 0:
				jobs[i] = new TestJob("TestFirstJobGroup", 1000000, 10);
				jobs[i].setJobGroup(firstJobGroup);
				break;
			case 1:
				jobs[i] = new TestJob("TestSecondJobGroup", 1000000, 10);
				jobs[i].setJobGroup(secondJobGroup);
				break;
			case 2:
				jobs[i] = new TestJob("TestThirdJobGroup", 1000000, 10);
				jobs[i].setJobGroup(thirdJobGroup);
				break;
			case 3:
				jobs[i] = new TestJob("TestFourthJobGroup", 1000000, 10);
				jobs[i].setJobGroup(fourthJobGroup);
				break;
			default:
				jobs[i] = new TestJob("TestFifthJobGroup", 1000000, 10);
				jobs[i].setJobGroup(fifthJobGroup);
				break;
			}
			jobs[i].schedule();
		}

		for (int i = 0; i < 5; i++) {
			waitForStart(jobs[i]);
		}

		// Try finding all jobs by supplying the NULL parameter.
		// Note: Running the test framework may cause other system jobs to run,
		// so check that the jobs started by this test are a subset of all running jobs.
		HashSet<Job> testJobs = new HashSet<>();
		testJobs.addAll(Arrays.asList(jobs));
		Job[] allJobs = manager.find(null);
		assertTrue("1.0", allJobs.length >= NUM_JOBS);
		for (int i = 0; i < allJobs.length; i++) {
			// Only test jobs that we know about.
			if (testJobs.remove(allJobs[i])) {
				JobGroup group = allJobs[i].getJobGroup();
				assertTrue("1." + i, (group == firstJobGroup || group == secondJobGroup || group == thirdJobGroup || group == fourthJobGroup || group == fifthJobGroup));
			}
		}
		assertTrue("1.2", testJobs.isEmpty());

		List<Job> activeJobs;

		// Try finding all jobs from the first job group.
		activeJobs = firstJobGroup.getActiveJobs();
		assertEquals("2.0", 4, activeJobs.size());
		for (int i = 0; i < activeJobs.size(); i++) {
			assertEquals("2." + (i + 1), firstJobGroup, activeJobs.get(i).getJobGroup());
		}

		// Try finding all jobs from the second job group.
		activeJobs = secondJobGroup.getActiveJobs();
		assertEquals("3.0", 4, activeJobs.size());
		for (int i = 0; i < activeJobs.size(); i++) {
			assertEquals("3." + (i + 1), secondJobGroup, activeJobs.get(i).getJobGroup());
		}

		// Try finding all jobs from the third job group.
		activeJobs = thirdJobGroup.getActiveJobs();
		assertEquals("4.0", 4, activeJobs.size());
		for (int i = 0; i < activeJobs.size(); i++) {
			assertEquals("4." + (i + 1), thirdJobGroup, activeJobs.get(i).getJobGroup());
		}

		// Try finding all jobs from the fourth job group.
		activeJobs = fourthJobGroup.getActiveJobs();
		assertEquals("5.0", 4, activeJobs.size());
		for (int i = 0; i < activeJobs.size(); i++) {
			assertEquals("5." + (i + 1), fourthJobGroup, activeJobs.get(i).getJobGroup());
		}

		// Try finding all jobs from the fifth job group.
		activeJobs = fifthJobGroup.getActiveJobs();
		assertEquals("6.0", 4, activeJobs.size());
		for (int i = 0; i < activeJobs.size(); i++) {
			assertEquals("6." + (i + 1), fifthJobGroup, activeJobs.get(i).getJobGroup());
		}

		// The first job should still be running.
		for (int i = 0; i < 5; i++) {
			assertState("7.0", jobs[i], Job.RUNNING);
		}

		// Cancel the first job group.
		firstJobGroup.cancel();
		waitForCompletion(firstJobGroup);

		// First job group should not contain any active jobs.
		activeJobs = firstJobGroup.getActiveJobs();
		assertTrue("7.2", activeJobs.isEmpty());

		// Cancel the second job group.
		secondJobGroup.cancel();
		waitForCompletion(secondJobGroup);
		// Second job group should not contain any active jobs.
		activeJobs = secondJobGroup.getActiveJobs();
		assertTrue("9.0", activeJobs.isEmpty());

		// Cancel the fourth job group.
		fourthJobGroup.cancel();
		waitForCompletion(fourthJobGroup);
		// Fourth job group should not contain any active jobs.
		activeJobs = fourthJobGroup.getActiveJobs();
		assertTrue("9.1", activeJobs.isEmpty());

		// Finding all jobs by supplying the NULL parameter should return at least 8 jobs
		// (4 from the 3rd family, and 4 from the 5th family)
		// Note: Running the test framework may cause other system jobs to run,
		// so check that the expected jobs started by this test are a subset of all running jobs.
		testJobs.addAll(Arrays.asList(jobs));
		allJobs = manager.find(null);
		assertTrue("11.0", allJobs.length >= 8);
		for (int i = 0; i < allJobs.length; i++) {
			// Only test jobs that we know about.
			if (testJobs.remove(allJobs[i])) {
				JobGroup group = allJobs[i].getJobGroup();
				assertTrue("11." + (i + 1), (group == thirdJobGroup || group == fifthJobGroup));
			}
		}

		assertEquals("11.2", 12, testJobs.size());
		testJobs.clear();

		// Cancel the fifth and third job groups.
		fifthJobGroup.cancel();
		waitForCompletion(fifthJobGroup);
		thirdJobGroup.cancel();
		waitForCompletion(thirdJobGroup);

		// Verify that all jobs are canceled and moved to NONE state
		for (int i = 0; i < NUM_JOBS; i++) {
			assertState("12." + i, jobs[i], Job.NONE);
		}

		// Finding all jobs should return no jobs from our job groups.
		// Note: Running the test framework may cause other system jobs to run,
		// so check that there no jobs started by this test are present in all running jobs.
		testJobs.addAll(Arrays.asList(jobs));
		allJobs = manager.find(null);
		for (int i = 0; i < allJobs.length; i++) {
			// Verify that no jobs that we know about are found (they should have all been removed)
			assertFalse(allJobs[i].toString(), testJobs.remove(allJobs[i]));
		}
		assertEquals("15.0", NUM_JOBS, testJobs.size());
		testJobs.clear();
	}

	public void testJoinWithoutTimeout() {
		final AtomicIntegerArray status = new AtomicIntegerArray(new int[1]);
		status.set(0, TestBarrier2.STATUS_WAIT_FOR_START);
		final int NUM_JOBS = 20;
		Job[] jobs = new Job[NUM_JOBS];
		// Create two different job groups. The join operation is performed and tested on the
		// firstJobGroup. The secondJobGroup is used to make sure that the presence of a job group
		// will not affect the working of another job group.
		final JobGroup firstJobGroup = new JobGroup("FirstJobGroup", 1, NUM_JOBS / 2);
		final JobGroup secondJobGroup = new JobGroup("SecondJobGroup", 1, NUM_JOBS / 2);
		for (int i = 0; i < NUM_JOBS; i++) {
			// Assign half the jobs to the first group, the other half to the second group.
			if (i % 2 == 0) {
				jobs[i] = new TestJob("TestFirstJobGroup", 1, 1);
				jobs[i].setJobGroup(firstJobGroup);
				jobs[i].schedule(1000000);
			} else {
				jobs[i] = new TestJob("TestSecondJobGroup", 1000000, 10);
				jobs[i].setJobGroup(secondJobGroup);
				jobs[i].schedule();
			}
		}

		Thread t = new Thread(() -> {
			status.set(0, TestBarrier2.STATUS_START);
			try {
				TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_WAIT_FOR_RUN);
				status.set(0, TestBarrier2.STATUS_RUNNING);
				firstJobGroup.join(0, null);
			} catch (OperationCanceledException | InterruptedException e) {
				// ignore
			}
			status.set(0, TestBarrier2.STATUS_DONE);
		});

		// Start the thread that will join the first group of jobs and be blocked until they finish execution.
		t.start();
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_START);
		status.set(0, TestBarrier2.STATUS_WAIT_FOR_RUN);
		// Wake up the first family of jobs
		for (Job job : firstJobGroup.getActiveJobs()) {
			job.wakeUp();
		}

		int i = 0;
		for (; i < 10000; i++) {
			int currentStatus = status.get(0);
			List<Job> result = firstJobGroup.getActiveJobs();

			// Verify that when the thread is complete then all jobs must be done.
			if (currentStatus == TestBarrier2.STATUS_DONE) {
				assertTrue("1." + i, result.isEmpty());
				break;
			}
			sleep(1);
		}
		assertTrue("2.0", i < 10000);

		// Cancel the second job group.
		secondJobGroup.cancel();
		waitForCompletion(secondJobGroup);

		// Verify that all the jobs are now in the NONE state.
		for (int j = 0; j < NUM_JOBS; j++) {
			assertState("3." + j, jobs[j], Job.NONE);
		}
	}

	public void testJoinWithTimeout() {
		final AtomicIntegerArray status = new AtomicIntegerArray(new int[1]);
		status.set(0, TestBarrier2.STATUS_WAIT_FOR_START);
		final int NUM_JOBS = 20;
		Job[] jobs = new Job[NUM_JOBS];
		// Create two different job groups. The join operation is performed and tested on the
		// firstJobGroup. The secondJobGroup is used to make sure that the presence of a job group
		// will not affect the working of another job group.
		final JobGroup firstJobGroup = new JobGroup("FirstJobGroup", 5, NUM_JOBS / 2);
		final JobGroup secondJobGroup = new JobGroup("SecondJobGroup", 5, NUM_JOBS / 2);
		for (int i = 0; i < NUM_JOBS; i++) {
			// Assign half the jobs to the first group, the other half to the second group.
			if (i % 2 == 0) {
				jobs[i] = new TestJob("TestFirstGroup", 1000000, 1);
				jobs[i].setJobGroup(firstJobGroup);
				jobs[i].schedule();
			} else {
				jobs[i] = new TestJob("TestSecondGroup", 1000000, 1);
				jobs[i].setJobGroup(secondJobGroup);
				jobs[i].schedule();
			}

		}

		final long timeout = 100;
		final long duration[] = {-1};

		Thread t = new Thread(() -> {
			status.set(0, TestBarrier2.STATUS_START);
			try {
				long start = now();
				firstJobGroup.join(timeout, null);
				duration[0] = now() - start;
			} catch (OperationCanceledException | InterruptedException e) {
				// ignore
			}
			status.set(0, TestBarrier2.STATUS_DONE);
		});

		// Start the thread that will join the first job group and be blocked until the join call is returned.
		t.start();
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_START);
		int i = 0;
		for (; i < 11; i++) {
			if (status.get(0) == TestBarrier2.STATUS_DONE) {
				// Verify that the join call is blocked for at least for the duration of given timeout.
				assertTrue("1.0 duration: " + Arrays.toString(duration) + " timeout: " + timeout, duration[0] >= timeout);
				break;
			}
			sleep(100);
		}
		// Verify that the join call is returned is finished with in reasonable time of 1100 ms (given timeout + 100ms).
		assertTrue("2.0", i < 11);

		// Cancel both job groups.
		firstJobGroup.cancel();
		waitForCompletion(firstJobGroup);
		secondJobGroup.cancel();
		waitForCompletion(secondJobGroup);

		// Verify that all the jobs are now in the NONE state.
		for (int j = 0; j < NUM_JOBS; j++) {
			assertState("3." + j, jobs[j], Job.NONE);
		}
	}

	/**
	 * Tests joining on a job group, and then canceling the jobs that are blocking the join call.
	 */
	public void testJoinWithCancelingJobs() {
		final AtomicIntegerArray status = new AtomicIntegerArray(new int[1]);
		status.set(0, TestBarrier2.STATUS_WAIT_FOR_START);
		final int NUM_JOBS = 20;
		TestJob[] jobs = new TestJob[NUM_JOBS];
		// Create two different job groups. The join operation is performed and tested on the
		// firstJobGroup. The secondJobGroup is used to make sure that the presence of a job group
		// will not affect the working of another job group.
		final JobGroup firstJobGroup = new JobGroup("FirstJobGroup", 1, NUM_JOBS / 2);
		final JobGroup secondJobGroup = new JobGroup("SecondJobGroup", 1, NUM_JOBS / 2);
		for (int i = 0; i < NUM_JOBS; i++) {
			// Assign half the jobs to the first group, the other half to the second group.
			if (i % 2 == 0) {
				jobs[i] = new TestJob("TestFirstJobGroup", 1000000, 10);
				jobs[i].setJobGroup(firstJobGroup);
			} else {
				jobs[i] = new TestJob("TestSecondJobGroup", 1000000, 10);
				jobs[i].setJobGroup(secondJobGroup);
			}
			jobs[i].schedule();
		}

		Thread t = new Thread(() -> {
			status.set(0, TestBarrier2.STATUS_START);
			try {
				TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_WAIT_FOR_RUN);
				status.set(0, TestBarrier2.STATUS_RUNNING);
				firstJobGroup.join(0, null);
			} catch (OperationCanceledException | InterruptedException e) {
				// ignore
			}
			status.set(0, TestBarrier2.STATUS_DONE);
		});

		// Start the thread that will join the first job group. It will be blocked
		// until all jobs in the first group finish execution or are canceled.
		t.start();
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_START);
		status.set(0, TestBarrier2.STATUS_WAIT_FOR_RUN);
		waitForStart(jobs[0]);
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_RUNNING);

		assertState("2.0", jobs[0], Job.RUNNING);
		assertEquals("2.1", TestBarrier2.STATUS_RUNNING, status.get(0));

		// Cancel the first job group. The join call should be unblocked when
		// all the jobs are canceled.
		firstJobGroup.cancel();
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_DONE);

		// Verify that there are no active jobs in the the first group.
		assertTrue("2.2", firstJobGroup.getActiveJobs().isEmpty());

		// Cancel the second job group.
		secondJobGroup.cancel();
		waitForCompletion(secondJobGroup);

		// Verify that all the jobs are now in the NONE state.
		for (int j = 0; j < NUM_JOBS; j++) {
			assertState("3." + j, jobs[j], Job.NONE);
		}
	}

	/**
	 * Tests joining on a job group, and then canceling the monitor.
	 */
	public void testJoinWithCancelingMonitor() {
		final AtomicIntegerArray status = new AtomicIntegerArray(new int[1]);
		status.set(0, TestBarrier2.STATUS_WAIT_FOR_START);
		final int NUM_JOBS = 20;
		TestJob[] jobs = new TestJob[NUM_JOBS];
		// Create a progress monitor to cancel the join call.
		final IProgressMonitor canceler = new FussyProgressMonitor();
		// Create two different job groups. The join operation is performed and tested on the
		// firstJobGroup. The secondJobGroup is used to make sure that the presence of a job group
		// will not affect the working of another job group.
		final JobGroup firstJobGroup = new JobGroup("FirstJobGroup", 1, NUM_JOBS / 2);
		final JobGroup secondJobGroup = new JobGroup("SecondJobGroup", 1, NUM_JOBS / 2);
		for (int i = 0; i < NUM_JOBS; i++) {
			// Assign half the jobs to the first group, the other half to the second group.
			if (i % 2 == 0) {
				jobs[i] = new TestJob("TestFirstJobGroup", 1000000, 10);
				jobs[i].setJobGroup(firstJobGroup);
			} else {
				jobs[i] = new TestJob("TestSecondJobGroup", 1000000, 10);
				jobs[i].setJobGroup(secondJobGroup);
			}
			jobs[i].schedule();
		}

		Thread t = new Thread(() -> {
			status.set(0, TestBarrier2.STATUS_START);
			try {
				TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_WAIT_FOR_RUN);
				status.set(0, TestBarrier2.STATUS_RUNNING);
				firstJobGroup.join(0, canceler);
			} catch (OperationCanceledException | InterruptedException e) {
				// ignore
			}
			status.set(0, TestBarrier2.STATUS_DONE);
		});

		// Start the thread that will join the first job group. It will be blocked
		// until the monitor is canceled.
		t.start();
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_START);
		status.set(0, TestBarrier2.STATUS_WAIT_FOR_RUN);
		waitForStart(jobs[0]);
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_RUNNING);

		assertState("2.0", jobs[0], Job.RUNNING);
		assertEquals("2.1", TestBarrier2.STATUS_RUNNING, status.get(0));

		// Cancel the monitor that is attached to the join call.
		canceler.setCanceled(true);
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_DONE);

		// The first job in the first group should still be running.
		assertState("2.2", jobs[0], Job.RUNNING);
		assertEquals("2.3", TestBarrier2.STATUS_DONE, status.get(0));
		assertFalse("2.4", firstJobGroup.getActiveJobs().isEmpty());

		// Cancel both job groups.
		secondJobGroup.cancel();
		waitForCompletion(secondJobGroup);
		firstJobGroup.cancel();
		waitForCompletion(firstJobGroup);

		// Verify that all the jobs are now in the NONE state.
		for (int j = 0; j < NUM_JOBS; j++) {
			assertState("3." + j, jobs[j], Job.NONE);
		}
	}

	/**
	 * Tests joining a job that repeats in a loop.
	 */
	public void testJoinWithRepeatingJobs() {
		JobGroup jobGroup = new JobGroup("JobGroup", 1, 1);
		int count = 25;
		RepeatingJob job = new RepeatingJob("RepeatingJob", count);
		job.setJobGroup(jobGroup);
		job.schedule();
		try {
			jobGroup.join(0, null);
		} catch (OperationCanceledException e) {
			fail("1.0", e);
		} catch (InterruptedException e) {
			fail("1.1", e);
		}
		// Verify that the job has run the expected number of times.
		assertEquals("1.2", count, job.getRunCount());
	}

	/**
	 * Tests that joining a job from another job that is in the same job group
	 * yields an IllegalStateException.
	 */
	public void testJoiningAJobInTheSameJobGroupFails() {
		JobGroup jobGroup = new JobGroup("JobGroup", 2, 2);
		final TestJob firstJob = new TestJob("FirstJob", 1000000, 10);
		firstJob.setJobGroup(jobGroup);
		firstJob.schedule();
		waitForStart(firstJob);

		final boolean joinFailed[] = {false};
		Job secondJob = new Job("SecondJob") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					firstJob.join();
				} catch (InterruptedException ie) {
					// ignore
				} catch (IllegalStateException ise) {
					// expected
					joinFailed[0] = true;
				}
				return Status.OK_STATUS;
			}
		};
		secondJob.setJobGroup(jobGroup);
		secondJob.schedule();
		waitForCompletion(secondJob);
		assertTrue("1.0", joinFailed[0]);

		firstJob.cancel();
		waitForCompletion(jobGroup);
	}

	/**
	 * Tests that the progress is reported on the monitor used for join.
	 */
	public void testJoinWithProgressMonitor() {
		final int NUM_JOBS = 100;
		JobGroup jobGroup = new JobGroup("JobGroup", 10, NUM_JOBS);
		final TestBarrier2 barrier = new TestBarrier2();
		for (int i = 0; i < NUM_JOBS; i++) {
			TestJob testJob = new TestJob("TestJob", 1, 10) {
				@Override
				public IStatus run(IProgressMonitor monitor) {
					barrier.waitForStatus(TestBarrier2.STATUS_START);
					return super.run(monitor);
				}
			};
			testJob.setJobGroup(jobGroup);
			testJob.schedule();
		}
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		barrier.setStatus(TestBarrier2.STATUS_START);
		try {
			jobGroup.join(0, monitor);
		} catch (OperationCanceledException | InterruptedException e) {
			// ignore
		}
		// Check the progress reporting on monitor.
		monitor.sanityCheck();
		monitor.assertUsedUp();
	}

	/**
	 * Test for bug 543660 - JobGroup.join() blocks if scheduling more jobs as seed
	 * count
	 */
	public void testJoinIfJobCoundExceedsSeedCount() throws Exception {
		class ExclusiveRule implements ISchedulingRule {
			@Override
			public boolean contains(ISchedulingRule rule) {
				return isConflicting(rule);
			}
			@Override
			public boolean isConflicting(ISchedulingRule rule) {
				return rule instanceof ExclusiveRule;
			}
		}

		ExclusiveRule rule = new ExclusiveRule();

		final int SEED_JOBS = 2;
		AtomicLong count = new AtomicLong(0);
		JobGroup jobGroup = new JobGroup("JobGroup", 2, SEED_JOBS);
		for (int i = 0; i < SEED_JOBS; i++) {
			TestJob testJob = new TestJob("TestJob", 0, 0) {
				@Override
				public IStatus run(IProgressMonitor monitor) {
					return new Status(IStatus.INFO, "hello", "" + count.incrementAndGet());
				}
			};
			testJob.setRule(rule);
			testJob.setJobGroup(jobGroup);
			testJob.schedule();
		}

		jobGroup.join(0, null);

		IStatus[] children = jobGroup.getResult().getChildren();
		assertEquals(SEED_JOBS, children.length);
		Integer[] results = Arrays.stream(children).map(s -> Integer.valueOf(s.getMessage())).toArray(Integer[]::new);
		for (int i = 0; i < results.length; i++) {
			assertEquals("Job result in unexpected order", i + 1, results[i].intValue());
		}

		TestJob testJob = new TestJob("TestJob", 1, 10) {
			@Override
			public IStatus run(IProgressMonitor monitor) {
				return new Status(IStatus.INFO, "hello", "" + count.incrementAndGet());
			}
		};
		testJob.setRule(rule);
		testJob.setJobGroup(jobGroup);
		testJob.schedule();


		FussyProgressMonitor monitor = new FussyProgressMonitor();
		// should not block
		jobGroup.join(0, monitor);

		children = jobGroup.getResult().getChildren();
		assertEquals(SEED_JOBS + 1, children.length);
		results = Arrays.stream(children).map(s -> Integer.valueOf(s.getMessage())).toArray(Integer[]::new);
		for (int i = 0; i < results.length; i++) {
			assertEquals("Job result in unexpected order", i + 1, results[i].intValue());
		}

		// Check the progress reporting on monitor.
		monitor.sanityCheck();
		monitor.assertUsedUp();

		testJob.setJobGroup(jobGroup);
		testJob.schedule();

		monitor = new FussyProgressMonitor();
		// should not block
		jobGroup.join(0, monitor);

		children = jobGroup.getResult().getChildren();
		assertEquals(SEED_JOBS + 2, children.length);
		results = Arrays.stream(children).map(s -> Integer.valueOf(s.getMessage())).toArray(Integer[]::new);
		for (int i = 0; i < results.length; i++) {
			assertEquals("Job result in unexpected order", i + 1, results[i].intValue());
		}
		// Check the progress reporting on monitor.
		monitor.sanityCheck();
		monitor.assertUsedUp();
	}

	/**
	 * Tested scenario: - Create and add a WaitingJob to the JobGroup and schedule
	 * it when the job manager is suspended - Join on the JobGroup when the job
	 * manager is suspended
	 *
	 * Expected result: The join call on the JobGroup should not wait for the
	 * WaitingJob as the WaitingJob is not going to be executed when the job manger
	 * is suspended.
	 */
	public void testJoinWithJobManagerSuspended_1() throws InterruptedException {
		final JobGroup jobGroup = new JobGroup("JobGroup", 1, 1);
		final TestBarrier2 barrier = new TestBarrier2();
		final int[] groupJobsCount = new int[] {-1};
		final TestJob waiting = new TestJob("WaitingJob", 1000000, 1);
		waiting.setJobGroup(jobGroup);
		final TestJob running = new TestJob("RunningJob", 2, 1);
		running.setJobGroup(jobGroup);
		Job job = new Job("MainJob") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				barrier.setStatus(TestBarrier2.STATUS_START);
				try {
					running.schedule();
					// Wait until the running job is actually running.
					waitForStart(running);
					// Suspend before join.
					manager.suspend();
					waiting.schedule();
					running.join();
					jobGroup.join(0, null);
					groupJobsCount[0] = jobGroup.getActiveJobs().size();
				} catch (InterruptedException e) {
					// ignore
				} finally {
					// clean up
					waiting.cancel();
					try {
						waiting.join();
					} catch (InterruptedException e) {
						// ignore
					}
					manager.resume();
				}
				barrier.setStatus(TestBarrier2.STATUS_DONE);
				return Status.OK_STATUS;
			}
		};
		try {
			job.schedule();
			barrier.waitForStatus(TestBarrier2.STATUS_DONE);
			assertEquals(1, groupJobsCount[0]);
		} catch (AssertionFailedError e) {
			// interrupt to avoid deadlock and perform cleanup
			Thread thread = job.getThread();
			if (thread != null) {
				thread.interrupt();
			}
			// re-throw since the test failed
			throw e;
		} finally {
			// Wait until cleanup is done.
			job.join();
		}
	}

	/**
	 * Tested scenario:
	 *   - Join on the JobGroup when the job manager is NOT suspended
	 *   - Create and add a WaitingJob to the JobGroup and schedule it when the job manager is suspended
	 *
	 * Expected result:
	 *   The join call on the JobGroup should not wait for the WaitingJob as the WaitingJob is not going
	 *   to be executed when the job manger is suspended.
	 */
	public void testJoinWithJobManagerSuspended_2() throws InterruptedException {
		final JobGroup jobGroup = new JobGroup("JobGroup", 1, 1);
		final TestBarrier2 barrier = new TestBarrier2();
		final int[] groupJobsCount = new int[] {-1};
		final TestJob waiting = new TestJob("WaitingJob", 1000000, 10);
		waiting.setJobGroup(jobGroup);
		final TestJob running = new TestJob("RunningJob", 1000000, 10);
		running.setJobGroup(jobGroup);

		final Thread t = new Thread(() -> {
			barrier.setStatus(TestBarrier2.STATUS_RUNNING);
			try {
				jobGroup.join(0, null);
			} catch (OperationCanceledException | InterruptedException e) {
				// ignore
			}
			barrier.setStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);
		});
		Job job = new Job("MainJob") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				barrier.setStatus(TestBarrier2.STATUS_START);
				try {
					running.schedule();
					// wait until the running job is actually running
					waitForStart(running);
					t.start();
					barrier.waitForStatus(TestBarrier2.STATUS_RUNNING);
					// suspend before scheduling new job
					manager.suspend();
					waiting.schedule();
					running.cancel();
					barrier.waitForStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);
					groupJobsCount[0] = jobGroup.getActiveJobs().size();
					barrier.setStatus(TestBarrier2.STATUS_DONE);
				} finally {
					// clean up
					waiting.cancel();
					try {
						waiting.join();
					} catch (InterruptedException e) {
						// ignore
					}
					manager.resume();
				}
				return Status.OK_STATUS;
			}
		};
		try {
			job.schedule();
			barrier.waitForStatus(TestBarrier2.STATUS_DONE);
			assertEquals(1, groupJobsCount[0]);
		} catch (AssertionFailedError e) {
			// interrupt to avoid deadlock and perform cleanup
			Thread thread = job.getThread();
			if (thread != null) {
				thread.interrupt();
			}
			// re-throw since the test failed
			throw e;
		} finally {
			// wait until cleanup is done
			job.join();
		}
	}

	/**
	 * Tested scenario:
	 *   - Join on the JobGroup when the job manager is NOT suspended
	 *   - Create and add a WaitingJob to the JobGroup and schedule it when the job manager is suspended
	 *   - Resume the job manager which causes the waiting job to start
	 *
	 * Expected result:
	 *   The join call on the JobGroup should wait for the WaitingJob as the WaitingJob was started
	 *   to execute before the join ended.
	 */
	public void testJoinWithJobManagerSuspended_3() throws InterruptedException {
		final JobGroup jobGroup = new JobGroup("JobGroup", 1, 1);
		final TestBarrier2 barrier = new TestBarrier2();
		final int[] groupJobsCount = new int[] {-1};
		final TestJob waiting = new TestJob("waiting job", 1000000, 10);
		waiting.setJobGroup(jobGroup);
		final TestJob running = new TestJob("running job", 1000000, 10);
		running.setJobGroup(jobGroup);

		final Thread t = new Thread(() -> {
			barrier.setStatus(TestBarrier2.STATUS_RUNNING);
			try {
				jobGroup.join(0, null);
			} catch (OperationCanceledException | InterruptedException e) {
				// ignore
			}
			barrier.setStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);
		});
		Job job = new Job("MainJob") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				barrier.setStatus(TestBarrier2.STATUS_START);

				running.schedule();
				// Wait until the running job is actually running.
				waitForStart(running);
				// Start the thread to make join call.
				t.start();
				barrier.waitForStatus(TestBarrier2.STATUS_RUNNING);
				// Suspend before scheduling the waiting job.
				manager.suspend();
				waiting.schedule();
				manager.resume();
				running.cancel();
				waitForStart(waiting);
				waiting.cancel();
				barrier.waitForStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);
				groupJobsCount[0] = jobGroup.getActiveJobs().size();
				barrier.setStatus(TestBarrier2.STATUS_DONE);

				return Status.OK_STATUS;
			}
		};
		try {
			job.schedule();
			barrier.waitForStatus(TestBarrier2.STATUS_DONE);
			assertEquals(0, groupJobsCount[0]);
		} catch (AssertionFailedError e) {
			// interrupt to avoid deadlock and perform cleanup
			Thread thread = job.getThread();
			if (thread != null) {
				thread.interrupt();
			}
			// re-throw since the test failed
			throw e;
		} finally {
			// Wait until cleanup is done
			job.join();
		}
	}

	/**
	 * Tested scenario:
	 *   - Add a failing job to the JobGroup between passing jobs.
	 *
	 * Expected result:
	 *   The JobGroup should be canceled when the failing job is completed, because by default
	 *   a job group is canceled when a job belonging to the group is failed.
	 */
	public void testShouldCancel_1() {
		final int NUM_SEED_JOBS = 10;
		final int NUM_ADDITIONAL_JOBs = 10;
		final Job jobs[] = new Job[NUM_SEED_JOBS + NUM_ADDITIONAL_JOBs];
		final JobGroup jobGroup = new JobGroup("JobGroup", NUM_SEED_JOBS, NUM_SEED_JOBS);
		for (int i = 0; i < NUM_SEED_JOBS - 1; i++) {
			TestJob job = new TestJob("TestJob", 1000000, 1);
			jobs[i] = job;
			job.setJobGroup(jobGroup);
			job.schedule();
			waitForStart(job);
		}
		// Verify that all the test jobs are running.
		assertEquals("1.0", JobGroup.ACTIVE, jobGroup.getState());
		assertEquals("1.1", NUM_SEED_JOBS - 1, jobGroup.getActiveJobs().size());
		for (int i = 0; i < NUM_SEED_JOBS - 1; i++) {
			assertState("2." + i, jobs[i], Job.RUNNING);
		}

		final TestBarrier2 barrier = new TestBarrier2();
		Job failedJob = new Job("FailedJob") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				barrier.setStatus(TestBarrier2.STATUS_WAIT_FOR_RUN);
				barrier.waitForStatus(TestBarrier2.STATUS_RUNNING);
				return new Status(IStatus.ERROR, "org.eclipse.core.jobs", "Error");
			}
		};
		jobs[NUM_SEED_JOBS - 1] = failedJob;
		failedJob.setJobGroup(jobGroup);
		failedJob.schedule();
		barrier.waitForStatus(TestBarrier2.STATUS_WAIT_FOR_RUN);

		// Verify that the failing job also started running.
		assertEquals("3.0", NUM_SEED_JOBS, jobGroup.getActiveJobs().size());
		assertState("3.1", failedJob, Job.RUNNING);

		for (int i = NUM_SEED_JOBS; i < NUM_SEED_JOBS + NUM_ADDITIONAL_JOBs; i++) {
			Job job = new TestJob("AdditionalJob", 1000000, 1);
			jobs[i] = job;
			job.setJobGroup(jobGroup);
			job.schedule();
		}

		// Verify that all the jobs are active.
		assertEquals("4.0", NUM_SEED_JOBS + NUM_ADDITIONAL_JOBs, jobGroup.getActiveJobs().size());
		for (int i = NUM_SEED_JOBS; i < NUM_SEED_JOBS + NUM_ADDITIONAL_JOBs; i++) {
			assertState("5." + i, jobs[i], Job.WAITING);
		}
		// Allow the failing job to complete.
		barrier.setStatus(TestBarrier2.STATUS_RUNNING);
		// wait for the job group to complete.
		waitForCompletion(jobGroup);
		// Verify that all the jobs are moved to NONE state. Also verify that the failing job failed,
		// other running jobs got canceled and the waiting jobs are never allowed to run.
		for (int i = 0; i < NUM_SEED_JOBS + NUM_ADDITIONAL_JOBs; i++) {
			assertState("6." + i, jobs[i], Job.NONE);
			if (i < NUM_SEED_JOBS - 1) {
				assertEquals("6." + i, IStatus.CANCEL, jobs[i].getResult().getSeverity());
			} else if (i == NUM_SEED_JOBS - 1) {
				assertEquals("6." + i, IStatus.ERROR, jobs[i].getResult().getSeverity());
			} else if (i == NUM_SEED_JOBS) {
				// This job might have been started running before the group gets canceled.
				IStatus result = jobs[i].getResult();
				if (result != null) {
					assertEquals("6." + i, IStatus.CANCEL, result.getSeverity());
				}
			} else {
				assertNull("6." + i, jobs[i].getResult());
			}
		}
	}

	/**
	 * Tested scenario:
	 *   - Record the number of times the JobGroup.shouldCancel method is invoked.
	 *
	 * Expected result:
	 *   The shouldCancel method of the JobGroup should be called after the completion of every job
	 *   belonging to that group except the last one (shouldCancel method is not called after the
	 *   completion of the last job in the jobGroup as there are no jobs left to cancel).
	 */
	public void testShouldCancel_2() {
		final int NUM_JOBS = 10;
		final int numShouldCancelCalled[] = {0};
		final JobGroup jobGroup = new JobGroup("JobGroup", 1, NUM_JOBS) {
			@Override
			protected boolean shouldCancel(IStatus lastCompletedJobResult, int numberOfFailedJobs, int numberOfCanceledJobs) {
				numShouldCancelCalled[0]++;
				return super.shouldCancel(lastCompletedJobResult, numberOfFailedJobs, numberOfCanceledJobs);
			}
		};
		for (int i = 0; i < NUM_JOBS; i++) {
			Job job = new TestJob("TestJob", 1, 1);
			job.setJobGroup(jobGroup);
			job.schedule();
		}
		waitForCompletion(jobGroup);
		assertEquals("1.0", NUM_JOBS - 1, numShouldCancelCalled[0]);
	}

	/**
	 * Tested scenario:
	 *   - Record the number of times the JobGroup.shouldCancel method is invoked and all the values passed to it
	 *   - Always return false from the shouldCancel method of the JobGroup to avoid the group cancellation due to failed jobs
	 *
	 * Expected result:
	 *   The shouldCancel method of the JobGroup should be called with appropriate values after
	 *   the completion of every job that belongs to that group except the last one
	 *   (the shouldCancel method is not called after the completion of the last job in the
	 *   jobGroup as there are no jobs left to cancel).
	 */
	public void testShouldCancel_3() {
		final int status[] = {IStatus.OK, IStatus.INFO, IStatus.WARNING, IStatus.ERROR, IStatus.CANCEL, IStatus.OK};
		final int numShouldCancelCalled[] = {0};
		final int failedJobsCount[] = {0};
		final int canceledJobsCount[] = {0};
		final IStatus completedJobResult[] = new Status[1];
		final TestBarrier2 barrier = new TestBarrier2();

		final JobGroup jobGroup = new JobGroup("JobGroup", 1, status.length) {
			@Override
			protected boolean shouldCancel(IStatus lastCompletedJobResult, int numberOfFailedJobs, int numberOfCanceledJobs) {
				numShouldCancelCalled[0]++;
				failedJobsCount[0] = numberOfFailedJobs;
				canceledJobsCount[0] = numberOfCanceledJobs;
				completedJobResult[0] = lastCompletedJobResult;
				barrier.setStatus(TestBarrier2.STATUS_DONE);
				return false;
			}
		};

		for (int i = 0; i < status.length; i++) {
			final int jobNumber = i;
			final IStatus returnedStatus[] = new IStatus[1];
			Job job = new TestJob("TestJob", 1, 1) {
				@Override
				public IStatus run(IProgressMonitor monitor) {
					super.run(monitor);
					returnedStatus[0] = new Status(status[jobNumber], "org.eclipse.core.jobs", "Job " + jobNumber);
					return returnedStatus[0];
				}
			};
			job.setJobGroup(jobGroup);
			barrier.setStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);
			job.schedule();

			// shouldCancel method will not be invoked for the last job.
			if (i == status.length - 1) {
				continue;
			}

			barrier.waitForStatus(TestBarrier2.STATUS_DONE);
			// Verify that the shouldCancel method is called with appropriate values.
			assertEquals("1." + i, i + 1, numShouldCancelCalled[0]);
			assertEquals("2." + i, returnedStatus[0], completedJobResult[0]);
			if (i < 3) {
				assertEquals("3." + i, 0, failedJobsCount[0]);
			} else {
				assertEquals("3." + i, 1, failedJobsCount[0]);
			}
			if (i < 4) {
				assertEquals("4." + i, 0, canceledJobsCount[0]);
			} else {
				assertEquals("4." + i, 1, canceledJobsCount[0]);
			}
		}
		waitForCompletion(jobGroup);
	}

	/**
	 * Tested scenario:
	 *   - JobGroup.shouldCancel returns true after certain jobs are completed.
	 *
	 * Expected result:
	 *   The remaining jobs are canceled in a reasonable time after the shouldCancel method of the
	 *   JobGroup returns true.
	 */
	public void testShouldCancel_4() {
		final int NUM_JOBS = 1000;
		final int NUM_JOBS_LIMIT = 100;
		final int numShouldCancelCalled[] = {0};
		final TestBarrier2 barrier = new TestBarrier2();
		final JobGroup jobGroup = new JobGroup("JobGroup", 10, NUM_JOBS) {
			@Override
			protected boolean shouldCancel(IStatus lastCompletedJobResult, int numberOfFailedJobs, int numberOfCanceledJobs) {
				numShouldCancelCalled[0]++;
				if (numShouldCancelCalled[0] == NUM_JOBS_LIMIT) {
					return true;
				}
				return false;
			}
		};
		for (int i = 0; i < NUM_JOBS; i++) {
			final int jobNumber = i;
			Job job = new TestJob("TestJob", 1, 1) {
				@Override
				public IStatus run(IProgressMonitor monitor) {
					barrier.waitForStatus(TestBarrier2.STATUS_START);
					super.run(monitor);
					return new Status(IStatus.INFO, "org.eclipse.core.jobs", "Job " + jobNumber);
				}
			};
			job.setJobGroup(jobGroup);
			job.schedule();
		}
		// Allow the jobs to proceed to run.
		barrier.setStatus(TestBarrier2.STATUS_START);
		waitForCompletion(jobGroup);
		assertTrue("1.0", numShouldCancelCalled[0] >= NUM_JOBS_LIMIT);
		// Verify that the group is canceled in a reasonable time,
		// i.e only 10 jobs are allowed to run after the shouldCancel method returned true.
		assertTrue("2.0", numShouldCancelCalled[0] < NUM_JOBS_LIMIT + 10);
	}

	public void testDefaultComputeGroupResult() {
		final int status[] = {IStatus.OK, IStatus.INFO, IStatus.WARNING, IStatus.ERROR, IStatus.CANCEL};
		final JobGroup jobGroup = new JobGroup("JobGroup", 1, status.length) {
			@Override
			protected boolean shouldCancel(IStatus lastCompletedJobResult, int numberOfFailedJobs, int numberOfCanceledJobs) {
				// Return false always so that the group will not be canceled due to failed jobs.
				return false;
			}
		};

		for (int i = 0; i < status.length; i++) {
			final int jobNumber = i;
			Job job = new TestJob("TestJob", 10, 10) {
				@Override
				public IStatus run(IProgressMonitor monitor) {
					super.run(monitor);
					return new Status(status[jobNumber], "org.eclipse.core.jobs", "Job " + jobNumber);
				}
			};
			job.setJobGroup(jobGroup);
			job.schedule();
		}
		waitForCompletion(jobGroup);
		IStatus[] jobResults = jobGroup.getResult().getChildren();
		// Verify that the group result contains all the job results except the OK statuses.
		assertEquals("1.0", status.length - 1, jobResults.length);
		for (int i = 1; i < status.length; i++) {
			assertEquals("2." + i, status[i], jobResults[i - 1].getSeverity());
		}
	}

	public void testCustomComputeGroupResult() {
		final MultiStatus returnedGroupResult[] = new MultiStatus[1];
		final IStatus originalJobResults[][] = {new IStatus[0]};
		final int status[] = {IStatus.OK, IStatus.INFO, IStatus.WARNING, IStatus.ERROR, IStatus.CANCEL};
		final JobGroup jobGroup = new JobGroup("group", 1, status.length) {
			@Override
			protected boolean shouldCancel(IStatus lastCompletedJobResult, int numberOfFailedJobs, int numberOfCanceledJobs) {
				return false;
			}

			@Override
			protected MultiStatus computeGroupResult(List<IStatus> jobResults) {
				// Record the original job results and return a dummy groupresult.
				originalJobResults[0] = jobResults.toArray(new IStatus[jobResults.size()]);
				returnedGroupResult[0] = new MultiStatus("org.eclipse.core.jobs", 0, new IStatus[0], "custom result", null);
				return returnedGroupResult[0];
			}
		};

		for (int i = 0; i < status.length; i++) {
			final int jobNumber = i;
			Job job = new TestJob("TestJob", 10, 10) {
				@Override
				public IStatus run(IProgressMonitor monitor) {
					super.run(monitor);
					return new Status(status[jobNumber], "org.eclipse.core.jobs", "Job " + jobNumber);
				}
			};
			job.setJobGroup(jobGroup);
			job.schedule();
		}
		waitForCompletion(jobGroup);
		// Verify that the compute group result is called with all the completed job results.
		assertEquals("2.0", status.length, originalJobResults[0].length);
		for (int i = 0; i < status.length; i++) {
			assertEquals("3." + i, status[i], originalJobResults[0][i].getSeverity());
		}
		// Verify that JobGroup.getResult returns the status returned by JobGroup.computeGroupResult method.
		assertEquals("4.0", returnedGroupResult[0], jobGroup.getResult());
	}

	// https://bugs.eclipse.org/461621
	public void testSlowComputeGroupResult() throws Exception {
		final JobGroup jobGroup = new JobGroup("group", 1, 1) {
			@Override
			protected MultiStatus computeGroupResult(List<IStatus> jobResults) {
				sleep(500);
				return new MultiStatus("org.eclipse.core.jobs", 0, new IStatus[0], "custom result", null);
			}
		};

		Job job = new Job("TestJob") {
			@Override
			public IStatus run(IProgressMonitor monitor) {
				return Status.OK_STATUS;
			}
		};
		job.setJobGroup(jobGroup);
		job.schedule();
		waitForCompletion(job, 100);

		boolean completed = jobGroup.join(1000, null);
		assertTrue("2.0", completed);
		MultiStatus result = jobGroup.getResult();
		assertNotNull("3.0", result);
	}

	/**
	 * Tests that job groups work fine with normal jobs that are not belonging to any group.
	 */
	public void testJobGroupAlongWithNormalJobs() {
		final int NUM_GROUP_JOBS = 1000;
		final int NUM_NORMAL_JOBS = 100;
		JobGroup jobGroup = new JobGroup("JobGroup", 1, NUM_GROUP_JOBS);
		for (int i = 0; i < NUM_GROUP_JOBS; i++) {
			TestJob testJob = new TestJob("GroupJob", 1000000, 10);
			testJob.setJobGroup(jobGroup);
			testJob.schedule();
		}
		assertEquals("1.0", JobGroup.ACTIVE, jobGroup.getState());
		assertEquals("2.0", NUM_GROUP_JOBS, jobGroup.getActiveJobs().size());

		TestJob normalJobs[] = new TestJob[NUM_NORMAL_JOBS];
		for (int i = 0; i < NUM_NORMAL_JOBS; i++) {
			TestJob testJob = new TestJob("NormalJob", 10, 10);
			normalJobs[i] = testJob;
			testJob.schedule();
		}
		for (int i = 0; i < NUM_NORMAL_JOBS; i++) {
			waitForCompletion(normalJobs[i]);
		}

		// Tests that the normal jobs are completed fine while the group jobs are still running.
		assertEquals("3.0", JobGroup.ACTIVE, jobGroup.getState());
		assertEquals("4.0", NUM_GROUP_JOBS, jobGroup.getActiveJobs().size());
		jobGroup.cancel();
		waitForCompletion(jobGroup);
	}

	/**
	 * Tests that the JobManager publishes a final job group status to IJobChangeListeners.
	 */
	public void testJobManagerPublishesJobGroupResults() throws InterruptedException {
		final int NUM_GROUP_JOBS = 3;
		final String GROUP_NAME = "TestJobGroup";
		final JobGroup jobGroup = new JobGroup(GROUP_NAME, 1, NUM_GROUP_JOBS);

		// Record job completion events for all jobs in this job group.
		Collection<IJobChangeEvent> eventQueue = new ConcurrentLinkedQueue<>();
		IJobChangeListener listener = new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				if (event.getJob().getJobGroup() == jobGroup) {
					eventQueue.add(event);
				}
			}
		};
		manager.addJobChangeListener(listener);

		// Execute all jobs in the job group and validate that the last job includes the job group result.
		try {
			for (int i = 0; i < NUM_GROUP_JOBS; i++) {
				TestJob testJob = new TestJob("GroupJob", 10, 1);
				testJob.setJobGroup(jobGroup);
				testJob.schedule();
			}
			waitForCompletion(jobGroup);

			// That the job state completed is no guarantee that all Notifications did
			// happen. Lets wait some more:
			for (int i = 0; i < 1000 && eventQueue.size() < NUM_GROUP_JOBS; i++) {
				Thread.sleep(1);
			}

			List<IJobChangeEvent> events = new ArrayList<>();
			eventQueue.forEach(events::add);

			assertEquals("Should have seen as many job completion events as the count of jobs in the job group.", NUM_GROUP_JOBS, events.size());
			for (int i = 0; i < NUM_GROUP_JOBS; i++) {
				IJobChangeEvent event = events.get(i);
				assertNotNull("All job completion events should have a job status.", event.getResult());
				if (i < NUM_GROUP_JOBS - 1) {
					assertNull("Only the last job competion event shoud have a job group status.", event.getJobGroupResult());
				} else {
					assertNotNull("The last job competion event shoud have a job group status.", event.getJobGroupResult());
				}
			}
		} finally {
			manager.removeJobChangeListener(listener);
		}
	}

	private void assertState(String msg, Job job, int expectedState) {
		int actualState = job.getState();
		assertSame(
				msg + ": expected state: " + printState(expectedState) + " actual state: " + printState(actualState),
				actualState, expectedState);
	}

	private String printState(int state) {
		switch (state) {
			case Job.NONE :
				return "NONE";
			case Job.WAITING :
				return "WAITING";
			case Job.SLEEPING :
				return "SLEEPING";
			case Job.RUNNING :
				return "RUNNING";
		}
		return "UNKNOWN";
	}

	private void waitForStart(TestJob job) {
		int i = 0;
		while (job.getRunCount() < 1) {
			Thread.yield();
			sleep(100);
			Thread.yield();
			// Sanity test to avoid hanging tests.
			if (i++ >= 100) {
				dumpState();
				fail("Timeout waiting for job to start. Job: " + job + ", state: " + job.getState());
			}
		}
	}

	private void waitForCompletion(JobGroup jobGroup) {
		int i = 0;
		while (jobGroup.getState() != JobGroup.NONE) {
			Thread.yield();
			sleep(1);
			Thread.yield();
			// Sanity test to avoid hanging tests.
			if (i++ >= 10000) {
				dumpState();
				fail("Timeout waiting for job group " + jobGroup.getName() + " to be completed");
			}
		}
	}
}
