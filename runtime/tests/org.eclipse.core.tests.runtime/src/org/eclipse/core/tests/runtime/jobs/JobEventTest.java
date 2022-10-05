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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.core.tests.harness.TestBarrier2;
import org.eclipse.core.tests.runtime.jobs.OrderAsserter.Event;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test for bug https://github.com/eclipse-platform/eclipse.platform/issues/193
 */
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
		Event SCHEDULED2 = asserter.getNext("Second IJobChangeEvent.scheduled()");
		Event DONE1 = asserter.getNext("First IJobChangeEvent.done()");
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
	@Ignore("https://github.com/eclipse-platform/eclipse.platform/issues/193")
	public void testDeadlock() throws Exception {
		Object deadlock = new Object();
		AtomicBoolean stopped = new AtomicBoolean();
		CountDownLatch testDoneSignal1 = new CountDownLatch(1);
		CountDownLatch testDoneSignal2 = new CountDownLatch(1);
		Collection<String> errors = new ConcurrentLinkedQueue<>();

		Job job = new Job("testDeadlock") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				return Status.OK_STATUS;
			}

		};
		IJobChangeListener jobListener = new JobChangeAdapter() {

			@Override
			public void done(IJobChangeEvent event) {
				testDoneSignal1.countDown();
				try {
					testDoneSignal2.await(5, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
				}
				synchronized (deadlock) {
					// wait till done() is called and won't progress
					while (!stopped.get()) {
						// do not progress until timeout
					}
				}
			}
		};

		job.addJobChangeListener(jobListener);
		try {
			job.schedule();
			job.wakeUp();
			Timer timeout = new Timer();
			timeout.schedule(new TimerTask() {

				@Override
				public void run() {
					System.out.println(TestBarrier2.getThreadDump());
					errors.add("timeout (probably deadlock)");
					stopped.set(true);
				}
			}, 3000);
			// wait till synchronized (deadlock) in other test thread
			try {
				testDoneSignal1.await(5, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
			}
			synchronized (deadlock) {
				testDoneSignal2.countDown();
				Thread thread = new Thread("deadlock") {
					@Override
					public void run() {
						job.schedule(); // will not progress if notification
					}
				};
				// waiting for JobChangeAdapter.done
				thread.start();
				thread.join(5000);
			}
			errors.forEach(e -> {
				throw new AssertionError(e);
			});
		} finally {
			job.removeJobChangeListener(jobListener);
			stopped.set(true);
		}
	}
}
