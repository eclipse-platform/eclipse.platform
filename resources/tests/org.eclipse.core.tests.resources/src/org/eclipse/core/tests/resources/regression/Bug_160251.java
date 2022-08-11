/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources.regression;

import org.eclipse.core.filesystem.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.ResourceTest;

/**
 * Tests regression of bug 160251.  In this case, attempting to move a project
 * to an existing directory on disk failed, but should succeed as long as
 * the destination directory is empty.
 */
public class Bug_160251 extends ResourceTest {
	/**
	 * The destination directory does not exist.
	 */
	public void testNonExistentDestination() {
		IProject source = getWorkspace().getRoot().getProject("project");
		IFile sourceFile = source.getFile("Important.txt");
		IFileStore destination = getTempStore();
		IFileStore destinationFile = destination.getChild(sourceFile.getName());
		ensureExistsInWorkspace(source, true);
		ensureExistsInWorkspace(sourceFile, true);

		//move the project (should succeed)
		try {
			IProjectDescription description = source.getDescription();
			description.setLocationURI(destination.toURI());
			source.move(description, IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}
		//ensure project still exists
		assertTrue("2.0", source.exists());
		assertTrue("2.1", sourceFile.exists());
		assertTrue("2.2", source.isSynchronized(IResource.DEPTH_INFINITE));
		assertTrue("2.3", URIUtil.equals(source.getLocationURI(), destination.toURI()));
		assertTrue("2.4", URIUtil.equals(sourceFile.getLocationURI(), destinationFile.toURI()));
	}

	/**
	 * The destination directory exists, but is empty
	 */
	public void testEmptyDestination() {
		IProject source = getWorkspace().getRoot().getProject("project");
		IFile sourceFile = source.getFile("Important.txt");
		IFileStore destination = getTempStore();
		IFileStore destinationFile = destination.getChild(sourceFile.getName());
		ensureExistsInWorkspace(source, true);
		ensureExistsInWorkspace(sourceFile, true);
		try {
			destination.mkdir(EFS.NONE, getMonitor());
		} catch (CoreException e) {
			fail("0.99", e);
		}

		//move the project (should succeed)
		try {
			IProjectDescription description = source.getDescription();
			description.setLocationURI(destination.toURI());
			source.move(description, IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}
		//ensure project still exists
		assertTrue("2.0", source.exists());
		assertTrue("2.1", sourceFile.exists());
		assertTrue("2.2", source.isSynchronized(IResource.DEPTH_INFINITE));
		assertTrue("2.3", URIUtil.equals(source.getLocationURI(), destination.toURI()));
		assertTrue("2.4", URIUtil.equals(sourceFile.getLocationURI(), destinationFile.toURI()));
	}

	/**
	 * The destination directory exists, and contains an overlapping file. This should fail.
	 */
	public void testOccupiedDestination() {
		IProject source = getWorkspace().getRoot().getProject("project");
		IFile sourceFile = source.getFile("Important.txt");
		IFileStore destination = getTempStore();
		IFileStore destinationFile = destination.getChild(sourceFile.getName());
		ensureExistsInWorkspace(source, true);
		ensureExistsInWorkspace(sourceFile, true);
		try {
			destination.mkdir(EFS.NONE, getMonitor());
			createFileInFileSystem(destinationFile, getRandomContents());
		} catch (CoreException e) {
			fail("0.99", e);
		}

		//move the project (should fail)
		try {
			IProjectDescription description = source.getDescription();
			description.setLocationURI(destination.toURI());
			source.move(description, IResource.NONE, getMonitor());
			fail("1.0");
		} catch (CoreException e) {
			//should fail
		}
		//ensure project still exists in old location
		assertTrue("2.0", source.exists());
		assertTrue("2.1", sourceFile.exists());
		assertTrue("2.2", source.isSynchronized(IResource.DEPTH_INFINITE));
		assertTrue("2.3", !URIUtil.equals(source.getLocationURI(), destination.toURI()));
		assertTrue("2.4", !URIUtil.equals(sourceFile.getLocationURI(), destinationFile.toURI()));
	}
}
