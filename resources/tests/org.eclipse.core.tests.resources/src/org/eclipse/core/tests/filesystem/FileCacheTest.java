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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.internal.filesystem.ram.MemoryFileStore;
import org.eclipse.core.tests.internal.filesystem.ram.MemoryTree;

/**
 * Tests the file caching provided by FileStore.toLocalFile.
 */
public class FileCacheTest extends FileSystemTest {

	/**
	 * Overrides generic method from Assert to perform proper array equality test.
	 */
	public void assertEquals(String message, byte[] expected, byte[] actual) {
		if (expected.length != actual.length) {
			fail(message + " arrays of different length");
		}
		assertEquals(message + " different length", expected.length, actual.length);
		for (int i = 0; i < actual.length; i++) {
			if (expected[i] != actual[i]) {
				fail(message + " arrays differ at position " + i + "; expected: " + expected[i] + " but was: " + actual[i]);
			}
		}
	}

	/**
	 * Overrides generic method from Assert to perform proper array equality test.
	 */
	public void assertNotSame(String message, byte[] expected, byte[] actual) {
		if (expected.length != actual.length) {
			return;
		}
		for (int i = 0; i < actual.length; i++) {
			if (expected[i] != actual[i]) {
				return;
			}
		}
		fail(message + " arrays should be different, but they are not: " + Arrays.toString(expected));
	}

	/**
	 * Returns the byte[] contents of the given file.
	 */
	private byte[] getBytes(File cachedFile) {
		FileInputStream in = null;
		ByteArrayOutputStream out = null;
		try {
			in = new FileInputStream(cachedFile);
			out = new ByteArrayOutputStream();
			transferData(in, out);
			in.close();
			out.close();
			return out.toByteArray();
		} catch (IOException e) {
			fail("Exception in FileCacheTest.getBytes", e);
		}
		return new byte[0];
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		MemoryTree.TREE.deleteAll();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		MemoryTree.TREE.deleteAll();
	}

	public void testCacheFile() {
		try {
			IFileStore store = new MemoryFileStore(IPath.fromOSString("testCacheFile"));
			OutputStream out = store.openOutputStream(EFS.NONE, getMonitor());
			byte[] contents = "test".getBytes();
			out.write(contents);
			out.close();
			File cachedFile = store.toLocalFile(EFS.CACHE, getMonitor());
			assertTrue("1.0", cachedFile.exists());
			assertTrue("1.1", !cachedFile.isDirectory());
			assertEquals("1.2", contents, getBytes(cachedFile));

			//write out new file contents
			byte[] newContents = "newContents".getBytes();
			out = store.openOutputStream(EFS.NONE, getMonitor());
			out.write(newContents);
			out.close();

			//old cache will be out of date
			assertNotSame("2.0", newContents, getBytes(cachedFile));

			//fetching the cache again should return up to date file
			cachedFile = store.toLocalFile(EFS.CACHE, getMonitor());
			assertTrue("3.0", cachedFile.exists());
			assertTrue("3.1", !cachedFile.isDirectory());
			assertEquals("3.2", newContents, getBytes(cachedFile));

		} catch (IOException | CoreException e) {
			fail("1.99", e);
		}
	}

	public void testCacheFolder() {
		try {
			IFileStore store = new MemoryFileStore(IPath.fromOSString("testCacheFolder"));
			store.mkdir(EFS.NONE, getMonitor());
			File cachedFile = store.toLocalFile(EFS.CACHE, getMonitor());
			assertTrue("1.0", cachedFile.exists());
			assertTrue("1.1", cachedFile.isDirectory());
		} catch (CoreException e) {
			fail("1.99", e);
		}
	}

	/**
	 * Tests invoking the toLocalFile method without the CACHE option flag.
	 */
	public void testNoCacheFlag() {
		try {
			IFileStore store = new MemoryFileStore(IPath.fromOSString("testNoCacheFlag"));
			store.mkdir(EFS.NONE, getMonitor());
			File cachedFile = store.toLocalFile(EFS.NONE, getMonitor());
			assertNull("1.0", cachedFile);
		} catch (CoreException e) {
			fail("4.99", e);
		}
	}

	/**
	 * Tests caching a non-existing file
	 */
	public void testNonExisting() {
		try {
			IFileStore store = new MemoryFileStore(IPath.fromOSString("testNonExisting"));
			File cachedFile = store.toLocalFile(EFS.CACHE, getMonitor());
			assertTrue("1.0", !cachedFile.exists());
		} catch (CoreException e) {
			fail("4.99", e);
		}
	}
}
