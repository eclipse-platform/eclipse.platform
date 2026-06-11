/*******************************************************************************
 * Copyright (c) 2007, 2018 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Michael Scharf (Wind River) - initial API and implementation
 * Anton Leherbauer (Wind River) - [206329] Changing terminal size right after connect does not scroll properly
 * Anton Leherbauer (Wind River) - [433751] Add option to enable VT100 line wrapping mode
 * Anton Leherbauer (Wind River) - [458218] Add support for ANSI insert mode
 * Anton Leherbauer (Wind River) - [458402] Add support for scroll up/down and scroll region
 *******************************************************************************/
package org.eclipse.terminal.internal.emulator;

import org.eclipse.terminal.internal.model.CharWidth;
import org.eclipse.terminal.internal.model.TerminalTextDataStore;
import org.eclipse.terminal.model.ITerminalTextData;
import org.eclipse.terminal.model.TerminalStyle;

/**
 * @noextend This class is not intended to be subclassed by clients.
 * @noreference This class not intended to be referenced by clients.
 *      It used to be package protected, and it is public only for Unit Tests.
 *
 */
public class VT100EmulatorBackend implements IVT100EmulatorBackend {

	private static class ScrollRegion {
		static final ScrollRegion FULL_WINDOW = new ScrollRegion(0, Integer.MAX_VALUE - 1);
		private final int fTop;
		private final int fBottom;

		ScrollRegion(int top, int bottom) {
			fTop = top;
			fBottom = bottom;
		}

		boolean contains(int line) {
			return line >= fTop && line <= fBottom;
		}

		int getTopLine() {
			return fTop;
		}

		int getBottomLine() {
			return fBottom;
		}

		int getHeight() {
			return fBottom - fTop + 1;
		}
	}

	/**
	 * This field holds the number of the column in which the cursor is
	 * logically positioned. The leftmost column on the screen is column 0, and
	 * column numbers increase to the right. The maximum value of this field is
	 * {@link #widthInColumns} - 1. We track the cursor column using this field
	 * to avoid having to recompute it repeatly using StyledText method calls.
	 * <p>
	 *
	 * The StyledText widget that displays text has a vertical bar (called the
	 * "caret") that appears _between_ character cells, but ANSI terminals have
	 * the concept of a cursor that appears _in_ a character cell, so we need a
	 * convention for which character cell the cursor logically occupies when
	 * the caret is physically between two cells. The convention used in this
	 * class is that the cursor is logically in column N when the caret is
	 * physically positioned immediately to the _left_ of column N.
	 * <p>
	 *
	 * When fCursorColumn is N, the next character output to the terminal appears
	 * in column N. When a character is output to the rightmost column on a
	 * given line (column widthInColumns - 1), the cursor moves to column 0 on
	 * the next line after the character is drawn (this is the default line wrapping
	 * mode). If VT100 line wrapping mode is enabled, the cursor does not move
	 * to the next line until the next character is printed (this is known as
	 * the VT100 'eat_newline_glitch').
	 * If the cursor is in the bottommost line when line wrapping
	 * occurs, the topmost visible line is scrolled off the top edge of the
	 * screen.
	 * <p>
	 */
	private int fCursorColumn;
	private int fCursorLine;
	/* true if last output occurred on rightmost column
	 * and next output requires line wrap */
	private boolean fWrapPending;
	private boolean fInsertMode;
	private TerminalStyle fDefaultStyle;
	private TerminalStyle fStyle;
	int fLines;
	int fColumns;
	final private ITerminalTextData fTerminal;

