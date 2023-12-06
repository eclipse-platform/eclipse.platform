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
package org.eclipse.core.tests.resources;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertDoesNotExistInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.isReadOnlySupported;
import static org.junit.Assert.assertThrows;

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

public class IFolderTest extends ResourceTest {
	@Override
	protected void tearDown() throws Exception {
		getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);
		super.tearDown();
	}

	public void testChangeCase() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFolder before = project.getFolder("folder");
		IFolder after = project.getFolder("Folder");
		IFile beforeFile = before.getFile("file");
		IFile afterFile = after.getFile("file");

		// create the resources and set some content in a file that will be moved.
		createInWorkspace(before);
		beforeFile.create(getRandomContents(), false, createTestMonitor());

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

	public void testCopyMissingFolder() throws CoreException {
		//tests copying a folder that is missing from the file system
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFolder before = project.getFolder("OldFolder");
		IFolder after = project.getFolder("NewFolder");
		createInWorkspace(project);
		createInWorkspace(before);
		ensureDoesNotExistInFileSystem(before);

		// should fail because 'before' does not exist in the filesystem
		assertThrows(CoreException.class, () -> before.copy(after.getFullPath(), IResource.FORCE, createTestMonitor()));

		//the destination should not exist, because the source does not exist
		assertTrue("1.1", !before.exists());
		assertTrue("1.2", !after.exists());
	}

	public void testCreateDerived() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFolder derived = project.getFolder("derived");
		createInWorkspace(project);
		ensureDoesNotExistInWorkspace(derived);

		derived.create(IResource.DERIVED, true, createTestMonitor());
		assertTrue("1.0", derived.isDerived());
		assertTrue("1.1", !derived.isTeamPrivateMember());
		derived.delete(false, createTestMonitor());
		derived.create(IResource.NONE, true, createTestMonitor());
		assertTrue("2.0", !derived.isDerived());
		assertTrue("2.1", !derived.isTeamPrivateMember());
	}

	public void testDeltaOnCreateDerived() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFolder derived = project.getFolder("derived");
		createInWorkspace(project);

		ResourceDeltaVerifier verifier = new ResourceDeltaVerifier();
		getWorkspace().addResourceChangeListener(verifier, IResourceChangeEvent.POST_CHANGE);

		verifier.addExpectedChange(derived, IResourceDelta.ADDED, IResource.NONE);

		derived.create(IResource.FORCE | IResource.DERIVED, true, createTestMonitor());

		assertTrue("2.0", verifier.isDeltaValid());
	}

	public void testCreateDerivedTeamPrivate() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFolder teamPrivate = project.getFolder("teamPrivate");
		createInWorkspace(project);
		ensureDoesNotExistInWorkspace(teamPrivate);

		teamPrivate.create(IResource.TEAM_PRIVATE | IResource.DERIVED, true, createTestMonitor());
		assertTrue("1.0", teamPrivate.isTeamPrivateMember());
		assertTrue("1.1", teamPrivate.isDerived());

		teamPrivate.delete(false, createTestMonitor());
		teamPrivate.create(IResource.NONE, true, createTestMonitor());
		assertTrue("2.0", !teamPrivate.isTeamPrivateMember());
		assertTrue("2.1", !teamPrivate.isDerived());
	}

	public void testCreateTeamPrivate() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFolder teamPrivate = project.getFolder("teamPrivate");
		createInWorkspace(project);
		ensureDoesNotExistInWorkspace(teamPrivate);

		teamPrivate.create(IResource.TEAM_PRIVATE, true, createTestMonitor());
		assertTrue("1.0", teamPrivate.isTeamPrivateMember());
		assertTrue("1.1", !teamPrivate.isDerived());

		teamPrivate.delete(false, createTestMonitor());
		teamPrivate.create(IResource.NONE, true, createTestMonitor());
		assertTrue("2.0", !teamPrivate.isTeamPrivateMember());
		assertTrue("2.1", !teamPrivate.isDerived());
	}

	public void testFolderCreation() throws Exception {
		// basic folder creation
		IProject project = getWorkspace().getRoot().getProject("Project");
		createInWorkspace(project);

		IFolder target = project.getFolder("Folder1");
		assertTrue("1.0", !target.exists());
		target.create(true, true, createTestMonitor());
		assertTrue("1.1", target.exists());

		// nested folder creation
		IFolder nestedTarget = target.getFolder("Folder2");
		assertTrue("2.0", !nestedTarget.exists());
		nestedTarget.create(true, true, createTestMonitor());
		assertTrue("2.1", nestedTarget.exists());

		// try to create a folder that already exists
		assertTrue("3.0", target.exists());
		IFolder folderTarget = target;
		assertThrows(CoreException.class, () -> folderTarget.create(true, true, createTestMonitor()));
		assertTrue("3.2", target.exists());

		// try to create a folder over a file that exists
		IFile file = target.getFile("File1");
		target = target.getFolder("File1");
		file.create(getRandomContents(), true, createTestMonitor());
		assertTrue("4.0", file.exists());

		IFolder subfolderTarget = target;
		assertThrows(CoreException.class, () -> subfolderTarget.create(true, true, createTestMonitor()));
		assertTrue("5.1", file.exists());
		assertTrue("5.2", !target.exists());

		// try to create a folder on a project (one segment) path
		assertThrows(IllegalArgumentException.class,
				() -> getWorkspace().getRoot().getFolder(IPath.fromOSString("/Folder3")));

		// try to create a folder as a child of a file
		file = project.getFile("File2");
		file.create(null, true, createTestMonitor());

		target = project.getFolder("File2/Folder4");
		assertTrue("7.1", !target.exists());
		IFolder nonexistentSubfolderTarget = target;
		assertThrows(CoreException.class, () -> nonexistentSubfolderTarget.create(true, true, createTestMonitor()));
		assertTrue("7.3", file.exists());
		assertTrue("7.4", !target.exists());

		// try to create a folder under a non-existant parent
		IFolder folder = project.getFolder("Folder5");
		target = folder.getFolder("Folder6");
		assertTrue("8.0", !folder.exists());
		IFolder nonexistentFolderTarget = target;
		assertThrows(CoreException.class, () -> nonexistentFolderTarget.create(true, true, createTestMonitor()));
		assertTrue("8.2", !folder.exists());
		assertTrue("8.3", !target.exists());
	}

	public void testFolderDeletion() throws Throwable {
		IProject project = getWorkspace().getRoot().getProject("Project");
		IResource[] before = buildResources(project, new String[] {"c/", "c/b/", "c/x", "c/b/y", "c/b/z"});
		createInWorkspace(before);
		//
		assertExistsInWorkspace(before);
		project.getFolder("c").delete(true, createTestMonitor());
		assertDoesNotExistInWorkspace(before);
	}

	public void testFolderMove() throws Throwable {
		IProject project = getWorkspace().getRoot().getProject("Project");
		IResource[] before = buildResources(project, new String[] {"b/", "b/b/", "b/x", "b/b/y", "b/b/z"});
		IResource[] after = buildResources(project, new String[] {"a/", "a/b/", "a/x", "a/b/y", "a/b/z"});

		// create the resources and set some content in a file that will be moved.
		createInWorkspace(before);
		String content = getRandomString();
		IFile file = project.getFile(IPath.fromOSString("b/b/z"));
		file.setContents(getContents(content), true, false, createTestMonitor());

		// Be sure the resources exist and then move them.
		assertExistsInWorkspace(before);
		project.getFolder("b").move(project.getFullPath().append("a"), true, createTestMonitor());

		//
		assertDoesNotExistInWorkspace(before);
		assertExistsInWorkspace(after);
		file = project.getFile(IPath.fromOSString("a/b/z"));
		assertTrue("2.1", compareContent(getContents(content), file.getContents(false)));
	}

	public void testFolderOverFile() throws Throwable {
		IPath path = IPath.fromOSString("/Project/File");
		IFile existing = getWorkspace().getRoot().getFile(path);
		createInWorkspace(existing);
		IFolder target = getWorkspace().getRoot().getFolder(path);
		assertThrows("Should not be able to create folder over a file", CoreException.class,
				() -> target.create(true, true, createTestMonitor()));
		assertTrue("2.0", existing.exists());
	}

	/**
	 * Tests creation and manipulation of folder names that are reserved on some platforms.
	 */
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
			assertTrue("1.0 " + name, !folder.exists());
			assertThrows(CoreException.class, () -> folder.create(true, true, createTestMonitor()));
			assertTrue("1.2 " + name, !folder.exists());
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
			assertTrue("2.0 " + name, !folder.exists());
			folder.create(true, true, createTestMonitor());
			assertTrue("2.2 " + name, folder.exists());
		}
	}

	public void testLeafFolderMove() throws Exception {
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFolder source = project.getFolder("Folder1");
		createInWorkspace(source);
		IFolder dest = project.getFolder("Folder2");
		source.move(dest.getFullPath(), true, createTestMonitor());
		assertExistsInWorkspace(dest);
		assertDoesNotExistInWorkspace(source);
	}

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
		assertTrue("1.2", dest.isReadOnly());

		// cleanup - ensure that the files can be deleted.
		source.setReadOnly(false);
		dest.setReadOnly(false);
	}

	public void testSetGetFolderPersistentProperty() throws Throwable {
		IResource target = getWorkspace().getRoot().getFolder(IPath.fromOSString("/Project/Folder"));
		String value = "this is a test property value";
		QualifiedName name = new QualifiedName("itp-test", "testProperty");
		// getting/setting persistent properties on non-existent resources should throw an exception
		ensureDoesNotExistInWorkspace(target);
		assertThrows(CoreException.class, () -> target.getPersistentProperty(name));
		assertThrows(CoreException.class, () -> target.setPersistentProperty(name, value));

		createInWorkspace(target);
		target.setPersistentProperty(name, value);
		// see if we can get the property
		assertTrue("2.0", target.getPersistentProperty(name).equals(value));
		// see what happens if we get a non-existant property
		QualifiedName nonExistentPropertyName = new QualifiedName("itp-test", "testNonProperty");
		assertNull("2.1", target.getPersistentProperty(nonExistentPropertyName));
	}
}
