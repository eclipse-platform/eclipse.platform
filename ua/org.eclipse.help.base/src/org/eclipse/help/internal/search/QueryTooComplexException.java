/*******************************************************************************
 * Copyright (c) 2003, 2007 IBM Corporation and others.
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
package org.eclipse.help.internal.search;

/**
 * Thrown to save resources, and not perform search if search query seems too
 * complicated.
 */
public class QueryTooComplexException extends RuntimeException {
	private static final long serialVersionUID = 1L;
}
