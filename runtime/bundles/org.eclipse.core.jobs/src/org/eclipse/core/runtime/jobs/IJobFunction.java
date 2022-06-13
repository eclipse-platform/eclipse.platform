/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 474276
 *******************************************************************************/
package org.eclipse.core.runtime.jobs;

import org.eclipse.core.runtime.*;

/**
 * This is a functional interface representation of {@link Job}, suitable
 * for use in lambda expressions.
 *
 * @see Job#create(String, IJobFunction)
 * @since 3.6
 */
public interface IJobFunction {
	/**
	 * Executes this job. Returns the result of the execution.
	 * <p>
	 * The provided monitor can be used to report progress and respond to
	 * cancellation. If the progress monitor has been canceled, the job should
	 * finish its execution at the earliest convenience and return a result
	 * status of severity {@link IStatus#CANCEL}. The singleton cancel status
	 * {@link Status#CANCEL_STATUS} can be used for this purpose. The monitor is
	 * only valid for the duration of the invocation of this method.
	 * <p>
	 * This method must not be called directly by clients. Clients should call
	 * <code>schedule</code>, which will in turn cause this method to be called.
	 * <p>
	 * Jobs can optionally finish their execution asynchronously (in another
	 * thread) by returning a result status of {@link Job#ASYNC_FINISH}. Jobs
	 * that finish asynchronously <b>must</b> specify the execution thread by
	 * calling {@link Job#setThread(Thread)}, and must indicate when they are
	 * finished by calling the method {@link Job#done(IStatus)}.
	 *
	 * @param monitor
	 *            the monitor to be used for reporting progress and responding
	 *            to cancellation. The monitor is never {@code null}. It is the
	 *            caller's responsibility to call
	 *            {@link IProgressMonitor#done()} after this method returns or
	 *            throws an exception.
	 *
	 * @return resulting status of the run. The result must not be {@code null}.
	 * @see Job#ASYNC_FINISH
	 * @see Job#done(IStatus)
	 */
	IStatus run(IProgressMonitor monitor);
}
