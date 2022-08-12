/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
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
package org.eclipse.compare.tests;

import static org.junit.Assert.assertEquals;

import org.eclipse.compare.contentmergeviewer.ITokenComparator;
import org.eclipse.compare.internal.DocLineComparator;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.rangedifferencer.RangeDifferencer;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;
import org.junit.Test;

public class RangeDifferencerThreeWayDiffTest {

	static final String S = System.lineSeparator();

	@Test
	public void testInsertConflict() {
		String a = "A" + S + "B" + S + "C" + S + "D"; //$NON-NLS-1$
		String l = "A" + S + "B" + S + "x" + S + "C" + S + "D"; //$NON-NLS-1$
		String r = "A" + S + "B" + S + "y" + S + "C" + S + "D"; //$NON-NLS-1$

		RangeDifference[] diffs = findRange(a, l, r);

		assertEquals(3, diffs.length);
		assertEquals(RangeDifference.NOCHANGE, diffs[0].kind());
		assertEquals(RangeDifference.CONFLICT, diffs[1].kind());
		assertEquals(RangeDifference.NOCHANGE, diffs[2].kind());
	}

	@Test
	public void testChangeConflict() {
		String a = "A" + S + "B" + S + "C" + S + "D"; //$NON-NLS-1$
		String l = "A" + S + "b1" + S + "C" + S + "D"; //$NON-NLS-1$
		String r = "A" + S + "b2" + S + "C" + S + "D"; //$NON-NLS-1$

		RangeDifference[] diffs = findRange(a, l, r);

		assertEquals(3, diffs.length);
		assertEquals(RangeDifference.NOCHANGE, diffs[0].kind());
		assertEquals(RangeDifference.CONFLICT, diffs[1].kind());
		assertEquals(RangeDifference.NOCHANGE, diffs[2].kind());
	}

	@Test
	public void testDeleteAndChangeConflict() {
		String a = "A" + S + "B" + S + "C"; //$NON-NLS-1$
		String l = "A" + S + "C"; //$NON-NLS-1$
		String r = "A" + S + "b1" + S + "C"; //$NON-NLS-1$

		RangeDifference[] diffs = findRange(a, l, r);

		assertEquals(3, diffs.length);
		assertEquals(RangeDifference.NOCHANGE, diffs[0].kind());
		assertEquals(RangeDifference.CONFLICT, diffs[1].kind());
		assertEquals(RangeDifference.NOCHANGE, diffs[2].kind());
	}

	@Test
	public void testInsertWithinMultilineChangeConflict() {
		String a = "A" + S + "B" + S + "C" + S + "D"; //$NON-NLS-1$
		String l = "A" + S + "B" + S + "x" + S + "C" + S + "D"; //$NON-NLS-1$
		String r = "A" + S + "x" + S + "y" + S + "D"; //$NON-NLS-1$

		RangeDifference[] diffs = findRange(a, l, r);

		assertEquals(3, diffs.length);
		assertEquals(RangeDifference.NOCHANGE, diffs[0].kind());
		assertEquals(RangeDifference.CONFLICT, diffs[1].kind());
		assertEquals(RangeDifference.NOCHANGE, diffs[2].kind());
	}

	@Test
	public void testAdjoiningChangesNoConflict() {
		String a = "A" + S + "B" + S + "C" + S + "D"; //$NON-NLS-1$
		String l = "A" + S + "b1" + S + "C" + S + "D"; //$NON-NLS-1$
		String r = "A" + S + "B" + S + "c1" + S + "D"; //$NON-NLS-1$

		RangeDifference[] diffs = findRange(a, l, r);

		assertEquals(4, diffs.length);
		assertEquals(RangeDifference.NOCHANGE, diffs[0].kind());
		assertEquals(RangeDifference.LEFT, diffs[1].kind());
		assertEquals(RangeDifference.RIGHT, diffs[2].kind());
		assertEquals(RangeDifference.NOCHANGE, diffs[3].kind());
	}

	@Test
	public void testAdjoiningInsertAndChangeNoConflict() {
		String a = "A" + S + "B" + S + "C" + S + "D"; //$NON-NLS-1$
		String l = "A" + S + "B" + S + "x" + S + "C" + S + "D"; //$NON-NLS-1$
		String r = "A" + S + "B" + S + "c1" + S + "D"; //$NON-NLS-1$

		RangeDifference[] diffs = findRange(a, l, r);

		assertEquals(4, diffs.length);
		assertEquals(RangeDifference.NOCHANGE, diffs[0].kind());
		assertEquals(RangeDifference.LEFT, diffs[1].kind());
		assertEquals(RangeDifference.RIGHT, diffs[2].kind());
		assertEquals(RangeDifference.NOCHANGE, diffs[3].kind());
	}

	@Test
	public void testAdjoiningMultilineChangeNoConflict() {
		String a = "A" + S + "B" + S + "C" + S + "D"; //$NON-NLS-1$
		String l = "A" + S + "x" + S + "y" + S + "D"; //$NON-NLS-1$
		String r = "A" + S + "B" + S + "C" + S + "d1"; //$NON-NLS-1$

		RangeDifference[] diffs = findRange(a, l, r);

		assertEquals(3, diffs.length);
		assertEquals(RangeDifference.NOCHANGE, diffs[0].kind());
		assertEquals(RangeDifference.LEFT, diffs[1].kind());
		assertEquals(RangeDifference.RIGHT, diffs[2].kind());
	}

	private RangeDifference[] findRange(String a, String l, String r) {
		ITokenComparator ancestor = new DocLineComparator(new Document(a), null, false);
		ITokenComparator left = new DocLineComparator(new Document(l), null, false);
		ITokenComparator right = new DocLineComparator(new Document(r), null, false);
		return RangeDifferencer.findRanges(new NullProgressMonitor(), ancestor, left, right);
	}

}