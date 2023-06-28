/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
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

import java.math.BigInteger;

import org.eclipse.debug.core.DebugException;

/**
 * Extensions to {@link org.eclipse.debug.core.model.IMemoryBlock}. Allows
 * for bytes to be accessed in a larger address space, and for state information
 * to be provided for each byte.
 * <p>
 * Clients may optionally implement this interface when providing implementations of
 * {@link org.eclipse.debug.core.model.IMemoryBlock}.
 * </p>
 * @since 3.1
 * @see org.eclipse.debug.core.model.MemoryByte
 */
public interface IMemoryBlockExtension extends IMemoryBlock {

	/**
	 * Returns the expression used to create this memory block. An expression can
	 * be used as name for a memory block and typically represents an expression
	 * used to compute a base address for a memory block.
	 *
	 * @return the expression used to create this memory block
	 */
	String getExpression();

	/**
	 * Returns the base address of this memory block as a big integer. The
	 * address is in terms of addressable units.
	 *
	 * @return the base address of this memory block
	 * @throws DebugException if unable to retrieve the base address
	 */
	BigInteger getBigBaseAddress() throws DebugException;

	/**
	 * Returns the hard start address of this memory block as a big integer, or
	 * <code>null</code> if none. A <code>null</code> start address indicates that
	 * memory may be retrieved at any address less than this memory block's base
	 * address.
	 *
	 * @return the hard start address of this memory block or <code>null</code>
	 * @throws DebugException if unable to retrieve the start address of this memory block.
	 */
	BigInteger getMemoryBlockStartAddress() throws DebugException;

	/**
	 * Returns the hard end address of this memory block as a big integer, or
	 * <code>null</code> if none. A <code>null</code> end address indicates that
	 * memory may be retrieved from any positive offset relative to the base address
	 * of this memory block.
	 *
	 * @return the hard end address of this memory block or <code>null</code>
	 * @throws DebugException if unable to retrieve the end address of this memory block.
	 */
	BigInteger getMemoryBlockEndAddress() throws DebugException;

	/**
	 * Returns the length of this memory block, or -1 if unbounded.
	 * Returns "end address - start address"  for a memory block with a fixed
	 * length (i.e. when both start and end address are known).
	 * Returns -1 for an unbounded memory block (i.e. when start or end address is
	 * <code>null</code>).
	 *
	 * @return length of this memory block or -1 if unbounded
	 * @throws DebugException if unable to retrieve the length of this memory block.
	 */
	BigInteger getBigLength() throws DebugException;

	/**
	 * Returns the address size of this memory block in number of bytes. The address
	 * size indicates the number of bytes used to construct an address.
	 *
	 * @return address size in number of bytes
	 * @throws DebugException if unable to retrieve the address size
	 */
	int getAddressSize() throws DebugException;

	/**
	 * Returns whether the base address of this memory block can be modified.
	 *
	 * @return whether the base address of this memory block can be modified
	 * @throws DebugException is unable to determine if base address modification is supported
	 */
	boolean supportBaseAddressModification() throws DebugException;

	/**
	 * Returns whether this memory block manages the change state of its bytes.
	 * <p>
	 * If a memory block manages changes the memory block is responsible for
	 * setting the <code>CHANGED</code> state of its <code>MemoryByte</code>'s
	 * returned from <code>getBytesFromAddress</code> and
	 * <code>getBytesFromOffset</code>. The changed state of a byte should
	 * be updated each time a thread suspends in a memory block's target.
	 * </p>
	 * @return whether this memory block manages the change state of its bytes
	 */
	boolean supportsChangeManagement();

	/**
	 * Sets the base address of this memory block to the given address.
	 *
	 * @param address new base address
	 * @throws DebugException if unable to modify the base address, or modification
	 *  of the base address fails
	 */
	void setBaseAddress(BigInteger address) throws DebugException;

