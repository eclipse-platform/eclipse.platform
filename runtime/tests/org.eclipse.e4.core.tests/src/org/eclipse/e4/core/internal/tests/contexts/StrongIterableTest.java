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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

import org.eclipse.e4.core.internal.contexts.StrongIterable;
import org.junit.Test;

public class StrongIterableTest {

	@Test
	public void testStrongIterableDeque() {
		testStrongIterable(ConcurrentLinkedDeque::new);
	}

	@Test
	public void testStrongIterableQueue() {
		testStrongIterable(ConcurrentLinkedQueue::new);
	}

	void testStrongIterable(Supplier<Collection<Reference<Integer>>> constructor) {
		testIterate(constructor.get());
		testRemoveWhileIterate(constructor.get());
		testRemoveAll(constructor.get());
		testRemoveByClear(constructor.get());
		testNextWithoutHasNext(constructor.get());
	}

	private void testNextWithoutHasNext(Collection<Reference<Integer>> iterable) {
		WeakReference<Integer> EMPTY1 = new WeakReference<>(null);
		WeakReference<Integer> EMPTY2 = new WeakReference<>(null);
		WeakReference<Integer> ONE = new WeakReference<>(1);
		WeakReference<Integer> TWO = new WeakReference<>(2);
		WeakReference<Integer> THREE = new WeakReference<>(3);
		iterable.add(EMPTY1); // ignored
		iterable.add(ONE);
		iterable.add(TWO);
		iterable.add(EMPTY2); // ignored
		iterable.add(THREE);
		StrongIterable<Integer> strongIterable = new StrongIterable<>(iterable);
		{
			Iterator<Integer> i = strongIterable.iterator();
			assertEquals(1, i.next().intValue());
			assertEquals(2, i.next().intValue());
			assertEquals(3, i.next().intValue());
			try {
				i.next();
			} catch (NoSuchElementException e) {
				assertNotNull(e);
			}
		}
		{
			Iterator<Integer> i = strongIterable.iterator();
			assertEquals(1, i.next().intValue());
			assertEquals(3L, count(strongIterable));
			assertEquals(3L, poorMansCount(strongIterable));
			i.remove();
			assertEquals(2L, count(strongIterable));
			assertEquals(2L, poorMansCount(strongIterable));
			assertEquals(2, i.next().intValue());
			i.remove();
			assertEquals(3, i.next().intValue());
			i.remove();
			try {
				i.next();
			} catch (NoSuchElementException e) {
				assertNotNull(e);
			}
		}
		assertEquals(0L, count(strongIterable));
		assertEquals(0L, poorMansCount(strongIterable));
	}

	void testRemoveWhileIterate(Collection<Reference<Integer>> iterable) {
		WeakReference<Integer> ONE = new WeakReference<>(1);
		WeakReference<Integer> TWO = new WeakReference<>(2);
		WeakReference<Integer> THREE = new WeakReference<>(3);
		iterable.add(ONE);
		iterable.add(TWO);
		iterable.add(THREE);

		assertEquals(3L, poorMansCount(iterable));
		StrongIterable<Integer> strongIterable = new StrongIterable<>(iterable);
		{
			Iterator<Integer> i = strongIterable.iterator();
			assertEquals(3L, count(strongIterable));
			assertEquals(3L, poorMansCount(strongIterable));

			assertTrue(i.hasNext());
			assertEquals(1, i.next().intValue());
			assertTrue(i.hasNext());
			assertEquals(2, i.next().intValue());
			i.remove(); // remove TWO
			assertEquals(2L, count(strongIterable));
			assertEquals(2L, poorMansCount(strongIterable));
			assertTrue(i.hasNext());
			assertEquals(3, i.next().intValue());
			i.remove(); // remove THREE
			assertEquals(1L, count(strongIterable));
			assertEquals(1L, poorMansCount(strongIterable));
			assertFalse(i.hasNext());
			try {
				i.next();
			} catch (NoSuchElementException e) {
				assertNotNull(e);
			}
		}
	}

