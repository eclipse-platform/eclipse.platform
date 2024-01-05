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
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInputStream;
import static org.eclipse.core.tests.resources.usecase.IResourceTestUtil.FILE;
import static org.eclipse.core.tests.resources.usecase.IResourceTestUtil.FOLDER;
import static org.eclipse.core.tests.resources.usecase.IResourceTestUtil.PROJECT;
import static org.eclipse.core.tests.resources.usecase.IResourceTestUtil.Q_NAME_SESSION;
import static org.eclipse.core.tests.resources.usecase.IResourceTestUtil.STRING_VALUE;
import static org.eclipse.core.tests.resources.usecase.IResourceTestUtil.commonFailureTestsForResource;
import static org.eclipse.core.tests.resources.usecase.IResourceTestUtil.isLocal;

import org.eclipse.core.resources.IFile;
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

public class IFileTest {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	/**
	 * Tests failure on get/set methods invoked on a nonexistent file.
	 * Get methods either throw an exception or return null (abnormally).
	 * Set methods throw an exception.
	 */
	protected void nonexistentFileFailureTests(IFile file, IWorkspace wb) {
		/* Tests for failure in get/set methods in IResource. */
		assertThat(file).matches(it -> !isLocal(it, IResource.DEPTH_ZERO), "is not local")
				.matches(it -> !isLocal(it, IResource.DEPTH_ONE), "is not local with direct children")
				.matches(it -> !isLocal(it, IResource.DEPTH_INFINITE), "is not local with all children");
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
	@Test
	public void testFile() throws CoreException {
		IProgressMonitor monitor = null;
		IWorkspace workspace = getWorkspace();

		// Create & open a project
		IProject proj = workspace.getRoot().getProject(PROJECT);
		proj.create(monitor);
		proj.open(monitor);

		// Construct a folder handle without creating the folder.
		IFolder folder = proj.getFolder(IPath.fromOSString(FOLDER));

		// Construct a file handle
		IFile file = folder.getFile(IPath.fromOSString(FILE));

		// Inspection methods with meaningful results invoked on a handle for a nonexistent folder.
		assertThat(file).matches(not(IFile::exists), "does not exist");
		assertThat(file.getWorkspace()).isEqualTo(workspace);
		assertThat(file.getProject()).isEqualTo(proj);
		assertThat(file.getParent()).isEqualTo(folder);
		assertThat(file.getType()).isEqualTo(IResource.FILE);
		assertThat(file.getFullPath()).isEqualTo(IPath.fromOSString("/" + PROJECT + "/" + FOLDER + "/" + FILE));
		assertThat(file.getName()).isEqualTo(FILE);
		assertThat(proj.getFolder(IPath.fromOSString(FOLDER))).isEqualTo(folder);
		assertThat(workspace.getRoot().getFile(file.getFullPath())).isEqualTo(file);
		IPath projRelativePath = IPath.fromOSString(FOLDER + "/" + FILE);
		assertThat(proj.getFile(projRelativePath)).isEqualTo(file);
		assertThat(folder.getFile(IPath.fromOSString(FILE))).isEqualTo(file);
		assertThat(file).matches(it -> !workspace.getRoot().exists(it.getFullPath()), "is not contained in workspace");
		IPath absolutePath = IPath.fromOSString(proj.getLocation().toOSString() + "/" + FOLDER + "/" + FILE);
		assertThat(file.getLocation()).isEqualTo(absolutePath);
		assertThat(file.getProjectRelativePath()).isEqualTo(IPath.fromOSString(FOLDER + "/" + FILE));

		// Create a folder.
		folder.create(false, true, monitor);

		// Parent folder must exist for this.
		assertThat(workspace.getRoot().findMember(file.getFullPath())).isNull();

		// These tests produce failure because the file does not exist yet.
		nonexistentFileFailureTests(file, workspace);

		// Create the file
		file.create(createInputStream("0123456789"), false, monitor);

		// Now tests pass that require that the file exists.
		assertThat(file).matches(IFile::exists, "exists")
				.matches(it -> workspace.getRoot().exists(it.getFullPath()), "is contained in workspace")
				.matches(it -> folder.findMember(it.getName()).exists(), "is contained in folder: " + folder)
				.isEqualTo(workspace.getRoot().findMember(file.getFullPath()));
		assertThat(file.getLocation()).isEqualTo(absolutePath);

		/* Session Property */
		assertThat(file.getSessionProperty(Q_NAME_SESSION)).isNull();
		file.setSessionProperty(Q_NAME_SESSION, STRING_VALUE);
		assertThat(file.getSessionProperty(Q_NAME_SESSION)).isEqualTo(STRING_VALUE);
		file.setSessionProperty(Q_NAME_SESSION, null);
		assertThat(file.getSessionProperty(Q_NAME_SESSION)).isNull();

		// IResource.isLocal(int)
		// There is no server (yet) so everything should be local.
		assertThat(file).matches(it -> isLocal(it, IResource.DEPTH_ZERO), "is locally available")
				// No kids, but it should still answer yes.
				.matches(it -> isLocal(it, IResource.DEPTH_ONE), "is locally available with direct children")
				.matches(it -> isLocal(it, IResource.DEPTH_INFINITE), "is locally available with all children");
		// These guys have kids.
		assertThat(proj).matches(it -> isLocal(it, IResource.DEPTH_ZERO), "is locally available");
		assertThat(folder).matches(it -> isLocal(it, IResource.DEPTH_ONE), "is locally available with direct children")
				.matches(it -> isLocal(it, IResource.DEPTH_INFINITE), "is locally available with all children");

		// Delete the file
		file.delete(false, monitor);
		assertThat(file).matches(not(IFile::exists), "does not exist")
				.matches(it -> !workspace.getRoot().exists(it.getFullPath()), "is not contained in workspace");
		assertThat(folder.members()).isEmpty();
		assertThat(workspace.getRoot().findMember(file.getFullPath())).isNull();
		assertThat(file.getLocation()).isEqualTo(absolutePath);

		// These tests produce failure because the file no longer exists.
		nonexistentFileFailureTests(file, workspace);
	}

}
