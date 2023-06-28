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
package org.eclipse.core.variables;

import org.eclipse.core.runtime.CoreException;

/**
 * Resolves the value for a dynamic variable. A dynamic variable extension
 * contributes a resolver which must implement this interface.
 * <p>
 * Clients contributing a dynamic variable are intended to provide an implementation
 * of this interface.
 * </p>
 * @since 3.0
 */
public interface IDynamicVariableResolver {

	/**
	 * Resolves and returns a value for the specified variable when referenced
	 * with the given argument, possibly <code>null</code>
	 *
	 * @param variable variable to resolve a value for
	 * @param argument argument present in expression or <code>null</code> if none
	 * @return variable value, possibly <code>null</code>
	 * @throws CoreException if unable to resolve a value for the given variable
	 */
	String resolveValue(IDynamicVariable variable, String argument) throws CoreException;
}
