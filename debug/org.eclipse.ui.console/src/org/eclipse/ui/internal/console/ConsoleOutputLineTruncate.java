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
	private int currentLineLength;

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

	/**
	 * Truncates the given line if it exceeds the line limit.
	 *
	 * @return the number of characters removed or added
	 */
	private int truncateLine(StringBuilder text, String newlineMatch, int start, int end) {
		int previousLineLength = currentLineLength;
		int n = end - start;
		currentLineLength += n;
		int diff = 0;
		if (previousLineLength > lineLimit) {
			// This segment is a continuation of an already-truncated line; drop it entirely.
			// Note: start is always 0 here because currentLineLength is reset to 0 after
			// every newline inside modify(), so previousLineLength > lineLimit is only
			// possible for the very first segment of a new chunk. No surrogate-pair guard
			// is needed: we cannot be mid-pair at position 0 of a fresh chunk.
			int e1 = end;
			if (newlineMatch != null) {
				e1 += newlineMatch.length();
				n += newlineMatch.length();
			}
			text.replace(start, e1, ""); //$NON-NLS-1$
			diff = -n;
		} else if (currentLineLength > lineLimit) {
			int s1 = start + lineLimit - previousLineLength;
			if (s1 > 0 && Character.isLowSurrogate(text.charAt(s1))) {
				--s1;
			}
			String dots = DOTS;
			if (newlineMatch == null) {
				// No real newline follows: append a synthetic one so that subsequent
				// chunks continuing this overlong line are still recognized as part of
				// the same (already-truncated) line and are dropped by the
				// previousLineLength > lineLimit branch above.
				dots += nl;
			}
			text.replace(s1, end, dots);
			n = end - s1;
			diff = dots.length() - n;
		}
		return diff;
	}
}
