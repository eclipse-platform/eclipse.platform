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

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/**
 * This session add some resources to MyProject. All resources
 * from Project2 are removed and some resources are added to it
 * but not written to disk (only in the tree).
 * Only snapshots are taken. No full saves.
 */
public class Snapshot2Test extends SnapshotTest {

	protected static String[] defineHierarchy1() {
		List<String> result = new ArrayList<>();
		String[] old = Snapshot1Test.defineHierarchy1();
		result.addAll(Arrays.asList(old));
		result.add(IPath.fromOSString(PROJECT_1).append("added file").toString());
		result.add(IPath.fromOSString(PROJECT_1).append("yet another file").toString());
		result.add(IPath.fromOSString(PROJECT_1).append("a folder").addTrailingSeparator().toString());
		return result.toArray(new String[result.size()]);
	}

	protected static String[] defineHierarchy2() {
		return new String[] {"/added file", "/yet another file", "/a folder/"};
	}

	public void testChangeMyProject() throws CoreException {
		// MyProject
		IProject project = getWorkspace().getRoot().getProject(PROJECT_1);
		assertTrue("0.1", project.exists());
		assertTrue("0.2", project.isOpen());

		// create some children
		IResource[] resources = buildResources(project, defineHierarchy1());
		ensureExistsInWorkspace(resources, true);
		assertExistsInFileSystem(resources);
		assertExistsInWorkspace(resources);
	}

	public void testChangeProject2() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("Project2");
		assertTrue("0.1", project.exists());
		assertTrue("0.2", project.isOpen());

		// remove all resources
		IResource[] children = project.members();
		getWorkspace().delete(children, true, null);

		// create some children
		IResource[] resources = buildResources(project, defineHierarchy2());
		ensureExistsInWorkspace(resources, true);
		assertExistsInFileSystem(resources);
		assertExistsInWorkspace(resources);
	}

	public void testSnapshotWorkspace() throws CoreException {
		getWorkspace().save(false, null);
	}

	public void testVerifyPreviousSession() throws CoreException {
		// MyProject
		IProject project = getWorkspace().getRoot().getProject(PROJECT_1);
		assertTrue("0.0", project.exists());
		assertTrue("0.1", !project.isOpen());

		project.open(null);
		assertTrue("1.2", project.isOpen());

		// verify existence of children
		IResource[] resources = buildResources(project, Snapshot1Test.defineHierarchy1());
		assertExistsInFileSystem(resources);
		assertExistsInWorkspace(resources);

		// Project2
		project = getWorkspace().getRoot().getProject(PROJECT_2);
		assertTrue("3.0", project.exists());
		assertTrue("3.1", project.isOpen());

		// verify existence of children
		resources = buildResources(project, Snapshot1Test.defineHierarchy2());
		assertExistsInFileSystem(resources);
		assertExistsInWorkspace(resources);
	}
}
