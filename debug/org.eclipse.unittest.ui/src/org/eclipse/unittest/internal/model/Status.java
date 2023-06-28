/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.unittest.internal.model;

import org.eclipse.unittest.model.ITestElement.Result;

/**
 * An object describing a test current status
 */
public final class Status {
	public static final Status RUNNING_ERROR = new Status("RUNNING_ERROR"); //$NON-NLS-1$
	public static final Status RUNNING_FAILURE = new Status("RUNNING_FAILURE"); //$NON-NLS-1$
	public static final Status RUNNING = new Status("RUNNING"); //$NON-NLS-1$

	public static final Status ERROR = new Status("ERROR"); //$NON-NLS-1$
	public static final Status FAILURE = new Status("FAILURE"); //$NON-NLS-1$
	public static final Status OK = new Status("OK"); //$NON-NLS-1$
	public static final Status NOT_RUN = new Status("NOT_RUN"); //$NON-NLS-1$

	private static final Status[] OLD_CODE = { OK, ERROR, FAILURE };

	private final String fName;

	private Status(String name) {
		fName = name;
	}

	@Override
	public String toString() {
		return fName;
	}

	/* error state predicates */

	/**
	 * Indicates if current test status is OK
	 *
	 * @return <code>true</code> if current status is OK, otherwise returns
	 *         <code>false</code>
	 */
	public boolean isOK() {
		return this == OK || this == RUNNING || this == NOT_RUN;
	}

	/**
	 * Indicates if current test has failures
	 *
	 * @return <code>true</code> if current test has failures, otherwise return
	 *         <code>false</code>
	 */
	public boolean isFailure() {
		return this == FAILURE || this == RUNNING_FAILURE;
	}

	/**
	 * Indicates if current test has errors
	 *
	 * @return <code>true</code> if current test has errors, otherwise return
	 *         <code>false</code>
	 */
	public boolean isError() {
		return this == ERROR || this == RUNNING_ERROR;
	}

	/**
	 * Indicates if current test has errors and/or failures
	 *
	 * @return <code>true</code> if current test has errors and/or failures,
	 *         otherwise return <code>false</code>
	 */
	public boolean isErrorOrFailure() {
		return isError() || isFailure();
	}

	/* progress state predicates */

	/**
	 * Indicates if current test isn't run yet
	 *
	 * @return <code>true</code> if current test isn't run yet, otherwise return
	 *         <code>false</code>
	 */
	public boolean isNotRun() {
		return this == NOT_RUN;
	}

	/**
	 * Indicates if current test is running
	 *
	 * @return <code>true</code> if current test is running, otherwise return
	 *         <code>false</code>
	 */
	public boolean isRunning() {
		return this == RUNNING || this == RUNNING_FAILURE || this == RUNNING_ERROR;
	}

	public boolean isDone() {
		return this == OK || this == FAILURE || this == ERROR;
	}

	/**
	 * Converts from an old status code to a {@link Status} constant
	 *
	 * @param oldStatus one of {@link Status}'s constants
	 * @return the {@link Status} constant
	 */
	public static Status convert(int oldStatus) {
		return OLD_CODE[oldStatus];
	}

	/**
	 * Converts the current {@link Status} object into a
	 * {@link org.eclipse.unittest.model.ITestElement.Result} object
	 *
	 * @return a {@link org.eclipse.unittest.model.ITestElement.Result} object
	 *         instance
	 */
	public Result convertToResult() {
		if (isNotRun())
			return Result.UNDEFINED;
		if (isError())
			return Result.ERROR;
		if (isFailure())
			return Result.FAILURE;
		if (isRunning()) {
			return Result.UNDEFINED;
		}
		return Result.OK;
	}

	/**
	 * Converts the current {@link Status} object into a {@link ProgressState}
	 * object
	 *
	 * @return a {@link ProgressState} object instance
	 */
	public ProgressState convertToProgressState() {
		if (isRunning()) {
			return ProgressState.RUNNING;
		}
		if (isDone()) {
			return ProgressState.COMPLETED;
		}
		return ProgressState.NOT_STARTED;
	}

	/**
	 * Creates a {@link Status} object from a given
	 * {@link org.eclipse.unittest.model.ITestElement.Result} object
	 *
	 * @param status a {@link Status} object
	 * @return an {@link org.eclipse.unittest.model.ITestElement.Result} object
	 *         instance
	 */
	public static Status fromResult(Result status) {
		switch (status) {
		case ERROR:
			return Status.ERROR;
		case FAILURE:
			return Status.FAILURE;
		case OK:
			return Status.OK;
		case IGNORED:
			return Status.OK;
		case UNDEFINED:
			return Status.NOT_RUN;
		default:
			return Status.NOT_RUN;
		}
	}
}