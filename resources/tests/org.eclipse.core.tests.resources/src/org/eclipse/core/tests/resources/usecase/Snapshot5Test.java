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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

/**
 * Only verifies previous session.
 */
public class Snapshot5Test extends SnapshotTest {
	public void testVerifyPreviousSession() throws CoreException {
		// MyProject
		IProject project = getWorkspace().getRoot().getProject(PROJECT_1);
		assertTrue("0.0", project.exists());
		assertTrue("0.1", project.isOpen());

		// verify existence of children
		IResource[] resources = buildResources(project, Snapshot4Test.defineHierarchy1());
		assertExistsInFileSystem(resources);
		assertExistsInWorkspace(resources);
		IFile file = project.getFile("added file");
		assertDoesNotExistInFileSystem(file);
		assertDoesNotExistInWorkspace(file);
		file = project.getFile("yet another file");
		assertDoesNotExistInFileSystem(file);
		assertDoesNotExistInWorkspace(file);
		IFolder folder = project.getFolder("a folder");
		assertDoesNotExistInFileSystem(folder);
		assertDoesNotExistInWorkspace(folder);

		// Project2
		project = getWorkspace().getRoot().getProject(PROJECT_2);
		assertTrue("3.0", !project.exists());
	}

}
