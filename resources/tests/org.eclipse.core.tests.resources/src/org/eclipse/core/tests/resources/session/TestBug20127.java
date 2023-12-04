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
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *******************************************************************************/
package org.eclipse.core.tests.resources.session;

import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;

import java.util.Map;
import junit.framework.Test;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.internal.builders.DeltaVerifierBuilder;
import org.eclipse.core.tests.internal.builders.TestBuilder;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

/**
 * Tests regression of bug 20127 - error restoring builder state after
 * project move.
 */
public class TestBug20127 extends WorkspaceSerializationTest {
	/**
	 * Setup.  Creates a project with a builder, with a built state,
	 * autobuild off.
	 */
	public void test1() throws CoreException {
		IProject project = workspace.getRoot().getProject("Project1");
		ensureExistsInWorkspace(project, true);
		setAutoBuilding(false);

		//create a project and configure builder
		IProjectDescription description = project.getDescription();
		ICommand command = description.newCommand();
		Map<String, String> args = command.getArguments();
		args.put(TestBuilder.BUILD_ID, "Project1Build1");
		command.setBuilderName(DeltaVerifierBuilder.BUILDER_NAME);
		command.setArguments(args);
		description.setBuildSpec(new ICommand[] { command });
		project.setDescription(description, getMonitor());

		//initial build
		workspace.build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());

		getWorkspace().save(true, getMonitor());
	}

	/**
	 * Rename the project without invoking any builds.
	 */
	public void test2() throws CoreException {
		IProject project = workspace.getRoot().getProject("Project1");
		IProjectDescription desc = project.getDescription();
		desc.setName("MovedProject");
		project.move(desc, IResource.NONE, getMonitor());
		workspace.save(true, getMonitor());
	}

	/**
	 * If this session starts correctly then the bug is fixed
	 */
	public void test3() throws CoreException {
		IProject oldLocation = workspace.getRoot().getProject("Project1");
		IProject newLocation = workspace.getRoot().getProject("MovedProject");

		assertTrue("1.0", !oldLocation.exists());
		assertTrue("1.0", newLocation.exists());
		assertTrue("1.1", newLocation.isOpen());
		workspace.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
	}

	public static Test suite() {
		return new WorkspaceSessionTestSuite(PI_RESOURCES_TESTS, TestBug20127.class);
	}
}
