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

import java.io.IOException;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

public class CaseSensitivityTest extends LocalStoreTest {
	private final boolean isCaseSensitive = Workspace.caseSensitive;

	public void testCreateProjects() {
		String projectName = "testProject31415";

		// create a project, should be fine
		IProject project1 = getWorkspace().getRoot().getProject(projectName);
		try {
			project1.create(null);
			project1.open(null);
		} catch (CoreException e) {
			fail("1.0", e);
		}

		// create a second project; should fail because has same name with different casing
		IProject project2 = getWorkspace().getRoot().getProject(projectName.toUpperCase());
		try {
			project2.create(null);
			project2.open(null);
			assertTrue("2.0", isCaseSensitive);
		} catch (CoreException e) {
			if (isCaseSensitive) {
				fail("2.1", e);
			}
		}
	}

	public void testCreateFolders() {
		String folderName = "testFolder31415";
		IProject aProject = getWorkspace().getRoot().getProjects()[0];

		// create a folder, should be fine
		IFolder folder1 = aProject.getFolder(folderName);
		try {
			folder1.create(true, true, null);
		} catch (CoreException e) {
			fail("1.0", e);
		}

		// create a second folder; should fail because has same name with different casing
		IFolder folder2 = aProject.getFolder(folderName.toUpperCase());
		try {
			folder2.create(true, true, null);
			assertTrue("2.0", isCaseSensitive);
		} catch (CoreException e) {
			if (isCaseSensitive) {
				fail("2.1", e);
			}
		}

		// create a file; should fail because has same name with different casing
		IFile file = aProject.getFile(folderName.toUpperCase());
		try {
			file.create(getRandomContents(), true, null);
			fail("3.0");
		} catch (CoreException e) {
			// expected
		}
	}

	public void testCreateFiles() {
		String fileName = "testFile31415";
		IProject aProject = getWorkspace().getRoot().getProjects()[0];

		// create a file, should be fine
		IFile file1 = aProject.getFile(fileName);
		try {
			file1.create(getRandomContents(), true, null);
		} catch (CoreException e) {
			fail("1.0", e);
		}

		// create a second file; should fail because has same name with different casing
		IFile file2 = aProject.getFile(fileName.toUpperCase());
		try {
			file2.create(getRandomContents(), true, null);
			assertTrue("2.0", isCaseSensitive);
		} catch (CoreException e) {
			if (isCaseSensitive) {
				fail("2.1", e);
			}
		}

		// create a folder; should fail because has same name with different casing
		IFolder folder = aProject.getFolder(fileName.toUpperCase());
		try {
			folder.create(true, true, null);
			fail("3.0");
		} catch (CoreException e) {
			// expected
		}
	}

	public void testRenameProject() {
		String project1name = "project1test31415";
		String project2name = "project2test31415";

		// create 2 projects with different names
		IProject project1 = getWorkspace().getRoot().getProject(project1name);
		try {
			project1.create(null);
			project1.open(null);
		} catch (CoreException e) {
			fail("1.0", e);
		}

		IProject project2 = getWorkspace().getRoot().getProject(project2name);
		try {
			project2.create(null);
			project2.open(null);
		} catch (CoreException e) {
			fail("2.0", e);
		}

		// try to rename project 1 to the uppercase name of project 2, should fail
		try {
			project1.move(IPath.ROOT.append(project2.getName().toUpperCase()), true, null);
			assertTrue("3.0", isCaseSensitive);
		} catch (CoreException e) {
			if (isCaseSensitive) {
				fail("3.99", e);
			}
		}
	}

	public void testRenameFolder() {
		String folder1name = "folder1test31415";
		String folder2name = "folder2test31415";
		IProject aProject = getWorkspace().getRoot().getProjects()[0];

		// create 2 folders with different names
		IFolder folder1 = aProject.getFolder(folder1name);
		try {
			folder1.create(true, true, null);
		} catch (CoreException e) {
			fail("1.1", e);
		}

		IFolder folder2 = aProject.getFolder(folder2name);
		try {
			folder2.create(true, true, null);
		} catch (CoreException e) {
			fail("2.2", e);
		}

		// try to rename folder 1 to the uppercase name of folder 2, should fail
		IFolder folder3 = aProject.getFolder(folder2name.toUpperCase());
		try {
			folder1.move(folder3.getFullPath(), true, null);
			assertTrue("3.1", isCaseSensitive);
		} catch (CoreException e) {
			if (isCaseSensitive) {
				fail("3.2", e);
			}
		}
	}

