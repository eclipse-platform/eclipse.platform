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

import junit.framework.Test;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.AutomatedResourceTests;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

/**
 * Create a project, close it, save, crash, recover.  Recovered project should still be closed.
 */
public class TestSaveWithClosedProject extends WorkspaceSerializationTest {
	public void test1() throws CoreException {
		/* create some resource handles */
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT);
		IFile file = project.getFile(FILE);
		project.create(getMonitor());
		project.open(getMonitor());
		file.create(getRandomContents(), true, null);
		project.close(getMonitor());

		workspace.save(true, getMonitor());
	}

	public void test2() throws CoreException {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT);
		IFile file = project.getFile(FILE);

		assertTrue("1.0", project.exists());
		assertTrue("1.1", !project.isOpen());
		assertTrue("1.2", !file.exists());

		project.open(getMonitor());

		assertTrue("2.0", project.isOpen());
		assertTrue("2.1", file.exists());
	}

	public static Test suite() {
		return new WorkspaceSessionTestSuite(AutomatedResourceTests.PI_RESOURCES_TESTS, TestSaveWithClosedProject.class);
	}
}
