/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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

import java.io.File;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.resources.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;

//
public class RefreshLocalTest extends LocalStoreTest implements ICoreConstants {

	/**
	 * Tests refreshing a folder whose case has changed on disk.
	 * This is a regression test for bug 79090.
	 */
	public void testDiscoverCaseChange() {
		IProject project = projects[0];
		IFolder folder = project.getFolder("A");
		IFolder folderVariant = project.getFolder("a");
		IFile file = folder.getFile("file");
		IFile fileVariant = folderVariant.getFile(file.getName());
		//create the project, folder, and file
		ensureExistsInWorkspace(file, true);

		//change the case of the folder on disk
		project.getLocation().append("A").toFile().renameTo((project.getLocation().append("a").toFile()));

		try {
			//refresh the project
			project.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		} catch (CoreException e) {
			fail("1.99", e);
		}

		//variant should exist but original shouldn't
		assertTrue("1.1", folderVariant.exists());
		assertTrue("1.2", fileVariant.exists());
		assertTrue("1.3", !folder.exists());
		assertTrue("1.4", !file.exists());
		assertTrue("1.5", project.isSynchronized(IResource.DEPTH_INFINITE));
	}

	/**
	 * Test discovery of a linked resource on refresh.
	 */
	public void testDiscoverLinkedResource() {
		//create a linked resource with local contents missing
		//	IProject project = projects[0];
		//	ensureExistsInWorkspace(project, true);
		//	IPath location = getRandomLocation();
		//	IFile link = project.getFile("Link");
		//	try {
		//		link.createLink(location, IResource.ALLOW_MISSING_LOCAL, getMonitor());
		//	} catch (CoreException e) {
		//		fail("0.99", e);
		//	}
		//
		//	//should not be synchronized (exists in ws, but not in fs)
		//	assertTrue("1.0", !project.isSynchronized(IResource.DEPTH_INFINITE));
		//	assertTrue("1.1", !link.isSynchronized(IResource.DEPTH_ZERO));
		//
		//	//should exist in workspace
		//	assertTrue("1.3", link.exists());
		//
		//	//refreshing shouldn't get rid of the link
		//	try {
		//		link.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		//	} catch (CoreException e) {
		//		fail("1.99", e);
		//	}
		//	assertTrue("1.4", link.exists());
		//
		//	//create the contents in the file system
		//	try {
		//		createFileInFileSystem(location);
		//	} catch (CoreException e) {
		//		fail("2.99", e);
		//	}
		//
		//	//should now be synchronized
		//	assertTrue("2.1", project.isSynchronized(IResource.DEPTH_INFINITE));
		//	assertTrue("2.2", link.isSynchronized(IResource.DEPTH_ZERO));
		//
		//	//refresh
		//	try {
		//		link.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		//	} catch (CoreException e) {
		//		fail("3.99", e);
		//	}
		//
		//	//assert should exist in workspace
		//	assertTrue("2.3", link.exists());
	}

	/**
	 * Tests discovering a file via refresh local when neither the file
	 * nor its parent exists in the workspace.
	 */
	public void testFileDiscovery() throws Throwable {
		/* initialize common objects */
		IProject project = projects[0];

		IFolder folder = project.getFolder("Folder");
		IFile file = folder.getFile("File");
		IResource[] both = new IResource[] {folder, file};

		ensureExistsInFileSystem(both);
		ensureDoesNotExistInWorkspace(both);

		assertTrue("1.0", !file.exists());
		assertTrue("1.1", !folder.exists());
		assertTrue("1.2", !file.isSynchronized(IResource.DEPTH_ZERO));
		assertTrue("1.3", !folder.isSynchronized(IResource.DEPTH_INFINITE));
		file.refreshLocal(IResource.DEPTH_ZERO, getMonitor());
		assertTrue("1.4", file.exists());
		assertTrue("1.5", folder.exists());

		//try again with deleted project
		project.delete(IResource.FORCE, getMonitor());

		ensureExistsInFileSystem(both);
		ensureDoesNotExistInWorkspace(both);

		assertTrue("2.0", !file.exists());
		assertTrue("2.1", !folder.exists());
		file.refreshLocal(IResource.DEPTH_ZERO, getMonitor());
		assertTrue("2.2", !file.exists());
		assertTrue("2.3", !folder.exists());
	}

	public void testFileToFolder() throws Throwable {
		/* initialize common objects */
		IProject project = projects[0];

		/* */
		IFile file = project.getFile("file");
		file.create(null, true, null);
		ensureDoesNotExistInFileSystem(file);
		//
		File target = file.getLocation().toFile();
		target.mkdirs();
		//
		assertTrue("1.1", file.exists());
		assertTrue("1.2", target.isDirectory());
		file.refreshLocal(IResource.DEPTH_ZERO, null);
		assertTrue("1.3", !file.exists());
		IFolder folder = project.getFolder("file");
		assertTrue("1.4", folder.exists());
	}