	public void testRenameFile() {
		String file1name = "file1test31415";
		String file2name = "file2test31415";
		IProject aProject = getWorkspace().getRoot().getProjects()[0];

		// create 2 files with different names
		IFile file1 = aProject.getFile(file1name);
		try {
			file1.create(getRandomContents(), true, null);
		} catch (CoreException e) {
			fail("1.1", e);
		}

		IFile file2 = aProject.getFile(file2name);
		try {
			file2.create(getRandomContents(), true, null);
		} catch (CoreException e) {
			fail("2.2", e);
		}

		// try to rename folder 1 to the uppercase name of folder 2, should fail
		IFile file3 = aProject.getFile(file2name.toUpperCase());
		try {
			file1.move(file3.getFullPath(), true, null);
			assertTrue("3.1", isCaseSensitive);
		} catch (CoreException e) {
			if (isCaseSensitive) {
				fail("3.2", e);
			}
		}
	}

	public void testCopyAndMoveFolder() {
		String folderName = "folderTest31415";
		IProject sourceProject = getWorkspace().getRoot().getProjects()[0];
		IProject destinationProject = getWorkspace().getRoot().getProjects()[1];

		// create 2 folders, one in each project, with case-different names
		IFolder folder1 = sourceProject.getFolder(folderName);
		try {
			folder1.create(true, true, null);
		} catch (CoreException e) {
			fail("1.0", e);
		}

		IFolder folder2 = destinationProject.getFolder(folderName.toUpperCase());
		try {
			folder2.create(true, true, null);
		} catch (CoreException e) {
			fail("1.1", e);
		}

		// try to copy the folder from source project to destination project.
		// should fail due to conflict
		try {
			folder1.copy(destinationProject.getFullPath().append(folder1.getName()), true, null);
			assertTrue("1.2.1", isCaseSensitive);
		} catch (CoreException e) {
			assertTrue("1.2.2." + e.getMessage(), !isCaseSensitive);
		}

		// try to move the folder from source project to destination project.
		// should fail due to conflict
		try {
			folder1.move(destinationProject.getFullPath().append(folder1.getName()), true, null);
			fail("1.3");
		} catch (CoreException e) {
			// expected
		}
	}

	public void testCopyAndMoveFile() {
		String fileName = "fileTest31415";
		IProject sourceProject = getWorkspace().getRoot().getProjects()[0];
		IProject destinationProject = getWorkspace().getRoot().getProjects()[1];

		// create 2 files, one in each project, with case-different names
		IFile file1 = sourceProject.getFile(fileName);
		try {
			file1.create(getRandomContents(), true, null);
		} catch (CoreException e) {
			fail("1.0", e);
		}

		IFile file2 = destinationProject.getFile(fileName.toUpperCase());
		try {
			file2.create(getRandomContents(), true, null);
		} catch (CoreException e) {
			fail("1.1", e);
		}

		// try to copy the file from source project to destination project.
		// should fail due to conflict
		try {
			file1.copy(destinationProject.getFullPath().append(file1.getName()), true, null);
			assertTrue("1.2.1", isCaseSensitive);
		} catch (CoreException e) {
			if (isCaseSensitive) {
				fail("1.2.2", e);
			}
		}

		// try to move the file from source project to destination project.
		// should fail due to conflict
		try {
			file1.move(destinationProject.getFullPath().append(file1.getName()), true, null);
			fail("1.3");
		} catch (CoreException e) {
			// expected
		}
	}

	public void testCopyAndMoveFolderOverFile() {
		String name = "test31415";
		IProject sourceProject = getWorkspace().getRoot().getProjects()[0];
		IProject destinationProject = getWorkspace().getRoot().getProjects()[1];

		// create 2 resources, one in each project, with case-different names
		IFolder folder = sourceProject.getFolder(name);
		try {
			folder.create(true, true, null);
		} catch (CoreException e) {
			fail("1.0", e);
		}

		IFile file = destinationProject.getFile(name.toUpperCase());
		try {
			file.create(getRandomContents(), true, null);
		} catch (CoreException e) {
			fail("1.1", e);
		}

		// try to copy the folder from source project to destination project.
		// should fail due to conflict with existing file with case-different name
		try {
			folder.copy(destinationProject.getFullPath().append(folder.getName()), true, null);
			assertTrue("1.2.1", isCaseSensitive);
		} catch (CoreException e) {
			assertTrue("1.2.2." + e.getMessage(), !isCaseSensitive);
		}

		// try to move the folder from source project to destination project.
		// should fail due to conflict with existing file with case-different name
		try {
			folder.move(destinationProject.getFullPath().append(folder.getName()), true, null);
			fail("1.3");
		} catch (CoreException e) {
			// expected
		}
	}

