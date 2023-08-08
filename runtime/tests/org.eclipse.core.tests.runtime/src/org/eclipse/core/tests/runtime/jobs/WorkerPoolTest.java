/*******************************************************************************
 * Copyright (c) 2017 salesforce.com and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     salesforce.com - initial API and implementation
 *     Vector Informatik GmbH - runtime and structure improvements
 *
 *******************************************************************************/
package org.eclipse.core.tests.runtime.jobs;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import org.eclipse.core.internal.jobs.Worker;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("restriction")
public class WorkerPoolTest {

	/** See org.eclipse.core.internal.jobs.WorkerPool.MAX_THREADS */
	private static final int MAX_ALLOWED_IDLE_WORKER_THREADS = 50;
	/** Multiplier to create an excess of threads */
	private static final int THREAD_MULTIPLIER = 10;
	/** Maximum time in seconds before job start barrier timeout */
	private static final int BARRIER_TIMEOUT_IN_SEC = 100;
	/** Maximum time in milliseconds to before test failure */
	private static final int WORKER_TIMEOUT_IN_MSEC = 5000;

	/**
	 * Tests the upper limit of idle worker threads allowed in the job system after
	 * scheduling a large number of concurrent jobs.
	 * <p>
	 * This test performs the following steps:
	 * <ul>
	 * <li>Initializes a cyclic barrier with a count based on a multiplier of the
	 * defined maximum worker threads.</li>
	 * <li>Schedules a number of jobs equal to the multiplier of the maximum worker
	 * threads. Each job waits on the barrier until all jobs are running.</li>
	 * <li>Monitors the number of active worker threads. Continuously polls until
	 * the count of active worker threads is less than or equal to the defined
	 * maximum or a timeout occurs.</li>
	 * </ul>
	 * </p>
	 *
	 * @throws Exception If the barrier timeout runs out
	 *
	 * @see <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=516609"> Bug
	 *      516609 </a>
	 */
	@Test
	public void testIdleWorkerCap() throws Exception {
		final int totalJobs = MAX_ALLOWED_IDLE_WORKER_THREADS * THREAD_MULTIPLIER;
		final CyclicBarrier parallelJobStartBarrier = new CyclicBarrier(totalJobs + 1);

		for (int i = 0; i < totalJobs; i++) {
			new Job("testIdleWorkerCap-" + i) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						parallelJobStartBarrier.await();
					} catch (InterruptedException | BrokenBarrierException e) {
						return Status.CANCEL_STATUS;
					}
					return Status.OK_STATUS;
				}
			}.schedule();
		}

		// wait for jobs to reach the barrier
		parallelJobStartBarrier.await(BARRIER_TIMEOUT_IN_SEC, TimeUnit.SECONDS);

		long startTimeInMSec = System.currentTimeMillis();

		// wait for workerThreadCount to sink below MAX_ALLOWED_IDLE_WORKER_THREADS
		int workerThreadCount = getWorkerThreadCount();
		while (workerThreadCount > MAX_ALLOWED_IDLE_WORKER_THREADS) {
			if (System.currentTimeMillis() - startTimeInMSec > WORKER_TIMEOUT_IN_MSEC) {
				Assert.fail("Timeout reached! Too many worker threads active: " + workerThreadCount
						+ ", expected <= "
						+ MAX_ALLOWED_IDLE_WORKER_THREADS);
			}

			Thread.yield();
			workerThreadCount = getWorkerThreadCount();
		}
	}

	private int getWorkerThreadCount() {
		Thread[] threads = new Thread[Thread.activeCount() * 2];
		int enumeratedThreadCount = Thread.enumerate(threads);
		assertTrue("Too many active threads: " + enumeratedThreadCount, enumeratedThreadCount < threads.length);

		int workerThreadCount = 0;
		for (int i = 0; i < enumeratedThreadCount; i++) {
			if (threads[i] instanceof Worker) {
				workerThreadCount++;
			}
		}
		return workerThreadCount;
	}
}
