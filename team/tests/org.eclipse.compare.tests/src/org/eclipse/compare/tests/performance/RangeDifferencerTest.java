/*******************************************************************************
 * Copyright (c) 2005, 2026 IBM Corporation and others.
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
 *     Eclipse contributors - migrated off org.eclipse.test.performance
 *******************************************************************************/
package org.eclipse.compare.tests.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.compare.contentmergeviewer.ITokenComparator;
import org.eclipse.compare.internal.DocLineComparator;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.rangedifferencer.RangeDifferencer;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;

/**
 * Timing measurement for a three way {@link RangeDifferencer} run on 5000 line
 * documents. Correctness of the produced difference pattern is asserted on every
 * repetition; the median wall time of the non-warmup repetitions is printed as a
 * greppable {@code PERF ...} line. Only a generous sanity bound is asserted so the
 * test stays useful on shared CI machines.
 */
public class RangeDifferencerTest {

	private static final int REPETITIONS = 20;
	private static final int WARMUP = 5;
	private static final long SANITY_BOUND_MILLIS = 10_000;

	private static final List<Long> durations = new ArrayList<>();

	/*
	 * Creates a document with 5000 lines. Parameter code determines where
	 * additional lines are added.
	 */
	private IDocument createDocument(int code) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 5000; i++) {
			sb.append("line "); //$NON-NLS-1$
			sb.append(Integer.toString(i));
			sb.append('\n');

			int mod = i % 10;
			switch (code) {
			case 1:
				if (mod == 1) {
					sb.append("outgoing\n"); //$NON-NLS-1$
				}
				if (mod == 4) {
					sb.append("conflict1\n"); //$NON-NLS-1$
				}
				break;
			case 2:
				if (mod == 7) {
					sb.append("incoming\n"); //$NON-NLS-1$
				}
				if (mod == 4) {
					sb.append("conflict2\n"); //$NON-NLS-1$
				}
				break;
			}
		}
		return new Document(sb.toString());
	}

	@RepeatedTest(REPETITIONS)
	public void testLargeDocument(RepetitionInfo info) {
		ITokenComparator ancestor = new DocLineComparator(createDocument(0), null, false);
		ITokenComparator left = new DocLineComparator(createDocument(1), null, false);
		ITokenComparator right = new DocLineComparator(createDocument(2), null, false);

		long start = System.nanoTime();
		RangeDifference[] diffs = RangeDifferencer.findRanges(new NullProgressMonitor(), ancestor, left, right);
		long durationMillis = (System.nanoTime() - start) / 1_000_000;

		// Assert the result is correct on every repetition.
		for (int i = 0; i < diffs.length - 6; i += 6) {
			assertEquals(RangeDifference.NOCHANGE, diffs[i + 0].kind());
			assertEquals(RangeDifference.LEFT, diffs[i + 1].kind());
			assertEquals(RangeDifference.NOCHANGE, diffs[i + 2].kind());
			assertEquals(RangeDifference.CONFLICT, diffs[i + 3].kind());
			assertEquals(RangeDifference.NOCHANGE, diffs[i + 4].kind());
			assertEquals(RangeDifference.RIGHT, diffs[i + 5].kind());
		}

		if (info.getCurrentRepetition() > WARMUP) {
			durations.add(durationMillis);
		}
		assertTrue(durationMillis < SANITY_BOUND_MILLIS,
				"findRanges exceeded the sanity bound: " + durationMillis + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@AfterAll
	public static void reportResults() {
		if (durations.isEmpty()) {
			return;
		}
		List<Long> sorted = new ArrayList<>(durations);
		Collections.sort(sorted);
		int n = sorted.size();
		long median = n % 2 == 1 ? sorted.get(n / 2) : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2;
		System.out.println("PERF range-differencer lines=5000 threeWay findRanges=" + median //$NON-NLS-1$
				+ "ms (median, n=" + n + ")"); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
