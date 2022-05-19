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

import static org.junit.Assert.assertThrows;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.localstore.FileSystemResourceManager;
import org.eclipse.core.internal.resources.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.tests.internal.filesystem.bug440110.Bug440110FileSystem;

public class FileSystemResourceManagerTest extends LocalStoreTest implements ICoreConstants {

	@Override
	public String[] defineHierarchy() {
		return new String[] {"/Folder1/", "/Folder1/File1", "/Folder1/Folder2/", "/Folder1/Folder2/File2", "/Folder1/Folder2/Folder3/"};
	}

	public void testBug440110() throws URISyntaxException, CoreException {
		String projectName = getUniqueString();
		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject(projectName);
		IProjectDescription projectDescription = workspace.newProjectDescription(projectName);
		projectDescription.setLocationURI(new URI(Bug440110FileSystem.SCHEME + "://" + projectName));
		project.create(projectDescription, null);
		project.open(null);
		assertEquals("0.1", Bug440110FileSystem.SCHEME, project.getLocationURI().getScheme());

		IFolder folder = project.getFolder("folder");
		folder.create(true, true, null);
		assertEquals("0.2", Bug440110FileSystem.SCHEME, folder.getLocationURI().getScheme());

		Bug440110FileSystem.clearFetchedFileTree();
		folder.refreshLocal(IResource.DEPTH_ZERO, null);
		assertFalse("1.0", Bug440110FileSystem.hasFetchedFileTree());

		Bug440110FileSystem.clearFetchedFileTree();
		folder.refreshLocal(IResource.DEPTH_ONE, null);
		assertTrue("2.0", Bug440110FileSystem.hasFetchedFileTree());

		Bug440110FileSystem.clearFetchedFileTree();
		folder.refreshLocal(IResource.DEPTH_INFINITE, null);
		assertTrue("3.0", Bug440110FileSystem.hasFetchedFileTree());
	}

	public void testContainerFor() {

		/* test null parameter */
		assertThrows(RuntimeException.class, () -> getLocalManager().containerForLocation(null));

		/* test normal conditions under default mapping */

		// project/target
		Path path = new Path("target");
		IFolder folder = projects[0].getFolder(path);
		IPath location = projects[0].getLocation().append(path);
		IFolder testFolder = (IFolder) getLocalManager().containerForLocation(location);
		assertEquals("2.1", folder, testFolder);

		// project/folder/target
		path = new Path("folder/target");
		folder = projects[0].getFolder(path);
		location = projects[0].getLocation().append(path);
		testFolder = (IFolder) getLocalManager().containerForLocation(location);
		assertEquals("2.2", folder, testFolder);

		// project/folder/folder/target
		path = new Path("folder/folder/target");
		folder = projects[0].getFolder(path);
		location = projects[0].getLocation().append(path);
		testFolder = (IFolder) getLocalManager().containerForLocation(location);
		assertEquals("2.3", folder, testFolder);

		/* non-existent location */
		testFolder = (IFolder) getLocalManager().containerForLocation(new Path("../this/path/must/not/exist"));
		assertNull("3.1", testFolder);
	}

	/**
	 * this test should move to FileTest
	 */
	public void testCreateFile() {
		/* initialize common objects */
		IProject project = projects[0];
		File file = (File) project.getFile("testCreateFile");
		/* common contents */
		String originalContent = "this string should not be equal the other";

		/* create file with flag false */
		try {
			file.create(getContents(originalContent), false, null);
		} catch (CoreException e) {
			fail("1.1", e);
		}
		assertTrue("1.2", file.exists());
		assertTrue("1.3", file.isLocal(IResource.DEPTH_ZERO));
		assertEquals("1.4", file.getStore().fetchInfo().getLastModified(), file.getResourceInfo(false, false).getLocalSyncInfo());
		try {
			assertTrue("1.5", compareContent(getContents(originalContent), getLocalManager().read(file, true, null)));
		} catch (CoreException e) {
			fail("1.6", e);
		}
	}

