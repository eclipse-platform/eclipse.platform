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
package org.eclipse.compare.rangedifferencer;

import java.util.ArrayList;
import java.util.List;

/**
 * A custom iterator to iterate over a List of <code>RangeDifferences</code>.
 * It is used internally by the <code>RangeDifferencer</code>.
 */
/* package */ class DifferencesIterator {

	List fRange;
	int fIndex;
	RangeDifference[] fArray;	
	RangeDifference fDifference;
	
	/*
	 * Creates a differences iterator on an array of <code>RangeDifference</code>s.
	 */
	DifferencesIterator(RangeDifference[] differenceRanges) {
		
		this.fArray= differenceRanges;
		this.fIndex= 0;
		this.fRange= new ArrayList();
		if (this.fIndex < this.fArray.length)
			this.fDifference= this.fArray[this.fIndex++];
		else
			this.fDifference= null;
	}

	/*
	  * Returns the number of RangeDifferences
	  */
	int getCount() {
		return this.fRange.size();
	}

	/*
	 * Appends the edit to its list and moves to the next <code>RangeDifference</code>.
	 */
	void next() {
		this.fRange.add(this.fDifference);
		if (this.fDifference != null) {
			if (this.fIndex < this.fArray.length)
				this.fDifference= this.fArray[this.fIndex++];
			else
				this.fDifference= null;
		}
	}

	/*
	 * Difference iterators are used in pairs.
	 * This method returns the other iterator.
	 */
	DifferencesIterator other(DifferencesIterator right, DifferencesIterator left) {
		if (this == right)
			return left;
		return right;
	}

	/*
	  * Removes all <code>RangeDifference</code>s
	  */
	void removeAll() {
		this.fRange.clear();
	}
}
