/*******************************************************************************
 * Copyright (c) 2008, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Serge Beauchamp (Freescale Semiconductor) - initial API and implementation
 *     James Blackburn - Test fix for bug 266712
 *     IBM - ongoing development
 *******************************************************************************/
package org.eclipse.core.tests.resources;

import static org.junit.Assert.assertThrows;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/**
 * Tests Virtual Folders
 */
public class VirtualFolderTest extends ResourceTest {
	protected IProject existingProject;
	protected IFolder existingVirtualFolderInExistingProject;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		existingProject = getWorkspace().getRoot().getProject("ExistingProject");
		existingVirtualFolderInExistingProject = existingProject.getFolder("existingVirtualFolderInExistingProject");
		ensureExistsInWorkspace(new IResource[] { existingProject }, true);
		existingVirtualFolderInExistingProject.create(IResource.VIRTUAL, true, getMonitor());
	}

	/**
	 * Tests creating a virtual folder
	 */
	public void testCreateVirtualFolder() throws CoreException {
		IFolder virtualFolder = existingProject.getFolder(getUniqueString());

		virtualFolder.create(IResource.VIRTUAL, true, getMonitor());

		assertTrue("2.0", virtualFolder.exists());
		assertTrue("3.0", virtualFolder.isVirtual());

		// delete should succeed
		virtualFolder.delete(IResource.NONE, getMonitor());
	}

	/**
	 * Tests creating a file under a virtual folder
	 */
	public void testCreateFileUnderVirtualFolder() {
		IFile file = existingVirtualFolderInExistingProject.getFile(getUniqueString());
		assertThrows(CoreException.class, () -> create(file, true));
		assertTrue("2.0", !file.exists());
	}

	/**
	 * Tests creating a folder under a virtual folder
	 */
	public void testCreateFolderUnderVirtualFolder() {
		IFolder folder = existingVirtualFolderInExistingProject.getFolder(getUniqueString());
		assertThrows(CoreException.class, () -> create(folder, true));
		assertTrue("2.0", !folder.exists());
	}

	/**
	 * Tests creating a virtual folder under a virtual folder
	 */
	public void testCreateVirtualFolderUnderVirtualFolder() throws CoreException {
		IFolder virtualFolder = existingVirtualFolderInExistingProject.getFolder(getUniqueString());
		virtualFolder.create(IResource.VIRTUAL, true, null);

		assertTrue("2.0", virtualFolder.exists());
		assertTrue("3.0", virtualFolder.isVirtual());

		// delete should succeed
		virtualFolder.delete(IResource.NONE, getMonitor());
	}

	/**
	 * Tests creating a linked folder under a virtual folder
	 */
	public void testCreateLinkedFolderUnderVirtualFolder() throws CoreException {
		// get a non-existing location
		IPath location = getRandomLocation();
		IFolder linkedFolder = existingVirtualFolderInExistingProject.getFolder(getUniqueString());

		linkedFolder.createLink(location, IResource.ALLOW_MISSING_LOCAL, getMonitor());

		assertTrue("2.0", linkedFolder.exists());
		assertEquals("3.0", location, linkedFolder.getLocation());
		assertTrue("4.0", !location.toFile().exists());

		// getting children should succeed (and be empty)
		assertEquals("5.0", 0, linkedFolder.members().length);

		// delete should succeed
		linkedFolder.delete(IResource.NONE, getMonitor());
	}

	/**
	 * Tests creating a linked file under a virtual folder
	 */
	public void testCreateLinkedFileUnderVirtualFolder() throws CoreException {
		// get a non-existing location
		IPath location = getRandomLocation();
		IFile file = existingVirtualFolderInExistingProject.getFile(getUniqueString());

		file.createLink(location, IResource.ALLOW_MISSING_LOCAL, getMonitor());

		assertTrue("2.0", file.exists());
		assertEquals("3.0", location, file.getLocation());
		assertTrue("4.0", !location.toFile().exists());

		// delete should succeed
		file.delete(IResource.NONE, getMonitor());
	}

	public void testCopyProjectWithVirtualFolder() throws CoreException {
		IPath fileLocation = getRandomLocation();
		deleteOnTearDown(fileLocation);
		IPath folderLocation = getRandomLocation();
		deleteOnTearDown(folderLocation);

		IFile linkedFile = existingVirtualFolderInExistingProject.getFile(getUniqueString());
		IFolder linkedFolder = existingVirtualFolderInExistingProject.getFolder(getUniqueString());

		createFileInFileSystem(fileLocation, getRandomContents());
		folderLocation.toFile().mkdir();

		linkedFolder.createLink(folderLocation, IResource.NONE, getMonitor());
		linkedFile.createLink(fileLocation, IResource.NONE, getMonitor());

		// copy the project
		IProject destinationProject = getWorkspace().getRoot().getProject("CopyTargetProject");
		existingProject.copy(destinationProject.getFullPath(), IResource.SHALLOW, getMonitor());

		IFile newFile = destinationProject.getFile(linkedFile.getProjectRelativePath());
		assertTrue("3.0", newFile.isLinked());
		assertEquals("3.1", linkedFile.getLocation(), newFile.getLocation());
		assertTrue("3.2", newFile.getParent().isVirtual());

		IFolder newFolder = destinationProject.getFolder(linkedFolder.getProjectRelativePath());
		assertTrue("4.0", newFolder.isLinked());
		assertEquals("4.1", linkedFolder.getLocation(), newFolder.getLocation());
		assertTrue("4.2", newFolder.getParent().isVirtual());

		// test project deep copy
		destinationProject.delete(IResource.NONE, getMonitor());
		existingProject.copy(destinationProject.getFullPath(), IResource.NONE, getMonitor());

		assertTrue("5.1", newFile.isLinked());
		assertEquals("5.2", linkedFile.getLocation(), newFile.getLocation());
		assertTrue("5.3", newFile.getParent().isVirtual());
		assertTrue("5.4", newFolder.isLinked());
		assertEquals("5.5", linkedFolder.getLocation(), newFolder.getLocation());
		assertTrue("5.6", newFolder.getParent().isVirtual());

		destinationProject.delete(IResource.NONE, getMonitor());
	}

	public void testMoveProjectWithVirtualFolder() throws CoreException {
		IPath fileLocation = getRandomLocation();
		deleteOnTearDown(fileLocation);
		IPath folderLocation = getRandomLocation();
		deleteOnTearDown(folderLocation);

		IFile file = existingVirtualFolderInExistingProject.getFile(getUniqueString());
		IFolder folder = existingVirtualFolderInExistingProject.getFolder(getUniqueString());
		IFile childFile = folder.getFile(getUniqueString());
		IResource[] oldResources = new IResource[] {existingProject, file, folder, childFile};

		assertDoesNotExistInWorkspace(new IResource[] { folder, file, childFile });

		createFileInFileSystem(fileLocation);
		folderLocation.toFile().mkdir();

		folder.createLink(folderLocation, IResource.NONE, getMonitor());
		file.createLink(fileLocation, IResource.NONE, getMonitor());

		childFile.create(getRandomContents(), true, getMonitor());

		// move the project
		IProject destinationProject = getWorkspace().getRoot().getProject("MoveTargetProject");
		assertDoesNotExistInWorkspace(destinationProject);

		existingProject.move(destinationProject.getFullPath(), IResource.SHALLOW, getMonitor());

		IFile newFile = destinationProject.getFile(file.getProjectRelativePath());
		IFolder newFolder = destinationProject.getFolder(folder.getProjectRelativePath());
		IFile newChildFile = newFolder.getFile(childFile.getName());
		IResource[] newResources = new IResource[] { destinationProject, newFile, newFolder, newChildFile };

		assertExistsInWorkspace(newResources);
		assertDoesNotExistInWorkspace(oldResources);
		assertTrue("7.0", existingProject.isSynchronized(IResource.DEPTH_INFINITE));
		assertTrue("8.0", destinationProject.isSynchronized(IResource.DEPTH_INFINITE));

		assertTrue("9.0", newFile.getParent().isVirtual());
		assertTrue("10.0", newFile.isLinked());

		assertTrue("11.0", newFolder.isLinked());
		assertTrue("12.0", newFolder.getParent().isVirtual());
	}

	public void testDeleteProjectWithVirtualFolder() throws CoreException {
		IFolder virtualFolder = existingProject.getFolder(getUniqueString());

		virtualFolder.create(IResource.VIRTUAL, true, null);
		existingProject.delete(IResource.NEVER_DELETE_PROJECT_CONTENT, getMonitor());
		existingProject.create(getMonitor());

		// virtual folder should not exist until the project is open
		assertTrue("2.0", !virtualFolder.exists());

		existingProject.open(getMonitor());

		// virtual folder should now exist
		assertTrue("4.0", virtualFolder.exists());
		assertTrue("5.0", virtualFolder.isVirtual());
	}

	public void testDeleteProjectWithVirtualFolderAndLink() throws CoreException {
		IPath folderLocation = getRandomLocation();
		deleteOnTearDown(folderLocation);

		IFolder virtualFolder = existingProject.getFolder(getUniqueString());
		IFolder linkedFolder = virtualFolder.getFolder("a_link");

		folderLocation.toFile().mkdir();
		virtualFolder.create(IResource.VIRTUAL, true, null);
		linkedFolder.createLink(folderLocation, IResource.NONE, getMonitor());
		existingProject.delete(IResource.NEVER_DELETE_PROJECT_CONTENT, getMonitor());
		existingProject.create(getMonitor());

		// virtual folder should not exist until the project is open
		assertTrue("2.0", !virtualFolder.exists());
		assertTrue("3.0", !linkedFolder.exists());

		existingProject.open(getMonitor());

		// virtual folder should now exist
		assertTrue("5.0", virtualFolder.exists());
		assertTrue("6.0", virtualFolder.isVirtual());

		// link should now exist
		assertTrue("7.0", linkedFolder.exists());
		assertTrue("8.0", linkedFolder.isLinked());

		assertEquals("9.0", folderLocation, linkedFolder.getLocation());
	}

	public void testLinkedFolderInVirtualFolder_FileStoreURI() throws CoreException {
		IPath folderLocation = getRandomLocation();
		IFolder folder = existingVirtualFolderInExistingProject.getFolder(getUniqueString());

		folder.createLink(folderLocation, IResource.ALLOW_MISSING_LOCAL, getMonitor());

		assertTrue("2.0", folder.exists());
		assertEquals("3.0", folderLocation, folder.getLocation());
		assertTrue("4.0", !folderLocation.toFile().exists());

		// Check file store URI for the linked resource
		IFileStore fs = EFS.getStore(existingVirtualFolderInExistingProject.getLocationURI());
		fs = fs.getChild(folder.getName());
		assertNotNull("5.0", fs);
		assertNotNull("6.0", fs.toURI());
	}

	public void testIsVirtual() throws CoreException {
		// create a virtual folder
		IFolder virtualFolder = existingProject.getFolder(getUniqueString());
		virtualFolder.create(IResource.VIRTUAL, true, null);
		assertTrue("2.0", virtualFolder.isVirtual());
	}

	public void testVirtualFolderInLinkedFolder() throws CoreException {
		// setup handles
		IFolder topFolder = existingProject.getFolder("topFolder");
		IFolder linkedFolder = topFolder.getFolder("linkedFolder");
		IFolder subFolder = linkedFolder.getFolder("subFolder");
		IFolder virtualFolder = subFolder.getFolder("virtualFolder");

		IPath linkedFolderLocation = getRandomLocation();
		deleteOnTearDown(linkedFolderLocation);
		IPath subFolderLocation = linkedFolderLocation.append(subFolder.getName());
		deleteOnTearDown(subFolderLocation);

		// create the structure on disk
		linkedFolderLocation.toFile().mkdir();
		subFolderLocation.toFile().mkdir();

		// create the structure in the workspace
		ensureExistsInWorkspace(topFolder, true);
		linkedFolder.createLink(linkedFolderLocation, IResource.NONE, getMonitor());
		virtualFolder.create(IResource.VIRTUAL, true, getMonitor());

		// assert locations
		assertEquals("2.0", linkedFolderLocation, linkedFolder.getLocation());
		assertEquals("3.0", linkedFolderLocation.append(subFolder.getName()), subFolder.getLocation());
		assertTrue("4.0", virtualFolder.isVirtual());
		assertTrue("5.0", virtualFolder.getLocation() == null);

		// assert URIs
		assertEquals("6.0", URIUtil.toURI(linkedFolderLocation), linkedFolder.getLocationURI());
		assertEquals("7.0", URIUtil.toURI(subFolderLocation), subFolder.getLocationURI());
		// assertTrue("8.0", virtualFolder.getLocationURI() == null);
	}

	/* Regression for Bug 296470 */
	public void testGetVirtualFolderAttributes() {
		long timeStamp = existingVirtualFolderInExistingProject.getLocalTimeStamp();
		assertEquals("1.0", timeStamp, IResource.NULL_STAMP);

		ResourceAttributes attributes = existingVirtualFolderInExistingProject.getResourceAttributes();
		assertEquals("1.1", attributes, null);
	}
}
