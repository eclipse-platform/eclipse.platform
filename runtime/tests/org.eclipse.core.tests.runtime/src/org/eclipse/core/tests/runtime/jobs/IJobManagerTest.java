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

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import junit.framework.AssertionFailedError;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.core.tests.harness.*;

/**
 * Tests the API of the class IJobManager
 */
@SuppressWarnings("restriction")
public class IJobManagerTest extends AbstractJobManagerTest {
	class TestJobListener extends JobChangeAdapter {
		private Set<Job> scheduled = Collections.synchronizedSet(new HashSet<Job>());

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

	public IJobManagerTest() {
		super("");
	}

	public IJobManagerTest(String name) {
		super(name);
	}

	/**
	 * Asserts the current job state
	 */
	public void assertState(String msg, Job job, int expectedState) {
		int actualState = job.getState();
		if (actualState != expectedState) {
			assertTrue(msg + ": expected state: " + printState(expectedState) + " actual state: " + printState(actualState), false);
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

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		completedJobs = new AtomicInteger();
		scheduledJobs = new AtomicInteger();
		jobListeners = new IJobChangeListener[] {/* new VerboseJobListener(),*/
		new TestJobListener()};
		for (IJobChangeListener jobListener : jobListeners) {
			manager.addJobChangeListener(jobListener);
		}
	}

	@Override
	protected void tearDown() throws Exception {
		for (IJobChangeListener jobListener : jobListeners) {
			if (jobListener instanceof TestJobListener) {
				((TestJobListener) jobListener).cancelAllJobs();
			}
		}
		waitForCompletion();
		for (IJobChangeListener jobListener : jobListeners) {
			manager.removeJobChangeListener(jobListener);
		}
		super.tearDown();
		//		manager.startup();
	}

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
			try {
				Job.getJobManager().beginRule(root, null);
				fail("1.0");
			} catch (IllegalArgumentException e) {
				// expected
			} finally {
				Job.getJobManager().endRule(root);
			}
		} finally {
			Job.getJobManager().endRule(invalid);
		}
	}

