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
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomContentsStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.removeFromFileSystem;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

public class CaseSensitivityTest {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	private final boolean isCaseSensitive = Workspace.caseSensitive;

	@Test
	public void testCreateProjects() throws Throwable {
		String projectName = "testProject31415";

		// create a project, should be fine
		IProject project1 = getWorkspace().getRoot().getProject(projectName);
		project1.create(null);
		project1.open(null);

		// create a second project; should fail because has same name with different casing
		IProject project2 = getWorkspace().getRoot().getProject(projectName.toUpperCase());
		ThrowingRunnable projectCreation = () -> {
			project2.create(null);
			project2.open(null);
		};
		if (isCaseSensitive) {
			projectCreation.run();
		} else {
			assertThrows(CoreException.class, projectCreation);
		}
	}

	@Test
	public void testCreateFolders() throws Throwable {
		IProject project = getWorkspace().getRoot().getProject("Project");
		createInWorkspace(project);

		String folderName = "testFolder31415";
		// create a folder, should be fine
		IFolder folder1 = project.getFolder(folderName);
		folder1.create(true, true, null);

		// create a second folder; should fail because has same name with different casing
		IFolder folder2 = project.getFolder(folderName.toUpperCase());
		ThrowingRunnable folderCreation = () -> folder2.create(true, true, null);
		if (isCaseSensitive) {
			folderCreation.run();
		} else {
			assertThrows(CoreException.class, folderCreation);
		}

		// create a file; should fail because has same name with different casing
		IFile file = project.getFile(folderName.toUpperCase());
		assertThrows(CoreException.class, () -> file.create(createRandomContentsStream(), true, null));
	}

	@Test
	public void testCreateFiles() throws Throwable {
		IProject project = getWorkspace().getRoot().getProject("Project");
		createInWorkspace(project);

		String fileName = "testFile31415";
		// create a file, should be fine
		IFile file1 = project.getFile(fileName);
		file1.create(createRandomContentsStream(), true, null);

		// create a second file; should fail because has same name with different casing
		IFile file2 = project.getFile(fileName.toUpperCase());
		ThrowingRunnable fileCreation = () -> file2.create(createRandomContentsStream(), true, null);
		if (isCaseSensitive) {
			fileCreation.run();
		} else {
			assertThrows(CoreException.class, fileCreation);
		}

		// create a folder; should fail because has same name with different casing
		IFolder folder = project.getFolder(fileName.toUpperCase());
		assertThrows(CoreException.class, () -> folder.create(true, true, null));
	}

	@Test
	public void testRenameProject() throws Throwable {
		String project1name = "project1test31415";
		String project2name = "project2test31415";

		// create 2 projects with different names
		IProject project1 = getWorkspace().getRoot().getProject(project1name);
		project1.create(null);
		project1.open(null);

		IProject project2 = getWorkspace().getRoot().getProject(project2name);
		project2.create(null);
		project2.open(null);

		// try to rename project 1 to the uppercase name of project 2, should fail
		ThrowingRunnable projectMovement = () -> project1.move(IPath.ROOT.append(project2.getName().toUpperCase()),
				true, null);
		if (isCaseSensitive) {
			projectMovement.run();
		} else {
			assertThrows(CoreException.class, projectMovement);
		}
	}

	@Test
	public void testRenameFolder() throws Throwable {
		IProject project = getWorkspace().getRoot().getProject("Project");
		createInWorkspace(project);

		String folder1name = "folder1test31415";
		String folder2name = "folder2test31415";

		// create 2 folders with different names
		IFolder folder1 = project.getFolder(folder1name);
		folder1.create(true, true, null);

		IFolder folder2 = project.getFolder(folder2name);
		folder2.create(true, true, null);

		// try to rename folder 1 to the uppercase name of folder 2, should fail
		IFolder folder3 = project.getFolder(folder2name.toUpperCase());
		ThrowingRunnable folderMovement = () -> folder1.move(folder3.getFullPath(), true, null);
		if (isCaseSensitive) {
			folderMovement.run();
		} else {
			assertThrows(CoreException.class, folderMovement);
		}
	}

