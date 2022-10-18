/*******************************************************************************
 * Copyright (c) 2003, 2012 IBM Corporation and others.
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
package org.eclipse.core.runtime.jobs;

/**
 * Callback interface for clients interested in being notified when jobs change state.
 * <p>
 * A single job listener instance can be added either to the job manager, for notification
 * of all scheduled jobs, or to any set of individual jobs.  A single listener instance should
 * not be added to both the job manager, and to individual jobs (such a listener may
 * receive duplicate notifications).
 * </p><p>
 * Clients should not rely on the result of the <code>Job#getState()</code>
 * method on jobs for which notification is occurring. Listeners are notified of
 * all job state changes, but whether the state change occurs before, during, or
 * after listeners are notified is unspecified.
 * </p><p>
 * It is undefined in which Thread the notification occurs.
 * </p><p>
 * Clients may implement this interface.
 * </p>
 * @see JobChangeAdapter
 * @see IJobManager#addJobChangeListener(IJobChangeListener)
 * @see IJobManager#removeJobChangeListener(IJobChangeListener)
 * @see Job#addJobChangeListener(IJobChangeListener)
 * @see Job#getState()
 * @see Job#removeJobChangeListener(IJobChangeListener)
 * @since 3.0
 */
public interface IJobChangeListener {
	/**
	 * <p>
	 * Notification that a job is about to be run. Listeners are allowed to sleep,
	 * cancel, or change the priority of the job before it is started (and as a
	 * result may prevent the run from actually occurring).
	 * </p>
	 * <p>
	 * Implementations should not block and return promptly.
	 * </p>
	 *
	 * @param event the event details
	 */
	void aboutToRun(IJobChangeEvent event);

	/**
	 * <p>
	 * Notification that a job was previously sleeping and has now been rescheduled
	 * to run.
	 * </p>
	 * <p>
	 * Implementations should not block and return promptly.
	 * </p>
	 *
	 * @param event the event details
	 */
	void awake(IJobChangeEvent event);

	/**
	 * <p>
	 * Notification that a job has completed execution, either due to cancelation,
	 * successful completion, or failure. The event status object indicates how the
	 * job finished, and the reason for failure, if applicable.
	 * </p>
	 * <p>
	 * Implementations should not block and return promptly.
	 * </p>
	 *
	 * @param event the event details
	 */
	void done(IJobChangeEvent event);

	/**
	 * <p>
	 * Notification that a job has started running.
	 * </p>
	 * <p>
	 * Implementations should not block and return promptly.
	 * </p>
	 *
	 * @param event the event details
	 */
	void running(IJobChangeEvent event);

	/**
	 * <p>
	 * Notification that a job is being added to the queue of scheduled jobs. The
	 * event details includes the scheduling delay before the job should start
	 * running.
	 * </p>
	 * <p>
	 * Implementations should not block and return promptly.
	 * </p>
	 *
	 * @param event the event details, including the job instance and the scheduling
	 *              delay
	 */
	void scheduled(IJobChangeEvent event);

	/**
	 * <p>
	 * Notification that a job was waiting to run and has now been put in the
	 * sleeping state.
	 * </p>
	 * <p>
	 * Implementations should not block and return promptly.
	 * </p>
	 *
	 * @param event the event details
	 */
	void sleeping(IJobChangeEvent event);
}
