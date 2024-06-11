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
import java.io.IOException;
import java.io.InputStream;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;


public class MoveTest {

	@BeforeEach
	public void setup() throws Exception {
		ZipFileSystemTestSetup.setupWithTwoProjects();
	}

	@AfterEach
	public void teardown() throws Exception {
		ZipFileSystemTestSetup.teardown();
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestUtil#zipFileNames")
	public void testMoveZipFileWithinProject(String zipFileName) throws CoreException, IOException {
		IFolder openedZipFile = ZipFileSystemTestSetup.firstProject.getFolder(zipFileName);
		IFolder destinationFolder = ZipFileSystemTestSetup.firstProject.getFolder("destinationFolder");
		destinationFolder.create(false, true, getMonitor());
		IFolder destination = ZipFileSystemTestSetup.firstProject
				.getFolder("destinationFolder/" + zipFileName);
		openedZipFile.move(destination.getFullPath(), false, getMonitor());
		IFolder newFolder = ZipFileSystemTestSetup.firstProject
				.getFolder(destinationFolder.getName() + "/" + zipFileName);
		ensureExists(newFolder);
		ensureDoesNotExist(openedZipFile);
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestUtil#zipFileNames")
	public void testMoveZipFileToOtherProject(String zipFileName) throws CoreException, IOException {
		IFolder openedZipFile = ZipFileSystemTestSetup.firstProject.getFolder(zipFileName);
		IFolder destination = ZipFileSystemTestSetup.secondProject.getFolder(zipFileName);
		openedZipFile.move(destination.getFullPath(), false, getMonitor());
		IFolder newFolder = ZipFileSystemTestSetup.secondProject.getFolder(zipFileName);
		ensureExists(newFolder);
		ensureDoesNotExist(openedZipFile);
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestUtil#zipFileNames")
	public void testMoveZipFileToOtherProjectFolder(String zipFileName) throws CoreException, IOException {
		IFolder openedZipFile = ZipFileSystemTestSetup.firstProject.getFolder(zipFileName);
		IFolder destinationFolder = ZipFileSystemTestSetup.secondProject.getFolder("destinationFolder");
		destinationFolder.create(false, true, getMonitor());
		IFolder destination = ZipFileSystemTestSetup.secondProject
				.getFolder("destinationFolder/" + zipFileName);
		openedZipFile.move(destination.getFullPath(), false, getMonitor());
		IFolder newFolder = ZipFileSystemTestSetup.secondProject
				.getFolder(destinationFolder.getName() + "/" + zipFileName);
		ensureExists(newFolder);
		ensureDoesNotExist(openedZipFile);
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestUtil#zipFileNames")
	public void testMoveFileIntoZipFile(String zipFileName) throws Exception {
		IFile textFile = ZipFileSystemTestSetup.firstProject.getFile("NewFile.txt");
		ensureDoesNotExist(textFile);
		String text = "Foo";
		InputStream stream = new ByteArrayInputStream(text.getBytes());
		textFile.create(stream, false, getMonitor());
		stream.close();
		ensureExists(textFile);
		IFile destinationFile = ZipFileSystemTestSetup.firstProject
				.getFile(zipFileName + "/" + "NewFile.txt");
		textFile.move(destinationFile.getFullPath(), false, getMonitor());
		ensureExists(destinationFile);
		assertTextFileContent(destinationFile, text);
		ensureDoesNotExist(textFile);
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestUtil#zipFileNames")
	public void testMoveFolderIntoZipFile(String zipFileName) throws Exception {
		IFolder openedZipFile = ZipFileSystemTestSetup.firstProject.getFolder(zipFileName);
		IFolder destinationFolder = openedZipFile.getFolder("destinationFolder");
		ensureDoesNotExist(destinationFolder);
		destinationFolder.create(false, true, getMonitor());
		ensureExists(destinationFolder);
		IFolder newFolder = ZipFileSystemTestSetup.firstProject.getFolder("NewFolder");
		ensureDoesNotExist(newFolder);
		newFolder.create(false, true, getMonitor());
		ensureExists(newFolder);
		IFolder newFolderDestination = destinationFolder.getFolder("NewFolder");
		newFolder.move(newFolderDestination.getFullPath(), false, getMonitor());
		ensureDoesNotExist(newFolder);
		ensureExists(newFolderDestination);
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestUtil#zipFileNames")
	public void testMoveFolderWithContentIntoZipFile(String zipFileName) throws Exception {
		IFolder openedZipFile = ZipFileSystemTestSetup.firstProject.getFolder(zipFileName);
		IFolder destinationFolder = openedZipFile.getFolder("destinationFolder");
		ensureDoesNotExist(destinationFolder);
		destinationFolder.create(false, true, getMonitor());
		ensureExists(destinationFolder);
		IFolder newFolder = ZipFileSystemTestSetup.firstProject.getFolder("NewFolder");
		ensureDoesNotExist(newFolder);
		newFolder.create(false, true, getMonitor());
		ensureExists(newFolder);
		IFile textFile = newFolder.getFile("NewFile.txt");
		ensureDoesNotExist(textFile);
		String text = "Foo";
		InputStream stream = new ByteArrayInputStream(text.getBytes());
		textFile.create(stream, false, getMonitor());
		stream.close();
		ensureExists(textFile);
		IFolder newFolderDestination = destinationFolder.getFolder("NewFolder");
		newFolder.move(newFolderDestination.getFullPath(), false, getMonitor());
		ensureDoesNotExist(newFolder);
		ensureExists(newFolderDestination);
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestUtil#zipFileNames")
	public void testMoveFileFromZipFile(String zipFileName) throws Exception {
		IFile textFile = ZipFileSystemTestSetup.firstProject
				.getFile(zipFileName + "/" + ZipFileSystemTestSetup.TEXT_FILE_NAME);
		ensureExists(textFile);
		IFile destinationFile = ZipFileSystemTestSetup.firstProject.getFile(ZipFileSystemTestSetup.TEXT_FILE_NAME);
		textFile.move(destinationFile.getFullPath(), false, getMonitor());
		ensureExists(destinationFile);
		assertTextFileContent(destinationFile, "Hello World!");
		ensureDoesNotExist(textFile);
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestUtil#zipFileNames")
	public void testMoveFolderFromZipFile(String zipFileName) throws Exception {
		IFolder openedZipFile = ZipFileSystemTestSetup.firstProject.getFolder(zipFileName);
		IFolder newFolder = openedZipFile.getFolder("NewFolder");
		ensureDoesNotExist(newFolder);
		newFolder.create(false, true, getMonitor());
		ensureExists(newFolder);
		IFolder folderDestination = ZipFileSystemTestSetup.firstProject.getFolder("NewFolder");
		newFolder.move(folderDestination.getFullPath(), false, getMonitor());
		ensureDoesNotExist(newFolder);
		ensureExists(folderDestination);
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestUtil#zipFileNames")
	public void testMoveFolderWithContentFromZipFile(String zipFileName) throws Exception {
		IFolder openedZipFile = ZipFileSystemTestSetup.firstProject.getFolder(zipFileName);
		IFolder newFolder = openedZipFile.getFolder("NewFolder");
		ensureDoesNotExist(newFolder);
		newFolder.create(false, true, getMonitor());
		ensureExists(newFolder);
		IFile textFile = newFolder.getFile("NewFile.txt");
		ensureDoesNotExist(textFile);
		String text = "Foo";
		InputStream stream = new ByteArrayInputStream(text.getBytes());
		textFile.create(stream, false, getMonitor());
		stream.close();
		ensureExists(textFile);
		IFolder folderDestination = ZipFileSystemTestSetup.firstProject.getFolder("NewFolder");
		newFolder.move(folderDestination.getFullPath(), false, getMonitor());
		ensureDoesNotExist(newFolder);
		ensureExists(folderDestination);
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestUtil#zipFileNames")
	public void testMoveFolderWithContentFromZipFileIntoOtherZipFile(String zipFileName) throws Exception {
		IFolder firstZipFile = ZipFileSystemTestSetup.firstProject.getFolder(zipFileName);
		// create and open second ZipFile
		String secondZipFileName = zipFileName.replace(".", "New.");
		IFile secondZipFile = ZipFileSystemTestSetup.firstProject.getFile(secondZipFileName);
		ensureDoesNotExist(secondZipFile);
		ZipFileSystemTestSetup.copyZipFileIntoProject(ZipFileSystemTestSetup.firstProject, secondZipFileName);
		ensureExists(secondZipFile);
		ZipFileSystemTestUtil.openZipFile(secondZipFile);
		IFolder openedSecondZipFile = ZipFileSystemTestSetup.firstProject.getFolder(secondZipFileName);
		ensureExists(openedSecondZipFile);

		IFolder newFolder = firstZipFile.getFolder("NewFolder");
		ensureDoesNotExist(newFolder);
		newFolder.create(false, true, getMonitor());
		ensureExists(newFolder);
		IFile textFile = newFolder.getFile("NewFile.txt");
		ensureDoesNotExist(textFile);
		String text = "Foo";
		InputStream stream = new ByteArrayInputStream(text.getBytes());
		textFile.create(stream, false, getMonitor());
		stream.close();
		ensureExists(textFile);
		IFolder movedFolderDestination = openedSecondZipFile.getFolder("NewFolder");
		newFolder.move(movedFolderDestination.getFullPath(), false, getMonitor());
		ensureDoesNotExist(newFolder);
		ensureExists(movedFolderDestination);
		IFile movedTextFile = movedFolderDestination.getFile("NewFile.txt");
		ensureExists(movedTextFile);
	}


	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestUtil#zipFileNames")
	public void testMoveFileInsideOfZipFile(String zipFileName) throws Exception {
		IFolder openedZipFile = ZipFileSystemTestSetup.firstProject.getFolder(zipFileName);
		IFolder destinationFolder = openedZipFile.getFolder("destinationFolder");
		ensureDoesNotExist(destinationFolder);
		destinationFolder.create(false, true, getMonitor());
		ensureExists(destinationFolder);
		IFile textFile = openedZipFile.getFile(ZipFileSystemTestSetup.TEXT_FILE_NAME);
		ensureExists(textFile);
		IFile fileDestination = destinationFolder.getFile(ZipFileSystemTestSetup.TEXT_FILE_NAME);
		ensureDoesNotExist(fileDestination);
		textFile.move(fileDestination.getFullPath(), false, getMonitor());
		ensureExists(fileDestination);
		ensureDoesNotExist(textFile);
	}



	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestUtil#zipFileNames")
	public void testMoveZipFileIntoZipFile(String zipFileName) throws Exception {
		IFolder openedZipFile = ZipFileSystemTestSetup.firstProject.getFolder(zipFileName);
		// create and open second ZipFile
		String newZipFileName = zipFileName.replace(".", "New.");
		IFile newZipFile = ZipFileSystemTestSetup.firstProject.getFile(newZipFileName);
		ensureDoesNotExist(newZipFile);
		ZipFileSystemTestSetup.copyZipFileIntoProject(ZipFileSystemTestSetup.firstProject, newZipFileName);
		ensureExists(newZipFile);
		ZipFileSystemTestUtil.openZipFile(newZipFile);
		IFolder newOpenedZipFile = ZipFileSystemTestSetup.firstProject.getFolder(newZipFileName);
		ensureExists(newOpenedZipFile);
		// move second ZipFile into first ZipFile
		IFile newOpenedZipFileDestination = openedZipFile.getFile(newZipFileName);
		newOpenedZipFile.move(newOpenedZipFileDestination.getFullPath(), false, getMonitor());
		ensureExists(newOpenedZipFileDestination);
		ensureDoesNotExist(newOpenedZipFile);
	}

	/**
	 * When moving or expanding an opened zip file that contains a folder with
	 * content. errors can occur. This is because the local name of the resources
	 * inside the folder contains "\" seperators that are not allowed when
	 * refreshing the Workspace. This test checks if this specific error is handeled
	 * correctly in RefreshLocalVisitor#visit()
	 */
	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestUtil#zipFileNames")
	public void testMoveZipFileWithFolder(String zipFileName) throws Exception {
		IFolder openedZipFile = ZipFileSystemTestSetup.firstProject.getFolder(zipFileName);
		String contentFolderPath = zipFileName + "/" + "Folder";
		IFolder contentFolder = ZipFileSystemTestSetup.firstProject.getFolder(contentFolderPath);
		ensureDoesNotExist(contentFolder);
		contentFolder.create(false, true, getMonitor());
		ensureExists(contentFolder);
		String text = "Foo";
		InputStream stream = new ByteArrayInputStream(text.getBytes());
		IFile textFile = ZipFileSystemTestSetup.firstProject.getFile(contentFolderPath + "/" + "textFile");
		ensureDoesNotExist(textFile);
		textFile.create(stream, false, getMonitor());
		ensureExists(textFile);
		IFolder destinationFolder = ZipFileSystemTestSetup.firstProject.getFolder("destinationFolder");
		ensureDoesNotExist(destinationFolder);
		destinationFolder.create(false, true, getMonitor());
		ensureExists(destinationFolder);
		IFolder zipFileDestination = ZipFileSystemTestSetup.firstProject.getFolder("destinationFolder/" + zipFileName);
		ensureDoesNotExist(zipFileDestination);
		openedZipFile.move(zipFileDestination.getFullPath(), false, getMonitor());
		ensureExists(zipFileDestination);
	}
}
