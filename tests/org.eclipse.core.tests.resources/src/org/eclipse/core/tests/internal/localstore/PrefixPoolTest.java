/*******************************************************************************
 * Copyright (c) 2007, 2012 Wind River Systems, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Martin Oberhuber (Wind River) - initial API and implementation for [105554]
 *******************************************************************************/
package org.eclipse.core.tests.internal.localstore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.internal.localstore.PrefixPool;
import org.junit.Test;

public class PrefixPoolTest {

	@Test
	public void testIllegalCapacity() {
		boolean exceptionOK=true;
		try {
			new PrefixPool(0);
			exceptionOK=false;
		} catch(IllegalArgumentException e) {
			/*ignore, exception is expected*/
		}
		assertTrue(exceptionOK);
		try {
			new PrefixPool(-1);
			exceptionOK=false;
		} catch(IllegalArgumentException e) {
			/*ignore, exception is expected*/
		}
		assertTrue(exceptionOK);
	}

	@Test
	public void testPrefixPool() {
		PrefixPool p = new PrefixPool(1);
		assertFalse(p.containsAsPrefix(""));
		assertFalse(p.containsAsPrefix("/a"));
		assertFalse(p.hasPrefixOf("/a"));
		boolean rv=p.insertShorter("/a/");
		assertFalse(rv);
		p.insertLonger("/a/b/"); //overrides /a/
		assertEquals(1, p.size());
		p.insertLonger("/A/");
		assertEquals(2, p.size());
		p.insertLonger("/"); //shorter than /A/ --> no-op
		assertEquals(2, p.size());
		assertFalse(p.hasPrefixOf("/c"));
		rv=p.insertShorter("/a/b/c/"); //longer than /a/b/ --> no-op
		assertFalse(rv);
		assertEquals(2, p.size());
		p.insertLonger("/a/B/c/"); //no override
		assertEquals(3, p.size());
		p.insertShorter("/a/B/"); //overrides
		assertEquals(3, p.size());
		//we now have: /a/b/, /A/, /a/B/
		assertTrue(p.hasPrefixOf("/a/B/"));
		assertTrue(p.hasPrefixOf("/a/b/c/d/"));
		assertFalse(p.hasPrefixOf("/"));
		assertTrue(p.containsAsPrefix("/a/B"));
		assertTrue(p.containsAsPrefix("/a/b/"));
		assertTrue(p.containsAsPrefix(""));
		assertFalse(p.containsAsPrefix("/a/b//"));
		assertFalse(p.containsAsPrefix("/A/B/"));
		assertEquals(3, p.size());
		p.clear();
		assertEquals(0, p.size());
		assertFalse(p.hasPrefixOf(""));
		assertFalse(p.containsAsPrefix(""));
	}

}