	public void testFolderToFile() throws Throwable {
		/* initialize common objects */
		IProject project = projects[0];

		/* test folder to file */
		IFolder folder = project.getFolder("folder");
		folder.create(true, true, null);
		ensureDoesNotExistInFileSystem(folder);
		//
		IFile file = project.getFile("folder");
		ensureExistsInFileSystem(file);
		//
		assertTrue("1.1", folder.exists());
		folder.refreshLocal(IResource.DEPTH_ZERO, null);
		assertTrue("1.2", !folder.exists());
		assertTrue("1.3", file.exists());
	}

	public void testRefreshClosedProject() {
		IProject project = projects[0];
		try {
			project.close(getMonitor());
		} catch (CoreException e) {
			fail("0.99", e);
		}
		//refreshing a closed project should not fail
		try {
			project.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
			project.refreshLocal(IResource.DEPTH_ZERO, getMonitor());
			project.refreshLocal(IResource.DEPTH_ONE, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}
	}

	public void testRefreshFolder() throws Throwable {
		/* initialize common objects */
		IProject project = projects[0];

		/* test deletion of a child */
		IFile file = project.getFile("file");
		final IFile hackFile = file;
		final Workspace workspace = (Workspace) getWorkspace();
		IWorkspaceRunnable operation = monitor -> {
			workspace.createResource(hackFile, false);
			((Resource) hackFile).getResourceInfo(false, true).set(M_LOCAL_EXISTS);
		};
		workspace.run(operation, null);
		assertTrue("1.0", file.exists());
		assertTrue("1.1", file.isLocal(IResource.DEPTH_ZERO));
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		assertTrue("1.2", !file.exists());
		ensureDoesNotExistInWorkspace(file);
		ensureDoesNotExistInFileSystem(file);

		/* test creation of a child */
		file = project.getFile("file");
		ensureExistsInFileSystem(file);
		assertTrue("2.0", !file.exists());
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		assertTrue("2.1", file.exists());
		ensureDoesNotExistInWorkspace(file);
		ensureDoesNotExistInFileSystem(file);

		/* test changes of a child (child is folder) */
		IFolder folder = project.getFolder("folder");
		folder.create(true, true, null);
		file = folder.getFile("file");
		ensureExistsInFileSystem(file);
		assertTrue("3.1", folder.exists());
		assertTrue("3.2", folder.isLocal(IResource.DEPTH_ZERO));
		assertTrue("3.3", !file.exists());
		folder.refreshLocal(IResource.DEPTH_ZERO, null);
		assertTrue("3.4", folder.exists());
		assertTrue("3.5", folder.isLocal(IResource.DEPTH_ZERO));
		assertTrue("3.6", !file.exists());
		folder.refreshLocal(IResource.DEPTH_ONE, null);
		assertTrue("3.7", folder.exists());
		assertTrue("3.8", folder.isLocal(IResource.DEPTH_ZERO));
		assertTrue("3.9", file.exists());
		ensureDoesNotExistInWorkspace(folder);
		ensureDoesNotExistInFileSystem(folder);

		/* test changes of a child (child is file) */
		file = project.getFile("file");
		IFileStore fileStore = ((Resource) file).getStore();
		ensureExistsInWorkspace(file, true);
		assertTrue("4.1", file.exists());
		assertTrue("4.2", file.isLocal(IResource.DEPTH_ZERO));
		assertEquals("4.3", fileStore.fetchInfo().getLastModified(), ((Resource) file).getResourceInfo(false, false).getLocalSyncInfo());
		ensureOutOfSync(file);
		assertTrue("4.4", ((Resource) file).getResourceInfo(false, false).getLocalSyncInfo() != fileStore.fetchInfo().getLastModified());
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		assertEquals("4.5", fileStore.fetchInfo().getLastModified(), ((Resource) file).getResourceInfo(false, false).getLocalSyncInfo());
		ensureDoesNotExistInWorkspace(file);
		ensureDoesNotExistInFileSystem(file);
	}

	public void testSimpleRefresh() throws Throwable {
		/* initialize common objects */
		IProject project = projects[0];

		/* test root deletion */
		IFile file = project.getFile("file");
		ensureExistsInWorkspace(file, true);
		ensureDoesNotExistInFileSystem(file);
		assertTrue("1.0", file.exists());
		file.refreshLocal(IResource.DEPTH_INFINITE, null);
		assertTrue("1.1", !file.exists());

		/* test root and children creation */
		IFolder folder = project.getFolder("folder");
		IFileStore target = ((Resource) folder).getStore();
		createTree(getTree(target));
		assertTrue("2.0", !folder.exists());
		folder.refreshLocal(IResource.DEPTH_INFINITE, null);
		assertTrue("2.1", folder.exists());
		assertTrue("2.2", ((Resource) folder).countResources(IResource.DEPTH_INFINITE, false) == (getTree(target).length + 1));
	}
}
