/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
package org.eclipse.debug.ui.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IVariable;

/**
 * An optional adapter used to create a watch expression for a selected variable.
 * <p>
 * The 'Create Watch Expression' action is enabled for instances of
 * {@link org.eclipse.debug.core.model.IVariable} that have an associated
 * {@link org.eclipse.debug.core.model.IWatchExpressionDelegate} registered
 * for that debug model.
 * When a watch expression factory adapter is available for a variable, the factory is
 * consulted to create a watch expression for that variable. When no adapter is provided,
 * the watch expression is generated based on the variable's name.
 * </p>
 * <p>
 * Also see the optional interface {@link IWatchExpressionFactoryAdapterExtension}.
 * </p>
 * <p>
 * Clients may implement this interface.
 * </p>
 * @since 3.2
 */
public interface IWatchExpressionFactoryAdapter {

	/**
	 * Creates and returns an expression for the specified variable
	 * which is used to created an {@link org.eclipse.debug.core.model.IWatchExpression}.
	 *
	 * @param variable variable a watch expression is required for
	 * @return text used to create a watch expression
	 * @exception org.eclipse.core.runtime.CoreException if unable to create a watch
	 *  expression
	 */
	String createWatchExpression(IVariable variable) throws CoreException;

}
