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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

public class IFileTest extends IResourceTest {

	/**
	 * Tests failure on get/set methods invoked on a nonexistent file.
	 * Get methods either throw an exception or return null (abnormally).
	 * Set methods throw an exception.
	 */
	protected void nonexistentFileFailureTests(IFile file, IFolder parent, IWorkspace wb) {
		String method = "nonexistentFileFailureTests(IFolder,IWorkspace)";

		/* Tests for failure in get/set methods in IResource. */

		try {
			if (file.isLocal(IResource.DEPTH_ZERO)) {
				fail(method + "1.1");
			}
		} catch (RuntimeException e) {
		}
		try {
			if (file.isLocal(IResource.DEPTH_ONE)) {
				fail(method + "1.2");
			}
		} catch (RuntimeException e) {
		}
		try {
			if (file.isLocal(IResource.DEPTH_INFINITE)) {
				fail(method + "1.3");
			}
		} catch (RuntimeException e) {
		}
		commonFailureTestsForResource(file, false);

	}

	/**
	 * Create a folder in an open project.
	 * Contruct a file handle "in" the folder &amp; check its initial state.
	 * Create the file &amp; check its state further.
	 * Delete the file.
	 *
	 * TBD:
	 *
	 * Test file created over already existing file (failure).
	 * Test file created "below" another file (ie. fail since its parent should be a directory).
	 * Test deleting file that doesn't exist (failure?).
	 * Finish testing IResource API
	 */
	public void testFile() {
		IProgressMonitor monitor = null;
		IWorkspace workspace = getWorkspace();

		// Create & open a project
		IProject proj = workspace.getRoot().getProject(PROJECT);
		try {
			proj.create(monitor);
		} catch (CoreException e) {
			fail("2.1", e);
		}
		try {
			proj.open(monitor);
		} catch (CoreException e) {
			fail("2.2", e);
		}

		// Construct a folder handle without creating the folder.
		IFolder folder = proj.getFolder(IPath.fromOSString(FOLDER));

		// Construct a file handle
		IFile file = folder.getFile(IPath.fromOSString(FILE));

		// Inspection methods with meaninful results invoked on a handle for a nonexistent folder.
		assertTrue("3.1", !file.exists());
		assertTrue("3.2", file.getWorkspace().equals(workspace));
		assertTrue("3.4", file.getProject().equals(proj));
		assertTrue("3.5", file.getParent().equals(folder));
		assertTrue("3.5", file.getType() == IResource.FILE);
		assertTrue("3.6", file.getFullPath().equals(IPath.fromOSString("/" + PROJECT + "/" + FOLDER + "/" + FILE)));
		assertTrue("3.7", file.getName().equals(FILE));
		assertTrue("3.8", proj.getFolder(IPath.fromOSString(FOLDER)).equals(folder));
		assertTrue("3.9", workspace.getRoot().getFile(file.getFullPath()).equals(file));
		IPath projRelativePath = IPath.fromOSString(FOLDER + "/" + FILE);
		assertTrue("3.11", proj.getFile(projRelativePath).equals(file));
		assertTrue("3.12", folder.getFile(IPath.fromOSString(FILE)).equals(file));
		assertTrue("3.13", !workspace.getRoot().exists(file.getFullPath()));
		IPath absolutePath = IPath.fromOSString(proj.getLocation().toOSString() + "/" + FOLDER + "/" + FILE);
		assertTrue("3.14", file.getLocation().equals(absolutePath));
		assertTrue("3.15", file.getProjectRelativePath().equals(IPath.fromOSString(FOLDER + "/" + FILE)));

		// Create a folder.
		try {
			folder.create(false, true, monitor);
		} catch (CoreException e) {
			fail("4", e);
		}

		// Parent folder must exist for this.
		assertTrue("5", workspace.getRoot().findMember(file.getFullPath()) == null);

		// These tests produce failure because the file does not exist yet.
		nonexistentFileFailureTests(file, folder, workspace);

		// Create the file
		try {
			file.create(getContents("0123456789"), false, monitor);
		} catch (CoreException e) {
			fail("6", e);
		}

		// Now tests pass that require that the file exists.
		assertTrue("7.0", file.exists());
		assertTrue("7.1", folder.findMember(file.getName()).exists());
		assertTrue("7.3", workspace.getRoot().findMember(file.getFullPath()).equals(file));
		assertTrue("7.4", workspace.getRoot().exists(file.getFullPath()));
		assertTrue("7.5", file.getLocation().equals(absolutePath));

		/* Session Property */

		try {
			assertTrue("8.0", file.getSessionProperty(Q_NAME_SESSION) == null);
		} catch (CoreException e) {
			assertTrue("8.1", false);
		}
		try {
			file.setSessionProperty(Q_NAME_SESSION, STRING_VALUE);
		} catch (CoreException e) {
			assertTrue("8.2", false);
		}
		try {
			assertTrue("8.2", file.getSessionProperty(Q_NAME_SESSION).equals(STRING_VALUE));
		} catch (CoreException e) {
			assertTrue("8.3", false);
		}

		try {
			file.setSessionProperty(Q_NAME_SESSION, null);
		} catch (CoreException e) {
			assertTrue("8.4", false);
		}
		try {
			assertTrue("8.5", file.getSessionProperty(Q_NAME_SESSION) == null);
		} catch (CoreException e) {
			assertTrue("8.6", false);
		}

		// IResource.isLocal(int)
		// There is no server (yet) so everything should be local.
		assertTrue("9.0", file.isLocal(IResource.DEPTH_ZERO));
		// No kids, but it should still answer yes.
		assertTrue("9.1", file.isLocal(IResource.DEPTH_ONE));
		assertTrue("9.2", file.isLocal(IResource.DEPTH_INFINITE));
		// These guys have kids.
		assertTrue("9.3", proj.isLocal(IResource.DEPTH_INFINITE));
		assertTrue("9.5", folder.isLocal(IResource.DEPTH_ONE));
		assertTrue("9.6", folder.isLocal(IResource.DEPTH_INFINITE));

		// Delete the file
		try {
			file.delete(false, monitor);
		} catch (CoreException e) {
			fail("10.0", e);
		}
		assertTrue("11.1", !file.exists());
		try {
			assertTrue("11.2", folder.members().length == 0);
		} catch (CoreException e) {
			assertTrue("11.3", false);
		}
		assertTrue("11.4", workspace.getRoot().findMember(file.getFullPath()) == null);
		assertTrue("11.5", !workspace.getRoot().exists(file.getFullPath()));
		assertTrue("11.6", file.getLocation().equals(absolutePath));

		// These tests produce failure because the file no longer exists.
		nonexistentFileFailureTests(file, folder, workspace);

		/* remove garbage */
		try {
			proj.delete(true, getMonitor());
		} catch (CoreException e) {
			fail("12.0", e);
		}
	}
}
