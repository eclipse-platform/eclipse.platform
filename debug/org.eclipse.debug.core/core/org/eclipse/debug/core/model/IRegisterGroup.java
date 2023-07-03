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
 * A register group is a group of registers that are
 * assigned to a stack frame. Some debug architectures
 * provide access to registers, and registers are often
 * grouped logically. For example, a floating point
 * register group.
 * <p>
 * Clients may implement this interface.
 * </p>
 * @since 2.0
 */
public interface IRegisterGroup extends IDebugElement {


	/**
	 * Returns the name of this register group.
	 *
	 * @return this register group's name
	 * @exception DebugException if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the debug target. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                </ul>
	 */
	String getName() throws DebugException;

	/**
	 * Returns the registers in this register group.
	 *
	 * @return the registers in this register group
	 * @exception DebugException if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the debug target. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                </ul>
	 */
	IRegister[] getRegisters() throws DebugException;

	/**
	 * Returns whether this register group currently contains any registers.
	 *
	 * @return whether this register group currently contains any registers
	 * @exception DebugException if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the debug target. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                </ul>
	 */
	boolean hasRegisters() throws DebugException;

}

