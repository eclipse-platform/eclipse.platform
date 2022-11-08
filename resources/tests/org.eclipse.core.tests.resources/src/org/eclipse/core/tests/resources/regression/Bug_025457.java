/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources.regression;

import java.io.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.ResourceTest;

/**
 * Tests regression of bug 25457.  In this case, attempting to move a project
 * that is only a case change, where the move fails due to another handle being
 * open on a file in the hierarchy, would cause deletion of the source.
 *
 * Note: this is similar to Bug_32076, which deals with failure to move in
 * the non case-change scenario.
 */
public class Bug_025457 extends ResourceTest {

	public void testFile() throws IOException, CoreException {
		//this test only works on windows
		if (!isWindows()) {
			return;
		}
		IProject source = getWorkspace().getRoot().getProject("project");
		IFile sourceFile = source.getFile("file.txt");
		IFile destFile = source.getFile("File.txt");
		ensureExistsInWorkspace(source, true);
		final String content = getRandomString();
		ensureExistsInWorkspace(sourceFile, content);

		//open a stream in the source to cause the rename to fail
		try (InputStream stream = sourceFile.getContents()) {
			//try to rename the file (should fail)
			try {
				sourceFile.move(destFile.getFullPath(), IResource.NONE, getMonitor());
				fail("1.99");
			} catch (CoreException e1) {
				//should fail
			}
		}
		//ensure source still exists and has same content
		assertTrue("2.0", source.exists());
		assertTrue("2.1", sourceFile.exists());
		try (InputStream stream = sourceFile.getContents()) {
			assertTrue("2.2", compareContent(stream, new ByteArrayInputStream(content.getBytes())));
		}
		//ensure destination file does not exist
		assertTrue("2.3", !destFile.exists());
	}

	public void testFolder() throws IOException, CoreException {
		//this test only works on windows
		//native code must also be present so move can detect the case change
		if (!isWindows() || !isReadOnlySupported()) {
			return;
		}
		IProject source = getWorkspace().getRoot().getProject("SourceProject");
		IFolder sourceFolder = source.getFolder("folder");
		IFile sourceFile = sourceFolder.getFile("Important.txt");
		IFolder destFolder = source.getFolder("Folder");
		IFile destFile = destFolder.getFile("Important.txt");
		ensureExistsInWorkspace(source, true);
		ensureExistsInWorkspace(sourceFolder, true);
		ensureExistsInWorkspace(sourceFile, true);

		//open a stream in the source to cause the rename to fail
		try (InputStream stream = sourceFile.getContents()) {
			//try to rename the project (should fail)
			try {
				sourceFolder.move(destFolder.getFullPath(), IResource.NONE, getMonitor());
				fail("1.99");
			} catch (CoreException e1) {
				//should fail
			}
			//ensure source still exists
			assertTrue("2.0", source.exists());
			assertTrue("2.1", sourceFolder.exists());
			assertTrue("2.2", sourceFile.exists());

			//ensure destination does not exist
			assertTrue("2.3", !destFolder.exists());
			assertTrue("2.4", !destFile.exists());
		}
	}

	public void testProject() throws IOException, CoreException {
		//this test only works on windows
		if (!isWindows()) {
			return;
		}
		IProject source = getWorkspace().getRoot().getProject("project");
		IProject destination = getWorkspace().getRoot().getProject("Project");
		IFile sourceFile = source.getFile("Important.txt");
		IFile destFile = destination.getFile("Important.txt");
		ensureExistsInWorkspace(source, true);
		ensureExistsInWorkspace(sourceFile, true);

		//open a stream in the source to cause the rename to fail
		try (InputStream stream = sourceFile.getContents()) {
			//try to rename the project (should fail)
			try {
				source.move(destination.getFullPath(), IResource.NONE, getMonitor());
				fail("1.99");
			} catch (CoreException e1) {
				//should fail
			}
			//ensure source does not exist
			assertTrue("2.0", !source.exists());
			assertTrue("2.1", !sourceFile.exists());

			//ensure destination does not exist
			assertTrue("2.2", destination.exists());
			assertTrue("2.3", destFile.exists());
		}
	}
}
