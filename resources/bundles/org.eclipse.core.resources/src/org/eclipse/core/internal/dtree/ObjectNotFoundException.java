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
package org.eclipse.core.internal.dtree;

/**
 * This exception is thrown when an attempt is made to reference a source tree
 * element that does not exist in the given tree.
 */
public class ObjectNotFoundException extends RuntimeException {
	/**
	 * All serializable objects should have a stable serialVersionUID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * ObjectNotFoundException constructor comment.
	 * @param s java.lang.String
	 */
	public ObjectNotFoundException(String s) {
		super(s);
	}
}
