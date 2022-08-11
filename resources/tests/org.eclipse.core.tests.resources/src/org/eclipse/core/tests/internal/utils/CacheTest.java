/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
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
package org.eclipse.core.tests.internal.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.internal.utils.Cache;
import org.junit.Test;

public class CacheTest {
	@Test
	public void testBasic() {
		Cache cache = new Cache(1);
		cache.addEntry("foo", "foo");
		Cache.Entry foo = cache.getEntry("foo", false);
		assertNotNull("1.0", foo);
		assertTrue("2.1", foo.isTail());
		assertTrue("2.2", foo.isHead());
		assertEquals("2.4", foo, cache.getTail());
		assertEquals("2.5", foo, cache.getHead());
	}

	@Test
	public void testBasic2() {
		Cache cache = new Cache(2);
		cache.addEntry("foo", "foo");
		cache.addEntry("bar", "bar");
		Cache.Entry foo = cache.getEntry("foo", false);
		Cache.Entry bar = cache.getEntry("bar", false);
		assertNotNull("1.0", foo);
		assertNotNull("1.1", bar);
		assertTrue("2.0", bar.isHead());
		assertTrue("2.1", foo.isTail());
		assertEquals("2.4", bar, cache.getHead());
		assertEquals("2.5", foo, cache.getTail());
		assertFalse("2.8", bar.isTail());
		assertFalse("2.9", foo.isHead());
		assertEquals("3.0", foo, bar.getNext());
		assertEquals("3.1", bar, foo.getPrevious());
	}

	@Test
	public void testUpdate() {
		Cache cache = new Cache(2);
		cache.addEntry("foo", "foo");
		cache.addEntry("bar", "bar");
		Cache.Entry foo = cache.getEntry("foo", true);
		Cache.Entry bar = cache.getEntry("bar", false);
		assertNotNull("1.0", foo);
		assertNotNull("1.1", bar);
		assertTrue("2.0", foo.isHead());
		assertTrue("2.1", bar.isTail());
		assertEquals("2.4", foo, cache.getHead());
		assertEquals("2.5", bar, cache.getTail());
		assertFalse("2.8", foo.isTail());
		assertFalse("2.9", bar.isHead());
		assertEquals("3.0", foo, bar.getPrevious());
		assertEquals("3.1", bar, foo.getNext());
		bar = cache.getEntry("bar", true);
		assertNotNull("4.1", bar);
		assertTrue("5.0", bar.isHead());
		assertTrue("5.1", foo.isTail());
		assertEquals("5.4", bar, cache.getHead());
		assertEquals("5.5", foo, cache.getTail());
		assertFalse("5.8", bar.isTail());
		assertFalse("5.9", foo.isHead());
		assertEquals("6.0", foo, bar.getNext());
		assertEquals("6.1", bar, foo.getPrevious());
	}

	@Test
	public void testDiscardAll() {
		Cache cache = new Cache(2);
		assertNull("1.0", cache.getHead());
		assertNull("1.1", cache.getTail());
		assertEquals("1.2", 0, cache.size());

		cache.addEntry("foo", "foo");
		cache.addEntry("bar", "bar");
		cache.discardAll();
		assertNull("2.0", cache.getHead());
		assertNull("2.1", cache.getTail());
		assertEquals("2.2", 0, cache.size());
	}

	@Test
	public void testDiscardHead() {
		Cache cache = new Cache(2);
		cache.addEntry("foo", "foo");
		cache.addEntry("bar", "bar");
		Cache.Entry foo = cache.getEntry("foo", true);
		Cache.Entry bar = cache.getEntry("bar", true);
		bar.discard();
		assertNull("1.0", cache.getEntry("bar"));
		assertTrue("2.0", foo.isHead());
		assertTrue("2.1", foo.isTail());
		assertEquals("2.4", foo, cache.getHead());
		assertEquals("2.5", foo, cache.getTail());
		foo.discard();
		assertNull("3.0", cache.getEntry("foo"));
		assertNull("3.1", cache.getHead());
		assertNull("3.2", cache.getTail());
	}

	@Test
	public void testCacheLimit() {
		Cache cache = new Cache(1, 3, 0.33);
		cache.addEntry("foo", "foo");
		cache.addEntry("bar", "bar");
		cache.addEntry("zoo", "zoo");
		cache.addEntry("fred", "fred");
		cache.addEntry("zar", "zar");
		assertEquals("3.0", 3, cache.size());
		assertNull("3.1", cache.getEntry("foo"));
		assertNull("3.2", cache.getEntry("bar"));
		assertNotNull("3.3", cache.getEntry("zoo", false));
		assertNotNull("3.4", cache.getEntry("fred", false));
		assertNotNull("3.5", cache.getEntry("zar", false));
		// force fred to go up in the list
		cache.getEntry("fred");
		cache.addEntry("foo", "foo");
		cache.addEntry("bar", "bar");
		assertEquals("4.0", 3, cache.size());
		assertNotNull("4.1", cache.getEntry("foo", false));
		assertNotNull("4.2", cache.getEntry("bar", false));
		assertNull("4.3", cache.getEntry("zoo", false));
		assertNotNull("4.4", cache.getEntry("fred", false));
		assertNull("4.5", cache.getEntry("zar", false));
	}

}
