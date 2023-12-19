/*******************************************************************************
 * Copyright (c) 2003, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.runtime.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.tests.harness.TestBarrier2;

/**
 * A runnable class that executes the given job and calls done when it is finished
 */
public class AsynchExecThread extends Thread {
	private final IProgressMonitor current;
	private final Job job;
	private final String jobName;
	private final TestBarrier2 barrier;

	public AsynchExecThread(IProgressMonitor current, Job job, String jobName, TestBarrier2 testBarrier) {
		this.current = current;
		this.job = job;
		this.jobName = jobName;
		this.barrier = testBarrier;
	}

	@Override
	public void run() {
		//wait until the main testing method allows this thread to run
		barrier.waitForStatus(TestBarrier2.STATUS_WAIT_FOR_RUN);

		//set the current thread as the execution thread
		job.setThread(Thread.currentThread());

		barrier.upgradeTo(TestBarrier2.STATUS_RUNNING);

		//wait until this job is allowed to run by the tester
		barrier.waitForStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);

		//must have positive work
		current.beginTask(jobName, 1);
		if (current.isCanceled()) {
			job.done(Status.CANCEL_STATUS);
		} else {
			job.done(Status.OK_STATUS);
		}
		barrier.upgradeTo(TestBarrier2.STATUS_DONE);
		current.done();
	}

}
