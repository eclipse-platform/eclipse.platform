/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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

import static org.junit.Assert.assertThrows;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;

public class TeamPrivateMemberTest extends ResourceTest {
	public void testRefreshLocal() throws CoreException {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject("MyProject");
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subfile.txt");
		IResource[] resources = { project, folder, file, subFile };
		ensureExistsInWorkspace(resources, true);

		ResourceDeltaVerifier listener = new ResourceDeltaVerifier();
		listener.addExpectedChange(subFile, IResourceDelta.CHANGED, IResourceDelta.CONTENT);
		getWorkspace().addResourceChangeListener(listener);
		try {
			setTeamPrivateMember("3.0", folder, true, IResource.DEPTH_ZERO);
			ensureOutOfSync(subFile);
			project.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
			assertTrue(listener.getMessage(), listener.isDeltaValid());
		} finally {
			getWorkspace().removeResourceChangeListener(listener);
		}
	}

	/**
	 * Resources which are marked as team private members should always be found.
	 */
	public void testFindMember() {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject("MyProject");
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subfile.txt");
		IResource[] resources = { project, folder, file, subFile };
		ensureExistsInWorkspace(resources, true);

		// no team private members
		assertEquals("1.0", project, root.findMember(project.getFullPath()));
		assertEquals("1.1", folder, root.findMember(folder.getFullPath()));
		assertEquals("1.2", file, root.findMember(file.getFullPath()));
		assertEquals("1.3", subFile, root.findMember(subFile.getFullPath()));

		// the folder is team private
		setTeamPrivateMember("2.0", folder, true, IResource.DEPTH_ZERO);
		assertEquals("2.1", project, root.findMember(project.getFullPath()));
		assertEquals("2.2", folder, root.findMember(folder.getFullPath()));
		assertEquals("2.3", file, root.findMember(file.getFullPath()));
		assertEquals("2.4", subFile, root.findMember(subFile.getFullPath()));

		// all are team private
		setTeamPrivateMember("3.0", project, true, IResource.DEPTH_INFINITE);
		assertEquals("3.1", project, root.findMember(project.getFullPath()));
		assertEquals("3.2", folder, root.findMember(folder.getFullPath()));
		assertEquals("3.3", file, root.findMember(file.getFullPath()));
		assertEquals("3.4", subFile, root.findMember(subFile.getFullPath()));
	}

	/**
	 * Resources which are marked as team private members are not included in #members
	 * calls unless specifically included by calling #members(IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS)
	 */
	public void testMembers() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subfile.txt");
		IFolder settings = project.getFolder(".settings");
		IFile prefs = settings.getFile("org.eclipse.core.resources.prefs");
		IResource[] resources = new IResource[] { project, folder, file, subFile, settings, prefs };
		IResource[] members = null;
		ensureExistsInWorkspace(resources, true);

		// Initial values should be false.
		assertTeamPrivateMember("1.0", project, false, IResource.DEPTH_INFINITE);

		// Check the calls to #members
		members = project.members();
		// +1 for the project description file
		assertEquals("2.1", 4, members.length);

		members = folder.members();
		assertEquals("2.3", 1, members.length);

		// Set the values.
		setTeamPrivateMember("3.0", project, true, IResource.DEPTH_INFINITE);
		assertTeamPrivateMember("3.1", project, true, IResource.DEPTH_INFINITE);

		// Check the values
		assertTeamPrivateMember("4.0", project, true, IResource.DEPTH_INFINITE);

		// Check the calls to #members
		members = project.members();
		assertEquals("5.1", 0, members.length);
		members = folder.members();
		assertEquals("5.3", 0, members.length);

		// FIXME: add the tests for #members(int)

		// reset to false
		setTeamPrivateMember("6.0", project, false, IResource.DEPTH_INFINITE);
		assertTeamPrivateMember("6.1", project, false, IResource.DEPTH_INFINITE);

