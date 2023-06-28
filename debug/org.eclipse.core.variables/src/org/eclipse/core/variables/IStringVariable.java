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


/**
 * A variable that can be referenced in an expression, which resolves to a
 * string value. Variables are referenced in expressions via their name, in the
 * following format.
 *
 * <pre>
 * ${varname} or ${varname:argument}
 * </pre>
 * <p>
 * A variable is identified by its name, and optionally accepts an argument.
 * When an argument is present, a colon separates the variable name from its
 * argument.
 * </p>
 * <p>
 * Variables can be contributed by extensions or programmatically. There are two
 * kinds of variables.
 * </p>
 * <ul>
 * <li><code>IValueVariable</code> - variables that have a value (with getter
 * and setter), and accept no arguments. The value of this type of variable is
 * resolved at the time its value is set via its setter API.</li>
 * <li><code>IDynamicVariable</code> - variables whose value is resolved at the
 * time a string substitution is performed by a contributed resolver. Dynamic
 * variables may accept an argument.</li>
 * </ul>
 *
 * @since 3.0
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IStringVariable {

	/**
	 * Returns the name of this variable. A variable is uniquely identified by
	 * its name.
	 *
	 * @return variable name
	 */
	String getName();

	/**
	 * Returns a human readable description of this variable, possibly <code>null</code>
	 *
	 * @return a description of this variable, or <code>null</code> if none
	 */
	String getDescription();

}
