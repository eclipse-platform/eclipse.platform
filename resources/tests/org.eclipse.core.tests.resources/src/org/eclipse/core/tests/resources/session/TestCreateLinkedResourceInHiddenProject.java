/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
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

import junit.framework.Test;
import org.eclipse.core.internal.resources.ProjectDescription;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.resources.AutomatedResourceTests;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

/**
 * Tests for the bug 219568.
 */
public class TestCreateLinkedResourceInHiddenProject extends WorkspaceSerializationTest {

	public void test1() {
		/* create some resource handles */
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT);
		try {
			IProjectDescription desc = new ProjectDescription();
			desc.setName(PROJECT);
			project.create(desc, IResource.HIDDEN, getMonitor());
			project.open(getMonitor());

			workspace.save(true, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}
	}

	public void test2() {
		IPath path = getTempDir().addTrailingSeparator().append(getUniqueString());
		path.toFile().mkdir();

		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT);
		IFolder folder = project.getFolder(getUniqueString());

		try {
			folder.createLink(path, IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("2.0", e);
		}
	}

	public static Test suite() {
		return new WorkspaceSessionTestSuite(AutomatedResourceTests.PI_RESOURCES_TESTS, TestCreateLinkedResourceInHiddenProject.class);
	}
}
