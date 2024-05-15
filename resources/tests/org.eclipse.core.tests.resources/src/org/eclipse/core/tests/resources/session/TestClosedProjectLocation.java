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
import static org.eclipse.core.tests.harness.FileSystemHelper.clear;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.harness.session.SessionTestExtension;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * This is a test for bug 12507.  Immediately after workspace startup, closed projects
 * would always specify the default location, even if they were not at the default
 * location.  After opening the project, the location would be corrected.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestClosedProjectLocation {
	private static final String PROJECT = "Project";
	private static final String FILE = "File";

	@RegisterExtension
	static SessionTestExtension sessionTestExtension = SessionTestExtension.forPlugin(PI_RESOURCES_TESTS)
			.withCustomization(SessionTestExtension.createCustomWorkspace()).create();

	private IPath location = Platform.getLocation().removeLastSegments(1).append("OtherLocation");

	/**
	 * Create a project at a non-default location, and close it.
	 */
	@Test
	@Order(1)
	public void test1() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject(PROJECT);
		IFile file = project.getFile(FILE);
		IProjectDescription desc = getWorkspace().newProjectDescription(PROJECT);
		desc.setLocation(location);
		project.create(desc, createTestMonitor());
		project.open(createTestMonitor());
		createInWorkspace(file);
		project.close(createTestMonitor());
		assertEquals(location, project.getLocation());

		getWorkspace().save(true, createTestMonitor());
	}

	/**
	 * Now check the location of the closed project.
	 */
	@Test
	@Order(2)
	public void test2() {
		try {
			IProject project = getWorkspace().getRoot().getProject(PROJECT);
			IFile file = project.getFile(FILE);
			assertTrue(project.exists());
			assertFalse(project.isOpen());
			assertFalse(file.exists());
			assertEquals(location, project.getLocation());
		} finally {
			clear(location.toFile());
		}
	}

}
