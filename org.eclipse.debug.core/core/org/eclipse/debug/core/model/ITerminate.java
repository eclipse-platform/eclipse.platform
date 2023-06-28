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
 * Provides the ability to terminate an execution
 * context - for example, a thread, debug target or process.
 * <p>
 * Clients may implement this interface.
 * </p>
 */
public interface ITerminate {
	/**
	 * Returns whether this element can be terminated.
	 *
	 * @return whether this element can be terminated
	 */
	boolean canTerminate();
	/**
	 * Returns whether this element is terminated.
	 *
	 * @return whether this element is terminated
	 */
	boolean isTerminated();
	/**
	 * Causes this element to terminate, generating a <code>TERMINATE</code> event.
	 * Implementations may be blocking or non-blocking.
	 *
	 * @exception DebugException on failure. Reasons include:<ul>
	 * <li>TARGET_REQUEST_FAILED - The request failed in the target
	 * <li>NOT_SUPPORTED - The capability is not supported by the target
	 * </ul>
	 */
	void terminate() throws DebugException;
}
