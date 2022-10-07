/*******************************************************************************
 * Copyright (c) 2003, 2022 IBM Corporation and others.
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

import static org.junit.Assert.assertTrue;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import org.eclipse.core.internal.jobs.*;
import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.core.runtime.jobs.LockListener;
import org.eclipse.core.tests.harness.TestBarrier2;
import org.eclipse.core.tests.runtime.jobs.LockAcquiringRunnable.RandomOrder;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests implementation of ILock objects
 */
@SuppressWarnings("restriction")
public class OrderedLockTest {
	@Rule
	public RetryTestRule retry = new RetryTestRule(10); // executes all tests in the Class multiple times

	/**
	 * Creates n runnables on the given lock and adds them to the given list.
	 */
	private void createRunnables(ILock[] locks, int n, ArrayList<LockAcquiringRunnable> allRunnables) {
		for (int i = 0; i < n; i++) {
			allRunnables.add(new LockAcquiringRunnable(locks));
		}
	}

	@Test
	public void testComplex() {
		DeadlockDetector.runSilent(() -> {
			ArrayList<LockAcquiringRunnable> allRunnables = new ArrayList<>();
			LockManager manager = new LockManager();
			OrderedLock lock1 = manager.newLock();
			OrderedLock lock2 = manager.newLock();
			OrderedLock lock3 = manager.newLock();
			createRunnables(new ILock[] { lock1, lock2, lock3 }, 5, allRunnables);
			createRunnables(new ILock[] { lock3, lock2, lock1 }, 5, allRunnables);
			createRunnables(new ILock[] { lock1, lock3, lock2 }, 5, allRunnables);
			createRunnables(new ILock[] { lock2, lock3, lock1 }, 5, allRunnables);
			execute(allRunnables);
			// the underlying array has to be empty
			assertTrue("Locks not removed from graph.", manager.isEmpty());
		});
	}

	@Test
	public void testSimple() {
		DeadlockDetector.runSilent(() -> {
			ArrayList<LockAcquiringRunnable> allRunnables = new ArrayList<>();
			LockManager manager = new LockManager();
			OrderedLock lock1 = manager.newLock();
			OrderedLock lock2 = manager.newLock();
			OrderedLock lock3 = manager.newLock();
			createRunnables(new ILock[] { lock1, lock2, lock3 }, 1, allRunnables);
			createRunnables(new ILock[] { lock3, lock2, lock1 }, 1, allRunnables);
			execute(allRunnables);
			// the underlying array has to be empty
			assertTrue("Locks not removed from graph.", manager.isEmpty());
		});
	}

