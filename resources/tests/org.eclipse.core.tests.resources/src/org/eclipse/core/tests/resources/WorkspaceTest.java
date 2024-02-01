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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.harness.FileSystemHelper.canCreateSymLinks;
import static org.eclipse.core.tests.harness.FileSystemHelper.createSymLink;
import static org.eclipse.core.tests.harness.FileSystemHelper.getRandomLocation;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertDoesNotExistInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.buildResources;
import static org.eclipse.core.tests.resources.ResourceTestUtil.compareContent;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInputStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomContentsStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.InputStream;
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * This class should be refactored into the black box tests for
 * solution, project, folder and file.
 */
public class WorkspaceTest {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

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

	@Test
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

	@Test
	public void testFileEmptyDeletion() throws Throwable {
		IPath path = IPath.fromOSString("/testProject/testFileForDelete2");
		IFile target = getWorkspace().getRoot().getFile(path);
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		target.create(createInputStream(""), true, monitor);
		monitor.assertUsedUp();
		assertTrue(target.exists());
		monitor.prepare();
		target.delete(true, monitor);
		monitor.assertUsedUp();
		assertTrue(!target.exists());
	}

	@Test
	public void testFileInFolderCreation() throws Throwable {
		IPath path = IPath.fromOSString("/testProject/testFolder/testFile2");
		IFile target = getWorkspace().getRoot().getFile(path);
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		target.create(createRandomContentsStream(), true, monitor);
		monitor.assertUsedUp();
		assertTrue(target.exists());
	}

	@Test
	public void testFileMove() throws Throwable {
		IPath path = IPath.fromOSString("/testProject/targetFile");
		IFile target = getWorkspace().getRoot().getFile(path);
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		target.create(createRandomContentsStream(), true, monitor);
		monitor.assertUsedUp();
		IFile destination = getWorkspace().getRoot().getFile(IPath.fromOSString("/testProject/movedFile"));
		monitor.prepare();
		target.move(destination.getFullPath(), true, monitor);
		monitor.assertUsedUp();
		assertTrue(destination.exists());
		assertTrue(!target.exists());
	}

	@Test
	public void testFileOverFolder() throws Throwable {
		IPath path = IPath.fromOSString("/testProject/testFolder");
		IFolder existing = getWorkspace().getRoot().getFolder(path);
		assertTrue(existing.exists());
		IFile target = getWorkspace().getRoot().getFile(path);
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		assertThrows(CoreException.class, () -> target.create(null, true, monitor));
		monitor.assertUsedUp();
		assertTrue(existing.exists());
	}

	@Test
	public void testFolderDeletion() throws Throwable {
		IProject project = getTestProject();
		IResource[] before = buildResources(project, new String[] {"c/", "c/b/", "c/x", "c/b/y", "c/b/z"});
		createInWorkspace(before);
		//
		assertExistsInWorkspace(before);
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		project.getFolder("c").delete(true, monitor);
		monitor.assertUsedUp();
		assertDoesNotExistInWorkspace(before);
	}

	@Test
	public void testFolderMove() throws Throwable {
		IProject project = getTestProject();
		IResource[] before = buildResources(project, new String[] {"b/", "b/b/", "b/x", "b/b/y", "b/b/z"});
		IResource[] after = buildResources(project, new String[] {"a/", "a/b/", "a/x", "a/b/y", "a/b/z"});

		// create the resources and set some content in a file that will be moved.
		createInWorkspace(before);
		String content = createRandomString();
		IFile file = project.getFile(IPath.fromOSString("b/b/z"));
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		file.setContents(createInputStream(content), true, false, monitor);
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
		assertTrue("get not equal set", compareContent(createInputStream(content), file.getContents(false)));
	}

	@Test
	public void testFolderOverFile() throws Throwable {
		IPath path = IPath.fromOSString("/testProject/testFile");
		IFile existing = getWorkspace().getRoot().getFile(path);
		assertTrue(existing.exists());
		IFolder target = getWorkspace().getRoot().getFolder(path);
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		monitor.prepare();
		assertThrows(CoreException.class, () -> target.create(true, true, monitor));
		monitor.assertUsedUp();
		assertTrue(existing.exists());
	}

	@Test
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

	@Test
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

