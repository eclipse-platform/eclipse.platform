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

import org.eclipse.terminal.model.ITerminalTextData;
import org.eclipse.terminal.model.ITerminalTextDataSnapshot;
import org.eclipse.terminal.model.LineSegment;
import org.eclipse.terminal.model.TerminalStyle;

/**
 * This class is optimized for scrolling the entire {@link #getHeight()}.
 * The scrolling is done by moving an offset into the data and using
 * the modulo operator.
 *
 */
public class TerminalTextDataFastScroll implements ITerminalTextData {

	final ITerminalTextData fData;
	private int fHeight;
	private int fMaxHeight;
	/**
	 * The offset into the array.
	 */
	int fOffset;

	public TerminalTextDataFastScroll(ITerminalTextData data, int maxHeight) {
		fMaxHeight = maxHeight;
		fData = data;
		fData.setDimensions(maxHeight, fData.getWidth());
		if (maxHeight > 2) {
			moveOffset(-2);
		}
	}

	public TerminalTextDataFastScroll(int maxHeight) {
		this(new TerminalTextDataStore(), maxHeight);
	}

	public TerminalTextDataFastScroll() {
		this(new TerminalTextDataStore(), 1);
	}

	/**
	 *
	 * @param line
	 * @return the actual line number in {@link #fData}
	 */
	int getPositionOfLine(int line) {
		return (line + fOffset) % fMaxHeight;
	}

