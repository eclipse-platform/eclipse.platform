/*******************************************************************************
 * Copyright (c) 2008, 2014 IBM Corporation and others.
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
 *     Sergey Prigogin (Google) - [440283] Modify symlink tests to run on Windows with or without administrator privileges
 *******************************************************************************/
package org.eclipse.core.tests.resources.regression;

import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;

import java.net.URI;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.internal.utils.FileUtil;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.resources.ResourceTest;

public class Bug_233939 extends ResourceTest {
	/**
	 * Create a symbolic link in the given container, pointing to the given target.
	 * Refresh the workspace and verify that the symbolic link attribute is set.
	 */
	protected void symLinkAndRefresh(IContainer container, String linkName, IPath linkTarget) throws CoreException {
		createSymLink(container.getLocation().toFile(), linkName, linkTarget.toOSString(), false);
		container.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		IResource theLink = container.findMember(linkName);
		assertExistsInWorkspace(theLink);
		assertTrue("2.2", theLink.getResourceAttributes().isSymbolicLink());
	}

	/**
	 * Test whether the given file and the file store refer to the same canonical location.
	 */
	protected boolean isSameLocation(IFile file, IFileStore store) {
		URI loc1 = FileUtil.canonicalURI(file.getLocationURI());
		URI loc2 = FileUtil.canonicalURI(store.toURI());
		return loc1.equals(loc2);
	}

	public void testBug() throws CoreException {
		// Only activate this test if testing of symbolic links is possible.
		if (!canCreateSymLinks()) {
			return;
		}
		String fileName = "file.txt";

		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(getUniqueString());
		IFile file = project.getFile(fileName);

		// create a project
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		// create a file: getTempStore() will be cleaned up in tearDown()
		IFileStore tempFileStore = getTempStore().getChild(fileName);
		createFileInFileSystem(tempFileStore);
		IPath fileInTempDirPath = URIUtil.toPath(tempFileStore.toURI());

		// create a link to the file in the temp dir and refresh
		symLinkAndRefresh(project, fileName, fileInTempDirPath);

		IFile[] files = root.findFilesForLocationURI(file.getLocationURI());
		assertEquals("7.0", 1, files.length);
		assertEquals("7.1", file, files[0]);

		// Bug 198291: We do not track canonical symlink locations below project level
		//		IFile[] files = root.findFilesForLocation(fileInTempDirPath);
		//		assertEquals("6.0", 1, files.length);
		//		assertEquals("6.1", file, files[0]);
	}

	public void testMultipleLinksToFolder() throws CoreException {
		// Only activate this test if testing of symbolic links is possible.
		if (!canCreateSymLinks()) {
			return;
		}
		// create a folder: getTempStore() will be cleaned up in tearDown()
		IFileStore tempStore = getTempStore();
		createFileInFileSystem(tempStore.getChild("foo.txt"));
		IPath tempFolderPath = URIUtil.toPath(tempStore.toURI());

		// create two projects with a symlink to the folder each
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject projectA = root.getProject(getUniqueString());
		IProject projectB = root.getProject(getUniqueString());
		create(projectA, true);
		create(projectB, true);
		symLinkAndRefresh(projectA, "folderA", tempFolderPath);
		symLinkAndRefresh(projectB, "folderB", tempFolderPath);

		//		IFolder folderA = projectA.getFolder("folderA");
		//		IFolder folderB = projectB.getFolder("folderB");
		//
		//		// Bug 198291: We do not track aliasing due to symlinks
		//		IFile[] files = root.findFilesForLocation(folderA.getLocation());
		//		assertEquals("3.0", 2, files.length);
		//		assertNotSame("3.1", files[0], files[1]);
		//		assertTrue("3.2", isSameLocation(files[0], tempStore));
		//		assertTrue("3.3", isSameLocation(files[1], tempStore));
		//
		//		files = root.findFilesForLocationURI(folderB.getLocationURI());
		//		assertEquals("4.0", 2, files.length);
		//		assertNotSame("4.1", files[0], files[1]);
		//		assertTrue("4.2", isSameLocation(files[0], tempStore));
		//		assertTrue("4.3", isSameLocation(files[1], tempStore));
		//
		//		// Bug 198291: We do not track canonical symlink locations below project level
		//		files = root.findFilesForLocationURI(tempStore.toURI());
		//		assertEquals("5.0", 2, files.length);
		//		assertNotSame("5.1", files[0], files[1]);
		//		assertTrue("5.2", isSameLocation(files[0], tempStore));
		//		assertTrue("5.3", isSameLocation(files[1], tempStore));
	}
}
