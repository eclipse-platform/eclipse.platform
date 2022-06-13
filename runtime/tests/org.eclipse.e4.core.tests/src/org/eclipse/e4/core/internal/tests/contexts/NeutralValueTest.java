/*******************************************************************************
 * Copyright (c) 2021 Joerg Kubitz.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Joerg Kubitz              - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.core.internal.tests.contexts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.e4.core.internal.contexts.ConcurrentNeutralValueMap;
import org.eclipse.e4.core.internal.contexts.ConcurrentNeutralValueMap.Value;
import org.junit.Test;

public class NeutralValueTest {

	@Test
	public void testConcurrentNeutralValueMap() {
		ConcurrentNeutralValueMap<String, Double> map = new ConcurrentNeutralValueMap<>(Double.NaN);
		map.put("nix", null); // modify
		map.put("2", 2.0); // modify
		map.put("3", 3.0); // modify
		map.put("4", 4.0); // modify
		map.put("garnix", null); // modify
		assertTrue(map.containsKey("garnix"));
		map.remove("garnix");

		assertTrue(map.containsKey("nix"));
		assertTrue(map.containsKey("2"));
		assertFalse(map.containsKey("1"));
		assertFalse(map.containsKey("garnix"));

		assertEquals(4, map.size());

		assertFalse(map.isEmpty());

		assertEquals(null, map.get("nix"));
		assertEquals(null, map.get("1"));
		assertEquals(Double.valueOf(2.0), map.get("2"));
		assertEquals(Double.valueOf(3.0), map.get("3"));
		assertEquals(Double.valueOf(4.0), map.get("4"));

		Set<String> keys = new HashSet<>();
		Set<Double> values = new HashSet<>();
		map.forEach((k, v) -> keys.add(k));
		map.forEach((k, v) -> values.add(v));
		assertEquals(Set.of("nix", "2", "3", "4"), keys);
		assertTrue(values.contains(null));
		assertTrue(values.contains(2.0));

		assertTrue(map.getValue("nix").isPresent());
		assertFalse(map.getValue("1").isPresent());
		assertTrue(map.getValue("2").isPresent());
		assertTrue(map.getValue("3").isPresent());
		assertTrue(map.getValue("4").isPresent());

		{
			Value<Double> v = map.getValue("nix");
			assertTrue(v.isPresent());
			assertEquals(null, v.unwrapped());
		}
		{
			Value<Double> v = map.getValue("1");
			assertFalse(v.isPresent());
			assertEquals(null, v.unwrapped());
		}
		{
			Value<Double> v = map.getValue("2");
			assertTrue(v.isPresent());
			assertEquals(Double.valueOf(2.0), v.unwrapped());
		}

		{
			Value<Double> v = map.putAndGetOld("5", 5555.0); // modify
			assertFalse(v.isPresent());
			assertEquals(null, v.unwrapped());
			assertEquals(Double.valueOf(5555.0), map.get("5"));
		}
		{
			Value<Double> v = map.putAndGetOld("5", 5.0); // modify
			assertTrue(v.isPresent());
			assertEquals(Double.valueOf(5555.0), v.unwrapped());
			assertEquals(Double.valueOf(5.0), map.get("5"));
		}
		map.putIfAbsent("5", 5555.0); // modify
		assertEquals(Double.valueOf(5.0), map.get("5"));
		map.remove("5"); // modify
		assertFalse(map.containsKey("5"));

		{
			Value<Double> v = map.putAndGetOld("five", null); // modify
			assertFalse(v.isPresent());
			assertEquals(null, v.unwrapped());
			assertEquals(null, map.get("five"));
		}
		{
			Value<Double> v = map.putAndGetOld("five", 5.0); // modify
			assertTrue(v.isPresent());
			assertEquals(null, v.unwrapped());
			assertEquals(Double.valueOf(5.0), map.get("five"));
		}
		{
			map.putIfAbsent("five", null); // modify
			Value<Double> v = map.getValue("five");
			assertTrue(v.isPresent());
			assertEquals(Double.valueOf(5.0), v.unwrapped());
		}
		map.remove("five"); // modify
		{
			map.putIfAbsent("five", null); // modify
			Value<Double> v = map.getValue("five");
			assertTrue(v.isPresent());
			assertEquals(null, v.unwrapped());
		}
		map.remove("five"); // modify
		assertFalse(map.containsKey("five"));

		map.clear(); // modify
		assertEquals(0, map.size());
		assertTrue(map.isEmpty());
	}

	@Test
	public void testToString() {
		ConcurrentNeutralValueMap<String, Float> map1 = new ConcurrentNeutralValueMap<>();
		map1.put("0", 0f);
		map1.put("NULL", null);
		map1.put("nothing", null);
		map1.put("1", 1f);
		map1.put("~2", 2.1f);
		assertTrue(map1.toString().contains("0=0.0"));
		assertTrue(map1.toString().contains("1=1.0"));
		assertTrue(map1.toString().contains("~2=2.1"));
		assertTrue(map1.toString().contains("NULL=null"));
		assertTrue(map1.toString().contains("nothing=null"));
	}

	@Test
	public void testCustomToString() {
		ConcurrentNeutralValueMap<String, Float> map1 = new ConcurrentNeutralValueMap<>(Float.NaN);
		map1.put("0", 0f);
		map1.put("NULL", null);
		assertTrue(map1.toString().contains("0=0.0"));
		assertTrue(map1.toString().contains("NULL=NaN"));
	}

	@Test
	public void testEquals() {
		{
			ConcurrentNeutralValueMap<String, Float> map1 = new ConcurrentNeutralValueMap<>();
			ConcurrentNeutralValueMap<String, Float> map2 = new ConcurrentNeutralValueMap<>();
			map1.put("0", 0f);
			map1.put("NULL", null);
			map2.put("NULL", null);
			map2.put("0", 0f);
			assertEquals(map1.hashCode(), map2.hashCode());
			assertEquals(map1, map2);
		}
		{
			ConcurrentNeutralValueMap<String, Float> map1 = new ConcurrentNeutralValueMap<>();
			ConcurrentNeutralValueMap<String, Float> map2 = new ConcurrentNeutralValueMap<>();
			map1.put("0", 0f);
			map1.put("1", 1f);
			map1.put("NULL", null);
			map2.put("NULL", null);
			map2.put("0", 0f);
			assertNotEquals(map1.hashCode(), map2.hashCode());
			assertNotEquals(map1, map2);
		}
		{
			ConcurrentNeutralValueMap<String, Float> map1 = new ConcurrentNeutralValueMap<>(Float.NaN);
			ConcurrentNeutralValueMap<String, Float> map2 = new ConcurrentNeutralValueMap<>(Float.NaN);
			map1.put("0", 0f);
			map1.put("NULL", null);
			map2.put("NULL", null);
			map2.put("0", 0f);
			assertEquals(map1.hashCode(), map2.hashCode());
			assertEquals(map1, map2);
		}
		{
			ConcurrentNeutralValueMap<String, Float> map1 = new ConcurrentNeutralValueMap<>(Float.NaN);
			ConcurrentNeutralValueMap<String, Float> map2 = new ConcurrentNeutralValueMap<>(Float.NaN);
			map1.put("0", 0f);
			map1.put("1", 1f);
			map1.put("NULL", null);
			map2.put("NULL", null);
			map2.put("0", 0f);
			assertNotEquals(map1.hashCode(), map2.hashCode());
			assertNotEquals(map1, map2);
		}
	}
}
