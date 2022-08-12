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
package org.eclipse.core.tests.internal.localstore;

import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

public class DeleteTest extends LocalStoreTest {

	public void testDeleteOpenProject() {
		IProject project = projects[0];
		IFolder folder = project.getFolder("folder");
		IFile file = folder.getFile("file");

		/* ===========================================================
		 * project is OPEN, deleteContents=FALSE, force=TRUE
		 * =========================================================== */

		/* create some resources */
		ensureExistsInWorkspace(new IResource[] {project, folder, file}, true);
		IPath folderPath = folder.getLocation();
		IPath filePath = file.getLocation();
		IPath projectLocation = project.getLocation();

		/* delete */
		try {
			project.delete(false, true, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}

		/* assert project does not exist anymore in the workspace*/
		assertTrue("1.1", !project.exists());
		assertTrue("1.2", !((Workspace) getWorkspace()).getMetaArea().locationFor(project).toFile().exists());
		assertNull("1.3", project.getLocation());

		/* assert resources still exist */
		assertTrue("1.4", folderPath.toFile().isDirectory());
		assertTrue("1.5", filePath.toFile().isFile());

		/* remove trash */
		Workspace.clear(projectLocation.toFile());

		/* ===========================================================
		 * project is OPEN, deleteContents=TRUE, force=TRUE
		 * 	- uses default default mapping
		 * ========================================================== */

		/* initialize common objects */
		ensureExistsInWorkspace(new IResource[] {project, folder, file}, true);
		folderPath = folder.getLocation();
		filePath = file.getLocation();
		projectLocation = project.getLocation();

		/* delete */
		try {
			project.delete(true, true, getMonitor());
		} catch (CoreException e) {
			fail("2.0", e);
		}

		/* assert project does not exist anymore in the workspace */
		assertTrue("2.1", !project.exists());
		assertTrue("2.2", !((Workspace) getWorkspace()).getMetaArea().locationFor(project).toFile().exists());
		assertNull("2.3", project.getLocation());

		/* assert resources do not exist anymore */
		assertTrue("2.4", !projectLocation.toFile().exists());
		assertTrue("2.5", !folderPath.toFile().exists());
		assertTrue("2.6", !filePath.toFile().exists());

		/* ===========================================================
		 * project is OPEN, deleteContents=TRUE, force=TRUE
		 * 	- defines default mapping
		 * 	- does not create resources on disk
		 * =========================================================== */

		/* initialize common objects */
		ensureExistsInWorkspace(project, true);
		ensureExistsInWorkspace(new IResource[] {folder, file}, false);
		folderPath = folder.getLocation();
		filePath = file.getLocation();
		projectLocation = project.getLocation();

		/* delete */
		try {
			project.delete(true, true, getMonitor());
		} catch (CoreException e) {
			fail("3.0", e);
		}

		/* assert project does not exist anymore */
		assertTrue("3.1", !project.exists());
		assertTrue("3.2", !((Workspace) getWorkspace()).getMetaArea().locationFor(project).toFile().exists());
		assertNull("3.3", project.getLocation());

		/* assert resources do not exist anymore */
		assertTrue("3.4", !folderPath.toFile().isDirectory());
		assertTrue("3.5", !filePath.toFile().isFile());

		/* ===========================================================
		 * project is OPEN, deleteContents=TRUE, force=true
		 * 	- create resources at default default area
		 * 	- defines default mapping
		 * =========================================================== */

		/* initialize common objects */
		ensureExistsInWorkspace(new IResource[] {project, folder, file}, true);
		folderPath = folder.getLocation();
		filePath = file.getLocation();

		/* delete */
		try {
			project.delete(true, true, getMonitor());
		} catch (CoreException e) {
			fail("4.0", e);
		}

		/* assert project does not exist anymore */
		assertTrue("6.1", !project.exists());
		assertTrue("6.2", !((Workspace) getWorkspace()).getMetaArea().locationFor(project).toFile().exists());
		assertNull("6.3", project.getLocation());

		/* assert resources do not still exist at default default area */
		assertTrue("6.4", !folderPath.toFile().exists());
		assertTrue("6.5", !filePath.toFile().exists());

		/* remove trash */
		Workspace.clear(folderPath.toFile());

		/* ===========================================================
		 * project is OPEN, deleteContents=TRUE, force=TRUE
		 * 	- defines default mapping
		 * 	- creates resources only on disk
		 * =========================================================== */

		/* initialize common objects */
		ensureExistsInWorkspace(project, true);
		ensureExistsInFileSystem(new IResource[] {folder, file});
		folderPath = folder.getLocation();
		filePath = file.getLocation();
		projectLocation = project.getLocation();

		/* delete */
		try {
			project.delete(true, true, getMonitor());
		} catch (CoreException e) {
			fail("7.0", e);
		}

		/* assert project does not exist anymore */
		assertTrue("7.1", !project.exists());
		assertTrue("7.2", !((Workspace) getWorkspace()).getMetaArea().locationFor(project).toFile().exists());
		assertNull("7.3", project.getLocation());

		/* assert resources do not exist anymore */
		assertTrue("7.4", !folderPath.toFile().isDirectory());
		assertTrue("7.5", !filePath.toFile().isFile());

	}

	public void testDeleteClosedProject() throws Throwable {

		IProject project = projects[0];
		IFolder folder = project.getFolder("folder");
		IFile file = folder.getFile("file");

		/* ===========================================================
		 * project is CLOSED, deleteContents=FALSE, force=TRUE
		 * 	- resources exist in workspace but not on disk
		 * =========================================================== */

		/* initialize common objects */
		ensureExistsInWorkspace(new IResource[] {project, folder}, true);
		ensureExistsInWorkspace(file, false);
		IPath folderPath = folder.getLocation();
		IPath filePath = file.getLocation();
		IPath projectLocation = project.getLocation();

		/* close and delete */
		try {
			project.close(getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}
		try {
			project.delete(false, true, getMonitor());
		} catch (CoreException e) {
			fail("1.1", e);
		}

		/* assert project does not exist anymore in the workspace */
		assertTrue("1.2", !project.exists());
		assertTrue("1.3", !((Workspace) getWorkspace()).getMetaArea().locationFor(project).toFile().exists());
		assertNull("1.4", project.getLocation());

		/* assert resources still exist (if appropriate) */
		assertTrue("1.5", folderPath.toFile().exists());
		assertTrue("1.6", !filePath.toFile().exists());

		/* remove trash */
		Workspace.clear(projectLocation.toFile());

		/* ===========================================================
		 * project is CLOSED, deleteContents=TRUE, force=TRUE
		 * 	- uses default default mapping
		 * =========================================================== */

		/* initialize common objects */
		ensureExistsInWorkspace(new IResource[] {project, folder, file}, true);
		folderPath = folder.getLocation();
		filePath = file.getLocation();

		/* close and delete */
		try {
			project.close(getMonitor());
		} catch (CoreException e) {
			fail("2.0", e);
		}
		try {
			project.delete(true, true, getMonitor());
		} catch (CoreException e) {
			fail("2.1", e);
		}

		/* assert project does not exist anymore */
		assertTrue("2.2", !project.exists());
		assertTrue("2.3", !((Workspace) getWorkspace()).getMetaArea().locationFor(project).toFile().exists());
		assertNull("2.4", project.getLocation());

		/* assert resources do not exist anymore */
		assertTrue("2.5", !folderPath.toFile().isDirectory());
		assertTrue("2.6", !filePath.toFile().isFile());

		/* ===========================================================
		 * project is CLOSED, deleteContents=TRUE, force = FALSE
		 * 	- uses default default mapping
		 * =========================================================== */

		/* initialize common objects */
		ensureExistsInWorkspace(new IResource[] {project, folder}, true);
		ensureExistsInWorkspace(file, false);
		folderPath = folder.getLocation();
		filePath = file.getLocation();
		projectLocation = project.getLocation();

		/* close and delete */
		try {
			projects[0].close(getMonitor());
		} catch (CoreException e) {
			fail("3.0", e);
		}
		try {
			projects[0].delete(true, false, getMonitor());
		} catch (CoreException e) {
			fail("3.1", e);
		}

		/* assert project was deleted */
		assertTrue("3.2", !project.exists());
		IPath metaAreaLocation = ((Workspace) getWorkspace()).getMetaArea().locationFor(project);
		assertTrue("3.3", !metaAreaLocation.toFile().exists());
		assertTrue("3.4", !metaAreaLocation.append(".properties").toFile().exists());
		assertTrue("3.5", !projectLocation.append(IProjectDescription.DESCRIPTION_FILE_NAME).toFile().exists());
		assertNull("3.6", project.getLocation());

		/* assert resources do not exist anymore */
		assertTrue("3.7", !folderPath.toFile().exists());
		assertTrue("3.8", !filePath.toFile().exists());
	}

	public void testDeleteResource() throws Throwable {
		/* test's hierarchy

		 P0
		 |
		 |-- folder
		 |
		 |-- fileSync
		 |
		 |-- fileUnsync
		 |
		 |-- fileCreated
		 |
		 |-- subfolderSync
		 |	|
		 |	|-- deletedfolderSync
		 |
		 |-- subfolderUnsync
		 |	|
		 |	|-- subsubfolderUnsync
		 |		|
		 |		|-- susubfileSync
		 |		|
		 |		|-- susubfileUnsync

		 */

		/* =================== */
		/* (1) force = TRUE    */
		/* =================== */

		/* create some resources */
		IFolder folder = projects[0].getFolder("folder");
		ensureExistsInWorkspace(folder, true);
		IFile fileSync = folder.getFile("fileSync");
		ensureExistsInWorkspace(fileSync, true);
		IFile fileUnsync = folder.getFile("fileUnsync");
		ensureExistsInWorkspace(fileUnsync, true);
		IFile fileCreated = folder.getFile("fileCreated");
		ensureExistsInFileSystem(fileCreated); // create only in file system
		IFolder subfolderSync = folder.getFolder("subfolderSync");
		ensureExistsInWorkspace(subfolderSync, true);
		IFolder deletedfolderSync = subfolderSync.getFolder("deletedfolderSync");
		ensureExistsInWorkspace(deletedfolderSync, true);
		IFolder subfolderUnsync = folder.getFolder("subfolderUnsync");
		ensureExistsInWorkspace(subfolderUnsync, true);
		IFolder subsubfolderUnsync = subfolderUnsync.getFolder("subsubfolderUnsync");
		ensureExistsInWorkspace(subsubfolderUnsync, true);
		IFile subsubfileSync = subsubfolderUnsync.getFile("subsubfileSync");
		ensureExistsInWorkspace(subsubfileSync, true);
		IFile subsubfileUnsync = subsubfolderUnsync.getFile("subsubfileUnsync");
		ensureExistsInWorkspace(subsubfileUnsync, true);

		/* make some resources "unsync" with the workspace */
		ensureOutOfSync(fileUnsync);
		ensureDoesNotExistInFileSystem(deletedfolderSync);
		ensureOutOfSync(subsubfileUnsync);

		/* delete */
		folder.delete(true, null);

		/* assert resources do not exist anymore */
		assertTrue("1.1", !folder.getLocation().toFile().exists());

		/* =================== */
		/* (2) force = FALSE   */
		/* =================== */

		/* create some resources */
		folder = projects[0].getFolder("folder");
		ensureExistsInWorkspace(folder, true);
		//
		fileSync = folder.getFile("fileSync");
		ensureExistsInWorkspace(fileSync, true);
		//
		fileUnsync = folder.getFile("fileUnsync");
		ensureExistsInWorkspace(fileUnsync, true);
		//
		fileCreated = folder.getFile("fileCreated");
		ensureExistsInFileSystem(fileCreated); // create only in file system
		//
		subfolderSync = folder.getFolder("subfolderSync");
		ensureExistsInWorkspace(subfolderSync, true);
		//
		deletedfolderSync = subfolderSync.getFolder("deletedfolderSync");
		ensureExistsInWorkspace(deletedfolderSync, true);
		//
		subfolderUnsync = folder.getFolder("subfolderUnsync");
		ensureExistsInWorkspace(subfolderUnsync, true);
		//
		subsubfolderUnsync = subfolderUnsync.getFolder("subsubfolderUnsync");
		ensureExistsInWorkspace(subsubfolderUnsync, true);
		//
		subsubfileSync = subsubfolderUnsync.getFile("subsubfileSync");
		ensureExistsInWorkspace(subsubfileSync, true);
		//
		subsubfileUnsync = subsubfolderUnsync.getFile("subsubfileUnsync");
		ensureExistsInWorkspace(subsubfileUnsync, true);

		/* make some resources "unsync" with the workspace */
		ensureOutOfSync(fileUnsync);
		ensureDoesNotExistInFileSystem(deletedfolderSync);
		ensureOutOfSync(subsubfileUnsync);

		/* delete */
		try {
			folder.delete(false, null);
			fail("2.0");
		} catch (CoreException e) {
			// expected
		}

		/* assert resources do not exist anymore in the file system */
		assertTrue("2.1", folder.getLocation().toFile().exists());
		assertTrue("2.2", !fileSync.getLocation().toFile().exists());
		assertTrue("2.3", fileUnsync.getLocation().toFile().exists());
		assertTrue("2.4", !subfolderSync.getLocation().toFile().exists());
		assertTrue("2.5", subfolderUnsync.getLocation().toFile().exists());
		assertTrue("2.6", !deletedfolderSync.getLocation().toFile().exists());
		assertTrue("2.7", subsubfolderUnsync.getLocation().toFile().exists());
		assertTrue("2.8", subsubfileUnsync.getLocation().toFile().exists());
		assertTrue("2.9", !subsubfileSync.getLocation().toFile().exists());
		assertTrue("2.10", fileCreated.getLocation().toFile().exists());
	}
}
