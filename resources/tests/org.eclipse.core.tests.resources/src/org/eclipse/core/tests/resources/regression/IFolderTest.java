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

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
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
	public void testBug25662() {

		// We need to know whether or not we can unset the read-only flag
		// in order to perform this test.
		Assume.assumeTrue(isReadOnlySupported());

		// Only run this test on Linux for now since Windows lets you create
		// a file within a read-only folder.
		Assume.assumeTrue(isLinux());

		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IFolder parentFolder = project.getFolder("parentFolder");
		ensureExistsInWorkspace(new IResource[] {project, parentFolder}, true);
		IFolder folder = parentFolder.getFolder("folder");

		try {
			parentFolder.setReadOnly(true);
			assertTrue("0.0", parentFolder.isReadOnly());
			try {
				folder.create(true, true, getMonitor());
				fail("0.1");
			} catch (CoreException e) {
				assertEquals("0.2", IResourceStatus.PARENT_READ_ONLY, e.getStatus().getCode());
			}
		} finally {
			parentFolder.setReadOnly(false);
		}
	}

	/**
	 * Bug 11510 [resources] Non-local folders do not become local when directory is created.
	 */
	@Test
	public void testBug11510() {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject("TestProject");
		IFolder folder = project.getFolder("fold1");
		IFile subFile = folder.getFile("f1");
		IFile file = project.getFile("f2");
		ensureExistsInWorkspace(project, true);
		ensureExistsInWorkspace(new IResource[] {folder, file, subFile}, false);

		assertTrue("1.0", !folder.isLocal(IResource.DEPTH_ZERO));
		assertTrue("1.1", !file.isLocal(IResource.DEPTH_ZERO));
		assertTrue("1.1", !subFile.isLocal(IResource.DEPTH_ZERO));

		// now create the resources in the local file system and refresh
		ensureExistsInFileSystem(file);
		try {
			project.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		} catch (CoreException e) {
			fail("2.0", e);
		}
		assertTrue("2.1", file.isLocal(IResource.DEPTH_ZERO));
		assertTrue("2.2", !folder.isLocal(IResource.DEPTH_ZERO));
		assertTrue("2.3", !subFile.isLocal(IResource.DEPTH_ZERO));

		folder.getLocation().toFile().mkdir();
		try {
			project.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		} catch (CoreException e) {
			fail("3.0", e);
		}
		assertTrue("3.1", folder.isLocal(IResource.DEPTH_ZERO));
		assertTrue("3.2", file.isLocal(IResource.DEPTH_ZERO));
		assertTrue("3.3", !subFile.isLocal(IResource.DEPTH_ZERO));

		ensureExistsInFileSystem(subFile);
		try {
			project.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		} catch (CoreException e) {
			fail("4.0", e);
		}
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

		ensureExistsInWorkspace(project, true);
		ensureExistsInWorkspace(new IResource[] {folder}, true);

		IFileStore dir = EFS.getLocalFileSystem().fromLocalFile(folder.getLocation().toFile());
		assertTrue(dir.fetchInfo().exists());

		dir.mkdir(EFS.NONE, null);
		dir.mkdir(EFS.SHALLOW, null);
		// should not throw an exception
	}
}
