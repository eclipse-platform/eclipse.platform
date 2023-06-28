/*******************************************************************************
 * Copyright (c) 2007, 2021 IBM Corporation and others.
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
 *     David Pickens - [Memory View] Endian in hex view and ASCII view doesn't work
 *     John Dallaway - Accommodate addressableSize != 1 (bug 575413)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.memory.renderings;

import java.math.BigInteger;

import org.eclipse.debug.core.model.MemoryByte;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.IDebugUIConstants;

/**
 * @since 3.3
 */
public class HexIntegerRendering extends AbstractIntegerRendering {

	public HexIntegerRendering(String renderingId)
	{
		super(renderingId);
	}

	@Override
	public String getString(String dataType, BigInteger address,
			MemoryByte[] data) {
		StringBuilder strBuffer = new StringBuilder();
		int endianess = getEndianness(data);

		String paddedStr = DebugUIPlugin.getDefault().getPreferenceStore().getString(IDebugUIConstants.PREF_PADDED_STR);

		if (endianess == RenderingsUtil.LITTLE_ENDIAN) {
			int addressableSize = getAddressableSize();
			MemoryByte[] swapped = new MemoryByte[data.length];
			for (int i = 0; i < data.length; i += addressableSize) {
				System.arraycopy(data, i, swapped, data.length - i - addressableSize, addressableSize);
			}
			data = swapped;
		}

		for (MemoryByte memByte : data) {
			if (memByte.isReadable()) {
				strBuffer.append(new String(RenderingsUtil.convertByteToCharArray(memByte.getValue())));
			} else {
				// pad with padded string
				strBuffer.append(paddedStr);
			}
		}

		return strBuffer.toString().toUpperCase();
	}

	/**
	 * @todo davidp needs to add a method comment.
	 * @param data
	 * @return
	 */
	private int getEndianness (MemoryByte[] data) {
		// if the user has not set an endianess to the rendering
		// take default
		int endianess = getDisplayEndianess();
		if (endianess == RenderingsUtil.ENDIANESS_UNKNOWN) {
			endianess = getBytesEndianess(data);
		}
		return endianess;
	}

	@Override
	public byte[] getBytes(String dataType, BigInteger address,
			MemoryByte[] currentValues, String data) {

		int endianess = getEndianness(currentValues);
		byte[] bytes = RenderingsUtil.convertHexStringToByteArray(data, currentValues.length, getNumCharsPerByte());


		if (endianess == RenderingsUtil.LITTLE_ENDIAN) {
			int addressableSize = getAddressableSize();
			byte[] swapped = new byte[bytes.length];
			for (int i = 0; i < bytes.length; i += addressableSize) {
				System.arraycopy(bytes, i, swapped, bytes.length - i - addressableSize, addressableSize);
			}
			bytes = swapped;
		}

		return bytes;
	}

	@Override
	public int getNumCharsPerByte()
	{
		return 2;
	}


}
