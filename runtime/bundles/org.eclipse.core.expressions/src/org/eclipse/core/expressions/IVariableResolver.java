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
package org.eclipse.core.expressions;

import org.eclipse.core.runtime.CoreException;

/**
 * A variable resolver can be used to add additional variable resolving
 * strategies to an {@link EvaluationContext}.
 *
 * @see org.eclipse.core.expressions.EvaluationContext#resolveVariable(String, Object[])
 *
 * @since 3.0
 */
public interface IVariableResolver {

	/**
	 * Resolves a variable for the given name and arguments. The
	 * handler is allowed to return <code>null</code> to indicate
	 * that it is not able to resolve the requested variable.
	 *
	 * @param name the variable to resolve
	 * @param args an object array of arguments used to resolve the
	 *  variable
	 * @return the variable's value or <code>null</code> if no variable
	 *  could be resolved
	 * @exception CoreException if an errors occurs while resolving
	 *  the variable
	 */
	Object resolve(String name, Object[] args) throws CoreException;
}
