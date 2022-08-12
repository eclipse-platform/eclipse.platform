/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.core.internal.utils;

import java.nio.charset.StandardCharsets;

public class Convert {

	/**
	 * Converts the string argument to a byte array.
	 */
	public static String fromUTF8(byte[] b) {
		return new String(b, StandardCharsets.UTF_8);
	}

	/**
	 * Converts the string argument to a byte array.
	 */
	public static byte[] toUTF8(String s) {
		return s.getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * Performs conversion of a long value to a byte array representation.
	 *
	 * @see #bytesToLong(byte[])
	 */
	public static byte[] longToBytes(long value) {

		// A long value is 8 bytes in length.
		byte[] bytes = new byte[8];

		// Convert and copy value to byte array:
		//   -- Cast long to a byte to retrieve least significant byte;
		//   -- Left shift long value by 8 bits to isolate next byte to be converted;
		//   -- Repeat until all 8 bytes are converted (long = 64 bits).
		// Note: In the byte array, the least significant byte of the long is held in
		// the highest indexed array bucket.

		for (int i = 0; i < bytes.length; i++) {
			bytes[(bytes.length - 1) - i] = (byte) value;
			value >>>= 8;
		}

		return bytes;
	}

	/**
	 * Performs conversion of a byte array to a long representation.
	 *
	 * @see #longToBytes(long)
	 */
	public static long bytesToLong(byte[] value) {

		long longValue = 0L;

		// See method convertLongToBytes(long) for algorithm details.
		for (byte element : value) {
			// Left shift has no effect thru first iteration of loop.
			longValue <<= 8;
			longValue ^= element & 0xFF;
		}

		return longValue;
	}
}