	@Test
	public void testMultiDeletion() throws Throwable {
		IProject project = getTestProject();
		IResource[] before = buildResources(project, new String[] {"c/", "c/b/", "c/x", "c/b/y", "c/b/z"});
		createInWorkspace(before);
		//
		assertExistsInWorkspace(before);
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		getWorkspace().delete(before, true, monitor);
		monitor.assertUsedUp();
		assertDoesNotExistInWorkspace(before);
	}

	@Test
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

	@Test
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

	@Before
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
		String testString = createRandomString();
		monitor = new FussyProgressMonitor();
		fileTarget.setContents(createInputStream(testString), true, false, monitor);
		monitor.assertUsedUp();
		try (InputStream content = fileTarget.getContents(false)) {
			assertTrue("get not equal set", compareContent(content, createInputStream(testString)));
		}
	}

	@Test
	public void testProjectDeletion() throws Throwable {
		IProject target = getTestProject();
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		target.delete(true, monitor);
		monitor.assertUsedUp();
		assertTrue("Project Deletion failed", !target.exists());
	}

	@Test
	public void testWorkingLocationDeletion_bug433061() throws Throwable {
		assumeTrue("only relevant for platforms supporting symbolic links", canCreateSymLinks());

		IProject project = getTestProject();
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		IPath workingLocation = project.getWorkingLocation("org.eclipse.core.tests.resources");
		IPath linkTarget = getRandomLocation();
		workspaceRule.deleteOnTearDown(linkTarget);
		linkTarget.toFile().mkdirs();
		File file = linkTarget.append("aFile").toFile();
		assertTrue(file.createNewFile());
		assertTrue(file.exists());
		// Create a symlink in the working location of the project pointing to
		// linkTarget.
		createSymLink(workingLocation.toFile(), "link", linkTarget.toOSString(), true);
		monitor.prepare();
		project.delete(true, monitor);
		monitor.assertUsedUp();
		assertTrue("Project deletion failed", !project.exists());
		assertTrue("Working location was not deleted", !workingLocation.toFile().exists());
		assertTrue("File inside a symlinked directory got deleted", file.exists());
	}

	@Test
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
		assertThat(target.getReferencingProjects()).hasSize(1);

		monitor.prepare();
		target.delete(true, true, monitor);
		monitor.assertUsedUp();
		assertTrue(!target.exists());
	}

	@Test
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
		assertThat(getWorkspace().getDanglingReferences().get(p1)).containsExactly(p2);
	}

	@Test
	public void testSetContents() throws Throwable {
		IPath path = IPath.fromOSString("/testProject/testFile");
		IFile target = getWorkspace().getRoot().getFile(path);
		String testString = createRandomString();
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		target.setContents(createInputStream(testString), true, false, monitor);
		monitor.assertUsedUp();
		try (InputStream content = target.getContents(false)) {
			assertTrue("get not equal set", compareContent(content, createInputStream(testString)));
		}
	}

	@Test
	public void testSetGetFilePersistentProperty() throws Throwable {
		IResource target = getWorkspace().getRoot().getFile(IPath.fromOSString("/testProject/testFile"));
		setGetPersistentProperty(target);
	}

	@Test
	public void testSetGetFolderPersistentProperty() throws Throwable {
		IResource target = getWorkspace().getRoot().getFolder(IPath.fromOSString("/testProject/testFolder"));
		setGetPersistentProperty(target);
	}

	@Test
	public void testSetGetProjectPersistentProperty() throws Throwable {
		IResource target = getWorkspace().getRoot().getProject("/testProject");
		setGetPersistentProperty(target);
	}

	@Test
	public void testSetProperty() throws Throwable {
		IPath path = IPath.fromOSString("/testProject/testFile");
		IFile target = getWorkspace().getRoot().getFile(path);
		String value = "this is a test property value";
		QualifiedName name = new QualifiedName("itp-test", "testProperty");
		target.setPersistentProperty(name, value);
		assertTrue("get not equal set", target.getPersistentProperty(name).equals(value));
	}

	@Test
	public void testSimpleMove() throws Throwable {
		IPath path = IPath.fromOSString("/testProject/simpleFile");
		IFile target = getWorkspace().getRoot().getFile(path);
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		target.create(createRandomContentsStream(), true, monitor);
		monitor.assertUsedUp();
		IFile destination = getWorkspace().getRoot().getFile(IPath.fromOSString("/testProject/newSimpleFile"));
		monitor.prepare();
		target.move(destination.getFullPath(), true, monitor);
		monitor.assertUsedUp();
		assertTrue(destination.exists());
		assertTrue(!target.exists());
	}

}
