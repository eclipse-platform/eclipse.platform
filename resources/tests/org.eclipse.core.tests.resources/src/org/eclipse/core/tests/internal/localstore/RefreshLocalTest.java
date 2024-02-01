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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.internal.localstore.LocalStoreTestUtil.createTree;
import static org.eclipse.core.tests.internal.localstore.LocalStoreTestUtil.getTree;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.ensureOutOfSync;
import static org.eclipse.core.tests.resources.ResourceTestUtil.removeFromFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.removeFromWorkspace;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

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
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class RefreshLocalTest implements ICoreConstants {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	private IProject project;

	@Before
	public void createTestProject() throws CoreException {
		project = getWorkspace().getRoot().getProject("Project");
		createInWorkspace(project);
	}

	/**
	 * Tests refreshing a folder whose case has changed on disk.
	 * This is a regression test for bug 79090.
	 */
	@Test
	public void testDiscoverCaseChange() throws CoreException {
		IFolder folder = project.getFolder("A");
		IFolder folderVariant = project.getFolder("a");
		IFile file = folder.getFile("file");
		IFile fileVariant = folderVariant.getFile(file.getName());
		//create the project, folder, and file
		createInWorkspace(file);

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
	@Test
	public void testDiscoverLinkedResource() {
		//create a linked resource with local contents missing
		//	IProject project = projects[0];
		//	ensureExistsInWorkspace(project);
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
	@Test
	public void testFileDiscovery() throws Throwable {
		workspaceRule.deleteOnTearDown(project.getLocation());
		IFolder folder = project.getFolder("Folder");
		IFile file = folder.getFile("File");

		createInFileSystem(folder);
		removeFromWorkspace(folder);
		createInFileSystem(file);
		removeFromWorkspace(file);

		assertFalse(file.exists());
		assertFalse(folder.exists());
		assertFalse(file.isSynchronized(IResource.DEPTH_ZERO));
		assertFalse(folder.isSynchronized(IResource.DEPTH_INFINITE));
		file.refreshLocal(IResource.DEPTH_ZERO, createTestMonitor());
		assertTrue(file.exists());
		assertTrue(folder.exists());

		//try again with deleted project
		project.delete(IResource.FORCE, createTestMonitor());

		createInFileSystem(folder);
		removeFromWorkspace(folder);
		createInFileSystem(file);
		removeFromWorkspace(file);

		assertFalse(file.exists());
		assertFalse(folder.exists());
		file.refreshLocal(IResource.DEPTH_ZERO, createTestMonitor());
		assertFalse(file.exists());
		assertFalse(folder.exists());
	}

	@Test
	public void testFileToFolder() throws Throwable {
		/* */
		IFile file = project.getFile("file");
		file.create(null, true, null);
		removeFromFileSystem(file);
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

	@Test
	public void testFolderToFile() throws Throwable {
		/* test folder to file */
		IFolder folder = project.getFolder("folder");
		folder.create(true, true, null);
		removeFromFileSystem(folder);
		//
		IFile file = project.getFile("folder");
		createInFileSystem(file);
		//
		assertTrue(folder.exists());
		folder.refreshLocal(IResource.DEPTH_ZERO, null);
		assertFalse(folder.exists());
		assertTrue(file.exists());
	}

	@Test
	public void testRefreshClosedProject() throws CoreException {
		project.close(createTestMonitor());

		//refreshing a closed project should not fail
		project.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		project.refreshLocal(IResource.DEPTH_ZERO, createTestMonitor());
		project.refreshLocal(IResource.DEPTH_ONE, createTestMonitor());
	}

	@Test
	public void testRefreshFolder() throws Throwable {
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
		removeFromWorkspace(file);
		removeFromFileSystem(file);

		/* test creation of a child */
		file = project.getFile("file");
		createInFileSystem(file);
		assertFalse(file.exists());
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		assertTrue(file.exists());
		removeFromWorkspace(file);
		removeFromFileSystem(file);

		/* test changes of a child (child is folder) */
		IFolder folder = project.getFolder("folder");
		folder.create(true, true, null);
		file = folder.getFile("file");
		createInFileSystem(file);
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
		removeFromWorkspace(folder);
		removeFromFileSystem(folder);

		/* test changes of a child (child is file) */
		file = project.getFile("file");
		IFileStore fileStore = ((Resource) file).getStore();
		createInWorkspace(file);
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
		removeFromWorkspace(file);
		removeFromFileSystem(file);
	}

	@Test
	public void testSimpleRefresh() throws Throwable {
		/* test root deletion */
		IFile file = project.getFile("file");
		createInWorkspace(file);
		removeFromFileSystem(file);
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
		assertThat(getTree(target)).hasSize(((Resource) folder).countResources(IResource.DEPTH_INFINITE, false) - 1);
	}

}
