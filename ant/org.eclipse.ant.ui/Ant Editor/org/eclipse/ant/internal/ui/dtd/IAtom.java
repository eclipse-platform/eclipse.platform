/*******************************************************************************
 * Copyright (c) 2002, 2005 Object Factory Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *		Object Factory Inc. - Initial implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.dtd;

/**
 * Schema atom interface.
 *
 * @author Bob Foster
 */
public interface IAtom {

	public static final int ELEMENT = 0;
	public static final int ATTRIBUTE = 1;

	/**
	 * Return the atom name.
	 */
	public String getName();

	/**
	 * Return the atom name.
	 */
	@Override
	public String toString();
}
