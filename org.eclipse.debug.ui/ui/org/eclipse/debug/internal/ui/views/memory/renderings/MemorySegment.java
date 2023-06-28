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
import java.util.ArrayList;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.debug.core.model.MemoryByte;

public class MemorySegment extends PlatformObject {

	private BigInteger fAddress;
	private BigInteger fEndAddress;
	private MemoryByte[] fBytes;
	private int fNumAddressableUnits;

	public MemorySegment(BigInteger address, MemoryByte[] bytes, int numAddressableUnits)
	{
		fAddress = address;
		fBytes = bytes;
		fNumAddressableUnits = numAddressableUnits;
	}

	public BigInteger getAddress() {
		return fAddress;
	}

	public MemoryByte[] getBytes() {
		return fBytes;
	}

	public int getNumAddressableUnits() {
		return fNumAddressableUnits;
	}

	public boolean containsAddress(BigInteger address)
	{
		if (getAddress().compareTo(address) <= 0 && getEndAddress().compareTo(address) >= 0)
			return true;
		return false;
	}

	public BigInteger getEndAddress()
	{
		if (fEndAddress == null)
		{
			fEndAddress = fAddress.add(BigInteger.valueOf(fNumAddressableUnits).subtract(BigInteger.ONE));
		}
		return fEndAddress;
	}

	/**
	 * @param start - zero-based start offset
	 * @param length - number of bytes to get
	 * @return the bytes from start offset to the end.
	 */
	public MemoryByte[] getBytes(int start, int length)
	{
		if (start < 0)
			return new MemoryByte[0];

		if (start + length > fBytes.length)
			return new MemoryByte[0];

		ArrayList<MemoryByte> ret = new ArrayList<>();

		for (int i=start; i< start+length; i++)
		{
			ret.add(fBytes[i]);
		}
		return ret.toArray(new MemoryByte[ret.size()]);
	}

}
