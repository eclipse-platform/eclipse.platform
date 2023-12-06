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

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertDoesNotExistInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.waitForBuild;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.is;
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

public class TeamPrivateMemberTest extends ResourceTest {
	public void testRefreshLocal() throws Exception {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject("MyProject");
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subfile.txt");
		IResource[] resources = { project, folder, file, subFile };
		createInWorkspace(resources);

		ResourceDeltaVerifier listener = new ResourceDeltaVerifier();
		listener.addExpectedChange(subFile, IResourceDelta.CHANGED, IResourceDelta.CONTENT);
		getWorkspace().addResourceChangeListener(listener);
		try {
			setTeamPrivateMember(folder, true, IResource.DEPTH_ZERO);
			ensureOutOfSync(subFile);
			project.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
			assertTrue(listener.getMessage(), listener.isDeltaValid());
		} finally {
			getWorkspace().removeResourceChangeListener(listener);
		}
	}

	/**
	 * Resources which are marked as team private members should always be found.
	 */
	public void testFindMember() throws CoreException {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject("MyProject");
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subfile.txt");
		IResource[] resources = { project, folder, file, subFile };
		createInWorkspace(resources);

		// no team private members
		assertThat(root.findMember(project.getFullPath()), is(project));
		assertThat(root.findMember(folder.getFullPath()), is(folder));
		assertThat(root.findMember(file.getFullPath()), is(file));
		assertThat(root.findMember(subFile.getFullPath()), is(subFile));

		// the folder is team private
		setTeamPrivateMember(folder, true, IResource.DEPTH_ZERO);
		assertThat(root.findMember(project.getFullPath()), is(project));
		assertThat(root.findMember(folder.getFullPath()), is(folder));
		assertThat(root.findMember(file.getFullPath()), is(file));
		assertThat(root.findMember(subFile.getFullPath()), is(subFile));

		// all are team private
		setTeamPrivateMember(project, true, IResource.DEPTH_INFINITE);
		assertThat(root.findMember(project.getFullPath()), is(project));
		assertThat(root.findMember(folder.getFullPath()), is(folder));
		assertThat(root.findMember(file.getFullPath()), is(file));
		assertThat(root.findMember(subFile.getFullPath()), is(subFile));
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
		createInWorkspace(resources);

		// Initial values should be false.
		assertTeamPrivateMember(project, false, IResource.DEPTH_INFINITE);

		// Check the calls to #members
		// +1 for the project description file
		assertThat(project.members(), arrayWithSize(4));
		assertThat(folder.members(), arrayWithSize(1));

		// Set the values.
		setTeamPrivateMember(project, true, IResource.DEPTH_INFINITE);
		assertTeamPrivateMember(project, true, IResource.DEPTH_INFINITE);

		// Check the values
		assertTeamPrivateMember(project, true, IResource.DEPTH_INFINITE);

		// Check the calls to #members
		assertThat(project.members(), arrayWithSize(0));
		assertThat(folder.members(), arrayWithSize(0));

		// reset to false
		setTeamPrivateMember(project, false, IResource.DEPTH_INFINITE);
		assertTeamPrivateMember(project, false, IResource.DEPTH_INFINITE);

		// Check the calls to members(IResource.NONE);
		// +1 for the project description file
		assertThat(project.members(IResource.NONE), arrayWithSize(4));
		// +1 for the project description file
		assertThat(project.members(IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS), arrayWithSize(4));
		assertThat(folder.members(), arrayWithSize(1));

		// Set one of the children to be TEAM_PRIVATE and try again
		setTeamPrivateMember(folder, true, IResource.DEPTH_ZERO);
		// +1 for project description, -1 for team private folder
		assertThat(project.members(), arrayWithSize(3));
		assertThat(folder.members(), arrayWithSize(1));
		// +1 for project description, -1 for team private folder
		assertThat(project.members(IResource.NONE), arrayWithSize(3));
		assertThat(folder.members(), arrayWithSize(1));
		// +1 for project description
		assertThat(project.members(IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS), arrayWithSize(4));
		assertThat(folder.members(), arrayWithSize(1));

		// Set all the resources to be team private
		setTeamPrivateMember(project, true, IResource.DEPTH_INFINITE);
		assertTeamPrivateMember(project, true, IResource.DEPTH_INFINITE);
		assertThat(project.members(IResource.NONE), arrayWithSize(0));
		assertThat(folder.members(IResource.NONE), arrayWithSize(0));
		assertThat(project.members(IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS), arrayWithSize(4));
		assertThat(folder.members(IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS), arrayWithSize(1));
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
		createInWorkspace(resources);
		IResource description = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);

