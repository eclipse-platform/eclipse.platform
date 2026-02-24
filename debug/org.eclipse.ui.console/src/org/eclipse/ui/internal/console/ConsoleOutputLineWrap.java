/*******************************************************************************
 * Copyright (c) 2026 Simeon Andreev and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Simeon Andreev - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.console;

import org.eclipse.jface.text.MultiStringMatcher;
import org.eclipse.jface.text.MultiStringMatcher.Match;

/**
 * Processes chunks of character sequences. Wraps lines longer than a specified
 * length.
 */
public class ConsoleOutputLineWrap implements IConsoleOutputModifier {

	private final int lineLimit;
	private final MultiStringMatcher newlineMatcher;
	private final String nl;
	private int currentLineLength = 0;

	public ConsoleOutputLineWrap(int lineLimit, String... newlines) {
		this.lineLimit = lineLimit;
		newlineMatcher = MultiStringMatcher.create(newlines);
		nl = newlines[0];
	}

	@Override
	public void reset() {
		currentLineLength = 0;
	}

	@Override
	public CharSequence modify(CharSequence t) {
		StringBuilder text = new StringBuilder(t);
		Match newlineMatch = newlineMatcher.indexOf(text, 0);
		int currentNewline = matchIndex(newlineMatch);
		int start = 0;
		int end = currentNewline;
		while (end >= 0) {
			int breaks = breakLine(text, start, end);
			start = end + newlineMatch.getText().length() + breaks;
			newlineMatch = newlineMatcher.indexOf(text, start);
			end = matchIndex(newlineMatch);
			currentLineLength = 0;
		}
		int n = text.length();
		breakLine(text, start, n);
		return text;
	}

	private static int matchIndex(Match match) {
		return match != null ? match.getOffset() : -1;
	}

	private int breakLine(StringBuilder text, int s, int e) {
		int previousLineLength = currentLineLength;
		currentLineLength += e - s;
		int b = 0;
		if (currentLineLength > lineLimit) {
			int s1 = s + lineLimit - previousLineLength;
			char c = text.charAt(s1);
			if (s1 > 0 && (Character.isLowSurrogate(c))) {
				--s1;
			}
			int e1 = e;
			for (int i = s1; i < e1; i += lineLimit) {
				text.insert(i, nl);
				currentLineLength = e1 - i;
				i += nl.length();
				e1 += nl.length();
				b += nl.length();
			}
		}
		return b;
	}
}
