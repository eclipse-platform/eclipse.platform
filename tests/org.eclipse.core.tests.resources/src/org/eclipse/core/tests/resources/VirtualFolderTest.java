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

import org.eclipse.core.filesystem.*;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/**
 * Tests Virtual Folders
 */
public class VirtualFolderTest extends ResourceTest {
	protected IProject existingProject;
	protected IFolder existingVirtualFolderInExistingProject;

	protected void doCleanup() throws Exception {
		ensureExistsInWorkspace(new IResource[] {existingProject}, true);
		existingVirtualFolderInExistingProject.create(IResource.VIRTUAL, true, getMonitor());
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		existingProject = getWorkspace().getRoot().getProject("ExistingProject");
		existingVirtualFolderInExistingProject = existingProject.getFolder("existingVirtualFolderInExistingProject");
		doCleanup();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * Tests creating a virtual folder
	 */
	public void testCreateVirtualFolder() {
		IFolder virtualFolder = existingProject.getFolder(getUniqueString());

		try {
			virtualFolder.create(IResource.VIRTUAL, true, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}

		assertTrue("2.0", virtualFolder.exists());
		assertTrue("3.0", virtualFolder.isVirtual());

		// delete should succeed
		try {
			virtualFolder.delete(IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("4.0", e);
		}
	}

	/**
	 * Tests creating a file under a virtual folder
	 */
	public void testCreateFileUnderVirtualFolder() {
		IFile file = existingVirtualFolderInExistingProject.getFile(getUniqueString());
		boolean failed = false;
		try {
			create(file, true);
			fail("1.0");
		} catch (CoreException e) {
			failed = true;
		}

		assertTrue("2.0", !file.exists());
		assertTrue("3.0", failed);
	}

	/**
	 * Tests creating a folder under a virtual folder
	 */
	public void testCreateFolderUnderVirtualFolder() {
		IFolder folder = existingVirtualFolderInExistingProject.getFolder(getUniqueString());
		boolean failed = false;
		try {
			create(folder, true);
			fail("1.0");
		} catch (CoreException e) {
			failed = true;
		}

		assertTrue("2.0", !folder.exists());
		assertTrue("3.0", failed);
	}

	/**
	 * Tests creating a virtual folder under a virtual folder
	 */
	public void testCreateVirtualFolderUnderVirtualFolder() {
		IFolder virtualFolder = existingVirtualFolderInExistingProject.getFolder(getUniqueString());
		try {
			virtualFolder.create(IResource.VIRTUAL, true, null);
		} catch (CoreException e) {
			fail("1.0", e);
		}

		assertTrue("2.0", virtualFolder.exists());
		assertTrue("3.0", virtualFolder.isVirtual());

		// delete should succeed
		try {
			virtualFolder.delete(IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("4.0", e);
		}
	}

	/**
	 * Tests creating a linked folder under a virtual folder
	 */
	public void testCreateLinkedFolderUnderVirtualFolder() {
		// get a non-existing location
		IPath location = getRandomLocation();
		IFolder linkedFolder = existingVirtualFolderInExistingProject.getFolder(getUniqueString());

		try {
			linkedFolder.createLink(location, IResource.ALLOW_MISSING_LOCAL, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}

		assertTrue("2.0", linkedFolder.exists());
		assertEquals("3.0", location, linkedFolder.getLocation());
		assertTrue("4.0", !location.toFile().exists());

		// getting children should succeed (and be empty)
		try {
			assertEquals("5.0", 0, linkedFolder.members().length);
		} catch (CoreException e) {
			fail("6.0", e);
		}

		// delete should succeed
		try {
			linkedFolder.delete(IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("7.0", e);
		}
	}

	/**
	 * Tests creating a linked file under a virtual folder
	 */
	public void testCreateLinkedFileUnderVirtualFolder() {
		// get a non-existing location
		IPath location = getRandomLocation();
		IFile file = existingVirtualFolderInExistingProject.getFile(getUniqueString());

		try {
			file.createLink(location, IResource.ALLOW_MISSING_LOCAL, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}

		assertTrue("2.0", file.exists());
		assertEquals("3.0", location, file.getLocation());
		assertTrue("4.0", !location.toFile().exists());

		// delete should succeed
		try {
			file.delete(IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("5.0", e);
		}
	}

	public void testCopyProjectWithVirtualFolder() {
		IPath fileLocation = getRandomLocation();
		IPath folderLocation = getRandomLocation();

		IFile linkedFile = existingVirtualFolderInExistingProject.getFile(getUniqueString());
		IFolder linkedFolder = existingVirtualFolderInExistingProject.getFolder(getUniqueString());

		try {
			try {
				createFileInFileSystem(fileLocation, getRandomContents());
				folderLocation.toFile().mkdir();

				linkedFolder.createLink(folderLocation, IResource.NONE, getMonitor());
				linkedFile.createLink(fileLocation, IResource.NONE, getMonitor());
			} catch (CoreException e) {
				fail("1.0", e);
			}

			// copy the project
			IProject destinationProject = getWorkspace().getRoot().getProject("CopyTargetProject");
			try {
				existingProject.copy(destinationProject.getFullPath(), IResource.SHALLOW, getMonitor());
			} catch (CoreException e) {
				fail("2.0", e);
			}

			IFile newFile = destinationProject.getFile(linkedFile.getProjectRelativePath());
			assertTrue("3.0", newFile.isLinked());
			assertEquals("3.1", linkedFile.getLocation(), newFile.getLocation());
			assertTrue("3.2", newFile.getParent().isVirtual());

			IFolder newFolder = destinationProject.getFolder(linkedFolder.getProjectRelativePath());
			assertTrue("4.0", newFolder.isLinked());
			assertEquals("4.1", linkedFolder.getLocation(), newFolder.getLocation());
			assertTrue("4.2", newFolder.getParent().isVirtual());

			// test project deep copy
			try {
				destinationProject.delete(IResource.NONE, getMonitor());
				existingProject.copy(destinationProject.getFullPath(), IResource.NONE, getMonitor());
			} catch (CoreException e) {
				fail("5.0", e);
			}

			assertTrue("5.1", newFile.isLinked());
			assertEquals("5.2", linkedFile.getLocation(), newFile.getLocation());
			assertTrue("5.3", newFile.getParent().isVirtual());
			assertTrue("5.4", newFolder.isLinked());
			assertEquals("5.5", linkedFolder.getLocation(), newFolder.getLocation());
			assertTrue("5.6", newFolder.getParent().isVirtual());

			try {
				destinationProject.delete(IResource.NONE, getMonitor());
			} catch (CoreException e) {
				fail("6.0", e);
			}
		} finally {
			Workspace.clear(fileLocation.toFile());
			Workspace.clear(folderLocation.toFile());
		}
	}

	public void testMoveProjectWithVirtualFolder() {
		IPath fileLocation = getRandomLocation();
		IPath folderLocation = getRandomLocation();

		IFile file = existingVirtualFolderInExistingProject.getFile(getUniqueString());
		IFolder folder = existingVirtualFolderInExistingProject.getFolder(getUniqueString());
		IFile childFile = folder.getFile(getUniqueString());
		IResource[] oldResources = new IResource[] {existingProject, file, folder, childFile};

		try {
			assertDoesNotExistInWorkspace("1.0", new IResource[] {folder, file, childFile});

			try {
				createFileInFileSystem(fileLocation);
				folderLocation.toFile().mkdir();

				folder.createLink(folderLocation, IResource.NONE, getMonitor());
				file.createLink(fileLocation, IResource.NONE, getMonitor());

				childFile.create(getRandomContents(), true, getMonitor());
			} catch (CoreException e) {
				fail("2.0", e);
			}

			// move the project
			IProject destinationProject = getWorkspace().getRoot().getProject("MoveTargetProject");
			assertDoesNotExistInWorkspace("3.0", destinationProject);

			try {
				existingProject.move(destinationProject.getFullPath(), IResource.SHALLOW, getMonitor());
			} catch (CoreException e) {
				fail("4.0", e);
			}

			IFile newFile = destinationProject.getFile(file.getProjectRelativePath());
			IFolder newFolder = destinationProject.getFolder(folder.getProjectRelativePath());
			IFile newChildFile = newFolder.getFile(childFile.getName());
			IResource[] newResources = new IResource[] {destinationProject, newFile, newFolder, newChildFile};

			assertExistsInWorkspace("5.0", newResources);
			assertDoesNotExistInWorkspace("6.1", oldResources);
			assertTrue("7.0", existingProject.isSynchronized(IResource.DEPTH_INFINITE));
			assertTrue("8.0", destinationProject.isSynchronized(IResource.DEPTH_INFINITE));

			assertTrue("9.0", newFile.getParent().isVirtual());
			assertTrue("10.0", newFile.isLinked());

			assertTrue("11.0", newFolder.isLinked());
			assertTrue("12.0", newFolder.getParent().isVirtual());
		} finally {
			Workspace.clear(fileLocation.toFile());
			Workspace.clear(folderLocation.toFile());
		}
	}

	public void testDeleteProjectWithVirtualFolder() {
		IFolder virtualFolder = existingProject.getFolder(getUniqueString());

		try {
			virtualFolder.create(IResource.VIRTUAL, true, null);
			existingProject.delete(IResource.NEVER_DELETE_PROJECT_CONTENT, getMonitor());
			existingProject.create(getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}

		// virtual folder should not exist until the project is open
		assertTrue("2.0", !virtualFolder.exists());

		try {
			existingProject.open(getMonitor());
		} catch (CoreException e) {
			fail("3.0", e);
		}

		// virtual folder should now exist
		assertTrue("4.0", virtualFolder.exists());
		assertTrue("5.0", virtualFolder.isVirtual());
	}

	public void testDeleteProjectWithVirtualFolderAndLink() {
		IPath folderLocation = getRandomLocation();

		IFolder virtualFolder = existingProject.getFolder(getUniqueString());
		IFolder linkedFolder = virtualFolder.getFolder("a_link");

		try {
			try {
				folderLocation.toFile().mkdir();
				virtualFolder.create(IResource.VIRTUAL, true, null);
				linkedFolder.createLink(folderLocation, IResource.NONE, getMonitor());
				existingProject.delete(IResource.NEVER_DELETE_PROJECT_CONTENT, getMonitor());
				existingProject.create(getMonitor());
			} catch (CoreException e) {
				fail("1.0", e);
			}

			// virtual folder should not exist until the project is open
			assertTrue("2.0", !virtualFolder.exists());
			assertTrue("3.0", !linkedFolder.exists());

			try {
				existingProject.open(getMonitor());
			} catch (CoreException e) {
				fail("4.0", e);
			}

			// virtual folder should now exist
			assertTrue("5.0", virtualFolder.exists());
			assertTrue("6.0", virtualFolder.isVirtual());

			// link should now exist
			assertTrue("7.0", linkedFolder.exists());
			assertTrue("8.0", linkedFolder.isLinked());

			assertEquals("9.0", folderLocation, linkedFolder.getLocation());
		} finally {
			Workspace.clear(folderLocation.toFile());
		}
	}

	public void testLinkedFolderInVirtualFolder_FileStoreURI() {
		IPath folderLocation = getRandomLocation();
		IFolder folder = existingVirtualFolderInExistingProject.getFolder(getUniqueString());

		try {
			folder.createLink(folderLocation, IResource.ALLOW_MISSING_LOCAL, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}

		assertTrue("2.0", folder.exists());
		assertEquals("3.0", folderLocation, folder.getLocation());
		assertTrue("4.0", !folderLocation.toFile().exists());

		// Check file store URI for the linked resource
		try {
			IFileStore fs = EFS.getStore(existingVirtualFolderInExistingProject.getLocationURI());
			fs = fs.getChild(folder.getName());
			assertNotNull("5.0", fs);
			assertNotNull("6.0", fs.toURI());
		} catch (CoreException e) {
			fail("7.0", e);
		}
	}

	public void testIsVirtual() {
		// create a virtual folder
		IFolder virtualFolder = existingProject.getFolder(getUniqueString());
		try {
			virtualFolder.create(IResource.VIRTUAL, true, null);
		} catch (CoreException e) {
			fail("1.0", e);
		}
		assertTrue("2.0", virtualFolder.isVirtual());
	}

	//	We should decide what is the proper value returned for Virtual Folders
	//
	//	public void testIsLocal() {
	//		// create a virtual folder
	//		IFolder virtualFolder = existingProject.getFolder(getUniqueString());
	//		try {
	//			virtualFolder.createGroup(IResource.NONE, null);
	//		} catch (CoreException e) {
	//			fail("1.0", e);
	//		}
	//		assertTrue("2.0", virtualFolder.isLocal(IResource.DEPTH_INFINITE));
	//	}
	//
	//	public void testIsSynchronizedforVirtualFolder() {
	//		// create a virtual folder
	//		IFolder virtualFolder = existingProject.getFolder(getUniqueString());
	//		try {
	//			virtualFolder.createGroup(IResource.NONE, null);
	//		} catch (CoreException e) {
	//			fail("1.0", e);
	//		}
	//		assertTrue("2.0", virtualFolder.isSynchronized(IResource.DEPTH_INFINITE));
	//	}

	public void testVirtualFolderInLinkedFolder() {
		// setup handles
		IFolder topFolder = existingProject.getFolder("topFolder");
		IFolder linkedFolder = topFolder.getFolder("linkedFolder");
		IFolder subFolder = linkedFolder.getFolder("subFolder");
		IFolder virtualFolder = subFolder.getFolder("virtualFolder");

		IPath linkedFolderLocation = getRandomLocation();
		IPath subFolderLocation = linkedFolderLocation.append(subFolder.getName());

		try {
			try {
				// create the structure on disk
				linkedFolderLocation.toFile().mkdir();
				subFolderLocation.toFile().mkdir();

				// create the structure in the workspace
				ensureExistsInWorkspace(topFolder, true);
				linkedFolder.createLink(linkedFolderLocation, IResource.NONE, getMonitor());
				virtualFolder.create(IResource.VIRTUAL, true, getMonitor());
			} catch (CoreException e) {
				fail("1.0", e);
			}

			// assert locations
			assertEquals("2.0", linkedFolderLocation, linkedFolder.getLocation());
			assertEquals("3.0", linkedFolderLocation.append(subFolder.getName()), subFolder.getLocation());
			assertTrue("4.0", virtualFolder.isVirtual());
			assertTrue("5.0", virtualFolder.getLocation() == null);

			// assert URIs
			assertEquals("6.0", URIUtil.toURI(linkedFolderLocation), linkedFolder.getLocationURI());
			assertEquals("7.0", URIUtil.toURI(subFolderLocation), subFolder.getLocationURI());
			// assertTrue("8.0", virtualFolder.getLocationURI() == null);
		} finally {
			Workspace.clear(subFolderLocation.toFile());
			Workspace.clear(linkedFolderLocation.toFile());
		}
	}

	/* Regression for Bug 296470 */
	public void testGetVirtualFolderAttributes() {
		long timeStamp = existingVirtualFolderInExistingProject.getLocalTimeStamp();
		assertEquals("1.0", timeStamp, IResource.NULL_STAMP);

		ResourceAttributes attributes = existingVirtualFolderInExistingProject.getResourceAttributes();
		assertEquals("1.1", attributes, null);
	}
}
