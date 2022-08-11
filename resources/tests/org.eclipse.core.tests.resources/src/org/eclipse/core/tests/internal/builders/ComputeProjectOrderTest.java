/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Mickael Istria (Red Hat Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.internal.builders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.*;
import java.util.function.Predicate;
import org.eclipse.core.internal.resources.ComputeProjectOrder;
import org.eclipse.core.internal.resources.ComputeProjectOrder.Digraph;
import org.eclipse.core.internal.resources.ComputeProjectOrder.Digraph.Edge;
import org.junit.Test;

public class ComputeProjectOrderTest {

	@Test
	public void testComputeVertexOrderDuration() {
		final Digraph<Object> digraph = new Digraph<>(Object.class);
		for (int i = 0; i < 320; i++) {
			Collection<Object> existingVertexes = new HashSet<>(digraph.vertexMap.keySet());
			Object newVertex = new Object();
			digraph.addVertex(newVertex);
			existingVertexes.forEach(existingVertex -> digraph.addEdge(existingVertex, newVertex));
		}
		long timestamp = System.currentTimeMillis();
		digraph.freeze();
		ComputeProjectOrder.computeVertexOrder(digraph, Object.class);
		long duration = System.currentTimeMillis() - timestamp;
		System.err.println(duration);
		assertTrue(duration < 1000);
	}

	@Test
	public void testFilterDigraphDuration() {
		final Digraph<Object> digraph = new Digraph<>(Object.class);
		Object initialVertex = new Object();
		digraph.addVertex(initialVertex);
		for (int i = 0; i < 320; i++) {
			Collection<Object> existingVertexes = new HashSet<>(digraph.vertexMap.keySet());
			Object newVertex = new Object();
			digraph.addVertex(newVertex);
			existingVertexes.forEach(existingVertex -> digraph.addEdge(existingVertex, newVertex));
		}
		// keep everything
		long timestamp = System.currentTimeMillis();
		Digraph<Object> filtered = ComputeProjectOrder.buildFilteredDigraph(digraph, o -> false, Object.class);
		assertEquals(digraph.vertexMap.keySet(), filtered.vertexMap.keySet());
		long duration = System.currentTimeMillis() - timestamp;
		System.err.println(duration);
		assertTrue(duration < 1000);
		// keep only 1 element
		timestamp = System.currentTimeMillis();
		filtered = ComputeProjectOrder.buildFilteredDigraph(digraph, o -> o != initialVertex, Object.class);
		assertEquals(Collections.singleton(initialVertex), filtered.vertexMap.keySet());
		duration = System.currentTimeMillis() - timestamp;
		System.err.println(duration);
		assertTrue(duration < 1000);
		// keep half elements
		timestamp = System.currentTimeMillis();
		Predicate<Object> removeOneOutOfTwo = new Predicate<>() {
			private int i = 0;

			@Override
			public boolean test(Object t) {
				return (i++) % 2 == 0;
			}
		};
		filtered = ComputeProjectOrder.buildFilteredDigraph(digraph, removeOneOutOfTwo, Object.class);
		duration = System.currentTimeMillis() - timestamp;
		System.err.println(duration);
		assertTrue(duration < 1000);
	}

	@Test
	public void testFilterDigraphBasic() {
		final Digraph<Object> digraph = new Digraph<>(Object.class);
		Object a = new Object();
		Object b = new Object();
		Object c = new Object();
		Object d = new Object();
		Object e = new Object();
		Object f = new Object();
		digraph.addVertex(a);
		digraph.addVertex(b);
		digraph.addEdge(a, b);
		digraph.addVertex(c);
		digraph.addEdge(b, c);
		digraph.addVertex(d);
		digraph.addEdge(c, d);
		digraph.addVertex(e);
		digraph.addEdge(d, e);
		digraph.addVertex(f);
		digraph.addEdge(e, f);
		//
		Digraph<Object> filtered = ComputeProjectOrder.buildFilteredDigraph(digraph, v -> v == b || v == e, Object.class);
		Set<Object> expectedVertexes = new HashSet<>(2, (float) 1.);
		expectedVertexes.add(a);
		expectedVertexes.add(c);
		expectedVertexes.add(d);
		expectedVertexes.add(f);
		assertEquals(expectedVertexes, filtered.vertexMap.keySet());
		Set<Edge<Object>> expectedEdges = new HashSet<>(3, (float) 1.);
		expectedEdges.add(new Edge<>(a, c));
		expectedEdges.add(new Edge<>(c, d));
		expectedEdges.add(new Edge<>(d, f));
		assertEquals(expectedEdges, filtered.getEdges());
	}

	@Test
	public void testFilterSelfReference() {
		final Digraph<String> digraph = new Digraph<>(String.class);
		String a = "a";
		String b = "b";
		String c = "c";
		digraph.addVertex(a);
		digraph.addVertex(b);
		digraph.addVertex(c);
		digraph.addEdge(a, b);
		digraph.addEdge(b, b);
		digraph.addEdge(b, c);
		//
		Digraph<String> filtered = ComputeProjectOrder.buildFilteredDigraph(digraph, b::equals, String.class);
		Set<Object> expectedVertexes = new HashSet<>(2, (float) 1.);
		expectedVertexes.add(a);
		expectedVertexes.add(c);
		assertEquals(expectedVertexes, filtered.vertexMap.keySet());
		Set<Edge<Object>> expectedEdges = Collections.singleton(new Edge<>(a, c));
		assertEquals(expectedEdges, filtered.getEdges());
	}

	@Test
	public void testCycle3Nodes() {
		final Digraph<String> digraph = new Digraph<>(String.class);
		String a = "a";
		String b = "b";
		String c = "c";
		String d = "d";
		String e = "e";
		digraph.addVertex(a);
		digraph.addVertex(b);
		digraph.addVertex(c);
		digraph.addVertex(d);
		digraph.addVertex(e);
		digraph.addEdge(a, b);
		digraph.addEdge(b, c);
		digraph.addEdge(c, d);
		digraph.addEdge(d, b);
		digraph.addEdge(d, e);
		// check some nodes from cycle can be removed
		{
			Digraph<String> filtered = ComputeProjectOrder.buildFilteredDigraph(digraph, b::equals, String.class);
			Set<Object> expectedVertexes = new HashSet<>(4, (float) 1.);
			expectedVertexes.add(a);
			expectedVertexes.add(c);
			expectedVertexes.add(d);
			expectedVertexes.add(e);
			assertEquals(expectedVertexes, filtered.vertexMap.keySet());
			Set<Edge<Object>> expectedEdges = new HashSet<>();
			expectedEdges.add(new Edge<>(a, c));
			expectedEdges.add(new Edge<>(d, e));
			expectedEdges.add(new Edge<>(d, c));
			expectedEdges.add(new Edge<>(c, d));
			assertEquals(expectedEdges, filtered.getEdges());
		}
		{
			Digraph<String> filtered = ComputeProjectOrder.buildFilteredDigraph(digraph, c::equals, String.class);
			Set<Object> expectedVertexes = new HashSet<>(4, (float) 1.);
			expectedVertexes.add(a);
			expectedVertexes.add(b);
			expectedVertexes.add(d);
			expectedVertexes.add(e);
			assertEquals(expectedVertexes, filtered.vertexMap.keySet());
			Set<Edge<Object>> expectedEdges = new HashSet<>();
			expectedEdges.add(new Edge<>(a, b));
			expectedEdges.add(new Edge<>(b, d));
			expectedEdges.add(new Edge<>(d, b));
			expectedEdges.add(new Edge<>(d, e));
			assertEquals(expectedEdges, filtered.getEdges());
		}
		// check whole cycle can be removed
		Digraph<String> filtered = ComputeProjectOrder.buildFilteredDigraph(digraph,
				node -> node.equals(b) || node.equals(c) || node.equals(d), String.class);
		Set<Object> expectedVertexes = new HashSet<>(4, (float) 1.);
		expectedVertexes.add(a);
		expectedVertexes.add(e);
		assertEquals(expectedVertexes, filtered.vertexMap.keySet());
		assertEquals(Collections.singleton(new Edge<>(a, e)), filtered.getEdges());
	}

	@Test
	public void testCycle2Nodes() {
		final Digraph<String> digraph = new Digraph<>(String.class);
		String a = "a";
		String b = "b";
		String c = "c";
		String d = "d";
		digraph.addVertex(a);
		digraph.addVertex(b);
		digraph.addVertex(c);
		digraph.addVertex(d);
		digraph.addEdge(a, b);
		digraph.addEdge(b, c);
		digraph.addEdge(c, b);
		digraph.addEdge(c, d);
		// check cycle nodes can be removed
		{
			Digraph<String> filtered = ComputeProjectOrder.buildFilteredDigraph(digraph,
					v -> v.equals(b) || v.equals(c), String.class);
			Set<Object> expectedVertexes = new HashSet<>(4, (float) 1.);
			expectedVertexes.add(a);
			expectedVertexes.add(d);
			assertEquals(expectedVertexes, filtered.vertexMap.keySet());
			assertEquals(Collections.singleton(new Edge<>(a, d)), filtered.getEdges());
		}
		// check removing another node keeps self reference
		Digraph<String> filtered = ComputeProjectOrder.buildFilteredDigraph(digraph, d::equals, String.class);
		Set<String> expectedVertexes = new HashSet<>(3, (float) 1.);
		expectedVertexes.add(a);
		expectedVertexes.add(b);
		expectedVertexes.add(c);
		assertEquals(expectedVertexes, filtered.vertexMap.keySet());
		Set<Edge<String>> expectedEdges = new HashSet<>(3, (float) 1.);
		expectedEdges.add(new Edge<>(a, b));
		expectedEdges.add(new Edge<>(b, c));
		expectedEdges.add(new Edge<>(c, b));
		assertEquals(expectedEdges, filtered.getEdges());
	}
}
