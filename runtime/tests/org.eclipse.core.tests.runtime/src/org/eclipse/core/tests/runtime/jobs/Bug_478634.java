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

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.junit.Test;

/**
 *
 */
public class Bug_478634 extends AbstractJobTest {

	PathRule rootRule = new PathRule("/");
	PathRule projectRule = new PathRule("/a");
	IJobManager jobManager = Job.getJobManager();
	ProjectJob projectJob = new ProjectJob();

	@Test
	public void testWaitingThreadJob() {
		projectJob.schedule();
		waitForCompletion(projectJob);
		ShouldNotBeBlockedJob j = new ShouldNotBeBlockedJob();
		j.setRule(rootRule);
		j.schedule();
		waitForCompletion(j);
		assertFalse("Job was blocked", j.wasBlocked());

	}

	class ShouldNotBeBlockedJob extends Job {

		private boolean blocked;

		public ShouldNotBeBlockedJob() {
			super("ShouldNotBeBlockedJob");
		}

		public boolean wasBlocked() {
			return blocked;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			blocked = isBlocking();
			return Status.OK_STATUS;
		}

	}

	class RootJob extends Job {
		private ThreadJobListener listener;

		public RootJob(ThreadJobListener listener) {
			super("RootJob");
			this.listener = listener;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			jobManager.beginRule(rootRule, new NullProgressMonitor());
			listener.notifyBeginRule();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
			jobManager.transferRule(rootRule, projectJob.getThread());
			return Status.OK_STATUS;
		}

	}

	class ProjectJob extends Job {

		public ProjectJob() {
			super("ProjectJob");
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			ThreadJobListener tListener = new ThreadJobListener();
			RootJob rootJob = new RootJob(tListener);
			rootJob.schedule();
			while (!tListener.isBeginRule()) {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
				}
			}
			jobManager.beginRule(projectRule, new NullProgressMonitor());
			jobManager.endRule(projectRule);
			jobManager.endRule(rootRule);
			return Status.OK_STATUS;

		}

	}

	static class ThreadJobListener extends JobChangeAdapter {

		private boolean beginRule = false;

		public void notifyBeginRule() {
			beginRule = true;
		}

		public boolean isBeginRule() {
			return beginRule;
		}

	}

}
