/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
 * Optional extension to <code>ILogicalStructureTypeDelegate</code> that allows
 * a logical structure type delegate to provide a description for a value.
 * This allows a logical structure type to provide logical structures and
 * descriptions for more than one type of value.
 * <p>
 * If a logical structure type delegate implements this interface, it will
 * be consulted for a description rather than using the description attribute
 * provided in plug-in XML.
 * </p>
 * <p>
 * Clients contributing logical structure types can implement this
 * interface.
 * </p>
 * @since 3.1
 * @see org.eclipse.debug.core.ILogicalStructureType
 */
public interface ILogicalStructureTypeDelegate2 {

	/**
	 * Returns a simple description of the logical structure provided by this
	 * structure type delegate, for the given value.
	 * Cannot return <code>null</code>. This method is only called if this
	 * logical structure type delegate returns <code>true</code> for
	 * <code>providesLogicalStructure(IValue)</code>.
	 *
	 * @param value a value a description is requested for
	 * @return a simple description of the logical structure provided by this
	 * structure type delegate, for the given value
	 */
	String getDescription(IValue value);

}
