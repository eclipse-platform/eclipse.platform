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

import static org.eclipse.core.tests.harness.FileSystemHelper.clear;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;

import junit.framework.Test;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

/**
 * This is a test for bug 12507.  Immediately after workspace startup, closed projects
 * would always specify the default location, even if they were not at the default
 * location.  After opening the project, the location would be corrected.
 */
public class TestClosedProjectLocation extends WorkspaceSerializationTest {
	IPath location = Platform.getLocation().removeLastSegments(1).append("OtherLocation");

	/**
	 * Create a project at a non-default location, and close it.
	 */
	public void test1() throws CoreException {
		IProject project = workspace.getRoot().getProject(PROJECT);
		IFile file = project.getFile(FILE);
		IProjectDescription desc = workspace.newProjectDescription(PROJECT);
		desc.setLocation(location);
		project.create(desc, createTestMonitor());
		project.open(createTestMonitor());
		createInWorkspace(file);
		project.close(createTestMonitor());
		assertEquals("1.1", location, project.getLocation());

		workspace.save(true, createTestMonitor());
	}

	/**
	 * Now check the location of the closed project.
	 */
	public void test2() {
		try {
			IProject project = workspace.getRoot().getProject(PROJECT);
			IFile file = project.getFile(FILE);
			assertTrue("1.0", project.exists());
			assertTrue("1.1", !project.isOpen());
			assertTrue("1.2", !file.exists());
			assertEquals("1.3", location, project.getLocation());
		} finally {
			clear(location.toFile());
		}
	}

	public static Test suite() {
		return new WorkspaceSessionTestSuite(PI_RESOURCES_TESTS, TestClosedProjectLocation.class);
	}
}
