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

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.harness.session.SessionTestExtension;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestBug12575 {
	private static final String projectName = "Project";

	@RegisterExtension
	static SessionTestExtension sessionTestExtension = SessionTestExtension.forPlugin(PI_RESOURCES_TESTS)
			.withCustomization(SessionTestExtension.createCustomWorkspace()).create();

	/**
	 * Setup.  Create a simple project, delete the .project file, shutdown
	 * cleanly.
	 */
	@Test
	@Order(1)
	public void test1() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject(projectName);
		project.create(createTestMonitor());
		project.open(createTestMonitor());
		IFile dotProject = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		dotProject.delete(IResource.NONE, createTestMonitor());
		getWorkspace().save(true, createTestMonitor());
	}

	/**
	 * Infection.  Modify the .project, cause a snapshot, crash
	 */
	@Test
	@Order(2)
	public void test2() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject(projectName);
		IProject other = getWorkspace().getRoot().getProject("Other");
		IProjectDescription desc = project.getDescription();
		desc.setReferencedProjects(new IProject[] { other });
		project.setDescription(desc, IResource.FORCE, createTestMonitor());
		//creating a project will cause a snapshot
		createInWorkspace(other);

		//crash
	}

	/**
	 * Impact. Fails to start.
	 */
	@Test
	@Order(3)
	public void test3() {
		//just starting this test is a sign of success
	}

}
