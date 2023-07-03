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
package org.eclipse.debug.core.model;

/**
 * An expression that can report errors which occurred during the
 * expression's evaluation.
 *
 * @since 3.0
 */
public interface IErrorReportingExpression extends IExpression {
	/**
	 * Returns whether this expression has errors to report. An expression
	 * can have errors if errors were generated the last time its value was
	 * computed
	 *
	 * @return whether this expression's result has errors
	 */
	boolean hasErrors();
	/**
	 * Returns this expression's error messages, if any. An expression can
	 * have errors if errors were generated the last time its value was
	 * computed.
	 *
	 * @return this expression's error messages
	 */
	String[] getErrorMessages();
}
