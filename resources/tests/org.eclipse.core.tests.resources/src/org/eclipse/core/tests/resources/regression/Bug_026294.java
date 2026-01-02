/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.regression;

import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertDoesNotExistInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertDoesNotExistInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createUniqueString;
import static org.eclipse.core.tests.resources.ResourceTestUtil.isReadOnlySupported;
import static org.eclipse.core.tests.resources.ResourceTestUtil.setReadOnly;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.InputStream;
import java.util.function.Predicate;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform.OS;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * A parent container (projects and folders) would become out-of-sync if any of
 * its children could not be deleted for some reason. These platform-
 * specific test cases ensure that it does not happen.
 */
@ExtendWith(WorkspaceResetExtension.class)
public class Bug_026294 {

	private static final Predicate<IResource> isSynchronizedDepthInfinite = resource -> resource
			.isSynchronized(IResource.DEPTH_INFINITE);

	private static final Predicate<IResource> isSynchronizedDepthZero = resource -> resource
			.isSynchronized(IResource.DEPTH_ZERO);

	/**
	 * Tries to delete an open project containing an unremovable file.
	 * Works only for Windows.
	 */
	@Test
	public void testDeleteOpenProjectWindows() throws Exception {
		assumeTrue(OS.isWindows(), "only relevant on Windows\"");

		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject(createUniqueString());
		IFolder folder = project.getFolder("a_folder");
		IFile file1 = folder.getFile("file1.txt");
		IFile file2 = project.getFile("file2.txt");
		IFile file3 = folder.getFile("file3.txt");
		IFile projectFile = project.getFile(IPath.fromOSString(".project"));

		createInWorkspace(new IResource[] { file1, file2, file3 });
		IPath projectRoot = project.getLocation();

		assertExistsInFileSystem(file1);
		assertExistsInFileSystem(file2);
		assertExistsInFileSystem(file3);
		assertExistsInFileSystem(folder);
		assertExistsInFileSystem(projectFile);

		// opens a file so it cannot be removed on Windows
		try (InputStream input = file1.getContents()) {
			assertThat(projectFile).matches(IResource::exists, "exists");
			assertThat(projectFile).matches(isSynchronizedDepthInfinite, "is synchronized");

			assertThrows(CoreException.class, () -> project.delete(IResource.FORCE, createTestMonitor()));

			// Delete is best-case so check all the files.
			// Do a check on disk and in the workspace in case something is out of sync.
			assertExistsInWorkspace(project);
			assertExistsInFileSystem(project);

			assertExistsInWorkspace(file1);
			assertExistsInFileSystem(file1);
			assertThat(file1).matches(isSynchronizedDepthInfinite, "is synchronized");

			assertDoesNotExistInWorkspace(file2);
			assertDoesNotExistInFileSystem(file2);
			assertThat(file2).matches(isSynchronizedDepthInfinite, "is synchronized");

			assertDoesNotExistInWorkspace(file3);
			assertDoesNotExistInFileSystem(file3);
			assertThat(file3).matches(isSynchronizedDepthInfinite, "is synchronized");

			assertExistsInWorkspace(folder);
			assertExistsInFileSystem(folder);
			assertThat(folder).matches(isSynchronizedDepthInfinite, "is synchronized");

			assertExistsInWorkspace(projectFile);
			assertExistsInFileSystem(projectFile);
			assertThat(projectFile).matches(isSynchronizedDepthInfinite, "is synchronized");

			assertThat(project).matches(isSynchronizedDepthZero, "is synchronized");
			assertThat(project).matches(isSynchronizedDepthInfinite, "is synchronized");
		}

		assertThat(project).matches(isSynchronizedDepthInfinite, "is synchronized");
		project.delete(IResource.FORCE, createTestMonitor());
		assertThat(project).matches(not(IResource::exists), "not exists");
		assertThat(file1).matches(not(IResource::exists), "not exists");
		assertThat(file1).matches(isSynchronizedDepthInfinite, "is synchronized");
		assertThat(project).matches(isSynchronizedDepthInfinite, "is synchronized");
		assertThat(projectRoot).matches(it -> !it.toFile().exists(), "not exists");
	}

