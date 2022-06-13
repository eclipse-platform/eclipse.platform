/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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

import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.tests.harness.TestBarrier2;
import org.eclipse.core.tests.harness.TestJob;

/**
 * Regression test for bug 129551.  A job changes to the ABOUT_TO_RUN
 * state, and then another job tries to run and is queued behind it. Before
 * the job starts, it is put to sleep.  When the bug existed, putting the job
 * to sleep would cause any blocked jobs behind the job to be lost.
 */
public class Bug_129551 extends AbstractJobManagerTest {
	final boolean[] shouldSleep = new boolean[] {true};
	TestBarrier2 barrier = new TestBarrier2();
	RuntimeException[] failure = new RuntimeException[1];

	class BugJob extends TestJob {
		public BugJob() {
			super("Bug_129551", 1, 1);
		}

		@Override
		public boolean shouldRun() {
			//skip everything if the test is already done
			if (!shouldSleep[0]) {
				return true;
			}
			barrier.setStatus(TestBarrier2.STATUS_RUNNING);
			//wait for blocking jobs to queue up
			barrier.waitForStatus(TestBarrier2.STATUS_START);
			//put the job to sleep
			try {
				this.sleep();
			} catch (RuntimeException e) {
				failure[0] = e;
			}
			barrier.setStatus(TestBarrier2.STATUS_DONE);
			return true;
		}
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		//don't use fussy progress monitor, because in this case we kill
		// a job before it has started running
		manager.setProgressProvider(null);
	}

	public void testBug() {
		ISchedulingRule rule = new IdentityRule();
		BugJob job = new BugJob();
		job.setRule(rule);
		TestJob other = new TestJob("bug_129551_other", 1, 1);
		other.setRule(rule);
		job.schedule();
		other.schedule();
		//wait until the first job is about to run
		barrier.waitForStatus(TestBarrier2.STATUS_RUNNING);
		//wait to ensure the other job is blocked
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			fail("4.99", e);
		}
		//let the first job go
		barrier.setStatus(TestBarrier2.STATUS_START);
		barrier.waitForStatus(TestBarrier2.STATUS_DONE);

		//check for failure
		if (failure[0] != null) {
			fail(failure[0].getMessage());
		}
		//tell the job not to sleep this time around
		shouldSleep[0] = false;
		job.wakeUp();
		waitForCompletion(job);
		waitForCompletion(other);
	}
}
