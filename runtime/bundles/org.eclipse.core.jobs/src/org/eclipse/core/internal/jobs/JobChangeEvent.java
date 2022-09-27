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
 *     Terry Parker - Bug 457504, Publish a job group's final status to IJobChangeListeners
 *******************************************************************************/
package org.eclipse.core.internal.jobs;

import org.eclipse.core.internal.jobs.JobListeners.IListenerDoit;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;

public class JobChangeEvent implements IJobChangeEvent {
	/**
	 * The job on which this event occurred.
	 */
	final Job job;
	/**
	 * The result returned by the job's run method, or <code>null</code> if
	 * not applicable.
	 */
	final IStatus result;
	/**
	 * The result returned by the job's job group, if this event signals
	 * completion of the last job in a group, or <code>null</code> if not
	 * applicable.
	 */
	IStatus jobGroupResult;
	/**
	 * The amount of time to wait after scheduling the job before it should be run,
	 * or <code>-1</code> if not applicable for this type of event.
	 */
	final long delay;
	/**
	 * Whether this job is being immediately rescheduled.
	 */
	final boolean reschedule;

	final IListenerDoit doit;

	JobChangeEvent(IListenerDoit doit, Job job) {
		this.doit = doit;
		this.job = job;

		this.result = null;
		this.jobGroupResult = null;
		this.reschedule = false;
		this.delay = -1;
	}

	JobChangeEvent(IListenerDoit doit, Job job, IStatus result, boolean reschedule) {
		this.doit = doit;
		this.job = job;
		this.result = result;

		this.jobGroupResult = null;
		this.reschedule = reschedule;
		this.delay = -1;
	}

	JobChangeEvent(IListenerDoit doit, Job job, long delay, boolean reschedule) {
		this.doit = doit;
		this.job = job;
		this.delay = delay;

		this.result = null;
		this.jobGroupResult = null;
		this.reschedule = reschedule;
	}

	@Override
	public long getDelay() {
		return delay;
	}

	@Override
	public Job getJob() {
		return job;
	}

	@Override
	public IStatus getResult() {
		return result;
	}

	@Override
	public IStatus getJobGroupResult() {
		return jobGroupResult;
	}
}
