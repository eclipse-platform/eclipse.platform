/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.core.tests.filesystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.tests.filesystem.FileSystemTestUtil.getMonitor;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.internal.filesystem.ram.MemoryFileStore;
import org.eclipse.core.tests.internal.filesystem.ram.MemoryTree;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the file caching provided by FileStore.toLocalFile.
 */
public class FileCacheTest {

	/**
	 * Returns the byte[] contents of the given file.
	 */
	private byte[] getBytes(File cachedFile) throws FileNotFoundException, IOException {
		try (FileInputStream in = new FileInputStream(cachedFile)) {
			return in.readAllBytes();
		}
	}

	@Before
	public void setUp() throws Exception {
		MemoryTree.TREE.deleteAll();
	}

	@After
	public void tearDown() throws Exception {
		MemoryTree.TREE.deleteAll();
	}

	@Test
	public void testCacheFile() throws Exception {
		IFileStore store = new MemoryFileStore(IPath.fromOSString("testCacheFile"));
		byte[] contents = "test".getBytes();
		try (OutputStream out = store.openOutputStream(EFS.NONE, getMonitor())) {
			out.write(contents);
		}
		File cachedFile = store.toLocalFile(EFS.CACHE, getMonitor());
		assertTrue("1.0", cachedFile.exists());
		assertTrue("1.1", !cachedFile.isDirectory());
		assertArrayEquals("1.2", contents, getBytes(cachedFile));

		// write out new file contents
		byte[] newContents = "newContents".getBytes();
		try (OutputStream out = store.openOutputStream(EFS.NONE, getMonitor())) {
			out.write(newContents);
		}

		// old cache will be out of date
		assertThat(newContents).isNotEqualTo(getBytes(cachedFile));

		// fetching the cache again should return up to date file
		cachedFile = store.toLocalFile(EFS.CACHE, getMonitor());
		assertTrue("3.0", cachedFile.exists());
		assertTrue("3.1", !cachedFile.isDirectory());
		assertArrayEquals("3.2", newContents, getBytes(cachedFile));
	}

	@Test
	public void testCacheFolder() throws Exception {
		IFileStore store = new MemoryFileStore(IPath.fromOSString("testCacheFolder"));
		store.mkdir(EFS.NONE, getMonitor());
		File cachedFile = store.toLocalFile(EFS.CACHE, getMonitor());
		assertTrue("1.0", cachedFile.exists());
		assertTrue("1.1", cachedFile.isDirectory());
	}

	/**
	 * Tests invoking the toLocalFile method without the CACHE option flag.
	 */
	@Test
	public void testNoCacheFlag() throws Exception {
		IFileStore store = new MemoryFileStore(IPath.fromOSString("testNoCacheFlag"));
		store.mkdir(EFS.NONE, getMonitor());
		File cachedFile = store.toLocalFile(EFS.NONE, getMonitor());
		assertNull("1.0", cachedFile);
	}

	/**
	 * Tests caching a non-existing file
	 */
	@Test
	public void testNonExisting() throws Exception {
		IFileStore store = new MemoryFileStore(IPath.fromOSString("testNonExisting"));
		File cachedFile = store.toLocalFile(EFS.CACHE, getMonitor());
		assertTrue("1.0", !cachedFile.exists());
	}
}
