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

import static org.eclipse.core.tests.filesystem.FileSystemTestUtil.ensureDoesNotExist;
import static org.eclipse.core.tests.filesystem.FileSystemTestUtil.ensureExists;
import static org.eclipse.core.tests.filesystem.FileSystemTestUtil.getMonitor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.net.URI;
import java.util.UUID;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.filesystem.FileStoreCreationExtension.FileSystemType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Black box testing of mkdir method.
 */
public class CreateDirectoryTest {
	protected IFileStore topDir, subDir, file, subFile;

	@RegisterExtension
	public final FileStoreCreationExtension localFileStoreExtension = new FileStoreCreationExtension(FileSystemType.LOCAL);

	@RegisterExtension
	public final FileStoreCreationExtension inMemoryFileStoreExtension = new FileStoreCreationExtension(
			FileSystemType.IN_MEMORY);

	@BeforeEach
	public void setUp() throws Exception {
		IFileStore baseStore = inMemoryFileStoreExtension.getFileStore();
		baseStore.mkdir(EFS.NONE, null);
		topDir = baseStore.getChild("topDir");
		subDir = topDir.getChild("subDir");
		file = baseStore.getChild("file");
		subFile = file.getChild("subFile");
		ensureExists(topDir.getParent(), true);
		ensureDoesNotExist(topDir);
		ensureDoesNotExist(file);
	}

	@AfterEach
	public void tearDown() throws Exception {
		ensureDoesNotExist(topDir);
		ensureDoesNotExist(file);
	}

	@Test
	public void testParentExistsDeep() throws Exception {
		topDir.mkdir(EFS.NONE, getMonitor());
		IFileInfo info = topDir.fetchInfo();
		assertTrue(info.exists());
		assertTrue(info.isDirectory());
	}

	@Test
	public void testParentExistsShallow() throws Exception {
		topDir.mkdir(EFS.SHALLOW, getMonitor());
		IFileInfo info = topDir.fetchInfo();
		assertTrue(info.exists());
		assertTrue(info.isDirectory());
	}

	@Test
	public void testParentFileDeep() throws Exception {
		ensureExists(file, false);
		assertThrows(CoreException.class, () -> subFile.mkdir(EFS.NONE, getMonitor()));
		IFileInfo info = subFile.fetchInfo();
		assertFalse(info.exists());
		assertFalse(info.isDirectory());
	}

	@Test
	public void testParentFileShallow() throws Exception {
		ensureExists(file, false);
		assertThrows(CoreException.class, () -> subFile.mkdir(EFS.SHALLOW, getMonitor()));
		IFileInfo info = subFile.fetchInfo();
		assertFalse(info.exists());
		assertFalse(info.isDirectory());
	}

	@Test
	public void testParentNotExistsDeep() throws Exception {
		subDir.mkdir(EFS.NONE, getMonitor());
		IFileInfo info = topDir.fetchInfo();
		assertTrue(info.exists());
		assertTrue(info.isDirectory());
		info = subDir.fetchInfo();
		assertTrue(info.exists());
		assertTrue(info.isDirectory());
	}

	@Test
	public void testParentNotExistsShallow() {
		assertThrows(CoreException.class, () -> subDir.mkdir(EFS.SHALLOW, getMonitor()));
		IFileInfo info = topDir.fetchInfo();
		assertFalse(info.exists());
		assertFalse(info.isDirectory());
		info = subDir.fetchInfo();
		assertFalse(info.exists());
		assertFalse(info.isDirectory());
	}

	@Test
	public void testParentNotExistsShallowInLocalFile() throws CoreException {
		IFileStore localFileBaseStore = localFileStoreExtension.getFileStore();
		localFileBaseStore.delete(EFS.NONE, getMonitor());
		CoreException e = assertThrows(CoreException.class, () -> {
			IFileStore localFileTopDir = localFileBaseStore.getChild("topDir");
			localFileTopDir.mkdir(EFS.SHALLOW, getMonitor());
		});
		assertNotNull(e.getStatus());
		assertEquals(EFS.ERROR_NOT_EXISTS, e.getStatus().getCode());
	}

	@Test
	public void testTargetIsFileInLocalFile() throws Exception {
		IFileStore localFileBaseStore = localFileStoreExtension.getFileStore();
		localFileBaseStore.delete(EFS.NONE, getMonitor());
		CoreException e = assertThrows(CoreException.class, () -> {
			ensureExists(localFileBaseStore, true);
			IFileStore localFileTopDir = localFileBaseStore.getChild("topDir");
			ensureExists(localFileTopDir, false);
			localFileTopDir.mkdir(EFS.SHALLOW, getMonitor());
			fail("Should not be reached");
		});
		assertNotNull(e.getStatus());
		assertEquals(EFS.ERROR_WRONG_TYPE, e.getStatus().getCode());
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

		CoreException e = assertThrows(CoreException.class, () -> {
			IFileStore localFileTopDir = EFS.getStore(URI.create("file:/" + device + ":" + UUID.randomUUID()));
			localFileTopDir.mkdir(EFS.SHALLOW, getMonitor());
			fail("Should not be reached");
		});
		assertNotNull(e.getStatus());
		assertEquals(EFS.ERROR_WRITE, e.getStatus().getCode());
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
