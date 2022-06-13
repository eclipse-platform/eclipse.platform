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

import java.util.concurrent.atomic.AtomicIntegerArray;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.tests.harness.TestBarrier2;

/**
 * A runnable class that executes the given job and calls done when it is finished
 */
public class AsynchExecThread extends Thread {
	private IProgressMonitor current;
	private Job job;
	private int ticks;
	private int tickLength;
	private String jobName;
	private AtomicIntegerArray status;
	private int index;

	public AsynchExecThread(IProgressMonitor current, Job job, int ticks, int tickLength, String jobName,
			AtomicIntegerArray status, int index) {
		this.current = current;
		this.job = job;
		this.ticks = ticks;
		this.tickLength = tickLength;
		this.jobName = jobName;
		this.status = status;
		this.index = index;
	}

	@Override
	public void run() {
		//wait until the main testing method allows this thread to run
		TestBarrier2.waitForStatus(status, index, TestBarrier2.STATUS_WAIT_FOR_RUN);

		//set the current thread as the execution thread
		job.setThread(Thread.currentThread());

		status.set(index, TestBarrier2.STATUS_RUNNING);

		//wait until this job is allowed to run by the tester
		TestBarrier2.waitForStatus(status, index, TestBarrier2.STATUS_WAIT_FOR_DONE);

		//must have positive work
		current.beginTask(jobName, ticks <= 0 ? 1 : ticks);
		try {

			for (int i = 0; i < ticks; i++) {
				current.subTask("Tick: " + i);
				if (current.isCanceled()) {
					status.set(index, TestBarrier2.STATUS_DONE);
					job.done(Status.CANCEL_STATUS);
				}
				try {
					//Thread.yield();
					Thread.sleep(tickLength);
				} catch (InterruptedException e) {
				}
				current.worked(1);
			}
			if (ticks <= 0) {
				current.worked(1);
			}
		} finally {
			status.set(index, TestBarrier2.STATUS_DONE);
			current.done();
			job.done(Status.OK_STATUS);
		}
	}

}
