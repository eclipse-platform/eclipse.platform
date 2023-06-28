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
 * Registry for string variables.
 * @since 3.0
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IStringVariableManager {

	/**
	 * Simple identifier constant (value <code>"dynamicVariables"</code>) for the
	 * dynamic variables extension point.
	 */
	String EXTENSION_POINT_DYNAMIC_VARIABLES = "dynamicVariables"; //$NON-NLS-1$

	/**
	 * Simple identifier constant (value <code>"valueVariables"</code>) for the
	 * value variables extension point.
	 */
	String EXTENSION_POINT_VALUE_VARIABLES = "valueVariables"; //$NON-NLS-1$

	/**
	 * Returns all registered variables.
	 *
	 * @return a collection of all registered variables
	 */
	IStringVariable[] getVariables();

	/**
	 * Returns all registered value variables.
	 *
	 * @return a collection of all registered value variables
	 */
	IValueVariable[] getValueVariables();

	/**
	 * Returns the value variable with the given name, or <code>null</code>
	 * if none.
	 *
	 * @param name variable name
	 * @return the value variable with the given name, or <code>null</code>
	 * if none
	 */
	IValueVariable getValueVariable(String name);

	/**
	 * Returns all registered dynamic variables.
	 *
	 * @return a collection of all registered dynamic variables
	 */
	IDynamicVariable[] getDynamicVariables();

	/**
	 * Returns the dynamic variable with the given name or <code>null</code>
	 * if none.
	 *
	 * @param name variable name
	 * @return the dynamic variable with the given name or <code>null</code>
	 * if none
	 */
	IDynamicVariable getDynamicVariable(String name);

	/**
	 * Returns the plug-in identifier of the plug-in that contributed the
	 * given variable via extension or <code>null</code> if the given
	 * variable wasn't contributed via extension.
	 *
	 * @param variable the variable
	 * @return the plug-in identifier of the plug-in that contributed the
	 *  given variable or <code>null</code>
	 * @since 3.1
	 */
	String getContributingPluginId(IStringVariable variable);

	/**
	 * Recursively resolves and replaces all variable references in the given
	 * expression with their corresponding values. Reports errors for references
	 * to undefined variables (equivalent to calling
	 * <code>performStringSubstitution(expression, true)</code>).
	 *
	 * @param expression expression referencing variables
	 * @return expression with variable references replaced with variable values
	 * @throws CoreException if unable to resolve the value of one or more variables
	 */
	String performStringSubstitution(String expression) throws CoreException;

	/**
	 * Recursively resolves and replaces all variable references in the given
	 * expression with their corresponding values. Allows the client to control
	 * whether references to undefined variables are reported as an error (i.e.
	 * an exception is thrown).
	 *
	 * @param expression expression referencing variables
	 * @param reportUndefinedVariables whether a reference to an undefined variable
	 *  is to be considered an error (i.e. throw an exception)
	 * @return expression with variable references replaced with variable values
	 * @throws CoreException if unable to resolve the value of one or more variables
	 */
	String performStringSubstitution(String expression, boolean reportUndefinedVariables) throws CoreException;

	/**
	 * Validates variables references in the given expression and reports errors
	 * for references to undefined variables.
	 *
	 * @param expression expression referencing variables
	 * @throws CoreException if one or more referenced variables do not exist
	 */
	void validateStringVariables(String expression) throws CoreException;

	/**
	 * Returns a new read-write value variable with the given name and description
	 * with a <code>null</code> value.
	 *
	 * @param name variable name, cannot be <code>null</code>
	 * @param description variable description, possibly <code>null</code>
	 * @return a new value variable
	 */
	IValueVariable newValueVariable(String name, String description);

	/**
	 * Returns a new value variable with the given properties.
	 *
	 * @param name variable name, cannot be <code>null</code>
	 * @param description variable description, possibly <code>null</code>
	 * @param readOnly whether this variable is to be a read only variable
	 * @param value the string value to initialize this variable to - should
	 * 	not be <code>null</code> for read-only variables
	 * @return a new value variable
	 * @since 3.3
	 */
	IValueVariable newValueVariable(String name, String description, boolean readOnly, String value);

	/**
	 * Adds the given variables to the variable registry.
	 *
	 * @param variables the variables to add
	 * @throws CoreException if one or more variables to add has a name collision with
	 *  an existing variable
	 */
	void addVariables(IValueVariable[] variables) throws CoreException;

	/**
	 * Removes the given variables from the registry. Has no effect for unregistered
	 * variables.
	 *
	 * @param variables variables to remove
	 */
	void removeVariables(IValueVariable[] variables);

	/**
	 * Registers the given listener for value variable notifications. Has no effect
	 * if an identical listener is already registered.
	 *
	 * @param listener value variable listener to add
	 */
	void addValueVariableListener(IValueVariableListener listener);

	/**
	 * Removes the given listener from the list of registered value variable
	 * listeners. Has no effect if an identical listener is not already registered.
	 *
	 * @param listener value variable listener to remove
	 */
	void removeValueVariableListener(IValueVariableListener listener);

	/**
	 * Convenience method that returns an expression referencing the given
	 * variable and optional argument. For example, calling the method with
	 * a <code>varName</code> of <code>my_var</code> and an <code>argument</code>
	 * of <code>my_arg</code> results in the string <code>$(my_var:my_arg}</code>.
	 *
	 * @param varName variable name
	 * @param arg argument text or <code>null</code>
	 * @return an expression referencing the given variable and
	 *  optional argument
	 */
	String generateVariableExpression(String varName, String arg);

}
