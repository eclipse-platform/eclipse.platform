/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
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

import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;

import junit.framework.Test;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.TestUtil;
import org.eclipse.core.tests.resources.WorkspaceSessionTest;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

/**
 * Test for bug 202384
 */
public class TestBug202384 extends WorkspaceSessionTest {

	public void testInitializeWorkspace() throws CoreException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject project = workspace.getRoot().getProject("project");
		ensureExistsInWorkspace(project, true);
		project.setDefaultCharset("UTF-8", getMonitor());
		assertEquals("2.0", "UTF-8", project.getDefaultCharset(false));
		project.close(getMonitor());
		workspace.save(true, getMonitor());
	}

	public void testStartWithClosedProject() throws CoreException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject project = workspace.getRoot().getProject("project");
		assertFalse("1.0", project.isOpen());
		// project is closed so it is not possible to read correct encoding
		assertNull("2.0", project.getDefaultCharset(false));
		// opening the project should initialize ProjectPreferences
		project.open(getMonitor());
		// correct values should be available after initialization
		assertEquals("5.0", "UTF-8", project.getDefaultCharset(false));
		workspace.save(true, getMonitor());
	}

	public void testStartWithOpenProject() throws CoreException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject project = workspace.getRoot().getProject("project");
		assertTrue("1.0", project.isOpen());
		// correct values should be available if ProjectPreferences got
		// initialized upon creation
		String expectedEncoding = "UTF-8";
		// check with a timeout, in case some initialize operation is slow
		long timeout = 10_000;
		long start = System.currentTimeMillis();
		while (!expectedEncoding.equals(project.getDefaultCharset(false))
				&& System.currentTimeMillis() - start < timeout) {
			TestUtil.dumpRunnigOrWaitingJobs(getName());
			TestUtil.waitForJobs(getName(), 500, 1000);
		}
		assertEquals("2.0", expectedEncoding, project.getDefaultCharset(false));
		workspace.save(true, getMonitor());
	}

	public static Test suite() {
		return new WorkspaceSessionTestSuite(PI_RESOURCES_TESTS, TestBug202384.class);
	}
}