	@Test
	public void testRenameFile() throws Throwable {
		IProject project = getWorkspace().getRoot().getProject("Project");
		createInWorkspace(project);

		String file1name = "file1test31415";
		String file2name = "file2test31415";

		// create 2 files with different names
		IFile file1 = project.getFile(file1name);
		file1.create(createRandomContentsStream(), true, null);

		IFile file2 = project.getFile(file2name);
		file2.create(createRandomContentsStream(), true, null);

		// try to rename folder 1 to the uppercase name of folder 2, should fail
		IFile file3 = project.getFile(file2name.toUpperCase());
		ThrowingRunnable fileMovement = () -> file1.move(file3.getFullPath(), true, null);
		if (isCaseSensitive) {
			fileMovement.run();
		} else {
			assertThrows(CoreException.class, fileMovement);
		}
	}

	@Test
	public void testCopyAndMoveFolder() throws Throwable {
		IProject sourceProject = getWorkspace().getRoot().getProject("Source");
		createInWorkspace(sourceProject);
		IProject destinationProject = getWorkspace().getRoot().getProject("Target");
		createInWorkspace(destinationProject);

		String folderName = "folderTest31415";
		// create 2 folders, one in each project, with case-different names
		IFolder folder1 = sourceProject.getFolder(folderName);
		folder1.create(true, true, null);

		IFolder folder2 = destinationProject.getFolder(folderName.toUpperCase());
		folder2.create(true, true, null);

		// try to copy the folder from source project to destination project.
		// should fail due to conflict
		ThrowingRunnable folderCopy = () -> folder1.copy(destinationProject.getFullPath().append(folder1.getName()),
				true, null);
		if (isCaseSensitive) {
			folderCopy.run();
		} else {
			assertThrows(CoreException.class, folderCopy);
		}

		// try to move the folder from source project to destination project.
		// should fail due to conflict
		assertThrows(CoreException.class,
				() -> folder1.move(destinationProject.getFullPath().append(folder1.getName()), true, null));
	}

	@Test
	public void testCopyAndMoveFile() throws Throwable {
		IProject sourceProject = getWorkspace().getRoot().getProject("Source");
		createInWorkspace(sourceProject);
		IProject destinationProject = getWorkspace().getRoot().getProject("Target");
		createInWorkspace(destinationProject);

		String fileName = "fileTest31415";
		// create 2 files, one in each project, with case-different names
		IFile file1 = sourceProject.getFile(fileName);
		file1.create(createRandomContentsStream(), true, null);

		IFile file2 = destinationProject.getFile(fileName.toUpperCase());
		file2.create(createRandomContentsStream(), true, null);

		// try to copy the file from source project to destination project.
		// should fail due to conflict
		ThrowingRunnable fileCopy = () -> file1.copy(destinationProject.getFullPath().append(file1.getName()), true,
				null);
		if (isCaseSensitive) {
			fileCopy.run();
		} else {
			assertThrows(CoreException.class, fileCopy);
		}

		// try to move the file from source project to destination project.
		// should fail due to conflict
		assertThrows(CoreException.class,
				() -> file1.move(destinationProject.getFullPath().append(file1.getName()), true, null));
	}

	@Test
	public void testCopyAndMoveFolderOverFile() throws Throwable {
		IProject sourceProject = getWorkspace().getRoot().getProject("Source");
		createInWorkspace(sourceProject);
		IProject destinationProject = getWorkspace().getRoot().getProject("Target");
		createInWorkspace(destinationProject);

		String name = "test31415";
		// create 2 resources, one in each project, with case-different names
		IFolder folder = sourceProject.getFolder(name);
		folder.create(true, true, null);

		IFile file = destinationProject.getFile(name.toUpperCase());
		file.create(createRandomContentsStream(), true, null);

		// try to copy the folder from source project to destination project.
		// should fail due to conflict with existing file with case-different name
		ThrowingRunnable folderCopy = () -> folder.copy(destinationProject.getFullPath().append(folder.getName()), true,
				null);
		if (isCaseSensitive) {
			folderCopy.run();
		} else {
			assertThrows(CoreException.class, folderCopy);
		}

		// try to move the folder from source project to destination project.
		// should fail due to conflict with existing file with case-different name
		assertThrows(CoreException.class,
				() -> folder.move(destinationProject.getFullPath().append(folder.getName()), true, null));
	}

