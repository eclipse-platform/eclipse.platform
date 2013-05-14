/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.contentmergeviewer;

import org.eclipse.compare.rangedifferencer.IRangeComparator;
import org.eclipse.core.runtime.Assert;

/**
 * Implements the <code>ITokenComparator</code> interface for words (or tokens)
 * in a string.
 * A <code>TokenComparator</code> is used as the input for the <code>RangeDifferencer</code>
 * engine to perform a token oriented compare on strings.
 * <p>
 * This class may be instantiated by clients but is not intended to be subclassed.
 * @since 3.4
 */
public class TokenComparator implements ITokenComparator {

	private String fText;
	private int fCount;
	private int[] fStarts;
	private int[] fLengths;

	/**
	 * Creates a <code>TokenComparator</code> for the given string.
	 *
	 * @param text the string that is split into token
	 */
	public TokenComparator(String text) {
		
		Assert.isNotNull(text);

		fText= text;
		
		int length= fText.length();
		fStarts= new int[length];	// pessimistic assumption!
		fLengths= new int[length];
		fCount= 0;
		
		char lastCategory= 0;	// 0: no category
		for (int i= 0; i < length; i++) {
			char c= fText.charAt(i);
			
			char category= '?';	// unspecified category
			if (Character.isWhitespace(c))
				category= ' ';	// white space category
			else if (Character.isDigit(c))
				category= '0';	// digits
			else if (Character.isLetter(c))
				category= 'a';	// letters
			else if (c == '\"' || c == '\'')
				category= '\"';	// quotes (see bug 198671)
			
			if (category != lastCategory) {
				// start a new token
				fStarts[fCount++]= i;
				lastCategory= category;
			}
			fLengths[fCount-1]++;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.rangedifferencer.IRangeComparator#getRangeCount()
	 */
	public int getRangeCount() {
		return fCount;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.contentmergeviewer.ITokenComparator#getTokenStart(int)
	 */
	public int getTokenStart(int index) {
		if (index < fCount)
			return fStarts[index];
		return fText.length();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.contentmergeviewer.ITokenComparator#getTokenLength(int)
	 */
	public int getTokenLength(int index) {
		if (index < fCount)
			return fLengths[index];
		return 0;
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.compare.rangedifferencer.IRangeComparator#rangesEqual(int, org.eclipse.compare.rangedifferencer.IRangeComparator, int)
	 */
	public boolean rangesEqual(int thisIndex, IRangeComparator other, int otherIndex) {
		if (other != null && getClass() == other.getClass()) {
			TokenComparator tc= (TokenComparator) other;
			int thisLen= getTokenLength(thisIndex);
			int otherLen= tc.getTokenLength(otherIndex);
			if (thisLen == otherLen)
				return fText.regionMatches(false, getTokenStart(thisIndex), tc.fText, tc.getTokenStart(otherIndex), thisLen);
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.rangedifferencer.IRangeComparator#skipRangeComparison(int, int, org.eclipse.compare.rangedifferencer.IRangeComparator)
	 */
	public boolean skipRangeComparison(int length, int max, IRangeComparator other) {

		if (getRangeCount() < 50 || other.getRangeCount() < 50)
			return false;

		if (max < 100)
			return false;

		if (length < 100)
			return false;

		if (max > 800)
			return true;

		if (length < max / 4)
			return false;

		return true;
	}
}
