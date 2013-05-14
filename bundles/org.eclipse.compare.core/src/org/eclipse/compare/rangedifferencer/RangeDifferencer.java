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

import org.eclipse.compare.internal.core.Messages;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

/**
 * A <code>RangeDifferencer</code> finds the differences between two or three <code>IRangeComparator</code>s.
 * <p>
 * To use the differencer, clients provide an <code>IRangeComparator</code>
 * that breaks their input data into a sequence of comparable entities. The differencer
 * returns the differences among these sequences as an array of <code>RangeDifference</code> objects
 * (<code>findDifferences</code> methods).
 * Every <code>RangeDifference</code> represents a single kind of difference
 * and the corresponding ranges of the underlying comparable entities in the
 * left, right, and optionally ancestor sides.
 * </p>
 * <p>
 * Alternatively, the <code>findRanges</code> methods not only return objects for
 * the differing ranges but for non-differing ranges too.
 * </p>
 *
 * @see IRangeComparator
 * @see RangeDifference
 */
public final class RangeDifferencer {
	
	private static final RangeDifference[] EMPTY_RESULT= new RangeDifference[0];
	
	private static final AbstractRangeDifferenceFactory defaultFactory = new AbstractRangeDifferenceFactory() {
		protected RangeDifference createRangeDifference() {
			return new RangeDifference(RangeDifference.NOCHANGE);
		}
	};
	
	/* (non Javadoc)
	 * Cannot be instantiated!
	 */
	private RangeDifferencer() {
		// nothing to do
	}
	
	/**
	 * Finds the differences between two <code>IRangeComparator</code>s.
	 * The differences are returned as an array of <code>RangeDifference</code>s.
	 * If no differences are detected an empty array is returned.
	 * 
	 * @param left the left range comparator
	 * @param right the right range comparator
	 * @return an array of range differences, or an empty array if no differences were found
	 */
	public static RangeDifference[] findDifferences(IRangeComparator left, IRangeComparator right) {
		return findDifferences((IProgressMonitor)null, left, right);
	}
	
	/**
	 * Finds the differences between two <code>IRangeComparator</code>s.
	 * The differences are returned as an array of <code>RangeDifference</code>s.
	 * If no differences are detected an empty array is returned.
	 * 
	 * @param pm if not <code>null</code> used to report progress
	 * @param left the left range comparator
	 * @param right the right range comparator
	 * @return an array of range differences, or an empty array if no differences were found
	 * @since 2.0
	 */
	public static RangeDifference[] findDifferences(IProgressMonitor pm, IRangeComparator left, IRangeComparator right) {
		return findDifferences(defaultFactory, null, left, right);
	}
	
	/**
	 * Finds the differences between two <code>IRangeComparator</code>s.
	 * The differences are returned as an array of <code>RangeDifference</code>s.
	 * If no differences are detected an empty array is returned.
	 * 
	 * @param factory
	 * @param pm if not <code>null</code> used to report progress
	 * @param left the left range comparator
	 * @param right the right range comparator
	 * @return an array of range differences, or an empty array if no differences were found
	 * @since org.eclipse.compare.core 3.5
	 */
	public static RangeDifference[] findDifferences(AbstractRangeDifferenceFactory factory, IProgressMonitor pm, IRangeComparator left, IRangeComparator right) {
		return RangeComparatorLCS.findDifferences(factory, pm, left, right);
	}

	/**
	 * Finds the differences among three <code>IRangeComparator</code>s.
	 * The differences are returned as a list of <code>RangeDifference</code>s.
	 * If no differences are detected an empty list is returned.
	 * If the ancestor range comparator is <code>null</code>, a two-way
	 * comparison is performed.
	 * 
	 * @param ancestor the ancestor range comparator or <code>null</code>
	 * @param left the left range comparator
	 * @param right the right range comparator
	 * @return an array of range differences, or an empty array if no differences were found
	 */
	public static RangeDifference[] findDifferences(IRangeComparator ancestor, IRangeComparator left, IRangeComparator right) {
		return findDifferences(null, ancestor, left, right);
	}
	
	/**
	 * Finds the differences among three <code>IRangeComparator</code>s.
	 * The differences are returned as a list of <code>RangeDifference</code>s.
	 * If no differences are detected an empty list is returned.
	 * If the ancestor range comparator is <code>null</code>, a two-way
	 * comparison is performed.
	 * 
	 * @param pm if not <code>null</code> used to report progress
	 * @param ancestor the ancestor range comparator or <code>null</code>
	 * @param left the left range comparator
	 * @param right the right range comparator
	 * @return an array of range differences, or an empty array if no differences were found
	 * @since 2.0
	 */
	public static RangeDifference[] findDifferences(IProgressMonitor pm, IRangeComparator ancestor, IRangeComparator left, IRangeComparator right) {
		return findDifferences(defaultFactory, pm, ancestor, left, right);
	}
	