	/**
	 * Tries to delete an open project containing an non-removable file. Works only
	 * for Linux with natives.
	 */
	@Test
	public void testDeleteOpenProjectLinux() throws CoreException {
		assumeTrue(OS.isLinux() && isReadOnlySupported(), "only relevant on Linux");

		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject(createUniqueString());
		IFolder folder = project.getFolder("a_folder");
		IFile file1 = folder.getFile("file1.txt");
		IFile file2 = project.getFile("file2.txt");

		createInWorkspace(new IResource[] { file1, file2 });
		IPath projectRoot = project.getLocation();

		try {
			// marks folder as read-only so its files cannot be deleted on Linux
			setReadOnly(folder, true);

			IFile projectFile = project.getFile(".project");
			assertThat(projectFile).matches(IResource::exists, "exists");
			assertThat(projectFile).matches(isSynchronizedDepthInfinite, "is synchronized");

			assertThrows(CoreException.class, () -> project.delete(IResource.FORCE, createTestMonitor()));
			assertThat(project).matches(IResource::exists, "exists");
			assertThat(file1).matches(IResource::exists, "exists");
			assertThat(file2).matches(not(IResource::exists), "not exists");
			assertThat(folder).matches(IResource::exists, "exists");
			assertThat(projectFile).matches(IResource::exists, "exists");
			assertThat(project).matches(isSynchronizedDepthInfinite, "is synchronized");
		} finally {
			if (folder.exists()) {
				setReadOnly(folder, false);
			}
		}

		assertThat(project).matches(isSynchronizedDepthInfinite, "is synchronized");
		project.delete(IResource.FORCE, createTestMonitor());
		assertThat(project).matches(not(IResource::exists), "not exists");
		assertThat(file1).matches(not(IResource::exists), "not exists");
		assertThat(file1).matches(isSynchronizedDepthInfinite, "is synchronized");
		assertThat(project).matches(isSynchronizedDepthInfinite, "is synchronized");
		assertThat(projectRoot).matches(it -> !it.toFile().exists(), "not exists");
	}

	/**
	 * Tries to delete a closed project containing a non-removable file.
	 * Works only for Windows.
	 */
	@Test
	public void testDeleteClosedProjectWindows() throws Exception {
		assumeTrue(OS.isWindows(), "only relevant on Windows");

		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject(createUniqueString());
		IFolder folder = project.getFolder("a_folder");
		IFile file1 = folder.getFile("file1.txt");
		IFile file2 = project.getFile("file2.txt");
		IFile file3 = folder.getFile("file3.txt");
		IFile projectFile = project.getFile(IPath.fromOSString(".project"));

		createInWorkspace(new IResource[] { file1, file2, file3 });
		IPath projectRoot = project.getLocation();

		// opens a file so it cannot be removed on Windows
		try (InputStream input = file1.getContents()) {
			project.close(createTestMonitor());
			assertThrows(CoreException.class,
					() -> project.delete(IResource.FORCE | IResource.ALWAYS_DELETE_PROJECT_CONTENT, createTestMonitor()));
			assertThat(project).matches(IResource::exists, "exists");
			assertThat(project).matches(isSynchronizedDepthInfinite, "is synchronized");
			assertExistsInFileSystem(projectFile);

		}
		assertThat(project).matches(isSynchronizedDepthInfinite, "is synchronized");
		project.delete(IResource.FORCE | IResource.ALWAYS_DELETE_PROJECT_CONTENT, createTestMonitor());
		assertThat(project).matches(not(IResource::exists), "not exists");
		assertThat(project).matches(isSynchronizedDepthInfinite, "is synchronized");
		assertThat(projectRoot).matches(it -> !it.toFile().exists(), "not exists");
		assertDoesNotExistInFileSystem(projectFile);
	}

