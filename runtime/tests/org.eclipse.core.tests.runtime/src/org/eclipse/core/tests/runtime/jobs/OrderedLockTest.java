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
 *******************************************************************************/
package org.eclipse.core.tests.runtime.jobs;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicIntegerArray;
import org.eclipse.core.internal.jobs.LockManager;
import org.eclipse.core.internal.jobs.OrderedLock;
import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.core.runtime.jobs.LockListener;
import org.eclipse.core.tests.harness.TestBarrier2;
import org.junit.Test;

/**
 * Tests implementation of ILock objects
 */
public class OrderedLockTest {

	/**
	 * Creates n runnables on the given lock and adds them to the given list.
	 */
	private void createRunnables(ILock[] locks, int n, ArrayList<LockAcquiringRunnable> allRunnables) {
		for (int i = 0; i < n; i++) {
			allRunnables.add(new LockAcquiringRunnable(locks));
		}
	}

	private void kill(ArrayList<LockAcquiringRunnable> allRunnables) {
		for (LockAcquiringRunnable lockAcquiringRunnable : allRunnables) {
			lockAcquiringRunnable.kill();
		}
	}

	@Test
	public void testComplex() {
		ArrayList<LockAcquiringRunnable> allRunnables = new ArrayList<>();
		LockManager manager = new LockManager();
		OrderedLock lock1 = manager.newLock();
		OrderedLock lock2 = manager.newLock();
		OrderedLock lock3 = manager.newLock();
		createRunnables(new ILock[] {lock1, lock2, lock3}, 5, allRunnables);
		createRunnables(new ILock[] {lock3, lock2, lock1}, 5, allRunnables);
		createRunnables(new ILock[] {lock1, lock3, lock2}, 5, allRunnables);
		createRunnables(new ILock[] {lock2, lock3, lock1}, 5, allRunnables);
		start(allRunnables);
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
		}
		kill(allRunnables);
		for (LockAcquiringRunnable runnable : allRunnables) {
			runnable.isDone();
		}
		//the underlying array has to be empty
		assertTrue("Locks not removed from graph.", manager.isEmpty());
	}

	@Test
	public void testSimple() {
		ArrayList<LockAcquiringRunnable> allRunnables = new ArrayList<>();
		LockManager manager = new LockManager();
		OrderedLock lock1 = manager.newLock();
		OrderedLock lock2 = manager.newLock();
		OrderedLock lock3 = manager.newLock();
		createRunnables(new ILock[] {lock1, lock2, lock3}, 1, allRunnables);
		createRunnables(new ILock[] {lock3, lock2, lock1}, 1, allRunnables);
		start(allRunnables);
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
		}
		kill(allRunnables);
		for (LockAcquiringRunnable runnable : allRunnables) {
			runnable.isDone();
		}
		//the underlying array has to be empty
		assertTrue("Locks not removed from graph.", manager.isEmpty());
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
		Thread.sleep(500);
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
		final AtomicIntegerArray status = new AtomicIntegerArray(new int[] { TestBarrier2.STATUS_START });
		//array to end a runnable after it is no longer needed
		final boolean[] alive = {true};

		//first runnable which is going to hold the created lock
		Runnable getLock = () -> {
			lock.acquire();
			status.set(0, TestBarrier2.STATUS_RUNNING);
			while (alive[0]) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
			lock.release();
			status.set(0, TestBarrier2.STATUS_DONE);
		};

		//second runnable which is going to try and acquire the given lock and then time out
		Runnable tryForLock = () -> {
			boolean success = false;
			try {
				success = lock.acquire(100);
			} catch (InterruptedException e) {
			}
			assertTrue("1.0", !success);
			assertTrue("1.1", !manager.isLockOwner());
			status.set(0, TestBarrier2.STATUS_WAIT_FOR_DONE);
		};

		Thread first = new Thread(getLock);
		Thread second = new Thread(tryForLock);

		//start the first thread and wait for it to acquire the lock
		first.start();
		TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_RUNNING);
		//start the second thread, make sure the assertion passes
		second.start();
		TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_WAIT_FOR_DONE);
		//let the first thread die
		alive[0] = false;
		TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_DONE);
		//the underlying array has to be empty
		assertTrue("Locks not removed from graph.", manager.isEmpty());
	}

	/**
	 * test that when a Lock Listener forces the Lock Manager to grant a lock
	 * to a waiting thread, that other threads in the queue don't get disposed (regression test)
	 */
	@Test
	public void testLockRequestDisappearence() {
		//create a new lock manager and 1 lock
		final LockManager manager = new LockManager();
		final OrderedLock lock = manager.newLock();
		//status array for communicating between threads
		final AtomicIntegerArray status = new AtomicIntegerArray(
				new int[] { TestBarrier2.STATUS_WAIT_FOR_START, TestBarrier2.STATUS_WAIT_FOR_START });

		//first runnable which is going to hold the created lock
		Runnable getLock = () -> {
			lock.acquire();
			status.set(0, TestBarrier2.STATUS_START);
			TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_RUNNING);
			lock.release();
			status.set(0, TestBarrier2.STATUS_DONE);
		};

		//second runnable which is going to submit a request for this lock and wait until it is available
		Runnable waitForLock = () -> {
			status.set(1, TestBarrier2.STATUS_START);
			lock.acquire(); // has to be in waiting state before manager.setLockListener(listener) should
							// happen
			assertTrue("1.0", manager.isLockOwner()); // XXX fails silently in other thread and test times out.
			status.set(1, TestBarrier2.STATUS_WAIT_FOR_DONE);
			lock.release();
			status.set(1, TestBarrier2.STATUS_DONE);

		};

		//third runnable which is going to submit a request for this lock but not wait
		//because the hook is going to force it to be given the lock (implicitly)
		Runnable forceGetLock = () -> {
			lock.acquire();
			lock.release();
			status.set(0, TestBarrier2.STATUS_WAIT_FOR_DONE);
		};


		//a locklistener to force lock manager to give the lock to the third runnable (implicitly)
		LockListener listener = new LockListener() {
			@Override
			public boolean aboutToWait(Thread lockOwner) {
				return true;
			}
		};

		//assign each runnable to a separate thread
		Thread first = new Thread(getLock);
		Thread second = new Thread(waitForLock);
		Thread third = new Thread(forceGetLock);

		LockListener waitListener = new LockListener() {
			@Override
			public boolean aboutToWait(Thread lockOwner) {
				if (Thread.currentThread() == second) {
					status.set(1, TestBarrier2.STATUS_WAIT_FOR_DONE);
				}
				return false;
			}
		};
		//start the first thread and wait for it to acquire the lock
		first.start();
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_START);
		manager.setLockListener(waitListener);
		//start the second thread, make sure it is added to the lock wait queue
		second.start();
		TestBarrier2.waitForStatus(status, 1, TestBarrier2.STATUS_START);
		// wait till waitForLock's "lock.acquire()" has been progressed enough:
		TestBarrier2.waitForStatus(status, 1, TestBarrier2.STATUS_WAIT_FOR_DONE);

		//assign our listener to the manager
		manager.setLockListener(listener);
		//start the third thread
		third.start();
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_WAIT_FOR_DONE);

		//let the first runnable complete
		status.set(0, TestBarrier2.STATUS_RUNNING);
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_DONE);

		//now wait for the second runnable to get the lock, and have the assertion pass
		TestBarrier2.waitForStatus(status, 1, TestBarrier2.STATUS_DONE);

		//the underlying array has to be empty
		assertTrue("Locks not removed from graph.", manager.isEmpty());
	}

	private void start(ArrayList<LockAcquiringRunnable> allRunnables) {
		for (LockAcquiringRunnable lockAcquiringRunnable : allRunnables) {
			new Thread(lockAcquiringRunnable).start();
		}
	}
}
