/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
package org.eclipse.core.internal.jobs;

import java.util.*;

/**
 * A specialized map implementation that is optimized for a small set of object
 * keys.
 *
 * Implemented as a single array that alternates keys and values.
 *
 * Note: This class is copied from org.eclipse.core.resources
 */
public class ObjectMap implements Map {
	// 8 attribute keys, 8 attribute values
	protected static final int DEFAULT_SIZE = 16;
	protected static final int GROW_SIZE = 10;
	protected int count = 0;
	protected Object[] elements = null;

	/**
	 * Creates a new object map.
	 *
	 * @param initialCapacity
	 *                  The initial number of elements that will fit in the map.
	 */
	public ObjectMap(int initialCapacity) {
		if (initialCapacity > 0)
			elements = new Object[Math.max(initialCapacity * 2, 0)];
	}

	/**
	 * Creates a new object map of the same size as the given map and populate
	 * it with the key/attribute pairs found in the map.
	 *
	 * @param map
	 *                  The entries in the given map will be added to the new map.
	 */
	public ObjectMap(Map map) {
		this(map.size());
		putAll(map);
	}

	/**
	 * @see Map#clear()
	 */
	@Override
	public void clear() {
		elements = null;
		count = 0;
	}

	/**
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Object clone() {
		return new ObjectMap(this);
	}

	/**
	 * @see Map#containsKey(java.lang.Object)
	 */
	@Override
	public boolean containsKey(Object key) {
		if (elements == null || count == 0)
			return false;
		for (int i = 0; i < elements.length; i = i + 2)
			if (elements[i] != null && elements[i].equals(key))
				return true;
		return false;
	}

	/**
	 * @see Map#containsValue(java.lang.Object)
	 */
	@Override
	public boolean containsValue(Object value) {
		if (elements == null || count == 0)
			return false;
		for (int i = 1; i < elements.length; i = i + 2)
			if (elements[i] != null && elements[i].equals(value))
				return true;
		return false;
	}

	/**
	 * @see Map#entrySet()
	 *
	 * Note: This implementation does not conform properly to the
	 * specification in the Map interface. The returned collection will not
	 * be bound to this map and will not remain in sync with this map.
	 */
	@Override
	public Set entrySet() {
		return count == 0 ? Collections.EMPTY_SET : toHashMap().entrySet();
	}

	/**
	 * @see Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Map))
			return false;
		Map other = (Map) o;
		//must be same size
		if (count != other.size())
			return false;
		//keysets must be equal
		if (!keySet().equals(other.keySet()))
			return false;
		//values for each key must be equal
		for (int i = 0; i < elements.length; i = i + 2) {
			if (elements[i] != null && (!elements[i + 1].equals(other.get(elements[i]))))
				return false;
		}
		return true;
	}

	/**
	 * @see Map#get(java.lang.Object)
	 */
	@Override
	public Object get(Object key) {
		if (elements == null || count == 0)
			return null;
		for (int i = 0; i < elements.length; i = i + 2)
			if (elements[i] != null && elements[i].equals(key))
				return elements[i + 1];
		return null;
	}

	/**
	 * The capacity of the map has been exceeded, grow the array by GROW_SIZE to
	 * accommodate more entries.
	 */
	protected void grow() {
		Object[] expanded = new Object[elements.length + GROW_SIZE];
		System.arraycopy(elements, 0, expanded, 0, elements.length);
		elements = expanded;
	}

	/**
	 * @see Object#hashCode()
	 */
	@Override
	public int hashCode() {
		int hash = 0;
		for (int i = 0; i < elements.length; i = i + 2) {
			if (elements[i] != null) {
				hash += elements[i].hashCode();
			}
		}
		return hash;
	}

	/**
	 * @see Map#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		return count == 0;
	}

	/**
	 * @see Map#keySet()
	 *
	 * Note: This implementation does not conform properly to the
	 * specification in the Map interface. The returned collection will not
	 * be bound to this map and will not remain in sync with this map.
	 */
	@Override
	public Set keySet() {
		Set<Object> result = new HashSet<>(size());
		for (int i = 0; i < elements.length; i = i + 2) {
			if (elements[i] != null) {
				result.add(elements[i]);
			}
		}
		return result;
	}

	/**
	 * @see Map#put(java.lang.Object, java.lang.Object)
	 */
	@Override
	public Object put(Object key, Object value) {
		if (key == null)
			throw new NullPointerException();
		if (value == null)
			return remove(key);

		// handle the case where we don't have any attributes yet
		if (elements == null)
			elements = new Object[DEFAULT_SIZE];
		if (count == 0) {
			elements[0] = key;
			elements[1] = value;
			count++;
			return null;
		}

		int emptyIndex = -1;
		// replace existing value if it exists
		for (int i = 0; i < elements.length; i += 2) {
			if (elements[i] != null) {
				if (elements[i].equals(key)) {
					Object oldValue = elements[i + 1];
					elements[i + 1] = value;
					return oldValue;
				}
			} else if (emptyIndex == -1) {
				// keep track of the first empty index
				emptyIndex = i;
			}
		}
		// this will put the emptyIndex greater than the size but
		// that's ok because we will grow first.
		if (emptyIndex == -1)
			emptyIndex = count * 2;

		// otherwise add it to the list of elements.
		// grow if necessary
		if (elements.length <= (count * 2))
			grow();
		elements[emptyIndex] = key;
		elements[emptyIndex + 1] = value;
		count++;
		return null;
	}

	/**
	 * @see Map#putAll(java.util.Map)
	 */
	@Override
	public void putAll(Map map) {
		for (Iterator i = map.keySet().iterator(); i.hasNext();) {
			Object key = i.next();
			Object value = map.get(key);
			put(key, value);
		}
	}

	/**
	 * @see Map#remove(java.lang.Object)
	 */
	@Override
	public Object remove(Object key) {
		if (elements == null || count == 0)
			return null;
		for (int i = 0; i < elements.length; i = i + 2) {
			if (elements[i] != null && elements[i].equals(key)) {
				elements[i] = null;
				Object result = elements[i + 1];
				elements[i + 1] = null;
				count--;
				return result;
			}
		}
		return null;
	}

	/**
	 * @see Map#size()
	 */
	@Override
	public int size() {
		return count;
	}

	/**
	 * Creates a new hash map with the same contents as this map.
	 */
	private HashMap<Object, Object> toHashMap() {
		HashMap<Object, Object> result = new HashMap<>(size());
		for (int i = 0; i < elements.length; i = i + 2) {
			if (elements[i] != null) {
				result.put(elements[i], elements[i + 1]);
			}
		}
		return result;
	}

	/**
	 * @see Map#values()
	 *
	 * Note: This implementation does not conform properly to the
	 * specification in the Map interface. The returned collection will not
	 * be bound to this map and will not remain in sync with this map.
	 */
	@Override
	public Collection values() {
		Set<Object> result = new HashSet<>(size());
		for (int i = 1; i < elements.length; i = i + 2) {
			if (elements[i] != null) {
				result.add(elements[i]);
			}
		}
		return result;
	}
}