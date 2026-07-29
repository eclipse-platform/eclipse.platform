/*******************************************************************************
 * Copyright (c) 2026 Lars Vogel and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eclipse contributors - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.tests;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.eclipse.compare.internal.DocLineComparator;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.rangedifferencer.IRangeComparator;
import org.eclipse.compare.rangedifferencer.RangeDifferencer;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.Document;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the two-way {@code findDifferences} honors the progress monitor
 * it is given, so that a long running comparison can be canceled.
 */
public class RangeDifferencerMonitorTest {

	private static final String S = System.lineSeparator();

	@Test
	public void testFindDifferencesIsCanceledByMonitor() {
		IRangeComparator left = comparator("left"); //$NON-NLS-1$
		IRangeComparator right = comparator("right"); //$NON-NLS-1$
		NullProgressMonitor canceled = new NullProgressMonitor();
		canceled.setCanceled(true);

		try {
			RangeDifferencer.findDifferences(canceled, left, right);
			fail("a canceled monitor must abort the comparison"); //$NON-NLS-1$
		} catch (OperationCanceledException expected) {
			// the monitor is honored
		}
	}

	@Test
	public void testFindDifferencesCompletesWithoutCancellation() {
		IRangeComparator left = comparator("left"); //$NON-NLS-1$
		IRangeComparator right = comparator("right"); //$NON-NLS-1$

		RangeDifference[] diffs = RangeDifferencer.findDifferences(new NullProgressMonitor(), left, right);

		assertNotNull(diffs, "an uncanceled comparison must produce differences"); //$NON-NLS-1$
	}

	private static IRangeComparator comparator(String prefix) {
		StringBuilder content = new StringBuilder();
		for (int i = 0; i < 200; i++) {
			content.append(prefix).append(' ').append(i).append(S);
		}
		return new DocLineComparator(new Document(content.toString()), null, false);
	}
}
