/*******************************************************************************
 * Copyright (c) 2008, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Serge Beauchamp (Freescale Semiconductor) - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources;

import static org.junit.Assert.assertThrows;

import java.io.File;
import java.net.URI;
import org.eclipse.core.internal.resources.ICoreConstants;
import org.eclipse.core.internal.resources.ProjectDescriptionReader;
import org.eclipse.core.internal.resources.RegexFileInfoMatcher;
import org.eclipse.core.internal.resources.Resource;
import org.eclipse.core.internal.resources.ResourceInfo;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.FileInfoMatcherDescription;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceFilterDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/**
 * Tests the following API methods:
 *  {@link IContainer#createFilter(int, FileInfoMatcherDescription, int, org.eclipse.core.runtime.IProgressMonitor)}
 *  {@link IContainer#getFilters()}
 *
 * This test tests resource filters with projects, folders, linked resource folders,
 * and moving those resources to different parents.
 */
public class FilteredResourceTest extends ResourceTest {
	private static final String REGEX_FILTER_PROVIDER = "org.eclipse.core.resources.regexFilterMatcher";
	protected String childName = "File.txt";
	protected IProject closedProject;
	protected IFile existingFileInExistingProject;
	protected IFolder existingFolderInExistingFolder;
	protected IFolder existingFolderInExistingProject;
	protected IProject existingProject;
	protected IPath localFile;
	protected IPath localFolder;
	protected IFile nonExistingFileInExistingFolder;
	protected IFile nonExistingFileInExistingProject;
	protected IFile nonExistingFileInOtherExistingProject;
	protected IFolder nonExistingFolderInExistingFolder;
	protected IFolder nonExistingFolderInExistingProject;
	protected IFolder nonExistingFolderInOtherExistingProject;
	protected IFolder nonExistingFolder2InOtherExistingProject;
	protected IProject otherExistingProject;

	protected void doCleanup() throws Exception {
		ensureExistsInWorkspace(new IResource[] {existingProject, otherExistingProject, closedProject, existingFolderInExistingProject, existingFolderInExistingFolder, existingFileInExistingProject}, true);
		closedProject.close(getMonitor());
		ensureDoesNotExistInWorkspace(new IResource[] {nonExistingFolderInExistingProject, nonExistingFolderInExistingFolder, nonExistingFolderInOtherExistingProject, nonExistingFolder2InOtherExistingProject, nonExistingFileInExistingProject, nonExistingFileInOtherExistingProject, nonExistingFileInExistingFolder});
		resolve(localFolder).toFile().mkdirs();
		createFileInFileSystem(resolve(localFile), getRandomContents());
	}

	/**
	 * Maybe overridden in subclasses that use path variables.
	 */
	protected IPath resolve(IPath path) {
		return path;
	}

	/**
	 * Maybe overridden in subclasses that use path variables.
	 */
	protected URI resolve(URI uri) {
		return uri;
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		existingProject = getWorkspace().getRoot().getProject("ExistingProject");
		otherExistingProject = getWorkspace().getRoot().getProject("OtherExistingProject");
		closedProject = getWorkspace().getRoot().getProject("ClosedProject");
		existingFolderInExistingProject = existingProject.getFolder("existingFolderInExistingProject");
		existingFolderInExistingFolder = existingFolderInExistingProject.getFolder("existingFolderInExistingFolder");
		nonExistingFolderInExistingProject = existingProject.getFolder("nonExistingFolderInExistingProject");
		nonExistingFolderInOtherExistingProject = otherExistingProject.getFolder("nonExistingFolderInOtherExistingProject");
		nonExistingFolder2InOtherExistingProject = otherExistingProject.getFolder("nonExistingFolder2InOtherExistingProject");
		nonExistingFolderInExistingFolder = existingFolderInExistingProject.getFolder("nonExistingFolderInExistingFolder");
		existingFileInExistingProject = existingProject.getFile("existingFileInExistingProject");
		nonExistingFileInExistingProject = existingProject.getFile("nonExistingFileInExistingProject");
		nonExistingFileInOtherExistingProject = otherExistingProject.getFile("nonExistingFileInOtherExistingProject");
		nonExistingFileInExistingFolder = existingFolderInExistingProject.getFile("nonExistingFileInExistingFolder");
		localFolder = getRandomLocation();
		localFile = localFolder.append(childName);
		doCleanup();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		Workspace.clear(resolve(localFolder).toFile());
	}

	/**
	 * Tests the creation of a simple filter on a folder.
	 */
	public void testCreateFilterOnFolder() throws CoreException {
		FileInfoMatcherDescription matcherDescription = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, "foo");
		existingFolderInExistingProject.createFilter(IResourceFilterDescription.INCLUDE_ONLY
				| IResourceFilterDescription.FILES | IResourceFilterDescription.FOLDERS, matcherDescription, 0,
				getMonitor());

		IFile foo = existingFolderInExistingProject.getFile("foo");
		IFile bar = existingFolderInExistingProject.getFile("bar");

		ensureExistsInWorkspace(new IResource[] {foo, bar}, true);

