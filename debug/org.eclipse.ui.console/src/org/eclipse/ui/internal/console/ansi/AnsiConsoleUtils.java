/*******************************************************************************
 * Copyright (c) 2012-2022 Mihai Nita and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.ui.internal.console.ansi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnsiConsoleUtils {
	public static final Pattern ESCAPE_SEQUENCE_REGEX_TXT = Pattern.compile("\u001b\\[[\\d;]*[A-HJKSTfimnsu]"); //$NON-NLS-1$
	public static final Pattern ESCAPE_SEQUENCE_REGEX_HTML = Pattern.compile("<span[^>]*>\u001b\\[[\\d;]*[A-HJKSTfimnsu]</span>"); //$NON-NLS-1$
	public static final Pattern ESCAPE_SEQUENCE_REGEX_RTF = Pattern.compile("\\{\\\\cf\\d+[^}]* \u001b\\[[\\d;]*[A-HJKSTfimnsu][^}]*\\}"); //$NON-NLS-1$
	// These two are used to replace \chshdng#1\chcbpat#2 with \chshdng#1\chcbpat#2\cb#2
	public static final Pattern ESCAPE_SEQUENCE_REGEX_RTF_FIX_SRC = Pattern.compile("\\\\chshdng\\d+\\\\chcbpat(\\d+)"); //$NON-NLS-1$
	public static final String ESCAPE_SEQUENCE_REGEX_RTF_FIX_TRG = "$0\\\\cb$1"; //$NON-NLS-1$

	public static final char ESCAPE_SGR = 'm';

	/**
	 * Removes all ANSI escape sequences from the given text.
	 *
	 * @param text the text to clean, may be <code>null</code>
	 * @return the text without ANSI escape sequences, or the original text if it
	 *         is <code>null</code>, empty or contains no escape sequence
	 */
	public static String removeAnsiSequences(String text) {
		if (text == null || text.isEmpty()) {
			return text;
		}
		Matcher matcher = ESCAPE_SEQUENCE_REGEX_TXT.matcher(text);
		if (!matcher.find()) {
			return text;
		}
		StringBuilder result = new StringBuilder(text.length());
		int last = 0;
		do {
			result.append(text, last, matcher.start());
			last = matcher.end();
		} while (matcher.find());
		result.append(text, last, text.length());
		return result.toString();
	}

	private AnsiConsoleUtils() {
		// Utility class, should not be instantiated
	}
}
