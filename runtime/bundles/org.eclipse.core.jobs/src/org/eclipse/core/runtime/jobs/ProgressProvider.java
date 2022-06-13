/*******************************************************************************
 * Copyright (c) 2003, 2022 IBM Corporation and others.
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
 *     Christoph Läubrich - Issue #40
 *******************************************************************************/
package org.eclipse.core.runtime.jobs;

import org.eclipse.core.runtime.*;

/**
 * The progress provider supplies the job manager with progress monitors for
 * running jobs.  There can only be one progress provider at any given time.
 * <p>
 * This class is intended for use by the currently executing Eclipse application.
 * Plug-ins outside the currently running application should not reference or
 * subclass this class.
 * </p>
 *
 * @see IJobManager#setProgressProvider(ProgressProvider)
 * @since 3.0
 */
public abstract class ProgressProvider {
	/**
	 * Provides a new progress monitor instance to be used by the given job.
	 * This method is called prior to running any job that does not belong to a
	 * progress group. The returned monitor will be supplied to the job's
	 * <code>run</code> method.
	 *
	 * @see #createProgressGroup()
	 * @see Job#setProgressGroup(IProgressMonitor, int)
	 * @param job the job to create a progress monitor for
	 * @return a progress monitor, or <code>null</code> if no progress monitoring
	 * is needed.
	 */
	public abstract IProgressMonitor createMonitor(Job job);

	/**
	 * Returns a progress monitor that can be used to provide aggregated progress
	 * feedback on a set of running jobs. This method implements
	 * <code>IJobManager.createProgressGroup</code>, and must obey all rules
	 * specified in that contract.
	 * <p>
	 * This default implementation returns a new <code>NullProgressMonitor</code>.
	 * Subclasses may override.
	 *
	 * @see IJobManager#createProgressGroup()
	 * @return a progress monitor
	 */
	public IProgressMonitor createProgressGroup() {
		return new NullProgressMonitor();
	}

	/**
	 * Returns a progress monitor that can be used by a running job to report
	 * progress in the context of a progress group. One of the two
	 * <code>createMonitor</code> methods will be invoked prior to each execution of
	 * a job, depending on whether a progress group was specified for the job.
	 * <p>
	 * The provided monitor must be a monitor returned by the method
	 * <code>createProgressGroup</code>. This method is responsible for asserting
	 * this and throwing an appropriate runtime exception if an invalid monitor is
	 * provided.
	 * <p>
	 * This default implementation returns a new <code>SubMonitor</code>. Subclasses
	 * may override.
	 *
	 * @see IJobManager#createProgressGroup()
	 * @see Job#setProgressGroup(IProgressMonitor, int)
	 * @param job   the job to create a progress monitor for
	 * @param group the progress monitor group that this job belongs to
	 * @param ticks the number of ticks of work for the progress monitor
	 * @return a progress monitor, or <code>null</code> if no progress monitoring is
	 *         needed.
	 */
	public IProgressMonitor createMonitor(Job job, IProgressMonitor group, int ticks) {
		return SubMonitor.convert(group, ticks);
	}

	/**
	 * Returns a progress monitor to use when none has been provided
	 * by the client running the job.
	 * <p>
	 * This default implementation returns a new
	 * <code>NullProgressMonitor</code>  Subclasses may override.
	 *
	 * @return a progress monitor
	 */
	public IProgressMonitor getDefaultMonitor() {
		return new NullProgressMonitor();
	}

	/**
	 * Returns a (possibly wrapped) progress monitor to use when running in a job.
	 * <p>
	 * The default implementation does the following:
	 * <ul>
	 * <li>if <code>monitor</code> is null or an instance of
	 * {@link NullProgressMonitor} returns the value from a call to
	 * ProgressProvider#getDefaultMonitor()</li>
	 * <li>in all other cases return the the <code>monitor</code> passed to this
	 * method.
	 * </ul>
	 *
	 * @param monitor the monitor to wrap, might be null
	 * @return a progress monitor for the given <code>monitor</code> but never
	 *         <code>null</code>
	 * @since 3.13
	 */
	public IProgressMonitor monitorFor(IProgressMonitor monitor) {
		if (monitor == null || monitor instanceof NullProgressMonitor) {
			return getDefaultMonitor();
		}
		return monitor;
	}
}
