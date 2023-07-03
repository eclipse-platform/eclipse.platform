/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.help.ui.internal;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledTextContent;
import org.eclipse.swt.custom.TextChangeListener;
import org.eclipse.swt.graphics.Drawable;
import org.eclipse.swt.graphics.GC;

public class StyledLineWrapper implements StyledTextContent {

	/**
	 * Internal representation of &lt;b&gt; - unlikely to occur in a text
	 */
	public static final String BOLD_CLOSE_TAG = "</@#$b>"; //$NON-NLS-1$
	/**
	 * Internal representation of &lt;b&gt; - unlikely to occur in a text
	 */
	public static final String BOLD_TAG = "<@#$b>"; //$NON-NLS-1$

	private Drawable drawable;

	/** Lines after splitting */
	private ArrayList<String> lines = new ArrayList<>();

	/** Style ranges, per line */
	private ArrayList<StyleRange> lineStyleRanges = new ArrayList<>();

	/** Character count */
	private int charCount = -1;

	/** Line breaker */
	private static BreakIterator lineBreaker = BreakIterator.getLineInstance();

	/** Beyond this length (pixels), lines should wrap */
	public final static int DEFAULT_WIDTH = 350;

	public int maxWidth;

	/**
	 * Constructor
	 */
	public StyledLineWrapper(String text, Drawable drawable, int minWidth) {
		this.drawable = drawable;
		maxWidth = Math.max(DEFAULT_WIDTH, minWidth);
		if (text == null || text.length() == 0)
			text = " "; // use one blank space //$NON-NLS-1$
		setText(text);
	}

	@Override
	public void addTextChangeListener(TextChangeListener l) {
		// do nothing
	}

	@Override
	public int getCharCount() {
		if (charCount != -1)
			return charCount;
		charCount = 0;
		for (Iterator<String> i = lines.iterator(); i.hasNext();)
			charCount += i.next().length();
		return charCount;
	}

	@Override
	public String getLine(int i) {
		if ((i >= lines.size()) || (i < 0))
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		return lines.get(i);
	}

	@Override
	public int getLineAtOffset(int offset) {
		if (offset >= getCharCount())
			return getLineCount() - 1;
		int count = 0;
		int line = -1;
		while (count <= offset) {
			count += getLine(++line).length();
		}
		return line;
	}

	@Override
	public int getLineCount() {
		if (lines.isEmpty())
			return 1;
		return lines.size();
	}

	@Override
	public String getLineDelimiter() {
		return null;
	}

	@Override
	public int getOffsetAtLine(int line) {
		if (lines.isEmpty())
			return 0;
		int offset = 0;
		for (int i = 0; i < line; i++)
			offset += getLine(i).length();
		return offset;
	}

	@Override
	public String getTextRange(int start, int end) {
		int l1 = getLineAtOffset(start);
		int l2 = getLineAtOffset(end);
		if (l1 == l2)
			return getLine(l1).substring(start - getOffsetAtLine(l1),
					end - start);
		StringBuilder range = new StringBuilder(getLine(l1).substring(
				start - getOffsetAtLine(l1)));
		for (int i = l1 + 1; i < l2; i++)
			range.append(getLine(i));
		range.append(getLine(l2).substring(0, end - getOffsetAtLine(l2)));
		return range.toString();
	}

	@Override
	public void removeTextChangeListener(TextChangeListener arg0) {
		// do nothing
	}

	@Override
	public void replaceTextRange(int arg0, int arg1, String arg2) {
		// do nothing
	}

	@Override
	public void setText(String text) {
		if (text == null)
			text = " "; //$NON-NLS-1$
		processLineBreaks(text);
		processStyles(text);
	}

	/**
	 * Returns the array of styles.
	 */
	public StyleRange[] getStyles() {
		StyleRange[] array = new StyleRange[lineStyleRanges.size()];
		lineStyleRanges.toArray(array);
		return array;
	}

	/**
	 * Create an array of lines with sytles stripped off. Each lines is at most
	 * MAX_LINE_LENGTH characters.
	 */
	private void processLineBreaks(String text) {
		// Create the original lines with style stripped
		lines = new ArrayList<>();
		char[] textChars = getUnstyledText(text).toCharArray();
		int start = 0;
		for (int i = start; i < textChars.length; i++) {
			char ch = textChars[i];
			if (ch == SWT.CR) {
				lines.add(new String(textChars, start, i - start));
				start = i + 1;
				// if we reached the end, stop
				if (start >= textChars.length)
					break;
				// see if the next character is an LF
				ch = textChars[start];
				if (ch == SWT.LF) {
					start++;
					i++;
					if (start >= textChars.length)
						break;
				}
			} else if (ch == SWT.LF) {
				lines.add(new String(textChars, start, i - start));
				start = i + 1;
				if (start >= textChars.length)
					break;
			} else if (i == textChars.length - 1) {
				lines.add(new String(textChars, start, i - start + 1));
			}
		}
		// Break long lines
		GC gc = new GC(drawable);
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			while (line.length() > 0) {
				int linebreak = getLineBreak(line, gc);
				if (linebreak == 0 || linebreak == line.length())
					break;
				String newline = line.substring(0, linebreak);
				lines.remove(i);
				lines.add(i, newline);
				line = line.substring(linebreak);
				lines.add(++i, line);
			}
		}
		gc.dispose();
	}

	/**
	 * Returns the text without the style
	 */
	private static String getUnstyledText(String styledText) {
		return styledText.replaceAll("</?@#\\$b>", ""); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Finds a good line breaking point
	 */
	private int getLineBreak(String line, GC gc) {
		lineBreaker.setText(line);
		int lastGoodIndex = 0;
		int currentIndex = lineBreaker.first();
		int width = gc.textExtent(line.substring(0, currentIndex)).x;
		while (width < maxWidth && currentIndex != BreakIterator.DONE) {
			lastGoodIndex = currentIndex;
			currentIndex = lineBreaker.next();
			if (currentIndex == BreakIterator.DONE) {
				break;
			}
			width = gc.textExtent(line.substring(0, currentIndex)).x;
		}
		return lastGoodIndex;
	}

	/**
	 * Creates all the (bold) style ranges for the text. It is assumed that the
	 * text has been split across lines.
	 */
	private void processStyles(String text) {
		// create a new array of styles
		lineStyleRanges = new ArrayList<>();
		// first, remove the line breaks
		text = text.replaceAll("\n|\r", ""); //$NON-NLS-1$ //$NON-NLS-2$
		int offset = 0;
		do {
			// create a style
			StyleRange style = new StyleRange();
			style.fontStyle = SWT.BOLD;
			// the index of the starting style in styled text
			int start = text.indexOf(BOLD_TAG, offset);
			if (start == -1)
				break;
			String prefix = getUnstyledText(text.substring(0, start));
			style.start = prefix.length();
			// the index of the ending style in styled text
			offset = start + 1;
			int end = text.indexOf(BOLD_CLOSE_TAG, offset);
			if (end == -1)
				break;
			prefix = getUnstyledText(text.substring(0, end));
			style.length = prefix.length() - style.start;
			lineStyleRanges.add(style);
			offset = end + 1;
		} while (offset < text.length());
	}
}
