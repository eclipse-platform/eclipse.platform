/*******************************************************************************
 * Copyright (c) 2019 Xored Software Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Xored Software Inc - initial implementation
 *******************************************************************************/
package org.eclipse.core.tests.runtime.jobs;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.core.tests.harness.TestJob;

/**
 * Regression test for bug 550738.
 *
 * Job should not start after cancel() if no schedule() is called after it. Job
 * should start after schedule() if no cancel() is called after it.
 *
 */
public class Bug_550738 extends AbstractJobManagerTest {
	private static final class BusyLoopJob extends TestJob {
		public Runnable started = () -> {
		};
		public volatile boolean cancelWithoutRelyingOnFramework = false;

		public BusyLoopJob() {
			super("Bug_550738", 1, 1);
		}

		@Override
		public IStatus run(IProgressMonitor monitor) {
			started.run();
			while (!monitor.isCanceled() && !cancelWithoutRelyingOnFramework) {
				Thread.yield();
			}
			return super.run(monitor);
		}
	}

	private static final class EventCount extends JobChangeAdapter {
		public final AtomicInteger doneCount = new AtomicInteger(0);
		public final AtomicInteger scheduledCount = new AtomicInteger(0);

		@Override
		public void done(IJobChangeEvent event) {
			super.done(event);
			doneCount.incrementAndGet();
		}

		@Override
		public void scheduled(IJobChangeEvent event) {
			super.scheduled(event);
			scheduledCount.incrementAndGet();
		}
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		// don't use fussy progress monitor, because in this test we may kill
		// a job before it has started running
		manager.setProgressProvider(null);
	}

	public void testCancelSchedule() throws InterruptedException {
		BusyLoopJob job = new BusyLoopJob();
		try {
			for (int i = 0; i < 10000; i++) {
				// TestBarrier is too slow to do enough iterations for this test
				CountDownLatch startedLatch = new CountDownLatch(1);
				job.started = startedLatch::countDown;
				job.cancel();
				job.schedule();

				assertTrue("Job should start after schedule. Iteration " + i, startedLatch.await(5, TimeUnit.SECONDS));

				// A sequence of cancellations and rescheduling that is not expected to affect
				// further operations.
				// But it does!
				job.cancel();
				job.schedule();

				job.cancel();
				assertTrue("Job should stop after cancellation. Iteration " + i, job.join(5000, null));

			}
		} finally {
			// This bug prevents guaranteed cancellation, so we can't rely on framework to
			// kill the job
			job.cancelWithoutRelyingOnFramework = true;
			waitForCompletion(job);
		}
	}

	public void testReportDoneOncePerSchedule() throws InterruptedException {
		BusyLoopJob job = new BusyLoopJob();
		EventCount eventCount = new EventCount();
		job.addJobChangeListener(eventCount);

		try {
			for (int i = 0; i < 10000; i++) {
				job.schedule();
				job.cancel();
			}
			assertTrue(job.join(5000, null));
			// There is no way to synchronize with notification section of
			// org.eclipse.core.internal.jobs.JobManager.endJob(InternalJob, IStatus,
			// boolean)
			// Job post-processing may happen after it is switched to NONE state
			// join() alone is not enough
			Thread.sleep(200);
			assertEquals("Job should be completed once per schedule.", eventCount.scheduledCount.get(),
					eventCount.doneCount.get());

		} finally {
			job.cancelWithoutRelyingOnFramework = true;
			waitForCompletion(job);
		}
	}
}
