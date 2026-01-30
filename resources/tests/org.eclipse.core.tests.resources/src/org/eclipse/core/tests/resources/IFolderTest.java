/*******************************************************************************
 * Copyright (c) 2000, 2026 IBM Corporation and others.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertDoesNotExistInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.buildResources;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInputStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomContentsStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomString;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.isReadOnlySupported;
import static org.eclipse.core.tests.resources.ResourceTestUtil.removeFromFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.removeFromWorkspace;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform.OS;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(WorkspaceResetExtension.class)
public class IFolderTest {

	@Test
	public void testChangeCase() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFolder before = project.getFolder("folder");
		IFolder after = project.getFolder("Folder");
		IFile beforeFile = before.getFile("file");
		IFile afterFile = after.getFile("file");

		// create the resources and set some content in a file that will be moved.
		createInWorkspace(before);
		beforeFile.create(createRandomContentsStream(), false, createTestMonitor());

		// Be sure the resources exist and then move them.
		assertExistsInWorkspace(before);
		assertExistsInWorkspace(beforeFile);
		assertDoesNotExistInWorkspace(after);
		assertDoesNotExistInWorkspace(afterFile);
		before.move(after.getFullPath(), IResource.NONE, createTestMonitor());

		assertDoesNotExistInWorkspace(before);
		assertDoesNotExistInWorkspace(beforeFile);
		assertExistsInWorkspace(after);
		assertExistsInWorkspace(afterFile);
	}

	@Test
	public void testCopyMissingFolder() throws CoreException {
		//tests copying a folder that is missing from the file system
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFolder before = project.getFolder("OldFolder");
		IFolder after = project.getFolder("NewFolder");
		createInWorkspace(project);
		createInWorkspace(before);
		removeFromFileSystem(before);

		// should fail because 'before' does not exist in the filesystem
		assertThrows(CoreException.class, () -> before.copy(after.getFullPath(), IResource.FORCE, createTestMonitor()));

		//the destination should not exist, because the source does not exist
		assertFalse(before.exists());
		assertFalse(after.exists());
	}

	@Test
	public void testCreateDerived() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFolder derived = project.getFolder("derived");
		createInWorkspace(project);
		removeFromWorkspace(derived);

		derived.create(IResource.DERIVED, true, createTestMonitor());
		assertTrue(derived.isDerived());
		assertFalse(derived.isTeamPrivateMember());
		derived.delete(false, createTestMonitor());
		derived.create(IResource.NONE, true, createTestMonitor());
		assertFalse(derived.isDerived());
		assertFalse(derived.isTeamPrivateMember());
	}

	@Test
	public void testDeltaOnCreateDerived() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFolder derived = project.getFolder("derived");
		createInWorkspace(project);

		ResourceDeltaVerifier verifier = new ResourceDeltaVerifier();
		getWorkspace().addResourceChangeListener(verifier, IResourceChangeEvent.POST_CHANGE);

		verifier.addExpectedChange(derived, IResourceDelta.ADDED, IResource.NONE);

		derived.create(IResource.FORCE | IResource.DERIVED, true, createTestMonitor());

		assertTrue(verifier.isDeltaValid());
	}

	@Test
	public void testCreateDerivedTeamPrivate() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFolder teamPrivate = project.getFolder("teamPrivate");
		createInWorkspace(project);
		removeFromWorkspace(teamPrivate);

		teamPrivate.create(IResource.TEAM_PRIVATE | IResource.DERIVED, true, createTestMonitor());
		assertTrue(teamPrivate.isTeamPrivateMember());
		assertTrue(teamPrivate.isDerived());

		teamPrivate.delete(false, createTestMonitor());
		teamPrivate.create(IResource.NONE, true, createTestMonitor());
		assertFalse(teamPrivate.isTeamPrivateMember());
		assertFalse(teamPrivate.isDerived());
	}

	@Test
	public void testCreateTeamPrivate() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFolder teamPrivate = project.getFolder("teamPrivate");
		createInWorkspace(project);
		removeFromWorkspace(teamPrivate);

		teamPrivate.create(IResource.TEAM_PRIVATE, true, createTestMonitor());
		assertTrue(teamPrivate.isTeamPrivateMember());
		assertFalse(teamPrivate.isDerived());

		teamPrivate.delete(false, createTestMonitor());
		teamPrivate.create(IResource.NONE, true, createTestMonitor());
		assertFalse(teamPrivate.isTeamPrivateMember());
		assertFalse(teamPrivate.isDerived());
	}

	@Test
	public void testFolderCreation() throws Exception {
		// basic folder creation
		IProject project = getWorkspace().getRoot().getProject("Project");
		createInWorkspace(project);

		IFolder target = project.getFolder("Folder1");
		assertFalse(target.exists());
		target.create(true, true, createTestMonitor());
		assertTrue(target.exists());

		// nested folder creation
		IFolder nestedTarget = target.getFolder("Folder2");
		assertFalse(nestedTarget.exists());
		nestedTarget.create(true, true, createTestMonitor());
		assertTrue(nestedTarget.exists());

		// try to create a folder that already exists
		assertTrue(target.exists());
		IFolder folderTarget = target;
		assertThrows(CoreException.class, () -> folderTarget.create(true, true, createTestMonitor()));
		assertTrue(target.exists());

		// try to create a folder over a file that exists
		IFile file = target.getFile("File1");
		target = target.getFolder("File1");
		file.create(createRandomContentsStream(), true, createTestMonitor());
		assertTrue(file.exists());

		IFolder subfolderTarget = target;
		assertThrows(CoreException.class, () -> subfolderTarget.create(true, true, createTestMonitor()));
		assertTrue(file.exists());
		assertFalse(target.exists());

		// try to create a folder on a project (one segment) path
		assertThrows(IllegalArgumentException.class,
				() -> getWorkspace().getRoot().getFolder(IPath.fromOSString("/Folder3")));

		// try to create a folder as a child of a file
		file = project.getFile("File2");
		file.create(null, true, createTestMonitor());

		target = project.getFolder("File2/Folder4");
		assertFalse(target.exists());
		IFolder nonexistentSubfolderTarget = target;
		assertThrows(CoreException.class, () -> nonexistentSubfolderTarget.create(true, true, createTestMonitor()));
		assertTrue(file.exists());
		assertFalse(target.exists());

		// try to create a folder under a non-existant parent
		IFolder folder = project.getFolder("Folder5");
		target = folder.getFolder("Folder6");
		assertFalse(folder.exists());
		IFolder nonexistentFolderTarget = target;
		assertThrows(CoreException.class, () -> nonexistentFolderTarget.create(true, true, createTestMonitor()));
		assertFalse(folder.exists());
		assertFalse(target.exists());
	}

	@Test
	public void testFolderDeletion() throws Throwable {
		IProject project = getWorkspace().getRoot().getProject("Project");
		IResource[] before = buildResources(project, "c/", "c/b/", "c/x", "c/b/y", "c/b/z");
		createInWorkspace(before);
		//
		assertExistsInWorkspace(before);
		project.getFolder("c").delete(true, createTestMonitor());
		assertDoesNotExistInWorkspace(before);
	}

	@Test
	public void testFolderMove() throws Throwable {
		IProject project = getWorkspace().getRoot().getProject("Project");
		IResource[] before = buildResources(project, "b/", "b/b/", "b/x", "b/b/y", "b/b/z");
		IResource[] after = buildResources(project, "a/", "a/b/", "a/x", "a/b/y", "a/b/z");

		// create the resources and set some content in a file that will be moved.
		createInWorkspace(before);
		String content = createRandomString();
		IFile file = project.getFile(IPath.fromOSString("b/b/z"));
		file.setContents(createInputStream(content), true, false, createTestMonitor());

		// Be sure the resources exist and then move them.
		assertExistsInWorkspace(before);
		project.getFolder("b").move(project.getFullPath().append("a"), true, createTestMonitor());

		//
		assertDoesNotExistInWorkspace(before);
		assertExistsInWorkspace(after);
		file = project.getFile(IPath.fromOSString("a/b/z"));
		try (InputStream fileInput = file.getContents(false)) {
			assertThat(fileInput).hasContent(content);
		}
	}

	@Test
	public void testFolderOverFile() throws Throwable {
		IPath path = IPath.fromOSString("/Project/File");
		IFile existing = getWorkspace().getRoot().getFile(path);
		createInWorkspace(existing);
		IFolder target = getWorkspace().getRoot().getFolder(path);
		assertThrows(CoreException.class, () -> target.create(true, true, createTestMonitor()),
				"Should not be able to create folder over a file");
		assertTrue(existing.exists());
	}

	/**
	 * Tests creation and manipulation of folder names that are reserved on some platforms.
	 */
	@Test
	public void testInvalidFolderNames() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("Project");
		createInWorkspace(project);

		//do some tests with invalid names
		String[] names = new String[0];
		if (OS.isWindows()) {
			//invalid windows names
			names = new String[] {"prn", "nul", "con", "aux", "clock$", "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9", "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9", "AUX", "con.foo", "LPT4.txt", "*", "?", "\"", "<", ">", "|"};
		} else {
			//invalid names on non-windows platforms
			names = new String[] {};
		}

		for (String name : names) {
			IFolder folder = project.getFolder(name);
			assertFalse(folder.exists(), name);
			assertThrows(CoreException.class, () -> folder.create(true, true, createTestMonitor()));
			assertFalse(folder.exists(), name);
		}

		//do some tests with valid names that are *almost* invalid
		if (OS.isWindows()) {
			//these names are valid on windows
			names = new String[] {"hello.prn.txt", "null", "con3", "foo.aux", "lpt0", "com0", "com10", "lpt10", ",", "'", ";"};
		} else {
			//these names are valid on non-windows platforms
			names = new String[] {"prn", "nul", "con", "aux", "clock$", "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9", "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9", "con.foo", "LPT4.txt", "*", "?", "\"", "<", ">", "|", "hello.prn.txt", "null", "con3", "foo.aux", "lpt0", "com0", "com10", "lpt10", ",", "'", ";"};
		}
		for (String name : names) {
			IFolder folder = project.getFolder(name);
			assertFalse(folder.exists(), name);
			folder.create(true, true, createTestMonitor());
			assertTrue(folder.exists(), name);
		}
	}

	@Test
	public void testLeafFolderMove() throws Exception {
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFolder source = project.getFolder("Folder1");
		createInWorkspace(source);
		IFolder dest = project.getFolder("Folder2");
		source.move(dest.getFullPath(), true, createTestMonitor());
		assertExistsInWorkspace(dest);
		assertDoesNotExistInWorkspace(source);
	}

	@Test
	public void testReadOnlyFolderCopy() throws Exception {
		// We need to know whether or not we can unset the read-only flag
		// in order to perform this test.
		if (!isReadOnlySupported()) {
			return;
		}
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFolder source = project.getFolder("Folder1");
		createInWorkspace(source);
		source.setReadOnly(true);
		IFolder dest = project.getFolder("Folder2");
		source.copy(dest.getFullPath(), true, createTestMonitor());
		assertExistsInWorkspace(dest);
		assertExistsInWorkspace(source);
		assertTrue(dest.isReadOnly());

		// cleanup - ensure that the files can be deleted.
		source.setReadOnly(false);
		dest.setReadOnly(false);
	}

	@Test
	public void testSetGetFolderPersistentProperty() throws Throwable {
		IResource target = getWorkspace().getRoot().getFolder(IPath.fromOSString("/Project/Folder"));
		String value = "this is a test property value";
		QualifiedName name = new QualifiedName("itp-test", "testProperty");
		// getting/setting persistent properties on non-existent resources should throw an exception
		removeFromWorkspace(target);
		assertThrows(CoreException.class, () -> target.getPersistentProperty(name));
		assertThrows(CoreException.class, () -> target.setPersistentProperty(name, value));

		createInWorkspace(target);
		target.setPersistentProperty(name, value);
		// see if we can get the property
		assertTrue(target.getPersistentProperty(name).equals(value));
		// see what happens if we get a non-existant property
		QualifiedName nonExistentPropertyName = new QualifiedName("itp-test", "testNonProperty");
		assertNull(target.getPersistentProperty(nonExistentPropertyName));
	}

}
