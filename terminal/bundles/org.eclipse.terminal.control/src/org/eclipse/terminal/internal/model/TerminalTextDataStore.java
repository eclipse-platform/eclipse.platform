/*******************************************************************************
 * Copyright (c) 2007, 2026 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Michael Scharf (Wind River) - initial API and implementation
 * Anton Leherbauer (Wind River) - [453393] Add support for copying wrapped lines without line break
 *******************************************************************************/
package org.eclipse.terminal.internal.model;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.eclipse.terminal.model.ITerminalTextData;
import org.eclipse.terminal.model.ITerminalTextDataSnapshot;
import org.eclipse.terminal.model.LineSegment;
import org.eclipse.terminal.model.TerminalStyle;

/**
 * This class is thread safe.
 *
 */
public class TerminalTextDataStore implements ITerminalTextData {
	private char[][] fChars;
	private TerminalStyle[][] fStyle;
	private int fWidth;
	private int fHeight;
	private int fMaxHeight;
	private int fCursorColumn;
	private int fCursorLine;
	final private BitSet fWrappedLines = new BitSet();

	public TerminalTextDataStore() {
		fChars = new char[0][];
		fStyle = new TerminalStyle[0][];
		fWidth = 0;
	}

	@Override
	public int getWidth() {
		return fWidth;
	}

	@Override
	public int getHeight() {
		return fHeight;
	}

	@Override
	public void setDimensions(int height, int width) {
		if (height < 0) {
			throw new IllegalArgumentException("Parameter 'height' can't be negative value:" + height); //$NON-NLS-1$
		}
		if (width < 0) {
			throw new IllegalArgumentException("Parameter 'width' can't be negative value:" + width); //$NON-NLS-1$
		}
		// just extend the region
		if (height > fChars.length) {
			int h = 4 * height / 3;
			if (fMaxHeight > 0 && h > fMaxHeight) {
				h = fMaxHeight;
			}
			fStyle = (TerminalStyle[][]) resizeArray(fStyle, height);
			fChars = (char[][]) resizeArray(fChars, height);
		}
		// clean the new lines
		if (height > fHeight) {
			for (int i = fHeight; i < height; i++) {
				cleanLine(i);
			}
		}
		// set dimensions after successful resize!
		fWidth = width;
		fHeight = height;
	}

	/**
	 * Reallocates an array with a new size, and copies the contents of the old
	 * array to the new array.
	 *
	 * @param origArray the old array, to be reallocated.
	 * @param newSize the new array size.
	 * @return A new array with the same contents (chopped off if needed or filled with 0 or null).
	 */
	private Object resizeArray(Object origArray, int newSize) {
		int oldSize = Array.getLength(origArray);
		if (oldSize == newSize) {
			return origArray;
		}
		Class<?> elementType = origArray.getClass().getComponentType();
		Object newArray = Array.newInstance(elementType, newSize);
		int preserveLength = Math.min(oldSize, newSize);
		if (preserveLength > 0) {
			System.arraycopy(origArray, 0, newArray, 0, preserveLength);
		}
		return newArray;
	}

	@Override
	public LineSegment[] getLineSegments(int line, int column, int len) {
		// get the styles and chars for this line
		TerminalStyle[] styles = fStyle[line];
		char[] chars = fChars[line];
		int col = column;
		int n = column + len;

		// expand the line if needed....
		if (styles == null) {
			styles = new TerminalStyle[n];
		} else if (styles.length < n) {
			styles = (TerminalStyle[]) resizeArray(styles, n);
		}

		if (chars == null) {
			chars = new char[n];
		} else if (chars.length < n) {
			chars = (char[]) resizeArray(chars, n);
		}

		// and create the line segments
		TerminalStyle style = styles[column];
		List<LineSegment> segments = new ArrayList<>();
		for (int i = column; i < n; i++) {
			if (styles[i] != style) {
				segments.add(new LineSegment(col, new String(chars, col, i - col), style));
				style = styles[i];
				col = i;
			}
		}
		if (col < n) {
			segments.add(new LineSegment(col, new String(chars, col, n - col), style));
		}
		return segments.toArray(new LineSegment[segments.size()]);
	}

