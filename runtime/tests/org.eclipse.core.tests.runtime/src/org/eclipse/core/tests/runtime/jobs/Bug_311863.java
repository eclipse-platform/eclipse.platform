/*******************************************************************************
 * Copyright (c) 2010, 2015 Broadcom Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     James Blackburn (Broadcom Corp.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.runtime.jobs;

import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.tests.harness.TestBarrier2;

/**
 * Regression test for bug 311863
 * Interrupting a thread, just as it acquires a lock, loses the lock and deadlocks
 * occurs if another thread is in the queue waiting for the lock
 */
public class Bug_311863 extends AbstractJobManagerTest {

	/** Signal to the threads that we're done */
	volatile boolean finished = false;

	final int WAIT_ACQUIRE = 10002;
	final int RELEASED = 10013;
	final int DONE = 10023;

	final ILock lock = Job.getJobManager().newLock();

	/**
	 * Test thread that tries to acquire the lock,
	 * yields and sleeps
	 */
	class TestThread extends Thread {
		private final TestBarrier2 tb;
		private final int yield_time;

		public TestThread(TestBarrier2 tb, int yield_time) {
			this.tb = tb;
			this.yield_time = yield_time;
		}

		@Override
		public void run() {
			while (true) {
				tb.waitForStatus(WAIT_ACQUIRE);
				if (finished)
					break;
				for (int i = 0; i < 5; i++) {
					lock.acquire();
					try {
						Thread.sleep(yield_time);
						Thread.yield();
					} catch (InterruptedException e) {
					}
					lock.release();
					Thread.interrupted();
				}
				tb.setStatus(RELEASED);
			}
			tb.setStatus(DONE);
		}
	}

	/**
	 * Threads: main, t1, t2, t3
	 * Locks: lock1
	 *
	 * All three threads try to acquire and release lock1 while being concurrently
	 * interrupted.
	 *
	 * What goes wrong in this:
	 * Say t1 holds lock1.
	 * If t2 is interrupted *just* as t1 releases the lock. t2's released
	 * semaphore is discarded leading to deadlock as t1 gets put behind
	 * t3 in the queue to acquire the lock.
	 *
	 * Particularly insidious as the UI thread is interrupted frequently
	 *
	 * @throws Exception
	 */
	public void testInterruptDuringLockRelease() throws Exception {
		final TestBarrier2 tb1 = new TestBarrier2(-1);
		final TestBarrier2 tb2 = new TestBarrier2(-1);
		final TestBarrier2 tb3 = new TestBarrier2(-1);

		// The threads that will fight over the lock
		Thread t1 = new TestThread(tb1, 1);
		Thread t2 = new TestThread(tb2, 2);
		Thread t3 = new TestThread(tb3, 3);

		// Start the threads
		t1.start();
		t2.start();
		t3.start();

		// iterations
		for (int i = 0; i < 10; i++) {
			// t1, t2, t3 fight for the lock
			tb1.setStatus(WAIT_ACQUIRE);
			tb2.setStatus(WAIT_ACQUIRE);
			tb3.setStatus(WAIT_ACQUIRE);

			// Comment out makes the test pass
			for (int j = 0; j < 10; j++) {
				t1.interrupt();
				t2.interrupt();
				t3.interrupt();
				Thread.yield();
			}

			// Release everything and start again
			tb1.waitForStatus(RELEASED);
			tb2.waitForStatus(RELEASED);
			tb3.waitForStatus(RELEASED);
			//			System.out.println("Success " + i);
		}
		finished = true;

		// release the threads
		tb1.setStatus(WAIT_ACQUIRE);
		tb2.setStatus(WAIT_ACQUIRE);
		tb3.setStatus(WAIT_ACQUIRE);

		// wait for them to go away
		tb1.waitForStatus(DONE);
		tb2.waitForStatus(DONE);
		tb3.waitForStatus(DONE);
	}
}