	@Test
	public void testCopyAndMoveFileOverFolder() throws Throwable {
		IProject sourceProject = getWorkspace().getRoot().getProject("Source");
		createInWorkspace(sourceProject);
		IProject destinationProject = getWorkspace().getRoot().getProject("Target");
		createInWorkspace(destinationProject);

		String name = "test31415";
		// create 2 resources, one in each project, with case-different names
		IFile file = sourceProject.getFile(name);
		file.create(createRandomContentsStream(), true, null);

		IFolder folder = destinationProject.getFolder(name.toUpperCase());
		folder.create(true, true, null);

		// try to copy the file from source project to destination project.
		// should fail due to conflict with existing folder with case-different name
		ThrowingRunnable fileCopy = () -> file.copy(destinationProject.getFullPath().append(file.getName()), true,
				null);
		if (isCaseSensitive) {
			fileCopy.run();
		} else {
			assertThrows(CoreException.class, fileCopy);
		}

		// try to move the file from source project to destination project.
		// should fail due to conflict with existing folder with case-different name
		assertThrows(CoreException.class,
				() -> file.move(destinationProject.getFullPath().append(file.getName()), true, null));
	}

	@Test
	public void testCopyAndMoveFolderBecomeProject() throws CoreException {
		IProject sourceProject = getWorkspace().getRoot().getProject("Source");
		createInWorkspace(sourceProject);
		IProject blockingProject = getWorkspace().getRoot().getProject("Blocking");
		createInWorkspace(blockingProject);

		// create a folder in the source project with a case-different name to the second project
		IFolder folder = sourceProject.getFolder(blockingProject.getName().toUpperCase());
		folder.create(true, true, null);

		// try to move the folder from source project to the root, which makes it a project.
		// should always fails since we aren't allowed to move a folder to be a project.
		assertThrows(CoreException.class, () -> folder.move(IPath.ROOT.append(folder.getName()), true, null));

		// try to copy the folder from source project to the root, which makes it a project.
		// should always fail since we aren't allowed to copy a folder to be a project
		assertThrows(CoreException.class, () -> folder.copy(IPath.ROOT.append(folder.getName()), true, null));
	}

	@Test
	public void testCopyAndMoveProjectBecomeFolder() throws CoreException {
		IProject sourceProject = getWorkspace().getRoot().getProject("Source");
		createInWorkspace(sourceProject);
		IProject destinationProject = getWorkspace().getRoot().getProject("Target");
		createInWorkspace(destinationProject);

		// create a file in the destination project with a case-different name of the source project
		IFile file = destinationProject.getFile(sourceProject.getName().toUpperCase());
		file.create(createRandomContentsStream(), true, null);

		// try to move the source project to the destination project, which makes it a folder.
		// should fail because we aren't allowed to move a project to be a folder
		assertThrows(CoreException.class,
				() -> sourceProject.move(destinationProject.getFullPath().append(sourceProject.getName()), true, null));

		// try to copy the source project to the destination project, which makes it a folder.
		// should fail because we aren't allowed to copy a project to be a folder
		assertThrows(CoreException.class,
				() -> sourceProject.copy(destinationProject.getFullPath().append(sourceProject.getName()), true, null));
	}

	@Test
	public void testRefreshLocalFolder1() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("Project");
		createInWorkspace(project);

		String name = "test31415";
		// create a Folder, which should be fine
		IFolder folder = project.getFolder(name.toUpperCase());
		folder.create(true, true, null);

		// get a Folder handle with the same name but different casing
		// in order to determine file system location
		IFolder herringRouge = project.getFolder(name);

