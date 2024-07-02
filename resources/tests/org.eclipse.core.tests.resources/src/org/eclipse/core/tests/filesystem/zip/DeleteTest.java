/*******************************************************************************
 * Copyright (c) 2024 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: Vector Informatik GmbH - initial API and implementation
 *******************************************************************************/

package org.eclipse.core.tests.filesystem.zip;

import static org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestUtil.ensureDoesNotExist;
import static org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestUtil.ensureExists;
import static org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestUtil.getMonitor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DeleteTest {

	@BeforeEach
	public void setup() throws Exception {
		ZipFileSystemTestSetup.defaultSetup();
	}

	@AfterEach
	public void teardown() throws Exception {
		ZipFileSystemTestSetup.teardown();
	}

	@Test
	public void testDeleteZipFile() throws CoreException, IOException {
		IFolder openedZipFile = ZipFileSystemTestSetup.firstProject
				.getFolder(ZipFileSystemTestSetup.ZIP_FILE_VIRTUAL_FOLDER_NAME);
		ensureExists(openedZipFile);
		openedZipFile.delete(false, false, getMonitor());
		ensureDoesNotExist(openedZipFile);
		IFile zipFile = ZipFileSystemTestSetup.firstProject
				.getFile(ZipFileSystemTestSetup.ZIP_FILE_VIRTUAL_FOLDER_NAME);
		ensureDoesNotExist(zipFile);
	}

	@Test
	public void testDeleteFileInsideOfZipFile() throws CoreException, IOException {
		IFolder openedZipFile = ZipFileSystemTestSetup.firstProject
				.getFolder(ZipFileSystemTestSetup.ZIP_FILE_VIRTUAL_FOLDER_NAME);
		IFile textFile = openedZipFile.getFile(ZipFileSystemTestSetup.TEXT_FILE_NAME);
		ensureExists(textFile);
		textFile.delete(true, getMonitor());
		ensureDoesNotExist(textFile);
	}

	@Test
	public void testDeleteEmptyFolder() throws CoreException, IOException {
		IFolder openedZipFile = ZipFileSystemTestSetup.firstProject
				.getFolder(ZipFileSystemTestSetup.ZIP_FILE_VIRTUAL_FOLDER_NAME);
		IFolder folder = openedZipFile.getFolder("FolderToDelete");
		ensureDoesNotExist(folder);
		folder.create(true, true, getMonitor());
		ensureExists(folder);
		folder.delete(true, getMonitor());
		ensureDoesNotExist(folder);
	}

	@Test
	public void testDeleteFolderWithChildren() throws CoreException, IOException {
		IFolder openedZipFile = ZipFileSystemTestSetup.firstProject
				.getFolder(ZipFileSystemTestSetup.ZIP_FILE_VIRTUAL_FOLDER_NAME);
		IFolder folder = openedZipFile.getFolder("FolderToDelete");
		ensureDoesNotExist(folder);
		folder.create(true, true, getMonitor());
		ensureExists(folder);
		IFile textFile = folder.getFile(ZipFileSystemTestSetup.TEXT_FILE_NAME);
		textFile.create(new ByteArrayInputStream("Hello World!".getBytes()), true, getMonitor());
		ensureExists(textFile);
		folder.delete(true, getMonitor());
		ensureDoesNotExist(folder);
		ensureDoesNotExist(textFile);
	}
}
