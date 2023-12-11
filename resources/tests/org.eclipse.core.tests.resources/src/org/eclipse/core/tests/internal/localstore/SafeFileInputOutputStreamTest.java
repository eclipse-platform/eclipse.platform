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

import static org.eclipse.core.tests.harness.FileSystemHelper.getRandomLocation;
import static org.eclipse.core.tests.resources.ResourceTestUtil.compareContent;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInputStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.eclipse.core.internal.localstore.SafeFileInputStream;
import org.eclipse.core.internal.localstore.SafeFileOutputStream;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class SafeFileInputOutputStreamTest {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

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

	@Before
	public void setUp() throws Exception {
		temp = getRandomLocation().append("temp");
		temp.toFile().mkdirs();
		workspaceRule.deleteOnTearDown(temp);
		assertTrue("could not create temp directory", temp.toFile().isDirectory());
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
		InputStream diskContents = new SafeFileInputStream(tempLocation.toOSString(), target.getAbsolutePath());
		assertTrue(compareContent(diskContents, createInputStream(contents)));
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
		InputStream diskContents = getContents(target);
		assertTrue(compareContent(diskContents, createInputStream(contents)));

		contents = createRandomString();
		// update target contents
		File tempFile;
		try (SafeFileOutputStream safeStream = createSafeStream(target)) {
			tempFile = new File(safeStream.getTempFilePath());
			assertTrue(tempFile.exists());
			createInputStream(contents).transferTo(safeStream);
		}
		assertFalse(tempFile.exists());
		diskContents = getContents(target);
		assertTrue(compareContent(diskContents, createInputStream(contents)));
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
		InputStream diskContents = getContents(target);
		assertTrue(compareContent(diskContents, createInputStream(contents)));

		contents = createRandomString();
		// now we should have a temp file
		try (SafeFileOutputStream safeStream = createSafeStream(target.getAbsolutePath(), tempLocation.toOSString())) {
			assertTrue(tempFile.exists());
			// update target contents
			createInputStream(contents).transferTo(safeStream);
		}
		assertFalse(tempFile.exists());
		diskContents = getContents(target);
		assertTrue(compareContent(diskContents, createInputStream(contents)));
		Workspace.clear(target); // make sure there was nothing here before
	}

}
