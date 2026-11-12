/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.terminal.internal.model;

import java.text.Normalizer;

import org.eclipse.terminal.model.ITerminalTextDataReadOnly;

/**
 * The characters of a line as its cells hold them, for everything that reads
 * them: the selection, copying, the word under the pointer, drawing and the
 * emulator. How many cells a character takes is kept by the text data
 * ({@link ITerminalTextDataReadOnly#getCellWidth(int, int)}); this puts the
 * character back together from them: a surrogate pair from its two cells, a
 * grapheme cluster from beside the line
 * ({@link ITerminalTextDataReadOnly#getCluster(int, int)}).
 */
public final class Cells {

	private Cells() {
	}

	/** @return the first cell of the character the cell belongs to */
	public static int start(ITerminalTextDataReadOnly data, int line, int col) {
		return col > 0 && data.getCellWidth(line, col) == 0 ? col - 1 : col;
	}

	/**
	 * @return the character starting in the cell as the program wrote it: a whole
	 *         grapheme cluster, both halves of a surrogate pair, or the one char (a
	 *         replacement character for half a pair); "" for the second cell of a
	 *         character and for a cell never written
	 */
	public static String text(ITerminalTextDataReadOnly data, int line, int col) {
		return text(data, line, col, false);
	}

	/**
	 * @param pastMargin whether the text goes on past the margin, where a narrowed
	 *            screen keeps what it no longer shows: a pair the margin cuts is whole
	 *            there
	 */
	private static String text(ITerminalTextDataReadOnly data, int line, int col, boolean pastMargin) {
		if (data.getCellWidth(line, col) == 0) {
			return ""; //$NON-NLS-1$
		}
		String cluster = data.getCluster(line, col);
		if (cluster != null) {
			return cluster;
		}
		char c = charAt(data, line, col);
		if (c == '\000') {
			return ""; //$NON-NLS-1$
		}
		if (Character.isHighSurrogate(c) && (data.getCellWidth(line, col) == 2
				|| pastMargin && Character.isLowSurrogate(charAt(data, line, col + 1)))) {
			return new String(new char[] { c, charAt(data, line, col + 1) });
		}
		// half a pair, its other half cut off by a narrowed screen, is no character
		return Character.isSurrogate(c) ? "\uFFFD" : String.valueOf(c); //$NON-NLS-1$
	}

	/**
	 * @return the char in the cell, also one a narrowed screen keeps past its margin
	 *         (0 where there is none)
	 */
	private static char charAt(ITerminalTextDataReadOnly data, int line, int col) {
		if (col < data.getWidth()) {
			return data.getChar(line, col);
		}
		char[] chars = data.getChars(line);
		return chars != null && col < chars.length ? chars[col] : 0;
	}

	/**
	 * The text of a whole row, also what a narrowed screen keeps past its margin and
	 * shows again when it widens.
	 */
	public static String row(ITerminalTextDataReadOnly data, int line, int from) {
		char[] chars = data.getChars(line);
		int to = Math.max(data.getWidth(), chars == null ? 0 : chars.length);
		return text(data, line, from, to, to > data.getWidth());
	}

	/**
	 * @return the character starting in the cell as it is drawn: a cluster with its
	 *         marks composed where they have a composed form (NFC)
	 */
	public static String drawn(ITerminalTextDataReadOnly data, int line, int col) {
		String text = text(data, line, col);
		return text.length() > 1 ? Normalizer.normalize(text, Normalizer.Form.NFC) : text;
	}

	/**
	 * The characters starting in the cells from <code>from</code> to
	 * <code>to</code> as they are drawn, a cell never written as a space: text that
	 * lines up cell for cell as long as each character is drawn as wide as the cells
	 * it takes.
	 *
	 * @param cells the chars of those cells, as
	 *            {@link ITerminalTextDataReadOnly#getLineSegments(int, int, int)}
	 *            has them
	 * @return the text, or null when a character the range starts or ends in is cut
	 *         by it
	 */
	public static String drawn(ITerminalTextDataReadOnly data, int line, int from, String cells) {
		if (data.isSimpleLine(line)) {
			return cells.replace('\000', ' '); // the chars are the text
		}
		int to = from + cells.length();
		if (data.getCellWidth(line, from) == 0) {
			return null;
		}
		StringBuilder drawn = new StringBuilder(cells.length());
		for (int col = from; col < to;) {
			int width = Math.max(data.getCellWidth(line, col), 1); // always on, whatever the cells hold
			if (col + width > to) {
				return null;
			}
			String cluster = data.getCluster(line, col);
			char c = cells.charAt(col - from);
			if (cluster != null) {
				drawn.append(Normalizer.normalize(cluster, Normalizer.Form.NFC));
			} else if (c == '\000') {
				drawn.append(' ');
			} else {
				drawn.append(c);
				if (width == 2 && Character.isHighSurrogate(c)) {
					drawn.append(cells.charAt(col - from + 1));
				}
			}
			col += width;
		}
		return drawn.toString();
	}

	/**
	 * The text of the characters starting in the cells from <code>from</code> to
	 * <code>to</code>: a cell never written stands for a space, and those at the
	 * end of the range are left out.
	 */
	public static String text(ITerminalTextDataReadOnly data, int line, int from, int to) {
		return text(data, line, from, to, false);
	}

	private static String text(ITerminalTextDataReadOnly data, int line, int from, int to, boolean pastMargin) {
		StringBuilder text = new StringBuilder(to - from);
		int written = 0;
		for (int col = from; col < to; col++) {
			if (data.getCellWidth(line, col) == 0) {
				continue;
			}
			String c = text(data, line, col, pastMargin);
			if (c.isEmpty()) {
				text.append(' ');
			} else {
				text.append(c);
				written = text.length();
			}
		}
		text.setLength(written);
		return text.toString();
	}
}
