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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class RenameTest {

	@BeforeEach
	public void setup() throws Exception {
		ZipFileSystemTestSetup.defaultSetup();
	}

	@AfterEach
	public void teardown() throws Exception {
		ZipFileSystemTestSetup.teardown();
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestUtil#zipFileNames")
	public void testRenameZipFile(String zipFileName) throws Exception {
		// IFolder is renamed by moving with the new path
		IFolder openedZipFile = ZipFileSystemTestSetup.firstProject.getFolder(zipFileName);
		IFolder renamedOpenZipFile = ZipFileSystemTestSetup.firstProject.getFolder(zipFileName + "Renamed");
		openedZipFile.move(renamedOpenZipFile.getFullPath(), false, getMonitor());
		ensureExists(renamedOpenZipFile);
		ensureDoesNotExist(openedZipFile);
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestUtil#zipFileNames")
	public void testRenameFileInsideOfZipFile(String zipFileName) throws Exception {
		IFolder openedZipFile = ZipFileSystemTestSetup.firstProject.getFolder(zipFileName);
		IFile textFile = openedZipFile.getFile(ZipFileSystemTestSetup.TEXT_FILE_NAME);
		IFile renamedTextFile = openedZipFile.getFile(textFile.getName() + "Renamed");
		textFile.move(renamedTextFile.getFullPath(), false, getMonitor());
		ensureExists(renamedTextFile);
		ensureDoesNotExist(textFile);
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestUtil#zipFileNames")
	public void testRenameFolderInsideOfZipFile(String zipFileName) throws Exception {
		IFolder openedZipFile = ZipFileSystemTestSetup.firstProject.getFolder(zipFileName);
		IFolder folder = openedZipFile.getFolder("newFolder");
		ensureDoesNotExist(folder);
		folder.create(false, true, getMonitor());
		ensureExists(folder);
		IFolder renamedFolder = openedZipFile.getFolder(folder.getName() + "Renamed");
		folder.move(renamedFolder.getFullPath(), false, getMonitor());
		ensureExists(renamedFolder);
		ensureDoesNotExist(folder);
	}
}
