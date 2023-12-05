/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
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
package org.eclipse.core.tests.resources;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.net.URI;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform.OS;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.tests.internal.filesystem.wrapper.WrapperFileSystem;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class IWorkspaceRootTest extends ResourceTest {

	/**
	 * Tests findFilesForLocation when non-canonical paths are used (bug 155101).
	 */
	@Test
	public void testFindFilesNonCanonicalPath() throws CoreException {
		// this test is for windows only
		Assume.assumeTrue(OS.isWindows());

		IProject project = getWorkspace().getRoot().getProject("testFindFilesNonCanonicalPath");
		ensureExistsInWorkspace(project, true);

		IFile link = project.getFile("file.txt");
		IFileStore fileStore = getTempStore();
		createFileInFileSystem(fileStore);
		assertEquals("0.1", EFS.SCHEME_FILE, fileStore.getFileSystem().getScheme());
		IPath fileLocationLower = URIUtil.toPath(fileStore.toURI());
		fileLocationLower = fileLocationLower.setDevice(fileLocationLower.getDevice().toLowerCase());
		IPath fileLocationUpper = fileLocationLower.setDevice(fileLocationLower.getDevice().toUpperCase());
		//create the link with lower case device
		link.createLink(fileLocationLower, IResource.NONE, getMonitor());

		//try to find the file using the upper case device
		IFile[] files = getWorkspace().getRoot().findFilesForLocation(fileLocationUpper);
		assertEquals("1.0", 1, files.length);
		assertEquals("1.1", link, files[0]);
	}

	/**
	 * Tests the API method findContainersForLocation.
	 */
	@Test
	public void testFindContainersForLocation() throws Exception {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject p1 = root.getProject("p1");
		IProject p2 = root.getProject("p2");
		testFindContainersForLocation(p1, p2);
	}

	private void replaceProject(IProject project, URI newLocation) throws CoreException {
		IProjectDescription projectDesc = project.getDescription();
		projectDesc.setLocationURI(newLocation);
		project.move(projectDesc, IResource.REPLACE, null);
	}

	@Test
	public void testFindContainersForLocationOnWrappedFileSystem() throws Exception {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject p1 = root.getProject("p1");
		IProject p2 = root.getProject("p2");
		ensureExistsInWorkspace(new IResource[] {p1, p2}, true);
		replaceProject(p1, WrapperFileSystem.getWrappedURI(p1.getLocationURI()));
		replaceProject(p2, WrapperFileSystem.getWrappedURI(p2.getLocationURI()));
		testFindContainersForLocation(p1, p2);
	}

	/**
	 * Tests the API method findContainersForLocation.
	 */
	private void testFindContainersForLocation(IProject p1, IProject p2) throws Exception {
		//should find the workspace root
		IWorkspaceRoot root = getWorkspace().getRoot();
		IContainer[] result = root.findContainersForLocation(root.getLocation());
		assertEquals("1.0", 1, result.length);
		assertEquals("1.1", root, result[0]);

		//deep linked resource
		IFolder parent = p2.getFolder("parent");
		IFolder link = parent.getFolder("link");
		ensureExistsInWorkspace(new IResource[] {p1, p2, parent}, true);
		link.createLink(p1.getLocationURI(), IResource.NONE, getMonitor());
		assertResources("2.0", p1, link, root.findContainersForLocation(p1.getLocation()));

		//existing folder
		IFolder existing = p2.getFolder("existing");
		ensureExistsInWorkspace(existing, true);
		assertResources("3.0", existing, root.findContainersForLocation(existing.getLocation()));
		assertResources("3.1", existing, root.findContainersForLocationURI(existing.getLocationURI()));

		//non-existing
		IFolder nonExisting = p2.getFolder("nonExisting");
		assertResources("3.2", nonExisting, root.findContainersForLocation(nonExisting.getLocation()));
		assertResources("3.3", nonExisting, root.findContainersForLocationURI(nonExisting.getLocationURI()));

		//relative path
		assertResources("3.4", existing, root.findContainersForLocation(existing.getLocation().makeRelative()));
		assertResources("3.5", nonExisting, root.findContainersForLocation(nonExisting.getLocation().makeRelative()));

		//relative URI is illegal
		URI relative = new URI(null, "hello", null);
		assertThrows(RuntimeException.class, () -> root.findContainersForLocationURI(relative));
		//linked folder that does not overlap a project location
		IFolder otherLink = p1.getFolder("otherLink");
		IFileStore linkStore = getTempStore();
		URI location = linkStore.toURI();
		linkStore.mkdir(EFS.NONE, getMonitor());
		otherLink.createLink(location, IResource.NONE, getMonitor());
		result = root.findContainersForLocationURI(location);
		assertResources("5.1", otherLink, result);

		//child of linked folder
		IFolder child = otherLink.getFolder("link-child");
		URI childLocation = linkStore.getChild(child.getName()).toURI();
		result = root.findContainersForLocationURI(childLocation);
		assertResources("5.1", child, result);

	}

	/**
	 * Tests the API method findFilesForLocation.
	 */
	@Test
	public void testFindFilesForLocationOnWrappedFileSystem() throws CoreException {
		//should not find the workspace root
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject("p1");
		ensureExistsInWorkspace(project, true);
		replaceProject(project, WrapperFileSystem.getWrappedURI(project.getLocationURI()));
		testFindFilesForLocation(project);
	}

	/**
	 * Tests the API method findFilesForLocation on non-default file system.
	 */
	@Test
	public void testFindFilesForLocation() throws CoreException {
		//should not find the workspace root
		IWorkspaceRoot root = getWorkspace().getRoot();
		testFindFilesForLocation(root.getProject("p1"));
	}

	/**
	 * Tests the API method findFilesForLocation.
	 */
	private void testFindFilesForLocation(IProject project) throws CoreException {
		//should not find the workspace root
		IWorkspaceRoot root = getWorkspace().getRoot();
		IFile[] result = root.findFilesForLocation(root.getLocation());
		assertEquals("1.0", 0, result.length);

		IFile existing = project.getFile("file1");
		ensureExistsInWorkspace(existing, true);

		//existing file
		final IPath existingFileLocation = existing.getLocation();
		result = root.findFilesForLocation(existingFileLocation);
		assertResources("2.0", existing, result);
		result = root.findFilesForLocationURI(existing.getLocationURI());
		assertResources("2.1", existing, result);

		//non-existing file
		IFile nonExisting = project.getFile("nonExisting");
		result = root.findFilesForLocation(nonExisting.getLocation());
		assertResources("3.1", nonExisting, result);
		result = root.findFilesForLocationURI(nonExisting.getLocationURI());
		assertResources("3.2", nonExisting, result);

		//relative path
		result = root.findFilesForLocation(existingFileLocation.makeRelative());
		assertResources("4.0", existing, result);
		result = root.findFilesForLocation(nonExisting.getLocation().makeRelative());
		assertResources("4.1", nonExisting, result);

		//existing file with different case
		if (!Workspace.caseSensitive) {
			IPath differentCase = IPath.fromOSString(existingFileLocation.toOSString().toUpperCase());
			result = root.findFilesForLocation(differentCase);
			assertResources("5.0", existing, result);
			result = root.findFilesForLocationURI(existing.getLocationURI());
			assertResources("5.1", existing, result);
		}

		//linked resource
		IFolder link = project.getFolder("link");
		IFileStore linkStore = getTempStore();
		URI location = linkStore.toURI();
		linkStore.mkdir(EFS.NONE, getMonitor());
		link.createLink(location, IResource.NONE, getMonitor());
		IFile child = link.getFile("link-child.txt");
		URI childLocation = linkStore.getChild(child.getName()).toURI();
		result = root.findFilesForLocationURI(childLocation);
		assertResources("2.1", child, result);
	}

	/**
	 * Asserts that the given result array contains only the given resource.
	 */
	private void assertResources(String message, IResource expected, IResource[] actual) {
		assertEquals(message, 1, actual.length);
		assertEquals(message, expected, actual[0]);
	}

	/**
	 * Asserts that the given result array contains only the two given resources
	 */
	private void assertResources(String message, IResource expected0, IResource expected1, IResource[] actual) {
		assertEquals(message, 2, actual.length);
		if (actual[0].equals(expected0)) {
			assertEquals(message, expected1, actual[1]);
		} else if (actual[0].equals(expected1)) {
			assertEquals(message, expected0, actual[1]);
		} else {
			assertEquals(message, expected0, actual[0]);
		}
	}

	/**
	 * Tests the API method getContainerForLocation.
	 */
	@Test
	public void testGetContainerForLocation() {
		IWorkspaceRoot root = getWorkspace().getRoot();
		assertEquals("1.0", root, root.getContainerForLocation(root.getLocation()));
	}

	/**
	 * Tests the AP method getFile(IPath)
	 */
	@Test
	public void testGetFile() {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IFile file = root.getFile(IPath.fromOSString("//P1/a.txt"));
		assertTrue("1.0", !file.getFullPath().isUNC());
	}

	/**
	 * Tests the API method getFileForLocation
	 */
	@Test
	public void testGetFileForLocation() {
		IWorkspaceRoot root = getWorkspace().getRoot();
		assertTrue("1.0", root.getFileForLocation(root.getLocation()) == null);
	}

	@Test
	public void testPersistentProperty() throws CoreException {
		IWorkspaceRoot root = getWorkspace().getRoot();
		String value = "this is a test property value";
		QualifiedName name = new QualifiedName("test", "testProperty");
		root.setPersistentProperty(name, value);

		String storedValue = root.getPersistentProperty(name);
		assertEquals("2.0", value, storedValue);

		name = new QualifiedName("test", "testNonProperty");
		storedValue = root.getPersistentProperty(name);
		assertEquals("3.0", null, storedValue);
	}

	/**
	 * Since reading a persistent property doesn't normally happen inside an
	 * operation, try it inside an operation to make sure it still works.
	 * (See bug 14179).
	 */
	@Test
	public void testPersistentPropertyInRunnable() throws CoreException {
		final IWorkspaceRoot root = getWorkspace().getRoot();
		final String value = "this is a test property value";
		final QualifiedName name = new QualifiedName("test", "testProperty");
		getWorkspace().run((IWorkspaceRunnable) monitor -> root.setPersistentProperty(name, value), getMonitor());

		final String[] storedValue = new String[1];
		getWorkspace().run((IWorkspaceRunnable) monitor -> storedValue[0] = root.getPersistentProperty(name),
				getMonitor());
		assertEquals("2.0", value, storedValue[0]);

		final QualifiedName name2 = new QualifiedName("test", "testNonProperty");
		final String[] changedStoredValue = new String[1];
		getWorkspace().run((IWorkspaceRunnable) monitor -> changedStoredValue[0] = root.getPersistentProperty(name2),
				getMonitor());
		assertEquals("3.0", null, changedStoredValue[0]);
	}

	@Test
	public void testRefreshLocal() throws CoreException {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject("Project");
		ensureExistsInWorkspace(project, true);
		project.close(getMonitor());
		//refreshing the root shouldn't fail
		root.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
	}

	@Test
	public void testBug234343_folderInHiddenProject() throws CoreException {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject hiddenProject = root.getProject(getUniqueString());
		ensureDoesNotExistInWorkspace(hiddenProject);
		hiddenProject.create(null, IResource.HIDDEN, getMonitor());
		hiddenProject.open(getMonitor());

		IFolder folder = hiddenProject.getFolder("foo");
		folder.create(true, true, getMonitor());

		IContainer[] containers = root.findContainersForLocationURI(folder.getLocationURI());
		assertEquals("2.0", 0, containers.length);

		containers = root.findContainersForLocationURI(folder.getLocationURI(), IContainer.INCLUDE_HIDDEN);
		assertEquals("3.0", 1, containers.length);
	}

	@Test
	public void testBug234343_fileInHiddenProject() throws CoreException {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject hiddenProject = root.getProject(getUniqueString());
		ensureDoesNotExistInWorkspace(hiddenProject);
		hiddenProject.create(null, IResource.HIDDEN, getMonitor());
		hiddenProject.open(getMonitor());

		IFile file = hiddenProject.getFile("foo");
		file.create(new ByteArrayInputStream("foo".getBytes()), true, getMonitor());

		IFile[] files = root.findFilesForLocationURI(file.getLocationURI());
		assertEquals("3.0", 0, files.length);

		files = root.findFilesForLocationURI(file.getLocationURI(), IContainer.INCLUDE_HIDDEN);
		assertEquals("4.0", 1, files.length);

		IContainer[] containers = root.findContainersForLocationURI(file.getLocationURI());
		assertEquals("5.0", 0, containers.length);

		containers = root.findContainersForLocationURI(file.getLocationURI(), IContainer.INCLUDE_HIDDEN);
		assertEquals("6.0", 1, containers.length);
	}

	/**
	 * Regression test for bug 476585: IWorkspaceRoot#getFileForLocation(IPath) should return IFile in nested project
	 */
	@Test
	public void testBug476585() throws CoreException {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject("a");
		ensureExistsInWorkspace(project, true);

		String subProjectName = "subProject";
		IPath subProjectLocation = project.getLocation().append(subProjectName);

		IPath fileLocation = subProjectLocation.append("file.txt");
		IFile file = root.getFileForLocation(fileLocation);
		assertEquals("1.0", project, file.getProject());

		IPath containerLocation = subProjectLocation.append("folder");
		IContainer container = root.getContainerForLocation(containerLocation);
		assertEquals("1.1", project, container.getProject());

		IProject subProject = root.getProject(subProjectName);
		IProjectDescription newProjectDescription = getWorkspace().newProjectDescription(subProjectName);
		newProjectDescription.setLocation(subProjectLocation);

		subProject.create(newProjectDescription, getMonitor());

		file = root.getFileForLocation(fileLocation);
		assertNotNull("2.0", file);
		assertEquals("2.1", subProject, file.getProject());

		container = root.getContainerForLocation(containerLocation);
		assertNotNull("2.2", container);
		assertEquals("2.3", subProject, container.getProject());
	}

	/*
	* see bug 232765 for details
	*/
	@Test
	public void testFindMethodsWithHiddenAndTeamPrivateFlags() throws Exception {
		checkFindMethods(IResource.NONE, new int[][] {{IResource.NONE, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0}, {IContainer.INCLUDE_HIDDEN, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0}, {IContainer.INCLUDE_HIDDEN | IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, {IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}});

		checkFindMethods(IResource.HIDDEN, new int[][] {{IResource.NONE, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0}, {IContainer.INCLUDE_HIDDEN, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0}, {IContainer.INCLUDE_HIDDEN | IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, {IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0}});

		checkFindMethods(IResource.TEAM_PRIVATE, new int[][] {{IResource.NONE, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0}, {IContainer.INCLUDE_HIDDEN, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0}, {IContainer.INCLUDE_HIDDEN | IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, {IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}});

		checkFindMethods(IResource.TEAM_PRIVATE | IResource.HIDDEN, new int[][] {{IResource.NONE, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0}, {IContainer.INCLUDE_HIDDEN, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0}, {IContainer.INCLUDE_HIDDEN | IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, {IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0}});
	}

	public void checkFindMethods(int updateFlags, int[][] results) throws Exception {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject(getUniqueString());
		ensureDoesNotExistInWorkspace(project);

		project.create(null, IResource.NONE, getMonitor());
		project.open(getMonitor());

		// a team private folder
		IFolder teamFolder = createFolder(project, IResource.TEAM_PRIVATE, false);

		IFile mFileInTeamFolder = createFile(teamFolder, updateFlags, false);
		IFile mLinkedFileInTeamFolder = createFile(teamFolder, updateFlags, true);

		IFolder mFolderInTeamFolder = createFolder(teamFolder, updateFlags, false);
		IFolder mLinkedFolderInTeamFolder = createFolder(teamFolder, updateFlags, true);

		// a hidden folder
		IFolder hiddenFolder = createFolder(project, IResource.HIDDEN, false);

		IFile mFileInHiddenFolder = createFile(hiddenFolder, updateFlags, false);
		IFile mLinkedFileInHiddenFolder = createFile(hiddenFolder, updateFlags, true);

		IFolder mFolderInHiddenFolder = createFolder(hiddenFolder, updateFlags, false);
		IFolder mLinkedFolderInHiddenFolder = createFolder(hiddenFolder, updateFlags, true);

		// a regular folder
		IFolder folder = createFolder(project, IResource.NONE, false);

		IFile mFileInFolder = createFile(folder, updateFlags, false);
		IFile mLinkedFileInFolder = createFile(folder, updateFlags, true);

		IFolder mFolderInFolder = createFolder(folder, updateFlags, false);
		IFolder mLinkedFolderInFolder = createFolder(folder, updateFlags, true);

		for (int[] result : results) {
			checkFindContainers(hiddenFolder.getLocationURI(), result[0], result[1]);
			checkFindFiles(mFileInHiddenFolder.getLocationURI(), result[0], result[4]);
			checkFindFiles(mLinkedFileInHiddenFolder.getLocationURI(), result[0], result[5]);
			checkFindContainers(mFolderInHiddenFolder.getLocationURI(), result[0], result[2]);
			checkFindContainers(mLinkedFolderInHiddenFolder.getLocationURI(), result[0], result[3]);

			checkFindContainers(folder.getLocationURI(), result[0], result[6]);
			checkFindFiles(mFileInFolder.getLocationURI(), result[0], result[7]);
			checkFindFiles(mLinkedFileInFolder.getLocationURI(), result[0], result[8]);
			checkFindContainers(mFolderInFolder.getLocationURI(), result[0], result[9]);
			checkFindContainers(mLinkedFolderInFolder.getLocationURI(), result[0], result[10]);

			checkFindContainers(teamFolder.getLocationURI(), result[0], result[11]);
			checkFindFiles(mFileInTeamFolder.getLocationURI(), result[0], result[12]);
			checkFindFiles(mLinkedFileInTeamFolder.getLocationURI(), result[0], result[13]);
			checkFindContainers(mFolderInTeamFolder.getLocationURI(), result[0], result[14]);
			checkFindContainers(mLinkedFolderInTeamFolder.getLocationURI(), result[0], result[15]);
		}
	}

	private void checkFindFiles(URI location, int memberFlags, int foundResources) {
		IFile[] files = getWorkspace().getRoot().findFilesForLocationURI(location, memberFlags);
		assertEquals(foundResources, files.length);
	}

	private void checkFindContainers(URI location, int memberFlags, int foundResources) {
		IContainer[] containers = getWorkspace().getRoot().findContainersForLocationURI(location, memberFlags);
		assertEquals(foundResources, containers.length);
	}

	private IFile createFile(IContainer parent, int updateFlags, boolean linked) throws Exception {
		IFile file = parent.getFile(IPath.fromOSString(getUniqueString()));
		if (linked) {
			IPath path = getTempDir().append(getUniqueString());
			path.toFile().createNewFile();
			file.createLink(URIUtil.toURI(path), updateFlags, getMonitor());
			if ((updateFlags & IResource.TEAM_PRIVATE) == IResource.TEAM_PRIVATE) {
				file.setTeamPrivateMember(true);
			}
		} else {
			try (ByteArrayInputStream inputStream = new ByteArrayInputStream("content".getBytes())) {
				file.create(inputStream, updateFlags, getMonitor());
			}
		}
		return file;
	}

	private IFolder createFolder(IContainer parent, int updateFlags, boolean linked) throws CoreException {
		IFolder folder = parent.getFolder(IPath.fromOSString(getUniqueString()));
		if (linked) {
			IPath path = getTempDir().append(getUniqueString());
			path.toFile().mkdir();
			folder.createLink(URIUtil.toURI(path), updateFlags, getMonitor());
			if ((updateFlags & IResource.TEAM_PRIVATE) == IResource.TEAM_PRIVATE) {
				folder.setTeamPrivateMember(true);
			}
		} else {
			folder.create(updateFlags, true, getMonitor());
		}
		return folder;
	}
}
