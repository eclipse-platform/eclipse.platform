/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.ResourceTest;

/**
 * Changing a project description requires the workspace root
 * scheduling rule.
 */
public class Bug_127562 extends ResourceTest {

	public void testBug() throws CoreException {
		final IProject project = getWorkspace().getRoot().getProject("Bug127562");
		ensureExistsInWorkspace(project, true);
		IProjectDescription description = project.getDescription();
		description.setComment("Foo");
		getWorkspace().run((IWorkspaceRunnable) monitor -> project.setDescription(description, getMonitor()),
				getWorkspace().getRoot(), IResource.NONE, getMonitor());
	}
}
