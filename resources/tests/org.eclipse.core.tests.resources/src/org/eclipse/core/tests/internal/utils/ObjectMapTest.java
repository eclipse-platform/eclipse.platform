/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *******************************************************************************/
package org.eclipse.core.tests.internal.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.eclipse.core.internal.utils.ObjectMap;
import org.junit.jupiter.api.Test;

public class ObjectMapTest {
	private static final int MAXIMUM = 100;

	@Test
	public void testPut() {
		// create the objects to insert into the map
		ObjectMap<Integer, Object> map = new ObjectMap<>();
		Object[] values = new Object[MAXIMUM];
		for (int i = 0; i < MAXIMUM; i++) {
			values[i] = Long.valueOf(System.currentTimeMillis());
		}

		// add each object to the map
		for (int i = 0; i < values.length; i++) {
			Integer key = Integer.valueOf(i);
			map.put(key, values[i]);
			assertThat(map).containsKey(key);
			assertThat(map).containsValue(values[i]);
			assertThat(map).hasSize(i + 1);
		}

		// make sure they are all still there
		assertThat(map).hasSize(MAXIMUM);
		for (int i = 0; i < values.length; i++) {
			Integer key = Integer.valueOf(i);
			assertThat(map).containsKey(key);
			assertNotNull(map.get(key), "" + i);
		}
	}

	@Test
	public void testPutEmptyMap() {
		ObjectMap<Object, Object> map = new ObjectMap<>(new HashMap<>());
		map.put(new Object(), new Object());
	}

	@Test
	public void testRemove() {
		// populate the map
		Object[] values = new Object[MAXIMUM];
		ObjectMap<Integer, Object> map = populateMap(values);

		// remove each element
		for (int i = MAXIMUM - 1; i >= 0; i--) {
			Integer key = Integer.valueOf(i);
			map.remove(key);
			assertThat(map).doesNotContainKey(key);
			assertThat(map).hasSize(i);
			// check that the others still exist
			for (int j = 0; j < i; j++) {
				assertThat(map).containsKey(Integer.valueOf(j));
			}
		}

		// all gone?
		assertThat(map).isEmpty();
	}

	@Test
	public void testContains() {
		Object[] values = new Object[MAXIMUM];
		ObjectMap<Integer, Object> map = populateMap(values);

		for (int i = 0; i < MAXIMUM; i++) {
			assertThat(map).containsKey(Integer.valueOf(i));
			assertThat(map).containsValue(values[i]);
		}

		assertThat(map).doesNotContainKey(Integer.valueOf(MAXIMUM + 1));
		assertThat(map).doesNotContainKey(Integer.valueOf(-1));
		assertThat(map).doesNotContainValue(null);
		assertThat(map).doesNotContainValue(createRandomString());
	}

	@Test
	public void testValues() {
		Object[] values = new Object[MAXIMUM];
		ObjectMap<Integer, Object> map = populateMap(values);

		Collection<Object> result = map.values();
		for (int i = 0; i < MAXIMUM; i++) {
			assertThat(result).contains(values[i]);
		}
	}

	@Test
	public void testKeySet() {
		Object[] values = new Object[MAXIMUM];
		ObjectMap<Integer, Object> map = populateMap(values);
		Set<Integer> keys = map.keySet();
		assertThat(keys).hasSize(MAXIMUM);
	}

	@Test
	public void testEntrySet() {
		Object[] values = new Object[MAXIMUM];
		ObjectMap<Integer, Object> map = populateMap(values);
		Set<Map.Entry<Integer, Object>> entries = map.entrySet();
		for (int i = 0; i < MAXIMUM; i++) {
			assertTrue(contains(entries, values[i]), "" + i);
		}
	}

	/**
	 * The given set is a set of Map.Entry objects.
	 */
	private boolean contains(Set<Map.Entry<Integer, Object>> set, Object value) {
		for (Map.Entry<Integer, Object> entry : set) {
			if (entry.getValue().equals(value)) {
				return true;
			}
		}
		return false;
	}

	private ObjectMap<Integer, Object> populateMap(Object[] values) {
		// populate the map
		ObjectMap<Integer, Object> map = new ObjectMap<>();
		for (int i = 0; i < values.length; i++) {
			values[i] = Long.valueOf(System.currentTimeMillis());
			map.put(Integer.valueOf(i), values[i]);
		}
		assertThat(values).hasSize(map.size());
		return map;
	}

	/*
	 * Bug 62231 - empty ObjectMap.toHashMap() causes NullPointerException
	 */
	@Test
	public void testBug_62231() {
		ObjectMap<Object, Object> map = new ObjectMap<>();
		map.entrySet();
		map.clear();
		map.entrySet();

	}
}
