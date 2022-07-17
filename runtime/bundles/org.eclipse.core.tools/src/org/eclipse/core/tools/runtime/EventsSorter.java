/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tools.runtime;

import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import org.eclipse.core.runtime.PerformanceStats;
import org.eclipse.core.tools.ISorter;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

public class EventsSorter extends ViewerComparator implements ISorter {
	protected boolean reversed = false;
	protected int columnNumber;

	protected int[][] SORT_ORDERS_BY_COLUMN = {
	// Event
			{EventsView.COLUMN_EVENT, EventsView.COLUMN_BLAME, EventsView.COLUMN_CONTEXT},
			// Blame
			{EventsView.COLUMN_BLAME, EventsView.COLUMN_EVENT, EventsView.COLUMN_CONTEXT},
			// Context
			{EventsView.COLUMN_CONTEXT, EventsView.COLUMN_EVENT, EventsView.COLUMN_BLAME},
			// Count
			{EventsView.COLUMN_COUNT, EventsView.COLUMN_EVENT, EventsView.COLUMN_BLAME, EventsView.COLUMN_CONTEXT},
			// Time
			{EventsView.COLUMN_TIME, EventsView.COLUMN_EVENT, EventsView.COLUMN_BLAME, EventsView.COLUMN_CONTEXT}};

	public EventsSorter(int columnNumber) {
		this.columnNumber = columnNumber;
	}

	/**
	 * Returns the number of the column by which this is sorting.
	 */
	@Override
	public int getColumnNumber() {
		return columnNumber;
	}

	/**
	 * Returns true for descending, or false for ascending sorting order.
	 */
	@Override
	public boolean isReversed() {
		return reversed;
	}

	/**
	 * Sets the sorting order.
	 */
	@Override
	public void setReversed(boolean newReversed) {
		reversed = newReversed;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void sort(final Viewer viewer, Object[] elements) {
		@SuppressWarnings("rawtypes")
		Comparator comparator = new Comparator<PerformanceStats>() {
			Collator c = Collator.getInstance();

			/**
			 * Compares two stats objects, sorting first by the main column of this sorter,
			 * then by subsequent columns, depending on the column sort order.
			 */
			@Override
			public int compare(PerformanceStats s1, PerformanceStats s2) {
				//always sort failures above non-failures
				if (s1.isFailure() && !s2.isFailure())
					return -1;
				if (s2.isFailure() && !s1.isFailure())
					return 1;
				int[] columnSortOrder = SORT_ORDERS_BY_COLUMN[columnNumber];
				int result = 0;
				for (int element : columnSortOrder) {
					result = compareColumnValue(element, s1, s2);
					if (result != 0)
						break;
				}
				if (reversed)
					result = -result;
				return result;
			}

			/**
			 * Compares two markers, based only on the value of the specified column.
			 */
			int compareColumnValue(int column, PerformanceStats s1, PerformanceStats s2) {
				switch (column) {
					case EventsView.COLUMN_EVENT :
						return c.compare(s1.getEvent(), s2.getEvent());
					case EventsView.COLUMN_BLAME :
						return c.compare(s1.getBlameString(), s2.getBlameString());
					case EventsView.COLUMN_CONTEXT :
						String name1 = s1.getContext() == null ? "" : s1.getContext(); //$NON-NLS-1$
						String name2 = s2.getContext() == null ? "" : s2.getContext(); //$NON-NLS-1$
						return c.compare(name1, name2);
					case EventsView.COLUMN_COUNT :
						return s2.getRunCount() - s1.getRunCount();
					case EventsView.COLUMN_TIME :
						return (int) (s2.getRunningTime() - s1.getRunningTime());
				}
				return 0;
			}
		};
		Arrays.sort(elements, comparator);
	}

	@Override
	public int states() {
		return 2;
	}
}