	/**
	 * What the normal screen held while the alternate one is showing, and null the
	 * rest of the time, which is also how we know which screen we are on.
	 */
	private ITerminalTextData fNormalScreen;
	private int fNormalMaxHeight;
	/** in the normal buffer, not on the screen: the screen may be resized meanwhile */
	private int fNormalCursorLine;
	private int fNormalCursorColumn;
	/** the screen the normal buffer was saved from, and whether a wrap was pending on it */
	private int fNormalLines;
	private int fNormalColumns;
	private boolean fNormalWrapPending;
	/** the rendition the shell had selected: 1049 saves it with the cursor, as DECSC does */
	private TerminalStyle fNormalStyle;
	private boolean fVT100LineWrapping;
	private ScrollRegion fScrollRegion = ScrollRegion.FULL_WINDOW;

	public VT100EmulatorBackend(ITerminalTextData terminal) {
		fTerminal = terminal;
	}

	@Override
	public void enableAlternateScreen(boolean enable, boolean cursor) {
		synchronized (fTerminal) {
			if (enable == (fNormalScreen != null)) {
				// Already on the screen being asked for. Programs do ask twice.
				return;
			}
			if (enable) {
				fNormalScreen = new TerminalTextDataStore();
				fNormalScreen.copy(fTerminal);
				fNormalCursorLine = toAbsoluteLine(fCursorLine);
				fNormalCursorColumn = fCursorColumn;
				fNormalLines = fLines;
				fNormalColumns = fColumns;
				fNormalWrapPending = fWrapPending;
				fNormalStyle = fStyle;
				fNormalMaxHeight = fTerminal.getMaxHeight();
				// clearing leaves the buffer the size of the screen, which is what the
				// alternate screen is: no history to scroll back through. Capping the
				// buffer there keeps it so: scrolling drops the top line instead of
				// growing the buffer, as a program on this screen expects.
				clearBuffer();
				fTerminal.setMaxHeight(fLines);
				// the rendition carries over: clearing resets it, entering does not
				setStyle(fNormalStyle);
				if (!cursor) {
					// 47 and 1047 move no cursor: it stays on its row and column, a
					// wrap pending there still pending
					setCursor(fNormalCursorLine - (fNormalScreen.getHeight() - fLines), fNormalCursorColumn);
					fWrapPending = fNormalWrapPending;
				}
			} else {
				restoreNormalScreen(cursor);
			}
		}
	}

	/**
	 * Puts the normal screen back as if it had been resized along with the alternate
	 * one: the window may have changed while the program had it, and the buffer put
	 * back is the one from before. Narrower, and every write past its margin throws;
	 * shorter than the screen, and the top of the screen sits above its first line.
	 */
	private void restoreNormalScreen(boolean cursorBack) {
		// where the program left the cursor, for the modes that keep it there
		int programLine = fCursorLine;
		int programColumn = fCursorColumn;
		boolean programWrapPending = fWrapPending;
		TerminalStyle programStyle = fStyle;
		ITerminalTextData saved = fNormalScreen;
		fNormalScreen = null;
		// the margins were the full screen program's, not the shell's
		fScrollRegion = ScrollRegion.FULL_WINDOW;
		int height = saved.getHeight();
		int cursor = fNormalCursorLine; // a line of the saved buffer
		// the height setDimensions would give the normal screen: at least the screen,
		// and with no history, the rows below the cursor go when the screen shrinks
		int newHeight = Math.max(fLines, height);
		if (fLines < fNormalLines && height == fNormalLines) {
			newHeight = Math.max(fLines, cursor + 1);
		}
		fTerminal.setMaxHeight(Math.max(fTerminal.getMaxHeight(), Math.max(height, newHeight)));
		fTerminal.copy(saved);
		fTerminal.setDimensions(newHeight, fColumns);
		// rows the buffer gains are blank: they may still hold what the program drew there
		for (int line = height; line < newHeight; line++) {
			fTerminal.cleanLine(line);
		}
		// a limit lowered meanwhile takes the oldest lines, never the screen
		int limit = Math.max(fNormalMaxHeight, fLines);
		int excess = newHeight - limit;
		if (excess > 0) {
			fTerminal.scroll(0, newHeight, -excess);
			fTerminal.setDimensions(limit, fColumns);
			cursor -= excess;
		}
		fTerminal.setMaxHeight(Math.max(limit, fTerminal.getHeight()));
		// back to the line the cursor was on, wherever it is on the screen now
		if (!cursorBack) {
			// 47 and 1047 save no cursor: it stays on the row and column the program left it
			setCursor(programLine, programColumn);
			fWrapPending = programWrapPending && fCursorColumn == fColumns - 1;
			setStyle(programStyle);
			return;
		}
		setCursor(cursor - (fTerminal.getHeight() - fLines), fNormalCursorColumn);
		// and to a wrap that was pending there, if the line is as wide as it was
		fWrapPending = fNormalWrapPending && fColumns == fNormalColumns && fCursorColumn == fColumns - 1;
		// in the colours the shell was writing in, not the ones the program left selected
		setStyle(fNormalStyle);
	}

