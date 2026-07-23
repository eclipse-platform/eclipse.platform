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
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 *
 */
public class CopyTest {

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
	public void testCopyZipFile(String zipFileName) throws Exception {
		IFolder openedZipFile = ZipFileSystemTestSetup.projects.get(0)
				.getFolder(zipFileName);
		ZipFileSystemTestSetup.ensureExists(openedZipFile);
		IFolder destinationFolder = ZipFileSystemTestSetup.projects.get(0).getFolder("Folder");
		destinationFolder.create(true, true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(destinationFolder);
		IFolder copyDestination = ZipFileSystemTestSetup.projects.get(0)
				.getFolder("Folder" + "/" + zipFileName);
		openedZipFile.copy(copyDestination.getFullPath(), true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(copyDestination);
		ZipFileSystemTestSetup.ensureExists(openedZipFile);
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestSetup#zipFileNames")
	public void testCopyFileInsideOfZipFile(String zipFileName) throws Exception {
		IFile textFile = ZipFileSystemTestSetup.projects.get(0).getFile(
				zipFileName + "/" + ZipFileSystemTestSetup.TEXT_FILE_NAME);
		ZipFileSystemTestSetup.ensureExists(textFile);
		IFolder destinationFolder = ZipFileSystemTestSetup.projects.get(0).getFolder("Folder");
		destinationFolder.create(true, true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(destinationFolder);
		IFile copyDestination = ZipFileSystemTestSetup.projects.get(0)
				.getFile("Folder" + "/" + ZipFileSystemTestSetup.TEXT_FILE_NAME);
		textFile.copy(copyDestination.getFullPath(), true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(copyDestination);
		ZipFileSystemTestSetup.ensureExists(textFile);
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestSetup#zipFileNames")
	public void testCopyFolderInsideOfZipFile(String zipFileName) throws Exception {
		IFolder openedZipFile = ZipFileSystemTestSetup.projects.get(0)
				.getFolder(zipFileName);
		ZipFileSystemTestSetup.ensureExists(openedZipFile);
		IFolder newFolder = ZipFileSystemTestSetup.projects.get(0).getFolder("NewFolder");
		ZipFileSystemTestSetup.ensureDoesNotExist(newFolder);
		newFolder.create(false, true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(newFolder);
		IFile textFile = newFolder.getFile("NewFile.txt");
		ZipFileSystemTestSetup.ensureDoesNotExist(textFile);
		textFile.create(new ByteArrayInputStream(new byte[0]), true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(textFile);
		IFolder copyDestination = openedZipFile.getFolder("NewFolder");
		ZipFileSystemTestSetup.ensureDoesNotExist(copyDestination);
		newFolder.copy(copyDestination.getFullPath(), true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(copyDestination);
		ZipFileSystemTestSetup.ensureExists(newFolder);
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestSetup#zipFileNames")
	public void testCopyFileIntoZipFile(String zipFileName) throws Exception {
		IFile textFile = ZipFileSystemTestSetup.projects.get(0).getFile("NewFile.txt");
		ZipFileSystemTestSetup.ensureDoesNotExist(textFile);
		textFile.create(new ByteArrayInputStream(new byte[0]), true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(textFile);
		IFile copyDestination = ZipFileSystemTestSetup.projects.get(0)
				.getFile(zipFileName + "/" + "NewFile.txt");
		textFile.copy(copyDestination.getFullPath(), true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(copyDestination);
		ZipFileSystemTestSetup.ensureExists(textFile);
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestSetup#zipFileNames")
	public void testCopyFolderIntoZipFile(String zipFileName) throws Exception {
		IFile textFile = ZipFileSystemTestSetup.projects.get(0).getFile("NewFile.txt");
		ZipFileSystemTestSetup.ensureDoesNotExist(textFile);
		textFile.create(new ByteArrayInputStream(new byte[0]), true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(textFile);
		IFile copyDestination = ZipFileSystemTestSetup.projects.get(0)
				.getFile(zipFileName + "/" + "NewFile.txt");
		textFile.copy(copyDestination.getFullPath(), true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(copyDestination);
		ZipFileSystemTestSetup.ensureExists(textFile);
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestSetup#zipFileNames")
	public void testCopyFileFromOutsideOfZipFIleIntoFolderInZipFile(String zipFileName) throws Exception {
		IFolder openedZipFile = ZipFileSystemTestSetup.projects.get(0)
				.getFolder(zipFileName);
		IFolder newFolder = openedZipFile.getFolder("NewFolder");
		ZipFileSystemTestSetup.ensureDoesNotExist(newFolder);
		newFolder.create(false, true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(newFolder);
		IFile textFile = ZipFileSystemTestSetup.projects.get(0).getFile("NewFile.txt");
		ZipFileSystemTestSetup.ensureDoesNotExist(textFile);
		textFile.create(new ByteArrayInputStream(new byte[0]), true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(textFile);
		IFile copyDestination = newFolder.getFile("NewFile.txt");
		ZipFileSystemTestSetup.ensureDoesNotExist(copyDestination);
		textFile.copy(copyDestination.getFullPath(), true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(copyDestination);
		ZipFileSystemTestSetup.ensureExists(textFile);
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestSetup#zipFileNames")
	public void testCopyFolderIntoFolderInZipFile(String zipFileName) throws Exception {
		IFolder openedZipFile = ZipFileSystemTestSetup.projects.get(0)
				.getFolder(zipFileName);
		IFolder firstNewFolder = openedZipFile.getFolder("FirstNewFolder");
		ZipFileSystemTestSetup.ensureDoesNotExist(firstNewFolder);
		firstNewFolder.create(false, true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(firstNewFolder);
		IFolder secondNewFolder = openedZipFile.getFolder("SecondNewFolder");
		ZipFileSystemTestSetup.ensureDoesNotExist(secondNewFolder);
		secondNewFolder.create(false, true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(secondNewFolder);
		IFile textFile = firstNewFolder.getFile("NewFile.txt");
		ZipFileSystemTestSetup.ensureDoesNotExist(textFile);
		textFile.create(new ByteArrayInputStream(new byte[0]), true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(textFile);
		IFolder copyDestination = secondNewFolder.getFolder("FirstNewFolder");
		ZipFileSystemTestSetup.ensureDoesNotExist(copyDestination);
		firstNewFolder.copy(copyDestination.getFullPath(), true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(copyDestination);
		ZipFileSystemTestSetup.ensureExists(firstNewFolder);
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestSetup#zipFileNames")
	public void testCopyFileFromOneFolderToOtherFolderInsideofZipFile(String zipFileName) throws Exception {
		IFolder openedZipFile = ZipFileSystemTestSetup.projects.get(0)
				.getFolder(zipFileName);
		IFolder firstNewFolder = openedZipFile.getFolder("FirstNewFolder");
		ZipFileSystemTestSetup.ensureDoesNotExist(firstNewFolder);
		firstNewFolder.create(false, true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(firstNewFolder);
		IFolder secondNewFolder = openedZipFile.getFolder("SecondNewFolder");
		ZipFileSystemTestSetup.ensureDoesNotExist(secondNewFolder);
		secondNewFolder.create(false, true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(secondNewFolder);
		IFile textFile = firstNewFolder.getFile("NewFile.txt");
		ZipFileSystemTestSetup.ensureDoesNotExist(textFile);
		textFile.create(new ByteArrayInputStream(new byte[0]), true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(textFile);
		IFile copyDestination = secondNewFolder.getFile("NewFile.txt");
		ZipFileSystemTestSetup.ensureDoesNotExist(copyDestination);
		textFile.copy(copyDestination.getFullPath(), true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(copyDestination);
		ZipFileSystemTestSetup.ensureExists(textFile);
	}
}
