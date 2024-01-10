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

import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.usecase.IResourceTestUtil.FOLDER;
import static org.eclipse.core.tests.resources.usecase.IResourceTestUtil.PROJECT;
import static org.eclipse.core.tests.resources.usecase.IResourceTestUtil.Q_NAME_SESSION;
import static org.eclipse.core.tests.resources.usecase.IResourceTestUtil.STRING_VALUE;
import static org.eclipse.core.tests.resources.usecase.IResourceTestUtil.commonFailureTestsForResource;
import static org.eclipse.core.tests.resources.usecase.IResourceTestUtil.isLocal;
import static org.junit.Assert.assertThrows;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.Rule;
import org.junit.Test;

public class IFolderTest {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	/**
	 * Tests failure on get/set methods invoked on a nonexistent folder.
	 * Get methods either throw an exception or return null (abnormally).
	 * Set methods throw an exception.
	 */
	protected void nonexistentFolderFailureTests(IFolder folder, IContainer parent, IWorkspace workspace)
			throws CoreException {
		/* Tests for failure in get/set methods in IResource. */
		commonFailureTestsForResource(folder, false);
		assertThat(parent.findMember(folder.getName())).isNull();

		IResource[] members = parent.members();
		for (IResource member : members) {
			assertThat(member.getName()).isNotEqualTo(folder.getName());
		}
		assertThat(folder).matches(it -> !workspace.getRoot().exists(it.getFullPath()),
				"is not contained in workspace");
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
	@Test
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
		assertThat(folder).matches(not(IFolder::exists), "does not exist");
		assertThat(folder.getWorkspace()).isEqualTo(workspace);
		assertThat(folder.getProject()).isEqualTo(proj);
		assertThat(folder.getType()).isEqualTo(IResource.FOLDER);
		assertThat(folder.getFullPath()).isEqualTo(IPath.fromOSString("/" + PROJECT + "/" + FOLDER));
		assertThat(folder.getName()).isEqualTo(FOLDER);
		assertThat(workspace.getRoot().getFolder(folder.getFullPath())).isEqualTo(folder);
		assertThat(proj.getFolder(path)).isEqualTo(folder);
		assertThat(folder.getParent()).isEqualTo(proj);
		assertThat(folder.getProjectRelativePath()).isEqualTo(IPath.fromOSString(FOLDER));

		// Create a project without opening it.
		proj.create(monitor);

		// These tests produce failure because the project is not open yet.
		unopenedProjectFailureTests(folder, proj, workspace);

		// Open project.
		proj.open(monitor);

		// These tests produce failure because the folder does not exist yet.
		nonexistentFolderFailureTests(folder, proj, workspace);
		IPath absolutePath = IPath.fromOSString(proj.getLocation().toOSString() + "/" + FOLDER);
		assertThat(folder.getLocation()).isEqualTo(absolutePath);

		// Now create folder.
		folder.create(false, true, monitor);

		// The tests that failed above (becaues the folder must exist) now pass.
		assertThat(folder).matches(IFolder::exists, "exists")
				.matches(it -> workspace.getRoot().exists(it.getFullPath()), "is contained in workspace")
				.matches(it -> workspace.getRoot().findMember(it.getFullPath()).exists(),
						"is found existing in workspace")
				.isEqualTo(workspace.getRoot().findMember(folder.getFullPath()));
		assertThat(folder.getLocation()).isEqualTo(absolutePath);

		// Session Property
		assertThat(folder.getSessionProperty(Q_NAME_SESSION)).isNull();
		folder.setSessionProperty(Q_NAME_SESSION, STRING_VALUE);
		assertThat(folder.getSessionProperty(Q_NAME_SESSION)).isEqualTo(STRING_VALUE);
		folder.setSessionProperty(Q_NAME_SESSION, null);
		assertThat(folder.getSessionProperty(Q_NAME_SESSION)).isNull();

		// IResource.isLocal(int)
		// There is no server (yet) so everything should be local.
		assertThat(folder).matches(it -> isLocal(it, IResource.DEPTH_ZERO), "is locally available")
				// No kids, but it should still answer yes.
				.matches(it -> isLocal(it, IResource.DEPTH_ONE), "is locally available with direct children")
				.matches(it -> isLocal(it, IResource.DEPTH_INFINITE), "is locally available with all children");
		// These guys have kids.
		assertThat(proj).matches(it -> isLocal(it, IResource.DEPTH_ONE), "is locally available with direct children")
				.matches(it -> isLocal(it, IResource.DEPTH_INFINITE), "is locally available with all children");

		// Construct a nested folder handle.
		IFolder nestedFolder = getWorkspace().getRoot().getFolder(folder.getFullPath().append(FOLDER));

		// Inspection methods with meaningful results invoked on a handle for a
		// nonexistent folder.
		assertThat(nestedFolder).matches(not(IFolder::exists), "does not exist");
		assertThat(nestedFolder.getWorkspace()).isEqualTo(workspace);
		assertThat(nestedFolder.getProject()).isEqualTo(proj);
		assertThat(nestedFolder.getType()).isEqualTo(IResource.FOLDER);
		assertThat(nestedFolder.getFullPath())
				.isEqualTo(IPath.fromOSString("/" + PROJECT + "/" + FOLDER + "/" + FOLDER));
		assertThat(nestedFolder.getName()).isEqualTo(FOLDER);
		assertThat(workspace.getRoot().getFolder(nestedFolder.getFullPath())).isEqualTo(nestedFolder);
		IPath projRelativePath = IPath.fromOSString(FOLDER + "/" + FOLDER);
		assertThat(proj.getFolder(projRelativePath)).isEqualTo(nestedFolder);
		assertThat(nestedFolder.getParent()).isEqualTo(folder);
		assertThat(nestedFolder.getProjectRelativePath()).isEqualTo(IPath.fromOSString(FOLDER + "/" + FOLDER));
		// Now the parent folder has a kid.
		assertThat(folder).matches(it -> isLocal(it, IResource.DEPTH_ONE), "is locally available with direct children")
				.matches(it -> isLocal(it, IResource.DEPTH_INFINITE), "is locally available with all children");

		// These tests produce failure because the nested folder does not exist yet.
		nonexistentFolderFailureTests(nestedFolder, folder, workspace);

		// Create the nested folder.
		nestedFolder.create(false, true, monitor);

		// The tests that failed above (becauese the folder must exist) now pass.
		assertThat(nestedFolder).matches(IFolder::exists, "exists")
				.matches(it -> folder.findMember(it.getName()).exists(), "is contained in folder: " + folder)
				.matches(it -> workspace.getRoot().exists(it.getFullPath()), "is contained in workspace")
				.matches(it -> workspace.getRoot().findMember(it.getFullPath()).exists(),
						"is found existing in workspace")
				.isEqualTo(workspace.getRoot().findMember(nestedFolder.getFullPath()));
		assertThat(nestedFolder.getLocation()).isEqualTo(absolutePath.append(FOLDER));

		// Delete the nested folder
		nestedFolder.delete(false, monitor);
		assertThat(nestedFolder).matches(not(IFolder::exists), "does not exist");
		assertThat(folder.members()).isEmpty();
		assertThat(workspace.getRoot().findMember(nestedFolder.getFullPath())).isNull();
		assertThat(nestedFolder).matches(it -> !workspace.getRoot().exists(it.getFullPath()),
				"is not contained in workspace");
		assertThat(nestedFolder.getLocation()).isEqualTo(absolutePath.append(FOLDER));

		// These tests produce failure because the nested folder no longer exists.
		nonexistentFolderFailureTests(nestedFolder, folder, workspace);

		// Parent is still there.
		assertThat(folder).matches(IFolder::exists, "exists")
				.matches(it -> workspace.getRoot().exists(it.getFullPath()), "is contained in workspace")
				.matches(it -> workspace.getRoot().findMember(it.getFullPath()).exists(),
						"is found existing in workspace")
				.isEqualTo(workspace.getRoot().findMember(folder.getFullPath()));
		assertThat(folder.getLocation()).isEqualTo(absolutePath);

		// Delete the parent folder
		folder.delete(false, monitor);
		assertThat(folder).matches(not(IFolder::exists), "does not exist")
				.matches(it -> !workspace.getRoot().exists(it.getFullPath()), "is not contained in workspace");
		assertThat(workspace.getRoot().findMember(folder.getFullPath())).isNull();
		assertThat(folder.getLocation()).isEqualTo(absolutePath);

		// These tests produce failure because the parent folder no longer exists.
		nonexistentFolderFailureTests(folder, proj, workspace);
	}

	/**
	 * Tests failure on get/set methods invoked on a nonexistent folder.
	 * Get methods either throw an exception or return null (abnormally).
	 * Set methods throw an exception.
	 */
	protected void unopenedProjectFailureTests(IFolder folder, IContainer parent, IWorkspace workspace) {
		/* Try creating a folder in a project which is not yet open. */
		assertThrows(CoreException.class, () -> folder.create(false, true, null));
		assertThat(folder).matches(it -> !workspace.getRoot().exists(it.getFullPath()),
				"is not contained in workspace");
	}

}