	@Override
	public int getBufferLineLimit() {
		synchronized (fTerminal) {
			// while the alternate screen shows, the model's limit is that screen's height
			return fNormalScreen != null ? fNormalMaxHeight : fTerminal.getMaxHeight();
		}
	}

	@Override
	public void setBufferLineLimit(int bufferLineLimit) {
		if (bufferLineLimit <= 0) {
			return;
		}
		synchronized (fTerminal) {
			if (fNormalScreen != null) {
				// the alternate screen keeps its cap; the normal screen gets the limit back
				fNormalMaxHeight = bufferLineLimit;
				return;
			}
			if (fTerminal.getHeight() > bufferLineLimit) {
				fTerminal.setDimensions(bufferLineLimit, fTerminal.getWidth());
			}
			fTerminal.setMaxHeight(bufferLineLimit);
		}
	}

	@Override
	public void clearAll() {
		synchronized (fTerminal) {
			if (fNormalScreen != null) {
				// Clearing while a full screen program shows clears what is behind it as
				// well, or leaving the program would bring back what the user cleared:
				// a blank normal screen, the cursor on the row of it where it was.
				int row = fNormalCursorLine - (fNormalScreen.getHeight() - fNormalLines);
				fNormalScreen = new TerminalTextDataStore();
				fNormalScreen.setDimensions(fNormalLines, fNormalColumns);
				fNormalCursorLine = Math.max(0, Math.min(row, fNormalLines - 1));
				fNormalWrapPending = false;
			}
			clearBuffer();
		}
	}

	private void clearBuffer() {
		synchronized (fTerminal) {
			// clear the history
			int n = fTerminal.getHeight();
			for (int line = 0; line < n; line++) {
				fTerminal.cleanLine(line);
			}
			fTerminal.setDimensions(fLines, fTerminal.getWidth());
			setStyle(getDefaultStyle());
			setCursor(0, 0);
		}
	}

	@Override
	public void setDimensions(int lines, int cols) {
		synchronized (fTerminal) {
			if (lines == fLines && cols == fColumns) {
				return; // nothing to do
			}
			// relative cursor line
			int cl = getCursorLine();
			int cc = getCursorColumn();
			if (fNormalScreen != null) {
				// The alternate screen has no history, so its buffer stays exactly as
				// high as the screen. On a shorter screen the lines above the cursor go
				// first, as many as it takes to keep the cursor on it; the cap follows
				// once the buffer is no higher than it, which the buffer requires.
				int drop = Math.max(0, cl + 1 - lines);
				if (drop > 0) {
					fTerminal.scroll(0, fTerminal.getHeight(), -drop);
				}
				fLines = lines;
				fColumns = cols;
				fTerminal.setDimensions(lines, cols);
				fTerminal.setMaxHeight(lines);
				setCursor(cl - drop, cc);
				return;
			}
			int height = fTerminal.getHeight();
			// absolute cursor line
			int acl = cl + height - fLines;
			int newLines = Math.max(lines, height);
			if (lines < fLines) {
				if (height == fLines) {
					// if the terminal has no history, then resize by
					// setting the size to the new size
					// TODO We are assuming that cursor line points at end of text
					newLines = Math.max(lines, cl + 1);
				}
			}
			fLines = lines;
			fColumns = cols;
			// make the terminal at least as high as we need lines
			fTerminal.setDimensions(newLines, fColumns);
			// compute relative cursor line
			cl = acl - (newLines - fLines);
			setCursor(cl, cc);
		}
	}

