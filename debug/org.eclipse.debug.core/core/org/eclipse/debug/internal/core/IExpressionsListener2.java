/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
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
package org.eclipse.debug.internal.core;

import org.eclipse.debug.core.IExpressionsListener;
import org.eclipse.debug.core.model.IExpression;

/**
 * Provides call-back methods for expressions that have been moved or inserted
 * @since 3.4
 */
public interface IExpressionsListener2 extends IExpressionsListener {

	/**
	 * Fires the model delta necessary to update the viewer after one or more
	 * expressions have been moved to a different index in the tree.  The
	 * expression array must be in the same order as they were added.  The given index
	 * <strong>must</strong> take into account the removal of the expressions to be removed.
	 * Therefore, for each of the expressions being moved with indices lower than the expect
	 * insertion index, the passed insertion index must be reduced by one.
	 *
	 * @param expressions array of expressions to be moved
	 * @param index the index the expressions will be added to, adjusted for moved expressions
	 */
	void expressionsMoved(IExpression[] expressions, int index);

	/**
	 * Fires the model delta necessary to update the viewer after one or more
	 * expressions have been inserted into a specific index in the tree.  The
	 * expression array must be in the same order as they were added.
	 *
	 * @param expressions array of expressions to be moved
	 * @param index the index the expressions will be added to
	 */
	void expressionsInserted(IExpression[] expressions, int index);

}
