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

import java.io.File;
import junit.framework.Test;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.AutomatedResourceTests;
import org.eclipse.core.tests.resources.WorkspaceSessionTest;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

/**
 * Tests regression of bug 208833 - project resource tree is deleted when Eclipse fails to access its metainfo
 * during startup. It results in empty resource tree when the project's metadata is accessible again and the project is open.
 */
public class TestBug208833 extends WorkspaceSessionTest {
	/**
	 * Setup.  Creates a project with a file.
	 */
	public void test1() {
		IWorkspace workspace = getWorkspace();

		IProject project = workspace.getRoot().getProject("Project1");
		IFile file = project.getFile("file1.txt");

		// create a project with a file
		ensureExistsInWorkspace(project, true);
		ensureExistsInWorkspace(file, getRandomContents());

		// save the workspace
		try {
			workspace.save(true, null);
		} catch (CoreException e) {
			fail("1.0", e);
		}

		// move the project to another location, before the workbench is started again
		// to emulate disconnection of a device (e.g. USB key) or a remote file system
		assertTrue("2.0", project.getLocation().toFile().renameTo(new File(project.getLocation().toFile().getAbsolutePath() + "_temp")));
	}

	/**
	 * Eclipse started again.
	 */
	public void test2() {
		IWorkspace workspace = getWorkspace();

		IProject p1 = workspace.getRoot().getProject("Project1");

		// the project should exist, but closed
		assertTrue("1.0", p1.exists());
		assertTrue("2.0", !p1.isOpen());

		// move the project back
		assertTrue("3.0", new File(p1.getLocation().toFile().getAbsolutePath() + "_temp").renameTo(p1.getLocation().toFile()));

		// now the project should be opened without any problems
		try {
			p1.open(null);
		} catch (CoreException e) {
			fail("4.0", e);
		}

		// the project should be opened and the file should exist
		assertTrue("5.0", p1.isOpen());
		IFile file1 = p1.getFile("file1.txt");
		assertTrue("6.0", file1.exists());
	}

	public static Test suite() {
		return new WorkspaceSessionTestSuite(AutomatedResourceTests.PI_RESOURCES_TESTS, TestBug208833.class);
	}
}
