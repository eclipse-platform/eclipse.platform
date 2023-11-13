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

import static org.eclipse.core.tests.filesystem.FileSystemTestUtil.ensureDoesNotExist;
import static org.eclipse.core.tests.filesystem.FileSystemTestUtil.ensureExists;
import static org.eclipse.core.tests.filesystem.FileSystemTestUtil.getMonitor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URI;
import java.util.UUID;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.filesystem.FileStoreCreationRule.FileSystemType;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Black box testing of mkdir method.
 */
public class CreateDirectoryTest {
	protected IFileStore topDir, subDir, file, subFile;

	@Rule
	public final FileStoreCreationRule localFileStoreRule = new FileStoreCreationRule(FileSystemType.LOCAL);

	@Rule
	public final FileStoreCreationRule inMemoryFileStoreRule = new FileStoreCreationRule(FileSystemType.IN_MEMORY);

	@Before
	public void setUp() throws Exception {
		IFileStore baseStore = inMemoryFileStoreRule.getFileStore();
		baseStore.mkdir(EFS.NONE, null);
		topDir = baseStore.getChild("topDir");
		subDir = topDir.getChild("subDir");
		file = baseStore.getChild("file");
		subFile = file.getChild("subFile");
		ensureExists(topDir.getParent(), true);
		ensureDoesNotExist(topDir);
		ensureDoesNotExist(file);
	}

	@After
	public void tearDown() throws Exception {
		ensureDoesNotExist(topDir);
		ensureDoesNotExist(file);
	}

	@Test
	public void testParentExistsDeep() throws Exception {
		topDir.mkdir(EFS.NONE, getMonitor());
		IFileInfo info = topDir.fetchInfo();
		assertTrue("1.1", info.exists());
		assertTrue("1.2", info.isDirectory());
	}

	@Test
	public void testParentExistsShallow() throws Exception {
		topDir.mkdir(EFS.SHALLOW, getMonitor());
		IFileInfo info = topDir.fetchInfo();
		assertTrue("2.1", info.exists());
		assertTrue("2.2", info.isDirectory());
	}

	@Test
	public void testParentFileDeep() throws Exception {
		ensureExists(file, false);
		assertThrows(CoreException.class, () -> subFile.mkdir(EFS.NONE, getMonitor()));
		IFileInfo info = subFile.fetchInfo();
		assertTrue("2.1", !info.exists());
		assertTrue("2.2", !info.isDirectory());
	}

	@Test
	public void testParentFileShallow() throws Exception {
		ensureExists(file, false);
		assertThrows(CoreException.class, () -> subFile.mkdir(EFS.SHALLOW, getMonitor()));
		IFileInfo info = subFile.fetchInfo();
		assertTrue("2.1", !info.exists());
		assertTrue("2.2", !info.isDirectory());
	}

	@Test
	public void testParentNotExistsDeep() throws Exception {
		subDir.mkdir(EFS.NONE, getMonitor());
		IFileInfo info = topDir.fetchInfo();
		assertTrue("1.1", info.exists());
		assertTrue("1.2", info.isDirectory());
		info = subDir.fetchInfo();
		assertTrue("1.3", info.exists());
		assertTrue("1.4", info.isDirectory());
	}

	@Test
	public void testParentNotExistsShallow() {
		assertThrows(CoreException.class, () -> subDir.mkdir(EFS.SHALLOW, getMonitor()));
		IFileInfo info = topDir.fetchInfo();
		assertTrue("1.1", !info.exists());
		assertTrue("1.2", !info.isDirectory());
		info = subDir.fetchInfo();
		assertTrue("1.3", !info.exists());
		assertTrue("1.4", !info.isDirectory());
	}

	@Test
	public void testParentNotExistsShallowInLocalFile() throws CoreException {
		IFileStore localFileBaseStore = localFileStoreRule.getFileStore();
		localFileBaseStore.delete(EFS.NONE, getMonitor());
		CoreException e = assertThrows(CoreException.class, () -> {
			IFileStore localFileTopDir = localFileBaseStore.getChild("topDir");
			localFileTopDir.mkdir(EFS.SHALLOW, getMonitor());
		});
		assertNotNull("1.1", e.getStatus());
		assertEquals("1.2", EFS.ERROR_NOT_EXISTS, e.getStatus().getCode());
	}

	@Test
	public void testTargetIsFileInLocalFile() throws Exception {
		IFileStore localFileBaseStore = localFileStoreRule.getFileStore();
		localFileBaseStore.delete(EFS.NONE, getMonitor());
		CoreException e = assertThrows(CoreException.class, () -> {
			ensureExists(localFileBaseStore, true);
			IFileStore localFileTopDir = localFileBaseStore.getChild("topDir");
			ensureExists(localFileTopDir, false);
			localFileTopDir.mkdir(EFS.SHALLOW, getMonitor());
			fail("1.99");
		});
		assertNotNull("1.1", e.getStatus());
		assertEquals("1.2", EFS.ERROR_WRONG_TYPE, e.getStatus().getCode());
	}

	@Test
	public void testParentDeviceNotExistsInLocalFile() {
		if (!Platform.getOS().equals(Platform.OS_WIN32)) {
			return;
		}
		String device = findNonExistingDevice();
		if (device == null) {
			return;
		}

		try {
			IFileStore localFileTopDir = EFS.getStore(URI.create("file:/" + device + ":" + UUID.randomUUID()));
			localFileTopDir.mkdir(EFS.SHALLOW, getMonitor());
			fail("1.99");
		} catch (CoreException e) {
			assertNotNull("1.1", e.getStatus());
			assertEquals("1.2", EFS.ERROR_WRITE, e.getStatus().getCode());
		}
	}

	private String findNonExistingDevice() {
		String device = null;
		for (int i = 97/*a*/; i < 123/*z*/; i++) {
			char c = (char) i;
			if (!new File(c + ":\\").exists()) {
				device = "" + c;
				break;
			}
		}
		return device;
	}
}
