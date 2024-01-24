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
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.TestUtil;
import org.eclipse.core.tests.resources.WorkspaceSessionTest;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

import junit.framework.Test;

/**
 * Test for bug 202384
 */
public class TestBug202384 extends WorkspaceSessionTest {

	public void testInitializeWorkspace() throws CoreException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject project = workspace.getRoot().getProject("project");
		createInWorkspace(project);
		project.setDefaultCharset("UTF-8", createTestMonitor());
		assertEquals("UTF-8", project.getDefaultCharset(false));
		project.close(createTestMonitor());
		workspace.save(true, createTestMonitor());
	}

	public void testStartWithClosedProject() throws CoreException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject project = workspace.getRoot().getProject("project");
		assertFalse(project.isOpen());
		// project is closed so it is not possible to read correct encoding
		assertNull(project.getDefaultCharset(false));
		// opening the project should initialize ProjectPreferences
		project.open(createTestMonitor());
		// correct values should be available after initialization
		assertEquals("UTF-8", project.getDefaultCharset(false));
		workspace.save(true, createTestMonitor());
	}

	public void testStartWithOpenProject() throws CoreException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject project = workspace.getRoot().getProject("project");
		assertTrue(project.isOpen());
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
		assertEquals(expectedEncoding, project.getDefaultCharset(false));
		workspace.save(true, createTestMonitor());
	}

	public static Test suite() {
		return new WorkspaceSessionTestSuite(PI_RESOURCES_TESTS, TestBug202384.class);
	}
}
