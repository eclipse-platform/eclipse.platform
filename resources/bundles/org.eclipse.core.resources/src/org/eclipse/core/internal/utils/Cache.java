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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A cache for entries that have a resource modification timestamp to identify
 * if the value is still up-to-date.
 */
public class Cache<K, V> {
	private final ConcurrentHashMap<K, EntryRef<K, V>> map = new ConcurrentHashMap<>();
	private final ReferenceQueue<Entry<V>> referenceQueue = new ReferenceQueue<>();

	private static class EntryRef<K, V> extends SoftReference<Entry<V>> {
		private final K key;

		public EntryRef(K key, Entry<V> entry, ReferenceQueue<Entry<V>> queue) {
			super(entry, queue);
			this.key = key;
		}
	}

	public static record Entry<V>(V cached, long timestamp) {
		public V getCached() {
			return cached;
		}

		public long getTimestamp() {
			return timestamp;
		}
	}

	public void addEntry(K key, V toCache) {
		addEntry(key, toCache, 0);
	}

	@SuppressWarnings("unchecked")
	private void cleanup(){
		// remove keys of Entries that have been Garbage collected:
		EntryRef<K, V> e = null;
		while ((e = (EntryRef<K, V>) referenceQueue.poll()) != null) {
            map.remove(e.key);
        }
	}
	public Entry<V> addEntry(K key, V toCache, long timestamp) {
		cleanup();
		Entry<V> e = new Entry<>(toCache, timestamp);
		map.put(key, new EntryRef<>(key, e, referenceQueue));
		return e;
	}

	public Entry<V> getEntry(K key) {
		cleanup();
		SoftReference<Entry<V>> ref = map.get(key);
		if (ref == null) {
			return null;
		}
		return ref.get();
	}

	public void clear() {
		map.clear();
	}
}
