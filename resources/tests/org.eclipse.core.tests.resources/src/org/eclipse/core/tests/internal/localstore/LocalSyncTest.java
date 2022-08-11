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
package org.eclipse.core.tests.internal.localstore;

import org.eclipse.core.internal.resources.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;

public class LocalSyncTest extends LocalStoreTest implements ICoreConstants {

	public void assertExistsInFileSystemWithNoContent(IFile target) {
		assertTrue(existsInFileSystemWithNoContent(target));
	}

	@Override
	public String[] defineHierarchy() {
		return new String[] {"/File1", "/Folder1/", "/Folder1/File1", "/Folder1/Folder2/"};
	}

	private boolean existsInFileSystemWithNoContent(IResource resource) {
		IPath path = resource.getLocation();
		return path.toFile().exists() && path.toFile().length() == 0;
	}

	public void testProjectDeletion() {
		/* initialize common objects */
		Project project = (Project) projects[0];

		//snapshot will recreate the deleted .project file
		TestingSupport.waitForSnapshot();

		// create resources
		IResource[] resources = buildResources(project, defineHierarchy());
		ensureExistsInWorkspace(resources, true);

		// delete project's default directory
		Workspace.clear(project.getLocation().toFile());

		// run synchronize
		//The .project file has been deleted, so this will fail
		try {
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
			fail("1.0");
		} catch (CoreException e) {
			// expected
		}

		/* project should still exists */
		assertTrue("1.1", project.exists());

		/* resources should not exist anymore */
		for (int i = 1; i < resources.length; i++) {
			assertTrue("1.2", !resources[i].exists());
		}
	}

	public void testProjectWithNoResources() {
		/* initialize common objects */
		Project project = (Project) projects[0];

		try {
			/* check normal behaviour */
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			fail("1.0", e);
		}
		assertTrue("1.1", project.exists());
	}

	/**
	 * Simple synchronization test. Uses one solution and one project.
	 */
	public void testSimpleSync() {
		/* initialize common objects */
		Project project = (Project) projects[0];

		// create resource handles
		IResource index = project.getFile(new Path("index.html"));
		IResource toc = project.getFile(new Path("toc.html"));
		IResource file = project.getFile(new Path("file"));
		IResource folder = project.getFolder(new Path("folder"));

		// add resources to the workspace
		ensureExistsInWorkspace((IFile) index, "");
		ensureExistsInWorkspace(toc, true);
		ensureExistsInWorkspace((IFile) file, "");
		ensureExistsInWorkspace(folder, true);

		try {
			// run synchronize
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			fail("0.0", e);
		}

		//-----------------------------------------------------------
		// test synchronize
		assertExistsInWorkspace(index);
		assertExistsInFileSystemWithNoContent((IFile) index);
		//
		assertExistsInWorkspace(toc);
		assertExistsInFileSystem(toc);
		assertTrue(toc.exists());
		//
		assertExistsInWorkspace(file);
		assertExistsInFileSystemWithNoContent((IFile) file);
		assertTrue(file.exists());
		assertTrue(file.getType() == IResource.FILE);
		//
		assertExistsInWorkspace(folder);
		assertTrue(folder.getType() == IResource.FOLDER);
		//-----------------------------------------------------------

		// make some modifications in the local resources
		// index stays the same
		ensureDoesNotExistInFileSystem(toc);
		//
		ensureDoesNotExistInFileSystem(file);
		ensureDoesNotExistInFileSystem(folder);
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			fail("3.0", e);
		}
		file = project.getFolder(new Path("file"));
		folder = project.getFile(new Path("folder"));
		ensureExistsInFileSystem(file);
		ensureExistsInFileSystem((IFile) folder);

		try {
			// run synchronize
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			fail("4.0", e);
		}

		//-----------------------------------------------------------
		// test synchronize
		assertExistsInWorkspace(index);
		assertExistsInFileSystemWithNoContent((IFile) index);
		//
		assertDoesNotExistInFileSystem(toc);
		assertDoesNotExistInWorkspace(toc);
		//
		assertExistsInWorkspace(file);
		assertExistsInFileSystem(file);
		assertTrue(file.exists());
		assertTrue(file.getType() == IResource.FOLDER);
		//
		assertExistsInWorkspace(folder);
		assertExistsInFileSystem(folder);
		assertTrue(file.exists());
		assertTrue(folder.getType() == IResource.FILE);
		//-----------------------------------------------------------
	}
}