	/**
	 * Moves offset by delta. This does <b>not</b> move the data!
	 * @param delta
	 */
	void moveOffset(int delta) {
		if (Math.abs(delta) >= fMaxHeight) {
			throw new IllegalArgumentException(
					"Parameter 'delta' absolute value (" + delta + ") must be less than maxHeight(" + fMaxHeight + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		fOffset = (fMaxHeight + fOffset + delta) % fMaxHeight;

	}

	@Override
	public void addLine() {
		if (getHeight() < fMaxHeight) {
			setDimensions(getHeight() + 1, getWidth());
		} else {
			scroll(0, getHeight(), -1);
		}
	}

	@Override
	public void cleanLine(int line) {
		fData.cleanLine(getPositionOfLine(line));
	}

	@Override
	public void copy(ITerminalTextData source) {
		int n = source.getHeight();
		setDimensions(source.getHeight(), source.getWidth());
		for (int i = 0; i < n; i++) {
			fData.copyLine(source, i, getPositionOfLine(i));
		}
	}

	@Override
	public void copyLine(ITerminalTextData source, int sourceLine, int destLine) {
		fData.copyLine(source, sourceLine, getPositionOfLine(destLine));
	}

	@Override
	public void copyRange(ITerminalTextData source, int sourceStartLine, int destStartLine, int length) {
		if (destStartLine < 0 || destStartLine + length > fHeight) {
			throw new IllegalArgumentException(
					"Value of 'destStartLine'+'length' parameters must be valid line (range [0-" //$NON-NLS-1$
							+ getHeight() + "). Parameter values: 'destStartLine'=" + destStartLine + ", 'size'=" //$NON-NLS-1$//$NON-NLS-2$
							+ length);
		}
		for (int i = 0; i < length; i++) {
			fData.copyLine(source, i + sourceStartLine, getPositionOfLine(i + destStartLine));
		}
	}

	@Override
	public char getChar(int line, int column) {
		validateLineParameter(line);
		return fData.getChar(getPositionOfLine(line), column);
	}

	@Override
	public char[] getChars(int line) {
		return fData.getChars(getPositionOfLine(line));
	}

	@Override
	public int getHeight() {
		return fHeight;
	}

	@Override
	public LineSegment[] getLineSegments(int line, int startCol, int numberOfCols) {
		validateLineParameter(line);
		return fData.getLineSegments(getPositionOfLine(line), startCol, numberOfCols);
	}

	@Override
	public int getMaxHeight() {
		return fMaxHeight;
	}

	@Override
	public TerminalStyle getStyle(int line, int column) {
		validateLineParameter(line);
		return fData.getStyle(getPositionOfLine(line), column);
	}

	@Override
	public TerminalStyle[] getStyles(int line) {
		validateLineParameter(line);
		return fData.getStyles(getPositionOfLine(line));
	}

	@Override
	public int getWidth() {
		return fData.getWidth();
	}

	@Override
	public ITerminalTextDataSnapshot makeSnapshot() {
		return fData.makeSnapshot();
	}

	private void cleanLines(int line, int len) {
		for (int i = line; i < line + len; i++) {
			fData.cleanLine(getPositionOfLine(i));
		}
	}

	@Override
	public void scroll(int startLine, int size, int shift) {
		if (startLine + size > getHeight()) {
			throw new IllegalArgumentException("Value of 'startLine'+'size' parameters must be valid line (range [0-" //$NON-NLS-1$
					+ getHeight() + "). Parameter values: 'startLine'=" + startLine + ", 'size'=" + size); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (shift >= fMaxHeight || -shift >= fMaxHeight) {
			cleanLines(startLine, fMaxHeight - startLine);
			return;
		}
		if (size == fHeight) {
			// This is the case this class is optimized for!
			moveOffset(-shift);
			// we only have to clean the lines that appear by the move
			if (shift < 0) {
				cleanLines(Math.max(startLine, startLine + size + shift), Math.min(-shift, getHeight() - startLine));
			} else {
				cleanLines(startLine, Math.min(shift, getHeight() - startLine));
			}
		} else {
			// we have to copy the lines.
			if (shift < 0) {
				// move the region up
				// shift is negative!!
				for (int i = startLine; i < startLine + size + shift; i++) {
					fData.copyLine(fData, getPositionOfLine(i - shift), getPositionOfLine(i));
				}
				// then clean the opened lines
				cleanLines(Math.max(0, startLine + size + shift), Math.min(-shift, getHeight() - startLine));
			} else {
				for (int i = startLine + size - 1; i >= startLine && i - shift >= 0; i--) {
					fData.copyLine(fData, getPositionOfLine(i - shift), getPositionOfLine(i));
				}
				cleanLines(startLine, Math.min(shift, getHeight() - startLine));
			}
		}
	}

	@Override
	public void setChar(int line, int column, char c, TerminalStyle style) {
		validateLineParameter(line);
		fData.setChar(getPositionOfLine(line), column, c, style);
	}

	@Override
	public void setChars(int line, int column, char[] chars, int start, int len, TerminalStyle style) {
		validateLineParameter(line);
		fData.setChars(getPositionOfLine(line), column, chars, start, len, style);
	}

	@Override
	public void setChars(int line, int column, char[] chars, TerminalStyle style) {
		validateLineParameter(line);
		fData.setChars(getPositionOfLine(line), column, chars, style);
	}

	@Override
	public void setDimensions(int height, int width) {
		if (height < 0) {
			throw new IllegalArgumentException("Parameter 'height' can't be negative value:" + height); //$NON-NLS-1$
		}
		if (width < 0) {
			throw new IllegalArgumentException("Parameter 'width' can't be negative value:" + width); //$NON-NLS-1$
		}
		if (height > fMaxHeight) {
			setMaxHeight(height);
		}
		fHeight = height;
		if (width != fData.getWidth()) {
			fData.setDimensions(fMaxHeight, width);
		}
	}

	@Override
	public void setMaxHeight(int maxHeight) {
		if (maxHeight < fHeight) {
			throw new IllegalArgumentException("Parameter 'maxHeight' (value '" + maxHeight //$NON-NLS-1$
					+ "') must't be less than 'fHeight' (value '" + fHeight + "')"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		// move everything to offset0
		int start = getPositionOfLine(0);
		if (start != 0) {
			// invent a more efficient algorithm....
			ITerminalTextData buffer = new TerminalTextDataStore();
			// create a buffer with the expected height
			buffer.setDimensions(maxHeight, getWidth());
			int n = Math.min(fMaxHeight - start, maxHeight);
			// copy the first part
			buffer.copyRange(fData, start, 0, n);
			// copy the second part
			if (n < maxHeight) {
				buffer.copyRange(fData, 0, n, Math.min(fMaxHeight - n, maxHeight - n));
			}
			// copy the buffer back to our data
			fData.copy(buffer);
			moveOffset(-start);
		} else {
			fData.setDimensions(maxHeight, fData.getWidth());
		}
		fMaxHeight = maxHeight;
	}

	@Override
	public int getCursorColumn() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getCursorLine() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setCursorColumn(int column) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setCursorLine(int line) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isWrappedLine(int line) {
		validateLineParameter(line);
		return fData.isWrappedLine(getPositionOfLine(line));
	}

	private void validateLineParameter(int line) {
		if (line < 0 || line >= fHeight) {
			throw new IllegalArgumentException(
					"Parameter 'line' must be >= 0 and less than 'width' (current value '" + fHeight + "')"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	@Override
	public void setWrappedLine(int line) {
		validateLineParameter(line);
		fData.setWrappedLine(getPositionOfLine(line));
	}

}
