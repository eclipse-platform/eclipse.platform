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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.eclipse.core.internal.localstore.SafeFileInputStream;
import org.eclipse.core.internal.localstore.SafeFileOutputStream;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.resources.ResourceTest;

public class SafeFileInputOutputStreamTest extends ResourceTest {
	protected IPath temp;

	public SafeFileOutputStream createSafeStream(File target) throws IOException {
		return createSafeStream(target.getAbsolutePath(), null);
	}

	public SafeFileOutputStream createSafeStream(String targetPath, String tempFilePath)
			throws IOException {
		return new SafeFileOutputStream(targetPath, tempFilePath);
	}

	public InputStream getContents(java.io.File target) throws IOException {
		return new SafeFileInputStream(target);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		IPath location = getRandomLocation();
		temp = location.append("temp");
		temp.toFile().mkdirs();
		deleteOnTearDown(temp);
		assertTrue("could not create temp directory", temp.toFile().isDirectory());
	}

	public void testSafeFileInputStream() throws IOException {
		File target = new File(temp.toFile(), "target");
		Workspace.clear(target); // make sure there was nothing here before
		assertFalse(target.exists());

		// define temp path
		IPath parentLocation = IPath.fromOSString(target.getParentFile().getAbsolutePath());
		IPath tempLocation = parentLocation.append(target.getName() + ".backup");
		String contents = getRandomString();
		File tempFile = tempLocation.toFile();

		// we did not have a file on the destination, so we should not have a temp file
		try (SafeFileOutputStream safeStream = createSafeStream(target.getAbsolutePath(), tempLocation.toOSString())) {
			getContents(contents).transferTo(safeStream);
		}
		// now we should have a temp file
		try(SafeFileOutputStream safeStream = createSafeStream(target.getAbsolutePath(), tempLocation.toOSString())) {
			getContents(contents).transferTo(safeStream);
		}
		assertTrue(target.exists());
		assertFalse(tempFile.exists());
		InputStream diskContents = new SafeFileInputStream(tempLocation.toOSString(), target.getAbsolutePath());
		assertTrue(compareContent(diskContents, getContents(contents)));
		Workspace.clear(target); // make sure there was nothing here before
	}

	public void testSimple() throws IOException {
		File target = new File(temp.toFile(), "target");
		Workspace.clear(target); // make sure there was nothing here before
		assertTrue(!target.exists());
		String contents = getRandomString();

		// basic use (like a FileOutputStream)
		try (SafeFileOutputStream safeStream = createSafeStream(target)) {
			getContents(contents).transferTo(safeStream);
		}
		InputStream diskContents = getContents(target);
		assertTrue(compareContent(diskContents, getContents(contents)));

		contents = getRandomString();
		// update target contents
		File tempFile;
		try (SafeFileOutputStream safeStream = createSafeStream(target)) {
			tempFile = new File(safeStream.getTempFilePath());
			assertTrue(tempFile.exists());
			getContents(contents).transferTo(safeStream);
		}
		assertFalse(tempFile.exists());
		diskContents = getContents(target);
		assertTrue(compareContent(diskContents, getContents(contents)));
		Workspace.clear(target); // make sure there was nothing here before
	}

	public void testSpecifiedTempFile() throws IOException {
		File target = new File(temp.toFile(), "target");
		Workspace.clear(target); // make sure there was nothing here before
		assertTrue(!target.exists());

		// define temp path
		IPath parentLocation = IPath.fromOSString(target.getParentFile().getAbsolutePath());
		IPath tempLocation = parentLocation.append(target.getName() + ".backup");

		String contents = getRandomString();
		File tempFile = tempLocation.toFile();
		// we did not have a file on the destination, so we should not have a temp file
		try (SafeFileOutputStream safeStream = createSafeStream(target.getAbsolutePath(), tempLocation.toOSString())) {
			assertFalse(tempFile.exists());
			// update target contents
			getContents(contents).transferTo(safeStream);
		}
		assertFalse(tempFile.exists());
		InputStream diskContents = getContents(target);
		assertTrue(compareContent(diskContents, getContents(contents)));

		contents = getRandomString();
		// now we should have a temp file
		try (SafeFileOutputStream safeStream = createSafeStream(target.getAbsolutePath(), tempLocation.toOSString())) {
			assertTrue(tempFile.exists());
			// update target contents
			getContents(contents).transferTo(safeStream);
		}
		assertFalse(tempFile.exists());
		diskContents = getContents(target);
		assertTrue(compareContent(diskContents, getContents(contents)));
		Workspace.clear(target); // make sure there was nothing here before
	}
}
