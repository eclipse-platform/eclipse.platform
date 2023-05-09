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

import java.io.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.tests.resources.ResourceTest;

/**
 * A parent container (projects and folders) would become out-of-sync if any of
 * its children could not be deleted for some reason. These platform-
 * specific test cases ensure that it does not happen.
 */
public class Bug_026294 extends ResourceTest {

	/**
	 * Tries to delete an open project containing an unremovable file.
	 * Works only for Windows.
	 */
	public void testDeleteOpenProjectWindows() {
		if (!(isWindows())) {
			return;
		}

		IProject project = null;
		InputStream input = null;
		File projectRoot = null;
		try {
			IWorkspace workspace = getWorkspace();
			project = workspace.getRoot().getProject(getUniqueString());
			IFolder folder = project.getFolder("a_folder");
			IFile file1 = folder.getFile("file1.txt");
			IFile file2 = project.getFile("file2.txt");
			IFile file3 = folder.getFile("file3.txt");
			IFile projectFile = project.getFile(new Path(".project"));

			ensureExistsInWorkspace(new IResource[] {file1, file2, file3}, true);
			projectRoot = project.getLocation().toFile();

			assertExistsInFileSystem("0.0", file1);
			assertExistsInFileSystem("0.1", file2);
			assertExistsInFileSystem("0.2", file3);
			assertExistsInFileSystem("0.3", folder);
			assertExistsInFileSystem("0.4", projectFile);

			// opens a file so it cannot be removed on Windows
			try {
				input = file1.getContents();
			} catch (CoreException ce) {
				ce.printStackTrace();
				fail("1.0");
			}
			assertTrue("1.2", projectFile.exists());
			assertTrue("1.3", projectFile.isSynchronized(IResource.DEPTH_INFINITE));

			try {
				project.delete(IResource.FORCE, getMonitor());
				fail("2.0 - should have failed");
			} catch (CoreException ce) {
				// success - a file couldn't be removed
			}

			// Delete is best-case so check all the files.
			// Do a check on disk and in the workspace in case something is out of sync.
			assertExistsInWorkspace("2.1.1", project);
			assertExistsInFileSystem("2.1.2", project);

			assertExistsInWorkspace("2.2.1", file1);
			assertExistsInFileSystem("2.2.2", file1);
			assertTrue("2.2.3", file1.isSynchronized(IResource.DEPTH_INFINITE));

			assertDoesNotExistInWorkspace("2.3.1", file2);
			assertDoesNotExistInFileSystem("2.3.2", file2);
			assertTrue("2.3.3", file2.isSynchronized(IResource.DEPTH_INFINITE));

			assertDoesNotExistInWorkspace("2.4.1", file3);
			assertDoesNotExistInFileSystem("2.4.2", file3);
			assertTrue("2.4.3", file3.isSynchronized(IResource.DEPTH_INFINITE));

			assertExistsInWorkspace("2.5.1", folder);
			assertExistsInFileSystem("2.5.2", folder);
			assertTrue("2.5.3", folder.isSynchronized(IResource.DEPTH_INFINITE));

			assertExistsInWorkspace("2.6.1", projectFile);
			assertExistsInFileSystem("2.6.2", projectFile);
			assertTrue("2.6.3", projectFile.isSynchronized(IResource.DEPTH_INFINITE));

			assertTrue("2.7.0", project.isSynchronized(IResource.DEPTH_ZERO));
			assertTrue("2.7.1", project.isSynchronized(IResource.DEPTH_INFINITE));

			assertClose(input);

			assertTrue("3.5", project.isSynchronized(IResource.DEPTH_INFINITE));
			try {
				project.delete(IResource.FORCE, getMonitor());
			} catch (CoreException e) {
				fail("4.0", e);
			}

			assertTrue("5.1", !project.exists());
			assertTrue("5.2", !file1.exists());
			assertTrue("5.3", file1.isSynchronized(IResource.DEPTH_INFINITE));
			assertTrue("5.4", project.isSynchronized(IResource.DEPTH_INFINITE));

			assertTrue("6.0", !projectRoot.exists());
		} finally {
			try {
				assertClose(input);
			} finally {
				if (projectRoot != null) {
					ensureDoesNotExistInFileSystem(projectRoot);
				}
			}
		}
	}

