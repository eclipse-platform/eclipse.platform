/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
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
package org.eclipse.core.tests.resources;

import static org.junit.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.net.URI;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.internal.resources.projectvariables.ProjectLocationVariableResolver;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

public class LinkedResourceSyncMoveAndCopyTest extends ResourceTest {

	protected IProject existingProject;
	protected IProject otherExistingProject;

	/**
	 * Maybe overridden in subclasses that use path variables.
	 */
	protected IPath resolve(IPath path) {
		return path;
	}

	/**
	 * Maybe overridden in subclasses that use path variables.
	 */
	protected URI resolve(URI uri) {
		return uri;
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		existingProject = getWorkspace().getRoot().getProject("ExistingProject");
		otherExistingProject = getWorkspace().getRoot().getProject("OtherExistingProject");
		ensureExistsInWorkspace(new IResource[] { existingProject, otherExistingProject }, true);
	}

	public void internalMovedAndCopyTest(IResource resource, int copyMoveFlag, boolean copyMoveSucceeds) {
		//		try {
		//			resource.copy(otherExistingProject.getFullPath().append(resource.getProjectRelativePath()), copyMoveFlag, getMonitor());
		//			if (!copyMoveSucceeds)
		//				fail("1.0");
		//		} catch (CoreException e) {
		//			if (copyMoveSucceeds)
		//				fail("4.99", e);
		//		} finally {
		//			if (otherExistingProject.findMember(resource.getProjectRelativePath()) != null)
		//				ensureDoesNotExistInWorkspace(otherExistingProject.findMember(resource.getProjectRelativePath()));
		//		}

		//		try {
		//			resource.move(otherExistingProject.getFullPath().append(resource.getProjectRelativePath()), copyMoveFlag, getMonitor());
		//			if (!copyMoveSucceeds)
		//				fail("1.0");
		//		} catch (CoreException e) {
		//			if (copyMoveSucceeds)
		//				fail("4.99", e);
		//		} finally {
		//			if (otherExistingProject.findMember(resource.getProjectRelativePath()) != null)
		//				ensureDoesNotExistInWorkspace(otherExistingProject.findMember(resource.getProjectRelativePath()));
		//		}
	}

	public void testFileLinkedToNonExistent_Deep() throws CoreException {
		IFile fileLink = existingProject.getFile(getUniqueString());
		IPath fileLocation = getRandomLocation();
		fileLink.createLink(fileLocation, IResource.ALLOW_MISSING_LOCAL, getMonitor());

		CoreException exception = assertThrows(CoreException.class, () -> fileLink
				.setContents(new ByteArrayInputStream(getRandomString().getBytes()), IResource.NONE, getMonitor()));
		assertEquals("1.2", IResourceStatus.NOT_FOUND_LOCAL, exception.getStatus().getCode());

		assertTrue("2.0", fileLink.isSynchronized(IResource.DEPTH_INFINITE));
		internalMovedAndCopyTest(fileLink, IResource.NONE, false);

		createFileInFileSystem(fileLocation);
		deleteOnTearDown(fileLocation);

		exception = assertThrows(CoreException.class, () -> fileLink
				.setContents(new ByteArrayInputStream(getRandomString().getBytes()), IResource.NONE, getMonitor()));
		assertEquals("2.2", IResourceStatus.OUT_OF_SYNC_LOCAL, exception.getStatus().getCode());

		assertFalse("3.0", fileLink.isSynchronized(IResource.DEPTH_INFINITE));
		internalMovedAndCopyTest(fileLink, IResource.NONE, false);

		fileLink.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());

