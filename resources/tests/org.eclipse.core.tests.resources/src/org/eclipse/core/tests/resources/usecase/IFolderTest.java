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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

public class IFolderTest extends IResourceTest {

	/**
	 * Tests failure on get/set methods invoked on a nonexistent folder.
	 * Get methods either throw an exception or return null (abnormally).
	 * Set methods throw an exception.
	 */
	protected void nonexistentFolderFailureTests(IFolder folder, IContainer parent, IWorkspace wb) {
		String method = "nonexistentFolderFailureTests(IFolder,IWorkspace)";

		/* Tests for failure in get/set methods in IResource. */
		commonFailureTestsForResource(folder, false);
		assertTrue(method + "4.0", parent.findMember(folder.getName()) == null);

		try {
			IResource[] members = parent.members();
			for (int i = 0; i < members.length; i++) {
				assertTrue("4.1: i=" + i, !members[i].getName().equals(folder.getName()));
			}
		} catch (CoreException e) {
			assertTrue(method + "4.2", false);
		}
		assertTrue(method + "5", !wb.getRoot().exists(folder.getFullPath()));
	}

	/**
	 * Create a project in an open solution. Don't open the project.
	 * Contruct a folder handle &amp; check its initial state.
	 * Try creating the folder in the unopened project.
	 * Open the project.
	 * Create the folder &amp; check its state further.
	 * Construct a nested folder handle &amp; check its initial state.
	 * Create the nested folder &amp; check its further.
	 * Delete the nested folder.
	 * Delete the parent folder.
	 * Close the workspace.
	 *
	 * TBD:
	 *
	 * Test deleting a folder that doesn't exist.
	 * Test that deleting a folder recursively deletes all children.
	 * Test deleting a folder the is in a closed project.
	 * Test IResource API
	 * Test IFolder API
	 */
	public void testFolder() {
		IProgressMonitor monitor = null;
		IWorkspace workspace = getWorkspace();

		// Construct a project handle.
		IProject proj = workspace.getRoot().getProject(PROJECT);
		// Construct a folder handle
		IPath path = IPath.fromOSString(FOLDER);

		// Inspection methods with meaninful results invoked on a handle for a nonexistent folder
		// in a nonexistent project.
		IFolder folder = proj.getFolder(path);
		assertTrue("2.1", !folder.exists());
		assertTrue("2.2", folder.getWorkspace().equals(workspace));
		assertTrue("2.4", folder.getProject().equals(proj));
		assertTrue("2.5", folder.getType() == IResource.FOLDER);
		assertTrue("2.6", folder.getFullPath().equals(IPath.fromOSString("/" + PROJECT + "/" + FOLDER)));
		assertTrue("2.7", folder.getName().equals(FOLDER));
		assertTrue("2.8", workspace.getRoot().getFolder(folder.getFullPath()).equals(folder));
		assertTrue("2.10", proj.getFolder(path).equals(folder));
		assertTrue("2.11", folder.getParent().equals(proj));
		assertTrue("2.13", folder.getProjectRelativePath().equals(IPath.fromOSString(FOLDER)));

		// Create a project without opening it.
		try {
			proj.create(monitor);
		} catch (CoreException e) {
			fail("3", e);
		}

		// These tests produce failure because the project is not open yet.
		unopenedProjectFailureTests(folder, proj, workspace);

		// Open project.
		try {
			proj.open(monitor);
		} catch (CoreException e) {
			fail("4", e);
		}

		// These tests produce failure because the folder does not exist yet.
		nonexistentFolderFailureTests(folder, proj, workspace);
		IPath absolutePath = IPath.fromOSString(proj.getLocation().toOSString() + "/" + FOLDER);
		assertTrue("5", folder.getLocation().equals(absolutePath));

		// Now create folder.
		try {
			folder.create(false, true, monitor);
		} catch (CoreException e) {
			fail("6", e);
		}

		// The tests that failed above (becaues the folder must exist) now pass.
		assertTrue("7.0", folder.exists());
		assertTrue("7.1", workspace.getRoot().findMember(folder.getFullPath()).exists());
		assertTrue("7.3", workspace.getRoot().findMember(folder.getFullPath()).equals(folder));
		assertTrue("7.4", workspace.getRoot().exists(folder.getFullPath()));
		assertTrue("7.5", folder.getLocation().equals(absolutePath));

		// Session Property
		try {
			assertTrue("8.0", folder.getSessionProperty(Q_NAME_SESSION) == null);
		} catch (CoreException e) {
			fail("8.1");
		}
		try {
			folder.setSessionProperty(Q_NAME_SESSION, STRING_VALUE);
		} catch (CoreException e) {
			fail("8.2");
		}
		try {
			assertTrue("8.3", folder.getSessionProperty(Q_NAME_SESSION).equals(STRING_VALUE));
		} catch (CoreException e) {
			fail("8.4");
		}
		try {
			folder.setSessionProperty(Q_NAME_SESSION, null);
		} catch (CoreException e) {
			fail("8.5");
		}
		try {
			assertTrue("8.6", folder.getSessionProperty(Q_NAME_SESSION) == null);
		} catch (CoreException e) {
			fail("8.7");
		}

		// IResource.isLocal(int)
		// There is no server (yet) so everything should be local.
		assertTrue("9.1", folder.isLocal(IResource.DEPTH_ZERO));
		// No kids, but it should still answer yes.
		assertTrue("9.2", folder.isLocal(IResource.DEPTH_ONE));
		assertTrue("9.3", folder.isLocal(IResource.DEPTH_INFINITE));
		// These guys have kids.
		assertTrue("9.4", proj.isLocal(IResource.DEPTH_ONE));
		assertTrue("9.5", proj.isLocal(IResource.DEPTH_INFINITE));

		// Construct a nested folder handle.
		IFolder nestedFolder = getWorkspace().getRoot().getFolder(folder.getFullPath().append(FOLDER));

		// Inspection methods with meaninful results invoked on a handle for a nonexistent folder.
		assertTrue("10.0", !nestedFolder.exists());
		assertTrue("10.1", nestedFolder.getWorkspace().equals(workspace));
		assertTrue("10.3", nestedFolder.getProject().equals(proj));
		assertTrue("10.4", nestedFolder.getType() == IResource.FOLDER);
		assertTrue("10.5", nestedFolder.getFullPath().equals(IPath.fromOSString("/" + PROJECT + "/" + FOLDER + "/" + FOLDER)));
		assertTrue("10.6", nestedFolder.getName().equals(FOLDER));
		assertTrue("10.7", workspace.getRoot().getFolder(nestedFolder.getFullPath()).equals(nestedFolder));
		IPath projRelativePath = IPath.fromOSString(FOLDER + "/" + FOLDER);
		assertTrue("10.9", proj.getFolder(projRelativePath).equals(nestedFolder));
		assertTrue("10.10", nestedFolder.getParent().equals(folder));
		assertTrue("10.11", nestedFolder.getProjectRelativePath().equals(IPath.fromOSString(FOLDER + "/" + FOLDER)));
		// Now the parent folder has a kid.
		assertTrue("10.12", folder.isLocal(IResource.DEPTH_ONE));
		assertTrue("10.13", folder.isLocal(IResource.DEPTH_INFINITE));

		// These tests produce failure because the nested folder does not exist yet.
		nonexistentFolderFailureTests(nestedFolder, folder, workspace);

		// Create the nested folder.
		try {
			nestedFolder.create(false, true, monitor);
		} catch (CoreException e) {
			fail("11", e);
		}

		// The tests that failed above (becaues the folder must exist) now pass.
		assertTrue("12.0", workspace.getRoot().exists(nestedFolder.getFullPath()));
		assertTrue("12.1", nestedFolder.exists());
		assertTrue("12.2", folder.findMember(nestedFolder.getName()).exists());
		assertTrue("12.4", workspace.getRoot().findMember(nestedFolder.getFullPath()).equals(nestedFolder));
		assertTrue("12.5", workspace.getRoot().exists(nestedFolder.getFullPath()));
		assertTrue("12.6", nestedFolder.getLocation().equals(absolutePath.append(FOLDER)));

		// Delete the nested folder
		try {
			nestedFolder.delete(false, monitor);
		} catch (CoreException e) {
			fail("13.0", e);
		}
		assertTrue("13.1", !nestedFolder.exists());
		try {
			assertTrue("13.2", folder.members().length == 0);
		} catch (CoreException e) {
			fail("13.3");
		}
		assertTrue("13.4", workspace.getRoot().findMember(nestedFolder.getFullPath()) == null);
		assertTrue("13.5", !workspace.getRoot().exists(nestedFolder.getFullPath()));
		assertTrue("13.6", nestedFolder.getLocation().equals(absolutePath.append(FOLDER)));

		// These tests produce failure because the nested folder no longer exists.
		nonexistentFolderFailureTests(nestedFolder, folder, workspace);

		// Parent is still there.
		assertTrue("14.0", folder.exists());
		assertTrue("14.1", workspace.getRoot().findMember(folder.getFullPath()).exists());
		assertTrue("14.3", workspace.getRoot().findMember(folder.getFullPath()).equals(folder));
		assertTrue("14.4", workspace.getRoot().exists(folder.getFullPath()));
		assertTrue("14.5", folder.getLocation().equals(absolutePath));

		// Delete the parent folder
		try {
			folder.delete(false, monitor);
		} catch (CoreException e) {
			fail("15.0", e);
		}
		assertTrue("15.1", !folder.exists());
		assertTrue("15.4", workspace.getRoot().findMember(folder.getFullPath()) == null);
		assertTrue("15.5", !workspace.getRoot().exists(folder.getFullPath()));
		assertTrue("15.6", folder.getLocation().equals(absolutePath));

		// These tests produce failure because the parent folder no longer exists.
		nonexistentFolderFailureTests(folder, proj, workspace);

		try {
			proj.delete(true, true, getMonitor());
		} catch (CoreException e) {
			fail("20.0", e);
		}
	}

	/**
	 * Tests failure on get/set methods invoked on a nonexistent folder.
	 * Get methods either throw an exception or return null (abnormally).
	 * Set methods throw an exception.
	 */
	protected void unopenedProjectFailureTests(IFolder folder, IContainer parent, IWorkspace wb) {
		String method = "unopenedProjectFailureTests(IFolder,IWorkspace)";
		IProgressMonitor monitor = null;

		/* Try creating a folder in a project which is not yet open. */
		try {
			folder.create(false, true, monitor);
			fail(method + "1");
		} catch (CoreException e) {
			// expected
		}
		assertTrue(method + "2", !wb.getRoot().exists(folder.getFullPath()));
	}
}
