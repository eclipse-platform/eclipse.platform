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

import java.io.File;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;

public class ResourceAttributeTest extends ResourceTest {

	private void setArchive(IResource resource, boolean value) throws CoreException {
		ResourceAttributes attributes = resource.getResourceAttributes();
		assertNotNull("setAchive for null attributes", attributes);
		attributes.setArchive(value);
		resource.setResourceAttributes(attributes);
	}

	private void setExecutable(IResource resource, boolean value) throws CoreException {
		ResourceAttributes attributes = resource.getResourceAttributes();
		assertNotNull("setExecutable for null attributes", attributes);
		attributes.setExecutable(value);
		resource.setResourceAttributes(attributes);
	}

	private void setHidden(IResource resource, boolean value) throws CoreException {
		ResourceAttributes attributes = resource.getResourceAttributes();
		assertNotNull("setHidden for null attributes", attributes);
		attributes.setHidden(value);
		resource.setResourceAttributes(attributes);
	}

	private void setSymlink(IResource resource, boolean value) throws CoreException {
		ResourceAttributes attributes = resource.getResourceAttributes();
		assertNotNull("setSymlink for null attributes", attributes);
		attributes.setSymbolicLink(value);
		resource.setResourceAttributes(attributes);
	}

	public void testAttributeArchive() throws CoreException {
		// only activate this test on platforms that support it
		if (!isAttributeSupported(EFS.ATTRIBUTE_ARCHIVE)) {
			return;
		}
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFile file = project.getFile("target");
		ensureExistsInWorkspace(file, getRandomContents());

		// file bit is set already for a new file
		assertTrue("1.0", file.getResourceAttributes().isArchive());
		setArchive(file, false);
		assertTrue("1.2", !file.getResourceAttributes().isArchive());
		setArchive(file, true);
		assertTrue("1.4", file.getResourceAttributes().isArchive());

		// folder bit is not set already for a new folder
		assertTrue("2.0", !project.getResourceAttributes().isArchive());
		setArchive(project, true);
		assertTrue("2.2", project.getResourceAttributes().isArchive());
		setArchive(project, false);
		assertTrue("2.4", !project.getResourceAttributes().isArchive());
	}

	public void testAttributeExecutable() throws CoreException {
		// only activate this test on platforms that support it
		if (!isAttributeSupported(EFS.ATTRIBUTE_EXECUTABLE)) {
			return;
		}
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFile file = project.getFile("target");
		ensureExistsInWorkspace(file, getRandomContents());

		// file
		assertTrue("1.0", !file.getResourceAttributes().isExecutable());
		setExecutable(file, true);
		assertTrue("1.2", file.getResourceAttributes().isExecutable());
		setExecutable(file, false);
		assertTrue("1.4", !file.getResourceAttributes().isExecutable());

		// folder
		// folder is executable initially
		assertTrue("2.0", project.getResourceAttributes().isExecutable());
		setExecutable(project, false);
		assertTrue("2.2", !project.getResourceAttributes().isExecutable());
		setExecutable(project, true);
		assertTrue("2.4", project.getResourceAttributes().isExecutable());
	}

	public void testAttributeHidden() throws CoreException {
		// only activate this test on platforms that support it
		if (!isAttributeSupported(EFS.ATTRIBUTE_HIDDEN)) {
			return;
		}
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFile file = project.getFile("target");
		ensureExistsInWorkspace(file, getRandomContents());

		// file
		assertTrue("1.0", !file.getResourceAttributes().isHidden());
		setHidden(file, true);
		assertTrue("1.2", file.getResourceAttributes().isHidden());
		setHidden(file, false);
		assertTrue("1.4", !file.getResourceAttributes().isHidden());

		// folder
		assertTrue("2.0", !project.getResourceAttributes().isHidden());
		setHidden(project, true);
		assertTrue("2.2", project.getResourceAttributes().isHidden());
		setHidden(project, false);
		assertTrue("2.4", !project.getResourceAttributes().isHidden());
	}

	public void testAttributeReadOnly() throws CoreException {
		// only activate this test on platforms that support it
		if (!isAttributeSupported(EFS.ATTRIBUTE_READ_ONLY)) {
			return;
		}
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFile file = project.getFile("target");
		ensureExistsInWorkspace(file, getRandomContents());

		// file
		assertTrue("1.0", !file.getResourceAttributes().isReadOnly());
		setReadOnly(file, true);
		assertTrue("1.2", file.getResourceAttributes().isReadOnly());
		setReadOnly(file, false);
		assertTrue("1.4", !file.getResourceAttributes().isReadOnly());

		// folder
		assertTrue("2.0", !project.getResourceAttributes().isReadOnly());
		setReadOnly(project, true);
		assertTrue("2.2", project.getResourceAttributes().isReadOnly());
		setReadOnly(project, false);
		assertTrue("2.4", !project.getResourceAttributes().isReadOnly());
	}

