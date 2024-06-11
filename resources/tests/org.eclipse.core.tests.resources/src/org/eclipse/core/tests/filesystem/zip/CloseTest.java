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

import static org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestUtil.ensureExists;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class CloseTest {

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
	public void testCloseZipFile(String zipFileName) throws Exception {
		IFolder openedZipFile = ZipFileSystemTestSetup.firstProject
				.getFolder(zipFileName);
		ensureExists(openedZipFile);
		ZipFileSystemTestUtil.closeZipFile(openedZipFile);
		IFile zipFile = ZipFileSystemTestSetup.firstProject.getFile(zipFileName);
		// Don't use Utility method ensureDoesNotExist because the fileStore is still
		// available after closing. The fileStore is the File itself in the local file
		// system that still exists after closing.
		assertTrue("folder was not properly deleted: " + openedZipFile, !openedZipFile.exists());
		ensureExists(zipFile);
	}

}
