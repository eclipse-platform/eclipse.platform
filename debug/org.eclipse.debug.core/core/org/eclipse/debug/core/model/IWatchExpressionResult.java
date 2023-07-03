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
 * The result of an evaluation performed by an
 * <code>org.eclipse.debug.core.model.IWatchExpressionDelegate</code>.
 * A watch expression reports the value of the evaluation
 * and any errors or exceptions that occurred.
 * <p>
 * Clients may implement this interface.
 * </p>
 * @see org.eclipse.debug.core.model.IWatchExpressionDelegate
 * @since 3.0
 */
public interface IWatchExpressionResult {
	/**
	 * Returns the value representing the result of the
	 * evaluation, or <code>null</code> if the
	 * associated evaluation failed. If
	 * the associated evaluation failed, there will
	 * be problems, or an exception in this result.
	 *
	 * @return the resulting value, possibly
	 * <code>null</code>
	 */
	IValue getValue();

	/**
	 * Returns whether the evaluation had any problems
	 * or if an exception occurred while performing the
	 * evaluation.
	 *
	 * @return whether there were any problems.
	 * @see #getErrorMessages()
	 * @see #getException()
	 */
	boolean hasErrors();

	/**
	 * Returns an array of problem messages. Each message describes a problem that
	 * occurred while compiling the snippet.
	 *
	 * @return evaluation error messages, or an empty array if no errors occurred
	 */
	String[] getErrorMessages();

	/**
	 * Returns the expression that was evaluated.
	 *
	 * @return The string expression.
	 */
	String getExpressionText();

	/**
	 * Returns any exception that occurred while performing the evaluation
	 * or <code>null</code> if an exception did not occur.
	 * The exception will be a debug exception or a debug exception
	 * that wrappers a debug model specific exception that indicates a problem communicating
	 * with the target or with actually performing some action in the target.
	 *
	 * @return The exception that occurred during the evaluation
	 * @see org.eclipse.debug.core.DebugException
	 */
	DebugException getException();
}
