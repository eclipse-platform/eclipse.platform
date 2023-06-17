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
 *     Sergey Prigogin (Google) - testWorkingLocationDeletion_bug433061
 *     Sergey Prigogin (Google) - [440283] Modify symlink tests to run on Windows with or without administrator privileges
 *******************************************************************************/
package org.eclipse.core.tests.resources;

import static org.junit.Assert.assertArrayEquals;

import java.io.File;
import java.io.InputStream;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.tests.harness.FussyProgressMonitor;

/**
 * This class should be refactored into the black box tests for
 * solution, project, folder and file.
 */
public class WorkspaceTest extends ResourceTest {
	/**
	 * Returns a collection of string paths describing the standard
	 * resource hierarchy for this test.  In the string forms, folders are
	 * represented as having trailing separators ('/').  All other resources
	 * are files.  It is generally assumed that this hierarchy will be
	 * inserted under some solution and project structure.
	 */
	@Override
	public String[] defineHierarchy() {
		return new String[] {"/", "/1/", "/1/1", "/1/2", "/1/3", "/2/", "/2/1", "/2/2", "/2/3", "/3/", "/3/1", "/3/2", "/3/3", "/4/", "/5"};
	}

	protected IProject getTestProject() {
		return getWorkspace().getRoot().getProject("testProject");
	}

	protected IProject getTestProject2() {
		return getWorkspace().getRoot().getProject("testProject2");
	}

	public void setGetPersistentProperty(IResource target) throws Throwable {
		String value = "this is a test property value";
		QualifiedName name = new QualifiedName("itp-test", "testProperty");
		target.setPersistentProperty(name, value);
		// see if we can get the property
		assertTrue("get not equal set", target.getPersistentProperty(name).equals(value));
		// see what happens if we get a non-existant property
		name = new QualifiedName("itp-test", "testNonProperty");
		assertNull("non-existant persistent property not missing", target.getPersistentProperty(name));
	}

	public void testFileDeletion() throws Throwable {
		IPath path = IPath.fromOSString("/testProject/testFileForDelete");
		IFile target = getWorkspace().getRoot().getFile(path);
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		target.create(null, true, monitor);
		monitor.assertUsedUp();
		assertTrue(target.exists());
		monitor.prepare();
		target.delete(true, monitor);
		monitor.assertUsedUp();
		assertTrue(!target.exists());
	}

	public void testFileEmptyDeletion() throws Throwable {
		IPath path = IPath.fromOSString("/testProject/testFileForDelete2");
		IFile target = getWorkspace().getRoot().getFile(path);
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		target.create(getContents(""), true, monitor);
		monitor.assertUsedUp();
		assertTrue(target.exists());
		monitor.prepare();
		target.delete(true, monitor);
		monitor.assertUsedUp();
		assertTrue(!target.exists());
	}

	public void testFileInFolderCreation() throws Throwable {
		IPath path = IPath.fromOSString("/testProject/testFolder/testFile2");
		IFile target = getWorkspace().getRoot().getFile(path);
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		target.create(getRandomContents(), true, monitor);
		monitor.assertUsedUp();
		assertTrue(target.exists());
	}

	public void testFileMove() throws Throwable {
		IPath path = IPath.fromOSString("/testProject/targetFile");
		IFile target = getWorkspace().getRoot().getFile(path);
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		target.create(getRandomContents(), true, monitor);
		monitor.assertUsedUp();
		IFile destination = getWorkspace().getRoot().getFile(IPath.fromOSString("/testProject/movedFile"));
		monitor.prepare();
		target.move(destination.getFullPath(), true, monitor);
		monitor.assertUsedUp();
		assertTrue(destination.exists());
		assertTrue(!target.exists());
	}

	public void testFileOverFolder() throws Throwable {
		IPath path = IPath.fromOSString("/testProject/testFolder");
		IFolder existing = getWorkspace().getRoot().getFolder(path);
		assertTrue(existing.exists());
		IFile target = getWorkspace().getRoot().getFile(path);
		try {
			FussyProgressMonitor monitor = new FussyProgressMonitor();
			target.create(null, true, monitor);
			monitor.assertUsedUp();
		} catch (CoreException e) {
			assertTrue(existing.exists());
			return;
		}
		fail("Should not be able to create file over folder");
	}

	public void testFolderDeletion() throws Throwable {
		IProject project = getTestProject();
		IResource[] before = buildResources(project, new String[] {"c/", "c/b/", "c/x", "c/b/y", "c/b/z"});
		ensureExistsInWorkspace(before, true);
		//
		assertExistsInWorkspace(before);
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		project.getFolder("c").delete(true, monitor);
		monitor.assertUsedUp();
		assertDoesNotExistInWorkspace(before);
	}

