/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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

import org.eclipse.osgi.util.NLS;

public final class ExpressionMessages extends NLS {

	private static final String BUNDLE_NAME= "org.eclipse.core.internal.expressions.ExpressionMessages";//$NON-NLS-1$

	private ExpressionMessages() {
		// Do not instantiate
	}

	public static String Expression_attribute_missing;
	public static String Expression_attribute_invalid_value;
	public static String Expression_variable_not_a_collection;
	public static String Expression_variable_not_a_list;

	public static String Expression_variable_not_iterable;
	public static String Expression_variable_not_countable;

	public static String Expression_unknown_element;
	public static String Missing_Expression;
	public static String Expression_string_not_correctly_escaped;
	public static String Expression_string_not_terminated;

	public static String TypeExtender_unknownMethod;
	public static String TypeExtender_incorrectType;

	public static String TestExpression_no_name_space;

	public static String WithExpression_variable_not_defined;

	public static String ResolveExpression_variable_not_defined;

	public static String PropertyTesterDescriptor_no_namespace;
	public static String PropertyTesterDescritpri_no_properties;

	public static String ElementHandler_unsupported_element;

	static {
		NLS.initializeMessages(BUNDLE_NAME, ExpressionMessages.class);
	}
}
