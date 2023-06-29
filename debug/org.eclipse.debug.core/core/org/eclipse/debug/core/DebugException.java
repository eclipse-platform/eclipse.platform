/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.debug.core;



import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;


/**
 * A checked exception representing a failure.
 * <p>
 * Defines status codes relevant to the debug plug-in. When a
 * debug exception is thrown, it contains a status object describing
 * the cause of the exception. The status objects originating from the
 * debug plug-in use the codes defined in this class.
 * </p>
 * @see IStatus
 * @noextend This class is not intended to be subclassed by clients.
 */
public class DebugException extends CoreException {

	/**
	 * All objects that can be serialized should have a stable serialVersionUID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Indicates a request made of a debug element has failed
	 * on the target side.
	 */
	public static final int TARGET_REQUEST_FAILED = 5010;

	/**
	 * Indicates a request is not supported by the capabilities of a debug element.
	 * For example, a request was made to terminate an element that does not
	 * support termination.
	 */
	public static final int NOT_SUPPORTED = 5011;

	/**
	 * Indicates that a request made of manager has failed, or a request made of a
	 * debug element has failed on the client side (that is, before the request was
	 * sent to the debug target).
	 */
	public static final int REQUEST_FAILED = 5012;

	/**
	 * Indicates an internal error. This is an unexpected state.
	 */
	public static final int INTERNAL_ERROR = 5013;

	/**
	 * Indicates an improperly configured breakpoint. Breakpoints have a minimal
	 * set of required attributes as defined by the breakpoint manager.
	 *
	 * @see IBreakpointManager
	 */
	public static final int CONFIGURATION_INVALID = 5014;

	/**
	 * Indicates a launch configuration could not be restored because its
	 * launch configuration type definition is missing.
	 *
	 * @since 3.0
	 */
	public static final int MISSING_LAUNCH_CONFIGURATION_TYPE = 5020;

	/**
	 * Constructs a new debug exception with the given status object.
	 *
	 * @param status the status object describing this exception
	 * @see IStatus
	 */
	public DebugException(IStatus status) {
		super(status);
	}

}
