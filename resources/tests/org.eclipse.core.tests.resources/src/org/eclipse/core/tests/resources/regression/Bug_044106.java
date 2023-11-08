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

import static org.eclipse.core.tests.resources.ResourceTestUtil.assertDoesNotExistInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createFile;
import static org.eclipse.core.tests.resources.ResourceTestUtil.ensureExistsInWorkspace;

import java.io.IOException;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Platform.OS;
import org.eclipse.core.tests.resources.ResourceTest;

/**
 * Tests regression of bug 44106. In this case deleting a file which was a
 * symbolic link on Linux actually deleted the files that it pointed too rather
 * than just the link itself.
 *
 * Also tests bug 174492, which is a similar bug except the KEEP_HISTORY
 * flag is used when the resource is deleted from the workspace.
 */
public class Bug_044106 extends ResourceTest {

	private void createSymLink(String target, String local) throws InterruptedException, IOException {
		Process p = Runtime.getRuntime().exec(new String[] { "/bin/ln", "-s", target, local });
		p.waitFor();
	}

	/**
	 * Tests various permutations of the bug.
	 * @param deleteFlags The option flags to use when deleting the resource.
	 */
	public void doTestDeleteLinkedFile(int deleteFlags) throws Exception {
		// create the file/folder that we are going to link to
		IFileStore linkDestFile = getTempStore();
		createFile(linkDestFile);
		assertTrue("0.1", linkDestFile.fetchInfo().exists());

		// create some resources in the workspace
		IProject project = getWorkspace().getRoot().getProject(getUniqueString());
		ensureExistsInWorkspace(project, true);

		// link in the folder
		String target = new java.io.File(linkDestFile.toURI()).getAbsolutePath();
		IFile linkedFile = project.getFile("linkedFile");
		String local = linkedFile.getLocation().toOSString();
		createSymLink(target, local);
		assertExistsInFileSystem("1.2", linkedFile);

		// do a refresh and ensure that the resources are in the workspace
		project.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		assertExistsInWorkspace("2.1", linkedFile);

		// delete the file
		linkedFile.delete(deleteFlags, getMonitor());

		// ensure that the folder and file weren't deleted in the filesystem
		assertDoesNotExistInWorkspace("4.0", linkedFile);
		assertTrue("4.1", linkDestFile.fetchInfo().exists());
	}

	/**
	 * Tests the various permutations of the bug
	 * @param deleteParent if true, the link's parent is deleted, otherwise the link
	 * is deleted
	 * @param deleteFlags The flags to use on the resource deletion call
	 */
	public void doTestDeleteLinkedFolder(IFolder linkedFolder, boolean deleteParent, int deleteFlags) throws Exception {
		if (!OS.isLinux()) {
			return;
		}
		IFileStore linkDestLocation = getTempStore();
		IFileStore linkDestFile = linkDestLocation.getChild(getUniqueString());
		createFile(linkDestFile);
		assertTrue("0.1", linkDestLocation.fetchInfo().exists());
		assertTrue("0.2", linkDestFile.fetchInfo().exists());

		// create some resources in the workspace
		ensureExistsInWorkspace(linkedFolder.getParent(), true);

		// link in the folder
		String target = new java.io.File(linkDestLocation.toURI()).getAbsolutePath();
		IFile linkedFile = linkedFolder.getFile(linkDestFile.getName());
		String local = linkedFolder.getLocation().toOSString();
		createSymLink(target, local);
		assertExistsInFileSystem("1.2", linkedFolder);
		assertExistsInFileSystem("1.3", linkedFile);

		// do a refresh and ensure that the resources are in the workspace
		linkedFolder.getProject().refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		assertExistsInWorkspace("2.1", linkedFolder);
		assertExistsInWorkspace("2.2", linkedFile);

		// delete the folder or project
		if (deleteParent) {
			linkedFolder.getParent().delete(deleteFlags, getMonitor());
		} else {
			linkedFolder.delete(deleteFlags, getMonitor());
		}

		// ensure that the folder and file weren't deleted in the filesystem
		assertDoesNotExistInWorkspace("4.0", linkedFolder);
		assertDoesNotExistInWorkspace("4.1", linkedFile);
		assertTrue("4.2", linkDestLocation.fetchInfo().exists());
		assertTrue("4.3", linkDestFile.fetchInfo().exists());
	}

	public void testDeleteLinkedFile() throws Exception {
		if (!OS.isLinux()) {
			return;
		}
		doTestDeleteLinkedFile(IResource.NONE);
	}

	public void testDeleteLinkedFolder() throws Exception {
		if (!OS.isLinux()) {
			return;
		}
		IProject project = getWorkspace().getRoot().getProject(getUniqueString());
		IFolder linkedFolder = project.getFolder("linkedFolder");
		doTestDeleteLinkedFolder(linkedFolder, false, IResource.NONE);
	}

	public void testDeleteLinkedResourceInProject() throws Exception {
		if (!OS.isLinux()) {
			return;
		}
		IProject project = getWorkspace().getRoot().getProject(getUniqueString());
		IFolder linkedFolder = project.getFolder("linkedFolder");
		doTestDeleteLinkedFolder(linkedFolder, true, IResource.NONE);
	}

	public void testDeleteLinkedFileKeepHistory() throws Exception {
		if (!OS.isLinux()) {
			return;
		}
		doTestDeleteLinkedFile(IResource.KEEP_HISTORY);
	}

	public void testDeleteLinkedFolderParentKeepHistory() throws Exception {
		if (!OS.isLinux()) {
			return;
		}
		IProject project = getWorkspace().getRoot().getProject(getUniqueString());
		IFolder parent = project.getFolder("parent");
		IFolder linkedFolder = parent.getFolder("linkedFolder");
		doTestDeleteLinkedFolder(linkedFolder, true, IResource.KEEP_HISTORY);
	}

	public void testDeleteLinkedFolderKeepHistory() throws Exception {
		if (!OS.isLinux()) {
			return;
		}
		IProject project = getWorkspace().getRoot().getProject(getUniqueString());
		IFolder linkedFolder = project.getFolder("linkedFolder");
		doTestDeleteLinkedFolder(linkedFolder, false, IResource.KEEP_HISTORY);
	}

	public void testDeleteLinkedResourceInProjectKeepHistory() throws Exception {
		if (!OS.isLinux()) {
			return;
		}
		IProject project = getWorkspace().getRoot().getProject(getUniqueString());
		IFolder linkedFolder = project.getFolder("linkedFolder");
		doTestDeleteLinkedFolder(linkedFolder, true, IResource.KEEP_HISTORY);
	}

}
