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
package org.eclipse.core.tests.resources;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.tests.internal.filesystem.wrapper.WrapperFileSystem;

public class IWorkspaceRootTest extends ResourceTest {

	@Override
	protected void tearDown() throws Exception {
		IProject[] projects = getWorkspace().getRoot().getProjects();
		getWorkspace().delete(projects, true, null);
	}

	/**
	 * Tests findFilesForLocation when non-canonical paths are used (bug 155101).
	 */
	public void testFindFilesNonCanonicalPath() {
		// this test is for windows only
		if (!isWindows()) {
			return;
		}
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
		try {
			link.createLink(fileLocationLower, IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("1.99", e);
		}
		//try to find the file using the upper case device
		IFile[] files = getWorkspace().getRoot().findFilesForLocation(fileLocationUpper);
		assertEquals("1.0", 1, files.length);
		assertEquals("1.1", link, files[0]);
	}

	/**
	 * Tests the API method findContainersForLocation.
	 */
	public void testFindContainersForLocation() {
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

	public void testFindContainersForLocationOnWrappedFileSystem() {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject p1 = root.getProject("p1");
		IProject p2 = root.getProject("p2");
		ensureExistsInWorkspace(new IResource[] {p1, p2}, true);
		try {
			replaceProject(p1, WrapperFileSystem.getWrappedURI(p1.getLocationURI()));
		} catch (CoreException e) {
			fail("0.1", e);
		}
		try {
			replaceProject(p2, WrapperFileSystem.getWrappedURI(p2.getLocationURI()));
		} catch (CoreException e) {
			fail("0.2", e);
		}
		testFindContainersForLocation(p1, p2);
	}

	/**
	 * Tests the API method findContainersForLocation.
	 */
	public void testFindContainersForLocation(IProject p1, IProject p2) {
		//should find the workspace root
		IWorkspaceRoot root = getWorkspace().getRoot();
		IContainer[] result = root.findContainersForLocation(root.getLocation());
		assertEquals("1.0", 1, result.length);
		assertEquals("1.1", root, result[0]);

		//deep linked resource
		IFolder parent = p2.getFolder("parent");
		IFolder link = parent.getFolder("link");
		ensureExistsInWorkspace(new IResource[] {p1, p2, parent}, true);
		try {
			link.createLink(p1.getLocationURI(), IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("1.99", e);
		}
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
		URI relative = null;
		try {
			relative = new URI(null, "hello", null);
		} catch (URISyntaxException e) {
			fail("4.99", e);
		}
		try {
			root.findContainersForLocationURI(relative);
			//should fail
			fail("4.1");
		} catch (RuntimeException e) {
			//expected
		}
		//linked folder that does not overlap a project location
		IFolder otherLink = p1.getFolder("otherLink");
		IFileStore linkStore = getTempStore();
		URI location = linkStore.toURI();
		try {
			linkStore.mkdir(EFS.NONE, getMonitor());
			otherLink.createLink(location, IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("5.99", e);
		}
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
	public void testFindFilesForLocationOnWrappedFileSystem() {
		//should not find the workspace root
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject("p1");
		ensureExistsInWorkspace(project, true);
		try {
			replaceProject(project, WrapperFileSystem.getWrappedURI(project.getLocationURI()));
		} catch (CoreException e) {
			fail("0.1", e);
		}
		testFindFilesForLocation(project);
	}

	/**
	 * Tests the API method findFilesForLocation on non-default file system.
	 */
	public void testFindFilesForLocation() {
		//should not find the workspace root
		IWorkspaceRoot root = getWorkspace().getRoot();
		testFindFilesForLocation(root.getProject("p1"));
	}

	/**
	 * Tests the API method findFilesForLocation.
	 */
	public void testFindFilesForLocation(IProject project) {
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
		if (!isCaseSensitive(existing)) {
			IPath differentCase = new Path(existingFileLocation.toOSString().toUpperCase());
			result = root.findFilesForLocation(differentCase);
			assertResources("5.0", existing, result);
			result = root.findFilesForLocationURI(existing.getLocationURI());
			assertResources("5.1", existing, result);
		}

		//linked resource
		IFolder link = project.getFolder("link");
		IFileStore linkStore = getTempStore();
		URI location = linkStore.toURI();
		try {
			linkStore.mkdir(EFS.NONE, getMonitor());
			link.createLink(location, IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("5.99", e);
		}
		IFile child = link.getFile("link-child.txt");
		URI childLocation = linkStore.getChild(child.getName()).toURI();
		result = root.findFilesForLocationURI(childLocation);
		assertResources("2.1", child, result);
	}

	/**
	 * Asserts that the given result array contains only the given resource.
	 * @param string
	 * @param file1
	 * @param result
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
	public void testGetContainerForLocation() {
		IWorkspaceRoot root = getWorkspace().getRoot();
		assertEquals("1.0", root, root.getContainerForLocation(root.getLocation()));
	}

	/**
	 * Tests the AP method getFile(IPath)
	 */
	public void testGetFile() {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IFile file = root.getFile(new Path("//P1/a.txt"));
		assertTrue("1.0", !file.getFullPath().isUNC());
	}

	/**
	 * Tests the API method getFileForLocation
	 */
	public void testGetFileForLocation() {
		IWorkspaceRoot root = getWorkspace().getRoot();
		assertTrue("1.0", root.getFileForLocation(root.getLocation()) == null);
	}

	public void testPersistentProperty() {
		IWorkspaceRoot root = getWorkspace().getRoot();
		String value = "this is a test property value";
		QualifiedName name = new QualifiedName("test", "testProperty");
		try {
			root.setPersistentProperty(name, value);
		} catch (CoreException e) {
			fail("1.0", e);
		}

		try {
			String storedValue = root.getPersistentProperty(name);
			assertEquals("2.0", value, storedValue);
		} catch (CoreException e) {
			fail("2.1", e);
		}

		try {
			name = new QualifiedName("test", "testNonProperty");
			String storedValue = root.getPersistentProperty(name);
			assertEquals("3.0", null, storedValue);
		} catch (CoreException e) {
			fail("3.1", e);
		}
	}

	/**
	 * Since reading a persistent property doesn't normally happen inside an
	 * operation, try it inside an operation to make sure it still works.
	 * (See bug 14179).
	 */
	public void testPersistentPropertyInRunnable() {
		final IWorkspaceRoot root = getWorkspace().getRoot();
		final String value = "this is a test property value";
		final QualifiedName name = new QualifiedName("test", "testProperty");
		try {
			getWorkspace().run((IWorkspaceRunnable) monitor -> root.setPersistentProperty(name, value), getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}

		try {
			final String[] storedValue = new String[1];
			getWorkspace().run((IWorkspaceRunnable) monitor -> storedValue[0] = root.getPersistentProperty(name), getMonitor());
			assertEquals("2.0", value, storedValue[0]);
		} catch (CoreException e) {
			fail("2.1", e);
		}

		try {
			final QualifiedName name2 = new QualifiedName("test", "testNonProperty");
			final String[] storedValue = new String[1];
			getWorkspace().run((IWorkspaceRunnable) monitor -> storedValue[0] = root.getPersistentProperty(name2), getMonitor());
			assertEquals("3.0", null, storedValue[0]);
		} catch (CoreException e) {
			fail("3.1", e);
		}
	}

	public void testRefreshLocal() {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject("Project");
		ensureExistsInWorkspace(project, true);
		try {
			project.close(getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}
		//refreshing the root shouldn't fail
		try {
			root.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		} catch (CoreException e) {
			fail("1.1", e);
		}
	}

	public void testBug234343_folderInHiddenProject() {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject hiddenProject = root.getProject(getUniqueString());
		ensureDoesNotExistInWorkspace(hiddenProject);
		try {
			hiddenProject.create(null, IResource.HIDDEN, getMonitor());
			hiddenProject.open(getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}

		IFolder folder = hiddenProject.getFolder("foo");
		try {
			folder.create(true, true, getMonitor());
		} catch (CoreException e) {
			fail("4.99", e);
		}

		IContainer[] containers = root.findContainersForLocationURI(folder.getLocationURI());
		assertEquals("2.0", 0, containers.length);

		containers = root.findContainersForLocationURI(folder.getLocationURI(), IContainer.INCLUDE_HIDDEN);
		assertEquals("3.0", 1, containers.length);
	}

	public void testBug234343_fileInHiddenProject() {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject hiddenProject = root.getProject(getUniqueString());
		ensureDoesNotExistInWorkspace(hiddenProject);
		try {
			hiddenProject.create(null, IResource.HIDDEN, getMonitor());
			hiddenProject.open(getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}

		IFile file = hiddenProject.getFile("foo");
		try {
			file.create(new ByteArrayInputStream("foo".getBytes()), true, getMonitor());
		} catch (CoreException e) {
			fail("2.0", e);
		}

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
	public void testBug476585() {
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

		try {
			subProject.create(newProjectDescription, getMonitor());
		} catch (CoreException e) {
			fail("1.99", e);
		}

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
	public void testFindMethodsWithHiddenAndTeamPrivateFlags() {
		checkFindMethods(IResource.NONE, new int[][] {{IResource.NONE, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0}, {IContainer.INCLUDE_HIDDEN, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0}, {IContainer.INCLUDE_HIDDEN | IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, {IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}});

		checkFindMethods(IResource.HIDDEN, new int[][] {{IResource.NONE, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0}, {IContainer.INCLUDE_HIDDEN, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0}, {IContainer.INCLUDE_HIDDEN | IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, {IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0}});

		checkFindMethods(IResource.TEAM_PRIVATE, new int[][] {{IResource.NONE, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0}, {IContainer.INCLUDE_HIDDEN, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0}, {IContainer.INCLUDE_HIDDEN | IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, {IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}});

		checkFindMethods(IResource.TEAM_PRIVATE | IResource.HIDDEN, new int[][] {{IResource.NONE, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0}, {IContainer.INCLUDE_HIDDEN, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0}, {IContainer.INCLUDE_HIDDEN | IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, {IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0}});
	}

	public void checkFindMethods(int updateFlags, int[][] results) {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject(getUniqueString());
		ensureDoesNotExistInWorkspace(project);

		try {
			project.create(null, IResource.NONE, getMonitor());
			project.open(getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}

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

	private IFile createFile(IContainer parent, int updateFlags, boolean linked) {
		IFile file = parent.getFile(new Path(getUniqueString()));
		try {
			if (linked) {
				try {
					IPath path = getTempDir().append(getUniqueString());
					path.toFile().createNewFile();
					file.createLink(URIUtil.toURI(path), updateFlags, getMonitor());
					if ((updateFlags & IResource.TEAM_PRIVATE) == IResource.TEAM_PRIVATE) {
						file.setTeamPrivateMember(true);
					}
				} catch (IOException e) {
					fail("Can't create the file", e);
				}
			} else {
				file.create(new ByteArrayInputStream("content".getBytes()), updateFlags, getMonitor());
			}
		} catch (CoreException e) {
			fail("Can't create the file", e);
		}
		return file;
	}

	private IFolder createFolder(IContainer parent, int updateFlags, boolean linked) {
		IFolder folder = parent.getFolder(new Path(getUniqueString()));
		try {
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
		} catch (CoreException e) {
			fail("Can't create the folder", e);
		}
		return folder;
	}
}
