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
package org.eclipse.core.tests.resources.session;

import junit.framework.Test;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.AutomatedResourceTests;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

public class TestBug12575 extends WorkspaceSerializationTest {

	private static final String projectName = "Project";

	/**
	 * Setup.  Create a simple project, delete the .project file, shutdown
	 * cleanly.
	 */
	public void test1() {
		IProject project = workspace.getRoot().getProject(projectName);
		try {
			project.create(getMonitor());
			project.open(getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}
		IFile dotProject = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		try {
			dotProject.delete(IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("1.1", e);
		}
		try {
			workspace.save(true, getMonitor());
		} catch (CoreException e) {
			fail("1.2", e);
		}
	}

	/**
	 * Infection.  Modify the .project, cause a snapshot, crash
	 */
	public void test2() {
		IProject project = workspace.getRoot().getProject(projectName);
		IProject other = workspace.getRoot().getProject("Other");
		try {
			IProjectDescription desc = project.getDescription();
			desc.setReferencedProjects(new IProject[] {other});
			project.setDescription(desc, IResource.FORCE, getMonitor());
		} catch (CoreException e) {
			fail("2.0", e);
		}
		//creating a project will cause a snapshot
		ensureExistsInWorkspace(other, true);

		//crash
	}

	/**
	 * Impact. Fails to start.
	 */
	public void test3() {
		//just starting this test is a sign of success
	}

	public static Test suite() {
		return new WorkspaceSessionTestSuite(AutomatedResourceTests.PI_RESOURCES_TESTS, TestBug12575.class);
	}
}
