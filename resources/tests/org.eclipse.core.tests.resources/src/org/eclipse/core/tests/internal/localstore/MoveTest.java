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
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertDoesNotExistInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertDoesNotExistInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.junit.Assert.assertThrows;

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
import org.eclipse.core.runtime.Platform.OS;
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

	/**
	 * This test has Windows as the target OS. Drives C: and D: should be available.
	 */
	@Test
	public void testMoveFileAcrossVolumes() throws CoreException {
		Assume.assumeTrue(OS.isWindows());

		/* look for the adequate environment */
		String[] devices = findAvailableDevices();
		Assume.assumeFalse(devices[0] == null || devices[1] == null);

		// create common objects
		String location = getUniqueString();
		IProject source = getWorkspace().getRoot().getProject(location + "1");
		IProject destination = getWorkspace().getRoot().getProject(location + "2");
		source.create(createTestMonitor());
		source.open(createTestMonitor());

		IProjectDescription description = getWorkspace().newProjectDescription(destination.getName());
		description.setLocation(IPath.fromOSString(devices[1] + location));
		destination.create(description, createTestMonitor());
		destination.open(createTestMonitor());

		String fileName = "fileToBeMoved.txt";
		IFile file = source.getFile(fileName);
		file.create(getRandomContents(), true, createTestMonitor());

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
		IPath dest = destination.getFile(fileName).getFullPath();
		file.move(dest, true, createTestMonitor());

		// assert file was moved
		IFile newFile = destination.getFile(fileName);
		assertDoesNotExistInWorkspace(file);
		assertDoesNotExistInFileSystem(file);
		assertExistsInWorkspace(newFile);
		assertExistsInFileSystem(newFile);

		// assert properties still exist (server, local and session)
		for (int j = 0; j < numberOfProperties; j++) {
			String persistentValue = newFile.getPersistentProperty(propNames[j]);
			Object sessionValue = newFile.getSessionProperty(propNames[j]);
			assertEquals("5.1", persistentValue, propValues[j]);
			assertEquals("5.2", sessionValue, propValues[j]);
		}
	}

	/**
	 * Move one file from one project to another.
	 */
	@Test
	public void testMoveFileBetweenProjects() throws Exception {
		// create common objects
		IProject[] testProjects = getWorkspace().getRoot().getProjects();

		// get file instance
		String fileName = "newFile.txt";
		IFile file = testProjects[0].getFile(fileName);
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
		IPath destination = testProjects[1].getFile(fileName).getFullPath();
		file.move(destination, true, null);

		// get new file instance
		IFile newFile = testProjects[1].getFile(fileName);

		// assert file was renamed
		assertDoesNotExistInWorkspace(file);
		assertDoesNotExistInFileSystem(file);
		assertExistsInWorkspace(newFile);
		assertExistsInFileSystem(newFile);

		// assert properties still exist (server, local and session)
		for (int j = 0; j < numberOfProperties; j++) {
			String persistentValue = newFile.getPersistentProperty(propNames[j]);
			Object sessionValue = newFile.getSessionProperty(propNames[j]);
			assertEquals("persistent property value is not the same", propValues[j], persistentValue);
			assertEquals("session property value is not the same", propValues[j], sessionValue);
		}
	}

	/**
	 * This test has Windows as the target OS. Drives C: and D: should be available.
	 */
	@Test
	public void testMoveFolderAcrossVolumes() throws CoreException {
		Assume.assumeTrue(OS.isWindows());

		/* look for the adequate environment */
		String[] devices = findAvailableDevices();
		Assume.assumeFalse(devices[0] == null || devices[1] == null);

		// create common objects
		String location = getUniqueString();
		IProject source = getWorkspace().getRoot().getProject(location + "1");
		IProject destination = getWorkspace().getRoot().getProject(location + "2");
		source.create(createTestMonitor());
		source.open(createTestMonitor());

		IProjectDescription description = getWorkspace().newProjectDescription(destination.getName());
		description.setLocation(IPath.fromOSString(devices[1] + location));
		destination.create(description, createTestMonitor());
		destination.open(createTestMonitor());

		// get folder instance
		String folderName = "folderToBeMoved";
		IFolder folder = source.getFolder(folderName);
		folder.create(true, true, createTestMonitor());

		// add some properties to file (persistent and session)
		QualifiedName[] propNames = new QualifiedName[numberOfProperties];
		String[] propValues = new String[numberOfProperties];
		for (int j = 0; j < numberOfProperties; j++) {
			propNames[j] = new QualifiedName("test", "prop" + j);
			propValues[j] = "value" + j;
			folder.setPersistentProperty(propNames[j], propValues[j]);
			folder.setSessionProperty(propNames[j], propValues[j]);
		}

		// rename folder
		IPath dest = destination.getFile(folderName).getFullPath();
		folder.move(dest, true, createTestMonitor());

		// assert folder was renamed
		IFolder newFolder = destination.getFolder(folderName);
		assertDoesNotExistInWorkspace(folder);
		assertDoesNotExistInFileSystem(folder);
		assertExistsInWorkspace(newFolder);
		assertExistsInFileSystem(newFolder);

		// assert properties still exist (server, local and session)
		for (int j = 0; j < numberOfProperties; j++) {
			String persistentValue = newFolder.getPersistentProperty(propNames[j]);
			Object sessionValue = newFolder.getSessionProperty(propNames[j]);
			assertEquals(persistentValue, propValues[j]);
			assertEquals(sessionValue, propValues[j]);
		}
	}

	/**
	 * Move one folder from one project to another.
	 */
	@Test
	public void testMoveFolderBetweenProjects() throws Exception {
		// create common objects
		IProject[] testProjects = getWorkspace().getRoot().getProjects();

		// get folder instance
		String folderName = "newFolder";
		IFolder folder = testProjects[0].getFolder(folderName);
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
		IPath destination = testProjects[1].getFolder(folderName).getFullPath();
		folder.move(destination, true, null);

		// get new folder instance
		IFolder newFolder = testProjects[1].getFolder(folderName);

		// assert folder was renamed
		assertDoesNotExistInWorkspace(folder);
		assertDoesNotExistInFileSystem(folder);
		assertExistsInWorkspace(newFolder);
		assertExistsInFileSystem(newFolder);

		// assert properties still exist (server, local and session)
		for (int j = 0; j < numberOfProperties; j++) {
			String persistentValue = newFolder.getPersistentProperty(propNames[j]);
			Object sessionValue = newFolder.getSessionProperty(propNames[j]);
			assertEquals("persistent property value is not the same", propValues[j], persistentValue);
			assertEquals("session property value is not the same", propValues[j], sessionValue);
		}
	}

	/**
	 * Move some hierarchy of folders and files.
	 */
	@Test
	public void testMoveHierarchy() throws Exception {
		// create common objects
		IProject[] testPprojects = getWorkspace().getRoot().getProjects();

		// create the source folder
		String folderSourceName = "folder source";
		IFolder folderSource = testPprojects[0].getFolder(folderSourceName);
		ensureExistsInWorkspace(folderSource, true);

		// create hierarchy
		String[] hierarchy = new String[] { "/", "/file1", "/file2", "/folder1/", "/folder1/file3",
				"/folder1/file4", "/folder2/", "/folder2/file5", "/folder2/file6", "/folder1/folder3/",
				"/folder1/folder3/file7", "/folder1/folder3/file8" };
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
		IFolder folderDestination = testPprojects[0].getFolder(folderDestinationName);

		// move hierarchy
		//IProgressMonitor monitor = new LoggingProgressMonitor(System.out);
		IProgressMonitor monitor = createTestMonitor();
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
				assertEquals("persistent property value is not the same", propValue, persistentValue);
				assertEquals("session property value is not the same", propValue, sessionValue);
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
		IProject[] testProjects = getWorkspace().getRoot().getProjects();

		// create the source folder
		String folderSourceName = "source";
		IFolder folderSource = testProjects[0].getFolder(folderSourceName);
		ensureExistsInWorkspace(folderSource, true);

		// build hierarchy
		String[] hierarchy = new String[] { "/", "/file1", "/file2", "/folder1/", "/folder1/file3", "/folder1/file4",
				"/folder2/", "/folder2/file5", "/folder2/file6", "/folder1/folder3/", "/folder1/folder3/file7",
				"/folder1/folder3/file8" };
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
		IFolder folderDestination = testProjects[1].getFolder(folderDestinationName);

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
				assertEquals("persistent property value is not the same", propValue, persistentValue);
				assertEquals("session property value is not the same", propValue, sessionValue);
			}
		}
	}

	@Test
	public void testMoveResource() throws Exception {
		/* create common objects */
		IProject[] testProjects = getWorkspace().getRoot().getProjects();

		/* create folder and file */
		IFolder folder = projects[0].getFolder("folder");
		IFile file = folder.getFile("file.txt");
		ensureExistsInWorkspace(folder, true);
		ensureExistsInWorkspace(file, true);

		/* move to absolute destination */
		IResource destination = testProjects[0].getFile("file.txt");
		file.move(destination.getFullPath(), true, null);
		assertFalse(file.exists());
		assertTrue(destination.exists());
		destination.move(file.getFullPath(), true, null);
		assertTrue(file.exists());
		assertFalse(destination.exists());

		/* move to relative destination */
		IPath path = IPath.fromOSString("destination");
		destination = folder.getFile(path);
		file.move(path, true, null);
		assertFalse(file.exists());
		assertTrue(destination.exists());
		destination.move(file.getFullPath(), true, null);
		assertTrue(file.exists());
		assertFalse(destination.exists());

		/* move folder to destination under its hierarchy */
		IFolder subFolderDestination = folder.getFolder("subfolder");
		assertThrows(RuntimeException.class, () -> folder.move(subFolderDestination.getFullPath(), true, null));

		/* test flag force = false */
		testProjects[0].refreshLocal(IResource.DEPTH_INFINITE, null);
		IFolder subfolder = folder.getFolder("aaa");
		ensureExistsInFileSystem(subfolder);
		IFile anotherFile = folder.getFile("bbb");
		ensureExistsInFileSystem(anotherFile);
		IFolder folderDestination = testProjects[0].getFolder("destination");
		assertThrows(CoreException.class, () -> folder.move(folderDestination.getFullPath(), false, null));
		assertThrows(CoreException.class, () -> folder.move(folderDestination.getFullPath(), false, null));
		assertTrue(folder.exists());
		// FIXME: should #move be a best effort operation?
		// its ok for the root to be moved but ensure the destination child wasn't moved
		IResource destChild = ((IContainer) folderDestination).getFile(IPath.fromOSString(anotherFile.getName()));
		assertFalse(folderDestination.exists());
		assertFalse(destChild.exists());
		// cleanup and delete the destination
		folderDestination.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		folderDestination.delete(true, createTestMonitor());

		folder.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		folder.move(folderDestination.getFullPath(), false, createTestMonitor());

		folderDestination.move(folder.getFullPath(), true, null);
		assertTrue(folder.exists());
		assertFalse(destination.exists());

		/* move a file that is not local but exists in the workspace */
		IFile ghostFile = testProjects[0].getFile("ghost");
		final IFile hackFile = ghostFile;
		final Workspace workspace = (Workspace) getWorkspace();
		IWorkspaceRunnable operation = monitor -> workspace.createResource(hackFile, false);
		workspace.run(operation, null);
		IFile fileDestination = testProjects[0].getFile("destination");
		assertThrows(CoreException.class, () -> ghostFile.move(fileDestination.getFullPath(), true, null));

		/* move file over a phantom */
		assertTrue(ghostFile.exists());
		operation = monitor -> ((Resource) hackFile).convertToPhantom();
		workspace.run(operation, null);
		assertFalse(ghostFile.exists());
		ResourceInfo info = ((File) ghostFile).getResourceInfo(true, false);
		int flags = ((File) ghostFile).getFlags(info);
		assertTrue(((Resource) ghostFile).exists(flags, true));
		anotherFile = folder.getFile("anotherFile");
		ensureExistsInWorkspace(anotherFile, true);
		anotherFile.move(ghostFile.getFullPath(), true, null);
		assertTrue(ghostFile.exists());
	}

	/**
	 * A simple test that renames a file.
	 */
	@Test
	public void testRenameFile() throws Exception {
		// create common objects
		IProject[] testProjects = getWorkspace().getRoot().getProjects();

		// create a folder
		String fileName = "file.txt";
		IFile file = testProjects[0].getFile(fileName);
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
		IPath destination = testProjects[0].getFile(newFileName).getFullPath();
		file.move(destination, true, null);

		// get new folder instance
		IFile newFile = testProjects[0].getFile(newFileName);

		// assert file was renamed
		assertDoesNotExistInWorkspace(file);
		assertDoesNotExistInFileSystem(file);
		assertExistsInWorkspace(newFile);
		assertExistsInFileSystem(newFile);

		// assert properties still exist (server, local and session)
		for (int j = 0; j < numberOfProperties; j++) {
			String persistentValue = newFile.getPersistentProperty(propNames[j]);
			Object sessionValue = newFile.getSessionProperty(propNames[j]);
			assertEquals("persistent property value is not the same", propValues[j], persistentValue);
			assertEquals("session property value is not the same", propValues[j], sessionValue);
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
		IProject[] testProjects = getWorkspace().getRoot().getProjects();

		// create a folder
		String folderName = "folder";
		IFolder folder = testProjects[0].getFolder(folderName);
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
		IPath destination = testProjects[0].getFolder(newFolderName).getFullPath();
		folder.move(destination, true, null);

		// get new folder instance
		IFolder newFolder = testProjects[0].getFolder(newFolderName);

		// assert folder was renamed
		assertDoesNotExistInWorkspace(folder);
		assertDoesNotExistInFileSystem(folder);
		assertExistsInWorkspace(newFolder);
		assertExistsInFileSystem(newFolder);

		// assert properties still exist (server, local and session)
		for (int j = 0; j < numberOfProperties; j++) {
			String persistentValue = newFolder.getPersistentProperty(propNames[j]);
			Object sessionValue = newFolder.getSessionProperty(propNames[j]);
			assertEquals("persistent property value is not the same", propValues[j], persistentValue);
			assertEquals("session property value is not the same", propValues[j], sessionValue);
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
		IProject[] testProjects = getWorkspace().getRoot().getProjects();

		// add some properties to projects (persistent and session)
		numberOfProperties = numberOfProjects;
		QualifiedName[] propNames = new QualifiedName[numberOfProperties];
		String[] propValues = new String[numberOfProperties];
		for (int i = 0; i < numberOfProjects; i++) {
			propNames[i] = new QualifiedName("test", "prop" + i);
			propValues[i] = "value" + i;
			testProjects[i].setPersistentProperty(propNames[i], propValues[i]);
			testProjects[i].setSessionProperty(propNames[i], propValues[i]);
		}

		// assert properties exist (persistent and session)
		for (int i = 0; i < numberOfProjects; i++) {
			String persistentValue = testProjects[i].getPersistentProperty(propNames[i]);
			Object sessionValue = testProjects[i].getSessionProperty(propNames[i]);
			assertEquals("persistent property value is not the same", propValues[i], persistentValue);
			assertEquals("session property value is not the same", propValues[i], sessionValue);
		}

		// move (rename) projects
		String prefix = "Renamed_PrOjEcT";
		for (int i = 0; i < numberOfProjects; i++) {
			String projectName = prefix + i;
			IPath destination = getWorkspace().getRoot().getProject(projectName).getFullPath();
			testProjects[i].move(destination, true, null);
			projectNames[i] = projectName;
		}

		// get new projects instances
		for (int i = 0; i < numberOfProjects; i++) {
			testProjects[i] = getWorkspace().getRoot().getProject(projectNames[i]);
		}

		// assert properties still exist (persistent and session)
		for (int i = 0; i < numberOfProjects; i++) {
			String persistentValue = testProjects[i].getPersistentProperty(propNames[i]);
			Object sessionValue = testProjects[i].getSessionProperty(propNames[i]);
			assertEquals("persistent property value is not the same", propValues[i], persistentValue);
			assertEquals("session property value is not the same", propValues[i], sessionValue);
		}
	}
}
