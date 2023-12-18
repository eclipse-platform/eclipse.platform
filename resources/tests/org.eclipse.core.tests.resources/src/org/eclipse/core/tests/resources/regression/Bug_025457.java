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

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.compareContent;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomString;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.isReadOnlySupported;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform.OS;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests regression of bug 25457.  In this case, attempting to move a project
 * that is only a case change, where the move fails due to another handle being
 * open on a file in the hierarchy, would cause deletion of the source.
 *
 * Note: this is similar to Bug_32076, which deals with failure to move in
 * the non case-change scenario.
 */
public class Bug_025457 {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	@Test
	public void testFile() throws Exception {
		assumeTrue("test only works on Windows", OS.isWindows());

		IProject source = getWorkspace().getRoot().getProject("project");
		IFile sourceFile = source.getFile("file.txt");
		IFile destFile = source.getFile("File.txt");
		createInWorkspace(source);
		final String content = createRandomString();
		createInWorkspace(sourceFile, content);

		//open a stream in the source to cause the rename to fail
		try (InputStream stream = sourceFile.getContents()) {
			//try to rename the file (should fail)
			assertThrows(CoreException.class,
					() -> sourceFile.move(destFile.getFullPath(), IResource.NONE, createTestMonitor()));
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

	@Test
	public void testFolder() throws IOException, CoreException {
		//native code must also be present so move can detect the case change
		assumeTrue("test only works on Windows", OS.isWindows() && isReadOnlySupported());

		IProject source = getWorkspace().getRoot().getProject("SourceProject");
		IFolder sourceFolder = source.getFolder("folder");
		IFile sourceFile = sourceFolder.getFile("Important.txt");
		IFolder destFolder = source.getFolder("Folder");
		IFile destFile = destFolder.getFile("Important.txt");
		createInWorkspace(source);
		createInWorkspace(sourceFolder);
		createInWorkspace(sourceFile);

		//open a stream in the source to cause the rename to fail
		try (InputStream stream = sourceFile.getContents()) {
			//try to rename the project (should fail)
			assertThrows(CoreException.class,
					() -> sourceFolder.move(destFolder.getFullPath(), IResource.NONE, createTestMonitor()));
			//ensure source still exists
			assertTrue("2.0", source.exists());
			assertTrue("2.1", sourceFolder.exists());
			assertTrue("2.2", sourceFile.exists());

			//ensure destination does not exist
			assertTrue("2.3", !destFolder.exists());
			assertTrue("2.4", !destFile.exists());
		}
	}

	@Test
	public void testProject() throws IOException, CoreException {
		assumeTrue("test only works on Windows", OS.isWindows());

		IProject source = getWorkspace().getRoot().getProject("project");
		IProject destination = getWorkspace().getRoot().getProject("Project");
		IFile sourceFile = source.getFile("Important.txt");
		IFile destFile = destination.getFile("Important.txt");
		createInWorkspace(source);
		createInWorkspace(sourceFile);

		//open a stream in the source to cause the rename to fail
		try (InputStream stream = sourceFile.getContents()) {
			//try to rename the project (should fail)
			assertThrows(CoreException.class,
					() -> source.move(destination.getFullPath(), IResource.NONE, createTestMonitor()));

			//ensure source does not exist
			assertTrue("2.0", !source.exists());
			assertTrue("2.1", !sourceFile.exists());

			//ensure destination does not exist
			assertTrue("2.2", destination.exists());
			assertTrue("2.3", destFile.exists());
		}
	}
}
