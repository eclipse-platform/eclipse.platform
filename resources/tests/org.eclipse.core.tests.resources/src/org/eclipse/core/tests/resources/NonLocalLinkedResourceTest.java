/*******************************************************************************
 *  Copyright (c) 2006, 2015 IBM Corporation and others.
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
package org.eclipse.core.tests.resources;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.internal.filesystem.bogus.BogusFileSystem;
import org.eclipse.core.tests.internal.filesystem.ram.MemoryFileSystem;
import org.eclipse.core.tests.internal.filesystem.ram.MemoryTree;

/**
 * Tests behaviour of manipulating linked resources that are not linked into
 * the local file system.
 */
public class NonLocalLinkedResourceTest extends ResourceTest {
	private int nextFolder = 0;

	/**
	 * Creates a folder in the test file system with the given name
	 */
	protected IFileStore createFolderStore(String name) {
		IFileSystem system = getFileSystem();
		IFileStore store = system.getStore(IPath.ROOT.append(name));
		try {
			store.mkdir(EFS.NONE, getMonitor());
		} catch (CoreException e) {
			fail("createFolderStore", e);
		}
		return store;
	}

	protected IFileSystem getFileSystem() {
		try {
			return EFS.getFileSystem(MemoryFileSystem.SCHEME_MEMORY);
		} catch (CoreException e) {
			fail("Test file system missing", e);
		}
		//can't get here
		return null;
	}

	@Override
	protected IFileStore getTempStore() {
		IFileSystem system = getFileSystem();
		IFileStore store;
		do {
			store = system.getStore(IPath.ROOT.append(Integer.toString(nextFolder++)));
		} while (store.fetchInfo().exists());
		return store;
	}

	@Override
	protected void tearDown() throws Exception {
		MemoryTree.TREE.deleteAll();
		super.tearDown();
	}

