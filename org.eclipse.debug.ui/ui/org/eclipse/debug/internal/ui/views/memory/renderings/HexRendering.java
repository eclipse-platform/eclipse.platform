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
 *     Ken Dyck - Bug 90154: [Memory View] Short Input Rejected by Hex Rendering
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.memory.renderings;

import java.math.BigInteger;

import org.eclipse.debug.core.model.MemoryByte;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.memory.provisional.AbstractAsyncTableRendering;
import org.eclipse.debug.ui.IDebugUIConstants;

/**
 * @since 3.1
 */
public class HexRendering extends AbstractAsyncTableRendering {

	public HexRendering(String renderingId)
	{
		super(renderingId);
	}

	@Override
	public String getString(String dataType, BigInteger address,
			MemoryByte[] data) {
		StringBuilder strBuffer = new StringBuilder();

		String paddedStr = DebugUIPlugin.getDefault().getPreferenceStore().getString(IDebugUIConstants.PREF_PADDED_STR);

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

	@Override
	public byte[] getBytes(String dataType, BigInteger address,
			MemoryByte[] currentValues, String data) {
		byte[] bytes = RenderingsUtil.convertHexStringToByteArray(data, currentValues.length, getNumCharsPerByte());

		return bytes;
	}

	@Override
	public int getNumCharsPerByte()
	{
		return 2;
	}

}
