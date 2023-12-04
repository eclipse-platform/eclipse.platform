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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/**
 * Change some resources mixing full saves and snapshots.
 */
public class Snapshot4Test extends SnapshotTest {

	protected static String[] defineHierarchy1() {
		List<String> result = new ArrayList<>();
		String[] old = Snapshot3Test.defineHierarchy1();
		result.addAll(Arrays.asList(old));
		result.remove(IPath.fromOSString(PROJECT_1).append("added file").toString());
		result.remove(IPath.fromOSString(PROJECT_1).append("yet another file").toString());
		result.remove(IPath.fromOSString(PROJECT_1).append("a folder").addTrailingSeparator().toString());
		return result.toArray(new String[result.size()]);
	}

	protected static String[] defineHierarchy2() {
		return new String[0];
	}

	public void testChangeMyProject() throws CoreException {
		// MyProject
		IProject project = getWorkspace().getRoot().getProject(PROJECT_1);
		assertTrue("0.1", project.exists());
		assertTrue("0.2", project.isOpen());

		// remove resources
		IFile file = project.getFile("added file");
		file.delete(true, true, null);
		assertDoesNotExistInFileSystem(file);
		assertDoesNotExistInWorkspace(file);

		// full save
		getWorkspace().save(true, null);

		// remove resources
		file = project.getFile("yet another file");
		file.delete(true, true, null);
		assertDoesNotExistInFileSystem(file);
		assertDoesNotExistInWorkspace(file);

		// snapshot
		getWorkspace().save(false, null);

		// remove resources
		IFolder folder = project.getFolder("a folder");
		folder.delete(true, true, null);
		assertDoesNotExistInFileSystem(folder);
		assertDoesNotExistInWorkspace(folder);

		// snapshot
		getWorkspace().save(false, null);
	}

	public void testChangeProject2() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject(PROJECT_2);
		assertTrue("0.1", project.exists());
		assertTrue("0.2", project.isOpen());

		// remove project
		project.delete(true, true, null);
		assertTrue("1.1", !project.exists());

		// snapshot
		getWorkspace().save(false, null);
	}

	public void testVerifyPreviousSession() throws CoreException {
		// MyProject
		IProject project = getWorkspace().getRoot().getProject(PROJECT_1);
		assertTrue("0.0", project.exists());
		assertTrue("0.1", project.isOpen());

		// verify existence of children
		IResource[] resources = buildResources(project, Snapshot3Test.defineHierarchy1());
		assertExistsInFileSystem(resources);
		assertExistsInWorkspace(resources);

		// Project2
		project = getWorkspace().getRoot().getProject(PROJECT_2);
		assertTrue("3.0", project.exists());
		assertTrue("3.1", project.isOpen());

		assertEquals("4.0", 4, project.members().length);
		assertNotNull("4.1", project.findMember(IProjectDescription.DESCRIPTION_FILE_NAME));

		// verify existence of children
		resources = buildResources(project, Snapshot3Test.defineHierarchy2());
		assertExistsInFileSystem(resources);
		assertExistsInWorkspace(resources);
	}
}
