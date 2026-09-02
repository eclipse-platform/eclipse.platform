/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.terminal.internal.emulator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Random;

import org.eclipse.terminal.internal.model.Cells;
import org.eclipse.terminal.internal.model.TerminalTextDataStore;
import org.eclipse.terminal.internal.textcanvas.AbstractTextCanvasModel;
import org.eclipse.terminal.model.ITerminalTextData;
import org.eclipse.terminal.model.ITerminalTextDataSnapshot;
import org.eclipse.terminal.model.LineSegment;
import org.eclipse.terminal.model.TerminalTextDataFactory;
import org.junit.jupiter.api.Test;

/**
 * Random writes and edits, checking after every step that the cells hold what
 * {@link Cells} says they hold: what any path that writes cells has to keep.
 */
public class VT100EmulatorCellsTest {

	/** characters that are hard to lay out, and some that are not */
	private static final String[] PIECES = { "a", "x", " ", "1", "e", "\u4e2d", "\uac00", "\ud83d\ude00",
			"\ud83d\udc68", "\u200d", "\ufe0f", "\ufe0e", "\u0301", "\u3099", "\u30ab", "\u0600", "\u200b",
			"\ud83c\uddf0", "\ud83c\uddf7", "\ud83c\udffd", "\u1100", "\u1161", "\u11a8", "\u2764", "\u20e3",
			"\u0915", "\u093f", "\ud87e\udc00", "\ufa6c", "\u3000" };

	@Test
	public void testRandomEditsKeepTheCellsConsistent() {
		Random random = new Random(2904);
		for (int run = 0; run < 400; run++) {
			ITerminalTextData term = TerminalTextDataFactory.makeTerminalTextData();
			VT100EmulatorBackend vt100 = new VT100EmulatorBackend(term);
			term.setMaxHeight(6);
			vt100.setDimensions(1 + random.nextInt(4), 1 + random.nextInt(6));
			vt100.setVT100LineWrapping(random.nextBoolean());
			StringBuilder log = new StringBuilder();
			for (int step = 0; step < 60; step++) {
				String op = step(vt100, random);
				log.append(op).append("; "); //$NON-NLS-1$
				check(term, log);
			}
		}
	}

