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
package org.eclipse.core.internal.expressions.util;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Copied from JDT/Core to get a cache which is independent from
 * JDK 1.4.
 */
public class LRUCache implements Cloneable {

	/**
	 * This type is used internally by the LRUCache to represent entries
	 * stored in the cache.
	 * It is static because it does not require a pointer to the cache
	 * which contains it.
	 *
	 * @see LRUCache
	 */
	protected static class LRUCacheEntry {

		/**
		 * Hash table key
		 */
		public Object _fKey;

		/**
		 * Hash table value (an LRUCacheEntry object)
		 */
		public Object _fValue;

		/**
		 * Time value for queue sorting
		 */
		public int _fTimestamp;

		/**
		 * Cache footprint of this entry
		 */
		public int _fSpace;

		/**
		 * Previous entry in queue
		 */
		public LRUCacheEntry _fPrevious;

		/**
		 * Next entry in queue
		 */
		public LRUCacheEntry _fNext;

		/**
		 * Creates a new instance of the receiver with the provided values
		 * for key, value, and space.
		 * @param key key
		 * @param value value
		 * @param space space
		 */
		public LRUCacheEntry (Object key, Object value, int space) {
			_fKey = key;
			_fValue = value;
			_fSpace = space;
		}

		/**
		 * Returns a String that represents the value of this object.
		 * @return a string
		 */
		@Override
		public String toString() {

			return "LRUCacheEntry [" + _fKey + "-->" + _fValue + "]"; //$NON-NLS-3$ //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * Amount of cache space used so far
	 */
	protected int fCurrentSpace;

	/**
	 * Maximum space allowed in cache
	 */
	protected int fSpaceLimit;

	/**
	 * Counter for handing out sequential timestamps
	 */
	protected int	fTimestampCounter;

	/**
	 * Hash table for fast random access to cache entries
	 */
	protected Hashtable<Object, LRUCacheEntry> fEntryTable;

	/**
	 * Start of queue (most recently used entry)
	 */
	protected LRUCacheEntry fEntryQueue;

	/**
	 * End of queue (least recently used entry)
	 */
	protected LRUCacheEntry fEntryQueueTail;

	/**
	 * Default amount of space in the cache
	 */
	protected static final int DEFAULT_SPACELIMIT = 100;
	/**
	 * Creates a new cache.  Size of cache is defined by
	 * <code>DEFAULT_SPACELIMIT</code>.
	 */
	public LRUCache() {

		this(DEFAULT_SPACELIMIT);
	}
	/**
	 * Creates a new cache.
	 * @param size Size of Cache
	 */
	public LRUCache(int size) {

		fTimestampCounter = fCurrentSpace = 0;
		fEntryQueue = fEntryQueueTail = null;
		fEntryTable = new Hashtable<>(size);
		fSpaceLimit = size;
	}
	/**
	 * Returns a new cache containing the same contents.
	 *
	 * @return New copy of object.
	 */
	@Override
	public Object clone() {

		LRUCache newCache = newInstance(fSpaceLimit);
		LRUCacheEntry qEntry;

		/* Preserve order of entries by copying from oldest to newest */
		qEntry = this.fEntryQueueTail;
		while (qEntry != null) {
			newCache.privateAdd (qEntry._fKey, qEntry._fValue, qEntry._fSpace);
			qEntry = qEntry._fPrevious;
		}
		return newCache;
	}
	public double fillingRatio() {
		return (fCurrentSpace) * 100.0 / fSpaceLimit;
	}
	/**
	 * Flushes all entries from the cache.
	 */
	public void flush() {

		fCurrentSpace = 0;
		LRUCacheEntry entry = fEntryQueueTail; // Remember last entry
		fEntryTable = new Hashtable<>();  // Clear it out
		fEntryQueue = fEntryQueueTail = null;
		while (entry != null) {  // send deletion notifications in LRU order
			privateNotifyDeletionFromCache(entry);
			entry = entry._fPrevious;
		}
	}
	/**
	 * Flushes the given entry from the cache.  Does nothing if entry does not
	 * exist in cache.
	 *
	 * @param key Key of object to flush
	 */
	public void flush (Object key) {

		LRUCacheEntry entry;

		entry = fEntryTable.get(key);

		/* If entry does not exist, return */
		if (entry == null) return;

		this.privateRemoveEntry (entry, false);
	}
	/**
	 * Answers the value in the cache at the given key.
	 * If the value is not in the cache, returns null
	 *
	 * @param key Hash table key of object to retrieve
	 * @return Retrieved object, or null if object does not exist
	 */
	public Object get(Object key) {

		LRUCacheEntry entry = fEntryTable.get(key);
		if (entry == null) {
			return null;
		}

		this.updateTimestamp (entry);
		return entry._fValue;
	}
	/**
	 * Returns the amount of space that is current used in the cache.
	 * @return an int
	 */
	public int getCurrentSpace() {
		return fCurrentSpace;
	}
	/**
	 * Returns the maximum amount of space available in the cache.
	 * @return an int
	 */
	public int getSpaceLimit() {
		return fSpaceLimit;
	}
	/**
	 * Returns an Enumeration of the keys currently in the cache.
	 * @return an enumeration
	 */
	public Enumeration<Object> keys() {
		return fEntryTable.keys();
	}
	/**
	 * Ensures there is the specified amount of free space in the receiver,
	 * by removing old entries if necessary.  Returns true if the requested space was
	 * made available, false otherwise.
	 *
	 * @param space Amount of space to free up
	 * @return a boolean
	 */
	protected boolean makeSpace (int space) {

		int limit;

		limit = this.getSpaceLimit();

		/* if space is already available */
		if (fCurrentSpace + space <= limit) {
			return true;
		}

		/* if entry is too big for cache */
		if (space > limit) {
			return false;
		}

		/* Free up space by removing oldest entries */
		while (fCurrentSpace + space > limit && fEntryQueueTail != null) {
			this.privateRemoveEntry (fEntryQueueTail, false);
		}
		return true;
	}
	/**
	 * Returns a new LRUCache instance
	 * @param size the size
	 * @return a cache
	 */
	protected LRUCache newInstance(int size) {
		return new LRUCache(size);
	}
	/**
	 * Answers the value in the cache at the given key.
	 * If the value is not in the cache, returns null
	 *
	 * This function does not modify timestamps.
	 * @param key the key
	 * @return the object
	 */
	public Object peek(Object key) {

		LRUCacheEntry entry = fEntryTable.get(key);
		if (entry == null) {
			return null;
		}
		return entry._fValue;
	}
	/**
	 * Adds an entry for the given key/value/space.
	 * @param key key
	 * @param value value
	 * @param space space
	 */
	protected void privateAdd (Object key, Object value, int space) {

		LRUCacheEntry entry;

		entry = new LRUCacheEntry(key, value, space);
		this.privateAddEntry (entry, false);
	}
	/**
	 * Adds the given entry from the receiver.
	 * @param entry the entry to add
	 * @param shuffle Indicates whether we are just shuffling the queue
	 * (in which case, the entry table is not modified).
	 */
	protected void privateAddEntry (LRUCacheEntry entry, boolean shuffle) {

		if (!shuffle) {
			fEntryTable.put (entry._fKey, entry);
			fCurrentSpace += entry._fSpace;
		}

		entry._fTimestamp = fTimestampCounter++;
		entry._fNext = this.fEntryQueue;
		entry._fPrevious = null;

		if (fEntryQueue == null) {
			/* this is the first and last entry */
			fEntryQueueTail = entry;
		} else {
			fEntryQueue._fPrevious = entry;
		}

		fEntryQueue = entry;
	}
	/**
	 * An entry has been removed from the cache, for example because it has
	 * fallen off the bottom of the LRU queue.
	 * Subclasses could over-ride this to implement a persistent cache below the LRU cache.
	 * @param entry the entry
	 */
	protected void privateNotifyDeletionFromCache(LRUCacheEntry entry) {
		// Default is NOP.
	}
	/**
	 * Removes the entry from the entry queue.
	 * @param entry the entry to remove
	 * @param shuffle indicates whether we are just shuffling the queue
	 * (in which case, the entry table is not modified).
	 */
	protected void privateRemoveEntry (LRUCacheEntry entry, boolean shuffle) {

		LRUCacheEntry previous, next;

		previous = entry._fPrevious;
		next = entry._fNext;

		if (!shuffle) {
			fEntryTable.remove(entry._fKey);
			fCurrentSpace -= entry._fSpace;
			privateNotifyDeletionFromCache(entry);
		}

		/* if this was the first entry */
		if (previous == null) {
			fEntryQueue = next;
		} else {
			previous._fNext = next;
		}

		/* if this was the last entry */
		if (next == null) {
			fEntryQueueTail = previous;
		} else {
			next._fPrevious = previous;
		}
	}
	/**
	 * Sets the value in the cache at the given key. Returns the value.
	 *
	 * @param key Key of object to add.
	 * @param value Value of object to add.
	 * @return added value.
	 */
	public Object put(Object key, Object value) {

		int newSpace, oldSpace, newTotal;
		LRUCacheEntry entry;

		/* Check whether there's an entry in the cache */
		newSpace = spaceFor(value);
		entry = fEntryTable.get(key);

		if (entry != null) {

			/**
			 * Replace the entry in the cache if it would not overflow
			 * the cache.  Otherwise flush the entry and re-add it so as
			 * to keep cache within budget
			 */
			oldSpace = entry._fSpace;
			newTotal = getCurrentSpace() - oldSpace + newSpace;
			if (newTotal <= getSpaceLimit()) {
				updateTimestamp (entry);
				entry._fValue = value;
				entry._fSpace = newSpace;
				this.fCurrentSpace = newTotal;
				return value;
			} else {
				privateRemoveEntry (entry, false);
			}
		}
		if (makeSpace(newSpace)) {
			privateAdd (key, value, newSpace);
		}
		return value;
	}
	/**
	 * Removes and returns the value in the cache for the given key.
	 * If the key is not in the cache, returns null.
	 *
	 * @param key Key of object to remove from cache.
	 * @return Value removed from cache.
	 */
	public Object removeKey (Object key) {

		LRUCacheEntry entry = fEntryTable.get(key);
		if (entry == null) {
			return null;
		}
		Object value = entry._fValue;
		this.privateRemoveEntry (entry, false);
		return value;
	}
	/**
	 * Sets the maximum amount of space that the cache can store
	 *
	 * @param limit Number of units of cache space
	 */
	public void setSpaceLimit(int limit) {
		if (limit < fSpaceLimit) {
			makeSpace(fSpaceLimit - limit);
		}
		fSpaceLimit = limit;
	}
	/**
	 * Returns the space taken by the given value.
	 * @param value the value
	 * @return an int
	 */
	protected int spaceFor (Object value) {
			return 1;
	}

	/**
	 * Returns a String that represents the value of this object.  This method
	 * is for debugging purposes only.
	 * @return a string
	 */
	@Override
	public String toString() {
		return
			toStringFillingRation("LRUCache") + //$NON-NLS-1$
			toStringContents();
	}

	/**
	 * Returns a String that represents the contents of this object.  This method
	 * is for debugging purposes only.
	 * @return a string
	 */
	protected String toStringContents() {
		StringBuilder result = new StringBuilder();
		int length = fEntryTable.size();
		Object[] unsortedKeys = new Object[length];
		String[] unsortedToStrings = new String[length];
		Enumeration<Object> e = this.keys();
		for (int i = 0; i < length; i++) {
			Object key = e.nextElement();
			unsortedKeys[i] = key;
			unsortedToStrings[i] = key.toString();
		}
		ToStringSorter sorter = new ToStringSorter();
		sorter.sort(unsortedKeys, unsortedToStrings);
		for (int i = 0; i < length; i++) {
			String toString = sorter.sortedStrings[i];
			Object value = this.get(sorter.sortedObjects[i]);
			result.append(toString);
			result.append(" -> "); //$NON-NLS-1$
			result.append(value);
			result.append("\n"); //$NON-NLS-1$
		}
		return result.toString();
	}

	public String toStringFillingRation(String cacheName) {
		StringBuilder buffer = new StringBuilder(cacheName);
		buffer.append('[');
		buffer.append(getSpaceLimit());
		buffer.append("]: "); //$NON-NLS-1$
		buffer.append(fillingRatio());
		buffer.append("% full"); //$NON-NLS-1$
		return buffer.toString();
	}

	/**
	 * Updates the timestamp for the given entry, ensuring that the queue is
	 * kept in correct order.  The entry must exist
	 * @param entry the entry
	 */
	protected void updateTimestamp (LRUCacheEntry entry) {

		entry._fTimestamp = fTimestampCounter++;
		if (fEntryQueue != entry) {
			this.privateRemoveEntry (entry, true);
			this.privateAddEntry (entry, true);
		}
		return;
	}
}
