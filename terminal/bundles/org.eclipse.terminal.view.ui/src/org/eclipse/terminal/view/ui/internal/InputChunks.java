/*******************************************************************************
 * Copyright (c) 2026 Eclipse contributors and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.terminal.view.ui.internal;

/**
 * Where to cut the input written to a process in pieces.
 */
public final class InputChunks {

	private InputChunks() {
	}

	/**
	 * How much of the data to write next: at most {@code max} bytes from {@code from},
	 * but never ending inside a UTF-8 character or an escape sequence. Between two
	 * writes there is a pause, and ConPTY has passed the pieces of a sequence split
	 * across reads on as plain text (microsoft/terminal#16352): the end marker of a
	 * bracketed paste cut as "ESC[20" and "1~" reached the shell as the text "1~",
	 * and the shell then took everything typed after it as part of the paste.
	 *
	 * @return the number of bytes, at least 1
	 */
	public static int writeLength(byte[] data, int from, int max) {
		int end = from + max;
		if (end >= data.length) {
			return data.length - from;
		}
		while (end > from && (data[end] & 0xC0) == 0x80) {
			end--; // a UTF-8 continuation byte: the next write starts at its character
		}
		// an escape sequence that would not be complete in this write moves to the next
		for (int i = end - 1; i >= Math.max(from, end - 32); i--) {
			if (data[i] == 0x1b) {
				if (!isComplete(data, i, end)) {
					end = i;
				}
				break;
			}
		}
		return end > from ? end - from : Math.min(max, data.length - from);
	}

	/** whether the escape sequence at {@code esc} ends before {@code end} */
	private static boolean isComplete(byte[] data, int esc, int end) {
		if (esc + 1 >= end) {
			return false;
		}
		byte type = data[esc + 1];
		if (type == '[') {
			// CSI: parameters and intermediates up to a final byte
			for (int i = esc + 2; i < end; i++) {
				if (data[i] >= 0x40 && data[i] <= 0x7e) {
					return true;
				}
			}
			return false;
		}
		if (type == 'O') {
			return esc + 2 < end; // SS3, as an arrow key in application mode
		}
		return true; // ESC and one character (Alt with a key)
	}
}