	public void testCopyAndMoveFileOverFolder() {
		String name = "test31415";
		IProject sourceProject = getWorkspace().getRoot().getProjects()[0];
		IProject destinationProject = getWorkspace().getRoot().getProjects()[1];

		// create 2 resources, one in each project, with case-different names
		IFile file = sourceProject.getFile(name);
		try {
			file.create(getRandomContents(), true, null);
		} catch (CoreException e) {
			fail("1.0", e);
		}

		IFolder folder = destinationProject.getFolder(name.toUpperCase());
		try {
			folder.create(true, true, null);
		} catch (CoreException e) {
			fail("1.1", e);
		}

		// try to copy the file from source project to destination project.
		// should fail due to conflict with existing folder with case-different name
		try {
			file.copy(destinationProject.getFullPath().append(file.getName()), true, null);
			assertTrue("1.2.1", isCaseSensitive);
		} catch (CoreException e) {
			assertTrue("1.2.2." + e.getMessage(), !isCaseSensitive);
		}

		// try to move the file from source project to destination project.
		// should fail due to conflict with existing folder with case-different name
		try {
			file.move(destinationProject.getFullPath().append(file.getName()), true, null);
			assertTrue("1.3", false);
		} catch (CoreException e) {
			// expected
		}
	}

	public void testCopyAndMoveFolderBecomeProject() {
		IProject sourceProject = getWorkspace().getRoot().getProjects()[0];
		IProject blockingProject = getWorkspace().getRoot().getProjects()[1];

		// create a folder in the source project with a case-different name to the second project
		IFolder folder = sourceProject.getFolder(blockingProject.getName().toUpperCase());
		try {
			folder.create(true, true, null);
		} catch (CoreException e) {
			fail("1.0", e);
		}

		// try to move the folder from source project to the root, which makes it a project.
		// should always fails since we aren't allowed to move a folder to be a project.
		try {
			folder.move(IPath.ROOT.append(folder.getName()), true, null);
			fail("1.1");
		} catch (CoreException e) {
			// expected
		}

		// try to copy the folder from source project to the root, which makes it a project.
		// should always fail since we aren't allowed to copy a folder to be a project
		try {
			folder.copy(IPath.ROOT.append(folder.getName()), true, null);
			fail("1.2");
		} catch (CoreException e) {
			// expected
		}
	}

	public void testCopyAndMoveProjectBecomeFolder() {
		IProject sourceProject = getWorkspace().getRoot().getProjects()[0];
		IProject destinationProject = getWorkspace().getRoot().getProjects()[1];

		// create a file in the destination project with a case-different name of the source project
		IFile file = destinationProject.getFile(sourceProject.getName().toUpperCase());
		try {
			file.create(getRandomContents(), true, null);
		} catch (CoreException e) {
			fail("1.0", e);
		}

		// try to move the source project to the destination project, which makes it a folder.
		// should fail because we aren't allowed to move a project to be a folder
		try {
			sourceProject.move(destinationProject.getFullPath().append(sourceProject.getName()), true, null);
			fail("1.1");
		} catch (CoreException e) {
			// expected
		}

		// try to copy the source project to the destination project, which makes it a folder.
		// should fail because we aren't allowed to copy a project to be a folder
		try {
			sourceProject.copy(destinationProject.getFullPath().append(sourceProject.getName()), true, null);
			fail("1.2");
		} catch (CoreException e) {
			// expected
		}
	}

	public void testRefreshLocalFolder1() {
		String name = "test31415";
		IProject project = getWorkspace().getRoot().getProjects()[0];

		// create a Folder, which should be fine
		IFolder folder = project.getFolder(name.toUpperCase());
		try {
			folder.create(true, true, null);
		} catch (CoreException e) {
			fail("1.1", e);
		}

		// get a Folder handle with the same name but different casing
		// in order to determine file system location
		IFolder herringRouge = project.getFolder(name);

		// create a directory with the folder's name
		folder.getLocation().toFile().delete();
		java.io.File dir = herringRouge.getLocation().toFile();
		dir.mkdir();

		// do a refresh, which should cause a problem
		try {
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			fail("1.2", e);
		}

		assertTrue("2.0", !folder.exists());
		assertTrue("2.1", herringRouge.exists());
	}

