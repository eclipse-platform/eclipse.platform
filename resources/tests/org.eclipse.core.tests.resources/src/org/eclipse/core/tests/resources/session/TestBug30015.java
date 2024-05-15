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
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
 * Tests regression of bug 30015.  Due to this bug, it was impossible to restore
 * a project whose location was relative to a workspace path variable.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestBug30015 {

	private static final String PROJECT_NAME = "Project";
	private static final String VAR_NAME = "ProjectLocatio";

	@RegisterExtension
	static SessionTestExtension sessionTestExtension = SessionTestExtension.forPlugin(PI_RESOURCES_TESTS)
			.withCustomization(SessionTestExtension.createCustomWorkspace()).create();

	private IPath varValue;
	private IPath rawLocation;

	/**
	 * Create and open the project
	 */
	@SuppressWarnings("deprecation")
	@Test
	@Order(1)
	public void test1() throws CoreException {
		varValue = Platform.getLocation().removeLastSegments(1);
		rawLocation = IPath.fromOSString(VAR_NAME).append("ProjectLocation");
		//define the variable
		getWorkspace().getPathVariableManager().setValue(VAR_NAME, varValue);
		IProject project = getWorkspace().getRoot().getProject(PROJECT_NAME);
		IProjectDescription description = getWorkspace().newProjectDescription(PROJECT_NAME);
		description.setLocation(rawLocation);
		//create the project
		project.create(description, createTestMonitor());
		project.open(createTestMonitor());
		//save and shutdown
		getWorkspace().save(true, createTestMonitor());
	}

	/**
	 * See if the project was successfully restored.
	 */
	@SuppressWarnings("deprecation")
	@Test
	@Order(2)
	public void test2() {
		varValue = Platform.getLocation().removeLastSegments(1);
		rawLocation = IPath.fromOSString(VAR_NAME).append("ProjectLocation");
		IProject project = getWorkspace().getRoot().getProject(PROJECT_NAME);

		assertEquals(varValue, getWorkspace().getPathVariableManager().getValue(VAR_NAME));
		assertTrue(project.exists());
		assertTrue(project.isOpen());
		assertEquals(rawLocation, project.getRawLocation());
		assertEquals(varValue.append(rawLocation.lastSegment()), project.getLocation());
	}

}
