/*******************************************************************************
 * Copyright (c) 2002, 2013 Object Factory Inc.
 *
 * This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *		Object Factory Inc. - Initial implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.dtd.util;

import java.text.MessageFormat;
import java.util.Comparator;
import java.util.Iterator;

/**
 * @author Bob Foster
 */
public class SortedMap implements FactoryObject {

	private final SortedSet fSet;
	private IMapHolder fHolder;
	private SortedMap fNext;

	public SortedMap(IMapHolder holder, Comparator<Object> comp) {
		fHolder = holder;
		fSet = new SortedSet(holder, comp);
	}

	public SortedMap(Comparator<Object> comp) {
		fSet = new SortedSet(comp);
	}

	public SortedMap(IMapHolder holder) {
		fHolder = holder;
		fSet = new SortedSet(holder);
	}

	public SortedMap() {
		fSet = new SortedSet();
	}

	public void setMapHolder(IMapHolder holder) {
		fHolder = holder;
		fSet.setKeyHolder(holder);
	}

	public void setComparator(Comparator<Object> comp) {
		fSet.setComparator(comp);
	}

	public boolean containsKey(Object key) {
		return fSet.contains(key);
	}

	public boolean containsKeyIdentity(Object key) {
		return fSet.containsIdentity(key);
	}

	public Object put(Object key, Object val) {
		Object[] values = fHolder.getValues();
		int index = fSet.indexOf(key);
		Object result = index >= 0 && values != null ? values[index] : null;

		int i = fSet.internalAdd(key, false);
		if (i >= 0)
			internalPut(i, val);
		return result;
	}

	protected void internalPut(int i, Object val) {
		Object[] values = fHolder.getValues();
		if (values == null) {
			values = new Object[1];
			values[0] = val;
			return;
		}

		Object[] tmp = new Object[values.length + 1];
		System.arraycopy(values, 0, tmp, 0, i);
		tmp[i] = val;
		System.arraycopy(values, i, tmp, i + 1, values.length - i);
		fHolder.setValues(tmp);
	}

	public int putAlways(Object key, Object val) {
		int i = fSet.internalAdd(key, true);
		internalPut(i, val);
		return i;
	}

	public void append(Object key, Object val) {
		Object[] values = fHolder.getValues();
		int len = values != null ? values.length : 0;
		fSet.internalAdd(len, key);
		internalPut(len, val);
	}

	public Object get(Object key) {
		Object[] values = fHolder.getValues();
		if (values == null)
			return null;
		int i = fSet.indexOf(key);
		if (i >= 0)
			return values[i];
		return null;
	}

	public Object getIdentity(Object key) {
		Object[] values = fHolder.getValues();
		if (values == null)
			return null;
		int i = fSet.indexOfIdentity(key);
		if (i >= 0)
			return values[i];
		return null;
	}

	public Object[] keys() {
		return fSet.members();
	}

	public Object[] values() {
		Object[] values = fHolder.getValues();
		if (values == null)
			return new Object[0];
		return values;
	}

	public Iterator<?> keyIterator() {
		return new ArrayIterator();
	}

	public Iterator<?> valueIterator() {
		return new ArrayIterator();
	}

	private class ArrayIterator implements Iterator<Object> {
		private int fIndex;

		public ArrayIterator() {
			fIndex = -1;
		}

		@Override
		public boolean hasNext() {
			Object[] array = SortedMap.this.fHolder.getKeys();
			if (array == null)
				return false;
			return fIndex + 1 < array.length;
		}

		@Override
		public Object next() {
			Object[] array = SortedMap.this.fHolder.getKeys();
			if (array == null)
				throw new IllegalStateException(AntDTDUtilMessages.SortedMap_next___called_for_empty_array_1);
			return array[++fIndex];
		}

		@Override
		public void remove() {
			SortedMap.this.remove(fIndex);
			--fIndex;
		}

	}

	public void remove(int i) {
		Object[] values = fHolder.getValues();
		if (values == null) {
			throw new IllegalArgumentException(MessageFormat.format(AntDTDUtilMessages.SortedMap_remove__0___in_empty_map_2, new Object[] {
					Integer.toString(i) }));
		}
		fSet.remove(i);
		Object[] tmp = new Object[values.length - 1];
		System.arraycopy(values, 0, tmp, 0, i);
		System.arraycopy(values, i + 1, tmp, i, values.length - i - 1);
		fHolder.setValues(tmp);
	}

	public Object remove(Object obj) {
		Object[] values = fHolder.getValues();
		if (values == null)
			return null;
		int i = fSet.indexOf(obj);
		if (i >= 0) {
			Object tmp = values[i];
			fSet.remove(i);
			remove(i);
			return tmp;
		}
		return null;
	}

	public Object removeIdentity(Object obj) {
		Object[] values = fHolder.getValues();
		if (values == null)
			return null;
		int i = fSet.indexOfIdentity(obj);
		if (i >= 0) {
			Object tmp = values[i];
			fSet.remove(i);
			remove(i);
			return tmp;
		}
		return null;
	}

	public int size() {
		return fSet.size();
	}

	public int keyIndex(Object key) {
		return fSet.indexOf(key);
	}

	public void merge(SortedMap other) {
		Object[] values = fHolder.getValues();
		Object[] keys = fHolder.getKeys();
		Object[] othervalues = other.fHolder.getValues();
		Object[] otherkeys = other.fHolder.getKeys();
		if (otherkeys == null)
			return;
		if (keys == null) {
			fHolder.setKeys(otherkeys);
			fHolder.setValues(othervalues);
			return;
		}
		int ithis = 0, iother = 0, i = 0;
		int mthis = keys.length, mother = otherkeys.length;
		Object[] ktmp = new Object[mthis + mother];
		Object[] vtmp = new Object[mthis + mother];
		while (ithis < mthis && iother < mother) {
			int comp = fSet.fComp.compare(keys[ithis], otherkeys[iother]);
			if (comp <= 0) {
				vtmp[i] = values[ithis];
				ktmp[i++] = keys[ithis++];
			} else {
				vtmp[i] = othervalues[iother];
				ktmp[i++] = otherkeys[iother++];
			}
		}
		while (ithis < mthis) {
			vtmp[i] = values[ithis];
			ktmp[i++] = keys[ithis++];
		}
		while (iother < mother) {
			vtmp[i] = othervalues[iother];
			ktmp[i++] = otherkeys[iother++];
		}
		fHolder.setKeys(ktmp);
		fHolder.setValues(vtmp);
	}

	@Override
	public FactoryObject next() {
		return fNext;
	}

	@Override
	public void next(FactoryObject next) {
		fNext = (SortedMap) next;
	}
}