	public void testRefreshLocalFile1() {
		String name = "test31415";
		IProject project = getWorkspace().getRoot().getProjects()[0];

		// create a File, which should be fine
		IFile file = project.getFile(name.toUpperCase());
		try {
			file.create(getRandomContents(), true, null);
		} catch (CoreException e) {
			fail("1.1", e);
		}

		// get a File handle with the same name but different casing
		// in order to determine file system location
		IFile herringRouge = project.getFile(name);

		// create a file in the local file system with the same name but different casing
		ensureDoesNotExistInFileSystem(file);
		ensureExistsInFileSystem(herringRouge);

		// do a refresh, which should cause a problem
		try {
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			fail("3.0", e);
		}

		assertTrue("4.0", !file.exists());
		assertTrue("4.1", herringRouge.exists());
	}

	public void testRefreshLocalFolder2() {
		String name = "test31415";
		IProject project = getWorkspace().getRoot().getProjects()[0];

		// create a Folder, which should be fine
		IFolder folder = project.getFolder(name.toUpperCase());
		try {
			folder.create(true, true, null);
		} catch (CoreException e) {
			fail("1.1", e);
		}

		// get a File handle with the same name but different casing
		// in order to determine file system location
		IFile herringRouge = project.getFile(name);

		// create a file in the local file system with the same name but different casing
		ensureDoesNotExistInFileSystem(folder);
		ensureExistsInFileSystem(herringRouge);

		// do a refresh, which should cause a problem
		try {
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			fail("3.0", e);
		}

		assertTrue("4.0", !folder.exists());
		assertTrue("4.1", herringRouge.exists());
	}

	public void testRefreshLocalFile2() {
		String name = "test31415";
		IProject project = getWorkspace().getRoot().getProjects()[0];

		// create a File, which should be fine
		IFile file = project.getFile(name.toUpperCase());
		try {
			file.create(getRandomContents(), true, null);
		} catch (CoreException e) {
			fail("1.1", e);
		}

		// get a Folder handle with the same name but different casing
		// in order to determine file system location
		IFolder herringRouge = project.getFolder(name);

		// create a directory with the folder's name
		java.io.File localFile = file.getLocation().toFile();
		localFile.delete();
		assertTrue("2.0", !localFile.exists());
		java.io.File dir = herringRouge.getLocation().toFile();
		dir.mkdir();
		assertTrue("2.1", dir.exists());

		// do a refresh, which should cause a problem
		try {
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			fail("3.0", e);
		}

		assertTrue("4.0", !file.exists());
		assertTrue("4.1", herringRouge.exists());
	}

	public void testDeleteResources() {
		String name = "test31415";

		// create a project, should be fine
		IProject project = getWorkspace().getRoot().getProject(name);
		try {
			project.create(null);
			project.open(null);
		} catch (CoreException e) {
			fail("1.1", e);
		}

		// create a Folder, which should be fine
		IFolder folder = project.getFolder(name);
		try {
			folder.create(true, true, null);
		} catch (CoreException e) {
			fail("2.1", e);
		}

		// create a File, which should be fine
		IFile file = folder.getFile(name);
		try {
			file.create(getRandomContents(), true, null);
		} catch (CoreException e) {
			fail("3.1", e);
		}

		// replace the File's filesystem rep. with a case-different name
		java.io.File localFile = file.getLocation().toFile();
		localFile.delete();
		assertTrue("4.0", !localFile.exists());
		localFile = new java.io.File(file.getLocation().removeLastSegments(1).toString(), name.toUpperCase());
		try {
			localFile.createNewFile();
		} catch (IOException e) {
			fail("4.1", e);
		}
		assertTrue("4.2", localFile.exists());

		try {
			file.delete(true, null);
		} catch (CoreException e) {
			fail("5.0", e);
		}

		localFile.delete(); // so that we can change its parent folder
		assertTrue("6.0", !localFile.exists());

		// replace the Folder's filesystem rep. with a case-different name
		java.io.File localFolder = folder.getLocation().toFile();
		localFolder.delete();
		assertTrue("7.0", !localFolder.exists());
		localFolder = new java.io.File(folder.getLocation().removeLastSegments(1).toString(), name.toUpperCase());
		localFolder.mkdir();
		assertTrue("7.1", localFolder.exists());

		try {
			folder.delete(true, null);
		} catch (CoreException e) {
			fail("8.0", e);
		}
	}
}
