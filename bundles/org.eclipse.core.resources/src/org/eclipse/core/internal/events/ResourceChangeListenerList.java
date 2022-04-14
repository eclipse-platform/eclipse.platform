/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.core.internal.events;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import org.eclipse.core.resources.IResourceChangeListener;

/**
 * This class is used to maintain a list of listeners. It is a fairly lightweight object,
 * occupying minimal space when no listeners are registered.
 * <p>
 * Note that the <code>add</code> method checks for and eliminates
 * duplicates based on identity (not equality).  Likewise, the
 * <code>remove</code> method compares based on identity.
 * </p>
 * <p>
 * This implementation is thread safe.  The listener list is copied every time
 * it is modified, so readers do not need to copy or synchronize. This optimizes
 * for frequent reads and infrequent writes, and assumes that readers can
 * be trusted not to modify the returned array.
 */
public class ResourceChangeListenerList {

	static final class ListenerEntry {
		final int eventMask;
		final IResourceChangeListener listener;

		ListenerEntry(IResourceChangeListener listener, int eventMask) {
			this.listener = listener;
			this.eventMask = eventMask;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("Listener [eventMask="); //$NON-NLS-1$
			sb.append(eventMask);
			sb.append(", "); //$NON-NLS-1$
			sb.append(listener);
			sb.append("]"); //$NON-NLS-1$
			return sb.toString();
		}
	}

	private volatile int count1 = 0;
	private volatile int count2 = 0;
	private volatile int count4 = 0;
	private volatile int count8 = 0;
	private volatile int count16 = 0;
	private volatile int count32 = 0;

	/**
	 * The list of listeners.
	 */
	private final CopyOnWriteArrayList<ListenerEntry> listeners = new CopyOnWriteArrayList<>();

	/**
	 * Adds the given listener to this list. If an identical listener is already
	 * registered the mask is updated.
	 *
	 * @param listener the listener
	 * @param mask     event types
	 */
	public synchronized void add(IResourceChangeListener listener, int mask) {
		Objects.requireNonNull(listener);
		if (mask == 0) {
			remove(listener);
			return;
		}
		ResourceChangeListenerList.ListenerEntry entry = new ResourceChangeListenerList.ListenerEntry(listener, mask);
		final int oldSize = listeners.size();
		// check for duplicates using identity
		for (int i = 0; i < oldSize; ++i) {
			ListenerEntry oldEntry = listeners.get(i);
			if (oldEntry.listener == listener) {
				removing(oldEntry.eventMask);
				adding(mask);
				listeners.set(i, entry);
				return;
			}
		}
		adding(mask);
		listeners.add(entry);
	}

	private void adding(int mask) {
		if ((mask & 1) != 0)
			count1++;
		if ((mask & 2) != 0)
			count2++;
		if ((mask & 4) != 0)
			count4++;
		if ((mask & 8) != 0)
			count8++;
		if ((mask & 16) != 0)
			count16++;
		if ((mask & 32) != 0)
			count32++;
	}

	/**
	 * Returns a copy of the registered listeners.
	 * @return the list of registered listeners that must not be modified
	 */
	public ListenerEntry[] getListeners() {
		return listeners.toArray(ListenerEntry[]::new);
	}

	public boolean hasListenerFor(int event) {
		switch (event) {
		case 1:
			return count1 > 0;
		case 2:
			return count2 > 0;
		case 4:
			return count4 > 0;
		case 8:
			return count8 > 0;
		case 16:
			return count16 > 0;
		case 32:
			return count32 > 0;
		default:
			return false;
		}
	}

	/**
	 * Removes the given listener from this list. Has no effect if an identical
	 * listener was not already registered.
	 *
	 * @param listener the listener to remove
	 */
	public synchronized void remove(IResourceChangeListener listener) {
		Objects.requireNonNull(listener);
		final int oldSize = listeners.size();
		for (int i = 0; i < oldSize; ++i) {
			ListenerEntry oldEntry = listeners.get(i);
			if (oldEntry.listener == listener) {
				removing(oldEntry.eventMask);
				listeners.remove(i);
				return;
			}
		}
	}

	public synchronized void clear() {
		listeners.clear();
		count1 = 0;
		count2 = 0;
		count4 = 0;
		count8 = 0;
		count16 = 0;
		count32 = 0;
	}

	private void removing(int mask) {
		if ((mask & 1) != 0)
			count1--;
		if ((mask & 2) != 0)
			count2--;
		if ((mask & 4) != 0)
			count4--;
		if ((mask & 8) != 0)
			count8--;
		if ((mask & 16) != 0)
			count16--;
		if ((mask & 32) != 0)
			count32--;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ResourceChangeListenerList ["); //$NON-NLS-1$
		if (listeners != null) {
			builder.append("listeners="); //$NON-NLS-1$
			builder.append(listeners.toString());
		}
		builder.append("]"); //$NON-NLS-1$
		return builder.toString();
	}
}
