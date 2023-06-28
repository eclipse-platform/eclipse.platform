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
package org.eclipse.debug.core.model;

/**
 * A watch expression is an expression that is evaluated in the context
 * of a specific stack frame, thread, debug target, process, or launch.
 * Generally, a watch expression is a snippet of code that is evaluated
 * each time a debug target suspends, or when a user provides a context
 * for an evaluation by selecting a debug target or thread. An expression
 * updates its value when it is provided with a context in which it
 * can perform an evaluation.
 * <p>
 * An implementation is provided by the debug platform. Clients that support watch expressions
 * should contribute and implement a watch expression delegate. Watch
 * expressions can be created via the <code>IExpressionManager</code>.
 * </p>
 * @see org.eclipse.debug.core.model.IWatchExpressionDelegate
 * @see org.eclipse.debug.core.IExpressionManager
 * @since 3.0
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IWatchExpression extends IErrorReportingExpression {

	/**
	 * Updates this watch expression's value based on the current evaluation
	 * context. This watch expression fires a debug change event when the
	 * evaluation is complete. A watch expression can be asked to
	 * evaluate even when it is disabled. Note that implementations should
	 * generally be asynchronous to avoid blocking the calling thread.
	 */
	void evaluate();

	/**
	 * Sets the context for this watch expression, or <code>null</code> if none.
	 * If the given context is valid for this expression, this expression may
	 * update its value. When the value update is complete, a debug change event
	 * is fired. When <code>null</code> is specified as a context, this
	 * expression may choose to retain its previous value.
	 * <p>
	 * The context is usually one of (but not limited to):
	 * </p>
	 * <ul>
	 * <li>a debug target (<code>IDebugTarget</code>)</li>
	 * <li>a thread (<code>IThread</code>)</li>
	 * <li>a stack frame (<code>IStackFrame</code>)</li>
	 * </ul>
	 *
	 * @param context context in which to update this expression's value, or
	 *            <code>null</code> if none
	 */
	void setExpressionContext(IDebugElement context);
	/**
	 * Sets this watch expression's snippet of code. This method
	 * causes the new snippet to be evaluated immediately in
	 * the expression's last context.
	 *
	 * @param expressionText the snippet which will be evaluated
	 */
	void setExpressionText(String expressionText);
	/**
	 * Returns whether the result of this watch expression is pending.
	 * An expression is pending if an evaluation has been requested, but
	 * the value has not yet been returned.
	 *
	 * @return whether this expression's result is pending
	 */
	boolean isPending();
	/**
	 * Returns whether this expression is enabled. An enabled expression will
	 * update its value. A disabled expression will not.
	 *
	 * @return whether this expression is enabled
	 */
	boolean isEnabled();
	/**
	 * Sets this expression's enabled state. This method
	 * causes the new snippet to be evaluated immediately in
	 * the expression's last context.
	 *
	 * @param enabled whether this expression should be enabled
	 */
	void setEnabled(boolean enabled);

}