	public void testFileFor() {

		/* test null parameter */
		assertThrows(RuntimeException.class, () -> getLocalManager().fileForLocation(null));

		/* test normal conditions under default mapping */

		// project/file
		Path path = new Path("file");
		IFile file = projects[0].getFile(path);
		IPath location = projects[0].getLocation().append(path);
		IFile testFile = getLocalManager().fileForLocation(location);
		assertEquals("2.1", file, testFile);

		// project/folder/file
		path = new Path("folder/file");
		file = projects[0].getFile(path);
		location = projects[0].getLocation().append(path);
		testFile = getLocalManager().fileForLocation(location);
		assertEquals("2.2", file, testFile);

		// project/folder/folder/file
		path = new Path("folder/folder/file");
		file = projects[0].getFile(path);
		location = projects[0].getLocation().append(path);
		testFile = getLocalManager().fileForLocation(location);
		assertEquals("2.3", file, testFile);

		/* non-existent location */
		testFile = getLocalManager().fileForLocation(new Path("../this/path/must/not/exist"));
		assertNull("7.1", testFile);
	}

	public void testIsLocal() throws CoreException {
		/* initialize common objects */
		final IProject project = projects[0];

		// create resources
		IResource[] resources = buildResources(project, defineHierarchy());
		ensureExistsInWorkspace(resources, true);
		ensureDoesNotExistInFileSystem(resources);

		// exists
		assertTrue("1.0", project.isLocal(IResource.DEPTH_INFINITE)); // test

		// test the depth parameter
		final IFolder folder = project.getFolder("Folder1");
		IWorkspaceRunnable operation = monitor -> {
			IResource[] members = folder.members();
			for (IResource member : members) {
				((Resource) member).getResourceInfo(false, true).clear(M_LOCAL_EXISTS);
			}
		};
		getWorkspace().run(operation, null);
		assertTrue("2.0", project.isLocal(IResource.DEPTH_ONE));
		assertTrue("3.0", folder.isLocal(IResource.DEPTH_ZERO));
		assertFalse("4.0", folder.isLocal(IResource.DEPTH_INFINITE));

		// remove the trash
		ensureDoesNotExistInWorkspace(project);
	}

	/**
	 * Scenario:
	 *		1 solution
	 * 		3 projects
	 */
	public void testLocationFor() throws Throwable {

		/* test project */
		IPath location = projects[0].getLocation();
		assertTrue("2.1", location.equals(getLocalManager().locationFor(projects[0])));
		assertTrue("2.2", location.equals(getWorkspace().getRoot().getLocation().append(projects[0].getName())));
	}

	/**
	 * Scenario:
	 *		1 solution
	 * 		3 projects
	 */
	public void testResourcePathFor() throws Throwable {

		///* test null parameter */
		//try {
		//getLocalManager().resourcePathFor(null);
		//fail("1.1");
		//} catch (RuntimeException e) {
		//} catch (CoreException e) {
		//fail("1.2", e);
		//}

		///* test normal conditions under default mapping */

		//// project/file
		//Path path = new Path("file");
		//IFile file = projects[0].getFile(path);
		//IPath location = projects[0].getLocation().append(path);
		//IPath testPath = getLocalManager().resourcePathFor(location);
		//assert("2.1", file.getFullPath().equals(testPath));

		//// project/folder/file
		//path = new Path("folder/file");
		//file = projects[0].getFile(path);
		//location = projects[0].getLocation().append(path);
		//testPath = getLocalManager().resourcePathFor(location);
		//assert("2.2", file.getFullPath().equals(testPath));

		//// project/folder/folder/file
		//path = new Path("folder/folder/file");
		//file = projects[0].getFile(path);
		//location = projects[0].getLocation().append(path);
		//testPath = getLocalManager().resourcePathFor(location);
		//assert("2.3", file.getFullPath().equals(testPath));
	}

