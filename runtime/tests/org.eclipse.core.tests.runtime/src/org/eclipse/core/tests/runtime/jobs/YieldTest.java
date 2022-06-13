/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.runtime.jobs;

import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicIntegerArray;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.core.tests.harness.TestBarrier2;
import org.eclipse.core.tests.harness.TestJob;

/**
 * Tests for {@link Job#yieldRule(IProgressMonitor)}.
 */
public class YieldTest extends AbstractJobManagerTest {

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
			synchronized (YieldTest.this) {
				if (scheduled.remove(event.getJob())) {
					//wake up the waitForCompletion method
					completedJobs++;
					YieldTest.this.notify();
				}
			}
		}

		@Override
		public void scheduled(IJobChangeEvent event) {
			Job job = event.getJob();
			synchronized (YieldTest.this) {
				if (job instanceof TestJob) {
					scheduledJobs++;
					scheduled.add(job);
				}
			}
		}
	}

	protected int completedJobs;

	private IJobChangeListener[] jobListeners;

	protected int scheduledJobs;

	public YieldTest(String name) {
		super(name);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		completedJobs = 0;
		scheduledJobs = 0;
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
	}

	public void testExceptionWhenYieldingNotOwner() {
		AtomicIntegerArray location = new AtomicIntegerArray(new int[2]);
		final TestBarrier2 barrier1 = new TestBarrier2(location, 0);
		final TestBarrier2 barrier2 = new TestBarrier2(location, 1);

		final Job yielding = new Job(getName() + " Yielding") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				barrier2.setStatus(TestBarrier2.STATUS_START);
				barrier1.waitForStatus(TestBarrier2.STATUS_START);
				return Status.OK_STATUS;
			}
		};
		Job otherJob = new Job(getName() + " ShouldFault") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				barrier2.waitForStatus(TestBarrier2.STATUS_START);
				try {
					yielding.yieldRule(null);
				} catch (IllegalArgumentException e) {
					//expected
					return new Status(IStatus.ERROR, "org.eclipse.core.tests.runtime", "Expected failure");
				} finally {
					barrier1.setStatus(TestBarrier2.STATUS_START);
				}
				return Status.OK_STATUS;
			}
		};
		yielding.schedule();
		otherJob.schedule();

		waitForCompletion(otherJob);
		assertTrue(!otherJob.getResult().isOK());
		waitForCompletion(yielding);
	}

	public void testExceptionWhenYieldingNotRunning() {
		final Job yielding = new Job(getName() + " Yielding") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				return Status.OK_STATUS;
			}
		};
		try {
			yielding.yieldRule(null);
			fail("Did not throw exception");
		} catch (IllegalArgumentException e) {
			// ignore
		}

	}

	public void testThreadRestored() {
		final PathRule rule = new PathRule(getName());

		final Job[] jobs = new Job[2];
		Job yieldJob = new Job(getName() + " Yielding") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				assertNull("Conflicting job result is not null: " + jobs[1].getResult(), jobs[1].getResult());
				Thread before = getThread();
				while (yieldRule(null) == null) {
					//loop until yield succeeds
				}
				waitForCompletion(jobs[1]);
				Thread after = getThread();
				assertEquals("Thread not restored", before, after);
				assertTrue("Conflicting job not done", jobs[1].getResult().isOK());
				assertEquals("Conflicting job still running", Job.NONE, jobs[1].getState());
				return Status.OK_STATUS;
			}
		};
		jobs[0] = yieldJob;
		yieldJob.setRule(rule);

		Job conflictingJob = new Job(getName() + " Conflicting") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				assertEquals(WAITING, jobs[0].getState());
				assertNull(jobs[0].getResult());
				return Status.OK_STATUS;
			}
		};
		jobs[1] = conflictingJob;
		conflictingJob.setRule(rule);

		yieldJob.schedule();
		conflictingJob.schedule();
		waitForCompletion(yieldJob);
		assertTrue("Result is not ok: " + yieldJob.getResult(), yieldJob.getResult().isOK());
		waitForCompletion(conflictingJob);
		assertTrue(conflictingJob.getResult().isOK());
	}

	public void testYieldJobToJob() {
		final PathRule rule = new PathRule(getName());

		final Job[] jobs = new Job[2];
		Job yieldJob = new Job(getName() + " Yielding") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				assertNull(jobs[1].getResult());
				while (yieldRule(null) == null) {
					//loop until yield succeeds
				}
				waitForCompletion(jobs[1]);
				assertTrue(jobs[1].getResult().isOK());
				assertEquals(Job.NONE, jobs[1].getState());
				return Status.OK_STATUS;
			}
		};
		jobs[0] = yieldJob;
		yieldJob.setRule(rule);

		Job conflictingJob = new Job(getName() + " Conflicting") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				assertEquals(WAITING, jobs[0].getState());
				assertNull(jobs[0].getResult());
				return Status.OK_STATUS;
			}
		};
		jobs[1] = conflictingJob;
		conflictingJob.setRule(rule);

		yieldJob.schedule();
		conflictingJob.schedule();
		waitForCompletion(yieldJob);
		final IStatus yieldResult = yieldJob.getResult();
		if (!yieldResult.isOK()) {
			Throwable t = yieldResult.getException();
			if (t != null) {
				fail("yieldJob failed", t);
			}
			fail("yieldJob failed:" + yieldResult);
		}
		waitForCompletion(conflictingJob);
		assertTrue(conflictingJob.getResult().isOK());
	}

	//	public void testILockTransfer() {
	//		final PathRule rule = new PathRule("testYield");
	//		final ILock lock = Job.getJobManager().newLock();
	//		final Job[] jobs = new Job[2];
	//		final IStatus[] results = new IStatus[2];
	//		Job yieldJob = new Job("Yielding") {
	//			protected IStatus run(IProgressMonitor monitor) {
	//				assertTrue(results[1] == null);
	//				assertTrue(jobs[1].getState() == Job.WAITING);
	//				lock.acquire();
	//				while (true)
	//					if (yield())
	//						break;
	//				assertTrue(results[1].isOK());
	//				assertTrue(jobs[1].getState() == Job.NONE);
	//				return Status.OK_STATUS;
	//			}
	//		};
	//		jobs[0] = yieldJob;
	//		yieldJob.setRule(rule);
	//		yieldJob.addJobChangeListener(new YieldJobListener(results, 0));
	//
	//		Job conflictingJob = new Job("Conflicting") {
	//			protected IStatus run(IProgressMonitor monitor) {
	//				lock.acquire();
	//				assertTrue(jobs[0].getState() == WAITING);
	//				assertTrue(results[0] == null);
	//				return Status.OK_STATUS;
	//			}
	//		};
	//		jobs[1] = conflictingJob;
	//		conflictingJob.setRule(rule);
	//		conflictingJob.addJobChangeListener(new YieldJobListener(results, 1));
	//
	//		yieldJob.schedule();
	//		conflictingJob.schedule();
	//		waitForCompletion(yieldJob);
	//		assertTrue(yieldJob.getResult().isOK());
	//		waitForCancel(conflictingJob);
	//		assertTrue(conflictingJob.getResult().isOK());
	//	}

	public void testYieldJobToJobAndEnsureConflictingRunsBeforeResume() {
		final PathRule rule = new PathRule(getName());
		AtomicIntegerArray location = new AtomicIntegerArray(new int[2]);
		final TestBarrier2 barrier1 = new TestBarrier2(location, 0);
		final Job[] jobs = new Job[2];
		Job yieldJob = new Job(getName() + " Yielding") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				barrier1.waitForStatus(TestBarrier2.STATUS_START);
				barrier1.setStatus(TestBarrier2.STATUS_RUNNING);
				while (yieldRule(null) == null) {
					//loop until yield succeeds
				}
				barrier1.waitForStatus(TestBarrier2.STATUS_DONE);
				return Status.OK_STATUS;
			}
		};
		jobs[0] = yieldJob;
		yieldJob.setRule(rule);

		final Job conflictingJob1 = new Job(getName() + " Conflicting") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				barrier1.setStatus(TestBarrier2.STATUS_DONE);
				return Status.OK_STATUS;
			}
		};
		conflictingJob1.setRule(rule);

		Job nonConflict = new Job(getName() + " Non-conflicting") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				return Status.OK_STATUS;
			}
		};

		yieldJob.schedule();
		conflictingJob1.schedule();
		barrier1.setStatus(TestBarrier2.STATUS_START);
		// we are testing a race-condition here... (This test will not catch all conditions)
		// make sure starting a non-conflicting job does not cause
		// the yielding job to resume before conflicting job has a chance to run
		nonConflict.schedule(1);

		waitForCompletion(yieldJob);
		waitForCompletion(conflictingJob1);
		waitForCompletion(nonConflict);
		assertTrue(yieldJob.getResult().isOK());
		assertTrue(conflictingJob1.getResult().isOK());
		assertTrue(nonConflict.getResult().isOK());
	}

	public void testYieldJobToThread() {

		final PathRule rule = new PathRule(getName());
		final TestBarrier2 barrier = new TestBarrier2();
		final Job yielding = new Job(getName() + " Yielding") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				barrier.setStatus(TestBarrier2.STATUS_START);
				while (yieldRule(null) == null) {
					//loop until yield succeeds
				}
				// ensure that the other job ran
				barrier.waitForStatus(TestBarrier2.STATUS_DONE);
				return Status.OK_STATUS;
			}
		};
		yielding.setRule(rule);
		Thread t = new Thread(getName() + " Conflicting") {
			@Override
			public void run() {
				try {
					Job.getJobManager().beginRule(rule, null);
					barrier.setStatus(TestBarrier2.STATUS_DONE);
				} finally {
					Job.getJobManager().endRule(rule);
				}
			}
		};

		yielding.schedule();
		barrier.waitForStatus(TestBarrier2.STATUS_START);
		t.start();

		waitForCompletion(yielding);
		assertTrue(yielding.getResult().isOK());
	}

	public void testYieldThreadJobToThread() {
		final PathRule rule = new PathRule(getName());
		final TestBarrier2 barrier = new TestBarrier2();
		final Job yielding = new Job(getName() + " Yielding") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					Job.getJobManager().beginRule(rule, null);
					barrier.setStatus(TestBarrier2.STATUS_START);
					while (yieldRule(null) == null) {
						//loop until yield succeeds
					}
					// ensure that the other job ran
					barrier.waitForStatus(TestBarrier2.STATUS_DONE);
				} finally {
					Job.getJobManager().endRule(rule);
				}
				return Status.OK_STATUS;
			}
		};

		Thread t = new Thread(getName() + " Conflicting") {
			@Override
			public void run() {
				try {
					Job.getJobManager().beginRule(rule, null);
					barrier.setStatus(TestBarrier2.STATUS_DONE);
				} finally {
					Job.getJobManager().endRule(rule);
				}
			}
		};

		yielding.schedule();
		barrier.waitForStatus(TestBarrier2.STATUS_START);
		t.start();

		waitForCompletion(yielding, 5000);
		assertTrue(yielding.getResult().isOK());
	}

	public void testYieldThreadToJob() {

		final PathRule rule = new PathRule(getName());
		final TestBarrier2 barrier = new TestBarrier2();
		final Thread t = new Thread() {
			@Override
			public void run() {
				IJobManager m = Job.getJobManager();
				try {
					m.beginRule(rule, null);
					Job j = m.currentJob();
					barrier.setStatus(TestBarrier2.STATUS_START);
					barrier.waitForStatus(TestBarrier2.STATUS_WAIT_FOR_START);
					while (j.yieldRule(null) == null) {
						//loop until yield succeeds
					}
					// ensure that the other job ran
					barrier.waitForStatus(TestBarrier2.STATUS_DONE);
				} finally {
					m.endRule(rule);
					barrier.setStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);
				}
			}
		};

		final Job conflictingJob = new Job(getName() + " Conflicting") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				barrier.setStatus(TestBarrier2.STATUS_DONE);
				return Status.OK_STATUS;
			}
		};

		t.start();
		barrier.waitForStatus(TestBarrier2.STATUS_START);
		conflictingJob.setRule(rule);
		conflictingJob.schedule();
		barrier.setStatus(TestBarrier2.STATUS_WAIT_FOR_START);
		barrier.waitForStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);
		waitForCompletion(conflictingJob);
		assertTrue(conflictingJob.getResult().isOK());
	}

	public void testYieldThreadToThreadJob() {
		final PathRule rule = new PathRule(getName());
		final TestBarrier2 barrier = new TestBarrier2();
		Thread t = new Thread(getName() + " Yielding") {
			@Override
			public void run() {
				try {
					Job.getJobManager().beginRule(rule, null);
					barrier.setStatus(TestBarrier2.STATUS_START);
					while (Job.getJobManager().currentJob().yieldRule(null) == null) {
						//loop until yield succeeds
					}
					// ensure that the other job ran
					barrier.waitForStatus(TestBarrier2.STATUS_DONE);
				} finally {
					Job.getJobManager().endRule(rule);
				}
				// status code irrelevant
				barrier.setStatus(TestBarrier2.STATUS_BLOCKED);
			}
		};

		final Job conflicting = new Job(getName() + " Conflicting") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					Job.getJobManager().beginRule(rule, null);
				} finally {
					Job.getJobManager().endRule(rule);
				}
				barrier.setStatus(TestBarrier2.STATUS_DONE);
				return Status.OK_STATUS;
			}
		};

		t.start();
		barrier.waitForStatus(TestBarrier2.STATUS_START);
		conflicting.schedule();

		waitForCompletion(conflicting, 5000);
		assertTrue(conflicting.getResult().isOK());
		barrier.waitForStatus(TestBarrier2.STATUS_BLOCKED);
	}

	private synchronized void waitForCompletion() {
		int i = 0;
		assertTrue("Jobs completed that weren't scheduled", completedJobs <= scheduledJobs);
		while (completedJobs < scheduledJobs) {
			try {
				wait(500);
			} catch (InterruptedException e) {
				//ignore
			}
			//sanity test to avoid hanging tests
			if (i++ > 1000) {
				dumpState();
				assertTrue("Timeout waiting for job to complete", false);
			}
		}
	}

	public void transferRuleToYieldingThreadJobException() {
		final TestBarrier2 barrier = new TestBarrier2();
		final PathRule rule = new PathRule(getName());

		final Thread A = new Thread() {
			@Override
			public void run() {
				try {
					Job.getJobManager().beginRule(rule, null);
					barrier.setStatus(TestBarrier2.STATUS_RUNNING);
					while (Job.getJobManager().currentJob().yieldRule(null) == null) {
						//loop until yield succeeds
					}

				} finally {
					Job.getJobManager().endRule(rule);
				}
			}
		};

		Thread B = new Thread() {
			@Override
			public void run() {
				barrier.waitForStatus(TestBarrier2.STATUS_RUNNING);

				try {
					Job.getJobManager().beginRule(rule, null);

					// transfer it back to other thread -
					// this will cause exception since the target thread already owns
					// this rule
					try {
						Job.getJobManager().transferRule(rule, A);
					} catch (Exception e) {
						barrier.setStatus(TestBarrier2.STATUS_DONE);
					}
				} finally {
					Job.getJobManager().endRule(rule);

				}

			}
		};
		A.start();
		B.start();
		barrier.waitForStatus(TestBarrier2.STATUS_DONE);
	}

	public void transferRuleToYieldingJobException() {
		final TestBarrier2 barrier = new TestBarrier2();
		final PathRule rule = new PathRule(getName());

		final Thread[] t = new Thread[1];
		final Job A = new Job(getName() + "A") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				t[0] = getThread();
				try {
					Job.getJobManager().beginRule(rule, null);
					barrier.setStatus(TestBarrier2.STATUS_RUNNING);
					while (Job.getJobManager().currentJob().yieldRule(null) == null) {
						//loop until yield succeeds
					}

				} finally {
					Job.getJobManager().endRule(rule);
				}
				return Status.OK_STATUS;
			}

		};
		Job B = new Job(getName() + "B") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				barrier.waitForStatus(TestBarrier2.STATUS_RUNNING);

				try {
					Job.getJobManager().beginRule(rule, null);

					// transfer it back to other thread -
					// this will cause exception since the target thread already owns
					// this rule
					try {
						Job.getJobManager().transferRule(rule, t[0]);
					} catch (Exception e) {
						barrier.setStatus(TestBarrier2.STATUS_DONE);
					}
				} finally {
					Job.getJobManager().endRule(rule);
				}
				return Status.OK_STATUS;
			}
		};
		A.schedule();
		B.schedule();
		barrier.waitForStatus(TestBarrier2.STATUS_DONE);
	}

	public void testYieldPingPong() {
		// yield from one job, then yield again
		final PathRule rule = new PathRule(getName());

		final Job[] jobs = new Job[2];
		Job yieldJob = new Job(getName() + " Yielding") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				assertNull(jobs[1].getResult());
				while (yieldRule(null) == null) {
					//loop until yield succeeds
				}
				assertEquals(Job.WAITING, jobs[1].getState());
				return Status.OK_STATUS;
			}
		};
		jobs[0] = yieldJob;
		yieldJob.setRule(rule);

		Job conflictingJob = new Job(getName() + " ConflictingJob1") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				assertEquals(WAITING, jobs[0].getState());
				while (yieldRule(null) == null) {
					//loop until yield succeeds
				}
				assertEquals(NONE, jobs[0].getState());
				return Status.OK_STATUS;
			}
		};
		jobs[1] = conflictingJob;
		conflictingJob.setRule(rule);

		yieldJob.schedule();
		conflictingJob.schedule();
		waitForCompletion(yieldJob);
		final IStatus yieldResult = yieldJob.getResult();
		if (!yieldResult.isOK()) {
			Throwable t = yieldResult.getException();
			if (t != null) {
				fail("yieldJob failed", t);
			}
			fail("yieldJob failed:" + yieldResult);
		}
		waitForCompletion(conflictingJob);
		assertTrue(conflictingJob.toString(), conflictingJob.getResult().isOK());
	}

	public void testYieldPingPongBetweenMultipleJobs() throws Throwable {
		final TestBarrier2 barrier = new TestBarrier2();
		final PathRule rule = new PathRule(getName());
		final Object SYNC = new Object();
		final int count = 100;
		final Integer[] started = new Integer[] {Integer.valueOf(0)};
		final List<Job> jobs = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			Job conflictingJob = new Job(getName() + " ConflictingJob" + i) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					synchronized (SYNC) {
						started[0] = Integer.valueOf(started[0].intValue() + 1);
						SYNC.notifyAll();
					}
					barrier.waitForStatus(TestBarrier2.STATUS_WAIT_FOR_START);
					while (true) {

						if (yieldRule(null) != null) {
							break;
						}

						if (getFinishedJobs(jobs.toArray(new Job[jobs.size()])).size() == count - 1) {
							System.out.println(this + " Ended via no more jobs to yield");
							break;
						}

						//loop until yield succeeds
					}
					return Status.OK_STATUS;
				}
			};
			conflictingJob.setRule(rule);
			jobs.add(conflictingJob);
		}

		for (Job conflict : jobs) {
			conflict.schedule();
		}

		// wait for jobs to start running
		synchronized (SYNC) {
			while (started[0].intValue() != count) {
				try {
					SYNC.wait();
				} catch (InterruptedException e) {
					fail("4.99", e);
				}
			}
		}
		// release all waiting jobs
		barrier.setStatus(TestBarrier2.STATUS_WAIT_FOR_START);
		// wait for jobs to finish within 5s
		waitForJobsCompletion(jobs.toArray(new Job[jobs.size()]), 5000);

		for (Job conflict : jobs) {
			assertNotNull("Null result for " + conflict, conflict.getResult());
			assertTrue(conflict.getResult().isOK());
		}

	}

	public void testParallelYieldPingPongBetweenMultipleJobs() throws Throwable {
		// same as above, but use two conflicting job families and make sure they don't interfere

		final int count = 10;

		//----------
		// FAMILY A
		//----------

		final TestBarrier2 barrier_A = new TestBarrier2();
		final PathRule rule_A = new PathRule(getName() + "_ruleA");
		final Object SYNC_A = new Object();
		final Integer[] started_A = new Integer[] {Integer.valueOf(0)};
		final List<Job> jobs_A = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			Job conflictingJob = new Job(getName() + " ConflictingJob_A_" + i) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					synchronized (SYNC_A) {
						started_A[0] = Integer.valueOf(started_A[0].intValue() + 1);
						SYNC_A.notifyAll();
					}
					barrier_A.waitForStatus(TestBarrier2.STATUS_WAIT_FOR_START);
					while (true) {

						if (yieldRule(null) != null) {
							break;
						}

						if (getFinishedJobs(jobs_A.toArray(new Job[jobs_A.size()])).size() == count - 1) {
							System.out.println(this + " Ended via no more jobs to yield");
							break;
						}

						//loop until yield succeeds
					}
					return Status.OK_STATUS;
				}
			};
			conflictingJob.setRule(rule_A);
			jobs_A.add(conflictingJob);
		}

		for (Job conflict : jobs_A) {
			conflict.schedule();
		}

		// wait for jobs to start running
		synchronized (SYNC_A) {
			while (started_A[0].intValue() != count) {
				try {
					SYNC_A.wait();
				} catch (InterruptedException e) {
					fail("4.99", e);
				}
			}
		}
		// release all waiting jobs
		barrier_A.setStatus(TestBarrier2.STATUS_WAIT_FOR_START);

		//----------
		// FAMILY B
		//----------

		final TestBarrier2 barrier_B = new TestBarrier2();
		final PathRule rule_B = new PathRule(getName() + "_ruleB");
		final Object SYNC_B = new Object();

		final Integer[] started_B = new Integer[] { Integer.valueOf(0) };
		final List<Job> jobs_B = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			Job conflictingJob = new Job(getName() + " ConflictingJob_B_" + i) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					synchronized (SYNC_B) {
						started_B[0] = Integer.valueOf(started_B[0].intValue() + 1);
						SYNC_B.notifyAll();
					}
					barrier_B.waitForStatus(TestBarrier2.STATUS_WAIT_FOR_START);
					while (true) {

						if (yieldRule(null) != null) {
							break;
						}

						if (getFinishedJobs(jobs_B.toArray(new Job[jobs_B.size()])).size() == count - 1) {
							System.out.println(this + " Ended via no more jobs to yield");
							break;
						}

						//loop until yield succeeds
					}
					return Status.OK_STATUS;
				}
			};
			conflictingJob.setRule(rule_B);
			jobs_B.add(conflictingJob);
		}

		for (Job conflict : jobs_B) {
			conflict.schedule();
		}

		// wait for jobs to start running
		synchronized (SYNC_B) {
			while (started_B[0].intValue() != count) {
				try {
					SYNC_B.wait();
				} catch (InterruptedException e) {
					fail("4.99", e);
				}
			}
		}
		// release all waiting jobs
		barrier_B.setStatus(TestBarrier2.STATUS_WAIT_FOR_START);

		// wait for jobs to finish within 5s
		waitForJobsCompletion(jobs_A.toArray(new Job[jobs_A.size()]), 5000);

		// wait for jobs to finish within 5s
		waitForJobsCompletion(jobs_B.toArray(new Job[jobs_B.size()]), 5000);

		for (Job conflict : jobs_A) {
			assertNotNull("Null result for " + conflict, conflict.getResult());
			assertTrue(conflict.getResult().isOK());
		}

		for (Job conflict : jobs_B) {
			assertNotNull("Null result for " + conflict, conflict.getResult());
			assertTrue(conflict.getResult().isOK());
		}
	}

	public void testYieldIsInterruptable() throws Exception {
		Semaphore semaphoreA = new Semaphore(0);
		Semaphore semaphoreB = new Semaphore(0);
		Semaphore mainThreadSemaphore = new Semaphore(0);
		final PathRule rule = new PathRule(getName());
		String[] failureMessage = new String[1];
		boolean[] operationWasCanceled = new boolean[1];
		final Job yieldB = new Job(getName() + " YieldingB") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				// Step 2
				semaphoreA.release();
				try {
					// Block until thread yieldA reaches step 3
					semaphoreB.acquire();
				} catch (InterruptedException e) {
					failureMessage[0] = "yieldB was interrupted too early";
					return Status.OK_STATUS;
				}
				// Step 5
				mainThreadSemaphore.release();
				return Status.OK_STATUS;
			}
		};
		Job yieldA = new Job(getName() + " YieldingA") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					// Block until the main thread reaches step 1
					semaphoreA.acquire();
				} catch (InterruptedException e) {
					failureMessage[0] = "yieldA was interrupted too early";
					return Status.OK_STATUS;
				}
				// If this fails to interrupt the following yieldRule call,
				// this thread will deadlock.
				Thread.currentThread().interrupt();
				try {
					// Allow yieldB to start execution
					while (yieldRule(null) != yieldB) {
					}

				} catch (OperationCanceledException e) {
					operationWasCanceled[0] = true;
				}
				if (Thread.interrupted()) {
					failureMessage[0] = "The thread was interrupted after yieldRule returned";
				}
				try {
					// Block until yieldB reaches step 2
					semaphoreA.acquire();
				} catch (InterruptedException e) {
					failureMessage[0] = "yieldA was interrupted too early";
					return Status.OK_STATUS;
				}
				// Step 3
				semaphoreB.release();
				// Step 4
				mainThreadSemaphore.release();
				return Status.OK_STATUS;
			}
		};
		yieldA.setRule(rule);
		yieldA.schedule();

		yieldB.setRule(rule);
		yieldB.schedule();

		// Step 1
		semaphoreA.release();

		// Block until step 4 and step 5 have occurred
		mainThreadSemaphore.acquire(2);
		if (failureMessage[0] != null) {
			assertNull(failureMessage[0], failureMessage[0]);
		}

		assertTrue("yieldRule should have thrown OperationCanceledException", operationWasCanceled[0]);
	}

	public void testYieldJobToJobsInterleaved() {
		// yield from job to multiple waiting others
		final TestBarrier2 barrier = new TestBarrier2();
		final PathRule rule = new PathRule(getName());

		final int count = 50;
		Job yieldA = new Job(getName() + " YieldingA") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				barrier.waitForStatus(TestBarrier2.STATUS_START);
				int yields = 0;
				while (yields < count) {
					if (yieldRule(null) != null) {
						yields++;
					}
				}
				return Status.OK_STATUS;
			}
		};
		yieldA.setRule(rule);
		yieldA.schedule();

		Job yieldB = new Job(getName() + " YieldingB") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				barrier.waitForStatus(TestBarrier2.STATUS_START);
				int yields = 0;
				while (yields < count) {
					if (yieldRule(null) != null) {
						yields++;
					}
				}
				return Status.OK_STATUS;
			}
		};
		yieldB.setRule(rule);
		yieldB.schedule();

		barrier.setStatus(TestBarrier2.STATUS_START);

		List<Job> jobs = new ArrayList<>();
		jobs.add(yieldA);
		jobs.add(yieldB);

		// wait for jobs to start
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			fail("4.99", e);
		}
		waitForJobsCompletion(jobs.toArray(new Job[jobs.size()]), 20000);
		for (Job conflict : jobs) {
			assertTrue(conflict.getResult().isOK());
		}
	}

	public void testYieldThreadJobToBlockedConflictingJob() throws Exception {
		final PathRule rule = new PathRule(getName());
		final TestBarrier2 b = new TestBarrier2(TestBarrier2.STATUS_START);
		final Job yieldA = new Job(getName()) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {

				try {
					Job.getJobManager().beginRule(rule, monitor);
					b.setStatus(TestBarrier2.STATUS_RUNNING);
					while (yieldRule(null) == null && !monitor.isCanceled()) {
						//loop until yield succeeds
					}
				} finally {
					Job.getJobManager().endRule(rule);
				}

				return Status.OK_STATUS;
			}
		};
		Job conflicting = new Job(getName() + " Conflicting") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				return Status.OK_STATUS;
			}
		};
		conflicting.setRule(rule);

		// start testing

		yieldA.schedule();
		b.waitForStatus(TestBarrier2.STATUS_RUNNING);
		conflicting.schedule();
		try {
			waitForCompletion(yieldA);
			waitForCompletion(conflicting);
		} finally {
			//always cleanup even if we fail
			yieldA.cancel();
		}

	}

	public void testResumingThreadJobIsNotRescheduled() {

		final PathRule rule = new PathRule(getName());
		final TestBarrier2 b = new TestBarrier2(TestBarrier2.STATUS_START);
		final Job yieldA = new Job(getName()) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {

				try {
					Job.getJobManager().beginRule(rule, monitor);
					b.setStatus(TestBarrier2.STATUS_RUNNING);
					while (yieldRule(null) == null) {
						if (monitor.isCanceled()) {
							return Status.CANCEL_STATUS;
						//loop until yield succeeds
						}
					}
				} finally {
					Job.getJobManager().endRule(rule);
				}

				return Status.OK_STATUS;
			}
		};
		final Job conflicting = new Job(getName() + " Conflicting") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				return Status.OK_STATUS;
			}
		};
		conflicting.setRule(rule);

		final int[] count = new int[1];
		JobChangeAdapter a = new JobChangeAdapter() {
			@Override
			public void running(org.eclipse.core.runtime.jobs.IJobChangeEvent event) {
				if (event.getJob() == yieldA) {
					count[0]++;
				}
			}
		};
		Job.getJobManager().addJobChangeListener(a);

		// start testing

		yieldA.schedule();
		b.waitForStatus(TestBarrier2.STATUS_RUNNING);
		conflicting.schedule();
		try {
			waitForCompletion(yieldA);
			waitForCompletion(conflicting);
			assertEquals("While resuming from yieldRule, implicit Job should only run once", 1, count[0]);
		} finally {
			//clean up even if the test fails
			yieldA.cancel();
			Job.getJobManager().removeJobChangeListener(a);
		}

	}

	public void testNestedAcquireJobIsNotRescheduled() {

		final PathRule rule = new PathRule(getName());
		final PathRule subRule = new PathRule(getName() + "/subRule");

		final TestBarrier2 b = new TestBarrier2(TestBarrier2.STATUS_START);
		final Job yieldA = new Job(getName()) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {

				b.setStatus(TestBarrier2.STATUS_RUNNING);
				try {
					Job.getJobManager().beginRule(subRule, null);

					while (yieldRule(null) == null) {
						//loop until yield succeeds
					}
				} finally {
					Job.getJobManager().endRule(subRule);
				}

				return Status.OK_STATUS;
			}
		};
		yieldA.setRule(rule);

		final Job conflicting = new Job(getName() + " Conflicting") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				return Status.OK_STATUS;
			}
		};
		conflicting.setRule(rule);

		final int[] count = new int[1];
		JobChangeAdapter a = new JobChangeAdapter() {
			@Override
			public void running(org.eclipse.core.runtime.jobs.IJobChangeEvent event) {
				if (event.getJob() == conflicting) {
					count[0]++;
				}
			}
		};
		Job.getJobManager().addJobChangeListener(a);
		try {
			// start testing
			yieldA.schedule();
			b.waitForStatus(TestBarrier2.STATUS_RUNNING);
			conflicting.schedule();
			waitForCompletion(yieldA);
			waitForCompletion(conflicting);
			assertEquals("While resuming from yieldRule, conflicting job should only run once", 1, count[0]);
		} finally {
			Job.getJobManager().removeJobChangeListener(a);
		}
	}

}
