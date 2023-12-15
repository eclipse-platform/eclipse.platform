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

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertDoesNotExistInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createUniqueString;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Platform.OS;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests regression of bug 44106. In this case deleting a file which was a
 * symbolic link on Linux actually deleted the files that it pointed too rather
 * than just the link itself.
 *
 * Also tests bug 174492, which is a similar bug except the KEEP_HISTORY
 * flag is used when the resource is deleted from the workspace.
 */
public class Bug_044106 {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

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
		IFileStore linkDestFile = workspaceRule.getTempStore();
		createInFileSystem(linkDestFile);
		assertTrue("0.1", linkDestFile.fetchInfo().exists());

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
		assertTrue("4.1", linkDestFile.fetchInfo().exists());
	}

	/**
	 * Tests the various permutations of the bug
	 * @param deleteParent if true, the link's parent is deleted, otherwise the link
	 * is deleted
	 * @param deleteFlags The flags to use on the resource deletion call
	 */
	private void doTestDeleteLinkedFolder(IFolder linkedFolder, boolean deleteParent, int deleteFlags)
			throws Exception {
		assumeTrue("only relevant on Linux", OS.isLinux());

		IFileStore linkDestLocation = workspaceRule.getTempStore();
		IFileStore linkDestFile = linkDestLocation.getChild(createUniqueString());
		createInFileSystem(linkDestFile);
		assertTrue("0.1", linkDestLocation.fetchInfo().exists());
		assertTrue("0.2", linkDestFile.fetchInfo().exists());

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
		assertTrue("4.2", linkDestLocation.fetchInfo().exists());
		assertTrue("4.3", linkDestFile.fetchInfo().exists());
	}

	@Test
	public void testDeleteLinkedFile() throws Exception {
		assumeTrue("only relevant on Linux", OS.isLinux());

		doTestDeleteLinkedFile(IResource.NONE);
	}

	@Test
	public void testDeleteLinkedFolder() throws Exception {
		assumeTrue("only relevant on Linux", OS.isLinux());

		IProject project = getWorkspace().getRoot().getProject(createUniqueString());
		IFolder linkedFolder = project.getFolder("linkedFolder");
		doTestDeleteLinkedFolder(linkedFolder, false, IResource.NONE);
	}

	@Test
	public void testDeleteLinkedResourceInProject() throws Exception {
		assumeTrue("only relevant on Linux", OS.isLinux());

		IProject project = getWorkspace().getRoot().getProject(createUniqueString());
		IFolder linkedFolder = project.getFolder("linkedFolder");
		doTestDeleteLinkedFolder(linkedFolder, true, IResource.NONE);
	}

	@Test
	public void testDeleteLinkedFileKeepHistory() throws Exception {
		assumeTrue("only relevant on Linux", OS.isLinux());

		doTestDeleteLinkedFile(IResource.KEEP_HISTORY);
	}

	@Test
	public void testDeleteLinkedFolderParentKeepHistory() throws Exception {
		assumeTrue("only relevant on Linux", OS.isLinux());

		IProject project = getWorkspace().getRoot().getProject(createUniqueString());
		IFolder parent = project.getFolder("parent");
		IFolder linkedFolder = parent.getFolder("linkedFolder");
		doTestDeleteLinkedFolder(linkedFolder, true, IResource.KEEP_HISTORY);
	}

	@Test
	public void testDeleteLinkedFolderKeepHistory() throws Exception {
		assumeTrue("only relevant on Linux", OS.isLinux());

		IProject project = getWorkspace().getRoot().getProject(createUniqueString());
		IFolder linkedFolder = project.getFolder("linkedFolder");
		doTestDeleteLinkedFolder(linkedFolder, false, IResource.KEEP_HISTORY);
	}

	@Test
	public void testDeleteLinkedResourceInProjectKeepHistory() throws Exception {
		assumeTrue("only relevant on Linux", OS.isLinux());

		IProject project = getWorkspace().getRoot().getProject(createUniqueString());
		IFolder linkedFolder = project.getFolder("linkedFolder");
		doTestDeleteLinkedFolder(linkedFolder, true, IResource.KEEP_HISTORY);
	}

}
