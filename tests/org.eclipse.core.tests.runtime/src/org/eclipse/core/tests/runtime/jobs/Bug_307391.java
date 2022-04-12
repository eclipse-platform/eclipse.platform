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

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.tests.harness.TestBarrier2;

/**
 * Regression test for bug 307391
 * 'ImplicitJob' style Job doesn't correctly leave YIELDING state
 */
public class Bug_307391 extends AbstractJobManagerTest {

	/**
	 * Threads: main, j, t2
	 * Locks: idSchedRule
	 *
	 * j owns idSchedRule with beginRule, t2 tries to acquire it
	 *
	 * Trips assertion:
	 * java.lang.IllegalArgumentException: Cannot yieldRule job that is YIELDING
	 * 		in JobManager#yieldRule
	 *
	 * @throws Exception
	 */
	public void testYieldWithlockAcquire() throws Exception {
		final IdentityRule idSchedRule = new IdentityRule();

		final TestBarrier2 tb1 = new TestBarrier2(-1);
		final TestBarrier2 tb2 = new TestBarrier2(-1);

		final int YIELD2 = 10002;
		final int ACQUIRE2 = 1005;

		// Job run with a scheduling rule begun inline
		// yields it to the thread
		Job j = new Job("rule-holding, lock-wanting job") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Job.getJobManager().beginRule(idSchedRule, null);

				tb1.setStatus(TestBarrier2.STATUS_WAIT_FOR_START);
				tb1.waitForStatus(TestBarrier2.STATUS_START);

				// Yield our rule
				Job.getJobManager().currentJob().yieldRule(null);

				tb1.waitForStatusNoFail(YIELD2);
				// Yield our rule
				Job.getJobManager().currentJob().yieldRule(null);
				tb1.setStatus(YIELD2);

				// The test executed successfully
				Job.getJobManager().endRule(idSchedRule);
				tb1.setStatus(TestBarrier2.STATUS_DONE);
				return Status.OK_STATUS;
			}
		};

		Thread t2 = new Thread() {
			@Override
			public void run() {
				tb2.setStatus(TestBarrier2.STATUS_WAIT_FOR_START);
				tb2.waitForStatus(TestBarrier2.STATUS_START);

				Job.getJobManager().beginRule(idSchedRule, null);
				Job.getJobManager().endRule(idSchedRule);

				tb2.waitForStatus(ACQUIRE2);
				Job.getJobManager().beginRule(idSchedRule, null);
				Job.getJobManager().endRule(idSchedRule);

				// that all went well, signal we've passed
				tb2.setStatus(TestBarrier2.STATUS_DONE);
			}
		};

		// Start the threads
		j.schedule();
		t2.start();

		// wait for the threads to get to start and acquire sched rules
		tb1.waitForStatus(TestBarrier2.STATUS_WAIT_FOR_START);
		tb2.waitForStatus(TestBarrier2.STATUS_WAIT_FOR_START);

		// first acquire and yield
		tb2.setStatus(TestBarrier2.STATUS_START);
		Thread.sleep(1000);
		tb1.setStatus(TestBarrier2.STATUS_START);

		// second acquire and yield
		tb2.setStatus(ACQUIRE2);
		Thread.sleep(1000);
		tb1.setStatus(YIELD2);

		// Wait for them to finish
		tb1.waitForStatus(TestBarrier2.STATUS_DONE);
		tb2.waitForStatus(TestBarrier2.STATUS_DONE);
	}
}