/*******************************************************************************
 *  Copyright (c) 2000, 2022 IBM Corporation and others.
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
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertDoesNotExistInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomContentsStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createUniqueString;
import static org.eclipse.core.tests.resources.ResourceTestUtil.ensureOutOfSync;
import static org.eclipse.core.tests.resources.ResourceTestUtil.removeFromWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.waitForBuild;
import static org.eclipse.core.tests.resources.ResourceTestUtil.waitForEncodingRelatedJobs;
import static org.junit.Assert.assertThrows;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;

public class HiddenResourceTest extends ResourceTest {
	public void testRefreshLocal() throws Exception {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject(createUniqueString());
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subfile.txt");
		IResource[] resources = new IResource[] {project, folder, file, subFile};
		createInWorkspace(resources);
		waitForEncodingRelatedJobs(getName());

		ResourceDeltaVerifier listener = new ResourceDeltaVerifier();
		listener.addExpectedChange(subFile, IResourceDelta.CHANGED, IResourceDelta.CONTENT);
		addResourceChangeListener(listener);
		try {
			setHidden(folder, true, IResource.DEPTH_ZERO);
			ensureOutOfSync(subFile);
			project.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
			assertTrue(listener.getMessage(), listener.isDeltaValid());
		} finally {
			removeResourceChangeListener(listener);
		}
	}

	/**
	 * Resources which are marked as hidden resources should always be found.
	 */
	public void testFindMember() throws CoreException {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject(createUniqueString());
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subfile.txt");
		IResource[] resources = new IResource[] {project, folder, file, subFile};
		createInWorkspace(resources);

		// no hidden resources
		assertEquals("1.0", project, root.findMember(project.getFullPath()));
		assertEquals("1.1", folder, root.findMember(folder.getFullPath()));
		assertEquals("1.2", file, root.findMember(file.getFullPath()));
		assertEquals("1.3", subFile, root.findMember(subFile.getFullPath()));

		// the folder is hidden
		setHidden(folder, true, IResource.DEPTH_ZERO);
		assertEquals("2.1", project, root.findMember(project.getFullPath()));
		assertEquals("2.2", folder, root.findMember(folder.getFullPath()));
		assertEquals("2.3", file, root.findMember(file.getFullPath()));
		assertEquals("2.4", subFile, root.findMember(subFile.getFullPath()));

		// all are hidden
		setHidden(project, true, IResource.DEPTH_INFINITE);
		assertEquals("3.1", project, root.findMember(project.getFullPath()));
		assertEquals("3.2", folder, root.findMember(folder.getFullPath()));
		assertEquals("3.3", file, root.findMember(file.getFullPath()));
		assertEquals("3.4", subFile, root.findMember(subFile.getFullPath()));
	}

	/**
	 * Resources which are marked as hidden are not included in #members
	 * calls unless specifically included by calling #members(IContainer.INCLUDE_HIDDEN)
	 */
	public void testMembers() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject(createUniqueString());
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subfile.txt");
		IResource[] resources = new IResource[] {project, folder, file, subFile};
		IResource[] members = null;
		createInWorkspace(resources);

		// Initial values should be false.
		assertHidden(project, false, IResource.DEPTH_INFINITE);

		// Check the calls to #members
		members = project.members();

		// +1 for the project description file
		assertEquals("2.1", 4, members.length);
		members = folder.members();
		assertEquals("2.3", 1, members.length);

		// Set the values.
		setHidden(project, true, IResource.DEPTH_INFINITE);
		assertHidden(project, true, IResource.DEPTH_INFINITE);

		// Check the values
		assertHidden(project, true, IResource.DEPTH_INFINITE);

		// Check the calls to #members
		members = project.members();
		assertEquals("5.1", 0, members.length);
		members = folder.members();
		assertEquals("5.3", 0, members.length);

		// FIXME: add the tests for #members(int)

		// reset to false
		setHidden(project, false, IResource.DEPTH_INFINITE);
		assertHidden(project, false, IResource.DEPTH_INFINITE);

		// Check the calls to members(IResource.NONE);
		members = project.members(IResource.NONE);
		// +1 for the project description file
		assertEquals("7.1", 4, members.length);
		members = project.members(IContainer.INCLUDE_HIDDEN);
		// +1 for the project description file
		assertEquals("7.3", 4, members.length);
		members = folder.members();
		assertEquals("7.5", 1, members.length);

		// Set one of the children to be HIDDEN and try again
		setHidden(folder, true, IResource.DEPTH_ZERO);
		members = project.members();

		// +1 for project description, -1 for hidden folder
		assertEquals("8.2", 3, members.length);
		members = folder.members();
		assertEquals("8.4", 1, members.length);
		members = project.members(IResource.NONE);
		// +1 for project description, -1 for hidden folder
		assertEquals("8.6", 3, members.length);
		members = folder.members();
		assertEquals("8.8", 1, members.length);
		members = project.members(IContainer.INCLUDE_HIDDEN);
		// +1 for project description
		assertEquals("8.10", 4, members.length);
		members = folder.members();
		assertEquals("8.12", 1, members.length);

		// Set all the resources to be hidden
		setHidden(project, true, IResource.DEPTH_INFINITE);
		assertHidden(project, true, IResource.DEPTH_INFINITE);
		members = project.members(IResource.NONE);
		assertEquals("9.3", 0, members.length);
		members = folder.members(IResource.NONE);
		assertEquals("9.5", 0, members.length);
		members = project.members(IContainer.INCLUDE_HIDDEN);
		// +1 for project description
		assertEquals("9.7", 4, members.length);
		members = folder.members(IContainer.INCLUDE_HIDDEN);
		assertEquals("9.9", 1, members.length);
	}

	/**
	 * Resources which are marked as hidden resources should not be visited by
	 * resource visitors.
	 */
	public void testAccept() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject(createUniqueString());
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subfile.txt");
		IFolder settings = project.getFolder(".settings");
		IFile prefs = settings.getFile("org.eclipse.core.resources.prefs");
		IResource[] resources = new IResource[] { project, folder, file, subFile, settings, prefs };
		createInWorkspace(resources);
		IResource description = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);

		// default case, no hidden resources
		ResourceVisitorVerifier visitor = new ResourceVisitorVerifier();
		visitor.addExpected(resources);
		visitor.addExpected(description);
		project.accept(visitor);
		assertTrue(visitor.getMessage(), visitor.isValid());

		visitor.reset();
		visitor.addExpected(resources);
		visitor.addExpected(description);
		project.accept(visitor, IResource.DEPTH_INFINITE, IResource.NONE);
		assertTrue(visitor.getMessage(), visitor.isValid());

		visitor.reset();
		visitor.addExpected(resources);
		visitor.addExpected(description);
		project.accept(visitor, IResource.DEPTH_INFINITE, IContainer.INCLUDE_HIDDEN);

		assertTrue(visitor.getMessage(), visitor.isValid());

		// set the folder to be hidden. It and its children should
		// be ignored by the visitor
		setHidden(folder, true, IResource.DEPTH_ZERO);
		visitor.reset();
		visitor.addExpected(project);
		visitor.addExpected(file);
		visitor.addExpected(description);
		visitor.addExpected(settings);
		visitor.addExpected(prefs);
		project.accept(visitor);
		assertTrue(visitor.getMessage(), visitor.isValid());

		visitor.reset();
		visitor.addExpected(project);
		visitor.addExpected(file);
		visitor.addExpected(description);
		visitor.addExpected(settings);
		visitor.addExpected(prefs);
		project.accept(visitor, IResource.DEPTH_INFINITE, IResource.NONE);
		assertTrue(visitor.getMessage(), visitor.isValid());
		// should see all resources if we include the flag
		visitor.reset();
		visitor.addExpected(resources);
		visitor.addExpected(description);
		project.accept(visitor, IResource.DEPTH_INFINITE, IContainer.INCLUDE_HIDDEN);
		assertTrue(visitor.getMessage(), visitor.isValid());
		// should NOT visit the folder and its members if we call accept on it directly
		visitor.reset();
		folder.accept(visitor);
		assertTrue(visitor.getMessage(), visitor.isValid());

		visitor.reset();
		folder.accept(visitor, IResource.DEPTH_INFINITE, IResource.NONE);
		assertTrue(visitor.getMessage(), visitor.isValid());

		visitor.reset();
		visitor.addExpected(folder);
		visitor.addExpected(subFile);
		folder.accept(visitor, IResource.DEPTH_INFINITE, IContainer.INCLUDE_HIDDEN);
		assertTrue(visitor.getMessage(), visitor.isValid());
	}

	public void testCopy() throws CoreException {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject(createUniqueString());
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subfile.txt");
		IResource[] resources = new IResource[] {project, folder, file, subFile};
		createInWorkspace(resources);

		// handles to the destination resources
		IProject destProject = root.getProject("MyOtherProject");
		IFolder destFolder = destProject.getFolder(folder.getName());
		IFile destFile = destProject.getFile(file.getName());
		IFile destSubFile = destFolder.getFile(subFile.getName());
		IResource[] destResources = new IResource[] {destProject, destFolder, destFile, destSubFile};
		removeFromWorkspace(destResources);

		// set a folder to be hidden
		setHidden(folder, true, IResource.DEPTH_ZERO);
		// copy the project
		int flags = IResource.FORCE;
		project.copy(destProject.getFullPath(), flags, createTestMonitor());
		assertExistsInWorkspace(resources);
		assertExistsInWorkspace(destResources);

		// do it again and but just copy the folder
		removeFromWorkspace(destResources);
		createInWorkspace(resources);
		createInWorkspace(destProject);
		setHidden(folder, true, IResource.DEPTH_ZERO);
		folder.copy(destFolder.getFullPath(), flags, createTestMonitor());
		assertExistsInWorkspace(new IResource[] { folder, subFile });
		assertExistsInWorkspace(new IResource[] { destFolder, destSubFile });

		// set all the resources to be hidden
		// copy the project
		removeFromWorkspace(destResources);
		createInWorkspace(resources);
		setHidden(project, true, IResource.DEPTH_INFINITE);
		project.copy(destProject.getFullPath(), flags, createTestMonitor());
		assertExistsInWorkspace(resources);
		assertExistsInWorkspace(destResources);

		// do it again but only copy the folder
		removeFromWorkspace(destResources);
		createInWorkspace(resources);
		createInWorkspace(destProject);
		setHidden(project, true, IResource.DEPTH_INFINITE);
		folder.copy(destFolder.getFullPath(), flags, createTestMonitor());
		assertExistsInWorkspace(new IResource[] { folder, subFile });
		assertExistsInWorkspace(new IResource[] { destFolder, destSubFile });
	}

	public void testMove() throws CoreException {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject(createUniqueString());
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subfile.txt");
		IResource[] resources = new IResource[] {project, folder, file, subFile};
		createInWorkspace(resources);

		// handles to the destination resources
		IProject destProject = root.getProject("MyOtherProject");
		IFolder destFolder = destProject.getFolder(folder.getName());
		IFile destFile = destProject.getFile(file.getName());
		IFile destSubFile = destFolder.getFile(subFile.getName());
		IResource[] destResources = new IResource[] {destProject, destFolder, destFile, destSubFile};
		removeFromWorkspace(destResources);

		// set a folder to be hidden
		setHidden(folder, true, IResource.DEPTH_ZERO);
		// move the project
		int flags = IResource.FORCE;
		project.move(destProject.getFullPath(), flags, createTestMonitor());
		assertDoesNotExistInWorkspace(resources);
		assertExistsInWorkspace(destResources);

		// do it again and but just move the folder
		removeFromWorkspace(destResources);
		createInWorkspace(resources);
		createInWorkspace(destProject);
		setHidden(folder, true, IResource.DEPTH_ZERO);
		folder.move(destFolder.getFullPath(), flags, createTestMonitor());
		assertDoesNotExistInWorkspace(new IResource[] { folder, subFile });
		assertExistsInWorkspace(new IResource[] { destFolder, destSubFile });

		// set all the resources to be hidden
		// move the project
		removeFromWorkspace(destResources);
		createInWorkspace(resources);
		setHidden(project, true, IResource.DEPTH_INFINITE);
		project.move(destProject.getFullPath(), flags, createTestMonitor());
		assertDoesNotExistInWorkspace(resources);
		assertExistsInWorkspace(destResources);

		// do it again but only move the folder
		removeFromWorkspace(destResources);
		createInWorkspace(resources);
		createInWorkspace(destProject);
		setHidden(project, true, IResource.DEPTH_INFINITE);
		folder.move(destFolder.getFullPath(), flags, createTestMonitor());
		assertDoesNotExistInWorkspace(new IResource[] { folder, subFile });
		assertExistsInWorkspace(new IResource[] { destFolder, destSubFile });
	}

	public void testDelete() throws CoreException {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject(createUniqueString());
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subfile.txt");
		IResource[] resources = new IResource[] {project, folder, file, subFile};
		createInWorkspace(resources);

		// default behavior with no hidden
		int flags = IResource.ALWAYS_DELETE_PROJECT_CONTENT | IResource.FORCE;
		// delete the project
		project.delete(flags, createTestMonitor());
		assertDoesNotExistInWorkspace(resources);
		// delete a file
		createInWorkspace(resources);
		file.delete(flags, createTestMonitor());
		assertDoesNotExistInWorkspace(file);
		assertExistsInWorkspace(new IResource[] { project, folder, subFile });
		// delete a folder
		createInWorkspace(resources);
		folder.delete(flags, createTestMonitor());
		assertDoesNotExistInWorkspace(new IResource[] { folder, subFile });
		assertExistsInWorkspace(new IResource[] { project, file });

		// set one child to be hidden
		createInWorkspace(resources);
		setHidden(folder, true, IResource.DEPTH_ZERO);
		// delete the project
		project.delete(flags, createTestMonitor());
		assertDoesNotExistInWorkspace(resources);
		// delete a folder
		createInWorkspace(resources);
		setHidden(folder, true, IResource.DEPTH_ZERO);
		folder.delete(flags, createTestMonitor());
		assertDoesNotExistInWorkspace(new IResource[] { folder, subFile });
		assertExistsInWorkspace(new IResource[] { project, file });

		// set all resources to be hidden
		createInWorkspace(resources);
		setHidden(project, true, IResource.DEPTH_INFINITE);
		// delete the project
		project.delete(flags, createTestMonitor());
		assertDoesNotExistInWorkspace(resources);
		// delete a file
		createInWorkspace(resources);
		setHidden(project, true, IResource.DEPTH_INFINITE);
		file.delete(flags, createTestMonitor());
		assertDoesNotExistInWorkspace(file);
		assertExistsInWorkspace(new IResource[] { project, folder, subFile });
		// delete a folder
		createInWorkspace(resources);
		setHidden(project, true, IResource.DEPTH_INFINITE);
		folder.delete(flags, createTestMonitor());
		assertDoesNotExistInWorkspace(new IResource[] { folder, subFile });
		assertExistsInWorkspace(new IResource[] { project, file });
	}

	public void testDeltas() throws CoreException {
		IWorkspaceRoot root = getWorkspace().getRoot();
		final IProject project = root.getProject(createUniqueString());
		final IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subfile.txt");
		IFile description = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		IFolder settings = project.getFolder(".settings");
		IFile prefs = settings.getFile("org.eclipse.core.resources.prefs");
		final IResource[] resources = new IResource[] { project, folder, file, subFile, settings, prefs };
		final ResourceDeltaVerifier listener = new ResourceDeltaVerifier();
		try {
			IWorkspaceRunnable body = monitor -> createInWorkspace(resources);
			listener.addExpectedChange(resources, IResourceDelta.ADDED, IResource.NONE);
			listener.addExpectedChange(project, IResourceDelta.ADDED, IResourceDelta.OPEN);
			listener.addExpectedChange(description, IResourceDelta.ADDED, IResource.NONE);
			addResourceChangeListener(listener);
			getWorkspace().run(body, createTestMonitor());
			waitForBuild();
			waitForEncodingRelatedJobs(getName());
			// FIXME sometimes fails with "Verifier has not yet been given a resource
			// delta":
			assertTrue(listener.getMessage(), listener.isDeltaValid());
			removeFromWorkspace(resources);
		} finally {
			removeResourceChangeListener(listener);
		}

		try {
			IWorkspaceRunnable body = monitor -> {
				createInWorkspace(resources);
				setHidden(folder, true, IResource.DEPTH_ZERO);
			};
			listener.reset();
			listener.addExpectedChange(resources, IResourceDelta.ADDED, IResource.NONE);
			listener.addExpectedChange(project, IResourceDelta.ADDED, IResourceDelta.OPEN);
			listener.addExpectedChange(description, IResourceDelta.ADDED, IResource.NONE);
			addResourceChangeListener(listener);
			getWorkspace().run(body, createTestMonitor());
			// FIXME sometimes fails with "Verifier has not yet been given a resource
			// delta":
			assertTrue(listener.getMessage(), listener.isDeltaValid());
			removeFromWorkspace(resources);
		} finally {
			removeResourceChangeListener(listener);
		}

		try {
			IWorkspaceRunnable body = monitor -> {
				createInWorkspace(resources);
				setHidden(project, true, IResource.DEPTH_INFINITE);
			};
			listener.reset();
			listener.addExpectedChange(resources, IResourceDelta.ADDED, IResource.NONE);
			listener.addExpectedChange(project, IResourceDelta.ADDED, IResourceDelta.OPEN);
			listener.addExpectedChange(description, IResourceDelta.ADDED, IResource.NONE);
			addResourceChangeListener(listener);
			getWorkspace().run(body, createTestMonitor());
			// FIXME sometimes fails with "Verifier has not yet been given a resource
			// delta":
			assertTrue("3.1." + listener.getMessage(), listener.isDeltaValid());
			removeFromWorkspace(resources);
		} finally {
			removeResourceChangeListener(listener);
		}
	}

	private void removeResourceChangeListener(final ResourceDeltaVerifier listener) throws CoreException {
		// removeResourceChangeListener need to happen in an atomic workspace operation
		// otherwise it would be removed while auto refresh is running
		// and might even get called in another thread after removing in this thread
		listener.shutDown();
		getWorkspace().run(p -> getWorkspace().removeResourceChangeListener(listener), null);
	}

	private void addResourceChangeListener(ResourceDeltaVerifier listener) throws CoreException {
		// addResourceChangeListener need to happen in an atomic workspace operation
		// otherwise it would be added while auto refresh is running
		// and might get called in another thread before explicit refresh in this thread
		getWorkspace().run(p -> {
			getWorkspace().addResourceChangeListener(listener);
			listener.active();
		}, null);
	}


	/**
	 * Resources which are marked as hidden resources return TRUE
	 * in all calls to #exists.
	 */
	public void testExists() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject(createUniqueString());
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subfile.txt");
		IResource[] resources = new IResource[] {project, folder, file, subFile};
		createInWorkspace(resources);

		// check to see if all the resources exist in the workspace tree.
		assertExistsInWorkspace(resources);

		// set a folder to be hidden
		setHidden(folder, true, IResource.DEPTH_ZERO);
		assertHidden(folder, true, IResource.DEPTH_ZERO);
		assertExistsInWorkspace(resources);

		// set all resources to be hidden
		setHidden(project, true, IResource.DEPTH_INFINITE);
		assertHidden(project, true, IResource.DEPTH_INFINITE);
		assertExistsInWorkspace(resources);
	}

	/**
	 * Test the set and get methods for hidden resources.
	 */
	public void testSetGet() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject(createUniqueString());
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subfile.txt");
		IResource[] resources = new IResource[] {project, folder, file, subFile};

		// trying to set the value on non-existing resources will fail
		for (IResource resource : resources) {
			assertThrows(CoreException.class, () -> resource.setHidden(true));
		}

		// create the resources
		createInWorkspace(resources);

		// initial values should be false
		for (IResource resource2 : resources) {
			IResource resource = resource2;
			assertTrue("1.0: " + resource.getFullPath(), !resource.isHidden());
		}

		// now set the values
		for (IResource resource : resources) {
			resource.setHidden(true);
		}

		// the values should be true for projects only
		for (IResource resource2 : resources) {
			IResource resource = resource2;
			switch (resource.getType()) {
				case IResource.PROJECT :
				case IResource.FOLDER :
				case IResource.FILE :
					assertTrue("3.0: " + resource.getFullPath(), resource.isHidden());
					break;
				case IResource.ROOT :
					assertFalse("3.1: " + resource.getFullPath(), resource.isHidden());
					break;
			}
		}

		// clear the values
		for (IResource resource : resources) {
			resource.setHidden(false);
		}

		// values should be false again
		for (IResource resource2 : resources) {
			IResource resource = resource2;
			assertFalse("5.0: " + resource.getFullPath(), resource.isHidden());
		}
	}

	/**
	 * Tests whether {@link IFile#create(java.io.InputStream, int, IProgressMonitor)},
	 * {@link IFolder#create(int, boolean, IProgressMonitor)}
	 * and {@link IProject#create(IProjectDescription, int, IProgressMonitor)}
	 * handles {@link IResource#HIDDEN} flag properly.
	 */
	public void testCreateHiddenResources() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject(createUniqueString());
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");

		createInWorkspace(project);
		folder.create(IResource.HIDDEN, true, createTestMonitor());
		file.create(createRandomContentsStream(), IResource.HIDDEN, createTestMonitor());

		assertHidden(project, false, IResource.DEPTH_ZERO);
		assertHidden(folder, true, IResource.DEPTH_ZERO);
		assertHidden(file, true, IResource.DEPTH_ZERO);

		IProject project2 = getWorkspace().getRoot().getProject(createUniqueString());

		project2.create(null, IResource.HIDDEN, createTestMonitor());
		project2.open(createTestMonitor());

		assertHidden(project2, true, IResource.DEPTH_ZERO);
	}

	protected void assertHidden(IResource root, final boolean value, int depth)
			throws CoreException {
		IResourceVisitor visitor = resource -> {
			boolean expected = false;
			if (resource.getType() == IResource.PROJECT || resource.getType() == IResource.FILE || resource.getType() == IResource.FOLDER) {
				expected = value;
			}
			assertEquals(resource.getFullPath() + "", expected, resource.isHidden());
			return true;
		};
		root.accept(visitor, depth, IContainer.INCLUDE_HIDDEN);
	}

	protected void setHidden(IResource root, final boolean value, int depth)
			throws CoreException {
		IResourceVisitor visitor = resource -> {
			resource.setHidden(value);
			return true;
		};
		root.accept(visitor, depth, IContainer.INCLUDE_HIDDEN);
	}
}
