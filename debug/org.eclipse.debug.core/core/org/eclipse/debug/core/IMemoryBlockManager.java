/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
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

package org.eclipse.debug.core;

import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IMemoryBlockRetrieval;


/**
 * Manages registered memory blocks in the workspace. Clients
 * interested in notification of the addition and removal of
 * memory blocks may register as a memory block listener with
 * the memory block manager.
 * @see org.eclipse.debug.core.model.IMemoryBlock
 * @see org.eclipse.debug.core.IMemoryBlockListener
 * @since 3.1
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IMemoryBlockManager {

	/**
	 * Adds the given memory blocks to the memory block manager.
	 * Registered memory block listeners are notified of the additions.
	 * Has no effect on memory blocks that are already registered.
	 *
	 * @param memoryBlocks memory blocks to add
	 */
	void addMemoryBlocks(IMemoryBlock[] memoryBlocks);

	/**
	 * Removes the given memory blocks from the memory block manager.
	 * Registered memory block listeners are notified of the removals.
	 * Has no effect on memory blocks that are not currently registered.
	 *
	 * @param memoryBlocks memory blocks to remove
	 */
	void removeMemoryBlocks(IMemoryBlock[] memoryBlocks);

	/**
	 * Registers the given listener for memory block addition and
	 * removal notification. Has no effect if an identical listener
	 * is already registered.
	 *
	 * @param listener the listener to add
	 */
	void addListener(IMemoryBlockListener listener);

	/**
	 * Unregisters the given listener for memory block addition and
	 * removal notification. Has no effect if an identical listener
	 * is not already registered.
	 *
	 * @param listener the listener to remove
	 */
	void removeListener(IMemoryBlockListener listener);

	/**
	 * Returns all registered memory blocks.
	 *
	 * @return all registered memory blocks
	 */
	IMemoryBlock[] getMemoryBlocks();

	/**
	 * Returns all registered memory blocks associated with the
	 * given debug target. That is, all registered memory blocks
	 * whose <code>getDebugTarget()</code> method returns the
	 * specified debug target.
	 *
	 * @param debugTarget target for which memory blocks have been requested
	 * @return all registered memory blocks associated with the given debug
	 *  target
	 */
	IMemoryBlock[] getMemoryBlocks(IDebugTarget debugTarget);

	/**
	 * Returns all registered memory blocks that originated from the
	 * given memory retrieval source.
	 *
	 * @param source source for which memory blocks have been requested
	 * @return all registered memory blocks that originated from the
	 *  given memory retrieval source
	 */
	IMemoryBlock[] getMemoryBlocks(IMemoryBlockRetrieval source);

}
