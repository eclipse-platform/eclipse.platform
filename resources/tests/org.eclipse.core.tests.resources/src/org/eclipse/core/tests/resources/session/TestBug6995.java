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

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomContentsStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.setAutoBuilding;
import static org.eclipse.core.tests.resources.ResourceTestUtil.updateProjectDescription;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.harness.session.SessionTestExtension;
import org.eclipse.core.tests.internal.builders.SortBuilder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests the fix for bug 6995.  In this bug, a snapshot immediately after startup and
 * before doing any builds was losing the old built tree.  A subsequent build would
 * revert to a full build.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestBug6995 {

	@RegisterExtension
	static SessionTestExtension sessionTestExtension = SessionTestExtension.forPlugin(PI_RESOURCES_TESTS)
			.withCustomization(SessionTestExtension.createCustomWorkspace()).create();

	/**
	 * Create a project and configure a builder for it.
	 */
	@Test
	@Order(1)
	public void test1() throws CoreException {
		IWorkspace workspace = getWorkspace();
		setAutoBuilding(false);

		//create a project and configure builder
		IProject project = workspace.getRoot().getProject("Project");
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		updateProjectDescription(project).addingCommand(SortBuilder.BUILDER_NAME).withTestBuilderId("Project1Build1")
				.apply();

		//do an initial build
		project.build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());

		//save the workspace
		workspace.save(true, createTestMonitor());
	}

	/**
	 * After restarted the workspace, do a snapshot, then try to build.
	 */
	@Test
	@Order(2)
	public void test2() throws CoreException {
		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject("Project");
		//snapshot
		workspace.save(false, createTestMonitor());

		//build
		//make a change so build doesn't get short-circuited
		IFile file = project.getFile("File");
		file.create(createRandomContentsStream(), true, createTestMonitor());
		project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());

		//make sure an incremental build occurred
		SortBuilder builder = SortBuilder.getInstance();
		assertFalse(builder.wasDeltaNull());
		assertTrue(builder.wasIncrementalBuild());
	}

}