	/**
	 * Tries to delete a closed project containing an non-removable file. Works only
	 * for Linux with natives.
	 */
	@Test
	public void testDeleteClosedProjectLinux() throws CoreException {
		assumeTrue(OS.isLinux(), "only relevant on Linux");

		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject(createUniqueString());
		IFolder folder = project.getFolder("a_folder");
		IFile file1 = folder.getFile("file1.txt");
		IFile file2 = project.getFile("file2.txt");
		IFile projectFile = project.getFile(IPath.fromOSString(".project"));
		createInWorkspace(new IResource[] { file1, file2 });
		IPath projectRoot = project.getLocation();

		try {
			// marks folder as read-only so its files cannot be removed on Linux
			setReadOnly(folder, true);

			project.close(createTestMonitor());
			assertThrows(CoreException.class,
					() -> project.delete(IResource.FORCE | IResource.ALWAYS_DELETE_PROJECT_CONTENT, createTestMonitor()));

			assertThat(project).matches(IResource::exists, "exists");
			assertThat(project).matches(isSynchronizedDepthInfinite, "is synchronized");
			assertExistsInFileSystem(projectFile);

			project.open(createTestMonitor());
		} finally {
			if (folder.exists()) {
				setReadOnly(folder, false);
			}
		}

		project.delete(IResource.FORCE | IResource.ALWAYS_DELETE_PROJECT_CONTENT, createTestMonitor());
		assertThat(project).matches(not(IResource::exists), "not exists");
		assertThat(project).matches(isSynchronizedDepthInfinite, "is synchronized");
		assertThat(projectRoot).matches(it -> !it.toFile().exists(), "not exists");
		assertDoesNotExistInFileSystem(projectFile);
	}

	/**
	 * Tries to delete a folder containing a non-removable file. Works only for
	 * Windows.
	 */
	@Test
	public void testDeleteFolderWindows() throws Exception {
		assumeTrue(OS.isWindows(), "only relevant on Windows");

		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject(createUniqueString());
		IFolder folder = project.getFolder("a_folder");
		IFile file1 = folder.getFile("file1.txt");
		IFile file3 = folder.getFile("file3.txt");
		createInWorkspace(new IResource[] { file1, file3 });

		// opens a file so it cannot be removed on Windows
		try (InputStream input = file1.getContents()) {
			assertThrows(CoreException.class, () -> folder.delete(IResource.FORCE, createTestMonitor()));
			assertThat(file1).matches(IResource::exists, "exists");
			assertThat(file3).matches(not(IResource::exists), "not exists");
			assertThat(folder).matches(IResource::exists, "exists");
			assertThat(folder).matches(isSynchronizedDepthInfinite, "is synchronized");
		}

		assertThat(project).matches(isSynchronizedDepthInfinite, "is synchronized");
		folder.delete(IResource.FORCE, createTestMonitor());
		assertThat(file1).matches(not(IResource::exists), "not exists");
		assertThat(folder).matches(not(IResource::exists), "not exists");
		assertThat(file1).matches(isSynchronizedDepthInfinite, "is synchronized");
		assertThat(folder).matches(isSynchronizedDepthInfinite, "is synchronized");
	}

	/**
	 * Tries to delete a folder containing a non-removable file. Works only for
	 * Linux with natives.
	 */
	@Test
	public void testDeleteFolderLinux() throws CoreException {
		assumeTrue(OS.isLinux(), "only relevant on Linux");

		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject(createUniqueString());
		IFolder folder = project.getFolder("a_folder");
		IFolder subFolder = folder.getFolder("sub-folder");
		IFile file1 = subFolder.getFile("file1.txt");
		IFile file3 = folder.getFile("file3.txt");
		createInWorkspace(new IResource[] { file1, file3 });

		try {
			// marks sub-folder as read-only so its files cannot be removed on Linux
			setReadOnly(subFolder, true);

			assertThrows(CoreException.class, () -> folder.delete(IResource.FORCE, createTestMonitor()));
			assertThat(file1).matches(IResource::exists, "exists");
			assertThat(subFolder).matches(IResource::exists, "exists");
			assertThat(file3).matches(not(IResource::exists), "not exists");
			assertThat(folder).matches(IResource::exists, "exists");
			assertThat(folder).matches(isSynchronizedDepthInfinite, "is synchronized");
		} finally {
			if (subFolder.exists()) {
				setReadOnly(subFolder, false);
			}
		}

		assertThat(project).matches(isSynchronizedDepthInfinite, "is synchronized");
		folder.delete(IResource.FORCE, createTestMonitor());
		assertThat(file1).matches(not(IResource::exists), "not exists");
		assertThat(subFolder).matches(not(IResource::exists), "not exists");
		assertThat(folder).matches(not(IResource::exists), "not exists");
		assertThat(file1).matches(isSynchronizedDepthInfinite, "is synchronized");
		assertThat(folder).matches(isSynchronizedDepthInfinite, "is synchronized");
	}

}
