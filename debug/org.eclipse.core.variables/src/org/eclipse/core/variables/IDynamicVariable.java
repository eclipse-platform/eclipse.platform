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
package org.eclipse.core.variables;

import org.eclipse.core.runtime.CoreException;

/**
 * A dynamic variable is a variable whose value is computed dynamically by a
 * resolver at the time a string substitution is performed. A dynamic variable
 * is contributed by an extension.
 * <p>
 * The following is a definition of a dynamic variable that resolves to the name
 * of the selected resource:
 * </p>
 *
 * <pre>
 *  &lt;extension point="org.eclipse.core.variables.dynamicVariables"&gt;
 *    &lt;variable
 *       name="resource_name"
 *       resolver="com.example.ResourceNameResolver"
 *       description="The name of the selected resource"
 *       supportsArgument="false"&gt;
 *    &lt;/variable&gt;
 *  &lt;/extension&gt;
 * </pre>
 * <p>
 * Clients contributing a dynamic variable provide an implementation of
 * {@link org.eclipse.core.variables.IDynamicVariableResolver}.
 * </p>
 *
 * @since 3.0
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IDynamicVariable extends IStringVariable {

	/**
	 * Returns the value of this variable when referenced with the given
	 * argument, possibly <code>null</code>.
	 *
	 * @param argument argument present in variable expression or <code>null</code>
	 *   if none
	 * @return value of this variable when referenced with the given argument, possibly
	 *   <code>null</code>
	 * @throws CoreException if unable to resolve a value for this variable
	 */
	String getValue(String argument) throws CoreException;

	/**
	 * Returns whether this variable supports an argument, as specified
	 * by this variable's extension definition in plug-in XML.
	 *
	 * @return whether this variable supports an argument
	 */
	boolean supportsArgument();
}
