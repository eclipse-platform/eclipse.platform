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

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.buildResources;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

/**
 * This session only performs a full save. The workspace should stay
 * the same.
 */
public class Snapshot3Test extends SnapshotTest {

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
		IProject project = getWorkspace().getRoot().getProject(PROJECT_1);
		assertTrue("0.0", project.exists());
		assertTrue("0.1", project.isOpen());

		// verify existence of children
		IResource[] resources = buildResources(project, Snapshot2Test.defineHierarchy1());
		assertExistsInFileSystem(resources);
		assertExistsInWorkspace(resources);

		// Project2
		project = getWorkspace().getRoot().getProject(PROJECT_2);
		assertTrue("3.0", project.exists());
		assertTrue("3.1", project.isOpen());

		assertEquals("4.0", 4, project.members().length);
		assertNotNull("4.1", project.findMember(IProjectDescription.DESCRIPTION_FILE_NAME));

		// verify existence of children
		resources = buildResources(project, Snapshot2Test.defineHierarchy2());
		assertExistsInFileSystem(resources);
		assertExistsInWorkspace(resources);
	}
}