		// create a directory with the folder's name
		folder.getLocation().toFile().delete();
		java.io.File dir = herringRouge.getLocation().toFile();
		dir.mkdir();

		// do a refresh, which should cause a problem
		project.refreshLocal(IResource.DEPTH_INFINITE, null);

		assertFalse(folder.exists());
		assertTrue(herringRouge.exists());
	}

	@Test
	public void testRefreshLocalFile1() throws Exception {
		IProject project = getWorkspace().getRoot().getProject("Project");
		createInWorkspace(project);

		String name = "test31415";
		// create a File, which should be fine
		IFile file = project.getFile(name.toUpperCase());
		file.create(createRandomContentsStream(), true, null);

		// get a File handle with the same name but different casing
		// in order to determine file system location
		IFile herringRouge = project.getFile(name);

		// create a file in the local file system with the same name but different casing
		removeFromFileSystem(file);
		createInFileSystem(herringRouge);

		// do a refresh, which should cause a problem
		project.refreshLocal(IResource.DEPTH_INFINITE, null);

		assertFalse(file.exists());
		assertTrue(herringRouge.exists());
	}

	@Test
	public void testRefreshLocalFolder2() throws Exception {
		IProject project = getWorkspace().getRoot().getProject("Project");
		createInWorkspace(project);

		String name = "test31415";
		// create a Folder, which should be fine
		IFolder folder = project.getFolder(name.toUpperCase());
		folder.create(true, true, null);

		// get a File handle with the same name but different casing
		// in order to determine file system location
		IFile herringRouge = project.getFile(name);

		// create a file in the local file system with the same name but different casing
		removeFromFileSystem(folder);
		createInFileSystem(herringRouge);

		// do a refresh, which should cause a problem
		project.refreshLocal(IResource.DEPTH_INFINITE, null);

		assertFalse(folder.exists());
		assertTrue(herringRouge.exists());
	}

	@Test
	public void testRefreshLocalFile2() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("Project");
		createInWorkspace(project);

		String name = "test31415";
		// create a File, which should be fine
		IFile file = project.getFile(name.toUpperCase());
		file.create(createRandomContentsStream(), true, null);

		// get a Folder handle with the same name but different casing
		// in order to determine file system location
		IFolder herringRouge = project.getFolder(name);

		// create a directory with the folder's name
		java.io.File localFile = file.getLocation().toFile();
		localFile.delete();
		assertFalse(localFile.exists());
		java.io.File dir = herringRouge.getLocation().toFile();
		dir.mkdir();
		assertTrue(dir.exists());

		// do a refresh, which should cause a problem
		project.refreshLocal(IResource.DEPTH_INFINITE, null);

		assertFalse(file.exists());
		assertTrue(herringRouge.exists());
	}

	@Test
	public void testDeleteResources() throws CoreException, IOException {
		String name = "test31415";

		// create a project, should be fine
		IProject project = getWorkspace().getRoot().getProject(name);
		project.create(null);
		project.open(null);

		// create a Folder, which should be fine
		IFolder folder = project.getFolder(name);
		folder.create(true, true, null);

		// create a File, which should be fine
		IFile file = folder.getFile(name);
		file.create(createRandomContentsStream(), true, null);

		// replace the File's filesystem rep. with a case-different name
		java.io.File localFile = file.getLocation().toFile();
		localFile.delete();
		assertTrue(!localFile.exists());
		localFile = new java.io.File(file.getLocation().removeLastSegments(1).toString(), name.toUpperCase());
		localFile.createNewFile();
		assertTrue(localFile.exists());

		file.delete(true, null);

		localFile.delete(); // so that we can change its parent folder
		assertFalse(localFile.exists());

		// replace the Folder's filesystem rep. with a case-different name
		java.io.File localFolder = folder.getLocation().toFile();
		localFolder.delete();
		assertFalse(localFolder.exists());
		localFolder = new java.io.File(folder.getLocation().removeLastSegments(1).toString(), name.toUpperCase());
		localFolder.mkdir();
		assertTrue(localFolder.exists());

		folder.delete(true, null);
	}

}
