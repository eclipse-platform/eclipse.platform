/*******************************************************************************
 * Copyright (c) 2003, 2018 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.core.tests.runtime.jobs;

import static java.util.Collections.synchronizedList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.stream.Stream;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.core.runtime.jobs.LockListener;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.core.tests.harness.FussyProgressMonitor;
import org.eclipse.core.tests.harness.TestBarrier2;
import org.eclipse.core.tests.harness.TestJob;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the API of the class IJobManager
 */
@SuppressWarnings("restriction")
public class IJobManagerTest extends AbstractJobTest {
	class TestJobListener extends JobChangeAdapter {
		private final Set<Job> scheduled = Collections.synchronizedSet(new HashSet<>());

		public void cancelAllJobs() {
			Job[] jobs = scheduled.toArray(new Job[0]);
			for (Job job : jobs) {
				job.cancel();
			}
		}

		@Override
		public void done(IJobChangeEvent event) {
			synchronized (IJobManagerTest.this) {
				if (scheduled.remove(event.getJob())) {
					//wake up the waitForCompletion method
					completedJobs.incrementAndGet();
					IJobManagerTest.this.notify();
				}
			}
		}

		@Override
		public void scheduled(IJobChangeEvent event) {
			Job job = event.getJob();
			synchronized (IJobManagerTest.this) {
				if (job instanceof TestJob) {
					scheduledJobs.incrementAndGet();
					scheduled.add(job);
				}
			}
		}
	}

	/**
	 * Tests that are timing sensitive cannot be released in automated tests.
	 * Set this flag to true to do manual timing sanity tests
	 */
	private static final boolean PEDANTIC = false;

	protected AtomicInteger completedJobs;
	private IJobChangeListener[] jobListeners;

	protected AtomicInteger scheduledJobs;

	/**
	 * Asserts the current job state
	 */
	public void assertState(Job job, int expectedState) {
		int actualState = job.getState();
		if (actualState != expectedState) {
			fail("expected state: " + printState(expectedState) + " actual state: " + printState(actualState));
		}
	}

	/**
	 * Asserts the current job state
	 */
	public void assertState(Job job, int expectedState, int jobNumer) {
		int actualState = job.getState();
		if (actualState != expectedState) {
			fail("job number " + jobNumer + ", expected state: " + printState(expectedState) + " actual state: "
					+ printState(actualState));
		}
	}

