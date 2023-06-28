/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
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
 *     Wind River Systems - integration with non-standard debug models (Bug 209883)
 *******************************************************************************/
package org.eclipse.debug.ui.actions;

import org.eclipse.core.runtime.CoreException;

/**
 * An optional adapter used to create a watch expression for a selected element.
 * <p>
 * The 'Create Watch Expression' action is enabled for an adaptable element
 * that have an associated <code>IWatchExpressionFactoryAdapter2</code>.
 *
 * When a watch expression factory adapter is available for an element, the factory is
 * consulted to create a watch expression for that element.
 * </p>
 * <p>
 * Clients may implement this interface.
 * </p>
 * @since 3.4
 */
public interface IWatchExpressionFactoryAdapter2  {

	/**
	 * Creates and returns an expression for the specified variable
	 * which is used to created an {@link org.eclipse.debug.core.model.IWatchExpression}.
	 *
	 * @param element element a watch expression is required for
	 * @return text used to create a watch expression
	 * @exception org.eclipse.core.runtime.CoreException if unable to create a watch
	 *  expression
	 */
	String createWatchExpression(Object element) throws CoreException;

	/**
	 * Returns whether a watch expression can be created for the specified variable.
	 *
	 * @param variable the specified variable
	 * @return whether an expression can be created
	 */
	boolean canCreateWatchExpression(Object variable);

}
