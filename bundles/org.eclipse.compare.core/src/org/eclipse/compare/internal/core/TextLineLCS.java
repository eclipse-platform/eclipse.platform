/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal.core;

import java.util.ArrayList;
import java.util.List;


public class TextLineLCS extends LCS {

	private final TextLine[] lines1;
	private final TextLine[] lines2;
	private TextLine[][] lcs;

	public TextLineLCS(TextLine[] lines1, TextLine[] lines2) {
		this.lines1 = lines1;
		this.lines2 = lines2;
	}

	public TextLine[][] getResult() {
		int length = getLength();
		if (length == 0)
			return new TextLine[2][0];
		TextLine[][] result = new TextLine[2][];

		// compact and shift the result
		result[0] = compactAndShiftLCS(this.lcs[0], length, this.lines1);
		result[1] = compactAndShiftLCS(this.lcs[1], length, this.lines2);

		return result;
	}

	protected int getLength2() {
		return this.lines2.length;
	}

	protected int getLength1() {
		return this.lines1.length;
	}
	
	protected boolean isRangeEqual(int i1, int i2) {
		return this.lines1[i1].sameText(this.lines2[i2]);
	}
	
	protected void setLcs(int sl1, int sl2) {
		this.lcs[0][sl1] = this.lines1[sl1];
		this.lcs[1][sl1] = this.lines2[sl2];
	}
	
	protected void initializeLcs(int length) {
		this.lcs = new TextLine[2][length];
	}
	
	/**
	 * This method takes an lcs result interspersed with nulls, compacts it and
	 * shifts the LCS chunks as far towards the front as possible. This tends to
	 * produce good results most of the time.
	 * 
	 * TODO: investigate what to do about comments. shifting either up or down
	 * hurts them
	 * 
	 * @param lcsSide A subsequence of original, presumably it is the LCS of it and
	 *            some other collection of lines
	 * @param len The number of non-null entries in lcs
	 * @param original The original sequence of lines of which lcs is a
	 *            subsequence
	 * 
	 * @return The subsequence lcs compacted and chunks shifted towards the
	 *         front
	 */
	private TextLine[] compactAndShiftLCS(TextLine[] lcsSide, int len,
			TextLine[] original) {
		TextLine[] result = new TextLine[len];

		if (len == 0) {
			return result;
		}

		int j = 0;

		while (lcsSide[j] == null) {
			j++;
		}

		result[0] = lcsSide[j];
		j++;

		for (int i = 1; i < len; i++) {
			while (lcsSide[j] == null) {
				j++;
			}

			if (original[result[i - 1].lineNumber() + 1].sameText(lcsSide[j])) {
				result[i] = original[result[i - 1].lineNumber() + 1];
			} else {
				result[i] = lcsSide[j];
			}
			j++;
		}

		return result;
	}
	
	/**
	 * Breaks the given text up into lines and returns an array of TextLine
	 * objects each corresponding to a single line, ordered according to the
	 * line number. That is result[i].lineNumber() == i and is the i'th line in
	 * text (starting from 0) Note: there are 1 more lines than there are
	 * newline characters in text. Corollary 1: if the last character is
	 * newline, the last line is empty Corollary 2: the empty string is 1 line
	 * 
	 * @param text The text to extract lines from
	 * @return the array of TextLine object each corresponding to a line of text
	 */
	public static TextLine[] getTextLines(String text) {
		List lines = new ArrayList();
		int begin = 0;
		int end = getEOL(text, 0);
		int lineNum = 0;
		while (end != -1) {
			lines.add(new TextLine(lineNum++, text.substring(begin, end)));
			begin = end + 1;
			end = getEOL(text, begin);
			if (end == begin && text.charAt(begin - 1) == '\r'
					&& text.charAt(begin) == '\n') {
				// we have '\r' followed by '\n', skip it
				begin = end + 1;
				end = getEOL(text, begin);
			}
		}

		/*
		 * this is the last line, no more newline characters, so take the rest
		 * of the string
		 */
		lines.add(new TextLine(lineNum, text.substring(begin)));
		TextLine[] aLines = new TextLine[lines.size()];
		lines.toArray(aLines);
		return aLines;
	}

	/**
	 * Returns the index of the next end of line marker ('\n' or '\r') after
	 * start
	 * 
	 * @param text The string to examine
	 * @param start The location in the string to start looking
	 * @return the index such that text.charAt(index) == '\n' or '\r', -1 if not
	 *         found
	 */
	private static int getEOL(String text, int start) {
		int max = text.length();
		for (int i = start; i < max; i++) {
			char c = text.charAt(i);
			if (c == '\n' || c == '\r') {
				return i;
			}
		}
		return -1;
	}

	/* used to store information about a single line of text */
	public static class TextLine {
		private int number; // the line number

		private String text; // the actual text

		public TextLine(int number, String text) {
			this.number = number;
			this.text = text;
		}

		/**
		 * Compares this TextLine to l and returns true if they have the same
		 * text
		 * 
		 * @param l the TextLine to compare to
		 * @return true if this and l have the same text
		 */
		public boolean sameText(TextLine l) {
			// compare the hashCode() first since that is much faster and most
			// of the time the text lines won't match
			return this.text.hashCode() == l.text.hashCode() && l.text.equals(this.text);
		}

		/**
		 * Returns the line number of this line
		 * 
		 * @return the line number
		 */
		public int lineNumber() {
			return this.number;
		}

		public String toString() {
			return "" + this.number + " " + this.text + "\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}
}
