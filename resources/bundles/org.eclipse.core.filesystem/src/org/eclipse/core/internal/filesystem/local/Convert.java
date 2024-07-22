/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
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

	public static final String WIN32_RAW_PATH_PREFIX = "\\\\?\\"; //$NON-NLS-1$
	public static final String WIN32_UNC_RAW_PATH_PREFIX = "\\\\?\\UNC"; //$NON-NLS-1$

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
}
