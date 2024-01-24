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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.resources.WorkspaceSessionTest;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

import junit.framework.Test;

/**
 * Tests regression of bug 30015.  Due to this bug, it was impossible to restore
 * a project whose location was relative to a workspace path variable.
 */
public class TestBug30015 extends WorkspaceSessionTest {
	protected static final String PROJECT_NAME = "Project";
	protected static final String VAR_NAME = "ProjectLocatio";
	protected IPath varValue;
	protected IPath rawLocation;

	/**
	 * Create and open the project
	 */
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

	public static Test suite() {
		return new WorkspaceSessionTestSuite(PI_RESOURCES_TESTS, TestBug30015.class);
	}
}