	public void testFolderMove() throws Throwable {
		IProject project = getTestProject();
		IResource[] before = buildResources(project, new String[] {"b/", "b/b/", "b/x", "b/b/y", "b/b/z"});
		IResource[] after = buildResources(project, new String[] {"a/", "a/b/", "a/x", "a/b/y", "a/b/z"});

		// create the resources and set some content in a file that will be moved.
		ensureExistsInWorkspace(before, true);
		String content = getRandomString();
		IFile file = project.getFile(IPath.fromOSString("b/b/z"));
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		file.setContents(getContents(content), true, false, monitor);
		monitor.assertUsedUp();

		// Be sure the resources exist and then move them.
		assertExistsInWorkspace(before);
		monitor.prepare();
		project.getFolder("b").move(project.getFullPath().append("a"), true, monitor);
		monitor.assertUsedUp();

		//
		assertDoesNotExistInWorkspace(before);
		assertExistsInWorkspace(after);
		file = project.getFile(IPath.fromOSString("a/b/z"));
		assertTrue("get not equal set", compareContent(getContents(content), file.getContents(false)));
	}

	public void testFolderOverFile() throws Throwable {
		IPath path = IPath.fromOSString("/testProject/testFile");
		IFile existing = getWorkspace().getRoot().getFile(path);
		assertTrue(existing.exists());
		IFolder target = getWorkspace().getRoot().getFolder(path);
		try {
			FussyProgressMonitor monitor = new FussyProgressMonitor();
			monitor.prepare();
			target.create(true, true, monitor);
			monitor.assertUsedUp();
		} catch (CoreException e) {
			assertTrue(existing.exists());
			return;
		}
		fail("Should not be able to create folder over a file");
	}

	public void testLeafFolderMove() throws Throwable {
		IProject project = getTestProject();
		IFolder source = project.getFolder("testFolder");
		IFolder dest = project.getFolder("movedFolder");
		//
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		source.move(dest.getFullPath(), true, monitor);
		monitor.assertUsedUp();
		assertExistsInWorkspace(dest);
		assertDoesNotExistInWorkspace(source);
	}

	public void testMultiCreation() throws Throwable {
		final IProject project = getWorkspace().getRoot().getProject("bar");
		final IResource[] resources = buildResources(project, new String[] {"a/", "a/b"});
		IWorkspaceRunnable body = monitor -> {
			if (!project.exists()) {
				project.create(null);
			}
			if (!project.isOpen()) {
				project.open(null);
			}
			// define an operation which will create a bunch of resources including a project.
			for (IResource resource : resources) {
				switch (resource.getType()) {
					case IResource.FILE :
					((IFile) resource).create(null, false, monitor);
						break;
					case IResource.FOLDER :
					((IFolder) resource).create(false, true, monitor);
						break;
					case IResource.PROJECT :
					((IProject) resource).create(monitor);
						break;
				}
			}
		};
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		getWorkspace().run(body, monitor);
		monitor.assertUsedUp();
		assertExistsInWorkspace(project);
		assertExistsInWorkspace(resources);
	}

	public void testMultiDeletion() throws Throwable {
		IProject project = getTestProject();
		IResource[] before = buildResources(project, new String[] {"c/", "c/b/", "c/x", "c/b/y", "c/b/z"});
		ensureExistsInWorkspace(before, true);
		//
		assertExistsInWorkspace(before);
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		getWorkspace().delete(before, true, monitor);
		monitor.assertUsedUp();
		assertDoesNotExistInWorkspace(before);
	}

	public void testProjectCloseOpen() throws Throwable {
		IProject target = getTestProject();
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		target.close(monitor);
		monitor.assertUsedUp();
		assertTrue(target.exists());
		assertTrue(!target.isOpen());
		assertTrue(!target.getFolder("testFolder").exists());
		monitor.prepare();
		target.open(monitor);
		monitor.assertUsedUp();
		assertTrue(target.isOpen());
		assertTrue(target.getFolder("testFolder").exists());
	}

	public void testProjectCreateOpenCloseDelete() throws Throwable {
		IProject target = getTestProject2();
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		target.create(null, monitor);
		monitor.assertUsedUp();
		monitor.assertUsedUp();
		assertTrue(target.exists());
		monitor.prepare();
		target.open(monitor);
		monitor.assertUsedUp();
		assertTrue(target.isOpen());
		monitor.prepare();
		target.close(monitor);
		monitor.assertUsedUp();
		assertTrue(!target.isOpen());
		monitor.prepare();
		target.delete(true, true, monitor);
		monitor.assertUsedUp();
		assertTrue(!target.exists());
	}

	@Override
	public void setUp() throws Exception {
		IProject target = getTestProject();
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		target.create(null, monitor);
		monitor.assertUsedUp();
		assertTrue(target.exists());
		monitor.prepare();
		target.open(monitor);
		monitor.assertUsedUp();
		assertTrue(target.isOpen());
		IPath path = IPath.fromOSString("/testProject/testFolder");
		IFolder folderTarget = getWorkspace().getRoot().getFolder(path);
		monitor = new FussyProgressMonitor();
		folderTarget.create(true, true, monitor);
		monitor.assertUsedUp();
		assertTrue(folderTarget.exists());
		IPath filePath = IPath.fromOSString("/testProject/testFile");
		IFile fileTarget = getWorkspace().getRoot().getFile(filePath);
		monitor = new FussyProgressMonitor();
		fileTarget.create(null, true, monitor);
		monitor.assertUsedUp();
		assertTrue(fileTarget.exists());
		String testString = getRandomString();
		monitor = new FussyProgressMonitor();
		fileTarget.setContents(getContents(testString), true, false, monitor);
		monitor.assertUsedUp();
		try (InputStream content = fileTarget.getContents(false)) {
			assertTrue("get not equal set", compareContent(content, getContents(testString)));
		}
	}

