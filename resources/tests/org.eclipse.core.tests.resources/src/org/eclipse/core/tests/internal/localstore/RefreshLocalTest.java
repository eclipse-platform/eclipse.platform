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

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;

import java.io.File;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.resources.ICoreConstants;
import org.eclipse.core.internal.resources.Resource;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;

//
public class RefreshLocalTest extends LocalStoreTest implements ICoreConstants {

	/**
	 * Tests refreshing a folder whose case has changed on disk.
	 * This is a regression test for bug 79090.
	 */
	public void testDiscoverCaseChange() throws CoreException {
		IProject project = projects[0];
		IFolder folder = project.getFolder("A");
		IFolder folderVariant = project.getFolder("a");
		IFile file = folder.getFile("file");
		IFile fileVariant = folderVariant.getFile(file.getName());
		//create the project, folder, and file
		ensureExistsInWorkspace(file, true);

		//change the case of the folder on disk
		project.getLocation().append("A").toFile().renameTo((project.getLocation().append("a").toFile()));

		// refresh the project
		project.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());

		//variant should exist but original shouldn't
		assertTrue(folderVariant.exists());
		assertTrue(fileVariant.exists());
		assertFalse(folder.exists());
		assertFalse(file.exists());
		assertTrue(project.isSynchronized(IResource.DEPTH_INFINITE));
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

		ensureExistsInFileSystem(folder);
		ensureDoesNotExistInWorkspace(folder);
		ensureExistsInFileSystem(file);
		ensureDoesNotExistInWorkspace(file);

		assertFalse(file.exists());
		assertFalse(folder.exists());
		assertFalse(file.isSynchronized(IResource.DEPTH_ZERO));
		assertFalse(folder.isSynchronized(IResource.DEPTH_INFINITE));
		file.refreshLocal(IResource.DEPTH_ZERO, createTestMonitor());
		assertTrue(file.exists());
		assertTrue(folder.exists());

		//try again with deleted project
		project.delete(IResource.FORCE, createTestMonitor());

		ensureExistsInFileSystem(folder);
		ensureDoesNotExistInWorkspace(folder);
		ensureExistsInFileSystem(file);
		ensureDoesNotExistInWorkspace(file);

		assertFalse(file.exists());
		assertFalse(folder.exists());
		file.refreshLocal(IResource.DEPTH_ZERO, createTestMonitor());
		assertFalse(file.exists());
		assertFalse(folder.exists());
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
		assertTrue(file.exists());
		assertTrue(target.isDirectory());
		file.refreshLocal(IResource.DEPTH_ZERO, null);
		assertFalse(file.exists());
		IFolder folder = project.getFolder("file");
		assertTrue(folder.exists());
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
		assertTrue(folder.exists());
		folder.refreshLocal(IResource.DEPTH_ZERO, null);
		assertFalse(folder.exists());
		assertTrue(file.exists());
	}

	public void testRefreshClosedProject() throws CoreException {
		IProject project = projects[0];
		project.close(createTestMonitor());

		//refreshing a closed project should not fail
		project.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		project.refreshLocal(IResource.DEPTH_ZERO, createTestMonitor());
		project.refreshLocal(IResource.DEPTH_ONE, createTestMonitor());
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
		assertTrue(file.exists());
		assertTrue(file.isLocal(IResource.DEPTH_ZERO));
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		assertFalse(file.exists());
		ensureDoesNotExistInWorkspace(file);
		ensureDoesNotExistInFileSystem(file);

		/* test creation of a child */
		file = project.getFile("file");
		ensureExistsInFileSystem(file);
		assertFalse(file.exists());
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		assertTrue(file.exists());
		ensureDoesNotExistInWorkspace(file);
		ensureDoesNotExistInFileSystem(file);

		/* test changes of a child (child is folder) */
		IFolder folder = project.getFolder("folder");
		folder.create(true, true, null);
		file = folder.getFile("file");
		ensureExistsInFileSystem(file);
		assertTrue(folder.exists());
		assertTrue(folder.isLocal(IResource.DEPTH_ZERO));
		assertFalse(file.exists());
		folder.refreshLocal(IResource.DEPTH_ZERO, null);
		assertTrue(folder.exists());
		assertTrue(folder.isLocal(IResource.DEPTH_ZERO));
		assertFalse(file.exists());
		folder.refreshLocal(IResource.DEPTH_ONE, null);
		assertTrue(folder.exists());
		assertTrue(folder.isLocal(IResource.DEPTH_ZERO));
		assertTrue(file.exists());
		ensureDoesNotExistInWorkspace(folder);
		ensureDoesNotExistInFileSystem(folder);

		/* test changes of a child (child is file) */
		file = project.getFile("file");
		IFileStore fileStore = ((Resource) file).getStore();
		ensureExistsInWorkspace(file, true);
		assertTrue(file.exists());
		assertTrue(file.isLocal(IResource.DEPTH_ZERO));
		assertEquals(fileStore.fetchInfo().getLastModified(),
				((Resource) file).getResourceInfo(false, false).getLocalSyncInfo());
		ensureOutOfSync(file);
		assertNotSame(((Resource) file).getResourceInfo(false, false).getLocalSyncInfo(),
				fileStore.fetchInfo()
				.getLastModified());
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		assertEquals(fileStore.fetchInfo().getLastModified(),
				((Resource) file).getResourceInfo(false, false).getLocalSyncInfo());
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
		assertTrue(file.exists());
		file.refreshLocal(IResource.DEPTH_INFINITE, null);
		assertFalse(file.exists());

		/* test root and children creation */
		IFolder folder = project.getFolder("folder");
		IFileStore target = ((Resource) folder).getStore();
		createTree(getTree(target));
		assertFalse(folder.exists());
		folder.refreshLocal(IResource.DEPTH_INFINITE, null);
		assertTrue(folder.exists());
		assertEquals(((Resource) folder).countResources(IResource.DEPTH_INFINITE, false), getTree(target).length + 1);
	}
}
