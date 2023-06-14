/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.views.memory.renderings;

import java.math.BigInteger;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IMemoryBlockExtension;
import org.eclipse.debug.internal.ui.memory.provisional.AbstractAsyncTableRendering;
import org.eclipse.debug.ui.memory.AbstractTableRendering;
import org.eclipse.debug.ui.memory.IMemoryRendering;

/**
 * This is an internal class for storing information about the content
 * in the table viewer.
 */
public class TableRenderingContentInput extends PlatformObject {

	private IMemoryRendering fRendering;
	private int fPreBuffer;					// number of lines before the top visible line
	private int fPostBuffer;				// number of lines after thes last visible line
	private BigInteger fLoadAddress;		// Top address to load at the table
	private int fNumLines;					// number of visible lines
	private boolean fUpdateDelta;			// should the content provider calculate delta info
	private BigInteger fMemoryBlockBaseAddress;		// base address of the memory block when this input is set
	private BigInteger fStartAddress;
	private BigInteger fEndAddress;

	public TableRenderingContentInput(IMemoryRendering rendering, int preBuffer, int postBuffer, BigInteger loadAddress, int numOfLines, boolean updateDelta, BigInteger contentBaseAddress)
	{
		fRendering = rendering;
		fPreBuffer = preBuffer;
		fPostBuffer = postBuffer;
		fLoadAddress = loadAddress;
		fNumLines = numOfLines;
		fUpdateDelta = updateDelta;

		if (contentBaseAddress == null)
		{
			try {
				updateContentBaseAddress();
			} catch (DebugException e) {
			}
		}
		else
		{
			fMemoryBlockBaseAddress = contentBaseAddress;
		}
	}

	public int getPostBuffer() {
		return fPostBuffer;
	}
	public int getPreBuffer() {
		return fPreBuffer;
	}
	public BigInteger getLoadAddress() {
		return fLoadAddress;
	}

	public IMemoryBlock getMemoryBlock()
	{
		return fRendering.getMemoryBlock();
	}
	public void setPostBuffer(int postBuffer) {
		fPostBuffer = postBuffer;
	}
	public void setPreBuffer(int preBuffer) {
		fPreBuffer = preBuffer;
	}
	public boolean isUpdateDelta() {
		return fUpdateDelta;
	}
	public void setUpdateDelta(boolean updateDelta) {
		fUpdateDelta = updateDelta;
	}

	public void setLoadAddress(BigInteger address)
	{
		fLoadAddress = address;
	}
	public BigInteger getContentBaseAddress() {

		if (fMemoryBlockBaseAddress == null)
		{
			try {
				updateContentBaseAddress();
			} catch (DebugException e) {
				fMemoryBlockBaseAddress = BigInteger.ZERO;
			}
		}

		return fMemoryBlockBaseAddress;
	}

	public void updateContentBaseAddress() throws DebugException {
		IMemoryBlock memoryBlock = fRendering.getMemoryBlock();
		if (memoryBlock instanceof IMemoryBlockExtension) {
			fMemoryBlockBaseAddress = ((IMemoryBlockExtension)memoryBlock).getBigBaseAddress();
		} else {
			fMemoryBlockBaseAddress = BigInteger.valueOf(memoryBlock.getStartAddress());
		}
	}

	/**
	 * @return start address of the memory block
	 */
	public BigInteger getStartAddress()
	{
		if (fStartAddress == null)
		{
			try {
				IMemoryBlock memoryBlock = fRendering.getMemoryBlock();
				if(memoryBlock instanceof IMemoryBlockExtension)
				{
					BigInteger startAddress = ((IMemoryBlockExtension)memoryBlock).getMemoryBlockStartAddress();
					if (startAddress != null) {
						fStartAddress =  startAddress;
					}
				}
			} catch (DebugException e) {
				// default to 0 if we have trouble getting the start address
				fStartAddress =  BigInteger.ZERO;
			}

			if (fStartAddress == null) {
				fStartAddress =  BigInteger.ZERO;
			}
		}
		return fStartAddress;
	}

	/**
	 * @return end address of the memory block
	 */
	public BigInteger getEndAddress()
	{
		if (fEndAddress == null)
		{
			IMemoryBlock memoryBlock = fRendering.getMemoryBlock();
			if(memoryBlock instanceof IMemoryBlockExtension)
			{
				BigInteger endAddress;
				try {
					endAddress = ((IMemoryBlockExtension)memoryBlock).getMemoryBlockEndAddress();
					if (endAddress != null) {
						fEndAddress = endAddress;
					}
				} catch (DebugException e) {
					fEndAddress = null;
				}

				if (fEndAddress == null)
				{
					int addressSize;
					try {
						addressSize = ((IMemoryBlockExtension)memoryBlock).getAddressSize();
					} catch (DebugException e) {
						addressSize = 4;
					}

					endAddress = BigInteger.valueOf(2);
					endAddress = endAddress.pow(addressSize*8);
					endAddress = endAddress.subtract(BigInteger.valueOf(1));
					fEndAddress =  endAddress;
				}
			}

			if (fEndAddress == null) {
				fEndAddress = BigInteger.valueOf(Integer.MAX_VALUE);
			}
		}
		return fEndAddress;
	}

	public int getNumLines()
	{
		return fNumLines;
	}

	public void setNumLines(int numLines)
	{
		fNumLines = numLines;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == AbstractTableRendering.class)
		{
			if (fRendering instanceof AbstractTableRendering) {
				return (T) fRendering;
			}
		}
		if (adapter == AbstractAsyncTableRendering.class)
		{
			if (fRendering instanceof AbstractAsyncTableRendering) {
				return (T) fRendering;
			}
		}

		return super.getAdapter(adapter);
	}
}
