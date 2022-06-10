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
package org.eclipse.core.internal.expressions;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Represents the outcome of an expression evaluation. Status objects are
 * used inside {@link org.eclipse.core.runtime.CoreException} objects to
 * indicate what went wrong.
 *
 * @see org.eclipse.core.runtime.CoreException
 *
 * @since 3.0
 */
public class ExpressionStatus extends Status {

	/** Error code indicating that the variable in focus in not a collection */
	public static final int VARIABLE_IS_NOT_A_COLLECTION= 3;

	/** Error code indicating that the variable in focus in not a list */
	public static final int VARIABLE_IS_NOT_A_LIST= 4;

	/** Error code indicating that an attribute value doesn't present an integer */
	public static final int VALUE_IS_NOT_AN_INTEGER= 5;

	/** Error code indicating that a mandatory attribute is missing */
	public static final int MISSING_ATTRIBUTE= 50;

	/** Error code indicating that the value specified for an attribute is invalid */
	public static final int WRONG_ATTRIBUTE_VALUE= 51;

	/** Error code indicating that we are unable to find an expression */
	public static final int MISSING_EXPRESSION = 52;

	/** Error code indicating that the number of arguments passed to resolve variable is incorrect. */
	public static final int VARAIBLE_POOL_WRONG_NUMBER_OF_ARGUMENTS= 100;

	/** Error code indicating that the argument passed to resolve a variable is not of type java.lang.String */
	public static final int VARAIBLE_POOL_ARGUMENT_IS_NOT_A_STRING= 101;

	/** Error code indicating that a plugin providing a certain type extender isn't loaded yet */
	public static final int TYPE_EXTENDER_PLUGIN_NOT_LOADED= 200;

	/** Error indicating that a property referenced in a test expression can't be resolved */
	public static final int TYPE_EXTENDER_UNKOWN_METHOD= 201;

	/** Error code indicating that the implementation class of a type extender is not of type TypeExtender */
	public static final int TYPE_EXTENDER_INCORRECT_TYPE= 202;

	/** Error indicating that the value returned from a type extender isn't of type boolean */
	public static final int TEST_EXPRESSION_NOT_A_BOOLEAN= 203;

	/** Error indicating that the property attribute of the test element doesn't have a name space */
	public static final int NO_NAMESPACE_PROVIDED= 300;

	/** Error indicating that a variable accessed in a with expression isn't available in the evaluation context */
	public static final int VARIABLE_NOT_DEFINED= 301;

	/** Error indicating that in a string passed via a arg attribute the apostrophe character isn't correctly escaped */
	public static final int STRING_NOT_CORRECT_ESCAPED= 302;

	/** Error indicating that a string passed via a arg attribute isn't correctly terminated with an apostrophe */
	public static final int STRING_NOT_TERMINATED= 303;

	/**
	 * Creates a new expression status.
	 *
	 * @param errorCode the error code of the status
	 * @param message a human-readable message, localized to the current locale
	 */
	public ExpressionStatus(int errorCode, String message) {
		this(errorCode, message, null);
	}

	/**
	 * Creates a new expression status.
	 *
	 * @param errorCode the error code of the status
	 * @param message a human-readable message, localized to the current locale
	 * @param exception a low-level exception, or <code>null</code> if not applicable
	 */
	public ExpressionStatus(int errorCode, String message, Throwable exception) {
		super(IStatus.ERROR, ExpressionStatus.class, errorCode, message, exception);
	}
}
