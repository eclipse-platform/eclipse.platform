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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.InputStream;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform.OS;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * A parent container (projects and folders) would become out-of-sync if any of
 * its children could not be deleted for some reason. These platform-
 * specific test cases ensure that it does not happen.
 */
public class Bug_026294 {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	/**
	 * Tries to delete an open project containing an unremovable file.
	 * Works only for Windows.
	 */
	@Test
	public void testDeleteOpenProjectWindows() throws Exception {
		assumeTrue("only relevant on Windows", OS.isWindows());

		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject(createUniqueString());
		IFolder folder = project.getFolder("a_folder");
		IFile file1 = folder.getFile("file1.txt");
		IFile file2 = project.getFile("file2.txt");
		IFile file3 = folder.getFile("file3.txt");
		IFile projectFile = project.getFile(IPath.fromOSString(".project"));

		createInWorkspace(new IResource[] { file1, file2, file3 });
		IPath projectRoot = project.getLocation();
		workspaceRule.deleteOnTearDown(projectRoot);

		assertExistsInFileSystem(file1);
		assertExistsInFileSystem(file2);
		assertExistsInFileSystem(file3);
		assertExistsInFileSystem(folder);
		assertExistsInFileSystem(projectFile);

		// opens a file so it cannot be removed on Windows
		try (InputStream input = file1.getContents()) {
			assertTrue("1.2", projectFile.exists());
			assertTrue("1.3", projectFile.isSynchronized(IResource.DEPTH_INFINITE));

			assertThrows(CoreException.class, () -> project.delete(IResource.FORCE, createTestMonitor()));

			// Delete is best-case so check all the files.
			// Do a check on disk and in the workspace in case something is out of sync.
			assertExistsInWorkspace(project);
			assertExistsInFileSystem(project);

			assertExistsInWorkspace(file1);
			assertExistsInFileSystem(file1);
			assertTrue("2.2.3", file1.isSynchronized(IResource.DEPTH_INFINITE));

			assertDoesNotExistInWorkspace(file2);
			assertDoesNotExistInFileSystem(file2);
			assertTrue("2.3.3", file2.isSynchronized(IResource.DEPTH_INFINITE));

			assertDoesNotExistInWorkspace(file3);
			assertDoesNotExistInFileSystem(file3);
			assertTrue("2.4.3", file3.isSynchronized(IResource.DEPTH_INFINITE));

			assertExistsInWorkspace(folder);
			assertExistsInFileSystem(folder);
			assertTrue("2.5.3", folder.isSynchronized(IResource.DEPTH_INFINITE));

			assertExistsInWorkspace(projectFile);
			assertExistsInFileSystem(projectFile);
			assertTrue("2.6.3", projectFile.isSynchronized(IResource.DEPTH_INFINITE));

			assertTrue("2.7.0", project.isSynchronized(IResource.DEPTH_ZERO));
			assertTrue("2.7.1", project.isSynchronized(IResource.DEPTH_INFINITE));
		}

		assertTrue("3.5", project.isSynchronized(IResource.DEPTH_INFINITE));
		project.delete(IResource.FORCE, createTestMonitor());
		assertTrue("5.1", !project.exists());
		assertTrue("5.2", !file1.exists());
		assertTrue("5.3", file1.isSynchronized(IResource.DEPTH_INFINITE));
		assertTrue("5.4", project.isSynchronized(IResource.DEPTH_INFINITE));
		assertTrue("6.0", !projectRoot.toFile().exists());
	}

