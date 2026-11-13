/*******************************************************************************
 * Copyright (c) 2026 and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.ui.internal.console;

import java.util.List;

import org.eclipse.swt.custom.StyleRange;

public final class StyleRangeUtil {

	private StyleRangeUtil() {
	}

	/**
	 * Apply new style range to list of existing style ranges. If the new style
	 * range overlaps with any of the existing style ranges the new style overrides
	 * the existing one in the affected range by splitting/adjusting the existing
	 * ones.
	 *
	 * @param ranges   list of existing style ranges (will contain the new style
	 *                 range when finished)
	 * @param newRange new style range which should be combined with the existing
	 *                 ranges
	 */
	public static void overrideStyleRange(List<StyleRange> ranges, StyleRange newRange) {
		final int overrideStart = newRange.start;
		final int overrideEnd = overrideStart + newRange.length;
		int insertIndex = ranges.size();
		for (int i = ranges.size() - 1; i >= 0; i--) {
			final StyleRange existingRange = ranges.get(i);
			final int existingStart = existingRange.start;
			final int existingEnd = existingStart + existingRange.length;

			// Find first position to insert where offset of new range is smaller then all
			// offsets before. This way the list is still sorted by offset after insert if
			// it was sorted before and it will not fail if list was not sorted.
			if (overrideStart <= existingStart) {
				insertIndex = i;
			}

			// adjust the existing range if required
			if (overrideStart <= existingStart) { // new range starts before or with existing
				if (overrideEnd < existingStart) {
					// new range lies before existing range. No overlapping.
					// new range: ++++_________
					// existing : ________=====
					// . result : ++++____=====
					// nothing to do
				} else if (overrideEnd < existingEnd) {
					// new range overlaps start of existing.
					// new range: ++++++++_____
					// existing : _____========
					// . result : ++++++++=====
					final int overlap = overrideEnd - existingStart;
					existingRange.start += overlap;
					existingRange.length -= overlap;
				} else {
					// new range completely overlaps existing.
					// new range: ___++++++++++
					// existing : ___======____
					// . result : ___++++++++++
					ranges.remove(i);
				}
			} else { // new range starts inside or after existing
				if (existingEnd < overrideStart) {
					// new range lies after existing range. No overlapping.
					// new range: _________++++
					// existing : =====________
					// . result : =====____++++
					// nothing to do
				} else if (overrideEnd >= existingEnd) {
					// new range overlaps end of existing.
					// new range: _____++++++++
					// existing : ========_____
					// . result : =====++++++++
					existingRange.length -= existingEnd - overrideStart;
				} else {
					// new range lies inside existing range but not overrides all of it
					// (and does not touch first or last offset of existing)
					// new range: ____+++++____
					// existing : =============
					// . result : ====+++++====
					final StyleRange clonedRange = (StyleRange) existingRange.clone();
					existingRange.length = overrideStart - existingStart;
					clonedRange.start = overrideEnd;
					clonedRange.length = existingEnd - overrideEnd;
					ranges.add(i + 1, clonedRange);
				}
			}
		}
		ranges.add(insertIndex, newRange);
	}
}
