/*******************************************************************************
 * Copyright (c) 2023 Simeon Andreev and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Simeon Andreev - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources;

import java.nio.file.*;
import java.util.List;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IPath;

/**
 *
 */
public class ProjectLinksTest extends ResourceTest {

	private IProject project;
	private Path tmpFolder;
	private IPath tmpPath;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		tmpPath = getRandomLocation();
		tmpFolder = Paths.get(tmpPath.toOSString());
		Files.createDirectory(tmpFolder);

		IWorkspaceRoot root = getWorkspace().getRoot();
		project = root.getProject(getUniqueString());

		project.create(getMonitor());
		project.open(getMonitor());
		project.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
	}

	@Override
	protected void tearDown() throws Exception {
		try {
			Files.deleteIfExists(tmpFolder);
			project.delete(true, getMonitor());
		} finally {
			super.tearDown();
		}
	}

	/**
	 * Tests that link information is updated after closing a project, deleting a
	 * link in the {@code .project} file and then opening the project.
	 */
	public void testCloseProjectDeleteLinksAndOpen_GH470() throws Exception {
		IFile dotProject = project.getFile(".project");
		Path dotProjectPath = Paths.get(dotProject.getLocationURI());
		List<String> dotProjectContentsWithoutLink = Files.readAllLines(dotProjectPath);

		String linkedFolderName = "test";
		IFolder folder = project.getFolder(linkedFolderName);
		folder.createLink(tmpPath, IResource.NONE, getMonitor());
		project.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());

		assertTrue("Failed to create linked folder in test project", folder.isLinked());

		project.close(getMonitor());

		Files.write(dotProjectPath, dotProjectContentsWithoutLink);

		project.open(getMonitor());
		project.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());

		folder = project.getFolder(linkedFolderName);
		assertFalse("Expected folder to not be linked after re-opening project", folder.isLinked());
	}

	/**
	 * Tests that link information is correct after closing a project and then
	 * opening the project.
	 */
	public void testCloseAndOpenProject() throws Exception {
		String linkedFolderName = "test";
		IFolder folder = project.getFolder(linkedFolderName);
		folder.createLink(tmpPath, IResource.NONE, getMonitor());
		project.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());

		assertTrue("Failed to create linked folder in test project", folder.isLinked());

		project.close(getMonitor());

		project.open(getMonitor());
		project.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());

		folder = project.getFolder(linkedFolderName);
		assertTrue("Expected folder to be linked after re-opening project", folder.isLinked());
	}
}
