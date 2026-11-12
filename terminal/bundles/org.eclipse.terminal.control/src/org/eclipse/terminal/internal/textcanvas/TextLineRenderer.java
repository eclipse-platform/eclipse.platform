/*******************************************************************************
 * Copyright (c) 2007, 2018 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Michael Scharf (Wind River) - initial API and implementation
 * Michael Scharf (Wind River) - [205260] Terminal does not take the font from the preferences
 * Michael Scharf (Wind River) - [206328] Terminal does not draw correctly with proportional fonts
 * Anton Leherbauer (Wind River) - [294468] Fix scroller and text line rendering
 * Martin Oberhuber (Wind River) - [265352][api] Allow setting fonts programmatically
 *******************************************************************************/
package org.eclipse.terminal.internal.textcanvas;

import java.util.Map;
import java.util.function.Supplier;

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Drawable;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.terminal.connector.Logger;
import org.eclipse.terminal.internal.model.Cells;
import org.eclipse.terminal.model.ITerminalTextDataReadOnly;
import org.eclipse.terminal.model.LineSegment;
import org.eclipse.terminal.model.TerminalColor;
import org.eclipse.terminal.model.TerminalStyle;

/**
 *
 */
public class TextLineRenderer implements ILinelRenderer {
	private static final boolean DEBUG_HOVER = Platform.getDebugBoolean(Logger.TRACE_DEBUG_LOG_HOVER);
	private final ITextCanvasModel fModel;
	private final StyleMap fStyleMap;

	public TextLineRenderer(Supplier<Drawable> usageContextProvider, ITextCanvasModel model) {
		fModel = model;
		fStyleMap = new StyleMap(usageContextProvider);
	}

	@Override
	public int getCellWidth() {
		return fStyleMap.getFontWidth();
	}

	@Override
	public int getCellHeight() {
		return fStyleMap.getFontHeight();
	}

	@Override
	public void drawLine(ITextCanvasModel model, GC gc, int line, int x, int y, int colFirst, int colLast) {
		int width = getCellWidth() * (colLast - colFirst);
		int height = getCellHeight();
		if (width <= 0 || height <= 0) {
			return;
		}
		if (line < 0 || line >= getTerminalText().getHeight() || colFirst >= getTerminalText().getWidth()
				|| colFirst - colLast == 0) {
			fillBackground(gc, x, y, width, height);
		} else {
			colLast = Math.min(colLast, getTerminalText().getWidth());
			LineSegment[] segments = getTerminalText().getLineSegments(line, colFirst, colLast - colFirst);
			for (int i = 0; i < segments.length; i++) {
				LineSegment segment = segments[i];
				TerminalStyle style = segment.getStyle();
				setupGC(gc, style);
				drawText(gc, line, x, y, colFirst, segment.getColumn(), segment.getText());
				drawCursor(model, gc, line, x, y, colFirst);
			}
			if (fModel.hasHoverSelection(line)) {
				if (DEBUG_HOVER) {
					System.out.format("hover: %s  contains hover selection\n", line); //$NON-NLS-1$
				}
				Point hsStart = fModel.getHoverSelectionStart();
				Point hsEnd = fModel.getHoverSelectionEnd();
				int colStart = line == hsStart.y ? hsStart.x : 0;
				int colEnd = line == hsEnd.y ? hsEnd.x : getTerminalText().getWidth();
				if (colStart < colEnd) {
					Color defaultFg = fStyleMap.getForegroundColor(null);
					gc.setForeground(defaultFg);
					drawUnderline(gc, x, y, colStart, colEnd);
				}
			}
			if (fModel.hasLineSelection(line)) {
				TerminalStyle style = TerminalStyle.getStyle(TerminalColor.SELECTION_FOREGROUND,
						TerminalColor.SELECTION_BACKGROUND);
				setupGC(gc, style);
				Point start = model.getSelectionStart();
				Point end = model.getSelectionEnd();
				// Everything the selection covers on this line goes down first. Drawing
				// it as the text is drawn leaves out whatever the text does not reach:
				// the cells past the last character a program put on the line, and the
				// cells where the font draws a glyph narrower than the one it sits in.
				int from = Math.max(start.y == line ? start.x : 0, colFirst);
				int to = end.y == line ? Math.min(end.x + 1, colLast) : colLast;
				if (to > from) {
					gc.fillRectangle(x + (from - colFirst) * getCellWidth(), y, (to - from) * getCellWidth(),
							getCellHeight());
				}
				char[] chars = model.getTerminalText().getChars(line);
				if (chars != null) {
					int offset = 0;
					if (start.y == line) {
						offset = start.x;
					}
					offset = Math.max(offset, colFirst);
					int len;
					if (end.y == line) {
						len = end.x - offset + 1;
					} else {
						len = chars.length - offset + 1;
					}
					len = Math.min(len, chars.length - offset);
					if (len > 0) {
						drawText(gc, line, x, y, colFirst, offset, new String(chars, offset, len));
					}
				}
			}
		}
	}

	private void fillBackground(GC gc, int x, int y, int width, int height) {
		Color bg = gc.getBackground();
		gc.setBackground(getDefaultBackgroundColor());
		gc.fillRectangle(x, y, width, height);
		gc.setBackground(bg);

	}

	@Override
	public Color getDefaultBackgroundColor() {
		return fStyleMap.getBackgroundColor(null);
	}

