/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.help.internal.util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class URLCoder {

	public static String encode(String s) {
		return urlEncode(s.getBytes(StandardCharsets.UTF_8), true);
	}

	public static String compactEncode(String s) {
		return urlEncode(s.getBytes(StandardCharsets.UTF_8), false);
	}

	public static String decode(String s) {
		return new String(urlDecode(s), StandardCharsets.UTF_8);
	}

	private static String urlEncode(byte[] data, boolean encodeAllCharacters) {
		StringBuilder buf = new StringBuilder(data.length);
		for (int i = 0; i < data.length; i++) {
			byte nextByte = data[i];
			if (!encodeAllCharacters && isAlphaNumericOrDot(nextByte)) {
				buf.append((char)nextByte);
			} else {
				buf.append('%');
				buf.append(Character.forDigit((nextByte & 240) >>> 4, 16));
				buf.append(Character.forDigit(nextByte & 15, 16));
			}
		}
		return buf.toString();
	}

	private static boolean isAlphaNumericOrDot(byte b) {
		return (b >= '0' && b <= '9') || (b >= 'a' && b <= 'z') || ( b >= 'A' && b <= 'Z')
			|| b == '.';
	}

	private static byte[] urlDecode(String encodedURL) {
		int len = encodedURL.length();
		ByteArrayOutputStream os = new ByteArrayOutputStream(len);
		for (int i = 0; i < len;) {
			switch (encodedURL.charAt(i)) {
			case '%':
				if (len >= i + 3) {
					os.write(Integer.parseInt(encodedURL.substring(i + 1, i + 3), 16));
				}
				i += 3;
				break;
			case '+': // exception from standard
				os.write(' ');
				i++;
				break;
			default:
				os.write(encodedURL.charAt(i++));
				break;
			}

		}
		return os.toByteArray();
	}
}
