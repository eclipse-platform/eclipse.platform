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

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomContentsStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.harness.session.SessionShouldError;
import org.eclipse.core.tests.harness.session.SessionTestExtension;
import org.eclipse.core.tests.internal.builders.DeltaVerifierBuilder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Test1GALH44 {

	@RegisterExtension
	static SessionTestExtension sessionTestExtension = SessionTestExtension.forPlugin(PI_RESOURCES_TESTS)
			.withCustomization(SessionTestExtension.createCustomWorkspace()).create();

	/**
	 * Prepares the environment.  Create some resources and save the workspace.
	 */
	@Test
	@Order(1)
	public void test1() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IProjectDescription description = getWorkspace().newProjectDescription("MyProject");
		ICommand command = description.newCommand();
		command.setBuilderName(DeltaVerifierBuilder.BUILDER_NAME);
		description.setBuildSpec(new ICommand[] {command});
		project.create(createTestMonitor());
		project.open(createTestMonitor());
		project.setDescription(description, createTestMonitor());

		IFile file = project.getFile("foo.txt");
		file.create(createRandomContentsStream(), true, createTestMonitor());

		getWorkspace().save(true, createTestMonitor());
	}

	/**
	 * Step 2, edit a file then immediately crash.
	 */
	@Test
	@SessionShouldError
	@Order(2)
	public void test2() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IFile file = project.getFile("foo.txt");
		file.setContents(createRandomContentsStream(), true, true, createTestMonitor());
		// crash
		System.exit(-1);
	}

	/**
	 * Now immediately try to save after recovering from crash.
	 */
	@Test
	@Order(3)
	public void test3() throws CoreException {
		getWorkspace().save(true, createTestMonitor());
	}

}
