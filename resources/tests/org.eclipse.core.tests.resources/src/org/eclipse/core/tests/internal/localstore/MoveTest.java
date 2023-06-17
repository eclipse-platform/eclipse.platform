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

import org.eclipse.core.internal.resources.File;
import org.eclipse.core.internal.resources.Resource;
import org.eclipse.core.internal.resources.ResourceInfo;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests the move operation.
 */
@RunWith(JUnit4.class)
public class MoveTest extends LocalStoreTest {

	@Override
	public String[] defineHierarchy() {
		return new String[] {"/", "/file1", "/file2", "/folder1/", "/folder1/file3", "/folder1/file4", "/folder2/", "/folder2/file5", "/folder2/file6", "/folder1/folder3/", "/folder1/folder3/file7", "/folder1/folder3/file8"};
	}

	/**
	 * This test has Windows as the target OS. Drives C: and D: should be available.
	 */
	@Test
	public void testMoveFileAcrossVolumes() {
		Assume.assumeTrue(isWindows());

		/* look for the adequate environment */
		String[] devices = findAvailableDevices();
		Assume.assumeFalse(devices[0] == null || devices[1] == null);

		// create common objects
		String location = getUniqueString();
		IProject source = getWorkspace().getRoot().getProject(location + "1");
		IProject destination = getWorkspace().getRoot().getProject(location + "2");
		try {
			source.create(getMonitor());
			source.open(getMonitor());

			IProjectDescription description = getWorkspace().newProjectDescription(destination.getName());
			description.setLocation(IPath.fromOSString(devices[1] + location));
			destination.create(description, getMonitor());
			destination.open(getMonitor());
		} catch (CoreException e) {
			fail("0.0", e);
		}

		String fileName = "fileToBeMoved.txt";
		IFile file = source.getFile(fileName);
		try {
			file.create(getRandomContents(), true, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}

		// add some properties to file (persistent and session)
		QualifiedName[] propNames = new QualifiedName[numberOfProperties];
		String[] propValues = new String[numberOfProperties];
		try {
			for (int j = 0; j < numberOfProperties; j++) {
				propNames[j] = new QualifiedName("test", "prop" + j);
				propValues[j] = "value" + j;
				file.setPersistentProperty(propNames[j], propValues[j]);
				file.setSessionProperty(propNames[j], propValues[j]);
			}
		} catch (CoreException e) {
			fail("2.0", e);
		}

		// move file
		IPath dest = destination.getFile(fileName).getFullPath();
		try {
			file.move(dest, true, getMonitor());
		} catch (CoreException e) {
			fail("3.0", e);
		}

		// assert file was moved
		IFile newFile = destination.getFile(fileName);
		assertDoesNotExistInWorkspace("4.1", file);
		assertDoesNotExistInFileSystem("4.2", file);
		assertExistsInWorkspace("4.3", newFile);
		assertExistsInFileSystem("4.4", newFile);

		// assert properties still exist (server, local and session)
		try {
			for (int j = 0; j < numberOfProperties; j++) {
				String persistentValue = newFile.getPersistentProperty(propNames[j]);
				Object sessionValue = newFile.getSessionProperty(propNames[j]);
				assertEquals("5.1", persistentValue, propValues[j]);
				assertEquals("5.2", sessionValue, propValues[j]);
			}
		} catch (CoreException e) {
			fail("5.3", e);
		}
	}

	/**
	 * Move one file from one project to another.
	 */
	@Test
	public void testMoveFileBetweenProjects() throws Exception {
		// create common objects
		IProject[] projects = getWorkspace().getRoot().getProjects();

		// get file instance
		String fileName = "newFile.txt";
		IFile file = projects[0].getFile(fileName);
		ensureExistsInWorkspace(file, true);

		// add some properties to file (persistent and session)
		QualifiedName[] propNames = new QualifiedName[numberOfProperties];
		String[] propValues = new String[numberOfProperties];
		for (int j = 0; j < numberOfProperties; j++) {
			propNames[j] = new QualifiedName("test", "prop" + j);
			propValues[j] = "value" + j;
			file.setPersistentProperty(propNames[j], propValues[j]);
			file.setSessionProperty(propNames[j], propValues[j]);
		}

		// move file
		IPath destination = projects[1].getFile(fileName).getFullPath();
		file.move(destination, true, null);

		// get new file instance
		IFile newFile = projects[1].getFile(fileName);

		// assert file was renamed
		assertDoesNotExistInWorkspace(file);
		assertDoesNotExistInFileSystem(file);
		assertExistsInWorkspace(newFile);
		assertExistsInFileSystem(newFile);

		// assert properties still exist (server, local and session)
		for (int j = 0; j < numberOfProperties; j++) {
			String persistentValue = newFile.getPersistentProperty(propNames[j]);
			Object sessionValue = newFile.getSessionProperty(propNames[j]);
			assertTrue("persistent property value is not the same", propValues[j].equals(persistentValue));
			assertTrue("session property value is not the same", propValues[j].equals(sessionValue));
		}
	}

	/**
	 * This test has Windows as the target OS. Drives C: and D: should be available.
	 */
	@Test
	public void testMoveFolderAcrossVolumes() {
		Assume.assumeTrue(isWindows());

		/* look for the adequate environment */
		String[] devices = findAvailableDevices();
		Assume.assumeFalse(devices[0] == null || devices[1] == null);

		// create common objects
		String location = getUniqueString();
		IProject source = getWorkspace().getRoot().getProject(location + "1");
		IProject destination = getWorkspace().getRoot().getProject(location + "2");
		try {
			source.create(getMonitor());
			source.open(getMonitor());

			IProjectDescription description = getWorkspace().newProjectDescription(destination.getName());
			description.setLocation(IPath.fromOSString(devices[1] + location));
			destination.create(description, getMonitor());
			destination.open(getMonitor());
		} catch (CoreException e) {
			fail("0.0", e);
		}

		// get folder instance
		String folderName = "folderToBeMoved";
		IFolder folder = source.getFolder(folderName);
		try {
			folder.create(true, true, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}

		// add some properties to file (persistent and session)
		QualifiedName[] propNames = new QualifiedName[numberOfProperties];
		String[] propValues = new String[numberOfProperties];
		try {
			for (int j = 0; j < numberOfProperties; j++) {
				propNames[j] = new QualifiedName("test", "prop" + j);
				propValues[j] = "value" + j;
				folder.setPersistentProperty(propNames[j], propValues[j]);
				folder.setSessionProperty(propNames[j], propValues[j]);
			}
		} catch (CoreException e) {
			fail("2.0", e);
		}

		// rename folder
		IPath dest = destination.getFile(folderName).getFullPath();
		try {
			folder.move(dest, true, getMonitor());
		} catch (CoreException e) {
			fail("3.0", e);
		}

		// assert folder was renamed
		IFolder newFolder = destination.getFolder(folderName);
		assertDoesNotExistInWorkspace("4.1", folder);
		assertDoesNotExistInFileSystem("4.2", folder);
		assertExistsInWorkspace("4.3", newFolder);
		assertExistsInFileSystem("4.4", newFolder);

		// assert properties still exist (server, local and session)
		try {
			for (int j = 0; j < numberOfProperties; j++) {
				String persistentValue = newFolder.getPersistentProperty(propNames[j]);
				Object sessionValue = newFolder.getSessionProperty(propNames[j]);
				assertEquals("5.1", persistentValue, propValues[j]);
				assertEquals("5.2", sessionValue, propValues[j]);
			}
		} catch (CoreException e) {
			fail("5.3", e);
		}
	}

	/**
	 * Move one folder from one project to another.
	 */
	@Test
	public void testMoveFolderBetweenProjects() throws Exception {
		// create common objects
		IProject[] projects = getWorkspace().getRoot().getProjects();

		// get folder instance
		String folderName = "newFolder";
		IFolder folder = projects[0].getFolder(folderName);
		ensureExistsInWorkspace(folder, true);

		// add some properties to folder (persistent and session)
		QualifiedName[] propNames = new QualifiedName[numberOfProperties];
		String[] propValues = new String[numberOfProperties];
		for (int j = 0; j < numberOfProperties; j++) {
			propNames[j] = new QualifiedName("test", "prop" + j);
			propValues[j] = "value" + j;
			folder.setPersistentProperty(propNames[j], propValues[j]);
			folder.setSessionProperty(propNames[j], propValues[j]);
		}

		// rename folder
		IPath destination = projects[1].getFolder(folderName).getFullPath();
		folder.move(destination, true, null);

		// get new folder instance
		IFolder newFolder = projects[1].getFolder(folderName);

		// assert folder was renamed
		assertDoesNotExistInWorkspace(folder);
		assertDoesNotExistInFileSystem(folder);
		assertExistsInWorkspace(newFolder);
		assertExistsInFileSystem(newFolder);

		// assert properties still exist (server, local and session)
		for (int j = 0; j < numberOfProperties; j++) {
			String persistentValue = newFolder.getPersistentProperty(propNames[j]);
			Object sessionValue = newFolder.getSessionProperty(propNames[j]);
			assertTrue("persistent property value is not the same", propValues[j].equals(persistentValue));
			assertTrue("session property value is not the same", propValues[j].equals(sessionValue));
		}
	}

	/**
	 * Move some hierarchy of folders and files.
	 */
	@Test
	public void testMoveHierarchy() throws Exception {
		// create common objects
		IProject[] projects = getWorkspace().getRoot().getProjects();

		// create the source folder
		String folderSourceName = "folder source";
		IFolder folderSource = projects[0].getFolder(folderSourceName);
		ensureExistsInWorkspace(folderSource, true);

		// create hierarchy
		String[] hierarchy = defineHierarchy();
		IResource[] resources = buildResources(folderSource, hierarchy);
		ensureExistsInWorkspace(resources, true);

		// add some properties to each resource (persistent and session)
		String[] propNames = new String[numberOfProperties];
		String[] propValues = new String[numberOfProperties];
		for (int j = 0; j < numberOfProperties; j++) {
			propNames[j] = "prop" + j;
			propValues[j] = "value" + j;
			for (IResource resource : resources) {
				QualifiedName propName = new QualifiedName("test", resource.getName() + propNames[j]);
				String propValue = resource.getName() + propValues[j];
				resource.setPersistentProperty(propName, propValue);
				resource.setSessionProperty(propName, propValue);
			}
		}

		// create the destination folder
		String folderDestinationName = "folder destination";
		IFolder folderDestination = projects[0].getFolder(folderDestinationName);

		// move hierarchy
		//IProgressMonitor monitor = new LoggingProgressMonitor(System.out);
		IProgressMonitor monitor = getMonitor();
		folderSource.move(folderDestination.getFullPath(), true, monitor);

		// get new hierarchy instance
		IResource[] newResources = buildResources(folderDestination, hierarchy);

		// assert hierarchy was moved
		assertDoesNotExistInWorkspace(resources);
		assertDoesNotExistInFileSystem(resources);
		assertExistsInWorkspace(newResources);
		assertExistsInFileSystem(newResources);

		// assert properties still exist (server, local and session)
		for (int j = 0; j < numberOfProperties; j++) {
			for (int i = 0; i < newResources.length; i++) {
				IResource destResource = newResources[i];
				IResource sourceResource = resources[i];
				/* The names of the properties will remain the same in both the source and
				 destination hierarchies.  So, be sure to use sourceResource to get the
				 name or your qualified name will contain 'folder destination' instead of
				 'folder source' and the property value will be null.
				 */
				QualifiedName propName = new QualifiedName("test", sourceResource.getName() + propNames[j]);
				String propValue = sourceResource.getName() + propValues[j];
				String persistentValue = destResource.getPersistentProperty(propName);
				Object sessionValue = destResource.getSessionProperty(propName);
				assertTrue("persistent property value is not the same", propValue.equals(persistentValue));
				assertTrue("session property value is not the same", propValue.equals(sessionValue));
			}
		}
	}

	/**
	 * Move some hierarchy of folders and files between projects. It also test moving a
	 * hierarchy across volumes.
	 */
	@Test
	public void testMoveHierarchyBetweenProjects() throws Exception {
		// create common objects
		IProject[] projects = getWorkspace().getRoot().getProjects();

		// create the source folder
		String folderSourceName = "source";
		IFolder folderSource = projects[0].getFolder(folderSourceName);
		ensureExistsInWorkspace(folderSource, true);

		// build hierarchy
		String[] hierarchy = defineHierarchy();
		IResource[] resources = buildResources(folderSource, hierarchy);
		ensureExistsInWorkspace(resources, true);

		// add some properties to each resource
		String[] propNames = new String[numberOfProperties];
		String[] propValues = new String[numberOfProperties];
		for (int j = 0; j < numberOfProperties; j++) {
			propNames[j] = "prop" + j;
			propValues[j] = "value" + j;
			for (IResource resource : resources) {
				QualifiedName propName = new QualifiedName("test", resource.getName() + propNames[j]);
				String propValue = resource.getName() + propValues[j];
				resource.setPersistentProperty(propName, propValue);
				resource.setSessionProperty(propName, propValue);
			}
		}

		// create the destination folder
		String folderDestinationName = "destination";
		IFolder folderDestination = projects[1].getFolder(folderDestinationName);

		// move hierarchy
		folderSource.move(folderDestination.getFullPath(), true, null);

		// get new hierarchy instance
		IResource[] newResources = buildResources(folderDestination, hierarchy);

		// assert hierarchy was moved
		assertDoesNotExistInWorkspace(resources);
		assertDoesNotExistInFileSystem(resources);
		assertExistsInWorkspace(newResources);
		assertExistsInFileSystem(newResources);

		// assert properties still exist (server, local and session)
		for (int j = 0; j < numberOfProperties; j++) {
			for (int i = 0; i < newResources.length; i++) {
				IResource destResource = newResources[i];
				IResource sourceResource = resources[i];
				/* The names of the properties will remain the same in both the source and
				 destination hierarchies.  So, be sure to use sourceResource to get the
				 name or your qualified name will contain 'destination' instead of
				 'source' and the property value will be null.
				 */
				QualifiedName propName = new QualifiedName("test", sourceResource.getName() + propNames[j]);
				String propValue = sourceResource.getName() + propValues[j];
				String persistentValue = destResource.getPersistentProperty(propName);
				Object sessionValue = destResource.getSessionProperty(propName);
				assertTrue("persistent property value is not the same", propValue.equals(persistentValue));
				assertTrue("session property value is not the same", propValue.equals(sessionValue));
			}
		}
	}

	@Test
	public void testMoveResource() throws Exception {
		/* create common objects */
		IProject[] projects = getWorkspace().getRoot().getProjects();

		/* create folder and file */
		IFolder folder = projects[0].getFolder("folder");
		IFile file = folder.getFile("file.txt");
		ensureExistsInWorkspace(folder, true);
		ensureExistsInWorkspace(file, true);

		/* move to absolute destination */
		IResource destination = projects[0].getFile("file.txt");
		file.move(destination.getFullPath(), true, null);
		assertTrue("1.1", !file.exists());
		assertTrue("1.2", destination.exists());
		destination.move(file.getFullPath(), true, null);
		assertTrue("1.3", file.exists());
		assertTrue("1.4", !destination.exists());

		/* move to relative destination */
		IPath path = IPath.fromOSString("destination");
		destination = folder.getFile(path);
		file.move(path, true, null);
		assertTrue("2.1", !file.exists());
		assertTrue("2.2", destination.exists());
		destination.move(file.getFullPath(), true, null);
		assertTrue("2.3", file.exists());
		assertTrue("2.4", !destination.exists());

		/* move folder to destination under its hierarchy */
		destination = folder.getFolder("subfolder");
		boolean ok = false;
		try {
			folder.move(destination.getFullPath(), true, null);
		} catch (RuntimeException e) {
			ok = true;
		}
		assertTrue("3.1", ok);

		/* test flag force = false */
		projects[0].refreshLocal(IResource.DEPTH_INFINITE, null);
		IFolder subfolder = folder.getFolder("aaa");
		ensureExistsInFileSystem(subfolder);
		IFile anotherFile = folder.getFile("bbb");
		ensureExistsInFileSystem(anotherFile);
		destination = projects[0].getFolder("destination");
		ok = false;
		try {
			folder.move(destination.getFullPath(), false, null);
		} catch (CoreException e) {
			ok = true;
			// FIXME: remove this check?
			//		assertTrue("4.1", e.getStatus().getChildren().length == 2);
		}
		assertTrue("4.2", ok);
		try {
			folder.move(destination.getFullPath(), false, null);
			fail("4.2.1");
		} catch (CoreException e) {
			// expected
		}
		assertTrue("4.3", folder.exists());
		// FIXME: should #move be a best effort operation?
		// its ok for the root to be moved but ensure the destination child wasn't moved
		IResource destChild = ((IContainer) destination).getFile(IPath.fromOSString(anotherFile.getName()));
		assertTrue("4.4", !destination.exists());
		assertTrue("4.5", !destChild.exists());
		// cleanup and delete the destination
		try {
			destination.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		} catch (CoreException e) {
			fail("4.6", e);
		}
		try {
			destination.delete(true, getMonitor());
		} catch (CoreException e) {
			fail("4.7", e);
		}

		folder.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		try {
			folder.move(destination.getFullPath(), false, getMonitor());
		} catch (CoreException e) {
			fail("4.8");
		}

		destination.move(folder.getFullPath(), true, null);
		assertTrue("4.9", folder.exists());
		assertTrue("4.10", !destination.exists());

		/* move a file that is not local but exists in the workspace */
		file = projects[0].getFile("ghost");
		final IFile hackFile = file;
		final Workspace workspace = (Workspace) getWorkspace();
		IWorkspaceRunnable operation = monitor -> workspace.createResource(hackFile, false);
		workspace.run(operation, null);
		destination = projects[0].getFile("destination");
		ok = false;
		try {
			file.move(destination.getFullPath(), true, null);
		} catch (CoreException e) {
			ok = true;
		}
		assertTrue("5.1", ok);

		/* move file over a phantom */
		assertTrue("6.1", file.exists());
		operation = monitor -> ((Resource) hackFile).convertToPhantom();
		workspace.run(operation, null);
		assertTrue("6.2", !file.exists());
		ResourceInfo info = ((File) file).getResourceInfo(true, false);
		int flags = ((File) file).getFlags(info);
		assertTrue("6.3", ((Resource) file).exists(flags, true));
		anotherFile = folder.getFile("anotherFile");
		ensureExistsInWorkspace(anotherFile, true);
		anotherFile.move(file.getFullPath(), true, null);
		assertTrue("6.4", file.exists());
	}

	/**
	 * A simple test that renames a file.
	 */
	@Test
	public void testRenameFile() throws Exception {
		// create common objects
		IProject[] projects = getWorkspace().getRoot().getProjects();

		// create a folder
		String fileName = "file.txt";
		IFile file = projects[0].getFile(fileName);
		ensureExistsInWorkspace(file, true);

		// add some properties to file (persistent and session)
		QualifiedName[] propNames = new QualifiedName[numberOfProperties];
		String[] propValues = new String[numberOfProperties];
		for (int j = 0; j < numberOfProperties; j++) {
			propNames[j] = new QualifiedName("test", "prop" + j);
			propValues[j] = "value" + j;
			file.setPersistentProperty(propNames[j], propValues[j]);
			file.setSessionProperty(propNames[j], propValues[j]);
		}

		// rename file
		String newFileName = "newFile.txt";
		IPath destination = projects[0].getFile(newFileName).getFullPath();
		file.move(destination, true, null);

		// get new folder instance
		IFile newFile = projects[0].getFile(newFileName);

		// assert file was renamed
		assertDoesNotExistInWorkspace(file);
		assertDoesNotExistInFileSystem(file);
		assertExistsInWorkspace(newFile);
		assertExistsInFileSystem(newFile);

		// assert properties still exist (server, local and session)
		for (int j = 0; j < numberOfProperties; j++) {
			String persistentValue = newFile.getPersistentProperty(propNames[j]);
			Object sessionValue = newFile.getSessionProperty(propNames[j]);
			assertTrue("persistent property value is not the same", propValues[j].equals(persistentValue));
			assertTrue("session property value is not the same", propValues[j].equals(sessionValue));
		}
	}

	/**
	 * A simple test that renames a folder.
	 *
	 * - creates a folder
	 * - set properties (server, local and session)
	 * - rename folder
	 * - assert rename worked
	 * - assert properties still exist
	 */
	@Test
	public void testRenameFolder() throws Exception {
		// create common objects
		IProject[] projects = getWorkspace().getRoot().getProjects();

		// create a folder
		String folderName = "folder";
		IFolder folder = projects[0].getFolder(folderName);
		ensureExistsInWorkspace(folder, true);

		// add some properties to folder (persistent and session)
		QualifiedName[] propNames = new QualifiedName[numberOfProperties];
		String[] propValues = new String[numberOfProperties];
		for (int j = 0; j < numberOfProperties; j++) {
			propNames[j] = new QualifiedName("test", "prop" + j);
			propValues[j] = "value" + j;
			folder.setPersistentProperty(propNames[j], propValues[j]);
			folder.setSessionProperty(propNames[j], propValues[j]);
		}

		// rename folder
		String newFolderName = "newFolder";
		IPath destination = projects[0].getFolder(newFolderName).getFullPath();
		folder.move(destination, true, null);

		// get new folder instance
		IFolder newFolder = projects[0].getFolder(newFolderName);

		// assert folder was renamed
		assertDoesNotExistInWorkspace(folder);
		assertDoesNotExistInFileSystem(folder);
		assertExistsInWorkspace(newFolder);
		assertExistsInFileSystem(newFolder);

		// assert properties still exist (server, local and session)
		for (int j = 0; j < numberOfProperties; j++) {
			String persistentValue = newFolder.getPersistentProperty(propNames[j]);
			Object sessionValue = newFolder.getSessionProperty(propNames[j]);
			assertTrue("persistent property value is not the same", propValues[j].equals(persistentValue));
			assertTrue("session property value is not the same", propValues[j].equals(sessionValue));
		}
	}

	/**
	 * Renames 3 projects using their names.
	 *
	 *	- add properties to projects (server, local and session)
	 *	- rename projects
	 *	- assert properties are correct
	 *	- assert resources are correct
	 */
	@Test
	public void testRenameProjects() throws Exception {
		/* create common objects */
		IProject[] projects = getWorkspace().getRoot().getProjects();

		// add some properties to projects (persistent and session)
		numberOfProperties = numberOfProjects;
		QualifiedName[] propNames = new QualifiedName[numberOfProperties];
		String[] propValues = new String[numberOfProperties];
		for (int i = 0; i < numberOfProjects; i++) {
			propNames[i] = new QualifiedName("test", "prop" + i);
			propValues[i] = "value" + i;
			projects[i].setPersistentProperty(propNames[i], propValues[i]);
			projects[i].setSessionProperty(propNames[i], propValues[i]);
		}

		// assert properties exist (persistent and session)
		for (int i = 0; i < numberOfProjects; i++) {
			String persistentValue = projects[i].getPersistentProperty(propNames[i]);
			Object sessionValue = projects[i].getSessionProperty(propNames[i]);
			assertTrue("1.0." + i, propValues[i].equals(persistentValue));
			assertTrue("1.1." + i, propValues[i].equals(sessionValue));
		}

		// move (rename) projects
		String prefix = "Renamed_PrOjEcT";
		for (int i = 0; i < numberOfProjects; i++) {
			String projectName = prefix + i;
			IPath destination = getWorkspace().getRoot().getProject(projectName).getFullPath();
			projects[i].move(destination, true, null);
			projectNames[i] = projectName;
		}

		// get new projects instances
		for (int i = 0; i < numberOfProjects; i++) {
			projects[i] = getWorkspace().getRoot().getProject(projectNames[i]);
		}

		// assert properties still exist (persistent and session)
		for (int i = 0; i < numberOfProjects; i++) {
			String persistentValue = projects[i].getPersistentProperty(propNames[i]);
			Object sessionValue = projects[i].getSessionProperty(propNames[i]);
			assertTrue("2.0." + i, propValues[i].equals(persistentValue));
			assertTrue("2.1." + i, propValues[i].equals(sessionValue));
		}
	}
}
