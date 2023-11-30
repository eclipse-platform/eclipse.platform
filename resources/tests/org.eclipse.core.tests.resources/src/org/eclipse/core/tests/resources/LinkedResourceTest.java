/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *     Sergey Prigogin (Google) - [338010] Resource.createLink() does not preserve symbolic links
 *******************************************************************************/
package org.eclipse.core.tests.resources;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.NATURE_SIMPLE;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertDoesNotExistInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createUniqueString;
import static org.eclipse.core.tests.resources.ResourceTestUtil.readStringInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.waitForRefresh;
import static org.junit.Assert.assertThrows;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.internal.resources.LinkDescription;
import org.eclipse.core.internal.resources.Project;
import org.eclipse.core.internal.resources.ProjectDescription;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.internal.utils.FileUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform.OS;
import org.eclipse.core.tests.harness.CancelingProgressMonitor;
import org.eclipse.core.tests.harness.FussyProgressMonitor;
import org.junit.function.ThrowingRunnable;

/**
 * Tests the following API methods:
 *  IFile#createLink
 * 	IFolder#createLink
 *
 * This test supports both variable-based and non-variable-based locations.
 * Although the method used for creating random locations
 * <code>ResourceTest#getRandomLocation()</code> never returns variable-
 * based paths, this method is overwritten in the derived class
 * <code>LinkedResourceWithPathVariable</code> to always return variable-based
 * paths.
 *
 * To support variable-based paths wherever a file system location is used, it
 * is mandatory first to resolve it and only then using it, except in calls to
 * <code>IFile#createLink</code> and <code>IFolder#createLink</code> and when
 * the location is obtained using <code>IResource#getLocation()</code>.
 */
public class LinkedResourceTest extends ResourceTest {
	protected String childName = "File.txt";
	protected IProject closedProject;
	protected IFile existingFileInExistingProject;
	protected IFolder existingFolderInExistingFolder;
	protected IFolder existingFolderInExistingProject;
	protected IProject existingProject;
	protected IProject existingProjectInSubDirectory;
	protected IPath localFile;
	protected IPath localFolder;
	protected IFile nonExistingFileInExistingFolder;
	protected IFile nonExistingFileInExistingProject;
	protected IFile nonExistingFileInOtherExistingProject;
	protected IFolder nonExistingFolderInExistingFolder;
	protected IFolder nonExistingFolderInExistingProject;
	protected IFolder nonExistingFolderInNonExistingFolder;
	protected IFolder nonExistingFolderInNonExistingProject;
	protected IFolder nonExistingFolderInOtherExistingProject;
	protected IPath nonExistingLocation;
	protected IProject nonExistingProject;
	protected IProject otherExistingProject;

	protected void doCleanup() throws Exception {
		waitForRefresh();
		createInWorkspace(new IResource[] {existingProject, otherExistingProject, closedProject, existingFolderInExistingProject, existingFolderInExistingFolder, existingFileInExistingProject});
		closedProject.close(createTestMonitor());
		removeFromWorkspace(new IResource[] { nonExistingProject, nonExistingFolderInExistingProject, nonExistingFolderInExistingFolder, nonExistingFolderInOtherExistingProject, nonExistingFolderInNonExistingProject, nonExistingFolderInNonExistingFolder, nonExistingFileInExistingProject, nonExistingFileInOtherExistingProject, nonExistingFileInExistingFolder });
		removeFromFileSystem(resolve(nonExistingLocation).toFile());
		resolve(localFolder).toFile().mkdirs();
		createFileInFileSystem(resolve(localFile));
	}

	private byte[] getFileContents(IFile file) throws CoreException, IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		try (InputStream inputStream = new BufferedInputStream(file.getContents())) {
			inputStream.transferTo(bout);
		}
		return bout.toByteArray();
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
		existingProjectInSubDirectory = getWorkspace().getRoot().getProject("ExistingProjectInSubDirectory");
		otherExistingProject = getWorkspace().getRoot().getProject("OtherExistingProject");
		closedProject = getWorkspace().getRoot().getProject("ClosedProject");
		existingFolderInExistingProject = existingProject.getFolder("existingFolderInExistingProject");
		existingFolderInExistingFolder = existingFolderInExistingProject.getFolder("existingFolderInExistingFolder");
		nonExistingFolderInExistingProject = existingProject.getFolder("nonExistingFolderInExistingProject");
		nonExistingFolderInOtherExistingProject = otherExistingProject.getFolder("nonExistingFolderInOtherExistingProject");
		nonExistingFolderInNonExistingFolder = nonExistingFolderInExistingProject.getFolder("nonExistingFolderInNonExistingFolder");
		nonExistingFolderInExistingFolder = existingFolderInExistingProject.getFolder("nonExistingFolderInExistingFolder");

		nonExistingProject = getWorkspace().getRoot().getProject("NonProject");
		nonExistingFolderInNonExistingProject = nonExistingProject.getFolder("nonExistingFolderInNonExistingProject");

		existingFileInExistingProject = existingProject.getFile("existingFileInExistingProject");
		nonExistingFileInExistingProject = existingProject.getFile("nonExistingFileInExistingProject");
		nonExistingFileInOtherExistingProject = otherExistingProject.getFile("nonExistingFileInOtherExistingProject");
		nonExistingFileInExistingFolder = existingFolderInExistingProject.getFile("nonExistingFileInExistingFolder");
		localFolder = getRandomLocation();
		nonExistingLocation = getRandomLocation();
		localFile = localFolder.append(childName);
		doCleanup();

