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

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.junit.Assert.assertThrows;

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
	/**
	 * Creates a folder in the test file system with the given name
	 */
	protected IFileStore createFolderStore(String name) throws CoreException {
		IFileSystem system = getFileSystem();
		IFileStore store = system.getStore(IPath.ROOT.append(name));
		store.mkdir(EFS.NONE, createTestMonitor());
		return store;
	}

	protected IFileSystem getFileSystem() throws CoreException {
		return EFS.getFileSystem(MemoryFileSystem.SCHEME_MEMORY);
	}

	@Override
	protected void tearDown() throws Exception {
		MemoryTree.TREE.deleteAll();
		super.tearDown();
	}

	public void testCopyFile() throws CoreException {
		IFileStore sourceStore = createFolderStore("source");
		IFileStore destinationStore = createFolderStore("destination");
		IProject project = getWorkspace().getRoot().getProject("project");
		IFolder source = project.getFolder("source");
		IFolder destination = project.getFolder("destination");
		IFile sourceFile = source.getFile("file.txt");
		IFile destinationFile = destination.getFile(sourceFile.getName());
		IFile localFile = project.getFile(sourceFile.getName());

		//setup initial resources
		ensureExistsInWorkspace(project);
		source.createLink(sourceStore.toURI(), IResource.NONE, createTestMonitor());
		destination.createLink(destinationStore.toURI(), IResource.NONE, createTestMonitor());
		sourceFile.create(getRandomContents(), IResource.NONE, createTestMonitor());

		//copy to linked destination should succeed
		sourceFile.copy(destinationFile.getFullPath(), IResource.NONE, createTestMonitor());
		//copy to local destination should succeed
		sourceFile.copy(localFile.getFullPath(), IResource.NONE, createTestMonitor());
		//copy from local to non local
		ensureDoesNotExistInWorkspace(destinationFile);
		//copy from local to non local
		localFile.copy(destinationFile.getFullPath(), IResource.NONE, createTestMonitor());

		//copy to self should fail
		assertThrows(CoreException.class, () -> localFile.copy(localFile.getFullPath(), IResource.NONE, createTestMonitor()));
	}

	public void testCopyFolder() throws CoreException {
		IFileStore sourceStore = createFolderStore("source");
		IProject project = getWorkspace().getRoot().getProject("project");
		IFolder parentFolder = project.getFolder("parent");
		IFolder source = parentFolder.getFolder("source");
		IFolder destination = project.getFolder("destination");

		//setup initial resources
		ensureExistsInWorkspace(project);
		parentFolder.create(IResource.NONE, true, createTestMonitor());
		source.createLink(sourceStore.toURI(), IResource.NONE, createTestMonitor());

		//shallow copy to destination should succeed
		source.copy(destination.getFullPath(), IResource.SHALLOW, createTestMonitor());
		assertTrue("1.1", destination.exists());

		//deep copy to destination should succeed
		destination.delete(IResource.NONE, createTestMonitor());
		source.copy(destination.getFullPath(), IResource.NONE, createTestMonitor());
		assertTrue("2.1", destination.exists());

		//should fail when destination is occupied
		assertThrows(CoreException.class, () -> source.copy(destination.getFullPath(), IResource.NONE, createTestMonitor()));

		//copy to self should fail
		assertThrows(CoreException.class, () -> source.copy(source.getFullPath(), IResource.NONE, createTestMonitor()));
	}

	public void testMoveFile() throws CoreException {
		IFileStore sourceStore = createFolderStore("source");
		IFileStore destinationStore = createFolderStore("destination");
		IProject project = getWorkspace().getRoot().getProject("project");
		IFolder source = project.getFolder("source");
		IFolder destination = project.getFolder("destination");
		IFile sourceFile = source.getFile("file.txt");
		IFile destinationFile = destination.getFile(sourceFile.getName());
		IFile localFile = project.getFile(sourceFile.getName());

		//setup initial resources
		ensureExistsInWorkspace(project);
		source.createLink(sourceStore.toURI(), IResource.NONE, createTestMonitor());
		destination.createLink(destinationStore.toURI(), IResource.NONE, createTestMonitor());
		sourceFile.create(getRandomContents(), IResource.NONE, createTestMonitor());

		//move to linked destination should succeed
		sourceFile.move(destinationFile.getFullPath(), IResource.NONE, createTestMonitor());
		//move back to source location
		//move to linked destination should succeed
		destinationFile.move(sourceFile.getFullPath(), IResource.NONE, createTestMonitor());

		//move to local destination should succeed
		sourceFile.move(localFile.getFullPath(), IResource.NONE, createTestMonitor());

		//movefrom local to non local
		localFile.move(destinationFile.getFullPath(), IResource.NONE, createTestMonitor());

		//copy to self should fail
		assertThrows(CoreException.class, () -> localFile.copy(localFile.getFullPath(), IResource.NONE, createTestMonitor()));
	}

	// Test for Bug 342060 - Renaming a project failing with custom EFS
	public void test342060() throws CoreException {
		IFileStore sourceStore = createBogusFolderStore("source");
		IFileStore destinationStore = createBogusFolderStore("destination");
		IProject project = getWorkspace().getRoot().getProject("project");
		IFolder source = project.getFolder("source");
		IFolder destination = project.getFolder("destination");
		IFile sourceFile = source.getFile("file.txt");
		//setup initial resources
		ensureExistsInWorkspace(project);
		source.createLink(sourceStore.toURI(), IResource.NONE, createTestMonitor());
		destination.createLink(destinationStore.toURI(), IResource.NONE, createTestMonitor());
		sourceFile.create(getRandomContents(), IResource.NONE, createTestMonitor());

		//move to linked destination should succeed
		project.move(IPath.fromPortableString("movedProject"), IResource.NONE, createTestMonitor());
	}

	protected IFileStore createBogusFolderStore(String name) throws CoreException {
		IFileSystem system = getBogusFileSystem();
		IFileStore store = system.getStore(IPath.ROOT.append(name));
		deleteOnTearDown(
					IPath.fromOSString(system.getStore(IPath.ROOT).toLocalFile(EFS.NONE, createTestMonitor()).getPath()));
		store.mkdir(EFS.NONE, createTestMonitor());
		return store;
	}

	protected IFileSystem getBogusFileSystem() throws CoreException {
		return EFS.getFileSystem(BogusFileSystem.SCHEME_BOGUS);
	}
}
