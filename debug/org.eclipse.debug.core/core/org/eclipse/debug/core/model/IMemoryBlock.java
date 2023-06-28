/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
 * A contiguous segment of memory in an execution context.
 * A memory block is represented by a starting memory address
 * and a length. Not all debug architectures support the retrieval
 * of memory blocks.
 * <p>
 * Clients may implement this interface.
 * </p>
 * @see IMemoryBlockRetrieval
 * @since 2.0
 */
public interface IMemoryBlock extends IDebugElement {

	/**
	 * Returns the start address of this memory block.
	 *
	 * @return the start address of this memory block
	 */
	long getStartAddress();

	/**
	 * Returns the length of this memory block in bytes.
	 *
	 * @return the length of this memory block in bytes
	 */
	long getLength();

	/**
	 * Returns the values of the bytes currently contained
	 * in this this memory block.
	 *
	 * @return the values of the bytes currently contained
	 *  in this this memory block
	 * @exception DebugException if this method fails.  Reasons include:
	 * <ul><li>Failure communicating with the debug target.  The DebugException's
	 * status code contains the underlying exception responsible for
	 * the failure.</li>
	 * </ul>
	 */
	byte[] getBytes() throws DebugException;

	/**
	 * Returns whether this memory block supports value modification
	 *
	 * @return whether this memory block supports value modification
	 */
	boolean supportsValueModification();

	/**
	 * Sets the value of the bytes in this memory block at the specified
	 * offset within this memory block to the specified bytes.
	 * The offset is zero based.
	 *
	 * @param offset the offset at which to set the new values
	 * @param bytes the new values
	 * @exception DebugException if this method fails.  Reasons include:
	 * <ul><li>Failure communicating with the debug target.  The DebugException's
	 * status code contains the underlying exception responsible for
	 * the failure.</li>
	 * <li>This memory block does not support value modification</li>
	 * <li>The specified offset is greater than or equal to the length
	 *   of this memory block, or the number of bytes specified goes
	 *   beyond the end of this memory block (index of out of range)</li>
	 * </ul>
	 */
	void setValue(long offset, byte[] bytes) throws DebugException;

}

