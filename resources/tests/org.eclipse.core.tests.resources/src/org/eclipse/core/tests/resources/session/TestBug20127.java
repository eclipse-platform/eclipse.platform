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
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.setAutoBuilding;
import static org.eclipse.core.tests.resources.ResourceTestUtil.updateProjectDescription;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.harness.session.SessionTestExtension;
import org.eclipse.core.tests.internal.builders.DeltaVerifierBuilder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests regression of bug 20127 - error restoring builder state after
 * project move.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestBug20127 {

	@RegisterExtension
	static SessionTestExtension sessionTestExtension = SessionTestExtension.forPlugin(PI_RESOURCES_TESTS)
			.withCustomization(SessionTestExtension.createCustomWorkspace()).create();

	/**
	 * Setup. Creates a project with a builder, with a built state, autobuild off.
	 */
	@Test
	@Order(1)
	public void test1() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("Project1");
		createInWorkspace(project);
		setAutoBuilding(false);

		//create a project and configure builder
		updateProjectDescription(project).addingCommand(DeltaVerifierBuilder.BUILDER_NAME)
				.withTestBuilderId("Project1Build1").apply();

		//initial build
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());

		getWorkspace().save(true, createTestMonitor());
	}

	/**
	 * Rename the project without invoking any builds.
	 */
	@Test
	@Order(2)
	public void test2() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("Project1");
		IProjectDescription desc = project.getDescription();
		desc.setName("MovedProject");
		project.move(desc, IResource.NONE, createTestMonitor());
		getWorkspace().save(true, createTestMonitor());
	}

	/**
	 * If this session starts correctly then the bug is fixed
	 */
	@Test
	@Order(3)
	public void test3() throws CoreException {
		IProject oldLocation = getWorkspace().getRoot().getProject("Project1");
		IProject newLocation = getWorkspace().getRoot().getProject("MovedProject");

		assertFalse(oldLocation.exists());
		assertTrue(newLocation.exists());
		assertTrue(newLocation.isOpen());
		getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
	}

}
