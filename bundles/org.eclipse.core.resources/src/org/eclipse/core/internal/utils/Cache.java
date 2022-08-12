/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
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
package org.eclipse.core.internal.utils;

import org.eclipse.core.runtime.Assert;

/**
 * A cache that keeps a collection of at most maximumCapacity+threshold entries.
 * When the number of entries exceeds that limit, least recently used entries are removed
 * so the current size is the same as the maximum capacity.
 */
public class Cache {
	public class Entry implements KeyedHashSet.KeyedElement {
		Object cached;
		Object key;
		Entry next;
		Entry previous;
		long timestamp;

		public Entry(Object key, Object cached, long timestamp) {
			this.key = key;
			this.cached = cached;
			this.timestamp = timestamp;
		}

		@Override
		public boolean compare(KeyedHashSet.KeyedElement other) {
			if (!(other instanceof Entry))
				return false;
			Entry otherEntry = (Entry) other;
			return key.equals(otherEntry.key);
		}

		/* Removes this entry from the cache */
		public void discard() {
			unchain();
			cached = null;
			entries.remove(this);
		}

		public Object getCached() {
			return cached;
		}

		@Override
		public Object getKey() {
			return key;
		}

		@Override
		public int getKeyHashCode() {
			return key.hashCode();
		}

		public Entry getNext() {
			return next;
		}

		public Entry getPrevious() {
			return previous;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public boolean isHead() {
			return previous == null;
		}

		public boolean isTail() {
			return next == null;
		}

		/* Inserts into the head of the list  */
		void makeHead() {
			Entry oldHead = head;
			head = this;
			next = oldHead;
			previous = null;
			if (oldHead == null)
				tail = this;
			else
				oldHead.previous = this;
		}

		public void setCached(Object cached) {
			this.cached = cached;
		}

		public void setTimestamp(long timestamp) {
			this.timestamp = timestamp;
		}

		@Override
		public String toString() {
			return key + " -> " + cached + " [" + timestamp + ']'; //$NON-NLS-1$ //$NON-NLS-2$
		}

		/* Removes from the linked list, but not from the entries collection */
		void unchain() {
			// it may be in the tail
			if (tail == this)
				tail = previous;
			else
				next.previous = previous;
			// it may be in the head
			if (head == this)
				head = next;
			else
				previous.next = next;
		}
	}

	KeyedHashSet entries;
	Entry head;
	private int maximumCapacity;
	Entry tail;
	private double threshold;

	public Cache(int maximumCapacity) {
		this(Math.min(KeyedHashSet.MINIMUM_SIZE, maximumCapacity), maximumCapacity, 0.25);
	}

	public Cache(int initialCapacity, int maximumCapacity, double threshold) {
		Assert.isTrue(maximumCapacity >= initialCapacity, "maximum capacity < initial capacity"); //$NON-NLS-1$
		Assert.isTrue(threshold >= 0 && threshold <= 1, "threshold should be between 0 and 1"); //$NON-NLS-1$
		Assert.isTrue(initialCapacity > 0, "initial capacity must be greater than zero"); //$NON-NLS-1$
		entries = new KeyedHashSet(initialCapacity);
		this.maximumCapacity = maximumCapacity;
		this.threshold = threshold;
	}

	public void addEntry(Object key, Object toCache) {
		addEntry(key, toCache, 0);
	}

	public Entry addEntry(Object key, Object toCache, long timestamp) {
		Entry newHead = (Entry) entries.getByKey(key);
		if (newHead == null)
			entries.add(newHead = new Entry(key, toCache, timestamp));
		newHead.cached = toCache;
		newHead.timestamp = timestamp;
		newHead.makeHead();
		int extraEntries = entries.size() - maximumCapacity;
		if (extraEntries > maximumCapacity * threshold)
			// we have reached our limit - ensure we are under the maximum capacity
			// by discarding older entries
			packEntries(extraEntries);
		return newHead;
	}

	public Entry getEntry(Object key) {
		return getEntry(key, true);
	}

	public Entry getEntry(Object key, boolean update) {
		Entry existing = (Entry) entries.getByKey(key);
		if (existing == null)
			return null;
		if (!update)
			return existing;
		existing.unchain();
		existing.makeHead();
		return existing;
	}

	public Entry getHead() {
		return head;
	}

	public Entry getTail() {
		return tail;
	}

	private void packEntries(int extraEntries) {
		// should remove in an ad-hoc way to get better performance
		Entry current = tail;
		for (; current != null && extraEntries > 0; extraEntries--) {
			current.discard();
			current = current.previous;
		}
	}

	public long size() {
		return entries.size();
	}

	public void discardAll() {
		entries.clear();
		head = tail = null;
	}

	public void dispose() {
		discardAll();
		entries = null;
		head = tail = null;
	}
}
