/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
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

/** Adapted from a homonym class in org.eclipse.osgi. A hash table of
 * <code>KeyedElement</code>s.
 */

public class KeyedHashSet {
	public interface KeyedElement {

		boolean compare(KeyedElement other);

		Object getKey();

		int getKeyHashCode();
	}

	protected static final int MINIMUM_SIZE = 7;
	private int capacity;
	protected int elementCount = 0;
	protected KeyedElement[] elements;
	protected boolean replace;

	public KeyedHashSet(int capacity) {
		this(capacity, true);
	}

	public KeyedHashSet(int capacity, boolean replace) {
		elements = new KeyedElement[Math.max(MINIMUM_SIZE, capacity * 2)];
		this.replace = replace;
		this.capacity = capacity;
	}

	/**
	 * Adds an element to this set. If an element with the same key already exists,
	 * replaces it depending on the replace flag.
	 * @return true if the element was added/stored, false otherwise
	 */
	public boolean add(KeyedElement element) {
		int hash = hash(element);

		// search for an empty slot at the end of the array
		for (int i = hash; i < elements.length; i++) {
			if (elements[i] == null) {
				elements[i] = element;
				elementCount++;
				// grow if necessary
				if (shouldGrow())
					expand();
				return true;
			}
			if (elements[i].compare(element)) {
				if (replace)
					elements[i] = element;
				return replace;
			}
		}

		// search for an empty slot at the beginning of the array
		for (int i = 0; i < hash - 1; i++) {
			if (elements[i] == null) {
				elements[i] = element;
				elementCount++;
				// grow if necessary
				if (shouldGrow())
					expand();
				return true;
			}
			if (elements[i].compare(element)) {
				if (replace)
					elements[i] = element;
				return replace;
			}
		}

		// if we didn't find a free slot, then try again with the expanded set
		expand();
		return add(element);
	}

	public void clear() {
		elements = new KeyedElement[Math.max(MINIMUM_SIZE, capacity * 2)];
		elementCount = 0;
	}

	/**
	 * The array isn't large enough so double its size and rehash
	 * all its current values.
	 */
	protected void expand() {
		KeyedElement[] oldElements = elements;
		elements = new KeyedElement[elements.length * 2];

		int maxArrayIndex = elements.length - 1;
		for (KeyedElement element : oldElements) {
			if (element != null) {
				int hash = hash(element);
				while (elements[hash] != null) {
					hash++;
					if (hash > maxArrayIndex)
						hash = 0;
				}
				elements[hash] = element;
			}
		}
	}

	/**
	 * Returns the set element with the given id, or null
	 * if not found.
	 */
	public KeyedElement getByKey(Object key) {
		if (elementCount == 0)
			return null;
		int hash = keyHash(key);

		// search the last half of the array
		for (int i = hash; i < elements.length; i++) {
			KeyedElement element = elements[i];
			if (element == null)
				return null;
			if (element.getKey().equals(key))
				return element;
		}

		// search the beginning of the array
		for (int i = 0; i < hash - 1; i++) {
			KeyedElement element = elements[i];
			if (element == null)
				return null;
			if (element.getKey().equals(key))
				return element;
		}

		// nothing found so return null
		return null;
	}

	private int hash(KeyedElement key) {
		return (key.getKeyHashCode() & 0x7FFF_FFFF) % elements.length;
	}

	private int keyHash(Object key) {
		return (key.hashCode() & 0x7FFF_FFFF) % elements.length;
	}

	/**
	 * The element at the given index has been removed so move
	 * elements to keep the set properly hashed.
	 */
	protected void rehashTo(int anIndex) {

		int target = anIndex;
		int index = anIndex + 1;
		if (index >= elements.length)
			index = 0;
		KeyedElement element = elements[index];
		while (element != null) {
			int hashIndex = hash(element);
			boolean match;
			if (index < target)
				match = !(hashIndex > target || hashIndex <= index);
			else
				match = !(hashIndex > target && hashIndex <= index);
			if (match) {
				elements[target] = element;
				target = index;
			}
			index++;
			if (index >= elements.length)
				index = 0;
			element = elements[index];
		}
		elements[target] = null;
	}

	public boolean remove(KeyedElement toRemove) {
		if (elementCount == 0)
			return false;

		int hash = hash(toRemove);

		for (int i = hash; i < elements.length; i++) {
			KeyedElement element = elements[i];
			if (element == null)
				return false;
			if (element.compare(toRemove)) {
				rehashTo(i);
				elementCount--;
				return true;
			}
		}

		for (int i = 0; i < hash - 1; i++) {
			KeyedElement element = elements[i];
			if (element == null)
				return false;
			if (element.compare(toRemove)) {
				rehashTo(i);
				elementCount--;
				return true;
			}
		}
		return false;
	}

	private boolean shouldGrow() {
		return elementCount > elements.length * 0.75;
	}

	public int size() {
		return elementCount;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder(100);
		result.append('{');
		boolean first = true;
		for (KeyedElement element : elements) {
			if (element != null) {
				if (first)
					first = false;
				else
					result.append(", "); //$NON-NLS-1$
				result.append(element);
			}
		}
		result.append('}');
		return result.toString();
	}
}