	public void testSynchronizeProject() throws Throwable {
		/* test DEPTH parameter */
		/* DEPTH_ZERO */
		IFile file = projects[0].getFile("file");
		ensureExistsInFileSystem(file);
		projects[0].refreshLocal(IResource.DEPTH_ZERO, null);
		assertFalse("1.1", file.exists());
		/* DEPTH_ONE */
		IFolder folder = projects[0].getFolder("folder");
		IFolder subfolder = folder.getFolder("subfolder");
		IFile subfile = folder.getFile("subfile");
		ensureExistsInFileSystem(folder);
		ensureExistsInFileSystem(subfolder);
		ensureExistsInFileSystem(subfile);
		projects[0].refreshLocal(IResource.DEPTH_ONE, null);
		assertTrue("2.1", file.exists());
		assertTrue("2.2", folder.exists());
		assertFalse("2.3", subfolder.exists());
		assertFalse("2.4", subfile.exists());
		/* DEPTH_INFINITE */
		projects[0].refreshLocal(IResource.DEPTH_INFINITE, null);
		assertTrue("3.1", file.exists());
		assertTrue("3.2", folder.exists());
		assertTrue("3.3", subfolder.exists());
		assertTrue("3.4", subfile.exists());

		/* closed project */
		file = projects[0].getFile("closed");
		projects[0].close(null);
		ensureExistsInFileSystem(file);
		projects[0].open(null);
		assertFalse("4.1", file.exists());
		projects[0].refreshLocal(IResource.DEPTH_INFINITE, null);
		assertTrue("4.2", file.exists());

		/* refreshing an inexisting project should do nothing */
		getWorkspace().getRoot().getProject("inexistingProject").refreshLocal(IResource.DEPTH_INFINITE, null);
	}

	public void testWriteFile() throws Exception {
		/* initialize common objects */
		IProject project = projects[0];
		IFile file = project.getFile("testWriteFile");
		ensureExistsInWorkspace(file, true);
		/* common contents */
		String originalContent = "this string should not be equal the other";
		String anotherContent = "and this string should not... well, you know...";
		InputStream original;
		InputStream another;

		/* write file for the first time */
		original = getContents(originalContent);
		write(file, original, true, null);

		original = getContents(originalContent);
		assertTrue("Unexpected content in " + original,
				compareContent(original, getLocalManager().read(file, true, null)));

		/* test the overwrite parameter (false) */
		another = getContents(anotherContent);
		write(file, another, false, null);

		another = getContents(anotherContent);
		assertTrue("Unexpected content in " + another,
				compareContent(another, getLocalManager().read(file, true, null)));

		/* test the overwrite parameter (true) */
		original = getContents(originalContent);
		write(file, original, true, null);

		original = getContents(originalContent);
		assertTrue("Unexpected content in " + original,
				compareContent(original, getLocalManager().read(file, true, null)));

		/* test the overwrite parameter (false) */
		ensureOutOfSync(file);
		InputStream another2 = getContents(anotherContent);
		assertThrows("Should fail writing out of sync file #1", CoreException.class,
				() -> write(file, another2, false, null));
		ensureOutOfSync(file);
		assertThrows("Should fail writing out of sync file #2", CoreException.class,
				() -> file.setContents(another2, false, false, null));
		file.setContents(another, true, false, null);

		/* test the overwrite parameter (false) */
		ensureDoesNotExistInFileSystem(file); // FIXME Race Condition with asynchronous workplace refresh see Bug 571133
		InputStream another3 = getContents(anotherContent);
		assertThrows("Should fail writing non existing file", CoreException.class,
				() -> write(file, another3, false, null));

		/* remove trash */
		ensureDoesNotExistInWorkspace(project);
	}

