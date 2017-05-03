/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.net;

import java.util.ArrayList;

public class StringUtil {

	/**
	 * Splits a string into substrings using a delimiter array.
	 *
	 * @param value
	 *            string to be tokenized
	 * @param delimiters
	 *            array of delimiters
	 * @return array of tokens
	 */
	public static String[] split(String value, String[] delimiters) {
		ArrayList<String> result = new ArrayList<>();
		int firstIndex = 0;
		int separator = 0;
		while (firstIndex != -1) {
			firstIndex = -1;
			for (int i = 0; i < delimiters.length; i++) {
				int index = value.indexOf(delimiters[i]);
				if (index != -1 && (index < firstIndex || firstIndex == -1)) {
					firstIndex = index;
					separator = i;
				}
			}
			if (firstIndex != -1) {
				if (firstIndex != 0) {
					result.add(value.substring(0, firstIndex));
				}
				int newStart = firstIndex + delimiters[separator].length();
				if (newStart <= value.length()) {
					value = value.substring(newStart);
				}
			} else if (value.length() > 0) {
				result.add(value);
			}
		}
		return result.toArray(new String[0]);
	}

	/**
	 * Tests equality of the given strings.
	 *
	 * @param sequence1
	 *            candidate 1, may be null
	 * @param sequence2
	 *            candidate 2, may be null
	 * @return true if both sequences are null or the sequences are equal
	 */
	public static final boolean equals(final CharSequence sequence1,
			final CharSequence sequence2) {
		if (sequence1 == sequence2) {
			return true;
		} else if (sequence1 == null || sequence2 == null) {
			return false;
		}
		return sequence1.equals(sequence2);
	}

	/**
	 * Replace within <code>source</code> the occurrences of <code>from</code>
	 * with <code>to</code>.<br>
	 * <b>Note:</b> This has the same behavior as the
	 * <code>String.replace()</code> method within JDK 1.5.
	 *
	 * @param source
	 * @param from
	 * @param to
	 * @return the substituted string
	 */
	public static String replace(String source, String from, String to) {
		if (from.length() == 0)
			return source;
		StringBuffer buffer = new StringBuffer();
		int current = 0;
		int pos = 0;
		while (pos != -1) {
			pos = source.indexOf(from, current);
			if (pos == -1) {
				buffer.append(source.substring(current));
			} else {
				buffer.append(source.substring(current, pos));
				buffer.append(to);
				current = pos + from.length();
			}
		}
		return buffer.toString();
	}

	public static boolean hostMatchesFilter(String host, String filter) {
		String suffixMatchingFilter = "*" + filter; //$NON-NLS-1$
		StringMatcher matcher = new StringMatcher(suffixMatchingFilter, true, false);
		return matcher.match(host);
	}

}