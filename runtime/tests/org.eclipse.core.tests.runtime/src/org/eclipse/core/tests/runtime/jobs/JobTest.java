/*******************************************************************************
 *  Copyright (c) 2003, 2018 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM - Initial API and implementation
 *     Stephan Wahlbrink  - Test fix for bug 200997.
 *     Dmitry Karasik - Test cases for bug 255384
 *     Jan Koehnlein - Test case for bug 60964 (454698)
 *     Alexander Kurtakov <akurtako@redhat.com> - bug 458490
 *     Thirumala Reddy Mutchukota (thirumala@google.com) -
 *     		Bug 105821, Support for Job#join with timeout and progress monitor
 *******************************************************************************/
package org.eclipse.core.tests.runtime.jobs;

import static org.junit.Assert.assertNotEquals;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.atomic.*;
import org.eclipse.core.internal.jobs.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.core.tests.harness.*;
import org.junit.Assert;

/**
 * Tests the implemented get/set methods of the abstract class Job
 */
@SuppressWarnings("restriction")
public class JobTest extends AbstractJobTest {
	protected Job longJob;
	protected Job shortJob;
	private FussyProgressProvider progressProvider;

	public JobTest(String name) {
		super(name);
	}

	//see bug #43591
	public void _testDone() {
		//calling the done method on a job that is not executing asynchronously should have no effect

		shortJob.done(Status.OK_STATUS);
		assertTrue("1.0", shortJob.getResult() == null);

		shortJob.done(Status.CANCEL_STATUS);
		assertTrue("2.0", shortJob.getResult() == null);

		//calling the done method after the job is scheduled
		shortJob.schedule();
		shortJob.done(Status.CANCEL_STATUS);
		waitForState(shortJob, Job.NONE);

		//the done call should be ignored, and the job should finish execution normally
		assertTrue("3.0", shortJob.getResult().getSeverity() == IStatus.OK);

		shortJob.done(Status.CANCEL_STATUS);
		assertTrue("4.0", shortJob.getResult().getSeverity() == IStatus.OK);

		//calling the done method before a job is canceled
		longJob.schedule();
		waitForState(longJob, Job.RUNNING);
		longJob.done(Status.OK_STATUS);
		longJob.cancel();
		waitForState(longJob, Job.NONE);

		//the done call should be ignored, and the job status should still be canceled
		assertEquals("5.0", IStatus.CANCEL, longJob.getResult().getSeverity());

		longJob.done(Status.OK_STATUS);
		assertEquals("6.0", IStatus.CANCEL, longJob.getResult().getSeverity());

	}

	private void cancel(Job[] jobs) {
		for (Job job : jobs) {
			job.cancel();
		}
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		shortJob = new TestJob("Short Test Job", 10, 1); // job that tests wait on
		longJob = new TestJob("Long Test Job", 10000000, 10); // job that never finishes in time
		progressProvider = new FussyProgressProvider();
		Job.getJobManager().setProgressProvider(progressProvider);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		Job.getJobManager().setProgressProvider(null);
		shortJob.cancel();
		waitForState(shortJob, Job.NONE);
		longJob.cancel();
		waitForState(longJob, Job.NONE);
	}

	//see bug #43566
	public void testAsynchJob() {
		final AtomicIntegerArray status = new AtomicIntegerArray(new int[] { TestBarrier2.STATUS_WAIT_FOR_START });

		//execute a job asynchronously and check the result
		AsynchTestJob main = new AsynchTestJob("Test Asynch Finish", status, 0);

		assertNull("1.0", main.getThread());
		assertNull("2.0", main.getResult());
		//schedule the job to run
		main.schedule();
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_RUNNING);
		assertEquals("3.0", Job.RUNNING, main.getState());
		//the asynchronous process that assigns the thread the job is going to run in has not been started yet
		//the job is running in the thread provided to it by the manager
		assertTrue("3.1" + main.getThread().getName(), main.getThread() instanceof Worker);