	/**
	 * Tries to delete an open project containing an irremovable file.
	 * Works only for Linux with natives.
	 */
	public void testDeleteOpenProjectLinux() {
		if (!(isLinux() && isReadOnlySupported())) {
			return;
		}

		IProject project = null;
		File projectRoot = null;
		IFolder folder = null;
		try {
			IWorkspace workspace = getWorkspace();
			project = workspace.getRoot().getProject(getUniqueString());
			folder = project.getFolder("a_folder");
			IFile file1 = folder.getFile("file1.txt");
			IFile file2 = project.getFile("file2.txt");

			ensureExistsInWorkspace(new IResource[] {file1, file2}, true);
			projectRoot = project.getLocation().toFile();

			// marks folder as read-only so its files cannot be deleted on Linux
			setReadOnly(folder, true);

			IFile projectFile = project.getFile(".project");
			assertTrue("1.2", projectFile.exists());
			assertTrue("1.3", projectFile.isSynchronized(IResource.DEPTH_INFINITE));

			try {
				project.delete(IResource.FORCE, getMonitor());
				fail("2.0 - should have failed");
			} catch (CoreException ce) {
				// success - a file couldn't be removed
			}
			assertTrue("2.1", project.exists());
			assertTrue("2.2", file1.exists());
			assertTrue("2.3", !file2.exists());
			assertTrue("2.5", folder.exists());
			assertTrue("2.6", projectFile.exists());
			assertTrue("2.7", project.isSynchronized(IResource.DEPTH_INFINITE));

			setReadOnly(folder, false);

			assertTrue("3.5", project.isSynchronized(IResource.DEPTH_INFINITE));
			try {
				project.delete(IResource.FORCE, getMonitor());
			} catch (CoreException ce) {
				ce.printStackTrace();
				fail("4.0", ce);
			}

			assertTrue("5.1", !project.exists());
			assertTrue("5.2", !file1.exists());
			assertTrue("5.3", file1.isSynchronized(IResource.DEPTH_INFINITE));
			assertTrue("5.4", project.isSynchronized(IResource.DEPTH_INFINITE));

			assertTrue("6.0", !projectRoot.exists());
		} finally {
			if (folder != null && folder.exists()) {
				setReadOnly(folder, false);
			}
			if (projectRoot != null) {
				ensureDoesNotExistInFileSystem(projectRoot);
			}
		}
	}

	/**
	 * Tries to delete a closed project containing an unremovable file.
	 * Works only for Windows.
	 */
	public void testDeleteClosedProjectWindows() {
		if (!isWindows()) {
			return;
		}

		IProject project = null;
		InputStream input = null;
		File projectRoot = null;
		IFile file1 = null;
		try {
			IWorkspace workspace = getWorkspace();
			project = workspace.getRoot().getProject(getUniqueString());
			IFolder folder = project.getFolder("a_folder");
			file1 = folder.getFile("file1.txt");
			IFile file2 = project.getFile("file2.txt");
			IFile file3 = folder.getFile("file3.txt");
			IFile projectFile = project.getFile(new Path(".project"));

			ensureExistsInWorkspace(new IResource[] {file1, file2, file3}, true);

			projectRoot = project.getLocation().toFile();

			// opens a file so it cannot be removed on Windows
			try {
				input = file1.getContents();
			} catch (CoreException ce) {
				ce.printStackTrace();
				fail("1.0");
			}

			try {
				project.close(getMonitor());
			} catch (CoreException e) {
				fail("1.1", e);
			}

			try {
				project.delete(IResource.FORCE | IResource.ALWAYS_DELETE_PROJECT_CONTENT, getMonitor());
				fail("2.0 - should have failed");
			} catch (CoreException ce) {
				// success - a file couldn't be removed
			}
			assertTrue("2.1", project.exists());
			assertTrue("2.7", project.isSynchronized(IResource.DEPTH_INFINITE));
			assertExistsInFileSystem("2.8", projectFile);

			assertClose(input);
			assertTrue("3.5", project.isSynchronized(IResource.DEPTH_INFINITE));
			try {
				project.delete(IResource.FORCE | IResource.ALWAYS_DELETE_PROJECT_CONTENT, getMonitor());
			} catch (CoreException ce) {
				ce.printStackTrace();
				fail("4.0", ce);
			}

			assertTrue("5.1", !project.exists());
			assertTrue("5.3", project.isSynchronized(IResource.DEPTH_INFINITE));
			assertTrue("6.0", !projectRoot.exists());
			assertDoesNotExistInFileSystem("7.0", projectFile);
		} finally {
			try {
				if (input != null) {
					input.close();
				}
			} catch (IOException e) {
				fail("8.0", e);
			} finally {
				if (projectRoot != null) {
					ensureDoesNotExistInFileSystem(projectRoot);
				}
			}
		}
	}