	/**
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 * @noreference This method is not intended to be referenced by clients.
	 *      It used to be package protected, and it is public only for Unit Tests.
	 */
	public int toAbsoluteLine(int line) {
		synchronized (fTerminal) {
			return fTerminal.getHeight() - fLines + line;
		}
	}

	@Override
	public void insertCharacters(int charactersToInsert) {
		if (charactersToInsert <= 0) {
			return; // nothing moves, so no wide character may be broken either
		}
		synchronized (fTerminal) {
			int line = toAbsoluteLine(fCursorLine);
			int n = charactersToInsert;
			// A wide character is not left half on the line: one the insertion splits
			// is blanked whole, and so is one whose second cell it pushes off the end.
			if (isSecondCell(line, fCursorColumn)) {
				eraseCells(line, fCursorColumn - 1, fCursorColumn + 1);
			}
			if (fCursorColumn + n < fColumns && isSecondCell(line, fColumns - n)) {
				eraseCells(line, fColumns - n - 1, fColumns - n);
			}
			for (int col = fColumns - 1; col >= fCursorColumn + n; col--) {
				char c = fTerminal.getChar(line, col - n);
				TerminalStyle style = fTerminal.getStyle(line, col - n);
				fTerminal.setChar(line, col, c, style);
			}
			eraseCells(line, fCursorColumn, Math.min(fCursorColumn + n, fColumns));
		}
	}

	@Override
	public void eraseToEndOfScreen() {
		synchronized (fTerminal) {
			eraseLineToEnd();
			for (int line = toAbsoluteLine(fCursorLine + 1); line < toAbsoluteLine(fLines); line++) {
				eraseLine(line);
			}
		}

	}

	@Override
	public void eraseToCursor() {
		synchronized (fTerminal) {
			for (int line = toAbsoluteLine(0); line < toAbsoluteLine(fCursorLine); line++) {
				eraseLine(line);
			}
			eraseLineToCursor();
		}
	}

	@Override
	public void eraseAll() {
		synchronized (fTerminal) {
			// What is on the screen goes into the history before it is erased, as in
			// Windows Terminal and VTE, so that clearing the screen does not destroy
			// what the user was reading. It matters on Windows, where ConPTY clears
			// the screen at the start of every connection. Blank lines at the bottom
			// of the screen are left out, but not the row a line runs on to: the
			// history ends where a line ends. The alternate screen keeps no history,
			// so there the lines simply go.
			int used = fLines;
			while (used > 0 && isBlank(toAbsoluteLine(used - 1))) {
				used--;
			}
			while (used > 0 && used < fLines && fTerminal.isWrappedLine(toAbsoluteLine(used - 1))) {
				used++;
			}
			for (int i = 0; i < used; i++) {
				fTerminal.addLine();
			}
			if (used > 0) {
				// the screen moved down the buffer: the cursor stays on its row of it
				setCursorLine(fCursorLine);
			}
			for (int line = toAbsoluteLine(0); line < toAbsoluteLine(fLines); line++) {
				eraseLine(line);
			}
		}
	}

