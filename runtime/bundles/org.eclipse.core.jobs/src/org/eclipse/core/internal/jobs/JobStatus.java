/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
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
package org.eclipse.core.internal.jobs;

import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobStatus;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Standard implementation of the IJobStatus interface.
 */
public class JobStatus extends Status implements IJobStatus {
	private Job job;

	/**
	 * Creates a new job status with no interesting error code or exception.
	 * @param severity
	 * @param job
	 * @param message
	 */
	public JobStatus(int severity, Job job, String message) {
		super(severity, JobManager.PI_JOBS, 1, message, null);
		this.job = job;
	}

	@Override
	public Job getJob() {
		return job;
	}
}
