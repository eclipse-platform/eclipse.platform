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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.ResourceTest;

/**
 * Changing a project description requires the workspace root
 * scheduling rule.
 */
public class Bug_127562 extends ResourceTest {

	public void testBug() {
		final IProject project = getWorkspace().getRoot().getProject("Bug127562");
		ensureExistsInWorkspace(project, true);
		IProjectDescription description = null;
		try {
			description = project.getDescription();
			description.setComment("Foo");
		} catch (CoreException e) {
			fail("1.99", e);
		}
		final IProjectDescription finalDescription = description;
		try {
			getWorkspace().run((IWorkspaceRunnable) monitor -> project.setDescription(finalDescription, getMonitor()), getWorkspace().getRoot(), IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("4.99", e);
		}
	}
}
