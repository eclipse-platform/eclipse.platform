/*******************************************************************************
 *  Copyright (c) 2005, 2014 IBM Corporation and others.
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
 *     Martin Oberhuber (Wind River) - [335864] ResourceAttributeTest fails on Win7
 *     Sergey Prigogin (Google) - [440283] Modify symlink tests to run on Windows with or without administrator privileges
 *******************************************************************************/
package org.eclipse.core.tests.resources;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.harness.FileSystemHelper.canCreateSymLinks;
import static org.eclipse.core.tests.harness.FileSystemHelper.createSymLink;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomString;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createUniqueString;
import static org.eclipse.core.tests.resources.ResourceTestUtil.isAttributeSupported;
import static org.eclipse.core.tests.resources.ResourceTestUtil.removeFromWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.setReadOnly;
import static org.eclipse.core.tests.resources.ResourceTestUtil.waitForRefresh;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(WorkspaceResetExtension.class)
public class ResourceAttributeTest {

	private void setArchive(IResource resource, boolean value) throws CoreException {
		ResourceAttributes attributes = resource.getResourceAttributes();
		assertNotNull(attributes);
		attributes.setArchive(value);
		resource.setResourceAttributes(attributes);
	}

	private void setExecutable(IResource resource, boolean value) throws CoreException {
		ResourceAttributes attributes = resource.getResourceAttributes();
		assertNotNull(attributes);
		attributes.setExecutable(value);
		resource.setResourceAttributes(attributes);
	}

	private void setHidden(IResource resource, boolean value) throws CoreException {
		ResourceAttributes attributes = resource.getResourceAttributes();
		assertNotNull(attributes);
		attributes.setHidden(value);
		resource.setResourceAttributes(attributes);
	}

	private void setSymlink(IResource resource, boolean value) throws CoreException {
		ResourceAttributes attributes = resource.getResourceAttributes();
		assertNotNull(attributes);
		attributes.setSymbolicLink(value);
		resource.setResourceAttributes(attributes);
	}

	@Test
	public void testAttributeArchive() throws CoreException {
		assumeTrue(isAttributeSupported(EFS.ATTRIBUTE_ARCHIVE),
				"only relevant for platforms supporting archive attribute");

		IProject project = getWorkspace().getRoot().getProject("Project");
		IFile file = project.getFile("target");
		createInWorkspace(file, createRandomString());

		// file bit is set already for a new file
		assertTrue(file.getResourceAttributes().isArchive());
		setArchive(file, false);
		assertFalse(file.getResourceAttributes().isArchive());
		setArchive(file, true);
		assertTrue(file.getResourceAttributes().isArchive());

		// folder bit is not set already for a new folder
		assertFalse(project.getResourceAttributes().isArchive());
		setArchive(project, true);
		assertTrue(project.getResourceAttributes().isArchive());
		setArchive(project, false);
		assertFalse(project.getResourceAttributes().isArchive());
	}

	@Test
	public void testAttributeExecutable() throws CoreException {
		assumeTrue(isAttributeSupported(EFS.ATTRIBUTE_EXECUTABLE),
				"only relevant for platforms supporting executable attribute");

		IProject project = getWorkspace().getRoot().getProject("Project");
		IFile file = project.getFile("target");
		createInWorkspace(file, createRandomString());

		// file
		assertFalse(file.getResourceAttributes().isExecutable());
		setExecutable(file, true);
		assertTrue(file.getResourceAttributes().isExecutable());
		setExecutable(file, false);
		assertFalse(file.getResourceAttributes().isExecutable());

		// folder
		// folder is executable initially
		assertTrue(project.getResourceAttributes().isExecutable());
		setExecutable(project, false);
		assertFalse(project.getResourceAttributes().isExecutable());
		setExecutable(project, true);
		assertTrue(project.getResourceAttributes().isExecutable());
	}

	@Test
	public void testAttributeHidden() throws CoreException {
		assumeTrue(isAttributeSupported(EFS.ATTRIBUTE_HIDDEN),
				"only relevant for platforms supporting hidden attribute");

		IProject project = getWorkspace().getRoot().getProject("Project");
		IFile file = project.getFile("target");
		createInWorkspace(file, createRandomString());

		// file
		assertFalse(file.getResourceAttributes().isHidden());
		setHidden(file, true);
		assertTrue(file.getResourceAttributes().isHidden());
		setHidden(file, false);
		assertFalse(file.getResourceAttributes().isHidden());

		// folder
		assertFalse(project.getResourceAttributes().isHidden());
		setHidden(project, true);
		assertTrue(project.getResourceAttributes().isHidden());
		setHidden(project, false);
		assertFalse(project.getResourceAttributes().isHidden());
	}

	@Test
	public void testAttributeReadOnly() throws CoreException {
		assumeTrue(isAttributeSupported(EFS.ATTRIBUTE_READ_ONLY),
				"only relevant for platforms supporting read-only attribute");

		IProject project = getWorkspace().getRoot().getProject("Project");
		IFile file = project.getFile("target");
		createInWorkspace(file, createRandomString());

		// file
		assertFalse(file.getResourceAttributes().isReadOnly());
		setReadOnly(file, true);
		assertTrue(file.getResourceAttributes().isReadOnly());
		setReadOnly(file, false);
		assertFalse(file.getResourceAttributes().isReadOnly());

		// folder
		assertFalse(project.getResourceAttributes().isReadOnly());
		setReadOnly(project, true);
		assertTrue(project.getResourceAttributes().isReadOnly());
		setReadOnly(project, false);
		assertFalse(project.getResourceAttributes().isReadOnly());
	}

