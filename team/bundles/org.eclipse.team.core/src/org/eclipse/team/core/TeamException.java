/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.core;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.internal.core.TeamPlugin;

/**
 * This exception is thrown by the team provider API.  It represents a failure in an API call.
 * Since some API calls take multiple arguments, the exception is capable of returning multiple
 * statuses.  The API definition determines if the exception represents a single or multiple status
 * response; this can also be tested on the exception instance itself.
 * <p>
 * To determine the exact cause of the failure the caller should look at each status in detail.
 * </p>
 * @since 2.0
 */
public class TeamException extends CoreException {

	// Field required to avoid compiler warning
	private static final long serialVersionUID = 1L;

	// The operation completed successfully.
	public static final int OK = 0;

	// The operation failed because the resource is not checked-in.
	public static final int NOT_CHECKED_IN = -1;

	// The operation failed because the resource is not checked-out.
	public static final int NOT_CHECKED_OUT = -2;

	// The corresponding remote resource no longer exists or was never created.
	public static final int NO_REMOTE_RESOURCE = -3;

	// The provider suffered an IO failure, the operation may be retried.
	public static final int IO_FAILED = -4;

	// The user is not authorized to execute the attempted operation.
	public static final int NOT_AUTHORIZED = -5;

	// The provider was unable to complete the operation for an unspecified reason.
	public static final int UNABLE = -6;

	// The operation cannot be performed due to a conflict with other work.
	public static final int CONFLICT = -7;

	/**
	 * Create a <code>TeamException</code> which contains the given status object.
	 * @param status the status for this exception
	 */
	public TeamException(IStatus status) {
		super(status);
	}

	/**
	 * Create a <code>TeamException</code> with an
	 * error status that contains the given message and
	 * throwable.
	 * @param message the message for the exception
	 * @param e an associated exception
	 * @since 3.0
	 */
	public TeamException(String message, Throwable e) {
		super(new Status(IStatus.ERROR, TeamPlugin.ID, 0, message, e));
	}

	/**
	 * Create a <code>TeamException</code> with an
	 * error status that contains the given message.
	 * @param message the message for the exception
	 */
	public TeamException(String message) {
		this(message, null);
	}

	/**
	 * Create a <code>TeamException</code> that wraps the given <code>CoreException</code>
	 * @param e a <code>CoreException</code>
	 * @since 3.0
	 */
	protected TeamException(CoreException e) {
		super(asStatus(e));
	}

	private static Status asStatus(CoreException e) {
		IStatus status = e.getStatus();
		return new Status(status.getSeverity(), status.getPlugin(), status.getCode(), status.getMessage(), e);
	}

	/**
	 * Return a <code>TeamException</code> for the given exception.
	 * @param e an exception
	 * @return a <code>TeamException</code> for the given exception
	 * @since 3.0
	 */
	public static TeamException asTeamException(CoreException e) {
		if (e instanceof TeamException) {
			return (TeamException)e;
		}
		return new TeamException(e);
	}

	/**
	 * Return a <code>TeamException</code> for the given exception.
	 * @param e an exception
	 * @return a <code>TeamException</code> for the given exception
	 * @since 3.0
	 */
	public static TeamException asTeamException(InvocationTargetException e) {
		Throwable target = e.getTargetException();
		if (target instanceof TeamException) {
			return (TeamException) target;
		}
		return new TeamException(new Status(IStatus.ERROR, TeamPlugin.ID, UNABLE, target.getMessage() != null ? target.getMessage() : "",	target)); //$NON-NLS-1$
	}
}
