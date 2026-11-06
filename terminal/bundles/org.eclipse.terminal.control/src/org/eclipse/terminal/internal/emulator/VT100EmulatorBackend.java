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

import java.text.BreakIterator;
import java.text.Normalizer;

import org.eclipse.terminal.internal.model.Cells;
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

	/** a run of characters on its way into the cells, with their fillers */
	private char[] fRun = new char[0];

	/** finds where a grapheme cluster ends, by the rules of UAX #29 */
	private final BreakIterator fGraphemes = BreakIterator.getCharacterInstance();

	/** the first half of a surrogate pair that ended the last write, waiting for the second */
	private char fHighSurrogate;

	/** the cluster written last, which what comes next may still go onto */
	private String fLastText;
	/** its first cell, and how many lines above the cursor it is: 1 after an immediate wrap */
	private int fLastColumn, fLastLinesBack;
	/**
	 * How many times anything but writing text has been done (a cursor move, an
	 * erase, an edit, a scroll), and how many times when the cluster was written:
	 * only what comes straight after it in the text can go on with it.
	 */
	private int fMoves, fAfterMoves;
	/** whether a wrap was pending right after it */
	private boolean fAfterPending;

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
		endCluster();
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
		endCluster();
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
			fLastText = null; // its place is no longer what it was
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
		endCluster();
		if (charactersToInsert <= 0) {
			return; // nothing moves, so no wide character may be broken either
		}
		synchronized (fTerminal) {
			int line = toAbsoluteLine(fCursorLine);
			int col = fCursorColumn;
			int n = Math.min(charactersToInsert, fColumns - col);
			// A character is not left half on the line: one the insertion splits is
			// erased whole, and so is one whose second cell it pushes off the end.
			eraseWhole(line, col, col);
			if (col + n < fColumns) {
				eraseWhole(line, fColumns - n, fColumns - n);
			}
			moveCells(line, col, fColumns - n, n);
			eraseCells(line, col, col + n);
		}
	}

	@Override
	public void eraseToEndOfScreen() {
		endCluster();
		synchronized (fTerminal) {
			eraseLineToEnd();
			for (int line = toAbsoluteLine(fCursorLine + 1); line < toAbsoluteLine(fLines); line++) {
				eraseLine(line);
			}
		}

	}

	@Override
	public void eraseToCursor() {
		endCluster();
		synchronized (fTerminal) {
			for (int line = toAbsoluteLine(0); line < toAbsoluteLine(fCursorLine); line++) {
				eraseLine(line);
			}
			eraseLineToCursor();
		}
	}

	@Override
	public void eraseAll() {
		endCluster();
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
		return Cells.text(fTerminal, line, 0, fColumns).isBlank();
	}

	@Override
	public void eraseLine() {
		endCluster();
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
		endCluster();
		synchronized (fTerminal) {
			int line = toAbsoluteLine(fCursorLine);
			eraseWhole(line, fCursorColumn, fColumns);
			// Nothing is left out to the margin, so the line ends where the cursor is.
			fTerminal.clearWrappedLine(line);
		}
	}

	/**
	 * Erasing either half of a wide character erases all of it, in the same
	 * background as the rest of the range.
	 *
	 * @return where the erase ended, past a wide character it took whole
	 */
	private int eraseWhole(int line, int from, int to) {
		if (from < fColumns) {
			from = Cells.start(fTerminal, line, from);
		}
		if (to < fColumns && fTerminal.getCellWidth(line, to) == 0) {
			to++; // the range ends inside a character: all of it goes
		}
		eraseCells(line, from, to);
		return to;
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
		endCluster();
		synchronized (fTerminal) {
			eraseWhole(toAbsoluteLine(fCursorLine), 0, fCursorColumn + 1);
		}
	}

	@Override
	public void insertLines(int n) {
		endCluster();
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
		endCluster();
		if (n <= 0) {
			return; // nothing moves, so no wide character may be broken either
		}
		synchronized (fTerminal) {
			int line = toAbsoluteLine(fCursorLine);
			int col = fCursorColumn;
			n = Math.min(n, fColumns - col);
			// deleting either half of a character leaves none of it
			eraseWhole(line, col, col);
			if (col + n < fColumns) {
				eraseWhole(line, col + n, col + n);
			}
			moveCells(line, col + n, fColumns, -n);
			eraseCells(line, fColumns - n, fColumns);
			// what reached the margin moved off it, so the line ends before it (xterm)
			fTerminal.clearWrappedLine(line);
		}
	}

	@Override
	public void deleteLines(int n) {
		endCluster();
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
			// a reader may hand over the two halves of a surrogate pair apart
			if (fHighSurrogate != 0) {
				buffer = fHighSurrogate + buffer;
				fHighSurrogate = 0;
			}
			if (!buffer.isEmpty() && Character.isHighSurrogate(buffer.charAt(buffer.length() - 1))) {
				fHighSurrogate = buffer.charAt(buffer.length() - 1);
				buffer = buffer.substring(0, buffer.length() - 1);
			}
			buffer = withoutHidden(buffer);
			char[] chars = buffer.toCharArray();
			int line = toAbsoluteLine(fCursorLine);
			boolean[] starts = clusterStarts(lastCluster() >= 0 ? fLastText : "", buffer); //$NON-NLS-1$
			int i = 0;
			while (i < chars.length) {
				int codePoint = Character.codePointAt(chars, i);
				int charsUsed = Character.charCount(codePoint);
				// What goes on with the cluster written last goes onto it, even when that is
				// on the last cell with a wrap pending: it belongs there, not on the next line.
				if ((!starts[i] || takesNoCell(codePoint)) && lastCluster() >= 0) {
					line = goOn(Character.toString(codePoint));
					i += charsUsed;
					continue;
				}
				if (fWrapPending) {
					line = doLineWrap();
				}
				int room = fColumns - fCursorColumn;
				int col = fCursorColumn;
				// a run of characters of the BMP that are each a cluster of their own goes
				// in at once: most text is nothing else
				if (fRun.length < room) {
					fRun = new char[room];
				}
				int n = 0;
				int cells = 0;
				int width = 0;
				while (i + n < chars.length && starts[i + n] && !Character.isSurrogate(chars[i + n])) {
					width = CharWidth.of(chars[i + n]);
					if (width == 0 || cells + width > room) {
						break;
					}
					fRun[cells++] = chars[i + n++];
					if (width == 2) {
						fRun[cells++] = '\000'; // the filler of a wide character
					}
				}
				if (n > 0) {
					if (fInsertMode) {
						// room on the line the run goes to, which a wrap may have changed
						insertCharacters(cells);
					}
					breakAround(line, col, col + cells);
					fTerminal.setChars(line, col, fRun, 0, cells, fStyle);
					i += n;
					char last = chars[i - 1];
					line = written(col + cells - CharWidth.of(last), String.valueOf(last), col + cells);
					continue;
				}
				String text = Character.toString(codePoint);
				cells = CharWidth.clusterCells(text);
				if (cells > room) {
					if (col > 0) {
						// a wide character is never split across the right margin,
						// and the cell it leaves unused there is erased
						eraseWhole(line, col, fColumns);
						line = doLineWrap();
						continue;
					}
					cells = room; // a screen narrower than the character
				}
				if (fInsertMode) {
					insertCharacters(cells); // room in cells, not characters
				}
				put(line, col, text, cells, fStyle);
				i += charsUsed;
				line = written(col, text, col + cells);
			}
		}
	}

	/**
	 * Where a grapheme cluster starts in the text, by the Unicode rules (UAX #29),
	 * the cluster written last going first as the context the text goes on from.
	 *
	 * @return for each char of the text, whether a cluster starts there
	 */
	private boolean[] clusterStarts(String before, String text) {
		boolean[] starts = new boolean[text.length()];
		int simple = before.isEmpty() || startsAlways(before.charAt(before.length() - 1)) ? 0 : -1;
		while (simple >= 0 && simple < starts.length && startsAlways(text.charAt(simple))) {
			starts[simple++] = true;
		}
		if (simple == starts.length) {
			return starts; // most text is nothing else
		}
		fGraphemes.setText(before + text);
		int b = fGraphemes.following(Math.max(before.length() - 1, 0));
		for (; b != BreakIterator.DONE; b = fGraphemes.next()) {
			if (b >= before.length() && b - before.length() < starts.length) {
				starts[b - before.length()] = true;
			}
		}
		if (before.isEmpty() && starts.length > 0) {
			starts[0] = true;
		}
		return starts;
	}

	/** the longest cluster kept, in chars: the longest emoji sequence is 15 */
	private static final int MAX_CLUSTER = 32;

	/**
	 * The text without what is kept out of the cells: control characters, format
	 * characters no cluster needs (a zero width space, bidirectional marks and
	 * overrides), and what only hides text: tag characters outside the tag
	 * sequence of a flag, and a variation selector that does not follow a
	 * character it could vary. None is drawn, and in text copied from the
	 * terminal it could make a command read differently from how it runs, or
	 * carry text no one saw. Taken out before the clusters are found, so that
	 * where the text was split into writes makes no difference.
	 */
	private String withoutHidden(String text) {
		int i = 0;
		while (i < text.length() && CharWidth.of(text.charAt(i)) != 0 && !Character.isSurrogate(text.charAt(i))) {
			i++;
		}
		if (i == text.length()) {
			return text; // nothing that could be: most text
		}
		StringBuilder kept = new StringBuilder(text.length()).append(text, 0, i);
		String before = i > 0 ? text.substring(0, i) : lastCluster() >= 0 ? fLastText : ""; //$NON-NLS-1$
		int last = before.isEmpty() ? -1 : before.codePointBefore(before.length());
		int tags = 0; // tag characters of a flag so far
		for (int k = before.length(); k > 0 && isTagSpec(before.codePointBefore(k)); k -= 2) {
			tags++;
		}
		for (; i < text.length(); i += Character.charCount(text.codePointAt(i))) {
			int codePoint = text.codePointAt(i);
			if (!isHidden(codePoint, last, tags)) {
				kept.appendCodePoint(codePoint);
				tags = isTagSpec(codePoint) ? tags + 1 : 0;
				last = codePoint;
			}
		}
		return kept.toString();
	}

	/**
	 * @param last the code point kept before it
	 * @param tags how many tag characters of a flag are right before it
	 * @return whether the code point is kept out of the cells
	 */
	private boolean isHidden(int codePoint, int last, int tags) {
		int type = Character.getType(codePoint);
		if (type == Character.CONTROL) {
			return true;
		}
		if (isSelector(codePoint)) {
			return last < 0 || CharWidth.cells(last) == 0; // it varies or joins the character before it
		}
		if (type != Character.FORMAT || codePoint == 0x200D || codePoint == 0x200C) {
			return false;
		}
		if (isTagSpec(codePoint)) {
			// a flag spelled with tags (UTS #51): the black flag, up to six letters or digits, a cancel tag
			return !(last == 0x1F3F4 || tags > 0) || tags >= 6;
		}
		if (codePoint == 0xE007F) {
			return tags == 0;
		}
		// a prefix sign goes with what follows it, but one is enough
		String letter = "a"; //$NON-NLS-1$
		return breaks(Character.toString(codePoint), letter)
				|| last >= 0 && Character.getType(last) == Character.FORMAT && !breaks(Character.toString(last), letter);
	}

	/**
	 * @return whether the code point is invisible and only says how to show the
	 *         character before it: a variation selector, a Mongolian free variation
	 *         selector (not U+180E between them, a format character); the combining
	 *         grapheme joiner is not one, it keeps marks apart (Unicode 23.2)
	 */
	private static boolean isSelector(int codePoint) {
		return codePoint >= 0xFE00 && codePoint <= 0xFE0F || codePoint >= 0xE0100 && codePoint <= 0xE01EF
				|| codePoint >= 0x180B && codePoint <= 0x180D || codePoint == 0x180F;
	}

	/** @return whether the code point is a tag letter or digit, as in the tag sequence of a flag */
	private static boolean isTagSpec(int codePoint) {
		return codePoint >= 0xE0030 && codePoint <= 0xE0039 || codePoint >= 0xE0061 && codePoint <= 0xE007A;
	}

	/** per char of the BMP: 0 not asked yet, 1 a cluster of its own next to any other such, 2 not */
	private static final byte[] ALONE = new byte[0x10000];

	/**
	 * Whether no rule of UAX #29 can join the char to the one before it or after it,
	 * when that one is such a char too: for a run of them, every char starts a
	 * cluster and the rules need not be run. Asked once of the rules themselves:
	 * nothing joins it after a letter or before one, after a Hangul syllable (a
	 * conjoining vowel or final would) or before one (a leading consonant would).
	 * Half of a surrogate pair and a control character are never such a char.
	 */
	private boolean startsAlways(int c) {
		if (ALONE[c] == 0) {
			String one = String.valueOf((char) c);
			String letter = "a"; //$NON-NLS-1$
			String syllable = "\uac00"; //$NON-NLS-1$
			boolean alone = !Character.isSurrogate((char) c) && Character.getType(c) != Character.CONTROL
					&& Character.getType(c) != Character.FORMAT && breaks(letter, one) && breaks(one, letter)
					&& breaks(syllable, one) && breaks(one, syllable);
			ALONE[c] = (byte) (alone ? 1 : 2);
		}
		return ALONE[c] == 1;
	}

	/** Whatever is done but writing text: the cluster written last is done with. */
	private void endCluster() {
		synchronized (fTerminal) { // a resize comes from the display thread
			fMoves++;
		}
	}

	private boolean breaks(String first, String second) {
		fGraphemes.setText(first + second);
		return fGraphemes.isBoundary(first.length());
	}

	/**
	 * A cluster that starts with a code point that takes no cell, such as a zero
	 * width space or a mark the rules leave on its own, goes onto the cluster
	 * before it, which has a cell to keep it in. A prefix sign does not: it goes
	 * with what follows it. Decided by the code point alone, so that it does not
	 * matter where the text was split into writes.
	 */
	private boolean takesNoCell(int codePoint) {
		if (CharWidth.cells(codePoint) > 0) {
			return false;
		}
		String prefix = Character.toString(codePoint);
		fGraphemes.setText(prefix + "a"); //$NON-NLS-1$
		return fGraphemes.isBoundary(prefix.length());
	}

	/**
	 * @return the line of the cluster written last, when what comes next can still go
	 *         onto it: the cursor has not moved since, and the cells still hold it; -1
	 *         otherwise
	 */
	private int lastCluster() {
		if (fLastText == null || fMoves != fAfterMoves || fWrapPending != fAfterPending
				|| fLastColumn >= fColumns) {
			return -1;
		}
		int line = toAbsoluteLine(fCursorLine) - fLastLinesBack;
		return line >= 0 && fLastText.equals(Cells.text(fTerminal, line, fLastColumn)) ? line : -1;
	}

	/**
	 * Moves the cursor past a character just written, whose cells end before
	 * <code>end</code>, and remembers it for what may still go onto it.
	 *
	 * @return the line the cursor is now on
	 */
	private int written(int col, String text, int end) {
		int line = advance(end);
		fLastText = text;
		fLastColumn = col;
		fLastLinesBack = end == fColumns && !fVT100LineWrapping ? 1 : 0;
		fAfterMoves = fMoves;
		fAfterPending = fWrapPending;
		return line;
	}

	/**
	 * A grapheme cluster is what a reader takes for one character, however many code
	 * points it runs to: a letter with its accents, a Hangul syllable sent as jamo,
	 * an emoji with a skin tone or joined to others, a flag. The code point goes on
	 * with the cluster written last. The cluster takes the cells its code points
	 * would, but at least one and no more than two, summed as Windows Terminal sums
	 * them: a mark takes none, VS16 (emoji presentation) makes it wide. Grown wide,
	 * it takes the cell after it, or moves whole to the next line from the last one.
	 *
	 * @return the line the cursor is on
	 */
	private int goOn(String codePoint) {
		if (fLastText.length() + codePoint.length() > MAX_CLUSTER) {
			return toAbsoluteLine(fCursorLine); // a flood of marks must not grow one cell without bound
		}
		int line = lastCluster();
		int col = fLastColumn;
		String text = fLastText + codePoint;
		int cells = fTerminal.getCellWidth(line, col);
		TerminalStyle style = fTerminal.getStyle(line, col);
		if (CharWidth.clusterCells(text) <= cells || fColumns < 2) {
			put(line, col, text, cells, style);
			fLastText = text; // in the same cells, with the cursor where it was
			return toAbsoluteLine(fCursorLine);
		}
		if (col + 2 > fColumns) {
			// on the last cell: a wide character is never split across the margin, so
			// the cluster moves whole to the next line, the cell it leaves erased
			boolean pending = fLastLinesBack == 0;
			eraseCells(line, col, col + 1);
			line = pending ? doLineWrap() : toAbsoluteLine(fCursorLine);
			col = 0;
			if (fInsertMode) {
				insertCharacters(2);
			}
		} else if (fInsertMode) {
			insertCharacters(1); // the second cell is room made, not a cell taken from what follows
		}
		put(line, col, text, 2, style);
		return written(col, text, col + 2);
	}

	/**
	 * Writes a character, which may be a whole grapheme cluster, into its cells from
	 * <code>col</code>, the one way characters get into cells. The cell gets the
	 * character that is drawn: the first that takes a cell (not a prefix sign before
	 * it), composed with its marks (NFC) where they have a composed form; a second
	 * cell gets a filler, unless the character is a surrogate pair. The cluster, as
	 * the program wrote it, is kept beside the line whenever it is more than that.
	 * A character the write cuts in half is blanked whole.
	 */
	private void put(int line, int col, String text, int cells, TerminalStyle style) {
		breakAround(line, col, col + cells);
		int base = ' ';
		String composed = text.length() > Character.charCount(text.codePointAt(0))
				? Normalizer.normalize(text, Normalizer.Form.NFC)
				: text;
		for (int i = 0; i < composed.length(); i += Character.charCount(composed.codePointAt(i))) {
			base = composed.codePointAt(i);
			if (CharWidth.cells(base) > 0) {
				break;
			}
			base = ' ';
		}
		boolean lone = base >= Character.MIN_SURROGATE && base <= Character.MAX_SURROGATE; // half a pair
		char[] chars = lone || Character.charCount(base) > cells ? new char[] { '\uFFFD' } : Character.toChars(base);
		String drawn = new String(chars);
		if (cells == 2 && chars.length == 1) {
			chars = new char[] { chars[0], '\000' }; // the filler, written with it
		}
		fTerminal.setChars(line, col, chars, 0, chars.length, style);
		if (!text.equals(drawn)) {
			fTerminal.setCluster(line, col, text);
		}
	}

	/**
	 * A character that the cells from <code>from</code> to <code>to</code> cut in
	 * half is blanked whole, as a terminal is expected to do: half of it cannot be
	 * shown.
	 */
	private void breakAround(int line, int from, int to) {
		int first = Cells.start(fTerminal, line, from);
		int last = Cells.start(fTerminal, line, to - 1);
		int end = Math.min(last + fTerminal.getCellWidth(line, last), fColumns);
		for (int col = first; col < from; col++) {
			blank(line, col);
		}
		for (int col = to; col < end; col++) {
			blank(line, col);
		}
	}

	/**
	 * Moves the characters in the cells from <code>from</code> to <code>to</code> by
	 * <code>shift</code> cells, each written whole, with its cluster.
	 */
	private void moveCells(int line, int from, int to, int shift) {
		int n = to - from;
		if (n <= 0) {
			return;
		}
		// read all first: where they go may be where they were
		char[] chars = new char[n];
		TerminalStyle[] styles = new TerminalStyle[n];
		String[] clusters = new String[n];
		int[] widths = new int[n];
		for (int i = 0; i < n; i++) {
			chars[i] = fTerminal.getChar(line, from + i);
			styles[i] = fTerminal.getStyle(line, from + i);
			clusters[i] = fTerminal.getCluster(line, from + i);
			widths[i] = fTerminal.getCellWidth(line, from + i);
		}
		for (int i = 0; i < n;) {
			int width = i + widths[i] <= n ? Math.max(widths[i], 1) : 1;
			fTerminal.setChars(line, from + shift + i, chars, i, width, styles[i]);
			if (clusters[i] != null) {
				fTerminal.setCluster(line, from + shift + i, clusters[i]);
			}
			i += width;
		}
	}

	private void blank(int line, int col) {
		fTerminal.setChar(line, col, ' ', fTerminal.getStyle(line, col));
	}

	/**
	 * Moves the cursor past what was just written, which ends before <code>col</code>.
	 *
	 * @return the line the cursor is now on
	 */
	private int advance(int col) {
		if (col == fColumns) {
			if (fVT100LineWrapping) {
				// deferred line wrapping (eat_newline_glitch)
				setCursorColumn(col - 1);
				fWrapPending = true;
			} else {
				return doLineWrap();
			}
		} else {
			setCursorColumn(col);
		}
		return toAbsoluteLine(fCursorLine);
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
			scrollUp(1);
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

	@Override
	public void processNewline() {
		endCluster();
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
		endCluster();
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
		endCluster();
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
			endCluster(); // a cursor moved away and back no longer goes on with what it wrote
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
			endCluster();
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
		endCluster();
		assert n > 0;
		synchronized (fTerminal) {
			int line = toAbsoluteLine(fScrollRegion.getTopLine());
			int nLines = Math.min(fTerminal.getHeight() - line, fScrollRegion.getHeight());
			fTerminal.scroll(line, nLines, -n);
		}
	}

	@Override
	public void scrollDown(int n) {
		endCluster();
		assert n > 0;
		synchronized (fTerminal) {
			int line = toAbsoluteLine(fScrollRegion.getTopLine());
			int nLines = Math.min(fTerminal.getHeight() - line, fScrollRegion.getHeight());
			fTerminal.scroll(line, nLines, n);
		}
	}

	@Override
	public void eraseCharacters(int n) {
		endCluster();
		if (n <= 0) {
			return; // nothing is erased, so no wide character may be broken either
		}
		synchronized (fTerminal) {
			int line = toAbsoluteLine(fCursorLine);
			int end = n >= fColumns - fCursorColumn ? fColumns : fCursorColumn + n;
			// a wide character the erase ends in is erased whole, maybe up to the margin
			if (eraseWhole(line, fCursorColumn, end) == fColumns) {
				// Nothing is left out to the margin, so the line ends before it, as
				// erasing to the end of the line says (xterm; Ghostty on any ECH).
				fTerminal.clearWrappedLine(line);
			}
		}
	}
}
