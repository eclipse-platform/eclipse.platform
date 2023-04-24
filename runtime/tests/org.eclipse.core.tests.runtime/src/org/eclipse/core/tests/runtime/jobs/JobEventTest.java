/*******************************************************************************
 * Copyright (c) 2022 Joerg Kubitz and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Joerg Kubitz - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.runtime.jobs;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.core.internal.jobs.JobListeners;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.core.tests.harness.TestBarrier2;
import org.eclipse.core.tests.runtime.jobs.OrderAsserter.Event;
import org.junit.Test;

/**
 * Test for bug https://github.com/eclipse-platform/eclipse.platform/issues/193
 */
@SuppressWarnings("restriction")
public class JobEventTest {
	@Test
	public void testScheduleOrderOfEvents() {
		OrderAsserter asserter = new OrderAsserter();
		Event START = asserter.getNext("START"); // happens in test-Thread
		/** queued in schedule() thread OR rescheduled in WORKER-Thread */
		Event SCHEDULED = asserter.getNext("IJobChangeEvent.scheduled()");
		/** queued in WORKER-Thread */
		Event ABOUTTORUN = asserter.getNext("IJobChangeEvent.aboutToRun()");
		/** happens in schedule-Thread **/
		// race condition ABOUTTORUN with RETURN_FROM_SCHEDULE
//		Event RETURN_FROM_SCHEDULE = asserter.getNext("RETURN FROM Job.schedule()");
		/** queued in WORKER-Thread **/
		Event RUNNING = asserter.getNext("IJobChangeEvent.running()");
		/** happens in WORKER-Thread **/
		Event RUN = asserter.getNext("Job.run()");
		/** queued in WORKER-Thread OR canceling (cancel(),schedule(),wakeUp()) **/
		Event DONE = asserter.getNext("IJobChangeEvent.done()");
		/** happens in join()-Thread **/
		Event RETURN_FROM_JOIN = asserter.getNext("RETURN FROM Job.join()");

		/** queued in sleep() Thread **/
		Event SLEEPING = asserter.never("IJobChangeEvent.sleeping()");
		/** queued in wakeUp() Thread **/
		Event AWAKE = asserter.never("IJobChangeEvent.awake()");

		Job job = new Job("testScheduleOrderOfEvents") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				asserter.expect(RUN);
				return Status.OK_STATUS;
			}

		};
		asserter.expect(START);

		IJobChangeListener jobListener = new IJobChangeListener() {
			@Override
			public void scheduled(IJobChangeEvent event) {
				asserter.expect(SCHEDULED);
			}

			@Override
			public void sleeping(IJobChangeEvent event) {
				asserter.expect(SLEEPING);
			}

			@Override
			public void awake(IJobChangeEvent event) {
				asserter.expect(AWAKE);
			}

			@Override
			public void aboutToRun(IJobChangeEvent event) {
				asserter.expect(ABOUTTORUN);
			}

			@Override
			public void running(IJobChangeEvent event) {
				asserter.expect(RUNNING);
			}

			@Override
			public void done(IJobChangeEvent event) {
				asserter.expect(DONE);
			}
		};
		job.addJobChangeListener(jobListener);
		try {
			job.schedule();
//			asserter.expect(RETURN_FROM_SCHEDULE);
			try {
				job.join();
			} catch (InterruptedException e) {
				asserter.addError(e);
			}
			asserter.expect(RETURN_FROM_JOIN);
			asserter.assertNoErrors();
		} finally {
			job.removeJobChangeListener(jobListener);
		}
	}

	@Test
	public void testSelfRescheduleOrderOfEvents() {
		OrderAsserter asserter = new OrderAsserter();
		Event SCHEDULED1 = asserter.getNext("First IJobChangeEvent.scheduled()");
		Event ABOUTTORUN1 = asserter.getNext("First IJobChangeEvent.aboutToRun()");
		Event RUNNING1 = asserter.getNext("First IJobChangeEvent.running()");
		Event RUN1 = asserter.getNext("First Job.run()");
		Event DONE1 = asserter.getNext("First IJobChangeEvent.done()");
		Event SCHEDULED2 = asserter.getNext("Second IJobChangeEvent.scheduled()");
		// RETURN_FROM_JOIN1 race condition with ABOUTTORUN2
		// Event RETURN_FROM_JOIN1 = asserter.getNext("First RETURN FROM Job.join()");

		Event ABOUTTORUN2 = asserter.getNext("Second IJobChangeEvent.aboutToRun()");
		Event RUNNING2 = asserter.getNext("Second IJobChangeEvent.running()");
		Event RUN2 = asserter.getNext("Second Job.run()");
		Event DONE2 = asserter.getNext("Second IJobChangeEvent.done()");
		Event RETURN_FROM_JOIN2 = asserter.getNext("Second RETURN FROM Job.join()");

		Event SLEEPING = asserter.never("IJobChangeEvent.sleeping()");
		Event AWAKE = asserter.never("IJobChangeEvent.awake()");

		Job job = new Job("testScheduleOrderOfEvents") {
			private final AtomicInteger runCount = new AtomicInteger();

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				switch (runCount.incrementAndGet()) {
				case 1:
					asserter.expect(RUN1);
					schedule(); // reschedule
					break;
				case 2:
					asserter.expect(RUN2);
					break;
				}
				return Status.OK_STATUS;
			}

		};

		IJobChangeListener jobListener = new IJobChangeListener() {
			private final AtomicInteger scheduledCount = new AtomicInteger();
			private final AtomicInteger aboutToRunCount = new AtomicInteger();
			private final AtomicInteger runningCount = new AtomicInteger();
			private final AtomicInteger doneCount = new AtomicInteger();

			@Override
			public void scheduled(IJobChangeEvent event) {
				switch (scheduledCount.incrementAndGet()) {
				case 1:
					asserter.expect(SCHEDULED1);
					break;
				case 2:
					asserter.expect(SCHEDULED2);
					break;
				}
			}

			@Override
			public void sleeping(IJobChangeEvent event) {
				asserter.expect(SLEEPING);
			}

			@Override
			public void awake(IJobChangeEvent event) {
				asserter.expect(AWAKE);
			}

			@Override
			public void aboutToRun(IJobChangeEvent event) {
				switch (aboutToRunCount.incrementAndGet()) {
				case 1:
					asserter.expect(ABOUTTORUN1);
					break;
				case 2:
					asserter.expect(ABOUTTORUN2);
					break;
				}
			}

			@Override
			public void running(IJobChangeEvent event) {
				switch (runningCount.incrementAndGet()) {
				case 1:
					asserter.expect(RUNNING1);
					break;
				case 2:
					asserter.expect(RUNNING2);
					break;
				}
			}

			@Override
			public void done(IJobChangeEvent event) {
				switch (doneCount.incrementAndGet()) {
				case 1:
					asserter.expect(DONE1);
					break;
				case 2:
					asserter.expect(DONE2);
					break;
				}
			}
		};
		job.addJobChangeListener(jobListener);
		try {
			job.schedule();
			try {
				job.join();
			} catch (InterruptedException e) {
				asserter.addError(e);
			}
			// asserter.expect(RETURN_FROM_JOIN1);
			try {
				job.join();
			} catch (InterruptedException e) {
				asserter.addError(e);
			}
			asserter.expect(RETURN_FROM_JOIN2);
			asserter.assertNoErrors();
		} finally {
			job.removeJobChangeListener(jobListener);
		}
	}

	@Test
	public void testSleepOrderOfEvents() {
		OrderAsserter asserter = new OrderAsserter();
		Event SCHEDULED = asserter.getNext("IJobChangeEvent.scheduled()");
		Event ABOUTTORUN1 = asserter.getNext("First IJobChangeEvent.aboutToRun()");
		Event SLEEPING = asserter.getNext("IJobChangeEvent.sleeping()");
		Event AWAKE = asserter.getNext("IJobChangeEvent.awake()");
		Event ABOUTTORUN2 = asserter.getNext("Second IJobChangeEvent.aboutToRun()");
		Event RUNNING = asserter.getNext("IJobChangeEvent.running()");
		Event RUN = asserter.getNext("Job.run()");
		Event DONE = asserter.getNext("IJobChangeEvent.done()");
		Event RETURN_FROM_JOIN = asserter.getNext("RETURN FROM Job.join()");

		Job job = new Job("testSleepOrderOfEvents") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				asserter.expect(RUN);
				return Status.OK_STATUS;
			}

		};
		IJobChangeListener jobListener = new IJobChangeListener() {
			private final AtomicInteger aboutToRunCount = new AtomicInteger();

			@Override
			public void scheduled(IJobChangeEvent event) {
				asserter.expect(SCHEDULED);
			}

			@Override
			public void sleeping(IJobChangeEvent event) {
				asserter.expect(SLEEPING);
			}

			@Override
			public void awake(IJobChangeEvent event) {
				asserter.expect(AWAKE);
			}

			@Override
			public void aboutToRun(IJobChangeEvent event) {
				switch (aboutToRunCount.incrementAndGet()) {
				case 1:
					asserter.expect(ABOUTTORUN1);
					job.sleep();
					job.wakeUp();
					break;
				case 2:
					asserter.expect(ABOUTTORUN2);
					break;
				}
			}

			@Override
			public void running(IJobChangeEvent event) {
				asserter.expect(RUNNING);
			}

			@Override
			public void done(IJobChangeEvent event) {
				asserter.expect(DONE);
			}
		};
		job.addJobChangeListener(jobListener);
		try {
			job.schedule();
			try {
				job.join();
			} catch (InterruptedException e) {
				asserter.addError(e);
			}
			asserter.expect(RETURN_FROM_JOIN);
			asserter.assertNoErrors();
		} finally {
			job.removeJobChangeListener(jobListener);
		}
	}

	@Test
	public void testCancelOrderOfEvents() {
		OrderAsserter asserter = new OrderAsserter();
		Event SCHEDULED = asserter.getNext("IJobChangeEvent.scheduled()");
		Event ABOUTTORUN = asserter.getNext("IJobChangeEvent.aboutToRun()");
		Event SLEEPING = asserter.never("IJobChangeEvent.sleeping()");
		Event AWAKE = asserter.never("IJobChangeEvent.awake()");
		Event RUNNING = asserter.never("IJobChangeEvent.running()");
		Event RUN = asserter.never("Job.run()");
		Event DONE = asserter.getNext("IJobChangeEvent.done()");
		// race condition DONE and RETURN_FROM_JOIN
		// Event RETURN_FROM_JOIN = asserter.getNext("RETURN FROM Job.join()");

		Job job = new Job("testCancelOrderOfEvents") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				asserter.expect(RUN);
				return Status.OK_STATUS;
			}

		};

		IJobChangeListener jobListener = new IJobChangeListener() {
			@Override
			public void scheduled(IJobChangeEvent event) {
				asserter.expect(SCHEDULED);
			}

			@Override
			public void sleeping(IJobChangeEvent event) {
				asserter.expect(SLEEPING);
			}

			@Override
			public void awake(IJobChangeEvent event) {
				asserter.expect(AWAKE);
			}

			@Override
			public void aboutToRun(IJobChangeEvent event) {
				asserter.expect(ABOUTTORUN);
				job.cancel();
				job.wakeUp();
			}

			@Override
			public void running(IJobChangeEvent event) {
				asserter.expect(RUNNING);
			}

			@Override
			public void done(IJobChangeEvent event) {
				asserter.expect(DONE);
			}
		};
		job.addJobChangeListener(jobListener);
		try {
			job.schedule();
			try {
				job.join();
			} catch (InterruptedException e) {
				asserter.addError(e);
			}
			// asserter.expect(RETURN_FROM_JOIN);
			asserter.assertNoErrors();
		} finally {
			job.removeJobChangeListener(jobListener);
		}
	}

	@Test
	public void testNoTimeoutOccured() throws Exception {
		AbstractJobTest.assertNoTimeoutOccured();
	}

	@Test
	public void testDeadlockRecovery() throws Exception {
		Object commonLockObject = new Object();
		TestBarrier2 waitingForJobListenerBarrier = new TestBarrier2();
		TestBarrier2 waitingForBlockBarrier = new TestBarrier2();

		Job jobWithListener = new Job("Job-Listener-Triggering Job") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				return Status.OK_STATUS;
			}
		};
		IJobChangeListener jobListener = new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				waitingForJobListenerBarrier.setStatus(TestBarrier2.STATUS_RUNNING);
				// wait for threadDeadlockingWithJobListener having lock
				waitingForBlockBarrier.waitForStatus(TestBarrier2.STATUS_BLOCKED);
				synchronized (commonLockObject) {
					// can not enter/proceed while lock is hold by threadDeadlockingWithJobListener
				}
			}
		};
		jobWithListener.addJobChangeListener(jobListener);
		Thread threadDeadlockingWithJobListener = new Thread("Deadlocking Thread") {
			@Override
			public void run() {
				// Ensure job listener is executing
				waitingForJobListenerBarrier.waitForStatus(TestBarrier2.STATUS_RUNNING);
				synchronized (commonLockObject) {
					// Try to schedule job with acquired lock while listener is still being
					// processed and demands lock to finish --> deadlock
					waitingForBlockBarrier.setStatus(TestBarrier2.STATUS_BLOCKED);
					jobWithListener.schedule(); // NOK - deadlock
				}
				// jobWithListener.schedule(); // would be OK
			}
		};

		final int DEADLOCK_TIMEOUT = 250;
		final int ABORT_TEST_TIMEOUT = 60_000;
		try {
			testNoTimeoutOccured(); // before changing timeout
			JobListeners.setJobListenerTimeout(DEADLOCK_TIMEOUT);
			threadDeadlockingWithJobListener.start();
			jobWithListener.schedule();

			waitingForBlockBarrier.waitForStatus(TestBarrier2.STATUS_BLOCKED);
			threadDeadlockingWithJobListener.join(ABORT_TEST_TIMEOUT);
			assertEquals(0, JobListeners.getJobListenerTimeout());
		} finally {
			JobListeners.resetJobListenerTimeout();
			jobWithListener.removeJobChangeListener(jobListener);
		}
	}
}
