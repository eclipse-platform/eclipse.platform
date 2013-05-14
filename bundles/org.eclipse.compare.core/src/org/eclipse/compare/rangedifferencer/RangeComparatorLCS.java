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
package org.eclipse.compare.rangedifferencer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.internal.core.LCS;
import org.eclipse.compare.internal.core.Messages;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;

/* package */ class RangeComparatorLCS extends LCS {
	
	private final IRangeComparator comparator1, comparator2;
	private int[][] lcs;
	
	public static RangeDifference[] findDifferences(AbstractRangeDifferenceFactory factory, IProgressMonitor pm, IRangeComparator left, IRangeComparator right) {
		RangeComparatorLCS lcs = new RangeComparatorLCS(left, right);
		SubMonitor monitor = SubMonitor.convert(pm, Messages.RangeComparatorLCS_0, 100);
		try {
			lcs.longestCommonSubsequence(monitor.newChild(95));
			return lcs.getDifferences(monitor.newChild(5), factory);
		} finally {
			if (pm != null)
				pm.done();
		}
	}
	
	public RangeComparatorLCS(IRangeComparator comparator1, IRangeComparator comparator2) {
		this.comparator1 = comparator1;
		this.comparator2 = comparator2;
	}
	
	protected int getLength1() {
		return this.comparator1.getRangeCount();
	}

	protected int getLength2() {
		return this.comparator2.getRangeCount();
	}

	protected void initializeLcs(int lcsLength) {
		this.lcs = new int[2][lcsLength];
	}

	protected boolean isRangeEqual(int i1, int i2) {
		return this.comparator1.rangesEqual(i1, this.comparator2, i2);
	}

	protected void setLcs(int sl1, int sl2) {
		// Add one to the values so that 0 can mean that the slot is empty
		this.lcs[0][sl1] = sl1 + 1;
		this.lcs[1][sl1] = sl2 + 1;
	}
	
	public RangeDifference[] getDifferences(SubMonitor subMonitor, AbstractRangeDifferenceFactory factory) {
		try {
			List differences = new ArrayList();
			int length = getLength();
			if (length == 0) {
				differences.add(factory.createRangeDifference(RangeDifference.CHANGE, 0, this.comparator2.getRangeCount(), 0, this.comparator1.getRangeCount()));
			} else {
				subMonitor.beginTask(null, length);
				int index1, index2;
				index1 = index2 = 0;
				int l1, l2;
				int s1 = -1;
				int s2 = -1;
				while(index1 < this.lcs[0].length && index2 < this.lcs[1].length) {
					// Move both LCS lists to the next occupied slot
					while ((l1= this.lcs[0][index1]) == 0) {
						index1++;
						if (index1 >= this.lcs[0].length)
							break;
					}
					if (index1 >= this.lcs[0].length)
						break;
					while ((l2= this.lcs[1][index2]) == 0) {
						index2++;
						if (index2 >= this.lcs[1].length)
							break;
					}
					if (index2 >= this.lcs[1].length)
						break;
					// Convert the entry to an array index (see setLcs(int, int))
					int end1 = l1 - 1;
					int end2 = l2 - 1;
					if (s1 == -1 && (end1 != 0 || end2 != 0)) {
						// There is a diff at the beginning
						// TODO: We need to conform that this is the proper order
						differences.add(factory.createRangeDifference(RangeDifference.CHANGE, 0, end2, 0, end1));
					} else if (end1 != s1 + 1 || end2 != s2 + 1) {
						// A diff was found on one of the sides
						int leftStart = s1 + 1;
						int leftLength = end1 - leftStart;
						int rightStart = s2 + 1;
						int rightLength = end2 - rightStart;
						// TODO: We need to conform that this is the proper order
						differences.add(factory.createRangeDifference(RangeDifference.CHANGE, rightStart, rightLength, leftStart, leftLength));
					}
					s1 = end1;
					s2 = end2;
					index1++;
					index2++;
					worked(subMonitor, 1);
				}
				if (s1 != -1 && (s1 + 1 < this.comparator1.getRangeCount() || s2 + 1 < this.comparator2.getRangeCount())) {
					// TODO: we need to find the proper way of representing an append
					int leftStart = s1 < this.comparator1.getRangeCount() ? s1 + 1 : s1;
					int rightStart = s2 < this.comparator2.getRangeCount() ? s2 + 1 : s2;
					// TODO: We need to confirm that this is the proper order
					differences.add(factory.createRangeDifference(RangeDifference.CHANGE, rightStart, this.comparator2.getRangeCount() - (s2 + 1), leftStart, this.comparator1.getRangeCount() - (s1 + 1)));
				}
				
			}
			return (RangeDifference[]) differences.toArray(new RangeDifference[differences.size()]);
		} finally {
			subMonitor.done();
		}
	}
	
	private void worked(SubMonitor subMonitor, int work) {
		if (subMonitor.isCanceled())
			throw new OperationCanceledException();
		subMonitor.worked(work);	
	}

	/**
	 * This method takes an LCS result interspersed with zeros (i.e. empty slots 
	 * from the LCS algorithm), compacts it and shifts the LCS chunks as far towards 
	 * the front as possible. This tends to produce good results most of the time.
	 * 
	 * @param lcsSide A subsequence of original, presumably it is the LCS of it and
	 *            some other collection of lines
	 * @param length The number of non-empty (i.e non-zero) entries in LCS
	 * @param comparator The comparator used to generate the LCS
	 */
	 private void compactAndShiftLCS(int[] lcsSide, int length,
			IRangeComparator comparator) {
		// If the LCS is empty, just return
		if (length == 0)
			return;
		// Skip any leading empty slots
		int j = 0;
		while (lcsSide[j] == 0) {
			j++;
		}
		// Put the first non-empty value in position 0
		lcsSide[0] = lcsSide[j];
		j++;
		// Push all non-empty values down into the first N slots (where N is the length)
		for (int i = 1; i < length; i++) {
			while (lcsSide[j] == 0) {
				j++;
			}
			// Push the difference down as far as possible by comparing the line at the 
			// start of the diff with the line and the end and adjusting if they are the same
			int nextLine = lcsSide[i - 1] + 1;
			if (nextLine != lcsSide[j] && comparator.rangesEqual(nextLine - 1, comparator, lcsSide[j] - 1)) {
				lcsSide[i] = nextLine;
			} else {
				lcsSide[i] = lcsSide[j];
			}
			j++;
		}
		// Zero all slots after the length
		for (int i = length; i < lcsSide.length; i++) {
			lcsSide[i] = 0;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.internal.LCS#longestCommonSubsequence(org.eclipse.core.runtime.SubMonitor)
	 */
	public void longestCommonSubsequence(SubMonitor subMonitor) {
		super.longestCommonSubsequence(subMonitor);
		if (this.lcs != null) { // The LCS can be null if one of the sides is empty
			compactAndShiftLCS(this.lcs[0], getLength(), this.comparator1);
			compactAndShiftLCS(this.lcs[1], getLength(), this.comparator2);
		}
	}
}
