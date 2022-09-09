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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * Test for bug https://github.com/eclipse-platform/eclipse.platform/issues/160
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class Bug_574884Schedule extends AbstractJobManagerTest {

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
			setPriority(Job.INTERACTIVE);
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
					schedule(); // this sometimes does not work?
				}
			}
			return Status.OK_STATUS;
		}

		/**
		 * Enqueue an action asynchronously.
		 */
		public void schedule(Runnable action) {
			queue.offer(action);
			schedule(); // or this sometimes does not work?
		}
	}

	final int RUNS = 1_000_000;


	/**
	 * starts many jobs that should run three times but sometimes only run exactly
	 * once (i have never seen running twice)
	 */
	@Test
	public void testJoinLambdaQuick() throws InterruptedException {
		String firstMessage = null;
		int fails = 0;
		for (int l = 0; l < RUNS; l++) {
			// Executor has to execute every task. Even when they are scheduled fast
			// and execute fast
			SerialExecutor serialExecutor = new SerialExecutor("test", this);
			AtomicInteger executions = new AtomicInteger();
			int INNER_RUNS = 3;
			for (int i = 0; i < INNER_RUNS; i++) {
				serialExecutor.schedule(() -> executions.incrementAndGet());
			}
			Job.getJobManager().join(this, null);
			int executionsAfterJoin = executions.get();
			String message = "after " + l + " tries: executionsAfterJoin: " + executionsAfterJoin + "/" + INNER_RUNS;
			if (executionsAfterJoin != INNER_RUNS) {
				System.out.println(message);
				Thread.sleep(1000); // wait till the Job did finish (no way such a simple job can still be running)
				int executionsCured = executions.get();
				if (executionsCured != INNER_RUNS) {
					System.out.println("but did finish"); // would be a join() bug
				} else {
					fails++;
					if (firstMessage == null) {
						firstMessage = message;
					}
				}
			}
		}
		assertEquals("Job was not (re)scheduled " + fails + "/" + RUNS + " times. example: " + firstMessage, 0, fails);
	}
}