	public void testWriteFile2() {
		// Bug 571133
		IProject project = projects[0];
		IFile file = project.getFile("testWriteFile2");

		// this file does NOT exist in workspace yet -> no ResourceInfo;

		/* common contents */
		String anotherContent = "and this string should not... well, you know...";
		InputStream another = getContents(anotherContent);
		assertThrows(IllegalStateException.class, () -> write(file, another, false, null));

		/* remove trash */
		ensureDoesNotExistInWorkspace(project);
	}

	public void testWriteFolder() throws Throwable {
		/* initialize common objects */
		IProject project = projects[0];
		IFolder folder = project.getFolder("testWriteFolder");
		ensureExistsInWorkspace(folder, true);

		/* existing file on destination */
		ensureDoesNotExistInFileSystem(folder);
		IFile file = project.getFile("testWriteFolder");
		ensureExistsInFileSystem(file);
		/* force = true */
		assertThrows(CoreException.class, () -> write(folder, true, null));
		/* force = false */
		assertThrows(CoreException.class, () -> write(folder, false, null));
		ensureDoesNotExistInFileSystem(file);

		/* existing folder on destination */
		ensureExistsInFileSystem(folder);
		/* force = true */
		write(folder, true, null);
		assertTrue("2.1", folder.getLocation().toFile().isDirectory());
		/* force = false */
		assertThrows(CoreException.class, () -> write(folder, false, null));
		ensureDoesNotExistInFileSystem(folder);

		/* inexisting resource on destination */
		/* force = true */
		write(folder, true, null);
		assertTrue("3.1", folder.getLocation().toFile().isDirectory());
		ensureDoesNotExistInFileSystem(folder);
		/* force = false */
		write(folder, false, null);
		assertTrue("3.1", folder.getLocation().toFile().isDirectory());
	}

	/**
	 * 1.2 - write a project with default local location
	 * 2.1 - delete project's local location
	 */
	public void testWriteProject() throws Throwable {
		/* initialize common objects */
		final IProject project = projects[0];
		IFile dotProject = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		final IFileStore fileStore = ((Resource) project).getStore();
		// create project and then delete from file system
		// wrap in runnable to prevent snapshot from occurring in the middle.
		getWorkspace().run((IWorkspaceRunnable) monitor -> {
			ensureDoesNotExistInFileSystem(project);
			assertFalse("2.1", fileStore.fetchInfo().isDirectory());
			//write project in a runnable, otherwise tree will be locked
			((Project) project).writeDescription(IResource.FORCE);
		}, null);
		assertTrue("2.2", fileStore.fetchInfo().isDirectory());
		long lastModified = ((Resource) dotProject).getStore().fetchInfo().getLastModified();
		assertEquals("2.3", lastModified, ((Resource) project).getResourceInfo(false, false).getLocalSyncInfo());
	}

	/**
	 * Test for bug 547691, exception when passing deleted project path to
	 * {@link FileSystemResourceManager#locationURIFor(IResource)}.
	 */
	public void testBug547691() throws Exception {
		String projectName = getUniqueString();
		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject(projectName);
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

	protected void write(final IFile file, final InputStream contents, final boolean force, IProgressMonitor monitor)
			throws CoreException {
		try {
			IWorkspace workspace = getWorkspace();
			assertNotNull("workspace cannot be null", workspace);
			workspace.run(new WriteFileContents(file, contents, force, getLocalManager()), null);
		} catch (Throwable t) {
			// Bug 541493: we see unlikely stack traces reported by JUnit here, log the
			// exceptions in case JUnit filters stack frames
			String errorMessage = "exception occured during write of file: " + file;
			IStatus errorStatus = new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, errorMessage, t);
			ResourcesPlugin.getPlugin().getLog().log(errorStatus);
			throw t;
		}
	}

	protected void write(final IFolder folder, final boolean force, IProgressMonitor monitor) throws CoreException {
		IWorkspaceRunnable operation = pm -> getLocalManager().write(folder, force, null);
		getWorkspace().run(operation, null);
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
}
