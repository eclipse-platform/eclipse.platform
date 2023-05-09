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

import java.io.File;
import java.net.URI;
import org.eclipse.core.filesystem.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;

/**
 * Black box testing of mkdir method.
 */
public class CreateDirectoryTest extends FileSystemTest {
	protected IFileStore topDir, subDir, file, subFile;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		topDir = baseStore.getChild("topDir");
		subDir = topDir.getChild("subDir");
		file = baseStore.getChild("file");
		subFile = file.getChild("subFile");
		ensureExists(topDir.getParent(), true);
		ensureDoesNotExist(topDir);
		ensureDoesNotExist(file);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		ensureDoesNotExist(topDir);
		ensureDoesNotExist(file);
	}

	public void testParentExistsDeep() {
		try {
			topDir.mkdir(EFS.NONE, getMonitor());
		} catch (CoreException e) {
			fail("1.99", e);
		}
		IFileInfo info = topDir.fetchInfo();
		assertTrue("1.1", info.exists());
		assertTrue("1.2", info.isDirectory());
	}

	public void testParentExistsShallow() {
		try {
			topDir.mkdir(EFS.SHALLOW, getMonitor());
		} catch (CoreException e) {
			fail("2.99", e);
		}
		IFileInfo info = topDir.fetchInfo();
		assertTrue("2.1", info.exists());
		assertTrue("2.2", info.isDirectory());
	}

	public void testParentFileDeep() {
		ensureExists(file, false);
		try {
			subFile.mkdir(EFS.NONE, getMonitor());
			fail("1.99");
		} catch (CoreException e) {
			//should fail
		}
		IFileInfo info = subFile.fetchInfo();
		assertTrue("2.1", !info.exists());
		assertTrue("2.2", !info.isDirectory());
	}

	public void testParentFileShallow() {
		ensureExists(file, false);
		try {
			subFile.mkdir(EFS.SHALLOW, getMonitor());
			fail("1.99");
		} catch (CoreException e) {
			//should fail
		}
		IFileInfo info = subFile.fetchInfo();
		assertTrue("2.1", !info.exists());
		assertTrue("2.2", !info.isDirectory());
	}

	public void testParentNotExistsDeep() {
		try {
			subDir.mkdir(EFS.NONE, getMonitor());
		} catch (CoreException e) {
			fail("1.99", e);
		}
		IFileInfo info = topDir.fetchInfo();
		assertTrue("1.1", info.exists());
		assertTrue("1.2", info.isDirectory());
		info = subDir.fetchInfo();
		assertTrue("1.3", info.exists());
		assertTrue("1.4", info.isDirectory());
	}

	public void testParentNotExistsShallow() {
		try {
			subDir.mkdir(EFS.SHALLOW, getMonitor());
			fail("1.99");
		} catch (CoreException e) {
			//expected
		}
		IFileInfo info = topDir.fetchInfo();
		assertTrue("1.1", !info.exists());
		assertTrue("1.2", !info.isDirectory());
		info = subDir.fetchInfo();
		assertTrue("1.3", !info.exists());
		assertTrue("1.4", !info.isDirectory());
	}

	public void testParentNotExistsShallowInLocalFile() {
		try {
			IFileStore localFileTopDir = localFileBaseStore.getChild("topDir");
			localFileTopDir.mkdir(EFS.SHALLOW, getMonitor());
			fail("1.99");
		} catch (CoreException e) {
			assertNotNull("1.1", e.getStatus());
			assertEquals("1.2", EFS.ERROR_NOT_EXISTS, e.getStatus().getCode());
		}
	}

	public void testTargetIsFileInLocalFile() {
		try {
			ensureExists(localFileBaseStore, true);
			IFileStore localFileTopDir = localFileBaseStore.getChild("topDir");
			ensureExists(localFileTopDir, false);
			localFileTopDir.mkdir(EFS.SHALLOW, getMonitor());
			fail("1.99");
		} catch (CoreException e) {
			assertNotNull("1.1", e.getStatus());
			assertEquals("1.2", EFS.ERROR_WRONG_TYPE, e.getStatus().getCode());
		}
	}

	public void testParentDeviceNotExistsInLocalFile() {
		if (!Platform.getOS().equals(Platform.OS_WIN32)) {
			return;
		}
		String device = findNonExistingDevice();
		if (device == null) {
			return;
		}

		try {
			IFileStore localFileTopDir = EFS.getStore(URI.create("file:/" + device + ":" + getUniqueString()));
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
