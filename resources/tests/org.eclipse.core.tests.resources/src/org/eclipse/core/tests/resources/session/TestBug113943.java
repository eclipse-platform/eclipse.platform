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

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;

import junit.framework.Test;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

/**
 * Tests regression of bug 113943 - linked resources not having
 * correct location after restart.
 */
public class TestBug113943 extends WorkspaceSerializationTest {
	IPath location = Platform.getLocation().removeLastSegments(1).append("OtherLocation");

	public static Test suite() {
		return new WorkspaceSessionTestSuite(PI_RESOURCES_TESTS, TestBug113943.class);
	}

	/**
	 * Setup.  Creates a project with a linked resource.
	 */
	public void test1() throws Exception {
		IProject project = workspace.getRoot().getProject("Project1");
		IFolder link = project.getFolder("link");
		IFile linkChild = link.getFile("child.txt");
		createInWorkspace(project);
		IFileStore parent = EFS.getStore(location.toFile().toURI());
		IFileStore child = parent.getChild(linkChild.getName());
		parent.mkdir(EFS.NONE, createTestMonitor());
		child.openOutputStream(EFS.NONE, createTestMonitor()).close();
		link.createLink(location, IResource.NONE, createTestMonitor());

		assertTrue("1.0", link.exists());
		assertTrue("1.1", linkChild.exists());

		getWorkspace().save(true, createTestMonitor());
	}

	/**
	 * Refresh the linked resource and check that its content is intact
	 */
	public void test2() throws CoreException {
		IProject project = workspace.getRoot().getProject("Project1");
		IFolder link = project.getFolder("link");
		IFile linkChild = link.getFile("child.txt");
		link.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());

		assertTrue("1.0", link.exists());
		assertTrue("1.1", linkChild.exists());
	}
}
