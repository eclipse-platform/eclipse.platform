/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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

/**
 * An expression listener is notified of expression additions,
 * removals, and changes. Listeners register and unregister with the
 * expression manager.
 * <p>
 * Clients may implement this interface.
 * </p>
 * @see IExpressionManager
 * @since 2.0
 */

public interface IExpressionListener {

	/**
	 * Notifies this listener that the given expression has been added
	 * to the expression manager.
	 *
	 * @param expression the added expression
	 */
	void expressionAdded(IExpression expression);
	/**
	 * Notifies this listener that the given expression has been removed
	 * from the expression manager.
	 *
	 * @param expression the removed expression
	 */
	void expressionRemoved(IExpression expression);

	/**
	 * Notifies this listener that the given expression has
	 * changed.
	 *
	 * @param expression the changed expression
	 */
	void expressionChanged(IExpression expression);

}
