/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
import static org.eclipse.core.tests.resources.ResourceTestUtil.buildResources;
import static org.eclipse.core.tests.resources.ResourceTestUtil.compareContent;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInputStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createUniqueString;
import static org.eclipse.core.tests.resources.ResourceTestUtil.ensureOutOfSync;
import static org.eclipse.core.tests.resources.ResourceTestUtil.removeFromFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.removeFromWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.waitForRefresh;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.net.URI;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.localstore.FileSystemResourceManager;
import org.eclipse.core.internal.resources.File;
import org.eclipse.core.internal.resources.ICoreConstants;
import org.eclipse.core.internal.resources.Project;
import org.eclipse.core.internal.resources.Resource;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.tests.internal.filesystem.bug440110.Bug440110FileSystem;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class FileSystemResourceManagerTest implements ICoreConstants {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	private IProject project;

	@Before
	public void createTestProject() throws CoreException {
		project = getWorkspace().getRoot().getProject("Project");
		createInWorkspace(project);
	}

	@Test
	public void testBug440110() throws Exception {
		String projectName = createUniqueString();
		IWorkspace workspace = getWorkspace();
		project = workspace.getRoot().getProject(projectName);
		IProjectDescription projectDescription = workspace.newProjectDescription(projectName);
		projectDescription.setLocationURI(new URI(Bug440110FileSystem.SCHEME + "://" + projectName));
		project.create(projectDescription, null);
		project.open(null);
		assertEquals(Bug440110FileSystem.SCHEME, project.getLocationURI().getScheme());

		IFolder folder = project.getFolder("folder");
		folder.create(true, true, null);
		assertEquals(Bug440110FileSystem.SCHEME, folder.getLocationURI().getScheme());

		Bug440110FileSystem.clearFetchedFileTree();
		folder.refreshLocal(IResource.DEPTH_ZERO, null);
		assertFalse(Bug440110FileSystem.hasFetchedFileTree());

		Bug440110FileSystem.clearFetchedFileTree();
		folder.refreshLocal(IResource.DEPTH_ONE, null);
		assertTrue(Bug440110FileSystem.hasFetchedFileTree());

		Bug440110FileSystem.clearFetchedFileTree();
		folder.refreshLocal(IResource.DEPTH_INFINITE, null);
		assertTrue(Bug440110FileSystem.hasFetchedFileTree());
	}

	@Test
	public void testContainerFor() {
		/* test null parameter */
		assertThrows(RuntimeException.class, () -> getLocalManager().containerForLocation(null));

		/* test normal conditions under default mapping */

		// project/target
		IPath path = IPath.fromOSString("target");
		IFolder folder = project.getFolder(path);
		IPath location = project.getLocation().append(path);
		IFolder testFolder = (IFolder) getLocalManager().containerForLocation(location);
		assertEquals(folder, testFolder);

		// project/folder/target
		path = IPath.fromOSString("folder/target");
		folder = project.getFolder(path);
		location = project.getLocation().append(path);
		testFolder = (IFolder) getLocalManager().containerForLocation(location);
		assertEquals(folder, testFolder);

		// project/folder/folder/target
		path = IPath.fromOSString("folder/folder/target");
		folder = project.getFolder(path);
		location = project.getLocation().append(path);
		testFolder = (IFolder) getLocalManager().containerForLocation(location);
		assertEquals(folder, testFolder);

		/* non-existent location */
		testFolder = (IFolder) getLocalManager().containerForLocation(IPath.fromOSString("../this/path/must/not/exist"));
		assertNull(testFolder);
	}

	/**
	 * this test should move to FileTest
	 */
	@Test
	public void testCreateFile() throws Exception {
		File file = (File) project.getFile("testCreateFile");
		/* common contents */
		String originalContent = "this string should not be equal the other";

		/* create file with flag false */
		file.create(createInputStream(originalContent), false, null);
		assertTrue(file.exists());
		assertTrue(file.isLocal(IResource.DEPTH_ZERO));
		assertEquals(file.getStore().fetchInfo().getLastModified(), file.getResourceInfo(false, false).getLocalSyncInfo());
		assertTrue(compareContent(createInputStream(originalContent), getLocalManager().read(file, true, null)));
	}

	@Test
	public void testFileFor() {
		/* test null parameter */
		assertThrows(RuntimeException.class, () -> getLocalManager().fileForLocation(null));

		/* test normal conditions under default mapping */

		// project/file
		IPath path = IPath.fromOSString("file");
		IFile file = project.getFile(path);
		IPath location = project.getLocation().append(path);
		IFile testFile = getLocalManager().fileForLocation(location);
		assertEquals(file, testFile);

		// project/folder/file
		path = IPath.fromOSString("folder/file");
		file = project.getFile(path);
		location = project.getLocation().append(path);
		testFile = getLocalManager().fileForLocation(location);
		assertEquals(file, testFile);

		// project/folder/folder/file
		path = IPath.fromOSString("folder/folder/file");
		file = project.getFile(path);
		location = project.getLocation().append(path);
		testFile = getLocalManager().fileForLocation(location);
		assertEquals(file, testFile);

		/* non-existent location */
		testFile = getLocalManager().fileForLocation(IPath.fromOSString("../this/path/must/not/exist"));
		assertNull(testFile);
	}

	@Test
	public void testIsLocal() throws CoreException {
		// create resources
		IResource[] resources = buildResources(project, new String[] { "/Folder1/", "/Folder1/File1",
				"/Folder1/Folder2/", "/Folder1/Folder2/File2", "/Folder1/Folder2/Folder3/" });
		createInWorkspace(resources);
		for (IResource resource : resources) {
			removeFromFileSystem(resource);
		}

		// exists
		assertTrue(project.isLocal(IResource.DEPTH_INFINITE)); // test

		// test the depth parameter
		final IFolder folder = project.getFolder("Folder1");
		IWorkspaceRunnable operation = monitor -> {
			IResource[] members = folder.members();
			for (IResource member : members) {
				((Resource) member).getResourceInfo(false, true).clear(M_LOCAL_EXISTS);
			}
		};
		getWorkspace().run(operation, null);
		assertTrue(project.isLocal(IResource.DEPTH_ONE));
		assertTrue(folder.isLocal(IResource.DEPTH_ZERO));
		assertFalse(folder.isLocal(IResource.DEPTH_INFINITE));

		// remove the trash
		removeFromWorkspace(project);
	}

	/**
	 * Scenario:
	 *		1 solution
	 * 		3 projects
	 */
	@Test
	public void testLocationFor() {
		/* test project */
		IPath location = project.getLocation();
		assertEquals(getLocalManager().locationFor(project), location);
		assertEquals(getWorkspace().getRoot().getLocation().append(project.getName()), location);
	}

	@Test
	public void testSynchronizeProject() throws Exception {
		/* test DEPTH parameter */
		/* DEPTH_ZERO */
		IFile file = project.getFile("file");
		createInFileSystem(file);
		project.refreshLocal(IResource.DEPTH_ZERO, null);
		assertFalse(file.exists());
		/* DEPTH_ONE */
		IFolder folder = project.getFolder("folder");
		IFolder subfolder = folder.getFolder("subfolder");
		IFile subfile = folder.getFile("subfile");
		createInFileSystem(folder);
		createInFileSystem(subfolder);
		createInFileSystem(subfile);
		project.refreshLocal(IResource.DEPTH_ONE, null);
		assertTrue(file.exists());
		assertTrue(folder.exists());
		assertFalse(subfolder.exists());
		assertFalse(subfile.exists());
		/* DEPTH_INFINITE */
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		assertTrue(file.exists());
		assertTrue(folder.exists());
		assertTrue(subfolder.exists());
		assertTrue(subfile.exists());

		/* closed project */
		file = project.getFile("closed");
		project.close(null);
		createInFileSystem(file);
		project.open(null);
		assertFalse(file.exists());
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		assertTrue(file.exists());

		/* refreshing an inexisting project should do nothing */
		getWorkspace().getRoot().getProject("inexistingProject").refreshLocal(IResource.DEPTH_INFINITE, null);
	}

	@Test
	public void testWriteFile() throws Exception {
		IFile file = project.getFile("testWriteFile");
		createInWorkspace(file);
		/* common contents */
		String originalContent = "this string should not be equal the other";
		String anotherContent = "and this string should not... well, you know...";
		InputStream original;
		InputStream another;

		/* write file for the first time */
		original = createInputStream(originalContent);
		write(file, original, true, null);

		original = createInputStream(originalContent);
		assertTrue("Unexpected content in " + original,
				compareContent(original, getLocalManager().read(file, true, null)));

		/* test the overwrite parameter (false) */
		another = createInputStream(anotherContent);
		write(file, another, false, null);

		another = createInputStream(anotherContent);
		assertTrue("Unexpected content in " + another,
				compareContent(another, getLocalManager().read(file, true, null)));

		/* test the overwrite parameter (true) */
		original = createInputStream(originalContent);
		write(file, original, true, null);

		original = createInputStream(originalContent);
		assertTrue("Unexpected content in " + original,
				compareContent(original, getLocalManager().read(file, true, null)));

		/* test the overwrite parameter (false) */
		ensureOutOfSync(file);
		InputStream another2 = createInputStream(anotherContent);
		assertThrows("Should fail writing out of sync file #1", CoreException.class,
				() -> write(file, another2, false, null));
		ensureOutOfSync(file);
		assertThrows("Should fail writing out of sync file #2", CoreException.class,
				() -> file.setContents(another2, false, false, null));
		file.setContents(another, true, false, null);

		/* test the overwrite parameter (false) */
		removeFromFileSystem(file); // FIXME Race Condition with asynchronous workplace refresh see Bug 571133
		InputStream another3 = createInputStream(anotherContent);
		waitForRefresh(); // wait for refresh to ensure that file is not present in workspace
		assertThrows("Should fail writing non existing file", CoreException.class,
				() -> write(file, another3, false, null));

		/* remove trash */
		removeFromWorkspace(project);
	}

	// See https://github.com/eclipse-platform/eclipse.platform/issues/103
	@Test
	public void testWriteDeletedFile() throws CoreException {
		IFile file = project.getFile("testWriteFile");
		createInWorkspace(file);
		String content = "original";

		/* write file for the first time */
		write(file, createInputStream(content), true, null);

		file.delete(true, null);
		assertThrows("Should fail writing file that is already deleted", CoreException.class,
				() -> write(file, createInputStream(content), false, null));
	}

	@Test
	public void testWriteFileNotInWorkspace() throws CoreException {
		// Bug 571133
		IFile file = project.getFile("testWriteFile2");

		// this file does NOT exist in workspace yet -> no ResourceInfo;

		/* common contents */
		String anotherContent = "and this string should not... well, you know...";
		InputStream another = createInputStream(anotherContent);
		assertThrows(CoreException.class, () -> write(file, another, false, null));

		/* remove trash */
		removeFromWorkspace(project);
	}

	@Test
	public void testWriteFolder() throws Exception {
		IFolder folder = project.getFolder("testWriteFolder");
		createInWorkspace(folder);

		/* existing file on destination */
		removeFromFileSystem(folder);
		IFile file = project.getFile("testWriteFolder");
		createInFileSystem(file);
		/* force = true */
		assertThrows(CoreException.class, () -> write(folder, true, null));
		/* force = false */
		assertThrows(CoreException.class, () -> write(folder, false, null));
		removeFromFileSystem(file);

		/* existing folder on destination */
		createInFileSystem(folder);
		/* force = true */
		write(folder, true, null);
		assertTrue(folder.getLocation().toFile().isDirectory());
		/* force = false */
		assertThrows(CoreException.class, () -> write(folder, false, null));
		removeFromFileSystem(folder);

		/* inexisting resource on destination */
		/* force = true */
		write(folder, true, null);
		assertTrue(folder.getLocation().toFile().isDirectory());
		removeFromFileSystem(folder);
		/* force = false */
		write(folder, false, null);
		assertTrue(folder.getLocation().toFile().isDirectory());
	}

	/**
	 * 1.2 - write a project with default local location
	 * 2.1 - delete project's local location
	 */
	@Test
	public void testWriteProject() throws CoreException {
		IFile dotProject = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		final IFileStore fileStore = ((Resource) project).getStore();
		// create project and then delete from file system
		// wrap in runnable to prevent snapshot from occurring in the middle.
		getWorkspace().run((IWorkspaceRunnable) monitor -> {
			removeFromFileSystem(project);
			assertFalse("2.1", fileStore.fetchInfo().isDirectory());
			//write project in a runnable, otherwise tree will be locked
			((Project) project).writeDescription(IResource.FORCE);
		}, null);
		assertTrue(fileStore.fetchInfo().isDirectory());
		long lastModified = ((Resource) dotProject).getStore().fetchInfo().getLastModified();
		assertEquals(lastModified, ((Resource) project).getResourceInfo(false, false).getLocalSyncInfo());
	}

	/**
	 * Test for bug 547691, exception when passing deleted project path to
	 * {@link FileSystemResourceManager#locationURIFor(IResource)}.
	 */
	@Test
	public void testBug547691() throws CoreException {
		String projectName = createUniqueString();
		IWorkspace workspace = getWorkspace();
		project = workspace.getRoot().getProject(projectName);
		IProjectDescription projectDescription = workspace.newProjectDescription(projectName);
		project.create(projectDescription, null);
		project.open(null);
		FileSystemResourceManager manager = ((Workspace) getWorkspace()).getFileSystemManager();
		URI location = manager.locationURIFor(project);
		assertNotNull("Expected location for accessible project to not be null", location);
		project.delete(true, null);
		URI locationAfterDelete = manager.locationURIFor(project);
		assertEquals("Expected location of project to not change after delete", location, locationAfterDelete);
	}

	@Test
	public void testLightweightAutoRefreshPrefChange() {
		FileSystemResourceManager manager = ((Workspace) getWorkspace()).getFileSystemManager();
		InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES)
				.putBoolean(ResourcesPlugin.PREF_LIGHTWEIGHT_AUTO_REFRESH, false);
		assertFalse(manager.isLightweightAutoRefreshEnabled());
		InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES)
				.putBoolean(ResourcesPlugin.PREF_LIGHTWEIGHT_AUTO_REFRESH, true);
		assertTrue(manager.isLightweightAutoRefreshEnabled());
	}

	protected void write(final IFile file, final InputStream contents, final boolean force, IProgressMonitor monitor)
			throws CoreException {
		try {
			IWorkspace workspace = getWorkspace();
			assertNotNull("workspace cannot be null", workspace);
			workspace.run(new WriteFileContents(file, contents, force, getLocalManager()), monitor);
		} catch (Throwable t) {
			// Bug 541493: we see unlikely stack traces reported by JUnit here, log the
			// exceptions in case JUnit filters stack frames
			String errorMessage = "exception occured during write of file: " + file;
			IStatus errorStatus = new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, errorMessage, t);
			ResourcesPlugin.getPlugin().getLog().log(errorStatus);
			throw t;
		}
	}

	private void write(final IFolder folder, final boolean force, IProgressMonitor monitor) throws CoreException {
		IWorkspaceRunnable operation = pm -> getLocalManager().write(folder, force, monitor);
		getWorkspace().run(operation, monitor);
	}

	static class WriteFileContents implements IWorkspaceRunnable {
		private final IFile file;
		private final InputStream contents;
		private final boolean force;
		private final FileSystemResourceManager localManager;

		WriteFileContents(IFile file, InputStream contents, boolean force, FileSystemResourceManager localManager) {
			this.file = file;
			assertNotNull("file cannot be null", file);
			this.contents = contents;
			assertNotNull("contents cannot be null", contents);
			this.force = force;
			this.localManager = localManager;
			assertNotNull("file system resource manager cannot be null", localManager);
		}

		@Override
		public void run(IProgressMonitor jobMonitor) throws CoreException {
			int flags = force ? IResource.FORCE : IResource.NONE;
			IFileStore store = ((Resource) file).getStore();
			assertNotNull("file store cannot be null", store);
			IFileInfo info = store.fetchInfo();
			assertNotNull("file info cannot be null for file " + file, info);
			localManager.write(file, contents, info, flags, false, jobMonitor);
		}
	}

	private FileSystemResourceManager getLocalManager() {
		return ((Workspace) getWorkspace()).getFileSystemManager();
	}

}
