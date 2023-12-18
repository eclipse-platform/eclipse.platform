/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
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
package org.eclipse.core.tests.internal.localstore;

import static org.eclipse.core.tests.harness.FileSystemHelper.getRandomLocation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.eclipse.core.internal.localstore.Bucket.Entry;
import org.eclipse.core.internal.localstore.HistoryBucket;
import org.eclipse.core.internal.utils.UniversalUniqueIdentifier;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.Rule;
import org.junit.Test;

public class HistoryBucketTest {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	/**
	 * Ensures that if another entry having exactly the same UUID is added,
	 * the original one is not replaced.
	 */
	@Test
	public void testDuplicates() throws CoreException {
		IPath baseLocation = getRandomLocation();
		workspaceRule.deleteOnTearDown(baseLocation);
		HistoryBucket index1 = new HistoryBucket();
		IPath location1 = baseLocation.append("location1");
		index1.load("foo", location1.toFile());
		IPath path = IPath.fromOSString("/foo/bar");
		UniversalUniqueIdentifier uuid = new UniversalUniqueIdentifier();
		long lastModified = (long) (Math.random() * Long.MAX_VALUE);
		index1.addBlob(path, uuid, lastModified);
		assertEquals(1, index1.getEntryCount());
		index1.addBlob(path, uuid, lastModified);
		assertEquals(1, index1.getEntryCount());
		HistoryBucket.HistoryEntry entry = index1.getEntry(path);
		assertNotNull(entry);
		assertEquals(path, entry.getPath());
		assertEquals(1, entry.getOccurrences());
		assertEquals(uuid, entry.getUUID(0));
		assertEquals(lastModified, entry.getTimestamp(0));
	}

	@Test
	public void testPersistence() throws CoreException {
		IPath baseLocation = getRandomLocation();
		workspaceRule.deleteOnTearDown(baseLocation);
		HistoryBucket index1 = new HistoryBucket();
		IPath location = baseLocation.append("location");
		index1.load("foo", location.toFile());
		assertEquals(0, index1.getEntryCount());
		IPath path = IPath.fromOSString("/foo/bar");
		UniversalUniqueIdentifier uuid1 = new UniversalUniqueIdentifier();
		long lastModified1 = (long) (Math.random() * Long.MAX_VALUE);
		index1.addBlob(path, uuid1, lastModified1);
		assertEquals(1, index1.getEntryCount());
		index1.save();
		HistoryBucket index2 = new HistoryBucket();
		index2.load("foo", location.toFile(), false);
		assertEquals(1, index1.getEntryCount());
		HistoryBucket.HistoryEntry entry = index1.getEntry(path);
		assertNotNull(entry);
		assertEquals(path, entry.getPath());
		assertEquals(1, entry.getOccurrences());
		assertEquals(uuid1, entry.getUUID(0));
		assertEquals(lastModified1, entry.getTimestamp(0));
		UniversalUniqueIdentifier uuid2 = new UniversalUniqueIdentifier();
		long lastModified2 = (long) Math.abs((Math.random() * Long.MAX_VALUE));
		index2.addBlob(path, uuid2, lastModified2);
		index2.save();
		index1.load("foo", location.toFile(), true);
		assertEquals(1, index1.getEntryCount());
		entry = index1.getEntry(path);
		assertNotNull(entry);
		assertEquals(path, entry.getPath());
		assertEquals(2, entry.getOccurrences());

		// test deletion
		index1.accept(new HistoryBucket.Visitor() {
			@Override
			public int visit(Entry fileEntry) {
				fileEntry.delete();
				return CONTINUE;
			}
		}, path, 0);
		entry = index1.getEntry(path);
		assertNull(entry);
		index2.load("foo", location.toFile(), true);
		entry = index2.getEntry(path);
		assertNull(entry);
	}

	/**
	 * This test does not cause any data to be written.
	 */
	@Test
	public void testSort() {
		HistoryBucket index = new HistoryBucket();
		IPath path = IPath.fromOSString("/foo");
		assertNull(index.getEntry(path));
		UniversalUniqueIdentifier uuid1 = new UniversalUniqueIdentifier();
		long timestamp1 = 10;
		index.addBlob(path, uuid1, timestamp1);
		HistoryBucket.HistoryEntry entry = index.getEntry(path);
		assertNotNull(entry);
		assertEquals(1, entry.getOccurrences());
		assertEquals(uuid1, entry.getUUID(0));
		assertEquals(timestamp1, entry.getTimestamp(0));
		// adds a new state with a more recent timestamp
		UniversalUniqueIdentifier uuid2 = new UniversalUniqueIdentifier();
		long timestamp2 = timestamp1 + 1;
		index.addBlob(path, uuid2, timestamp2);
		entry = index.getEntry(path);
		assertNotNull(entry);
		// since it is newer, should appear first
		assertEquals(2, entry.getOccurrences());
		assertEquals(uuid2, entry.getUUID(0));
		assertEquals(timestamp2, entry.getTimestamp(0));
		assertEquals(uuid1, entry.getUUID(1));
		assertEquals(timestamp1, entry.getTimestamp(1));
		// adds a 3rd state, with the same timestamp as the 1st
		UniversalUniqueIdentifier uuid3 = new UniversalUniqueIdentifier();
		long timestamp3 = timestamp1;
		index.addBlob(path, uuid3, timestamp3);
		entry = index.getEntry(path);
		assertNotNull(entry);
		// its UUID was created later so it will be considered more recent
		// even if it has the same timestamp
		assertEquals(3, entry.getOccurrences());
		assertEquals(uuid2, entry.getUUID(0));
		assertEquals(timestamp2, entry.getTimestamp(0));
		assertEquals(uuid3, entry.getUUID(1));
		assertEquals(timestamp3, entry.getTimestamp(1));
		assertEquals(uuid1, entry.getUUID(2));
		assertEquals(timestamp1, entry.getTimestamp(2));
	}

}
