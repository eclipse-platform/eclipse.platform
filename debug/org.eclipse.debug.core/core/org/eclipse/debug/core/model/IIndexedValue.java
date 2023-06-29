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

import org.eclipse.debug.core.DebugException;

/**
 * A value containing an indexed collection of variables - for example,
 * an array.
 * <p>
 * The indexed collection value has been added to the debug model to support
 * automatic partitioning of large arrays in the debug UI. Clients are not required
 * to implement this interface for values representing indexed collections,
 * however, doing so will provide enhanced display options in the debug UI.
 * </p>
 * <p>
 * Clients may implement this interface.
 * </p>
 * @since 3.0
 */
public interface IIndexedValue extends IValue {

	/**
	 * Returns the variable at the given offset in this collection.
	 * The offset is zero based.
	 * @param offset zero based offset into this collection
	 * @return returns the variable in this collection at the given
	 *  offset
	 * @throws DebugException if unable to retrieve the variable at the
	 * given offset
	 */
	IVariable getVariable(int offset) throws DebugException;

	/**
	 * Returns a subset of the elements in this collection of variables as
	 * specified by the given offset and length.
	 *
	 * @param offset beginning offset of the subset of elements to return
	 * @param length the number of elements to return
	 * @return a subset of the elements in this collection of variables as
	 *  specified by the given offset and length
	 * @throws DebugException if unable to retrieve the variables
	 */
	IVariable[] getVariables(int offset, int length) throws DebugException;

	/**
	 * Returns the number of entries in this indexed collection.
	 *
	 * @return the number of entries in this indexed collection
	 * @throws DebugException if unable to determine the number
	 * of entries in this collection
	 */
	int getSize() throws DebugException;

	/**
	 * Returns the index of the first variable contained in this value.
	 * Generally, indexed values are zero based, but this allows for
	 * an arbitrary base offset.
	 *
	 * @return the index of the first variable contained in this value
	 */
	int getInitialOffset();
}
