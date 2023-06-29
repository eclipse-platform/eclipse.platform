/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IMemoryBlockExtension;

public class TableRenderingContentDescriptor{
	private AbstractBaseTableRendering fRendering;
	private int fPreBuffer;					// number of lines before the top visible line
	private int fPostBuffer;				// number of lines after thes last visible line
	private BigInteger fLoadAddress;		// Top address to load at the table
	private int fNumLines;					// number of visible lines
	private BigInteger fMemoryBlockBaseAddress;		// base address of the memory block when this input is set
	private BigInteger fStartAddress;
	private BigInteger fEndAddress;

	private int fAddressSize = -1;
	private int fAddressableSize = -1;

	private boolean fAlignAddress = true;

	private boolean fIsDynamicLoad;

	public TableRenderingContentDescriptor(AbstractBaseTableRendering rendering)
	{
		fRendering = rendering;
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

	public AbstractBaseTableRendering getRendering()
	{
		return fRendering;
	}

	public int getAddressableSize() {
		return fAddressableSize;
	}

	public void setAddressableSize(int addressableSize) {
		fAddressableSize = addressableSize;
	}

	public int getAddressSize() {
		return fAddressSize;
	}

	public void setAddressSize(int addressSize) {
		fAddressSize = addressSize;
	}

	public void setDynamicLoad(boolean dynamic)
	{
		fIsDynamicLoad = dynamic;
	}

	public boolean isDynamicLoad()
	{
		return fIsDynamicLoad;
	}

	public boolean isMemoryBlockBaseAddressInitialized()
	{
		return (fMemoryBlockBaseAddress != null);
	}

	public boolean isAlignAddressToBoundary()
	{
		return fAlignAddress;
	}

	public void setAlignAddressToBoundary(boolean align)
	{
		fAlignAddress = align;
	}

}
