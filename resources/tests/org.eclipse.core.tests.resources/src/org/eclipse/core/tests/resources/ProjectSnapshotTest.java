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
 * Francis Lynch (Wind River) - adapted from FileSystemResourceManagerTest
 * Francis Lynch (Wind River) - [305718] Allow reading snapshot into renamed project
 * Martin Oberhuber (Wind River) - [306575] Save snapshot location with project
 *******************************************************************************/
package org.eclipse.core.tests.resources;

import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.resources.Project;
import org.eclipse.core.internal.resources.ProjectDescription;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

/**
 * Tests API for save/load refresh snapshots introduced in 3.6M6 (bug 301563):
 * <ul>
 * <li>{@link IProject#loadSnapshot(int, URI, org.eclipse.core.runtime.IProgressMonitor)}
 * <li>{@link IProject#saveSnapshot(int, URI, org.eclipse.core.runtime.IProgressMonitor)}
 * <li>{@link IProject#SNAPSHOT_TREE}
 * </ul>
 */
public class ProjectSnapshotTest extends ResourceTest {

	/** location of refresh snapshot file */
	private static final String REFRESH_SNAPSHOT_FILE_LOCATION = "resource-index.zip";
	/** test projects that we operate on */
	protected IProject[] projects = new IProject[2];