		if (!existingProjectInSubDirectory.exists()) {
			IProjectDescription desc = getWorkspace().newProjectDescription(existingProjectInSubDirectory.getName());
			File dir = existingProject.getLocation().toFile();
			dir = dir.getParentFile();
			dir = new File(dir + File.separator + "sub");
			deleteOnTearDown(IPath.fromOSString(dir.getAbsolutePath()));
			dir = new File(dir + File.separator + "dir" + File.separator + "more" + File.separator + "proj");
			dir.mkdirs();
			desc.setLocation(IPath.fromOSString(dir.getAbsolutePath()));
			existingProjectInSubDirectory.create(desc, createTestMonitor());
		}
		if (!existingProjectInSubDirectory.isOpen()) {
			existingProjectInSubDirectory.open(createTestMonitor());
		}
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		Workspace.clear(resolve(localFolder).toFile());
		Workspace.clear(resolve(nonExistingLocation).toFile());
	}

	/**
	 * Tests creation of a linked resource whose corresponding file system
	 * path does not exist. This should succeed but no operations will be
	 * available on the resulting resource.
	 */
	public void testAllowMissingLocal() throws CoreException {
		//get a non-existing location
		IPath location = getRandomLocation();
		IFolder folder = nonExistingFolderInExistingProject;

		//try to create without the flag (should fail)
		assertThrows(CoreException.class, () -> folder.createLink(location, IResource.NONE, createTestMonitor()));

		//now try to create with the flag (should succeed)
		folder.createLink(location, IResource.ALLOW_MISSING_LOCAL, createTestMonitor());
		assertEquals("1.2", resolve(location), folder.getLocation());
		assertTrue("1.3", !resolve(location).toFile().exists());
		//getting children should succeed (and be empty)
		assertEquals("1.4", 0, folder.members().length);

		//delete should succeed
		folder.delete(IResource.NONE, createTestMonitor());

		//try to create with local path that can never exist
		IPath nonExistentLocation;
		if (OS.isWindows()) {
			nonExistentLocation = IPath.fromOSString("b:\\does\\not\\exist");
		} else {
			nonExistentLocation = IPath.fromOSString("/dev/null/does/not/exist");
		}
		IPath canonicalPathLocation = FileUtil.canonicalPath(nonExistentLocation);
		assertThrows(CoreException.class, () -> folder.createLink(canonicalPathLocation, IResource.NONE, createTestMonitor()));
		folder.createLink(canonicalPathLocation, IResource.ALLOW_MISSING_LOCAL, createTestMonitor());
		assertEquals("2.3", canonicalPathLocation, folder.getLocation());
		assertTrue("2.4", !canonicalPathLocation.toFile().exists());

		// creating child should fail
		assertThrows(CoreException.class,
				() -> folder.getFile("abc.txt").create(getRandomContents(), IResource.NONE, createTestMonitor()));
	}

	/**
	 * Tests case where a resource in the file system cannot be added to the workspace
	 * because it is blocked by a linked resource of the same name.
	 */
	public void testBlockedFolder() throws Exception {
		//create local folder that will be blocked
		createInFileSystem(nonExistingFolderInExistingProject);
		IFile blockedFile = nonExistingFolderInExistingProject.getFile("BlockedFile");
		createFileInFileSystem(blockedFile.getLocation());

		// link the folder elsewhere
		nonExistingFolderInExistingProject.createLink(localFolder, IResource.NONE, createTestMonitor());
		// refresh the project
		existingProject.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());

		//the blocked file should not exist in the workspace
		assertTrue("1.2", !blockedFile.exists());
		assertTrue("1.3", nonExistingFolderInExistingProject.exists());
		assertTrue("1.4", nonExistingFolderInExistingProject.getFile(childName).exists());
		assertEquals("1.5", nonExistingFolderInExistingProject.getLocation(), resolve(localFolder));

		//now delete the link
		nonExistingFolderInExistingProject.delete(IResource.NONE, createTestMonitor());
		//the blocked file and the linked folder should not exist in the workspace
		assertTrue("2.0", !blockedFile.exists());
		assertTrue("2.1", !nonExistingFolderInExistingProject.exists());
		assertTrue("2.2", !nonExistingFolderInExistingProject.getFile(childName).exists());
		assertEquals("2.3", nonExistingFolderInExistingProject.getLocation(), existingProject.getLocation().append(nonExistingFolderInExistingProject.getName()));

		//now refresh again to discover the blocked resource
		existingProject.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());

		//the blocked file should now exist
		assertTrue("3.0", blockedFile.exists());
		assertTrue("3.1", nonExistingFolderInExistingProject.exists());
		assertTrue("3.2", !nonExistingFolderInExistingProject.getFile(childName).exists());
		assertEquals("3.3", nonExistingFolderInExistingProject.getLocation(), existingProject.getLocation().append(nonExistingFolderInExistingProject.getName()));

		//attempting to link again will fail because the folder exists in the workspace
		assertThrows(CoreException.class,
				() -> nonExistingFolderInExistingProject.createLink(localFolder, IResource.NONE, createTestMonitor()));
	}

	/**
	 * This test creates a linked folder resource, then changes the directory in
	 * the file system to be a file.  On refresh, the linked resource should
	 * still exist, should have the correct gender, and still be a linked
	 * resource.
	 */
	public void testChangeLinkGender() throws Exception {
		IFolder folder = nonExistingFolderInExistingProject;
		IFile file = folder.getProject().getFile(folder.getProjectRelativePath());
		IPath resolvedLocation = resolve(localFolder);
		folder.createLink(localFolder, IResource.NONE, createTestMonitor());

		removeFromFileSystem(resolvedLocation.toFile());
		createFileInFileSystem(resolvedLocation);
		folder.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());

		assertTrue("3.0", !folder.exists());
		assertTrue("3.1", file.exists());
		assertTrue("3.2", file.isLinked());
		assertEquals("3.3", resolvedLocation, file.getLocation());

		//change back to folder
		removeFromFileSystem(resolvedLocation.toFile());
		resolvedLocation.toFile().mkdirs();

		folder.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		assertTrue("4.0", folder.exists());
		assertTrue("4.1", !file.exists());
		assertTrue("4.2", folder.isLinked());
		assertEquals("4.3", resolvedLocation, folder.getLocation());
	}

	public void testCopyFile() throws Exception {
		IResource[] sources = new IResource[] {nonExistingFileInExistingProject, nonExistingFileInExistingFolder};
		IResource[] destinationResources = new IResource[] {existingProject, closedProject, nonExistingFileInOtherExistingProject, nonExistingFileInExistingFolder};
		Boolean[] deepCopy = new Boolean[] {Boolean.TRUE, Boolean.FALSE};
		IProgressMonitor[] monitors = new IProgressMonitor[] {new FussyProgressMonitor(), new CancelingProgressMonitor(), null};
		Object[][] inputs = new Object[][] {sources, destinationResources, deepCopy, monitors};
		new TestPerformer("LinkedResourceTest.testCopyFile") {
			protected static final String CANCELED = "canceled";

			@Override
			public void cleanUp(Object[] args, int count) throws Exception {
				super.cleanUp(args, count);
				try {
					doCleanup();
				} catch (Exception e) {
					throw new IllegalStateException("invocation " + count + " failed to cleanup", e);
				}
			}

			@Override
			public Object invokeMethod(Object[] args, int count) throws Exception {
				IFile source = (IFile) args[0];
				IResource destination = (IResource) args[1];
				boolean isDeep = ((Boolean) args[2]).booleanValue();
				IProgressMonitor monitor = (IProgressMonitor) args[3];
				if (monitor instanceof FussyProgressMonitor fussy) {
					fussy.prepare();
				}
				try {
					source.createLink(localFile, IResource.NONE, null);
					source.copy(destination.getFullPath(), isDeep ? IResource.NONE : IResource.SHALLOW, monitor);
				} catch (OperationCanceledException e) {
					return CANCELED;
				}
				if (monitor instanceof FussyProgressMonitor fussy) {
					fussy.sanityCheck();
				}
				return null;
			}

			@Override
			public boolean shouldFail(Object[] args, int count) {
				IFile source = (IFile) args[0];
				IResource destination = (IResource) args[1];
				boolean isDeep = ((Boolean) args[2]).booleanValue();
				IProgressMonitor monitor = (IProgressMonitor) args[3];
				if (monitor instanceof CancelingProgressMonitor) {
					return false;
				}
				if (source.equals(destination)) {
					return true;
				}
				IResource parent = destination.getParent();
				if (!isDeep && parent == null) {
					return true;
				}
				if (!parent.isAccessible()) {
					return true;
				}
				if (destination.exists()) {
					return true;
				}
				//passed all failure cases so it should succeed
				return false;
			}

			@Override
			public boolean wasSuccess(Object[] args, Object result, Object[] oldState) throws Exception {
				IFile source = (IFile) args[0];
				IResource destination = (IResource) args[1];
				boolean isDeep = ((Boolean) args[2]).booleanValue();
				IProgressMonitor monitor = (IProgressMonitor) args[3];
				if (result == CANCELED) {
					return monitor instanceof CancelingProgressMonitor;
				}
				if (!destination.exists()) {
					return false;
				}
				//destination should only be linked for a shallow copy
				if (isDeep) {
					if (destination.isLinked()) {
						return false;
					}
					if (source.getLocation().equals(destination.getLocation())) {
						return false;
					}
					if (!destination.getProject().getLocation().isPrefixOf(destination.getLocation())) {
						return false;
					}
				} else {
					if (!destination.isLinked()) {
						return false;
					}
					if (!source.getLocation().equals(destination.getLocation())) {
						return false;
					}
					if (!source.getRawLocation().equals(destination.getRawLocation())) {
						return false;
					}
					if (!source.getLocationURI().equals(destination.getLocationURI())) {
						return false;
					}
				}
				return true;
			}
		}.performTest(inputs);
	}

	public void testCopyFolder() throws Exception {
		IFolder[] sources = new IFolder[] {nonExistingFolderInExistingProject, nonExistingFolderInExistingFolder};
		IResource[] destinations = new IResource[] {existingProject, closedProject, nonExistingProject, existingFolderInExistingProject, nonExistingFolderInOtherExistingProject, nonExistingFolderInExistingFolder};
		Boolean[] deepCopy = new Boolean[] {Boolean.TRUE, Boolean.FALSE};
		IProgressMonitor[] monitors = new IProgressMonitor[] {new FussyProgressMonitor(), new CancelingProgressMonitor(), null};
		Object[][] inputs = new Object[][] {sources, destinations, deepCopy, monitors};
		new TestPerformer("LinkedResourceTest.testCopyFolder") {
			protected static final String CANCELED = "canceled";

			@Override
			public void cleanUp(Object[] args, int count) throws Exception {
				super.cleanUp(args, count);
				try {
					doCleanup();
				} catch (Exception e) {
					throw new IllegalStateException("invocation " + count + " failed to cleanup", e);
				}
			}

			@Override
			public Object invokeMethod(Object[] args, int count) throws Exception {
				IFolder source = (IFolder) args[0];
				IResource destination = (IResource) args[1];
				boolean isDeep = ((Boolean) args[2]).booleanValue();
				IProgressMonitor monitor = (IProgressMonitor) args[3];
				if (monitor instanceof FussyProgressMonitor fussy) {
					fussy.prepare();
				}
				try {
					source.createLink(localFolder, IResource.NONE, null);
					source.copy(destination.getFullPath(), isDeep ? IResource.NONE : IResource.SHALLOW, monitor);
				} catch (OperationCanceledException e) {
					return CANCELED;
				}
				if (monitor instanceof FussyProgressMonitor fussy) {
					fussy.sanityCheck();
				}
				return null;
			}

			@Override
			public boolean shouldFail(Object[] args, int count) {
				IFolder source = (IFolder) args[0];
				IResource destination = (IResource) args[1];
				boolean isDeep = ((Boolean) args[2]).booleanValue();
				IProgressMonitor monitor = (IProgressMonitor) args[3];
				if (monitor instanceof CancelingProgressMonitor) {
					return false;
				}
				IResource parent = destination.getParent();
				if (destination.getType() == IResource.PROJECT) {
					return true;
				}
				if (source.equals(destination)) {
					return true;
				}
				if (!isDeep && parent == null) {
					return true;
				}
				if (!parent.isAccessible()) {
					return true;
				}
				if (destination.exists()) {
					return true;
				}
				//passed all failure case so it should succeed
				return false;
			}

			@Override
			public boolean wasSuccess(Object[] args, Object result, Object[] oldState) throws Exception {
				IFolder source = (IFolder) args[0];
				IResource destination = (IResource) args[1];
				boolean isDeep = ((Boolean) args[2]).booleanValue();
				IProgressMonitor monitor = (IProgressMonitor) args[3];
				if (result == CANCELED) {
					return monitor instanceof CancelingProgressMonitor;
				}
				if (!destination.exists()) {
					return false;
				}
				//destination should only be linked for a shallow copy
				if (isDeep) {
					if (destination.isLinked()) {
						return false;
					}
					if (source.getLocation().equals(destination.getLocation())) {
						return false;
					}
					if (!destination.getProject().getLocation().isPrefixOf(destination.getLocation())) {
						return false;
					}
				} else {
					if (!destination.isLinked()) {
						return false;
					}
					if (!source.getLocation().equals(destination.getLocation())) {
						return false;
					}
					if (!source.getLocationURI().equals(destination.getLocationURI())) {
						return false;
					}
					if (!source.getRawLocation().equals(destination.getRawLocation())) {
						return false;
					}
				}
				return true;
			}
		}.performTest(inputs);
	}

	/**
	 * Tests copying a linked file resource that doesn't exist in the file system
	 */
	public void testCopyMissingFile() throws CoreException {
		IPath location = getRandomLocation();
		IFile linkedFile = nonExistingFileInExistingProject;
		linkedFile.createLink(location, IResource.ALLOW_MISSING_LOCAL, createTestMonitor());

		IFile dest = existingProject.getFile("FailedCopyDest");
		assertThrows(CoreException.class, () -> linkedFile.copy(dest.getFullPath(), IResource.NONE, createTestMonitor()));
		assertTrue("2.1", !dest.exists());
		assertThrows(CoreException.class, () -> linkedFile.copy(dest.getFullPath(), IResource.FORCE, createTestMonitor()));
		assertTrue("2.3", !dest.exists());
	}

	/**
	 * Tests copying a linked folder that doesn't exist in the file system
	 */
	public void testCopyMissingFolder() throws CoreException {
		IPath location = getRandomLocation();
		IFolder linkedFolder = nonExistingFolderInExistingProject;
		linkedFolder.createLink(location, IResource.ALLOW_MISSING_LOCAL, createTestMonitor());

		IFolder dest = existingProject.getFolder("FailedCopyDest");
		assertThrows(CoreException.class, () -> linkedFolder.copy(dest.getFullPath(), IResource.NONE, createTestMonitor()));
		assertTrue("2.1", !dest.exists());
		assertThrows(CoreException.class, () -> linkedFolder.copy(dest.getFullPath(), IResource.FORCE, createTestMonitor()));
		assertTrue("2.3", !dest.exists());
	}

	public void testCopyProjectWithLinks() throws Exception {
		IPath fileLocation = getRandomLocation();
		deleteOnTearDown(fileLocation);
		IFile linkedFile = nonExistingFileInExistingProject;
		IFolder linkedFolder = nonExistingFolderInExistingProject;
		createFileInFileSystem(resolve(fileLocation));
		linkedFolder.createLink(localFolder, IResource.NONE, createTestMonitor());
		linkedFile.createLink(fileLocation, IResource.NONE, createTestMonitor());

		// copy the project
		IProject destination = getWorkspace().getRoot().getProject("CopyTargetProject");
		existingProject.copy(destination.getFullPath(), IResource.SHALLOW, createTestMonitor());

		IFile newFile = destination.getFile(linkedFile.getProjectRelativePath());
		assertTrue("3.0", newFile.isLinked());
		assertEquals("3.1", linkedFile.getLocation(), newFile.getLocation());

		IFolder newFolder = destination.getFolder(linkedFolder.getProjectRelativePath());
		assertTrue("4.0", newFolder.isLinked());
		assertEquals("4.1", linkedFolder.getLocation(), newFolder.getLocation());

		// test project deep copy
		destination.delete(IResource.NONE, createTestMonitor());
		existingProject.copy(destination.getFullPath(), IResource.NONE, createTestMonitor());
		assertTrue("5.1", !newFile.isLinked());
		assertEquals("5.2", destination.getLocation().append(newFile.getProjectRelativePath()), newFile.getLocation());
		assertTrue("5.3", !newFolder.isLinked());
		assertEquals("5.4", destination.getLocation().append(newFolder.getProjectRelativePath()),
				newFolder.getLocation());

		// test copy project when linked resources don't exist with force=false
		destination.delete(IResource.NONE, createTestMonitor());
		assertTrue("6.0", resolve(fileLocation).toFile().delete());

		assertThrows(CoreException.class,
				() -> existingProject.copy(destination.getFullPath(), IResource.NONE, createTestMonitor()));
		// all members except the missing link should have been copied
		assertTrue("6.2", destination.exists());
		assertTrue("6.2.1", !destination.getFile(linkedFile.getName()).exists());
		IResource[] srcChildren = existingProject.members();
		for (int i = 0; i < srcChildren.length; i++) {
			if (!srcChildren[i].equals(linkedFile)) {
				assertNotNull("6.3." + i, destination.findMember(srcChildren[i].getProjectRelativePath()));
			}
		}
		// test copy project when linked resources don't exist with force=true
		// this should mostly succeed, but still throw an exception indicating
		// a resource could not be copied because its location was missing
		destination.delete(IResource.NONE, createTestMonitor());
		assertThrows(CoreException.class,
				() -> existingProject.copy(destination.getFullPath(), IResource.FORCE, createTestMonitor()));
		assertTrue("6.7", destination.exists());
		assertTrue("6.7.1", !destination.getFile(linkedFile.getName()).exists());
		// all members except the missing link should have been copied
		srcChildren = existingProject.members();
		for (int i = 0; i < srcChildren.length; i++) {
			if (!srcChildren[i].equals(linkedFile)) {
				assertNotNull("6.8." + i, destination.findMember(srcChildren[i].getProjectRelativePath()));
			}
		}
	}

	/**
	 * Tests creating a linked folder and performing refresh in the background
	 */
	public void testCreateFolderInBackground() throws Exception {
		final IFileStore rootStore = getTempStore();
		rootStore.mkdir(IResource.NONE, createTestMonitor());
		IFileStore childStore = rootStore.getChild("file.txt");
		createFileInFileSystem(childStore);

		IFolder link = nonExistingFolderInExistingProject;
		link.createLink(rootStore.toURI(), IResource.BACKGROUND_REFRESH, createTestMonitor());
		waitForRefresh();
		IFile linkChild = link.getFile(childStore.getName());
		assertTrue("1.0", link.exists());
		assertTrue("1.1", link.isSynchronized(IResource.DEPTH_INFINITE));
		assertTrue("1.2", linkChild.exists());
		assertTrue("1.3", linkChild.isSynchronized(IResource.DEPTH_INFINITE));
	}

	/**
	 * Tests creating a linked resource with the same name but different
	 * case as an existing resource.  On case insensitive platforms this should fail.
	 */
	public void testCreateLinkCaseVariant() throws Throwable {
		IFolder link = nonExistingFolderInExistingProject;
		IFolder variant = link.getParent().getFolder(IPath.fromOSString(link.getName().toUpperCase()));
		createInWorkspace(variant);

		ThrowingRunnable linkCreation = () -> link.createLink(localFolder, IResource.NONE, createTestMonitor());
		// should fail on case insensitive platforms
		if (Workspace.caseSensitive) {
			linkCreation.run();
		} else {
			assertThrows(CoreException.class, linkCreation);
		}
	}

	/**
	 * Tests creating a linked resource by modifying the .project file directly.
	 * This is a regression test for bug 63331.
	 */
	public void testCreateLinkInDotProject() throws Exception {
		final IFile dotProject = existingProject.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		IFile link = nonExistingFileInExistingProject;
		byte[] oldContents = null;
		// create a linked file
		link.createLink(localFile, IResource.NONE, createTestMonitor());
		// copy the .project file contents
		oldContents = getFileContents(dotProject);
		// delete linked file
		link.delete(IResource.NONE, createTestMonitor());

		final byte[] finalContents = oldContents;
		// recreate the link in a workspace runnable with create scheduling rule
		getWorkspace().run(
				(IWorkspaceRunnable) monitor -> dotProject.setContents(new ByteArrayInputStream(finalContents),
						IResource.NONE, createTestMonitor()),
				getWorkspace().getRuleFactory().modifyRule(dotProject), IResource.NONE, createTestMonitor());
	}

	/**
	 * Tests creating a project whose .project file already defines links at
	 * depth greater than one. See bug 121322.
	 */
	public void testCreateProjectWithDeepLinks() throws CoreException {
		IProject project = existingProject;
		IFolder parent = existingFolderInExistingProject;
		IFolder folder = nonExistingFolderInExistingFolder;
		folder.createLink(localFolder, IResource.NONE, createTestMonitor());
		// delete and recreate the project
		project.delete(IResource.NEVER_DELETE_PROJECT_CONTENT, createTestMonitor());
		project.create(createTestMonitor());
		project.open(IResource.BACKGROUND_REFRESH, createTestMonitor());
		assertTrue("1.0", folder.exists());
		assertTrue("1.1", parent.exists());
		assertTrue("1.2", parent.isLocal(IResource.DEPTH_INFINITE));
	}

	/**
	 * Tests whether {@link IFile#createLink} and {@link IFolder#createLink}
	 * handle {@link IResource#HIDDEN} flag properly.
	 */
	public void testCreateHiddenLinkedResources() throws CoreException {
		IFolder folder = existingProject.getFolder("folder");
		IFile file = existingProject.getFile("file.txt");

		folder.createLink(localFolder, IResource.HIDDEN, createTestMonitor());
		file.createLink(localFile, IResource.HIDDEN, createTestMonitor());

		assertTrue("3.0", folder.isHidden());
		assertTrue("4.0", file.isHidden());
	}

	public void testDeepMoveProjectWithLinks() throws Exception {
		IPath fileLocation = getRandomLocation();
		deleteOnTearDown(fileLocation);
		IFile file = nonExistingFileInExistingProject;
		IFolder folder = nonExistingFolderInExistingProject;
		IFile childFile = folder.getFile(childName);
		IResource[] oldResources = new IResource[] {file, folder, existingProject, childFile};
		createFileInFileSystem(resolve(fileLocation));
		folder.createLink(localFolder, IResource.NONE, createTestMonitor());
		file.createLink(fileLocation, IResource.NONE, createTestMonitor());

		// move the project
		IProject destination = getWorkspace().getRoot().getProject("MoveTargetProject");
		IFile newFile = destination.getFile(file.getProjectRelativePath());
		IFolder newFolder = destination.getFolder(folder.getProjectRelativePath());
		IFile newChildFile = newFolder.getFile(childName);
		IResource[] newResources = new IResource[] { destination, newFile, newFolder, newChildFile };

		assertDoesNotExistInWorkspace(destination);

		existingProject.move(destination.getFullPath(), IResource.NONE, createTestMonitor());
		assertExistsInWorkspace(newResources);
		assertDoesNotExistInWorkspace(oldResources);
		assertTrue("3.2", existingProject.isSynchronized(IResource.DEPTH_INFINITE));
		assertTrue("3.3", destination.isSynchronized(IResource.DEPTH_INFINITE));

		assertTrue("3.4", !newFile.isLinked());
		assertEquals("3.5", destination.getLocation().append(newFile.getProjectRelativePath()), newFile.getLocation());

		assertTrue("3.6", !newFolder.isLinked());
		assertEquals("3.7", destination.getLocation().append(newFolder.getProjectRelativePath()),
				newFolder.getLocation());

		assertTrue("3.8", destination.isSynchronized(IResource.DEPTH_INFINITE));
	}

	/**
	 * Tests deleting the parent of a linked resource.
	 */
	public void testDeleteLinkParent() throws CoreException {
		IFolder link = nonExistingFolderInExistingFolder;
		IFolder linkParent = existingFolderInExistingProject;
		IFile linkChild = link.getFile("child.txt");
		IFileStore childStore = null;
		link.createLink(localFolder, IResource.NONE, createTestMonitor());
		createInWorkspace(linkChild);
		childStore = EFS.getStore(linkChild.getLocationURI());

		//everything should exist at this point
		assertTrue("1.0", linkParent.exists());
		assertTrue("1.1", link.exists());
		assertTrue("1.2", linkChild.exists());

		//delete the parent of the link
		linkParent.delete(IResource.KEEP_HISTORY, createTestMonitor());

		//resources should not exist, but link content should exist on disk
		assertTrue("2.0", !linkParent.exists());
		assertTrue("2.1", !link.exists());
		assertTrue("2.2", !linkChild.exists());
		assertTrue("2.3", childStore.fetchInfo().exists());
	}

	/**
	 * Tests deleting and then recreating a project
	 */
	public void testDeleteProjectWithLinks() throws CoreException {
		IFolder link = nonExistingFolderInExistingProject;
		link.createLink(localFolder, IResource.NONE, createTestMonitor());
		existingProject.delete(IResource.NEVER_DELETE_PROJECT_CONTENT, createTestMonitor());
		existingProject.create(createTestMonitor());

		//link should not exist until the project is open
		assertTrue("1.0", !link.exists());

		existingProject.open(createTestMonitor());

		//link should now exist
		assertTrue("2.0", link.exists());
		assertTrue("2.1", link.isLinked());
		assertEquals("2.2", resolve(localFolder), link.getLocation());
	}

	/**
	 * Tests deleting a linked resource when .project is read-only
	 */
	public void testDeleteLink_Bug351823() throws CoreException {
		IProject project = existingProject;

		IFile link = project.getFile(createUniqueString());
		link.createLink(localFile, IResource.NONE, createTestMonitor());

		// set .project read-only
		IFile descriptionFile = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		ResourceAttributes attrs = descriptionFile.getResourceAttributes();
		attrs.setReadOnly(true);
		descriptionFile.setResourceAttributes(attrs);

		try {
			assertThrows(CoreException.class, () -> link.delete(false, createTestMonitor()));
			assertTrue("2.0", link.exists());
			assertTrue("3.0", link.isLinked());

			HashMap<IPath, LinkDescription> links = ((Project) project).internalGetDescription().getLinks();
			assertNotNull("4.0", links);
			assertEquals("5.0", 1, links.size());

			LinkDescription linkDesc = links.values().iterator().next();
			assertEquals("6.0", link.getProjectRelativePath(), linkDesc.getProjectRelativePath());

			// try to delete again
			// if the project description in memory is ok, it should fail
			assertThrows(CoreException.class, () -> link.delete(false, createTestMonitor()));
		} finally {
			// set .project writable
			attrs = descriptionFile.getResourceAttributes();
			attrs.setReadOnly(false);
			descriptionFile.setResourceAttributes(attrs);
		}
	}

	/**
	 * Tests bug 209175.
	 */
	public void testDeleteFolderWithLinks() throws CoreException {
		IProject project = existingProject;
		IFolder folder = existingFolderInExistingProject;
		IFile file1 = folder.getFile(createUniqueString());
		IFile file2 = project.getFile(createUniqueString());
		file1.createLink(localFile, IResource.NONE, createTestMonitor());
		file2.createLink(localFile, IResource.NONE, createTestMonitor());

		HashMap<IPath, LinkDescription> links = ((Project) project).internalGetDescription().getLinks();
		LinkDescription linkDescription1 = links.get(file1.getProjectRelativePath());
		assertNotNull("1.0", linkDescription1);
		assertEquals("1.1", URIUtil.toURI(localFile), linkDescription1.getLocationURI());
		LinkDescription linkDescription2 = links.get(file2.getProjectRelativePath());
		assertNotNull("2.0", linkDescription2);
		assertEquals("2.1", URIUtil.toURI(localFile), linkDescription2.getLocationURI());

		folder.delete(true, createTestMonitor());

		links = ((Project) project).internalGetDescription().getLinks();
		linkDescription1 = links.get(file1.getProjectRelativePath());
		assertNull("3.0", linkDescription1);
		linkDescription2 = links.get(file2.getProjectRelativePath());
		assertNotNull("4.0", linkDescription2);
		assertEquals("4.1", URIUtil.toURI(localFile), linkDescription2.getLocationURI());
	}

	/**
	 * Tests that IWorkspaceRoot.findFilesForLocation works correctly
	 * in presence of a linked resource that does not match the case in the file system
	 */
	public void testFindFilesForLocationCaseVariant() throws CoreException {
		//this test only applies to file systems with a device in the path
		if (!OS.isWindows()) {
			return;
		}
		IFolder link = nonExistingFolderInExistingProject;
		IPath localLocation = resolve(localFolder);
		IPath upperCase = localLocation.setDevice(localLocation.getDevice().toUpperCase());
		IPath lowerCase = localLocation.setDevice(localLocation.getDevice().toLowerCase());

		link.createLink(upperCase, IResource.NONE, createTestMonitor());
		IPath lowerCaseFilePath = lowerCase.append("file.txt");
		IFile[] files = getWorkspace().getRoot().findFilesForLocation(lowerCaseFilePath);
		assertEquals("1.0", 1, files.length);
	}

	/**
	 * Tests the {@link org.eclipse.core.resources.IResource#isLinked(int)} method.
	 */
	public void testIsLinked() throws CoreException {
		//initially nothing is linked
		IResource[] toTest = new IResource[] {closedProject, existingFileInExistingProject, existingFolderInExistingFolder, existingFolderInExistingProject, existingProject, nonExistingFileInExistingFolder, nonExistingFileInExistingProject, nonExistingFileInOtherExistingProject, nonExistingFolderInExistingFolder, nonExistingFolderInExistingProject, nonExistingFolderInNonExistingFolder, nonExistingFolderInNonExistingProject, nonExistingFolderInOtherExistingProject, nonExistingProject, otherExistingProject};
		for (IResource t : toTest) {
			assertTrue("1.0 " + t, !t.isLinked());
			assertTrue("1.1 " + t, !t.isLinked(IResource.NONE));
			assertTrue("1.2 " + t, !t.isLinked(IResource.CHECK_ANCESTORS));
		}
		// create a link
		IFolder link = nonExistingFolderInExistingProject;
		link.createLink(localFolder, IResource.NONE, createTestMonitor());
		IFile child = link.getFile(childName);
		assertTrue("2.0", child.exists());
		assertTrue("2.1", link.isLinked());
		assertTrue("2.2", link.isLinked(IResource.NONE));
		assertTrue("2.3", link.isLinked(IResource.CHECK_ANCESTORS));
		assertTrue("2.1", !child.isLinked());
		assertTrue("2.2", !child.isLinked(IResource.NONE));
		assertTrue("2.3", child.isLinked(IResource.CHECK_ANCESTORS));

	}

	public void testSetLinkLocation() throws CoreException {
		// initially nothing is linked
		IResource[] toTest = new IResource[] {closedProject, existingFileInExistingProject, existingFolderInExistingFolder, existingFolderInExistingProject, existingProject, nonExistingFileInExistingFolder, nonExistingFileInExistingProject, nonExistingFileInOtherExistingProject, nonExistingFolderInExistingFolder, nonExistingFolderInExistingProject, nonExistingFolderInNonExistingFolder, nonExistingFolderInNonExistingProject, nonExistingFolderInOtherExistingProject, nonExistingProject, otherExistingProject};
		for (IResource toTest1 : toTest) {
			assertTrue("1.0 " + toTest1, !toTest1.isLinked());
			assertTrue("1.1 " + toTest1, !toTest1.isLinked(IResource.NONE));
			assertTrue("1.2 " + toTest1, !toTest1.isLinked(IResource.CHECK_ANCESTORS));
		}
		// create a link
		IFolder link = nonExistingFolderInExistingProject;
		link.createLink(localFolder, IResource.NONE, createTestMonitor());
		IFile child = link.getFile(childName);
		assertTrue("2.0", child.exists());
		assertTrue("2.1", link.isLinked());
		assertTrue("2.2", link.isLinked(IResource.NONE));
		assertTrue("2.3", link.isLinked(IResource.CHECK_ANCESTORS));
		assertTrue("2.1", !child.isLinked());
		assertTrue("2.2", !child.isLinked(IResource.NONE));
		assertTrue("2.3", child.isLinked(IResource.CHECK_ANCESTORS));

		link.createLink(existingFileInExistingProject.getLocationURI(), IResource.REPLACE, createTestMonitor());
		assertTrue("3.1", link.isLinked());
		assertTrue("3.2", link.isLinked(IResource.NONE));
		assertTrue("3.3", link.isLinked(IResource.CHECK_ANCESTORS));
		assertTrue("3.4", link.getLocation().equals(existingFileInExistingProject.getLocation()));
	}

	/**
	 * Tests swapping the link location.
	 * This is a regression test for bug 268507
	 */
	public void testSetLinkLocationSwapLinkedResource() throws CoreException {
		final IPath parentLoc = existingFolderInExistingProject.getLocation();
		final IPath childLoc = existingFolderInExistingFolder.getLocation();

		nonExistingFolderInExistingProject.createLink(parentLoc, IResource.NONE, createTestMonitor());
		nonExistingFolderInOtherExistingProject.createLink(childLoc, IResource.NONE, createTestMonitor());
		createInWorkspace(nonExistingFolderInOtherExistingProject.getFile("foo"));

		assertTrue("2.0", existingFolderInExistingFolder.members().length == 1);
		assertTrue("3.0", existingFolderInExistingFolder.members()[0].getName().equals("foo"));
		assertTrue("2.0", nonExistingFolderInOtherExistingProject.members().length == 1);
		assertTrue("3.0", nonExistingFolderInOtherExistingProject.members()[0].getName().equals("foo"));

		assertTrue("4.0", nonExistingFolderInExistingProject.members().length == 1);
		assertTrue("5.0", nonExistingFolderInExistingProject.members()[0].getName()
				.equals(existingFolderInExistingFolder.getName()));

		// Swap links around
		nonExistingFolderInExistingProject.createLink(childLoc, IResource.REPLACE, createTestMonitor());
		nonExistingFolderInOtherExistingProject.createLink(parentLoc, IResource.REPLACE, createTestMonitor());

		assertTrue("2.0", existingFolderInExistingFolder.members().length == 1);
		assertTrue("3.0", existingFolderInExistingFolder.members()[0].getName().equals("foo"));
		assertTrue(nonExistingFolderInExistingProject.getLocation().equals(childLoc));
		assertTrue("2.0", nonExistingFolderInExistingProject.members().length == 1);
		assertTrue("3.0", nonExistingFolderInExistingProject.members()[0].getName().equals("foo"));

		assertTrue("4.0", nonExistingFolderInOtherExistingProject.members().length == 1);
		assertTrue("5.0", nonExistingFolderInOtherExistingProject.members()[0].getName()
				.equals(existingFolderInExistingFolder.getName()));
	}

	public void testSetLinkLocationPath() throws CoreException {
		//initially nothing is linked
		IResource[] toTest = new IResource[] {closedProject, existingFileInExistingProject, existingFolderInExistingFolder, existingFolderInExistingProject, existingProject, nonExistingFileInExistingFolder, nonExistingFileInExistingProject, nonExistingFileInOtherExistingProject, nonExistingFolderInExistingFolder, nonExistingFolderInExistingProject, nonExistingFolderInNonExistingFolder, nonExistingFolderInNonExistingProject, nonExistingFolderInOtherExistingProject, nonExistingProject, otherExistingProject};
		for (IResource toTest1 : toTest) {
			assertTrue("1.0 " + toTest1, !toTest1.isLinked());
			assertTrue("1.1 " + toTest1, !toTest1.isLinked(IResource.NONE));
			assertTrue("1.2 " + toTest1, !toTest1.isLinked(IResource.CHECK_ANCESTORS));
		}
		//create a link
		IFolder link = nonExistingFolderInExistingProject;
		link.createLink(localFolder, IResource.NONE, createTestMonitor());
		IFile child = link.getFile(childName);
		assertTrue("2.0", child.exists());
		assertTrue("2.1", link.isLinked());
		assertTrue("2.2", link.isLinked(IResource.NONE));
		assertTrue("2.3", link.isLinked(IResource.CHECK_ANCESTORS));
		assertTrue("2.1", !child.isLinked());
		assertTrue("2.2", !child.isLinked(IResource.NONE));
		assertTrue("2.3", child.isLinked(IResource.CHECK_ANCESTORS));

		link.createLink(existingFileInExistingProject.getLocation(), IResource.REPLACE, createTestMonitor());
		assertTrue("3.1", link.isLinked());
		assertTrue("3.2", link.isLinked(IResource.NONE));
		assertTrue("3.3", link.isLinked(IResource.CHECK_ANCESTORS));
		assertTrue("3.4", link.getLocation().equals(existingFileInExistingProject.getLocation()));
	}

	/**
	 * Specific testing of links within links.
	 */
	public void testLinkedFileInLinkedFolder() throws CoreException, IOException {
		//setup handles
		IProject project = existingProject;
		IFolder top = project.getFolder("topFolder");
		IFolder linkedFolder = top.getFolder("linkedFolder");
		IFolder subFolder = linkedFolder.getFolder("subFolder");
		IFile linkedFile = subFolder.getFile("Link.txt");
		IFileStore folderStore = getTempStore();
		IFileStore subFolderStore = folderStore.getChild(subFolder.getName());
		IFileStore fileStore = getTempStore();
		IPath folderLocation = URIUtil.toPath(folderStore.toURI());
		IPath fileLocation = URIUtil.toPath(fileStore.toURI());

		// create the structure on disk
		subFolderStore.mkdir(EFS.NONE, createTestMonitor());
		fileStore.openOutputStream(EFS.NONE, createTestMonitor()).close();

		// create the structure in the workspace
		createInWorkspace(top);
		linkedFolder.createLink(folderStore.toURI(), IResource.NONE, createTestMonitor());
		linkedFile.createLink(fileStore.toURI(), IResource.NONE, createTestMonitor());

		//assert locations
		assertEquals("1.0", folderLocation, linkedFolder.getLocation());
		assertEquals("1.1", folderLocation.append(subFolder.getName()), subFolder.getLocation());
		assertEquals("1.2", fileLocation, linkedFile.getLocation());
		//assert URIs
		assertEquals("1.0", folderStore.toURI(), linkedFolder.getLocationURI());
		assertEquals("1.1", subFolderStore.toURI(), subFolder.getLocationURI());
		assertEquals("1.2", fileStore.toURI(), linkedFile.getLocationURI());
	}

	/**
	 * Automated test of IFile#createLink
	 */
	public void testLinkFile() throws Exception {
		IResource[] interestingResources = new IResource[] {existingFileInExistingProject, nonExistingFileInExistingProject, nonExistingFileInExistingFolder};
		IPath[] interestingLocations = new IPath[] {localFile, localFolder, nonExistingLocation};
		IProgressMonitor[] monitors = new IProgressMonitor[] {new FussyProgressMonitor(), new CancelingProgressMonitor(), null};
		Object[][] inputs = new Object[][] {interestingResources, interestingLocations, monitors};
		new TestPerformer("LinkedResourceTest.testLinkFile") {
			protected static final String CANCELED = "canceled";

			@Override
			public void cleanUp(Object[] args, int count) throws Exception {
				super.cleanUp(args, count);
				try {
					doCleanup();
				} catch (Exception e) {
					throw new IllegalStateException("invocation " + count + " failed to cleanup", e);
				}
			}

			@Override
			public Object invokeMethod(Object[] args, int count) throws Exception {
				IFile file = (IFile) args[0];
				IPath location = (IPath) args[1];
				IProgressMonitor monitor = (IProgressMonitor) args[2];
				if (monitor instanceof FussyProgressMonitor fussy) {
					fussy.prepare();
				}
				try {
					file.createLink(location, IResource.NONE, monitor);
				} catch (OperationCanceledException e) {
					return CANCELED;
				}
				if (monitor instanceof FussyProgressMonitor fussy) {
					fussy.sanityCheck();
				}
				return null;
			}

			@Override
			public boolean shouldFail(Object[] args, int count) {
				IResource resource = (IResource) args[0];
				IPath location = (IPath) args[1];
				IProgressMonitor monitor = (IProgressMonitor) args[2];
				if (monitor instanceof CancelingProgressMonitor) {
					return false;
				}
				//This resource already exists in the workspace
				if (resource.exists()) {
					return true;
				}
				IPath resolvedLocation = resolve(location);
				//The corresponding location in the local file system does not exist.
				if (!resolvedLocation.toFile().exists()) {
					return true;
				}
				//The workspace contains a resource of a different type at the same path as this resource
				if (getWorkspace().getRoot().findMember(resource.getFullPath()) != null) {
					return true;
				}
				//The parent of this resource does not exist.
				if (!resource.getParent().isAccessible()) {
					return true;
				}
				//The name of this resource is not valid (according to IWorkspace.validateName)
				if (!getWorkspace().validateName(resource.getName(), IResource.FOLDER).isOK()) {
					return true;
				}
				//The corresponding location in the local file system is occupied by a directory (as opposed to a file)
				if (resolvedLocation.toFile().isDirectory()) {
					return true;
				}
				//passed all failure case so it should succeed
				return false;
			}

			@Override
			public boolean wasSuccess(Object[] args, Object result, Object[] oldState) throws Exception {
				IFile resource = (IFile) args[0];
				IPath location = (IPath) args[1];
				IProgressMonitor monitor = (IProgressMonitor) args[2];
				if (result == CANCELED) {
					return monitor instanceof CancelingProgressMonitor;
				}
				IPath resolvedLocation = resolve(location);
				if (!resource.exists() || !resolvedLocation.toFile().exists()) {
					return false;
				}
				if (!resource.getLocation().equals(resolvedLocation)) {
					return false;
				}
				if (!resource.isSynchronized(IResource.DEPTH_INFINITE)) {
					return false;
				}
				return true;
			}
		}.performTest(inputs);
	}

	/**
	 * Automated test of IFolder#createLink
	 */
	public void testLinkFolder() throws Exception {
		IResource[] interestingResources = new IResource[] {existingFolderInExistingProject, existingFolderInExistingFolder, nonExistingFolderInExistingProject, nonExistingFolderInNonExistingProject, nonExistingFolderInNonExistingFolder, nonExistingFolderInExistingFolder};
		IPath[] interestingLocations = new IPath[] {localFile, localFolder, nonExistingLocation};
		IProgressMonitor[] monitors = new IProgressMonitor[] {new FussyProgressMonitor(), new CancelingProgressMonitor(), null};
		Object[][] inputs = new Object[][] {interestingResources, interestingLocations, monitors};
		new TestPerformer("LinkedResourceTest.testLinkFolder") {
			protected static final String CANCELED = "canceled";

			@Override
			public void cleanUp(Object[] args, int count) throws Exception {
				super.cleanUp(args, count);
				try {
					doCleanup();
				} catch (Exception e) {
					throw new IllegalStateException("invocation " + count + " failed to cleanup", e);
				}
			}

			@Override
			public Object invokeMethod(Object[] args, int count) throws Exception {
				IFolder folder = (IFolder) args[0];
				IPath location = (IPath) args[1];
				IProgressMonitor monitor = (IProgressMonitor) args[2];
				if (monitor instanceof FussyProgressMonitor fussy) {
					fussy.prepare();
				}
				try {
					folder.createLink(location, IResource.NONE, monitor);
				} catch (OperationCanceledException e) {
					return CANCELED;
				}
				if (monitor instanceof FussyProgressMonitor fussy) {
					fussy.sanityCheck();
				}
				return null;
			}

			@Override
			public boolean shouldFail(Object[] args, int count) {
				IResource resource = (IResource) args[0];
				IPath location = (IPath) args[1];
				IProgressMonitor monitor = (IProgressMonitor) args[2];
				if (monitor instanceof CancelingProgressMonitor) {
					return false;
				}
				//This resource already exists in the workspace
				if (resource.exists()) {
					return true;
				}
				//The corresponding location in the local file system does not exist.
				if (!resolve(location).toFile().exists()) {
					return true;
				}
				//The workspace contains a resource of a different type at the same path as this resource
				if (getWorkspace().getRoot().findMember(resource.getFullPath()) != null) {
					return true;
				}
				//The parent of this resource does not exist.
				if (!resource.getParent().isAccessible()) {
					return true;
				}
				//The name of this resource is not valid (according to IWorkspace.validateName)
				if (!getWorkspace().validateName(resource.getName(), IResource.FOLDER).isOK()) {
					return true;
				}
				//The corresponding location in the local file system is occupied by a file (as opposed to a directory)
				if (resolve(location).toFile().isFile()) {
					return true;
				}
				//passed all failure case so it should succeed
				return false;
			}

			@Override
			public boolean wasSuccess(Object[] args, Object result, Object[] oldState) throws Exception {
				IFolder resource = (IFolder) args[0];
				IPath location = (IPath) args[1];
				IProgressMonitor monitor = (IProgressMonitor) args[2];
				if (result == CANCELED) {
					return monitor instanceof CancelingProgressMonitor;
				}
				IPath resolvedLocation = resolve(location);
				if (!resource.exists() || !resolvedLocation.toFile().exists()) {
					return false;
				}
				if (!resource.getLocation().equals(resolvedLocation)) {
					return false;
				}
				//ensure child exists
				if (!resource.getFile(childName).exists()) {
					return false;
				}
				return true;
			}
		}.performTest(inputs);

	}

	/**
	 * Automated test of IWorkspace#validateLinkLocation and IWorkspace#validateLinkLocationURI with empty location
	 * This is a regression test for bug 266662
	 */
	public void testValidateEmptyLinkLocation() {
		IFolder folder = nonExistingFolderInExistingProject;
		IPath newLocation = IPath.fromOSString("");
		URI newLocationURI = URIUtil.toURI(newLocation);
		IStatus linkedResourceStatus = getWorkspace().validateLinkLocation(folder, newLocation);
		assertEquals("1.0", IStatus.ERROR, linkedResourceStatus.getSeverity());
		linkedResourceStatus = getWorkspace().validateLinkLocationURI(folder, newLocationURI);
		assertEquals("1.1", IStatus.ERROR, linkedResourceStatus.getSeverity());
	}

	/**
	 * Tests creating a linked resource whose location contains a colon character.
	 */
	public void testLocationWithColon() throws CoreException {
		//windows does not allow a location with colon in the name
		if (OS.isWindows()) {
			return;
		}
		IFolder folder = nonExistingFolderInExistingProject;
		// Note that on *nix, "c:/temp" is a relative path with two segments
		// so this is treated as relative to an undefined path variable called "c:".
		IPath location = IPath.fromOSString("c:/temp");
		folder.createLink(location, IResource.ALLOW_MISSING_LOCAL, createTestMonitor());
		assertEquals("1.0", location, folder.getRawLocation());
	}

	/**
	 * Tests the timestamp of a linked file when the local file is created or
	 * deleted. See bug 34150 for more details.
	 */
	public void testModificationStamp() throws Exception {
		IPath location = getRandomLocation();
		deleteOnTearDown(location);
		IFile linkedFile = nonExistingFileInExistingProject;
		linkedFile.createLink(location, IResource.ALLOW_MISSING_LOCAL, createTestMonitor());
		assertEquals("1.0", IResource.NULL_STAMP, linkedFile.getModificationStamp());
		// create local file
		resolve(location).toFile().createNewFile();
		linkedFile.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		assertTrue("2.0", linkedFile.getModificationStamp() >= 0);

		// delete local file
		removeFromFileSystem(resolve(location).toFile());
		linkedFile.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		assertEquals("4.0", IResource.NULL_STAMP, linkedFile.getModificationStamp());
	}

	public void testMoveFile() throws Exception {
		IResource[] sources = new IResource[] {nonExistingFileInExistingProject, nonExistingFileInExistingFolder};
		IResource[] destinations = new IResource[] {existingProject, closedProject, nonExistingFileInOtherExistingProject, nonExistingFileInExistingFolder};
		Boolean[] deepCopy = new Boolean[] {Boolean.TRUE, Boolean.FALSE};
		IProgressMonitor[] monitors = new IProgressMonitor[] {new FussyProgressMonitor(), new CancelingProgressMonitor(), null};
		Object[][] inputs = new Object[][] {sources, destinations, deepCopy, monitors};
		new TestPerformer("LinkedResourceTest.testMoveFile") {
			protected static final String CANCELED = "canceled";

			@Override
			public void cleanUp(Object[] args, int count) throws Exception {
				super.cleanUp(args, count);
				try {
					doCleanup();
				} catch (Exception e) {
					throw new IllegalStateException("invocation " + count + " failed to cleanup", e);
				}
			}

			@Override
			public Object invokeMethod(Object[] args, int count) throws Exception {
				IFile source = (IFile) args[0];
				IResource destination = (IResource) args[1];
				boolean isDeep = ((Boolean) args[2]).booleanValue();
				IProgressMonitor monitor = (IProgressMonitor) args[3];
				if (monitor instanceof FussyProgressMonitor fussy) {
					fussy.prepare();
				}
				try {
					source.createLink(localFile, IResource.NONE, null);
					source.move(destination.getFullPath(), isDeep ? IResource.NONE : IResource.SHALLOW, monitor);
				} catch (OperationCanceledException e) {
					return CANCELED;
				}
				if (monitor instanceof FussyProgressMonitor fussy) {
					fussy.sanityCheck();
				}
				return null;
			}

			@Override
			public boolean shouldFail(Object[] args, int count) {
				IFile source = (IFile) args[0];
				IResource destination = (IResource) args[1];
				boolean isDeep = ((Boolean) args[2]).booleanValue();
				IProgressMonitor monitor = (IProgressMonitor) args[3];
				if (monitor instanceof CancelingProgressMonitor) {
					return false;
				}
				IResource parent = destination.getParent();
				if (!isDeep && parent == null) {
					return true;
				}
				if (!parent.isAccessible()) {
					return true;
				}
				if (source.equals(destination)) {
					return true;
				}
				if (source.getType() != destination.getType()) {
					return true;
				}
				if (destination.exists()) {
					return true;
				}
				//passed all failure case so it should succeed
				return false;
			}

			@Override
			public boolean wasSuccess(Object[] args, Object result, Object[] oldState) throws Exception {
				IResource destination = (IResource) args[1];
				boolean isDeep = ((Boolean) args[2]).booleanValue();
				IProgressMonitor monitor = (IProgressMonitor) args[3];
				IPath sourceLocation = resolve(localFile);
				URI sourceLocationURI = URIUtil.toURI(sourceLocation);
				if (result == CANCELED) {
					return monitor instanceof CancelingProgressMonitor;
				}
				if (!destination.exists()) {
					return false;
				}
				//destination should only be linked for a shallow move
				if (isDeep) {
					if (destination.isLinked()) {
						return false;
					}
					if (resolve(localFile).equals(destination.getLocation())) {
						return false;
					}
					if (!destination.getProject().getLocation().isPrefixOf(destination.getLocation())) {
						return false;
					}
				} else {
					if (!destination.isLinked()) {
						return false;
					}
					if (!sourceLocation.equals(destination.getLocation())) {
						return false;
					}
					if (!sourceLocationURI.equals(destination.getLocationURI())) {
						return false;
					}
				}
				return true;
			}
		}.performTest(inputs);
	}

	public void testMoveFolder() throws Exception {
		IResource[] sourceResources = new IResource[] {nonExistingFolderInExistingProject, nonExistingFolderInExistingFolder};
		IResource[] destinationResources = new IResource[] {existingProject, closedProject, nonExistingProject, existingFolderInExistingProject, nonExistingFolderInOtherExistingProject, nonExistingFolderInExistingFolder};
		IProgressMonitor[] monitors = new IProgressMonitor[] {new FussyProgressMonitor(), new CancelingProgressMonitor(), null};
		Object[][] inputs = new Object[][] {sourceResources, destinationResources, monitors};
		new TestPerformer("LinkedResourceTest.testMoveFolder") {
			protected static final String CANCELED = "canceled";

			@Override
			public void cleanUp(Object[] args, int count) throws Exception {
				super.cleanUp(args, count);
				try {
					doCleanup();
				} catch (Exception e) {
					throw new IllegalStateException("invocation " + count + " failed to cleanup", e);
				}
			}

			@Override
			public Object invokeMethod(Object[] args, int count) throws Exception {
				IFolder source = (IFolder) args[0];
				IResource destination = (IResource) args[1];
				IProgressMonitor monitor = (IProgressMonitor) args[2];
				if (monitor instanceof FussyProgressMonitor fussy) {
					fussy.prepare();
				}
				try {
					source.createLink(localFolder, IResource.NONE, null);
					source.move(destination.getFullPath(), IResource.SHALLOW, monitor);
				} catch (OperationCanceledException e) {
					return CANCELED;
				}
				if (monitor instanceof FussyProgressMonitor fussy) {
					fussy.sanityCheck();
				}
				return null;
			}

			@Override
			public boolean shouldFail(Object[] args, int count) {
				IFolder source = (IFolder) args[0];
				IResource destination = (IResource) args[1];
				IProgressMonitor monitor = (IProgressMonitor) args[2];
				if (monitor instanceof CancelingProgressMonitor) {
					return false;
				}
				IResource parent = destination.getParent();
				if (parent == null) {
					return true;
				}
				if (source.equals(destination)) {
					return true;
				}
				if (source.getType() != destination.getType()) {
					return true;
				}
				if (!parent.isAccessible()) {
					return true;
				}
				if (destination.exists()) {
					return true;
				}
				//passed all failure case so it should succeed
				return false;
			}

			@Override
			public boolean wasSuccess(Object[] args, Object result, Object[] oldState) throws Exception {
				IResource destination = (IResource) args[1];
				IProgressMonitor monitor = (IProgressMonitor) args[2];
				if (result == CANCELED) {
					return monitor instanceof CancelingProgressMonitor;
				}
				if (!destination.exists()) {
					return false;
				}
				if (!destination.isLinked()) {
					return false;
				}
				if (!resolve(localFolder).equals(destination.getLocation())) {
					return false;
				}
				return true;
			}
		}.performTest(inputs);
	}

	/**
	 * Tests moving a linked file resource that doesn't exist in the file system
	 */
	public void testMoveMissingFile() throws CoreException {
		IPath location = getRandomLocation();
		IFile linkedFile = nonExistingFileInExistingProject;
		linkedFile.createLink(location, IResource.ALLOW_MISSING_LOCAL, createTestMonitor());

		IFile dest = existingProject.getFile("FailedMoveDest");
		assertThrows(CoreException.class, () -> linkedFile.move(dest.getFullPath(), IResource.NONE, createTestMonitor()));
		assertTrue("2.1", !dest.exists());
		assertThrows(CoreException.class, () -> linkedFile.move(dest.getFullPath(), IResource.FORCE, createTestMonitor()));
		assertTrue("2.3", !dest.exists());
	}

	/**
	 * Tests moving a linked folder that doesn't exist in the file system
	 */
	public void testMoveMissingFolder() throws CoreException {
		IPath location = getRandomLocation();
		IFolder linkedFolder = nonExistingFolderInExistingProject;
		linkedFolder.createLink(location, IResource.ALLOW_MISSING_LOCAL, createTestMonitor());

		IFolder dest = existingProject.getFolder("FailedMoveDest");
		assertThrows(CoreException.class, () -> linkedFolder.move(dest.getFullPath(), IResource.NONE, createTestMonitor()));
		assertTrue("2.1", !dest.exists());
		assertThrows(CoreException.class, () -> linkedFolder.move(dest.getFullPath(), IResource.FORCE, createTestMonitor()));
		assertTrue("2.3", !dest.exists());
	}

	public void testMoveProjectWithLinks() throws Exception {
		IPath fileLocation = getRandomLocation();
		deleteOnTearDown(fileLocation);
		IFile file = nonExistingFileInExistingProject;
		IFolder folder = nonExistingFolderInExistingProject;
		IFile childFile = folder.getFile(childName);
		IResource[] oldResources = new IResource[] {file, folder, existingProject, childFile};
		createFileInFileSystem(resolve(fileLocation));
		folder.createLink(localFolder, IResource.NONE, createTestMonitor());
		file.createLink(fileLocation, IResource.NONE, createTestMonitor());

		// move the project
		IProject destination = getWorkspace().getRoot().getProject("MoveTargetProject");
		IFile newFile = destination.getFile(file.getProjectRelativePath());
		IFolder newFolder = destination.getFolder(folder.getProjectRelativePath());
		IFile newChildFile = newFolder.getFile(childName);
		IResource[] newResources = new IResource[] { destination, newFile, newFolder, newChildFile };

		assertDoesNotExistInWorkspace(destination);

		existingProject.move(destination.getFullPath(), IResource.SHALLOW, createTestMonitor());
		assertExistsInWorkspace(newResources);
		assertDoesNotExistInWorkspace(oldResources);

		assertTrue("3.2", newFile.isLinked());
		assertEquals("3.3", resolve(fileLocation), newFile.getLocation());

		assertTrue("3.4", newFolder.isLinked());
		assertEquals("3.5", resolve(localFolder), newFolder.getLocation());

		assertTrue("3.6", destination.isSynchronized(IResource.DEPTH_INFINITE));

		// now do a deep move back to the original project
		destination.move(existingProject.getFullPath(), IResource.NONE, createTestMonitor());
		assertExistsInWorkspace(oldResources);
		assertDoesNotExistInWorkspace(newResources);
		assertTrue("5.3", !file.isLinked());
		assertTrue("5.4", !folder.isLinked());
		assertEquals("5.5", existingProject.getLocation().append(file.getProjectRelativePath()), file.getLocation());
		assertEquals("5.6", existingProject.getLocation().append(folder.getProjectRelativePath()),
				folder.getLocation());
		assertTrue("5.7", existingProject.isSynchronized(IResource.DEPTH_INFINITE));
		assertTrue("5.8", destination.isSynchronized(IResource.DEPTH_INFINITE));
	}

	/**
	 * Tests bug 117402.
	 */
	public void testMoveProjectWithLinks2() throws Exception {
		IPath fileLocation = getRandomLocation();
		deleteOnTearDown(fileLocation);
		IFile linkedFile = existingProject.getFile("(test)");
		createFileInFileSystem(resolve(fileLocation));
		linkedFile.createLink(fileLocation, IResource.NONE, createTestMonitor());

		// move the project
		IProject destination = getWorkspace().getRoot().getProject("CopyTargetProject");
		existingProject.move(destination.getFullPath(), IResource.SHALLOW, createTestMonitor());

		IFile newFile = destination.getFile(linkedFile.getProjectRelativePath());
		assertTrue("3.0", newFile.isLinked());
		assertEquals("3.1", resolve(fileLocation), newFile.getLocation());
	}

	/**
	 * Tests bug 298849.
	 */
	public void testMoveFolderWithLinks() throws Exception {
		// create a folder
		IFolder folderWithLinks = existingProject.getFolder(createUniqueString());
		folderWithLinks.create(true, true, createTestMonitor());

		IPath fileLocation = getRandomLocation();
		deleteOnTearDown(fileLocation);
		createFileInFileSystem(resolve(fileLocation));

		// create a linked file in the folder
		IFile linkedFile = folderWithLinks.getFile(createUniqueString());
		linkedFile.createLink(fileLocation, IResource.NONE, createTestMonitor());

		// there should be an entry in .project for the linked file
		String string = readStringInFileSystem(existingProject.getFile(".project"));
		assertTrue("3.0", string.contains(linkedFile.getProjectRelativePath().toString()));

		// move the folder
		folderWithLinks.move(otherExistingProject.getFolder(createUniqueString()).getFullPath(),
				IResource.SHALLOW | IResource.ALLOW_MISSING_LOCAL, createTestMonitor());

		// both the folder and link in the source project should not exist
		assertFalse("5.0", folderWithLinks.exists());
		assertFalse("6.0", linkedFile.exists());

		// the project description should not contain links
		HashMap<IPath, LinkDescription> links = ((ProjectDescription) existingProject.getDescription()).getLinks();
		assertNull("8.0", links);

		// and the entry from .project should be removed
		string = readStringInFileSystem(existingProject.getFile(".project"));
		assertEquals("9.0", -1, string.indexOf(linkedFile.getProjectRelativePath().toString()));
	}

	public void testNatureVeto() throws CoreException {
		//note: simpleNature has the link veto turned on.

		//test create link on project with nature veto
		IProjectDescription description = existingProject.getDescription();
		description.setNatureIds(new String[] { NATURE_SIMPLE });
		existingProject.setDescription(description, IResource.NONE, createTestMonitor());

		assertThrows(CoreException.class,
				() -> nonExistingFolderInExistingProject.createLink(localFolder, IResource.NONE, createTestMonitor()));
		assertThrows(CoreException.class,
				() -> nonExistingFileInExistingProject.createLink(localFile, IResource.NONE, createTestMonitor()));

		//test add nature with veto to project that already has link
		existingProject.delete(IResource.FORCE, createTestMonitor());
		existingProject.create(createTestMonitor());
		existingProject.open(createTestMonitor());
		nonExistingFolderInExistingProject.createLink(localFolder, IResource.NONE, createTestMonitor());

		IProjectDescription descriptionAfterProjectRecreation = existingProject.getDescription();
		descriptionAfterProjectRecreation.setNatureIds(new String[] { NATURE_SIMPLE });
		assertThrows(CoreException.class,
				() -> existingProject.setDescription(descriptionAfterProjectRecreation, IResource.NONE, createTestMonitor()));
	}

	/**
	 * Tests creating a link within a link, and ensuring that both links still
	 * exist when the project is closed/opened (bug 177367).
	 */
	public void testNestedLink() throws CoreException {
		final IFileStore store1 = getTempStore();
		final IFileStore store2 = getTempStore();
		URI location1 = store1.toURI();
		URI location2 = store2.toURI();
		//folder names are important here, because we want a certain order in the link hash map
		IFolder link = existingProject.getFolder("aA");
		IFolder linkChild = link.getFolder("b");
		store1.mkdir(EFS.NONE, createTestMonitor());
		store2.mkdir(EFS.NONE, createTestMonitor());
		link.createLink(location1, IResource.NONE, createTestMonitor());
		linkChild.createLink(location2, IResource.NONE, createTestMonitor());
		assertTrue("1.0", link.exists());
		assertTrue("1.1", link.isLinked());
		assertTrue("1.2", linkChild.exists());
		assertTrue("1.3", linkChild.isLinked());
		assertEquals("1.4", location1, link.getLocationURI());
		assertEquals("1.5", location2, linkChild.getLocationURI());

		//now delete and recreate the project
		existingProject.delete(IResource.NEVER_DELETE_PROJECT_CONTENT, createTestMonitor());
		existingProject.create(createTestMonitor());
		existingProject.open(IResource.NONE, createTestMonitor());

		assertTrue("2.0", link.exists());
		assertTrue("2.1", link.isLinked());
		assertTrue("2.2", linkChild.exists());
		assertTrue("2.3", linkChild.isLinked());
		assertEquals("2.4", location1, link.getLocationURI());
		assertEquals("2.5", location2, linkChild.getLocationURI());
	}

	/**
	 * Create a project with a linked resource at depth &gt; 2, and refresh it.
	 */
	public void testRefreshDeepLink() throws Exception {
		IFolder link = nonExistingFolderInExistingFolder;
		IPath linkLocation = localFolder;
		IPath localChild = linkLocation.append("Child");
		IFile linkChild = link.getFile(localChild.lastSegment());
		createFileInFileSystem(resolve(localChild));
		link.createLink(linkLocation, IResource.NONE, createTestMonitor());
		assertTrue("1.0", link.exists());
		assertTrue("1.1", linkChild.exists());

		IProject project = link.getProject();
		project.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());

		assertTrue("2.0", link.exists());
		assertTrue("2.1", linkChild.exists());
	}

	public void testLinkedFolderWithOverlappingLocation_Bug293935_() {
		IWorkspace workspace = getWorkspace();

		IPath projectLocation = existingProject.getLocation();
		IFolder folderByIPath = existingProject.getFolder("overlappingLinkedFolderByIPath");
		assertTrue("1.1", !workspace.validateLinkLocation(folderByIPath, projectLocation).isOK());
		assertThrows(CoreException.class,
				() -> folderByIPath.createLink(projectLocation, IResource.NONE, createTestMonitor()));

		URI projectLocationURI = existingProject.getLocationURI();
		IFolder folderByURI = existingProject.getFolder("overlappingLinkedFolderByURI");
		assertTrue("2.1", !workspace.validateLinkLocationURI(folderByURI, projectLocationURI).isOK());
		assertThrows(CoreException.class,
				() -> folderByURI.createLink(projectLocationURI, IResource.NONE, createTestMonitor()));

		// device is missing

		IPath projectLocationWithoutDevice = projectLocation.setDevice(null);
		IFolder folderByUnixLikeIPath = existingProject.getFolder("overlappingLinkedFolderByUnixLikeIPath");
		assertTrue("3.1", !workspace.validateLinkLocation(folderByUnixLikeIPath, projectLocationWithoutDevice).isOK());
		assertThrows(CoreException.class,
				() -> folderByUnixLikeIPath.createLink(projectLocationWithoutDevice, IResource.NONE, createTestMonitor()));

		URI projectLocationURIWithoutDevice = URIUtil.toURI(projectLocationWithoutDevice.toString());
		IFolder folderByUnixLikeURI = existingProject.getFolder("overlappingLinkedFolderByUnixLikeURI");
		assertTrue("4.1", !workspace.validateLinkLocationURI(folderByUnixLikeURI, projectLocationURIWithoutDevice).isOK());
		assertThrows(CoreException.class,
				() -> folderByUnixLikeURI.createLink(projectLocationURIWithoutDevice, IResource.NONE, createTestMonitor()));
	}

	public void testLinkedFolderWithSymlink_Bug338010() throws CoreException {
		// Only activate this test if testing of symbolic links is possible.
		if (!canCreateSymLinks()) {
			return;
		}
		IPath baseLocation = getRandomLocation();
		IPath resolvedBaseLocation = resolve(baseLocation);
		deleteOnTearDown(resolvedBaseLocation);
		IPath symlinkTarget = resolvedBaseLocation.append("dir1/target");
		symlinkTarget.toFile().mkdirs();
		createSymLink(resolvedBaseLocation.toFile(), "symlink", symlinkTarget.toOSString(), true);
		IPath linkChildLocation = baseLocation.append("symlink/dir2");
		IPath resolvedLinkChildLocation = resolve(linkChildLocation);
		File linkChild = resolvedLinkChildLocation.toFile();
		linkChild.mkdir();
		assertTrue("Could not create link at location: " + linkChild, linkChild.exists());

		IFolder folder = nonExistingFolderInExistingProject;
		folder.createLink(linkChildLocation, IResource.NONE, createTestMonitor());
		// Check that the symlink is preserved.
		assertEquals("1.2", resolvedLinkChildLocation, folder.getLocation());
	}

	/**
	 * Tests deleting of the target of a linked folder that itself is a symbolic link.
	 */
	public void testDeleteLinkTarget_Bug507084() throws Exception {
		// Only activate this test if testing of symbolic links is possible.
		if (!canCreateSymLinks()) {
			return;
		}
		IPath baseLocation = getRandomLocation();
		IPath resolvedBaseLocation = resolve(baseLocation);
		deleteOnTearDown(resolvedBaseLocation);
		IPath symlinkTarget = resolvedBaseLocation.append("dir1/A");
		symlinkTarget.append("B/C").toFile().mkdirs();
		IPath linkParentDir = resolvedBaseLocation.append("dir2");
		linkParentDir.toFile().mkdirs();
		createSymLink(linkParentDir.toFile(), "symlink", symlinkTarget.toOSString(), true);

		IFolder folder = nonExistingFolderInExistingProject;
		IPath symLink = linkParentDir.append("symlink");
		folder.createLink(symLink, IResource.NONE, createTestMonitor());
		assertTrue("1.1", folder.exists());
		assertTrue("1.2", folder.getFolder("B/C").exists());
		assertEquals("1.3", symLink, folder.getLocation());
		// Delete the symlink and the directory that contains it.
		symLink.toFile().delete();
		linkParentDir.toFile().delete();
		// Check that the directory that contained the symlink has been deleted.
		assertFalse("2.1", linkParentDir.toFile().exists());
		// Refresh the project.
		folder.getParent().refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		// Check that the linked folder still exists.
		assertTrue("3.1", folder.exists());
		// Check that the contents of the linked folder no longer exist.
		assertFalse("3.2", folder.getFolder("B").exists());
		assertFalse("3.3", folder.getFolder("B/C").exists());
	}
}