	@Test
	public void testLockAcquireInterrupt() throws InterruptedException {
		final TestBarrier2 barrier = new TestBarrier2();
		LockManager manager = new LockManager();
		final ILock lock = manager.newLock();
		final boolean[] wasInterupted = new boolean[] {false};
		Thread t = new Thread() {
			@Override
			public void run() {
				barrier.setStatus(TestBarrier2.STATUS_RUNNING);
				barrier.waitForStatus(TestBarrier2.STATUS_WAIT_FOR_START);
				barrier.setStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);
				lock.acquire();
				wasInterupted[0] = Thread.currentThread().isInterrupted();
				lock.release();
			}
		};
		//schedule and wait for thread to start
		t.start();
		barrier.waitForStatus(TestBarrier2.STATUS_RUNNING);
		//acquire the lock and let the thread proceed
		lock.acquire();
		barrier.setStatus(TestBarrier2.STATUS_WAIT_FOR_START);
		//make sure thread is blocked
		barrier.waitForStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);
		Thread.yield();
		t.interrupt();
		lock.release();
		t.join();
		assertTrue("1.0", wasInterupted[0]);
	}

	/**
	 * test that an acquire call that times out does not
	 * become the lock owner (regression test)
	 */
	@Test
	public void testLockTimeout() {
		//create a new lock manager and 1 lock
		final LockManager manager = new LockManager();
		final OrderedLock lock = manager.newLock();
		//status array for communicating between threads
		final TestBarrier2 status = new TestBarrier2(TestBarrier2.STATUS_START);
		//array to end a runnable after it is no longer needed
		final boolean[] alive = {true};

		//first runnable which is going to hold the created lock
		Runnable getLock = () -> {
			lock.acquire();
			status.upgradeTo(TestBarrier2.STATUS_RUNNING);
			while (alive[0]) {
				Thread.yield();
			}
			lock.release();
			status.upgradeTo(TestBarrier2.STATUS_DONE);
		};

		//second runnable which is going to try and acquire the given lock and then time out
		Runnable tryForLock = () -> {
			boolean success = false;
			try {
				success = lock.acquire(0);
			} catch (InterruptedException e) {
			}
			assertTrue("1.0", !success);
			assertTrue("1.1", !manager.isLockOwner());
			status.upgradeTo(TestBarrier2.STATUS_WAIT_FOR_DONE);
		};

		Thread first = new Thread(getLock);
		Thread second = new Thread(tryForLock);

		//start the first thread and wait for it to acquire the lock
		first.start();
		status.waitForStatus(TestBarrier2.STATUS_RUNNING);
		//start the second thread, make sure the assertion passes
		second.start();
		status.waitForStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);
		//let the first thread die
		alive[0] = false;
		status.waitForStatus(TestBarrier2.STATUS_DONE);
		//the underlying array has to be empty
		assertTrue("Locks not removed from graph.", manager.isEmpty());
	}

	/**
	 * test that when a Lock Listener forces the Lock Manager to grant a lock
	 * to a waiting thread, that other threads in the queue don't get disposed (regression test)
	 */
	@Test
	public void testLockRequestDisappearence() {
		// create a new lock manager and 1 lock
		final LockManager manager = new LockManager();
		final OrderedLock lock = manager.newLock();
		// communicating between threads:
		final TestBarrier2 status0main = new TestBarrier2(TestBarrier2.STATUS_WAIT_FOR_START);
		final TestBarrier2 status1getLock = new TestBarrier2(TestBarrier2.STATUS_WAIT_FOR_START);
		final TestBarrier2 status2waitForLock = new TestBarrier2(TestBarrier2.STATUS_WAIT_FOR_START);
		final TestBarrier2 status3forceGetLock = new TestBarrier2(TestBarrier2.STATUS_WAIT_FOR_START);
		Collection<Throwable> errors = new ConcurrentLinkedQueue<>();

		// first runnable which is going to hold the created lock
		Thread thread1getLock = new Thread("thread1getLock") {
			@Override
			public void run() {
				try {
					lock.acquire();
					status1getLock.upgradeTo(TestBarrier2.STATUS_START);
					status0main.waitForStatus(TestBarrier2.STATUS_RUNNING);
					lock.release();
					status1getLock.upgradeTo(TestBarrier2.STATUS_DONE);
				} catch (Throwable t) {
					errors.add(t);
				}
			}
		};

		// second runnable which is going to submit a request for this lock and wait
		// until it is available
		Thread thread2waitForLock = new Thread("thread2waitForLock") {
			@Override
			public void run() {
				try {
					status2waitForLock.upgradeTo(TestBarrier2.STATUS_START);
					lock.acquire(); // has to be in waiting state before manager.setLockListener(listener) should
									// happen
					assertTrue("1.0", manager.isLockOwner());
					// status2waitForLock.upgrade(TestBarrier2.STATUS_WAIT_FOR_DONE);// done in
					// waitListener
					lock.release();
					status2waitForLock.upgradeTo(TestBarrier2.STATUS_DONE);
				} catch (Throwable t) {
					errors.add(t);
				}
			}
		};

		// third runnable which is going to submit a request for this lock but not wait
		// because the hook is going to force it to be given the lock (implicitly)
		Thread thread3forceGetLock = new Thread("thread3forceGetLock") {
			@Override
			public void run() {
				try {
					lock.acquire();
					lock.release();
					status3forceGetLock.upgradeTo(TestBarrier2.STATUS_DONE);
				} catch (Throwable t) {
					errors.add(t);
				}
			}
		};

		// a LockListener to force lock manager to give the lock to the third runnable
		// (implicitly)
		LockListener listener = new LockListener() {
			@Override
			public boolean aboutToWait(Thread lockOwner) {
				return true;
			}
		};

		LockListener waitListener = new LockListener() {
			@Override
			public boolean aboutToWait(Thread lockOwner) {
				if (Thread.currentThread() == thread2waitForLock) {
					status0main.waitForStatus(TestBarrier2.STATUS_WAIT_FOR_RUN);
					status2waitForLock.upgradeTo(TestBarrier2.STATUS_WAIT_FOR_DONE);
				}
				return false;
			}
		};
		// start the first thread and wait for it to acquire the lock
		thread1getLock.start();
		manager.setLockListener(waitListener);
		status1getLock.waitForStatus(TestBarrier2.STATUS_START);
		// start the second thread, make sure it is added to the lock wait queue
		thread2waitForLock.start();
		status2waitForLock.waitForStatus(TestBarrier2.STATUS_START);
		status0main.upgradeTo(TestBarrier2.STATUS_WAIT_FOR_RUN);
		// wait till waitForLock's "lock.acquire()" has been progressed enough:
		status2waitForLock.waitForStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);

		// assign our listener to the manager
		manager.setLockListener(listener);
		// start the third thread
		thread3forceGetLock.start();
		status3forceGetLock.waitForStatus(TestBarrier2.STATUS_DONE);

		// let the first runnable complete
		status0main.upgradeTo(TestBarrier2.STATUS_RUNNING);
		status1getLock.waitForStatus(TestBarrier2.STATUS_DONE);

		// now wait for the second runnable to get the lock, and have the assertion pass
		status2waitForLock.waitForStatus(TestBarrier2.STATUS_DONE);

		// the underlying array has to be empty
		assertTrue("Locks not removed from graph.", manager.isEmpty());
		errors.forEach(e -> e.printStackTrace());
		assertTrue("Error happend: " + errors.stream().map(e -> "" + e).collect(Collectors.joining(", ")),
				errors.isEmpty());
	}

	private void execute(ArrayList<LockAcquiringRunnable> allRunnables) {
		RandomOrder randomOrder = new RandomOrder(allRunnables, 2);
		for (LockAcquiringRunnable lockAcquiringRunnable : allRunnables) {
			lockAcquiringRunnable.setRandomOrder(randomOrder);
		}
		List<Thread> threads = new ArrayList<>();
		for (LockAcquiringRunnable lockAcquiringRunnable : allRunnables) {
			Thread thread = new Thread(lockAcquiringRunnable);
			threads.add(thread);
			thread.start();
		}
		randomOrder.waitForEnd();
		long joinMillis = 5000;
		for (Thread thread : threads) {
			try {
				long n0 = System.nanoTime();
				thread.join(joinMillis);
				long n1 = System.nanoTime();
				joinMillis = Math.max(joinMillis - (n1 - n0) / 1000_000, 1);
				if (thread.isAlive()) {
					throw new IllegalStateException("thread did not end");

				}
			} catch (InterruptedException e) {
				throw new IllegalStateException("interrupted");
			}
		}
	}

}
