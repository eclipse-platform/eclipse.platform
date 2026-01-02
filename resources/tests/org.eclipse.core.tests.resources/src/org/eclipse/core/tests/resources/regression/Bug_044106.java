/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.regression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertDoesNotExistInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createUniqueString;
import static org.eclipse.core.tests.resources.ResourceTestUtil.getFileStore;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Predicate;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Platform.OS;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests regression of bug 44106. In this case deleting a file which was a
 * symbolic link on Linux actually deleted the files that it pointed too rather
 * than just the link itself.
 *
 * Also tests bug 174492, which is a similar bug except the KEEP_HISTORY
 * flag is used when the resource is deleted from the workspace.
 */
@ExtendWith(WorkspaceResetExtension.class)
public class Bug_044106 {

	private static final Predicate<IFileStore> exists = store -> store.fetchInfo().exists();

	private @TempDir Path tempDirectory;

	private void createSymLink(String target, String local) throws InterruptedException, IOException {
		Process p = Runtime.getRuntime().exec(new String[] { "/bin/ln", "-s", target, local });
		p.waitFor();
	}

	/**
	 * Tests various permutations of the bug.
	 * @param deleteFlags The option flags to use when deleting the resource.
	 */
	private void doTestDeleteLinkedFile(int deleteFlags) throws Exception {
		// create the file/folder that we are going to link to
		IFileStore linkDestFile = getFileStore(tempDirectory).getChild(createUniqueString());
		createInFileSystem(linkDestFile);
		assertThat(linkDestFile).matches(exists, "exists");

		// create some resources in the workspace
		IProject project = getWorkspace().getRoot().getProject(createUniqueString());
		createInWorkspace(project);

		// link in the folder
		String target = new java.io.File(linkDestFile.toURI()).getAbsolutePath();
		IFile linkedFile = project.getFile("linkedFile");
		String local = linkedFile.getLocation().toOSString();
		createSymLink(target, local);
		assertExistsInFileSystem(linkedFile);

		// do a refresh and ensure that the resources are in the workspace
		project.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		assertExistsInWorkspace(linkedFile);

		// delete the file
		linkedFile.delete(deleteFlags, createTestMonitor());

		// ensure that the folder and file weren't deleted in the filesystem
		assertDoesNotExistInWorkspace(linkedFile);
		assertThat(linkDestFile).matches(exists, "exists");
	}

	/**
	 * Tests the various permutations of the bug
	 * @param deleteParent if true, the link's parent is deleted, otherwise the link
	 * is deleted
	 * @param deleteFlags The flags to use on the resource deletion call
	 */
	private void doTestDeleteLinkedFolder(IFolder linkedFolder, boolean deleteParent, int deleteFlags)
			throws Exception {
		assumeTrue(OS.isLinux(), "only relevant on Linux");

		IFileStore linkDestLocation = getFileStore(tempDirectory);
		IFileStore linkDestFile = linkDestLocation.getChild(createUniqueString());
		createInFileSystem(linkDestFile);
		assertThat(linkDestLocation).matches(exists, "exists");
		assertThat(linkDestFile).matches(exists, "exists");

		// create some resources in the workspace
		createInWorkspace(linkedFolder.getParent());

		// link in the folder
		String target = new java.io.File(linkDestLocation.toURI()).getAbsolutePath();
		IFile linkedFile = linkedFolder.getFile(linkDestFile.getName());
		String local = linkedFolder.getLocation().toOSString();
		createSymLink(target, local);
		assertExistsInFileSystem(linkedFolder);
		assertExistsInFileSystem(linkedFile);

		// do a refresh and ensure that the resources are in the workspace
		linkedFolder.getProject().refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		assertExistsInWorkspace(linkedFolder);
		assertExistsInWorkspace(linkedFile);

		// delete the folder or project
		if (deleteParent) {
			linkedFolder.getParent().delete(deleteFlags, createTestMonitor());
		} else {
			linkedFolder.delete(deleteFlags, createTestMonitor());
		}

		// ensure that the folder and file weren't deleted in the filesystem
		assertDoesNotExistInWorkspace(linkedFolder);
		assertDoesNotExistInWorkspace(linkedFile);
		assertThat(linkDestLocation).matches(exists, "exists");
		assertThat(linkDestFile).matches(exists, "exists");
	}

	@Test
	public void testDeleteLinkedFile() throws Exception {
		assumeTrue(OS.isLinux(), "only relevant on Linux");

		doTestDeleteLinkedFile(IResource.NONE);
	}

	@Test
	public void testDeleteLinkedFolder() throws Exception {
		assumeTrue(OS.isLinux(), "only relevant on Linux");

		IProject project = getWorkspace().getRoot().getProject(createUniqueString());
		IFolder linkedFolder = project.getFolder("linkedFolder");
		doTestDeleteLinkedFolder(linkedFolder, false, IResource.NONE);
	}

	@Test
	public void testDeleteLinkedResourceInProject() throws Exception {
		assumeTrue(OS.isLinux(), "only relevant on Linux");

		IProject project = getWorkspace().getRoot().getProject(createUniqueString());
		IFolder linkedFolder = project.getFolder("linkedFolder");
		doTestDeleteLinkedFolder(linkedFolder, true, IResource.NONE);
	}

	@Test
	public void testDeleteLinkedFileKeepHistory() throws Exception {
		assumeTrue(OS.isLinux(), "only relevant on Linux");

		doTestDeleteLinkedFile(IResource.KEEP_HISTORY);
	}

	@Test
	public void testDeleteLinkedFolderParentKeepHistory() throws Exception {
		assumeTrue(OS.isLinux(), "only relevant on Linux");

		IProject project = getWorkspace().getRoot().getProject(createUniqueString());
		IFolder parent = project.getFolder("parent");
		IFolder linkedFolder = parent.getFolder("linkedFolder");
		doTestDeleteLinkedFolder(linkedFolder, true, IResource.KEEP_HISTORY);
	}

	@Test
	public void testDeleteLinkedFolderKeepHistory() throws Exception {
		assumeTrue(OS.isLinux(), "only relevant on Linux");

		IProject project = getWorkspace().getRoot().getProject(createUniqueString());
		IFolder linkedFolder = project.getFolder("linkedFolder");
		doTestDeleteLinkedFolder(linkedFolder, false, IResource.KEEP_HISTORY);
	}

	@Test
	public void testDeleteLinkedResourceInProjectKeepHistory() throws Exception {
		assumeTrue(OS.isLinux(), "only relevant on Linux");

		IProject project = getWorkspace().getRoot().getProject(createUniqueString());
		IFolder linkedFolder = project.getFolder("linkedFolder");
		doTestDeleteLinkedFolder(linkedFolder, true, IResource.KEEP_HISTORY);
	}

}