	public void testProjectDeletion() throws Throwable {
		IProject target = getTestProject();
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		target.delete(true, monitor);
		monitor.assertUsedUp();
		assertTrue("Project Deletion failed", !target.exists());
	}

	public void testWorkingLocationDeletion_bug433061() throws Throwable {
		// Only activate this test if testing of symbolic links is possible.
		if (!canCreateSymLinks()) {
			return;
		}
		IProject project = getTestProject();
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		IPath workingLocation = project.getWorkingLocation("org.eclipse.core.tests.resources");
		IPath linkTarget = getRandomLocation();
		try {
			linkTarget.toFile().mkdirs();
			File file = linkTarget.append("aFile").toFile();
			assertTrue(file.createNewFile());
			assertTrue(file.exists());
			// Create a symlink in the working location of the project pointing to linkTarget.
			createSymLink(workingLocation.toFile(), "link", linkTarget.toOSString(), true);
			monitor.prepare();
			project.delete(true, monitor);
			monitor.assertUsedUp();
			assertTrue("Project deletion failed", !project.exists());
			assertTrue("Working location was not deleted", !workingLocation.toFile().exists());
			assertTrue("File inside a symlinked directory got deleted", file.exists());
		} finally {
			Workspace.clear(linkTarget.toFile());
		}
	}

	public void testProjectReferences() throws Throwable {
		IProject target = getTestProject2();
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		target.create(null, monitor);
		monitor.assertUsedUp();
		assertTrue(target.exists());
		IProject project = getTestProject();
		IProjectDescription description = project.getDescription();
		description.setReferencedProjects(new IProject[] {target});
		monitor.prepare();
		project.setDescription(description, monitor);
		monitor.assertUsedUp();
		assertTrue(target.getReferencingProjects().length == 1);

		monitor.prepare();
		target.delete(true, true, monitor);
		monitor.assertUsedUp();
		assertTrue(!target.exists());
	}

	public void testDanglingReferences() throws Throwable {
		IProject p1 = null;
		IProject p2 = null;
		p2 = getWorkspace().getRoot().getProject("p2");
		p2.create(new NullProgressMonitor());
		p1 = getWorkspace().getRoot().getProject("p1");
		IProjectDescription description = getWorkspace().newProjectDescription("p1");
		description.setReferencedProjects(new IProject[] { p2 });
		p1.create(description, new NullProgressMonitor());
		p1.open(new NullProgressMonitor());
		assertFalse(getWorkspace().getDanglingReferences().containsKey(p1));
		p2.delete(true, true, new NullProgressMonitor());
		assertArrayEquals(new IProject[] { p2 }, getWorkspace().getDanglingReferences().get(p1));
	}

	public void testSetContents() throws Throwable {
		IPath path = IPath.fromOSString("/testProject/testFile");
		IFile target = getWorkspace().getRoot().getFile(path);
		String testString = getRandomString();
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		target.setContents(getContents(testString), true, false, monitor);
		monitor.assertUsedUp();
		try (InputStream content = target.getContents(false)) {
			assertTrue("get not equal set", compareContent(content, getContents(testString)));
		}
	}

	public void testSetGetFilePersistentProperty() throws Throwable {
		IResource target = getWorkspace().getRoot().getFile(IPath.fromOSString("/testProject/testFile"));
		setGetPersistentProperty(target);
	}

	public void testSetGetFolderPersistentProperty() throws Throwable {
		IResource target = getWorkspace().getRoot().getFolder(IPath.fromOSString("/testProject/testFolder"));
		setGetPersistentProperty(target);
	}

	public void testSetGetProjectPersistentProperty() throws Throwable {
		IResource target = getWorkspace().getRoot().getProject("/testProject");
		setGetPersistentProperty(target);
	}

	public void testSetProperty() throws Throwable {
		IPath path = IPath.fromOSString("/testProject/testFile");
		IFile target = getWorkspace().getRoot().getFile(path);
		String value = "this is a test property value";
		QualifiedName name = new QualifiedName("itp-test", "testProperty");
		target.setPersistentProperty(name, value);
		assertTrue("get not equal set", target.getPersistentProperty(name).equals(value));
	}

	public void testSimpleMove() throws Throwable {
		IPath path = IPath.fromOSString("/testProject/simpleFile");
		IFile target = getWorkspace().getRoot().getFile(path);
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		target.create(getRandomContents(), true, monitor);
		monitor.assertUsedUp();
		IFile destination = getWorkspace().getRoot().getFile(IPath.fromOSString("/testProject/newSimpleFile"));
		monitor.prepare();
		target.move(destination.getFullPath(), true, monitor);
		monitor.assertUsedUp();
		assertTrue(destination.exists());
		assertTrue(!target.exists());
	}

}