	/**
	 * Finds the differences among three <code>IRangeComparator</code>s.
	 * The differences are returned as a list of <code>RangeDifference</code>s.
	 * If no differences are detected an empty list is returned.
	 * If the ancestor range comparator is <code>null</code>, a two-way
	 * comparison is performed.
	 * 	 
	 * @param factory
	 * @param pm if not <code>null</code> used to report progress
	 * @param ancestor the ancestor range comparator or <code>null</code>
	 * @param left the left range comparator
	 * @param right the right range comparator
	 * @return an array of range differences, or an empty array if no differences were found
	 * @since org.eclipse.compare.core 3.5
	 */
	public static RangeDifference[] findDifferences(AbstractRangeDifferenceFactory factory, IProgressMonitor pm, IRangeComparator ancestor, IRangeComparator left, IRangeComparator right) {
		try {
			if (ancestor == null)
				return findDifferences(factory, pm, left, right);
			SubMonitor monitor = SubMonitor.convert(pm, Messages.RangeComparatorLCS_0, 100);
			RangeDifference[] leftAncestorScript= null;
			RangeDifference[] rightAncestorScript= findDifferences(factory, monitor.newChild(50), ancestor, right);
			if (rightAncestorScript != null) {
				monitor.setWorkRemaining(100);
				leftAncestorScript= findDifferences(factory, monitor.newChild(50), ancestor, left);
			}
			if (rightAncestorScript == null || leftAncestorScript == null)
				return null;
	
			DifferencesIterator myIter= new DifferencesIterator(rightAncestorScript);
			DifferencesIterator yourIter= new DifferencesIterator(leftAncestorScript);
	
			List diff3= new ArrayList();
			diff3.add(factory.createRangeDifference(RangeDifference.ERROR)); // add a sentinel
	
			int changeRangeStart= 0;
			int changeRangeEnd= 0;
			//
			// Combine the two two-way edit scripts into one
			//
			monitor.setWorkRemaining(rightAncestorScript.length + leftAncestorScript.length);
			while (myIter.fDifference != null || yourIter.fDifference != null) {
	
				DifferencesIterator startThread;
				myIter.removeAll();
				yourIter.removeAll();
				//
				// take the next diff that is closer to the start
				//
				if (myIter.fDifference == null)
					startThread= yourIter;
				else if (yourIter.fDifference == null)
					startThread= myIter;
				else { // not at end of both scripts take the lowest range
					if (myIter.fDifference.leftStart < yourIter.fDifference.leftStart) { // 2 -> common (Ancestor) change range
						startThread= myIter;
					} else if (myIter.fDifference.leftStart > yourIter.fDifference.leftStart) {
						startThread= yourIter;
					} else {
						if (myIter.fDifference.leftLength == 0 && yourIter.fDifference.leftLength == 0) {
							//insertion into the same position is conflict. 
							changeRangeStart= myIter.fDifference.leftStart;
							changeRangeEnd= myIter.fDifference.leftEnd();
							myIter.next();
							yourIter.next();
							diff3.add(createRangeDifference3(factory, myIter, yourIter, diff3, right, left, changeRangeStart, changeRangeEnd));
							continue;
						} else 	if (myIter.fDifference.leftLength == 0) {
							//insertion into a position, and modification to the next line, is not conflict.
							startThread= myIter;
						} else if (yourIter.fDifference.leftLength == 0) {
							startThread = yourIter;
						} else {
							//modifications to overlapping lines is conflict.
							startThread= myIter;
						}
					}
						
				}
				changeRangeStart= startThread.fDifference.leftStart;
				changeRangeEnd= startThread.fDifference.leftEnd();
	
				startThread.next();
				monitor.worked(1);
				//
				// check for overlapping changes with other thread
				// merge overlapping changes with this range
				//
				DifferencesIterator other= startThread.other(myIter, yourIter);
				while (other.fDifference != null && other.fDifference.leftStart < changeRangeEnd) {
					int newMax= other.fDifference.leftEnd();
					other.next();
					monitor.worked(1);
					if (newMax > changeRangeEnd) {
						changeRangeEnd= newMax;
						other= other.other(myIter, yourIter);
					}
				}
				diff3.add(createRangeDifference3(factory, myIter, yourIter, diff3, right, left, changeRangeStart, changeRangeEnd));
			}
	
			// remove sentinel
			diff3.remove(0);
			return (RangeDifference[]) diff3.toArray(EMPTY_RESULT);
		} finally {
			if (pm != null)
				pm.done();
		}
	}

