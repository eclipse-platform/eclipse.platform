/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.runtime.jobs;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * Test for bug 574883
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class Bug_574883 extends AbstractJobManagerTest {

	static class SerialExecutor extends Job {

		private final Queue<Runnable> queue;
		private final Object myFamily;

		/**
		 * @param jobName descriptive job name
		 * @param family  non null object to control this job execution
		 **/
		public SerialExecutor(String jobName, Object family) {
			super(jobName);
			Assert.isNotNull(family);
			this.myFamily = family;
			this.queue = new ConcurrentLinkedQueue<>();
			setSystem(true);
		}

		@Override
		public boolean belongsTo(Object family) {
			return myFamily == family;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Runnable action = queue.poll();
			try {
				if (action != null && !monitor.isCanceled()) {
					action.run();
				}
			} finally {
				if (!queue.isEmpty() && !monitor.isCanceled()) {
					// this call confuses JobManager and causes bug 574883 if the action above
					// runs *too fast*
					schedule();
				}
			}
			return Status.OK_STATUS;
		}

		/**
		 * Enqueue an action asynchronously.
		 */
		public void schedule(Runnable action) {
			queue.add(action);
			schedule();
		}
	}

	final int RUNS = 100_000;
	final int processors = Runtime.getRuntime().availableProcessors();

	@Test
	public void testReschedulingLambda() throws InterruptedException {
		// Executor has to execute every task. Even when they are scheduled fast
		// and execute fast
		SerialExecutor serialExecutor = new SerialExecutor("test", this);
		AtomicInteger executions = new AtomicInteger();
		for (int i = 0; i < RUNS; i++) {
			serialExecutor.schedule(() -> executions.incrementAndGet());
		}
		Job.getJobManager().join(this, null);
		Job[] jobs = Job.getJobManager().find(this);
		int length = jobs.length;
		int firstState = executions.get();
		try {
			if (length > 0) {
				// Check if that still would work?
				Job.getJobManager().join(this, null);
				if (Job.getJobManager().find(this).length > 0) {
					fail("Job still running after second join, executed before: " + firstState + ", executed now: "
							+ executions.get() + ", cpu: " + processors);
				}
			}
			assertEquals("Job still running after first join, executed: " + firstState + ", cpu: " + processors, 0,
					length);
			assertEquals(RUNS, executions.get());
		} catch (Throwable t) {
			Job.getJobManager().cancel(this);
			Thread.sleep(1000);
			Job.getJobManager().join(this, null);
			throw t;
		}
	}

	/**
	 * same as testReschedulingLambda but repeated with more joins to be more likely
	 * to fail
	 */
	@Test
	public void testJoinLambdaOften() throws InterruptedException {
		for (int l = 0; l < RUNS; l++) {
			// Executor has to execute every task. Even when they are scheduled fast
			// and execute fast
			SerialExecutor serialExecutor = new SerialExecutor("test", this);
			AtomicInteger executions = new AtomicInteger();
			int INNER_RUNS = 10;
			for (int i = 0; i < INNER_RUNS; i++) {
				serialExecutor.schedule(() -> executions.incrementAndGet());
			}
			Job.getJobManager().join(this, null);
			Job[] jobs = Job.getJobManager().find(this);
			int length = jobs.length;
			int firstState = executions.get();
			try {
				if (executions.get() != INNER_RUNS) {
					System.out.println("error");
				}
				assertEquals("after " + l + " tries: Job still running after join, executed: " + firstState
						+ ", cpu: " + processors, 0,
						length);
				assertEquals("after " + l + " tries", INNER_RUNS, executions.get());
			} catch (Throwable t) {
				Job.getJobManager().cancel(this);
				Thread.sleep(1000);
				Job.getJobManager().join(this, null);
				throw t;
			}
		}
	}

	@Test
	public void testReschedulingMethodRef() throws InterruptedException {
		// Executor has to execute every task. Even when they are scheduled fast
		// and execute fast
		SerialExecutor serialExecutor = new SerialExecutor("test", this);
		AtomicInteger executions = new AtomicInteger();
		for (int i = 0; i < RUNS; i++) {
			serialExecutor.schedule(executions::incrementAndGet);
		}
		Job.getJobManager().join(this, null);
		Job[] jobs = Job.getJobManager().find(this);
		int length = jobs.length;
		int firstState = executions.get();
		try {
			if (length > 0) {
				// Check if that still would work?
				Job.getJobManager().join(this, null);
				if (Job.getJobManager().find(this).length > 0) {
					fail("Job still running after second join, executed before: " + firstState + ", executed now: "
							+ executions.get() + ", cpu: " + processors);
				}
			}
			assertEquals("Job still running after first join, executed: " + firstState + ", cpu: " + processors, 0,
					length);
			assertEquals(RUNS, executions.get());
		} catch (Throwable t) {
			Job.getJobManager().cancel(this);
			Thread.sleep(1000);
			Job.getJobManager().join(this, null);
			throw t;
		}
	}

	/**
	 * This test always passes because it does a bit more work as both tests above
	 * inside run method
	 */
	@Test
	public void testReschedulingSomeMoreWork() throws InterruptedException {
		// Executor has to execute every task. Even when they are scheduled fast
		// and execute fast
		SerialExecutor serialExecutor = new SerialExecutor("test", this);
		AtomicInteger executions = new AtomicInteger();
		AtomicLong garbage = new AtomicLong(42);
		for (int i = 0; i < RUNS; i++) {
			serialExecutor.schedule(() -> {
				executions.incrementAndGet();
				// just consume some more CPU cycles
				garbage.getAndUpdate(x -> (long) (x + Math.sin(x) * 100));
			});
		}
		Job.getJobManager().join(this, null);
		Job[] jobs = Job.getJobManager().find(this);
		int length = jobs.length;
		int firstState = executions.get();
		System.out.println(garbage);
		try {
		if (length > 0) {
				// Check if that still would work?
				Job.getJobManager().join(this, null);
				if (Job.getJobManager().find(this).length > 0) {
					fail("Job still running after second join, executed before: " + firstState + ", executed now: "
							+ executions.get() + ", cpu: " + processors);
				}
			}
			assertEquals("Job still running after first join, executed: " + firstState + ", cpu: " + processors, 0,
					length);
			assertEquals(RUNS, executions.get());
		} catch (Throwable t) {
			Job.getJobManager().cancel(this);
			Thread.sleep(1000);
			Job.getJobManager().join(this, null);
			throw t;
		}
	}

	@Test
	public void testNow() throws Exception {
		AtomicInteger executions = new AtomicInteger();
		for (int i = 0; i < RUNS; i++) {
			long t1 = now();
			((Runnable) () -> executions.incrementAndGet()).run();
			long t2 = now();
			long diff = t2 - t1;
			assertTrue("Time should not go back: " + diff + " at: " + i, diff >= 0);
		}
		assertEquals(RUNS, executions.get());
	}
}