	private boolean isBlank(int line) {
		char[] chars = fTerminal.getChars(line);
		if (chars != null) {
			for (char c : chars) {
				if (c != '\000' && c != ' ') {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public void eraseLine() {
		synchronized (fTerminal) {
			eraseLine(toAbsoluteLine(fCursorLine));
		}
	}

	private void eraseLine(int line) {
		fWrapPending = false; // also when there is nothing to fill
		fTerminal.cleanLine(line);
		if (eraseStyle() != null) {
			eraseCells(line, 0, fColumns);
		}
	}

	@Override
	public void eraseLineToEnd() {
		synchronized (fTerminal) {
			eraseWhole(toAbsoluteLine(fCursorLine), fCursorColumn, fColumns);
		}
	}

	/**
	 * Erasing either half of a wide character erases all of it, in the same
	 * background as the rest of the range.
	 */
	private void eraseWhole(int line, int from, int to) {
		if (isSecondCell(line, from)) {
			from--;
		}
		if (isSecondCell(line, to)) {
			to++;
		}
		eraseCells(line, from, to);
	}

	/**
	 * Erasing leaves behind the background a program is writing in, not the one the
	 * terminal started with (xterm's "back color erase", which every terminal that
	 * supports colour implements). Colouring a run of a line by setting a background
	 * and erasing to the end of it is how a program paints anything wider than the
	 * characters it has to put there, and without this such a line keeps the
	 * terminal's own colour from its last character on. xterm applies this to every
	 * erase (EL, ED, ECH) and to the cells insertion and deletion uncover (ICH, DCH).
	 */
	private void eraseCells(int line, int from, int to) {
		fWrapPending = false; // erasing, inserting and deleting all forget a pending wrap
		if (from < to) {
			fTerminal.setChars(line, from, new char[to - from], eraseStyle());
		}
	}

	/**
	 * @return the style erased cells take: only the background carries over, what is
	 *         erased is not bold or underlined; null, as a cell that was never
	 *         written, when that background is the default one
	 */
	private TerminalStyle eraseStyle() {
		TerminalStyle plain = getDefaultStyle();
		if (plain == null || fStyle == null) {
			return fStyle;
		}
		TerminalStyle erased = plain.setBackground(fStyle);
		return erased.equals(plain) ? null : erased;
	}

	@Override
	public void eraseLineToCursor() {
		synchronized (fTerminal) {
			eraseWhole(toAbsoluteLine(fCursorLine), 0, fCursorColumn + 1);
		}
	}

	@Override
	public void insertLines(int n) {
		synchronized (fTerminal) {
			if (!isCusorInScrollingRegion()) {
				return;
			}
			assert n > 0;
			int line = toAbsoluteLine(fCursorLine);
			int nLines = Math.min(fTerminal.getHeight() - line, fScrollRegion.getBottomLine() - fCursorLine + 1);
			fTerminal.scroll(line, nLines, n);
		}
	}

	@Override
	public void deleteCharacters(int n) {
		if (n <= 0) {
			return; // nothing moves, so no wide character may be broken either
		}
		synchronized (fTerminal) {
			int line = toAbsoluteLine(fCursorLine);
			// deleting either half of a wide character leaves none of it
			int end = Math.min(fCursorColumn + n, fColumns);
			if (isSecondCell(line, fCursorColumn)) {
				eraseCells(line, fCursorColumn - 1, fCursorColumn);
			}
			if (isSecondCell(line, end)) {
				eraseCells(line, end, end + 1);
			}
			for (int col = fCursorColumn + n; col < fColumns; col++) {
				char c = fTerminal.getChar(line, col);
				TerminalStyle style = fTerminal.getStyle(line, col);
				fTerminal.setChar(line, col - n, c, style);
			}
			eraseCells(line, Math.max(fCursorColumn, fColumns - n), fColumns);
		}
	}

	@Override
	public void deleteLines(int n) {
		synchronized (fTerminal) {
			if (!isCusorInScrollingRegion()) {
				return;
			}
			assert n > 0;
			int line = toAbsoluteLine(fCursorLine);
			int nLines = Math.min(fTerminal.getHeight() - line, fScrollRegion.getBottomLine() - fCursorLine + 1);
			fTerminal.scroll(line, nLines, -n);
		}
	}

	private boolean isCusorInScrollingRegion() {
		return fScrollRegion.contains(fCursorLine);
	}

	@Override
	public TerminalStyle getDefaultStyle() {
		synchronized (fTerminal) {
			return fDefaultStyle;
		}
	}

	@Override
	public void setDefaultStyle(TerminalStyle defaultStyle) {
		synchronized (fTerminal) {
			fDefaultStyle = defaultStyle;
		}
	}

	@Override
	public TerminalStyle getStyle() {
		synchronized (fTerminal) {
			if (fStyle == null) {
				return fDefaultStyle;
			}
			return fStyle;
		}
	}

	@Override
	public void setStyle(TerminalStyle style) {
		synchronized (fTerminal) {
			fStyle = style;
		}
	}

	@Override
	public void appendString(String buffer) {
		synchronized (fTerminal) {
			char[] chars = buffer.toCharArray();
			int line = toAbsoluteLine(fCursorLine);
			int i = 0;
			while (i < chars.length) {
				if (fWrapPending) {
					line = doLineWrap();
				}
				int room = fColumns - fCursorColumn;
				int col;
				int n = narrowRun(chars, i, room);
				if (n > 0) {
					if (fInsertMode) {
						// room on the line the run goes to, which a wrap may have changed
						insertCharacters(n);
					}
					breakWideChar(line, fCursorColumn);
					breakWideChar(line, fCursorColumn + n - 1);
					fTerminal.setChars(line, fCursorColumn, chars, i, n, fStyle);
					col = fCursorColumn + n;
					i += n;
				} else {
					int codePoint = Character.codePointAt(chars, i);
					int charsUsed = Character.charCount(codePoint);
					int width = cellWidth(codePoint);
					if (width == 0) {
						// combining marks and other non-printing code points occupy no cell
						i += charsUsed;
						continue;
					}
					if (width > room) {
						if (fCursorColumn > 0) {
							// a wide character is never split across the right margin,
							// and the cell it leaves unused there is erased
							eraseCharacters(room);
							line = doLineWrap();
							continue;
						}
						// terminal narrower than the character itself
						width = room;
					}
					if (fInsertMode) {
						insertCharacters(width); // room in cells, not characters
					}
					breakWideChar(line, fCursorColumn);
					breakWideChar(line, fCursorColumn + width - 1);
					if (charsUsed > width) {
						// a one column terminal: the two chars of a surrogate pair cannot share its cell
						fTerminal.setChar(line, fCursorColumn, '\uFFFD', fStyle);
					} else if (charsUsed == 2) {
						fTerminal.setChars(line, fCursorColumn, chars, i, 2, fStyle);
					} else {
						fTerminal.setChar(line, fCursorColumn, chars[i], fStyle);
						if (width == 2) {
							fTerminal.setChar(line, fCursorColumn + 1, '\000', fStyle);
						}
					}
					col = fCursorColumn + width;
					i += charsUsed;
				}
				// wrap needed?
				if (col == fColumns) {
					if (fVT100LineWrapping) {
						// deferred line wrapping (eat_newline_glitch)
						setCursorColumn(col - 1);
						fWrapPending = true;
					} else {
						line = doLineWrap();
					}
				} else {
					setCursorColumn(col);
				}
			}
		}
	}

	/**
	 * A wide character owns two cells. Overwriting either one leaves the other
	 * stranded: a filler with nothing in front of it, or a glyph that now spills
	 * over whatever was written next to it. Blanking the partner before the write
	 * goes in keeps the line honest, which is what a terminal is expected to do.
	 */
	private void breakWideChar(int line, int col) {
		if (col < 0 || col >= fColumns) {
			return;
		}
		char c = fTerminal.getChar(line, col);
		if (Character.isLowSurrogate(c)) {
			// second cell of a character beyond the BMP
			if (col > 0 && Character.isHighSurrogate(fTerminal.getChar(line, col - 1))) {
				blank(line, col - 1);
			}
		} else if (Character.isHighSurrogate(c)) {
			if (col + 1 < fColumns && Character.isLowSurrogate(fTerminal.getChar(line, col + 1))) {
				blank(line, col + 1);
			}
		} else if (c == '\000') {
			if (col > 0 && CharWidth.of(fTerminal.getChar(line, col - 1)) == 2) {
				blank(line, col - 1);
			}
		} else if (CharWidth.of(c) == 2 && col + 1 < fColumns && fTerminal.getChar(line, col + 1) == '\000') {
			blank(line, col + 1);
		}
	}

	/**
	 * Cells a code point takes on the screen: its display width, except that a
	 * printable character beyond the BMP always takes two, since its two chars
	 * cannot share a cell.
	 */
	private static int cellWidth(int codePoint) {
		int width = CharWidth.of(codePoint);
		return width != 0 && Character.charCount(codePoint) == 2 ? 2 : width;
	}

	/**
	 * @return whether the cell is the second of a wide character's two cells
	 */
	private boolean isSecondCell(int line, int col) {
		return col > 0 && col < fColumns && CharWidth.isSecondCell(
				String.valueOf(new char[] { fTerminal.getChar(line, col - 1), fTerminal.getChar(line, col) }), 1);
	}

	private void blank(int line, int col) {
		fTerminal.setChar(line, col, ' ', fTerminal.getStyle(line, col));
	}

	/**
	 * Length of the run of characters starting at <code>offset</code> that each
	 * occupy exactly one cell, so that they can be copied in one block. Capped at
	 * <code>max</code> cells. Zero when the run does not start with such a
	 * character, which sends the caller down the code point by code point path.
	 */
	private static int narrowRun(char[] chars, int offset, int max) {
		int n = 0;
		while (n < max && offset + n < chars.length && !Character.isSurrogate(chars[offset + n])
				&& CharWidth.of(chars[offset + n]) == 1) {
			n++;
		}
		return n;
	}

	private int doLineWrap() {
		int line;
		line = toAbsoluteLine(fCursorLine);
		fTerminal.setWrappedLine(line);
		doNewline();
		line = toAbsoluteLine(fCursorLine);
		setCursorColumn(0);
		return line;
	}

	/**
	 * MUST be called from a synchronized block!
	 */
	private void doNewline() {
		fWrapPending = false; // DEC STD 070: any move of the cursor forgets a pending wrap
		if (fCursorLine == fScrollRegion.getBottomLine()) {
			if (fScrollRegion.getTopLine() == 0) {
				scrollTopAnchoredRegionUpIntoHistory();
			} else {
				scrollUp(1);
			}
		} else if (fCursorLine + 1 >= fLines) {
			int h = fTerminal.getHeight();
			fTerminal.addLine();
			if (h != fTerminal.getHeight()) {
				setCursorLine(fCursorLine + 1);
			}
		} else {
			setCursorLine(fCursorLine + 1);
		}
	}

	/**
	 * Scrolls a scroll region whose top margin is the top of the screen: the displaced top line
	 * is moved into the scroll-back history (like xterm/VTE) instead of being discarded.
	 * MUST be called from a synchronized block!
	 */
	private void scrollTopAnchoredRegionUpIntoHistory() {
		int regionBottom = fScrollRegion.getBottomLine();
		if (regionBottom == fLines - 1) {
			fTerminal.addLine();
		} else if (fTerminal.getMaxHeight() > 0 && fTerminal.getHeight() < fTerminal.getMaxHeight()) {
			// the history grows, so the former top line stays where it is and becomes history;
			// shift the lines below the region back down, vacating the bottom line of the region
			fTerminal.addLine();
			fTerminal.scroll(toAbsoluteLine(regionBottom), fLines - regionBottom, 1);
		} else {
			// one negative shift that snapshots can track, so selections move with the text
			fTerminal.scroll(0, toAbsoluteLine(regionBottom) + 1, -1);
		}
		// the absolute line of the cursor may have changed
		setCursorLine(fCursorLine);
	}

	@Override
	public void processNewline() {
		synchronized (fTerminal) {
			doNewline();
		}
	}

	private void doReverseLineFeed() {
		fWrapPending = false;
		if (fCursorLine == fScrollRegion.getTopLine()) {
			scrollDown(1);
		} else {
			setCursorLine(fCursorLine - 1);
		}
	}

	@Override
	public void processReverseLineFeed() {
		synchronized (fTerminal) {
			doReverseLineFeed();
		}
	}

	@Override
	public int getCursorLine() {
		synchronized (fTerminal) {
			return fCursorLine;
		}
	}

	@Override
	public int getCursorColumn() {
		synchronized (fTerminal) {
			return fCursorColumn;
		}
	}

	@Override
	public void setCursor(int targetLine, int targetColumn) {
		synchronized (fTerminal) {
			setCursorLine(targetLine);
			setCursorColumn(targetColumn);
		}
	}

	@Override
	public void setCursorColumn(int targetColumn) {
		synchronized (fTerminal) {
			if (targetColumn < 0) {
				targetColumn = 0;
			} else if (targetColumn >= fColumns) {
				targetColumn = fColumns - 1;
			}
			fCursorColumn = targetColumn;
			fWrapPending = false;
			// We make the assumption that nobody is changing the
			// terminal cursor except this class!
			// This assumption gives a huge performance improvement
			fTerminal.setCursorColumn(targetColumn);
		}
	}

	@Override
	public void setCursorLine(int targetLine) {
		synchronized (fTerminal) {
			if (targetLine < 0) {
				targetLine = 0;
			} else if (targetLine >= fLines) {
				targetLine = fLines - 1;
			}
			fCursorLine = targetLine;
			fWrapPending = false;
			// We make the assumption that nobody is changing the
			// terminal cursor except this class!
			// This assumption gives a huge performance improvement
			fTerminal.setCursorLine(toAbsoluteLine(targetLine));
		}
	}

	@Override
	public int getLines() {
		synchronized (fTerminal) {
			return fLines;
		}
	}

	@Override
	public int getColumns() {
		synchronized (fTerminal) {
			return fColumns;
		}
	}

	@Override
	public void setVT100LineWrapping(boolean enable) {
		fVT100LineWrapping = enable;
	}

	@Override
	public boolean isVT100LineWrapping() {
		return fVT100LineWrapping;
	}

	@Override
	public void setInsertMode(boolean enable) {
		fInsertMode = enable;
	}

	@Override
	public void setScrollRegion(int top, int bottom) {
		if (top < 0 || bottom < 0) {
			fScrollRegion = ScrollRegion.FULL_WINDOW;
		} else if (top < bottom) {
			fScrollRegion = new ScrollRegion(top, bottom);
		}
	}

	@Override
	public void scrollUp(int n) {
		assert n > 0;
		synchronized (fTerminal) {
			int line = toAbsoluteLine(fScrollRegion.getTopLine());
			int nLines = Math.min(fTerminal.getHeight() - line, fScrollRegion.getHeight());
			fTerminal.scroll(line, nLines, -n);
		}
	}

	@Override
	public void scrollDown(int n) {
		assert n > 0;
		synchronized (fTerminal) {
			int line = toAbsoluteLine(fScrollRegion.getTopLine());
			int nLines = Math.min(fTerminal.getHeight() - line, fScrollRegion.getHeight());
			fTerminal.scroll(line, nLines, n);
		}
	}

	@Override
	public void eraseCharacters(int n) {
		if (n <= 0) {
			return; // nothing is erased, so no wide character may be broken either
		}
		synchronized (fTerminal) {
			eraseWhole(toAbsoluteLine(fCursorLine), fCursorColumn, Math.min(fCursorColumn + n, fColumns));
		}
	}
}
