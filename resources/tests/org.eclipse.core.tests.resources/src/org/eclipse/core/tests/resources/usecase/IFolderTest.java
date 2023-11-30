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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThrows;

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
	protected void nonexistentFolderFailureTests(IFolder folder, IContainer parent, IWorkspace wb) throws CoreException {
		/* Tests for failure in get/set methods in IResource. */
		commonFailureTestsForResource(folder, false);
		assertThat(parent.findMember(folder.getName()), is(nullValue()));

		IResource[] members = parent.members();
		for (IResource member : members) {
			assertThat(member.getName(), not(is(folder.getName())));
		}
		assertThat("folder at path '" + folder.getFullPath() + "' unexpectedly exists in workspace",
				!wb.getRoot().exists(folder.getFullPath()));
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
	public void testFolder() throws CoreException {
		IProgressMonitor monitor = null;
		IWorkspace workspace = getWorkspace();

		// Construct a project handle.
		IProject proj = workspace.getRoot().getProject(PROJECT);
		// Construct a folder handle
		IPath path = IPath.fromOSString(FOLDER);

		// Inspection methods with meaningful results invoked on a handle for a
		// nonexistent folder
		// in a nonexistent project.
		IFolder folder = proj.getFolder(path);
		assertThat("folder does not exist: " + folder, !folder.exists());
		assertThat(folder.getWorkspace(), is(workspace));
		assertThat(folder.getProject(), is(proj));
		assertThat(folder.getType(), is(IResource.FOLDER));
		assertThat(folder.getFullPath(), is(IPath.fromOSString("/" + PROJECT + "/" + FOLDER)));
		assertThat(folder.getName(), is(FOLDER));
		assertThat(workspace.getRoot().getFolder(folder.getFullPath()), is(folder));
		assertThat(proj.getFolder(path), is(folder));
		assertThat(folder.getParent(), is(proj));
		assertThat(folder.getProjectRelativePath(), is(IPath.fromOSString(FOLDER)));

		// Create a project without opening it.
		proj.create(monitor);

		// These tests produce failure because the project is not open yet.
		unopenedProjectFailureTests(folder, proj, workspace);

		// Open project.
		proj.open(monitor);

		// These tests produce failure because the folder does not exist yet.
		nonexistentFolderFailureTests(folder, proj, workspace);
		IPath absolutePath = IPath.fromOSString(proj.getLocation().toOSString() + "/" + FOLDER);
		assertThat(folder.getLocation(), is(absolutePath));

		// Now create folder.
		folder.create(false, true, monitor);

		// The tests that failed above (becaues the folder must exist) now pass.
		assertThat("folder does not exist: " + folder, folder.exists());
		assertThat("no member exists at path '" + folder.getFullPath() + "' in workspace",
				workspace.getRoot().findMember(folder.getFullPath()).exists());
		assertThat(workspace.getRoot().findMember(folder.getFullPath()), is(folder));
		assertThat("folder at path '" + folder.getFullPath() + "' does not exist in workspace",
				workspace.getRoot().exists(folder.getFullPath()));
		assertThat(folder.getLocation(), is(absolutePath));

		// Session Property
		assertThat(folder.getSessionProperty(Q_NAME_SESSION), is(nullValue()));
		folder.setSessionProperty(Q_NAME_SESSION, STRING_VALUE);
		assertThat(folder.getSessionProperty(Q_NAME_SESSION), is(STRING_VALUE));
		folder.setSessionProperty(Q_NAME_SESSION, null);
		assertThat(folder.getSessionProperty(Q_NAME_SESSION), is(nullValue()));

		// IResource.isLocal(int)
		// There is no server (yet) so everything should be local.
		assertThat("folder is not available locally: " + folder, isLocal(folder, IResource.DEPTH_ZERO));
		// No kids, but it should still answer yes.
		assertThat("folder and its direct children are not available locally: " + folder,
				isLocal(folder, IResource.DEPTH_ONE));
		assertThat("folder and all its children are not available locally: " + folder,
				isLocal(folder, IResource.DEPTH_INFINITE));
		// These guys have kids.
		assertThat("project and direct children are not available locally: " + proj,
				isLocal(proj, IResource.DEPTH_ONE));
		assertThat("project and all its children are not available locally: " + proj,
				isLocal(proj, IResource.DEPTH_INFINITE));

		// Construct a nested folder handle.
		IFolder nestedFolder = getWorkspace().getRoot().getFolder(folder.getFullPath().append(FOLDER));

		// Inspection methods with meaningful results invoked on a handle for a
		// nonexistent folder.
		assertThat("nested folder exists unexpectedly: " + nestedFolder, !nestedFolder.exists());
		assertThat(nestedFolder.getWorkspace(), is(workspace));
		assertThat(nestedFolder.getProject(), is(proj));
		assertThat(nestedFolder.getType(), is(IResource.FOLDER));
		assertThat(nestedFolder.getFullPath(), is(IPath.fromOSString("/" + PROJECT + "/" + FOLDER + "/" + FOLDER)));
		assertThat(nestedFolder.getName(), is(FOLDER));
		assertThat(workspace.getRoot().getFolder(nestedFolder.getFullPath()), is(nestedFolder));
		IPath projRelativePath = IPath.fromOSString(FOLDER + "/" + FOLDER);
		assertThat(proj.getFolder(projRelativePath), is(nestedFolder));
		assertThat(nestedFolder.getParent(), is(folder));
		assertThat(nestedFolder.getProjectRelativePath(), is(IPath.fromOSString(FOLDER + "/" + FOLDER)));
		// Now the parent folder has a kid.
		assertThat("folder and its direct children are not available locally: " + folder,
				isLocal(folder, IResource.DEPTH_ONE));
		assertThat("folder and all its children are not available locally: " + folder,
				isLocal(folder, IResource.DEPTH_INFINITE));

		// These tests produce failure because the nested folder does not exist yet.
		nonexistentFolderFailureTests(nestedFolder, folder, workspace);

		// Create the nested folder.
		nestedFolder.create(false, true, monitor);

		// The tests that failed above (becaues the folder must exist) now pass.
		assertThat("nested folder at path '" + nestedFolder.getFullPath() + "' does not exist in workspace",
				workspace.getRoot().exists(nestedFolder.getFullPath()));
		assertThat("nested folder does not exist: " + nestedFolder, nestedFolder.exists());
		assertThat("no folder with name '" + nestedFolder.getName() + "' exists in folder: " + folder,
				folder.findMember(nestedFolder.getName()).exists());
		assertThat(workspace.getRoot().findMember(nestedFolder.getFullPath()), is(nestedFolder));
		assertThat("no nested folder at path '" + nestedFolder.getFullPath() + "' exists in workspace",
				workspace.getRoot().exists(nestedFolder.getFullPath()));
		assertThat(nestedFolder.getLocation(), is(absolutePath.append(FOLDER)));

		// Delete the nested folder
		nestedFolder.delete(false, monitor);
		assertThat("nested folder exists unexpectedly: " + nestedFolder, !nestedFolder.exists());
		assertThat(folder.members(), arrayWithSize(0));
		assertThat(workspace.getRoot().findMember(nestedFolder.getFullPath()), is(nullValue()));
		assertThat("nested folder at path '" + nestedFolder.getFullPath() + "' unexpectedly exists in workspace",
				!workspace.getRoot().exists(nestedFolder.getFullPath()));
		assertThat(nestedFolder.getLocation(), is(absolutePath.append(FOLDER)));

		// These tests produce failure because the nested folder no longer exists.
		nonexistentFolderFailureTests(nestedFolder, folder, workspace);

		// Parent is still there.
		assertThat("folder does not exist: " + folder, folder.exists());
		assertThat("no member at path '" + folder.getFullPath() + "' exists in workspace",
				workspace.getRoot().findMember(folder.getFullPath()).exists());
		assertThat(workspace.getRoot().findMember(folder.getFullPath()), is(folder));
		assertThat("no folder at path '" + folder.getFullPath() + "' exists in workspace",
				workspace.getRoot().exists(folder.getFullPath()));
		assertThat(folder.getLocation(), is(absolutePath));

		// Delete the parent folder
		folder.delete(false, monitor);
		assertThat("folder exists unexpectedly: " + folder, !folder.exists());
		assertThat(workspace.getRoot().findMember(folder.getFullPath()), is(nullValue()));
		assertThat("folder at path '" + folder.getFullPath() + "' unexpectedly exists in workspace",
				!workspace.getRoot().exists(folder.getFullPath()));
		assertThat(folder.getLocation(), is(absolutePath));

		// These tests produce failure because the parent folder no longer exists.
		nonexistentFolderFailureTests(folder, proj, workspace);
	}

	/**
	 * Tests failure on get/set methods invoked on a nonexistent folder.
	 * Get methods either throw an exception or return null (abnormally).
	 * Set methods throw an exception.
	 */
	protected void unopenedProjectFailureTests(IFolder folder, IContainer parent, IWorkspace wb) {
		/* Try creating a folder in a project which is not yet open. */
		assertThrows(CoreException.class, () -> folder.create(false, true, null));
		assertThat("folder at path '" + folder.getFullPath() + "' unexpectedly exists in workspace",
				!wb.getRoot().exists(folder.getFullPath()));
	}
}
