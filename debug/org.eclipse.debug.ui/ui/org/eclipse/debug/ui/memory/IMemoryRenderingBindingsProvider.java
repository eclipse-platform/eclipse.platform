/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
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
package org.eclipse.debug.ui.memory;

import org.eclipse.debug.core.model.IMemoryBlock;


/**
 * A rendering bindings provider provides rendering type bindings for a memory block.
 * <p>
 * By default, bindings for a memory block are provided by the memory rendering
 * manager. However, a client can provide dynamic renderings for a memory block
 * by contributing a dynamic rendering binding in the <code>renderingBindings</code>
 * element of a <code>memoryRenderings</code> extension.
 * </p>
 * <p>
 * Clients contributing dynamic rendering bindings are intended to implement this
 * interface.
 * </p>
 * @since 3.1
 */
public interface IMemoryRenderingBindingsProvider {

	/**
	 * Returns all rendering types bound to the given memory block.
	 * This includes default and primary rendering types.
	 *
	 * @param block memory block
	 * @return all rendering types bound to the given memory block
	 */
	IMemoryRenderingType[] getRenderingTypes(IMemoryBlock block);

	/**
	 * Returns default rendering types bound to the given memory block,
	 * possibly empty.
	 *
	 * @param block memory block
	 * @return default rendering types bound to the given memory block,
	 * possibly empty
	 */
	IMemoryRenderingType[] getDefaultRenderingTypes(IMemoryBlock block);

	/**
	 * Returns the primary rendering type bound to the given memory block,
	 * or <code>null</code> if none.
	 *
	 * @param block memory block
	 * @return the primary rendering type bound to the given memory block,
	 * or <code>null</code> if none
	 */
	IMemoryRenderingType getPrimaryRenderingType(IMemoryBlock block);

	/**
	 * Adds a listener to this binding provider.  The listener will be notified
	 * when rendering bindings change.
	 * <p>
	 * Has no effect if an identical listener is already registered.
	 * </p>
	 * @param listener listener to add
	 */
	void addListener(IMemoryRenderingBindingsListener listener);

	/**
	 * Removes a listener from this binding provider.
	 * <p>
	 * Has no effect if an identical listener is not already registered.
	 * </p>
	 * @param listener listener to remove
	 */
	void removeListener(IMemoryRenderingBindingsListener listener);
}