		// Check the calls to members(IResource.NONE);
		members = project.members(IResource.NONE);
		// +1 for the project description file
		assertEquals("7.1", 4, members.length);
		members = project.members(IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);
		// +1 for the project description file
		assertEquals("7.3", 4, members.length);
		members = folder.members();
		assertEquals("7.5", 1, members.length);

		// Set one of the children to be TEAM_PRIVATE and try again
		setTeamPrivateMember("8.0", folder, true, IResource.DEPTH_ZERO);
		members = project.members();
		// +1 for project description, -1 for team private folder
		assertEquals("8.2", 3, members.length);
		members = folder.members();
		assertEquals("8.4", 1, members.length);
		members = project.members(IResource.NONE);
		// +1 for project description, -1 for team private folder
		assertEquals("8.6", 3, members.length);
		members = folder.members();
		assertEquals("8.8", 1, members.length);
		members = project.members(IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);
		// +1 for project description
		assertEquals("8.10", 4, members.length);
		members = folder.members();
		assertEquals("8.12", 1, members.length);

		// Set all the resources to be team private
		setTeamPrivateMember("9.0", project, true, IResource.DEPTH_INFINITE);
		assertTeamPrivateMember("9.1", project, true, IResource.DEPTH_INFINITE);
		members = project.members(IResource.NONE);
		assertEquals("9.3", 0, members.length);
		members = folder.members(IResource.NONE);
		assertEquals("9.5", 0, members.length);
		members = project.members(IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);
		// +1 for project description
		assertEquals("9.7", 4, members.length);
		members = folder.members(IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);
		assertEquals("9.9", 1, members.length);
	}

	/**
	 * Resources which are marked as team private members should not be visited by
	 * resource visitors.
	 */
	public void testAccept() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subfile.txt");
		IFolder settings = project.getFolder(".settings");
		IFile prefs = settings.getFile("org.eclipse.core.resources.prefs");
		IResource[] resources = { project, folder, file, subFile, settings, prefs };
		ensureExistsInWorkspace(resources, true);
		IResource description = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);

		// default case, no team private members
		ResourceVisitorVerifier visitor = new ResourceVisitorVerifier();
		visitor.addExpected(resources);
		visitor.addExpected(description);
		project.accept(visitor);
		assertTrue("1.1." + visitor.getMessage(), visitor.isValid());

		visitor.reset();
		visitor.addExpected(resources);
		visitor.addExpected(description);
		project.accept(visitor, IResource.DEPTH_INFINITE, IResource.NONE);
		assertTrue("1.3." + visitor.getMessage(), visitor.isValid());

		visitor.reset();
		visitor.addExpected(resources);
		visitor.addExpected(description);
		project.accept(visitor, IResource.DEPTH_INFINITE, IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);
		assertTrue("1.5." + visitor.getMessage(), visitor.isValid());

		// set the folder to be team private. It and its children should
		// be ignored by the visitor
		setTeamPrivateMember("2.0", folder, true, IResource.DEPTH_ZERO);
		visitor.reset();
		visitor.addExpected(project);
		visitor.addExpected(file);
		visitor.addExpected(description);
		visitor.addExpected(settings);
		visitor.addExpected(prefs);
		project.accept(visitor);
		assertTrue("2.2." + visitor.getMessage(), visitor.isValid());

		visitor.reset();
		visitor.addExpected(project);
		visitor.addExpected(file);
		visitor.addExpected(description);
		visitor.addExpected(settings);
		visitor.addExpected(prefs);
		project.accept(visitor, IResource.DEPTH_INFINITE, IResource.NONE);
		assertTrue("2.4." + visitor.getMessage(), visitor.isValid());
		// should see all resources if we include the flag
		visitor.reset();
		visitor.addExpected(resources);
		visitor.addExpected(description);
		project.accept(visitor, IResource.DEPTH_INFINITE, IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);
		assertTrue("2.6." + visitor.getMessage(), visitor.isValid());
		// should NOT visit the folder and its members if we call accept on it directly
		visitor.reset();
		folder.accept(visitor);
		assertTrue("2.8." + visitor.getMessage(), visitor.isValid());

		visitor.reset();
		folder.accept(visitor, IResource.DEPTH_INFINITE, IResource.NONE);
		assertTrue("2.10." + visitor.getMessage(), visitor.isValid());

		visitor.reset();
		visitor.addExpected(folder);
		visitor.addExpected(subFile);
		folder.accept(visitor, IResource.DEPTH_INFINITE, IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);
		assertTrue("2.11." + visitor.getMessage(), visitor.isValid());

		// now set all file/folder resources to be team private.
		setTeamPrivateMember("3.0", project, true, IResource.DEPTH_INFINITE);
		assertTeamPrivateMember("3.1", project, true, IResource.DEPTH_INFINITE);
		visitor.reset();
		// projects are never team private
		visitor.addExpected(project);
		project.accept(visitor);
		assertTrue("3.3." + visitor.getMessage(), visitor.isValid());

		visitor.reset();
		visitor.addExpected(project);
		project.accept(visitor, IResource.DEPTH_INFINITE, IResource.NONE);
		assertTrue("3.5." + visitor.getMessage(), visitor.isValid());
		// should see all resources if we include the flag
		visitor.reset();
		visitor.addExpected(resources);
		visitor.addExpected(description);
		project.accept(visitor, IResource.DEPTH_INFINITE, IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);
		assertTrue("3.7." + visitor.getMessage(), visitor.isValid());
	}

	public void testCopy() throws CoreException {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject("MyProject");
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subfile.txt");
		IResource[] resources = { project, folder, file, subFile };
		ensureExistsInWorkspace(resources, true);

		// handles to the destination resources
		IProject destProject = root.getProject("MyOtherProject");
		IFolder destFolder = destProject.getFolder(folder.getName());
		IFile destFile = destProject.getFile(file.getName());
		IFile destSubFile = destFolder.getFile(subFile.getName());
		IResource[] destResources = { destProject, destFolder, destFile, destSubFile };
		ensureDoesNotExistInWorkspace(destResources);

		// set a folder to be team private
		setTeamPrivateMember("1.0", folder, true, IResource.DEPTH_ZERO);
		// copy the project
		int flags = IResource.FORCE;
		project.copy(destProject.getFullPath(), flags, getMonitor());
		assertExistsInWorkspace("1.2", resources);
		assertExistsInWorkspace("1.3", destResources);

		// Do it again and but just copy the folder
		ensureDoesNotExistInWorkspace(destResources);
		ensureExistsInWorkspace(resources, true);
		ensureExistsInWorkspace(destProject, true);
		setTeamPrivateMember("2.0", folder, true, IResource.DEPTH_ZERO);
		folder.copy(destFolder.getFullPath(), flags, getMonitor());
		assertExistsInWorkspace("2.2", new IResource[] {folder, subFile});
		assertExistsInWorkspace("2.3", new IResource[] {destFolder, destSubFile});

		// set all the resources to be team private
		// copy the project
		ensureDoesNotExistInWorkspace(destResources);
		ensureExistsInWorkspace(resources, true);
		setTeamPrivateMember("3.0", project, true, IResource.DEPTH_INFINITE);
		project.copy(destProject.getFullPath(), flags, getMonitor());
		assertExistsInWorkspace("3.2", resources);
		assertExistsInWorkspace("3.3", destResources);

		// do it again but only copy the folder
		ensureDoesNotExistInWorkspace(destResources);
		ensureExistsInWorkspace(resources, true);
		ensureExistsInWorkspace(destProject, true);
		setTeamPrivateMember("4.0", project, true, IResource.DEPTH_INFINITE);
		folder.copy(destFolder.getFullPath(), flags, getMonitor());
		assertExistsInWorkspace("4.2", new IResource[] {folder, subFile});
		assertExistsInWorkspace("4.3", new IResource[] {destFolder, destSubFile});
	}

	public void testMove() throws CoreException {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject("MyProject");
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subfile.txt");
		IResource[] resources = { project, folder, file, subFile };
		ensureExistsInWorkspace(resources, true);

		// handles to the destination resources
		IProject destProject = root.getProject("MyOtherProject");
		IFolder destFolder = destProject.getFolder(folder.getName());
		IFile destFile = destProject.getFile(file.getName());
		IFile destSubFile = destFolder.getFile(subFile.getName());
		IResource[] destResources = { destProject, destFolder, destFile, destSubFile };
		ensureDoesNotExistInWorkspace(destResources);

		// set a folder to be team private
		setTeamPrivateMember("1.0", folder, true, IResource.DEPTH_ZERO);
		// move the project
		int flags = IResource.FORCE;
		project.move(destProject.getFullPath(), flags, getMonitor());
		assertDoesNotExistInWorkspace("1.2", resources);
		assertExistsInWorkspace("1.3", destResources);

		// Do it again and but just move the folder
		ensureDoesNotExistInWorkspace(destResources);
		ensureExistsInWorkspace(resources, true);
		ensureExistsInWorkspace(destProject, true);
		setTeamPrivateMember("2.0", folder, true, IResource.DEPTH_ZERO);
		folder.move(destFolder.getFullPath(), flags, getMonitor());
		assertDoesNotExistInWorkspace("2.2", new IResource[] {folder, subFile});
		assertExistsInWorkspace("2.3", new IResource[] {destFolder, destSubFile});

		// set all the resources to be team private
		// move the project
		ensureDoesNotExistInWorkspace(destResources);
		ensureExistsInWorkspace(resources, true);
		setTeamPrivateMember("3.0", project, true, IResource.DEPTH_INFINITE);
		project.move(destProject.getFullPath(), flags, getMonitor());
		assertDoesNotExistInWorkspace("3.2", resources);
		assertExistsInWorkspace("3.3", destResources);

		// do it again but only move the folder
		ensureDoesNotExistInWorkspace(destResources);
		ensureExistsInWorkspace(resources, true);
		ensureExistsInWorkspace(destProject, true);
		setTeamPrivateMember("4.0", project, true, IResource.DEPTH_INFINITE);
		folder.move(destFolder.getFullPath(), flags, getMonitor());
		assertDoesNotExistInWorkspace("4.2", new IResource[] {folder, subFile});
		assertExistsInWorkspace("4.3", new IResource[] {destFolder, destSubFile});
	}

	public void testDelete() throws CoreException {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject("MyProject");
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subfile.txt");
		IResource[] resources = new IResource[] {project, folder, file, subFile};
		ensureExistsInWorkspace(resources, true);

		// default behaviour with no team private members
		int flags = IResource.ALWAYS_DELETE_PROJECT_CONTENT | IResource.FORCE;
		// delete the project
		project.delete(flags, getMonitor());
		assertDoesNotExistInWorkspace("1.1", resources);
		// delete a file
		ensureExistsInWorkspace(resources, true);
		file.delete(flags, getMonitor());
		assertDoesNotExistInWorkspace("1.3", file);
		assertExistsInWorkspace("1.4", new IResource[] {project, folder, subFile});
		// delete a folder
		ensureExistsInWorkspace(resources, true);
		folder.delete(flags, getMonitor());
		assertDoesNotExistInWorkspace("1.6", new IResource[] {folder, subFile});
		assertExistsInWorkspace("1.7", new IResource[] {project, file});

		// set one child to be team private
		ensureExistsInWorkspace(resources, true);
		setTeamPrivateMember("2.0", folder, true, IResource.DEPTH_ZERO);
		// delete the project
		project.delete(flags, getMonitor());
		assertDoesNotExistInWorkspace("2.2", resources);
		// delete a folder
		ensureExistsInWorkspace(resources, true);
		setTeamPrivateMember("2.3", folder, true, IResource.DEPTH_ZERO);
		folder.delete(flags, getMonitor());
		assertDoesNotExistInWorkspace("2.5", new IResource[] {folder, subFile});
		assertExistsInWorkspace("2.6", new IResource[] {project, file});

		// set all resources to be team private
		ensureExistsInWorkspace(resources, true);
		setTeamPrivateMember("3.0", project, true, IResource.DEPTH_INFINITE);
		// delete the project
		project.delete(flags, getMonitor());
		assertDoesNotExistInWorkspace("3.2", resources);
		// delete a file
		ensureExistsInWorkspace(resources, true);
		setTeamPrivateMember("3.3", project, true, IResource.DEPTH_INFINITE);
		file.delete(flags, getMonitor());
		assertDoesNotExistInWorkspace("3.5", file);
		assertExistsInWorkspace("3.6", new IResource[] {project, folder, subFile});
		// delete a folder
		ensureExistsInWorkspace(resources, true);
		setTeamPrivateMember("3.7", project, true, IResource.DEPTH_INFINITE);
		folder.delete(flags, getMonitor());
		assertDoesNotExistInWorkspace("3.9", new IResource[] {folder, subFile});
		assertExistsInWorkspace("3.10", new IResource[] {project, file});
	}

	public void testDeltas() throws CoreException {
		IWorkspaceRoot root = getWorkspace().getRoot();
		final IProject project = root.getProject("MyProject");
		final IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subfile.txt");
		IFolder settings = project.getFolder(".settings");
		IFile prefs = settings.getFile("org.eclipse.core.resources.prefs");
		IFile description = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		IResource[] resources = { project, folder, file, subFile, settings, prefs };

		final ResourceDeltaVerifier listener = new ResourceDeltaVerifier();
		getWorkspace().addResourceChangeListener(listener);
		try {
			IWorkspaceRunnable body = monitor -> ensureExistsInWorkspace(resources, true);
			listener.addExpectedChange(resources, IResourceDelta.ADDED, IResource.NONE);
			listener.addExpectedChange(project, IResourceDelta.ADDED, IResourceDelta.OPEN);
			listener.addExpectedChange(description, IResourceDelta.ADDED, IResource.NONE);
			getWorkspace().run(body, getMonitor());
			waitForBuild();
			assertTrue("1.0." + listener.getMessage(), listener.isDeltaValid());
			ensureDoesNotExistInWorkspace(resources);
		} finally {
			getWorkspace().removeResourceChangeListener(listener);
		}

		// set the folder to be team private and do the same test
		getWorkspace().addResourceChangeListener(listener);
		try {
			IWorkspaceRunnable body = monitor -> {
				ensureExistsInWorkspace(resources, true);
				setTeamPrivateMember("2.0", folder, true, IResource.DEPTH_ZERO);
			};
			listener.reset();
			listener.addExpectedChange(resources, IResourceDelta.ADDED, IResource.NONE);
			listener.addExpectedChange(project, IResourceDelta.ADDED, IResourceDelta.OPEN);
			listener.addExpectedChange(description, IResourceDelta.ADDED, IResource.NONE);
			getWorkspace().run(body, getMonitor());
			assertTrue("2.1." + listener.getMessage(), listener.isDeltaValid());
			ensureDoesNotExistInWorkspace(resources);
		} finally {
			getWorkspace().removeResourceChangeListener(listener);
		}

		// set all resources to be team private and do the same test
		getWorkspace().addResourceChangeListener(listener);
		try {
			IWorkspaceRunnable body = monitor -> {
				ensureExistsInWorkspace(resources, true);
				setTeamPrivateMember("3.0", project, true, IResource.DEPTH_INFINITE);
			};
			listener.reset();
			listener.addExpectedChange(resources, IResourceDelta.ADDED, IResource.NONE);
			listener.addExpectedChange(project, IResourceDelta.ADDED, IResourceDelta.OPEN);
			listener.addExpectedChange(description, IResourceDelta.ADDED, IResource.NONE);
			getWorkspace().run(body, getMonitor());
			assertTrue("3.1." + listener.getMessage(), listener.isDeltaValid());
			ensureDoesNotExistInWorkspace(resources);
		} finally {
			getWorkspace().removeResourceChangeListener(listener);
		}
	}

	/**
	 * Resources which are marked as team private members return TRUE
	 * in all calls to #exists.
	 */
	public void testExists() {
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subfile.txt");
		IResource[] resources = { project, folder, file, subFile };
		ensureExistsInWorkspace(resources, true);

		// Check to see if all the resources exist in the workspace tree.
		assertExistsInWorkspace("1.0", resources);

		// set a folder to be a team private member
		setTeamPrivateMember("2.0", folder, true, IResource.DEPTH_ZERO);
		assertTeamPrivateMember("2.1", folder, true, IResource.DEPTH_ZERO);
		assertExistsInWorkspace("2.2", resources);

		// set all resources to be team private
		setTeamPrivateMember("3.0", project, true, IResource.DEPTH_INFINITE);
		assertTeamPrivateMember("3.1", project, true, IResource.DEPTH_INFINITE);
		assertExistsInWorkspace("3.2", resources);
	}

	/**
	 * Test the set and get methods for team private members.
	 */
	public void testSetGet() {
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subfile.txt");
		IResource[] resources = { project, folder, file, subFile };

		// Trying to set the value on non-existing resources will fail
		for (IResource resource : resources) {
			assertThrows(CoreException.class, () -> resource.setTeamPrivateMember(true));
		}

		// create the resources
		ensureExistsInWorkspace(resources, true);

		// Initial values should be false.
		for (IResource resource2 : resources) {
			IResource resource = resource2;
			assertFalse("1.0: " + resource.getFullPath(), resource.isTeamPrivateMember());
		}

		// Now set the values.
		for (IResource resource : resources) {
			try {
				resource.setTeamPrivateMember(true);
			} catch (CoreException e) {
				fail("2.0: " + resource.getFullPath(), e);
			}
		}

		// The values should be true for files and folders, false otherwise.
		for (IResource resource2 : resources) {
			IResource resource = resource2;
			switch (resource.getType()) {
				case IResource.FILE :
				case IResource.FOLDER :
					assertTrue("3.0: " + resource.getFullPath(), resource.isTeamPrivateMember());
					break;
				case IResource.PROJECT :
				case IResource.ROOT :
					assertFalse("3.1: " + resource.getFullPath(), resource.isTeamPrivateMember());
					break;
			}
		}

		// Clear the values.
		for (IResource resource : resources) {
			try {
				resource.setTeamPrivateMember(false);
			} catch (CoreException e) {
				fail("4.0: " + resource.getFullPath(), e);
			}
		}

		// Values should be false again.
		for (IResource resource2 : resources) {
			IResource resource = resource2;
			assertFalse("5.0: " + resource.getFullPath(), resource.isTeamPrivateMember());
		}
	}

	protected void assertTeamPrivateMember(final String message, IResource root, final boolean value, int depth) {
		IResourceVisitor visitor = resource -> {
			boolean expected = false;
			if (resource.getType() == IResource.FILE || resource.getType() == IResource.FOLDER) {
				expected = value;
			}
			assertEquals(message + resource.getFullPath(), expected, resource.isTeamPrivateMember());
			return true;
		};
		try {
			root.accept(visitor, depth, IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);
		} catch (CoreException e) {
			fail(message + "resource.accept", e);
		}
	}

	protected void setTeamPrivateMember(final String message, IResource root, final boolean value, int depth) {
		IResourceVisitor visitor = resource -> {
			try {
				resource.setTeamPrivateMember(value);
			} catch (CoreException e) {
				fail(message + resource.getFullPath(), e);
			}
			return true;
		};
		try {
			root.accept(visitor, depth, IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);
		} catch (CoreException e) {
			fail(message + "resource.accept", e);
		}
	}
}
