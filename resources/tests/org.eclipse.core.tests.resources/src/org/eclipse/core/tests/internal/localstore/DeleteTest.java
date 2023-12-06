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

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.junit.Assert.assertThrows;

import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

public class DeleteTest extends LocalStoreTest {

	public void testDeleteOpenProject() throws Exception {
		IProject project = projects[0];
		IFolder folder = project.getFolder("folder");
		IFile file = folder.getFile("file");

		/* ===========================================================
		 * project is OPEN, deleteContents=FALSE, force=TRUE
		 * =========================================================== */

		/* create some resources */
		createInWorkspace(new IResource[] {project, folder, file});
		IPath folderPath = folder.getLocation();
		IPath filePath = file.getLocation();
		IPath projectLocation = project.getLocation();

		/* delete */
		project.delete(false, true, createTestMonitor());

		/* assert project does not exist anymore in the workspace*/
		assertFalse(project.exists());
		assertFalse(((Workspace) getWorkspace()).getMetaArea().locationFor(project).toFile().exists());
		assertNull(project.getLocation());

		/* assert resources still exist */
		assertTrue(folderPath.toFile().isDirectory());
		assertTrue(filePath.toFile().isFile());

		/* remove trash */
		Workspace.clear(projectLocation.toFile());

		/* ===========================================================
		 * project is OPEN, deleteContents=TRUE, force=TRUE
		 * 	- uses default default mapping
		 * ========================================================== */

		/* initialize common objects */
		createInWorkspace(new IResource[] {project, folder, file});
		folderPath = folder.getLocation();
		filePath = file.getLocation();
		projectLocation = project.getLocation();

		/* delete */
		project.delete(true, true, createTestMonitor());

		/* assert project does not exist anymore in the workspace */
		assertFalse(project.exists());
		assertFalse(((Workspace) getWorkspace()).getMetaArea().locationFor(project).toFile().exists());
		assertNull(project.getLocation());

		/* assert resources do not exist anymore */
		assertFalse(projectLocation.toFile().exists());
		assertFalse(folderPath.toFile().exists());
		assertFalse(filePath.toFile().exists());

		/* ===========================================================
		 * project is OPEN, deleteContents=TRUE, force=TRUE
		 * 	- defines default mapping
		 * 	- does not create resources on disk
		 * =========================================================== */

		/* initialize common objects */
		createInWorkspace(project);
		folder.create(true, false, createTestMonitor());
		file.create(null, true, createTestMonitor());
		folderPath = folder.getLocation();
		filePath = file.getLocation();
		projectLocation = project.getLocation();

		/* delete */
		project.delete(true, true, createTestMonitor());

		/* assert project does not exist anymore */
		assertFalse(project.exists());
		assertFalse(((Workspace) getWorkspace()).getMetaArea().locationFor(project).toFile().exists());
		assertNull(project.getLocation());

		/* assert resources do not exist anymore */
		assertFalse(folderPath.toFile().isDirectory());
		assertFalse(filePath.toFile().isFile());

		/* ===========================================================
		 * project is OPEN, deleteContents=TRUE, force=true
		 * 	- create resources at default default area
		 * 	- defines default mapping
		 * =========================================================== */

		/* initialize common objects */
		createInWorkspace(new IResource[] {project, folder, file});
		folderPath = folder.getLocation();
		filePath = file.getLocation();

		/* delete */
		project.delete(true, true, createTestMonitor());

		/* assert project does not exist anymore */
		assertFalse(project.exists());
		assertFalse(((Workspace) getWorkspace()).getMetaArea().locationFor(project).toFile().exists());
		assertNull(project.getLocation());

		/* assert resources do not still exist at default default area */
		assertFalse(folderPath.toFile().exists());
		assertFalse(filePath.toFile().exists());

		/* remove trash */
		Workspace.clear(folderPath.toFile());

		/* ===========================================================
		 * project is OPEN, deleteContents=TRUE, force=TRUE
		 * 	- defines default mapping
		 * 	- creates resources only on disk
		 * =========================================================== */

		/* initialize common objects */
		createInWorkspace(project);
		createInFileSystem(folder);
		createInFileSystem(file);
		folderPath = folder.getLocation();
		filePath = file.getLocation();
		projectLocation = project.getLocation();

		/* delete */
		project.delete(true, true, createTestMonitor());

		/* assert project does not exist anymore */
		assertFalse(project.exists());
		assertFalse(((Workspace) getWorkspace()).getMetaArea().locationFor(project).toFile().exists());
		assertNull(project.getLocation());

		/* assert resources do not exist anymore */
		assertFalse(folderPath.toFile().isDirectory());
		assertFalse(filePath.toFile().isFile());

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
		createInWorkspace(new IResource[] {project, folder});
		getWorkspace().run(monitor -> file.create(null, true, monitor), createTestMonitor());
		createInWorkspace(file);
		IPath folderPath = folder.getLocation();
		IPath filePath = file.getLocation();
		IPath projectLocation = project.getLocation();

		/* close and delete */
		project.close(createTestMonitor());
		project.delete(false, true, createTestMonitor());

		/* assert project does not exist anymore in the workspace */
		assertFalse(project.exists());
		assertFalse(((Workspace) getWorkspace()).getMetaArea().locationFor(project).toFile().exists());
		assertNull(project.getLocation());

		/* assert resources still exist (if appropriate) */
		assertTrue(folderPath.toFile().exists());
		assertFalse(filePath.toFile().exists());

		/* remove trash */
		Workspace.clear(projectLocation.toFile());

		/* ===========================================================
		 * project is CLOSED, deleteContents=TRUE, force=TRUE
		 * 	- uses default default mapping
		 * =========================================================== */

		/* initialize common objects */
		createInWorkspace(new IResource[] {project, folder, file});
		folderPath = folder.getLocation();
		filePath = file.getLocation();

		/* close and delete */
		project.close(createTestMonitor());
		project.delete(true, true, createTestMonitor());

		/* assert project does not exist anymore */
		assertFalse(project.exists());
		assertFalse(((Workspace) getWorkspace()).getMetaArea().locationFor(project).toFile().exists());
		assertNull(project.getLocation());

		/* assert resources do not exist anymore */
		assertFalse(folderPath.toFile().isDirectory());
		assertFalse(filePath.toFile().isFile());

		/* ===========================================================
		 * project is CLOSED, deleteContents=TRUE, force = FALSE
		 * 	- uses default default mapping
		 * =========================================================== */

		/* initialize common objects */
		createInWorkspace(new IResource[] {project, folder});
		createInWorkspace(file);
		folderPath = folder.getLocation();
		filePath = file.getLocation();
		projectLocation = project.getLocation();

		/* close and delete */
		projects[0].close(createTestMonitor());
		projects[0].delete(true, false, createTestMonitor());

		/* assert project was deleted */
		assertFalse(project.exists());
		IPath metaAreaLocation = ((Workspace) getWorkspace()).getMetaArea().locationFor(project);
		assertFalse(metaAreaLocation.toFile().exists());
		assertFalse(metaAreaLocation.append(".properties").toFile().exists());
		assertFalse(projectLocation.append(IProjectDescription.DESCRIPTION_FILE_NAME).toFile().exists());
		assertNull(project.getLocation());

		/* assert resources do not exist anymore */
		assertFalse(folderPath.toFile().exists());
		assertFalse(filePath.toFile().exists());
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
		createInWorkspace(folder);
		IFile fileSync = folder.getFile("fileSync");
		createInWorkspace(fileSync);
		IFile fileUnsync = folder.getFile("fileUnsync");
		createInWorkspace(fileUnsync);
		IFile fileCreated = folder.getFile("fileCreated");
		createInFileSystem(fileCreated); // create only in file system
		IFolder subfolderSync = folder.getFolder("subfolderSync");
		createInWorkspace(subfolderSync);
		IFolder deletedfolderSync = subfolderSync.getFolder("deletedfolderSync");
		createInWorkspace(deletedfolderSync);
		IFolder subfolderUnsync = folder.getFolder("subfolderUnsync");
		createInWorkspace(subfolderUnsync);
		IFolder subsubfolderUnsync = subfolderUnsync.getFolder("subsubfolderUnsync");
		createInWorkspace(subsubfolderUnsync);
		IFile subsubfileSync = subsubfolderUnsync.getFile("subsubfileSync");
		createInWorkspace(subsubfileSync);
		IFile subsubfileUnsync = subsubfolderUnsync.getFile("subsubfileUnsync");
		createInWorkspace(subsubfileUnsync);

		/* make some resources "unsync" with the workspace */
		ensureOutOfSync(fileUnsync);
		removeFromFileSystem(deletedfolderSync);
		ensureOutOfSync(subsubfileUnsync);

		/* delete */
		folder.delete(true, null);

		/* assert resources do not exist anymore */
		assertFalse(folder.getLocation().toFile().exists());

		/* =================== */
		/* (2) force = FALSE   */
		/* =================== */

		/* create some resources */
		IFolder recreatedFolder = projects[0].getFolder("folder");
		createInWorkspace(recreatedFolder);
		//
		fileSync = recreatedFolder.getFile("fileSync");
		createInWorkspace(fileSync);
		//
		fileUnsync = recreatedFolder.getFile("fileUnsync");
		createInWorkspace(fileUnsync);
		//
		fileCreated = recreatedFolder.getFile("fileCreated");
		createInFileSystem(fileCreated); // create only in file system
		//
		subfolderSync = recreatedFolder.getFolder("subfolderSync");
		createInWorkspace(subfolderSync);
		//
		deletedfolderSync = subfolderSync.getFolder("deletedfolderSync");
		createInWorkspace(deletedfolderSync);
		//
		subfolderUnsync = recreatedFolder.getFolder("subfolderUnsync");
		createInWorkspace(subfolderUnsync);
		//
		subsubfolderUnsync = subfolderUnsync.getFolder("subsubfolderUnsync");
		createInWorkspace(subsubfolderUnsync);
		//
		subsubfileSync = subsubfolderUnsync.getFile("subsubfileSync");
		createInWorkspace(subsubfileSync);
		//
		subsubfileUnsync = subsubfolderUnsync.getFile("subsubfileUnsync");
		createInWorkspace(subsubfileUnsync);

		/* make some resources "unsync" with the workspace */
		ensureOutOfSync(fileUnsync);
		removeFromFileSystem(deletedfolderSync);
		ensureOutOfSync(subsubfileUnsync);

		/* delete */
		assertThrows(CoreException.class, () -> recreatedFolder.delete(false, null));

		/* assert resources do not exist anymore in the file system */
		assertTrue(recreatedFolder.getLocation().toFile().exists());
		assertFalse(fileSync.getLocation().toFile().exists());
		assertTrue(fileUnsync.getLocation().toFile().exists());
		assertFalse(subfolderSync.getLocation().toFile().exists());
		assertTrue(subfolderUnsync.getLocation().toFile().exists());
		assertFalse(deletedfolderSync.getLocation().toFile().exists());
		assertTrue(subsubfolderUnsync.getLocation().toFile().exists());
		assertTrue(subsubfileUnsync.getLocation().toFile().exists());
		assertFalse(subsubfileSync.getLocation().toFile().exists());
		assertTrue(fileCreated.getLocation().toFile().exists());
	}
}
