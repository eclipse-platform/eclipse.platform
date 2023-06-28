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
 * The ability to end a debug session with a target program
 * and allow the target to continue running.
 * <p>
 * Clients may implement this interface.
 * </p>
 * @see IDebugTarget
 */
public interface IDisconnect {
	/**
	 * Returns whether this element can currently disconnect.
	 *
	 * @return whether this element can currently disconnect
	 */
	boolean canDisconnect();
	/**
	 * Disconnects this element from its target. Generally, disconnecting
	 * ends a debug session with a debug target, but allows the target
	 * program to continue running.
	 *
	 * @exception DebugException on failure. Reasons include:<ul>
	 * <li>TARGET_REQUEST_FAILED - The request failed in the target
	 * <li>NOT_SUPPORTED - The capability is not supported by the target
	 * </ul>
	 */
	void disconnect() throws DebugException;
	/**
	 * Returns whether this element is disconnected.
	 *
	 * @return whether this element is disconnected
	 */
	boolean isDisconnected();
}