	/**
	 * Tries to delete an open project containing an non-removable file. Works only
	 * for Linux with natives.
	 */
	@Test
	public void testDeleteOpenProjectLinux() throws CoreException {
		assumeTrue("only relevant on Linux", OS.isLinux() && isReadOnlySupported());

		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject(createUniqueString());
		IFolder folder = project.getFolder("a_folder");
		IFile file1 = folder.getFile("file1.txt");
		IFile file2 = project.getFile("file2.txt");

		createInWorkspace(new IResource[] { file1, file2 });
		IPath projectRoot = project.getLocation();
		workspaceRule.deleteOnTearDown(projectRoot);

		try {
			// marks folder as read-only so its files cannot be deleted on Linux
			setReadOnly(folder, true);

			IFile projectFile = project.getFile(".project");
			assertTrue("1.2", projectFile.exists());
			assertTrue("1.3", projectFile.isSynchronized(IResource.DEPTH_INFINITE));

			assertThrows(CoreException.class, () -> project.delete(IResource.FORCE, createTestMonitor()));
			assertTrue("2.1", project.exists());
			assertTrue("2.2", file1.exists());
			assertTrue("2.3", !file2.exists());
			assertTrue("2.5", folder.exists());
			assertTrue("2.6", projectFile.exists());
			assertTrue("2.7", project.isSynchronized(IResource.DEPTH_INFINITE));
		} finally {
			if (folder.exists()) {
				setReadOnly(folder, false);
			}
		}

		assertTrue("3.5", project.isSynchronized(IResource.DEPTH_INFINITE));
		project.delete(IResource.FORCE, createTestMonitor());
		assertTrue("5.1", !project.exists());
		assertTrue("5.2", !file1.exists());
		assertTrue("5.3", file1.isSynchronized(IResource.DEPTH_INFINITE));
		assertTrue("5.4", project.isSynchronized(IResource.DEPTH_INFINITE));
		assertTrue("6.0", !projectRoot.toFile().exists());
	}

	/**
	 * Tries to delete a closed project containing a non-removable file.
	 * Works only for Windows.
	 */
	@Test
	public void testDeleteClosedProjectWindows() throws Exception {
		assumeTrue("only relevant on Windows", OS.isWindows());

		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject(createUniqueString());
		IFolder folder = project.getFolder("a_folder");
		IFile file1 = folder.getFile("file1.txt");
		IFile file2 = project.getFile("file2.txt");
		IFile file3 = folder.getFile("file3.txt");
		IFile projectFile = project.getFile(IPath.fromOSString(".project"));

		createInWorkspace(new IResource[] { file1, file2, file3 });
		IPath projectRoot = project.getLocation();
		workspaceRule.deleteOnTearDown(projectRoot);

		// opens a file so it cannot be removed on Windows
		try (InputStream input = file1.getContents()) {
			project.close(createTestMonitor());
			assertThrows(CoreException.class,
					() -> project.delete(IResource.FORCE | IResource.ALWAYS_DELETE_PROJECT_CONTENT, createTestMonitor()));
			assertTrue("2.1", project.exists());
			assertTrue("2.7", project.isSynchronized(IResource.DEPTH_INFINITE));
			assertExistsInFileSystem(projectFile);

		}
		assertTrue("3.5", project.isSynchronized(IResource.DEPTH_INFINITE));
		project.delete(IResource.FORCE | IResource.ALWAYS_DELETE_PROJECT_CONTENT, createTestMonitor());
		assertTrue("5.1", !project.exists());
		assertTrue("5.3", project.isSynchronized(IResource.DEPTH_INFINITE));
		assertTrue("6.0", !projectRoot.toFile().exists());
		assertDoesNotExistInFileSystem(projectFile);
	}

	/**
	 * Tries to delete a closed project containing an non-removable file. Works only
	 * for Linux with natives.
	 */
	@Test
	public void testDeleteClosedProjectLinux() throws CoreException {
		assumeTrue("only relevant on Linux", OS.isLinux());

		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject(createUniqueString());
		IFolder folder = project.getFolder("a_folder");
		IFile file1 = folder.getFile("file1.txt");
		IFile file2 = project.getFile("file2.txt");
		IFile projectFile = project.getFile(IPath.fromOSString(".project"));

		createInWorkspace(new IResource[] { file1, file2 });
		IPath projectRoot = project.getLocation();
		workspaceRule.deleteOnTearDown(projectRoot);

		try {
			// marks folder as read-only so its files cannot be removed on Linux
			setReadOnly(folder, true);

			project.close(createTestMonitor());
			assertThrows(CoreException.class,
					() -> project.delete(IResource.FORCE | IResource.ALWAYS_DELETE_PROJECT_CONTENT, createTestMonitor()));

			assertTrue("3.0", project.exists());
			assertTrue("3.1", project.isSynchronized(IResource.DEPTH_INFINITE));
			assertExistsInFileSystem(projectFile);

			project.open(createTestMonitor());
		} finally {
			if (folder.exists()) {
				setReadOnly(folder, false);
			}
		}

		project.delete(IResource.FORCE | IResource.ALWAYS_DELETE_PROJECT_CONTENT, createTestMonitor());
		assertTrue("6.0", !project.exists());
		assertTrue("6.1", project.isSynchronized(IResource.DEPTH_INFINITE));
		assertTrue("6.2", !projectRoot.toFile().exists());
		assertDoesNotExistInFileSystem(projectFile);
	}

