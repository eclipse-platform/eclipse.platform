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


/**
 * For performing a so-called "token compare" on a line of text.
 * This interface extends the <code>IRangeComparator</code> interface
 * so that it can be used by the <code>TextMergeViewer</code>.
 * <p>
 * <code>TextMergeViewer</code> activates the token compare when navigating into
 * a range of differing lines. At first the lines are selected as a block.
 * When navigating into this block the token compare shows for every line 
 * the differing token by selecting them.
 * <p>
 * <code>TextMergeViewer</code>'s default token comparator works on characters separated
 * by whitespace. If a different strategy is needed (for example, to use Java tokens in
 * a Java-aware merge viewer), clients may create their own token
 * comparators by implementing this interface (and overriding the
 * <code>TextMergeViewer.createTokenComparator</code> factory method).
 * </p>
 *
 * @see TextMergeViewer
 */
public interface ITokenComparator extends IRangeComparator {

	/**
	 * Returns the start character position of the token with the given index.
	 * If the index is out of range (but not negative) the character position
	 * behind the last character (the length of the input string) is returned.
	 *
	 * @param index index of the token for which to return the start position
	 * @return the start position of the token with the given index
	 * @throws java.lang.IndexOutOfBoundsException if index is negative
	 */
	int getTokenStart(int index);

	/**
	 * Returns the character length of the token with the given index.
	 * If the index is out of range (but not negative) the value 0 is returned.
	 *
	 * @param index index of the token for which to return the start position
	 * @return the character length of the token with the given index
	 * @throws java.lang.IndexOutOfBoundsException if index is negative
	 */
	int getTokenLength(int index);
}
