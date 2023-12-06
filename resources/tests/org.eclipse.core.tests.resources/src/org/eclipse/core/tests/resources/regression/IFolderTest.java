/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.regression;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.isReadOnlySupported;
import static org.junit.Assert.assertThrows;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform.OS;
import org.eclipse.core.tests.resources.ResourceTest;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class IFolderTest extends ResourceTest {
	/**
	 * Bug requests that if a failed folder creation occurs on Linux that we check
	 * the immediate parent to see if it is read-only so we can return a better
	 * error code and message to the user.
	 */
	@Test
	public void testBug25662() throws CoreException {

		// We need to know whether or not we can unset the read-only flag
		// in order to perform this test.
		Assume.assumeTrue(isReadOnlySupported());

		// Only run this test on Linux for now since Windows lets you create
		// a file within a read-only folder.
		Assume.assumeTrue(OS.isLinux());

		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IFolder parentFolder = project.getFolder("parentFolder");
		ensureExistsInWorkspace(new IResource[] {project, parentFolder});
		IFolder folder = parentFolder.getFolder("folder");

		try {
			parentFolder.setReadOnly(true);
			assertTrue(parentFolder.isReadOnly());
			CoreException exception = assertThrows(CoreException.class, () -> folder.create(true, true, createTestMonitor()));
			assertEquals(IResourceStatus.PARENT_READ_ONLY, exception.getStatus().getCode());
		} finally {
			parentFolder.setReadOnly(false);
		}
	}

	/**
	 * Bug 11510 [resources] Non-local folders do not become local when directory is created.
	 */
	@Test
	public void testBug11510() throws Exception {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject("TestProject");
		IFolder folder = project.getFolder("fold1");
		IFile subFile = folder.getFile("f1");
		IFile file = project.getFile("f2");
		ensureExistsInWorkspace(project);
		folder.create(true, false, createTestMonitor());
		file.create(null, true, createTestMonitor());
		subFile.create(null, true, createTestMonitor());

		assertTrue("1.0", !folder.isLocal(IResource.DEPTH_ZERO));
		assertTrue("1.1", !file.isLocal(IResource.DEPTH_ZERO));
		assertTrue("1.1", !subFile.isLocal(IResource.DEPTH_ZERO));

		// now create the resources in the local file system and refresh
		createInFileSystem(file);
		project.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		assertTrue("2.1", file.isLocal(IResource.DEPTH_ZERO));
		assertTrue("2.2", !folder.isLocal(IResource.DEPTH_ZERO));
		assertTrue("2.3", !subFile.isLocal(IResource.DEPTH_ZERO));

		folder.getLocation().toFile().mkdir();
		project.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		assertTrue("3.1", folder.isLocal(IResource.DEPTH_ZERO));
		assertTrue("3.2", file.isLocal(IResource.DEPTH_ZERO));
		assertTrue("3.3", !subFile.isLocal(IResource.DEPTH_ZERO));

		createInFileSystem(subFile);
		project.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		assertTrue("4.1", subFile.isLocal(IResource.DEPTH_ZERO));
		assertTrue("4.2", folder.isLocal(IResource.DEPTH_ZERO));
		assertTrue("4.3", file.isLocal(IResource.DEPTH_ZERO));

	}

	/**
	 * Bug 514831: "shallow" mkdir fails if the directory already exists
	 */
	@Test
	public void testBug514831() throws CoreException {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject("TestProject");
		IFolder folder = project.getFolder("folder");

		ensureExistsInWorkspace(project);
		ensureExistsInWorkspace(new IResource[] {folder});

		IFileStore dir = EFS.getLocalFileSystem().fromLocalFile(folder.getLocation().toFile());
		assertTrue(dir.fetchInfo().exists());

		dir.mkdir(EFS.NONE, null);
		dir.mkdir(EFS.SHALLOW, null);
		// should not throw an exception
	}
}