	/**
	 * Tests that if we call beginRule with a monitor that has already been
	 * cancelled, it won't try to obtain the rule.
	 */
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
		assertTrue("An OperationCancelledException should have been thrown", canceledExceptionThrown);
		assertFalse("The Thread.interrupted() state leaked", interrupted);
	}

	/**
	 * Tests that if our monitor is cancelled while we're waiting on beginRule,
	 * it will stop waiting, will throw an {@link OperationCanceledException},
	 * and will clear the Thread.interrupted() flag.
	 */
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
		assertTrue("An OperationCancelledException should have been thrown", canceledExceptionThrown);
		assertFalse("The THread.interrupted() state leaked", interrupted);
	}

	/**
	 * Tests running a job that begins a rule but never ends it
	 */
	public void testBeginRuleNoEnd() {
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
		try {
			job.join();
		} catch (InterruptedException e) {
			fail("4.99", e);
		}
		//another thread should be able to access the rule now
		try {
			manager.beginRule(rule, null);
		} finally {
			manager.endRule(rule);
		}
	}

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
	public void testBug57656() {
		TestJob jobA = new TestJob("Job1");
		TestJob jobB = new TestJob("Job2");
		//schedule jobA
		jobA.schedule(50);
		//schedule jobB so it gets behind jobA in the queue
		jobB.schedule(100);
		//now put jobA to sleep indefinitely
		jobA.sleep();
		//jobB should still run within ten seconds
		waitForCompletion(jobB, 300);
	}

	/**
	 * This is a regression test for bug 71448. IJobManager.currentJob was not
	 * returning the correct value when executed in a thread that is performing
	 * asynchronous completion of a job (i.e., a UI Job)
	 */
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
		assertTrue("1.0", success[0]);
	}

	/**
	 * Tests for {@link IJobManager#currentRule()}.
	 */
	public void testCurrentRule() {
		//first test when not running in a job
		runRuleSequence();

		//next test in a job with no rule of its own
		final List<AssertionFailedError> errors = new ArrayList<>();
		Job sequenceJob = new Job("testCurrentRule") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					runRuleSequence();
				} catch (AssertionFailedError e) {
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

	public void testDelayedJob() {
		//schedule a delayed job and ensure it doesn't start until instructed
		int[] sleepTimes = new int[] { 0, 1, 5, 10, 50, 100, 200, 250 };
		for (int i = 0; i < sleepTimes.length; i++) {
			long start = now();
			TestJob job = new TestJob("Noop", 0, 0);
			assertEquals("1.0", 0, job.getRunCount());
			job.schedule(sleepTimes[i]);
			waitForCompletion();
			assertEquals("1.1." + i, 1, job.getRunCount());
			long duration = now() - start;
			assertTrue("1.2: duration: " + duration + " sleep: " + sleepTimes[i], duration >= sleepTimes[i]);
			//a no-op job shouldn't take any real time
			if (PEDANTIC) {
				assertTrue("1.3: duration: " + duration + " sleep: " + sleepTimes[i], duration < sleepTimes[i] + 1000);
			}
		}
	}

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

		assertState("1.0", jobs[0], Job.RUNNING);

		//first job is running, the rest are waiting
		for (int i = 1; i < NUM_JOBS; i++) {
			assertState("1." + i, jobs[i], Job.WAITING);
		}

		//cancel the first family of jobs
		manager.cancel(first);
		waitForFamilyCancel(jobs, first);

		//the previously running job should have no state
		assertState("2.0", jobs[0], Job.NONE);
		//the first job from the second family should now be running
		waitForStart(jobs[1]);

		for (int i = 2; i < NUM_JOBS; i++) {
			//all other jobs in the first family should be removed from the waiting queue
			//no operations can be performed on these jobs until they are scheduled with the manager again
			if (jobs[i].belongsTo(first)) {
				assertState("2." + i, jobs[i], Job.NONE);
				jobs[i].wakeUp();
				assertState("2." + i, jobs[i], Job.NONE);
				jobs[i].sleep();
				assertState("2." + i, jobs[i], Job.NONE);
			}
			//all other jobs in the second family should still be in the waiting queue
			else {
				assertState("3." + i, jobs[i], Job.WAITING);
			}
		}

		for (int i = 2; i < NUM_JOBS; i++) {
			//all the jobs in the second family that are waiting to start can now be set to sleep
			if (jobs[i].belongsTo(second)) {
				assertState("4." + i, jobs[i], Job.WAITING);
				assertTrue("5." + i, jobs[i].sleep());
				assertState("6." + i, jobs[i], Job.SLEEPING);
			}
		}
		//cancel the second family of jobs
		manager.cancel(second);
		waitForFamilyCancel(jobs, second);

		//the second job should now have no state
		assertState("7.0", jobs[1], Job.NONE);

		for (int i = 0; i < NUM_JOBS; i++) {
			//all jobs should now be in the NONE state
			assertState("8." + i, jobs[i], Job.NONE);
		}
	}

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
		HashSet<Job> allJobs = new HashSet<>();
		allJobs.addAll(Arrays.asList(jobs));
		Job[] result = manager.find(null);
		assertTrue("1.0", result.length >= NUM_JOBS);
		for (int i = 0; i < result.length; i++) {
			//only test jobs that we know about
			if (allJobs.remove(result[i])) {
				assertTrue("1." + i, (result[i].belongsTo(first) || result[i].belongsTo(second) || result[i].belongsTo(third) || result[i].belongsTo(fourth) || result[i].belongsTo(fifth)));
			}
		}
		assertEquals("1.2", 0, allJobs.size());

		//try finding all jobs from the first family
		result = manager.find(first);
		assertEquals("2.0", 4, result.length);
		for (int i = 0; i < result.length; i++) {
			assertTrue("2." + (i + 1), result[i].belongsTo(first));
		}

		//try finding all jobs from the second family
		result = manager.find(second);
		assertEquals("3.0", 4, result.length);
		for (int i = 0; i < result.length; i++) {
			assertTrue("3." + (i + 1), result[i].belongsTo(second));
		}

		//try finding all jobs from the third family
		result = manager.find(third);
		assertEquals("4.0", 4, result.length);
		for (int i = 0; i < result.length; i++) {
			assertTrue("4." + (i + 1), result[i].belongsTo(third));
		}

		//try finding all jobs from the fourth family
		result = manager.find(fourth);
		assertEquals("5.0", 4, result.length);
		for (int i = 0; i < result.length; i++) {
			assertTrue("5." + (i + 1), result[i].belongsTo(fourth));
		}

		//try finding all jobs from the fifth family
		result = manager.find(fifth);
		assertEquals("6.0", 4, result.length);
		for (int i = 0; i < result.length; i++) {
			assertTrue("6." + (i + 1), result[i].belongsTo(fifth));
		}

		//the first job should still be running
		assertState("7.0", jobs[0], Job.RUNNING);

		//put the second family of jobs to sleep
		manager.sleep(second);

		//cancel the first family of jobs
		manager.cancel(first);

		//the third job should start running
		waitForStart(jobs[2]);
		assertState("7.1", jobs[2], Job.RUNNING);

		//finding all jobs from the first family should return an empty array
		result = manager.find(first);
		assertEquals("7.2", 0, result.length);

		//finding all jobs from the second family should return all the jobs (they are just sleeping)
		result = manager.find(second);
		assertEquals("8.0", 4, result.length);
		for (int i = 0; i < result.length; i++) {
			assertTrue("8." + (i + 1), result[i].belongsTo(second));
		}

		//cancel the second family of jobs
		manager.cancel(second);
		//finding all jobs from the second family should now return an empty array
		result = manager.find(second);
		assertEquals("9.0", 0, result.length);

		//cancel the fourth family of jobs
		manager.cancel(fourth);
		//finding all jobs from the fourth family should now return an empty array
		result = manager.find(fourth);
		assertEquals("9.1", 0, result.length);

		//put the third family of jobs to sleep
		manager.sleep(third);
		//the first job from the third family should still be running
		assertState("9.2", jobs[2], Job.RUNNING);
		//wake up the last job from the third family
		jobs[NUM_JOBS - 3].wakeUp();
		//it should now be in the WAITING state
		assertState("9.3", jobs[NUM_JOBS - 3], Job.WAITING);

		//finding all jobs from the third family should return all 4 jobs (1 is running, 1 is waiting, 2 are sleeping)
		result = manager.find(third);
		assertEquals("10.0", 4, result.length);
		for (int i = 0; i < result.length; i++) {
			assertTrue("10." + (i + 1), result[i].belongsTo(third));
		}

		//finding all jobs by supplying the NULL parameter should return 8 jobs (4 from the 3rd family, and 4 from the 5th family)
		//note that this might find other jobs that are running as a side-effect of the test
		//suites running, such as snapshot
		allJobs.addAll(Arrays.asList(jobs));
		result = manager.find(null);
		assertTrue("11.0", result.length >= 8);
		for (int i = 0; i < result.length; i++) {
			//only test jobs that we know about
			if (allJobs.remove(result[i])) {
				assertTrue("11." + (i + 1), (result[i].belongsTo(third) || result[i].belongsTo(fifth)));
			}
		}

		assertEquals("11.2", 12, allJobs.size());
		allJobs.clear();

		//cancel the fifth family of jobs
		manager.cancel(fifth);
		//cancel the third family of jobs
		manager.cancel(third);
		waitForFamilyCancel(jobs, third);

		//all jobs should now be in the NONE state
		for (int i = 0; i < NUM_JOBS; i++) {
			assertState("12." + i, jobs[i], Job.NONE);
		}

		//finding all jobs should return an empty array
		//note that this might find other jobs that are running as a side-effect of the test
		//suites running, such as snapshot
		allJobs.addAll(Arrays.asList(jobs));
		result = manager.find(null);
		assertTrue("13.0", result.length >= 0);

		for (int i = 0; i < result.length; i++) {
			//test jobs that we know about should not be found (they should have all been removed)
			if (allJobs.remove(result[i])) {
				assertTrue("14." + i, false);
			}
		}
		assertEquals("15.0", NUM_JOBS, allJobs.size());
		allJobs.clear();
	}

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
				assertEquals("2." + i, 0, result.length);
				break;
			}
			sleep(1);
		}
		assertTrue("2.0", i < 10000);

		//cancel the second family of jobs
		manager.cancel(second);
		waitForFamilyCancel(jobs, second);

		//all the jobs should now be in the NONE state
		for (int j = 0; j < NUM_JOBS; j++) {
			assertState("3." + j, jobs[j], Job.NONE);
		}
	}

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

		assertState("2.0", jobs[0], Job.RUNNING);
		assertEquals("2.1", TestBarrier2.STATUS_RUNNING, status.get(0));

		//cancel the first family of jobs
		//the join call should be unblocked when all the jobs are canceled
		manager.cancel(first);
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_DONE);

		//all jobs in the first family should be removed from the manager
		assertEquals("2.2", 0, manager.find(first).length);

		//cancel the second family of jobs
		manager.cancel(second);
		waitForFamilyCancel(jobs, second);

		//all the jobs should now be in the NONE state
		for (int j = 0; j < NUM_JOBS; j++) {
			assertState("3." + j, jobs[j], Job.NONE);
		}
	}

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

		assertState("2.0", jobs[0], Job.RUNNING);
		assertEquals("2.1", TestBarrier2.STATUS_RUNNING, status.get(0));

		//cancel the monitor that is attached to the join call
		canceller.setCanceled(true);
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_DONE);

		//the first job in the first family should still be running
		assertState("2.2", jobs[0], Job.RUNNING);
		assertEquals("2.3", TestBarrier2.STATUS_DONE, status.get(0));
		assertTrue("2.4", manager.find(first).length > 0);

		//cancel the second family of jobs
		manager.cancel(second);
		waitForFamilyCancel(jobs, second);

		//cancel the first family of jobs
		manager.cancel(first);
		waitForFamilyCancel(jobs, first);

		//all the jobs should now be in the NONE state
		for (int j = 0; j < NUM_JOBS; j++) {
			assertState("3." + j, jobs[j], Job.NONE);
		}
	}

	/**
	 * Asserts that the LockListener is called correctly during invocation of
	 * {@link IJobManager#join(Object, IProgressMonitor)}.
	 * See bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=195839.
	 */
	public void testJobFamilyJoinLockListener() {
		final TestJobFamily family = new TestJobFamily(TestJobFamily.TYPE_ONE);
		int count = 5;
		Job[] jobs = new Job[count];
		for (int i = 0; i < jobs.length; i++) {
			jobs[i] = new FamilyTestJob("TestJobFamilyJoinLockListener" + i, 5, 5, family.getType());
			jobs[i].schedule();
		}
		TestLockListener lockListener = new TestLockListener();
		try {
			manager.setLockListener(lockListener);
			manager.join(family, new FussyProgressMonitor());
		} catch (OperationCanceledException | InterruptedException e) {
			fail("4.99", e);
		} finally {
			manager.setLockListener(null);
		}
		lockListener.assertNotWaiting("1.0");
	}

	public void testJobFamilyJoinNothing() {
		//test joining a bogus family, and the monitor should be used up
		try {
			final FussyProgressMonitor monitor = new FussyProgressMonitor();
			monitor.prepare();
			manager.join(new Object(), monitor);
			monitor.sanityCheck();
			monitor.assertUsedUp();
		} catch (OperationCanceledException | InterruptedException e) {
			fail("4.99", e);
		}
	}

	/**
	 * Tests joining a job that repeats in a loop
	 */
	public void testJobFamilyJoinRepeating() {
		Object family = new Object();
		int count = 25;
		RepeatingJob job = new RepeatingJob("testJobFamilyJoinRepeating", count);
		job.setFamily(family);
		job.schedule();
		try {
			Job.getJobManager().join(family, null);
		} catch (OperationCanceledException e) {
			fail("1.0", e);
		} catch (InterruptedException e) {
			fail("1.1", e);
		}
		//ensure the job has run the expected number of times
		assertEquals("1.2", count, job.getRunCount());
	}

	/**
	 * Tests joining a job family that repeats but returns false to shouldSchedule
	 */
	public void testJobFamilyJoinShouldSchedule() {
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
		try {
			Job.getJobManager().join(family, null);
		} catch (OperationCanceledException e) {
			fail("1.0", e);
		} catch (InterruptedException e) {
			fail("1.1", e);
		}
		//ensure the job has run the expected number of times
		assertEquals("1.2", count, job.getRunCount());
	}

	/**
	 * Tests simple usage of the IJobManager.join() method.
	 */
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
		assertEquals("1.0", TestBarrier2.STATUS_START, status.get(0));
		long startTime = now();
		status.set(0, TestBarrier2.STATUS_WAIT_FOR_RUN);
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_DONE);
		long endTime = now();

		assertEquals("2.0", TestBarrier2.STATUS_DONE, status.get(0));
		assertTrue("2.1", endTime >= startTime); // XXX this tests makes no sense. now() is guaranteed to be >= anyway.
													// and the expectation is that it takes NO time anyway... see next
													// comment

		//the join call should take no actual time (join call should not block thread at all)
		if (PEDANTIC) {
			assertTrue("2.2 start time: " + startTime + " end time: " + endTime, (endTime - startTime) < 300);
		}

		//cancel all jobs
		manager.cancel(first);
		manager.cancel(second);
		waitForFamilyCancel(jobs, first);
		waitForFamilyCancel(jobs, second);

		//all the jobs should now be in the NONE state
		for (int j = 0; j < NUM_JOBS; j++) {
			assertState("3." + j, jobs[j], Job.NONE);
		}
	}

	/**
	 * Tests scenario 1 described in https://bugs.eclipse.org/bugs/show_bug.cgi?id=403271#c0:
	 *  - join is called when job manager is suspended
	 *  - waiting job is scheduled when job manager is suspended
	 * In this scenario main job should not wait for the waiting job.
	 */
	public void testJobFamilyJoinWhenSuspended_1() throws InterruptedException {
		final Object family = new TestJobFamily(TestJobFamily.TYPE_ONE);
		final int[] familyJobsCount = new int[] {-1};
		final TestBarrier2 barrier = new TestBarrier2();
		final Job waiting = new FamilyTestJob("waiting job", 1000000, 10, TestJobFamily.TYPE_ONE);
		final Job running = new FamilyTestJob("running job", 2, 1, TestJobFamily.TYPE_ONE);
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
						private boolean scheduled = false;

						@Override
						public boolean aboutToWait(Thread lockOwner) {
							// aboutToWait will be called when main job will start joining the running job
							if (!scheduled) {
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
	 * Tests scenario 2 - verifies if the suspended flag is checked each time a job is scheduled:
	 *  - join is called when job manager is NOT suspended
	 *  - waiting job is scheduled when job manager is suspended
	 * In this scenario main job should not wait for the waiting job.
	 */
	public void testJobFamilyJoinWhenSuspended_2() throws InterruptedException {
		final Object family = new TestJobFamily(TestJobFamily.TYPE_ONE);
		final int[] familyJobsCount = new int[] {-1};
		final TestBarrier2 barrier = new TestBarrier2();
		final Job waiting = new FamilyTestJob("waiting job", 1000000, 10, TestJobFamily.TYPE_ONE);
		final Job running = new FamilyTestJob("running job", 2, 1, TestJobFamily.TYPE_ONE);
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
						private boolean scheduled = false;

						@Override
						public boolean aboutToWait(Thread lockOwner) {
							// aboutToWait will be called when main job will start joining the running job
							if (!scheduled) {
								// suspend before scheduling new job
								getJobManager().suspend();
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
	 * Tests scenario 3:
	 *  - join is called when job manager is NOT suspended
	 *  - waiting job is scheduled when job manager is suspended
	 *  - job manager is resumed causing waiting job to start
	 * In this scenario main thread should wait for the waiting job since the job was started before the join ended.
	 */
	public void testJobFamilyJoinWhenSuspended_3() throws InterruptedException {
		final Object family = new TestJobFamily(TestJobFamily.TYPE_ONE);
		final TestBarrier2 barrier = new TestBarrier2();
		final Job waiting = new FamilyTestJob("waiting job", 4, 1, TestJobFamily.TYPE_ONE);
		final Job running = new FamilyTestJob("running job", 2, 1, TestJobFamily.TYPE_ONE);
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
						scheduled = true;
					}
					return super.aboutToWait(lockOwner);
				}
			});
			manager.join(family, null);
			assertEquals(0, manager.find(family).length);
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
		assertState("1.0", jobs[0], Job.RUNNING);

		//put all jobs to sleep
		manager.sleep(null);
		//the first job should still be running
		assertState("2.0", jobs[0], Job.RUNNING);

		//all the other jobs should be sleeping
		for (int i = 1; i < NUM_JOBS; i++) {
			assertState("2." + i, jobs[i], Job.SLEEPING);
		}

		//wake up all the jobs
		manager.wakeUp(null);
		//the first job should still be running
		assertState("3.0", jobs[0], Job.RUNNING);

		//all the other jobs should be waiting
		for (int i = 1; i < NUM_JOBS; i++) {
			assertState("3." + i, jobs[i], Job.WAITING);
		}

		//cancel all the jobs
		manager.cancel(first);
		manager.cancel(second);
		waitForFamilyCancel(jobs, first);
		waitForFamilyCancel(jobs, second);

		//all the jobs should now be in the NONE state
		for (int i = 0; i < NUM_JOBS; i++) {
			assertState("4." + i, jobs[i], Job.NONE);
		}

	}

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

		assertState("1.0", jobs[0], Job.RUNNING);

		//first job is running, the rest are waiting
		for (int i = 1; i < NUM_JOBS; i++) {
			assertState("1." + i, jobs[i], Job.WAITING);
		}

		//set the first family of jobs to sleep
		manager.sleep(first);

		//the running job should still be running
		assertState("2.0", jobs[0], Job.RUNNING);

		for (int i = 1; i < NUM_JOBS; i++) {
			//all other jobs in the first family should be sleeping
			//they can now be canceled
			if (jobs[i].belongsTo(first)) {
				assertState("2." + i, jobs[i], Job.SLEEPING);
				jobs[i].cancel();
			}
			//all jobs in the second family should still be in the waiting queue
			else {
				assertState("3." + i, jobs[i], Job.WAITING);
			}
		}

		manager.sleep(second);
		//cancel the running job
		jobs[0].cancel();
		waitForCancel(jobs[0]);

		//no job should now be running
		assertNull("4.0", manager.currentJob());

		for (int i = 1; i < NUM_JOBS; i++) {
			//all other jobs in the second family should be sleeping
			//they can now be canceled
			if (jobs[i].belongsTo(second)) {
				assertState("4." + i, jobs[i], Job.SLEEPING);
				jobs[i].cancel();
			}
		}

		//all the jobs should now be in the NONE state
		for (int i = 0; i < NUM_JOBS; i++) {
			assertState("5." + i, jobs[i], Job.NONE);
		}
	}

	/**
	 * Tests the API method IJobManager.wakeUp(family)
	 */
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
		assertState("1.0", seedJob, Job.RUNNING);

		//create jobs in first family and put them to sleep
		for (int i = 0; i < JOBS_PER_FAMILY; i++) {
			family1[i] = new FamilyTestJob("TestFirstFamily", 1000000, 1, TestJobFamily.TYPE_ONE);
			family1[i].setRule(rule);
			family1[i].schedule();
			assertState("1.1." + i, family1[i], Job.WAITING);
			assertTrue("1.2." + i, family1[i].sleep());
			assertState("1.3." + i, family1[i], Job.SLEEPING);
		}
		//create jobs in second family and put them to sleep
		for (int i = 0; i < JOBS_PER_FAMILY; i++) {
			family2[i] = new FamilyTestJob("TestSecondFamily", 1000000, 1, TestJobFamily.TYPE_TWO);
			family2[i].setRule(rule);
			family2[i].schedule();
			assertState("2.1." + i, family2[i], Job.WAITING);
			assertTrue("2.2." + i, family2[i].sleep());
			assertState("2.3." + i, family2[i], Job.SLEEPING);
		}

		//cancel the seed job
		seedJob.cancel();
		waitForCancel(seedJob);
		assertState("3.0", seedJob, Job.NONE);

		//all family jobs should still be sleeping
		for (int i = 0; i < JOBS_PER_FAMILY; i++) {
			assertState("3.1." + i, family1[i], Job.SLEEPING);
		}
		for (int i = 0; i < JOBS_PER_FAMILY; i++) {
			assertState("3.2." + i, family2[i], Job.SLEEPING);
		}

		//wake-up the second family of jobs
		manager.wakeUp(second);

		//jobs in the first family should still be in the sleep state
		for (int i = 0; i < JOBS_PER_FAMILY; i++) {
			assertState("4.1." + i, family1[i], Job.SLEEPING);
		}
		//ensure all jobs in second family are either running or waiting
		int runningCount = 0;
		for (int i = 0; i < JOBS_PER_FAMILY; i++) {
			int state = family2[i].getState();
			if (state == Job.RUNNING) {
				runningCount++;
			} else if (state != Job.WAITING) {
				assertTrue("4.2." + i + ": expected state: " + printState(Job.WAITING) + " actual state: " + printState(state), false);
			}
		}
		//ensure only one job is running (it is possible that none have started yet)
		assertTrue("4.running", runningCount <= 1);

		//cycle through the jobs in the second family and cancel them
		for (int i = 0; i < JOBS_PER_FAMILY; i++) {
			//the running job may not respond immediately
			if (!family2[i].cancel()) {
				waitForCancel(family2[i]);
			}
			assertState("5." + i, family2[i], Job.NONE);
		}

		//all jobs in the first family should still be sleeping
		for (int i = 0; i < JOBS_PER_FAMILY; i++) {
			assertState("6.1." + i, family1[i], Job.SLEEPING);
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
				assertTrue("7.1." + i + ": expected state: " + printState(Job.WAITING) + " actual state: " + printState(state), false);
			}
		}
		//ensure only one job is running (it is possible that none have started yet)
		assertTrue("7.running", runningCount <= 1);

		//cycle through the jobs in the first family and cancel them
		for (int i = 0; i < JOBS_PER_FAMILY; i++) {
			//the running job may not respond immediately
			if (!family1[i].cancel()) {
				waitForCancel(family1[i]);
			}
			assertState("8." + i, family1[i], Job.NONE);
		}

		//all jobs should now be in the NONE state
		for (int i = 0; i < JOBS_PER_FAMILY; i++) {
			assertState("9.1." + i, family1[i], Job.NONE);
		}
		for (int i = 0; i < JOBS_PER_FAMILY; i++) {
			assertState("9.2." + i, family2[i], Job.NONE);
		}
	}

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
		assertState("1.0", jobs[0], Job.RUNNING);
		for (int i = 1; i < JOB_COUNT; i++) {
			assertState("1.1." + i, jobs[i], Job.WAITING);
		}
		//cancel job i, then i+1 should run and all others should wait
		for (int i = 0; i < JOB_COUNT - 1; i++) {
			jobs[i].cancel();
			waitForStart(jobs[i + 1]);
			assertState("2.0." + i, jobs[i + 1], Job.RUNNING);
			for (int j = i + 2; j < JOB_COUNT; j++) {
				assertState("2.1" + i + "." + j, jobs[j], Job.WAITING);
			}
		}
		//cancel the final job
		jobs[JOB_COUNT - 1].cancel();
	}

	public void testOrder() {
		//ensure jobs are run in order from lowest to highest sleep time.
		final Queue<Job> done = new ConcurrentLinkedQueue<>();
		int[] sleepTimes = new int[] { 5, 100, 200, 300 };
		Job[] jobs = new Job[sleepTimes.length];
		for (int i = 0; i < sleepTimes.length; i++) {
			jobs[i] = new Job("testOrder(" + i + ")") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					done.add(this);
					return Status.OK_STATUS;
				}

			};
		}
		for (int i = 0; i < sleepTimes.length; i++) {
			jobs[i].schedule(sleepTimes[i]);
		}
		// make sure listener has had a chance to process the finished job
		while (done.size() != jobs.length) {
			Thread.yield();
		}
		Job[] doneOrder = done.toArray(new Job[done.size()]);
		assertEquals("1.0", jobs.length, doneOrder.length);
		for (int i = 0; i < doneOrder.length; i++) {
			assertEquals("1.1." + i, jobs[i], doneOrder[i]);
		}
	}

	public void testReverseOrder() {
		//ensure jobs are run in order from lowest to highest sleep time.
		final Queue<Job> done = new ConcurrentLinkedQueue<>();
		int[] sleepTimes = new int[] { 300, 200, 100, 5 };
		Job[] jobs = new Job[sleepTimes.length];
		for (int i = 0; i < sleepTimes.length; i++) {
			jobs[i] = new Job("testReverseOrder(" + i + ")") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					done.add(this);
					return Status.OK_STATUS;
				}

			};
		}
		for (int i = 0; i < sleepTimes.length; i++) {
			jobs[i].schedule(sleepTimes[i]);
		}
		while (done.size() != jobs.length) {
			Thread.yield();
		}
		Job[] doneOrder = done.toArray(new Job[done.size()]);
		assertEquals("1.0", jobs.length, doneOrder.length);
		for (int i = 0; i < doneOrder.length; i++) {
			assertEquals("1.1." + i, jobs[i], doneOrder[doneOrder.length - 1 - i]);
		}
	}

	/**
	 * Tests conditions where there is a race to schedule the same job multiple times.
	 */
	public void testScheduleRace() {
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
		waitForCompletion(testJob, 5000);
		assertTrue("1.0", !failure[0]);
	}

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

		try {
			job.setRule(rule1);
			fail("invalid rule");
		} catch (IllegalArgumentException e) {
			//expected
		}
		try {
			job.setRule(rule2);
			fail("invalid rule");
		} catch (IllegalArgumentException e) {
			//expected
		}
		try {
			job.setRule(multi);
			fail("invalid rule");
		} catch (IllegalArgumentException e) {
			//expected
		}
	}

	public void testSleep() {
		TestJob job = new TestJob("ParentJob", 10, 10);
		//sleeping a job that isn't scheduled should have no effect
		assertEquals("1.0", Job.NONE, job.getState());
		assertTrue("1.1", job.sleep());
		assertEquals("1.2", Job.NONE, job.getState());

		//sleeping a job that is already running should not work
		job.schedule();
		//give the job a chance to start
		waitForStart(job);
		assertState("2.0", job, Job.RUNNING);
		assertTrue("2.1", !job.sleep());
		assertState("2.2", job, Job.RUNNING);

		waitForCompletion();

		//sleeping a job that is already sleeping should make sure it never runs
		job.schedule(500);
		assertState("3.0", job, Job.SLEEPING);
		assertTrue("3.1", job.sleep());
		assertState("3.2", job, Job.SLEEPING);
		//wait awhile and ensure the job is still sleeping
		Thread.yield();
		sleep(60);
		Thread.yield();
		assertState("3.3", job, Job.SLEEPING);
		assertTrue("3.4", job.cancel()); //should be possible to cancel a sleeping job
	}

	public void testSleepOnWait() {
		final ISchedulingRule rule = new PathRule("testSleepOnWait");
		TestJob blockingJob = new TestJob("Long Job", 1000000, 10);
		blockingJob.setRule(rule);
		blockingJob.schedule();

		TestJob job = new TestJob("Long Job", 1000000, 10);
		job.setRule(rule);
		job.schedule();
		//we know this job is waiting, so putting it to sleep should prevent it from running
		assertState("1.0", job, Job.WAITING);
		assertTrue("1.1", job.sleep());
		assertState("1.2", job, Job.SLEEPING);

		//cancel the blocking job, thus freeing the pool for the waiting job
		blockingJob.cancel();

		//make sure the job is still sleeping
		assertState("1.3", job, Job.SLEEPING);

		//now wake the job up
		job.wakeUp();
		waitForStart(job);
		assertState("2.0", job, Job.RUNNING);

		//finally cancel the job
		job.cancel();
		waitForCompletion(job);
	}

	public void testSuspend() {
		assertTrue("1.0", !manager.isSuspended());
		manager.suspend();
		try {
			assertTrue("1.1", manager.isSuspended());
		} finally {
			manager.resume();
		}
		assertTrue("1.1", !manager.isSuspended());
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
		assertNull("1.0", job.getResult());

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
		assertNull("1.1", job.getResult());

		//even ending the rule in this thread should not allow the job to continue
		manager.endRule(rule1);
		sleep(100);
		assertNull("1.2", job.getResult());

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
	public void testTransferFailure() {
		PathRule rule = new PathRule("/testTransferFailure");
		PathRule subRule = new PathRule("/testTransferFailure/Sub");
		Thread other = new Thread();
		//can't transfer a rule this thread doesn't own it
		try {
			manager.transferRule(rule, other);
			fail("1.0");
		} catch (RuntimeException e) {
			//expected
		}
		try {
			manager.beginRule(rule, null);
			//can't transfer a child rule of a rule currently owned by the caller
			try {
				manager.transferRule(subRule, other);
				fail("1.1");
			} catch (RuntimeException e) {
				//expected
			}
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
			try {
				manager.transferRule(rule, ruleRunner.getThread());
				fail("1.2");
			} catch (RuntimeException e) {
				//expected
			}
			//let the background job finish
			barrier.setStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);
			barrier.waitForStatus(TestBarrier2.STATUS_DONE);
			try {
				ruleRunner.join();
			} catch (InterruptedException e1) {
				fail("1.99", e1);
			}
		} finally {
			manager.endRule(rule);
		}
	}

	/**
	 * Tests transferring a scheduling rule from one job to another
	 */
	public void testTransferJobToJob() {
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
		assertTrue("1.0", !source.getResult().isOK());
		assertTrue("1.1", source.getResult().getException() instanceof RuntimeException);

		//let the destination finish
		barrier.setStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);
		waitForCompletion(destination);
		if (!destination.getResult().isOK()) {
			fail("1.2", destination.getResult().getException());
		}
	}

	/**
	 * Tests transferring a scheduling rule to the same thread
	 */
	public void testTransferSameThread() {
		PathRule rule = new PathRule("testTransferSameThread");
		try {
			manager.beginRule(rule, null);
			//transfer to same thread is ok
			manager.transferRule(rule, Thread.currentThread());
		} catch (Exception e) {
			fail("1.0", e);
		} finally {
			manager.endRule(rule);
		}
	}

	/**
	 * Simple test of rule transfer
	 */
	public void testTransferSimple() {
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
		try {
			destination.join();
		} catch (InterruptedException e) {
			fail("1.99", e);
		}
		if (ender.error != null) {
			fail("1.0", ender.error);
		}
	}

	/**
	 * Tests transferring a scheduling rule to a job and back again.
	 */
	public void testTransferToJob() {
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
			fail("1.0", failure[0]);
		}
		try {
			manager.endRule(rule);
		} catch (Exception e) {
			//we should own the rule so this shouldn't fail
			fail("2.00", e);
		}
	}

	/**
	 * Tests transferring a scheduling rule to a job that is waiting for a child of
	 * the transferred rule.
	 */
	public void testTransferToJobWaitingOnChildRule() {
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
			fail("1.0", failure[0]);
		}
		try {
			manager.endRule(rule);
		} catch (Exception e) {
			//we should own the rule so this shouldn't fail
			fail("2.00", e);
		}
	}

	/**
	 * Tests transferring a scheduling rule to a job that is waiting for that rule.
	 */
	public void testTransferToWaitingJob() {
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
			fail("1.0", failure[0]);
		}
		try {
			manager.endRule(rule);
		} catch (Exception e) {
			//we should own the rule so this shouldn't fail
			fail("2.00", e);
		}
	}

	/**
	 * Tests a batch of jobs that use two mutually exclusive rules.
	 */
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
		assertState("1.0", jobs[0], Job.RUNNING);
		assertState("1.1", jobs[1], Job.RUNNING);
		for (int i = 2; i < JOB_COUNT; i++) {
			assertState("1.2." + i, jobs[i], Job.WAITING);
		}
		//cancel job i then i+1 and i+2 should run and all others should wait
		for (int i = 0; i < JOB_COUNT; i++) {
			jobs[i].cancel();
			try {
				waitForStart(jobs[i + 1]);
				assertState("2.0." + i, jobs[i + 1], Job.RUNNING);
				waitForStart(jobs[i + 2]);
				assertState("2.1." + i, jobs[i + 2], Job.RUNNING);
			} catch (ArrayIndexOutOfBoundsException e) {
				//ignore
			}
			for (int j = i + 3; j < JOB_COUNT; j++) {
				assertState("2.2." + i + "." + j, jobs[j], Job.WAITING);
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
				assertTrue("Timeout waiting for job to cancel", false);
			}
		}
	}

	private void waitForCompletion() {
		int i = 0;
		assertTrue("Jobs completed that weren't scheduled", completedJobs.get() <= scheduledJobs.get());
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
				assertTrue("Timeout waiting for job to complete", false);
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
					assertTrue("Timeout waiting for job in family " + type.getType() + "to be canceled ", false);
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
				assertTrue("Timeout waiting for job to start. Job: " + job + ", state: " + job.getState(), false);
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