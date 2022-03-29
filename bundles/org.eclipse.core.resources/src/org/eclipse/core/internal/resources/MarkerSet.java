/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.util.Arrays;
import org.eclipse.core.internal.utils.IStringPoolParticipant;
import org.eclipse.core.internal.utils.StringPool;

public class MarkerSet implements Cloneable, IStringPoolParticipant {
	protected static final int MINIMUM_SIZE = 5;
	protected int elementCount = 0;
	protected IMarkerSetElement[] elements;

	public MarkerSet() {
		this(MINIMUM_SIZE);
	}

	public MarkerSet(int capacity) {
		super();
		this.elements = new IMarkerSetElement[Math.max(MINIMUM_SIZE, capacity * 2)];
	}

	public void add(IMarkerSetElement element) {
		if (element == null)
			return;
		int hash = hashFor(element.getId()) % elements.length;

		// search for an empty slot at the end of the array
		for (int i = hash; i < elements.length; i++) {
			if (elements[i] == null) {
				elements[i] = element;
				elementCount++;
				// grow if necessary
				if (shouldGrow())
					expand();
				return;
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
				return;
			}
		}

		// if we didn't find a free slot, then try again with the expanded set
		expand();
		add(element);
	}

	public void addAll(IMarkerSetElement[] toAdd) {
		for (IMarkerSetElement element : toAdd)
			add(element);
	}

	@Override
	protected Object clone() {
		try {
			MarkerSet copy = (MarkerSet) super.clone();
			//copy the attribute array
			copy.elements = elements.clone();
			return copy;
		} catch (CloneNotSupportedException e) {
			//cannot happen because this class implements Cloneable
			return null;
		}
	}

	public boolean contains(long id) {
		return get(id) != null;
	}

	public IMarkerSetElement[] elements() {
		IMarkerSetElement[] result = new IMarkerSetElement[elementCount];
		int j = 0;
		for (IMarkerSetElement element : elements) {
			if (element != null)
				result[j++] = element;
		}
		return result;
	}

	/**
	 * The array isn't large enough so double its size and rehash
	 * all its current values.
	 */
	protected void expand() {
		IMarkerSetElement[] array = new IMarkerSetElement[elements.length * 2];
		int maxArrayIndex = array.length - 1;
		for (IMarkerSetElement element : elements) {
			if (element != null) {
				int hash = hashFor(element.getId()) % array.length;
				while (array[hash] != null) {
					hash++;
					if (hash > maxArrayIndex)
						hash = 0;
				}
				array[hash] = element;
			}
		}
		elements = array;
	}

	/**
	 * Returns the set element with the given id, or null
	 * if not found.
	 */
	public IMarkerSetElement get(long id) {
		if (elementCount == 0)
			return null;
		int hash = hashFor(id) % elements.length;

		// search the last half of the array
		for (int i = hash; i < elements.length; i++) {
			IMarkerSetElement element = elements[i];
			if (element == null)
				return null;
			if (element.getId() == id)
				return element;
		}

		// search the beginning of the array
		for (int i = 0; i < hash - 1; i++) {
			IMarkerSetElement element = elements[i];
			if (element == null)
				return null;
			if (element.getId() == id)
				return element;
		}

		// marker info not found so return null
		return null;
	}

	private int hashFor(long id) {
		return Math.abs((int) id);
	}

	public boolean isEmpty() {
		return elementCount == 0;
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
		IMarkerSetElement element = elements[index];
		while (element != null) {
			int hashIndex = hashFor(element.getId()) % elements.length;
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

	public void remove(long id) {
		int hash = hashFor(id) % elements.length;

		for (int i = hash; i < elements.length; i++) {
			IMarkerSetElement element = elements[i];
			if (element == null)
				return;
			if (element.getId() == id) {
				rehashTo(i);
				elementCount--;
			}
		}

		for (int i = 0; i < hash - 1; i++) {
			IMarkerSetElement element = elements[i];
			if (element == null)
				return;
			if (element.getId() == id) {
				rehashTo(i);
				elementCount--;
			}
		}
	}

	public void remove(IMarkerSetElement element) {
		remove(element.getId());
	}

	public void removeAll(IMarkerSetElement[] toRemove) {
		for (IMarkerSetElement element : toRemove)
			remove(element);
	}

	private boolean shouldGrow() {
		return elementCount > elements.length * 0.75;
	}

	public int size() {
		return elementCount;
	}

	/* (non-Javadoc
	 * Method declared on IStringPoolParticipant
	 */
	@Override
	public void shareStrings(StringPool set) {
		//copy elements for thread safety
		IMarkerSetElement[] array = elements;
		if (array == null)
			return;
		for (IMarkerSetElement o : array) {
			if (o instanceof IStringPoolParticipant)
				((IStringPoolParticipant) o).shareStrings(set);
		}
	}

	/** for debugging only **/
	@Override
	public String toString() {
		return Arrays.toString(elements());
	}
}
