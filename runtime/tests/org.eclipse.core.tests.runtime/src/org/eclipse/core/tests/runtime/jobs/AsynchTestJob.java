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

import java.util.concurrent.atomic.AtomicIntegerArray;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.tests.harness.TestBarrier2;

/**
 * A job that executes asynchronously on a separate thread
 */
class AsynchTestJob extends Job {
	private AtomicIntegerArray status;
	private int index;

	public AsynchTestJob(String name, AtomicIntegerArray status, int index) {
		super(name);
		this.status = status;
		this.index = index;
	}

	@Override
	public IStatus run(IProgressMonitor monitor) {
		status.set(index, TestBarrier2.STATUS_RUNNING);
		AsynchExecThread t = new AsynchExecThread(monitor, this, 100, 10, getName(), status, index);
		TestBarrier2.waitForStatus(status, index, TestBarrier2.STATUS_START);
		t.start();
		status.set(index, TestBarrier2.STATUS_WAIT_FOR_START);
		return Job.ASYNC_FINISH;
	}

}
