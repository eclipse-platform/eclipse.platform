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
 * Schema attribute interface.
 *
 * @author Bob Foster
 */
public interface IAttribute extends IAtom {

	public static final String CDATA = "CDATA"; //$NON-NLS-1$
	public static final String ID = "ID"; //$NON-NLS-1$
	public static final String IDREF = "IDREF"; //$NON-NLS-1$
	public static final String IDREFS = "IDREFS"; //$NON-NLS-1$
	public static final String NMTOKEN = "NMTOKEN"; //$NON-NLS-1$
	public static final String NMTOKENS = "NMTOKENS"; //$NON-NLS-1$
	public static final String ENTITY = "ENTITY"; //$NON-NLS-1$
	public static final String ENTITIES = "ENTITIES"; //$NON-NLS-1$
	public static final String NOTATION = "NOTATION"; //$NON-NLS-1$
	public static final String ENUMERATION = "enumeration"; //$NON-NLS-1$

	/**
	 * @return the attribute type. This will be one of "CDATA", "ID", "IDREF", "IDREFS", "NMTOKEN", "NMTOKENS", "ENTITY", "ENTITIES", "NOTATION" or
	 *         "enumeration". The type is interned and may be compared to a constant with ==. For "NOTATION" or "enumeration", <code>getEnum()</code>
	 *         will return a list of values.
	 */
	public String getType();

	/**
	 *
	 * @return String[] or null if type is not "NOTATION" or "ENUMERATION".
	 */
	public String[] getEnum();

	/**
	 * Return the element the attribute is defined in.
	 */
	public IElement getElement();

	/**
	 * Return the default value or null if none.
	 */
	public String getDefault();

	/**
	 * Return true if the default value is fixed.
	 */
	public boolean isFixed();

	/**
	 * Return true if attribute is required, false if it is optional.
	 */
	public boolean isRequired();
}
