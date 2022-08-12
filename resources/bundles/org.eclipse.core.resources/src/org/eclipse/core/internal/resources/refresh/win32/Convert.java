/*******************************************************************************
 * Copyright (c) 2002, 2007 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.resources.refresh.win32;

import java.io.*;

/**
 * Performs character to byte conversion for passing strings to native win32
 * methods.
 */
public class Convert {

	/*
	 * Obtains the default encoding on this platform
	 */
	private static String defaultEncoding = new InputStreamReader(new ByteArrayInputStream(new byte[0])).getEncoding();

	/**
	 * Converts the given String to bytes using the platforms default
	 * encoding.
	 *
	 * @param target	The String to be converted, can not be <code>null</code>.
	 * @return byte[]	The resulting bytes, or <code>null</code>.
	 */
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
