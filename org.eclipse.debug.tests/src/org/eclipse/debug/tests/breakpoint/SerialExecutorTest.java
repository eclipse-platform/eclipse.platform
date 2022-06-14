/*******************************************************************************
 *  Copyright (c) 2021, 2022 Joerg Kubitz and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Joerg Kubitz - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.tests.breakpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.internal.ui.model.elements.SerialExecutor;
import org.eclipse.debug.tests.AbstractDebugTest;
import org.junit.Ignore;
import org.junit.Test;

@SuppressWarnings("restriction")
public class SerialExecutorTest extends AbstractDebugTest {
	@Override
	public void tearDown() throws Exception {
		Job.getJobManager().cancel(this);
		super.tearDown();
	}

	@Test
	public void testSimpleExecution() throws InterruptedException {
		SerialExecutor serialExecutor = new SerialExecutor("test", this);
		AtomicInteger executions = new AtomicInteger(0);
		serialExecutor.schedule(executions::incrementAndGet);
		Job.getJobManager().join(this, null);
		assertEquals(1, executions.get());
		serialExecutor.schedule(executions::incrementAndGet);
		Job.getJobManager().join(this, null);
		assertEquals(2, executions.get());
		serialExecutor.schedule(executions::incrementAndGet);
		serialExecutor.schedule(executions::incrementAndGet);
		Job.getJobManager().join(this, null);
		assertEquals(4, executions.get());
	}

	@Test
	public void testSerialExecution() throws InterruptedException {
		SerialExecutor serialExecutor = new SerialExecutor("test", this);
		AtomicInteger executions = new AtomicInteger(0);
		AtomicInteger parallelExecutions = new AtomicInteger(0);
		final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
		int RUNS = 20;
		int WAIT_MILLIS = 2;
		long start = System.nanoTime();
		for (int i = 0; i < RUNS; i++) {
			serialExecutor.schedule(() -> {
				WriteLock writeLock = rwl.writeLock();
				if (writeLock.tryLock()) {
					try {
						Thread.sleep(WAIT_MILLIS);
						executions.incrementAndGet();
					} catch (InterruptedException e) {
						// interrupt should not happen -> fail test
						parallelExecutions.incrementAndGet();
					}
					writeLock.unlock();
				} else {
					// another thread already holding the lock
					// should not happen -> fail test
					parallelExecutions.incrementAndGet();
				}
				Job[] jobs = Job.getJobManager().find(SerialExecutorTest.this);
				if (jobs.length != 1) {
					parallelExecutions.incrementAndGet();
				}
			});
		}
		Job.getJobManager().join(this, null);
		Job[] jobs = Job.getJobManager().find(this);
		assertEquals(0, jobs.length);
		long stop = System.nanoTime();
		long millis = (stop - start) / 1000_000;
		assertEquals(RUNS, executions.get());
		assertEquals(0, parallelExecutions.get());
		long minimalMillis = RUNS * WAIT_MILLIS;
		assertTrue("Test did finish too fast (" + millis + " ms)", millis >= minimalMillis);
	}

	@Test
	public void testSchedulingQueue() throws InterruptedException {
		// Executor has to execute every task. Even when they are faster
		// scheduled then executed
		SerialExecutor serialExecutor = new SerialExecutor("test", this);
		AtomicInteger executions = new AtomicInteger();
		int RUNS = 20;
		int WAIT_MILLIS = 2;
		for (int i = 0; i < RUNS; i++) {
			serialExecutor.schedule(() -> {
				try {
					Thread.sleep(WAIT_MILLIS);
					executions.incrementAndGet();
				} catch (InterruptedException e) {
					// error
				}
			});
		}
		Job.getJobManager().join(this, null);
		Job[] jobs = Job.getJobManager().find(this);
		assertEquals(0, jobs.length);
		assertEquals(RUNS, executions.get());
	}

	@Test
	@Ignore("See https://bugs.eclipse.org/bugs/show_bug.cgi?id=574883")
	public void testHeavyScheduling() throws InterruptedException {
		// Executor has to execute every task. Even when they are scheduled fast
		// and execute fast
		SerialExecutor serialExecutor = new SerialExecutor("test", this);
		AtomicInteger executions = new AtomicInteger();
		int RUNS = 200;
		for (int i = 0; i < RUNS; i++) {
			serialExecutor.schedule(executions::incrementAndGet);
		}
		Job.getJobManager().join(this, null);
		Job[] jobs = Job.getJobManager().find(this);
		assertEquals(0, jobs.length);
		assertEquals(RUNS, executions.get());
	}

	@Test
	public void testJoin() throws InterruptedException {
		// The last scheduled job has to be done before join() returns
		for (int run = 0; run < 100; run++) {
			SerialExecutor serialExecutor = new SerialExecutor("test", this);
			AtomicInteger executions = new AtomicInteger();
			int RUNS = 20;
			int WAIT_MILLIS = 1;
			for (int i = 0; i < RUNS; i++) {
				serialExecutor.schedule(() -> {
					try {
						Thread.sleep(WAIT_MILLIS);
						executions.incrementAndGet();
					} catch (InterruptedException e) {
						// error
					}
				});
			}
			Job.getJobManager().join(this, null);
			Job[] jobs = Job.getJobManager().find(this);
			assertEquals(0, jobs.length);
			assertEquals("failed on run " + run, RUNS, executions.get());
			// does fail on run ~ 40 if the final job.join() is removed.
		}
	}

}