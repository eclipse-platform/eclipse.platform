/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.debug.core.model;


import org.eclipse.debug.core.DebugException;

/**
 * Provides the ability to step into, over, and return
 * from the current execution location.  Implementations
 * must be non-blocking.
 * <p>
 * Implementations should honor step filter settings in their
 * associated debug target, as defined by <code>IStepFilters</code>.
 * </p>
 * <p>
 * Clients may implement this interface.
 * </p>
 * @see org.eclipse.debug.core.model.IStepFilters
 */
public interface IStep {
	/**
	 * Returns whether this element can currently perform a step into.
	 *
	 * @return whether this element can currently perform a step into
	 */
	boolean canStepInto();
	/**
	 * Returns whether this element can currently perform a step over.
	 *
	 * @return whether this element can currently perform a step over
	 */
	boolean canStepOver();
	/**
	 * Returns whether this element can currently perform a step return.
	 *
	 * @return whether this element can currently perform a step return
	 */
	boolean canStepReturn();
	/**
	 * Returns whether this element is currently stepping.
	 * <p>
	 * For example, a thread is considered to be stepping
	 * after the <code>stepOver</code> call until the step over is completed,
	 * a breakpoint is reached, an exception is thrown, or the thread or debug target is
	 * terminated.
	 * </p>
	 *
	 * @return whether this element is currently stepping
	 */
	boolean isStepping();
	/**
	 * Steps into the current statement, generating <code>RESUME</code>
	 * and <code>SUSPEND</code> events for the associated thread. Can only be called
	 * when the associated thread is suspended. Implementations must implement
	 * stepping as non-blocking.
	 *
	 * @exception DebugException on failure. Reasons include:<ul>
	 * <li>TARGET_REQUEST_FAILED - The request failed in the target</li>
	 * <li>NOT_SUPPORTED - The capability is not supported by the target</li>
	 * </ul>
	 */
	void stepInto() throws DebugException;
	/**
	 * Steps over the current statement, generating <code>RESUME</code>
	 * and <code>SUSPEND</code> events for the associated thread. Can only be called
	 * when the associated thread is suspended. Implementations must implement
	 * stepping as non-blocking.
	 *
	 * @exception DebugException on failure. Reasons include:<ul>
	 * <li>TARGET_REQUEST_FAILED - The request failed in the target</li>
	 * <li>NOT_SUPPORTED - The capability is not supported by the target</li>
	 * </ul>
	 */
	void stepOver() throws DebugException;
	/**
	 * Steps to the next return statement in the current scope,
	 * generating <code>RESUME</code> and <code>SUSPEND</code> events for
	 * the associated thread. Can only be called when the associated thread is suspended.
	 * Implementations must implement stepping as non-blocking.
	 *
	 * @exception DebugException on failure. Reasons include:<ul>
	 * <li>TARGET_REQUEST_FAILED - The request failed in the target</li>
	 * <li>NOT_SUPPORTED - The capability is not supported by the target</li>
	 * </ul>
	 */
	void stepReturn() throws DebugException;
}
