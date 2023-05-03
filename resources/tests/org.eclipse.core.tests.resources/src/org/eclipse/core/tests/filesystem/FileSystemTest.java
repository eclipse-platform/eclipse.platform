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

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.harness.CoreTest;
import org.eclipse.core.tests.harness.FileSystemHelper;
import org.eclipse.core.tests.internal.filesystem.ram.MemoryTree;
import org.junit.After;
import org.junit.Before;

/**
 * Abstract superclass for all generic file system tests.
 */
public abstract class FileSystemTest extends CoreTest {
	protected IFileStore baseStore, localFileBaseStore;

	public FileSystemTest() {
		super();
	}

	public FileSystemTest(String name) {
		super(name);
	}

	/**
	 * Bridge method to be able to run subclasses with JUnit4 as well as with
	 * JUnit3.
	 *
	 * @throws Exception
	 *             comes from {@link #setUp()}
	 */
	@Before
	public final void before() throws Exception {
		setUp();
	}

	/**
	 * Bridge method to be able to run subclasses with JUnit4 as well as with
	 * JUnit3.
	 *
	 * @throws Exception
	 *             comes from {@link #tearDown()}
	 */
	@After
	public final void after() throws Exception {
		tearDown();
	}

	protected void ensureDoesNotExist(IFileStore store) {
		try {
			store.delete(EFS.NONE, getMonitor());
			assertTrue("1.0", !store.fetchInfo().exists());
		} catch (CoreException e) {
			fail("ensureDoesNotExist", e);
		}
	}

	/**
	 * Asserts that a file store exists.
	 *
	 * @param message The failure message if the assertion fails
	 * @param store The store to check for existence
	 */
	protected void assertExists(String message, IFileStore store) {
		IFileInfo info = store.fetchInfo();
		assertTrue(message, info.exists());
		//check that the parent knows about it
		try {
			IFileInfo[] children = store.getParent().childInfos(EFS.NONE, getMonitor());
			for (IFileInfo element : children) {
				if (element.getName().equals(store.getName())) {
					return;
				}
			}
			assertTrue(message, false);
		} catch (CoreException e) {
			fail(message, e);
		}
	}

	/**
	 * Ensures that the provided store exists, as either a file or directory.
	 */
	protected void ensureExists(IFileStore store, boolean directory) {
		try {
			if (directory) {
				store.mkdir(EFS.NONE, getMonitor());
				final IFileInfo info = store.fetchInfo();
				assertTrue("1.0", info.exists());
				assertTrue("1.1", info.isDirectory());
			} else {
				try (OutputStream out = store.openOutputStream(EFS.NONE, getMonitor())) {
					out.write(5);
				}
				final IFileInfo info = store.fetchInfo();
				assertTrue("1.5", info.exists());
				assertTrue("1.6", !info.isDirectory());
			}
		} catch (CoreException | IOException e) {
			fail("ensureExists", e);
		}

	}

	/**
	 * Checks whether the local file system supports accessing and modifying the given attribute.
	 */
	protected boolean isAttributeSupported(int attribute) {
		return (EFS.getLocalFileSystem().attributes() & attribute) != 0;
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		doFSSetUp();
		localFileBaseStore = EFS.getLocalFileSystem().getStore(FileSystemHelper.getRandomLocation(getTempDir()));
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		localFileBaseStore.delete(EFS.NONE, null);
		doFSTearDown();
	}

	/**
	 * The base file system to be tested is setup here.
	 * The default implementation sets up in-memory file system (@see MemoryFileSystem).
	 * <p>
	 * Subclasses should override to test a different file system
	 * implementation and set up its base directory.
	 * </p>
	 */
	protected void doFSSetUp() throws Exception {
		MemoryTree.TREE.deleteAll();
		baseStore = EFS.getStore(URI.create("mem:/baseStore"));
		baseStore.mkdir(EFS.NONE, null);
	}

	/**
	 * Tear down the tested base file system and base directory here.
	 * The default implementation tears down in memory file system (@see MemoryFileSystem).
	 * <p>
	 * Subclasses should override to tear down a different file system
	 * implementation and its base directory.
	 * </p>
	 */
	protected void doFSTearDown() throws Exception {
		baseStore.delete(EFS.NONE, null);
		MemoryTree.TREE.deleteAll();
	}
}
