/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.help.ui.internal.views;

import java.util.LinkedList;

public class ReusableHelpPartHistory {
	private static final int CAPACITY = 50;
	private LinkedList<HistoryEntry> queue;
	private int cursor = -1;
	private boolean blocked;

	public ReusableHelpPartHistory() {
		queue = new LinkedList<>();
	}

	public void addEntry(HistoryEntry entry) {
		if (cursor!= -1) {
			// If we are adding a new entry while
			// the cursor is not at the end, discard
			// all the entries after the cursor.
			int extra = queue.size()-1 -cursor;
			if (extra>0) {
				for (int i=extra; i>0; i--) {
					queue.removeLast();
				}
			}
		}
		queue.add(entry);
		if (queue.size()>CAPACITY)
			queue.removeFirst();
		cursor = queue.size()-1;
	}

	public boolean hasNext() {
		return cursor != -1 && cursor < queue.size()-1;
	}

	public boolean hasPrev() {
		return cursor != -1 && cursor > 0;
	}

	public HistoryEntry getNext() {
		return hasNext()?(HistoryEntry)queue.get(cursor+1):null;
	}

	public HistoryEntry getPrev() {
		return hasPrev() ? (HistoryEntry)queue.get(cursor-1):null;
	}

	public HistoryEntry next() {
		if (hasNext()) {
			return queue.get(++cursor);
		}
		return null;
	}
	public HistoryEntry prev() {
		if (hasPrev()) {
			return queue.get(--cursor);
		}
		return null;
	}
	/**
	 * @return Returns the blocked.
	 */
	public boolean isBlocked() {
		return blocked;
	}
	/**
	 * @param blocked The blocked to set.
	 */
	public void setBlocked(boolean blocked) {
		this.blocked = blocked;
	}
}
