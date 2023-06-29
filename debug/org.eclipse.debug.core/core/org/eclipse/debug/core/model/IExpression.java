/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
 * An expression is a snippet of code that can be evaluated
 * to produce a value. When and how an expression is evaluated
 * is implementation specific. The context/binding required to
 * evaluate an expression varies by debug model, and by
 * user intent. Furthermore, an expression may need to be evaluated
 * at a specific location in a program (for example, at a
 * breakpoint/line where certain variables referenced in the
 * expression are visible/allocated). A user may want to
 * evaluate an expression once to produce a value that can
 * be inspected iteratively, or they may wish to evaluate an
 * expression iteratively producing new values each time
 * (i.e. as in a watch list).
 * <p>
 * Clients are intended to implement this interface.
 * </p>
 * @since 2.0
 */
public interface IExpression extends IDebugElement {

	/**
	 * Returns this expression's snippet of code.
	 *
	 * @return the expression
	 */
	String getExpressionText();

	/**
	 * Returns the current value of this expression or
	 * <code>null</code> if this expression does not
	 * currently have a value.
	 *
	 * @return value or <code>null</code>
	 */
	IValue getValue();

	/**
	 * Returns the debug target this expression is associated
	 * with, or <code>null</code> if this expression is not
	 * associated with a debug target.
	 *
	 * @return debug target or <code>null</code>
	 * @see IDebugElement#getDebugTarget()
	 */
	@Override IDebugTarget getDebugTarget();

	/**
	 * Notifies this expression that it has been removed
	 * from the expression manager. Any required clean up
	 * is be performed such that this expression can be
	 * garbage collected.
	 */
	void dispose();
}
