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

import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;

import junit.framework.Test;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

public class TestBug12575 extends WorkspaceSerializationTest {

	private static final String projectName = "Project";

	/**
	 * Setup.  Create a simple project, delete the .project file, shutdown
	 * cleanly.
	 */
	public void test1() throws CoreException {
		IProject project = workspace.getRoot().getProject(projectName);
		project.create(getMonitor());
		project.open(getMonitor());
		IFile dotProject = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		dotProject.delete(IResource.NONE, getMonitor());
		workspace.save(true, getMonitor());
	}

	/**
	 * Infection.  Modify the .project, cause a snapshot, crash
	 */
	public void test2() throws CoreException {
		IProject project = workspace.getRoot().getProject(projectName);
		IProject other = workspace.getRoot().getProject("Other");
		IProjectDescription desc = project.getDescription();
		desc.setReferencedProjects(new IProject[] { other });
		project.setDescription(desc, IResource.FORCE, getMonitor());
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
		return new WorkspaceSessionTestSuite(PI_RESOURCES_TESTS, TestBug12575.class);
	}
}
