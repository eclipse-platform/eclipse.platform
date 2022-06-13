/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
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
package org.eclipse.core.internal.content;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import org.eclipse.core.runtime.content.IContentDescription;

public class Util {
	public static String[] parseItems(String string) {
		return parseItems(string, ","); //$NON-NLS-1$
	}

	public static String[] parseItems(String string, String separator) {
		if (string == null)
			return new String[0];
		StringTokenizer tokenizer = new StringTokenizer(string, separator, true);
		if (!tokenizer.hasMoreTokens())
			return new String[] {string.trim()};
		String first = tokenizer.nextToken().trim();
		boolean wasSeparator = false;
		if (first.equals(separator)) {
			// leading separator
			first = ""; //$NON-NLS-1$
			wasSeparator = true;
		}
		// simple cases, do not create temporary list
		if (!tokenizer.hasMoreTokens())
			return wasSeparator ? /* two empty strings */new String[] {first, first} : /*single non-empty element  */new String[] {first};
		ArrayList<String> items = new ArrayList<>();
		items.add(first);
		String current;
		do {
			current = tokenizer.nextToken().trim();
			boolean isSeparator = current.equals(separator);
			if (isSeparator) {
				if (wasSeparator)
					items.add(""); //$NON-NLS-1$
			} else
				items.add(current);
			wasSeparator = isSeparator;
		} while (tokenizer.hasMoreTokens());
		if (wasSeparator)
			// trailing separator
			items.add(""); //$NON-NLS-1$
		return items.toArray(new String[items.size()]);
	}

	public static List<String> parseItemsIntoList(String string) {
		return parseItemsIntoList(string, ","); //$NON-NLS-1$
	}

	public static List<String> parseItemsIntoList(String string, String separator) {
		List<String> items = new ArrayList<>(5);
		if (string == null)
			return items;
		StringTokenizer tokenizer = new StringTokenizer(string, separator, true);
		if (!tokenizer.hasMoreTokens()) {
			items.add(string.trim());
			return items;
		}
		String first = tokenizer.nextToken().trim();
		boolean wasSeparator = false;
		if (first.equals(separator)) {
			// leading separator
			first = ""; //$NON-NLS-1$
			wasSeparator = true;
		}
		items.add(first);
		if (!tokenizer.hasMoreTokens())
			return items;
		String current;
		do {
			current = tokenizer.nextToken().trim();
			boolean isSeparator = current.equals(separator);
			if (isSeparator) {
				if (wasSeparator)
					items.add(""); //$NON-NLS-1$
			} else
				items.add(current);
			wasSeparator = isSeparator;
		} while (tokenizer.hasMoreTokens());
		if (wasSeparator)
			// trailing separator
			items.add(""); //$NON-NLS-1$
		return items;
	}

	public static String toListString(Object[] list) {
		return toListString(list, ","); //$NON-NLS-1$
	}

	public static String toListString(Object[] list, String separator) {
		if (list == null || list.length == 0)
			return null;
		StringBuilder result = new StringBuilder();
		for (Object element : list) {
			result.append(element);
			result.append(separator);
		}
		// ignore last comma
		return result.substring(0, result.length() - 1);
	}

	/*
	 * Reads bom from the stream. Note that the stream will not be repositioned
	 * when the method returns.
	 */
	public static byte[] getByteOrderMark(InputStream input) throws IOException {
		int first = input.read();
		switch (first) {
		case 0xEF:
			//look for the UTF-8 Byte Order Mark (BOM)
			int second = input.read();
			int third = input.read();
			if (second == 0xBB && third == 0xBF)
				return IContentDescription.BOM_UTF_8;
			break;
		case 0xFE:
			//look for the UTF-16 BOM
			if (input.read() == 0xFF)
				return IContentDescription.BOM_UTF_16BE;
			break;
		case 0xFF:
			if (input.read() == 0xFE)
				return IContentDescription.BOM_UTF_16LE;
			break;
		default:
			break;
		}
		return null;
	}
}
