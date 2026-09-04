/*******************************************************************************
 * Copyright (c) 2026 SAP
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * SAP - initial implementation
 *******************************************************************************/
package org.eclipse.compare.unifieddiff.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;

/**
 * Side-effect free text and style-range helpers used to lay out the unified
 * diff code minings.
 */
public final class UnifiedDiffText {

	private UnifiedDiffText() {
	}

	/**
	 * Returns the number of lines of the given string, counting a trailing line
	 * delimiter as starting a new (empty) line. An empty string is one line.
	 */
	public static int countLines(String str) {
		if (str == null) {
			return 0;
		}
		return countLines(str, str.length());
	}

	/**
	 * Returns the number of lines of the first {@code end} characters of the
	 * given string, like {@link #countLines(String)} on that prefix.
	 */
	public static int countLines(String str, int end) {
		int result = 1;
		for (int i = 0; i < end; i++) {
			if (str.charAt(i) == '\n') {
				result++;
			}
		}
		return result;
	}

	public static String removeTrailingNewLines(String str) {
		if (str == null) {
			return str;
		}
		int end = str.length();
		while (end > 0 && (str.charAt(end - 1) == '\n' || str.charAt(end - 1) == '\r')) {
			end--;
		}
		return end == str.length() ? str : str.substring(0, end);
	}

	public static String removeLeadingNewLines(String str) {
		if (str == null) {
			return str;
		}
		int start = 0;
		while (start < str.length() && (str.charAt(start) == '\n' || str.charAt(start) == '\r')) {
			start++;
		}
		return start == 0 ? str : str.substring(start);
	}

	/**
	 * Expands every tab into {@code tabWidth} spaces. {@code GC#stringExtent} does
	 * not account for tabs, so widths are always measured on the expanded string.
	 */
	public static String replaceTabWithSpaces(String str, int tabWidth) {
		if (str == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		for (int s = 0; s < tabWidth; s++) {
			sb.append(' ');
		}
		return str.replace("\t", sb); //$NON-NLS-1$
	}

	/**
	 * Maps an offset in {@code originalStr} to the corresponding offset in
	 * {@link #replaceTabWithSpaces(String, int)} of the same string.
	 */
	public static int mapOffsetToTabExpanded(String originalStr, int offset, int tabWidth) {
		int tabCount = 0;
		int limit = Math.min(offset, originalStr.length());
		for (int i = 0; i < limit; i++) {
			if (originalStr.charAt(i) == '\t') {
				tabCount++;
			}
		}
		return offset + tabCount * (tabWidth - 1);
	}

	/**
	 * Overlays the detailed-diff background ranges onto the syntax coloring ranges.
	 * Both lists must be sorted by offset and must not overlap within themselves.
	 * The returned ranges keep the foreground styling and cover exactly the extent
	 * of {@code foregrounds}.
	 */
	public static List<StyleRange> mergeStyleRanges(List<StyleRange> backgrounds, List<StyleRange> foregrounds) {
		if (backgrounds.isEmpty()) {
			return foregrounds;
		}
		if (foregrounds.isEmpty()) {
			return backgrounds;
		}
		List<StyleRange> result = new ArrayList<>();
		int bgIdx = 0;
		for (StyleRange fg : foregrounds) {
			int fgStart = fg.start;
			int fgEnd = fg.start + fg.length;
			int pos = fgStart;
			while (pos < fgEnd && bgIdx < backgrounds.size()) {
				StyleRange bg = backgrounds.get(bgIdx);
				int bgStart = bg.start;
				int bgEnd = bg.start + bg.length;
				if (bgEnd <= pos) {
					bgIdx++;
					continue;
				}
				if (bgStart >= fgEnd) {
					break;
				}
				if (bgStart > pos) {
					result.add(createRange(fg, pos, bgStart - pos, null));
					pos = bgStart;
				}
				int overlapEnd = Math.min(bgEnd, fgEnd);
				result.add(createRange(fg, pos, overlapEnd - pos, bg.background));
				pos = overlapEnd;
				if (bgEnd <= fgEnd) {
					bgIdx++;
				}
			}
			if (pos < fgEnd) {
				result.add(createRange(fg, pos, fgEnd - pos, null));
			}
		}
		return result;
	}

	private static StyleRange createRange(StyleRange base, int start, int length, Color background) {
		StyleRange r = (StyleRange) base.clone();
		r.start = start;
		r.length = length;
		if (background != null) {
			r.background = background;
		}
		return r;
	}
}
