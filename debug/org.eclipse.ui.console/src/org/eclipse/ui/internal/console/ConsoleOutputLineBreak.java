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

public class ConsoleOutputLineBreak implements IConsoleOutputModifier {

	private static final String NL = System.lineSeparator();
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
			int breaks = breakLine(text, start, end, lineLimit);
			start = end + 1 + breaks;
			end = text.indexOf(NL, start);
			currentLineLength = 0;
		}
		int n = text.length();
		breakLine(text, start, n, lineLimit);
		return text;
	}

	private int breakLine(StringBuilder text, int s, int e, int lineLimit) {
		int previousLineLength = currentLineLength;
		currentLineLength += e - s;
		int b = 0;
		if (currentLineLength > lineLimit) {
			int s1 = s + lineLimit - previousLineLength;
			int e1 = e;
			for (int i = s1; i < e1; i += lineLimit) {
				text.insert(i, NL);
				currentLineLength = e1 - i;
				++i;
				++e1;
				b += NL.length();
			}
		}
		return b;
	}
}