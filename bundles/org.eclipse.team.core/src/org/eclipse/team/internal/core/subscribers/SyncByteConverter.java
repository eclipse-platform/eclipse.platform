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
package org.eclipse.team.internal.core.subscribers;

import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.core.Messages;

/**
 * Utility for managing slash separated sync information fields.
 */
public class SyncByteConverter {

	protected static final byte SEPARATOR_BYTE = (byte)'/';

	/**
	 * Change the slot to the given bytes
	 * @param syncBytes the sync bytes
	 * @param slot the slot location
	 * @param newBytes the bytes to be put in the slot
	 * @return the new sync bytes
	 * @throws TeamException
	 */
	public static byte[] setSlot(byte[] syncBytes, int slot, byte[] newBytes) throws TeamException {
		int start = startOfSlot(syncBytes, slot);
		if (start == -1) {
			throw new TeamException(NLS.bind(Messages.SyncByteConverter_1, new String[] { new String(syncBytes) }));
		}
		int end = startOfSlot(syncBytes, slot + 1);
		int totalLength = start + 1 + newBytes.length;
		if (end != -1) {
			totalLength += syncBytes.length - end;
		}
		byte[] result = new byte[totalLength];
		System.arraycopy(syncBytes, 0, result, 0, start + 1);
		System.arraycopy(newBytes, 0, result, start + 1, newBytes.length);
		if (end != -1) {
			System.arraycopy(syncBytes, end, result, start + 1 + newBytes.length, syncBytes.length - end);
		}
		return result;
	}

	/**
	 * Method startOfSlot returns the index of the slash that occurs before the
	 * given slot index. The provided index should be &gt;= 1 which assumes that
	 * slot zero occurs before the first slash.
	 *
	 * @param syncBytes
	 * @param i
	 * @return int
	 */
	private static int startOfSlot(byte[] syncBytes, int slot) {
		int count = 0;
		for (int j = 0; j < syncBytes.length; j++) {
			if (syncBytes[j] == SEPARATOR_BYTE) {
				count++;
				if (count == slot) return j;
			}
		}
		return -1;
	}

	/**
	 * Return the offset the the Nth delimeter from the given start index.
	 * @param bytes
	 * @param delimiter
	 * @param start
	 * @param n
	 * @return int
	 */
	private static int getOffsetOfDelimeter(byte[] bytes, byte delimiter, int start, int n) {
		int count = 0;
		for (int i = start; i < bytes.length; i++) {
			if (bytes[i] == delimiter) count++;
			if (count == n) return i;
		}
		// the Nth delimeter was not found
		return -1;
	}

	/**
	 * Get the bytes in the given slot.
	 * @param bytes the sync bytes
	 * @param index the slot location
	 * @param includeRest whether to include the rest
	 * @return the bytes in the given slot
	 */
	public static byte[] getSlot(byte[] bytes, int index, boolean includeRest) {
		// Find the starting index
		byte delimiter = SEPARATOR_BYTE;
		int start;
		if (index == 0) {
			// make start -1 so that end determination will start at offset 0.
			start = -1;
		} else {
			start = getOffsetOfDelimeter(bytes, delimiter, 0, index);
			if (start == -1) return null;
		}
		// Find the ending index
		int end = getOffsetOfDelimeter(bytes, delimiter, start + 1, 1);
		// Calculate the length
		int length;
		if (end == -1 || includeRest) {
			length = bytes.length - start - 1;
		} else {
			length = end - start - 1;
		}
		byte[] result = new byte[length];
		System.arraycopy(bytes, start + 1, result, 0, length);
		return result;
	}

	public static byte[] toBytes(String[] slots) {
		StringBuilder buffer = new StringBuilder();
		for (String string : slots) {
			buffer.append(string);
			buffer.append(new String(new byte[] {SyncByteConverter.SEPARATOR_BYTE }));
		}
		return buffer.toString().getBytes();
	}
}
