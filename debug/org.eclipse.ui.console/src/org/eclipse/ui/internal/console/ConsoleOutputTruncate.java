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

public class ConsoleOutputTruncate implements IConsoleOutputModifier {

	private static final String NL = System.lineSeparator();
	private static final String DOTS = " ..."; //$NON-NLS-1$
	private int currentLineLength = 0;

	@Override
	public void reset() {
		currentLineLength = 0;
	}

	@Override
	public CharSequence modify(CharSequence t, int lineLimit) {
		StringBuilder text = new StringBuilder(t);
		int currentNewline = text.indexOf(NL);
		int start = 0;
		int end = currentNewline;
		while (end > 0) {
			int diff = truncateLine(text, start, end, lineLimit, true);
			start = end + 1 + diff;
			end = text.indexOf(NL, start);
			currentLineLength = 0;
		}
		int n = text.length();
		truncateLine(text, start, n, lineLimit, false);
		return text;
	}

	private int truncateLine(StringBuilder text, int s, int e, int lineLimit, boolean newline) {
		int previousLineLength = currentLineLength;
		int n = e - s;
		currentLineLength += n;
		int d = 0;
		if (previousLineLength > lineLimit) {
			int e1 = e;
			if (newline) {
				e1 += NL.length();
				n += NL.length();
			}
			text.replace(s, e1, ""); //$NON-NLS-1$
			d = -n;
		} else if (currentLineLength > lineLimit) {
			int s1 = s + lineLimit - previousLineLength;
			String dots = DOTS;
			if (!newline) {
				dots += NL;
			}
			text.replace(s1, e, dots);
			n = e - s1;
			d = dots.length() - n;
		}
		return d;
	}
}
