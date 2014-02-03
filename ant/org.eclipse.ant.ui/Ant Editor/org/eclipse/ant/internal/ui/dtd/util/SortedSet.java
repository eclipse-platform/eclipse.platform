/*******************************************************************************
 * Copyright (c) 2002, 2005 Object Factory Inc.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *		Object Factory Inc. - Initial implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.dtd.util;

import java.util.Comparator;

/**
 * SortedSet is a flyweight set implementation that uses
 * an external array provided by a KeyHolder.
 * SortedSet provides both equality/comparison operations
 * and identity operations.
 * @author Bob Foster
 */
public class SortedSet {
	protected Comparator fComp;
	protected IKeyHolder fKeyHolder;
	protected SortedSet fNext;
	
	public SortedSet(IKeyHolder holder, Comparator comp) {
		fComp = comp;
		fKeyHolder = holder;
	}
	/**
	 * Constructor. A keyholder must be
	 * supplied by <code>setKeyHolder()</code> prior ot
	 * <i>any</i> operations.
	 */
	public SortedSet(Comparator comp) {
		fComp = comp;
	}
	/**
	 * Constructor, no comparator. Only identity operations
	 * may be performed in this set.
	 */
	public SortedSet(IKeyHolder holder) {
		fKeyHolder = holder;
	}
	/**
	 * Constructor, no comparator. Only identity operations
	 * may be performed in this set. A keyholder must be
	 * supplied by <code>setKeyHolder()</code> prior ot
	 * <i>any</i> operations.
	 */
	public SortedSet() {
	}
	public void setKeyHolder(IKeyHolder holder) {
		fKeyHolder = holder;
	}
	public void setComparator(Comparator comp) {
		fComp = comp;
	}
	/**
	 * Add to set (no duplicates allowed).
	 * @param obj Object to add
	 * @return true if object was added; false
	 * if object was already in the set.
	 */
	public boolean add(Object obj) {
		return internalAdd(obj, false) >= 0;
	}
	protected int internalAdd(Object obj, boolean always) {
		Object[] array = fKeyHolder.getKeys();
		if (array == null) {
			array = new Object[1];
			fKeyHolder.setKeys(array);
			array[0] = obj;
			return 0;
		}
		int i = 0;
		int comp = -1;
		
		for (; i < array.length; i++) {
			if ((comp = fComp.compare(obj, array[i])) <= 0) {
				break;
			}
		}
		if (comp == 0 && !always)
			return -1;
		internalAdd(i, obj);
		return i;
	}
	protected void internalAdd(int i, Object obj) {
		Object[] array = fKeyHolder.getKeys();
		if (array == null) {
			array = new Object[1];
			array[0] = obj;
			fKeyHolder.setKeys(array);
		}
		else {
			Object[] tmp = new Object[array.length+1];
			System.arraycopy(array,0,tmp,0,i);
			tmp[i] = obj;
			System.arraycopy(array,i,tmp,i+1,array.length-i);
			fKeyHolder.setKeys(tmp);
		}
	}
	/**
	 * Add allowing duplicates.
	 * @param obj Object to add
	 * @return index where object was added in sorted order.
	 */
	public int addAlways(Object obj) {
		return internalAdd(obj, true);
	}
	/**
	 * Append, a variant of add allowing duplicates that
	 * always puts the new member at the end of the set.
	 * Set can be used with identity operations only.
	 */
	public void append(Object obj) {
		Object[] array = fKeyHolder.getKeys();
		int len = array != null ? array.length : 0;
		internalAdd(len, obj);
	}
	public boolean contains(Object obj) {
		return indexOf(obj) >= 0;
	}
	public int indexOf(Object obj) {
		Object[] array = fKeyHolder.getKeys();
		if (array == null)
			return -1;
		for (int i = 0; i < array.length; i++) {
			int comp = fComp.compare(obj, array[i]);
			if (comp == 0)
				return i;
			if (comp < 0)
				return -1;
		}
		return -1;
	}
	public boolean containsIdentity(Object obj) {
		return indexOf(obj) >= 0;
	}
	public int indexOfIdentity(Object obj) {
		Object[] array = fKeyHolder.getKeys();
		if (array == null)
			return -1;
		for (int i = 0; i < array.length; i++) {
			if (obj == array[i])
				return i;
		}
		return -1;
	}
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof SortedSet))
			return false;
		SortedSet other = (SortedSet) o;
		Object[] array = fKeyHolder.getKeys();
		Object[] otherarray = other.fKeyHolder.getKeys();
		if ((array == null) != (otherarray == null))
			return false;
		if (array == null)
			return true;
		if (array.length != otherarray.length)
			return false;
		for (int i = 0; i < array.length; i++) {
			if (array[i] != otherarray[i])
				return false;
		}
		return true;
	}
	public boolean equalsIdentify(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof SortedSet))
			return false;
		SortedSet other = (SortedSet) o;
		Object[] array = fKeyHolder.getKeys();
		Object[] otherarray = other.fKeyHolder.getKeys();
		if ((array == null) != (otherarray == null))
			return false;
		if (array == null)
			return true;
		if (array.length != otherarray.length)
			return false;
		for (int i = 0; i < array.length; i++) {
			if (fComp.compare(array[i], otherarray[i]) != 0)
				return false;
		}
		return true;
	}
	public void merge(SortedSet other) {
		Object[] array = fKeyHolder.getKeys();
		Object[] otherarray = other.fKeyHolder.getKeys();
		if (otherarray == null)
			return;
		if (array == null) {
			array = otherarray;
			return;
		}
		int ithis = 0, iother = 0, i = 0;
		int mthis = array.length, mother = otherarray.length;
		Object[] tmp = new Object[mthis+mother];
		while (ithis < mthis && iother < mother) {
			int comp = fComp.compare(array[ithis], otherarray[iother]);
			if (comp <= 0) {
				tmp[i++] = array[ithis++];
			}
			else {
				tmp[i++] = otherarray[iother++];
			}
		}
		while (ithis < mthis) {
			tmp[i++] = array[ithis++];
		}
		while (iother < mother) {
			tmp[i++] = otherarray[iother++];
		}
	}
	public Object[] members() {
		Object[] array = fKeyHolder.getKeys();
		if (array == null)
			return new Object[0];
		return array;
	}
	public int size() {
		Object[] array = fKeyHolder.getKeys();
		return array == null ? 0 : array.length;
	}
	public void remove(int i) {
		Object[] array = fKeyHolder.getKeys();
		Object[] tmp = new Object[array.length-1];
		System.arraycopy(array,0,tmp,0,i);
		System.arraycopy(array,i+1,tmp,i,array.length-i-1);
		fKeyHolder.setKeys(tmp);
	}
	public boolean remove(Object obj) {
		int i = indexOf(obj);
		if (i >= 0) {
			remove(i);
			return true;
		}
		return false;
	}
	public boolean removeIdentity(Object obj) {
		int i = indexOfIdentity(obj);
		if (i >= 0) {
			remove(i);
			return true;
		}
		return false;
	}
	public SortedSet getNextSet() {
		return fNext;
	}
	public void setNextSet(SortedSet next) {
		fNext = next;
	}

}
