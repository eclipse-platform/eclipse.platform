/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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

import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.tests.resources.ResourceTest;

public class IWorkspaceTest extends ResourceTest {

	/**
	 * 1GDKIHD: ITPCORE:WINNT - API - IWorkspace.move needs to keep history
	 */
	public void testMultiMove_1GDKIHD() throws CoreException {
		// create common objects
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		project.create(getMonitor());
		project.open(getMonitor());

		// test file (force = true)
		IFile file1 = project.getFile("file.txt");
		IFolder folder = project.getFolder("folder");
		IResource[] allResources = new IResource[] {file1, folder};
		folder.create(true, true, getMonitor());
		file1.create(getRandomContents(), true, getMonitor());
		file1.setContents(getRandomContents(), true, true, getMonitor());
		file1.setContents(getRandomContents(), true, true, getMonitor());
		getWorkspace().move(new IFile[] { file1 }, folder.getFullPath(), true, getMonitor());
		file1.create(getRandomContents(), true, getMonitor());
		IFileState[] states = file1.getHistory(getMonitor());
		assertEquals("1.0", 3, states.length);
		getWorkspace().delete(allResources, true, getMonitor());
		project.clearHistory(getMonitor());

		// test file (force = false)
		folder.create(true, true, getMonitor());
		file1.create(getRandomContents(), true, getMonitor());
		file1.setContents(getRandomContents(), true, true, getMonitor());
		file1.setContents(getRandomContents(), true, true, getMonitor());
		getWorkspace().move(new IFile[] { file1 }, folder.getFullPath(), false, getMonitor());
		file1.create(getRandomContents(), true, getMonitor());
		states = file1.getHistory(getMonitor());
		assertEquals("2.0", 3, states.length);
		getWorkspace().delete(allResources, true, getMonitor());
		project.clearHistory(getMonitor());
	}

	/**
	 * 1GDGRIZ: ITPCORE:WINNT - API - IWorkspace.delete needs to keep history
	 */
	public void testMultiDelete_1GDGRIZ() throws CoreException {
		// create common objects
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		project.create(getMonitor());
		project.open(getMonitor());

		// test file (force = true)
		IFile file1 = project.getFile("file.txt");
		file1.create(getRandomContents(), true, getMonitor());
		file1.setContents(getRandomContents(), true, true, getMonitor());
		file1.setContents(getRandomContents(), true, true, getMonitor());
		getWorkspace().delete(new IFile[] { file1 }, true, getMonitor());
		file1.create(getRandomContents(), true, getMonitor());
		IFileState[] states = file1.getHistory(getMonitor());
		assertEquals("1.0", 3, states.length);
		getWorkspace().delete(new IResource[] { file1 }, true, getMonitor());
		project.clearHistory(getMonitor());

		// test file (force = false)
		file1.create(getRandomContents(), true, getMonitor());
		file1.setContents(getRandomContents(), true, true, getMonitor());
		file1.setContents(getRandomContents(), true, true, getMonitor());
		getWorkspace().delete(new IFile[] { file1 }, false, getMonitor());
		file1.create(getRandomContents(), true, getMonitor());
		states = file1.getHistory(getMonitor());
		assertEquals("2.0", 3, states.length);
		getWorkspace().delete(new IResource[] { file1 }, true, getMonitor());
		project.clearHistory(getMonitor());

		// test folder (force = true)
		IFolder folder = project.getFolder("folder");
		IFile file2 = folder.getFile("file2.txt");
		folder.create(true, true, getMonitor());
		file2.create(getRandomContents(), true, getMonitor());
		file2.setContents(getRandomContents(), true, true, getMonitor());
		file2.setContents(getRandomContents(), true, true, getMonitor());
		getWorkspace().delete(new IResource[] { folder }, true, getMonitor());
		folder.create(true, true, getMonitor());
		file2.create(getRandomContents(), true, getMonitor());
		states = file2.getHistory(getMonitor());
		assertEquals("3.0", 3, states.length);
		getWorkspace().delete(new IResource[] { folder, file1, file2 }, true, getMonitor());
		project.clearHistory(getMonitor());

		// test folder (force = false)
		folder.create(true, true, getMonitor());
		file2.create(getRandomContents(), true, getMonitor());
		file2.setContents(getRandomContents(), true, true, getMonitor());
		file2.setContents(getRandomContents(), true, true, getMonitor());
		getWorkspace().delete(new IResource[] { folder }, false, getMonitor());
		folder.create(true, true, getMonitor());
		file2.create(getRandomContents(), true, getMonitor());
		states = file2.getHistory(getMonitor());
		assertEquals("4.0", 3, states.length);
		getWorkspace().delete(new IResource[] { folder, file1, file2 }, true, getMonitor());
		project.clearHistory(getMonitor());
	}

	public void test_8974() throws CoreException {
		IProject one = getWorkspace().getRoot().getProject("One");
		IPath oneLocation = getRandomLocation().append(one.getName());
		oneLocation.toFile().mkdirs();
		deleteOnTearDown(oneLocation.removeLastSegments(1));
		IProjectDescription oneDescription = getWorkspace().newProjectDescription(one.getName());
		oneDescription.setLocation(oneLocation);

		one.create(oneDescription, getMonitor());

		IProject two = getWorkspace().getRoot().getProject("Two");
		IPath twoLocation = oneLocation.removeLastSegments(1).append(oneLocation.lastSegment().toLowerCase());

		IStatus result = getWorkspace().validateProjectLocation(two, twoLocation);
		assertEquals(Workspace.caseSensitive, result.isOK());
	}
}
