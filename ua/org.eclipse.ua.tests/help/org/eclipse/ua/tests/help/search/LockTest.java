/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
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
package org.eclipse.ua.tests.help.search;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.channels.OverlappingFileLockException;

import org.eclipse.help.internal.search.SearchIndex;
import org.junit.Test;

/**
 * Test locking of search index
 */
public class LockTest {
	@Test
	public void testSingleLock() {
		SearchIndex index1 = new SearchIndex(null, null, null);
		try {
			boolean try1 = index1.tryLock();
			assertTrue(try1);
		} catch (OverlappingFileLockException e1) {
			fail("Exception thrown");
		}
		index1.releaseLock();
	}

	@Test
	public void testCompetingLocks() {
		SearchIndex index1 = new SearchIndex(null, null, null);
		try {
			boolean try1 = index1.tryLock();
			assertTrue(try1);
		} catch (OverlappingFileLockException e1) {
			fail("Exception thrown");
		}
		SearchIndex index2 = new SearchIndex(null, null, null);
		try {
			boolean try2 = index2.tryLock();
			assertFalse(try2);
		} catch (OverlappingFileLockException e) {
			// Throwing this exception or returning false is the expected result
		}
		index1.releaseLock();
		index2.releaseLock();
	}

	@Test
	public void testNonCompetingLocks() {
		SearchIndex index1 = new SearchIndex(null, null, null);
		try {
			boolean try1 = index1.tryLock();
			assertTrue(try1);
		} catch (OverlappingFileLockException e1) {
			fail("Exception thrown");
		}
		index1.releaseLock();
		SearchIndex index2 = new SearchIndex(null, null, null);
		try {
			boolean try2 = index2.tryLock();
			assertTrue(try2);
		} catch (OverlappingFileLockException e) {
			fail("Exception thrown");
		}
		index2.releaseLock();
	}

}