		existingProject.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());

		//close and reopen the project
		existingProject.close(getMonitor());
		existingProject.open(getMonitor());

		IResourceFilterDescription[] filters = existingFolderInExistingProject.getFilters();

		assertEquals("1.4", filters.length, 1);
		assertEquals("1.5", filters[0].getFileInfoMatcherDescription().getId(), REGEX_FILTER_PROVIDER);
		assertEquals("1.6", filters[0].getFileInfoMatcherDescription().getArguments(), "foo");
		assertEquals("1.7", filters[0].getType(), IResourceFilterDescription.INCLUDE_ONLY | IResourceFilterDescription.FILES | IResourceFilterDescription.FOLDERS);
		assertEquals("1.8", filters[0].getResource(), existingFolderInExistingProject);

		IResource members[] = existingFolderInExistingProject.members();
		assertEquals("2.0", members.length, 1);
		assertEquals("2.1", members[0].getType(), IResource.FILE);
		assertEquals("2.2", members[0].getName(), "foo");

		IWorkspace workspace = existingProject.getWorkspace();
		assertTrue("2.3", !workspace.validateFiltered(bar).isOK());
		assertTrue("2.4", workspace.validateFiltered(foo).isOK());
	}

	/**
	 * Tests the creation of a simple filter on a project.
	 */
	public void testCreateFilterOnProject() throws CoreException {
		FileInfoMatcherDescription matcherDescription = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, "foo");
		existingProject.createFilter(IResourceFilterDescription.INCLUDE_ONLY | IResourceFilterDescription.FOLDERS,
				matcherDescription, 0, getMonitor());

		IFolder foo = existingProject.getFolder("foo");
		IFolder bar = existingProject.getFolder("bar");

		ensureExistsInWorkspace(new IResource[] {foo, bar}, true);

		existingProject.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());

		//close and reopen the project
		existingProject.close(getMonitor());
		existingProject.open(getMonitor());
		IResourceFilterDescription[] filters = existingProject.getFilters();

		assertEquals("1.4", filters.length, 1);
		assertEquals("1.5", filters[0].getFileInfoMatcherDescription().getId(), REGEX_FILTER_PROVIDER);
		assertEquals("1.6", filters[0].getFileInfoMatcherDescription().getArguments(), "foo");
		assertEquals("1.7", filters[0].getType(), IResourceFilterDescription.INCLUDE_ONLY | IResourceFilterDescription.FOLDERS);
		assertEquals("1.8", filters[0].getResource(), existingProject);

		IResource members[] = existingProject.members();
		assertEquals("2.0", members.length, 3);
		for (IResource member : members) {
			if (member.getType() == IResource.FOLDER) {
				assertEquals("2.1", member.getType(), IResource.FOLDER);
				assertEquals("2.2", member.getName(), "foo");
			}
		}

		IWorkspace workspace = existingProject.getWorkspace();
		assertTrue("2.1", !workspace.validateFiltered(bar).isOK());
		assertTrue("2.2", workspace.validateFiltered(foo).isOK());
	}

	/**
	 * Tests the creation of a simple filter on a linked folder.
	 */
	public void testCreateFilterOnLinkedFolder() throws CoreException {
		IPath location = getRandomLocation();
		IFolder folder = nonExistingFolderInExistingProject;

		//try to create without the flag (should fail)
		assertThrows(CoreException.class, () -> folder.createLink(location, IResource.NONE, getMonitor()));

		FileInfoMatcherDescription matcherDescription = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, "foo");
		folder.createFilter(IResourceFilterDescription.INCLUDE_ONLY | IResourceFilterDescription.FILES,
				matcherDescription, 0, getMonitor());

		IFile foo = folder.getFile("foo");
		IFile bar = folder.getFile("bar");

		ensureExistsInWorkspace(new IResource[] {foo, bar}, true);

		existingProject.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());

		//close and reopen the project
		existingProject.close(getMonitor());
		existingProject.open(getMonitor());

		IResourceFilterDescription[] filters = folder.getFilters();

		assertEquals("1.4", filters.length, 1);
		assertEquals("1.5", filters[0].getFileInfoMatcherDescription().getId(), REGEX_FILTER_PROVIDER);
		assertEquals("1.6", filters[0].getFileInfoMatcherDescription().getArguments(), "foo");
		assertEquals("1.7", filters[0].getType(), IResourceFilterDescription.INCLUDE_ONLY | IResourceFilterDescription.FILES);
		assertEquals("1.8", filters[0].getResource(), folder);

		IResource members[] = folder.members();
		assertEquals("2.0", members.length, 1);
		assertEquals("2.1", members[0].getType(), IResource.FILE);
		assertEquals("2.2", members[0].getName(), "foo");

		IWorkspace workspace = existingProject.getWorkspace();
		assertTrue("2.1", !workspace.validateFiltered(bar).isOK());
		assertTrue("2.2", workspace.validateFiltered(foo).isOK());
	}

	/**
	 * Tests the creation of two different filters on a linked folder and the original.
	 * Regression for bug 267201
	 */
	public void testCreateFilterOnLinkedFolderAndTarget() throws Exception {
		IPath location = existingFolderInExistingFolder.getLocation();
		IFolder folder = nonExistingFolderInExistingProject;

		folder.createLink(location, IResource.NONE, getMonitor());

		FileInfoMatcherDescription matcherDescription1 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, ".*\\.cpp");
		FileInfoMatcherDescription matcherDescription2 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, ".*\\.h");

		folder.createFilter(IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FILES,
				matcherDescription1, 0, getMonitor());
		IResourceFilterDescription filterDescription2 = existingFolderInExistingFolder.createFilter(
				IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FILES, matcherDescription2, 0,
				getMonitor());

		IResource members[] = folder.members();
		assertEquals("1.2", members.length, 0);

		members = existingFolderInExistingFolder.members();
		assertEquals("1.4", members.length, 0);

		IFile newFile = existingFolderInExistingFolder.getFile("foo.cpp");
		assertTrue("1.5", newFile.getLocation().toFile().createNewFile());

		existingFolderInExistingFolder.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		members = existingFolderInExistingFolder.members();
		assertEquals("1.9", members.length, 1);
		assertEquals("2.0", members[0].getType(), IResource.FILE);
		assertEquals("2.1", members[0].getName(), "foo.cpp");

		members = folder.members();
		assertEquals("2.3", members.length, 0);

		newFile = existingFolderInExistingFolder.getFile("foo.h");
		assertTrue("2.5", newFile.getLocation().toFile().createNewFile());

		existingFolderInExistingFolder.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		members = existingFolderInExistingFolder.members();
		assertEquals("2.9", members.length, 1);
		assertEquals("3.0", members[0].getType(), IResource.FILE);
		assertEquals("3.1", members[0].getName(), "foo.cpp");

		members = folder.members();
		assertEquals("3.3", members.length, 0);

		folder.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		members = existingFolderInExistingFolder.members();
		assertEquals("3.9", members.length, 1);
		assertEquals("4.0", members[0].getType(), IResource.FILE);
		assertEquals("4.1", members[0].getName(), "foo.cpp");

		members = folder.members();
		assertEquals("4.3", members.length, 1);
		assertEquals("4.4", members[0].getType(), IResource.FILE);
		assertEquals("4.5", members[0].getName(), "foo.h");

		// create a file that shows under both
		newFile = existingFolderInExistingFolder.getFile("foo.text");
		assertTrue("5.5", newFile.getLocation().toFile().createNewFile());

		existingFolderInExistingFolder.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		members = existingFolderInExistingFolder.members();
		assertEquals("5.9", members.length, 2);
		assertEquals("6.0", members[0].getType(), IResource.FILE);
		assertEquals("6.1", members[0].getName(), "foo.cpp");
		assertEquals("6.2", members[1].getType(), IResource.FILE);
		assertEquals("6.3", members[1].getName(), "foo.text");

		members = folder.members();
		assertEquals("6.5", members.length, 2);
		assertEquals("6.6", members[0].getType(), IResource.FILE);
		assertEquals("6.7", members[0].getName(), "foo.h");
		assertEquals("6.8", members[1].getType(), IResource.FILE);
		assertEquals("6.9", members[1].getName(), "foo.text");

		// delete the common file
		newFile.delete(true, getMonitor());
		members = existingFolderInExistingFolder.members();
		assertEquals("7.2", members.length, 1);
		assertEquals("7.3", members[0].getType(), IResource.FILE);
		assertEquals("7.4", members[0].getName(), "foo.cpp");

		members = folder.members();
		assertEquals("7.6", members.length, 1);
		assertEquals("7.7", members[0].getType(), IResource.FILE);
		assertEquals("7.8", members[0].getName(), "foo.h");

		// remove the first filter
		filterDescription2.delete(0, getMonitor());
		members = existingFolderInExistingFolder.members();
		assertEquals("8.2", members.length, 2);
		assertEquals("8.3", members[0].getType(), IResource.FILE);
		assertEquals("8.4", members[0].getName(), "foo.cpp");
		assertEquals("8.4.1", members[1].getType(), IResource.FILE);
		assertEquals("8.4.2", members[1].getName(), "foo.h");

		members = folder.members();
		assertEquals("8.6", members.length, 1);
		assertEquals("8.7", members[0].getType(), IResource.FILE);
		assertEquals("8.8", members[0].getName(), "foo.h");

		// add the filter again
		existingFolderInExistingFolder.createFilter(
				IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FILES, matcherDescription2, 0,
				getMonitor());
		members = existingFolderInExistingFolder.members();
		assertEquals("9.2", members.length, 1);
		assertEquals("9.3", members[0].getType(), IResource.FILE);
		assertEquals("9.4", members[0].getName(), "foo.cpp");

		members = folder.members();
		assertEquals("9.6", members.length, 1);
		assertEquals("9.7", members[0].getType(), IResource.FILE);
		assertEquals("9.8", members[0].getName(), "foo.h");
	}

	public void testIResource_isFiltered() throws CoreException {
		IFolder folder = existingFolderInExistingProject.getFolder("virtual_folder.txt");
		IFile file = existingFolderInExistingProject.getFile("linked_file.txt");

		folder.create(IResource.VIRTUAL, true, getMonitor());
		file.createLink(existingFileInExistingProject.getLocation(), 0, getMonitor());

		FileInfoMatcherDescription matcherDescription = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, ".*\\.txt");
		existingFolderInExistingProject.createFilter(IResourceFilterDescription.EXCLUDE_ALL, matcherDescription, 0,
				getMonitor());

		IWorkspace workspace = existingProject.getWorkspace();
		assertTrue("1.0", workspace.validateFiltered(folder).isOK());
		assertTrue("1.1", workspace.validateFiltered(file).isOK());
	}

	/**
	 * Tests the creation of two different filters on a linked folder and the original.
	 * Check that creating and modifying files in the workspace doesn't make them appear in
	 * excluded locations.
	 * Regression for bug 267201
	 */
	public void testCreateFilterOnLinkedFolderAndTarget2() throws CoreException {
		final IPath location = existingFolderInExistingFolder.getLocation();
		final IFolder folder = nonExistingFolderInExistingProject;

		folder.createLink(location, IResource.NONE, getMonitor());

		FileInfoMatcherDescription matcherDescription1 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, ".*\\.h");
		FileInfoMatcherDescription matcherDescription2 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, ".*\\.cpp");

		folder.createFilter(IResourceFilterDescription.INCLUDE_ONLY | IResourceFilterDescription.FILES,
				matcherDescription1, 0, getMonitor());
		existingFolderInExistingFolder.createFilter(
				IResourceFilterDescription.INCLUDE_ONLY | IResourceFilterDescription.FILES, matcherDescription2, 0,
				getMonitor());


		// Create 'foo.cpp' in existingFolder...
		IFile newFile = existingFolderInExistingFolder.getFile("foo.cpp");
		create(newFile, true);
		IResource[] members = existingFolderInExistingFolder.members();
		assertEquals("1.9", 1, members.length);
		assertEquals("2.0", IResource.FILE, members[0].getType());
		assertEquals("2.1", "foo.cpp", members[0].getName());

		// Create a 'foo.h' under folder
		newFile = folder.getFile("foo.h");
		create(newFile, true);
		// Check that foo.h has appeared in 'folder'
		// // Refreshing restores sanity (hides the .cpp files)...
		// folder.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		members = folder.members();
		assertEquals("4.3", 1, members.length);
		assertEquals("4.4", IResource.FILE, members[0].getType());
		assertEquals("4.5", "foo.h", members[0].getName());

		// Check it hasn't appeared in existingFolder...
		// // Refresh restores sanity...
		// existingFolderInExistingFolder.refreshLocal(IResource.DEPTH_INFINITE,
		// getMonitor());
		members = existingFolderInExistingFolder.members();
		assertEquals("2.9", 1, members.length);
		assertEquals("3.0", IResource.FILE, members[0].getType());
		assertEquals("3.1", "foo.cpp", members[0].getName());
		// And refreshing doesn't change things
		existingFolderInExistingFolder.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		members = existingFolderInExistingFolder.members();
		assertEquals("2.9", 1, members.length);
		assertEquals("3.0", IResource.FILE, members[0].getType());
		assertEquals("3.1", "foo.cpp", members[0].getName());

		// Check modifying foo.h doesn't make it appear
		modifyInWorkspace(folder.getFile("foo.h"));
		members = existingFolderInExistingFolder.members();
		assertEquals("2.9", 1, members.length);
		assertEquals("3.0", IResource.FILE, members[0].getType());
		assertEquals("3.1", "foo.cpp", members[0].getName());
	}

	/**
	 * Tests that filtering a child directory which is linked from
	 * else where works
	 *
	 * Main tree:
	 * existingProject/existingFolderInExsitingProject/existingFolderInExistingFolder
	 * Links:
	 * otherExistingProject/nonExistingFolderInOtherExistingProject =&gt; existingProject/existingFolderInExsitingProject  (filter * of type folder)
	 * otherExistingProject/nonExistingFolder2InOtherExistingProject =&gt; existingProject/existingFolderInExsitingProject/existingFolderInExistingFolder
	 * This is a regression test for Bug 268518.
	 */
	public void testCreateFilterOnLinkedFolderWithAlias() throws Exception {
		final IProject project = otherExistingProject;
		final IPath parentLoc = existingFolderInExistingProject.getLocation();
		final IPath childLoc = existingFolderInExistingFolder.getLocation();
		final IFolder folder1 = nonExistingFolderInOtherExistingProject;
		final IFolder folder2 = nonExistingFolder2InOtherExistingProject;

		assertTrue("0.1", !folder1.exists());
		assertTrue("0.2", !folder2.exists());
		assertTrue("0.3", parentLoc.isPrefixOf(childLoc));

		FileInfoMatcherDescription matcherDescription1 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, ".*");

		// Filter out all children from existingFolderInExistingProject
		IResourceFilterDescription filterDescription1 = folder1.createFilter(
				IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FOLDERS, matcherDescription1, 0,
				getMonitor());

		folder1.createLink(parentLoc, IResource.NONE, getMonitor());
		folder2.createLink(childLoc, IResource.NONE, getMonitor());
		existingProject.close(getMonitor());

		assertTrue("12.0", folder1.exists());
		assertTrue("12.2", folder2.exists());
		assertTrue("12.4", folder1.isLinked());
		assertTrue("12.6", folder2.isLinked());
		assertTrue("12.8", folder1.getLocation().equals(parentLoc));
		assertTrue("12.10", folder2.getLocation().equals(childLoc));
		assertTrue("12.12", folder1.members().length == 0);
		assertTrue("12.14", folder2.members().length == 0);

		// Need to unset M_USED on the project's resource info, or
		// reconcileLinks will never be called...
		Workspace workspace = ((Workspace) ResourcesPlugin.getWorkspace());
		try {
			workspace.prepareOperation(project, getMonitor());
			workspace.beginOperation(true);

			ResourceInfo ri = ((Resource) project).getResourceInfo(false, true);
			ri.clear(ICoreConstants.M_USED);
		} finally {
			workspace.endOperation(project, true);
		}

		project.close(getMonitor());
		assertTrue("3.1", !project.isOpen());
		// Create a file under existingFolderInExistingFolder
		createFileInFileSystem(childLoc.append("foo"));
		// Reopen the project
		project.open(IResource.NONE, getMonitor());

		assertTrue("22.0", folder1.exists());
		assertTrue("22.2", folder2.exists());
		assertTrue("22.4", folder1.isLinked());
		assertTrue("22.6", folder2.isLinked());
		assertTrue("22.8", folder1.getLocation().equals(parentLoc));
		assertTrue("22.10", folder2.getLocation().equals(childLoc));
		assertTrue("22.12", folder1.members().length == 0);
		assertTrue("22.12", folder2.members().length == 1);

		// Swap the links around, loading may be order independent...
		folder2.createLink(parentLoc, IResource.REPLACE, getMonitor());
		folder1.createLink(childLoc, IResource.REPLACE | IResource.FORCE, getMonitor());

		// Filter out all children from existingFolderInExistingProject
		folder2.createFilter(IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FOLDERS,
				matcherDescription1, 0, getMonitor());
		filterDescription1.delete(0, getMonitor());
		assertTrue(folder1.getFilters().length == 0);

		// Need to unset M_USED on the project's resource info, or
		// reconcileLinks will never be called...
		try {
			workspace.prepareOperation(project, getMonitor());
			workspace.beginOperation(true);

			ResourceInfo ri = ((Resource) project).getResourceInfo(false, true);
			ri.clear(ICoreConstants.M_USED);
		} finally {
			workspace.endOperation(project, true);
		}

		project.close(getMonitor());
		assertTrue("3.1", !project.isOpen());
		project.open(IResource.NONE, getMonitor());

		assertTrue("32.0", folder1.exists());
		assertTrue("32.2", folder2.exists());
		assertTrue("32.4", folder1.isLinked());
		assertTrue("32.6", folder2.isLinked());
		assertTrue("32.8", folder2.getLocation().equals(parentLoc));
		assertTrue("32.10", folder1.getLocation().equals(childLoc));
		assertTrue("32.12", folder1.members().length == 1);
		assertTrue("32.12", folder2.members().length == 0);
	}

	/**
	 * Tests that filtering a child directory which is linked from
	 * else where works
	 *
	 * Main tree:
	 * existingProject/existingFolderInExsitingProject/existingFolderInExistingFolder
	 * Links:
	 * otherExistingProject/nonExistingFolderInOtherExistingProject =&gt; existingProject/existingFolderInExsitingProject  (filter * of type folder)
	 * otherExistingProject/nonExistingFolder2InOtherExistingProject =&gt; existingProject/existingFolderInExsitingProject/existingFolderInExistingFolder
	 * This is a regression test for Bug 268518.
	 */
	public void testCreateFilterOnLinkedFolderWithAlias2() throws CoreException {
		final IProject project = otherExistingProject;
		final IPath parentLoc = existingFolderInExistingProject.getLocation();
		final IPath childLoc = existingFolderInExistingFolder.getLocation();
		final IFolder folder1 = nonExistingFolderInOtherExistingProject;
		final IFolder folder2 = nonExistingFolder2InOtherExistingProject;

		assertTrue("0.1", !folder1.exists());
		assertTrue("0.2", !folder2.exists());
		assertTrue("0.3", parentLoc.isPrefixOf(childLoc));

		FileInfoMatcherDescription matcherDescription1 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, ".*");

		// Filter out all children from existingFolderInExistingProject
		IResourceFilterDescription filterDescription1 = folder1.createFilter(
				IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FOLDERS, matcherDescription1, 0,
				getMonitor());

		folder1.createLink(parentLoc, IResource.NONE, getMonitor());
		folder2.createLink(childLoc, IResource.NONE, getMonitor());
		existingProject.close(getMonitor());

		assertTrue("12.0", folder1.exists());
		assertTrue("12.2", folder2.exists());
		assertTrue("12.4", folder1.isLinked());
		assertTrue("12.6", folder2.isLinked());
		assertTrue("12.8", folder1.getLocation().equals(parentLoc));
		assertTrue("12.10", folder2.getLocation().equals(childLoc));
		assertTrue("12.12", folder1.members().length == 0);
		assertTrue("12.14", folder2.members().length == 0);

		// Need to unset M_USED on the project's resource info, or
		// reconcileLinks will never be called...
		Workspace workspace = ((Workspace) ResourcesPlugin.getWorkspace());
		try {
			workspace.prepareOperation(project, getMonitor());
			workspace.beginOperation(true);

			ResourceInfo ri = ((Resource) project).getResourceInfo(false, true);
			ri.clear(ICoreConstants.M_USED);
		} finally {
			workspace.endOperation(project, true);
		}

		// Create a file under existingFolderInExistingFolder
		create(folder2.getFile("foo"), true);

		assertTrue("22.0", folder1.exists());
		assertTrue("22.2", folder2.exists());
		assertTrue("22.4", folder1.isLinked());
		assertTrue("22.6", folder2.isLinked());
		assertTrue("22.8", folder1.getLocation().equals(parentLoc));
		assertTrue("22.10", folder2.getLocation().equals(childLoc));
		assertTrue("22.12", folder1.members().length == 0);
		assertTrue("22.12", folder2.members().length == 1);

		// Swap the links around, loading may be order independent...
		folder2.createLink(parentLoc, IResource.REPLACE | IResource.NONE, getMonitor());
		folder1.createLink(childLoc, IResource.REPLACE | IResource.FORCE, getMonitor());

		// Filter out all children from existingFolderInExistingProject
		folder2.createFilter(IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FOLDERS,
				matcherDescription1, 0, getMonitor());
		filterDescription1.delete(0, getMonitor());
		assertTrue(folder1.getFilters().length == 0);

		assertTrue("32.0", folder1.exists());
		assertTrue("32.2", folder2.exists());
		assertTrue("32.4", folder1.isLinked());
		assertTrue("32.6", folder2.isLinked());
		assertTrue("32.8", folder2.getLocation().equals(parentLoc));
		assertTrue("32.10", folder1.getLocation().equals(childLoc));
		assertTrue("32.12", folder1.members().length == 1);
		assertTrue("32.12", folder2.members().length == 0);
	}

	/**
	 * Tests the creation of a simple filter on a linked folder before the resource creation.
	 */
	public void testCreateFilterOnLinkedFolderBeforeCreation() throws CoreException {
		IPath location = existingFolderInExistingFolder.getLocation();
		IFolder folder = nonExistingFolderInExistingProject;

		assertTrue("0.1", !folder.exists());

		FileInfoMatcherDescription matcherDescription1 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, "foo");

		folder.createFilter(IResourceFilterDescription.INCLUDE_ONLY | IResourceFilterDescription.FILES,
				matcherDescription1, 0, getMonitor());
		folder.createLink(location, IResource.NONE, getMonitor());

		IFile foo = folder.getFile("foo");
		IFile bar = folder.getFile("bar");

		ensureExistsInWorkspace(new IResource[] {foo, bar}, true);

		existingProject.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());

		//close and reopen the project
		existingProject.close(getMonitor());
		existingProject.open(getMonitor());

		IResourceFilterDescription[] filters = folder.getFilters();

		assertEquals("1.4", filters.length, 1);
		assertEquals("1.5", filters[0].getFileInfoMatcherDescription().getId(), REGEX_FILTER_PROVIDER);
		assertEquals("1.6", filters[0].getFileInfoMatcherDescription().getArguments(), "foo");
		assertEquals("1.7", filters[0].getType(), IResourceFilterDescription.INCLUDE_ONLY | IResourceFilterDescription.FILES);
		assertEquals("1.8", filters[0].getResource(), folder);

		IResource members[] = folder.members();
		assertEquals("2.0", members.length, 1);
		assertEquals("2.1", members[0].getType(), IResource.FILE);
		assertEquals("2.2", members[0].getName(), "foo");
	}

	/**
	 * Tests the creation and removal of a simple filter on a folder.
	 */
	public void testCreateAndRemoveFilterOnFolder() throws CoreException {
		FileInfoMatcherDescription matcherDescription1 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, "foo");
		IResourceFilterDescription filterDescription = existingFolderInExistingFolder
				.createFilter(IResourceFilterDescription.INCLUDE_ONLY | IResourceFilterDescription.FILES
						| IResourceFilterDescription.FOLDERS, matcherDescription1, 0, getMonitor());

		IFile foo = existingFolderInExistingFolder.getFile("foo");
		IFile bar = existingFolderInExistingFolder.getFile("bar");

		ensureExistsInWorkspace(new IResource[] {foo, bar}, true);

		existingProject.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());

		//close and reopen the project
		existingProject.close(getMonitor());
		existingProject.open(getMonitor());

		filterDescription.delete(0, getMonitor());

		//close and reopen the project
		existingProject.close(getMonitor());
		existingProject.open(getMonitor());

		IResourceFilterDescription[] filters = existingFolderInExistingFolder.getFilters();

		assertEquals("1.6", filters.length, 0);

		IResource members[] = existingFolderInExistingFolder.members();
		assertEquals("1.8", members.length, 2);
		assertEquals("1.9", members[0].getType(), IResource.FILE);
		assertEquals("2.0", members[0].getName(), "bar");
		assertEquals("2.1", members[1].getType(), IResource.FILE);
		assertEquals("2.2", members[1].getName(), "foo");
	}

	/**
	 * Tests the creation and removal of a simple filter on a folder.
	 */
	public void testCreateAndRemoveFilterOnFolderWithoutClosingProject() throws CoreException {
		FileInfoMatcherDescription matcherDescription1 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, "foo");
		IResourceFilterDescription filterDescription = existingFolderInExistingFolder
				.createFilter(IResourceFilterDescription.INCLUDE_ONLY | IResourceFilterDescription.FILES
						| IResourceFilterDescription.FOLDERS, matcherDescription1, 0, getMonitor());

		IFile foo = existingFolderInExistingFolder.getFile("foo");
		IFile bar = existingFolderInExistingFolder.getFile("bar");

		ensureExistsInWorkspace(new IResource[] {foo, bar}, true);

		existingProject.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		filterDescription.delete(0, getMonitor());

		IResourceFilterDescription[] filters = existingFolderInExistingFolder.getFilters();
		assertEquals("1.6", filters.length, 0);

		IResource members[] = existingFolderInExistingFolder.members();
		assertEquals("1.8", members.length, 2);
		assertEquals("1.9", members[0].getType(), IResource.FILE);
		assertEquals("2.0", members[0].getName(), "bar");
		assertEquals("2.1", members[1].getType(), IResource.FILE);
		assertEquals("2.2", members[1].getName(), "foo");
	}

	/**
	 * Tests the creation of the include-only filter.
	 */
	public void testIncludeOnlyFilter() throws CoreException {
		FileInfoMatcherDescription matcherDescription1 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, ".*\\.c");

		existingFolderInExistingProject.createFilter(IResourceFilterDescription.INCLUDE_ONLY
				| IResourceFilterDescription.FILES | IResourceFilterDescription.FOLDERS, matcherDescription1, 0,
				getMonitor());

		IFile foo = existingFolderInExistingProject.getFile("foo.c");
		IFile file = existingFolderInExistingProject.getFile("file.c");
		IFile bar = existingFolderInExistingProject.getFile("bar.h");

		ensureExistsInWorkspace(new IResource[] {foo, bar, file}, true);
		existingProject.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());

		IResource members[] = existingFolderInExistingProject.members();
		assertEquals("2.0", members.length, 2);
		assertEquals("2.1", members[0].getType(), IResource.FILE);
		assertEquals("2.2", members[0].getName(), "file.c");
		assertEquals("2.3", members[1].getType(), IResource.FILE);
		assertEquals("2.4", members[1].getName(), "foo.c");

		FileInfoMatcherDescription matcherDescription2 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, ".*\\.c");
		existingFolderInExistingProject.createFilter(IResourceFilterDescription.INCLUDE_ONLY
				| IResourceFilterDescription.FILES | IResourceFilterDescription.FOLDERS, matcherDescription2, 0,
				getMonitor());

		existingProject.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		members = existingFolderInExistingProject.members();
		assertEquals("3.3", members.length, 2);
		assertEquals("3.4", members[0].getType(), IResource.FILE);
		assertEquals("3.5", members[0].getName(), "file.c");
		assertEquals("3.6", members[1].getType(), IResource.FILE);
		assertEquals("3.7", members[1].getName(), "foo.c");
	}

	/**
	 * Tests the creation of the exclude-all filter.
	 */
	public void testExcludeAllFilter() throws CoreException {
		FileInfoMatcherDescription matcherDescription1 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, ".*\\.c");

		existingFolderInExistingFolder.createFilter(IResourceFilterDescription.EXCLUDE_ALL
				| IResourceFilterDescription.FILES | IResourceFilterDescription.FOLDERS, matcherDescription1, 0,
				getMonitor());

		IFile foo = existingFolderInExistingFolder.getFile("foo.c");
		IFile file = existingFolderInExistingFolder.getFile("file.c");
		IFile fooh = existingFolderInExistingFolder.getFile("foo.h");
		IFile bar = existingFolderInExistingFolder.getFile("bar.h");

		ensureExistsInWorkspace(new IResource[] {foo, bar, file, fooh}, true);
		existingProject.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());

		IResource members[] = existingFolderInExistingFolder.members();
		assertEquals("2.0", members.length, 2);
		assertEquals("2.1", members[0].getType(), IResource.FILE);
		assertEquals("2.2", members[0].getName(), "bar.h");
		assertEquals("2.3", members[1].getType(), IResource.FILE);
		assertEquals("2.4", members[1].getName(), "foo.h");

		FileInfoMatcherDescription matcherDescription2 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, "foo.*");
		existingFolderInExistingFolder.createFilter(IResourceFilterDescription.EXCLUDE_ALL
				| IResourceFilterDescription.FILES | IResourceFilterDescription.FOLDERS, matcherDescription2, 0,
				getMonitor());
		existingProject.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());

		members = existingFolderInExistingFolder.members();
		assertEquals("3.3", members.length, 1);
		assertEquals("3.4", members[0].getType(), IResource.FILE);
		assertEquals("3.5", members[0].getName(), "bar.h");
	}

	/**
	 * Tests the creation of the mixed include-only exclude-all filter.
	 */
	public void testMixedFilter() throws CoreException {
		FileInfoMatcherDescription matcherDescription1 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, ".*\\.c");
		FileInfoMatcherDescription matcherDescription2 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, "foo.*");

		existingFolderInExistingProject.createFilter(IResourceFilterDescription.INCLUDE_ONLY
				| IResourceFilterDescription.FILES | IResourceFilterDescription.FOLDERS, matcherDescription1, 0,
				getMonitor());
		existingFolderInExistingProject.createFilter(IResourceFilterDescription.EXCLUDE_ALL
				| IResourceFilterDescription.FILES | IResourceFilterDescription.FOLDERS, matcherDescription2, 0,
				getMonitor());

		IFile foo = existingFolderInExistingProject.getFile("foo.c");
		IFile file = existingFolderInExistingProject.getFile("file.c");
		IFile fooh = existingFolderInExistingProject.getFile("foo.h");
		IFile bar = existingFolderInExistingProject.getFile("bar.h");

		ensureExistsInWorkspace(new IResource[] {foo, bar, file, fooh}, true);
		existingProject.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());

		IResource members[] = existingFolderInExistingProject.members();
		assertEquals("2.0", members.length, 1);
		assertEquals("2.1", members[0].getType(), IResource.FILE);
		assertEquals("2.2", members[0].getName(), "file.c");
	}

	/**
	 * Tests the creation of inheritable filter.
	 */
	public void testInheritedFilter() throws CoreException {
		FileInfoMatcherDescription matcherDescription1 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, ".*\\.c");
		FileInfoMatcherDescription matcherDescription2 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, "foo.*");

		existingProject.createFilter(IResourceFilterDescription.INCLUDE_ONLY | IResourceFilterDescription.INHERITABLE
				| IResourceFilterDescription.FILES, matcherDescription1, 0, getMonitor());
		existingFolderInExistingFolder.createFilter(IResourceFilterDescription.EXCLUDE_ALL
				| IResourceFilterDescription.FILES | IResourceFilterDescription.FOLDERS, matcherDescription2, 0,
				getMonitor());

		IFile foo = existingFolderInExistingFolder.getFile("foo.c");
		IFile file = existingFolderInExistingFolder.getFile("file.c");
		IFile fooh = existingFolderInExistingFolder.getFile("foo.h");
		IFile bar = existingFolderInExistingFolder.getFile("bar.h");

		ensureExistsInWorkspace(new IResource[] {foo, bar, file, fooh}, true);
		existingProject.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());

		IResource members[] = existingFolderInExistingFolder.members();
		assertEquals("2.0", members.length, 1);
		assertEquals("2.1", members[0].getType(), IResource.FILE);
		assertEquals("2.2", members[0].getName(), "file.c");
	}

	/**
	 * Tests the creation of FOLDER filter.
	 */
	public void testFolderOnlyFilters() throws CoreException {
		FileInfoMatcherDescription matcherDescription1 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, "foo.*");
		existingFolderInExistingFolder.createFilter(
				IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FOLDERS, matcherDescription1, 0,
				getMonitor());

		IFile foo = existingFolderInExistingFolder.getFile("foo.c");
		IFolder food = existingFolderInExistingFolder.getFolder("foo.d");

		ensureExistsInWorkspace(new IResource[] {foo, food}, true);
		existingProject.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());

		IResource members[] = existingFolderInExistingFolder.members();
		assertEquals("2.0", members.length, 1);
		assertEquals("2.1", members[0].getType(), IResource.FILE);
		assertEquals("2.2", members[0].getName(), "foo.c");
	}

	/**
	 * Tests the creation of FILE filter.
	 */
	public void testFileOnlyFilters() throws CoreException {
		FileInfoMatcherDescription matcherDescription1 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, "foo.*");
		existingFolderInExistingFolder.createFilter(
				IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FILES, matcherDescription1, 0,
				getMonitor());

		IFile foo = existingFolderInExistingFolder.getFile("foo.c");
		IFolder food = existingFolderInExistingFolder.getFolder("foo.d");

		ensureExistsInWorkspace(new IResource[] {foo, food}, true);
		existingProject.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());

		IResource members[] = existingFolderInExistingFolder.members();
		assertEquals("2.0", members.length, 1);
		assertEquals("2.1", members[0].getType(), IResource.FOLDER);
		assertEquals("2.2", members[0].getName(), "foo.d");
	}

	/**
	 * Tests moving a folder with filters.
	 */
	public void testMoveFolderWithFilterToAnotherProject() throws CoreException {
		FileInfoMatcherDescription matcherDescription1 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, "foo.*");
		existingFolderInExistingProject.createFilter(
				IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FILES, matcherDescription1, 0,
				getMonitor());

		IFile foo = existingFolderInExistingProject.getFile("foo.c");
		IFolder food = existingFolderInExistingProject.getFolder("foo.d");

		ensureExistsInWorkspace(new IResource[] {foo, food}, true);
		existingProject.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());

		IFolder destination = otherExistingProject.getFolder("destination");
		existingFolderInExistingProject.move(destination.getFullPath(), 0, getMonitor());

		IResourceFilterDescription[] filters = existingFolderInExistingProject.getFilters();
		assertEquals("1.4", filters.length, 0);

		filters = destination.getFilters();
		assertEquals("1.6", filters.length, 1);
		assertEquals("1.7", filters[0].getFileInfoMatcherDescription().getId(), REGEX_FILTER_PROVIDER);
		assertEquals("1.8", filters[0].getFileInfoMatcherDescription().getArguments(), "foo.*");
		assertEquals("1.9", filters[0].getType(), IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FILES);
		assertEquals("2.0", filters[0].getResource(), destination);
	}

	/**
	 * Tests copying a folder with filters.
	 */
	public void testCopyFolderWithFilterToAnotherProject() throws CoreException {
		FileInfoMatcherDescription matcherDescription1 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, "foo.*");
		existingFolderInExistingProject.createFilter(
				IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FILES, matcherDescription1, 0,
				getMonitor());

		IFile foo = existingFolderInExistingProject.getFile("foo.c");
		IFolder food = existingFolderInExistingProject.getFolder("foo.d");

		ensureExistsInWorkspace(new IResource[] {foo, food}, true);
		existingProject.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());

		IFolder destination = otherExistingProject.getFolder("destination");
		existingFolderInExistingProject.copy(destination.getFullPath(), 0, getMonitor());

		IResourceFilterDescription[] filters = existingFolderInExistingProject.getFilters();
		assertEquals("1.4", filters.length, 1);
		assertEquals("1.5", filters[0].getFileInfoMatcherDescription().getId(), REGEX_FILTER_PROVIDER);
		assertEquals("1.6", filters[0].getFileInfoMatcherDescription().getArguments(), "foo.*");
		assertEquals("1.7", filters[0].getType(), IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FILES);
		assertEquals("1.8", filters[0].getResource(), existingFolderInExistingProject);

		filters = destination.getFilters();
		assertEquals("2.1", filters.length, 1);
		assertEquals("2.2", filters[0].getFileInfoMatcherDescription().getId(), REGEX_FILTER_PROVIDER);
		assertEquals("2.3", filters[0].getFileInfoMatcherDescription().getArguments(), "foo.*");
		assertEquals("2.4", filters[0].getType(), IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FILES);
		assertEquals("2.5", filters[0].getResource(), destination);
	}

	/**
	 * Tests copying a folder with filters to another folder.
	 */
	public void testCopyFolderWithFilterToAnotherFolder() throws CoreException {
		FileInfoMatcherDescription matcherDescription1 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, "foo.*");
		existingFolderInExistingProject.createFilter(
				IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FILES, matcherDescription1, 0,
				getMonitor());

		IFile foo = existingFolderInExistingProject.getFile("foo.c");
		IFolder food = existingFolderInExistingProject.getFolder("foo.d");

		ensureExistsInWorkspace(new IResource[] {foo, food}, true);
		existingProject.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());

		ensureExistsInWorkspace(new IResource[] {nonExistingFolderInExistingProject}, true);
		IFolder destination = nonExistingFolderInExistingProject.getFolder("destination");
		existingFolderInExistingProject.copy(destination.getFullPath(), 0, getMonitor());

		IResourceFilterDescription[] filters = existingFolderInExistingProject.getFilters();
		assertEquals("1.4", filters.length, 1);
		assertEquals("1.5", filters[0].getFileInfoMatcherDescription().getId(), REGEX_FILTER_PROVIDER);
		assertEquals("1.6", filters[0].getFileInfoMatcherDescription().getArguments(), "foo.*");
		assertEquals("1.7", filters[0].getType(), IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FILES);
		assertEquals("1.8", filters[0].getResource(), existingFolderInExistingProject);

		filters = destination.getFilters();
		assertEquals("2.1", filters.length, 1);
		assertEquals("2.2", filters[0].getFileInfoMatcherDescription().getId(), REGEX_FILTER_PROVIDER);
		assertEquals("2.3", filters[0].getFileInfoMatcherDescription().getArguments(), "foo.*");
		assertEquals("2.4", filters[0].getType(), IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FILES);
		assertEquals("2.5", filters[0].getResource(), destination);
	}

	/**
	 * Tests moving a folder with filters to another folder.
	 */
	public void testMoveFolderWithFilterToAnotherFolder() throws CoreException {
		FileInfoMatcherDescription matcherDescription1 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, "foo.*");
		existingFolderInExistingProject.createFilter(
				IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FILES, matcherDescription1, 0,
				getMonitor());

		IFile foo = existingFolderInExistingProject.getFile("foo.c");
		IFolder food = existingFolderInExistingProject.getFolder("foo.d");

		ensureExistsInWorkspace(new IResource[] {foo, food}, true);
		existingProject.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());

		ensureExistsInWorkspace(new IResource[] {nonExistingFolderInExistingProject}, true);
		IFolder destination = nonExistingFolderInExistingProject.getFolder("destination");
		existingFolderInExistingProject.move(destination.getFullPath(), 0, getMonitor());

		IResourceFilterDescription[] filters = existingFolderInExistingProject.getFilters();
		assertEquals("1.4", filters.length, 0);

		filters = destination.getFilters();
		assertEquals("2.1", filters.length, 1);
		assertEquals("2.2", filters[0].getFileInfoMatcherDescription().getId(), REGEX_FILTER_PROVIDER);
		assertEquals("2.3", filters[0].getFileInfoMatcherDescription().getArguments(), "foo.*");
		assertEquals("2.4", filters[0].getType(), IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FILES);
		assertEquals("2.5", filters[0].getResource(), destination);
	}

	/**
	 * Tests deleting a folder with filters.
	 */
	public void testDeleteFolderWithFilterToAnotherFolder() throws CoreException {
		FileInfoMatcherDescription matcherDescription1 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, "foo.*");
		FileInfoMatcherDescription matcherDescription2 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, ".*\\.c");

		existingFolderInExistingProject.createFilter(
				IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FILES, matcherDescription1, 0,
				getMonitor());
		existingFolderInExistingFolder.createFilter(
				IResourceFilterDescription.INCLUDE_ONLY | IResourceFilterDescription.FILES, matcherDescription2, 0,
				getMonitor());

		IFile foo = existingFolderInExistingProject.getFile("foo.c");
		IFolder food = existingFolderInExistingProject.getFolder("foo.d");

		ensureExistsInWorkspace(new IResource[] {foo, food}, true);
		existingProject.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());

		ensureExistsInWorkspace(new IResource[] {nonExistingFolderInExistingProject}, true);
		existingFolderInExistingProject.delete(0, getMonitor());

		IResourceFilterDescription[] filters = existingFolderInExistingProject.getFilters();
		assertEquals("1.4", filters.length, 0);

		filters = existingFolderInExistingFolder.getFilters();
		assertEquals("2.1", filters.length, 0);
	}

	/* Regression test for Bug 304276 */
	public void testInvalidCharactersInRegExFilter() {
		RegexFileInfoMatcher matcher = new RegexFileInfoMatcher();
		assertThrows(CoreException.class, () -> matcher.initialize(existingProject, "*:*"));
	}

	/**
	 * Regression test for Bug 302146
	 */
	public void testBug302146() throws Exception {
		FileInfoMatcherDescription matcherDescription = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, "foo");
		existingFolderInExistingProject.createFilter(IResourceFilterDescription.INCLUDE_ONLY
				| IResourceFilterDescription.FILES | IResourceFilterDescription.FOLDERS, matcherDescription, 0,
				getMonitor());

		// close and reopen the project
		existingProject.close(getMonitor());
		existingProject.open(getMonitor());
		IResourceFilterDescription[] filters = existingFolderInExistingProject.getFilters();

		// check that filters are recreated when the project is reopened
		// it means that .project was updated with filter details
		assertEquals("4.0", filters.length, 1);
		assertEquals("4.1", filters[0].getFileInfoMatcherDescription().getId(), REGEX_FILTER_PROVIDER);
		assertEquals("4.2", filters[0].getFileInfoMatcherDescription().getArguments(), "foo");
		assertEquals("4.3", filters[0].getType(), IResourceFilterDescription.INCLUDE_ONLY | IResourceFilterDescription.FILES | IResourceFilterDescription.FOLDERS);
		assertEquals("4.4", filters[0].getResource(), existingFolderInExistingProject);

		new ProjectDescriptionReader(existingProject).read(existingProject.getFile(".project").getLocation());
	}

	/**
	 * Regression for  Bug 317783 -  Resource filters do not work at all in "Project Explorer"
	 * The problem is that a client calls explicitly refreshLocal on a folder that is filtered out by
	 * resource filters and that doesn't exist in the workspace.  This used to cause the resource to
	 * appear in the workspace, along with all its children, in spite of active resource filters to the
	 * contrary.
	 */
	public void test317783() throws CoreException {
		IFolder folder = existingProject.getFolder("foo");
		ensureExistsInWorkspace(folder, true);

		IFile file = folder.getFile("bar.txt");
		ensureExistsInWorkspace(file, "content");

		existingProject.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		FileInfoMatcherDescription matcherDescription = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, ".*");
		existingProject.createFilter(IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FOLDERS,
				matcherDescription, 0, getMonitor());
		existingProject.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());

		IResource members[] = existingProject.members();
		assertEquals("1.5", 2, members.length);
		assertEquals("1.6", ".project", members[0].getName());
		assertEquals("1.7", existingFileInExistingProject.getName(), members[1].getName());

		folder.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		members = existingProject.members();
		assertEquals("2.2", 2, members.length);
		assertEquals("2.3", ".project", members[0].getName());
		assertEquals("2.4", existingFileInExistingProject.getName(), members[1].getName());

		assertEquals("2.5", false, folder.exists());
		assertEquals("2.6", false, file.exists());
	}

	/**
	 * Regression for  Bug 317824 -  Renaming a project that contains resource filters fails,
	 * and copying a project that contains resource filters removes the resource filters.
	 */
	public void test317824() throws CoreException {
		IFolder folder = existingProject.getFolder("foo");
		ensureExistsInWorkspace(folder, true);

		IFile file = folder.getFile("bar.txt");
		ensureExistsInWorkspace(file, "content");

		existingProject.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		FileInfoMatcherDescription matcherDescription = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, ".*");
		existingProject.createFilter(IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FOLDERS,
				matcherDescription, 0, getMonitor());
		assertEquals(1, existingProject.getFilters().length);

		existingProject.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());

		IPath newPath = existingProject.getFullPath().removeLastSegments(1).append(existingProject.getName() + "_moved");
		existingProject.move(newPath, true, getMonitor());

		IProject newProject = ResourcesPlugin.getWorkspace().getRoot().getProject(existingProject.getName() + "_moved");
		assertTrue(newProject.exists());
		assertEquals(1, newProject.getFilters().length);

		newPath = newProject.getFullPath().removeLastSegments(1).append(newProject.getName() + "_copy");
		newProject.copy(newPath, true, getMonitor());

		newProject = ResourcesPlugin.getWorkspace().getRoot().getProject(newProject.getName() + "_copy");
		assertTrue(newProject.exists());
		assertEquals(1, newProject.getFilters().length);
	}

	/**
	 * Regression test for bug 328464
	 */
	public void test328464() throws CoreException {
		IFolder folder = existingProject.getFolder(getUniqueString());
		ensureExistsInWorkspace(folder, true);

		IFile file_a_txt = folder.getFile("a.txt");
		ensureExistsInWorkspace(file_a_txt, true);

		FileInfoMatcherDescription matcherDescription = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER,
				"a\\.txt");
		existingProject.createFilter(IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FILES
				| IResourceFilterDescription.INHERITABLE, matcherDescription, 0, getMonitor());

		assertFalse("2.0", existingProject.getWorkspace().validateFiltered(file_a_txt).isOK());

		// rename a.txt to A.txt in the file system
		File ioFile = file_a_txt.getLocation().toFile();
		assertTrue("3.0", ioFile.exists());
		ioFile.renameTo(new File(file_a_txt.getLocation().removeLastSegments(1).append("A.txt").toString()));

		assertFalse("4.0", existingProject.getWorkspace().validateFiltered(file_a_txt).isOK());

		folder.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());

		assertFalse("6.0", file_a_txt.exists());
		assertFalse("7.0", existingProject.getWorkspace().validateFiltered(file_a_txt).isOK());

		IFile file_A_txt = folder.getFile("A.txt");
		assertTrue("9.0", file_A_txt.exists());
		assertTrue("10.0", existingProject.getWorkspace().validateFiltered(file_A_txt).isOK());
	}

	/**
	 * Regression test for bug 343914
	 */
	public void test343914() throws CoreException {
		String subProjectName = "subProject";
		IPath subProjectLocation = existingProject.getLocation().append(subProjectName);

		FileInfoMatcherDescription matcherDescription = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER,
				subProjectName);
		existingProject.createFilter(
				IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FOLDERS
						| IResourceFilterDescription.FILES | IResourceFilterDescription.INHERITABLE,
				matcherDescription, 0, getMonitor());

		IPath fileLocation = subProjectLocation.append("file.txt");

		IWorkspaceRoot root = getWorkspace().getRoot();
		IFile result = root.getFileForLocation(fileLocation);

		assertTrue("2.0", result == null);

		IFile[] results = root.findFilesForLocation(fileLocation);

		assertTrue("2.1", results.length == 0);

		IPath containerLocation = subProjectLocation.append("folder");
		IContainer resultContainer = root.getContainerForLocation(containerLocation);

		assertTrue("2.2", resultContainer == null);

		IContainer[] resultsContainer = root.findContainersForLocation(containerLocation);

		assertTrue("2.3", resultsContainer.length == 0);

		IProject subProject = root.getProject(subProjectName);

		IProjectDescription newProjectDescription = getWorkspace().newProjectDescription(subProjectName);
		newProjectDescription.setLocation(subProjectLocation);

		subProject.create(newProjectDescription, getMonitor());
		result = root.getFileForLocation(fileLocation);

		assertTrue("3.0", result != null);
		assertEquals("3.1", subProject, result.getProject());

		results = root.findFilesForLocation(fileLocation);
		assertTrue("3.2", results.length == 1);
		assertEquals("3.3", subProject, results[0].getProject());

		resultContainer = root.getContainerForLocation(containerLocation);

		assertTrue("3.4", resultContainer != null);
		assertEquals("3.5", subProject, resultContainer.getProject());

		resultsContainer = root.findContainersForLocation(containerLocation);

		assertTrue("3.6", resultsContainer.length == 1);
		assertEquals("3.7", subProject, resultsContainer[0].getProject());
	}
}