		assertTrue("5.0", fileLink.isSynchronized(IResource.DEPTH_INFINITE));
		internalMovedAndCopyTest(fileLink, IResource.NONE, true);
	}

	public void testFileLinkedToNonExistent_Shallow() throws CoreException {
		IFile fileLink = existingProject.getFile(getUniqueString());
		IPath fileLocation = getRandomLocation();
		fileLink.createLink(fileLocation, IResource.ALLOW_MISSING_LOCAL, getMonitor());

		assertTrue("2.0", fileLink.isSynchronized(IResource.DEPTH_INFINITE));
		internalMovedAndCopyTest(fileLink, IResource.SHALLOW, true);

		createFileInFileSystem(fileLocation);
		deleteOnTearDown(fileLocation);

		assertFalse("3.0", fileLink.isSynchronized(IResource.DEPTH_INFINITE));
		internalMovedAndCopyTest(fileLink, IResource.SHALLOW, true);

		fileLink.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());

		assertTrue("5.0", fileLink.isSynchronized(IResource.DEPTH_INFINITE));
		internalMovedAndCopyTest(fileLink, IResource.SHALLOW, true);
	}

	public void testFolderLinkedToNonExistent_Deep() throws CoreException {
		IFolder folderLink = existingProject.getFolder(getUniqueString());
		IPath folderLocation = getRandomLocation();
		folderLink.createLink(folderLocation, IResource.ALLOW_MISSING_LOCAL, getMonitor());

		assertTrue("3.0", folderLink.isSynchronized(IResource.DEPTH_INFINITE));
		internalMovedAndCopyTest(folderLink, IResource.NONE, false);

		folderLocation.toFile().mkdir();
		deleteOnTearDown(folderLocation);

		assertFalse("3.0", folderLink.isSynchronized(IResource.DEPTH_INFINITE));
		internalMovedAndCopyTest(folderLink, IResource.NONE, true);

		folderLink.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());

		assertTrue("5.0", folderLink.isSynchronized(IResource.DEPTH_INFINITE));
		internalMovedAndCopyTest(folderLink, IResource.NONE, true);
	}

	public void testFolderLinkedToNonExistent_Shallow() throws CoreException {
		IFolder folderLink = existingProject.getFolder(getUniqueString());
		IPath folderLocation = getRandomLocation();
		folderLink.createLink(folderLocation, IResource.ALLOW_MISSING_LOCAL, getMonitor());

		assertTrue("2.0", folderLink.isSynchronized(IResource.DEPTH_INFINITE));
		internalMovedAndCopyTest(folderLink, IResource.SHALLOW, true);

		folderLocation.toFile().mkdir();
		deleteOnTearDown(folderLocation);

		assertFalse("3.0", folderLink.isSynchronized(IResource.DEPTH_INFINITE));
		internalMovedAndCopyTest(folderLink, IResource.SHALLOW, true);

		folderLink.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());

		assertTrue("5.0", folderLink.isSynchronized(IResource.DEPTH_INFINITE));
		internalMovedAndCopyTest(folderLink, IResource.SHALLOW, true);
	}

	/**
	 * Tests bug 299024.
	 */
	public void testMoveFolderWithLinksToNonExisitngLocations_withShallow() throws CoreException {
		// create a folder
		IFolder folderWithLinks = existingProject.getFolder(getUniqueString());
		folderWithLinks.create(true, true, getMonitor());

		// non-exisitng location
		IPath fileLocation = getRandomLocation();

		// create a linked file in the folder
		IFile linkedFile = folderWithLinks.getFile(getUniqueString());
		linkedFile.createLink(fileLocation, IResource.ALLOW_MISSING_LOCAL, getMonitor());

		// move the folder
		folderWithLinks.move(otherExistingProject.getFolder(getUniqueString()).getFullPath(), IResource.SHALLOW,
				getMonitor());

		// move the folder
		assertThrows(CoreException.class, () -> folderWithLinks
				.move(otherExistingProject.getFolder(getUniqueString()).getFullPath(), IResource.NONE, getMonitor()));

		// both the folder and link in the source project should not exist
		assertFalse("5.0", folderWithLinks.exists());
		assertFalse("6.0", linkedFile.exists());
	}

	/**
	 * Tests bug 299024.
	 */
	public void _testCopyFolderWithLinksToNonExisitngLocations_withShallow() throws CoreException {
		// create a folder
		IFolder folderWithLinks = existingProject.getFolder(getUniqueString());
		folderWithLinks.create(true, true, getMonitor());

		// non-exisitng location
		IPath fileLocation = getRandomLocation();

		// create a linked file in the folder
		IFile linkedFile = folderWithLinks.getFile(getUniqueString());
		linkedFile.createLink(fileLocation, IResource.ALLOW_MISSING_LOCAL, getMonitor());

		// copy the folder
		folderWithLinks.copy(otherExistingProject.getFolder(getUniqueString()).getFullPath(), IResource.SHALLOW,
				getMonitor());

		assertThrows(CoreException.class, () -> folderWithLinks
				.copy(otherExistingProject.getFolder(getUniqueString()).getFullPath(), IResource.NONE, getMonitor()));

		// both the folder and link in the source project should exist
		assertTrue("5.0", folderWithLinks.exists());
		assertTrue("6.0", linkedFile.exists());
	}

	public void testFolderWithFileLinkedToNonExistent_Deep() throws CoreException {
		IFolder folder = existingProject.getFolder(getUniqueString());
		ensureExistsInWorkspace(folder, true);

		IFile fileLinkInFolder = folder.getFile(getUniqueString());

		IPath fileLocation = getRandomLocation();
		fileLinkInFolder.createLink(fileLocation, IResource.ALLOW_MISSING_LOCAL, getMonitor());

		assertTrue(folder.isSynchronized(IResource.DEPTH_INFINITE));
		internalMovedAndCopyTest(folder, IResource.NONE, false);

		createFileInFileSystem(fileLocation);
		deleteOnTearDown(fileLocation);

		assertFalse(folder.isSynchronized(IResource.DEPTH_INFINITE));
		internalMovedAndCopyTest(folder, IResource.NONE, false);

		folder.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());

		assertTrue(folder.isSynchronized(IResource.DEPTH_INFINITE));
		internalMovedAndCopyTest(folder, IResource.NONE, true);
	}

	public void testFolderWithFileLinkedToNonExistent_Shallow() throws CoreException {
		IFolder folder = existingProject.getFolder(getUniqueString());
		ensureExistsInWorkspace(folder, true);

		IFile fileLinkInFolder = folder.getFile(getUniqueString());

		IPath fileLocation = getRandomLocation();
		fileLinkInFolder.createLink(fileLocation, IResource.ALLOW_MISSING_LOCAL, getMonitor());

		assertTrue(folder.isSynchronized(IResource.DEPTH_INFINITE));
		internalMovedAndCopyTest(folder, IResource.SHALLOW, true);

		createFileInFileSystem(fileLocation);
		deleteOnTearDown(fileLocation);

		assertFalse(folder.isSynchronized(IResource.DEPTH_INFINITE));
		internalMovedAndCopyTest(folder, IResource.SHALLOW, true);

		folder.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());

		assertTrue(folder.isSynchronized(IResource.DEPTH_INFINITE));
		internalMovedAndCopyTest(folder, IResource.SHALLOW, true);
	}

	public void testFolderWithFolderLinkedToNonExistent_Deep() throws CoreException {
		IFolder folder = existingProject.getFolder(getUniqueString());
		ensureExistsInWorkspace(folder, true);

		IFolder folderLinkInFolder = folder.getFolder(getUniqueString());

		IPath folderLocation = getRandomLocation();
		folderLinkInFolder.createLink(folderLocation, IResource.ALLOW_MISSING_LOCAL, getMonitor());

		assertTrue(folder.isSynchronized(IResource.DEPTH_INFINITE));
		internalMovedAndCopyTest(folder, IResource.NONE, false);

		folderLocation.toFile().mkdir();
		deleteOnTearDown(folderLocation);

		assertFalse(folder.isSynchronized(IResource.DEPTH_INFINITE));
		internalMovedAndCopyTest(folder, IResource.NONE, true);

		folder.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());

		assertTrue(folder.isSynchronized(IResource.DEPTH_INFINITE));
		internalMovedAndCopyTest(folder, IResource.NONE, true);
	}

	public void testFolderWithFolderLinkedToNonExistent_Shallow() throws CoreException {
		IFolder folder = existingProject.getFolder(getUniqueString());
		ensureExistsInWorkspace(folder, true);

		IFolder folderLinkInFolder = folder.getFolder(getUniqueString());

		IPath folderLocation = getRandomLocation();
		folderLinkInFolder.createLink(folderLocation, IResource.ALLOW_MISSING_LOCAL, getMonitor());

		assertTrue(folder.isSynchronized(IResource.DEPTH_INFINITE));
		internalMovedAndCopyTest(folder, IResource.SHALLOW, true);

		folderLocation.toFile().mkdir();
		deleteOnTearDown(folderLocation);

		assertFalse(folder.isSynchronized(IResource.DEPTH_INFINITE));
		internalMovedAndCopyTest(folder, IResource.SHALLOW, true);

		folder.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());

		assertTrue(folder.isSynchronized(IResource.DEPTH_INFINITE));
		internalMovedAndCopyTest(folder, IResource.SHALLOW, true);
	}

	public void test361201() throws CoreException {
		String linkName = getUniqueString();
		IFile fileLink = existingProject.getFile(linkName);
		IFile file = existingProject.getFolder("dir").getFile("foo.txt");

		ensureExistsInWorkspace(file.getParent(), true);
		ensureExistsInWorkspace(file, "content");
		IPath fileLocation = file.getLocation();

		URI relativeLocation = existingProject.getPathVariableManager().convertToRelative(URIUtil.toURI(fileLocation),
				true, ProjectLocationVariableResolver.NAME);
		fileLink.createLink(relativeLocation, IResource.ALLOW_MISSING_LOCAL, getMonitor());

		IProject destination = getWorkspace().getRoot().getProject("DestProject");
		IProjectDescription description = getWorkspace().newProjectDescription(destination.getName());

		assertDoesNotExistInWorkspace(destination);
		// without the fix, this call will cause an infinite loop in
		// PathVariableUtil.getUniqueVariableName()
		existingProject.move(description, IResource.SHALLOW, getMonitor());
		IProject destProject = ResourcesPlugin.getWorkspace().getRoot().getProject("DestProject");
		assertExistsInWorkspace(destProject);
		assertExistsInWorkspace(destProject.getFile(linkName));

	}
}
