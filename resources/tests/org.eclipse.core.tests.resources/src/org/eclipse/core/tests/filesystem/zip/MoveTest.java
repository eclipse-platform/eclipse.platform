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
import java.io.InputStream;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.ZipFileTransformer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;


public class MoveTest {

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
	public void testMoveZipFileWithinProject(String zipFileName) throws CoreException, IOException {
		IFolder openedZipFile = ZipFileSystemTestSetup.projects.get(0)
				.getFolder(zipFileName);
		IFolder destinationFolder = ZipFileSystemTestSetup.projects.get(0).getFolder("destinationFolder");
		destinationFolder.create(false, true, new NullProgressMonitor());
		IFolder destination = ZipFileSystemTestSetup.projects.get(0)
				.getFolder("destinationFolder/" + zipFileName);
		openedZipFile.move(destination.getFullPath(), false, new NullProgressMonitor());
		IFolder newFolder = ZipFileSystemTestSetup.projects.get(0)
				.getFolder(destinationFolder.getName() + "/" + zipFileName);
		ZipFileSystemTestSetup.ensureExists(newFolder);
		ZipFileSystemTestSetup.ensureDoesNotExist(openedZipFile);
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestSetup#zipFileNames")
	public void testMoveZipFileToOtherProject(String zipFileName) throws CoreException, IOException {
		IFolder openedZipFile = ZipFileSystemTestSetup.projects.get(0)
				.getFolder(zipFileName);
		IFolder destination = ZipFileSystemTestSetup.projects.get(1)
				.getFolder(zipFileName);
		destination.delete(true, new NullProgressMonitor());
		openedZipFile.move(destination.getFullPath(), false, new NullProgressMonitor());
		IFolder newFolder = ZipFileSystemTestSetup.projects.get(1)
				.getFolder(zipFileName);
		ZipFileSystemTestSetup.ensureExists(newFolder);
		ZipFileSystemTestSetup.ensureDoesNotExist(openedZipFile);
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestSetup#zipFileNames")
	public void testMoveZipFileToOtherProjectFolder(String zipFileName) throws CoreException, IOException {
		IFolder openedZipFile = ZipFileSystemTestSetup.projects.get(0)
				.getFolder(zipFileName);
		IFolder destinationFolder = ZipFileSystemTestSetup.projects.get(1).getFolder("destinationFolder");
		destinationFolder.create(false, true, new NullProgressMonitor());
		IFolder destination = ZipFileSystemTestSetup.projects.get(1)
				.getFolder("destinationFolder/" + zipFileName);
		openedZipFile.move(destination.getFullPath(), false, new NullProgressMonitor());
		IFolder newFolder = ZipFileSystemTestSetup.projects.get(1)
				.getFolder(destinationFolder.getName() + "/" + zipFileName);
		ZipFileSystemTestSetup.ensureExists(newFolder);
		ZipFileSystemTestSetup.ensureDoesNotExist(openedZipFile);
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestSetup#zipFileNames")
	public void testMoveFileIntoZipFile(String zipFileName) throws Exception {
		IFile textFile = ZipFileSystemTestSetup.projects.get(0).getFile("NewFile.txt");
		ZipFileSystemTestSetup.ensureDoesNotExist(textFile);
		String text = "Foo";
		InputStream stream = new ByteArrayInputStream(text.getBytes());
		textFile.create(stream, false, new NullProgressMonitor());
		stream.close();
		ZipFileSystemTestSetup.ensureExists(textFile);
		IFile destinationFile = ZipFileSystemTestSetup.projects.get(0)
				.getFile(zipFileName + "/" + "NewFile.txt");
		textFile.move(destinationFile.getFullPath(), false, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(destinationFile);
		ZipFileSystemTestSetup.ensureDoesNotExist(textFile);
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestSetup#zipFileNames")
	public void testMoveFolderIntoZipFile(String zipFileName) throws Exception {
		IFolder openedZipFile = ZipFileSystemTestSetup.projects.get(0)
				.getFolder(zipFileName);
		IFolder destinationFolder = openedZipFile.getFolder("destinationFolder");
		ZipFileSystemTestSetup.ensureDoesNotExist(destinationFolder);
		destinationFolder.create(false, true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(destinationFolder);
		IFolder newFolder = ZipFileSystemTestSetup.projects.get(0).getFolder("NewFolder");
		ZipFileSystemTestSetup.ensureDoesNotExist(newFolder);
		newFolder.create(false, true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(newFolder);
		IFolder newFolderDestination = destinationFolder.getFolder("NewFolder");
		newFolder.move(newFolderDestination.getFullPath(), false, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureDoesNotExist(newFolder);
		ZipFileSystemTestSetup.ensureExists(newFolderDestination);
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestSetup#zipFileNames")
	public void testMoveFolderWithContentIntoZipFile(String zipFileName) throws Exception {
		IFolder openedZipFile = ZipFileSystemTestSetup.projects.get(0)
				.getFolder(zipFileName);
		IFolder destinationFolder = openedZipFile.getFolder("destinationFolder");
		ZipFileSystemTestSetup.ensureDoesNotExist(destinationFolder);
		destinationFolder.create(false, true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(destinationFolder);
		IFolder newFolder = ZipFileSystemTestSetup.projects.get(0).getFolder("NewFolder");
		ZipFileSystemTestSetup.ensureDoesNotExist(newFolder);
		newFolder.create(false, true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(newFolder);
		IFile textFile = newFolder.getFile("NewFile.txt");
		ZipFileSystemTestSetup.ensureDoesNotExist(textFile);
		String text = "Foo";
		InputStream stream = new ByteArrayInputStream(text.getBytes());
		textFile.create(stream, false, new NullProgressMonitor());
		stream.close();
		ZipFileSystemTestSetup.ensureExists(textFile);
		IFolder newFolderDestination = destinationFolder.getFolder("NewFolder");
		newFolder.move(newFolderDestination.getFullPath(), false, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureDoesNotExist(newFolder);
		ZipFileSystemTestSetup.ensureExists(newFolderDestination);
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestSetup#zipFileNames")
	public void testMoveFileFromZipFile(String zipFileName) throws Exception {
		IFile textFile = ZipFileSystemTestSetup.projects.get(0)
				.getFile(zipFileName + "/"
						+ ZipFileSystemTestSetup.TEXT_FILE_NAME);
		ZipFileSystemTestSetup.ensureExists(textFile);
		IFile destinationFile = ZipFileSystemTestSetup.projects.get(0).getFile(ZipFileSystemTestSetup.TEXT_FILE_NAME);
		textFile.move(destinationFile.getFullPath(), false, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(destinationFile);
		ZipFileSystemTestSetup.ensureDoesNotExist(textFile);
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestSetup#zipFileNames")
	public void testMoveFolderFromZipFile(String zipFileName) throws Exception {
		IFolder openedZipFile = ZipFileSystemTestSetup.projects.get(0)
				.getFolder(zipFileName);
		IFolder newFolder = openedZipFile.getFolder("NewFolder");
		ZipFileSystemTestSetup.ensureDoesNotExist(newFolder);
		newFolder.create(false, true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(newFolder);
		IFolder folderDestination = ZipFileSystemTestSetup.projects.get(0).getFolder("NewFolder");
		newFolder.move(folderDestination.getFullPath(), false, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureDoesNotExist(newFolder);
		ZipFileSystemTestSetup.ensureExists(folderDestination);
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestSetup#zipFileNames")
	public void testMoveFolderWithContentFromZipFile(String zipFileName) throws Exception {
		IFolder openedZipFile = ZipFileSystemTestSetup.projects.get(0)
				.getFolder(zipFileName);
		IFolder newFolder = openedZipFile.getFolder("NewFolder");
		ZipFileSystemTestSetup.ensureDoesNotExist(newFolder);
		newFolder.create(false, true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(newFolder);
		IFile textFile = newFolder.getFile("NewFile.txt");
		ZipFileSystemTestSetup.ensureDoesNotExist(textFile);
		String text = "Foo";
		InputStream stream = new ByteArrayInputStream(text.getBytes());
		textFile.create(stream, false, new NullProgressMonitor());
		stream.close();
		ZipFileSystemTestSetup.ensureExists(textFile);
		IFolder folderDestination = ZipFileSystemTestSetup.projects.get(0).getFolder("NewFolder");
		newFolder.move(folderDestination.getFullPath(), false, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureDoesNotExist(newFolder);
		ZipFileSystemTestSetup.ensureExists(folderDestination);
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestSetup#zipFileNames")
	public void testMoveFolderWithContentFromZipFileIntoOtherZipFile(String zipFileName) throws Exception {
		IFolder firstZipFile = ZipFileSystemTestSetup.projects.get(0).getFolder(zipFileName);
		String secondZipFileName = zipFileName.replace(".", "2.");
		ZipFileSystemTestSetup.copyZipFileIntoProject(ZipFileSystemTestSetup.projects.get(0), secondZipFileName);
		ZipFileTransformer.openZipFile(ZipFileSystemTestSetup.projects.get(0).getFile(secondZipFileName), true);
		IFolder secondZipFile = ZipFileSystemTestSetup.projects.get(0).getFolder(secondZipFileName);

		IFolder newFolder = firstZipFile.getFolder("NewFolder");
		ZipFileSystemTestSetup.ensureDoesNotExist(newFolder);
		newFolder.create(false, true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(newFolder);
		IFile textFile = newFolder.getFile("NewFile.txt");
		ZipFileSystemTestSetup.ensureDoesNotExist(textFile);
		String text = "Foo";
		InputStream stream = new ByteArrayInputStream(text.getBytes());
		textFile.create(stream, false, new NullProgressMonitor());
		stream.close();
		ZipFileSystemTestSetup.ensureExists(textFile);
		IFolder movedFolderDestination = secondZipFile.getFolder("NewFolder");
		newFolder.move(movedFolderDestination.getFullPath(), false, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureDoesNotExist(newFolder);
		ZipFileSystemTestSetup.ensureExists(movedFolderDestination);
		IFile movedTextFile = movedFolderDestination.getFile("NewFile.txt");
		ZipFileSystemTestSetup.ensureExists(movedTextFile);
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestSetup#zipFileNames")
	public void testMoveFolderWithContentFromZipFileIntoOtherZipFileTwice(String zipFileName) throws Exception {
		IFolder firstZipFile = ZipFileSystemTestSetup.projects.get(0).getFolder(zipFileName);
		String secondZipFileName = zipFileName.replace(".", "2.");
		ZipFileSystemTestSetup.copyZipFileIntoProject(ZipFileSystemTestSetup.projects.get(0), secondZipFileName);
		ZipFileTransformer.openZipFile(ZipFileSystemTestSetup.projects.get(0).getFile(secondZipFileName), true);
		IFolder secondZipFile = ZipFileSystemTestSetup.projects.get(0).getFolder(secondZipFileName);

		IFolder newFolder = firstZipFile.getFolder("NewFolder");
		ZipFileSystemTestSetup.ensureDoesNotExist(newFolder);
		newFolder.create(false, true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(newFolder);
		IFile textFile = newFolder.getFile("NewFile.txt");
		ZipFileSystemTestSetup.ensureDoesNotExist(textFile);
		String text = "Foo";
		InputStream stream = new ByteArrayInputStream(text.getBytes());
		textFile.create(stream, false, new NullProgressMonitor());
		stream.close();
		ZipFileSystemTestSetup.ensureExists(textFile);
		IFolder movedFolderDestination = secondZipFile.getFolder("NewFolder");
		newFolder.move(movedFolderDestination.getFullPath(), false, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureDoesNotExist(newFolder);
		ZipFileSystemTestSetup.ensureExists(movedFolderDestination);
		IFile movedTextFile = movedFolderDestination.getFile("NewFile.txt");
		ZipFileSystemTestSetup.ensureExists(movedTextFile);

		// Move second time
		IFolder originDestination = newFolder;
		movedFolderDestination.move(originDestination.getFullPath(), false, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureDoesNotExist(movedFolderDestination);
		ZipFileSystemTestSetup.ensureExists(originDestination);
		movedTextFile = originDestination.getFile("NewFile.txt");
		ZipFileSystemTestSetup.ensureExists(movedTextFile);
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestSetup#zipFileNames")
	public void testMoveFileInsideOfZipFile(String zipFileName) throws Exception {
		IFolder openedZipFile = ZipFileSystemTestSetup.projects.get(0)
				.getFolder(zipFileName);
		IFolder destinationFolder = openedZipFile.getFolder("destinationFolder");
		ZipFileSystemTestSetup.ensureDoesNotExist(destinationFolder);
		destinationFolder.create(false, true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(destinationFolder);
		IFile textFile = openedZipFile.getFile(ZipFileSystemTestSetup.TEXT_FILE_NAME);
		ZipFileSystemTestSetup.ensureExists(textFile);
		IFile fileDestination = destinationFolder.getFile(ZipFileSystemTestSetup.TEXT_FILE_NAME);
		ZipFileSystemTestSetup.ensureDoesNotExist(fileDestination);
		textFile.move(fileDestination.getFullPath(), false, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(fileDestination);
		ZipFileSystemTestSetup.ensureDoesNotExist(textFile);
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestSetup#zipFileNames")
	public void testMoveZipFileIntoZipFile(String zipFileName) throws Exception {
		IFolder firstZipFile = ZipFileSystemTestSetup.projects.get(0)
				.getFolder(zipFileName);
		String secondZipFileName = zipFileName.replace(".", "2.");
		ZipFileSystemTestSetup.copyZipFileIntoProject(ZipFileSystemTestSetup.projects.get(0), secondZipFileName);
		ZipFileTransformer.openZipFile(ZipFileSystemTestSetup.projects.get(0).getFile(secondZipFileName), true);
		IFolder secondZipFile = ZipFileSystemTestSetup.projects.get(0).getFolder(secondZipFileName);

		// move second ZipFile into first ZipFile
		IFile secondZipFileDestination = firstZipFile.getFile(secondZipFileName);
		secondZipFile.move(secondZipFileDestination.getFullPath(), false, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(secondZipFileDestination);
		ZipFileSystemTestSetup.ensureDoesNotExist(secondZipFile);
	}

	/**
	 * When moving or expanding an opened zip file that contains a folder with
	 * content. errors can occur. This is because the local name of the resources
	 * inside the folder contains "\" seperators that are not allowed when
	 * refreshing the Workspace. This test checks if this specific error is handeled
	 * correctly in RefreshLocalVisitor#visit()
	 */
	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestSetup#zipFileNames")
	public void testMoveZipFileWithFolder(String zipFileName) throws Exception {
		IFolder openedZipFile = ZipFileSystemTestSetup.projects.get(0)
				.getFolder(zipFileName);
		String contentFolderPath = zipFileName + "/" + "Folder";
		IFolder contentFolder = ZipFileSystemTestSetup.projects.get(0).getFolder(contentFolderPath);
		ZipFileSystemTestSetup.ensureDoesNotExist(contentFolder);
		contentFolder.create(false, true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(contentFolder);
		String text = "Foo";
		InputStream stream = new ByteArrayInputStream(text.getBytes());
		IFile textFile = ZipFileSystemTestSetup.projects.get(0).getFile(contentFolderPath + "/" + "textFile");
		ZipFileSystemTestSetup.ensureDoesNotExist(textFile);
		textFile.create(stream, false, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(textFile);
		IFolder destinationFolder = ZipFileSystemTestSetup.projects.get(0).getFolder("destinationFolder");
		ZipFileSystemTestSetup.ensureDoesNotExist(destinationFolder);
		destinationFolder.create(false, true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(destinationFolder);
		IFolder zipFileDestination = ZipFileSystemTestSetup.projects.get(0)
				.getFolder("destinationFolder/" + zipFileName);
		ZipFileSystemTestSetup.ensureDoesNotExist(zipFileDestination);
		openedZipFile.move(zipFileDestination.getFullPath(), false, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(zipFileDestination);
	}
}
