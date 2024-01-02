/*******************************************************************************
 * Copyright (c) 2003, 2015 IBM Corporation and others.
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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.tests.harness.TestBarrier2;

/**
 * A job that executes asynchronously on a separate thread
 */
class AsynchTestJob extends Job {
	public final TestBarrier2 jobBarrier;

	public final TestBarrier2 threadBarrier;

	public AsynchTestJob(String name) {
		super(name);
		this.jobBarrier = new TestBarrier2();
		this.threadBarrier = new TestBarrier2();
	}

	@Override
	public IStatus run(IProgressMonitor monitor) {
		jobBarrier.upgradeTo(TestBarrier2.STATUS_START);
		AsynchExecThread t = new AsynchExecThread(monitor, this, getName(), threadBarrier);
		jobBarrier.waitForStatus(TestBarrier2.STATUS_WAIT_FOR_RUN);
		t.start();
		jobBarrier.upgradeTo(TestBarrier2.STATUS_RUNNING);
		return Job.ASYNC_FINISH;
	}

}
