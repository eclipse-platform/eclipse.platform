/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
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
package org.eclipse.debug.examples.internal.memory.core;

import java.math.BigInteger;
import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlockExtension;
import org.eclipse.debug.core.model.IMemoryBlockRetrieval;
import org.eclipse.debug.core.model.IMemoryBlockRetrievalExtension;
import org.eclipse.debug.core.model.MemoryByte;
import org.eclipse.debug.examples.internal.memory.MemoryViewSamplePlugin;
import org.eclipse.debug.examples.internal.memory.launchconfig.SampleModelPresentation;
import org.eclipse.jface.viewers.IColorProvider;

/**
 * Memory Block Implementation
 *
 */
public class SampleMemoryBlock extends DebugElement implements IMemoryBlockExtension {

	private String fExpression;
	private SampleDebugTarget fDebugTarget;

	private boolean isEnabled = true;
	private BigInteger fBaseAddress;

	private ArrayList<Object> fConnections = new ArrayList<>();

	/**
	 * Creates memory block
	 *
	 * @param debugTarget
	 * @param expression
	 * @param address
	 */
	public SampleMemoryBlock(SampleDebugTarget debugTarget, String expression, BigInteger address) {
		super(debugTarget);
		fDebugTarget = debugTarget;
		fExpression = expression;
		fBaseAddress = address;
	}

	@Override
	public BigInteger getBigBaseAddress() throws DebugException {
		fBaseAddress = fDebugTarget.getEngine().evaluateExpression(fExpression, null);
		return fBaseAddress;
	}

	@Override
	public boolean supportBaseAddressModification() throws DebugException {
		return fDebugTarget.getEngine().suppostsBaseAddressModification(this);
	}

	@Override
	public void setBaseAddress(BigInteger address) throws DebugException {
		try {
			fDebugTarget.getEngine().setBaseAddress(this, address);
		} catch (CoreException e) {
			throw new DebugException(e.getStatus());
		}
	}

	@Override
	synchronized public MemoryByte[] getBytesFromOffset(BigInteger offset, long length) throws DebugException {
		BigInteger address = fBaseAddress.subtract(offset);
		return getBytesFromAddress(address, length);
	}

	@Override
	public MemoryByte[] getBytesFromAddress(BigInteger address, long length) throws DebugException {

		try {
			MemoryByte[] bytes = new MemoryByte[(int) length * fDebugTarget.getEngine().getAddressableSize()];
			BigInteger addressCnt = address;
			int lengthCnt = (int) length;
			int i = 0;

			// asks engine to get bytes from address
			MemoryByte[] engineBytes = fDebugTarget.getEngine().getBytesFromAddress(addressCnt, lengthCnt);
			System.arraycopy(engineBytes, 0, bytes, i, engineBytes.length);

			// if engine did not return enough memory, pad with dummy memory
			for (int j = i + engineBytes.length; j < bytes.length; j++) {
				MemoryByte mb = new MemoryByte((byte) 0);
				mb.setReadable(false);
				mb.setWritable(false);
				mb.setBigEndian(fDebugTarget.getEngine().isBigEndian(address.add(BigInteger.valueOf(j))));
				bytes[j] = mb;
			}

			return bytes;
		} catch (RuntimeException e) {
			throw e;
		}
	}

	@Override
	public void connect(Object object) {

		if (!fConnections.contains(object)) {
			fConnections.add(object);
		}

		if (fConnections.size() == 1) {
			enable();
		}
	}

	/**
	 * Enable this memory block
	 */
	private void enable() {
		isEnabled = true;
	}

	@Override
	public void disconnect(Object object) {

		if (fConnections.contains(object)) {
			fConnections.remove(object);
		}

		if (fConnections.isEmpty()) {
			disable();
		}
	}

	@Override
	public Object[] getConnections() {
		return fConnections.toArray();
	}

	/**
	 * Disable this memory block
	 */
	private void disable() {
		isEnabled = false;
	}

	@Override
	public long getStartAddress() {
		// no need to implement this method as it belongs to IMemoryBlock
		return 0;
	}

	@Override
	public long getLength() {
		// no need to implement this method as it belongs to IMemoryBlock
		return 0;
	}

	@Override
	public byte[] getBytes() throws DebugException {
		// no need to implement this method as it belongs to IMemoryBlock
		return new byte[0];
	}

	@Override
	public boolean supportsValueModification() {
		return fDebugTarget.getEngine().supportsValueModification(this);
	}

	@Override
	public void setValue(BigInteger offset, byte[] bytes) throws DebugException {
		try {
			// ask the engine to modify memory at specified address
			fDebugTarget.getEngine().setValue(fBaseAddress.add(offset), bytes);
			fireContentChangeEvent();
		} catch (RuntimeException e) {
			IStatus status = new Status(IStatus.ERROR, MemoryViewSamplePlugin.PLUGIN_ID, 0, Messages.SampleMemoryBlock_0, e);
			DebugException exception = new DebugException(status);
			throw exception;
		}
	}

	@Override
	public String getModelIdentifier() {
		return getDebugTarget().getModelIdentifier();
	}

	@Override
	public IDebugTarget getDebugTarget() {
		return fDebugTarget;
	}

	@Override
	public ILaunch getLaunch() {
		return fDebugTarget.getLaunch();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {

		if (adapter.equals(IMemoryBlockRetrievalExtension.class)) {
			return (T) getDebugTarget();
		}

		if (adapter == IColorProvider.class) {
			return (T) SampleModelPresentation.getSampleModelPresentation();
		}

		return super.getAdapter(adapter);
	}

	@Override
	public String getExpression() {
		return fExpression;
	}

	@Override
	public void dispose() throws DebugException {
		// remove this memory block from debug target
		fDebugTarget.removeMemoryBlock(this);
	}

	/**
	 * @return is enabled
	 */
	public boolean isEnabled() {
		return isEnabled;
	}

	@Override
	public IMemoryBlockRetrieval getMemoryBlockRetrieval() {
		return getDebugTarget();
	}

	private void fireContentChangeEvent() {
		DebugEvent evt = new DebugEvent(this, DebugEvent.CHANGE);
		fireEvent(evt);
	}

	@Override
	public boolean supportsChangeManagement() {
		return false;
	}

	@Override
	public int getAddressableSize() throws DebugException {
		return fDebugTarget.getEngine().getAddressableSize();
	}

	@Override
	public int getAddressSize() throws DebugException {
		try {
			return fDebugTarget.getEngine().getAddressSize();
		} catch (CoreException e) {
			throw new DebugException(e.getStatus());
		}
	}

	@Override
	public BigInteger getMemoryBlockStartAddress() throws DebugException {

		// if (true)
		// return fBaseAddress.subtract(BigInteger.valueOf(250));
		// Return null by default.
		// Null is acceptable if default start address is to be used.
		// Default is 0.
		return null;
	}

	@Override
	public BigInteger getMemoryBlockEndAddress() throws DebugException {

		// if (true)
		// return fBaseAddress.add(BigInteger.valueOf(250));
		// Return null by default.
		// Null is accpetable if default end address is to be used.
		// Default end address is calculated based on address size.
		return null;
	}

	@Override
	public void setValue(long offset, byte[] bytes) throws DebugException {
		// do not need to implement for IMemoryBlockExtension
	}

	@Override
	public BigInteger getBigLength() throws DebugException {
		// return -1 by default and default length is calculated
		return BigInteger.valueOf(-1);
	}
}