	/**
	 * Tries to delete a closed project containing an unremovable file.
	 * Works only for Linux with natives.
	 *
	 * TODO: enable this test once bug 48321 is fixed.
	 */
	public void testDeleteClosedProjectLinux() {
		if (!isLinux()) {
			return;
		}

		IProject project = null;
		File projectRoot = null;
		IFolder folder = null;
		try {
			IWorkspace workspace = getWorkspace();
			project = workspace.getRoot().getProject(getUniqueString());
			folder = project.getFolder("a_folder");
			IFile file1 = folder.getFile("file1.txt");
			IFile file2 = project.getFile("file2.txt");
			IFile projectFile = project.getFile(new Path(".project"));

			ensureExistsInWorkspace(new IResource[] {file1, file2}, true);

			projectRoot = project.getLocation().toFile();

			// marks folder as read-only so its files cannot be removed on Linux
			setReadOnly(folder, true);

			try {
				project.close(getMonitor());
			} catch (CoreException e) {
				fail("1.0", e);
			}

			try {
				project.delete(IResource.FORCE | IResource.ALWAYS_DELETE_PROJECT_CONTENT, getMonitor());
				fail("2.0 - should have failed");
			} catch (CoreException ce) {
				// success - a file couldn't be removed
			}

			assertTrue("3.0", project.exists());
			assertTrue("3.1", project.isSynchronized(IResource.DEPTH_INFINITE));
			assertExistsInFileSystem("3.2", projectFile);

			try {
				project.open(getMonitor());
			} catch (CoreException e) {
				fail("4.0", e);
			}

			setReadOnly(folder, false);
			try {
				project.delete(IResource.FORCE | IResource.ALWAYS_DELETE_PROJECT_CONTENT, getMonitor());
			} catch (CoreException ce) {
				ce.printStackTrace();
				fail("5.0", ce);
			}

			assertTrue("6.0", !project.exists());
			assertTrue("6.1", project.isSynchronized(IResource.DEPTH_INFINITE));
			assertTrue("6.2", !projectRoot.exists());
			assertDoesNotExistInFileSystem("6.3", projectFile);
		} finally {
			if (folder != null && folder.exists()) {
				setReadOnly(folder, false);
			}
			if (projectRoot != null) {
				ensureDoesNotExistInFileSystem(projectRoot);
			}
		}
	}

