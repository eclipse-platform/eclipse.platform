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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class DeleteTest {

	@BeforeEach
	public void setup() throws Exception {
		ZipFileSystemTestSetup.setup();
	}

	@AfterEach
	public void teardown() throws Exception {
		ZipFileSystemTestSetup.teardown();
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestSetup#zipFileNames")
	public void testDeleteZipFile(String zipFileName) throws CoreException, IOException {
		IFolder openedZipFile = ZipFileSystemTestSetup.projects.get(0)
				.getFolder(zipFileName);
		ZipFileSystemTestSetup.ensureExists(openedZipFile);
		openedZipFile.delete(false, false, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureDoesNotExist(openedZipFile);
		IFile zipFile = ZipFileSystemTestSetup.projects.get(0)
				.getFile(zipFileName);
		ZipFileSystemTestSetup.ensureDoesNotExist(zipFile);
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestSetup#zipFileNames")
	public void testDeleteFileInsideOfZipFile(String zipFileName) throws CoreException, IOException {
		IFolder openedZipFile = ZipFileSystemTestSetup.projects.get(0)
				.getFolder(zipFileName);
		IFile textFile = openedZipFile.getFile(ZipFileSystemTestSetup.TEXT_FILE_NAME);
		ZipFileSystemTestSetup.ensureExists(textFile);
		textFile.delete(true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureDoesNotExist(textFile);
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestSetup#zipFileNames")
	public void testDeleteEmptyFolder(String zipFileName) throws CoreException, IOException {
		IFolder openedZipFile = ZipFileSystemTestSetup.projects.get(0)
				.getFolder(zipFileName);
		IFolder folder = openedZipFile.getFolder("FolderToDelete");
		ZipFileSystemTestSetup.ensureDoesNotExist(folder);
		folder.create(true, true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(folder);
		folder.delete(true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureDoesNotExist(folder);
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestSetup#zipFileNames")
	public void testDeleteFolderWithChildren(String zipFileName) throws CoreException, IOException {
		IFolder openedZipFile = ZipFileSystemTestSetup.projects.get(0)
				.getFolder(zipFileName);
		IFolder folder = openedZipFile.getFolder("FolderToDelete");
		ZipFileSystemTestSetup.ensureDoesNotExist(folder);
		folder.create(true, true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(folder);
		IFile textFile = folder.getFile(ZipFileSystemTestSetup.TEXT_FILE_NAME);
		textFile.create(new ByteArrayInputStream("Hello World!".getBytes()), true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(textFile);
		folder.delete(true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureDoesNotExist(folder);
		ZipFileSystemTestSetup.ensureDoesNotExist(textFile);
	}
}
