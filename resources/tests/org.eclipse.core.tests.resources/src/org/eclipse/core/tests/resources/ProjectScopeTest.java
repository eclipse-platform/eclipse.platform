/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
package org.eclipse.core.tests.resources;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createUniqueString;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(WorkspaceResetExtension.class)
public class ProjectScopeTest {

	@Test
	public void testEqualsAndHashCode() {
		IProject project = getWorkspace().getRoot().getProject(createUniqueString());
		ProjectScope projectScope1 = new ProjectScope(project);
		ProjectScope projectScope2 = new ProjectScope(project);
		assertTrue(projectScope1.equals(projectScope2));
		assertTrue(projectScope2.equals(projectScope1));
		assertTrue(projectScope1.hashCode() == projectScope2.hashCode());
	}

}