	private void drawCursor(ITextCanvasModel model, GC gc, int row, int x, int y, int colFirst) {
		if (!model.isCursorOn()) {
			return;
		}
		int cursorLine = model.getCursorLine();

		if (row == cursorLine) {
			int cursorColumn = model.getCursorColumn();
			if (cursorColumn < getTerminalText().getWidth()) {
				// On a wide character the cursor covers both of its cells, whichever
				// one it is on, as in xterm: half a glyph cannot be drawn reversed.
				cursorColumn = Cells.start(getTerminalText(), row, cursorColumn);
				int cells = getTerminalText().getCellWidth(row, cursorColumn);
				TerminalStyle style = getTerminalText().getStyle(row, cursorColumn);
				if (style == null) {
					// TODO make the cursor color customizable
					style = TerminalStyle.getStyle(TerminalColor.FOREGROUND, TerminalColor.BACKGROUND);
				}
				style = style.setReverse(!style.isReverse());
				setupGC(gc, style);
				Rectangle clipping = gc.getClipping();
				gc.setClipping(clipping.intersection(new Rectangle(x + (cursorColumn - colFirst) * getCellWidth(), y,
						cells * getCellWidth(), getCellHeight())));
				char[] text = new char[cells];
				for (int i = 0; i < cells; i++) {
					text[i] = getTerminalText().getChar(row, cursorColumn + i); // a null past what was written
				}
				drawText(gc, row, x, y, colFirst, cursorColumn, new String(text));
				gc.setClipping(clipping);
			}
		}
	}

	/**
	 * Draws the characters that start in the cells from <code>col</code> whose
	 * chars are <code>cells</code>, each as {@link Cells} has it: a cluster whole,
	 * over the cells it takes.
	 */
	private void drawText(GC gc, int line, int x, int y, int colFirst, int col, String cells) {
		ITerminalTextDataReadOnly data = getTerminalText();
		cells = cells.substring(0, Math.max(0, Math.min(cells.length(), data.getWidth() - col)));
		int end = col + cells.length();
		int offset = (col - colFirst) * getCellWidth();
		if (fStyleMap.isFontProportional()) {
			int xx = x + offset;
			for (int c = col; c < end;) {
				int width = Math.max(data.getCellWidth(line, c), 1);
				// TODO why do I have to draw the background character by character??????
				gc.fillRectangle(xx, y, width * fStyleMap.getFontWidth(), fStyleMap.getFontHeight());
				String drawn = Cells.drawn(data, line, c);
				if (!drawn.isBlank()) {
					// placed by the character the cell shows, not a prefix written before it
					gc.drawString(drawn, fStyleMap.getCharOffset(data.getChar(line, c)) + xx, y, false);
				}
				xx += width * fStyleMap.getFontWidth();
				c += width;
			}
			return;
		}
		// One call keeps whatever the font does with the run, ligatures included,
		// but only while it advances exactly one cell per column. A character the
		// font does not have is drawn from somewhere else and rarely does, and
		// then everything after it on the line sits in the wrong column.
		String drawn = Cells.drawn(data, line, col, cells);
		if (drawn != null && gc.textExtent(drawn).x == (end - col) * getCellWidth()) {
			gc.drawString(drawn, x + offset, y, false);
		} else {
			drawCellByCell(gc, data, line, col, end, x + offset, y);
		}
	}

	/**
	 * Puts every character at the start of its own cell, so the columns hold no
	 * matter what the font makes of it. A character that takes two cells is left to
	 * cover the second one.
	 */
	private void drawCellByCell(GC gc, ITerminalTextDataReadOnly data, int line, int col, int end, int x, int y) {
		// The whole run at once, because the characters are drawn over it one at a
		// time and the cells between them would otherwise keep what was there before.
		gc.fillRectangle(x, y, (end - col) * getCellWidth(), getCellHeight());
		for (int c = col; c < end; c++) {
			String drawn = Cells.drawn(data, line, c);
			if (!drawn.isBlank()) {
				gc.drawString(drawn, x + (c - col) * getCellWidth(), y, true);
			}
		}
	}

	/**
	 *
	 * @param gc
	 * @param colStart Starting text column to underline (inclusive)
	 * @param colEnd Ending text column to underline (inclusive)
	 */
	private void drawUnderline(GC gc, int xOffset, int yOffset, int colStart, int colEnd) {
		int y = yOffset + getCellHeight() - 1;
		int x = xOffset + getCellWidth() * colStart;

		// x2 is the right side of last column being underlined.
		int x2 = (colEnd + 1) * getCellWidth() - 1;
		gc.drawLine(x, y, x2, y);
	}

	private void setupGC(GC gc, TerminalStyle style) {
		Color foregroundColor = fStyleMap.getForegroundColor(style);
		gc.setForeground(foregroundColor);
		Color backgroundColor = fStyleMap.getBackgroundColor(style);
		gc.setBackground(backgroundColor);

		Font f = fStyleMap.getFont(style);
		if (f != gc.getFont()) {
			gc.setFont(f);
		}
	}

	ITerminalTextDataReadOnly getTerminalText() {
		return fModel.getTerminalText();
	}

	@Override
	public void updateFont(String fontName) {
		fStyleMap.updateFont(fontName);
	}

	@Override
	public void updateColors(Map<TerminalColor, Color> map) {
		fStyleMap.updateColors(map);
	}

	@Override
	public void setInvertedColors(boolean invert) {
		fStyleMap.setInvertedColors(invert);

	}

	@Override
	public boolean isInvertedColors() {
		return fStyleMap.isInvertedColors();
	}
}
