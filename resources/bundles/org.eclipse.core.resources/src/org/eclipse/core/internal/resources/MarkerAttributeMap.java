/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *     Joerg Kubitz - redesign
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.core.internal.utils.IStringPoolParticipant;
import org.eclipse.core.internal.utils.StringPool;

/**
 * A specialized Map<String,Object> implementation that is optimized for a small
 * set of strings as keys. The keys will be interned() on insert.
 *
 * Unlike a java.util.HashMap nulls are neither allowed for key or value.
 */
// the Map interface is not implemented as it would allow to insert null key or values
// or non interned keys via the iterator if not a specific entrySet is implemented.
public class MarkerAttributeMap implements IStringPoolParticipant {
	// This implementation is a copy on write map.
	private final AtomicReference<Map<String, Object>> mapRef;

	// Typically contains 9 keys:
	// "severity","sourceId","charStart","charEnd","arguments","id","message","lineNumber","categoryId"
	protected static final int DEFAULT_SIZE = 9;

	/**
	 * Creates a new marker attribute map of default size
	 */
	public MarkerAttributeMap() {
		this(DEFAULT_SIZE);
	}

	/**
	 * Creates a new marker attribute map.
	 *
	 * @param initialCapacity The initial number of elements that will fit in the
	 *                        map.
	 */
	public MarkerAttributeMap(int initialCapacity) {
		// ignore initialCapacity - a copy on write datastructure will be copied anyway.
		mapRef = new AtomicReference<>(Map.of());
	}

	/**
	 * Copy constructor. Note that a java.util.Map can not be passed since it could
	 * contain null keys or null values, or keys that are not interned.
	 */
	public MarkerAttributeMap(MarkerAttributeMap m) {
		mapRef = new AtomicReference<>(copy(m.getMap()));
	}

	/**
	 * Copy constructor. Entries with null keys are not allowed. Entries with null
	 * values are silently ignored.
	 */
	public MarkerAttributeMap(Map<String, ? extends Object> map, boolean validate) {
		mapRef = new AtomicReference<>(copy(map, validate));
	}

	/**
	 * delete all previous values and replace with given map. Entries with null keys
	 * are not allowed. Entries with null values are silently ignored.
	 */
	public void setAttributes(Map<String, ? extends Object> map, boolean validate) {
		mapRef.set(copy(map, validate));
	}

	private Map<String, Object> copy(Map<String, ? extends Object> map, boolean validate) {
		Map<String, Object> target = new HashMap<>();
		putAll(target, map, validate);
		return target;
	}

	/**
	 * puts all entries of the given map. Entries with null keys are not allowed.
	 * Entries with null values are silently ignored.
	 */
	public void putAll(Map<String, ? extends Object> map, boolean validate) {
		mapRef.getAndUpdate(old -> {
			Map<String, Object> copy = copy(old);
			putAll(copy, map, validate);
			return copy;
		});
	}

	private void putAll(Map<String, Object> target, Map<String, ? extends Object> source, boolean validate) {
		if (source == null) {
			return;
		}
		for (Entry<String, ? extends Object> e : source.entrySet()) {
			String key = e.getKey();
			Objects.requireNonNull(key, "insert of null key not allowed"); //$NON-NLS-1$
			Object value = e.getValue();
			if (validate) {
				value = MarkerInfo.checkValidAttribute(value);
			}
			if (value != null) { // null values => ignore
				target.put(e.getKey().intern(), value);
			}
		}
	}

	private Map<String, Object> copy(Map<String, ? extends Object> map) {
		return new HashMap<>(map);
	}

	private Map<String, Object> getMap() {
		return mapRef.get();
	}

	/** creates a copy that fulfills the java.util.Map interface **/
	public Map<String, Object> toMap() {
		return copy(this.getMap());
	}

	/** @see java.util.Map#entrySet **/
	public Set<Map.Entry<String, Object>> entrySet() {
		return getMap().entrySet();
	}

	/**
	 * like {@link java.util.Map#put(Object, Object)} but null keys or values are
	 * not allowed
	 */
	public void put(String k, Object value) {
		Objects.requireNonNull(k, "insert of null key not allowed"); //$NON-NLS-1$
		Objects.requireNonNull(value, "insert of null value not allowed"); //$NON-NLS-1$
		mapRef.getAndUpdate(map -> {
			Map<String, Object> m = copy(map);
			m.put(k.intern(), value);
			return m;
		});
	}

	@Override
	public void shareStrings(StringPool set) {
		// don't share keys because they are already interned
		for (java.util.Map.Entry<String, Object> e : getMap().entrySet()) {
			Object o = e.getValue();
			if (o instanceof String) {
				e.setValue(set.add((String) o));
			} else if (o instanceof IStringPoolParticipant) {
				((IStringPoolParticipant) o).shareStrings(set);
			}
		}
	}

	/** @see java.util.Map#isEmpty **/
	public boolean isEmpty() {
		return getMap().isEmpty();
	}

	/** @see java.util.Map#remove **/
	public Object remove(Object key) {
		return getMap().remove(key);
	}

	/** @see java.util.Map#get **/
	public Object get(Object key) {
		return getMap().get(key);
	}

	/** @see java.util.Map#size **/
	public int size() {
		return getMap().size();
	}

}
