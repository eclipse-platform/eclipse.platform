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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

/**
 * Basically, it is a representation of a user session. The other class,
 * in this case Snapshot2Test, is going to verify if this session ended
 * as expected (failing or not) and may add more "user actions" to be
 * verified in the next session and so on.
 */
public class Snapshot1Test extends SnapshotTest {

	protected static String[] defineHierarchy1() {
		return new String[] {"/folder110/", "/folder110/folder120/", "/folder110/folder120/folder130/", "/folder110/folder120/folder130/folder140/", "/folder110/folder120/folder130/folder140/folder150/", "/folder110/folder120/folder130/folder140/folder150/file160", "/folder110/folder120/folder130/folder140/file150", "/folder110/folder121/", "/folder110/folder121/folder131/", "/folder110/folder120/folder130/folder141/"};
	}

	protected static String[] defineHierarchy2() {
		return new String[] {"/file110", "/folder110/", "/folder110/file120", "/folder111/", "/folder111/folder120/", "/folder111/file121"};
	}

	// copy and paste in the scrapbook to run
	public void testCreateMyProject() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject(PROJECT_1);
		project.create(null);
		project.open(null);
		assertTrue("0.1", project.exists());
		assertTrue("0.2", project.isOpen());

		// create some children
		IResource[] resources = buildResources(project, defineHierarchy1());
		createInWorkspace(resources);
		assertExistsInFileSystem(resources);
		assertExistsInWorkspace(resources);

		project.close(null);
		assertTrue("2.1", project.exists());
		assertTrue("2.2", !project.isOpen());
	}

	/**
	 * Create another project and leave it closed for next session.
	 */
	public void testCreateProject2() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject(PROJECT_2);
		project.create(null);
		project.open(null);
		assertTrue("0.1", project.exists());
		assertTrue("0.2", project.isOpen());

		// create some children
		IResource[] resources = buildResources(project, defineHierarchy2());
		createInWorkspace(resources);
		assertExistsInFileSystem(resources);
		assertExistsInWorkspace(resources);
	}

	public void testSnapshotWorkspace() throws CoreException {
		getWorkspace().save(false, null);
	}
}
