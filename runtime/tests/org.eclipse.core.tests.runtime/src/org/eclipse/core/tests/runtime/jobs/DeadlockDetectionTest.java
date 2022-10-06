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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicIntegerArray;
import org.eclipse.core.internal.jobs.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.core.tests.harness.FussyProgressMonitor;
import org.eclipse.core.tests.harness.TestBarrier2;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
 * Tests implementation of ILock objects
 */
@SuppressWarnings("restriction")
public class DeadlockDetectionTest {
	private final IJobManager manager = Job.getJobManager();

	@Rule
	public TestName name = new TestName();

	/**
	 * Creates n runnables on the given lock and adds them to the given list.
	 */
	private void createRunnables(ILock[] locks, int n, ArrayList<RandomTestRunnable> allRunnables, boolean cond) {
		for (int i = 0; i < n; i++) {
			allRunnables
					.add(new RandomTestRunnable(locks, name.getMethodName() + " # " + (allRunnables.size() + 1), cond));
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				//ignore
			}
		}
	}

	private LockManager getLockManager() {
		return ((JobManager) manager).getLockManager();
	}

	/**
	 * Asks all threads to stop executing
	 */
	private void kill(ArrayList<RandomTestRunnable> allRunnables) {
		for (RandomTestRunnable r : allRunnables) {
			r.kill();
		}
	}

	/**
	 * Test that deadlock between locks is detected and resolved.
	 * Test with 6 threads competing for 3 locks from a set of 6.
	 */
	@Test
	public void testComplex() {
		DeadlockDetector.runSilent(() -> {
			ArrayList<RandomTestRunnable> allRunnables = new ArrayList<>();
			LockManager lockManager = new LockManager();
			OrderedLock lock1 = lockManager.newLock();
			OrderedLock lock2 = lockManager.newLock();
			OrderedLock lock3 = lockManager.newLock();
			OrderedLock lock4 = lockManager.newLock();
			OrderedLock lock5 = lockManager.newLock();
			OrderedLock lock6 = lockManager.newLock();
			createRunnables(new ILock[] { lock1, lock2, lock3 }, 1, allRunnables, true);
			createRunnables(new ILock[] { lock2, lock3, lock4 }, 1, allRunnables, true);
			createRunnables(new ILock[] { lock3, lock4, lock5 }, 1, allRunnables, true);
			createRunnables(new ILock[] { lock4, lock5, lock6 }, 1, allRunnables, true);
			createRunnables(new ILock[] { lock5, lock6, lock1 }, 1, allRunnables, true);
			createRunnables(new ILock[] { lock6, lock1, lock2 }, 1, allRunnables, true);
			wait(allRunnables, lockManager);
		});
	}

	private void wait(ArrayList<RandomTestRunnable> allRunnables, LockManager lockManager) {
		start(allRunnables);
		// wait till all threads looped at least twice:
		while (!allRunnables.stream().allMatch(t -> t.runs >= 2)) {
			Thread.yield();
		}
		kill(allRunnables);

		// join all threads:
		for (int i = 0; i < allRunnables.size(); i++) {
			try {
				((Thread) allRunnables.get(i)).join(100000);
			} catch (InterruptedException e1) {
				//ignore
			}
			assertTrue("1." + i, !((Thread) allRunnables.get(i)).isAlive());
		}
		//the underlying array has to be empty
		assertTrue("Locks not removed from graph.", lockManager.isEmpty());
	}

	/**
	 * Test simplest deadlock case (2 threads, 2 locks).
	 */
	@Test
	public void testSimpleDeadlock() {
		DeadlockDetector.runSilent(() -> {
			ArrayList<RandomTestRunnable> allRunnables = new ArrayList<>();
			LockManager localManager = new LockManager();
			OrderedLock lock1 = localManager.newLock();
			OrderedLock lock2 = localManager.newLock();

			createRunnables(new ILock[] { lock1, lock2 }, 1, allRunnables, false);
			createRunnables(new ILock[] { lock2, lock1 }, 1, allRunnables, false);

			wait(allRunnables, localManager);
		});
	}

	/**
	 * Test a more complicated scenario with 3 threads and 3 locks.
	 */
	@Test
	public void testThreeLocks() {
		DeadlockDetector.runSilent(() -> {
			ArrayList<RandomTestRunnable> allRunnables = new ArrayList<>();
			LockManager lockManager = new LockManager();
			OrderedLock lock1 = lockManager.newLock();
			OrderedLock lock2 = lockManager.newLock();
			OrderedLock lock3 = lockManager.newLock();

			createRunnables(new ILock[] { lock1, lock2 }, 1, allRunnables, false);
			createRunnables(new ILock[] { lock2, lock3 }, 1, allRunnables, false);
			createRunnables(new ILock[] { lock3, lock1 }, 1, allRunnables, false);

			wait(allRunnables, lockManager);
		});
	}

	/**
	 * Test simple deadlock with 2 threads trying to get 1 rule and 1 lock.
	 */
	@Test
	public void testRuleLockInteraction() {
		final ILock lock = manager.newLock();
		final ISchedulingRule rule = new IdentityRule();
		final AtomicIntegerArray status = new AtomicIntegerArray(
				new int[] { TestBarrier2.STATUS_WAIT_FOR_START, TestBarrier2.STATUS_WAIT_FOR_START });

		Thread first = new Thread("Test1") {
			@Override
			public void run() {
				lock.acquire();
				status.set(0, TestBarrier2.STATUS_START);
				assertTrue("1.0", getLockManager().isLockOwner());
				TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_RUNNING);
				manager.beginRule(rule, null);
				assertTrue("2.0", getLockManager().isLockOwner());
				manager.endRule(rule);
				lock.release();
				status.set(0, TestBarrier2.STATUS_DONE);
			}
		};

		Thread second = new Thread("Test2") {
			@Override
			public void run() {
				manager.beginRule(rule, null);
				status.set(1, TestBarrier2.STATUS_START);
				assertTrue("1.0", getLockManager().isLockOwner());
				TestBarrier2.waitForStatus(status, 1, TestBarrier2.STATUS_RUNNING);
				lock.acquire();
				assertTrue("2.0", getLockManager().isLockOwner());
				lock.release();
				manager.endRule(rule);
				status.set(1, TestBarrier2.STATUS_DONE);
			}
		};

		first.start();
		second.start();

		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_START);
		TestBarrier2.waitForStatus(status, 1, TestBarrier2.STATUS_START);

		status.set(0, TestBarrier2.STATUS_RUNNING);
		status.set(1, TestBarrier2.STATUS_RUNNING);

		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_DONE);
		TestBarrier2.waitForStatus(status, 1, TestBarrier2.STATUS_DONE);
		waitForThreadDeath(first);
		waitForThreadDeath(second);
		assertTrue("3.0", !first.isAlive());
		assertTrue("4.0", !second.isAlive());
		//the underlying array has to be empty
		if (!getLockManager().isEmpty()) {
			assertTrue("Jobs not removed from graph.", getLockManager().isEmpty());
		}
	}

	/**
	 * Test the interaction between jobs with rules and the acquisition of locks.
	 */
	@Test
	public void testJobRuleLockInteraction() {
		final AtomicIntegerArray status = new AtomicIntegerArray(
				new int[] { TestBarrier2.STATUS_WAIT_FOR_START, TestBarrier2.STATUS_WAIT_FOR_START });
		final ISchedulingRule rule1 = new IdentityRule();
		final ISchedulingRule rule2 = new IdentityRule();
		final ILock lock = manager.newLock();

		Job first = new Job("Test1") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					assertTrue("1.0", getLockManager().isLockOwner());
					monitor.beginTask("Testing", 1);
					status.set(0, TestBarrier2.STATUS_START);
					lock.acquire();
					TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_RUNNING);
					assertTrue("2.0", getLockManager().isLockOwner());
					lock.release();
					monitor.worked(1);
					status.set(0, TestBarrier2.STATUS_DONE);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};

		Job second = new Job("Test2") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					assertTrue("1.0", getLockManager().isLockOwner());
					monitor.beginTask("Testing", 1);
					status.set(1, TestBarrier2.STATUS_START);
					lock.acquire();
					TestBarrier2.waitForStatus(status, 1, TestBarrier2.STATUS_RUNNING);
					assertTrue("2.0", getLockManager().isLockOwner());
					lock.release();
					monitor.worked(1);
					status.set(1, TestBarrier2.STATUS_DONE);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};

		first.setRule(rule1);
		second.setRule(rule2);
		first.schedule();
		second.schedule();

		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_START);
		TestBarrier2.waitForStatus(status, 1, TestBarrier2.STATUS_START);

		status.set(0, TestBarrier2.STATUS_RUNNING);
		status.set(1, TestBarrier2.STATUS_RUNNING);

		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_DONE);
		TestBarrier2.waitForStatus(status, 1, TestBarrier2.STATUS_DONE);
		waitForCompletion(first);
		waitForCompletion(second);

		assertEquals("3.0", Job.NONE, first.getState());
		assertEquals("3.1", Status.OK_STATUS, first.getResult());
		assertEquals("4.0", Job.NONE, second.getState());
		assertEquals("4.1", Status.OK_STATUS, second.getResult());
		//the underlying array has to be empty
		assertTrue("Jobs not removed from graph.", getLockManager().isEmpty());
	}

	public static void fill(AtomicIntegerArray a, int val) {
		for (int i = 0, len = a.length(); i < len; i++) {
			a.set(i, val);
		}
	}

	/**
	 * Regression test for bug 46894. Stale entries left over in graph.
	 */
	@Test
	public void testRuleHierarchyWaitReplace() {
		final int NUM_JOBS = 3;
		final AtomicIntegerArray status = new AtomicIntegerArray(new int[NUM_JOBS]);
		fill(status, TestBarrier2.STATUS_WAIT_FOR_START);
		final ISchedulingRule[] rules = {new PathRule("/testRuleHierarchyWaitReplace"), new PathRule("/testRuleHierarchyWaitReplace/B"), new PathRule("/testRuleHierarchyWaitReplace/C")};
		final ILock[] locks = {manager.newLock(), manager.newLock()};
		Job[] jobs = new Job[NUM_JOBS];

		jobs[0] = new Job("Test 0") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					manager.beginRule(rules[0], null);
					status.set(0, TestBarrier2.STATUS_WAIT_FOR_RUN);
					TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_RUNNING);
					manager.endRule(rules[0]);
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};

		jobs[1] = new Job("Test 1") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					locks[0].acquire();
					TestBarrier2.waitForStatus(status, 1, TestBarrier2.STATUS_START);
					manager.beginRule(rules[1], new TestBlockingMonitor(status, 1));
					status.set(1, TestBarrier2.STATUS_WAIT_FOR_RUN);
					locks[1].acquire();
					locks[1].release();
					TestBarrier2.waitForStatus(status, 1, TestBarrier2.STATUS_RUNNING);
					manager.endRule(rules[1]);
					locks[0].release();
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};

		jobs[2] = new Job("Test 2") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					locks[1].acquire();
					TestBarrier2.waitForStatus(status, 2, TestBarrier2.STATUS_START);
					manager.beginRule(rules[2], new TestBlockingMonitor(status, 2));
					status.set(2, TestBarrier2.STATUS_WAIT_FOR_RUN);
					TestBarrier2.waitForStatus(status, 2, TestBarrier2.STATUS_RUNNING);
					manager.endRule(rules[2]);
					locks[1].release();
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};

		for (Job job : jobs) {
			job.schedule();
		}
		//wait until the first job starts
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_WAIT_FOR_RUN);
		//now let the second job start
		status.set(1, TestBarrier2.STATUS_START);
		//wait until it blocks on the beginRule call
		TestBarrier2.waitForStatus(status, 1, TestBarrier2.STATUS_BLOCKED);

		//let the third job start, and wait until it too blocks
		status.set(2, TestBarrier2.STATUS_START);
		//wait until it blocks on the beginRule call
		TestBarrier2.waitForStatus(status, 2, TestBarrier2.STATUS_BLOCKED);

		//end the first job
		status.set(0, TestBarrier2.STATUS_RUNNING);

		//wait until the second job gets the rule
		TestBarrier2.waitForStatus(status, 1, TestBarrier2.STATUS_WAIT_FOR_RUN);
		//let the job finish
		status.set(1, TestBarrier2.STATUS_RUNNING);

		//now wait until the third job gets the rule
		TestBarrier2.waitForStatus(status, 2, TestBarrier2.STATUS_WAIT_FOR_RUN);
		//let the job finish
		status.set(2, TestBarrier2.STATUS_RUNNING);

		for (Job job : jobs) {
			waitForCompletion(job);
		}

		for (int i = 0; i < jobs.length; i++) {
			assertEquals("10." + i, Job.NONE, jobs[i].getState());
			assertEquals("10." + i, Status.OK_STATUS, jobs[i].getResult());
		}
		//the underlying graph has to be empty
		assertTrue("Jobs not removed from graph.", getLockManager().isEmpty());
	}

	/**
	 * Regression test for bug 46894. Deadlock was not detected (before).
	 */
	@Test
	public void testDetectDeadlock() {
		DeadlockDetector.runSilent(() -> {
		final int NUM_JOBS = 3;
		final AtomicIntegerArray status = new AtomicIntegerArray(new int[NUM_JOBS]);
		fill(status, TestBarrier2.STATUS_WAIT_FOR_START);
		final ISchedulingRule[] rules = {new PathRule("/testDetectDeadlock"), new PathRule("/testDetectDeadlock/B"), new PathRule("/testDetectDeadlock/C")};
		final ILock lock = manager.newLock();
		Job[] jobs = new Job[NUM_JOBS];

		jobs[0] = new Job("Test 0") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					manager.beginRule(rules[1], null);
					status.set(0, TestBarrier2.STATUS_WAIT_FOR_RUN);
					TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_RUNNING);
					manager.endRule(rules[1]);
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};

		jobs[1] = new Job("Test 1") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					lock.acquire();
					TestBarrier2.waitForStatus(status, 1, TestBarrier2.STATUS_START);
					manager.beginRule(rules[0], new TestBlockingMonitor(status, 1));
					status.set(1, TestBarrier2.STATUS_WAIT_FOR_RUN);
					TestBarrier2.waitForStatus(status, 1, TestBarrier2.STATUS_RUNNING);
					manager.endRule(rules[0]);
					lock.release();
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};

		jobs[2] = new Job("Test 2") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					TestBarrier2.waitForStatus(status, 2, TestBarrier2.STATUS_START);
					manager.beginRule(rules[2], null);
					status.set(2, TestBarrier2.STATUS_WAIT_FOR_RUN);
					TestBarrier2.waitForStatus(status, 2, TestBarrier2.STATUS_RUNNING);
					lock.acquire();
					lock.release();
					manager.endRule(rules[2]);
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};

		for (Job job : jobs) {
			job.schedule();
		}
		//wait until the first job starts
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_WAIT_FOR_RUN);
		//now let the third job start
		status.set(2, TestBarrier2.STATUS_START);
		//wait until it gets the rule
		TestBarrier2.waitForStatus(status, 2, TestBarrier2.STATUS_WAIT_FOR_RUN);

		//let the second job start
		status.set(1, TestBarrier2.STATUS_START);
		//wait until it blocks on the beginRule call
		TestBarrier2.waitForStatus(status, 1, TestBarrier2.STATUS_BLOCKED);

		//let the third job try for the lock
		status.set(2, TestBarrier2.STATUS_RUNNING);
		//end the first job
		status.set(0, TestBarrier2.STATUS_RUNNING);

		//wait until the second job gets the rule
		TestBarrier2.waitForStatus(status, 1, TestBarrier2.STATUS_WAIT_FOR_RUN);
		//let the job finish
		status.set(1, TestBarrier2.STATUS_RUNNING);
		//wait until all jobs are done
		for (Job job : jobs) {
			waitForCompletion(job);
		}

		for (int i = 0; i < jobs.length; i++) {
			assertEquals("10." + i, Job.NONE, jobs[i].getState());
			assertEquals("10." + i, Status.OK_STATUS, jobs[i].getResult());
		}
		//the underlying graph has to be empty
		assertTrue("Jobs not removed from graph.", getLockManager().isEmpty());
		});
	}

	/**
	 * Test that when 3 columns and 1 row are empty, they are correctly removed from the graph.
	 */
	@Test
	public void testMultipleColumnRemoval() {
		final int NUM_JOBS = 3;
		final AtomicIntegerArray status = new AtomicIntegerArray(new int[NUM_JOBS]);
		fill(status, TestBarrier2.STATUS_WAIT_FOR_START);
		final ISchedulingRule[] rules = {new PathRule("/testMultipleColumnRemoval"), new PathRule("/testMultipleColumnRemoval/B"), new PathRule("/testMultipleColumnRemoval/C")};
		final IProgressMonitor first = new TestBlockingMonitor(status, 1);
		final IProgressMonitor second = new TestBlockingMonitor(status, 2);
		Job[] jobs = new Job[NUM_JOBS];

		jobs[0] = new Job("Test 0") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					manager.beginRule(rules[0], null);
					status.set(0, TestBarrier2.STATUS_WAIT_FOR_RUN);
					TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_RUNNING);
					manager.endRule(rules[0]);
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};

		jobs[1] = new Job("Test 1") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					TestBarrier2.waitForStatus(status, 1, TestBarrier2.STATUS_START);
					manager.beginRule(rules[1], first);
					monitor.worked(1);
				} finally {
					status.set(1, TestBarrier2.STATUS_DONE);
					manager.endRule(rules[1]);
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};

		jobs[2] = new Job("Test 2") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					TestBarrier2.waitForStatus(status, 2, TestBarrier2.STATUS_START);
					manager.beginRule(rules[2], second);
					monitor.worked(1);
				} finally {
					status.set(2, TestBarrier2.STATUS_DONE);
					manager.endRule(rules[2]);
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		//schedule all the jobs
		for (Job job : jobs) {
			job.schedule();
		}
		//wait until the first job starts
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_WAIT_FOR_RUN);
		//now let the other two jobs start
		status.set(1, TestBarrier2.STATUS_START);
		status.set(2, TestBarrier2.STATUS_START);
		//wait until both are blocked on the beginRule call
		TestBarrier2.waitForStatus(status, 1, TestBarrier2.STATUS_BLOCKED);
		TestBarrier2.waitForStatus(status, 2, TestBarrier2.STATUS_BLOCKED);

		//cancel the blocked jobs
		first.setCanceled(true);
		second.setCanceled(true);

		//wait until both jobs are done
		TestBarrier2.waitForStatus(status, 1, TestBarrier2.STATUS_DONE);
		TestBarrier2.waitForStatus(status, 2, TestBarrier2.STATUS_DONE);

		//end the first job
		status.set(0, TestBarrier2.STATUS_RUNNING);
		//wait until all jobs are done
		for (Job job : jobs) {
			waitForCompletion(job);
		}

		//the underlying graph has to be empty
		assertTrue("Jobs not removed from graph.", getLockManager().isEmpty());
	}

	/**
	 * Test that the graph is cleared after a thread stops waiting for a rule.
	 */
	@Test
	public void testBeginRuleCancelAfterWait() {
		final ISchedulingRule rule1 = new PathRule("/testBeginRuleCancelAfterWait");
		final ISchedulingRule rule2 = new PathRule("/testBeginRuleCancelAfterWait/B");

		final AtomicIntegerArray status = new AtomicIntegerArray(
				new int[] { TestBarrier2.STATUS_WAIT_FOR_START, TestBarrier2.STATUS_WAIT_FOR_START });
		final IProgressMonitor canceller = new FussyProgressMonitor();

		Job ruleOwner = new Job("Test1") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					status.set(0, TestBarrier2.STATUS_START);
					manager.beginRule(rule1, null);
					TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_RUNNING);
					manager.endRule(rule1);
					monitor.worked(1);
				} finally {
					monitor.done();
					status.set(0, TestBarrier2.STATUS_DONE);
				}
				return Status.OK_STATUS;
			}
		};

		Job ruleWait = new Job("Test2") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					status.set(1, TestBarrier2.STATUS_RUNNING);
					manager.beginRule(rule2, canceller);
					monitor.worked(1);
				} finally {
					manager.endRule(rule2);
					monitor.done();
					status.set(1, TestBarrier2.STATUS_DONE);
				}
				return Status.OK_STATUS;
			}
		};

		ruleOwner.schedule();
		TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_START);

		//schedule a job that is going to begin a conflicting rule and then cancel the wait
		ruleWait.schedule();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			//ignore
		}
		//cancel the wait for the rule
		canceller.setCanceled(true);
		//wait until the job completes
		TestBarrier2.waitForStatus(status, 1, TestBarrier2.STATUS_DONE);

		//let the first job finish
		status.set(0, TestBarrier2.STATUS_RUNNING);
		TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_DONE);
		waitForCompletion(ruleOwner);
		//the underlying graph should now be empty
		assertTrue("Canceled rule not removed from graph.", getLockManager().isEmpty());
	}

	/**
	 * Test that implicit rules do not create extraneous entries
	 */
	@Test
	public void testImplicitRules() {
		final int NUM_JOBS = 4;
		final AtomicIntegerArray status = new AtomicIntegerArray(new int[NUM_JOBS]);
		fill(status, TestBarrier2.STATUS_WAIT_FOR_START);
		final ISchedulingRule[] rules = {new PathRule("/testImplicitRules"), new PathRule("/testImplicitRules/B"), new PathRule("/testImplicitRules/C"), new PathRule("/testImplicitRules/B/D")};
		Job[] jobs = new Job[NUM_JOBS];

		jobs[0] = new Job("Test 0") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					manager.beginRule(rules[3], null);
					status.set(0, TestBarrier2.STATUS_WAIT_FOR_RUN);
					TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_RUNNING);
					manager.endRule(rules[3]);
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};

		jobs[1] = new Job("Test 1") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					manager.beginRule(rules[2], null);
					status.set(1, TestBarrier2.STATUS_WAIT_FOR_RUN);
					TestBarrier2.waitForStatus(status, 1, TestBarrier2.STATUS_RUNNING);
					manager.endRule(rules[2]);
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};

		jobs[2] = new Job("Test 2") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					TestBarrier2.waitForStatus(status, 2, TestBarrier2.STATUS_START);
					manager.beginRule(rules[0], new TestBlockingMonitor(status, 2));
					status.set(2, TestBarrier2.STATUS_WAIT_FOR_RUN);
					TestBarrier2.waitForStatus(status, 2, TestBarrier2.STATUS_RUNNING);
					manager.endRule(rules[0]);
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};

		jobs[3] = new Job("Test 3") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					TestBarrier2.waitForStatus(status, 3, TestBarrier2.STATUS_START);
					manager.beginRule(rules[1], new TestBlockingMonitor(status, 3));
					status.set(3, TestBarrier2.STATUS_WAIT_FOR_RUN);
					TestBarrier2.waitForStatus(status, 3, TestBarrier2.STATUS_RUNNING);
					manager.endRule(rules[1]);
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};

		for (Job job : jobs) {
			job.schedule();
		}
		//wait until the first 2 jobs start
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_WAIT_FOR_RUN);
		TestBarrier2.waitForStatus(status, 1, TestBarrier2.STATUS_WAIT_FOR_RUN);
		//now let the third job start
		status.set(2, TestBarrier2.STATUS_START);
		//wait until it blocks on the beginRule call
		TestBarrier2.waitForStatus(status, 2, TestBarrier2.STATUS_BLOCKED);

		//let the fourth job start
		status.set(3, TestBarrier2.STATUS_START);
		//wait until it blocks on the beginRule call
		TestBarrier2.waitForStatus(status, 3, TestBarrier2.STATUS_BLOCKED);

		//end the first 2 jobs
		status.set(0, TestBarrier2.STATUS_RUNNING);
		status.set(1, TestBarrier2.STATUS_RUNNING);

		//the third and fourth jobs will now compete in non-deterministic order
		int runningCount = 0;
		long waitStart = AbstractJobTest.now();
		while (runningCount < 2) {
			if (status.get(2) == TestBarrier2.STATUS_WAIT_FOR_RUN) {
				//the third job got the rule - let it finish
				runningCount++;
				status.set(2, TestBarrier2.STATUS_RUNNING);
			}
			if (status.get(3) == TestBarrier2.STATUS_WAIT_FOR_RUN) {
				//the fourth job got the rule - let it finish
				runningCount++;
				status.set(3, TestBarrier2.STATUS_RUNNING);
			}
			//timeout if the two jobs don't start within a reasonable time
			long elapsed = AbstractJobTest.now() - waitStart;
			assertTrue("Timeout waiting for job to end: " + elapsed, elapsed < 30000);
		}
		//wait until all jobs are done
		for (Job job : jobs) {
			waitForCompletion(job);
		}

		for (int i = 0; i < jobs.length; i++) {
			assertEquals("10." + i, Job.NONE, jobs[i].getState());
			assertEquals("10." + i, Status.OK_STATUS, jobs[i].getResult());
		}
		//the underlying graph has to be empty
		assertTrue("Jobs not removed from graph.", getLockManager().isEmpty());
	}

	/**
	 * Regression test for bug 46894. Stale rules left over in graph.
	 */
	public void _testRuleHierarchyLockInteraction() {
		final int NUM_JOBS = 5;
		final AtomicIntegerArray status = new AtomicIntegerArray(new int[NUM_JOBS]);
		fill(status, TestBarrier2.STATUS_WAIT_FOR_START);
		final ISchedulingRule[] rules = {new PathRule("/A"), new PathRule("/A/B"), new PathRule("/A/C")};
		Job[] jobs = new Job[NUM_JOBS];

		jobs[0] = new Job("Test 0") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					manager.beginRule(rules[1], null);
					status.set(0, TestBarrier2.STATUS_WAIT_FOR_RUN);
					TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_RUNNING);
					manager.endRule(rules[1]);
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};

		jobs[1] = new Job("Test 1") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					TestBarrier2.waitForStatus(status, 1, TestBarrier2.STATUS_START);
					manager.beginRule(rules[2], null);
					status.set(1, TestBarrier2.STATUS_WAIT_FOR_RUN);
					TestBarrier2.waitForStatus(status, 1, TestBarrier2.STATUS_RUNNING);
					manager.endRule(rules[2]);
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};

		jobs[2] = new Job("Test 2") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					TestBarrier2.waitForStatus(status, 2, TestBarrier2.STATUS_START);
					manager.beginRule(rules[0], new TestBlockingMonitor(status, 2));
					status.set(2, TestBarrier2.STATUS_WAIT_FOR_RUN);
					TestBarrier2.waitForStatus(status, 2, TestBarrier2.STATUS_RUNNING);
					manager.endRule(rules[0]);
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};

		jobs[3] = new Job("Test 3") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					TestBarrier2.waitForStatus(status, 3, TestBarrier2.STATUS_START);
					manager.beginRule(rules[2], new TestBlockingMonitor(status, 3));
					status.set(3, TestBarrier2.STATUS_WAIT_FOR_RUN);
					TestBarrier2.waitForStatus(status, 3, TestBarrier2.STATUS_RUNNING);
					manager.endRule(rules[2]);
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};

		jobs[4] = new Job("Test 4") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					TestBarrier2.waitForStatus(status, 4, TestBarrier2.STATUS_START);
					manager.beginRule(rules[2], new TestBlockingMonitor(status, 4));
					status.set(4, TestBarrier2.STATUS_WAIT_FOR_RUN);
					TestBarrier2.waitForStatus(status, 4, TestBarrier2.STATUS_RUNNING);
					manager.endRule(rules[2]);
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};

		for (Job job : jobs) {
			job.schedule();
		}
		//wait until the first job starts
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_WAIT_FOR_RUN);
		//now let the second job start
		status.set(1, TestBarrier2.STATUS_START);
		TestBarrier2.waitForStatus(status, 1, TestBarrier2.STATUS_WAIT_FOR_RUN);

		//let the third job register the wait
		status.set(2, TestBarrier2.STATUS_START);
		//wait until the job is blocked on the scheduling rule
		TestBarrier2.waitForStatus(status, 2, TestBarrier2.STATUS_BLOCKED);

		//let the fourth job register the wait
		status.set(3, TestBarrier2.STATUS_START);
		//wait until the job is blocked on the scheduling rule
		TestBarrier2.waitForStatus(status, 3, TestBarrier2.STATUS_BLOCKED);

		//end the first job, and the second job
		status.set(0, TestBarrier2.STATUS_RUNNING);
		status.set(1, TestBarrier2.STATUS_RUNNING);

		//wait until the third job gets the rule
		TestBarrier2.waitForStatus(status, 2, TestBarrier2.STATUS_WAIT_FOR_RUN);

		//let the fifth job start its wait
		status.set(4, TestBarrier2.STATUS_START);
		TestBarrier2.waitForStatus(status, 4, TestBarrier2.STATUS_BLOCKED);

		//let the third job finish
		status.set(2, TestBarrier2.STATUS_RUNNING);

		//wait until the fourth job gets the rule
		TestBarrier2.waitForStatus(status, 3, TestBarrier2.STATUS_WAIT_FOR_RUN);
		//let the fourth job end
		status.set(3, TestBarrier2.STATUS_RUNNING);

		//wait until the fifth job gets the rule
		TestBarrier2.waitForStatus(status, 4, TestBarrier2.STATUS_WAIT_FOR_RUN);
		//let the fifth job end
		status.set(4, TestBarrier2.STATUS_RUNNING);

		for (Job job : jobs) {
			waitForCompletion(job);
		}

		for (int i = 0; i < jobs.length; i++) {
			assertEquals("10." + i, Job.NONE, jobs[i].getState());
			assertEquals("10." + i, Status.OK_STATUS, jobs[i].getResult());
		}
		//the underlying graph has to be empty
		assertTrue("Jobs not removed from graph.", getLockManager().isEmpty());
	}

	/**
	 * Test that the deadlock detector resolves deadlock correctly.
	 * 60 threads are competing for 6 locks (need to acquire 3 locks at the same time).
	 */
	@Test
	public void testVeryComplex() {
		DeadlockDetector.runSilent(() -> {
			ArrayList<RandomTestRunnable> allRunnables = new ArrayList<>();
			LockManager lockManager = new LockManager();
			OrderedLock lock1 = lockManager.newLock();
			OrderedLock lock2 = lockManager.newLock();
			OrderedLock lock3 = lockManager.newLock();
			OrderedLock lock4 = lockManager.newLock();
			OrderedLock lock5 = lockManager.newLock();
			OrderedLock lock6 = lockManager.newLock();
			createRunnables(new ILock[] { lock1, lock2, lock3 }, 10, allRunnables, true);
			createRunnables(new ILock[] { lock2, lock3, lock4 }, 10, allRunnables, true);
			createRunnables(new ILock[] { lock3, lock4, lock5 }, 10, allRunnables, true);
			createRunnables(new ILock[] { lock4, lock5, lock6 }, 10, allRunnables, true);
			createRunnables(new ILock[] { lock5, lock6, lock1 }, 10, allRunnables, true);
			createRunnables(new ILock[] { lock6, lock1, lock2 }, 10, allRunnables, true);
			wait(allRunnables, lockManager);
		});
	}

	/**
	 * Spin until the given job completes
	 */
	private void waitForCompletion(Job job) {
		int i = 0;
		while (job.getState() != Job.NONE) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				//ignore
			}
			assertTrue("Timeout waiting for job to end:" + job, ++i < 100);
		}
	}

	/**
	 * Spin until the given thread dies
	 */
	private void waitForThreadDeath(Thread thread) {
		int i = 0;
		while (thread.isAlive()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				//ignore
			}
			assertTrue("Timeout waiting for job to end.", ++i < 100);
		}
	}

	/**
	 * Test a complex scenario of interaction between rules and locks.
	 * 15 jobs are competing for 5 rules and 5 locks.
	 * Each job must acquire 1 rule and 2 locks in random order.
	 */
	public void _testComplexRuleLockInteraction() {
		final int NUM_LOCKS = 5;
		final AtomicIntegerArray status = new AtomicIntegerArray(new int[] { TestBarrier2.STATUS_WAIT_FOR_START });
		final ISchedulingRule[] rules = {new PathRule("/A"), new PathRule("/A/B"), new PathRule("/A/C"), new PathRule("/A/B/D"), new PathRule("/A/C/E")};
		final ILock[] locks = {manager.newLock(), manager.newLock(), manager.newLock(), manager.newLock(), manager.newLock()};
		Job[] jobs = new Job[NUM_LOCKS * 3];
		final Random random = new Random();

		for (int i = 0; i < jobs.length; i++) {
			jobs[i] = new Job("Test" + i) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						monitor.beginTask("Testing", IProgressMonitor.UNKNOWN);
						while (status.get(0) != TestBarrier2.STATUS_DONE) {
							int indexRule = random.nextInt(NUM_LOCKS);
							int indexLock = random.nextInt(NUM_LOCKS);
							int secondIndex = random.nextInt(NUM_LOCKS);
							if ((indexRule % 2) == 0) {
								manager.beginRule(rules[indexRule], null);
								locks[indexLock].acquire();
								locks[secondIndex].acquire();
								assertTrue(indexRule + ".0", getLockManager().isLockOwner());
								locks[secondIndex].release();
								locks[indexLock].release();
								manager.endRule(rules[indexRule]);
							} else {
								locks[indexLock].acquire();
								manager.beginRule(rules[indexRule], null);
								locks[secondIndex].acquire();
								assertTrue(indexLock + ".0", getLockManager().isLockOwner());
								locks[secondIndex].release();
								manager.endRule(rules[indexRule]);
								locks[indexLock].release();
							}
							monitor.worked(1);
						}
					} catch (RuntimeException e) {
						e.printStackTrace();
					} finally {
						monitor.done();
					}
					return Status.OK_STATUS;
				}
			};
			jobs[i].schedule();
		}

		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			//ignore
		}

		status.set(0, TestBarrier2.STATUS_DONE);

		for (Job job : jobs) {
			int j = 0;
			while (job.getState() != Job.NONE) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					//ignore
				}
				//sanity check to avoid hanging tests
				assertTrue("Timeout waiting for jobs to finish.", ++j < 1000);
			}
		}

		for (int i = 0; i < jobs.length; i++) {
			assertEquals("10." + i, Job.NONE, jobs[i].getState());
			assertEquals("10." + i, Status.OK_STATUS, jobs[i].getResult());
		}
		//the underlying array has to be empty
		assertTrue("Jobs not removed from graph.", getLockManager().isEmpty());
	}

	/**
	 * Test that when a job with a rule is canceled, no stale entries are left in the graph.
	 */
	public void _testJobRuleCancellation() {
		final ISchedulingRule rule = new IdentityRule();
		final AtomicIntegerArray status = new AtomicIntegerArray(new int[] { TestBarrier2.STATUS_WAIT_FOR_START });

		Job first = new Job("Test1") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					assertTrue("1.0", getLockManager().isLockOwner());
					status.set(0, TestBarrier2.STATUS_START);
					TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_RUNNING);
					monitor.worked(1);
				} finally {
					monitor.done();
					status.set(0, TestBarrier2.STATUS_DONE);
				}
				return Status.OK_STATUS;
			}
		};

		Job second = new Job("Test2") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					assertTrue("2.0", getLockManager().isLockOwner());
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};

		first.setRule(rule);
		second.setRule(rule);

		first.schedule();
		TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_START);

		//schedule a job with the same rule and then cancel it
		second.schedule();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			//ignore
		}
		second.cancel();
		status.set(0, TestBarrier2.STATUS_RUNNING);
		TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_DONE);
		waitForCompletion(first);
		//the underlying graph should now be empty
		assertTrue("Canceled job not removed from graph.", getLockManager().isEmpty());
	}

	/**
	 * Test that a lock which was acquired several times and then suspended to resolve deadlock
	 * is set correctly to the proper depth when it is re-acquired by the thread that used to own it.
	 */
	public void _testLockMultipleAcquireThenSuspend() {
		final ISchedulingRule rule = new IdentityRule();
		final ILock lock = manager.newLock();
		final AtomicIntegerArray status = new AtomicIntegerArray(
				new int[] { TestBarrier2.STATUS_WAIT_FOR_START, TestBarrier2.STATUS_WAIT_FOR_START });

		Job first = new Job("Test1") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					manager.beginRule(rule, null);
					status.set(0, TestBarrier2.STATUS_WAIT_FOR_RUN);
					TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_START);
					lock.acquire();
					lock.release();
					manager.endRule(rule);
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};

		Job second = new Job("Test2") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					lock.acquire();
					lock.acquire();
					lock.acquire();
					lock.acquire();
					status.set(1, TestBarrier2.STATUS_WAIT_FOR_RUN);
					TestBarrier2.waitForStatus(status, 1, TestBarrier2.STATUS_START);
					manager.beginRule(rule, null);
					manager.endRule(rule);
					lock.release();
					status.set(1, TestBarrier2.STATUS_WAIT_FOR_RUN);
					TestBarrier2.waitForStatus(status, 1, TestBarrier2.STATUS_RUNNING);
					lock.release();
					lock.release();
					lock.release();
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		//schedule the jobs
		first.schedule();
		second.schedule();
		//wait until one gets a rule, and the other acquires a lock
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_WAIT_FOR_RUN);
		TestBarrier2.waitForStatus(status, 1, TestBarrier2.STATUS_WAIT_FOR_RUN);

		//let the deadlock happen
		status.set(0, TestBarrier2.STATUS_START);
		status.set(1, TestBarrier2.STATUS_START);

		//wait until it is resolved and the second job releases the lock once
		TestBarrier2.waitForStatus(status, 1, TestBarrier2.STATUS_WAIT_FOR_RUN);

		//the underlying graph should not be empty yet
		assertTrue("Held lock removed from graph.", !getLockManager().isEmpty());

		//wait until the jobs are done
		status.set(1, TestBarrier2.STATUS_RUNNING);
		waitForCompletion(first);
		waitForCompletion(second);
		//the underlying graph should now be empty
		assertTrue("Jobs not removed from graph.", getLockManager().isEmpty());
	}

	private void start(ArrayList<RandomTestRunnable> allRunnables) {
		for (RandomTestRunnable r : allRunnables) {
			r.start();
		}
	}
}
