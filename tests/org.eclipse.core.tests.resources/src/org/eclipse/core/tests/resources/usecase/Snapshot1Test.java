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
	public void testCreateMyProject() {
		IProject project = getWorkspace().getRoot().getProject(PROJECT_1);
		try {
			project.create(null);
			project.open(null);
		} catch (CoreException e) {
			fail("0.0", e);
		}
		assertTrue("0.1", project.exists());
		assertTrue("0.2", project.isOpen());

		// create some children
		IResource[] resources = buildResources(project, defineHierarchy1());
		ensureExistsInWorkspace(resources, true);
		assertExistsInFileSystem("1.1", resources);
		assertExistsInWorkspace("1.2", resources);

		try {
			project.close(null);
		} catch (CoreException e) {
			fail("2.0", e);
		}
		assertTrue("2.1", project.exists());
		assertTrue("2.2", !project.isOpen());
	}

	/**
	 * Create another project and leave it closed for next session.
	 */
	public void testCreateProject2() {
		IProject project = getWorkspace().getRoot().getProject(PROJECT_2);
		try {
			project.create(null);
			project.open(null);
		} catch (CoreException e) {
			fail("0.0", e);
		}
		assertTrue("0.1", project.exists());
		assertTrue("0.2", project.isOpen());

		// create some children
		IResource[] resources = buildResources(project, defineHierarchy2());
		ensureExistsInWorkspace(resources, true);
		assertExistsInFileSystem("3.1", resources);
		assertExistsInWorkspace("3.2", resources);
	}

	public void testSnapshotWorkspace() {
		try {
			getWorkspace().save(false, null);
		} catch (CoreException e) {
			fail("1.0", e);
		}
	}
}
