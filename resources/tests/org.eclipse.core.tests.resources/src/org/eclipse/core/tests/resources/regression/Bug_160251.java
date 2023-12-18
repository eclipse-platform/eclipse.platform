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

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests regression of bug 160251.  In this case, attempting to move a project
 * to an existing directory on disk failed, but should succeed as long as
 * the destination directory is empty.
 */
public class Bug_160251 {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	/**
	 * The destination directory does not exist.
	 */
	@Test
	public void testNonExistentDestination() throws CoreException {
		IProject source = getWorkspace().getRoot().getProject("project");
		IFile sourceFile = source.getFile("Important.txt");
		IFileStore destination = workspaceRule.getTempStore();
		IFileStore destinationFile = destination.getChild(sourceFile.getName());
		createInWorkspace(source);
		createInWorkspace(sourceFile);

		//move the project (should succeed)
		IProjectDescription description = source.getDescription();
		description.setLocationURI(destination.toURI());
		source.move(description, IResource.NONE, createTestMonitor());

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
	@Test
	public void testEmptyDestination() throws CoreException {
		IProject source = getWorkspace().getRoot().getProject("project");
		IFile sourceFile = source.getFile("Important.txt");
		IFileStore destination = workspaceRule.getTempStore();
		IFileStore destinationFile = destination.getChild(sourceFile.getName());
		createInWorkspace(source);
		createInWorkspace(sourceFile);
		destination.mkdir(EFS.NONE, createTestMonitor());

		//move the project (should succeed)
		IProjectDescription description = source.getDescription();
		description.setLocationURI(destination.toURI());
		source.move(description, IResource.NONE, createTestMonitor());

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
	@Test
	public void testOccupiedDestination() throws Exception {
		IProject source = getWorkspace().getRoot().getProject("project");
		IFile sourceFile = source.getFile("Important.txt");
		IFileStore destination = workspaceRule.getTempStore();
		IFileStore destinationFile = destination.getChild(sourceFile.getName());
		createInWorkspace(source);
		createInWorkspace(sourceFile);
		destination.mkdir(EFS.NONE, createTestMonitor());
		createInFileSystem(destinationFile);

		//move the project (should fail)
		IProjectDescription description = source.getDescription();
		description.setLocationURI(destination.toURI());
		assertThrows(CoreException.class, () -> source.move(description, IResource.NONE, createTestMonitor()));

		//ensure project still exists in old location
		assertTrue("2.0", source.exists());
		assertTrue("2.1", sourceFile.exists());
		assertTrue("2.2", source.isSynchronized(IResource.DEPTH_INFINITE));
		assertTrue("2.3", !URIUtil.equals(source.getLocationURI(), destination.toURI()));
		assertTrue("2.4", !URIUtil.equals(sourceFile.getLocationURI(), destinationFile.toURI()));
	}

}
