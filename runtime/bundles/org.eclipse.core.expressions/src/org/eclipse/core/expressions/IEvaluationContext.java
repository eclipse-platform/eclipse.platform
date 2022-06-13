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
 * An evaluation context is used to manage a set of objects needed during
 * XML expression evaluation. A context has a parent context, can manage
 * a set of named variables and has a default variable. The default variable
 * is used during XML expression evaluation if no explicit variable is
 * referenced.
 * <p>
 * This interface is not intended to be implemented by clients. Clients
 * are allowed to instantiate <code>EvaluationContext</code>.
 * </p>
 *
 * @since 3.0
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IEvaluationContext {

	/**
	 * Represents the value used by variables that exist but are not defined
	 * in a evaluation context. When tested by the 'with' expression, <code>false</code>
	 * will be returned.
	 *
	 * @since 3.4
	 */
	Object UNDEFINED_VARIABLE = new Object();

	/**
	 * Returns the parent context or <code>null</code> if
	 * this is the root of the evaluation context hierarchy.
	 *
	 * @return the parent evaluation context or <code>null</code>
	 */
	IEvaluationContext getParent();

	/**
	 * Returns the root evaluation context.
	 *
	 * @return the root evaluation context
	 */
	IEvaluationContext getRoot();

	/**
	 * Specifies whether this evaluation context allows activation
	 * of plug-ins for testers used in the expression tree. To actual
	 * trigger the plug-in loading this flag has to be set to <code>
	 * true</code> and the actual test expression must have the
	 * attribute <code>forcePluginActivation</code> set to <code>
	 * true</code> as well.
	 *
	 * @param value whether this evaluation context allows plug-in activation
	 * @since 3.2
	 */
	void setAllowPluginActivation(boolean value);

	/**
	 * Returns whether this evaluation context supports plug-in
	 * activation. If not set via {@link #setAllowPluginActivation(boolean)}
	 * the parent value is returned. If no parent is set <code>false</code>
	 * is returned.
	 *
	 * @return whether plug-in activation is supported or not
	 * @since 3.2
	 */
	boolean getAllowPluginActivation();

	/**
	 * Returns the default variable.
	 *
	 * @return the default variable or <code>null</code> if
	 *  no default variable is managed.
	 */
	Object getDefaultVariable();

	/**
	 * Adds a new named variable to this context. If a variable
	 * with the name already exists the new one overrides the
	 * existing one.
	 *
	 * @param name the variable's name
	 * @param value the variable's value
	 */
	void addVariable(String name, Object value);

	/**
	 * Removes the variable managed under the given name
	 * from this evaluation context.
	 *
	 * @param name the variable's name
	 * @return the currently stored value or <code>null</code> if
	 *  the variable doesn't exist
	 */
	Object removeVariable(String name);

	/**
	 * Returns the variable managed under the given name.
	 *
	 * @param name the variable's name
	 * @return the variable's value or <code>null</code> if the content
	 *  doesn't manage a variable with the given name
	 */
	Object getVariable(String name);

	/**
	 * Resolves a variable for the given name and arguments. This
	 * method can be used to dynamically resolve variable such as
	 * plug-in descriptors, resources, etc. The method is used
	 * by the <code>resolve</code> expression.
	 *
	 * @param name the variable to resolve
	 * @param args an object array of arguments used to resolve the
	 *  variable
	 * @return the variable's value or <code>null</code> if no variable
	 *  can be resolved for the given name and arguments
	 * @exception CoreException if an errors occurs while resolving
	 *  the variable
	 */
	Object resolveVariable(String name, Object[] args) throws CoreException;
}