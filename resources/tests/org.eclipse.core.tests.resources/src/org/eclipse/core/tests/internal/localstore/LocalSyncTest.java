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

import static org.junit.Assert.assertThrows;

import org.eclipse.core.internal.resources.ICoreConstants;
import org.eclipse.core.internal.resources.Project;
import org.eclipse.core.internal.resources.TestingSupport;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

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
		assertThrows(CoreException.class, () -> project.refreshLocal(IResource.DEPTH_INFINITE, null));

		/* project should still exists */
		assertTrue(project.exists());

		/* resources should not exist anymore */
		for (int i = 1; i < resources.length; i++) {
			assertFalse("Resource does unexpectedly exist: " + resources[i], resources[i].exists());
		}
	}

	public void testProjectWithNoResources() throws CoreException {
		/* initialize common objects */
		Project project = (Project) projects[0];

		/* check normal behaviour */
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		assertTrue(project.exists());
	}

	/**
	 * Simple synchronization test. Uses one solution and one project.
	 */
	public void testSimpleSync() throws Exception {
		/* initialize common objects */
		Project project = (Project) projects[0];

		// create resource handles
		IResource index = project.getFile(IPath.fromOSString("index.html"));
		IResource toc = project.getFile(IPath.fromOSString("toc.html"));
		IResource file = project.getFile(IPath.fromOSString("file"));
		IResource folder = project.getFolder(IPath.fromOSString("folder"));

		// add resources to the workspace
		ensureExistsInWorkspace((IFile) index, "");
		ensureExistsInWorkspace(toc, true);
		ensureExistsInWorkspace((IFile) file, "");
		ensureExistsInWorkspace(folder, true);

		// run synchronize
		project.refreshLocal(IResource.DEPTH_INFINITE, null);

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

		Thread.sleep(5000);

		file = project.getFolder(IPath.fromOSString("file"));
		folder = project.getFile(IPath.fromOSString("folder"));
		ensureExistsInFileSystem(file);
		ensureExistsInFileSystem((IFile) folder);

		// run synchronize
		project.refreshLocal(IResource.DEPTH_INFINITE, null);

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
