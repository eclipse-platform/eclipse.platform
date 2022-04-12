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
 * Regression test for bug 307282
 * This tests that a thread interrupted while it attempts to acquire a lock
 * doesn't break any future acquires of the lock.
 */
public class Bug_307282 extends AbstractJobManagerTest {

	/**
	 * Threads: main, t1, t2
	 * Locks: l1, l2
	 *
	 * t1 owns l1; t2 own l2
	 * t2 attempts acquire of l1
	 * main interrupt()s t2
	 * t1 release l1
	 * main attempt acquire() l1
	 *
	 * @throws Exception
	 */
	public void testInterruptDuringLockAcquireint() throws Exception {

		final ILock lock1 = Job.getJobManager().newLock();
		final ILock lock2 = Job.getJobManager().newLock();

		final TestBarrier2 tb1 = new TestBarrier2(-1);
		final TestBarrier2 tb2 = new TestBarrier2(-1);

		final int INTERRUPTED = 1000;
		final int RELEASE_LOCK = 10001;

		Thread t = new Thread() {
			@Override
			public void run() {
				lock1.acquire();

				tb1.setStatus(TestBarrier2.STATUS_WAIT_FOR_START);
				tb1.waitForStatus(TestBarrier2.STATUS_START);

				tb1.waitForStatus(RELEASE_LOCK);
				lock1.release();

				tb1.waitForStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);
				tb1.setStatus(TestBarrier2.STATUS_DONE);
			}
		};

		Thread t2 = new Thread() {
			@Override
			public void run() {
				lock2.acquire();

				tb2.setStatus(TestBarrier2.STATUS_WAIT_FOR_START);
				tb2.waitForStatus(TestBarrier2.STATUS_START);

				// Now attempt acquire lock1 with an integer delay
				try {
					assertTrue(!lock1.acquire(60 * 1000));
				} catch (InterruptedException e) {
					tb2.setStatus(INTERRUPTED);
				}

				tb2.waitForStatus(RELEASE_LOCK);
				lock2.release();

				tb2.waitForStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);
				tb2.setStatus(TestBarrier2.STATUS_DONE);
			}
		};

		// Start the threads
		t.start();
		t2.start();

		// wait for the threads to get to start and acquire first lock
		tb1.waitForStatus(TestBarrier2.STATUS_WAIT_FOR_START);
		tb2.waitForStatus(TestBarrier2.STATUS_WAIT_FOR_START);
		// now let thread 2 attempt to acquire lock 2
		tb1.setStatus(TestBarrier2.STATUS_START);
		tb2.setStatus(TestBarrier2.STATUS_START);
		Thread.sleep(1000); // not-so-small sleep so thread 2 is definitely in the acquire

		// Interrupt thread 2 while it's waiting for lock1
		t2.interrupt();
		tb2.waitForStatus(INTERRUPTED);

		// thread 1 release lock 1
		tb1.setStatus(RELEASE_LOCK);

		// We should now be able to acquire lock1 without difficulty
		assertTrue(lock1.acquire(1000));
		// T2 should still hold the lock2
		assertTrue(!lock2.acquire(0));

		tb2.setStatus(RELEASE_LOCK);
		assertTrue(lock2.acquire(1000));

		tb1.setStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);
		tb2.setStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);
		tb1.waitForStatus(TestBarrier2.STATUS_DONE);
		tb2.waitForStatus(TestBarrier2.STATUS_DONE);
	}
}
