/*******************************************************************************
 *  Copyright (c) 2021 John Dallaway and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     John Dallaway - initial implementation
 *******************************************************************************/
package org.eclipse.debug.tests.view.memory;

import java.math.BigInteger;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlockExtension;
import org.eclipse.debug.core.model.IMemoryBlockRetrieval;
import org.eclipse.debug.core.model.MemoryByte;

/**
 * Minimal memory block implementation for use with
 * {@link org.eclipse.debug.tests.view.memory.TableRenderingTests} only
 */
public class TableRenderingTestsMemoryBlock implements IMemoryBlockExtension {

	private int fAddressableSize;
	private byte[] fBytes;

	public TableRenderingTestsMemoryBlock(byte[] bytes, int addressableSize) {
		fBytes = bytes;
		fAddressableSize = addressableSize;
	}

	@Override
	public long getStartAddress() {
		return 0;
	}

	@Override
	public long getLength() {
		return fBytes.length;
	}

	@Override
	public byte[] getBytes() throws DebugException {
		return fBytes;
	}

	@Override
	public boolean supportsValueModification() {
		return false;
	}

	@Override
	public void setValue(long offset, byte[] bytes) throws DebugException {
	}

	@Override
	public String getModelIdentifier() {
		return null;
	}

	@Override
	public IDebugTarget getDebugTarget() {
		return null;
	}

	@Override
	public ILaunch getLaunch() {
		return null;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

	@Override
	public String getExpression() {
		return null;
	}

	@Override
	public BigInteger getBigBaseAddress() throws DebugException {
		return null;
	}

	@Override
	public BigInteger getMemoryBlockStartAddress() throws DebugException {
		return null;
	}

	@Override
	public BigInteger getMemoryBlockEndAddress() throws DebugException {
		return null;
	}

	@Override
	public BigInteger getBigLength() throws DebugException {
		return null;
	}

	@Override
	public int getAddressSize() throws DebugException {
		return 0;
	}

	@Override
	public boolean supportBaseAddressModification() throws DebugException {
		return false;
	}

	@Override
	public boolean supportsChangeManagement() {
		return false;
	}

	@Override
	public void setBaseAddress(BigInteger address) throws DebugException {
	}

	@Override
	public MemoryByte[] getBytesFromOffset(BigInteger unitOffset, long addressableUnits) throws DebugException {
		assert BigInteger.ZERO.equals(unitOffset);
		final MemoryByte[] memoryBytes = new MemoryByte[(int) (addressableUnits * getAddressableSize())];
		for (int n = 0; n < memoryBytes.length; n++) {
			memoryBytes[n] = new MemoryByte(fBytes[n]);
		}
		return memoryBytes;
	}

	@Override
	public MemoryByte[] getBytesFromAddress(BigInteger address, long units) throws DebugException {
		return null;
	}

	@Override
	public void setValue(BigInteger offset, byte[] bytes) throws DebugException {
	}

	@Override
	public void connect(Object client) {
	}

	@Override
	public void disconnect(Object client) {
	}

	@Override
	public Object[] getConnections() {
		return null;
	}

	@Override
	public void dispose() throws DebugException {
	}

	@Override
	public IMemoryBlockRetrieval getMemoryBlockRetrieval() {
		return null;
	}

	@Override
	public int getAddressableSize() throws DebugException {
		return fAddressableSize;
	}

}
