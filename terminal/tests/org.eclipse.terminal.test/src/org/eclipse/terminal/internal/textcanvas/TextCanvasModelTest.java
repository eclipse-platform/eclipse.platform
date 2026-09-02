/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.terminal.internal.textcanvas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.swt.graphics.Point;
import org.eclipse.terminal.internal.model.TerminalTextData;
import org.eclipse.terminal.model.ITerminalTextDataSnapshot;
import org.junit.jupiter.api.Test;

public class TextCanvasModelTest {

	@Test
	public void testHoverWordWithWideCharacters() {
		TerminalTextData data = new TerminalTextData();
		data.setDimensions(1, 20);
		// cells: a wide character is followed by its filler
		String cells = "ls /tmp/한\000글\000.txt x";
		data.setChars(0, 0, cells.toCharArray(), null);
		ITerminalTextDataSnapshot snapshot = data.makeSnapshot();
		snapshot.updateSnapshot(true);
		AbstractTextCanvasModel model = new AbstractTextCanvasModel(snapshot) {
		};
		// the filler of a wide character does not end the word, from wherever it is hovered
		for (int col : new int[] { 3, 8, 9, 15 }) {
			model.expandHoverSelectionAt(0, col);
			assertEquals("/tmp/한글.txt", model.getHoverSelectionText());
			assertEquals(new Point(3, 0), model.getHoverSelectionStart());
			assertEquals(new Point(15, 0), model.getHoverSelectionEnd());
		}
	}

	@Test
	public void testHoverWideSpaceIsBoundary() {
		TerminalTextData data = new TerminalTextData();
		data.setDimensions(1, 10);
		// an ideographic space is wide whitespace: both of its cells end a word
		data.setChars(0, 0, "a\u3000\000b c".toCharArray(), null);
		ITerminalTextDataSnapshot snapshot = data.makeSnapshot();
		snapshot.updateSnapshot(true);
		AbstractTextCanvasModel model = new AbstractTextCanvasModel(snapshot) {
		};
		model.expandHoverSelectionAt(0, 2); // the space's filler
		assertEquals("", model.getHoverSelectionText());
		model.expandHoverSelectionAt(0, 3);
		assertEquals("b", model.getHoverSelectionText());
		assertEquals(new Point(3, 0), model.getHoverSelectionStart());
	}

	@Test
	public void testSelectionCoversWideCharactersWhole() {
		TerminalTextData data = new TerminalTextData();
		data.setDimensions(1, 10);
		data.setChars(0, 0, "\uD55C\000\uAE00\000".toCharArray(), null);
		ITerminalTextDataSnapshot snapshot = data.makeSnapshot();
		snapshot.updateSnapshot(true);
		AbstractTextCanvasModel model = new AbstractTextCanvasModel(snapshot) {
		};
		// from the right half of the first character to the left half of the second
		model.setSelection(0, 0, 1, 2);
		assertEquals("\uD55C\uAE00", model.getSelectedText());
		assertEquals(new Point(0, 0), model.getSelectionStart());
		assertEquals(new Point(3, 0), model.getSelectionEnd());
	}

	@Test
	public void testCopyKeepsSpacesBeforeAFold() {
		TerminalTextData data = new TerminalTextData();
		data.setDimensions(2, 4);
		// "foo bar" folded at four columns: the space ends the first row
		data.setChars(0, 0, "foo ".toCharArray(), null);
		data.setWrappedLine(0);
		data.setChars(1, 0, "bar".toCharArray(), null);
		ITerminalTextDataSnapshot snapshot = data.makeSnapshot();
		snapshot.updateSnapshot(true);
		AbstractTextCanvasModel model = new AbstractTextCanvasModel(snapshot) {
		};
		model.setSelection(0, 1, 0, 3);
		assertEquals("foo bar", model.getSelectedText());
	}
}
