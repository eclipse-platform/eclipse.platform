/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.core.subscribers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.team.core.synchronize.SyncInfo;

/**
 * Counts SyncInfo states and allows for easy querying for different sync states.
 */
public class SyncInfoStatistics {
	//	{int sync kind -> int number of infos with that sync kind in this sync set}
	protected Map stats = new HashMap();

	/**
	 * Count this sync kind. Only the type of the sync info is stored.
	 * @param info the new info
	 */
	public void add(SyncInfo info) {
		// update statistics
		Long count = (Long)stats.get(new Integer(info.getKind()));
		if(count == null) {
			count = new Long(0);
		}
		stats.put(new Integer(info.getKind()), new Long(count.longValue() + 1));
	}

	/**
	 * Remove this sync kind.
	 * @param info the info type to remove
	 */
	public void remove(SyncInfo info) {
		// update stats
		Integer kind = new Integer(info.getKind());
		Long count = (Long)stats.get(kind);
		if(count == null) {
			// error condition, shouldn't be removing if we haven't added yet
			// programmer error calling remove before add.
		} else {
			long newCount = count.intValue() - 1;
			if(newCount > 0) {
				stats.put(kind, new Long(newCount));
			} else {
				stats.remove(kind);
			}
		}
	}

	/**
	 * Return the count of sync infos for the specified sync kind. A mask can be used to acucmulate
	 * counts for specific directions or change types.
	 * To return the number of outgoing changes:
	 * 	long outgoingChanges = stats.countFor(SyncInfo.OUTGOING, SyncInfo.DIRECTION_MASK);
	 *
	 * @param kind the sync kind for which to return the count
	 * @param mask the mask applied to the stored sync kind
	 * @return the number of sync info types added for the specific kind
	 */
	public long countFor(int kind, int mask) {
		if(mask == 0) {
			Long count = (Long)stats.get(new Integer(kind));
			return count == null ? 0 : count.longValue();
		} else {
			Iterator it = stats.keySet().iterator();
			long count = 0;
			while (it.hasNext()) {
				Integer key = (Integer) it.next();
				if((key.intValue() & mask) == kind) {
					count += ((Long)stats.get(key)).intValue();
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
	public String toString() {
		StringBuffer out = new StringBuffer();
		Iterator it = stats.keySet().iterator();
		while (it.hasNext()) {
			Integer kind = (Integer) it.next();
			out.append(SyncInfo.kindToString(kind.intValue()) + ": " + stats.get(kind) + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return out.toString();
	}
}
