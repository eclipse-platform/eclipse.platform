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
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *******************************************************************************/
package org.eclipse.core.tests.resources.usecase;

import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

/**
 * Change some resources mixing full saves and snapshots.
 */
public class Snapshot4Test extends SnapshotTest {

	protected static String[] defineHierarchy1() {
		List<String> result = new ArrayList<>();
		String[] old = Snapshot3Test.defineHierarchy1();
		result.addAll(Arrays.asList(old));
		result.remove(new Path(PROJECT_1).append("added file").toString());
		result.remove(new Path(PROJECT_1).append("yet another file").toString());
		result.remove(new Path(PROJECT_1).append("a folder").addTrailingSeparator().toString());
		return result.toArray(new String[result.size()]);
	}

	protected static String[] defineHierarchy2() {
		return new String[0];
	}

	public void testChangeMyProject() {
		// MyProject
		IProject project = getWorkspace().getRoot().getProject(PROJECT_1);
		assertTrue("0.1", project.exists());
		assertTrue("0.2", project.isOpen());

		// remove resources
		IFile file = project.getFile("added file");
		try {
			file.delete(true, true, null);
		} catch (CoreException e) {
			fail("1.0", e);
		}
		assertDoesNotExistInFileSystem("1.1", file);
		assertDoesNotExistInWorkspace("1.2", file);

		// full save
		try {
			getWorkspace().save(true, null);
		} catch (CoreException e) {
			fail("2.0", e);
		}

		// remove resources
		file = project.getFile("yet another file");
		try {
			file.delete(true, true, null);
		} catch (CoreException e) {
			fail("3.0", e);
		}
		assertDoesNotExistInFileSystem("3.1", file);
		assertDoesNotExistInWorkspace("3.2", file);

		// snapshot
		try {
			getWorkspace().save(false, null);
		} catch (CoreException e) {
			fail("4.0", e);
		}

		// remove resources
		IFolder folder = project.getFolder("a folder");
		try {
			folder.delete(true, true, null);
		} catch (CoreException e) {
			fail("5.0", e);
		}
		assertDoesNotExistInFileSystem("5.1", folder);
		assertDoesNotExistInWorkspace("5.2", folder);

		// snapshot
		try {
			getWorkspace().save(false, null);
		} catch (CoreException e) {
			fail("6.0", e);
		}
	}

	public void testChangeProject2() {
		IProject project = getWorkspace().getRoot().getProject(PROJECT_2);
		assertTrue("0.1", project.exists());
		assertTrue("0.2", project.isOpen());

		// remove project
		try {
			project.delete(true, true, null);
		} catch (CoreException e) {
			fail("1.0", e);
		}
		assertTrue("1.1", !project.exists());

		// snapshot
		try {
			getWorkspace().save(false, null);
		} catch (CoreException e) {
			fail("4.0", e);
		}
	}

	public void testVerifyPreviousSession() {
		// MyProject
		IProject project = getWorkspace().getRoot().getProject(PROJECT_1);
		assertTrue("0.0", project.exists());
		assertTrue("0.1", project.isOpen());

		// verify existence of children
		IResource[] resources = buildResources(project, Snapshot3Test.defineHierarchy1());
		assertExistsInFileSystem("2.1", resources);
		assertExistsInWorkspace("2.2", resources);

		// Project2
		project = getWorkspace().getRoot().getProject(PROJECT_2);
		assertTrue("3.0", project.exists());
		assertTrue("3.1", project.isOpen());

		try {
			assertEquals("4.0", 4, project.members().length);
			assertNotNull("4.1", project.findMember(IProjectDescription.DESCRIPTION_FILE_NAME));
		} catch (CoreException e) {
			fail("4.2", e);
		}

		// verify existence of children
		resources = buildResources(project, Snapshot3Test.defineHierarchy2());
		assertExistsInFileSystem("5.1", resources);
		assertExistsInWorkspace("5.2", resources);
	}
}
