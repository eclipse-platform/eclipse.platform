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

import junit.framework.Test;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.AutomatedResourceTests;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

/**
 * Tests performing a save on a workspace, then crashing and recovering.
 */
public class TestSave extends WorkspaceSerializationTest {

	public void test1() {
		/* create some resource handles */
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT);
		try {
			project.create(getMonitor());
			project.open(getMonitor());

			workspace.save(true, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}
	}

	public void test2() {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		assertTrue("1.0", root.exists());
		try {
			IResource[] children = root.members();
			assertEquals("1.2", 1, children.length);
			IProject project = (IProject) children[0];
			assertTrue("1.3", project.exists());
			assertTrue("1.4", project.isOpen());
			assertEquals("1.5", PROJECT, project.getName());
		} catch (CoreException e) {
			fail("1.99", e);
		}
	}

	public static Test suite() {
		return new WorkspaceSessionTestSuite(AutomatedResourceTests.PI_RESOURCES_TESTS, TestSave.class);
	}

}
