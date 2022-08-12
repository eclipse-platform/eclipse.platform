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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;

/**
 * Only verifies previous session.
 */
public class Snapshot5Test extends SnapshotTest {
	public void testVerifyPreviousSession() {
		// MyProject
		IProject project = getWorkspace().getRoot().getProject(PROJECT_1);
		assertTrue("0.0", project.exists());
		assertTrue("0.1", project.isOpen());

		// verify existence of children
		IResource[] resources = buildResources(project, Snapshot4Test.defineHierarchy1());
		assertExistsInFileSystem("2.1", resources);
		assertExistsInWorkspace("2.2", resources);
		IFile file = project.getFile("added file");
		assertDoesNotExistInFileSystem("2.3", file);
		assertDoesNotExistInWorkspace("2.4", file);
		file = project.getFile("yet another file");
		assertDoesNotExistInFileSystem("2.5", file);
		assertDoesNotExistInWorkspace("2.6", file);
		IFolder folder = project.getFolder("a folder");
		assertDoesNotExistInFileSystem("2.7", folder);
		assertDoesNotExistInWorkspace("2.8", folder);

		// Project2
		project = getWorkspace().getRoot().getProject(PROJECT_2);
		assertTrue("3.0", !project.exists());
	}

	public void cleanUp() {
		try {
			ensureDoesNotExistInWorkspace(getWorkspace().getRoot());
			getWorkspace().save(true, null);
		} catch (CoreException e) {
			fail("1.0", e);
		}
	}
}