	/**
	 * Attributes of a closed project should be null.
	 */
	@Test
	public void testClosedProject() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("Project");
		createInWorkspace(project);
		project.close(createTestMonitor());
		assertNull(project.getResourceAttributes());
	}

	@Test
	public void testNonExistingResource() throws CoreException {
		//asking for attributes of a non-existent resource should return null
		IProject project = getWorkspace().getRoot().getProject("testNonExistingResource");
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file");
		removeFromWorkspace(project);
		assertNull(project.getResourceAttributes());
		assertNull(folder.getResourceAttributes());
		assertNull(file.getResourceAttributes());

		//now create the resources and ensure non-null result
		createInWorkspace(project);
		createInWorkspace(folder);
		createInWorkspace(file);
		assertNotNull(project.getResourceAttributes());
		assertNotNull(folder.getResourceAttributes());
		assertNotNull(file.getResourceAttributes());
	}

	/**
	 * When the executable bit is cleared on a folder, it effectively
	 * causes the children of that folder to be removed from the
	 * workspace because the folder contents can no longer be listed.
	 * A refresh should happen automatically when the executable
	 * bit on a folder is changed. See bug 109979 for details.
	 *
	 * Test commented out because current failing on Hudson.
	 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=397353
	 */
	@Test
	@Disabled("currently failing on Hudson: see https://bugs.eclipse.org/bugs/show_bug.cgi?id=397353")
	public void testRefreshExecutableOnFolder() throws CoreException {
		assumeTrue(isAttributeSupported(EFS.ATTRIBUTE_EXECUTABLE),
				"only relevant for platforms supporting executable attribute");

		IProject project = getWorkspace().getRoot().getProject("testRefreshExecutableOnFolder");
		IFolder folder = project.getFolder("folder");
		IFile file = folder.getFile("file");
		createInWorkspace(file, createRandomString());

		// folder is executable initially and the file should exist
		assertTrue(project.getResourceAttributes().isExecutable());
		assertTrue(file.exists());

		setExecutable(folder, false);
		waitForRefresh();

		boolean wasExecutable = folder.getResourceAttributes().isExecutable();
		boolean fileExists = file.exists();

		// set the folder executable before asserting anything, otherwise cleanup will
		// fail
		setExecutable(folder, true);

		assertFalse(wasExecutable);
		assertFalse(fileExists);
	}

	@Test
	public void testAttributeSymlink() throws Exception {
		assumeTrue(canCreateSymLinks(), "only relevant for platforms supporting symbolic links");

		IProject project = getWorkspace().getRoot().getProject("Project");
		IFile link = project.getFile("link");
		createInWorkspace(link, createRandomString());

		// attempts to set the symbolic link attribute wont't affect
		// the resource and the underlying file
		assertFalse(link.getResourceAttributes().isSymbolicLink());
		setSymlink(link, true);
		assertFalse(link.getResourceAttributes().isSymbolicLink());
		setSymlink(link, false);
		assertFalse(link.getResourceAttributes().isSymbolicLink());

		removeFromWorkspace(link);

		// create the target file in the filesystem
		IFile target = project.getFile("target");
		createInFileSystem(target);

		// create a link to the target file and add it to the workspace,
		// the resource in the workspace should have symbolic link attribute set
		createSymLink(project.getLocation().toFile(), "link", "target", false);
		createInWorkspace(link);
		assertTrue(link.getResourceAttributes().isSymbolicLink());

		// attempts to clear the symbolic link attribute shouldn't affect
		// the resource and the underlying file
		setSymlink(link, false);
		assertTrue(link.getResourceAttributes().isSymbolicLink());

		// remove the underlying file and add it again as a local file,
		// the resource in the workspace should have the symbolic link attribute
		// cleared
		String s = link.getLocation().toOSString();

		link.getLocation().toFile().delete();
		new File(s).createNewFile();
		assertFalse(link.getResourceAttributes().isSymbolicLink());
	}

	@Test
	public void testAttributes() throws CoreException {
		int[] attributes = new int[] {EFS.ATTRIBUTE_GROUP_READ, EFS.ATTRIBUTE_GROUP_WRITE, EFS.ATTRIBUTE_GROUP_EXECUTE, EFS.ATTRIBUTE_OTHER_READ, EFS.ATTRIBUTE_OTHER_WRITE, EFS.ATTRIBUTE_OTHER_EXECUTE};

		IProject project = getWorkspace().getRoot().getProject(createUniqueString());
		IFile file = project.getFile(createUniqueString());
		createInWorkspace(file, createRandomString());

		for (int attribute : attributes) {
			// only activate this test on platforms that support it
			if (!isAttributeSupported(attribute)) {
				continue;
			}

			// file
			ResourceAttributes resAttr = file.getResourceAttributes();
			resAttr.set(attribute, true);
			file.setResourceAttributes(resAttr);
			assertTrue(file.getResourceAttributes().isSet(attribute));

			resAttr.set(attribute, false);
			file.setResourceAttributes(resAttr);
			assertFalse(file.getResourceAttributes().isSet(attribute));

			// folder
			resAttr = project.getResourceAttributes();
			resAttr.set(attribute, true);
			project.setResourceAttributes(resAttr);
			assertTrue(project.getResourceAttributes().isSet(attribute));

			resAttr.set(attribute, false);
			project.setResourceAttributes(resAttr);
			assertFalse(project.getResourceAttributes().isSet(attribute));
		}
	}

}