	@Override
	protected void setUp() throws Exception {
		super.setUp();
		projects[0] = getWorkspace().getRoot().getProject("p1");
		projects[1] = getWorkspace().getRoot().getProject("p2");
		ensureExistsInWorkspace(projects, true);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	private void populateProject(IProject project) {
		// add files and folders to project
		IFile file = project.getFile("file");
		ensureExistsInFileSystem(file);
		IFolder folder = project.getFolder("folder");
		IFolder subfolder = folder.getFolder("subfolder");
		IFile subfile = folder.getFile("subfile");
		ensureExistsInFileSystem(folder);
		ensureExistsInFileSystem(subfolder);
		ensureExistsInFileSystem(subfile);
	}

	private URI getSnapshotLocation(IProject project) {
		IPath projPath = project.getLocation();
		projPath = projPath.append(REFRESH_SNAPSHOT_FILE_LOCATION);
		return org.eclipse.core.filesystem.URIUtil.toURI(projPath);
	}

	/*
	 * Trying to save a null Snapshot throws CoreException.
	 */
	public void testSaveNullSnapshot() throws Throwable {
		boolean exceptionThrown = false;
		try {
			projects[0].saveSnapshot(IProject.SNAPSHOT_TREE, null, null);
		} catch (CoreException ce) {
			exceptionThrown = true;
		}
		assertTrue("1.0", exceptionThrown);
	}

	/*
	 * Create project and populate with resources. Save snapshot.
	 * Delete project (also delete resources on disk). Import project
	 * with snapshot. All resources must be marked as "exists" in the
	 * resource tree, although there is no resource actually on disk.
	 */
	public void testLoadNoRefresh() throws Throwable {
		final IProject project = projects[0];
		// add files and folders to project
		populateProject(project);
		// perform refresh to ensure new resources in tree
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		// save project refresh snapshot outside the project
		URI snapshotLocation = getSnapshotLocation(projects[1]);
		project.saveSnapshot(IProject.SNAPSHOT_TREE, snapshotLocation, null);
		// close and delete project contents
		project.close(null);
		// delete the project and import refresh snapshot
		project.delete(true, false, null);
		project.create(null);
		project.loadSnapshot(IProject.SNAPSHOT_TREE, snapshotLocation, null);
		project.open(IResource.NONE, null);
		// verify that the resources are thought to exist
		IFile file = project.getFile("file");
		IFolder folder = project.getFolder("folder");
		IFolder subfolder = folder.getFolder("subfolder");
		IFile subfile = folder.getFile("subfile");
		assertTrue("1.1", file.exists());
		assertTrue("1.2", folder.exists());
		assertTrue("1.3", subfolder.exists());
		assertTrue("1.4", subfile.exists());
	}

	/*
	 * Create project and populate with resources. Save snapshot.
	 * Delete project (also delete resources on disk). Import project
	 * with snapshot and perform a refresh. Resource delta must be created,
	 * and none of the snapshot resources is found any more.
	 */
	public void testLoadWithRefresh() throws Throwable {
		final IProject project = projects[0];
		// add files and folders to project
		populateProject(project);
		// perform refresh to ensure new resources in tree
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		// save project refresh snapshot outside the project
		URI snapshotLocation = getSnapshotLocation(projects[1]);
		project.saveSnapshot(IProject.SNAPSHOT_TREE, snapshotLocation, null);
		// wait before recreating the .project file on disk, to ensure it will have
		// a different time stamp and be reported as a modification. This is
		// because some file systems only have a 1 second timestamp granularity.
		TimeUnit.MILLISECONDS.sleep(1001);
		// close and delete project contents
		project.close(null);
		// delete the project and import refresh snapshot
		project.delete(true, false, null);
		project.create(null);
		project.loadSnapshot(IProject.SNAPSHOT_TREE, snapshotLocation, null);
		project.open(IResource.NONE, null);
		// set up resource delta verifier
		ResourceDeltaVerifier verifier = new ResourceDeltaVerifier();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(verifier);
		verifier.reset();
		IFile file = project.getFile("file");
		IFolder folder = project.getFolder("folder");
		IFolder subfolder = folder.getFolder("subfolder");
		IFile subfile = folder.getFile("subfile");
		verifier.addExpectedChange(file, IResourceDelta.REMOVED, 0);
		verifier.addExpectedChange(folder, IResourceDelta.REMOVED, 0);
		verifier.addExpectedChange(subfolder, IResourceDelta.REMOVED, 0);
		verifier.addExpectedChange(subfile, IResourceDelta.REMOVED, 0);
		verifier.addExpectedChange(project, IResourceDelta.CHANGED, IResourceDelta.DESCRIPTION);
		IFile dotProject = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		verifier.addExpectedChange(dotProject, IResourceDelta.CHANGED, IResourceDelta.CONTENT);
		// perform refresh to create resource delta against snapshot
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		verifier.verifyDelta(null);
		assertTrue("1.0 " + verifier.getMessage(), verifier.isDeltaValid());
		// verify that the resources are no longer thought to exist
		assertTrue("1.1", !file.exists());
		assertTrue("1.2", !folder.exists());
		assertTrue("1.3", !subfolder.exists());
		assertTrue("1.4", !subfile.exists());
	}

	/*
	 * Create project and populate with resources. Save snapshot.
	 * Import the project using the snapshot but with a different
	 * project name. All resources must be marked as "exists" in the
	 * resource tree for the new, renamed project.
	 */
	public void testLoadWithRename() throws Throwable {
		IProject project = projects[0];
		// add files and folders to project
		populateProject(project);
		// perform refresh to ensure new resources in tree
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		// save project refresh snapshot outside the project
		URI snapshotLocation = getSnapshotLocation(projects[1]);
		project.saveSnapshot(IProject.SNAPSHOT_TREE, snapshotLocation, null);
		// close and delete project contents
		project.close(null);
		project.delete(true, false, null);
		// open the project using a different name (p3) and import refresh snapshot
		project = getWorkspace().getRoot().getProject("p3");
		project.create(null);
		project.loadSnapshot(IProject.SNAPSHOT_TREE, snapshotLocation, null);
		project.open(IResource.NONE, null);

		// verify that the resources are thought to exist in this project
		IFile file = project.getFile("file");
		IFolder folder = project.getFolder("folder");
		IFolder subfolder = folder.getFolder("subfolder");
		IFile subfile = folder.getFile("subfile");
		assertTrue("1.1", file.exists());
		assertTrue("1.2", folder.exists());
		assertTrue("1.3", subfolder.exists());
		assertTrue("1.4", subfile.exists());
	}

	/*
	 * Create project and populate with resources. Rename the project.
	 * Save snapshot. Delete project (also delete resources on disk).
	 * Import project with snapshot with a different project name.
	 * All resources must be marked as "exists" in the resource tree
	 * for the new, renamed project, even though they are not actually present.
	 */
	public void testLoadWithRename2() throws Throwable {
		IProject project = projects[0];
		// add files and folders to project
		populateProject(project);
		// perform refresh to ensure new resources in tree
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		// rename the project
		project.move(Path.ROOT.append("p0"), true, null);
		project = getWorkspace().getRoot().getProject("p0");
		// add two more files to probably provoke a tree delta chain
		// In SaveManager.writeTree() line 1885, treesToSave.length must be 1
		IFile file2 = project.getFile("file2");
		ensureExistsInFileSystem(file2);
		project.getFile("file3");
		// save project refresh snapshot outside the project
		URI snapshotLocation = getSnapshotLocation(projects[1]);
		project.saveSnapshot(IProject.SNAPSHOT_TREE, snapshotLocation, null);
		// close and delete project contents
		project.close(null);
		project.delete(true, false, null);
		// open the project using a different name (p3) and import refresh snapshot
		project = getWorkspace().getRoot().getProject("p3");
		project.create(null);
		project.loadSnapshot(IProject.SNAPSHOT_TREE, snapshotLocation, null);
		project.open(IResource.NONE, null);

		// verify that the resources are thought to exist in this project
		IFile file = project.getFile("file");
		IFolder folder = project.getFolder("folder");
		IFolder subfolder = folder.getFolder("subfolder");
		IFile subfile = folder.getFile("subfile");
		assertTrue("1.1", file.exists());
		assertTrue("1.2", folder.exists());
		assertTrue("1.3", subfolder.exists());
		assertTrue("1.4", subfile.exists());
	}

	public void testAutoLoadInvalidURI() throws Throwable {
		// create project with invalid snapshot autoload location
		IProject project = getWorkspace().getRoot().getProject("project");
		IProjectDescription description = getWorkspace().newProjectDescription(project.getName());
		((ProjectDescription) description).setSnapshotLocationURI(URI.create("./relative/uri.zip"));
		project.create(description, null);
		ensureExistsInFileSystem(project.getFolder("foo"));
		assertFalse("1.0", project.getFolder("foo").exists());
		// expect to see warning logged, but project open successfully and refresh
		project.open(null);
		assertTrue("1.1", project.isOpen());
		assertTrue("1.2", project.getFolder("foo").exists());
		boolean errorReported = false;
		try {
			project.saveSnapshot(Project.SNAPSHOT_SET_AUTOLOAD, URI.create("NON_EXISTING/foo/bar.zip"), null);
		} catch (CoreException ce) {
			errorReported = true;
		}
		assertTrue("1.4", errorReported);
	}

	public void testAutoLoadMissingSnapshot() throws Throwable {
		IProject project = getWorkspace().getRoot().getProject("project");
		IProjectDescription description = getWorkspace().newProjectDescription(project.getName());
		// create project with non-existing snapshot autoload location
		((ProjectDescription) description).setSnapshotLocationURI(getTempStore().toURI());
		project.create(description, null);
		ensureExistsInFileSystem(project.getFile("foo"));
		assertFalse("1.0", project.getFile("foo").exists());
		project.open(null);
		// expect warning logged but project open and refreshed
		assertTrue("1.1", project.isOpen());
		assertTrue("1.2", project.getFile("foo").exists());
	}

	/*
	 * Create project and populate with resources. Specify
	 * snapshot location in project description.
	 * Import the project from a different location, with different name.
	 * All resources must be marked as "exists" in the resource tree for
	 * the new, renamed project.
	 */
	public void testAutoLoadWithRename() throws Throwable {
		// create project p0 outside the workspace
		IFileStore tempStore = getTempStore();
		tempStore.mkdir(EFS.NONE, null);
		IProject project = getWorkspace().getRoot().getProject("project");
		IProjectDescription description = getWorkspace().newProjectDescription(project.getName());
		description.setLocationURI(tempStore.getChild("project").toURI());
		project.create(description, null);
		project.open(null);

		// add files and folders to project and refresh to ensure resources in tree
		populateProject(project);
		project.refreshLocal(IResource.DEPTH_INFINITE, null);

		// specify snapshot location relative to project, and store in project description
		URI snapshotLocation = tempStore.getChild("project-index.zip").toURI();
		snapshotLocation = project.getPathVariableManager().convertToRelative(snapshotLocation, true, null);
		project.saveSnapshot(IProject.SNAPSHOT_TREE | Project.SNAPSHOT_SET_AUTOLOAD, snapshotLocation, null);

		// copy the project to a new temp store, close and delete original project
		IFileStore newProjectStore = tempStore.getChild("pnew");
		newProjectStore.mkdir(EFS.NONE, null);
		EFS.getStore(project.getLocationURI()).getChild(".project").copy(newProjectStore.getChild(".project"), EFS.NONE, null);
		project.close(null);
		project.delete(true, false, null);
		// import the project from new location and using a different name; must
		// auto-load snapshot
		try (InputStream is = newProjectStore.getChild(".project").openInputStream(EFS.NONE, null)) {
			description = getWorkspace().loadProjectDescription(is);
		}
		description.setLocationURI(newProjectStore.toURI());
		project = getWorkspace().getRoot().getProject(description.getName() + "-mybranch");
		project.create(description, null);
		project.open(IResource.NONE, null);

		// verify that the resources are thought to exist in this project
		IFile file = project.getFile("file");
		IFolder folder = project.getFolder("folder");
		IFolder subfolder = folder.getFolder("subfolder");
		IFile subfile = folder.getFile("subfile");
		assertTrue("1.1", file.exists());
		assertTrue("1.2", folder.exists());
		assertTrue("1.3", subfolder.exists());
		assertTrue("1.4", subfile.exists());
	}

	public void testResetAutoLoadSnapshot() throws Throwable {
		IProject project = projects[0];
		URI tempURI = getTempStore().toURI();
		IFile projectFile = project.getFile(".project");
		long stamp = projectFile.getModificationStamp();

		// set empty snapshot while already empty -> no change of .project file
		//project.saveSnapshot(Project.SNAPSHOT_SET_AUTOLOAD, null, null);
		ProjectDescription desc = (ProjectDescription) project.getDescription();
		desc.setSnapshotLocationURI(null);
		project.setDescription(desc, null);
		assertEquals("1.0", stamp, projectFile.getModificationStamp());

		// set or reset a snapshot -> .project file changed, unless setting to same existing URI
		project.saveSnapshot(Project.SNAPSHOT_SET_AUTOLOAD, tempURI, null);
		assertEquals("2.0", tempURI, ((ProjectDescription) project.getDescription()).getSnapshotLocationURI());
		long stamp2 = projectFile.getModificationStamp();
		assertFalse("2.1", stamp == stamp2);
		project.saveSnapshot(Project.SNAPSHOT_SET_AUTOLOAD, tempURI, null);
		assertEquals("2.2", stamp2, projectFile.getModificationStamp());

		//project.saveSnapshot(Project.SNAPSHOT_SET_AUTOLOAD, null, null);
		desc = (ProjectDescription) project.getDescription();
		desc.setSnapshotLocationURI(null);
		project.setDescription(desc, null);
		assertNull("3.0", ((ProjectDescription) project.getDescription()).getSnapshotLocationURI());
		assertFalse("3.1", stamp2 == projectFile.getModificationStamp());

		// setting snapshot while project is closed is forbidden
		project.close(null);
		boolean exceptionThrown = false;
		try {
			project.saveSnapshot(Project.SNAPSHOT_SET_AUTOLOAD, tempURI, null);
		} catch (CoreException e) {
			exceptionThrown = true;
		}
		assertTrue("4.0", exceptionThrown);
	}

}
