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
import static org.hamcrest.Matchers.nullValue;

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
	protected void nonexistentFileFailureTests(IFile file, IWorkspace wb) {
		/* Tests for failure in get/set methods in IResource. */
		assertThat("file is unexpectedly available locally: " + file, !isLocal(file, IResource.DEPTH_ZERO));
		assertThat("file and its direct children are unexpectedly available locally: " + file,
				!isLocal(file, IResource.DEPTH_ONE));
		assertThat("file and all its children are unexpectedly available locally: " + file,
				!isLocal(file, IResource.DEPTH_INFINITE));
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

		// Inspection methods with meaninful results invoked on a handle for a nonexistent folder.
		assertThat("file does not exist: " + file, !file.exists());
		assertThat(file.getWorkspace(), is(workspace));
		assertThat(file.getProject(), is(proj));
		assertThat(file.getParent(), is(folder));
		assertThat(file.getType(), is(IResource.FILE));
		assertThat(file.getFullPath(), is(IPath.fromOSString("/" + PROJECT + "/" + FOLDER + "/" + FILE)));
		assertThat(file.getName(), is(FILE));
		assertThat(proj.getFolder(IPath.fromOSString(FOLDER)), is(folder));
		assertThat(workspace.getRoot().getFile(file.getFullPath()), is(file));
		IPath projRelativePath = IPath.fromOSString(FOLDER + "/" + FILE);
		assertThat(proj.getFile(projRelativePath), is(file));
		assertThat(folder.getFile(IPath.fromOSString(FILE)), is(file));
		assertThat("file at path '" + file.getFullPath() + "' unexpectedly exists in workspace",
				!workspace.getRoot().exists(file.getFullPath()));
		IPath absolutePath = IPath.fromOSString(proj.getLocation().toOSString() + "/" + FOLDER + "/" + FILE);
		assertThat(file.getLocation(), is(absolutePath));
		assertThat(file.getProjectRelativePath(), is(IPath.fromOSString(FOLDER + "/" + FILE)));

		// Create a folder.
		folder.create(false, true, monitor);

		// Parent folder must exist for this.
		assertThat(workspace.getRoot().findMember(file.getFullPath()), is(nullValue()));

		// These tests produce failure because the file does not exist yet.
		nonexistentFileFailureTests(file, workspace);

		// Create the file
		file.create(getContents("0123456789"), false, monitor);

		// Now tests pass that require that the file exists.
		assertThat("file does not exist: " + file, file.exists());
		assertThat("no member with name '" + file.getName() + "' exists in folder: " + folder,
				folder.findMember(file.getName()).exists());
		assertThat(workspace.getRoot().findMember(file.getFullPath()), is(file));
		assertThat("no member at path '" + file.getFullPath() + "' exists in workspace",
				workspace.getRoot().exists(file.getFullPath()));
		assertThat(file.getLocation(), is(absolutePath));

		/* Session Property */
		assertThat(file.getSessionProperty(Q_NAME_SESSION), is(nullValue()));
		file.setSessionProperty(Q_NAME_SESSION, STRING_VALUE);
		assertThat(file.getSessionProperty(Q_NAME_SESSION), is(STRING_VALUE));
		file.setSessionProperty(Q_NAME_SESSION, null);
		assertThat(file.getSessionProperty(Q_NAME_SESSION), is(nullValue()));

		// IResource.isLocal(int)
		// There is no server (yet) so everything should be local.
		assertThat("file is not available locally: " + file, isLocal(file, IResource.DEPTH_ZERO));
		// No kids, but it should still answer yes.
		assertThat("file and its direct children are not available locally: " + file,
				isLocal(file, IResource.DEPTH_ONE));
		assertThat("file and all its children are not available locally: " + file,
				isLocal(file, IResource.DEPTH_INFINITE));
		// These guys have kids.
		assertThat("project and all its children are not available locally: " + proj,
				isLocal(proj, IResource.DEPTH_INFINITE));
		assertThat("folder and direct children are not available locally: " + folder,
				isLocal(folder, IResource.DEPTH_ONE));
		assertThat("folder and all its children are not available locally: " + folder,
				isLocal(folder, IResource.DEPTH_INFINITE));

		// Delete the file
		file.delete(false, monitor);
		assertThat("file exists unexpectedly: " + file, !file.exists());
		assertThat(folder.members(), arrayWithSize(0));
		assertThat(workspace.getRoot().findMember(file.getFullPath()), is(nullValue()));
		assertThat("file at path '" + file.getFullPath() + "' unexpectedly exists in workspace",
				!workspace.getRoot().exists(file.getFullPath()));
		assertThat(file.getLocation(), is(absolutePath));

		// These tests produce failure because the file no longer exists.
		nonexistentFileFailureTests(file, workspace);
	}
}
