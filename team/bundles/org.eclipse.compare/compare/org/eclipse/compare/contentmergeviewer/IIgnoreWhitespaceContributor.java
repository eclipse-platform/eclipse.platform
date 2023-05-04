/*******************************************************************************
 * Copyright (c) 2023 SAP SE and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.contentmergeviewer;

/**
 * @since 3.9
 */
public interface IIgnoreWhitespaceContributor {
	/**
	 * Method is called when a whitespace is detected in the "ignore whitespace"
	 * action run in the compare view. Implementors of this method can specify if
	 * whitespace can be ignored or not (e.g. whitespace in literals should never be
	 * ignored and always shown as diff).
	 *
	 * @param lineNumber   line number in source code starting at number zero
	 * @param columnNumber column number of current line starting at number zero
	 * @return boolean
	 */
	public boolean isIgnoredWhitespace(int lineNumber, int columnNumber);
}
