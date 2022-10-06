/*******************************************************************************
 *  Copyright (c) 2005, 2012 IBM Corporation and others.
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

import java.io.IOException;
import junit.framework.Test;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.tests.resources.AutomatedResourceTests;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

/**
 * Tests regression of bug 113943 - linked resources not having
 * correct location after restart.
 */
public class TestBug113943 extends WorkspaceSerializationTest {
	IPath location = Platform.getLocation().removeLastSegments(1).append("OtherLocation");

	public static Test suite() {
		return new WorkspaceSessionTestSuite(AutomatedResourceTests.PI_RESOURCES_TESTS, TestBug113943.class);
	}

	/**
	 * Setup.  Creates a project with a linked resource.
	 */
	public void test1() {
		IProject project = workspace.getRoot().getProject("Project1");
		IFolder link = project.getFolder("link");
		IFile linkChild = link.getFile("child.txt");
		ensureExistsInWorkspace(project, true);
		try {
			IFileStore parent = EFS.getStore(location.toFile().toURI());
			IFileStore child = parent.getChild(linkChild.getName());
			parent.mkdir(EFS.NONE, getMonitor());
			child.openOutputStream(EFS.NONE, getMonitor()).close();
			link.createLink(location, IResource.NONE, getMonitor());

			assertTrue("1.0", link.exists());
			assertTrue("1.1", linkChild.exists());

			getWorkspace().save(true, getMonitor());
		} catch (CoreException | IOException e) {
			fail("1.99", e);
		}
	}

	/**
	 * Refresh the linked resource and check that its content is intact
	 */
	public void test2() {
		IProject project = workspace.getRoot().getProject("Project1");
		IFolder link = project.getFolder("link");
		IFile linkChild = link.getFile("child.txt");
		try {
			link.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());

			assertTrue("1.0", link.exists());
			assertTrue("1.1", linkChild.exists());
			cleanup();
		} catch (CoreException e) {
			fail("1.0", e);
		}
	}
}
