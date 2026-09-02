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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class CellsTest {

	private static TerminalTextDataStore line(String cells, int width) {
		TerminalTextDataStore data = new TerminalTextDataStore();
		data.setDimensions(1, width);
		data.setChars(0, 0, cells.toCharArray(), null);
		return data;
	}

	private static String widths(TerminalTextDataStore data) {
		StringBuilder widths = new StringBuilder();
		for (int col = 0; col < data.getWidth(); col++) {
			widths.append(data.getCellWidth(0, col));
		}
		return widths.toString();
	}

	@Test
	public void testWideCharacters() {
		// a wide character of the BMP and its filler, a pair beyond it, and empty cells
		TerminalTextDataStore data = line("\uac00\000\ud83d\ude00a\000", 6);
		assertEquals("202011", widths(data));
		assertEquals(0, Cells.start(data, 0, 1));
		assertEquals(2, Cells.start(data, 0, 3));
		assertEquals("\uac00", Cells.text(data, 0, 0));
		assertEquals("", Cells.text(data, 0, 1));
		assertEquals("\ud83d\ude00", Cells.text(data, 0, 2));
		assertEquals("", Cells.text(data, 0, 5)); // never written
		// only the first null after a wide character is its filler
		assertEquals("2011", widths(line("\uac00\000\000a", 4)));
		// a lone low surrogate is a character of its own, and so is one cut off by the margin
		assertEquals("11", widths(line("\ude00a", 2)));
		assertEquals("11", widths(line("a\uac00", 2)));
	}

	@Test
	public void testClusters() {
		TerminalTextDataStore data = line("\u2764\000e|", 4);
		data.setCluster(0, 0, "\u2764\ufe0f"); // a heart made wide by VS16
		data.setCluster(0, 2, "e\u0301"); // an accent with its letter, in one cell
		assertEquals("2011", widths(data));
		assertEquals("\u2764\ufe0f", Cells.text(data, 0, 0));
		assertEquals("\u00e9", Cells.drawn(data, 0, 2));
		assertEquals("e\u0301", Cells.text(data, 0, 2));
		// a cluster of two cells needs its second one: written over, it is one cell
		data.setChars(0, 0, "\u2764x".toCharArray(), null);
		assertEquals("1111", widths(data));
	}

	@Test
	public void testTextOfRange() {
		TerminalTextDataStore data = line("a\000\uac00\000b\000\000", 7);
		assertEquals("a \uac00b", Cells.text(data, 0, 0, 7)); // never written: a space, none at the end
		assertEquals(" \uac00", Cells.text(data, 0, 1, 4)); // the empty cell after a narrow character is a space
		assertEquals("\uac00", Cells.text(data, 0, 2, 4));
		assertEquals("b", Cells.text(data, 0, 3, 7)); // a character that starts before the range is not in it
	}
}