	/**
	 * Finds the differences among two <code>IRangeComparator</code>s.
	 * In contrast to <code>findDifferences</code>, the result
	 * contains <code>RangeDifference</code> elements for non-differing ranges too.
	 * 
	 * @param left the left range comparator
	 * @param right the right range comparator
	 * @return an array of range differences
	 */
	public static RangeDifference[] findRanges(IRangeComparator left,
			IRangeComparator right) {
		return findRanges((IProgressMonitor) null, left, right);
	}
	
	/**
	 * Finds the differences among two <code>IRangeComparator</code>s.
	 * In contrast to <code>findDifferences</code>, the result
	 * contains <code>RangeDifference</code> elements for non-differing ranges too.
	 * 
	 * @param pm if not <code>null</code> used to report progress
	 * @param left the left range comparator
	 * @param right the right range comparator
	 * @return an array of range differences
	 * @since 2.0
	 */
	public static RangeDifference[] findRanges(IProgressMonitor pm, IRangeComparator left, IRangeComparator right) {
		return findRanges(defaultFactory, pm, left, right);
	}
	
	/**
	 * Finds the differences among two <code>IRangeComparator</code>s.
	 * In contrast to <code>findDifferences</code>, the result
	 * contains <code>RangeDifference</code> elements for non-differing ranges too.
	 * 
	 * @param factory
	 * @param pm if not <code>null</code> used to report progress
	 * @param left the left range comparator
	 * @param right the right range comparator
	 * @return an array of range differences
	 * @since org.eclipse.compare.core 3.5
	 */
	public static RangeDifference[] findRanges(AbstractRangeDifferenceFactory factory, IProgressMonitor pm, IRangeComparator left, IRangeComparator right) {
		RangeDifference[] in= findDifferences(factory, pm, left, right);
		List out= new ArrayList();

		RangeDifference rd;

		int mstart= 0;
		int ystart= 0;

		for (int i= 0; i < in.length; i++) {
			RangeDifference es= in[i];

			rd= factory.createRangeDifference(RangeDifference.NOCHANGE, mstart, es.rightStart() - mstart, ystart, es.leftStart() - ystart);
			if (rd.maxLength() != 0)
				out.add(rd);

			out.add(es);

			mstart= es.rightEnd();
			ystart= es.leftEnd();
		}
		rd= factory.createRangeDifference(RangeDifference.NOCHANGE, mstart, right.getRangeCount() - mstart, ystart, left.getRangeCount() - ystart);
		if (rd.maxLength() > 0)
			out.add(rd);

		return (RangeDifference[]) out.toArray(EMPTY_RESULT);
	}

	/**
	 * Finds the differences among three <code>IRangeComparator</code>s.
	 * In contrast to <code>findDifferences</code>, the result
	 * contains <code>RangeDifference</code> elements for non-differing ranges too.
	 * If the ancestor range comparator is <code>null</code>, a two-way
	 * comparison is performed.
	 * 
	 * @param ancestor the ancestor range comparator or <code>null</code>
	 * @param left the left range comparator
	 * @param right the right range comparator
	 * @return an array of range differences
	 */
	public static RangeDifference[] findRanges(IRangeComparator ancestor, IRangeComparator left, IRangeComparator right) {
		return findRanges(null, ancestor, left, right);
	}
	
	/**
	 * Finds the differences among three <code>IRangeComparator</code>s.
	 * In contrast to <code>findDifferences</code>, the result
	 * contains <code>RangeDifference</code> elements for non-differing ranges too.
	 * If the ancestor range comparator is <code>null</code>, a two-way
	 * comparison is performed.
	 * 
	 * @param pm if not <code>null</code> used to report progress
	 * @param ancestor the ancestor range comparator or <code>null</code>
	 * @param left the left range comparator
	 * @param right the right range comparator
	 * @return an array of range differences
	 * @since 2.0
	 */
	public static RangeDifference[] findRanges(IProgressMonitor pm, IRangeComparator ancestor, IRangeComparator left, IRangeComparator right) {
		return findRanges(defaultFactory, pm, ancestor, left, right);
	}
	
