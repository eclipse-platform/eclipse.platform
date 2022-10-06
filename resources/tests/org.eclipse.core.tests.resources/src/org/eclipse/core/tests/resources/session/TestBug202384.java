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

import junit.framework.Test;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.*;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

/**
 * Test for bug 202384
 */
public class TestBug202384 extends WorkspaceSessionTest {

	public void testInitializeWorkspace() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject project = workspace.getRoot().getProject("project");
		ensureExistsInWorkspace(project, true);
		try {
			project.setDefaultCharset("UTF-8", getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}
		try {
			assertEquals("2.0", "UTF-8", project.getDefaultCharset(false));
		} catch (CoreException e) {
			fail("3.0", e);
		}
		try {
			project.close(getMonitor());
		} catch (CoreException e) {
			fail("4.0", e);
		}
		try {
			workspace.save(true, getMonitor());
		} catch (CoreException e) {
			fail("5.0", e);
		}
	}

	public void testStartWithClosedProject() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject project = workspace.getRoot().getProject("project");
		assertFalse("1.0", project.isOpen());
		try {
			//project is closed so it is not possible to read correct encoding
			assertNull("2.0", project.getDefaultCharset(false));
		} catch (CoreException e) {
			fail("3.0", e);
		}
		try {
			//opening the project should initialize ProjectPreferences
			project.open(getMonitor());
		} catch (CoreException e) {
			fail("4.0", e);
		}
		try {
			//correct values should be available after initialization
			assertEquals("5.0", "UTF-8", project.getDefaultCharset(false));
		} catch (CoreException e) {
			fail("6.0", e);
		}
		try {
			workspace.save(true, getMonitor());
		} catch (CoreException e) {
			fail("7.0", e);
		}
	}

	public void testStartWithOpenProject() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject project = workspace.getRoot().getProject("project");
		assertTrue("1.0", project.isOpen());
		try {
			//correct values should be available if ProjectPreferences got
			//initialized upon creation
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
		} catch (CoreException e) {
			fail("3.0", e);
		}
		try {
			workspace.save(true, getMonitor());
		} catch (CoreException e) {
			fail("4.0", e);
		}
	}

	public static Test suite() {
		return new WorkspaceSessionTestSuite(AutomatedResourceTests.PI_RESOURCES_TESTS, TestBug202384.class);
	}
}