	public void testCopyFile() {
		IFileStore sourceStore = createFolderStore("source");
		IFileStore destinationStore = createFolderStore("destination");
		IProject project = getWorkspace().getRoot().getProject("project");
		IFolder source = project.getFolder("source");
		IFolder destination = project.getFolder("destination");
		IFile sourceFile = source.getFile("file.txt");
		IFile destinationFile = destination.getFile(sourceFile.getName());
		IFile localFile = project.getFile(sourceFile.getName());

		//setup initial resources
		ensureExistsInWorkspace(project, true);
		try {
			source.createLink(sourceStore.toURI(), IResource.NONE, getMonitor());
			destination.createLink(destinationStore.toURI(), IResource.NONE, getMonitor());
			sourceFile.create(getRandomContents(), IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("0.99", e);
		}

		//copy to linked destination should succeed
		try {
			sourceFile.copy(destinationFile.getFullPath(), IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}
		//copy to local destination should succeed
		try {
			sourceFile.copy(localFile.getFullPath(), IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("2.0", e);
		}
		//copy from local to non local
		ensureDoesNotExistInWorkspace(destinationFile);
		//copy from local to non local
		try {
			localFile.copy(destinationFile.getFullPath(), IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("3.0", e);
		}

		//copy to self should fail
		try {
			localFile.copy(localFile.getFullPath(), IResource.NONE, getMonitor());
			fail("4.0");
		} catch (CoreException e) {
			//should fail
		}
	}

	public void testCopyFolder() {
		IFileStore sourceStore = createFolderStore("source");
		IProject project = getWorkspace().getRoot().getProject("project");
		IFolder parentFolder = project.getFolder("parent");
		IFolder source = parentFolder.getFolder("source");
		IFolder destination = project.getFolder("destination");

		//setup initial resources
		ensureExistsInWorkspace(project, true);
		try {
			parentFolder.create(IResource.NONE, true, getMonitor());
			source.createLink(sourceStore.toURI(), IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("0.99", e);
		}

		//shallow copy to destination should succeed
		try {
			source.copy(destination.getFullPath(), IResource.SHALLOW, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}
		assertTrue("1.1", destination.exists());

		//deep copy to destination should succeed
		try {
			destination.delete(IResource.NONE, getMonitor());
			source.copy(destination.getFullPath(), IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("2.0", e);
		}
		assertTrue("2.1", destination.exists());

		//should fail when destination is occupied
		try {
			source.copy(destination.getFullPath(), IResource.NONE, getMonitor());
			fail("3.0");
		} catch (CoreException e) {
			//should fail
		}

		//copy to self should fail
		try {
			source.copy(source.getFullPath(), IResource.NONE, getMonitor());
			fail("4.0");
		} catch (CoreException e) {
			//should fail
		}
	}

	public void testMoveFile() {
		IFileStore sourceStore = createFolderStore("source");
		IFileStore destinationStore = createFolderStore("destination");
		IProject project = getWorkspace().getRoot().getProject("project");
		IFolder source = project.getFolder("source");
		IFolder destination = project.getFolder("destination");
		IFile sourceFile = source.getFile("file.txt");
		IFile destinationFile = destination.getFile(sourceFile.getName());
		IFile localFile = project.getFile(sourceFile.getName());

		//setup initial resources
		ensureExistsInWorkspace(project, true);
		try {
			source.createLink(sourceStore.toURI(), IResource.NONE, getMonitor());
			destination.createLink(destinationStore.toURI(), IResource.NONE, getMonitor());
			sourceFile.create(getRandomContents(), IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("0.99", e);
		}

		//move to linked destination should succeed
		try {
			sourceFile.move(destinationFile.getFullPath(), IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}
		//move back to source location
		//move to linked destination should succeed
		try {
			destinationFile.move(sourceFile.getFullPath(), IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("1.1", e);
		}

		//move to local destination should succeed
		try {
			sourceFile.move(localFile.getFullPath(), IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("2.0", e);
		}
		//movefrom local to non local
		try {
			localFile.move(destinationFile.getFullPath(), IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("3.0", e);
		}

		//copy to self should fail
		try {
			localFile.copy(localFile.getFullPath(), IResource.NONE, getMonitor());
			fail("4.0");
		} catch (CoreException e) {
			//should fail
		}
	}

	// Test for Bug 342060 - Renaming a project failing with custom EFS
	public void test342060() {
		IFileStore sourceStore = createBogusFolderStore("source");
		IFileStore destinationStore = createBogusFolderStore("destination");
		IProject project = getWorkspace().getRoot().getProject("project");
		IFolder source = project.getFolder("source");
		IFolder destination = project.getFolder("destination");
		IFile sourceFile = source.getFile("file.txt");
		//setup initial resources
		ensureExistsInWorkspace(project, true);
		try {
			source.createLink(sourceStore.toURI(), IResource.NONE, getMonitor());
			destination.createLink(destinationStore.toURI(), IResource.NONE, getMonitor());
			sourceFile.create(getRandomContents(), IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("0.99", e);
		}

		//move to linked destination should succeed
		try {
			project.move(IPath.fromPortableString("movedProject"), IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}
	}

	protected IFileStore createBogusFolderStore(String name) {
		IFileSystem system = getBogusFileSystem();
		IFileStore store = system.getStore(IPath.ROOT.append(name));
		try {
			deleteOnTearDown(
					IPath.fromOSString(system.getStore(IPath.ROOT).toLocalFile(EFS.NONE, getMonitor()).getPath()));
			store.mkdir(EFS.NONE, getMonitor());
		} catch (CoreException e) {
			fail("createFolderStore", e);
		}
		return store;
	}

	protected IFileSystem getBogusFileSystem() {
		try {
			return EFS.getFileSystem(BogusFileSystem.SCHEME_BOGUS);
		} catch (CoreException e) {
			fail("Test file system missing", e);
		}
		//can't get here
		return null;
	}
}
