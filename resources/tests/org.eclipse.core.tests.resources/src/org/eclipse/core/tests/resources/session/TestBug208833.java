/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources.session;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomString;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.WorkspaceSessionTest;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

import junit.framework.Test;

/**
 * Tests regression of bug 208833 - project resource tree is deleted when Eclipse fails to access its metainfo
 * during startup. It results in empty resource tree when the project's metadata is accessible again and the project is open.
 */
public class TestBug208833 extends WorkspaceSessionTest {
	/**
	 * Setup.  Creates a project with a file.
	 */
	public void test1() throws CoreException {
		IWorkspace workspace = getWorkspace();

		IProject project = workspace.getRoot().getProject("Project1");
		IFile file = project.getFile("file1.txt");

		// create a project with a file
		createInWorkspace(project);
		createInWorkspace(file, createRandomString());

		// save the workspace
		workspace.save(true, null);

		// move the project to another location, before the workbench is started again
		// to emulate disconnection of a device (e.g. USB key) or a remote file system
		assertTrue(project.getLocation().toFile()
				.renameTo(new File(project.getLocation().toFile().getAbsolutePath() + "_temp")));
	}

	/**
	 * Eclipse started again.
	 */
	public void test2() throws CoreException {
		IWorkspace workspace = getWorkspace();

		IProject p1 = workspace.getRoot().getProject("Project1");

		// the project should exist, but closed
		assertTrue(p1.exists());
		assertFalse(p1.isOpen());

		// move the project back
		assertTrue(new File(p1.getLocation().toFile().getAbsolutePath() + "_temp").renameTo(p1.getLocation().toFile()));

		// now the project should be opened without any problems
		p1.open(null);

		// the project should be opened and the file should exist
		assertTrue(p1.isOpen());
		IFile file1 = p1.getFile("file1.txt");
		assertTrue(file1.exists());
	}

	public static Test suite() {
		return new WorkspaceSessionTestSuite(PI_RESOURCES_TESTS, TestBug208833.class);
	}
}