	/**
	 * Tries to delete a folder containing an unremovable file.
	 * Works only for Windows.
	 */
	public void testDeleteFolderWindows() {
		if (!isWindows()) {
			return;
		}

		IProject project = null;
		InputStream input = null;
		File projectRoot = null;
		try {
			IWorkspace workspace = getWorkspace();
			project = workspace.getRoot().getProject(getUniqueString());
			IFolder folder = project.getFolder("a_folder");
			IFile file1 = folder.getFile("file1.txt");
			IFile file3 = folder.getFile("file3.txt");

			ensureExistsInWorkspace(new IResource[] {file1, file3}, true);
			projectRoot = project.getLocation().toFile();

			// opens a file so it cannot be removed on Windows
			try {
				input = file1.getContents();
			} catch (CoreException ce) {
				ce.printStackTrace();
				fail("1.0");
			}

			try {
				folder.delete(IResource.FORCE, getMonitor());
				fail("2.0 - should have failed");
			} catch (CoreException ce) {
				// success - a file couldn't be removed
			}
			assertTrue("2.2", file1.exists());
			assertTrue("2.4", !file3.exists());
			assertTrue("2.5", folder.exists());
			assertTrue("2.7", folder.isSynchronized(IResource.DEPTH_INFINITE));

			assertClose(input);

			assertTrue("3.5", project.isSynchronized(IResource.DEPTH_INFINITE));
			try {
				folder.delete(IResource.FORCE, getMonitor());
			} catch (CoreException ce) {
				ce.printStackTrace();
				fail("4.0", ce);
			}
			assertTrue("5.1", !file1.exists());
			assertTrue("5.2", !folder.exists());
			assertTrue("5.3", file1.isSynchronized(IResource.DEPTH_INFINITE));
			assertTrue("5.4", folder.isSynchronized(IResource.DEPTH_INFINITE));
		} finally {
			try {
				assertClose(input);
			} finally {
				if (projectRoot != null) {
					ensureDoesNotExistInFileSystem(projectRoot);
				}
			}
		}
	}

	/**
	 * Tries to delete a folder containing an irremovable file.
	 * Works only for Linux with natives.
	 */
	public void testDeleteFolderLinux() {
		if (!isLinux()) {
			return;
		}

		IProject project = null;
		File projectRoot = null;
		IFolder subFolder = null;
		try {
			IWorkspace workspace = getWorkspace();
			project = workspace.getRoot().getProject(getUniqueString());
			IFolder folder = project.getFolder("a_folder");
			subFolder = folder.getFolder("sub-folder");
			IFile file1 = subFolder.getFile("file1.txt");
			IFile file3 = folder.getFile("file3.txt");

			ensureExistsInWorkspace(new IResource[] {file1, file3}, true);
			projectRoot = project.getLocation().toFile();

			// marks sub-folder as read-only so its files cannot be removed on Linux
			setReadOnly(subFolder, true);

			try {
				folder.delete(IResource.FORCE, getMonitor());
				fail("2.0 - should have failed");
			} catch (CoreException ce) {
				// success - a file couldn't be removed
			}
			assertTrue("2.2", file1.exists());
			assertTrue("2.3", subFolder.exists());
			assertTrue("2.4", !file3.exists());
			assertTrue("2.5", folder.exists());
			assertTrue("2.7", folder.isSynchronized(IResource.DEPTH_INFINITE));

			setReadOnly(subFolder, false);

			assertTrue("3.5", project.isSynchronized(IResource.DEPTH_INFINITE));
			try {
				folder.delete(IResource.FORCE, getMonitor());
			} catch (CoreException ce) {
				ce.printStackTrace();
				fail("4.0", ce);
			}
			assertTrue("5.1", !file1.exists());
			assertTrue("5.2", !subFolder.exists());
			assertTrue("5.3", !folder.exists());
			assertTrue("5.4", file1.isSynchronized(IResource.DEPTH_INFINITE));
			assertTrue("5.5", folder.isSynchronized(IResource.DEPTH_INFINITE));
		} finally {
			if (subFolder != null && subFolder.exists()) {
				setReadOnly(subFolder, false);
			}
			if (projectRoot != null) {
				ensureDoesNotExistInFileSystem(projectRoot);
			}
		}
	}

	/**
	 * @see org.eclipse.core.tests.harness.ResourceTest#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}
}
