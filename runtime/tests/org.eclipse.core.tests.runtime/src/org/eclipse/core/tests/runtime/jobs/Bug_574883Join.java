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

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicInteger;
import junit.framework.TestCase;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * Test for bug https://github.com/eclipse-platform/eclipse.platform/issues/160
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class Bug_574883Join extends TestCase {

	static class SerialExecutor extends Job {

		private final Object myFamily;
		final Runnable action;
		AtomicInteger executions = new AtomicInteger();

		/**
		 * @param jobName descriptive job name
		 * @param family  non null object to control this job execution
		 **/
		public SerialExecutor(String jobName, Object family, Runnable action) {
			super(jobName);
			Assert.isNotNull(family);
			this.myFamily = family;
			setSystem(true);
			setPriority(Job.INTERACTIVE);
			this.action = action;
		}

		@Override
		public boolean belongsTo(Object family) {
			return myFamily == family;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			action.run();
			executions.incrementAndGet();
			return Status.OK_STATUS;
		}

		@Override
		public String toString() {
			return " executions=" + executions.get() + " run " + run;
		}

	}

	final int RUNS = 100_000;
	static volatile int run;

	/**
	 * starts many jobs that should run three times but sometimes only run exactly
	 * once (with reschedules=0 or 1) and rarely twice (reschedules=1)
	 */
	@Test
	public void testJoinLambdaQuick() throws InterruptedException {
		String firstMessage = null;
		int joinFails = 0;
		int scheduleFails = 0;
		for (run = 0; run < RUNS; run++) {
			AtomicInteger executions = new AtomicInteger();
			int EXPECTED_RUNS = 2;
			Reference<SerialExecutor> jobReference = createAndRunJobTwice(executions);

			Job.getJobManager().join(this, null);
			int executionsAfterJoin = executions.get();
			if (executionsAfterJoin != EXPECTED_RUNS) {
				int waits = 0;
				while (executions.get() != EXPECTED_RUNS) {
					waits++;
					if (waits < 10) {
						Thread.yield(); // give the job a chance to finish
					} else {
						System.gc();
						System.runFinalization(); // indirectly wait till job finished
						SerialExecutor job = jobReference.get();
						if (job == null) {
							// when job == null it can not be running anymore
							break;
						}
					}
				}
				int executionsCured = executions.get();
				String message = "after " + run + " tries: executionsAfterJoin: " + executionsAfterJoin + "/"
						+ EXPECTED_RUNS;
				if (executionsCured == EXPECTED_RUNS) {
					// join() bug
					if (joinFails == 0) {
						System.out.println(message + " but did finish with " + executionsCured);
						firstMessage = message;
					}
					joinFails++;
					// assertEquals("Job was not joined " + message, 0, joinFails);
				} else {
					// schedule bug
					if (scheduleFails == 0) {
						System.out.println(message);
						firstMessage = message;
					}
					scheduleFails++;
					// assertEquals("Job was not (re)scheduled " + message, 0, scheduleFails);
				}
			}
		}
		assertEquals("Job was not (re)scheduled " + scheduleFails + "/" + run + " times. example: " + firstMessage, 0,
				scheduleFails);
		assertEquals("Job was not joined " + joinFails + "/" + run + " times. example: " + firstMessage, 0, joinFails);
	}

	/** do not inline - so that SerialExecutor can be garbage collected **/
	private Reference<SerialExecutor> createAndRunJobTwice(AtomicInteger executions) {
		SerialExecutor serialExecutor = new SerialExecutor("test", this, () -> executions.incrementAndGet());
		serialExecutor.schedule();
		while (executions.get() == 0) {
			Thread.onSpinWait();
		}
		// according to contract the schedule() should run the Job at least once more.
		serialExecutor.schedule(); // this sometimes does not work when job is already scheduled (i.e may be
		// waiting/running)
		return new WeakReference<>(serialExecutor);
	}

}
