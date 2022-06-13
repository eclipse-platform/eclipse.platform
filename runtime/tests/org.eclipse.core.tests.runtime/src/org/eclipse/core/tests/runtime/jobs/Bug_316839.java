/*******************************************************************************
 * Copyright (c) 2010, 2015 Oracle Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Oracle Corporation - initial API and implementation
 *     IBM - conversion to JUnit test with assertions
 *******************************************************************************/
package org.eclipse.core.tests.runtime.jobs;

import org.eclipse.core.internal.jobs.JobManager;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.core.tests.harness.TestBarrier2;

/**
 * Regression test for bug 316839.
 */
public class Bug_316839 extends AbstractJobManagerTest {

	ILock LOCK = Job.getJobManager().newLock();

	TestBarrier2 barrier = new TestBarrier2(TestBarrier2.STATUS_WAIT_FOR_START);

	YieldingTestJob yieldingJob;
	TestJob interruptingJob;
	boolean lockGraphWasEmpty = true;

	public void testBug() {
		// Schedule jobs
		yieldingJob = new YieldingTestJob("job with project rule "); //$NON-NLS-1$
		yieldingJob.setTestRule(new PathRule("/pX")); //$NON-NLS-1$
		yieldingJob.schedule();

		interruptingJob = new TestJob("job with root rule"); //$NON-NLS-1$		job.setRule(new Root());
		interruptingJob.setRule(new PathRule("/"));
		interruptingJob.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				//at the moment we are done, the lock graph should be empty
				lockGraphWasEmpty = ((JobManager) Job.getJobManager()).getLockManager().isEmpty();
			}
		});
		barrier.waitForStatus(TestBarrier2.STATUS_RUNNING);
		interruptingJob.schedule();
		//let the yielding job perform its yield
		barrier.setStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);

		// wait for job to complete or for max time...
		waitForCompletion(yieldingJob);
		waitForCompletion(interruptingJob);
	}

	public static class TestJob extends Job {
		public TestJob(String name) {
			super(name);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			return Status.OK_STATUS;
		}
	}

	// This Job manages calls to begin/endRule()
	public class YieldingTestJob extends Job {
		private ISchedulingRule rule;

		public YieldingTestJob(String name) {
			super(name);
		}

		public void setTestRule(ISchedulingRule rule) {
			this.rule = rule;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			getJobManager().beginRule(rule, monitor);
			try {
				barrier.setStatus(TestBarrier2.STATUS_RUNNING);
				barrier.waitForStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);
				// Call to some dependent code that causes a yieldRule().
				// For example, the various routines of ModelManagerImpl
				// that get and return a shared model. If another thread /
				// Job is initializing the model, then the routine
				// ModelManagerImpl$SharedObject.waitForLoadAttempt()
				// calls Job.yieldRule().
				// This test will simulate the scenario by just calling
				// Job.yieldRule() in the same way.
				doYieldRule();
				return Status.OK_STATUS;
			} finally {
				getJobManager().endRule(rule);
			}
		}

		private void doYieldRule() {
			final Job current = Job.getJobManager().currentJob();
			if (current != null) {
				current.yieldRule(null);
			}
		}
	}
}
