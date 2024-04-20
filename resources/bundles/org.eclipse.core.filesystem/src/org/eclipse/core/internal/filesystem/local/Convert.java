/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
 * 	Martin Oberhuber (Wind River) - [170317] add symbolic link support to API
 *******************************************************************************/
package org.eclipse.core.internal.filesystem.local;

import java.io.UnsupportedEncodingException;
import org.eclipse.core.runtime.Platform;

public class Convert {

	/** Indicates the default native encoding on this platform */
	private static String defaultEncoding = Platform.getSystemCharset().name();

	/** Indicates if we are running on windows */
	private static final boolean IS_WINDOWS = Platform.OS.isWindows();

	private static final String WIN32_FILE_PREFIX = "\\\\?\\"; //$NON-NLS-1$
	private static final String WIN32_UNC_FILE_PREFIX = "\\\\?\\UNC"; //$NON-NLS-1$

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
			// Left shift has no effect through first iteration of loop.
			longValue <<= 8;
			longValue ^= element & 0xFF;
		}

		return longValue;
	}

	/**
	 * Calling new String(byte[] s) creates a new encoding object and other garbage.
	 * This can be avoided by calling new String(byte[] s, String encoding) instead.
	 * @param source buffer with String in platform bytes
	 * @param length number of relevant bytes in the buffer
	 * @return converted Java String
	 * @since org.eclipse.core.filesystem 1.1
	 */
	public static String fromPlatformBytes(byte[] source, int length) {
		if (defaultEncoding == null)
			return new String(source, 0, length);
		// try to use the default encoding
		try {
			return new String(source, 0, length, defaultEncoding);
		} catch (UnsupportedEncodingException e) {
			// null the default encoding so we don't try it again
			defaultEncoding = null;
			return new String(source, 0, length);
		}
	}

	/*
	 * This method is called via Reflection API from the legacy file system
	 * library (liblocalfile_1_0_0). Source for the native code is located
	 * in org.eclipse.core.filesystem/natives/unix/localfile.c.
	 */
	public static String fromPlatformBytes(byte[] source) {
		return fromPlatformBytes(source, source.length);
	}

	/**
	 * Calling String.getBytes() creates a new encoding object and other garbage.
	 * This can be avoided by calling String.getBytes(String encoding) instead.
	 */
	public static byte[] toPlatformBytes(String target) {
		if (defaultEncoding == null)
			return target.getBytes();
		// try to use the default encoding
		try {
			return target.getBytes(defaultEncoding);
		} catch (UnsupportedEncodingException e) {
			// null the default encoding so we don't try it again
			defaultEncoding = null;
			return target.getBytes();
		}
	}

	/**
	 * Converts a file name to a unicode char[] suitable for use by native methods.
	 * See http://msdn.microsoft.com/library/default.asp?url=/library/en-us/fileio/fs/naming_a_file.asp
	 */
	public static char[] toPlatformChars(String target) {
		//Windows use special prefix to handle long filenames
		if (!IS_WINDOWS) {
			return target.toCharArray();
		}
		//convert UNC path of form \\server\path to unicode form \\?\UNC\server\path
		if (target.startsWith("\\\\")) { //$NON-NLS-1$
			int nameLength = target.length();
			int prefixLength = WIN32_UNC_FILE_PREFIX.length();
			char[] result = new char[prefixLength + nameLength - 1];
			WIN32_UNC_FILE_PREFIX.getChars(0, prefixLength, result, 0);
			target.getChars(1, nameLength, result, prefixLength);
			return result;
		}
		//convert simple path of form c:\path to unicode form \\?\c:\path
		int nameLength = target.length();
		int prefixLength = WIN32_FILE_PREFIX.length();
		char[] result = new char[prefixLength + nameLength];
		WIN32_UNC_FILE_PREFIX.getChars(0, prefixLength, result, 0);
		target.getChars(0, nameLength, result, prefixLength);
		return result;
	}
}
