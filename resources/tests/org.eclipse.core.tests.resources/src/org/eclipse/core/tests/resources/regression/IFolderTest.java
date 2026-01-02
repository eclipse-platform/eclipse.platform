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

import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.isReadOnlySupported;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.function.Predicate;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform.OS;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(WorkspaceResetExtension.class)
public class IFolderTest {

	/**
	 * Bug requests that if a failed folder creation occurs on Linux that we check
	 * the immediate parent to see if it is read-only so we can return a better
	 * error code and message to the user.
	 */
	@Test
	public void testBug25662() throws CoreException {
		// We need to know whether or not we can unset the read-only flag
		// in order to perform this test.
		assumeTrue(isReadOnlySupported(), "only relevant for platforms supporting read-only files");

		// Only run this test on Linux for now since Windows lets you create
		// a file within a read-only folder.
		assumeTrue(OS.isLinux(), "only relevant on Linux");

		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IFolder parentFolder = project.getFolder("parentFolder");
		createInWorkspace(new IResource[] {project, parentFolder});
		IFolder folder = parentFolder.getFolder("folder");

		try {
			parentFolder.setReadOnly(true);
			assertThat(parentFolder).matches(IResource::isReadOnly, "is read only");
			CoreException exception = assertThrows(CoreException.class, () -> folder.create(true, true, createTestMonitor()));
			assertEquals(IResourceStatus.PARENT_READ_ONLY, exception.getStatus().getCode());
		} finally {
			parentFolder.setReadOnly(false);
		}
	}

	/**
	 * Bug 11510 [resources] Non-local folders do not become local when directory is created.
	 */
	// Explicitly tests deprecated API
	@SuppressWarnings("deprecation")
	@Test
	public void testBug11510() throws Exception {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject("TestProject");
		IFolder folder = project.getFolder("fold1");
		IFile subFile = folder.getFile("f1");
		IFile file = project.getFile("f2");
		createInWorkspace(project);
		folder.create(true, false, createTestMonitor());
		file.create(null, true, createTestMonitor());
		subFile.create(null, true, createTestMonitor());

		Predicate<IResource> isLocal = resource -> resource.isLocal(IResource.DEPTH_ZERO);

		assertThat(folder).matches(not(isLocal), "not is local");
		assertThat(file).matches(not(isLocal), "not is local");
		assertThat(subFile).matches(not(isLocal), "not is local");

		// now create the resources in the local file system and refresh
		createInFileSystem(file);
		project.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		assertThat(file).matches(isLocal, "is local");
		assertThat(folder).matches(not(isLocal), "not is local");
		assertThat(subFile).matches(not(isLocal), "not is local");

		folder.getLocation().toFile().mkdir();
		project.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		assertThat(folder).matches(isLocal, "is local");
		assertThat(file).matches(isLocal, "is local");
		assertThat(subFile).matches(not(isLocal), "not is local");

		createInFileSystem(subFile);
		project.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		assertThat(folder).matches(isLocal, "is local");
		assertThat(file).matches(isLocal, "is local");
		assertThat(subFile).matches(isLocal, "is local");
	}

	/**
	 * Bug 514831: "shallow" mkdir fails if the directory already exists
	 */
	@Test
	public void testBug514831() throws CoreException {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject("TestProject");
		IFolder folder = project.getFolder("folder");

		createInWorkspace(project);
		createInWorkspace(new IResource[] {folder});

		IFileStore dir = EFS.getLocalFileSystem().fromLocalFile(folder.getLocation().toFile());
		assertThat(dir).matches(it -> it.fetchInfo().exists(), "exists");

		dir.mkdir(EFS.NONE, null);
		dir.mkdir(EFS.SHALLOW, null);
		// should not throw an exception
	}

}