	/**
	 * Returns bytes from this memory block based on the base address and
	 * addressable size of this memory block.
	 * <p>
	 * A memory block may be asked to retrieve bytes beyond it's start
	 * or end address. If a memory block is unable to retrieve memory outside
	 * these boundaries, implementations should return memory bytes with
	 * the <code>READABLE</code> bit turned off for each byte outside
	 * the of the accessible range. An exception should not be thrown in this
	 * case.
	 * </p>
	 * @param unitOffset zero based offset into this memory block at which to start
	 *  retrieving bytes in terms of addressable units. Client should retrieve
	 *  memory starting from "base address + offset".
	 * @param addressableUnits the number of addressable units to retrieve
	 * @return an array of bytes from this memory block based on the given offset
	 *  and number of units. The size of the array returned must to be equal to
	 *  <code>units</code> * <code>getAddressableSize()</code>.
	 * @throws DebugException if unable to retrieve the specified bytes due to
	 *  a failure communicating with the target
	 * @see MemoryByte
	 */
	MemoryByte[] getBytesFromOffset(BigInteger unitOffset, long addressableUnits) throws DebugException;

	/**
	 * Returns bytes from this memory block based on the given address and the
	 * addressable size of this memory block.
	 * <p>
	 * A memory block may be asked to retrieve bytes beyond it's start
	 * or end address. If a memory block is unable to retrieve memory outside
	 * these boundaries, implementations should return memory bytes with
	 * the <code>READABLE</code> bit turned off for each byte outside
	 * the of the accessible range. An exception should not be thrown in this
	 * case.
	 * </p>
	 * @param address address at which to begin retrieving bytes in terms
	 *  of addressable units
	 * @param units is the number of addressable units of memory to retrieve
	 * @return an array of bytes from this memory block based on the given address
	 *  and number of units. The size of the array returned must to be equal to
	 *  <code>units</code> * <code>getAddressableSize()</code>.
	 * @throws DebugException if unable to retrieve the specified bytes due to
	 *  a failure communicating with the target
	 * @see MemoryByte
	 */
	MemoryByte[] getBytesFromAddress(BigInteger address, long units) throws DebugException;

	/**
	 * Sets bytes in this memory block at the specified offset within this memory block to
	 * the specified bytes. The offset is zero based. After successfully modifying the
	 * specified bytes, a debug event should be fired with a kind of <code>CHANGE</code>.
	 *
	 * @param offset the zero based offset at which to set the new value.  Modify
	 * the memory starting from base address + offset.
	 * @param bytes replacement bytes
	 * @exception DebugException if this method fails.  Reasons include:
	 * <ul><li>Failure communicating with the debug target.  The DebugException's
	 * status code contains the underlying exception responsible for
	 * the failure.</li>
	 * <li>This memory block does not support value modification</li>
	 * <li>The specified offset is greater than or equal to the length
	 *   of this memory block, or the number of bytes specified goes
	 *   beyond the end of this memory block (index of out of range)</li>
	 * </ul>
	 * @see org.eclipse.debug.core.DebugEvent
	 */
	void setValue(BigInteger offset, byte[] bytes) throws DebugException;

	/**
	 * Connects the given client to this memory block. Allows a memory block
	 * to know when it is being monitored. Has no effect if an identical
	 * client is already connected.
	 * <p>
	 * Memory blocks supporting change management may selectively turn off
	 * change management when no clients are connected, for reasons of
	 * efficiency. Clients that require access to change state information
	 * are required to connect to a memory block before change information
	 * is considered to be valid.
	 * </p>
	 * @param client the client to connect
	 */
	void connect(Object client);

	/**
	 * Disconnects the given client from this memory block. Has no effect if
	 * an identical client is not already connected.
	 *
	 * @param client the client to disconnect
	 */
	void disconnect(Object client);

	/**
	 * Returns the possibly empty list of clients currently connected to this
	 * memory block.
	 *
	 * @return the possibly empty list of clients currently connected to this
	 * memory block
	 */
	Object[] getConnections();

	/**
	 * Dispose this memory block. Connected clients are disconnected.
	 * @throws DebugException if the memory block cannot be disposed.
	 */
	void dispose() throws DebugException;

	/**
	 * Returns the origin of this memory block.
	 *
	 * @return the origin of this memory block
	 */
	IMemoryBlockRetrieval getMemoryBlockRetrieval();

	/**
	 * Returns this memory block's addressable size in number of bytes. The addressable size
	 * of memory block indicates the minimum number of bytes that can be retrieved as
	 * a single unit.
	 *
	 * @return this memory block's addressable size
	 * @throws DebugException if the addressable size cannot be obtained.
	 */
	int getAddressableSize() throws DebugException;
}
