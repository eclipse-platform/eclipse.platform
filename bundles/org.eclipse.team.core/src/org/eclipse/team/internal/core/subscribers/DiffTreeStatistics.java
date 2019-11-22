/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.core.subscribers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.synchronize.SyncInfo;

public class DiffTreeStatistics {
	/**
	 * {Integer sync kind -&gt; Long number of infos with that sync kind in this sync set}
	 */
	protected Map<Integer, Long> stats = Collections.synchronizedMap(new HashMap<>());

	/**
	 * Count this sync state.
	 * @param state the state
	 */
	public void add(int state) {
		// update statistics
		Long count = stats.get(Integer.valueOf(state));
		if(count == null) {
			count = Long.valueOf(0);
		}
		stats.put(Integer.valueOf(state), Long.valueOf(count.longValue() + 1));
	}

	/**
	 * Remove this sync kind.
	 * @param state the info type to remove
	 */
	public void remove(int state) {
		// update stats
		Integer kind = Integer.valueOf(state);
		Long count = stats.get(kind);
		if(count == null) {
			// error condition, shouldn't be removing if we haven't added yet
			// programmer error calling remove before add.
		} else {
			long newCount = count.intValue() - 1;
			if(newCount > 0) {
				stats.put(kind, Long.valueOf(newCount));
			} else {
				stats.remove(kind);
			}
		}
	}

	/**
	 * Return the count of sync infos for the specified sync kind. A mask can be used to accumulate
	 * counts for specific directions or change types.
	 * To return the number of outgoing changes:
	 * 	long outgoingChanges = stats.countFor(SyncInfo.OUTGOING, SyncInfo.DIRECTION_MASK);
	 *
	 * @param state the sync kind for which to return the count
	 * @param mask the mask applied to the stored sync kind
	 * @return the number of sync info types added for the specific kind
	 */
	public long countFor(int state, int mask) {
		if(mask == 0) {
			Long count = stats.get(Integer.valueOf(state));
			return count == null ? 0 : count.longValue();
		} else {
			Set keySet = stats.keySet();
			long count = 0;
			synchronized (stats) {
				Iterator it = keySet.iterator();
				while (it.hasNext()) {
					Integer key = (Integer) it.next();
					if((key.intValue() & mask) == state) {
						count += stats.get(key).intValue();
					}
				}
			}
			return count;
		}
	}

	/**
	 * Clear the statistics counts. All calls to countFor() will return 0 until new
	 * sync infos are added.
	 */
	public void clear() {
		stats.clear();
	}

	/**
	 * For debugging
	 */
	@Override
	public String toString() {
		StringBuilder out = new StringBuilder();
		for (Integer kind : stats.keySet()) {
			out.append(SyncInfo.kindToString(kind.intValue()) + ": " + stats.get(kind) + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return out.toString();
	}

	public void add(IDiff delta) {
		int state = getState(delta);
		add(state);
	}

	public void remove(IDiff delta) {
		int state = getState(delta);
		remove(state);
	}

	private int getState(IDiff delta) {
		int state = delta.getKind();
		if (delta instanceof IThreeWayDiff) {
			IThreeWayDiff twd = (IThreeWayDiff) delta;
			state |= twd.getDirection();
		}
		return state;
	}
}
