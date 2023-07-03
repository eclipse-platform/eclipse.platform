/*******************************************************************************
 * Copyright (c) 2005, 2020 IBM Corporation and others.
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
package org.eclipse.ua.tests.util;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.osgi.framework.Bundle;

/*
 * Utility methods for working with files.
 */
public class FileUtil {

	/**
	 * Gets the contents of the file with the given relative path in the given bundle,
	 * as a String (file must be encoded as UTF-8).
	 */
	public static String getContents(Bundle bundle, String relativePath) throws IOException {
		URL url = bundle.getEntry(relativePath);
		if (url != null) {
			try (InputStream is = url.openStream()) {
				return readString(is);
			}
		}
		return null;
	}

	/**
	 * Gets the contents of the file with the given absolute path as a String (file must
	 * be encoded as UTF-8).
	 */
	public static String getContents(String absolutePath) throws IOException {
		try (FileInputStream fis = new FileInputStream(absolutePath)) {
			return readString(fis);
		}
	}

	/**
	 * Generates a filename with path to the result file that will be generated
	 * for the intro xml referred to by the string.
	 */
	public static String getResultFile(String in) {
		return in.substring(0, in.lastIndexOf('.')) + "_expected.txt";
	}

	/**
	 * Reads the contents of the input stream as UTF-8 and constructs and returns
	 * as a String.
	 */
	public static String readString(InputStream in) throws IOException {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			byte[] buffer = new byte[4096];
			int num;
			while ((num = in.read(buffer)) > 0) {
				out.write(buffer, 0, num);
			}
			String result = new String(out.toByteArray(), StandardCharsets.UTF_8);
			if (result.length() > 0) {
				// filter windows-specific newline
				result = result.replaceAll("\r", "");
			}
			// ignore whitespace at start or end
			return result.trim();
		}
	}
}