	void testRemoveAll(Collection<Reference<Integer>> iterable) {
		WeakReference<Integer> ONE = new WeakReference<>(1);
		WeakReference<Integer> TWO = new WeakReference<>(2);
		WeakReference<Integer> THREE = new WeakReference<>(3);
		iterable.add(ONE);
		iterable.add(TWO);
		iterable.add(THREE);

		StrongIterable<Integer> strongIterable = new StrongIterable<>(iterable);
		assertEquals(3L, count(strongIterable));
		for (Iterator<Integer> i = strongIterable.iterator(); i.hasNext();) {
			i.next();
			i.remove();
		}
		assertEquals(0L, count(strongIterable));
		assertEquals(0L, poorMansCount(strongIterable));
	}

	void testRemoveByClear(Collection<Reference<Integer>> iterable) {
		WeakReference<Integer> ONE = new WeakReference<>(1);
		WeakReference<Integer> TWO = new WeakReference<>(2);
		WeakReference<Integer> THREE = new WeakReference<>(3);
		iterable.add(ONE);
		iterable.add(TWO);
		iterable.add(THREE);

		StrongIterable<Integer> strongIterable = new StrongIterable<>(iterable);
		assertEquals(3L, count(strongIterable));
		for (Reference<Integer> ref : iterable) {
			ref.clear();
		}
		assertEquals(0L, count(strongIterable));
	}

	void testIterate(Collection<Reference<Integer>> iterable) {
		WeakReference<Integer> EMPTY1 = new WeakReference<>(null);
		WeakReference<Integer> EMPTY2 = new WeakReference<>(null);
		WeakReference<Integer> ONE = new WeakReference<>(1);
		WeakReference<Integer> TWO = new WeakReference<>(2);
		WeakReference<Integer> THREE = new WeakReference<>(3);
		iterable.add(EMPTY1); // ignored
		iterable.add(ONE);
		iterable.add(TWO);
		iterable.add(EMPTY2); // ignored
		iterable.add(THREE);

		StrongIterable<Integer> strongIterable = new StrongIterable<>(iterable);
		{
			Iterator<Integer> i = strongIterable.iterator();
			assertEquals(3L, count(strongIterable));
			assertTrue(i.hasNext());
			assertEquals(1, i.next().intValue());
			assertTrue(i.hasNext());
			assertEquals(2, i.next().intValue());
			assertTrue(i.hasNext());
			assertEquals(3, i.next().intValue());
			assertFalse(i.hasNext());
			try {
				i.next();
			} catch (NoSuchElementException e) {
				assertNotNull(e);
			}
		}
		for (Integer strong : strongIterable) {
			assertNotNull(strong);
		}

		assertTrue(iterable.remove(TWO)); // remove

		for (Integer strong : strongIterable) {
			assertNotNull(strong);
		}
		{
			Iterator<Integer> i = strongIterable.iterator();
			assertEquals(2L, count(strongIterable));
			assertTrue(i.hasNext());
			assertEquals(1, i.next().intValue());
			assertTrue(i.hasNext());
			assertEquals(3, i.next().intValue());
			assertFalse(i.hasNext());
			try {
				i.next();
			} catch (NoSuchElementException e) {
				assertNotNull(e);
			}
		}

		ONE.clear(); // remove on iterate

		{
			Iterator<Integer> i = strongIterable.iterator();
			assertEquals(1L, count(strongIterable));
			assertTrue(i.hasNext());
			assertEquals(3, i.next().intValue());
			assertFalse(i.hasNext());
			try {
				i.next();
			} catch (NoSuchElementException e) {
				assertNotNull(e);
			}
			for (Integer strong : strongIterable) {
				assertNotNull(strong);
			}
		}

		THREE.clear(); // remove on iterate

		{
			Iterator<Integer> i = strongIterable.iterator();
			assertEquals(0L, count(strongIterable));
			assertFalse(i.hasNext());
			try {
				i.next();
			} catch (NoSuchElementException e) {
				assertNotNull(e);
			}
			for (Integer strong : strongIterable) {
				assertNotNull(strong);
			}
		}
	}

	private long count(Iterable<?> i) {
		return StreamSupport.stream(i.spliterator(), false).count();
	}

	private long poorMansCount(Iterable<?> i) {
		long count = -1;
		try {
			for (Iterator<?> it = i.iterator();; it.next()) {
				count++;
			}
		} catch (NoSuchElementException expected) {
		}
		return count;
	}
}
