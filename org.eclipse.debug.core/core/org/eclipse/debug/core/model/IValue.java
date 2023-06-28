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
 * A value represents the value of a variable.
 * A value representing a complex data structure contains variables.
 * <p>
 * An implementation may choose to re-use or discard
 * values on iterative thread suspensions. Clients
 * cannot assume that values are identical or equal across
 * iterative thread suspensions and must check for equality on iterative
 * suspensions if they wish to re-use the objects.
 * </p>
 * <p>
 * An implementation that preserves equality
 * across iterative suspensions may display more desirable behavior in
 * some clients. For example, if variables are preserved
 * while stepping, a UI client would be able to update the UI incrementally,
 * rather than collapse and redraw the entire list or tree.
 * </p>
 * <p>
 * Clients may implement this interface.
 * </p>
 * @see IVariable
 */


public interface IValue extends IDebugElement {

	/**
	 * Returns a description of the type of data this value contains or
	 * references.
	 *
	 * @return the name of this value's reference type
	 * @exception DebugException if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                </ul>
	 */
	String getReferenceTypeName() throws DebugException;

	/**
	 * Returns this value as a <code>String</code>.
	 *
	 * @return a String representation of this value
	 * @exception DebugException if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                </ul>
	 */
	String getValueString() throws DebugException;

	/**
	 * Returns whether this value is currently allocated.
	 * <p>
	 * For example, if this value represents an object that has been garbage
	 * collected, <code>false</code> is returned.
	 * </p>
	 *
	 * @return whether this value is currently allocated
	 * @exception DebugException if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                </ul>
	 */
	boolean isAllocated() throws DebugException;
	/**
	 * Returns the visible variables in this value. An empty
	 * collection is returned if there are no visible variables.
	 *
	 * @return an array of visible variables
	 * @exception DebugException if this method fails.  Reasons include:
	 * <ul><li>Failure communicating with the VM.  The DebugException's
	 * status code contains the underlying exception responsible for
	 * the failure.</li>
	 * </ul>
	 * @since 2.0
	 */
	IVariable[] getVariables() throws DebugException;

	/**
	 * Returns whether this value currently contains any visible variables.
	 *
	 * @return whether this value currently contains any visible variables
	 * @exception DebugException if this method fails.  Reasons include:
	 * <ul><li>Failure communicating with the debug target.  The DebugException's
	 * status code contains the underlying exception responsible for
	 * the failure.</li>
	 * </ul>
	 * @since 2.0
	 */
	boolean hasVariables() throws DebugException;
}
