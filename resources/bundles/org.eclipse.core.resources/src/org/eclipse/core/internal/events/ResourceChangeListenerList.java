/*******************************************************************************
 * Copyright (c) 2000, 2026 IBM Corporation and others.
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
import java.util.concurrent.atomic.AtomicIntegerArray;
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
			return "Listener [eventMask=" + eventMask + ", " + listener + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	/**
	 * Per-event-bit listener counts, indexed by
	 * {@code Integer.numberOfTrailingZeros(eventMask)}. An
	 * {@link AtomicIntegerArray} preserves the per-element volatile read
	 * semantics that {@link #hasListenerFor(int)} relies on outside the
	 * synchronized {@link #add}/{@link #remove}/{@link #clear} paths.
	 */
	private final AtomicIntegerArray bitCounts = new AtomicIntegerArray(Integer.SIZE);

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
		ListenerEntry entry = new ListenerEntry(listener, mask);
		final int oldSize = listeners.size();
		// check for duplicates using identity
		for (int i = 0; i < oldSize; ++i) {
			ListenerEntry oldEntry = listeners.get(i);
			if (oldEntry.listener == listener) {
				adjust(oldEntry.eventMask, -1);
				adjust(mask, +1);
				listeners.set(i, entry);
				return;
			}
		}
		adjust(mask, +1);
		listeners.add(entry);
	}

	/**
	 * Returns a copy of the registered listeners.
	 * @return the list of registered listeners that must not be modified
	 */
	public ListenerEntry[] getListeners() {
		return listeners.toArray(ListenerEntry[]::new);
	}

	public boolean hasListenerFor(int event) {
		// event is expected to be a single bit (a power of two)
		if (event <= 0 || Integer.bitCount(event) != 1) {
			return false;
		}
		return bitCounts.get(Integer.numberOfTrailingZeros(event)) > 0;
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
				adjust(oldEntry.eventMask, -1);
				listeners.remove(i);
				return;
			}
		}
	}

	public synchronized void clear() {
		listeners.clear();
		for (int i = 0; i < bitCounts.length(); i++) {
			bitCounts.set(i, 0);
		}
	}

	private void adjust(int mask, int delta) {
		int remaining = mask;
		while (remaining != 0) {
			int bit = Integer.numberOfTrailingZeros(remaining);
			bitCounts.addAndGet(bit, delta);
			remaining &= remaining - 1;
		}
	}

	@Override
	public String toString() {
		return "ResourceChangeListenerList [listeners=" + listeners + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
