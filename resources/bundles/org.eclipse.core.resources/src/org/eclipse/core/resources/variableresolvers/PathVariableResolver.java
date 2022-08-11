/*******************************************************************************
 * Copyright (c) 2008, 2010 Freescale Semiconductor and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Freescale Semiconductor - initial API and implementation
 *     IBM Corporation - ongoing development
 *******************************************************************************/
package org.eclipse.core.resources.variableresolvers;

import org.eclipse.core.resources.IResource;

/**
 * An interface that variable providers should implement in order
 * to extends the default path variable list used to resolve relative
 * locations of linked resources.
 * @since 3.6
 */
public abstract class PathVariableResolver {

	/**
	 * This method can return a list of possible variables resolved by
	 * this resolver.
	 * <p>
	 * This default implementation always returns <code>null</code>. Subclasses
	 * should override to provide custom extensions.
	 * </p>
	 *
	 * @param variable
	 *            The current variable name.
	 * @param resource
	 *            The resource that the variable is being resolved for.
	 * @return the list of supported variables
	 */
	public String[] getVariableNames(String variable, IResource resource) {
		return null;
	}

	/**
	 * Returns a variable value
	 *
	 * @param variable
	 *            The current variable name.
	 * @param resource
	 *            The resource that the variable is being resolved for.
	 * @return the variable value.
	 */
	public abstract String getValue(String variable, IResource resource);
}