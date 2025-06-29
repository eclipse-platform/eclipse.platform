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

public interface ISchema {

	/**
	 * Find element by name.
	 *
	 * @param qname
	 *            Element name.
	 * @return element or null if no such element.
	 */
	IElement getElement(String qname);

	/**
	 * @return IElement[] of all visible elements.
	 */
	IElement[] getElements();

	/**
	 * @return Exception thrown by parser when schema was built or <code>null</code> if none. Note that the exception does not necessarily mean the
	 *         schema is incomplete.
	 */
	Exception getErrorException();
}
