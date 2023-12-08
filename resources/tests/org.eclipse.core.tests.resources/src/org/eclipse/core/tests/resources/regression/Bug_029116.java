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
package org.eclipse.core.tests.resources.regression;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.NATURE_29116;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test regression of bug 29116. In this bug, triggering a builder during
 * installation of a nature caused an assertion failure.
 */
public class Bug_029116 {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	@Test
	public void testBug() throws CoreException {
		// Create some resource handles
		IProject project = getWorkspace().getRoot().getProject("PROJECT");
		// Create and open a project
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		// Create and set a build spec for the project
		IProjectDescription desc = project.getDescription();
		desc.setNatureIds(new String[] { NATURE_29116 });
		project.setDescription(desc, createTestMonitor());
	}

}