	/**
	 * Attributes of a closed project should be null.
	 */
	public void testClosedProject() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("Project");
		ensureExistsInWorkspace(project, true);
		project.close(getMonitor());
		assertNull("1.0", project.getResourceAttributes());
	}

	public void testNonExistingResource() throws CoreException {
		//asking for attributes of a non-existent resource should return null
		IProject project = getWorkspace().getRoot().getProject("testNonExistingResource");
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file");
		ensureDoesNotExistInWorkspace(project);
		assertNull("1.0", project.getResourceAttributes());
		assertNull("1.1", folder.getResourceAttributes());
		assertNull("1.2", file.getResourceAttributes());

		//now create the resources and ensure non-null result
		ensureExistsInWorkspace(project, true);
		ensureExistsInWorkspace(folder, true);
		ensureExistsInWorkspace(file, true);
		assertNotNull("2.0", project.getResourceAttributes());
		assertNotNull("2.1", folder.getResourceAttributes());
		assertNotNull("2.2", file.getResourceAttributes());
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
	public void _testRefreshExecutableOnFolder() throws CoreException {
		// only test on platforms that implement the executable bit
		if ((EFS.getLocalFileSystem().attributes() & EFS.ATTRIBUTE_EXECUTABLE) == 0) {
			return;
		}
		IProject project = getWorkspace().getRoot().getProject("testRefreshExecutableOnFolder");
		IFolder folder = project.getFolder("folder");
		IFile file = folder.getFile("file");
		ensureExistsInWorkspace(file, getRandomContents());

		// folder is executable initially and the file should exist
		assertTrue("1.0", project.getResourceAttributes().isExecutable());
		assertTrue("1.1", file.exists());

		setExecutable(folder, false);
		waitForRefresh();

		boolean wasExecutable = folder.getResourceAttributes().isExecutable();
		boolean fileExists = file.exists();

		// set the folder executable before asserting anything, otherwise cleanup will
		// fail
		setExecutable(folder, true);

		assertTrue("2.1", !wasExecutable);
		assertTrue("2.2", !fileExists);
	}

	public void testAttributeSymlink() throws Exception {
		// Only activate this test if testing of symbolic links is possible.
		if (!canCreateSymLinks()) {
			return;
		}
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFile link = project.getFile("link");
		ensureExistsInWorkspace(link, getRandomContents());

		// attempts to set the symbolic link attribute wont't affect
		// the resource and the underlying file
		assertTrue("1.0", !link.getResourceAttributes().isSymbolicLink());
		setSymlink(link, true);
		assertTrue("2.0", !link.getResourceAttributes().isSymbolicLink());
		setSymlink(link, false);
		assertTrue("3.0", !link.getResourceAttributes().isSymbolicLink());

		ensureDoesNotExistInWorkspace(link);

		// create the target file in the filesystem
		IFile target = project.getFile("target");
		ensureExistsInFileSystem(target);

		// create a link to the target file and add it to the workspace,
		// the resource in the workspace should have symbolic link attribute set
		createSymLink(project.getLocation().toFile(), "link", "target", false);
		ensureExistsInWorkspace(link, true);
		assertTrue("5.0", link.getResourceAttributes().isSymbolicLink());

		// attempts to clear the symbolic link attribute shouldn't affect
		// the resource and the underlying file
		setSymlink(link, false);
		assertTrue("3.0", link.getResourceAttributes().isSymbolicLink());

		// remove the underlying file and add it again as a local file,
		// the resource in the workspace should have the symbolic link attribute
		// cleared
		String s = link.getLocation().toOSString();

		link.getLocation().toFile().delete();
		new File(s).createNewFile();
		assertTrue("3.0", !link.getResourceAttributes().isSymbolicLink());
	}

	public void testAttributes() throws CoreException {
		int[] attributes = new int[] {EFS.ATTRIBUTE_GROUP_READ, EFS.ATTRIBUTE_GROUP_WRITE, EFS.ATTRIBUTE_GROUP_EXECUTE, EFS.ATTRIBUTE_OTHER_READ, EFS.ATTRIBUTE_OTHER_WRITE, EFS.ATTRIBUTE_OTHER_EXECUTE};

		IProject project = getWorkspace().getRoot().getProject(getUniqueString());
		IFile file = project.getFile(getUniqueString());
		ensureExistsInWorkspace(file, getRandomContents());

		for (int attribute : attributes) {
			// only activate this test on platforms that support it
			if (!isAttributeSupported(attribute)) {
				continue;
			}

			// file
			ResourceAttributes resAttr = file.getResourceAttributes();
			resAttr.set(attribute, true);
			file.setResourceAttributes(resAttr);
			assertTrue("1.0", file.getResourceAttributes().isSet(attribute));

			resAttr.set(attribute, false);
			file.setResourceAttributes(resAttr);
			assertFalse("2.0", file.getResourceAttributes().isSet(attribute));

			// folder
			resAttr = project.getResourceAttributes();
			resAttr.set(attribute, true);
			project.setResourceAttributes(resAttr);
			assertTrue("3.0", project.getResourceAttributes().isSet(attribute));

			resAttr.set(attribute, false);
			project.setResourceAttributes(resAttr);
			assertFalse("4.0", project.getResourceAttributes().isSet(attribute));
		}
	}
}