	/**
	 * Finds the differences among three <code>IRangeComparator</code>s.
	 * In contrast to <code>findDifferences</code>, the result
	 * contains <code>RangeDifference</code> elements for non-differing ranges too.
	 * If the ancestor range comparator is <code>null</code>, a two-way
	 * comparison is performed.
	 * 
	 * @param factory
	 * @param pm if not <code>null</code> used to report progress
	 * @param ancestor the ancestor range comparator or <code>null</code>
	 * @param left the left range comparator
	 * @param right the right range comparator
	 * @return an array of range differences
	 * @since org.eclipse.compare.core 3.5
	 */
	public static RangeDifference[] findRanges(AbstractRangeDifferenceFactory factory, IProgressMonitor pm, IRangeComparator ancestor, IRangeComparator left, IRangeComparator right) {
		if (ancestor == null)
			return findRanges(factory,pm, left, right);

		RangeDifference[] in= findDifferences(factory, pm, ancestor, left, right);
		List out= new ArrayList();

		RangeDifference rd;

		int mstart= 0;
		int ystart= 0;
		int astart= 0;

		for (int i= 0; i < in.length; i++) {
			RangeDifference es= in[i];

			rd= factory.createRangeDifference(RangeDifference.NOCHANGE, mstart, es.rightStart() - mstart, ystart, es.leftStart() - ystart, astart, es.ancestorStart() - astart);
			if (rd.maxLength() > 0)
				out.add(rd);

			out.add(es);

			mstart= es.rightEnd();
			ystart= es.leftEnd();
			astart= es.ancestorEnd();
		}
		rd= factory.createRangeDifference(RangeDifference.NOCHANGE, mstart, right.getRangeCount() - mstart, ystart, left.getRangeCount() - ystart, astart, ancestor.getRangeCount() - astart);
		if (rd.maxLength() > 0)
			out.add(rd);

		return (RangeDifference[]) out.toArray(EMPTY_RESULT);
	}

	//---- private methods

	/*
	 * Creates a <code>RangeDifference3</code> given the
	 * state of two DifferenceIterators.
	 */
	private static RangeDifference createRangeDifference3(AbstractRangeDifferenceFactory configurator, DifferencesIterator myIter, DifferencesIterator yourIter, List diff3, 
		IRangeComparator right, IRangeComparator left, int changeRangeStart,  int changeRangeEnd) {

		int rightStart, rightEnd;
		int leftStart, leftEnd;
		int kind= RangeDifference.ERROR;
		RangeDifference last= (RangeDifference) diff3.get(diff3.size() - 1);

		Assert.isTrue((myIter.getCount() != 0 || yourIter.getCount() != 0));	// At least one range array must be non-empty
		//
		// find corresponding lines to fChangeRangeStart/End in right and left
		//
		if (myIter.getCount() == 0) { // only left changed
			rightStart= changeRangeStart - last.ancestorEnd() + last.rightEnd();
			rightEnd= changeRangeEnd - last.ancestorEnd() + last.rightEnd();
			kind= RangeDifference.LEFT;
		} else {
			RangeDifference f= (RangeDifference) myIter.fRange.get(0);
			RangeDifference l= (RangeDifference) myIter.fRange.get(myIter.fRange.size() - 1);
			rightStart= changeRangeStart - f.leftStart + f.rightStart;
			rightEnd= changeRangeEnd - l.leftEnd() + l.rightEnd();
		}

		if (yourIter.getCount() == 0) { // only right changed
			leftStart= changeRangeStart - last.ancestorEnd() + last.leftEnd();
			leftEnd= changeRangeEnd - last.ancestorEnd() + last.leftEnd();
			kind= RangeDifference.RIGHT;
		} else {
			RangeDifference f= (RangeDifference) yourIter.fRange.get(0);
			RangeDifference l= (RangeDifference) yourIter.fRange.get(yourIter.fRange.size() - 1);
			leftStart= changeRangeStart - f.leftStart + f.rightStart;
			leftEnd= changeRangeEnd - l.leftEnd() + l.rightEnd();
		}

		if (kind == RangeDifference.ERROR) { // overlapping change (conflict) -> compare the changed ranges
			if (rangeSpansEqual(right, rightStart, rightEnd - rightStart, left, leftStart, leftEnd - leftStart))
				kind= RangeDifference.ANCESTOR;
			else
				kind= RangeDifference.CONFLICT;
		}
		return configurator.createRangeDifference(kind, rightStart, rightEnd - rightStart, leftStart, leftEnd - leftStart, changeRangeStart, changeRangeEnd - changeRangeStart);
	}

	/*
	 * Tests whether <code>right</code> and <code>left</code> changed in the same way
	 */
	private static boolean rangeSpansEqual(IRangeComparator right, int rightStart, int rightLen, IRangeComparator left, int leftStart, int leftLen) {
		if (rightLen == leftLen) {
			int i= 0;
			for (i= 0; i < rightLen; i++) {
				if (!rangesEqual(right, rightStart + i, left, leftStart + i))
					break;
			}
			if (i == rightLen)
				return true;
		}
		return false;
	}
	
	/*
	 * Tests if two ranges are equal
	 */
	private static boolean rangesEqual(IRangeComparator a, int ai, IRangeComparator b, int bi) {
		return a.rangesEqual(ai, b, bi);
	}
}

