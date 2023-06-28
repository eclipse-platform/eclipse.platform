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


import org.eclipse.debug.core.model.IExpression;
import org.eclipse.debug.core.model.IWatchExpression;
import org.eclipse.debug.core.model.IWatchExpressionDelegate;

/**
 * The expression manager manages the collection of registered
 * expressions in the workspace. An expression is a snippet of code
 * that can be evaluated to produce a value. Expression creation
 * and evaluation are client responsibilities.
 * <p>
 * Clients interested in expression change notification may
 * register with the expression manager - see
 * <code>IExpressionListener</code> and <code>IExpressionsListener</code>.
 * </p>
 * @see org.eclipse.debug.core.model.IExpression
 * @see org.eclipse.debug.core.IExpressionListener
 * @see org.eclipse.debug.core.IExpressionsListener
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IExpressionManager {
	/**
	 * Adds the given expression to the collection of registered expressions
	 * in the workspace and notifies all registered listeners. This has no effect
	 * if the given expression is already registered.
	 *
	 * @param expression the expression to add
	 */
	void addExpression(IExpression expression);

	/**
	 * Adds the given expressions to the collection of registered expressions
	 * in the workspace and notifies all registered listeners. Has no effect
	 * on expressions already registered.
	 *
	 * @param expressions the expressions to add
	 * @since 2.1
	 */
	void addExpressions(IExpression[] expressions);

	/**
	 * Creates and returns a new watch expression with the
	 * given text. The returned expression is <b>not</b> added to
	 * this manager.
	 *
	 * @return new watch expression
	 * @param expressionText the text for the new expression
	 * @since 3.0
	 */
	IWatchExpression newWatchExpression(String expressionText);

	/**
	 * Returns a collection of all registered expressions,
	 * possibly empty.
	 *
	 * @return an array of expressions
	 */
	IExpression[] getExpressions();

	/**
	 * Returns whether there are any registered expressions
	 *
	 * @return whether there are any registered expressions
	 */
	boolean hasExpressions();

	/**
	 * Returns a collection of all expressions registered for the
	 * given debug model,possibly empty.
	 *
	 * @param modelIdentifier identifier of a debug model plug-in
	 * @return an array of expressions
	 */
	IExpression[] getExpressions(String modelIdentifier);

	/**
	 * Removes the given expression from the expression manager,
	 * and notifies all registered listeners. Has no effect if the
	 * given expression is not currently registered.
	 *
	 * @param expression the expression to remove
	 */
	void removeExpression(IExpression expression);

	/**
	 * Removes the given expressions from the collection of registered expressions
	 * in the workspace and notifies all registered listeners. Has no effect
	 * on expressions not already registered.
	 *
	 * @param expressions the expressions to remove
	 * @since 2.1
	 */
	void removeExpressions(IExpression[] expressions);

	/**
	 * Adds the given listener to the collection of registered expression listeners.
	 * Has no effect if an identical listener is already registered.
	 *
	 * @param listener the listener to add
	 */
	void addExpressionListener(IExpressionListener listener);

	/**
	 * Removes the given listener from the collection of registered expression listeners.
	 * Has no effect if an identical listener is not already registered.
	 *
	 * @param listener the listener to remove
	 */
	void removeExpressionListener(IExpressionListener listener);

	/**
	 * Adds the given listener to the collection of registered expression listeners.
	 * Has no effect if an identical listener is already registered.
	 *
	 * @param listener the listener to add
	 * @since 2.1
	 */
	void addExpressionListener(IExpressionsListener listener);

	/**
	 * Removes the given listener from the collection of registered expression listeners.
	 * Has no effect if an identical listener is not already registered.
	 *
	 * @param listener the listener to remove
	 * @since 2.1
	 */
	void removeExpressionListener(IExpressionsListener listener);

	/**
	 * Returns a new watch expression delegate for the given debug
	 * model or <code>null</code> if no delegate is available.
	 *
	 * @param id the unique identifier of a debug model for which a
	 *  watch expression delegate has been contributed
	 * @return a watch expression delegate associated with the given model
	 * 		or <code>null</code> if none
	 * @since 3.0
	 * @see IWatchExpressionDelegate
	 */
	IWatchExpressionDelegate newWatchExpressionDelegate(String id);

	/**
	 * Returns whether a watch expression delegate has been contributed for
	 * the given debug model.
	 *
	 * @param id the unique identifier of a debug model
	 * @return whether a watch expression delegate has been contributed for
	 *    the given debug model
	 * @since 3.1
	 * @see IWatchExpressionDelegate
	 */
	boolean hasWatchExpressionDelegate(String id);
}


