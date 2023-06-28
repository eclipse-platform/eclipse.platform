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
 * This interface is analogous to <code>IExpressionListener</code>, except
 * notifications are batched for more than when expression when possible.
 * </p>
 * <p>
 * Clients may implement this interface.
 * </p>
 * @see IExpressionManager
 * @since 2.1
 */

public interface IExpressionsListener {

	/**
	 * Notifies this listener that the given expressions have been added
	 * to the expression manager.
	 *
	 * @param expressions the added expressions
	 */
	void expressionsAdded(IExpression[] expressions);
	/**
	 * Notifies this listener that the given expressions has been removed
	 * from the expression manager.
	 *
	 * @param expressions the removed expressions
	 */
	void expressionsRemoved(IExpression[] expressions);

	/**
	 * Notifies this listener that the given expressions have
	 * changed.
	 *
	 * @param expressions the changed expressions
	 */
	void expressionsChanged(IExpression[] expressions);

}