	@Override
	public char getChar(int line, int column) {
		if (column < 0 || column >= fWidth) {
			throw new IllegalArgumentException(
					"Parameter 'column' must be >= 0 and less than 'width' (current value '" + fWidth + "')"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (line < 0 || line >= fHeight) {
			throw new IllegalArgumentException(
					"Parameter 'line' must be >= 0 and less than 'height' (current value '" + fHeight + "')"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (fChars[line] == null || column >= fChars[line].length) {
			return 0;
		}
		return fChars[line][column];
	}

	@Override
	public TerminalStyle getStyle(int line, int column) {
		if (column >= fWidth) {
			throw new IllegalArgumentException(
					"Parameter 'column' must be >= 0 and less than 'width' (current value '" + fWidth + "')"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (fStyle[line] == null || column >= fStyle[line].length) {
			return null;
		}
		return fStyle[line][column];
	}

	void ensureLineLength(int line, int length) {
		if (line < 0 || line >= fHeight) {
			throw new IllegalArgumentException(
					"Parameter 'line' must be >= 0 and less than 'height' (current value '" + fHeight + "')"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (length > fWidth) {
			throw new IllegalArgumentException(
					"Parameter 'length'(value='" + length + "') exceeds 'width'(value='" + fWidth + "')"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		if (fChars[line] == null) {
			fChars[line] = new char[length];
		} else if (fChars[line].length < length) {
			fChars[line] = (char[]) resizeArray(fChars[line], length);
		}
		if (fStyle[line] == null) {
			fStyle[line] = new TerminalStyle[length];
		} else if (fStyle[line].length < length) {
			fStyle[line] = (TerminalStyle[]) resizeArray(fStyle[line], length);
		}
	}

	@Override
	public void setChar(int line, int column, char c, TerminalStyle style) {
		ensureLineLength(line, column + 1);
		fChars[line][column] = c;
		fStyle[line][column] = style;
	}

	@Override
	public void setChars(int line, int column, char[] chars, TerminalStyle style) {
		setChars(line, column, chars, 0, chars.length, style);
	}

	@Override
	public void setChars(int line, int column, char[] chars, int start, int len, TerminalStyle style) {
		if (column < 0 || column >= fWidth) {
			throw new IllegalArgumentException(
					"Parameter 'column' must be >= 0 and less than 'width' (current value '" + fWidth + "')"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		ensureLineLength(line, column + len);
		for (int i = 0; i < len; i++) {
			fChars[line][column + i] = chars[i + start];
			fStyle[line][column + i] = style;
		}
	}

	@Override
	public void scroll(int startLine, int size, int shift) {
		if (startLine + size > getHeight()) {
			throw new IllegalArgumentException("Value of 'startLine'+'size' parameters must be valid line (range [0-" //$NON-NLS-1$
					+ getHeight() + "). Parameter values: 'startLine'=" + startLine + ", 'size'=" + size); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (shift < 0) {
			// move the region up
			// shift is negative!!
			for (int i = startLine; i < startLine + size + shift; i++) {
				fChars[i] = fChars[i - shift];
				fStyle[i] = fStyle[i - shift];
				fWrappedLines.set(i, fWrappedLines.get(i - shift));
			}
			// then clean the opened lines
			cleanLines(Math.max(startLine, startLine + size + shift), Math.min(-shift, getHeight() - startLine));
			//			cleanLines(Math.max(0, startLine+size+shift),Math.min(-shift, getHeight()-startLine));
		} else {
			for (int i = startLine + size - 1; i >= startLine && i - shift >= 0; i--) {
				fChars[i] = fChars[i - shift];
				fStyle[i] = fStyle[i - shift];
				fWrappedLines.set(i, fWrappedLines.get(i - shift));
			}
			cleanLines(startLine, Math.min(shift, getHeight() - startLine));
		}
	}

	/**
	 * Replaces the lines with new empty data
	 * @param line
	 * @param len
	 */
	private void cleanLines(int line, int len) {
		for (int i = line; i < line + len; i++) {
			cleanLine(i);
		}
	}

	/*
	 * @return a text representation of the object.
	 * Lines are separated by '\n'. No style information is returned.
	 */
	@Override
	public String toString() {
		StringBuffer buff = new StringBuffer();
		for (int line = 0; line < getHeight(); line++) {
			if (line > 0) {
				buff.append("\n"); //$NON-NLS-1$
			}
			for (int column = 0; column < fWidth; column++) {
				buff.append(getChar(line, column));
			}
		}
		return buff.toString();
	}

	@Override
	public ITerminalTextDataSnapshot makeSnapshot() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addLine() {
		if (fMaxHeight > 0 && getHeight() < fMaxHeight) {
			setDimensions(getHeight() + 1, getWidth());
		} else {
			scroll(0, getHeight(), -1);
		}
	}

	@Override
	public void copy(ITerminalTextData source) {
		fWidth = source.getWidth();
		int n = source.getHeight();
		if (getHeight() != n) {
			fChars = new char[n][];
			fStyle = new TerminalStyle[n][];
		}
		for (int i = 0; i < n; i++) {
			copyLine(source, i, i);
		}
		fHeight = n;
		fCursorLine = source.getCursorLine();
		fCursorColumn = source.getCursorColumn();
	}

	@Override
	public void copyRange(ITerminalTextData source, int sourceStartLine, int destStartLine, int length) {
		if (destStartLine < 0 || destStartLine + length > fHeight) {
			throw new IllegalArgumentException(
					"Value of 'destStartLine'+'length' parameters must be valid line (range [0-" //$NON-NLS-1$
							+ fHeight + "). Parameter values: 'destStartLine'=" + destStartLine //$NON-NLS-1$
							+ ", 'size'=" //$NON-NLS-1$
							+ length);
		}
		if (sourceStartLine < 0 || sourceStartLine + length > source.getHeight()) {
			throw new IllegalArgumentException(
					"Value of 'sourceStartLine'+'length' parameters must be valid line (range [0-" //$NON-NLS-1$
							+ source.getHeight() + "). Parameter values: 'sourceStartLine'=" + sourceStartLine //$NON-NLS-1$
							+ ", 'size'=" //$NON-NLS-1$
							+ length);
		}
		for (int i = 0; i < length; i++) {
			copyLine(source, i + sourceStartLine, i + destStartLine);
		}
	}

	@Override
	public void copyLine(ITerminalTextData source, int sourceLine, int destLine) {
		fChars[destLine] = source.getChars(sourceLine);
		fStyle[destLine] = source.getStyles(sourceLine);
		fWrappedLines.set(destLine, source.isWrappedLine(sourceLine));
	}

	@Override
	public char[] getChars(int line) {
		if (fChars[line] == null) {
			return null;
		}
		return fChars[line].clone();
	}

	@Override
	public TerminalStyle[] getStyles(int line) {
		if (fStyle[line] == null) {
			return null;
		}
		return fStyle[line].clone();
	}

	public void setLine(int line, char[] chars, TerminalStyle[] styles) {
		fChars[line] = chars.clone();
		fStyle[line] = styles.clone();
		fWrappedLines.clear(line);
	}

	@Override
	public void setMaxHeight(int height) {
		fMaxHeight = height;
	}

	@Override
	public int getMaxHeight() {
		return fMaxHeight;
	}

	@Override
	public void cleanLine(int line) {
		fChars[line] = null;
		fStyle[line] = null;
		fWrappedLines.clear(line);
	}

	@Override
	public int getCursorColumn() {
		return fCursorColumn;
	}

	@Override
	public int getCursorLine() {
		return fCursorLine;
	}

	@Override
	public void setCursorColumn(int column) {
		fCursorColumn = column;
	}

	@Override
	public void setCursorLine(int line) {
		fCursorLine = line;
	}

	@Override
	public boolean isWrappedLine(int line) {
		return fWrappedLines.get(line);
	}

	@Override
	public void setWrappedLine(int line) {
		fWrappedLines.set(line);
	}
}
