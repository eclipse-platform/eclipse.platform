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

import java.util.Map;
import junit.framework.Test;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.internal.builders.SortBuilder;
import org.eclipse.core.tests.internal.builders.TestBuilder;
import org.eclipse.core.tests.resources.AutomatedResourceTests;
import org.eclipse.core.tests.resources.WorkspaceSessionTest;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

/**
 * Tests the fix for bug 6995.  In this bug, a snapshot immediately after startup and
 * before doing any builds was losing the old built tree.  A subsequent build would
 * revert to a full build.
 */
public class TestBug6995 extends WorkspaceSessionTest {

	/**
	 * Create a project and configure a builder for it.
	 */
	public void test1() {
		//turn off autobuild
		IWorkspace workspace = getWorkspace();
		try {
			IWorkspaceDescription desc = workspace.getDescription();
			desc.setAutoBuilding(false);
			workspace.setDescription(desc);
		} catch (CoreException e) {
			fail("1.0", e);
		}

		//create a project and configure builder
		IProject project = workspace.getRoot().getProject("Project");
		try {
			project.create(getMonitor());
			project.open(getMonitor());

			IProjectDescription description = project.getDescription();
			ICommand command = description.newCommand();
			Map<String, String> args = command.getArguments();
			args.put(TestBuilder.BUILD_ID, "Project1Build1");
			command.setBuilderName(SortBuilder.BUILDER_NAME);
			command.setArguments(args);
			description.setBuildSpec(new ICommand[] {command});
			project.setDescription(description, getMonitor());
		} catch (CoreException e) {
			fail("2.0", e);
		}

		//do an initial build
		try {
			project.build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
		} catch (CoreException e) {
			fail("3.0", e);
		}

		//save the workspace
		try {
			workspace.save(true, getMonitor());
		} catch (CoreException e) {
			fail("4.0", e);
		}
	}

	/**
	 * After restarted the workspace, do a snapshot, then try to build.
	 */
	public void test2() {
		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject("Project");
		//snapshot
		try {
			workspace.save(false, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}

		//build
		try {
			//make a change so build doesn't get short-circuited
			IFile file = project.getFile("File");
			file.create(getRandomContents(), true, getMonitor());
			project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
		} catch (CoreException e) {
			fail("2.0", e);
		}

		//make sure an incremental build occurred
		SortBuilder builder = SortBuilder.getInstance();
		assertTrue("3.0", !builder.wasDeltaNull());
		assertTrue("3.1", builder.wasIncrementalBuild());
	}

	public static Test suite() {
		return new WorkspaceSessionTestSuite(AutomatedResourceTests.PI_RESOURCES_TESTS, TestBug6995.class);
	}
}
