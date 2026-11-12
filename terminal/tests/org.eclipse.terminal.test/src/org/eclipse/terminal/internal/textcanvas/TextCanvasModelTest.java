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
	public void testHoverPrefixedSpaceIsBoundary() {
		TerminalTextData data = new TerminalTextData();
		data.setDimensions(1, 10);
		// the space shown in the cell ends a word, whatever prefix was written before it
		data.setChars(0, 0, "a b".toCharArray(), null);
		data.setCluster(0, 1, "\u0600 ");
		ITerminalTextDataSnapshot snapshot = data.makeSnapshot();
		snapshot.updateSnapshot(true);
		AbstractTextCanvasModel model = new AbstractTextCanvasModel(snapshot) {
		};
		model.expandHoverSelectionAt(0, 2);
		assertEquals("b", model.getHoverSelectionText());
		assertEquals(new Point(2, 0), model.getHoverSelectionStart());
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

	@Test
	public void testSelectionCoversWideClusterWhole() {
		TerminalTextData data = new TerminalTextData();
		data.setDimensions(1, 10);
		data.setChars(0, 0, "\u2764\000|".toCharArray(), null);
		data.setCluster(0, 0, "\u2764\ufe0f"); // a heart made wide by VS16
		ITerminalTextDataSnapshot snapshot = data.makeSnapshot();
		snapshot.updateSnapshot(true);
		AbstractTextCanvasModel model = new AbstractTextCanvasModel(snapshot) {
		};
		for (int col : new int[] { 0, 1 }) {
			model.setSelection(0, 0, col, col);
			assertEquals("\u2764\ufe0f", model.getSelectedText());
		}
	}

	@Test
	public void testCopiesClusters() {
		TerminalTextData data = new TerminalTextData();
		data.setDimensions(1, 12);
		// a black cat ends in a wide character of the BMP: the empty cells after it stay spaces
		data.setChars(0, 0, "\ud83d\udc08\000\000x".toCharArray(), null);
		data.setCluster(0, 0, "\ud83d\udc08\u200d\u2b1b");
		data.setChars(0, 6, new char[] { '\u00e9' }, null);
		data.setCluster(0, 6, "e\u0301");
		ITerminalTextDataSnapshot snapshot = data.makeSnapshot();
		snapshot.updateSnapshot(true);
		AbstractTextCanvasModel model = new AbstractTextCanvasModel(snapshot) {
		};
		model.setSelection(0, 0, 0, 11);
		assertEquals("\ud83d\udc08\u200d\u2b1b  x e\u0301", model.getSelectedText());
		assertEquals("\ud83d\udc08\u200d\u2b1b  x e\u0301", model.getAllText());
	}

	@Test
	public void testCopyKeepsWhatANarrowedScreenHolds() {
		TerminalTextData data = new TerminalTextData();
		data.setDimensions(1, 6);
		data.setChars(0, 0, "abcde\ud55c".toCharArray(), null);
		data.setDimensions(1, 3);
		ITerminalTextDataSnapshot snapshot = data.makeSnapshot();
		snapshot.updateSnapshot(true);
		AbstractTextCanvasModel model = new AbstractTextCanvasModel(snapshot) {
		};
		// the row comes whole, as it does again when the screen widens
		assertEquals("abcde\ud55c", model.getAllText());
		model.setSelection(0, 0, 0, 2);
		assertEquals("abcde\ud55c", model.getSelectedText());
		// a selection that ends inside the screen ends there
		model.setSelection(0, 0, 0, 1);
		assertEquals("ab", model.getSelectedText());
		// a pair the margin cuts comes whole, though the screen shows no half of it
		TerminalTextData pair = new TerminalTextData();
		pair.setDimensions(1, 4);
		pair.setChars(0, 0, "ab\ud83d\ude00".toCharArray(), null);
		pair.setDimensions(1, 3);
		ITerminalTextDataSnapshot narrowed = pair.makeSnapshot();
		narrowed.updateSnapshot(true);
		assertEquals("ab\ud83d\ude00", new AbstractTextCanvasModel(narrowed) {
		}.getAllText());
	}

	@Test
	public void testHoverWordKeepsClusters() {
		TerminalTextData data = new TerminalTextData();
		data.setDimensions(1, 8);
		data.setChars(0, 0, "a\u2764\000b c".toCharArray(), null);
		data.setCluster(0, 1, "\u2764\ufe0f"); // a heart made wide by VS16
		ITerminalTextDataSnapshot snapshot = data.makeSnapshot();
		snapshot.updateSnapshot(true);
		AbstractTextCanvasModel model = new AbstractTextCanvasModel(snapshot) {
		};
		model.expandHoverSelectionAt(0, 0);
		assertEquals("a\u2764\ufe0fb", model.getHoverSelectionText());
	}

	@Test
	public void testHoverAfterResizeAndWideClusters() {
		TerminalTextData data = new TerminalTextData();
		data.setDimensions(1, 5);
		data.setChars(0, 0, "abcde".toCharArray(), null);
		data.setDimensions(1, 4); // the row keeps all five characters
		ITerminalTextDataSnapshot snapshot = data.makeSnapshot();
		snapshot.updateSnapshot(true);
		AbstractTextCanvasModel model = new AbstractTextCanvasModel(snapshot) {
		};
		model.expandHoverSelectionAt(0, 1);
		assertEquals("abcd", model.getHoverSelectionText());
		// the empty cell after a wide character with a mark is still a gap
		data.setDimensions(1, 6);
		data.setChars(0, 0, "\u4e2d\000\000x".toCharArray(), null);
		data.setCluster(0, 0, "\u4e2d\u0301");
		snapshot.updateSnapshot(true);
		model.expandHoverSelectionAt(-1, -1); // let go of the last hover first
		model.expandHoverSelectionAt(0, 0);
		assertEquals("\u4e2d\u0301", model.getHoverSelectionText());
	}
}
