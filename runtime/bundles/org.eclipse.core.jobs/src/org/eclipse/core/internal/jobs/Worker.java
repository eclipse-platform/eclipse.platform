/*******************************************************************************
 *  Copyright (c) 2003, 2014 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Stefan Winkler - Bug 223492: Set current job name as thread name
 *******************************************************************************/
package org.eclipse.core.internal.jobs;

import org.eclipse.core.internal.runtime.RuntimeLog;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;

/**
 * A worker thread processes jobs supplied to it by the worker pool.  When
 * the worker pool gives it a null job, the worker dies.
 */
public class Worker extends Thread {
	// worker number used for debugging purposes only
	private static int nextWorkerNumber = 0;
	private volatile InternalJob currentJob;
	private final WorkerPool pool;
	private final String generalName;

	public Worker(WorkerPool pool) {
		super("Worker-" + nextWorkerNumber++); //$NON-NLS-1$
		this.generalName = getName();
		this.pool = pool;
		// set the context loader to avoid leaking the current context loader
		// for the thread that spawns this worker (bug 98376)
		setContextClassLoader(pool.defaultContextLoader);
	}

	/**
	 * Returns the currently running job, or null if none.
	 */
	public Job currentJob() {
		return (Job) currentJob;
	}

	private IStatus handleException(InternalJob job, Throwable t) {
		String message = NLS.bind(JobMessages.jobs_internalError, job.getName());
		return new Status(IStatus.ERROR, JobManager.PI_JOBS, JobManager.PLUGIN_ERROR, message, t);
	}

	@Override
	public void run() {
		setNormPriority();
		try {
			while ((currentJob = pool.startJob(this)) != null) {
				IStatus result = Status.OK_STATUS;
				IProgressMonitor monitor = currentJob.getProgressMonitor();
				try {
					setName(getJobName());
					result = currentJob.run(monitor);
				} catch (OperationCanceledException e) {
					result = Status.CANCEL_STATUS;
				} catch (ThreadDeath e) {
					// must not consume thread death
					result = handleException(currentJob, e);
					throw e;
				} catch (Exception | Error e) {
					result = handleException(currentJob, e);
				} finally {
					if (result != Job.ASYNC_FINISH && monitor != null) {
						monitor.done();
					}
					// clear interrupted state for this thread
					Thread.interrupted();
					// result must not be null
					if (result == null) {
						String message = NLS.bind(JobMessages.jobs_returnNoStatus, currentJob.getClass().getName());
						result = handleException(currentJob, new NullPointerException(message));
					}
					pool.endJob(currentJob, result);
					currentJob = null;
					setName(generalName);
					// reset thread priority in case job changed it
					setNormPriority();
				}
			}
		} catch (Throwable t) {
			RuntimeLog.log(new Status(IStatus.ERROR, JobManager.PI_JOBS, JobManager.PLUGIN_ERROR, "Unhandled error", t)); //$NON-NLS-1$
		} finally {
			currentJob = null;
			pool.endWorker(this);
		}
	}

	private void setNormPriority() {
		if (getPriority() != Thread.NORM_PRIORITY) {
			// Setting priority on some platforms may cause high overhead
			setPriority(Thread.NORM_PRIORITY);
		}
	}

	private String getJobName() {
		String name = currentJob.getName();
		if (name == null || name.trim().isEmpty()) {
			name = "<unnamed job: " + currentJob.getClass().getName() + ">"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return generalName + ": " + name; //$NON-NLS-1$
	}
}
