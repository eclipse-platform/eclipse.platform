/*******************************************************************************
 * Copyright (c) 2004, 2007 IBM Corporation and others.
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

import java.util.ArrayList;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.debug.core.model.MemoryByte;

/**
 * @since 3.0
 */

public class TableRenderingLine extends PlatformObject {
	private String fAddress;
	private String fStrRep;
	private MemoryByte[] fBytes;
	private byte[] fByteArray;
	private int fTableIndex = -1;
	private String fPaddedString;
	public boolean isMonitored;

	public static final String P_ADDRESS = "address"; //$NON-NLS-1$

	// for raw hex data, it's 2 characters per byte
	private static final int numCharPerByteForHex = 2;

	public TableRenderingLine(String address, MemoryByte[] bytes, int tableIndex, String paddedString) {
		fAddress = address;
		fBytes = bytes;
		fTableIndex = tableIndex;
		fPaddedString = paddedString;
	}

	public String getAddress() {
		return fAddress;
	}

	public void setAddress(String address) {
		fAddress = address;
	}

	public MemoryByte[] getBytes()
	{
		return fBytes;
	}

	public MemoryByte getByte(int offset)
	{
		if (fBytes == null) {
			return null;
		}

		if (offset < fBytes.length) {
			return fBytes[offset];
		}

		return null;
	}

	public MemoryByte[] getBytes(int start, int end)
	{
		ArrayList<MemoryByte> ret = new ArrayList<>();

		for (int i=start; i<end; i++)
		{
			ret.add(fBytes[i]);
		}
		return ret.toArray(new MemoryByte[ret.size()]);
	}

	public String getRawMemoryString()
	{
		if (fStrRep == null)
		{
			StringBuilder buffer = new StringBuilder();
			fStrRep = RenderingsUtil.convertByteArrayToHexString(getByteArray());
			fStrRep = fStrRep.toUpperCase();

			buffer = buffer.append(fStrRep);

			// pad unavailable bytes with padded string from memory block
			String paddedString = null;
			int bufferCounter = 0;
			for (MemoryByte mb : fBytes) {
				// if byte is invalid
				if (!mb.isReadable()) {
					if (paddedString == null)
					{
						paddedString = fPaddedString;

						if (paddedString.length() > TableRenderingLine.numCharPerByteForHex) {
							paddedString = paddedString.substring(0, TableRenderingLine.numCharPerByteForHex);
						}
					}
					buffer.replace(bufferCounter, bufferCounter+TableRenderingLine.numCharPerByteForHex, paddedString);
				}
				bufferCounter += TableRenderingLine.numCharPerByteForHex;
			}

			fStrRep = buffer.toString();
		}

		return fStrRep;
	}

	/**
	 * @param start
	 * @param end
	 * @return is the bytes between start and end are all valid
	 */
	public boolean isAvailable(int start, int end) {
		boolean available = true;
		for (int i=start; i<end; i++)
		{
			if (!fBytes[i].isReadable())
			{
				available = false;
				break;
			}
		}
		return available;
	}


	public byte[] getByteArray()
	{
		if (fByteArray == null)
		{
			fByteArray = new byte[fBytes.length];
			for (int i=0; i<fBytes.length; i++)
			{
				fByteArray[i] = fBytes[i].getValue();
			}
		}

		return fByteArray;
	}

	public byte[] getByteArray(int start, int end)
	{
		byte[] ret = new byte[end-start];
		int j=0;

		for (int i=start; i<end; i++)
		{
			ret[j] = fBytes[i].getValue();
			j++;
		}
		return ret;
	}

	public void markDeltas(TableRenderingLine oldData)
	{
		if (oldData == null) {
			return;
		}

		// if address is not the same, no need to compare
		if (!oldData.getAddress().equals(this.getAddress())) {
			return;
		}

		// if the string representation is the same, no need to compare
		if (oldData.getRawMemoryString().equals(getRawMemoryString()))
		{
			for (MemoryByte mb : fBytes) {
				// set history as known if we have old data for this line
				mb.setHistoryKnown(true);
			}
			return;
		}

		MemoryByte[] oldMemory = oldData.getBytes();

		if (oldMemory.length != fBytes.length) {
			return;
		}

		for (int i=0; i<fBytes.length; i++)
		{
			// turn on known bit
			fBytes[i].setHistoryKnown(true);

			if ((fBytes[i].getFlags() & MemoryByte.READABLE) != (oldMemory[i].getFlags() & MemoryByte.READABLE))
			{
				fBytes[i].setChanged(true);
				continue;
			}

			if (fBytes[i].isReadable() && oldMemory[i].isReadable())
			{
				if (fBytes[i].getValue() != oldMemory[i].getValue())
				{
					fBytes[i].setChanged(true);
				}
			}
		}
	}

	public void copyDeltas(TableRenderingLine oldData)
	{
		if (oldData == null) {
			return;
		}

		// if address is not the same, do not copy
		if (!oldData.getAddress().equals(this.getAddress())) {
			return;
		}

		// reuse delta information from old data
		MemoryByte[] oldMemory = oldData.getBytes();

		if (oldMemory.length != fBytes.length) {
			return;
		}

		for (int i=0; i<fBytes.length; i++)
		{
			fBytes[i].setFlags(oldMemory[i].getFlags());
		}
	}

	public boolean isLineChanged(TableRenderingLine oldData)
	{
		if (oldData == null) {
			return false;
		}

		// if address is not the same, no need to compare
		if (!oldData.getAddress().equals(this.getAddress())) {
			return false;
		}

		// if the string representation is not the same, this line has changed
		if (oldData.getRawMemoryString().equals(getRawMemoryString())) {
			return false;
		}
		return true;
	}

	/**
	 * @param offset
	 * @param endOffset
	 * @return true if the specified range of memory has changed, false otherwise
	 * */

	public boolean isRangeChange(int offset, int endOffset)
	{
		boolean allBytesKnown = true;
		boolean allBytesUnchanged = true;

		for (int i=offset; i<=endOffset; i++)
		{
			if (!fBytes[i].isHistoryKnown()) {
				allBytesKnown = false;
			}
			if (fBytes[i].isChanged()) {
				allBytesUnchanged = false;
			}
		}

		if (allBytesKnown && !allBytesUnchanged) {
			return true;
		}
		return false;
	}

	public void unmarkDeltas()
	{
		for (MemoryByte mb : fBytes) {
			// unset the change bit
			if (mb.isChanged()) {
				mb.setChanged(false);
			}
		}
	}

	@Override
	public String toString()
	{
		StringBuilder buf = new StringBuilder();
		buf.append(getAddress());

		buf.append(": "); //$NON-NLS-1$

		buf.append(getRawMemoryString());

		return buf.toString();
	}

	public int getTableIndex()
	{
		return fTableIndex;
	}

	public int getLength()
	{
		return fBytes.length;
	}

}