		status.set(0, TestBarrier2.STATUS_START);
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_WAIT_FOR_START);

		//the asynchronous process has been started, but the set thread method has not been called yet
		assertTrue("3.2", main.getThread() instanceof Worker);

		status.set(0, TestBarrier2.STATUS_WAIT_FOR_RUN);

		//make sure the job has set the thread it is going to run in
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_RUNNING);

		assertEquals("3.3", TestBarrier2.STATUS_RUNNING, status.get(0));
		assertTrue("3.4", main.getThread() instanceof AsynchExecThread);

		//let the job run
		status.set(0, TestBarrier2.STATUS_WAIT_FOR_DONE);
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_DONE);
		waitForState(main, Job.NONE);

		//after the job is finished, the thread should be reset
		assertEquals("4.0", Job.NONE, main.getState());
		assertEquals("4.1", IStatus.OK, main.getResult().getSeverity());
		assertNull("4.2", main.getThread());

		//reset status
		status.set(0, TestBarrier2.STATUS_WAIT_FOR_START);

		//schedule the job to run again
		main.schedule();
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_RUNNING);
		assertEquals("5.0", Job.RUNNING, main.getState());

		//the asynchronous process that assigns the thread the job is going to run in has not been started yet
		//job is running in the thread provided by the manager
		assertTrue("5.1", main.getThread() instanceof Worker);

		status.set(0, TestBarrier2.STATUS_START);
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_WAIT_FOR_START);

		//the asynchronous process has been started, but the set thread method has not been called yet
		assertTrue("5.2", main.getThread() instanceof Worker);

		status.set(0, TestBarrier2.STATUS_WAIT_FOR_RUN);

		//make sure the job has set the thread it is going to run in
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_RUNNING);

		assertEquals("5.3", TestBarrier2.STATUS_RUNNING, status.get(0));
		assertTrue("5.4", main.getThread() instanceof AsynchExecThread);

		//cancel the job, then let the job get the cancellation request
		main.cancel();
		status.set(0, TestBarrier2.STATUS_WAIT_FOR_DONE);
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_DONE);
		waitForState(main, Job.NONE);

		//thread should be reset to null after cancellation
		assertEquals("6.0", Job.NONE, main.getState());
		assertEquals("6.1", IStatus.CANCEL, main.getResult().getSeverity());
		assertNull("6.2", main.getThread());
	}

	public void testAsynchJobComplex() {
		final AtomicIntegerArray status = new AtomicIntegerArray(new int[] { TestBarrier2.STATUS_WAIT_FOR_START,
				TestBarrier2.STATUS_WAIT_FOR_START, TestBarrier2.STATUS_WAIT_FOR_START, TestBarrier2.STATUS_WAIT_FOR_START,
				TestBarrier2.STATUS_WAIT_FOR_START });

		//test the interaction of several asynchronous jobs
		AsynchTestJob[] jobs = new AsynchTestJob[5];

		for (int i = 0; i < jobs.length; i++) {
			jobs[i] = new AsynchTestJob("TestJob" + (i + 1), status, i);
			assertNull("1." + i, jobs[i].getThread());
			assertNull("2." + i, jobs[i].getResult());
			jobs[i].schedule();
			// status.set(i, TestBarrier.STATUS_START;
		}
		//all the jobs should be running at the same time
		waitForStart(jobs, status);

		//every job should now be waiting for the STATUS_START flag
		for (int i = 0; i < status.length(); i++) {
			assertEquals("3." + i, Job.RUNNING, jobs[i].getState());
			assertTrue("4." + i, jobs[i].getThread() instanceof Worker);
			status.set(i, TestBarrier2.STATUS_START);
		}

		for (int i = 0; i < status.length(); i++) {
			TestBarrier2.waitForStatus(status, i, TestBarrier2.STATUS_WAIT_FOR_START);
		}

		//every job should now be waiting for the STATUS_WAIT_FOR_RUN flag
		for (int i = 0; i < status.length(); i++) {
			assertTrue("5. " + i, jobs[i].getThread() instanceof Worker);
			status.set(i, TestBarrier2.STATUS_WAIT_FOR_RUN);
		}

		//wait until all jobs are in the running state
		for (int i = 0; i < status.length(); i++) {
			TestBarrier2.waitForStatus(status, i, TestBarrier2.STATUS_RUNNING);
		}

		//let the jobs execute
		for (int i = 0; i < status.length(); i++) {
			assertTrue("6. " + i, jobs[i].getThread() instanceof AsynchExecThread);
			status.set(i, TestBarrier2.STATUS_WAIT_FOR_DONE);
		}

		for (int i = 0; i < status.length(); i++) {
			TestBarrier2.waitForStatus(status, i, TestBarrier2.STATUS_DONE);
		}

		for (int i = 0; i < jobs.length; i++) {
			try {
				jobs[i].join();
			} catch (InterruptedException e) {
				fail("4.99", e);
			}
		}
		//the status for every job should be STATUS_OK
		//the threads should have been reset to null
		for (int i = 0; i < status.length(); i++) {
			assertEquals("7." + i, TestBarrier2.STATUS_DONE, status.get(i));
			assertEquals("8." + i, Job.NONE, jobs[i].getState());
			assertEquals("9." + i, IStatus.OK, jobs[i].getResult().getSeverity());
			assertNull("10." + i, jobs[i].getThread());
		}
	}

	public void testAsynchJobConflict() {
		final AtomicIntegerArray status = new AtomicIntegerArray(new int[] { TestBarrier2.STATUS_WAIT_FOR_START,
				TestBarrier2.STATUS_WAIT_FOR_START, TestBarrier2.STATUS_WAIT_FOR_START, TestBarrier2.STATUS_WAIT_FOR_START,
				TestBarrier2.STATUS_WAIT_FOR_START });

		//test the interaction of several asynchronous jobs when a conflicting rule is assigned to some of them
		AsynchTestJob[] jobs = new AsynchTestJob[5];

		ISchedulingRule rule = new IdentityRule();

		for (int i = 0; i < jobs.length; i++) {
			jobs[i] = new AsynchTestJob("TestJob" + (i + 1), status, i);
			assertNull("1." + i, jobs[i].getThread());
			assertNull("2." + i, jobs[i].getResult());
			if (i < 2) {
				jobs[i].schedule();
			} else if (i > 2) {
				jobs[i].setRule(rule);
			} else {
				jobs[i].setRule(rule);
				jobs[i].schedule();
			}

		}

		//these 3 jobs should be waiting for the STATUS_START flag
		for (int i = 0; i < 3; i++) {
			TestBarrier2.waitForStatus(status, i, TestBarrier2.STATUS_RUNNING);
			assertEquals("3." + i, Job.RUNNING, jobs[i].getState());
			assertTrue("4." + i, jobs[i].getThread() instanceof Worker);
			status.set(i, TestBarrier2.STATUS_START);
		}

		//the first 3 jobs should be running at the same time
		for (int i = 0; i < 3; i++) {
			TestBarrier2.waitForStatus(status, i, TestBarrier2.STATUS_WAIT_FOR_START);
		}

		//the 3 jobs should now be waiting for the STATUS_WAIT_FOR_RUN flag
		for (int i = 0; i < 3; i++) {
			assertTrue("5. " + i, jobs[i].getThread() instanceof Worker);
			status.set(i, TestBarrier2.STATUS_WAIT_FOR_RUN);
		}

		//wait until jobs block on running state
		for (int i = 0; i < 3; i++) {
			TestBarrier2.waitForStatus(status, i, TestBarrier2.STATUS_RUNNING);
		}

		//schedule the 2 remaining jobs
		jobs[3].schedule();
		jobs[4].schedule();

		//the 2 newly scheduled jobs should be waiting since they conflict with the third job
		//no threads were assigned to them yet
		assertEquals("6.1", Job.WAITING, jobs[3].getState());
		assertNull("6.2", jobs[3].getThread());
		assertEquals("6.3", Job.WAITING, jobs[4].getState());
		assertNull("6.4", jobs[4].getThread());

		//let the two non-conflicting jobs execute together
		for (int i = 0; i < 2; i++) {
			assertTrue("7. " + i, jobs[i].getThread() instanceof AsynchExecThread);
			status.set(i, TestBarrier2.STATUS_WAIT_FOR_DONE);
		}
		//wait until the non-conflicting jobs are done
		TestBarrier2.waitForStatus(status, 1, TestBarrier2.STATUS_DONE);

		//the third job should still be in the running state
		assertEquals("8.1", Job.RUNNING, jobs[2].getState());
		//the 2 conflicting jobs should still be in the waiting state
		assertEquals("8.2", Job.WAITING, jobs[3].getState());
		assertEquals("8.3", Job.WAITING, jobs[4].getState());

		//let the third job finish execution
		assertTrue("8.4", jobs[2].getThread() instanceof AsynchExecThread);
		status.set(2, TestBarrier2.STATUS_WAIT_FOR_DONE);

		//wait until the third job is done
		TestBarrier2.waitForStatus(status, 2, TestBarrier2.STATUS_DONE);

		//the fourth job should now start running, the fifth job should still be waiting
		TestBarrier2.waitForStatus(status, 3, TestBarrier2.STATUS_RUNNING);
		assertEquals("9.1", Job.RUNNING, jobs[3].getState());
		assertEquals("9.2", Job.WAITING, jobs[4].getState());

		//let the fourth job run, the fifth job is still waiting
		status.set(3, TestBarrier2.STATUS_START);
		assertEquals("9.3", Job.WAITING, jobs[4].getState());
		TestBarrier2.waitForStatus(status, 3, TestBarrier2.STATUS_WAIT_FOR_START);
		status.set(3, TestBarrier2.STATUS_WAIT_FOR_RUN);
		assertEquals("9.4", Job.WAITING, jobs[4].getState());
		TestBarrier2.waitForStatus(status, 3, TestBarrier2.STATUS_RUNNING);
		assertEquals("9.5", Job.WAITING, jobs[4].getState());

		//cancel the fifth job, finish the fourth job
		jobs[4].cancel();
		assertTrue("9.6", jobs[3].getThread() instanceof AsynchExecThread);
		status.set(3, TestBarrier2.STATUS_WAIT_FOR_DONE);

		//wait until the fourth job is done
		TestBarrier2.waitForStatus(status, 3, TestBarrier2.STATUS_DONE);

		//the status for the first 4 jobs should be STATUS_OK
		//the threads should have been reset to null
		for (int i = 0; i < status.length() - 1; i++) {
			assertEquals("10." + i, TestBarrier2.STATUS_DONE, status.get(i));
			waitForState(jobs[i], Job.NONE);
			assertEquals("11." + i, Job.NONE, jobs[i].getState());
			assertEquals("12." + i, IStatus.OK, jobs[i].getResult().getSeverity());
			assertNull("13." + i, jobs[i].getThread());
		}

		//the fifth job should have null as its status (it never finished running)
		//the thread for it should have also been reset
		assertEquals("14.1", TestBarrier2.STATUS_WAIT_FOR_START, status.get(4));
		assertEquals("14.2", Job.NONE, jobs[4].getState());
		assertNull("14.3", jobs[4].getResult());
		assertNull("14.4", jobs[4].getThread());
	}

	/**
	 * Tests cancelation of a job from the aboutToRun job event.
	 * See bug 70434 for details.
	 */
	public void testCancelFromAboutToRun() {
		final int[] doneCount = new int[] {0};
		final int[] runningCount = new int[] {0};
		TestJob job = new TestJob("testCancelFromAboutToRun", 0, 0);
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void aboutToRun(IJobChangeEvent event) {
				event.getJob().cancel();
			}

			@Override
			public void done(IJobChangeEvent event) {
				doneCount[0]++;
			}

			@Override
			public void running(IJobChangeEvent event) {
				runningCount[0]++;
			}
		});
		job.schedule();
		try {
			job.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
			fail("0.99 " + e.getMessage());
		}
		assertEquals("1.0", 0, job.getRunCount());
		assertEquals("1.1", 1, doneCount[0]);
		assertEquals("1.2", 0, runningCount[0]);
	}

	/**
	 * Basic test of {@link Job#shouldRun()}.
	 */
	public void testShouldRun() {
		class ShouldRunJob extends Job {
			public ShouldRunJob() {
				super("ShouldRunJob");
			}

			int runCount = 0;

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				++runCount;
				return Status.OK_STATUS;
			}

			@Override
			public boolean shouldRun() {
				return false;
			}
		}
		ShouldRunJob j = new ShouldRunJob();
		j.schedule();
		try {
			j.join();
		} catch (InterruptedException e) {
			fail("0.99", e);
		}

		//ensure the job never ran
		assertEquals(0, j.runCount);

	}

	/**
	 * Test of an ill-behaving {@link Job#shouldRun()}.
	 */
	public void testShouldRunFailure() {
		class ShouldRunJob extends Job {
			public ShouldRunJob() {
				super("ShouldRunJob");
			}

			int runCount = 0;

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				++runCount;
				return Status.OK_STATUS;
			}

			@Override
			public boolean shouldRun() {
				throw new RuntimeException("Exception thrown on purpose as part of a test");
			}
		}
		ShouldRunJob j = new ShouldRunJob();
		j.schedule();
		waitForState(j, Job.NONE);

		//ensure the job never ran
		assertEquals(0, j.runCount);

	}

	/**
	 * Tests canceling a job from the shouldRun method. See bug 255384.
	 */
	public void testCancelShouldRun() {
		final String[] failure = new String[1];
		final Job j = new Job("Test") {
			AtomicInteger runningCount = new AtomicInteger();
			boolean cancelled;

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				if (runningCount.incrementAndGet() > 1) {
					failure[0] = "Multiple running at once!";
				}
				try {
					Thread.sleep(500);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if (runningCount.decrementAndGet() != 0) {
						failure[0] = "Multiple were running at once!";
					}
				}
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				return JobTest.this == family;
			}

			@Override
			public boolean shouldRun() {
				if (!cancelled) {
					cancelled = true;
					this.sleep();
					this.cancel();
					this.schedule();
					try {
						Thread.sleep(500);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				return true;
			}
		};
		j.schedule();
		try {
			Thread.sleep(1000);
			Job.getJobManager().join(this, null);
		} catch (OperationCanceledException | InterruptedException e) {
			fail("4.99", e);
		}
		assertNull(failure[0], failure[0]);
	}

	/**
	 * Tests the hook method {@link Job#canceling}.
	 */
	public void testCanceling() {
		final TestBarrier2 barrier = new TestBarrier2();
		barrier.setStatus(TestBarrier2.STATUS_WAIT_FOR_START);
		final int[] canceling = new int[] {0};
		Job job = new Job("Testing#testCanceling") {
			@Override
			protected void canceling() {
				canceling[0]++;
			}

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				barrier.setStatus(TestBarrier2.STATUS_WAIT_FOR_RUN);
				barrier.waitForStatus(TestBarrier2.STATUS_RUNNING);
				return Status.OK_STATUS;
			}
		};
		//schedule the job and wait on the barrier until it is running
		job.schedule();
		barrier.waitForStatus(TestBarrier2.STATUS_WAIT_FOR_RUN);
		assertEquals("1.0", 0, canceling[0]);
		job.cancel();
		assertEquals("1.1", 1, canceling[0]);
		job.cancel();
		assertEquals("1.2", 1, canceling[0]);
		//let the job finish
		barrier.setStatus(TestBarrier2.STATUS_RUNNING);
		waitForState(job, Job.NONE);
	}

	/**
	 * Tests the hook method {@link Job#canceling}.
	 */
	public void testCancelingByMonitor() {
		final TestBarrier2 barrier = new TestBarrier2();
		barrier.setStatus(TestBarrier2.STATUS_WAIT_FOR_START);
		final int[] canceling = new int[] {0};
		final IProgressMonitor[] jobmonitor = new IProgressMonitor[1];
		Job job = new Job("Testing#testCancelingByMonitor") {
			@Override
			protected void canceling() {
				canceling[0]++;
			}

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				jobmonitor[0] = monitor;
				barrier.setStatus(TestBarrier2.STATUS_WAIT_FOR_RUN);
				barrier.waitForStatus(TestBarrier2.STATUS_RUNNING);
				return Status.OK_STATUS;
			}
		};
		//run test twice to ensure job is left in a clean state after first cancelation
		for (int i = 0; i < 2; i++) {
			canceling[0] = 0;
			//schedule the job and wait on the barrier until it is running
			job.schedule();
			barrier.waitForStatus(TestBarrier2.STATUS_WAIT_FOR_RUN);
			assertEquals(i + ".1.0", 0, canceling[0]);
			jobmonitor[0].setCanceled(true);
			assertEquals(i + ".1.1", 1, canceling[0]);
			jobmonitor[0].setCanceled(true);
			assertEquals(i + ".1.2", 1, canceling[0]);
			//let the job finish
			barrier.setStatus(TestBarrier2.STATUS_RUNNING);
			waitForState(job, Job.NONE);
		}
	}

	public void testCancelAboutToScheduleLegacy() {
		JobListeners.setJobListenerTimeout(0);
		testCancelAboutToSchedule();
		JobListeners.resetJobListenerTimeout();
	}

	public void testCancelAboutToSchedule() {
		final boolean[] failure = new boolean[1];
		final Job j = new Job("testCancelAboutToSchedule") {
			AtomicInteger runningCount = new AtomicInteger();

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				if (runningCount.incrementAndGet() > 1) {
					failure[0] = true;
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					if (runningCount.decrementAndGet() != 0) {
						failure[0] = true;
					}
				}
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				return JobTest.this == family;
			}
		};
		JobChangeAdapter listener = new JobChangeAdapter() {
			boolean canceled = false;

			@Override
			public void scheduled(IJobChangeEvent event) {
				if (event.getJob().belongsTo(JobTest.this) && !canceled) {
					canceled = true;
					j.cancel();
					j.schedule();
					try {
						Thread.sleep(100);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		};
		Job.getJobManager().addJobChangeListener(listener);
		try {
			j.schedule();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				fail("4.99", e);
			}
			try {
				Job.getJobManager().join(this, null);
			} catch (OperationCanceledException | InterruptedException e) {
				fail("4.99", e);
			}
			assertFalse("1.0", failure[0]);
		} finally {
			Job.getJobManager().removeJobChangeListener(listener);
		}
	}

	public void testGetName() {
		assertEquals("1.0", "Short Test Job", shortJob.getName());
		assertEquals("1.1", "Long Test Job", longJob.getName());

		//try creating a job with a null name
		try {
			new TestJob(null);
			fail("2.0");
		} catch (RuntimeException e) {
			//should fail
		}
	}

	public void testGetPriority() {
		//set priorities to all allowed options
		//check if getPriority() returns proper result

		int[] priority = {Job.SHORT, Job.LONG, Job.INTERACTIVE, Job.BUILD, Job.DECORATE};

		for (int i = 0; i < priority.length; i++) {
			shortJob.setPriority(priority[i]);
			assertEquals("1." + i, priority[i], shortJob.getPriority());
		}
	}

	public void testGetProperty() {
		QualifiedName n1 = new QualifiedName("org.eclipse.core.tests.runtime", "p1");
		QualifiedName n2 = new QualifiedName("org.eclipse.core.tests.runtime", "p2");
		assertNull("1.0", shortJob.getProperty(n1));
		shortJob.setProperty(n1, null);
		assertNull("1.1", shortJob.getProperty(n1));
		shortJob.setProperty(n1, shortJob);
		assertEquals("1.2", shortJob.getProperty(n1), shortJob);
		assertNull("1.3", shortJob.getProperty(n2));
		shortJob.setProperty(n1, "hello");
		assertEquals("1.4", "hello", shortJob.getProperty(n1));
		shortJob.setProperty(n1, null);
		assertNull("1.5", shortJob.getProperty(n1));
		assertNull("1.6", shortJob.getProperty(n2));
	}

	public void testGetResult() {
		//execute a short job
		assertNull("1.0", shortJob.getResult());
		shortJob.schedule();
		waitForState(shortJob, Job.NONE);
		assertEquals("1.1", IStatus.OK, shortJob.getResult().getSeverity());

		//cancel a long job
		waitForState(longJob, Job.NONE);
		longJob.schedule();
		waitForState(longJob, Job.RUNNING);
		longJob.cancel();
		waitForState(longJob, Job.NONE);
		assertEquals("2.0", IStatus.CANCEL, longJob.getResult().getSeverity());
	}

	public void testGetRule() {
		//set several rules for the job, check if getRule returns the rule that was set
		//no rule was set yet
		assertNull("1.0", shortJob.getRule());

		shortJob.setRule(new IdentityRule());
		assertTrue("1.1", (shortJob.getRule() instanceof IdentityRule));

		ISchedulingRule rule = new PathRule("/testGetRule");
		shortJob.setRule(rule);
		assertEquals("1.2", shortJob.getRule(), rule);

		shortJob.setRule(null);
		assertNull("1.3", shortJob.getRule());
	}

	public void testGetThread() {
		//check that getThread returns the thread that was passed in setThread, when the job is not running
		//if the job is scheduled, only jobs that return the asynch_exec status will run in the indicated thread

		//main is not running now
		assertNull("1.0", shortJob.getThread());

		Thread t = new Thread();
		shortJob.setThread(t);
		assertEquals("1.1", shortJob.getThread(), t);

		shortJob.setThread(new Thread());
		assertNotEquals("1.2", shortJob.getThread(), t);

		shortJob.setThread(null);
		assertNull("1.3", shortJob.getThread());
	}

	public void testIsBlocking() {
		IdentityRule rule = new IdentityRule();
		TestJob high = new TestJob("TestIsBlocking.long", 10000, 100);
		high.setRule(rule);
		high.setPriority(Job.LONG);
		TestJob medium = new TestJob("TestIsBlocking.build", 10000, 100);
		medium.setRule(rule);
		medium.setPriority(Job.BUILD);
		TestJob low = new TestJob("TestIsBlocking.decorate", 10000, 100);
		low.setRule(rule);
		low.setPriority(Job.DECORATE);

		//start the build job, and make sure it is not blocking
		medium.schedule();
		waitForState(medium, Job.RUNNING);
		assertTrue("1.0", !medium.isBlocking());
		//schedule a lower priority job, and it should still not be blocking
		low.schedule();
		assertTrue("1.1", !medium.isBlocking());
		//schedule a higher priority job - now it should be blocking
		high.schedule();
		//wait for the high priority job to become blocked
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			//ignore
		}
		assertTrue("1.2", medium.isBlocking());

		//cancel everything
		Job[] jobs = new Job[] {high, medium, low};
		cancel(jobs);
		waitForState(jobs, Job.NONE);

		//a higher priority system job should not be blocking
		high.setSystem(true);
		medium.schedule();
		waitForState(medium, Job.RUNNING);
		high.schedule();
		assertTrue("2.0", !medium.isBlocking());

		//clean up
		cancel(jobs);
		waitForState(jobs, Job.NONE);
	}

	// @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=60964
	public void testIsBlocking2() throws InterruptedException {
		final IJobManager manager = Job.getJobManager();
		final ISchedulingRule rule = new IdentityRule();
		Thread thread = new Thread("testIsBlocking2") {
			@Override
			public void run() {
				try {
					manager.beginRule(rule, null);
				} finally {
					manager.endRule(rule);
				}
			}
		};
		try {
			manager.beginRule(rule, null);
			thread.start();
			while (thread.getState() != Thread.State.WAITING) {
				Thread.sleep(50);
			}
			assertTrue(manager.currentJob().isBlocking());
		} finally {
			manager.endRule(rule);
			thread.join();
		}
	}

	public void testIsSystem() {
		//reset the system parameter several times
		shortJob.setUser(false);
		shortJob.setSystem(false);
		assertTrue("1.0", !shortJob.isUser());
		assertTrue("1.1", !shortJob.isSystem());
		shortJob.setSystem(true);
		assertTrue("1.2", !shortJob.isUser());
		assertTrue("1.3", shortJob.isSystem());
		shortJob.setSystem(false);
		assertTrue("1.4", !shortJob.isUser());
		assertTrue("1.5", !shortJob.isSystem());
	}

	public void testIsUser() {
		//reset the user parameter several times
		shortJob.setUser(false);
		shortJob.setSystem(false);
		assertTrue("1.0", !shortJob.isUser());
		assertTrue("1.1", !shortJob.isSystem());
		shortJob.setUser(true);
		assertTrue("1.2", shortJob.isUser());
		assertTrue("1.3", !shortJob.isSystem());
		shortJob.setUser(false);
		assertTrue("1.4", !shortJob.isUser());
		assertTrue("1.5", !shortJob.isSystem());
	}

	public void testJoin() {
		longJob.schedule(100000);
		//create a thread that will join the test job
		final AtomicIntegerArray status = new AtomicIntegerArray( new int[1]);
		status.set(0, TestBarrier2.STATUS_WAIT_FOR_START);
		Thread t = new Thread(() -> {
			status.set(0, TestBarrier2.STATUS_START);
			try {
				longJob.join();
			} catch (InterruptedException e) {
				Assert.fail("0.99");
			}
			status.set(0, TestBarrier2.STATUS_DONE);
		});
		t.start();
		TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_START);
		assertEquals("1.0", TestBarrier2.STATUS_START, status.get(0));
		//putting the job to sleep should not affect the join call
		longJob.sleep();
		//give a chance for the sleep to take effect
		sleep(100);
		assertEquals("1.0", TestBarrier2.STATUS_START, status.get(0));
		//similarly waking the job up should not affect the join
		longJob.wakeUp(100000);
		sleep(100);
		assertEquals("1.0", TestBarrier2.STATUS_START, status.get(0));

		//finally canceling the job will cause the join to return
		longJob.cancel();
		TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_DONE);
	}

	public void testJoinWithTimeout() {
		longJob.schedule();
		final long timeout = 1000;
		final long duration[] = {-1};
		// Create a thread that will join the test job
		final AtomicIntegerArray status = new AtomicIntegerArray(new int[1]);
		status.set(0, TestBarrier2.STATUS_WAIT_FOR_START);
		Thread t = new Thread(() -> {
			status.set(0, TestBarrier2.STATUS_START);
			try {
				long start = now();
				longJob.join(timeout, null);
				duration[0] = now() - start;
			} catch (InterruptedException e1) {
				Assert.fail("0.88");
			} catch (OperationCanceledException e2) {
				Assert.fail("0.99");
			}
			status.set(0, TestBarrier2.STATUS_DONE);
		});
		t.start();
		TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_START);
		assertEquals("1.0", TestBarrier2.STATUS_START, status.get(0));
		int i = 0;
		long n0 = System.nanoTime();
		for (; i < 999999; i++) {
			if (status.get(0) == TestBarrier2.STATUS_DONE) {
				// Verify that the join call is blocked for at least for the duration of given timeout
				assertTrue("2.0 duration: " + Arrays.toString(duration) + " timeout: " + timeout, duration[0] >= timeout);
				break;
			}
			sleep(1);
		}
		long n1 = System.nanoTime();
		// Verify that the join call is finished with in reasonable time of 1100 ms (given timeout + 100ms)
		long took = (n1 - n0) / 1_000_000;
		assertTrue("3.0 took:" + took, took < timeout + 100);
		assertTrue("3.1 took:" + took, took >= timeout - 100);
		// Verify that the join call is still running
		assertEquals("4.0", Job.RUNNING, longJob.getState());
		// Finally cancel the job
		longJob.cancel();
		waitForCompletion(longJob);
	}

	public void testJoinWithProgressMonitor() {
		shortJob.schedule(100000);
		// Create a progress monitor for the join call
		final FussyProgressMonitor monitor = new FussyProgressMonitor();
		// Create a thread that will join the test job
		final AtomicIntegerArray status = new AtomicIntegerArray(new int[1]);
		status.set(0, TestBarrier2.STATUS_WAIT_FOR_START);
		Thread t = new Thread(() -> {
			status.set(0, TestBarrier2.STATUS_START);
			try {
				shortJob.join(0, monitor);
			} catch (InterruptedException e1) {
				Assert.fail("0.88");
			} catch (OperationCanceledException e2) {
				Assert.fail("0.99");
			}
			status.set(0, TestBarrier2.STATUS_DONE);
		});
		t.start();
		TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_START);
		assertEquals("1.0", TestBarrier2.STATUS_START, status.get(0));
		// Wakeup the job to get the join call to complete
		shortJob.wakeUp();
		TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_DONE);
		monitor.sanityCheck();
	}

	public void testJoinWithCancelingMonitor() {
		longJob.schedule();
		// Create a progress monitor for the join call
		final FussyProgressMonitor monitor = new FussyProgressMonitor();
		// Create a thread that will join the test job
		final AtomicIntegerArray status = new AtomicIntegerArray(new int[1]);
		status.set(0, TestBarrier2.STATUS_WAIT_FOR_START);
		Thread t = new Thread(() -> {
			status.set(0, TestBarrier2.STATUS_START);
			try {
				longJob.join(0, monitor);
			} catch (InterruptedException e1) {
				Assert.fail("0.88");
			} catch (OperationCanceledException e2) {
				// expected
			}
			status.set(0, TestBarrier2.STATUS_DONE);
		});
		t.start();
		TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_START);
		assertEquals("1.0", TestBarrier2.STATUS_START, status.get(0));

		// Cancel the monitor that is attached to the join call
		monitor.setCanceled(true);
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_DONE);
		monitor.sanityCheck();
		// Verify that the join call is still running
		assertEquals("2.0", Job.RUNNING, longJob.getState());
		// Finally cancel the job
		longJob.cancel();
		waitForCompletion(longJob);
	}

	public void testJoinInterruptNonUIThread() throws InterruptedException {
		final Job job = new TestJob("job", 1000, 100);
		Thread t = new Thread(() -> {
			job.schedule();
			try {
				job.join();
			} catch (InterruptedException e) {
				job.cancel();
			}
		});
		t.start();
		// make sure the job is running before we interrupt the thread
		waitForState(job, Job.RUNNING);
		t.interrupt();
		job.join();
		assertEquals("Thread not interrupted", Status.CANCEL_STATUS, job.getResult());
	}

	public void testJoinInterruptUIThread() throws InterruptedException {
		final Job job = new TestJob("job", 3, org.eclipse.core.internal.jobs.JobManager.MAX_WAIT_INTERVAL);
		Thread t = new Thread(() -> {
			job.schedule();
			try {
				job.join();
			} catch (InterruptedException e) {
				job.cancel();
			}
		});
		try {
			Job.getJobManager().setLockListener(new LockListener() {
				@Override
				public boolean canBlock() {
					// pretend to be the UI thread
					return false;
				}
			});
			t.start();
			// make sure the job is running before we interrupt the thread
			waitForState(job, Job.RUNNING);
			t.interrupt();
			job.join();
			assertEquals("Thread interrupted", Status.OK_STATUS, job.getResult());
		} finally {
			Job.getJobManager().setLockListener(null);
		}
	}

	/**
	 * Asserts that the LockListener is called correctly during invocation of
	 * {@link Job#join()}.
	 * See bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=195839.
	 */
	public void testJoinLockListener() {
		Job testJob = new TestJob("testJoinLockListener", 5, 500);
		TestLockListener lockListener = new TestLockListener();
		try {
			Job.getJobManager().setLockListener(lockListener);
			testJob.join();
		} catch (OperationCanceledException | InterruptedException e) {
			fail("4.99", e);
		} finally {
			Job.getJobManager().setLockListener(null);
		}
		lockListener.assertNotWaiting("1.0");
	}

	/**
	 * Tests a job change listener that throws an exception.
	 * This would previously cause join attempts on that job to
	 * hang indefinitely because they would miss the notification
	 * required to end the join.
	 */
	public void testJoinFailingListener() {
		shortJob.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				throw new RuntimeException("This exception thrown on purpose as part of a test");
			}
		});
		final AtomicIntegerArray status = new AtomicIntegerArray(new int[1]);
		//create a thread that will join the job
		Thread t = new Thread(() -> {
			status.set(0, TestBarrier2.STATUS_START);
			try {
				shortJob.join();
			} catch (InterruptedException e) {
				Assert.fail("0.99");
			}
			status.set(0, TestBarrier2.STATUS_DONE);
		});
		//schedule the job and then fork the thread to join it
		shortJob.schedule();
		t.start();
		//wait until the join succeeds
		TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_DONE);
	}

	/**
	 * Tests that a job joining itself is an error.
	 */
	public void testJoinSelf() {
		final Exception[] failure = new Exception[1];
		Job selfJoiner = new Job("testJoinSelf") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					this.join();
				} catch (RuntimeException | InterruptedException e) {
					failure[0] = e;
				}
				return Status.OK_STATUS;
			}
		};
		selfJoiner.schedule();
		try {
			selfJoiner.join();
		} catch (InterruptedException e) {
			fail("Unexpected interrupt");
		}
		assertNotNull("1.0", failure[0]);
	}

	/**
	 * This is a regression test for bug 60323. If a job change listener
	 * removed itself from the listener list during the done() change event,
	 * then anyone joining on that job would hang forever.
	 */
	public void testJoinRemoveListener() {
		final IJobChangeListener listener = new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				shortJob.removeJobChangeListener(this);
			}
		};
		shortJob.addJobChangeListener(listener);
		final AtomicIntegerArray status = new AtomicIntegerArray(new int[1]);
		//create a thread that will join the job
		Thread t = new Thread(() -> {
			status.set(0, TestBarrier2.STATUS_START);
			try {
				shortJob.join();
			} catch (InterruptedException e) {
				Assert.fail("0.99");
			}
			status.set(0, TestBarrier2.STATUS_DONE);
		});
		//schedule the job and then fork the thread to join it
		shortJob.schedule();
		t.start();
		//wait until the join succeeds
		TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_DONE);
	}

	/*
	 * Test that a canceled job is rescheduled
	 */
	public void testRescheduleCancel() {
		final AtomicIntegerArray status = new AtomicIntegerArray(new int[] { TestBarrier2.STATUS_WAIT_FOR_START });
		Job job = new Job("Testing") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					status.set(0, TestBarrier2.STATUS_WAIT_FOR_RUN);
					TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_RUNNING);
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		//schedule the job, cancel it, then reschedule
		job.schedule();
		TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_WAIT_FOR_RUN);
		job.cancel();
		job.schedule();
		//let the first iteration of the job finish
		status.set(0, TestBarrier2.STATUS_RUNNING);
		//wait until the job runs again
		TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_WAIT_FOR_RUN);
		//let the job finish
		status.set(0, TestBarrier2.STATUS_RUNNING);
		waitForState(job, Job.NONE);
	}

	/*
	 * Test that multiple reschedules of the same job while it is running
	 * only remembers the last reschedule request
	 */
	public void testRescheduleComplex() {
		final AtomicIntegerArray status = new AtomicIntegerArray(new int[] { TestBarrier2.STATUS_WAIT_FOR_START });
		final int[] runCount = new int[] {0};
		Job job = new Job("Testing") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					status.set(0, TestBarrier2.STATUS_WAIT_FOR_RUN);
					TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_RUNNING);
					monitor.worked(1);
				} finally {
					runCount[0]++;
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		//schedule the job, reschedule when it is running
		job.schedule();
		TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_WAIT_FOR_RUN);
		//the last schedule value should win
		job.schedule(1000000);
		job.schedule(3000);
		job.schedule(200000000);
		job.schedule();
		status.set(0, TestBarrier2.STATUS_RUNNING);
		//wait until the job runs again
		TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_WAIT_FOR_RUN);
		assertEquals("1.0", 1, runCount[0]);
		//let the job finish
		status.set(0, TestBarrier2.STATUS_RUNNING);
		waitForState(job, Job.NONE);
		assertEquals("1.0", 2, runCount[0]);
	}

	/*
	 * Reschedule a running job with a delay
	 */
	public void testRescheduleDelay() {
		final AtomicIntegerArray status = new AtomicIntegerArray(new int[] { TestBarrier2.STATUS_WAIT_FOR_START });
		final int[] runCount = new int[] {0};
		Job job = new Job("Testing") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					status.set(0, TestBarrier2.STATUS_WAIT_FOR_RUN);
					TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_RUNNING);
					monitor.worked(1);
				} finally {
					runCount[0]++;
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		//schedule the job, reschedule when it is running
		job.schedule();
		TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_WAIT_FOR_RUN);
		job.schedule(1000000);
		status.set(0, TestBarrier2.STATUS_RUNNING);
		//now wait until the job is scheduled again and put to sleep
		waitForState(job, Job.SLEEPING);
		assertEquals("1.0", 1, runCount[0]);

		//reschedule the job while it is sleeping
		job.schedule();
		//wake up the currently sleeping job
		job.wakeUp();
		TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_WAIT_FOR_RUN);
		status.set(0, TestBarrier2.STATUS_RUNNING);
		//make sure the job was not rescheduled while the executing job was sleeping
		waitForState(job, Job.NONE);
		assertEquals("1.0", Job.NONE, job.getState());
		assertEquals("1.0", 2, runCount[0]);
	}

	/*
	 * Schedule a simple job that repeats several times from within the run method.
	 */
	public void testRescheduleRepeat() {
		final int[] count = new int[] {0};
		final int REPEATS = 10;
		Job job = new Job("testRescheduleRepeat") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				count[0]++;
				schedule();
				return Status.OK_STATUS;
			}

			@Override
			public boolean shouldSchedule() {
				return count[0] < REPEATS;
			}
		};
		job.schedule();
		int timeout = 0;
		while (timeout++ < 100 && count[0] < REPEATS) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				//ignore
			}
		}
		assertTrue("1.0", timeout < 100);
		assertEquals("1.1", REPEATS, count[0]);
	}

	/*
	 * Schedule a long running job several times without blocking current thread.
	 */
	public void testRescheduleRepeating() {
		AtomicLong runCount = new AtomicLong();
		AtomicLong scheduledCount = new AtomicLong();
		AtomicBoolean keepRunning = new AtomicBoolean(true);
		Job job = new Job("testRescheduleRepeat") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				runCount.incrementAndGet();
				while (!monitor.isCanceled() && keepRunning.get()) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						Thread.interrupted();
						return Status.CANCEL_STATUS;
					}
				}
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				return family == this;
			}
		};

		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void scheduled(IJobChangeEvent event) {
				scheduledCount.incrementAndGet();
			}
		});
		job.schedule();
		int timeout = 0;
		while (timeout++ < 100 && scheduledCount.get() == 0) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// ignore
			}
		}
		for (int i = 0; i < 100; i++) {
			job.schedule(i);
		}
		timeout = 0;
		while (timeout++ < 100 && runCount.get() < 1) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// ignore
			}
		}
		try {
			Job[] found = Job.getJobManager().find(job);
			assertEquals("Job should still run", 1, found.length);
			assertSame("Job should still run", job, found[0]);
			long currentRuns = runCount.get();
			if (currentRuns != 1) {
				fail("Expected to see exact one job execution, but saw: " + currentRuns);
			}
		} finally {
			keepRunning.set(false);
		}
	}

	/*
	 * Schedule a simple job that repeats several times from within the run method.
	 */

	public void testRescheduleRepeatWithDelay() {
		final int[] count = new int[] {0};
		final int REPEATS = 10;
		Job job = new Job("testRescheduleRepeat") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				count[0]++;
				schedule(10);
				return Status.OK_STATUS;
			}

			@Override
			public boolean shouldSchedule() {
				return count[0] < REPEATS;
			}
		};
		job.schedule();
		int timeout = 0;
		while (timeout++ < 100 && count[0] < REPEATS) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				//ignore
			}
		}
		assertTrue("1.0", timeout < 100);
		assertEquals("1.1", REPEATS, count[0]);
	}

	/*
	 * Schedule a job to run, and then reschedule it
	 */
	public void testRescheduleSimple() {
		final AtomicIntegerArray status = new AtomicIntegerArray(new int[] { TestBarrier2.STATUS_WAIT_FOR_START });
		Job job = new Job("testRescheduleSimple") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					status.set(0, TestBarrier2.STATUS_WAIT_FOR_RUN);
					TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_RUNNING);
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		//schedule the job, reschedule when it is running
		job.schedule();
		TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_WAIT_FOR_RUN);
		job.schedule();
		status.set(0, TestBarrier2.STATUS_RUNNING);
		//wait until the job runs again
		TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_WAIT_FOR_RUN);
		//let the job finish
		status.set(0, TestBarrier2.STATUS_RUNNING);
		waitForState(job, Job.NONE);

		//the job should only run once the second time around
		job.schedule();
		TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_WAIT_FOR_RUN);
		//let the job finish
		status.set(0, TestBarrier2.STATUS_RUNNING);
		//wait until the job truly finishes and has a chance to be rescheduled (it shouldn't reschedule)
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			//ignore
		}
		waitForState(job, Job.NONE);
	}

	/*
	 * Reschedule a waiting job.
	 */
	public void testRescheduleWaiting() {
		final AtomicIntegerArray status = new AtomicIntegerArray(
				new int[] { TestBarrier2.STATUS_WAIT_FOR_START, TestBarrier2.STATUS_WAIT_FOR_START });
		final int[] runCount = new int[] {0};
		final ISchedulingRule rule = new IdentityRule();
		Job first = new Job("Testing1") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					status.set(0, TestBarrier2.STATUS_WAIT_FOR_RUN);
					TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_RUNNING);
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		Job second = new Job("Testing2") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					status.set(1, TestBarrier2.STATUS_WAIT_FOR_RUN);
					TestBarrier2.waitForStatus(status, 1, TestBarrier2.STATUS_RUNNING);
					monitor.worked(1);
				} finally {
					runCount[0]++;
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		//set the same rule for both jobs so that the second job would have to wait
		first.setRule(rule);
		first.schedule();
		second.setRule(rule);
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_WAIT_FOR_RUN);
		second.schedule();
		waitForState(second, Job.WAITING);
		//reschedule the second job while it is waiting
		second.schedule();
		//let the first job finish
		status.set(0, TestBarrier2.STATUS_RUNNING);
		//the second job will start
		TestBarrier2.waitForStatus(status, 1, TestBarrier2.STATUS_WAIT_FOR_RUN);
		//let the second job finish
		status.set(1, TestBarrier2.STATUS_RUNNING);

		//make sure the second job was not rescheduled
		waitForState(second, Job.NONE);
		assertEquals("2.0", Job.NONE, second.getState());
		assertEquals("2.1", 1, runCount[0]);
	}

	/**
	 * It's OK to reschedule the same job from the JobChangeAdapter done()
	 * notification.
	 *
	 * @see <a href=
	 *      "https://github.com/eclipse-jdt/eclipse.jdt.debug/issues/122">eclipse.jdt.debug/issues/122</a>
	 */
	public void testRescheduleFromDone() throws InterruptedException {
		AtomicInteger runningCount = new AtomicInteger();
		final Job j = new Job("testCancelAboutToSchedule") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				runningCount.incrementAndGet();
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				return JobTest.this == family;
			}
		};
		JobChangeAdapter listener = new JobChangeAdapter() {

			@Override
			public void done(IJobChangeEvent event) {
				if (runningCount.get() < 3) {
					j.schedule();
				}
			}
		};
		Job.getJobManager().addJobChangeListener(listener);
		try {
			j.schedule();
			int ONE_SECOND = 1_000_000_000;
			long n0 = System.nanoTime();
			while (runningCount.get() < 3) {
				Thread.yield();
				assertTrue("timeout runningCount=" + runningCount.get(), (System.nanoTime() - n0) < ONE_SECOND);
			}
			j.join();
			assertEquals(3, runningCount.get());
		} finally {
			Job.getJobManager().removeJobChangeListener(listener);
		}
	}

	/*
	 * see bug #43458
	 */
	public void testSetPriority() {
		int[] wrongPriority = {1000, -Job.DECORATE, 25, 0, 5, Job.INTERACTIVE - Job.BUILD};

		for (int i = 0; i < wrongPriority.length; i++) {
			//set priority to non-existent type
			try {
				shortJob.setPriority(wrongPriority[i]);
				fail("1." + (i + 1));
			} catch (RuntimeException e) {
				//should fail
			}
		}
	}

	/**
	 * Tests the API methods Job.setProgressGroup
	 */
	public void testSetProgressGroup() {
		final TestBarrier2 barrier = new TestBarrier2();
		Job job = new Job("testSetProgressGroup") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				barrier.setStatus(TestBarrier2.STATUS_RUNNING);
				barrier.waitForStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				return Status.OK_STATUS;
			}
		};
		//null group
		try {
			job.setProgressGroup(null, 5);
			fail("1.0");
		} catch (RuntimeException e) {
			//should fail
		}
		IProgressMonitor group = Job.getJobManager().createProgressGroup();
		group.beginTask("Group task name", 10);
		job.setProgressGroup(group, 5);

		//ignore changes to group while waiting or running
		job.schedule(100);
		job.setProgressGroup(group, 0);
		//wait until job starts and try to set the progress group
		barrier.waitForStatus(TestBarrier2.STATUS_RUNNING);
		job.setProgressGroup(group, 0);

		//ensure cancelation still works
		job.cancel();
		barrier.setStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);
		waitForState(job, Job.NONE);
		assertEquals("1.0", IStatus.CANCEL, job.getResult().getSeverity());
		group.done();
	}

	/*
	 * see bug #43459
	 */
	public void testSetRule() {
		//setting a scheduling rule for a job after it was already scheduled should throw an exception
		shortJob.setRule(new IdentityRule());
		assertTrue("1.0", shortJob.getRule() instanceof IdentityRule);
		shortJob.schedule(1000000);
		try {
			shortJob.setRule(new PathRule("/testSetRule"));
			fail("1.1");
		} catch (RuntimeException e) {
			//should fail
		}

		//wake up the sleeping job
		shortJob.wakeUp();

		//setting the rule while running should fail
		try {
			shortJob.setRule(new PathRule("/testSetRule/B"));
			fail("2.0");
		} catch (RuntimeException e1) {
			//should fail
		}

		try {
			//wait for the job to complete
			shortJob.join();
		} catch (InterruptedException e2) {
			//ignore
		}

		//after the job has finished executing, the scheduling rule for it can once again be reset
		shortJob.setRule(new PathRule("/testSetRule/B/C/D"));
		assertTrue("1.2", shortJob.getRule() instanceof PathRule);
		shortJob.setRule(null);
		assertNull("1.3", shortJob.getRule());
	}

	public void testSetThread() {
		//setting the thread of a job that is not an asynchronous job should not affect the actual thread the job will run in
		assertNull("0.0", longJob.getThread());

		longJob.setThread(Thread.currentThread());
		assertEquals("1.0", longJob.getThread(), Thread.currentThread());
		longJob.schedule();
		waitForState(longJob, Job.RUNNING);

		//the setThread method should have no effect on jobs that execute normally
		assertNotEquals("2.0", longJob.getThread(), Thread.currentThread());

		longJob.cancel();
		waitForState(longJob, Job.NONE);

		//the thread should reset to null when the job finishes execution
		assertNull("3.0", longJob.getThread());

		longJob.setThread(null);
		assertNull("4.0", longJob.getThread());

		longJob.schedule();
		waitForState(longJob, Job.RUNNING);

		//the thread that the job is executing in is not the one that was set
		assertNotNull("5.0 (state=" + JobManager.printState(longJob.getState()) + ')', longJob.getThread());
		longJob.cancel();
		waitForState(longJob, Job.NONE);

		//thread should reset to null after execution of job
		assertNull("6.0", longJob.getThread());

		Thread t = new Thread();
		longJob.setThread(t);
		assertEquals("7.0", longJob.getThread(), t);
		longJob.schedule();
		waitForState(longJob, Job.RUNNING);

		//the thread that the job is executing in is not the one that it was set to
		assertNotEquals("8.0", longJob.getThread(), t);
		longJob.cancel();
		waitForState(longJob, Job.NONE);

		//execution thread should reset to null after job is finished
		assertNull("9.0", longJob.getThread());

		//when the state is changed to RUNNING, the thread should not be null
		final Thread[] thread = new Thread[1];
		IJobChangeListener listener = new JobChangeAdapter() {
			@Override
			public void running(IJobChangeEvent event) {
				thread[0] = event.getJob().getThread();
			}
		};
		longJob.addJobChangeListener(listener);
		longJob.schedule();
		waitForState(longJob, Job.RUNNING);
		longJob.cancel();
		longJob.removeJobChangeListener(listener);
		waitForState(longJob, Job.NONE);
		assertNotNull("10.0", thread[0]);
	}

	/**
	 * Several jobs were scheduled to run.
	 * Pause this thread until all the jobs start running.
	 */
	private void waitForStart(Job[] jobs, AtomicIntegerArray status) {
		for (int i = 0; i < jobs.length; i++) {
			TestBarrier2.waitForStatus(status, i, TestBarrier2.STATUS_RUNNING);
		}
	}

	/**
	 * A job has been scheduled.  Pause this thread so that a worker thread
	 * has a chance to pick up the new job.
	 */
	private void waitForState(Job job, int state) {
		long timeoutInMs = 10_000; // 100*100ms
		long start = System.nanoTime();
		try {
			Method internalGetState = InternalJob.class.getDeclaredMethod("internalGetState");
			internalGetState.setAccessible(true);
			// use internalGetState instead of getState() to avoid to hit ABOUT_TO_RUN when
			// waiting for RUNNING
			while (((state == Job.RUNNING) ? ((int) internalGetState.invoke(job)) : job.getState()) != state) {
				Thread.yield();
				long elapsed = (System.nanoTime() - start) / 1_000_000;
				// sanity test to avoid hanging tests
				assertTrue("Timeout waiting for job to change state.", elapsed < timeoutInMs);
			}
		} catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private void waitForState(Job[] jobs, int state) {
		for (Job job : jobs) {
			waitForState(job, state);
		}
	}

}