		// default case, no team private members
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
		project.accept(visitor, IResource.DEPTH_INFINITE, IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);
		assertTrue(visitor.getMessage(), visitor.isValid());

		// set the folder to be team private. It and its children should
		// be ignored by the visitor
		setTeamPrivateMember(folder, true, IResource.DEPTH_ZERO);
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
		project.accept(visitor, IResource.DEPTH_INFINITE, IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);
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
		folder.accept(visitor, IResource.DEPTH_INFINITE, IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);
		assertTrue(visitor.getMessage(), visitor.isValid());

		// now set all file/folder resources to be team private.
		setTeamPrivateMember(project, true, IResource.DEPTH_INFINITE);
		assertTeamPrivateMember(project, true, IResource.DEPTH_INFINITE);
		visitor.reset();
		// projects are never team private
		visitor.addExpected(project);
		project.accept(visitor);
		assertTrue(visitor.getMessage(), visitor.isValid());

		visitor.reset();
		visitor.addExpected(project);
		project.accept(visitor, IResource.DEPTH_INFINITE, IResource.NONE);
		assertTrue(visitor.getMessage(), visitor.isValid());
		// should see all resources if we include the flag
		visitor.reset();
		visitor.addExpected(resources);
		visitor.addExpected(description);
		project.accept(visitor, IResource.DEPTH_INFINITE, IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);
		assertTrue(visitor.getMessage(), visitor.isValid());
	}

	public void testCopy() throws CoreException {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject("MyProject");
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subfile.txt");
		IResource[] resources = { project, folder, file, subFile };
		createInWorkspace(resources);

		// handles to the destination resources
		IProject destProject = root.getProject("MyOtherProject");
		IFolder destFolder = destProject.getFolder(folder.getName());
		IFile destFile = destProject.getFile(file.getName());
		IFile destSubFile = destFolder.getFile(subFile.getName());
		IResource[] destResources = { destProject, destFolder, destFile, destSubFile };
		ensureDoesNotExistInWorkspace(destResources);

		// set a folder to be team private
		setTeamPrivateMember(folder, true, IResource.DEPTH_ZERO);
		// copy the project
		int flags = IResource.FORCE;
		project.copy(destProject.getFullPath(), flags, createTestMonitor());
		assertExistsInWorkspace(resources);
		assertExistsInWorkspace(destResources);

		// Do it again and but just copy the folder
		ensureDoesNotExistInWorkspace(destResources);
		createInWorkspace(resources);
		createInWorkspace(destProject);
		setTeamPrivateMember(folder, true, IResource.DEPTH_ZERO);
		folder.copy(destFolder.getFullPath(), flags, createTestMonitor());
		assertExistsInWorkspace(new IResource[] { folder, subFile });
		assertExistsInWorkspace(new IResource[] { destFolder, destSubFile });

		// set all the resources to be team private
		// copy the project
		ensureDoesNotExistInWorkspace(destResources);
		createInWorkspace(resources);
		setTeamPrivateMember(project, true, IResource.DEPTH_INFINITE);
		project.copy(destProject.getFullPath(), flags, createTestMonitor());
		assertExistsInWorkspace(resources);
		assertExistsInWorkspace(destResources);

		// do it again but only copy the folder
		ensureDoesNotExistInWorkspace(destResources);
		createInWorkspace(resources);
		createInWorkspace(destProject);
		setTeamPrivateMember(project, true, IResource.DEPTH_INFINITE);
		folder.copy(destFolder.getFullPath(), flags, createTestMonitor());
		assertExistsInWorkspace(new IResource[] { folder, subFile });
		assertExistsInWorkspace(new IResource[] { destFolder, destSubFile });
	}

	public void testMove() throws CoreException {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject("MyProject");
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subfile.txt");
		IResource[] resources = { project, folder, file, subFile };
		createInWorkspace(resources);

		// handles to the destination resources
		IProject destProject = root.getProject("MyOtherProject");
		IFolder destFolder = destProject.getFolder(folder.getName());
		IFile destFile = destProject.getFile(file.getName());
		IFile destSubFile = destFolder.getFile(subFile.getName());
		IResource[] destResources = { destProject, destFolder, destFile, destSubFile };
		ensureDoesNotExistInWorkspace(destResources);

		// set a folder to be team private
		setTeamPrivateMember(folder, true, IResource.DEPTH_ZERO);
		// move the project
		int flags = IResource.FORCE;
		project.move(destProject.getFullPath(), flags, createTestMonitor());
		assertDoesNotExistInWorkspace(resources);
		assertExistsInWorkspace(destResources);

		// Do it again and but just move the folder
		ensureDoesNotExistInWorkspace(destResources);
		createInWorkspace(resources);
		createInWorkspace(destProject);
		setTeamPrivateMember(folder, true, IResource.DEPTH_ZERO);
		folder.move(destFolder.getFullPath(), flags, createTestMonitor());
		assertDoesNotExistInWorkspace(new IResource[] { folder, subFile });
		assertExistsInWorkspace(new IResource[] { destFolder, destSubFile });

		// set all the resources to be team private
		// move the project
		ensureDoesNotExistInWorkspace(destResources);
		createInWorkspace(resources);
		setTeamPrivateMember(project, true, IResource.DEPTH_INFINITE);
		project.move(destProject.getFullPath(), flags, createTestMonitor());
		assertDoesNotExistInWorkspace(resources);
		assertExistsInWorkspace(destResources);

		// do it again but only move the folder
		ensureDoesNotExistInWorkspace(destResources);
		createInWorkspace(resources);
		createInWorkspace(destProject);
		setTeamPrivateMember(project, true, IResource.DEPTH_INFINITE);
		folder.move(destFolder.getFullPath(), flags, createTestMonitor());
		assertDoesNotExistInWorkspace(new IResource[] { folder, subFile });
		assertExistsInWorkspace(new IResource[] { destFolder, destSubFile });
	}

	public void testDelete() throws CoreException {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject("MyProject");
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subfile.txt");
		IResource[] resources = new IResource[] {project, folder, file, subFile};
		createInWorkspace(resources);

		// default behaviour with no team private members
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

		// set one child to be team private
		createInWorkspace(resources);
		setTeamPrivateMember(folder, true, IResource.DEPTH_ZERO);
		// delete the project
		project.delete(flags, createTestMonitor());
		assertDoesNotExistInWorkspace(resources);
		// delete a folder
		createInWorkspace(resources);
		setTeamPrivateMember(folder, true, IResource.DEPTH_ZERO);
		folder.delete(flags, createTestMonitor());
		assertDoesNotExistInWorkspace(new IResource[] { folder, subFile });
		assertExistsInWorkspace(new IResource[] { project, file });

		// set all resources to be team private
		createInWorkspace(resources);
		setTeamPrivateMember(project, true, IResource.DEPTH_INFINITE);
		// delete the project
		project.delete(flags, createTestMonitor());
		assertDoesNotExistInWorkspace(resources);
		// delete a file
		createInWorkspace(resources);
		setTeamPrivateMember(project, true, IResource.DEPTH_INFINITE);
		file.delete(flags, createTestMonitor());
		assertDoesNotExistInWorkspace(file);
		assertExistsInWorkspace(new IResource[] { project, folder, subFile });
		// delete a folder
		createInWorkspace(resources);
		setTeamPrivateMember(project, true, IResource.DEPTH_INFINITE);
		folder.delete(flags, createTestMonitor());
		assertDoesNotExistInWorkspace(new IResource[] { folder, subFile });
		assertExistsInWorkspace(new IResource[] { project, file });
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
			IWorkspaceRunnable body = monitor -> createInWorkspace(resources);
			listener.addExpectedChange(resources, IResourceDelta.ADDED, IResource.NONE);
			listener.addExpectedChange(project, IResourceDelta.ADDED, IResourceDelta.OPEN);
			listener.addExpectedChange(description, IResourceDelta.ADDED, IResource.NONE);
			getWorkspace().run(body, createTestMonitor());
			waitForBuild();
			assertTrue(listener.getMessage(), listener.isDeltaValid());
			ensureDoesNotExistInWorkspace(resources);
		} finally {
			getWorkspace().removeResourceChangeListener(listener);
		}

		// set the folder to be team private and do the same test
		getWorkspace().addResourceChangeListener(listener);
		try {
			IWorkspaceRunnable body = monitor -> {
				createInWorkspace(resources);
				setTeamPrivateMember(folder, true, IResource.DEPTH_ZERO);
			};
			listener.reset();
			listener.addExpectedChange(resources, IResourceDelta.ADDED, IResource.NONE);
			listener.addExpectedChange(project, IResourceDelta.ADDED, IResourceDelta.OPEN);
			listener.addExpectedChange(description, IResourceDelta.ADDED, IResource.NONE);
			getWorkspace().run(body, createTestMonitor());
			assertTrue(listener.getMessage(), listener.isDeltaValid());
			ensureDoesNotExistInWorkspace(resources);
		} finally {
			getWorkspace().removeResourceChangeListener(listener);
		}

		// set all resources to be team private and do the same test
		getWorkspace().addResourceChangeListener(listener);
		try {
			IWorkspaceRunnable body = monitor -> {
				createInWorkspace(resources);
				setTeamPrivateMember(project, true, IResource.DEPTH_INFINITE);
			};
			listener.reset();
			listener.addExpectedChange(resources, IResourceDelta.ADDED, IResource.NONE);
			listener.addExpectedChange(project, IResourceDelta.ADDED, IResourceDelta.OPEN);
			listener.addExpectedChange(description, IResourceDelta.ADDED, IResource.NONE);
			getWorkspace().run(body, createTestMonitor());
			assertTrue(listener.getMessage(), listener.isDeltaValid());
			ensureDoesNotExistInWorkspace(resources);
		} finally {
			getWorkspace().removeResourceChangeListener(listener);
		}
	}

	/**
	 * Resources which are marked as team private members return TRUE
	 * in all calls to #exists.
	 */
	public void testExists() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subfile.txt");
		IResource[] resources = { project, folder, file, subFile };
		createInWorkspace(resources);

		// Check to see if all the resources exist in the workspace tree.
		assertExistsInWorkspace(resources);

		// set a folder to be a team private member
		setTeamPrivateMember(folder, true, IResource.DEPTH_ZERO);
		assertTeamPrivateMember(folder, true, IResource.DEPTH_ZERO);
		assertExistsInWorkspace(resources);

		// set all resources to be team private
		setTeamPrivateMember(project, true, IResource.DEPTH_INFINITE);
		assertTeamPrivateMember(project, true, IResource.DEPTH_INFINITE);
		assertExistsInWorkspace(resources);
	}

	/**
	 * Test the set and get methods for team private members.
	 */
	public void testSetGet() throws CoreException {
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
		createInWorkspace(resources);

		// Initial values should be false.
		for (IResource resource : resources) {
			assertTeamPrivateMember(resource, false);
		}

		// Now set the values.
		for (IResource resource : resources) {
			resource.setTeamPrivateMember(true);
		}

		// The values should be true for files and folders, false otherwise.
		for (IResource resource2 : resources) {
			IResource resource = resource2;
			switch (resource.getType()) {
				case IResource.FILE :
				case IResource.FOLDER :
					assertTeamPrivateMember(resource, true);
					break;
				case IResource.PROJECT :
				case IResource.ROOT :
					assertTeamPrivateMember(resource, false);
					break;
			}
		}

		// Clear the values.
		for (IResource resource : resources) {
			resource.setTeamPrivateMember(false);
		}

		// Values should be false again.
		for (IResource resource : resources) {
			assertTeamPrivateMember(resource, false);
		}
	}

	private void assertTeamPrivateMember(IResource root, final boolean value, int depth) throws CoreException {
		IResourceVisitor visitor = resource -> {
			boolean expected = false;
			if (resource.getType() == IResource.FILE || resource.getType() == IResource.FOLDER) {
				expected = value;
			}
			assertTeamPrivateMember(resource, expected);
			return true;
		};
		root.accept(visitor, depth, IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);
	}

	private void assertTeamPrivateMember(IResource resource, boolean expectedValue) {
		assertThat("unexpected isTeamPrivateMember value for resource: " + resource, resource.isTeamPrivateMember(),
				is(expectedValue));
	}

	private void setTeamPrivateMember(IResource root, final boolean value, int depth)
			throws CoreException {
		IResourceVisitor visitor = resource -> {
			resource.setTeamPrivateMember(value);
			return true;
		};
		root.accept(visitor, depth, IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);
	}

}