	/**
	 * Tries to delete a folder containing a non-removable file. Works only for
	 * Windows.
	 */
	@Test
	public void testDeleteFolderWindows() throws Exception {
		assumeTrue("only relevant on Windows", OS.isWindows());

		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject(createUniqueString());
		IFolder folder = project.getFolder("a_folder");
		IFile file1 = folder.getFile("file1.txt");
		IFile file3 = folder.getFile("file3.txt");

		createInWorkspace(new IResource[] { file1, file3 });
		IPath projectRoot = project.getLocation();
		workspaceRule.deleteOnTearDown(projectRoot);

		// opens a file so it cannot be removed on Windows
		try (InputStream input = file1.getContents()) {
			assertThrows(CoreException.class, () -> folder.delete(IResource.FORCE, createTestMonitor()));
			assertTrue("2.2", file1.exists());
			assertTrue("2.4", !file3.exists());
			assertTrue("2.5", folder.exists());
			assertTrue("2.7", folder.isSynchronized(IResource.DEPTH_INFINITE));
		}

		assertTrue("3.5", project.isSynchronized(IResource.DEPTH_INFINITE));
		folder.delete(IResource.FORCE, createTestMonitor());
		assertTrue("5.1", !file1.exists());
		assertTrue("5.2", !folder.exists());
		assertTrue("5.3", file1.isSynchronized(IResource.DEPTH_INFINITE));
		assertTrue("5.4", folder.isSynchronized(IResource.DEPTH_INFINITE));
	}

	/**
	 * Tries to delete a folder containing a non-removable file. Works only for
	 * Linux with natives.
	 */
	@Test
	public void testDeleteFolderLinux() throws CoreException {
		assumeTrue("only relevant on Linux", OS.isLinux());

		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject(createUniqueString());
		IFolder folder = project.getFolder("a_folder");
		IFolder subFolder = folder.getFolder("sub-folder");
		IFile file1 = subFolder.getFile("file1.txt");
		IFile file3 = folder.getFile("file3.txt");

		createInWorkspace(new IResource[] { file1, file3 });
		IPath projectRoot = project.getLocation();
		workspaceRule.deleteOnTearDown(projectRoot);

		try {
			// marks sub-folder as read-only so its files cannot be removed on Linux
			setReadOnly(subFolder, true);

			assertThrows(CoreException.class, () -> folder.delete(IResource.FORCE, createTestMonitor()));
			assertTrue("2.2", file1.exists());
			assertTrue("2.3", subFolder.exists());
			assertTrue("2.4", !file3.exists());
			assertTrue("2.5", folder.exists());
			assertTrue("2.7", folder.isSynchronized(IResource.DEPTH_INFINITE));
		} finally {
			if (subFolder.exists()) {
				setReadOnly(subFolder, false);
			}
		}

		assertTrue("3.5", project.isSynchronized(IResource.DEPTH_INFINITE));
		folder.delete(IResource.FORCE, createTestMonitor());
		assertTrue("5.1", !file1.exists());
		assertTrue("5.2", !subFolder.exists());
		assertTrue("5.3", !folder.exists());
		assertTrue("5.4", file1.isSynchronized(IResource.DEPTH_INFINITE));
		assertTrue("5.5", folder.isSynchronized(IResource.DEPTH_INFINITE));
	}

}
