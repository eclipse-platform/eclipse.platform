/*******************************************************************************
 *  Copyright (c) 2005, 2025 IBM Corporation and others.
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.internal.filesystem.ram.MemoryFileStore;
import org.eclipse.core.tests.internal.filesystem.ram.MemoryTree;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the file caching provided by FileStore.toLocalFile.
 */
public class FileCacheTest {

	@BeforeEach
	public void setUp() throws Exception {
		MemoryTree.TREE.deleteAll();
	}

	@AfterEach
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
		assertTrue(cachedFile.exists());
		assertFalse(cachedFile.isDirectory());
		assertThat(Files.readAllBytes(cachedFile.toPath())).containsExactly(contents);

		// write out new file contents
		byte[] newContents = "newContents".getBytes();
		try (OutputStream out = store.openOutputStream(EFS.NONE, getMonitor())) {
			out.write(newContents);
		}

		// old cache will be out of date
		assertThat(newContents).isNotEqualTo(Files.readAllBytes(cachedFile.toPath()));

		// fetching the cache again should return up to date file
		cachedFile = store.toLocalFile(EFS.CACHE, getMonitor());
		assertTrue(cachedFile.exists());
		assertFalse(cachedFile.isDirectory());
		assertThat(Files.readAllBytes(cachedFile.toPath())).containsExactly(newContents);
	}

	@Test
	public void testCacheFolder() throws Exception {
		IFileStore store = new MemoryFileStore(IPath.fromOSString("testCacheFolder"));
		store.mkdir(EFS.NONE, getMonitor());
		File cachedFile = store.toLocalFile(EFS.CACHE, getMonitor());
		assertTrue(cachedFile.exists());
		assertTrue(cachedFile.isDirectory());
	}

	/**
	 * Tests invoking the toLocalFile method without the CACHE option flag.
	 */
	@Test
	public void testNoCacheFlag() throws Exception {
		IFileStore store = new MemoryFileStore(IPath.fromOSString("testNoCacheFlag"));
		store.mkdir(EFS.NONE, getMonitor());
		File cachedFile = store.toLocalFile(EFS.NONE, getMonitor());
		assertNull(cachedFile);
	}

	/**
	 * Tests caching a non-existing file
	 */
	@Test
	public void testNonExisting() throws Exception {
		IFileStore store = new MemoryFileStore(IPath.fromOSString("testNonExisting"));
		File cachedFile = store.toLocalFile(EFS.CACHE, getMonitor());
		assertFalse(cachedFile.exists());
	}
}
