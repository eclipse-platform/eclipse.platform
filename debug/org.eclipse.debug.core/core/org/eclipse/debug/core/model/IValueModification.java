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
 * Provides the ability to modify the value of a variable in
 * a target.
 * <p>
 * Clients may implement this interface.
 * </p>
 * @see IVariable
 */
public interface IValueModification {

	/**
	 * Attempts to set the value of this variable to the
	 * value of the given expression.
	 *
	 * @param expression an expression to generate a new value
	 * @exception DebugException on failure. Reasons include:<ul>
	 * <li>TARGET_REQUEST_FAILED - The request failed in the target
	 * <li>NOT_SUPPORTED - The capability is not supported by the target
	 * </ul>
	 */
	void setValue(String expression) throws DebugException;

	/**
	 * Sets the value of this variable to the given value.
	 *
	 * @param value a new value
	 * @exception DebugException on failure. Reasons include:<ul>
	 * <li>TARGET_REQUEST_FAILED - The request failed in the target
	 * <li>NOT_SUPPORTED - The capability is not supported by the target
	 * </ul>
	 * @since 2.0
	 */
	void setValue(IValue value) throws DebugException;

	/**
	 * Returns whether this variable supports value modification.
	 *
	 * @return whether this variable supports value modification
	 */
	boolean supportsValueModification();

	/**
	 * Returns whether the given expression is valid to be used in
	 * setting a new value for this variable.
	 *
	 * @param expression an expression to generate a new value
	 * @return whether the expression is valid
	 * @exception DebugException on failure. Reasons include:<ul>
	 * <li>TARGET_REQUEST_FAILED - The request failed in the target
	 * <li>NOT_SUPPORTED - The capability is not supported by the target
	 * </ul>
	 */
	boolean verifyValue(String expression) throws DebugException;

	/**
	 * Returns whether the given value can be used as
	 * a new value for this variable.
	 *
	 * @param value a new value
	 * @return whether the value is valid
	 * @exception DebugException on failure. Reasons include:<ul>
	 * <li>TARGET_REQUEST_FAILED - The request failed in the target
	 * <li>NOT_SUPPORTED - The capability is not supported by the target
	 * </ul>
	 * @since 2.0
	 */
	boolean verifyValue(IValue value) throws DebugException;
}


