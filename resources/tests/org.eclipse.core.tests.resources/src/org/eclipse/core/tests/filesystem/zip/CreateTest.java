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

public class CreateTest {

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
	public void testCreateFileInsideOfZipFile(String zipFileName) throws Exception {
		IFolder openedZipFile = ZipFileSystemTestSetup.projects.get(0)
				.getFolder(zipFileName);
		IFile textFile = openedZipFile.getFile("NewFile.txt");
		ZipFileSystemTestSetup.ensureDoesNotExist(textFile);
		textFile.create(new ByteArrayInputStream(new byte[0]), true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(textFile);
	}

	@ParameterizedTest
	@MethodSource("org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestSetup#zipFileNames")
	public void testCreateFolderInsideOfZipFile(String zipFileName) throws Exception {
		IFolder openedZipFile = ZipFileSystemTestSetup.projects.get(0)
				.getFolder(zipFileName);
		IFolder newFolder = openedZipFile.getFolder("NewFolder");
		ZipFileSystemTestSetup.ensureDoesNotExist(newFolder);
		newFolder.create(false, true, new NullProgressMonitor());
		ZipFileSystemTestSetup.ensureExists(newFolder);
	}
}
