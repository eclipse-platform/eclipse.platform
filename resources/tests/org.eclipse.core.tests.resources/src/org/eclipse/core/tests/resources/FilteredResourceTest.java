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

import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.harness.FileSystemHelper.getRandomLocation;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createUniqueString;
import static org.eclipse.core.tests.resources.ResourceTestUtil.removeFromWorkspace;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests the following API methods:
 *  {@link IContainer#createFilter(int, FileInfoMatcherDescription, int, org.eclipse.core.runtime.IProgressMonitor)}
 *  {@link IContainer#getFilters()}
 *
 * This test tests resource filters with projects, folders, linked resource folders,
 * and moving those resources to different parents.
 */
public class FilteredResourceTest {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

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
		createInWorkspace(new IResource[] {existingProject, otherExistingProject, closedProject, existingFolderInExistingProject, existingFolderInExistingFolder, existingFileInExistingProject});
		closedProject.close(createTestMonitor());
		removeFromWorkspace(new IResource[] {nonExistingFolderInExistingProject, nonExistingFolderInExistingFolder, nonExistingFolderInOtherExistingProject, nonExistingFolder2InOtherExistingProject, nonExistingFileInExistingProject, nonExistingFileInOtherExistingProject, nonExistingFileInExistingFolder});
		resolve(localFolder).toFile().mkdirs();
		createInFileSystem(resolve(localFile));
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

	@Before
	public void setUp() throws Exception {
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
		workspaceRule.deleteOnTearDown(resolve(localFolder));
		localFile = localFolder.append(childName);
		doCleanup();
	}

	/**
	 * Tests the creation of a simple filter on a folder.
	 */
	@Test
	public void testCreateFilterOnFolder() throws CoreException {
		FileInfoMatcherDescription matcherDescription = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, "foo");
		existingFolderInExistingProject.createFilter(IResourceFilterDescription.INCLUDE_ONLY
				| IResourceFilterDescription.FILES | IResourceFilterDescription.FOLDERS, matcherDescription, 0,
				createTestMonitor());

		IFile foo = existingFolderInExistingProject.getFile("foo");
		IFile bar = existingFolderInExistingProject.getFile("bar");

		createInWorkspace(new IResource[] {foo, bar});

		existingProject.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());

		//close and reopen the project
		existingProject.close(createTestMonitor());
		existingProject.open(createTestMonitor());

		IResourceFilterDescription[] filters = existingFolderInExistingProject.getFilters();
		assertThat(filters).hasSize(1).satisfiesExactly(filter -> {
			assertThat(filter.getFileInfoMatcherDescription().getId()).as("provider").isEqualTo(REGEX_FILTER_PROVIDER);
			assertThat(filter.getFileInfoMatcherDescription().getArguments()).as("arguments").isEqualTo("foo");
			assertThat(filter.getType()).as("type").isEqualTo(IResourceFilterDescription.INCLUDE_ONLY
					| IResourceFilterDescription.FILES | IResourceFilterDescription.FOLDERS);
			assertThat(filter.getResource()).as("resource").isEqualTo(existingFolderInExistingProject);
		});

		IResource members[] = existingFolderInExistingProject.members();
		assertThat(members).hasSize(1).satisfiesExactly(member -> {
			assertThat(member.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(member.getName()).as("name").isEqualTo("foo");
		});

		IWorkspace workspace = existingProject.getWorkspace();
		assertThat(bar).matches(it -> !workspace.validateFiltered(it).isOK(), "is filtered");
		assertThat(foo).matches(it -> workspace.validateFiltered(it).isOK(), "is not filtered");
	}

	/**
	 * Tests the creation of a simple filter on a project.
	 */
	@Test
	public void testCreateFilterOnProject() throws CoreException {
		FileInfoMatcherDescription matcherDescription = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, "foo");
		existingProject.createFilter(IResourceFilterDescription.INCLUDE_ONLY | IResourceFilterDescription.FOLDERS,
				matcherDescription, 0, createTestMonitor());

		IFolder foo = existingProject.getFolder("foo");
		IFolder bar = existingProject.getFolder("bar");

		createInWorkspace(new IResource[] {foo, bar});

		existingProject.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());

		//close and reopen the project
		existingProject.close(createTestMonitor());
		existingProject.open(createTestMonitor());
		IResourceFilterDescription[] filters = existingProject.getFilters();

		assertThat(filters).hasSize(1).satisfiesExactly(filter -> {
			assertThat(filter.getFileInfoMatcherDescription().getId()).as("provider").isEqualTo(REGEX_FILTER_PROVIDER);
			assertThat(filter.getFileInfoMatcherDescription().getArguments()).as("arguments").isEqualTo("foo");
			assertThat(filter.getType()).as("type")
					.isEqualTo(IResourceFilterDescription.INCLUDE_ONLY | IResourceFilterDescription.FOLDERS);
			assertThat(filter.getResource()).as("resource").isEqualTo(existingProject);
		});

		IResource members[] = existingProject.members();
		assertThat(members).hasSize(3).filteredOn(member -> member.getType() == IResource.FOLDER).allSatisfy(member -> {
			assertThat(member.getType()).as("type").isEqualTo(IResource.FOLDER);
			assertThat(member.getName()).as("name").isEqualTo("foo");
		});

		IWorkspace workspace = existingProject.getWorkspace();
		assertThat(bar).matches(it -> !workspace.validateFiltered(it).isOK(), "is filtered");
		assertThat(foo).matches(it -> workspace.validateFiltered(it).isOK(), "is not filtered");
	}

	/**
	 * Tests the creation of a simple filter on a linked folder.
	 */
	@Test
	public void testCreateFilterOnLinkedFolder() throws CoreException {
		IPath location = getRandomLocation();
		IFolder folder = nonExistingFolderInExistingProject;

		//try to create without the flag (should fail)
		assertThrows(CoreException.class, () -> folder.createLink(location, IResource.NONE, createTestMonitor()));

		FileInfoMatcherDescription matcherDescription = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, "foo");
		folder.createFilter(IResourceFilterDescription.INCLUDE_ONLY | IResourceFilterDescription.FILES,
				matcherDescription, 0, createTestMonitor());

		IFile foo = folder.getFile("foo");
		IFile bar = folder.getFile("bar");

		createInWorkspace(new IResource[] {foo, bar});

		existingProject.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());

		//close and reopen the project
		existingProject.close(createTestMonitor());
		existingProject.open(createTestMonitor());

		IResourceFilterDescription[] filters = folder.getFilters();

		assertThat(filters).hasSize(1).satisfiesExactly(filter -> {
			assertThat(filter.getFileInfoMatcherDescription().getId()).as("provider").isEqualTo(REGEX_FILTER_PROVIDER);
			assertThat(filter.getFileInfoMatcherDescription().getArguments()).as("arguments").isEqualTo("foo");
			assertThat(filter.getType()).as("type")
					.isEqualTo(IResourceFilterDescription.INCLUDE_ONLY | IResourceFilterDescription.FILES);
			assertThat(filter.getResource()).as("resource").isEqualTo(folder);
		});

		IResource members[] = folder.members();
		assertThat(members).hasSize(1).satisfiesExactly(member -> {
			assertThat(member.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(member.getName()).as("name").isEqualTo("foo");
		});

		IWorkspace workspace = existingProject.getWorkspace();
		assertThat(bar).matches(it -> !workspace.validateFiltered(it).isOK(), "is filtered");
		assertThat(foo).matches(it -> workspace.validateFiltered(it).isOK(), "is not filtered");
	}

	/**
	 * Tests the creation of two different filters on a linked folder and the original.
	 * Regression for bug 267201
	 */
	@Test
	public void testCreateFilterOnLinkedFolderAndTarget() throws Exception {
		IPath location = existingFolderInExistingFolder.getLocation();
		IFolder folder = nonExistingFolderInExistingProject;

		folder.createLink(location, IResource.NONE, createTestMonitor());

		FileInfoMatcherDescription matcherDescription1 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, ".*\\.cpp");
		FileInfoMatcherDescription matcherDescription2 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, ".*\\.h");

		folder.createFilter(IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FILES,
				matcherDescription1, 0, createTestMonitor());
		IResourceFilterDescription filterDescription2 = existingFolderInExistingFolder.createFilter(
				IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FILES, matcherDescription2, 0,
				createTestMonitor());

		IResource members[] = folder.members();
		assertThat(members).isEmpty();

		members = existingFolderInExistingFolder.members();
		assertThat(members).isEmpty();

		IFile newFile = existingFolderInExistingFolder.getFile("foo.cpp");
		assertThat(newFile.getLocation().toFile().createNewFile())
				.withFailMessage("file creation not successful: %s", newFile).isTrue();

		existingFolderInExistingFolder.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		members = existingFolderInExistingFolder.members();
		assertThat(members).hasSize(1).satisfiesExactly(member -> {
			assertThat(member.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(member.getName()).as("name").isEqualTo("foo.cpp");
		});

		members = folder.members();
		assertThat(members).isEmpty();

		newFile = existingFolderInExistingFolder.getFile("foo.h");
		assertThat(newFile.getLocation().toFile().createNewFile())
				.withFailMessage("file creation not successful: %s", newFile).isTrue();

		existingFolderInExistingFolder.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		members = existingFolderInExistingFolder.members();
		assertThat(members).hasSize(1).satisfiesExactly(member -> {
			assertThat(member.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(member.getName()).as("name").isEqualTo("foo.cpp");
		});

		members = folder.members();
		assertThat(members).isEmpty();

		folder.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		members = existingFolderInExistingFolder.members();
		assertThat(members).hasSize(1).satisfiesExactly(member -> {
			assertThat(member.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(member.getName()).as("name").isEqualTo("foo.cpp");
		});

		members = folder.members();
		assertThat(members).hasSize(1).satisfiesExactly(member -> {
			assertThat(member.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(member.getName()).as("name").isEqualTo("foo.h");
		});

		// create a file that shows under both
		newFile = existingFolderInExistingFolder.getFile("foo.text");
		assertThat(newFile.getLocation().toFile().createNewFile())
				.withFailMessage("file creation not successful: %s", newFile).isTrue();

		existingFolderInExistingFolder.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		members = existingFolderInExistingFolder.members();
		assertThat(members).hasSize(2).satisfiesExactly(firstMember -> {
			assertThat(firstMember.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(firstMember.getName()).as("name").isEqualTo("foo.cpp");
		}, secondMember -> {
			assertThat(secondMember.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(secondMember.getName()).as("name").isEqualTo("foo.text");
		});

		members = folder.members();
		assertThat(members).hasSize(2).satisfiesExactly(firstMember -> {
			assertThat(firstMember.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(firstMember.getName()).as("name").isEqualTo("foo.h");
		}, secondMember -> {
			assertThat(secondMember.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(secondMember.getName()).as("name").isEqualTo("foo.text");
		});

		// delete the common file
		newFile.delete(true, createTestMonitor());
		members = existingFolderInExistingFolder.members();
		assertThat(members).hasSize(1).satisfiesExactly(member -> {
			assertThat(member.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(member.getName()).as("name").isEqualTo("foo.cpp");
		});

		members = folder.members();
		assertThat(members).hasSize(1).satisfiesExactly(member -> {
			assertThat(member.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(member.getName()).as("name").isEqualTo("foo.h");
		});

		// remove the first filter
		filterDescription2.delete(0, createTestMonitor());
		members = existingFolderInExistingFolder.members();
		assertThat(members).hasSize(2).satisfiesExactly(firstMember -> {
			assertThat(firstMember.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(firstMember.getName()).as("name").isEqualTo("foo.cpp");
		}, secondMember -> {
			assertThat(secondMember.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(secondMember.getName()).as("name").isEqualTo("foo.h");
		});

		members = folder.members();
		assertThat(members).hasSize(1).satisfiesExactly(member -> {
			assertThat(member.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(member.getName()).as("name").isEqualTo("foo.h");
		});

		// add the filter again
		existingFolderInExistingFolder.createFilter(
				IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FILES, matcherDescription2, 0,
				createTestMonitor());
		members = existingFolderInExistingFolder.members();
		assertThat(members).hasSize(1).satisfiesExactly(member -> {
			assertThat(member.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(member.getName()).as("name").isEqualTo("foo.cpp");
		});

		members = folder.members();
		assertThat(members).hasSize(1).satisfiesExactly(member -> {
			assertThat(member.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(member.getName()).as("name").isEqualTo("foo.h");
		});
	}

	@Test
	public void testIResource_isFiltered() throws CoreException {
		IFolder folder = existingFolderInExistingProject.getFolder("virtual_folder.txt");
		IFile file = existingFolderInExistingProject.getFile("linked_file.txt");

		folder.create(IResource.VIRTUAL, true, createTestMonitor());
		file.createLink(existingFileInExistingProject.getLocation(), 0, createTestMonitor());

		FileInfoMatcherDescription matcherDescription = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, ".*\\.txt");
		existingFolderInExistingProject.createFilter(IResourceFilterDescription.EXCLUDE_ALL, matcherDescription, 0,
				createTestMonitor());

		IWorkspace workspace = existingProject.getWorkspace();
		assertThat(folder).matches(it -> workspace.validateFiltered(it).isOK(), "is not filtered");
		assertThat(folder).matches(it -> workspace.validateFiltered(it).isOK(), "is not filtered");
	}

	/**
	 * Tests the creation of two different filters on a linked folder and the original.
	 * Check that creating and modifying files in the workspace doesn't make them appear in
	 * excluded locations.
	 * Regression for bug 267201
	 */
	@Test
	public void testCreateFilterOnLinkedFolderAndTarget2() throws Exception {
		final IPath location = existingFolderInExistingFolder.getLocation();
		final IFolder folder = nonExistingFolderInExistingProject;

		folder.createLink(location, IResource.NONE, createTestMonitor());

		FileInfoMatcherDescription matcherDescription1 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, ".*\\.h");
		FileInfoMatcherDescription matcherDescription2 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, ".*\\.cpp");

		folder.createFilter(IResourceFilterDescription.INCLUDE_ONLY | IResourceFilterDescription.FILES,
				matcherDescription1, 0, createTestMonitor());
		existingFolderInExistingFolder.createFilter(
				IResourceFilterDescription.INCLUDE_ONLY | IResourceFilterDescription.FILES, matcherDescription2, 0,
				createTestMonitor());


		// Create 'foo.cpp' in existingFolder...
		IFile newFile = existingFolderInExistingFolder.getFile("foo.cpp");
		createInWorkspace(newFile);
		IResource[] members = existingFolderInExistingFolder.members();
		assertThat(members).hasSize(1).satisfiesExactly(member -> {
			assertThat(member.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(member.getName()).as("name").isEqualTo("foo.cpp");
		});

		// Create a 'foo.h' under folder
		newFile = folder.getFile("foo.h");
		createInWorkspace(newFile);
		// Check that foo.h has appeared in 'folder'
		// // Refreshing restores sanity (hides the .cpp files)...
		// folder.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		members = folder.members();
		assertThat(members).hasSize(1).satisfiesExactly(member -> {
			assertThat(member.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(member.getName()).as("name").isEqualTo("foo.h");
		});

		// Check it hasn't appeared in existingFolder...
		// // Refresh restores sanity...
		// existingFolderInExistingFolder.refreshLocal(IResource.DEPTH_INFINITE,
		// getMonitor());
		members = existingFolderInExistingFolder.members();
		assertThat(members).hasSize(1).allSatisfy(member -> {
			assertThat(member.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(member.getName()).as("name").isEqualTo("foo.cpp");
		});
		// And refreshing doesn't change things
		existingFolderInExistingFolder.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		members = existingFolderInExistingFolder.members();
		assertThat(members).hasSize(1).allSatisfy(member -> {
			assertThat(member.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(member.getName()).as("name").isEqualTo("foo.cpp");
		});

		// Check modifying foo.h doesn't make it appear
		modifyFileInWorkspace(folder.getFile("foo.h"));
		members = existingFolderInExistingFolder.members();
		assertThat(members).hasSize(1).allSatisfy(member -> {
			assertThat(member.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(member.getName()).as("name").isEqualTo("foo.cpp");
		});
	}

	private void modifyFileInWorkspace(final IFile file) throws IOException, CoreException {
		try (InputStream fileInputStream = file.getContents(false)) {
			ByteArrayOutputStream originalContentStream = new ByteArrayOutputStream();
			fileInputStream.transferTo(originalContentStream);
			String originalContent = new String(originalContentStream.toByteArray(), StandardCharsets.UTF_8);
			String newContent = originalContent + "w";
			ByteArrayInputStream modifiedContentStream = new ByteArrayInputStream(
					newContent.getBytes(StandardCharsets.UTF_8));
			file.setContents(modifiedContentStream, false, false, null);
		}
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
	@Test
	public void testCreateFilterOnLinkedFolderWithAlias() throws Exception {
		final IProject project = otherExistingProject;
		final IPath parentLoc = existingFolderInExistingProject.getLocation();
		final IPath childLoc = existingFolderInExistingFolder.getLocation();
		final IFolder folder1 = nonExistingFolderInOtherExistingProject;
		final IFolder folder2 = nonExistingFolder2InOtherExistingProject;

		assertThat(folder1).matches(not(IFolder::exists), "does not exist");
		assertThat(folder2).matches(not(IFolder::exists), "does not exist");
		assertThat(parentLoc).matches(parentLocation -> parentLocation.isPrefixOf(childLoc),
				"is prefix of child location");

		FileInfoMatcherDescription matcherDescription1 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, ".*");

		// Filter out all children from existingFolderInExistingProject
		IResourceFilterDescription filterDescription1 = folder1.createFilter(
				IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FOLDERS, matcherDescription1, 0,
				createTestMonitor());

		folder1.createLink(parentLoc, IResource.NONE, createTestMonitor());
		folder2.createLink(childLoc, IResource.NONE, createTestMonitor());
		existingProject.close(createTestMonitor());

		assertThat(folder1).matches(IFolder::exists, "exists").matches(IFolder::isLinked, "is linked")
				.extracting(IFolder::getLocation).isEqualTo(parentLoc);
		assertThat(folder2).matches(IFolder::exists, "exists").matches(IFolder::isLinked, "is linked")
				.extracting(IFolder::getLocation).isEqualTo(childLoc);
		assertThat(folder1.members()).isEmpty();
		assertThat(folder2.members()).isEmpty();

		// Need to unset M_USED on the project's resource info, or
		// reconcileLinks will never be called...
		Workspace workspace = ((Workspace) ResourcesPlugin.getWorkspace());
		try {
			workspace.prepareOperation(project, createTestMonitor());
			workspace.beginOperation(true);

			ResourceInfo ri = ((Resource) project).getResourceInfo(false, true);
			ri.clear(ICoreConstants.M_USED);
		} finally {
			workspace.endOperation(project, true);
		}

		project.close(createTestMonitor());
		assertThat(project).matches(not(IProject::isOpen), "is not open");
		// Create a file under existingFolderInExistingFolder
		createInFileSystem(childLoc.append("foo"));
		// Reopen the project
		project.open(IResource.NONE, createTestMonitor());

		assertThat(folder1).matches(IFolder::exists, "exists").matches(IFolder::isLinked, "is linked")
				.extracting(IFolder::getLocation).isEqualTo(parentLoc);
		assertThat(folder2).matches(IFolder::exists, "exists").matches(IFolder::isLinked, "is linked")
				.extracting(IFolder::getLocation).isEqualTo(childLoc);
		assertThat(folder1.members()).isEmpty();
		assertThat(folder2.members()).hasSize(1);

		// Swap the links around, loading may be order independent...
		folder2.createLink(parentLoc, IResource.REPLACE, createTestMonitor());
		folder1.createLink(childLoc, IResource.REPLACE | IResource.FORCE, createTestMonitor());

		// Filter out all children from existingFolderInExistingProject
		folder2.createFilter(IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FOLDERS,
				matcherDescription1, 0, createTestMonitor());
		filterDescription1.delete(0, createTestMonitor());
		assertThat(folder1.getFilters()).isEmpty();

		// Need to unset M_USED on the project's resource info, or
		// reconcileLinks will never be called...
		try {
			workspace.prepareOperation(project, createTestMonitor());
			workspace.beginOperation(true);

			ResourceInfo ri = ((Resource) project).getResourceInfo(false, true);
			ri.clear(ICoreConstants.M_USED);
		} finally {
			workspace.endOperation(project, true);
		}

		project.close(createTestMonitor());
		assertThat(project).matches(not(IProject::isOpen), "is not open");
		project.open(IResource.NONE, createTestMonitor());

		assertThat(folder1).matches(IFolder::exists, "exists").matches(IFolder::isLinked, "is linked")
				.extracting(IFolder::getLocation).isEqualTo(childLoc);
		assertThat(folder2).matches(IFolder::exists, "exists").matches(IFolder::isLinked, "is linked")
				.extracting(IFolder::getLocation).isEqualTo(parentLoc);
		assertThat(folder1.members()).hasSize(1);
		assertThat(folder2.members()).isEmpty();
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
	@Test
	public void testCreateFilterOnLinkedFolderWithAlias2() throws CoreException {
		final IProject project = otherExistingProject;
		final IPath parentLoc = existingFolderInExistingProject.getLocation();
		final IPath childLoc = existingFolderInExistingFolder.getLocation();
		final IFolder folder1 = nonExistingFolderInOtherExistingProject;
		final IFolder folder2 = nonExistingFolder2InOtherExistingProject;

		assertThat(folder1).matches(not(IFolder::exists), "does not exist");
		assertThat(folder2).matches(not(IFolder::exists), "does not exist");
		assertThat(parentLoc).matches(parentLocation -> parentLocation.isPrefixOf(childLoc),
				"is prefix of child location");

		FileInfoMatcherDescription matcherDescription1 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, ".*");

		// Filter out all children from existingFolderInExistingProject
		IResourceFilterDescription filterDescription1 = folder1.createFilter(
				IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FOLDERS, matcherDescription1, 0,
				createTestMonitor());

		folder1.createLink(parentLoc, IResource.NONE, createTestMonitor());
		folder2.createLink(childLoc, IResource.NONE, createTestMonitor());
		existingProject.close(createTestMonitor());

		assertThat(folder1).matches(IFolder::exists, "exists").matches(IFolder::isLinked, "is linked")
				.extracting(IFolder::getLocation).isEqualTo(parentLoc);
		assertThat(folder2).matches(IFolder::exists, "exists").matches(IFolder::isLinked, "is linked")
				.extracting(IFolder::getLocation).isEqualTo(childLoc);
		assertThat(folder1.members()).isEmpty();
		assertThat(folder2.members()).isEmpty();

		// Need to unset M_USED on the project's resource info, or
		// reconcileLinks will never be called...
		Workspace workspace = ((Workspace) ResourcesPlugin.getWorkspace());
		try {
			workspace.prepareOperation(project, createTestMonitor());
			workspace.beginOperation(true);

			ResourceInfo ri = ((Resource) project).getResourceInfo(false, true);
			ri.clear(ICoreConstants.M_USED);
		} finally {
			workspace.endOperation(project, true);
		}

		// Create a file under existingFolderInExistingFolder
		createInWorkspace(folder2.getFile("foo"));

		assertThat(folder1).matches(IFolder::exists, "exists").matches(IFolder::isLinked, "is linked")
				.extracting(IFolder::getLocation).isEqualTo(parentLoc);
		assertThat(folder2).matches(IFolder::exists, "exists").matches(IFolder::isLinked, "is linked")
				.extracting(IFolder::getLocation).isEqualTo(childLoc);
		assertThat(folder1.members()).isEmpty();
		assertThat(folder2.members()).hasSize(1);

		// Swap the links around, loading may be order independent...
		folder2.createLink(parentLoc, IResource.REPLACE | IResource.NONE, createTestMonitor());
		folder1.createLink(childLoc, IResource.REPLACE | IResource.FORCE, createTestMonitor());

		// Filter out all children from existingFolderInExistingProject
		folder2.createFilter(IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FOLDERS,
				matcherDescription1, 0, createTestMonitor());
		filterDescription1.delete(0, createTestMonitor());
		assertThat(folder1.getFilters()).isEmpty();

		assertThat(folder1).matches(IFolder::exists, "exists").matches(IFolder::isLinked, "is linked")
				.extracting(IFolder::getLocation).isEqualTo(childLoc);
		assertThat(folder2).matches(IFolder::exists, "exists").matches(IFolder::isLinked, "is linked")
				.extracting(IFolder::getLocation).isEqualTo(parentLoc);
		assertThat(folder1.members()).hasSize(1);
		assertThat(folder2.members()).isEmpty();
	}

	/**
	 * Tests the creation of a simple filter on a linked folder before the resource creation.
	 */
	@Test
	public void testCreateFilterOnLinkedFolderBeforeCreation() throws CoreException {
		IPath location = existingFolderInExistingFolder.getLocation();
		IFolder folder = nonExistingFolderInExistingProject;

		assertThat(folder).matches(not(IFolder::exists), "does not exist");

		FileInfoMatcherDescription matcherDescription1 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, "foo");

		folder.createFilter(IResourceFilterDescription.INCLUDE_ONLY | IResourceFilterDescription.FILES,
				matcherDescription1, 0, createTestMonitor());
		folder.createLink(location, IResource.NONE, createTestMonitor());

		IFile foo = folder.getFile("foo");
		IFile bar = folder.getFile("bar");

		createInWorkspace(new IResource[] {foo, bar});

		existingProject.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());

		//close and reopen the project
		existingProject.close(createTestMonitor());
		existingProject.open(createTestMonitor());

		IResourceFilterDescription[] filters = folder.getFilters();

		assertThat(filters).hasSize(1).satisfiesExactly(filter -> {
			assertThat(filter.getFileInfoMatcherDescription().getId()).as("provider").isEqualTo(REGEX_FILTER_PROVIDER);
			assertThat(filter.getFileInfoMatcherDescription().getArguments()).as("arguments").isEqualTo("foo");
			assertThat(filter.getType()).as("type")
					.isEqualTo(IResourceFilterDescription.INCLUDE_ONLY | IResourceFilterDescription.FILES);
			assertThat(filter.getResource()).as("resource").isEqualTo(folder);
		});

		IResource members[] = folder.members();
		assertThat(members).hasSize(1).satisfiesExactly(member -> {
			assertThat(member.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(member.getName()).as("name").isEqualTo("foo");
		});
	}

	/**
	 * Tests the creation and removal of a simple filter on a folder.
	 */
	@Test
	public void testCreateAndRemoveFilterOnFolder() throws CoreException {
		FileInfoMatcherDescription matcherDescription1 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, "foo");
		IResourceFilterDescription filterDescription = existingFolderInExistingFolder
				.createFilter(IResourceFilterDescription.INCLUDE_ONLY | IResourceFilterDescription.FILES
						| IResourceFilterDescription.FOLDERS, matcherDescription1, 0, createTestMonitor());

		IFile foo = existingFolderInExistingFolder.getFile("foo");
		IFile bar = existingFolderInExistingFolder.getFile("bar");

		createInWorkspace(new IResource[] {foo, bar});

		existingProject.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());

		//close and reopen the project
		existingProject.close(createTestMonitor());
		existingProject.open(createTestMonitor());

		filterDescription.delete(0, createTestMonitor());

		//close and reopen the project
		existingProject.close(createTestMonitor());
		existingProject.open(createTestMonitor());

		IResourceFilterDescription[] filters = existingFolderInExistingFolder.getFilters();

		assertThat(filters).isEmpty();

		IResource members[] = existingFolderInExistingFolder.members();
		assertThat(members).hasSize(2).satisfiesExactly(firstMember -> {
			assertThat(firstMember.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(firstMember.getName()).as("name").isEqualTo("bar");
		}, secondMember -> {
			assertThat(secondMember.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(secondMember.getName()).as("name").isEqualTo("foo");
		});
	}

	/**
	 * Tests the creation and removal of a simple filter on a folder.
	 */
	@Test
	public void testCreateAndRemoveFilterOnFolderWithoutClosingProject() throws CoreException {
		FileInfoMatcherDescription matcherDescription1 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, "foo");
		IResourceFilterDescription filterDescription = existingFolderInExistingFolder
				.createFilter(IResourceFilterDescription.INCLUDE_ONLY | IResourceFilterDescription.FILES
						| IResourceFilterDescription.FOLDERS, matcherDescription1, 0, createTestMonitor());

		IFile foo = existingFolderInExistingFolder.getFile("foo");
		IFile bar = existingFolderInExistingFolder.getFile("bar");

		createInWorkspace(new IResource[] {foo, bar});

		existingProject.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		filterDescription.delete(0, createTestMonitor());

		IResourceFilterDescription[] filters = existingFolderInExistingFolder.getFilters();
		assertThat(filters).isEmpty();

		IResource members[] = existingFolderInExistingFolder.members();
		assertThat(members).hasSize(2).satisfiesExactly(firstMember -> {
			assertThat(firstMember.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(firstMember.getName()).as("name").isEqualTo("bar");
		}, secondMember -> {
			assertThat(secondMember.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(secondMember.getName()).as("name").isEqualTo("foo");
		});
	}

	/**
	 * Tests the creation of the include-only filter.
	 */
	@Test
	public void testIncludeOnlyFilter() throws CoreException {
		FileInfoMatcherDescription matcherDescription1 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, ".*\\.c");

		existingFolderInExistingProject.createFilter(IResourceFilterDescription.INCLUDE_ONLY
				| IResourceFilterDescription.FILES | IResourceFilterDescription.FOLDERS, matcherDescription1, 0,
				createTestMonitor());

		IFile foo = existingFolderInExistingProject.getFile("foo.c");
		IFile file = existingFolderInExistingProject.getFile("file.c");
		IFile bar = existingFolderInExistingProject.getFile("bar.h");

		createInWorkspace(new IResource[] {foo, bar, file});
		existingProject.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());

		IResource members[] = existingFolderInExistingProject.members();
		assertThat(members).hasSize(2).satisfiesExactly(firstMember -> {
			assertThat(firstMember.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(firstMember.getName()).as("name").isEqualTo("file.c");
		}, secondMember -> {
			assertThat(secondMember.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(secondMember.getName()).as("name").isEqualTo("foo.c");
		});

		FileInfoMatcherDescription matcherDescription2 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, ".*\\.c");
		existingFolderInExistingProject.createFilter(IResourceFilterDescription.INCLUDE_ONLY
				| IResourceFilterDescription.FILES | IResourceFilterDescription.FOLDERS, matcherDescription2, 0,
				createTestMonitor());

		existingProject.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		members = existingFolderInExistingProject.members();
		assertThat(members).hasSize(2).satisfiesExactly(firstMember -> {
			assertThat(firstMember.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(firstMember.getName()).as("name").isEqualTo("file.c");
		}, secondMember -> {
			assertThat(secondMember.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(secondMember.getName()).as("name").isEqualTo("foo.c");
		});
	}

	/**
	 * Tests the creation of the exclude-all filter.
	 */
	@Test
	public void testExcludeAllFilter() throws CoreException {
		FileInfoMatcherDescription matcherDescription1 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, ".*\\.c");

		existingFolderInExistingFolder.createFilter(IResourceFilterDescription.EXCLUDE_ALL
				| IResourceFilterDescription.FILES | IResourceFilterDescription.FOLDERS, matcherDescription1, 0,
				createTestMonitor());

		IFile foo = existingFolderInExistingFolder.getFile("foo.c");
		IFile file = existingFolderInExistingFolder.getFile("file.c");
		IFile fooh = existingFolderInExistingFolder.getFile("foo.h");
		IFile bar = existingFolderInExistingFolder.getFile("bar.h");

		createInWorkspace(new IResource[] {foo, bar, file, fooh});
		existingProject.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());

		IResource members[] = existingFolderInExistingFolder.members();
		assertThat(members).hasSize(2).satisfiesExactly(firstMember -> {
			assertThat(firstMember.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(firstMember.getName()).as("name").isEqualTo("bar.h");
		}, secondMember -> {
			assertThat(secondMember.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(secondMember.getName()).as("name").isEqualTo("foo.h");
		});

		FileInfoMatcherDescription matcherDescription2 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, "foo.*");
		existingFolderInExistingFolder.createFilter(IResourceFilterDescription.EXCLUDE_ALL
				| IResourceFilterDescription.FILES | IResourceFilterDescription.FOLDERS, matcherDescription2, 0,
				createTestMonitor());
		existingProject.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());

		members = existingFolderInExistingFolder.members();
		assertThat(members).hasSize(1).satisfiesExactly(member -> {
			assertThat(member.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(member.getName()).as("name").isEqualTo("bar.h");
		});
	}

	/**
	 * Tests the creation of the mixed include-only exclude-all filter.
	 */
	@Test
	public void testMixedFilter() throws CoreException {
		FileInfoMatcherDescription matcherDescription1 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, ".*\\.c");
		FileInfoMatcherDescription matcherDescription2 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, "foo.*");

		existingFolderInExistingProject.createFilter(IResourceFilterDescription.INCLUDE_ONLY
				| IResourceFilterDescription.FILES | IResourceFilterDescription.FOLDERS, matcherDescription1, 0,
				createTestMonitor());
		existingFolderInExistingProject.createFilter(IResourceFilterDescription.EXCLUDE_ALL
				| IResourceFilterDescription.FILES | IResourceFilterDescription.FOLDERS, matcherDescription2, 0,
				createTestMonitor());

		IFile foo = existingFolderInExistingProject.getFile("foo.c");
		IFile file = existingFolderInExistingProject.getFile("file.c");
		IFile fooh = existingFolderInExistingProject.getFile("foo.h");
		IFile bar = existingFolderInExistingProject.getFile("bar.h");

		createInWorkspace(new IResource[] {foo, bar, file, fooh});
		existingProject.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());

		IResource members[] = existingFolderInExistingProject.members();
		assertThat(members).hasSize(1).satisfiesExactly(member -> {
			assertThat(member.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(member.getName()).as("name").isEqualTo("file.c");
		});
	}

	/**
	 * Tests the creation of inheritable filter.
	 */
	@Test
	public void testInheritedFilter() throws CoreException {
		FileInfoMatcherDescription matcherDescription1 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, ".*\\.c");
		FileInfoMatcherDescription matcherDescription2 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, "foo.*");

		existingProject.createFilter(IResourceFilterDescription.INCLUDE_ONLY | IResourceFilterDescription.INHERITABLE
				| IResourceFilterDescription.FILES, matcherDescription1, 0, createTestMonitor());
		existingFolderInExistingFolder.createFilter(IResourceFilterDescription.EXCLUDE_ALL
				| IResourceFilterDescription.FILES | IResourceFilterDescription.FOLDERS, matcherDescription2, 0,
				createTestMonitor());

		IFile foo = existingFolderInExistingFolder.getFile("foo.c");
		IFile file = existingFolderInExistingFolder.getFile("file.c");
		IFile fooh = existingFolderInExistingFolder.getFile("foo.h");
		IFile bar = existingFolderInExistingFolder.getFile("bar.h");

		createInWorkspace(new IResource[] {foo, bar, file, fooh});
		existingProject.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());

		IResource members[] = existingFolderInExistingFolder.members();
		assertThat(members).hasSize(1).satisfiesExactly(member -> {
			assertThat(member.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(member.getName()).as("name").isEqualTo("file.c");
		});
	}

	/**
	 * Tests the creation of FOLDER filter.
	 */
	@Test
	public void testFolderOnlyFilters() throws CoreException {
		FileInfoMatcherDescription matcherDescription1 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, "foo.*");
		existingFolderInExistingFolder.createFilter(
				IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FOLDERS, matcherDescription1, 0,
				createTestMonitor());

		IFile foo = existingFolderInExistingFolder.getFile("foo.c");
		IFolder food = existingFolderInExistingFolder.getFolder("foo.d");

		createInWorkspace(new IResource[] {foo, food});
		existingProject.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());

		IResource members[] = existingFolderInExistingFolder.members();
		assertThat(members).hasSize(1).satisfiesExactly(member -> {
			assertThat(member.getType()).as("type").isEqualTo(IResource.FILE);
			assertThat(member.getName()).as("name").isEqualTo("foo.c");
		});
	}

	/**
	 * Tests the creation of FILE filter.
	 */
	@Test
	public void testFileOnlyFilters() throws CoreException {
		FileInfoMatcherDescription matcherDescription1 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, "foo.*");
		existingFolderInExistingFolder.createFilter(
				IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FILES, matcherDescription1, 0,
				createTestMonitor());

		IFile foo = existingFolderInExistingFolder.getFile("foo.c");
		IFolder food = existingFolderInExistingFolder.getFolder("foo.d");

		createInWorkspace(new IResource[] {foo, food});
		existingProject.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());

		IResource members[] = existingFolderInExistingFolder.members();
		assertThat(members).hasSize(1).satisfiesExactly(member -> {
			assertThat(member.getType()).as("type").isEqualTo(IResource.FOLDER);
			assertThat(member.getName()).as("name").isEqualTo("foo.d");
		});
	}

	/**
	 * Tests moving a folder with filters.
	 */
	@Test
	public void testMoveFolderWithFilterToAnotherProject() throws CoreException {
		FileInfoMatcherDescription matcherDescription1 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, "foo.*");
		existingFolderInExistingProject.createFilter(
				IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FILES, matcherDescription1, 0,
				createTestMonitor());

		IFile foo = existingFolderInExistingProject.getFile("foo.c");
		IFolder food = existingFolderInExistingProject.getFolder("foo.d");

		createInWorkspace(new IResource[] {foo, food});
		existingProject.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());

		IFolder destination = otherExistingProject.getFolder("destination");
		existingFolderInExistingProject.move(destination.getFullPath(), 0, createTestMonitor());

		IResourceFilterDescription[] filters = existingFolderInExistingProject.getFilters();
		assertThat(filters).isEmpty();

		filters = destination.getFilters();
		assertThat(filters).hasSize(1).satisfiesExactly(filter -> {
			assertThat(filter.getFileInfoMatcherDescription().getId()).as("provider").isEqualTo(REGEX_FILTER_PROVIDER);
			assertThat(filter.getFileInfoMatcherDescription().getArguments()).as("arguments").isEqualTo("foo.*");
			assertThat(filter.getType()).as("type")
					.isEqualTo(IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FILES);
			assertThat(filter.getResource()).as("resource").isEqualTo(destination);
		});
	}

	/**
	 * Tests copying a folder with filters.
	 */
	@Test
	public void testCopyFolderWithFilterToAnotherProject() throws CoreException {
		FileInfoMatcherDescription matcherDescription1 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, "foo.*");
		existingFolderInExistingProject.createFilter(
				IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FILES, matcherDescription1, 0,
				createTestMonitor());

		IFile foo = existingFolderInExistingProject.getFile("foo.c");
		IFolder food = existingFolderInExistingProject.getFolder("foo.d");

		createInWorkspace(new IResource[] {foo, food});
		existingProject.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());

		IFolder destination = otherExistingProject.getFolder("destination");
		existingFolderInExistingProject.copy(destination.getFullPath(), 0, createTestMonitor());

		IResourceFilterDescription[] filters = existingFolderInExistingProject.getFilters();
		assertThat(filters).hasSize(1).satisfiesExactly(filter -> {
			assertThat(filter.getFileInfoMatcherDescription().getId()).as("provider").isEqualTo(REGEX_FILTER_PROVIDER);
			assertThat(filter.getFileInfoMatcherDescription().getArguments()).as("arguments").isEqualTo("foo.*");
			assertThat(filter.getType()).as("type")
					.isEqualTo(IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FILES);
			assertThat(filter.getResource()).as("resource").isEqualTo(existingFolderInExistingProject);
		});

		filters = destination.getFilters();
		assertThat(filters).hasSize(1).satisfiesExactly(filter -> {
			assertThat(filter.getFileInfoMatcherDescription().getId()).as("provider").isEqualTo(REGEX_FILTER_PROVIDER);
			assertThat(filter.getFileInfoMatcherDescription().getArguments()).as("arguments").isEqualTo("foo.*");
			assertThat(filter.getType()).as("type")
					.isEqualTo(IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FILES);
			assertThat(filter.getResource()).as("resource").isEqualTo(destination);
		});
	}

	/**
	 * Tests copying a folder with filters to another folder.
	 */
	@Test
	public void testCopyFolderWithFilterToAnotherFolder() throws CoreException {
		FileInfoMatcherDescription matcherDescription1 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, "foo.*");
		existingFolderInExistingProject.createFilter(
				IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FILES, matcherDescription1, 0,
				createTestMonitor());

		IFile foo = existingFolderInExistingProject.getFile("foo.c");
		IFolder food = existingFolderInExistingProject.getFolder("foo.d");

		createInWorkspace(new IResource[] {foo, food});
		existingProject.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());

		createInWorkspace(new IResource[] {nonExistingFolderInExistingProject});
		IFolder destination = nonExistingFolderInExistingProject.getFolder("destination");
		existingFolderInExistingProject.copy(destination.getFullPath(), 0, createTestMonitor());

		IResourceFilterDescription[] filters = existingFolderInExistingProject.getFilters();
		assertThat(filters).hasSize(1).satisfiesExactly(filter -> {
			assertThat(filter.getFileInfoMatcherDescription().getId()).as("provider").isEqualTo(REGEX_FILTER_PROVIDER);
			assertThat(filter.getFileInfoMatcherDescription().getArguments()).as("arguments").isEqualTo("foo.*");
			assertThat(filter.getType()).as("type")
					.isEqualTo(IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FILES);
			assertThat(filter.getResource()).as("resource").isEqualTo(existingFolderInExistingProject);
		});

		filters = destination.getFilters();
		assertThat(filters).hasSize(1).satisfiesExactly(filter -> {
			assertThat(filter.getFileInfoMatcherDescription().getId()).as("provider").isEqualTo(REGEX_FILTER_PROVIDER);
			assertThat(filter.getFileInfoMatcherDescription().getArguments()).as("arguments").isEqualTo("foo.*");
			assertThat(filter.getType()).as("type")
					.isEqualTo(IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FILES);
			assertThat(filter.getResource()).as("resource").isEqualTo(destination);
		});
	}

	/**
	 * Tests moving a folder with filters to another folder.
	 */
	@Test
	public void testMoveFolderWithFilterToAnotherFolder() throws CoreException {
		FileInfoMatcherDescription matcherDescription1 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, "foo.*");
		existingFolderInExistingProject.createFilter(
				IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FILES, matcherDescription1, 0,
				createTestMonitor());

		IFile foo = existingFolderInExistingProject.getFile("foo.c");
		IFolder food = existingFolderInExistingProject.getFolder("foo.d");

		createInWorkspace(new IResource[] {foo, food});
		existingProject.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());

		createInWorkspace(new IResource[] {nonExistingFolderInExistingProject});
		IFolder destination = nonExistingFolderInExistingProject.getFolder("destination");
		existingFolderInExistingProject.move(destination.getFullPath(), 0, createTestMonitor());

		IResourceFilterDescription[] filters = existingFolderInExistingProject.getFilters();
		assertThat(filters).isEmpty();

		filters = destination.getFilters();
		assertThat(filters).hasSize(1).satisfiesExactly(filter -> {
			assertThat(filter.getFileInfoMatcherDescription().getId()).as("provider").isEqualTo(REGEX_FILTER_PROVIDER);
			assertThat(filter.getFileInfoMatcherDescription().getArguments()).as("arguments").isEqualTo("foo.*");
			assertThat(filter.getType()).as("type")
					.isEqualTo(IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FILES);
			assertThat(filter.getResource()).as("resource").isEqualTo(destination);
		});
	}

	/**
	 * Tests deleting a folder with filters.
	 */
	@Test
	public void testDeleteFolderWithFilterToAnotherFolder() throws CoreException {
		FileInfoMatcherDescription matcherDescription1 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, "foo.*");
		FileInfoMatcherDescription matcherDescription2 = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, ".*\\.c");

		existingFolderInExistingProject.createFilter(
				IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FILES, matcherDescription1, 0,
				createTestMonitor());
		existingFolderInExistingFolder.createFilter(
				IResourceFilterDescription.INCLUDE_ONLY | IResourceFilterDescription.FILES, matcherDescription2, 0,
				createTestMonitor());

		IFile foo = existingFolderInExistingProject.getFile("foo.c");
		IFolder food = existingFolderInExistingProject.getFolder("foo.d");

		createInWorkspace(new IResource[] {foo, food});
		existingProject.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());

		createInWorkspace(new IResource[] {nonExistingFolderInExistingProject});
		existingFolderInExistingProject.delete(0, createTestMonitor());

		IResourceFilterDescription[] filters = existingFolderInExistingProject.getFilters();
		assertThat(filters).isEmpty();

		filters = existingFolderInExistingFolder.getFilters();
		assertThat(filters).isEmpty();
	}

	/* Regression test for Bug 304276 */
	@Test
	public void testInvalidCharactersInRegExFilter() {
		RegexFileInfoMatcher matcher = new RegexFileInfoMatcher();
		assertThrows(CoreException.class, () -> matcher.initialize(existingProject, "*:*"));
	}

	/**
	 * Regression test for Bug 302146
	 */
	@Test
	public void testBug302146() throws Exception {
		FileInfoMatcherDescription matcherDescription = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, "foo");
		existingFolderInExistingProject.createFilter(IResourceFilterDescription.INCLUDE_ONLY
				| IResourceFilterDescription.FILES | IResourceFilterDescription.FOLDERS, matcherDescription, 0,
				createTestMonitor());

		// close and reopen the project
		existingProject.close(createTestMonitor());
		existingProject.open(createTestMonitor());
		IResourceFilterDescription[] filters = existingFolderInExistingProject.getFilters();

		// check that filters are recreated when the project is reopened
		// it means that .project was updated with filter details
		assertThat(filters).hasSize(1).satisfiesExactly(filter -> {
			assertThat(filter.getFileInfoMatcherDescription().getId()).as("provider").isEqualTo(REGEX_FILTER_PROVIDER);
			assertThat(filter.getFileInfoMatcherDescription().getArguments()).as("arguments").isEqualTo("foo");
			assertThat(filter.getType()).as("type").isEqualTo(IResourceFilterDescription.INCLUDE_ONLY
					| IResourceFilterDescription.FILES | IResourceFilterDescription.FOLDERS);
			assertThat(filter.getResource()).as("resource").isEqualTo(existingFolderInExistingProject);
		});

		new ProjectDescriptionReader(existingProject).read(existingProject.getFile(".project").getLocation());
	}

	/**
	 * Regression for  Bug 317783 -  Resource filters do not work at all in "Project Explorer"
	 * The problem is that a client calls explicitly refreshLocal on a folder that is filtered out by
	 * resource filters and that doesn't exist in the workspace.  This used to cause the resource to
	 * appear in the workspace, along with all its children, in spite of active resource filters to the
	 * contrary.
	 */
	@Test
	public void test317783() throws CoreException {
		IFolder folder = existingProject.getFolder("foo");
		createInWorkspace(folder);

		IFile file = folder.getFile("bar.txt");
		createInWorkspace(file, "content");

		existingProject.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		FileInfoMatcherDescription matcherDescription = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, ".*");
		existingProject.createFilter(IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FOLDERS,
				matcherDescription, 0, createTestMonitor());
		existingProject.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());

		IResource members[] = existingProject.members();
		assertThat(members).hasSize(2).satisfiesExactly(first -> assertThat(first.getName()).isEqualTo(".project"),
				second -> assertThat(second.getName()).isEqualTo(existingFileInExistingProject.getName()));

		folder.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		members = existingProject.members();
		assertThat(members).hasSize(2).satisfiesExactly(first -> assertThat(first.getName()).isEqualTo(".project"),
				second -> assertThat(second.getName()).isEqualTo(existingFileInExistingProject.getName()));

		assertThat(folder).matches(not(IFolder::exists), "does not exist");
		assertThat(file).matches(not(IFile::exists), "does not exist");
	}

	/**
	 * Regression for  Bug 317824 -  Renaming a project that contains resource filters fails,
	 * and copying a project that contains resource filters removes the resource filters.
	 */
	@Test
	public void test317824() throws CoreException {
		IFolder folder = existingProject.getFolder("foo");
		createInWorkspace(folder);

		IFile file = folder.getFile("bar.txt");
		createInWorkspace(file, "content");

		existingProject.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		FileInfoMatcherDescription matcherDescription = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER, ".*");
		existingProject.createFilter(IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FOLDERS,
				matcherDescription, 0, createTestMonitor());
		assertThat(existingProject.getFilters()).hasSize(1);

		existingProject.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());

		IPath newPath = existingProject.getFullPath().removeLastSegments(1).append(existingProject.getName() + "_moved");
		existingProject.move(newPath, true, createTestMonitor());

		IProject newProject = ResourcesPlugin.getWorkspace().getRoot().getProject(existingProject.getName() + "_moved");
		assertThat(newProject).matches(IProject::exists, "exists");
		assertThat(newProject.getFilters()).hasSize(1);

		newPath = newProject.getFullPath().removeLastSegments(1).append(newProject.getName() + "_copy");
		newProject.copy(newPath, true, createTestMonitor());

		newProject = ResourcesPlugin.getWorkspace().getRoot().getProject(newProject.getName() + "_copy");
		assertThat(newProject).matches(IProject::exists, "exists");
		assertThat(newProject.getFilters()).hasSize(1);
	}

	/**
	 * Regression test for bug 328464
	 */
	@Test
	public void test328464() throws CoreException {
		IFolder folder = existingProject.getFolder(createUniqueString());
		createInWorkspace(folder);

		IFile file_a_txt = folder.getFile("a.txt");
		createInWorkspace(file_a_txt);

		FileInfoMatcherDescription matcherDescription = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER,
				"a\\.txt");
		existingProject.createFilter(IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FILES
				| IResourceFilterDescription.INHERITABLE, matcherDescription, 0, createTestMonitor());
		IWorkspace workspace = getWorkspace();

		assertThat(file_a_txt).matches(it -> !workspace.validateFiltered(it).isOK(), "is filtered");

		// rename a.txt to A.txt in the file system
		File ioFile = file_a_txt.getLocation().toFile();
		assertThat(ioFile).exists();
		ioFile.renameTo(new File(file_a_txt.getLocation().removeLastSegments(1).append("A.txt").toString()));

		assertThat(file_a_txt).matches(it -> !workspace.validateFiltered(it).isOK(), "is filtered");

		folder.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());

		assertThat(file_a_txt).matches(not(IFile::exists), "does not exists");
		assertThat(file_a_txt).matches(it -> !workspace.validateFiltered(it).isOK(), "is filtered");

		IFile file_A_txt = folder.getFile("A.txt");
		assertThat(file_A_txt).matches(IFile::exists, "exists");
		assertThat(file_A_txt).matches(it -> workspace.validateFiltered(it).isOK(), "is not filtered");
	}

	/**
	 * Regression test for bug 343914
	 */
	@Test
	public void test343914() throws CoreException {
		String subProjectName = "subProject";
		IPath subProjectLocation = existingProject.getLocation().append(subProjectName);

		FileInfoMatcherDescription matcherDescription = new FileInfoMatcherDescription(REGEX_FILTER_PROVIDER,
				subProjectName);
		existingProject.createFilter(
				IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FOLDERS
						| IResourceFilterDescription.FILES | IResourceFilterDescription.INHERITABLE,
				matcherDescription, 0, createTestMonitor());

		IPath fileLocation = subProjectLocation.append("file.txt");

		IWorkspaceRoot root = getWorkspace().getRoot();
		IFile result = root.getFileForLocation(fileLocation);

		assertThat(result).isNull();

		IFile[] results = root.findFilesForLocation(fileLocation);

		assertThat(results).isEmpty();

		IPath containerLocation = subProjectLocation.append("folder");
		IContainer resultContainer = root.getContainerForLocation(containerLocation);

		assertThat(resultContainer).isNull();

		IContainer[] resultsContainer = root.findContainersForLocation(containerLocation);

		assertThat(resultsContainer).isEmpty();

		IProject subProject = root.getProject(subProjectName);

		IProjectDescription newProjectDescription = getWorkspace().newProjectDescription(subProjectName);
		newProjectDescription.setLocation(subProjectLocation);

		subProject.create(newProjectDescription, createTestMonitor());
		result = root.getFileForLocation(fileLocation);

		assertThat(result).isNotNull().extracting(IFile::getProject).isEqualTo(subProject);

		results = root.findFilesForLocation(fileLocation);
		assertThat(results).hasSize(1).satisfiesExactly(it -> assertThat(it.getProject()).isEqualTo(subProject));

		resultContainer = root.getContainerForLocation(containerLocation);

		assertThat(resultContainer).isNotNull().extracting(IContainer::getProject).isEqualTo(subProject);

		resultsContainer = root.findContainersForLocation(containerLocation);

		assertThat(resultsContainer).hasSize(1)
				.satisfiesExactly(it -> assertThat(it.getProject()).isEqualTo(subProject));
	}

}