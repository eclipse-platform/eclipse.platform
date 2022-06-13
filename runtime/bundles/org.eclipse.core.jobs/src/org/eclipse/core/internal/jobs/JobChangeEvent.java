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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;

public class JobChangeEvent implements IJobChangeEvent {
	/**
	 * The job on which this event occurred.
	 */
	Job job = null;
	/**
	 * The result returned by the job's run method, or <code>null</code> if
	 * not applicable.
	 */
	IStatus result = null;
	/**
	 * The result returned by the job's job group, if this event signals
	 * completion of the last job in a group, or <code>null</code> if not
	 * applicable.
	 */
	IStatus jobGroupResult = null;
	/**
	 * The amount of time to wait after scheduling the job before it should be run,
	 * or <code>-1</code> if not applicable for this type of event.
	 */
	long delay = -1;
	/**
	 * Whether this job is being immediately rescheduled.
	 */
	boolean reschedule = false;

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
