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

import static org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestUtil.assertTextFileContent;
import static org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestUtil.ensureDoesNotExist;
import static org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestUtil.ensureExists;
import static org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestUtil.getMonitor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class CopyTest {

	@BeforeEach
	public void setup() throws Exception {
		ZipFileSystemTestSetup.defaultSetup();
	}

	@AfterEach
	public void teardown() throws Exception {
		ZipFileSystemTestSetup.teardown();
	}

	@Test
	public void testCopyZipFile() throws Exception {
		IFolder openedZipFile = ZipFileSystemTestSetup.firstProject
				.getFolder(ZipFileSystemTestSetup.ZIP_FILE_VIRTUAL_FOLDER_NAME);
		ensureExists(openedZipFile);
		IFolder destinationFolder = ZipFileSystemTestSetup.firstProject.getFolder("Folder");
		destinationFolder.create(true, true, getMonitor());
		ensureExists(destinationFolder);
		IFolder copyDestination = ZipFileSystemTestSetup.firstProject
				.getFolder("Folder" + "/" + ZipFileSystemTestSetup.ZIP_FILE_VIRTUAL_FOLDER_NAME);
		openedZipFile.copy(copyDestination.getFullPath(), true, getMonitor());
		ensureExists(copyDestination);
		ensureExists(openedZipFile);
	}

	@Test
	public void testCopyFileInsideOfZipFile() throws Exception {
		IFile textFile = ZipFileSystemTestSetup.firstProject.getFile(
				ZipFileSystemTestSetup.ZIP_FILE_VIRTUAL_FOLDER_NAME + "/" + ZipFileSystemTestSetup.TEXT_FILE_NAME);
		ensureExists(textFile);
		IFolder destinationFolder = ZipFileSystemTestSetup.firstProject.getFolder("Folder");
		destinationFolder.create(true, true, getMonitor());
		ensureExists(destinationFolder);
		IFile copyDestination = ZipFileSystemTestSetup.firstProject
				.getFile("Folder" + "/" + ZipFileSystemTestSetup.TEXT_FILE_NAME);
		textFile.copy(copyDestination.getFullPath(), true, getMonitor());
		ensureExists(copyDestination);
		ensureExists(textFile);
	}

	@Test
	public void testCopyFolderInsideOfZipFile() throws Exception {
		IFolder openedZipFile = ZipFileSystemTestSetup.firstProject
				.getFolder(ZipFileSystemTestSetup.ZIP_FILE_VIRTUAL_FOLDER_NAME);
		ensureExists(openedZipFile);
		IFolder newFolder = ZipFileSystemTestSetup.firstProject.getFolder("NewFolder");
		ensureDoesNotExist(newFolder);
		newFolder.create(false, true, getMonitor());
		ensureExists(newFolder);
		IFile textFile = newFolder.getFile("NewFile.txt");
		ensureDoesNotExist(textFile);
		String text = "Foo";
		InputStream stream = new ByteArrayInputStream(text.getBytes());
		textFile.create(stream, true, getMonitor());
		stream.close();
		ensureExists(textFile);
		IFolder copyDestination = openedZipFile.getFolder("NewFolder");
		ensureDoesNotExist(copyDestination);
		newFolder.copy(copyDestination.getFullPath(), true, getMonitor());
		ensureExists(copyDestination);
		ensureExists(newFolder);
		assertTextFileContent(textFile, "Foo");
	}

	@Test
	public void testCopyFileIntoZipFile() throws Exception {
		IFile textFile = ZipFileSystemTestSetup.firstProject.getFile("NewFile.txt");
		ensureDoesNotExist(textFile);
		String text = "Foo";
		InputStream stream = new ByteArrayInputStream(text.getBytes());
		textFile.create(stream, true, getMonitor());
		stream.close();
		ensureExists(textFile);
		IFile copyDestination = ZipFileSystemTestSetup.firstProject
				.getFile(ZipFileSystemTestSetup.ZIP_FILE_VIRTUAL_FOLDER_NAME + "/" + "NewFile.txt");
		textFile.copy(copyDestination.getFullPath(), true, getMonitor());
		ensureExists(copyDestination);
		ensureExists(textFile);
		assertTextFileContent(textFile, "Foo");
	}

	@Test
	public void testCopyFolderIntoZipFile() throws Exception {
		IFile textFile = ZipFileSystemTestSetup.firstProject.getFile("NewFile.txt");
		ensureDoesNotExist(textFile);
		String text = "Foo";
		InputStream stream = new ByteArrayInputStream(text.getBytes());
		textFile.create(stream, true, getMonitor());
		stream.close();
		ensureExists(textFile);
		IFile copyDestination = ZipFileSystemTestSetup.firstProject
				.getFile(ZipFileSystemTestSetup.ZIP_FILE_VIRTUAL_FOLDER_NAME + "/" + "NewFile.txt");
		textFile.copy(copyDestination.getFullPath(), true, getMonitor());
		ensureExists(copyDestination);
		ensureExists(textFile);
		assertTextFileContent(textFile, "Foo");
	}

	@Test
	public void testCopyFileFromOutsideOfZipFIleIntoFolderInZipFile() throws Exception {
		IFolder openedZipFile = ZipFileSystemTestSetup.firstProject
				.getFolder(ZipFileSystemTestSetup.ZIP_FILE_VIRTUAL_FOLDER_NAME);
		IFolder newFolder = openedZipFile.getFolder("NewFolder");
		ensureDoesNotExist(newFolder);
		newFolder.create(false, true, getMonitor());
		ensureExists(newFolder);
		IFile textFile = ZipFileSystemTestSetup.firstProject.getFile("NewFile.txt");
		ensureDoesNotExist(textFile);
		String text = "Foo";
		InputStream stream = new ByteArrayInputStream(text.getBytes());
		textFile.create(stream, true, getMonitor());
		stream.close();
		ensureExists(textFile);
		IFile copyDestination = newFolder.getFile("NewFile.txt");
		ensureDoesNotExist(copyDestination);
		textFile.copy(copyDestination.getFullPath(), true, getMonitor());
		ensureExists(copyDestination);
		ensureExists(textFile);
		assertTextFileContent(textFile, "Foo");
	}

	@Test
	public void testCopyFolderIntoFolderInZipFile() throws Exception {
		IFolder openedZipFile = ZipFileSystemTestSetup.firstProject
				.getFolder(ZipFileSystemTestSetup.ZIP_FILE_VIRTUAL_FOLDER_NAME);
		IFolder firstNewFolder = openedZipFile.getFolder("FirstNewFolder");
		ensureDoesNotExist(firstNewFolder);
		firstNewFolder.create(false, true, getMonitor());
		ensureExists(firstNewFolder);
		IFolder secondNewFolder = openedZipFile.getFolder("SecondNewFolder");
		ensureDoesNotExist(secondNewFolder);
		secondNewFolder.create(false, true, getMonitor());
		ensureExists(secondNewFolder);
		IFile textFile = firstNewFolder.getFile("NewFile.txt");
		ensureDoesNotExist(textFile);
		String text = "Foo";
		try (InputStream stream = new ByteArrayInputStream(text.getBytes())) {
			textFile.create(stream, true, getMonitor());
		}
		ensureExists(textFile);
		IFolder copyDestination = secondNewFolder.getFolder("FirstNewFolder");
		ensureDoesNotExist(copyDestination);
		firstNewFolder.copy(copyDestination.getFullPath(), true, getMonitor());
		ensureExists(copyDestination);
		ensureExists(firstNewFolder);
		assertTextFileContent(textFile, "Foo");
	}

	@Test
	public void testCopyFileFromOneFolderToOtherFolderInsideofZipFile() throws Exception {
		IFolder openedZipFile = ZipFileSystemTestSetup.firstProject
				.getFolder(ZipFileSystemTestSetup.ZIP_FILE_VIRTUAL_FOLDER_NAME);
		IFolder firstNewFolder = openedZipFile.getFolder("FirstNewFolder");
		ensureDoesNotExist(firstNewFolder);
		firstNewFolder.create(false, true, getMonitor());
		ensureExists(firstNewFolder);
		IFolder secondNewFolder = openedZipFile.getFolder("SecondNewFolder");
		ensureDoesNotExist(secondNewFolder);
		secondNewFolder.create(false, true, getMonitor());
		ensureExists(secondNewFolder);
		IFile textFile = firstNewFolder.getFile("NewFile.txt");
		ensureDoesNotExist(textFile);
		String text = "Foo";
		try (InputStream stream = new ByteArrayInputStream(text.getBytes())) {
			textFile.create(stream, true, getMonitor());
		}
		ensureExists(textFile);
		IFile copyDestination = secondNewFolder.getFile("NewFile.txt");
		ensureDoesNotExist(copyDestination);
		textFile.copy(copyDestination.getFullPath(), true, getMonitor());
		ensureExists(copyDestination);
		ensureExists(textFile);
		assertTextFileContent(textFile, "Foo");
	}
}
