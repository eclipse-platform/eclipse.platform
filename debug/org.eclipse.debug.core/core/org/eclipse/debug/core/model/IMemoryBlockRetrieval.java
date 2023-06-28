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
 * Supports the retrieval of arbitrary blocks of memory.
 *
 * @see IMemoryBlock
 * @since 2.0
 */
public interface IMemoryBlockRetrieval {

	/**
	 * Returns whether this debug target supports the retrieval
	 * of memory blocks.
	 *
	 * @return whether this debug target supports the retrieval
	 *  of memory blocks
	 */
	boolean supportsStorageRetrieval();

	/**
	 * Returns a memory block that starts at the specified
	 * memory address, with the specified length.
	 *
	 * @param startAddress starting address
	 * @param length length of the memory block in bytes
	 * @return a memory block that starts at the specified
	 *  memory address, with the specified length
	 * @exception DebugException if this method fails.  Reasons include:
	 * <ul><li>Failure communicating with the debug target.  The DebugException's
	 * status code contains the underlying exception responsible for
	 * the failure.</li>
	 * <li>This debug target does not support memory block retrieval</li>
	 * <li>The specified address and length are not within valid
	 *  ranges</li>
	 * </ul>
	 */
	IMemoryBlock getMemoryBlock(long startAddress, long length) throws DebugException;
}

