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
 * Processes chunks of character sequences. Truncates lines longer than a
 * specified length.
 */
public class ConsoleOutputLineTruncate implements IConsoleOutputModifier {

	private static final String DOTS = " ..."; //$NON-NLS-1$
	private final int lineLimit;
	private final MultiStringMatcher newlineMatcher;
	private final String nl;
	private int currentLineLength = 0;

	public ConsoleOutputLineTruncate(int lineLimit, String... newlines) {
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
			int diff = truncateLine(text, newlineMatch.getText(), start, end);
			start = end + newlineMatch.getText().length() + diff;
			newlineMatch = newlineMatcher.indexOf(text, start);
			end = matchIndex(newlineMatch);
			currentLineLength = 0;
		}
		int n = text.length();
		truncateLine(text, null, start, n);
		return text;
	}

	private static int matchIndex(Match match) {
		return match != null ? match.getOffset() : -1;
	}

	private int truncateLine(StringBuilder text, String newlineMatch, int s, int e) {
		int previousLineLength = currentLineLength;
		int n = e - s;
		currentLineLength += n;
		int d = 0;
		if (previousLineLength > lineLimit) {
			int e1 = e;
			if (newlineMatch != null) {
				e1 += newlineMatch.length();
				n += newlineMatch.length();
			}
			text.replace(s, e1, ""); //$NON-NLS-1$
			d = -n;
		} else if (currentLineLength > lineLimit) {
			int s1 = s + lineLimit - previousLineLength;
			if (s1 > 0 && Character.isLowSurrogate(text.charAt(s1))) {
				--s1;
			}
			String dots = DOTS;
			if (newlineMatch == null) {
				dots += nl;
			}
			text.replace(s1, e, dots);
			n = e - s1;
			d = dots.length() - n;
		}
		return d;
	}
}