	/**
	 * Cancels a list of jobs
	 */
	protected void cancel(ArrayList<Job> jobs) {
		for (Job job : jobs) {
			job.cancel();
		}
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

	@BeforeEach
	public void setUp() throws Exception {
		completedJobs = new AtomicInteger();
		scheduledJobs = new AtomicInteger();
		jobListeners = new IJobChangeListener[] {/* new VerboseJobListener(),*/
		new TestJobListener()};
		for (IJobChangeListener jobListener : jobListeners) {
			manager.addJobChangeListener(jobListener);
		}
	}

	@AfterEach
	public void tearDown() throws Exception {
		for (IJobChangeListener jobListener : jobListeners) {
			if (jobListener instanceof TestJobListener) {
				((TestJobListener) jobListener).cancelAllJobs();
			}
		}
		waitForCompletion();
		for (IJobChangeListener jobListener : jobListeners) {
			manager.removeJobChangeListener(jobListener);
		}
	}

	@Test
	public void testBadGlobalListener() {
		final AtomicIntegerArray status = new AtomicIntegerArray(new int[] { -1 });
		Job job = new Job("testBadGlobalListener") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				status.set(0, TestBarrier2.STATUS_RUNNING);
				return Status.OK_STATUS;
			}
		};
		IJobChangeListener listener = new JobChangeAdapter() {
			@Override
			public void running(IJobChangeEvent event) {
				throw new Error("Thrown from bad global listener");
			}
		};
		try {
			Job.getJobManager().addJobChangeListener(listener);
			job.schedule();
			TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_RUNNING);
		} finally {
			Job.getJobManager().removeJobChangeListener(listener);
		}
	}

	@Test
	public void testBadLocalListener() {
		final AtomicIntegerArray status = new AtomicIntegerArray(new int[] { -1 });
		Job job = new Job("testBadLocalListener") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				status.set(0, TestBarrier2.STATUS_RUNNING);
				return Status.OK_STATUS;
			}
		};
		IJobChangeListener listener = new JobChangeAdapter() {
			@Override
			public void running(IJobChangeEvent event) {
				throw new Error("Thrown from bad local listener");
			}
		};
		try {
			job.addJobChangeListener(listener);
			job.schedule();
			TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_RUNNING);
		} finally {
			job.removeJobChangeListener(listener);
		}
	}

	@Test
	public void testBeginInvalidNestedRules() {
		final ISchedulingRule root = new PathRule("/");
		final ISchedulingRule invalid = new ISchedulingRule() {
			@Override
			public boolean isConflicting(ISchedulingRule rule) {
				return this == rule;
			}

			@Override
			public boolean contains(ISchedulingRule rule) {
				return this == rule || root.contains(rule);
			}
		};
		try {
			Job.getJobManager().beginRule(invalid, null);
			assertThrows(IllegalArgumentException.class, () -> Job.getJobManager().beginRule(root, null));
			Job.getJobManager().endRule(root);
		} finally {
			Job.getJobManager().endRule(invalid);
		}
	}

	/**
	 * Tests that if we call beginRule with a monitor that has already been
	 * cancelled, it won't try to obtain the rule.
	 */
	@Test
	public void testCancellationPriorToBeginRuleWontHoldRule() throws Exception {
		final Semaphore mainThreadSemaphore = new Semaphore(0);
		final Semaphore lockSemaphore = new Semaphore(0);
		final PathRule rule = new PathRule("testBeginRuleNoEnd");
		IProgressMonitor cancelledMonitor = SubMonitor.convert(null);
		cancelledMonitor.setCanceled(true);

		// Create a job that will hold the lock until the semaphore is signaled
		Job job = Job.create("", monitor -> {
			mainThreadSemaphore.release();
			try {
				lockSemaphore.acquire();
			} catch (InterruptedException e) {
			}
		});
		job.setRule(rule);
		job.schedule();

		// Block until the job acquires the lock
		mainThreadSemaphore.acquire();
		boolean canceledExceptionThrown = false;
		try {
			// This will deadlock if it attempts to acquire the rule, and will
			// throw an OCE without doing anything if it is working correctly.
			manager.beginRule(rule, cancelledMonitor);
		} catch (OperationCanceledException e) {
			canceledExceptionThrown = true;
		} finally {
			// Code which follows the recommended pattern documented in
			// beginRule will call endRule even if beginRule threw an OCE.
			// Verify that calling endRule in this situation won't throw any
			// exceptions.
			manager.endRule(rule);
		}
		lockSemaphore.release();
		boolean interrupted = Thread.interrupted();
		assertTrue(canceledExceptionThrown, "An OperationCancelledException should have been thrown");
		assertFalse(interrupted, "The Thread.interrupted() state leaked");
	}

	/**
	 * Tests that if our monitor is cancelled while we're waiting on beginRule,
	 * it will stop waiting, will throw an {@link OperationCanceledException},
	 * and will clear the Thread.interrupted() flag.
	 */
	@Test
	public void testCancellationWhileWaitingOnRule() throws Exception {
		final Semaphore mainThreadSemaphore = new Semaphore(0);
		final Semaphore lockSemaphore = new Semaphore(0);
		final PathRule rule = new PathRule("testBeginRuleNoEnd");
		final NullProgressMonitor rootMonitor = new NullProgressMonitor();
		// We use a SubMonitor here to work around a special case in the
		// JobManager code that ignores NullProgressMonitor.
		IProgressMonitor nestedMonitor = SubMonitor.convert(rootMonitor);
		nestedMonitor.setCanceled(false);

		// Create a job that will hold the lock until the semaphore is signalled
		Job job = Job.create("", monitor -> {
			mainThreadSemaphore.release();
			try {
				lockSemaphore.acquire();
			} catch (InterruptedException e) {
			}
		});
		job.setRule(rule);
		job.schedule();

		// Block until the job acquires the lock
		mainThreadSemaphore.acquire();

		// Create a job that will cancel our monitor in 100ms
		Job cancellationJob = Job.create("", monitor -> {
			rootMonitor.setCanceled(true);
		});
		cancellationJob.schedule(100);

		boolean canceledExceptionThrown = false;
		// Now try to obtain the rule that is currently held by "job".
		try {
			manager.beginRule(rule, nestedMonitor);
		} catch (OperationCanceledException e) {
			canceledExceptionThrown = true;
		} finally {
			// Code which follows the recommended pattern documented in
			// beginRule will call endRule even if beginRule threw an OCE.
			// Verify that calling endRule in this situation won't throw any
			// exceptions.
			manager.endRule(rule);
		}
		lockSemaphore.release();
		boolean interrupted = Thread.interrupted();
		assertTrue(canceledExceptionThrown, "An OperationCancelledException should have been thrown");
		assertFalse(interrupted, "The Thread.interrupted() state leaked");
	}

	/**
	 * Tests running a job that begins a rule but never ends it
	 */
	@Test
	public void testBeginRuleNoEnd() throws InterruptedException {
		final PathRule rule = new PathRule("testBeginRuleNoEnd");
		Job job = new Job("testBeginRuleNoEnd") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask(getName(), 1);
				try {
					Job.getJobManager().beginRule(rule, null);
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
		job.join();
		//another thread should be able to access the rule now
		try {
			manager.beginRule(rule, null);
		} finally {
			manager.endRule(rule);
		}
	}

	@Test
	public void testBug48073() {
		ISchedulingRule ruleA = new PathRule("/testBug48073");
		ISchedulingRule ruleB = new PathRule("/testBug48073/B");
		ISchedulingRule ruleC = new PathRule("/testBug48073/C");
		TestJob jobA = new TestJob("Job1", 1000, 100);
		TestJob jobB = new TestJob("Job2", 1000, 100);
		TestJob jobC = new TestJob("Job3", 1000, 100);
		jobA.setRule(ruleA);
		jobB.setRule(ruleB);
		jobC.setRule(ruleC);

		//B should be running, A blocked by B and C blocked by A
		jobB.schedule();
		sleep(100);
		jobA.schedule();
		sleep(100);
		jobC.schedule();

		//cancel and restart A
		jobA.cancel();
		jobA.schedule();

		//cancel all jobs
		jobA.cancel();
		jobC.cancel();
		jobB.cancel();
	}

	/**
	 * Regression test for bug 57656
	 */
	@Test
	public void testBug57656() {
		TestJob jobA = new TestJob("Job1", 10000, 10);
		TestJob jobB = new TestJob("Job2", 1, 1);
		//schedule jobA
		jobA.schedule(100);
		//schedule jobB so it gets behind jobA in the queue
		jobB.schedule(101);
		//now put jobA to sleep indefinitely
		jobA.sleep();
		// jobB should still run even though jobA scheduled before did not start
		waitForCompletion(jobB, Duration.ofSeconds(1));
		jobA.terminate();
		jobA.cancel();
		waitForCompletion(jobA, Duration.ofSeconds(5));
	}

	/**
	 * This is a regression test for bug 71448. IJobManager.currentJob was not
	 * returning the correct value when executed in a thread that is performing
	 * asynchronous completion of a job (i.e., a UI Job)
	 */
	@Test
	public void testCurrentJob() {
		final Thread[] thread = new Thread[1];
		final boolean[] done = new boolean[] {false};
		final boolean[] success = new boolean[] {false};
		//create a job that will complete asynchronously
		final Job job = new Job("Test Job") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				setThread(thread[0]);
				done[0] = true;
				return ASYNC_FINISH;
			}
		};
		//create and run a thread that will run and finish the asynchronous job
		Runnable r = () -> {
			job.schedule();
			// wait for job to start running
			while (!done[0]) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// ignore
				}
			}
			// job should now be finishing asynchronously in this thread
			success[0] = job == Job.getJobManager().currentJob();
			job.done(Status.OK_STATUS);
		};
		thread[0] = new Thread(r);
		thread[0].start();
		try {
			thread[0].join();
		} catch (InterruptedException e) {
			//ignore
		}
		//assert that currentJob returned the correct value
		assertTrue(success[0]);
	}

	/**
	 * Tests for {@link IJobManager#currentRule()}.
	 */
	@Test
	public void testCurrentRule() {
		//first test when not running in a job
		runRuleSequence();

		//next test in a job with no rule of its own
		final List<AssertionError> errors = new ArrayList<>();
		Job sequenceJob = new Job("testCurrentRule") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					runRuleSequence();
				} catch (AssertionError e) {
					errors.add(e);
				}
				return Status.OK_STATUS;
			}
		};
		sequenceJob.schedule();
		waitForCompletion(sequenceJob);
		if (!errors.isEmpty()) {
			throw errors.iterator().next();
		}

		//now test in a job that has a scheduling rule
		ISchedulingRule jobRule = new PathRule("/testCurrentRule");
		sequenceJob.setRule(jobRule);
		sequenceJob.schedule();
		waitForCompletion(sequenceJob);
		if (!errors.isEmpty()) {
			throw errors.iterator().next();
		}
	}

	/**
	 * Helper method for testing {@link IJobManager#currentRule()}.
	 */
	protected void runRuleSequence() {
		if (runRuleSequenceInJobWithRule()) {
			return;
		}
		ISchedulingRule parent = new PathRule("/testCurrentRule/parent");
		ISchedulingRule child = new PathRule("/testCurrentRule/parent/child");
		assertNull(manager.currentRule());
		manager.beginRule(null, null);
		assertNull(manager.currentRule());
		manager.endRule(null);
		assertNull(manager.currentRule());
		manager.beginRule(parent, null);
		assertEquals(parent, manager.currentRule());
		//nested null rule
		manager.beginRule(null, null);
		assertEquals(parent, manager.currentRule());
		//nested non-null rule
		manager.beginRule(child, null);
		assertEquals(parent, manager.currentRule());
		manager.endRule(child);
		assertEquals(parent, manager.currentRule());
		manager.endRule(null);
		assertEquals(parent, manager.currentRule());
		manager.endRule(parent);
		assertNull(manager.currentRule());
	}

	/**
	 * Runs a sequence of begin/end rules and asserts that the
	 * job rule is always returned by {@link IJobManager#currentRule()}.
	 * Returns <code>false</code> if not invoked from within a job with
	 * a scheduling rule.
	 */
	private boolean runRuleSequenceInJobWithRule() {
		Job currentJob = manager.currentJob();
		if (currentJob == null) {
			return false;
		}
		ISchedulingRule jobRule = currentJob.getRule();
		if (jobRule == null) {
			return false;
		}
		//we are in a job with a rule, so now run our rule sequence
		ISchedulingRule parent = new PathRule("/testCurrentRule/parent");
		ISchedulingRule child = new PathRule("/testCurrentRule/parent/child");
		assertEquals(jobRule, manager.currentRule());
		manager.beginRule(null, null);
		assertEquals(jobRule, manager.currentRule());
		manager.endRule(null);
		assertEquals(jobRule, manager.currentRule());
		manager.beginRule(parent, null);
		assertEquals(jobRule, manager.currentRule());
		//nested null rule
		manager.beginRule(null, null);
		assertEquals(jobRule, manager.currentRule());
		//nested non-null rule
		manager.beginRule(child, null);
		assertEquals(jobRule, manager.currentRule());
		manager.endRule(child);
		assertEquals(jobRule, manager.currentRule());
		manager.endRule(null);
		assertEquals(jobRule, manager.currentRule());
		manager.endRule(parent);
		assertEquals(jobRule, manager.currentRule());
		return true;
	}

	@Test
	public void testDelayedJob() {
		//schedule a delayed job and ensure it doesn't start until instructed
		int[] sleepTimes = new int[] { 0, 1, 5, 10, 50, 100, 200, 250 };
		for (int i = 0; i < sleepTimes.length; i++) {
			long start = now();
			TestJob job = new TestJob("Noop", 0, 0);
			assertEquals(0, job.getRunCount());
			job.schedule(sleepTimes[i]);
			waitForCompletion();
			assertEquals(1, job.getRunCount(), i);
			long duration = now() - start;
			assertTrue(duration >= sleepTimes[i], "duration: " + duration + " sleep: " + sleepTimes[i]);
			//a no-op job shouldn't take any real time
			if (PEDANTIC) {
				assertTrue(duration < sleepTimes[i] + 1000, "duration: " + duration + " sleep: " + sleepTimes[i]);
			}
		}
	}

	@Test
	public void testJobFamilyCancel() {
		//test the cancellation of a family of jobs
		final int NUM_JOBS = 20;
		TestJob[] jobs = new TestJob[NUM_JOBS];
		//create two different families of jobs
		TestJobFamily first = new TestJobFamily(TestJobFamily.TYPE_ONE);
		TestJobFamily second = new TestJobFamily(TestJobFamily.TYPE_TWO);
		//need a scheduling rule so that the jobs would be executed one by one
		ISchedulingRule rule = new IdentityRule();

		for (int i = 0; i < NUM_JOBS; i++) {
			//assign half the jobs to the first family, the other half to the second family
			if (i % 2 == 0) {
				jobs[i] = new FamilyTestJob("TestFirstFamily", 1000000, 10, TestJobFamily.TYPE_ONE);
			} else {
				/*if(i%2 == 1)*/
				jobs[i] = new FamilyTestJob("TestSecondFamily", 1000000, 10, TestJobFamily.TYPE_TWO);
			}
			jobs[i].setRule(rule);
			jobs[i].schedule();
		}

		waitForStart(jobs[0]);

		assertState(jobs[0], Job.RUNNING);

		//first job is running, the rest are waiting
		for (int i = 1; i < NUM_JOBS; i++) {
			assertState(jobs[i], Job.WAITING, i);
		}

		//cancel the first family of jobs
		manager.cancel(first);
		waitForFamilyCancel(jobs, first);

		//the previously running job should have no state
		assertState(jobs[0], Job.NONE);
		//the first job from the second family should now be running
		waitForStart(jobs[1]);

		for (int i = 2; i < NUM_JOBS; i++) {
			//all other jobs in the first family should be removed from the waiting queue
			//no operations can be performed on these jobs until they are scheduled with the manager again
			if (jobs[i].belongsTo(first)) {
				assertState(jobs[i], Job.NONE, i);
				jobs[i].wakeUp();
				assertState(jobs[i], Job.NONE, i);
				jobs[i].sleep();
				assertState(jobs[i], Job.NONE, i);
			}
			//all other jobs in the second family should still be in the waiting queue
			else {
				assertState(jobs[i], Job.WAITING, i);
			}
		}

		for (int i = 2; i < NUM_JOBS; i++) {
			//all the jobs in the second family that are waiting to start can now be set to sleep
			if (jobs[i].belongsTo(second)) {
				assertState(jobs[i], Job.WAITING, i);
				assertTrue(jobs[i].sleep(), i + "");
				assertState(jobs[i], Job.SLEEPING, i);
			}
		}
		//cancel the second family of jobs
		manager.cancel(second);
		waitForFamilyCancel(jobs, second);

		//the second job should now have no state
		assertState(jobs[1], Job.NONE);

		for (int i = 0; i < NUM_JOBS; i++) {
			//all jobs should now be in the NONE state
			assertState(jobs[i], Job.NONE, i);
		}
	}

	@Test
	public void testJobFamilyFind() {
		//test of finding jobs based on the job family they belong to
		final int NUM_JOBS = 20;
		TestJob[] jobs = new TestJob[NUM_JOBS];
		//create five different families of jobs
		TestJobFamily first = new TestJobFamily(TestJobFamily.TYPE_ONE);
		TestJobFamily second = new TestJobFamily(TestJobFamily.TYPE_TWO);
		TestJobFamily third = new TestJobFamily(TestJobFamily.TYPE_THREE);
		TestJobFamily fourth = new TestJobFamily(TestJobFamily.TYPE_FOUR);
		TestJobFamily fifth = new TestJobFamily(TestJobFamily.TYPE_FIVE);

		//need a scheduling rule so that the jobs would be executed one by one
		ISchedulingRule rule = new IdentityRule();

		for (int i = 0; i < NUM_JOBS; i++) {
			//assign four jobs to each family
			switch (i % 5) {
			case 0:
				jobs[i] = new FamilyTestJob("TestFirstFamily", 1000000, 10, TestJobFamily.TYPE_ONE);
				break;
			case 1:
				jobs[i] = new FamilyTestJob("TestSecondFamily", 1000000, 10, TestJobFamily.TYPE_TWO);
				break;
			case 2:
				jobs[i] = new FamilyTestJob("TestThirdFamily", 1000000, 10, TestJobFamily.TYPE_THREE);
				break;
			case 3:
				jobs[i] = new FamilyTestJob("TestFourthFamily", 1000000, 10, TestJobFamily.TYPE_FOUR);
				break;
			default:
				/*if(i%5 == 4)*/
				jobs[i] = new FamilyTestJob("TestFifthFamily", 1000000, 10, TestJobFamily.TYPE_FIVE);
				break;
			}

			jobs[i].setRule(rule);
			jobs[i].schedule();
		}

		waitForStart(jobs[0]);

		//try finding all jobs by supplying the NULL parameter
		//note that this might find other jobs that are running as a side-effect of the test
		//suites running, such as snapshot
		HashSet<Job> allJobs = new HashSet<>(Arrays.asList(jobs));
		assertThat(manager.find(null)).hasSizeGreaterThanOrEqualTo(NUM_JOBS) //
				.filteredOn(job -> allJobs.remove(job)) // only test jobs that we know about
				.allMatch(job -> job.belongsTo(first) || job.belongsTo(second) || job.belongsTo(third)
						|| job.belongsTo(fourth) || job.belongsTo(fifth));
		assertThat(allJobs).isEmpty();

		//try finding all jobs from the first family
		assertThat(manager.find(first)).hasSize(4).allMatch(it -> it.belongsTo(first));

		//try finding all jobs from the second family
		assertThat(manager.find(second)).hasSize(4).allMatch(it -> it.belongsTo(second));

		// try finding all jobs from the third family
		assertThat(manager.find(third)).hasSize(4).allMatch(it -> it.belongsTo(third));

		// try finding all jobs from the fourth family
		assertThat(manager.find(fourth)).hasSize(4).allMatch(it -> it.belongsTo(fourth));

		// try finding all jobs from the fifth family
		assertThat(manager.find(fifth)).hasSize(4).allMatch(it -> it.belongsTo(fifth));

		// the first job should still be running
		assertState(jobs[0], Job.RUNNING);

		// put the second family of jobs to sleep
		manager.sleep(second);

		// cancel the first family of jobs
		manager.cancel(first);

		// the third job should start running
		waitForStart(jobs[2]);
		assertState(jobs[2], Job.RUNNING);

		// finding all jobs from the first family should return an empty array
		assertThat(manager.find(first)).isEmpty();

		// finding all jobs from the second family should return all the jobs (they are
		// just sleeping)
		assertThat(manager.find(second)).hasSize(4).allMatch(it -> it.belongsTo(second));

		// cancel the second family of jobs
		manager.cancel(second);
		// finding all jobs from the second family should now return an empty array
		assertThat(manager.find(second)).isEmpty();

		// cancel the fourth family of jobs
		manager.cancel(fourth);
		// finding all jobs from the fourth family should now return an empty array
		assertThat(manager.find(fourth)).isEmpty();

		// put the third family of jobs to sleep
		manager.sleep(third);
		// the first job from the third family should still be running
		assertState(jobs[2], Job.RUNNING);
		// wake up the last job from the third family
		jobs[NUM_JOBS - 3].wakeUp();
		// it should now be in the WAITING state
		assertState(jobs[NUM_JOBS - 3], Job.WAITING);

		// finding all jobs from the third family should return all 4 jobs (1 is
		// running, 1 is waiting, 2 are sleeping)
		assertThat(manager.find(third)).hasSize(4).allMatch(it -> it.belongsTo(third));

		// finding all jobs by supplying the NULL parameter should return 8 jobs (4 from
		// the 3rd family, and 4 from the 5th family)
		// note that this might find other jobs that are running as a side-effect of the
		// test suites running, such as snapshot
		allJobs.addAll(Arrays.asList(jobs));
		assertThat(manager.find(null)).hasSizeGreaterThanOrEqualTo(8) //
				.filteredOn(job -> allJobs.remove(job)) // only test jobs that we know about
				.allMatch(job -> job.belongsTo(third) || job.belongsTo(fifth));

		assertThat(allJobs).hasSize(12);
		allJobs.clear();

		// cancel the fifth family of jobs
		manager.cancel(fifth);
		// cancel the third family of jobs
		manager.cancel(third);
		waitForFamilyCancel(jobs, third);

		// all jobs should now be in the NONE state
		for (int i = 0; i < NUM_JOBS; i++) {
			assertState(jobs[i], Job.NONE, i);
		}

		// finding all jobs should return an empty array
		// note that this might find other jobs that are running as a side-effect of the
		// test suites running, such as snapshot
		allJobs.addAll(Arrays.asList(jobs));
		assertThat(manager.find(null)).hasSizeGreaterThanOrEqualTo(0);

		// test jobs that we know about should not be found (they should have all been
		// removed)
		assertThat(manager.find(null)).allMatch(job -> !allJobs.remove(job));
		assertThat(allJobs).hasSize(NUM_JOBS);
		allJobs.clear();
	}

	@Test
	public void testJobFamilyJoin() {
		//test the join method on a family of jobs
		final AtomicIntegerArray status = new AtomicIntegerArray(new int[1]);
		status.set(0, TestBarrier2.STATUS_WAIT_FOR_START);
		final int NUM_JOBS = 20;
		Job[] jobs = new Job[NUM_JOBS];
		//create two different families of jobs
		final TestJobFamily first = new TestJobFamily(TestJobFamily.TYPE_ONE);
		final TestJobFamily second = new TestJobFamily(TestJobFamily.TYPE_TWO);
		//need two scheduling rule so that jobs in each family would be executing one by one
		ISchedulingRule rule1 = new IdentityRule();
		ISchedulingRule rule2 = new IdentityRule();
		for (int i = 0; i < NUM_JOBS; i++) {
			//assign half the jobs to the first family, the other half to the second family
			if (i % 2 == 0) {
				jobs[i] = new FamilyTestJob("TestFirstFamily", 10, 1, TestJobFamily.TYPE_ONE);
				jobs[i].setRule(rule1);
				jobs[i].schedule(1000000);
			} else /*if(i%2 == 1)*/{
				jobs[i] = new FamilyTestJob("TestSecondFamily", 1000000, 1, TestJobFamily.TYPE_TWO);
				jobs[i].setRule(rule2);
				jobs[i].schedule();
			}

		}

		Thread t = new Thread(() -> {
			status.set(0, TestBarrier2.STATUS_START);
			try {
				TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_WAIT_FOR_RUN);
				status.set(0, TestBarrier2.STATUS_RUNNING);
				manager.join(first, null);
			} catch (OperationCanceledException | InterruptedException e) {
				// ignore
			}
			status.set(0, TestBarrier2.STATUS_DONE);
		});

		//start the thread that will join the first family of jobs and be blocked until they finish execution
		t.start();
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_START);
		status.set(0, TestBarrier2.STATUS_WAIT_FOR_RUN);
		//wake up the first family of jobs
		manager.wakeUp(first);

		int i = 0;
		for (; i < 10000; i++) {
			int currentStatus = status.get(0);
			Job[] result = manager.find(first);

			//if the thread is complete then all jobs must be done
			if (currentStatus == TestBarrier2.STATUS_DONE) {
				assertThat(result).as("failed in iteration " + i).isEmpty();
				break;
			}
			sleep(1);
		}
		assertThat(i).withFailMessage("did not timeout").isLessThan(10000);

		//cancel the second family of jobs
		manager.cancel(second);
		waitForFamilyCancel(jobs, second);

		//all the jobs should now be in the NONE state
		for (int j = 0; j < NUM_JOBS; j++) {
			assertState(jobs[j], Job.NONE, j);
		}
	}

	@Test
	public void testJobFamilyJoinCancelJobs() {
		//test the join method on a family of jobs, then cancel the jobs that are blocking the join call
		final AtomicIntegerArray status = new AtomicIntegerArray(new int[1]);
		status.set(0, TestBarrier2.STATUS_WAIT_FOR_START);
		final int NUM_JOBS = 20;
		TestJob[] jobs = new TestJob[NUM_JOBS];
		//create two different families of jobs
		final TestJobFamily first = new TestJobFamily(TestJobFamily.TYPE_ONE);
		final TestJobFamily second = new TestJobFamily(TestJobFamily.TYPE_TWO);
		//need two scheduling rule so that jobs in each family would be executing one by one
		ISchedulingRule rule1 = new IdentityRule();
		ISchedulingRule rule2 = new IdentityRule();
		for (int i = 0; i < NUM_JOBS; i++) {
			//assign half the jobs to the first family, the other half to the second family
			if (i % 2 == 0) {
				jobs[i] = new FamilyTestJob("TestFirstFamily", 1000000, 10, TestJobFamily.TYPE_ONE);
				jobs[i].setRule(rule1);
			} else /*if(i%2 == 1)*/{
				jobs[i] = new FamilyTestJob("TestSecondFamily", 1000000, 10, TestJobFamily.TYPE_TWO);
				jobs[i].setRule(rule2);
			}
			jobs[i].schedule();

		}

		Thread t = new Thread(() -> {
			status.set(0, TestBarrier2.STATUS_START);
			try {
				TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_WAIT_FOR_RUN);
				status.set(0, TestBarrier2.STATUS_RUNNING);
				manager.join(first, null);
			} catch (OperationCanceledException | InterruptedException e) {
				// ignore
			}
			status.set(0, TestBarrier2.STATUS_DONE);
		});

		//start the thread that will join the first family of jobs
		//it will be blocked until the all jobs in the first family finish execution or are canceled
		t.start();
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_START);
		status.set(0, TestBarrier2.STATUS_WAIT_FOR_RUN);
		waitForStart(jobs[0]);
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_RUNNING);

		assertState(jobs[0], Job.RUNNING);
		assertEquals(TestBarrier2.STATUS_RUNNING, status.get(0));

		//cancel the first family of jobs
		//the join call should be unblocked when all the jobs are canceled
		manager.cancel(first);
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_DONE);

		//all jobs in the first family should be removed from the manager
		assertThat(manager.find(first)).isEmpty();

		//cancel the second family of jobs
		manager.cancel(second);
		waitForFamilyCancel(jobs, second);

		//all the jobs should now be in the NONE state
		for (int j = 0; j < NUM_JOBS; j++) {
			assertState(jobs[j], Job.NONE, j);
		}
	}

	@Test
	public void testJobFamilyJoinCancelManager() {
		//test the join method on a family of jobs, then cancel the call
		final AtomicIntegerArray status = new AtomicIntegerArray(new int[1]);
		status.set(0, TestBarrier2.STATUS_WAIT_FOR_START);
		final int NUM_JOBS = 20;
		TestJob[] jobs = new TestJob[NUM_JOBS];
		//create a progress monitor to cancel the join call
		final IProgressMonitor canceller = new FussyProgressMonitor();
		//create two different families of jobs
		final TestJobFamily first = new TestJobFamily(TestJobFamily.TYPE_ONE);
		final TestJobFamily second = new TestJobFamily(TestJobFamily.TYPE_TWO);
		//need two scheduling rule so that jobs in each family would be executing one by one
		ISchedulingRule rule1 = new IdentityRule();
		ISchedulingRule rule2 = new IdentityRule();
		for (int i = 0; i < NUM_JOBS; i++) {
			//assign half the jobs to the first family, the other half to the second family
			if (i % 2 == 0) {
				jobs[i] = new FamilyTestJob("TestFirstFamily", 1000000, 10, TestJobFamily.TYPE_ONE);
				jobs[i].setRule(rule1);
			} else /*if(i%2 == 1)*/{
				jobs[i] = new FamilyTestJob("TestSecondFamily", 1000000, 10, TestJobFamily.TYPE_TWO);
				jobs[i].setRule(rule2);
			}
			jobs[i].schedule();

		}

		Thread t = new Thread(() -> {
			status.set(0, TestBarrier2.STATUS_START);
			try {
				TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_WAIT_FOR_RUN);
				status.set(0, TestBarrier2.STATUS_RUNNING);
				manager.join(first, canceller);
			} catch (OperationCanceledException | InterruptedException e) {
				// ignore
			}
			status.set(0, TestBarrier2.STATUS_DONE);
		});

		//start the thread that will join the first family of jobs
		//it will be blocked until the cancel call is made to the thread
		t.start();
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_START);
		status.set(0, TestBarrier2.STATUS_WAIT_FOR_RUN);
		waitForStart(jobs[0]);
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_RUNNING);

		assertState(jobs[0], Job.RUNNING);
		assertEquals(TestBarrier2.STATUS_RUNNING, status.get(0));

		//cancel the monitor that is attached to the join call
		canceller.setCanceled(true);
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_DONE);

		//the first job in the first family should still be running
		assertState(jobs[0], Job.RUNNING);
		assertEquals(TestBarrier2.STATUS_DONE, status.get(0));
		assertThat(manager.find(first)).isNotEmpty();

		//cancel the second family of jobs
		manager.cancel(second);
		waitForFamilyCancel(jobs, second);

		//cancel the first family of jobs
		manager.cancel(first);
		waitForFamilyCancel(jobs, first);

		//all the jobs should now be in the NONE state
		for (int j = 0; j < NUM_JOBS; j++) {
			assertState(jobs[j], Job.NONE, j);
		}
	}

	/**
	 * Asserts that the LockListener is called correctly during invocation of
	 * {@link IJobManager#join(Object, IProgressMonitor)}. See bug
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=195839.
	 */
	@Test
	public void testJobFamilyJoinLockListener() throws OperationCanceledException, InterruptedException {
		final TestJobFamily family = new TestJobFamily(TestJobFamily.TYPE_ONE);
		int count = 5;
		TestJob[] jobs = new TestJob[count];
		for (int i = 0; i < jobs.length; i++) {
			jobs[i] = new FamilyTestJob("TestJobFamilyJoinLockListener" + i, 100000, 10, family.getType());
			jobs[i].schedule();
		}
		TestLockListener lockListener = new TestLockListener(() -> Stream.of(jobs).forEach(TestJob::terminate));
		try {
			manager.setLockListener(lockListener);
			manager.join(family, new FussyProgressMonitor());
		} finally {
			manager.setLockListener(null);
		}
		lockListener.assertHasBeenWaiting("JobManager has not been waiting for lock");
		lockListener.assertNotWaiting("JobManager has not finished waiting for lock");
	}

	@Test
	public void testJobFamilyJoinNothing() throws OperationCanceledException, InterruptedException {
		//test joining a bogus family, and the monitor should be used up
		final FussyProgressMonitor monitor = new FussyProgressMonitor();
		monitor.prepare();
		manager.join(new Object(), monitor);
		monitor.sanityCheck();
		monitor.assertUsedUp();
	}

	/**
	 * Tests joining a job that repeats in a loop
	 */
	@Test
	public void testJobFamilyJoinRepeating() throws OperationCanceledException, InterruptedException {
		Object family = new Object();
		int count = 25;
		RepeatingJob job = new RepeatingJob("testJobFamilyJoinRepeating", count);
		job.setFamily(family);
		job.schedule();
		Job.getJobManager().join(family, null);
		//ensure the job has run the expected number of times
		assertEquals(count, job.getRunCount());
	}

	/**
	 * Tests joining a job family that repeats but returns false to shouldSchedule
	 */
	@Test
	public void testJobFamilyJoinShouldSchedule() throws OperationCanceledException, InterruptedException {
		Object family = new Object();
		final int count = 1;
		RepeatingJob job = new RepeatingJob("testJobFamilyJoinShouldSchedule", count) {
			@Override
			public boolean shouldSchedule() {
				return shouldRun();
			}
		};
		job.setFamily(family);
		job.schedule();
		Job.getJobManager().join(family, null);
		//ensure the job has run the expected number of times
		assertEquals(count, job.getRunCount());
	}

	/**
	 * Tests simple usage of the IJobManager.join() method.
	 */
	@Test
	public void testJobFamilyJoinSimple() {
		//test the join method on a family of jobs that is empty
		final AtomicIntegerArray status = new AtomicIntegerArray(new int[1]);
		status.set(0, TestBarrier2.STATUS_WAIT_FOR_START);
		final int NUM_JOBS = 20;
		TestJob[] jobs = new TestJob[NUM_JOBS];
		//create three different families of jobs
		final TestJobFamily first = new TestJobFamily(TestJobFamily.TYPE_ONE);
		final TestJobFamily second = new TestJobFamily(TestJobFamily.TYPE_TWO);
		final TestJobFamily third = new TestJobFamily(TestJobFamily.TYPE_THREE);
		//need two scheduling rule so that jobs in each family would be executing one by one
		ISchedulingRule rule1 = new IdentityRule();
		ISchedulingRule rule2 = new IdentityRule();
		for (int i = 0; i < NUM_JOBS; i++) {
			//assign half the jobs to the first family, the other half to the second family
			if (i % 2 == 0) {
				jobs[i] = new FamilyTestJob("TestFirstFamily", 1000000, 10, TestJobFamily.TYPE_ONE);
				jobs[i].setRule(rule1);
			} else /*if(i%2 == 1)*/{
				jobs[i] = new FamilyTestJob("TestSecondFamily", 1000000, 10, TestJobFamily.TYPE_TWO);
				jobs[i].setRule(rule2);
			}

			jobs[i].schedule();
		}

		Thread t = new Thread(() -> {
			status.set(0, TestBarrier2.STATUS_START);
			try {
				TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_WAIT_FOR_RUN);
				status.set(0, TestBarrier2.STATUS_RUNNING);
				manager.join(third, null);
			} catch (OperationCanceledException | InterruptedException e) {
				// ignore
			}
			status.set(0, TestBarrier2.STATUS_DONE);
		});

		//try joining the third family of jobs, which is empty
		//join method should return without blocking
		waitForStart(jobs[0]);
		t.start();

		//let the thread execute the join call
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_START);
		assertEquals(TestBarrier2.STATUS_START, status.get(0));
		long startTime = now();
		status.set(0, TestBarrier2.STATUS_WAIT_FOR_RUN);
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_DONE);
		long endTime = now();

		assertEquals(TestBarrier2.STATUS_DONE, status.get(0));
		assertTrue(endTime >= startTime); // XXX this tests makes no sense. now() is guaranteed to be >= anyway.
													// and the expectation is that it takes NO time anyway... see next
													// comment

		//the join call should take no actual time (join call should not block thread at all)
		if (PEDANTIC) {
			assertTrue((endTime - startTime) < 300, "start time: " + startTime + " end time: " + endTime);
		}

		//cancel all jobs
		manager.cancel(first);
		manager.cancel(second);
		waitForFamilyCancel(jobs, first);
		waitForFamilyCancel(jobs, second);

		//all the jobs should now be in the NONE state
		for (int j = 0; j < NUM_JOBS; j++) {
			assertState(jobs[j], Job.NONE, j);
		}
	}

	/**
	 * Tests scenario 1 described in https://bugs.eclipse.org/bugs/show_bug.cgi?id=403271#c0:
	 *  - join is called when job manager is suspended
	 *  - waiting job is scheduled when job manager is suspended
	 * In this scenario main job should not wait for the waiting job.
	 */
	@Test
	public void testJobFamilyJoinWhenSuspended_1() throws InterruptedException {
		final Object family = new TestJobFamily(TestJobFamily.TYPE_ONE);
		final int[] familyJobsCount = new int[] {-1};
		final TestBarrier2 barrier = new TestBarrier2();
		final TestJob waiting = new FamilyTestJob("waiting job", 1000000, 10, TestJobFamily.TYPE_ONE);
		final TestJob running = new FamilyTestJob("running job", 1000000, 10, TestJobFamily.TYPE_ONE);
		final IJobChangeListener listener = new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				if (event.getJob() == running) {
					barrier.waitForStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);
				}
			}

			@Override
			public void running(IJobChangeEvent event) {
				if (event.getJob() == running) {
					barrier.setStatus(TestBarrier2.STATUS_RUNNING);
				}
			}
		};
		Job job = new Job("main job") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					manager.addJobChangeListener(listener);
					running.schedule();
					// wait until running job is actually running
					barrier.waitForStatus(TestBarrier2.STATUS_RUNNING);
					manager.setLockListener(new LockListener() {
						private final boolean scheduled = false;

						@Override
						public boolean aboutToWait(Thread lockOwner) {
							// aboutToWait will be called when main job will start joining the running job
							if (!scheduled) {
								running.terminate();
								waiting.schedule();
								barrier.setStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);
							}
							return super.aboutToWait(lockOwner);
						}
					});
					// suspend before join
					manager.suspend();
					manager.join(family, null);
					familyJobsCount[0] = manager.find(family).length;
					barrier.setStatus(TestBarrier2.STATUS_DONE);
				} catch (InterruptedException e) {
					// ignore
				} finally {
					// clean up
					manager.removeJobChangeListener(listener);
					manager.setLockListener(null);
					running.cancel();
					waiting.cancel();
					try {
						running.join();
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
			assertEquals(1, familyJobsCount[0]);
		} catch (AssertionError e) {
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
	 * Tests scenario 2 - verifies if the suspended flag is checked each time a job is scheduled:
	 *  - join is called when job manager is NOT suspended
	 *  - waiting job is scheduled when job manager is suspended
	 * In this scenario main job should not wait for the waiting job.
	 */
	@Test
	public void testJobFamilyJoinWhenSuspended_2() throws InterruptedException {
		final Object family = new TestJobFamily(TestJobFamily.TYPE_ONE);
		final int[] familyJobsCount = new int[] {-1};
		final TestBarrier2 barrier = new TestBarrier2();
		final TestJob waiting = new FamilyTestJob("waiting job", 1000000, 10, TestJobFamily.TYPE_ONE);
		final TestJob running = new FamilyTestJob("running job", 1000000, 10, TestJobFamily.TYPE_ONE);
		final IJobChangeListener listener = new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				if (event.getJob() == running) {
					barrier.waitForStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);
				}
			}

			@Override
			public void running(IJobChangeEvent event) {
				if (event.getJob() == running) {
					barrier.setStatus(TestBarrier2.STATUS_RUNNING);
				}
			}
		};
		Job job = new Job("main job") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					manager.addJobChangeListener(listener);
					running.schedule();
					// wait until running job is actually running
					barrier.waitForStatus(TestBarrier2.STATUS_RUNNING);
					manager.setLockListener(new LockListener() {
						private final boolean scheduled = false;

						@Override
						public boolean aboutToWait(Thread lockOwner) {
							// aboutToWait will be called when main job will start joining the running job
							if (!scheduled) {
								// suspend before scheduling new job
								getJobManager().suspend();
								running.terminate();
								waiting.schedule();
								barrier.setStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);
							}
							return super.aboutToWait(lockOwner);
						}
					});
					manager.join(family, null);
					familyJobsCount[0] = manager.find(family).length;
					barrier.setStatus(TestBarrier2.STATUS_DONE);
				} catch (InterruptedException e) {
					// ignore
				} finally {
					// clean up
					manager.removeJobChangeListener(listener);
					manager.setLockListener(null);
					running.cancel();
					waiting.cancel();
					try {
						running.join();
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
			assertEquals(1, familyJobsCount[0]);
		} catch (AssertionError e) {
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
	 * Tests scenario 3:
	 *  - join is called when job manager is NOT suspended
	 *  - waiting job is scheduled when job manager is suspended
	 *  - job manager is resumed causing waiting job to start
	 * In this scenario main thread should wait for the waiting job since the job was started before the join ended.
	 */
	@Test
	public void testJobFamilyJoinWhenSuspended_3() throws InterruptedException {
		final Object family = new TestJobFamily(TestJobFamily.TYPE_ONE);
		final TestBarrier2 barrier = new TestBarrier2();
		final TestJob waiting = new FamilyTestJob("waiting job", 1000000, 10, TestJobFamily.TYPE_ONE);
		final TestJob running = new FamilyTestJob("running job", 1000000, 10, TestJobFamily.TYPE_ONE);
		final IJobChangeListener listener = new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				if (event.getJob() == running) {
					barrier.waitForStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);
				}
			}

			@Override
			public void running(IJobChangeEvent event) {
				if (event.getJob() == running) {
					barrier.setStatus(TestBarrier2.STATUS_RUNNING);
				} else if (event.getJob() == waiting) {
					barrier.setStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);
				}
			}
		};
		try {
			manager.addJobChangeListener(listener);
			running.schedule();
			// wait until the running job is actually running
			barrier.waitForStatus(TestBarrier2.STATUS_RUNNING);
			manager.setLockListener(new LockListener() {
				private boolean scheduled = false;

				@Override
				public boolean aboutToWait(Thread lockOwner) {
					// aboutToWait will be called when main thread will start joining the running job
					if (!scheduled) {
						// suspend before scheduling the waiting job
						manager.suspend();
						waiting.schedule();
						// resume to start the waiting job
						manager.resume();
						running.terminate();
						waiting.terminate();
						scheduled = true;
					}
					return super.aboutToWait(lockOwner);
				}
			});
			manager.join(family, null);
			assertThat(manager.find(family)).isEmpty();
		} finally {
			// clean up
			manager.removeJobChangeListener(listener);
			manager.setLockListener(null);
			running.cancel();
			waiting.cancel();
			running.join();
			waiting.join();
			manager.resume();
		}
	}

	@Test
	public void testJobFamilyNULL() {
		//test methods that accept the null job family (i.e. all jobs)
		final int NUM_JOBS = 20;
		TestJob[] jobs = new TestJob[NUM_JOBS];
		//create two different families of jobs
		TestJobFamily first = new TestJobFamily(TestJobFamily.TYPE_ONE);
		TestJobFamily second = new TestJobFamily(TestJobFamily.TYPE_TWO);
		//need one common scheduling rule so that the jobs would be executed one by one
		ISchedulingRule rule = new IdentityRule();
		for (int i = 0; i < NUM_JOBS; i++) {
			//assign half the jobs to the first family, the other half to the second family
			if (i % 2 == 0) {
				jobs[i] = new FamilyTestJob("TestFirstFamily", 1000000, 10, TestJobFamily.TYPE_ONE);
			} else {
				/*if(i%2 == 1)*/
				jobs[i] = new FamilyTestJob("TestSecondFamily", 1000000, 10, TestJobFamily.TYPE_TWO);
			}

			jobs[i].setRule(rule);
			jobs[i].schedule();
		}

		waitForStart(jobs[0]);
		assertState(jobs[0], Job.RUNNING);

		//put all jobs to sleep
		manager.sleep(null);
		//the first job should still be running
		assertState(jobs[0], Job.RUNNING);

		//all the other jobs should be sleeping
		for (int i = 1; i < NUM_JOBS; i++) {
			assertState(jobs[i], Job.SLEEPING, i);
		}

		//wake up all the jobs
		manager.wakeUp(null);
		//the first job should still be running
		assertState(jobs[0], Job.RUNNING);

		//all the other jobs should be waiting
		for (int i = 1; i < NUM_JOBS; i++) {
			assertState(jobs[i], Job.WAITING, i);
		}

		//cancel all the jobs
		manager.cancel(first);
		manager.cancel(second);
		waitForFamilyCancel(jobs, first);
		waitForFamilyCancel(jobs, second);

		//all the jobs should now be in the NONE state
		for (int i = 0; i < NUM_JOBS; i++) {
			assertState(jobs[i], Job.NONE, i);
		}
	}

	@Test
	public void testJobFamilySleep() {
		//test the sleep method on a family of jobs
		final int NUM_JOBS = 20;
		TestJob[] jobs = new TestJob[NUM_JOBS];
		//create two different families of jobs
		TestJobFamily first = new TestJobFamily(TestJobFamily.TYPE_ONE);
		TestJobFamily second = new TestJobFamily(TestJobFamily.TYPE_TWO);
		//need a common scheduling rule so that the jobs would be executed one by one
		ISchedulingRule rule = new IdentityRule();
		for (int i = 0; i < NUM_JOBS; i++) {
			//assign half the jobs to the first family, the other half to the second family
			if (i % 2 == 0) {
				jobs[i] = new FamilyTestJob("TestFirstFamily", 1000000, 10, TestJobFamily.TYPE_ONE);
			} else {
				/*if(i%2 == 1)*/
				jobs[i] = new FamilyTestJob("TestSecondFamily", 1000000, 10, TestJobFamily.TYPE_TWO);
			}

			jobs[i].setRule(rule);
			jobs[i].schedule();
		}

		waitForStart(jobs[0]);

		assertState(jobs[0], Job.RUNNING);

		//first job is running, the rest are waiting
		for (int i = 1; i < NUM_JOBS; i++) {
			assertState(jobs[i], Job.WAITING, i);
		}

		//set the first family of jobs to sleep
		manager.sleep(first);

		//the running job should still be running
		assertState(jobs[0], Job.RUNNING);

		for (int i = 1; i < NUM_JOBS; i++) {
			//all other jobs in the first family should be sleeping
			//they can now be canceled
			if (jobs[i].belongsTo(first)) {
				assertState(jobs[i], Job.SLEEPING, i);
				jobs[i].cancel();
			}
			//all jobs in the second family should still be in the waiting queue
			else {
				assertState(jobs[i], Job.WAITING, i);
			}
		}

		manager.sleep(second);
		//cancel the running job
		jobs[0].cancel();
		waitForCancel(jobs[0]);

		//no job should now be running
		assertNull(manager.currentJob());

		for (int i = 1; i < NUM_JOBS; i++) {
			//all other jobs in the second family should be sleeping
			//they can now be canceled
			if (jobs[i].belongsTo(second)) {
				assertState(jobs[i], Job.SLEEPING, i);
				jobs[i].cancel();
			}
		}

		//all the jobs should now be in the NONE state
		for (int i = 0; i < NUM_JOBS; i++) {
			assertState(jobs[i], Job.NONE, i);
		}
	}

	/**
	 * Tests the API method IJobManager.wakeUp(family)
	 */
	@Test
	public void testJobFamilyWakeUp() {
		final int JOBS_PER_FAMILY = 10;
		//create two different families of jobs
		Job[] family1 = new Job[JOBS_PER_FAMILY];
		Job[] family2 = new Job[JOBS_PER_FAMILY];
		TestJobFamily first = new TestJobFamily(TestJobFamily.TYPE_ONE);
		TestJobFamily second = new TestJobFamily(TestJobFamily.TYPE_TWO);
		//need one common scheduling rule so that the jobs would be executed one by one
		ISchedulingRule rule = new IdentityRule();
		//create and schedule a seed job that will cause all others to be blocked
		TestJob seedJob = new FamilyTestJob("SeedJob", 1000000, 1, TestJobFamily.TYPE_THREE);
		seedJob.setRule(rule);
		seedJob.schedule();
		waitForStart(seedJob);
		assertState(seedJob, Job.RUNNING);

		//create jobs in first family and put them to sleep
		for (int i = 0; i < JOBS_PER_FAMILY; i++) {
			family1[i] = new FamilyTestJob("TestFirstFamily", 1000000, 1, TestJobFamily.TYPE_ONE);
			family1[i].setRule(rule);
			family1[i].schedule();
			assertState(family1[i], Job.WAITING, i);
			assertTrue(family1[i].sleep(), i + "");
			assertState(family1[i], Job.SLEEPING, i);
		}
		//create jobs in second family and put them to sleep
		for (int i = 0; i < JOBS_PER_FAMILY; i++) {
			family2[i] = new FamilyTestJob("TestSecondFamily", 1000000, 1, TestJobFamily.TYPE_TWO);
			family2[i].setRule(rule);
			family2[i].schedule();
			assertState(family2[i], Job.WAITING, i);
			assertTrue(family2[i].sleep(), i + "");
			assertState(family2[i], Job.SLEEPING, i);
		}

		//cancel the seed job
		seedJob.cancel();
		waitForCancel(seedJob);
		assertState(seedJob, Job.NONE);

		//all family jobs should still be sleeping
		for (int i = 0; i < JOBS_PER_FAMILY; i++) {
			assertState(family1[i], Job.SLEEPING, i);
		}
		for (int i = 0; i < JOBS_PER_FAMILY; i++) {
			assertState(family2[i], Job.SLEEPING, i);
		}

		//wake-up the second family of jobs
		manager.wakeUp(second);

		//jobs in the first family should still be in the sleep state
		for (int i = 0; i < JOBS_PER_FAMILY; i++) {
			assertState(family1[i], Job.SLEEPING, i);
		}
		//ensure all jobs in second family are either running or waiting
		int runningCount = 0;
		for (int i = 0; i < JOBS_PER_FAMILY; i++) {
			int state = family2[i].getState();
			if (state == Job.RUNNING) {
				runningCount++;
			} else if (state != Job.WAITING) {
				fail(i + ": expected state: " + printState(Job.WAITING) + " actual state: " + printState(state));
			}
		}
		//ensure only one job is running (it is possible that none have started yet)
		assertTrue(runningCount <= 1);

		//cycle through the jobs in the second family and cancel them
		for (int i = 0; i < JOBS_PER_FAMILY; i++) {
			//the running job may not respond immediately
			if (!family2[i].cancel()) {
				waitForCancel(family2[i]);
			}
			assertState(family2[i], Job.NONE, i);
		}

		//all jobs in the first family should still be sleeping
		for (int i = 0; i < JOBS_PER_FAMILY; i++) {
			assertState(family1[i], Job.SLEEPING, i);
		}

		//wake up the first family
		manager.wakeUp(first);

		//ensure all jobs in first family are either running or waiting
		runningCount = 0;
		for (int i = 0; i < JOBS_PER_FAMILY; i++) {
			int state = family1[i].getState();
			if (state == Job.RUNNING) {
				runningCount++;
			} else if (state != Job.WAITING) {
				fail(i + ": expected state: " + printState(Job.WAITING) + " actual state: " + printState(state));
			}
		}
		//ensure only one job is running (it is possible that none have started yet)
		assertTrue(runningCount <= 1);

		//cycle through the jobs in the first family and cancel them
		for (int i = 0; i < JOBS_PER_FAMILY; i++) {
			//the running job may not respond immediately
			if (!family1[i].cancel()) {
				waitForCancel(family1[i]);
			}
			assertState(family1[i], Job.NONE, i);
		}

		//all jobs should now be in the NONE state
		for (int i = 0; i < JOBS_PER_FAMILY; i++) {
			assertState(family1[i], Job.NONE, i);
		}
		for (int i = 0; i < JOBS_PER_FAMILY; i++) {
			assertState(family2[i], Job.NONE, i);
		}
	}

	@Test
	public void testMutexRule() {
		final int JOB_COUNT = 10;
		TestJob[] jobs = new TestJob[JOB_COUNT];
		ISchedulingRule mutex = new IdentityRule();
		for (int i = 0; i < JOB_COUNT; i++) {
			jobs[i] = new TestJob("testMutexRule", 1000000, 1);
			jobs[i].setRule(mutex);
			jobs[i].schedule();
		}
		//first job should be running, all others should be waiting
		waitForStart(jobs[0]);
		assertState(jobs[0], Job.RUNNING);
		for (int i = 1; i < JOB_COUNT; i++) {
			assertState(jobs[i], Job.WAITING, i);
		}
		//cancel job i, then i+1 should run and all others should wait
		for (int i = 0; i < JOB_COUNT - 1; i++) {
			jobs[i].cancel();
			waitForStart(jobs[i + 1]);
			assertState(jobs[i + 1], Job.RUNNING, i);
			for (int j = i + 2; j < JOB_COUNT; j++) {
				assertState(jobs[j], Job.WAITING, i * JOB_COUNT + j);
			}
		}
		//cancel the final job
		jobs[JOB_COUNT - 1].cancel();
	}

	@Test
	public void testOrder() throws Exception {
		// ensure jobs are run in order from lowest to highest sleep time.
		int[] sleepTimes = new int[] { 0, 1, 2, 5, 10, 15, 25, 50 };
		final LinkedList<Job> allJobs = new LinkedList<>();
		final List<Job> jobsRunningBeforePrevious = synchronizedList(new ArrayList<>());

		for (int sleepTime : sleepTimes) {
			final Job previouslyScheduledJob = allJobs.isEmpty() ? null : allJobs.getLast();
			Job currentJob = new Job("testOrder job to be run with sleep time " + sleepTime) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					if (!hasPreviousJobStartedRunning()) {
						jobsRunningBeforePrevious.add(this);
					}
					return Status.OK_STATUS;
				}

				private boolean hasPreviousJobStartedRunning() {
					return previouslyScheduledJob == null || previouslyScheduledJob.getState() == Job.RUNNING
							|| previouslyScheduledJob.getState() == Job.NONE;
				}
			};
			currentJob.schedule(sleepTime);
			allJobs.add(currentJob);
		}
		for (Job job : allJobs) {
			job.join();
		}

		assertThat(jobsRunningBeforePrevious).as("job started running before a previously scheduled one").isEmpty();
	}

	@Test
	public void testReverseOrder() throws InterruptedException {
		// ensure that a job does not wait for one that is scheduled with a high delay
		Job directlyExecutedJob = new TestJob("Directly executed job", 0, 0);
		Job delayedJob = new TestJob("Delayed job", 0, 0);
		long delayInSeconds = 20;
		delayedJob.schedule(delayInSeconds * 1000);
		directlyExecutedJob.schedule(1);
		directlyExecutedJob.join(delayInSeconds * 1000 / 2, null);
		int delayedJobState = delayedJob.getState();
		delayedJob.cancel();
		assertThat(delayedJobState).as("state of delayed job that should be waiting").isEqualTo(Job.SLEEPING);
	}

	/**
	 * Tests conditions where there is a race to schedule the same job multiple
	 * times.
	 *
	 * @throws InterruptedException
	 */
	@Test
	public void testScheduleRace() throws InterruptedException {
		final int[] count = new int[1];
		final boolean[] running = new boolean[] {false};
		final boolean[] failure = new boolean[] {false};
		final Job testJob = new Job("testScheduleRace") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					synchronized (running) {
						//indicate job is running, and assert the job is not already running
						if (running[0]) {
							failure[0] = true;
						} else {
							running[0] = true;
						}
					}
					//sleep for awhile to let duplicate job start running
					Thread.sleep(100);
				} catch (InterruptedException e) {
					//ignore
				} finally {
					synchronized (running) {
						running[0] = false;
					}
				}
				return Status.OK_STATUS;
			}
		};
		testJob.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void scheduled(IJobChangeEvent event) {
				while (count[0]++ < 2) {
					testJob.schedule();
				}
			}
		});
		testJob.schedule();
		testJob.join();
		waitForCompletion(testJob, Duration.ofSeconds(5));
		assertFalse(failure[0]);
	}

	@Test
	public void testSimple() {
		final int JOB_COUNT = 10;
		for (int i = 0; i < JOB_COUNT; i++) {
			new TestJob("testSimple", 1, 1).schedule();
		}
		waitForCompletion();
		//
		for (int i = 0; i < JOB_COUNT; i++) {
			new TestJob("testSimple", 1, 1).schedule(50);
		}
		waitForCompletion();
	}

	/**
	 * Tests setting various kinds of invalid rules on jobs.
	 */
	@Test
	public void testSetInvalidRule() {
		class InvalidRule implements ISchedulingRule {
			@Override
			public boolean isConflicting(ISchedulingRule rule) {
				return false;
			}

			@Override
			public boolean contains(ISchedulingRule rule) {
				return false;
			}
		}

		InvalidRule rule1 = new InvalidRule();
		InvalidRule rule2 = new InvalidRule();
		ISchedulingRule multi = MultiRule.combine(rule1, rule2);

		Job job = new Job("job with invalid rule") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				return Status.OK_STATUS;
			}
		};

		assertThrows(IllegalArgumentException.class, () -> job.setRule(rule1));
		assertThrows(IllegalArgumentException.class, () -> job.setRule(rule2));
		assertThrows(IllegalArgumentException.class, () -> job.setRule(multi));
	}

	@Test
	public void testSleep() {
		TestJob job = new TestJob("ParentJob", 1000000, 10);
		//sleeping a job that isn't scheduled should have no effect
		assertEquals(Job.NONE, job.getState());
		assertTrue(job.sleep());
		assertEquals(Job.NONE, job.getState());

		//sleeping a job that is already running should not work
		job.schedule();
		//give the job a chance to start
		waitForStart(job);
		assertState(job, Job.RUNNING);
		assertFalse(job.sleep());
		assertState(job, Job.RUNNING);
		job.terminate();
		waitForCompletion();

		//sleeping a job that is already sleeping should make sure it never runs
		job.schedule(10000);
		assertState(job, Job.SLEEPING);
		assertTrue(job.sleep());
		assertState(job, Job.SLEEPING);
		//wait awhile and ensure the job is still sleeping
		Thread.yield();
		sleep(60);
		Thread.yield();
		assertState(job, Job.SLEEPING);
		assertTrue(job.cancel()); // should be possible to cancel a sleeping job
	}

	@Test
	public void testSleepOnWait() {
		final ISchedulingRule rule = new PathRule("testSleepOnWait");
		TestJob blockingJob = new TestJob("Long Job", 1000000, 10);
		blockingJob.setRule(rule);
		blockingJob.schedule();

		TestJob job = new TestJob("Long Job", 1000000, 10);
		job.setRule(rule);
		job.schedule();
		//we know this job is waiting, so putting it to sleep should prevent it from running
		assertState(job, Job.WAITING);
		assertTrue(job.sleep());
		assertState(job, Job.SLEEPING);

		//cancel the blocking job, thus freeing the pool for the waiting job
		blockingJob.cancel();

		//make sure the job is still sleeping
		assertState(job, Job.SLEEPING);

		//now wake the job up
		job.wakeUp();
		waitForStart(job);
		assertState(job, Job.RUNNING);

		//finally cancel the job
		job.cancel();
		waitForCompletion(job);
	}

	@Test
	public void testSuspend() {
		assertFalse(manager.isSuspended());
		manager.suspend();
		try {
			assertTrue(manager.isSuspended());
		} finally {
			manager.resume();
		}
		assertFalse(manager.isSuspended());
	}

	/**
	 * Tests the following sequence:
	 * [Thread[main,6,main]]Suspend rule: R/
	 * [Thread[main,6,main]]Begin rule: R/
	 * [Thread[Worker-3,5,main]]Begin rule: L/JUnit/junit/tests/framework/Failure.java
	 * [Thread[main,6,main]]End rule: R/
	 * [Thread[main,6,main]]Resume rule: R/
	 * [Thread[Worker-3,5,main]]End rule: L/JUnit/junit/tests/framework/Failure.java
	 * @deprecated tests deprecated API
	 */
	@Deprecated
	@Test
	public void testSuspendMismatchedBegins() {
		PathRule rule1 = new PathRule("/TestSuspendMismatchedBegins");
		PathRule rule2 = new PathRule("/TestSuspendMismatchedBegins/Child");
		manager.suspend(rule1, null);

		//start a job that acquires a child rule
		TestBarrier2 barrier = new TestBarrier2();
		JobRuleRunner runner = new JobRuleRunner("TestSuspendJob", rule2, barrier, 1, true);
		runner.schedule();
		barrier.waitForStatus(TestBarrier2.STATUS_START);
		//let the job start the rule
		barrier.setStatus(TestBarrier2.STATUS_WAIT_FOR_RUN);
		barrier.waitForStatus(TestBarrier2.STATUS_RUNNING);

		//now try to resume the rule in this thread
		manager.resume(rule1);

		//finally let the test runner resume the rule
		barrier.setStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);
		barrier.waitForStatus(TestBarrier2.STATUS_DONE);
		waitForCompletion(runner);
	}

	/**
	 * Tests IJobManager suspend and resume API
	 * @deprecated tests deprecated API
	 */
	@Deprecated
	@Test
	public void testSuspendMultiThreadAccess() {
		PathRule rule1 = new PathRule("/TestSuspend");
		PathRule rule2 = new PathRule("/TestSuspend/Child");
		manager.suspend(rule1, null);

		//should not be able to run a job that uses the rule
		Job job = new Job("TestSuspend") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				return Status.OK_STATUS;
			}
		};
		job.setRule(rule1);
		job.schedule();
		//give the job a chance to run
		sleep(200);
		assertNull(job.getResult());

		//should be able to run a thread that begins the rule
		AtomicIntegerArray status = new AtomicIntegerArray(new int[1]);
		SimpleRuleRunner runner = new SimpleRuleRunner(rule1, status, null);
		new Thread(runner).start();
		TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_DONE);

		//should be able to run a thread that begins a conflicting rule
		status.set(0, 0);
		runner = new SimpleRuleRunner(rule2, status, null);
		new Thread(runner).start();
		TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_DONE);

		//now begin the rule in this thread
		manager.beginRule(rule1, null);

		//should still be able to run a thread that begins the rule
		status.set(0, 0);
		runner = new SimpleRuleRunner(rule1, status, null);
		new Thread(runner).start();
		TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_DONE);

		//our job should still not have executed
		sleep(100);
		assertNull(job.getResult());

		//even ending the rule in this thread should not allow the job to continue
		manager.endRule(rule1);
		sleep(100);
		assertNull(job.getResult());

		//should still be able to run a thread that begins the rule
		status.set(0, 0);
		runner = new SimpleRuleRunner(rule1, status, null);
		new Thread(runner).start();
		TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_DONE);

		//finally resume the rule in this thread
		manager.resume(rule1);

		//job should now complete
		waitForCompletion(job);
	}

	/**
	 * Tests IJobManager#transfer(ISchedulingRule, Thread) failure conditions.
	 */
	@Test
	public void testTransferFailure() throws InterruptedException {
		PathRule rule = new PathRule("/testTransferFailure");
		PathRule subRule = new PathRule("/testTransferFailure/Sub");
		Thread other = new Thread();
		//can't transfer a rule this thread doesn't own it
		assertThrows(RuntimeException.class, () -> manager.transferRule(rule, other));
		try {
			manager.beginRule(rule, null);
			//can't transfer a child rule of a rule currently owned by the caller
			assertThrows(RuntimeException.class, () ->  manager.transferRule(subRule, other));
			//TODO This test is failing
			//can't transfer a rule when the destination already owns an unrelated rule
			TestBarrier2 barrier = new TestBarrier2();
			ISchedulingRule unrelatedRule = new PathRule("UnrelatedRule");
			JobRuleRunner ruleRunner = new JobRuleRunner("testTransferFailure", unrelatedRule, barrier, 1, false);
			ruleRunner.schedule();
			//wait for runner to start
			barrier.waitForStatus(TestBarrier2.STATUS_START);
			//let it acquire the rule
			barrier.setStatus(TestBarrier2.STATUS_WAIT_FOR_RUN);
			barrier.waitForStatus(TestBarrier2.STATUS_RUNNING);
			//transferring the calling thread's rule to the background job should fail
			//because the destination thread already owns a rule
			assertThrows(RuntimeException.class, () -> manager.transferRule(rule, ruleRunner.getThread()));
			//let the background job finish
			barrier.setStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);
			barrier.waitForStatus(TestBarrier2.STATUS_DONE);
			ruleRunner.join();
		} finally {
			manager.endRule(rule);
		}
	}

	/**
	 * Tests transferring a scheduling rule from one job to another
	 */
	@Test
	public void testTransferJobToJob() throws CoreException {
		final PathRule ruleToTransfer = new PathRule("testTransferJobToJob");
		final TestBarrier2 barrier = new TestBarrier2();
		final Thread[] sourceThread = new Thread[1];
		final Job destination = new Job("testTransferJobToJob.destination") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				barrier.setStatus(TestBarrier2.STATUS_RUNNING);
				barrier.waitForStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);
				return Status.OK_STATUS;
			}
		};
		final Job source = new Job("testTransferJobToJob.source") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				sourceThread[0] = Thread.currentThread();
				//schedule the destination job and wait until it is running
				destination.schedule();
				barrier.waitForStatus(TestBarrier2.STATUS_RUNNING);
				IJobManagerTest.this.sleep(100);

				//transferring the rule will fail because it must have been acquired by beginRule
				manager.transferRule(ruleToTransfer, destination.getThread());
				return Status.OK_STATUS;
			}
		};
		source.setRule(ruleToTransfer);
		source.schedule();
		waitForCompletion(source);
		//source job should have failed due to illegal use of transferRule
		assertFalse(source.getResult().isOK());
		assertTrue(source.getResult().getException() instanceof RuntimeException);

		//let the destination finish
		barrier.setStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);
		waitForCompletion(destination);
		if (!destination.getResult().isOK()) {
			throw new CoreException(destination.getResult());
		}
	}

	/**
	 * Tests transferring a scheduling rule to the same thread
	 */
	@Test
	public void testTransferSameThread() {
		PathRule rule = new PathRule("testTransferSameThread");
		try {
			manager.beginRule(rule, null);
			//transfer to same thread is ok
			manager.transferRule(rule, Thread.currentThread());
		} finally {
			manager.endRule(rule);
		}
	}

	/**
	 * Simple test of rule transfer
	 */
	@Test
	public void testTransferSimple() throws Exception {
		class RuleEnder implements Runnable {
			Exception error;
			private final ISchedulingRule rule;

			RuleEnder(ISchedulingRule rule) {
				this.rule = rule;
			}

			@Override
			public void run() {
				try {
					manager.endRule(rule);
				} catch (Exception e) {
					this.error = e;
				}
			}
		}
		PathRule rule = new PathRule("testTransferSimple");
		manager.beginRule(rule, null);
		RuleEnder ender = new RuleEnder(rule);
		Thread destination = new Thread(ender);
		manager.transferRule(rule, destination);
		destination.start();
		destination.join();
		if (ender.error != null) {
			throw ender.error;
		}
	}

	/**
	 * Tests transferring a scheduling rule to a job and back again.
	 */
	@Test
	public void testTransferToJob() throws Exception {
		final PathRule rule = new PathRule("testTransferToJob");
		final TestBarrier2 barrier = new TestBarrier2();
		barrier.setStatus(TestBarrier2.STATUS_WAIT_FOR_START);
		final Exception[] failure = new Exception[1];
		final Thread testThread = Thread.currentThread();
		//create a job that the rule will be transferred to
		Job job = new Job("testTransferSimple") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				barrier.setStatus(TestBarrier2.STATUS_RUNNING);
				barrier.waitForStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);

				//sleep a little to ensure the test thread is waiting
				IJobManagerTest.this.sleep(100);
				//at this point we should own the rule so we can transfer it back
				try {
					manager.transferRule(rule, testThread);
				} catch (RuntimeException e) {
					//should not fail
					failure[0] = e;
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
		//wait until the job starts
		barrier.waitForStatus(TestBarrier2.STATUS_RUNNING);

		//now begin and transfer the rule
		manager.beginRule(rule, null);
		manager.transferRule(rule, job.getThread());

		//kick the job to allow it to transfer the rule back
		barrier.setStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);

		//try to begin the rule again, which will block until the rule is transferred back
		manager.beginRule(rule, null);
		manager.endRule(rule);

		//ensure the job didn't fail, and finally end the rule to unwind the initial beginRule
		if (failure[0] != null) {
			throw failure[0];
		}
		manager.endRule(rule);
	}

	/**
	 * Tests transferring a scheduling rule to a job that is waiting for a child of
	 * the transferred rule.
	 */
	@Test
	public void testTransferToJobWaitingOnChildRule() throws Exception {
		final PathRule rule = new PathRule("testTransferToJobWaitingOnChildRule");
		final TestBarrier2 barrier = new TestBarrier2();
		barrier.setStatus(TestBarrier2.STATUS_WAIT_FOR_START);
		final Exception[] failure = new Exception[1];
		final Thread testThread = Thread.currentThread();
		//create a job that the rule will be transferred to
		Job job = new Job("testTransferToJobWaitingOnChildRule") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				barrier.setStatus(TestBarrier2.STATUS_RUNNING);
				//this will block until the rule is transferred
				PathRule child = new PathRule(rule.getFullPath().append("child"));
				try {
					manager.beginRule(child, null);
				} finally {
					manager.endRule(child);
				}
				//at this point we should own the rule so we can transfer it back
				try {
					manager.transferRule(rule, testThread);
				} catch (RuntimeException e) {
					//should not fail
					failure[0] = e;
				}
				return Status.OK_STATUS;
			}
		};
		manager.beginRule(rule, null);

		job.schedule();
		//wait until the job starts
		barrier.waitForStatus(TestBarrier2.STATUS_RUNNING);
		//wait a bit longer to ensure the job is blocked
		try {
			Thread.sleep(100);
		} catch (InterruptedException e1) {
			//ignore
		}

		//now transfer the rule, allowing the job to complete
		manager.transferRule(rule, job.getThread());
		waitForCompletion(job);

		//ensure the job didn't fail, and finally end the rule to assert we own it
		if (failure[0] != null) {
			throw failure[0];
		}
		manager.endRule(rule);
	}

	/**
	 * Tests transferring a scheduling rule to a job that is waiting for that rule.
	 */
	@Test
	public void testTransferToWaitingJob() throws Exception {
		final PathRule rule = new PathRule("testTransferToWaitingJob");
		final TestBarrier2 barrier = new TestBarrier2();
		barrier.setStatus(TestBarrier2.STATUS_WAIT_FOR_START);
		final Exception[] failure = new Exception[1];
		final Thread testThread = Thread.currentThread();
		//create a job that the rule will be transferred to
		Job job = new Job("testTransferToWaitingJob") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				barrier.setStatus(TestBarrier2.STATUS_RUNNING);
				//this will block until the rule is transferred
				try {
					manager.beginRule(rule, null);
				} finally {
					manager.endRule(rule);
				}
				//at this point we should own the rule so we can transfer it back
				try {
					manager.transferRule(rule, testThread);
				} catch (RuntimeException e) {
					//should not fail
					failure[0] = e;
				}
				return Status.OK_STATUS;
			}
		};
		manager.beginRule(rule, null);

		job.schedule();
		//wait until the job starts
		barrier.waitForStatus(TestBarrier2.STATUS_RUNNING);
		//wait a bit longer to ensure the job is blocked
		try {
			Thread.sleep(100);
		} catch (InterruptedException e1) {
			//ignore
		}

		//now transfer the rule, allowing the job to complete
		manager.transferRule(rule, job.getThread());
		waitForCompletion(job);

		//ensure the job didn't fail, and finally end the rule to assert we own it
		if (failure[0] != null) {
			throw failure[0];
		}
		manager.endRule(rule);
	}

	/**
	 * Tests a batch of jobs that use two mutually exclusive rules.
	 */
	@Test
	public void testTwoRules() {
		final int JOB_COUNT = 10;
		TestJob[] jobs = new TestJob[JOB_COUNT];
		ISchedulingRule evens = new IdentityRule();
		ISchedulingRule odds = new IdentityRule();
		for (int i = 0; i < JOB_COUNT; i++) {
			jobs[i] = new TestJob("testSimpleRules", 1000000, 1);
			jobs[i].setRule(((i & 0x1) == 0) ? evens : odds);
			jobs[i].schedule();
		}
		//first two jobs should be running, all others should be waiting
		waitForStart(jobs[0]);
		waitForStart(jobs[1]);
		assertState(jobs[0], Job.RUNNING);
		assertState(jobs[1], Job.RUNNING);
		for (int i = 2; i < JOB_COUNT; i++) {
			assertState(jobs[i], Job.WAITING, i);
		}
		//cancel job i then i+1 and i+2 should run and all others should wait
		for (int i = 0; i < JOB_COUNT; i++) {
			jobs[i].cancel();
			try {
				waitForStart(jobs[i + 1]);
				assertState(jobs[i + 1], Job.RUNNING, i);
				waitForStart(jobs[i + 2]);
				assertState(jobs[i + 2], Job.RUNNING, i);
			} catch (ArrayIndexOutOfBoundsException e) {
				//ignore
			}
			for (int j = i + 3; j < JOB_COUNT; j++) {
				assertState(jobs[j], Job.WAITING, i * JOB_COUNT + j);
			}
		}
	}

	/**
	 * A job has been canceled.  Pause this thread so that a worker thread
	 * has a chance to receive the cancel event.
	 */
	private void waitForCancel(Job job) {
		int i = 0;
		while (job.getState() == Job.RUNNING) {
			Thread.yield();
			sleep(100);
			Thread.yield();
			//sanity test to avoid hanging tests
			if (i++ > 1000) {
				dumpState();
				fail("Timeout waiting for job to cancel");
			}
		}
	}

	private void waitForCompletion() {
		int i = 0;
		assertTrue(completedJobs.get() <= scheduledJobs.get(), "Jobs completed that weren't scheduled");
		while (completedJobs.get() < scheduledJobs.get()) {
			try {
				synchronized (this) {
					this.wait(1);
				}
			} catch (InterruptedException e) {
				//ignore
			}
			//sanity test to avoid hanging tests
			if (i++ > 100000) {
				dumpState();
				fail("Timeout waiting for job to complete");
			}
		}
	}

	/**
	 * A family of jobs have been canceled. Pause this thread until all of the jobs
	 * in the family are canceled
	 */
	private void waitForFamilyCancel(Job[] jobs, TestJobFamily type) {

		for (Job job : jobs) {
			int i = 0;
			while (job.belongsTo(type) && (job.getState() != Job.NONE)) {
				Thread.yield();
				sleep(100);
				Thread.yield();
				//sanity test to avoid hanging tests
				if (i++ > 100) {
					dumpState();
					fail("Timeout waiting for job in family " + type.getType() + "to be canceled ");
				}
			}
		}
	}

	private void waitForRunCount(TestJob job, int runCount) {
		int i = 0;
		while (job.getRunCount() < runCount) {
			Thread.yield();
			sleep(100);
			Thread.yield();
			//sanity test to avoid hanging tests
			if (i++ >= 1000) {
				dumpState();
				fail("Timeout waiting for job to start. Job: " + job + ", state: " + job.getState());
			}
		}
	}

	/**
	 * A job has been scheduled.  Pause this thread so that a worker thread
	 * has a chance to pick up the new job.
	 */
	private void waitForStart(TestJob job) {
		waitForRunCount(job, 1);
	}

}