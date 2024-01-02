/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.regression;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.harness.FileSystemHelper.getTempDir;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.Rule;
import org.junit.Test;

public class PR_1GH2B0N_Test {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	@Test
	public void test_1GH2B0N() throws CoreException {
		IPath path = getTempDir().append("1GH2B0N");
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IProjectDescription description = getWorkspace().newProjectDescription("MyProject");
		IPath projectLocation = path.append(project.getName());
		workspaceRule.deleteOnTearDown(projectLocation);
		description.setLocation(projectLocation);
		project.create(description, createTestMonitor());
		project.open(createTestMonitor());

		IProject project2 = getWorkspace().getRoot().getProject("MyProject2");
		IStatus status = getWorkspace().validateProjectLocation(project2, project.getLocation().append(project2.getName()));
		//Note this is not the original error case -
		//since Eclipse 3.2 a project is allowed to be nested in another project
		assertTrue("2.0", status.isOK());
	}

}
