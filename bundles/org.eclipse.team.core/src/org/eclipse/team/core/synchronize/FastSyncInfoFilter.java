/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.core.synchronize;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A specialized <code>SyncInfoFilter</code> that does not require a progress monitor.
 * This enables these filters to be used when determining menu enablement or other
 * operations that must be short running.
 *
 * @see SyncInfo
 * @see SyncInfoSet
 * @see SyncInfoFilter
 * @since 3.0
 */
public class FastSyncInfoFilter extends SyncInfoFilter {

	/**
	 * Selects <code>SyncInfo</code> that match the given change type and direction.
	 *
	 * @param direction the change direction (<code>SyncInfo.OUTGOING</code>,
	 * <code>SyncInfo.INCOMING</code> and <code>SyncInfo.CONFLICTING</code>) that this filter matches
	 * @param change the change type (<code>SyncInfo.ADDITION</code>,
	 * <code>SyncInfo.DELETION</code> and <code>SyncInfo.CHANGE</code>) that this filter matches
	 * @return a <code>FastSyncInfoFilter</code> that selects <code>SyncInfo</code> that match the given
	 * change type and direction.
	 */
	public static FastSyncInfoFilter getDirectionAndChangeFilter(int direction, int change) {
		return new AndSyncInfoFilter(new FastSyncInfoFilter[]{new SyncInfoDirectionFilter(direction), new SyncInfoChangeTypeFilter(change)});
	}

	/**
	 * An abstract class which contains a set of <code>FastSyncInfoFilter</code> instances.
	 * Subclasses must provide the <code>select(SyncInfo)</code> method for determining
	 * matches.
	 */
	public static abstract class CompoundSyncInfoFilter extends FastSyncInfoFilter {
		/**
		 * Instance variable which contains all the child filters for this compound filter.
		 */
		protected FastSyncInfoFilter[] filters;
		/**
		 * Create a compound filter that contains the provided filters.
		 * @param filters the child filters
		 */
		protected CompoundSyncInfoFilter(FastSyncInfoFilter[] filters) {
			this.filters = filters;
		}
	}

	/**
	 * Selects <code>SyncInfo</code> which match all child filters.
	 */
	public static class AndSyncInfoFilter extends CompoundSyncInfoFilter {
		/**
		 * Create an AND filter from the given filters
		 * @param filters the filters to be ANDed
		 */
		public AndSyncInfoFilter(FastSyncInfoFilter[] filters) {
			super(filters);
		}
		@Override
		public boolean select(SyncInfo info) {
			for (FastSyncInfoFilter filter : filters) {
				if (!filter.select(info)) {
					return false;
				}
			}
			return true;
		}
	}

	/**
	 * Selects <code>SyncInfo</code> instances that are auto-mergable.
	 */
	public static class AutomergableFilter extends FastSyncInfoFilter {
		@Override
		public boolean select(SyncInfo info) {
			return (info.getKind() & SyncInfo.AUTOMERGE_CONFLICT) != 0;
		}
	}

	/**
	 * Selects <code>SyncInfo</code> instances that are pseudo-conflicts.
	 */
	public static class PseudoConflictFilter extends FastSyncInfoFilter {
		@Override
		public boolean select(SyncInfo info) {
			return info.getKind() != 0 && (info.getKind() & SyncInfo.PSEUDO_CONFLICT) == 0;
		}
	}

	/**
	 * Selects <code>SyncInfo</code> that match any of the child filters.
	 */
	public static class OrSyncInfoFilter extends CompoundSyncInfoFilter {
		/**
		 * Create an OR filter from the given filters
		 * @param filters the filters to be ORed
		 */
		public OrSyncInfoFilter(FastSyncInfoFilter[] filters) {
			super(filters);
		}
		@Override
		public boolean select(SyncInfo info) {
			for (FastSyncInfoFilter filter : filters) {
				if (filter.select(info)) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * Selects <code>SyncInfo</code> whose change type match those of the filter.
	 */
	public static class SyncInfoChangeTypeFilter extends FastSyncInfoFilter {
		private int[] changeFilters = new int[]{SyncInfo.ADDITION, SyncInfo.DELETION, SyncInfo.CHANGE};
		/**
		 * Create a filter that will match <code>SyncInfo</code> whose change type
		 * match those passed as arguments to this constructor.
		 * @param changeFilters the array of change types (<code>SyncInfo.ADDITION</code>,
		 * <code>SyncInfo.DELETION</code> and <code>SyncInfo.CHANGE</code>) that this filter match
		 */
		public SyncInfoChangeTypeFilter(int[] changeFilters) {
			this.changeFilters = changeFilters;
		}
		/**
		 * Create a filter that will match <code>SyncInfo</code> whose change type
		 * match that passed as an argument to this constructor.
		 * @param change the change type (<code>SyncInfo.ADDITION</code>,
		 * <code>SyncInfo.DELETION</code> and <code>SyncInfo.CHANGE</code>) that this filter matches
		 */
		public SyncInfoChangeTypeFilter(int change) {
			this(new int[]{change});
		}
		@Override
		public boolean select(SyncInfo info) {
			int syncKind = info.getKind();
			for (int filter : changeFilters) {
				if ((syncKind & SyncInfo.CHANGE_MASK) == filter)
					return true;
			}
			return false;
		}
	}

	/**
	 * Selects <code>SyncInfo</code> whose change direction match those of the filter.
	 */
	public static class SyncInfoDirectionFilter extends FastSyncInfoFilter {
		int[] directionFilters = new int[] {SyncInfo.OUTGOING, SyncInfo.INCOMING, SyncInfo.CONFLICTING};
		/**
		 * Create a filter that will match <code>SyncInfo</code> whose change direction
		 * match those passed as arguments to this constructor.
		 * @param directionFilters the array of change directions (<code>SyncInfo.OUTGOING</code>,
		 * <code>SyncInfo.INCOMING</code> and <code>SyncInfo.CONFLICTING</code>) that this filter match
		 */
		public SyncInfoDirectionFilter(int[] directionFilters) {
			this.directionFilters = directionFilters;
		}
		/**
		 * Create a filter that will match <code>SyncInfo</code> whose change direction
		 * match that passed as arguments to this constructor.
		 * @param direction the change direction (<code>SyncInfo.OUTGOING</code>,
		 * <code>SyncInfo.INCOMING</code> and <code>SyncInfo.CONFLICTING</code>) that this filter matches
		 */
		public SyncInfoDirectionFilter(int direction) {
			this(new int[] { direction });
		}
		@Override
		public boolean select(SyncInfo info) {
			int syncKind = info.getKind();
			for (int filter : directionFilters) {
				if ((syncKind & SyncInfo.DIRECTION_MASK) == filter)
					return true;
			}
			return false;
		}
	}

	/**
	 * Return whether the provided <code>SyncInfo</code> matches the filter. The default
	 * behavior it to include resources whose syncKind is non-zero.
	 *
	 * @param info the <code>SyncInfo</code> being tested
	 * @return <code>true</code> if the <code>SyncInfo</code> matches the filter
	 */
	public boolean select(SyncInfo info) {
		return info.getKind() != 0;
	}

	@Override
	public final boolean select(SyncInfo info, IProgressMonitor monitor) {
		return select(info);
	}
}
