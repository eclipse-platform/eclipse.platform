/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
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
/**
 *
 */
package org.eclipse.unittest.internal.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;

import org.eclipse.core.text.StringMatcher;

/**
 * A Textual Trace
 */
public class TextualTrace {
	/**
	 * An exception line type
	 */
	public static final int LINE_TYPE_EXCEPTION = 1;

	/**
	 * An normal line type
	 */
	public static final int LINE_TYPE_NORMAL = 0;

	/**
	 * An stackframe line type
	 */
	public static final int LINE_TYPE_STACKFRAME = 2;

	private final String fTrace;

	/**
	 * Constructs a Textual Trace object
	 *
	 * @param trace          a trace line
	 * @param filterPatterns a collection of filter string matchers
	 */
	public TextualTrace(String trace, Collection<StringMatcher> filterPatterns) {
		super();
		fTrace = filterStack(trace, filterPatterns);
	}

	/**
	 * Displays a trace line on a specified display
	 *
	 * @param display        a target display
	 * @param maxLabelLength a maximum number of characters to be displayed
	 */
	public void display(ITraceDisplay display, int maxLabelLength) {
		StringReader stringReader = new StringReader(fTrace);
		BufferedReader bufferedReader = new BufferedReader(stringReader);
		String line;

		try {
			// first line contains the thrown exception
			line = readLine(bufferedReader);
			if (line == null)
				return;

			displayWrappedLine(display, maxLabelLength, line, LINE_TYPE_EXCEPTION);

			// the stack frames of the trace
			while ((line = readLine(bufferedReader)) != null) {
				int type = isAStackFrame(line) ? LINE_TYPE_STACKFRAME : LINE_TYPE_NORMAL;
				displayWrappedLine(display, maxLabelLength, line, type);
			}
		} catch (IOException e) {
			display.addTraceLine(LINE_TYPE_NORMAL, fTrace);
		}
	}

	private void displayWrappedLine(ITraceDisplay display, int maxLabelLength, String line, int type) {
		final int labelLength = line.length();
		if (labelLength < maxLabelLength) {
			display.addTraceLine(type, line);
		} else {
			display.addTraceLine(type, line.substring(0, maxLabelLength));
			int offset = maxLabelLength;
			while (offset < labelLength) {
				int nextOffset = Math.min(labelLength, offset + maxLabelLength);
				display.addTraceLine(LINE_TYPE_NORMAL, line.substring(offset, nextOffset));
				offset = nextOffset;
			}
		}
	}

	private boolean filterLine(Collection<StringMatcher> patterns, String line) {
		for (StringMatcher pattern : patterns) {
			if (pattern.match(line)) {
				return true;
			}
		}
		return false;
	}

	private String filterStack(String stackTrace, Collection<StringMatcher> filterPatterns) {
		if (filterPatterns == null || filterPatterns.isEmpty() || stackTrace == null)
			return stackTrace;

		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		StringReader stringReader = new StringReader(stackTrace);
		BufferedReader bufferedReader = new BufferedReader(stringReader);

		String line;
		boolean firstLine = true;
		try {
			while ((line = bufferedReader.readLine()) != null) {
				if (firstLine || !filterLine(filterPatterns, line))
					printWriter.println(line);
				firstLine = false;
			}
		} catch (IOException e) {
			return stackTrace; // return the stack unfiltered
		}
		return stringWriter.toString();
	}

	private boolean isAStackFrame(String itemLabel) {
		// heuristic for detecting a stack frame - works for JDK
		return itemLabel.startsWith(" at "); //$NON-NLS-1$
	}

	private String readLine(BufferedReader bufferedReader) throws IOException {
		String readLine = bufferedReader.readLine();
		return readLine == null ? null : readLine.replace('\t', ' ');
	}
}
