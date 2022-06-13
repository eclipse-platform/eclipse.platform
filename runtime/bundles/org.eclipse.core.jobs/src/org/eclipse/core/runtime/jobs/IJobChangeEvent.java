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
package org.eclipse.core.runtime.jobs;

import org.eclipse.core.runtime.IStatus;

/**
 * An event describing a change to the state of a job.
 *
 * @see IJobChangeListener
 * @since 3.0
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IJobChangeEvent {
	/**
	 * The amount of time in milliseconds to wait after scheduling the job before it
	 * should be run, or <code>-1</code> if not applicable for this type of event.
	 * This value is only applicable for the <code>scheduled</code> event.
	 *
	 * @return the delay time for this event
	 */
	long getDelay();

	/**
	 * The job on which this event occurred.
	 *
	 * @return the job for this event
	 */
	Job getJob();

	/**
	 * The result returned by the job's run method, or <code>null</code> if
	 * not applicable.  This value is only applicable for the <code>done</code> event.
	 *
	 * @return the status for this event
	 */
	IStatus getResult();

	/**
	 * The result returned by the job's job group, if this event signals
	 * completion of the last job in a group, or <code>null</code> if not
	 * applicable.  This value is only applicable for the <code>done</code> event.
	 *
	 * @return the job group status for this event, or <code>null</code>
	 * @since 3.7
	 */
	IStatus getJobGroupResult();
}
