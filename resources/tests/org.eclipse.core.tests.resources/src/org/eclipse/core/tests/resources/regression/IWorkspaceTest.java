/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomContentsStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(WorkspaceResetExtension.class)
public class IWorkspaceTest {

	/**
	 * 1GDKIHD: ITPCORE:WINNT - API - IWorkspace.move needs to keep history
	 */
	@Test
	public void testMultiMove_1GDKIHD() throws CoreException {
		// create common objects
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		// test file (force = true)
		IFile file1 = project.getFile("file.txt");
		IFolder folder = project.getFolder("folder");
		IResource[] allResources = new IResource[] {file1, folder};
		folder.create(true, true, createTestMonitor());
		file1.create(createRandomContentsStream(), true, createTestMonitor());
		file1.setContents(createRandomContentsStream(), true, true, createTestMonitor());
		file1.setContents(createRandomContentsStream(), true, true, createTestMonitor());
		getWorkspace().move(new IFile[] { file1 }, folder.getFullPath(), true, createTestMonitor());
		file1.create(createRandomContentsStream(), true, createTestMonitor());
		IFileState[] states = file1.getHistory(createTestMonitor());
		assertThat(states).hasSize(3);
		getWorkspace().delete(allResources, true, createTestMonitor());
		project.clearHistory(createTestMonitor());

		// test file (force = false)
		folder.create(true, true, createTestMonitor());
		file1.create(createRandomContentsStream(), true, createTestMonitor());
		file1.setContents(createRandomContentsStream(), true, true, createTestMonitor());
		file1.setContents(createRandomContentsStream(), true, true, createTestMonitor());
		getWorkspace().move(new IFile[] { file1 }, folder.getFullPath(), false, createTestMonitor());
		file1.create(createRandomContentsStream(), true, createTestMonitor());
		states = file1.getHistory(createTestMonitor());
		assertThat(states).hasSize(3);
		getWorkspace().delete(allResources, true, createTestMonitor());
		project.clearHistory(createTestMonitor());
	}

	/**
	 * 1GDGRIZ: ITPCORE:WINNT - API - IWorkspace.delete needs to keep history
	 */
	@Test
	public void testMultiDelete_1GDGRIZ() throws CoreException {
		// create common objects
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		// test file (force = true)
		IFile file1 = project.getFile("file.txt");
		file1.create(createRandomContentsStream(), true, createTestMonitor());
		file1.setContents(createRandomContentsStream(), true, true, createTestMonitor());
		file1.setContents(createRandomContentsStream(), true, true, createTestMonitor());
		getWorkspace().delete(new IFile[] { file1 }, true, createTestMonitor());
		file1.create(createRandomContentsStream(), true, createTestMonitor());
		IFileState[] states = file1.getHistory(createTestMonitor());
		assertThat(states).hasSize(3);
		getWorkspace().delete(new IResource[] { file1 }, true, createTestMonitor());
		project.clearHistory(createTestMonitor());

		// test file (force = false)
		file1.create(createRandomContentsStream(), true, createTestMonitor());
		file1.setContents(createRandomContentsStream(), true, true, createTestMonitor());
		file1.setContents(createRandomContentsStream(), true, true, createTestMonitor());
		getWorkspace().delete(new IFile[] { file1 }, false, createTestMonitor());
		file1.create(createRandomContentsStream(), true, createTestMonitor());
		states = file1.getHistory(createTestMonitor());
		assertThat(states).hasSize(3);
		getWorkspace().delete(new IResource[] { file1 }, true, createTestMonitor());
		project.clearHistory(createTestMonitor());

		// test folder (force = true)
		IFolder folder = project.getFolder("folder");
		IFile file2 = folder.getFile("file2.txt");
		folder.create(true, true, createTestMonitor());
		file2.create(createRandomContentsStream(), true, createTestMonitor());
		file2.setContents(createRandomContentsStream(), true, true, createTestMonitor());
		file2.setContents(createRandomContentsStream(), true, true, createTestMonitor());
		getWorkspace().delete(new IResource[] { folder }, true, createTestMonitor());
		folder.create(true, true, createTestMonitor());
		file2.create(createRandomContentsStream(), true, createTestMonitor());
		states = file2.getHistory(createTestMonitor());
		assertThat(states).hasSize(3);
		getWorkspace().delete(new IResource[] { folder, file1, file2 }, true, createTestMonitor());
		project.clearHistory(createTestMonitor());

		// test folder (force = false)
		folder.create(true, true, createTestMonitor());
		file2.create(createRandomContentsStream(), true, createTestMonitor());
		file2.setContents(createRandomContentsStream(), true, true, createTestMonitor());
		file2.setContents(createRandomContentsStream(), true, true, createTestMonitor());
		getWorkspace().delete(new IResource[] { folder }, false, createTestMonitor());
		folder.create(true, true, createTestMonitor());
		file2.create(createRandomContentsStream(), true, createTestMonitor());
		states = file2.getHistory(createTestMonitor());
		assertThat(states).hasSize(3);
		getWorkspace().delete(new IResource[] { folder, file1, file2 }, true, createTestMonitor());
		project.clearHistory(createTestMonitor());
	}

	@Test
	public void test_8974(@TempDir Path tempDirectory) throws CoreException, IOException {
		IProject one = getWorkspace().getRoot().getProject("One");
		Path oneTempDirectory = tempDirectory.toRealPath().resolve(one.getName());
		IPath oneLocation = IPath.fromPath(oneTempDirectory);
		Files.createDirectories(oneTempDirectory);
		IProjectDescription oneDescription = getWorkspace().newProjectDescription(one.getName());
		oneDescription.setLocation(oneLocation);

		one.create(oneDescription, createTestMonitor());

		IProject two = getWorkspace().getRoot().getProject("Two");
		IPath twoLocation = oneLocation.removeLastSegments(1).append(oneLocation.lastSegment().toLowerCase());

		IStatus result = getWorkspace().validateProjectLocation(two, twoLocation);
		assertEquals(Workspace.caseSensitive, result.isOK());
	}

}
