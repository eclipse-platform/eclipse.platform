/*******************************************************************************
 * Copyright (c) 2017 salesforce.com.
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
 *******************************************************************************/
package org.eclipse.core.tests.runtime.jobs;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.*;
import org.eclipse.core.internal.jobs.Worker;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.Test;

public class WorkerPoolTest {
	@Test
	public void testIdleWorkerCap() throws Exception {
		// See org.eclipse.core.internal.jobs.WorkerPool.MAX_THREADS
		final int MAX_THREADS = 50;

		// number of concurrent jobs
		final int count = MAX_THREADS * 10;

		// cyclic barrier for count worker threads + one test thread
		final CyclicBarrier barrier = new CyclicBarrier(count + 1);

		// start count concurrent jobs
		for (int i = 0; i < count; i++) {
			new Job("testIdleWorkerCap-" + i) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						barrier.await();
					} catch (InterruptedException | BrokenBarrierException e) {
						return Status.CANCEL_STATUS;
					}
					return Status.OK_STATUS;
				}
			}.schedule();
		}

		// wait for jobs to reach the barrier
		barrier.await(10, TimeUnit.SECONDS);

		// this is the ugly part, wait until worker threads become idle
		Thread.sleep(5 * 1000L);

		// count worker threads, must be less than WorkerPool.MAX_THREADS
		Thread[] threads = new Thread[Thread.activeCount() * 2];
		int tcount = Thread.enumerate(threads);
		assertTrue("Too many active threads: " + tcount, tcount < threads.length);
		int wcount = 0;
		for (int i = 0; i < tcount; i++) {
			if (threads[i] instanceof Worker) {
				wcount++;
			}
		}
		assertTrue("Too many worker threads active: " + wcount + ", must be <= " + MAX_THREADS, wcount <= MAX_THREADS);
	}

}