	@Test
	public void testSplitWritesGiveTheSameCells() {
		Random random = new Random(29041);
		for (int run = 0; run < 2000; run++) {
			StringBuilder text = new StringBuilder();
			for (int i = random.nextInt(8); i >= 0; i--) {
				text.append(PIECES[random.nextInt(PIECES.length)]);
			}
			int columns = 2 + random.nextInt(6);
			boolean deferred = random.nextBoolean();
			String whole = cells(text.toString(), new int[0], columns, deferred);
			if (text.chars().noneMatch(c -> c == 0x200b || c == 0xfe0e || c == 0xfe0f)
					&& !text.toString().contains("\u0600\u0600")) { //$NON-NLS-1$
				// what is written comes back whole, but for what is kept out because it only hides text
				assertEquals(escape(text.toString()), escape(copied(text.toString(), columns, deferred)),
						"copied back"); //$NON-NLS-1$
			}
			int[] splits = random.ints(1 + random.nextInt(3), 0, text.length() + 1).sorted().toArray();
			assertEquals(whole, cells(text.toString(), splits, columns, deferred),
					"split at " + java.util.Arrays.toString(splits) + " of " + escape(text.toString())); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/** @return all the text, as copying it gives it back, after writing it alone */
	private static String copied(String text, int columns, boolean deferred) {
		ITerminalTextData term = TerminalTextDataFactory.makeTerminalTextData();
		VT100EmulatorBackend vt100 = new VT100EmulatorBackend(term);
		term.setMaxHeight(20);
		vt100.setDimensions(20, columns);
		vt100.setVT100LineWrapping(deferred);
		vt100.appendString(text);
		ITerminalTextDataSnapshot snapshot = term.makeSnapshot();
		snapshot.updateSnapshot(true);
		String all = new AbstractTextCanvasModel(snapshot) {
		}.getAllText();
		return all.substring(0, all.indexOf('\n') < 0 ? all.length() : all.indexOf('\n'));
	}

	private static String cells(String text, int[] splits, int columns, boolean deferred) {
		ITerminalTextData term = new TerminalTextDataStore();
		VT100EmulatorBackend vt100 = new VT100EmulatorBackend(term);
		term.setMaxHeight(8);
		vt100.setDimensions(4, columns);
		vt100.setVT100LineWrapping(deferred);
		int from = 0;
		for (int split : splits) {
			vt100.appendString(text.substring(from, split));
			from = split;
		}
		vt100.appendString(text.substring(from));
		StringBuilder cells = new StringBuilder();
		for (int line = 0; line < term.getHeight(); line++) {
			for (int col = 0; col < columns; col++) {
				cells.append(term.getCellWidth(line, col)).append(escape(Cells.text(term, line, col))).append('|');
			}
			cells.append('\n');
		}
		return cells.append(vt100.getCursorLine()).append(',').append(vt100.getCursorColumn()).toString();
	}

	private static String step(VT100EmulatorBackend vt100, Random random) {
		int lines = vt100.getLines();
		int columns = vt100.getColumns();
		switch (random.nextInt(20)) {
		case 0:
			vt100.setCursor(random.nextInt(lines), random.nextInt(columns));
			return "cup"; //$NON-NLS-1$
		case 1:
			vt100.setCursorColumn(0);
			return "cr"; //$NON-NLS-1$
		case 2:
			vt100.processNewline();
			return "lf"; //$NON-NLS-1$
		case 3:
			vt100.eraseCharacters(1 + random.nextInt(3));
			return "ech"; //$NON-NLS-1$
		case 4:
			vt100.insertCharacters(1 + random.nextInt(3));
			return "ich"; //$NON-NLS-1$
		case 5:
			vt100.deleteCharacters(1 + random.nextInt(3));
			return "dch"; //$NON-NLS-1$
		case 6:
			vt100.eraseLineToEnd();
			return "el0"; //$NON-NLS-1$
		case 7:
			vt100.eraseLineToCursor();
			return "el1"; //$NON-NLS-1$
		case 8:
			vt100.insertLines(1);
			return "il"; //$NON-NLS-1$
		case 9:
			vt100.deleteLines(1);
			return "dl"; //$NON-NLS-1$
		case 10:
			vt100.scrollUp(1);
			return "su"; //$NON-NLS-1$
		case 11:
			vt100.scrollDown(1);
			return "sd"; //$NON-NLS-1$
		case 12:
			int newColumns = 1 + random.nextInt(6);
			vt100.setDimensions(lines, newColumns);
			return "cols " + newColumns; //$NON-NLS-1$
		case 13:
			boolean on = random.nextBoolean();
			vt100.setInsertMode(on);
			return "irm " + on; //$NON-NLS-1$
		case 14:
			boolean alternate = random.nextBoolean();
			vt100.enableAlternateScreen(alternate, true);
			return "alt " + alternate; //$NON-NLS-1$
		case 15:
			vt100.processReverseLineFeed();
			return "ri"; //$NON-NLS-1$
		default:
			StringBuilder text = new StringBuilder();
			for (int i = random.nextInt(4); i >= 0; i--) {
				text.append(PIECES[random.nextInt(PIECES.length)]);
			}
			vt100.appendString(text.toString());
			return "write " + escape(text.toString()); //$NON-NLS-1$
		}
	}

	private static void check(ITerminalTextData term, CharSequence log) {
		int width = term.getWidth();
		for (int line = 0; line < term.getHeight(); line++) {
			for (int col = 0; col < width; col++) {
				int cells = term.getCellWidth(line, col);
				if (cells == 2 && (col + 1 >= width || term.getCellWidth(line, col + 1) != 0)) {
					fail("two cells without a second one at " + line + "," + col + " after " + log); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				if (cells == 0 && term.getCluster(line, col) != null) {
					fail("a cluster on a second cell at " + line + "," + col + " after " + log); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				String text = Cells.text(term, line, col);
				for (int i = 0; i < text.length(); i++) {
					char c = text.charAt(i);
					boolean paired = Character.isHighSurrogate(c) ? i + 1 < text.length() && Character.isLowSurrogate(text.charAt(i + 1))
							: !Character.isLowSurrogate(c) || i > 0 && Character.isHighSurrogate(text.charAt(i - 1));
					if (!paired) {
						fail("half a surrogate pair at " + line + "," + col + " after " + log); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			}
		}
		// selecting, copying and the word under the pointer read the same cells
		ITerminalTextDataSnapshot snapshot = term.makeSnapshot();
		snapshot.updateSnapshot(true);
		AbstractTextCanvasModel model = new AbstractTextCanvasModel(snapshot) {
		};
		model.getAllText();
		for (int line = 0; line < term.getHeight(); line++) {
			for (LineSegment segment : snapshot.getLineSegments(line, 0, width)) {
				Cells.drawn(snapshot, line, segment.getColumn(), segment.getText()); // what is drawn
			}
		}
		for (int line = 0; line < term.getHeight(); line++) {
			for (int col = 0; col < width; col++) {
				model.setSelection(line, line, col, col);
				model.expandHoverSelectionAt(-1, -1);
				model.expandHoverSelectionAt(line, col);
			}
		}
		snapshot.detach();
	}

	private static String escape(String text) {
		StringBuilder escaped = new StringBuilder();
		text.chars().forEach(c -> escaped.append(c < 0x80 ? Character.toString(c) : String.format("\\u%04x", c))); //$NON-NLS-1$
		return escaped.toString();
	}
}
