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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/**
 * Tests the public API of IResourceDelta
 */
public class IResourceDeltaTest extends ResourceTest {
	/* some random resource handles */
	protected IProject project1;
	protected IProject project2;
	protected IFolder folder1;//below project2
	protected IFolder folder2;//below folder1
	protected IFolder folder3;//same as file1
	protected IFile file1;//below folder1
	protected IFile file2;//below folder1
	protected IFile file3;//below folder2
	protected IFile file4;//below folder1
	protected IResource[] allResources;

	/**
	 * Sets up the fixture, for example, open a network connection.
	 * This method is called before a test is executed.
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();

		// Create some resource handles
		project1 = getWorkspace().getRoot().getProject("Project" + 1);
		project2 = getWorkspace().getRoot().getProject("Project" + 2);
		folder1 = project1.getFolder("Folder" + 1);
		folder2 = folder1.getFolder("Folder" + 2);
		folder3 = folder1.getFolder("Folder" + 3);
		file1 = folder1.getFile("File" + 1);
		file2 = folder1.getFile("File" + 2);
		file3 = folder2.getFile("File" + 3);
		file4 = folder1.getFile("File" + 4);//doesn't exist initially
		allResources = new IResource[] {project1, project2, folder1, folder2, folder3, file1, file2, file3};

		// Create and open the resources
		IWorkspaceRunnable body = monitor -> ensureExistsInWorkspace(allResources, true);
		try {
			getWorkspace().run(body, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}
	}

	/**
	 * Tests the IResourceDelta#findMember method.
	 */
	public void testFindMember() {
		/*
		 * The following changes will occur:
		 * - change file1
		 * - delete folder2 (which deletes file3)
		 * - add file4 below folder1
		 */
		IResourceChangeListener listener = event -> {
			//delta relative to root
			IResourceDelta delta = event.getDelta();
			assertNotNull("1.0", delta.findMember(project1.getFullPath()));
			assertNotNull("1.1", delta.findMember(file1.getFullPath()));
			assertNotNull("1.2", delta.findMember(folder2.getFullPath()));
			assertNotNull("1.3", delta.findMember(file3.getFullPath()));
			assertNotNull("1.4", delta.findMember(file4.getFullPath()));
			assertNull("1.5", delta.findMember(project2.getFullPath()));
			assertNull("1.6", delta.findMember(file2.getFullPath()));
			assertNull("1.7", delta.findMember(folder3.getFullPath()));

			//delta relative to project
			delta = delta.findMember(project1.getFullPath());
			assertNotNull("2.1", delta.findMember(file1.getProjectRelativePath()));
			assertNotNull("2.2", delta.findMember(folder2.getProjectRelativePath()));
			assertNotNull("2.3", delta.findMember(file3.getProjectRelativePath()));
			assertNotNull("2.4", delta.findMember(file4.getProjectRelativePath()));
			assertNull("2.5", delta.findMember(project2.getFullPath()));
			assertNull("2.6", delta.findMember(file2.getProjectRelativePath()));
			assertNull("2.7", delta.findMember(folder3.getProjectRelativePath()));
			assertNull("2.8", delta.findMember(project1.getFullPath()));
			assertNull("2.9", delta.findMember(file1.getFullPath()));

			//delta with no children
			delta = delta.findMember(file1.getProjectRelativePath());
			assertEquals("3.1", delta, delta.findMember(IPath.ROOT));
			assertNull("3.2", delta.findMember(IPath.fromOSString("foo")));
		};
		getWorkspace().addResourceChangeListener(listener);

		//do the work
		IWorkspaceRunnable body = monitor -> {
			file1.setContents(getRandomContents(), true, true, getMonitor());
			folder2.delete(true, getMonitor());
			file4.create(getRandomContents(), true, getMonitor());
		};
		try {
			getWorkspace().run(body, getMonitor());
		} catch (CoreException e) {
			fail("Exception1", e);
		} finally {
			getWorkspace().removeResourceChangeListener(listener);
		}
	}
}
