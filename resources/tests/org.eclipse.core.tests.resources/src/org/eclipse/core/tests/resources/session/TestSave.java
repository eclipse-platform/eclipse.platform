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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.harness.session.SessionTestExtension;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests performing a save on a workspace, then crashing and recovering.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestSave {
	private static final String PROJECT = "Project";

	@RegisterExtension
	static SessionTestExtension sessionTestExtension = SessionTestExtension.forPlugin(PI_RESOURCES_TESTS)
			.withCustomization(SessionTestExtension.createCustomWorkspace()).create();

	@Test
	@Order(1)
	public void test1() throws CoreException {
		/* create some resource handles */
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT);
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		getWorkspace().save(true, createTestMonitor());
	}

	@Test
	@Order(2)
	public void test2() throws CoreException {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		assertThat(root.exists()).isTrue();
		IResource[] children = root.members();
		assertThat(children).singleElement().asInstanceOf(InstanceOfAssertFactories.type(IProject.class))
				.satisfies(project -> {
					assertThat(project.exists()).isTrue();
					assertThat(project.isOpen()).isTrue();
					assertThat(project.getName()).isEqualTo(PROJECT);
				});
	}

}
