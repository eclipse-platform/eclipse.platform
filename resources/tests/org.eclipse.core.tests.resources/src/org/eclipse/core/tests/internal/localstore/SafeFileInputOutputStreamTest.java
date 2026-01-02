/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.core.tests.internal.localstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.tests.harness.FileSystemHelper.getRandomLocation;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInputStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.eclipse.core.internal.localstore.SafeFileInputStream;
import org.eclipse.core.internal.localstore.SafeFileOutputStream;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.resources.util.FileStoreAutoDeleteExtension;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

@ExtendWith(WorkspaceResetExtension.class)
public class SafeFileInputOutputStreamTest {

	@RegisterExtension
	private final FileStoreAutoDeleteExtension fileStoreExtension = new FileStoreAutoDeleteExtension();

	private IPath temp;

	private SafeFileOutputStream createSafeStream(File target) throws IOException {
		return createSafeStream(target.getAbsolutePath(), null);
	}

	private SafeFileOutputStream createSafeStream(String targetPath, String tempFilePath)
			throws IOException {
		return new SafeFileOutputStream(targetPath, tempFilePath);
	}

	private InputStream getContents(java.io.File target) throws IOException {
		return new SafeFileInputStream(target);
	}

	@BeforeEach
	public void setUp() throws Exception {
		temp = getRandomLocation().append("temp");
		temp.toFile().mkdirs();
		fileStoreExtension.deleteOnTearDown(temp);
		assertTrue(temp.toFile().isDirectory(), "could not create temp directory");
	}

	@Test
	public void testSafeFileInputStream() throws IOException {
		File target = new File(temp.toFile(), "target");
		Workspace.clear(target); // make sure there was nothing here before
		assertFalse(target.exists());

		// define temp path
		IPath parentLocation = IPath.fromOSString(target.getParentFile().getAbsolutePath());
		IPath tempLocation = parentLocation.append(target.getName() + ".backup");
		String contents = createRandomString();
		File tempFile = tempLocation.toFile();

		// we did not have a file on the destination, so we should not have a temp file
		try (SafeFileOutputStream safeStream = createSafeStream(target.getAbsolutePath(), tempLocation.toOSString())) {
			createInputStream(contents).transferTo(safeStream);
		}
		// now we should have a temp file
		try(SafeFileOutputStream safeStream = createSafeStream(target.getAbsolutePath(), tempLocation.toOSString())) {
			createInputStream(contents).transferTo(safeStream);
		}
		assertTrue(target.exists());
		assertFalse(tempFile.exists());
		try (InputStream diskContents = new SafeFileInputStream(tempLocation.toOSString(), target.getAbsolutePath())) {
			assertThat(diskContents).hasContent(contents);
		}
		Workspace.clear(target); // make sure there was nothing here before
	}

	@Test
	public void testSimple() throws IOException {
		File target = new File(temp.toFile(), "target");
		Workspace.clear(target); // make sure there was nothing here before
		assertTrue(!target.exists());
		String contents = createRandomString();

		// basic use (like a FileOutputStream)
		try (SafeFileOutputStream safeStream = createSafeStream(target)) {
			createInputStream(contents).transferTo(safeStream);
		}
		try (InputStream diskContents = getContents(target)) {
			assertThat(diskContents).hasContent(contents);
		}

		contents = createRandomString();
		// update target contents
		File tempFile;
		try (SafeFileOutputStream safeStream = createSafeStream(target)) {
			tempFile = new File(safeStream.getTempFilePath());
			assertTrue(tempFile.exists());
			createInputStream(contents).transferTo(safeStream);
		}
		assertFalse(tempFile.exists());
		try (InputStream diskContents = getContents(target)) {
			assertThat(diskContents).hasContent(contents);
		}
		Workspace.clear(target); // make sure there was nothing here before
	}

	@Test
	public void testSpecifiedTempFile() throws IOException {
		File target = new File(temp.toFile(), "target");
		Workspace.clear(target); // make sure there was nothing here before
		assertTrue(!target.exists());

		// define temp path
		IPath parentLocation = IPath.fromOSString(target.getParentFile().getAbsolutePath());
		IPath tempLocation = parentLocation.append(target.getName() + ".backup");

		String contents = createRandomString();
		File tempFile = tempLocation.toFile();
		// we did not have a file on the destination, so we should not have a temp file
		try (SafeFileOutputStream safeStream = createSafeStream(target.getAbsolutePath(), tempLocation.toOSString())) {
			assertFalse(tempFile.exists());
			// update target contents
			createInputStream(contents).transferTo(safeStream);
		}
		assertFalse(tempFile.exists());
		try (InputStream diskContents = getContents(target)) {
			assertThat(diskContents).hasContent(contents);
		}

		contents = createRandomString();
		// now we should have a temp file
		try (SafeFileOutputStream safeStream = createSafeStream(target.getAbsolutePath(), tempLocation.toOSString())) {
			assertTrue(tempFile.exists());
			// update target contents
			createInputStream(contents).transferTo(safeStream);
		}
		assertFalse(tempFile.exists());
		try (InputStream diskContents = getContents(target)) {
			assertThat(diskContents).hasContent(contents);
		}
		Workspace.clear(target); // make sure there was nothing here before
	}

}
