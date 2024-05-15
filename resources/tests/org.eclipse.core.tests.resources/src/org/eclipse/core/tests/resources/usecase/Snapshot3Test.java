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
package org.eclipse.core.tests.resources.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.buildResources;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

/**
 * This session only performs a full save. The workspace should stay
 * the same.
 */
public class Snapshot3Test {

	protected static String[] defineHierarchy1() {
		return Snapshot2Test.defineHierarchy1();
	}

	protected static String[] defineHierarchy2() {
		return Snapshot2Test.defineHierarchy2();
	}

	public void testSaveWorkspace() throws CoreException {
		getWorkspace().save(true, null);
	}

	public void testVerifyPreviousSession() throws CoreException {
		// MyProject
		IProject project = getWorkspace().getRoot().getProject(SnapshotTest.PROJECT_1);
		assertTrue(project.exists());
		assertTrue(project.isOpen());

		// verify existence of children
		IResource[] resources = buildResources(project, Snapshot2Test.defineHierarchy1());
		assertExistsInFileSystem(resources);
		assertExistsInWorkspace(resources);

		// Project2
		project = getWorkspace().getRoot().getProject(SnapshotTest.PROJECT_2);
		assertTrue(project.exists());
		assertTrue(project.isOpen());

		assertThat(project.members()).hasSize(4);
		assertNotNull(project.findMember(IProjectDescription.DESCRIPTION_FILE_NAME));

		// verify existence of children
		resources = buildResources(project, Snapshot2Test.defineHierarchy2());
		assertExistsInFileSystem(resources);
		assertExistsInWorkspace(resources);
	}
}
