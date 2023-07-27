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

import java.io.ByteArrayInputStream;
import java.net.URI;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.internal.resources.Workspace;
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

	protected void doCleanup() throws Exception {
		ensureExistsInWorkspace(new IResource[] {existingProject, otherExistingProject}, true);
	}

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
		doCleanup();
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

	public void testFileLinkedToNonExistent_Deep() {
		IFile fileLink = existingProject.getFile(getUniqueString());
		IPath fileLocation = getRandomLocation();
		try {
			fileLink.createLink(fileLocation, IResource.ALLOW_MISSING_LOCAL, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}

		try {
			fileLink.setContents(new ByteArrayInputStream(getRandomString().getBytes()), IResource.NONE, getMonitor());
			fail("1.1");
		} catch (CoreException e) {
			// should fail
			assertEquals("1.2", IResourceStatus.NOT_FOUND_LOCAL, e.getStatus().getCode());
		}

		assertTrue("2.0", fileLink.isSynchronized(IResource.DEPTH_INFINITE));
		internalMovedAndCopyTest(fileLink, IResource.NONE, false);

		createFileInFileSystem(fileLocation);

		try {
			fileLink.setContents(new ByteArrayInputStream(getRandomString().getBytes()), IResource.NONE, getMonitor());
			fail("2.1");
		} catch (CoreException e) {
			// should fail
			assertEquals("2.2", IResourceStatus.OUT_OF_SYNC_LOCAL, e.getStatus().getCode());
		}

		try {
			assertFalse("3.0", fileLink.isSynchronized(IResource.DEPTH_INFINITE));
			internalMovedAndCopyTest(fileLink, IResource.NONE, false);

			try {
				fileLink.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
			} catch (CoreException e) {
				fail("4.0", e);
			}

			assertTrue("5.0", fileLink.isSynchronized(IResource.DEPTH_INFINITE));
			internalMovedAndCopyTest(fileLink, IResource.NONE, true);
		} finally {
			Workspace.clear(resolve(fileLocation).toFile());
		}
	}

	public void testFileLinkedToNonExistent_Shallow() {
		IFile fileLink = existingProject.getFile(getUniqueString());
		IPath fileLocation = getRandomLocation();
		try {
			fileLink.createLink(fileLocation, IResource.ALLOW_MISSING_LOCAL, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}

		assertTrue("2.0", fileLink.isSynchronized(IResource.DEPTH_INFINITE));
		internalMovedAndCopyTest(fileLink, IResource.SHALLOW, true);

		createFileInFileSystem(fileLocation);

		try {
			assertFalse("3.0", fileLink.isSynchronized(IResource.DEPTH_INFINITE));
			internalMovedAndCopyTest(fileLink, IResource.SHALLOW, true);

			try {
				fileLink.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
			} catch (CoreException e) {
				fail("4.0", e);
			}

			assertTrue("5.0", fileLink.isSynchronized(IResource.DEPTH_INFINITE));
			internalMovedAndCopyTest(fileLink, IResource.SHALLOW, true);
		} finally {
			Workspace.clear(resolve(fileLocation).toFile());
		}
	}

	public void testFolderLinkedToNonExistent_Deep() {
		IFolder folderLink = existingProject.getFolder(getUniqueString());
		IPath folderLocation = getRandomLocation();
		try {
			folderLink.createLink(folderLocation, IResource.ALLOW_MISSING_LOCAL, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}

		assertTrue("3.0", folderLink.isSynchronized(IResource.DEPTH_INFINITE));
		internalMovedAndCopyTest(folderLink, IResource.NONE, false);

		folderLocation.toFile().mkdir();

		try {
			assertFalse("3.0", folderLink.isSynchronized(IResource.DEPTH_INFINITE));
			internalMovedAndCopyTest(folderLink, IResource.NONE, true);

			try {
				folderLink.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
			} catch (CoreException e) {
				fail("4.0", e);
			}

			assertTrue("5.0", folderLink.isSynchronized(IResource.DEPTH_INFINITE));
			internalMovedAndCopyTest(folderLink, IResource.NONE, true);
		} finally {
			Workspace.clear(resolve(folderLocation).toFile());
		}
	}

	public void testFolderLinkedToNonExistent_Shallow() {
		IFolder folderLink = existingProject.getFolder(getUniqueString());
		IPath folderLocation = getRandomLocation();
		try {
			folderLink.createLink(folderLocation, IResource.ALLOW_MISSING_LOCAL, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}

		assertTrue("2.0", folderLink.isSynchronized(IResource.DEPTH_INFINITE));
		internalMovedAndCopyTest(folderLink, IResource.SHALLOW, true);

		folderLocation.toFile().mkdir();

		try {
			assertFalse("3.0", folderLink.isSynchronized(IResource.DEPTH_INFINITE));
			internalMovedAndCopyTest(folderLink, IResource.SHALLOW, true);

			try {
				folderLink.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
			} catch (CoreException e) {
				fail("4.0", e);
			}

			assertTrue("5.0", folderLink.isSynchronized(IResource.DEPTH_INFINITE));
			internalMovedAndCopyTest(folderLink, IResource.SHALLOW, true);
		} finally {
			Workspace.clear(resolve(folderLocation).toFile());
		}
	}

	/**
	 * Tests bug 299024.
	 */
	public void testMoveFolderWithLinksToNonExisitngLocations_withShallow() {
		// create a folder
		IFolder folderWithLinks = existingProject.getFolder(getUniqueString());
		try {
			folderWithLinks.create(true, true, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}

		// non-exisitng location
		IPath fileLocation = getRandomLocation();

		// create a linked file in the folder
		IFile linkedFile = folderWithLinks.getFile(getUniqueString());
		try {
			linkedFile.createLink(fileLocation, IResource.ALLOW_MISSING_LOCAL, getMonitor());
		} catch (CoreException e) {
			fail("2.0", e);
		}

		// move the folder
		try {
			folderWithLinks.move(otherExistingProject.getFolder(getUniqueString()).getFullPath(), IResource.SHALLOW, getMonitor());
		} catch (CoreException e) {
			fail("3.0", e);
		}

		// move the folder
		try {
			folderWithLinks.move(otherExistingProject.getFolder(getUniqueString()).getFullPath(), IResource.NONE, getMonitor());
			fail("3.0");
		} catch (CoreException e) {

		}

		// both the folder and link in the source project should not exist
		assertFalse("5.0", folderWithLinks.exists());
		assertFalse("6.0", linkedFile.exists());
	}

	/**
	 * Tests bug 299024.
	 */
	public void _testCopyFolderWithLinksToNonExisitngLocations_withShallow() {
		// create a folder
		IFolder folderWithLinks = existingProject.getFolder(getUniqueString());
		try {
			folderWithLinks.create(true, true, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}

		// non-exisitng location
		IPath fileLocation = getRandomLocation();

		// create a linked file in the folder
		IFile linkedFile = folderWithLinks.getFile(getUniqueString());
		try {
			linkedFile.createLink(fileLocation, IResource.ALLOW_MISSING_LOCAL, getMonitor());
		} catch (CoreException e) {
			fail("2.0", e);
		}

		// copy the folder
		try {
			folderWithLinks.copy(otherExistingProject.getFolder(getUniqueString()).getFullPath(), IResource.SHALLOW, getMonitor());
		} catch (CoreException e) {
			fail("3.0", e);
		}

		try {
			folderWithLinks.copy(otherExistingProject.getFolder(getUniqueString()).getFullPath(), IResource.NONE, getMonitor());
			fail("3.0");
		} catch (CoreException e) {

		}

		// both the folder and link in the source project should exist
		assertTrue("5.0", folderWithLinks.exists());
		assertTrue("6.0", linkedFile.exists());
	}

	public void testFolderWithFileLinkedToNonExistent_Deep() {
		IFolder folder = existingProject.getFolder(getUniqueString());
		ensureExistsInWorkspace(folder, true);

		IFile fileLinkInFolder = folder.getFile(getUniqueString());

		IPath fileLocation = getRandomLocation();
		try {
			fileLinkInFolder.createLink(fileLocation, IResource.ALLOW_MISSING_LOCAL, getMonitor());
		} catch (CoreException e1) {
			fail("4.99", e1);
		}

		assertTrue(folder.isSynchronized(IResource.DEPTH_INFINITE));
		internalMovedAndCopyTest(folder, IResource.NONE, false);

		createFileInFileSystem(fileLocation);

		try {
			assertFalse(folder.isSynchronized(IResource.DEPTH_INFINITE));
			internalMovedAndCopyTest(folder, IResource.NONE, false);

			try {
				folder.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
			} catch (CoreException e) {
				fail("4.99", e);
			}

			assertTrue(folder.isSynchronized(IResource.DEPTH_INFINITE));
			internalMovedAndCopyTest(folder, IResource.NONE, true);
		} finally {
			Workspace.clear(resolve(fileLocation).toFile());
		}
	}

	public void testFolderWithFileLinkedToNonExistent_Shallow() {
		IFolder folder = existingProject.getFolder(getUniqueString());
		ensureExistsInWorkspace(folder, true);

		IFile fileLinkInFolder = folder.getFile(getUniqueString());

		IPath fileLocation = getRandomLocation();
		try {
			fileLinkInFolder.createLink(fileLocation, IResource.ALLOW_MISSING_LOCAL, getMonitor());
		} catch (CoreException e1) {
			fail("4.99", e1);
		}

		assertTrue(folder.isSynchronized(IResource.DEPTH_INFINITE));
		internalMovedAndCopyTest(folder, IResource.SHALLOW, true);

		createFileInFileSystem(fileLocation);

		try {
			assertFalse(folder.isSynchronized(IResource.DEPTH_INFINITE));
			internalMovedAndCopyTest(folder, IResource.SHALLOW, true);

			try {
				folder.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
			} catch (CoreException e) {
				fail("4.99", e);
			}

			assertTrue(folder.isSynchronized(IResource.DEPTH_INFINITE));
			internalMovedAndCopyTest(folder, IResource.SHALLOW, true);
		} finally {
			Workspace.clear(resolve(fileLocation).toFile());
		}
	}

	public void testFolderWithFolderLinkedToNonExistent_Deep() {
		IFolder folder = existingProject.getFolder(getUniqueString());
		ensureExistsInWorkspace(folder, true);

		IFolder folderLinkInFolder = folder.getFolder(getUniqueString());

		IPath folderLocation = getRandomLocation();
		try {
			folderLinkInFolder.createLink(folderLocation, IResource.ALLOW_MISSING_LOCAL, getMonitor());
		} catch (CoreException e1) {
			fail("4.99", e1);
		}

		assertTrue(folder.isSynchronized(IResource.DEPTH_INFINITE));
		internalMovedAndCopyTest(folder, IResource.NONE, false);

		folderLocation.toFile().mkdir();

		try {
			assertFalse(folder.isSynchronized(IResource.DEPTH_INFINITE));
			internalMovedAndCopyTest(folder, IResource.NONE, true);

			try {
				folder.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
			} catch (CoreException e) {
				fail("4.99", e);
			}

			assertTrue(folder.isSynchronized(IResource.DEPTH_INFINITE));
			internalMovedAndCopyTest(folder, IResource.NONE, true);
		} finally {
			Workspace.clear(resolve(folderLocation).toFile());
		}
	}

	public void testFolderWithFolderLinkedToNonExistent_Shallow() {
		IFolder folder = existingProject.getFolder(getUniqueString());
		ensureExistsInWorkspace(folder, true);

		IFolder folderLinkInFolder = folder.getFolder(getUniqueString());

		IPath folderLocation = getRandomLocation();
		try {
			folderLinkInFolder.createLink(folderLocation, IResource.ALLOW_MISSING_LOCAL, getMonitor());
		} catch (CoreException e1) {
			fail("4.99", e1);
		}

		assertTrue(folder.isSynchronized(IResource.DEPTH_INFINITE));
		internalMovedAndCopyTest(folder, IResource.SHALLOW, true);

		folderLocation.toFile().mkdir();

		try {
			assertFalse(folder.isSynchronized(IResource.DEPTH_INFINITE));
			internalMovedAndCopyTest(folder, IResource.SHALLOW, true);

			try {
				folder.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
			} catch (CoreException e) {
				fail("4.99", e);
			}

			assertTrue(folder.isSynchronized(IResource.DEPTH_INFINITE));
			internalMovedAndCopyTest(folder, IResource.SHALLOW, true);
		} finally {
			Workspace.clear(resolve(folderLocation).toFile());
		}
	}

	public void test361201() {
		String linkName = getUniqueString();
		IFile fileLink = existingProject.getFile(linkName);
		IFile file = existingProject.getFolder("dir").getFile("foo.txt");

		ensureExistsInWorkspace(file.getParent(), true);
		ensureExistsInWorkspace(file, "content");
		IPath fileLocation = file.getLocation();

		URI relativeLocation = null;
		try {
			relativeLocation = existingProject.getPathVariableManager().convertToRelative(URIUtil.toURI(fileLocation), true, ProjectLocationVariableResolver.NAME);
		} catch (CoreException e) {
			fail("0.99", e);
		}

		try {
			fileLink.createLink(relativeLocation, IResource.ALLOW_MISSING_LOCAL, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}

		IProject destination = getWorkspace().getRoot().getProject("DestProject");
		IProjectDescription description = getWorkspace().newProjectDescription(destination.getName());

		assertDoesNotExistInWorkspace("1.1", destination);
		try {
			// without the fix, this call will cause an infinite loop in PathVariableUtil.getUniqueVariableName()
			existingProject.move(description, IResource.SHALLOW, getMonitor());
		} catch (CoreException e) {
			fail("1.2", e);
		}
		IProject destProject = ResourcesPlugin.getWorkspace().getRoot().getProject("DestProject");
		assertExistsInWorkspace("2.0", destProject);
		assertExistsInWorkspace("2.1", destProject.getFile(linkName));

	}
}
