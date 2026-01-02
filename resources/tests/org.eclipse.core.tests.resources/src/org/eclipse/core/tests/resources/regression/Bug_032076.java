/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.regression;

import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomString;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createUniqueString;
import static org.eclipse.core.tests.resources.ResourceTestUtil.getFileStore;
import static org.eclipse.core.tests.resources.ResourceTestUtil.isReadOnlySupported;
import static org.eclipse.core.tests.resources.ResourceTestUtil.removeFromFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.setReadOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.Predicate;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.resources.Resource;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform.OS;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

/**
 * When moving a resource "x" from parent "a" to parent "b", if "x" or any of
 * its children can't be deleted, both "a" and "b" become out-of-sync and resource info is lost.
 */
@ExtendWith(WorkspaceResetExtension.class)
public class Bug_032076 {

	private static final Predicate<IResource> isSynchronizedDepthInfinite = resource -> resource
			.isSynchronized(IResource.DEPTH_INFINITE);

	private static final Predicate<IResource> isSynchronizedDepthZero = resource -> resource
			.isSynchronized(IResource.DEPTH_ZERO);

	@Test
	public void testFileBugOnWindows() throws Exception {
		assumeTrue(OS.isWindows(), "only relevant on Windows");

		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject(createUniqueString());
		IFolder sourceParent = project.getFolder("source_parent");
		IFolder destinationParent = project.getFolder("destination_parent");
		// this file will be made irremovable
		IFile sourceFile = sourceParent.getFile("file1.txt");
		IFile destinationFile = destinationParent.getFile(sourceFile.getName());
		createInWorkspace(new IResource[] { sourceFile, destinationParent });

		// add a marker to a file to ensure the move operation is not losing anything
		String attributeKey = createRandomString();
		String attributeValue = createRandomString();
		long markerId = -1;
		IMarker bookmark = sourceFile.createMarker(IMarker.BOOKMARK);
		bookmark.setAttribute(attributeKey, attributeValue);
		markerId = bookmark.getId();

		// opens the file so it cannot be removed on Windows
		try (InputStream input = sourceFile.getContents()) {
			assertThrows(CoreException.class,
					() -> sourceFile.move(destinationFile.getFullPath(), IResource.FORCE, createTestMonitor()));

			// the source parent is in sync
			assertThat(sourceParent).matches(isSynchronizedDepthInfinite, "is synchronized");
			// the target parent is in sync
			assertThat(destinationParent).matches(isSynchronizedDepthInfinite, "is synchronized");

			// file has been copied to destination
			assertThat(destinationFile).matches(IResource::exists, "exists");

			// ensure marker info has not been lost
			IMarker marker = destinationFile.findMarker(markerId);
			assertNotNull(marker);
			assertEquals(attributeValue, marker.getAttribute(attributeKey));

			// non-removable file has been moved (but not in file system - they are out-of-sync)
			assertThat(sourceFile).matches(IResource::exists, "exists");
			assertThat(sourceFile).matches(isSynchronizedDepthZero, "is synchronized");

			// refresh the source parent
			sourceParent.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());

			// file is still found in source tree
			assertThat(sourceFile).matches(IResource::exists, "exists");
		}
	}

	@Test
	public void testFolderBugOnWindows() throws Exception {
		assumeTrue(OS.isWindows(), "only relevant on Windows");

		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject(createUniqueString());
		IFolder sourceParent = project.getFolder("source_parent");
		IFolder destinationParent = project.getFolder("destination_parent");
		IFolder folder = sourceParent.getFolder("folder");
		IFolder destinationFolder = destinationParent.getFolder(folder.getName());
		// this file will be made un-removable
		IFile file1 = folder.getFile("file1.txt");
		// but not this one
		IFile file2 = folder.getFile("file2.txt");
		createInWorkspace(new IResource[] { file1, file2, destinationParent });

		// add a marker to a file to ensure the move operation is not losing anything
		String attributeKey = createRandomString();
		String attributeValue = createRandomString();
		long markerId = -1;
		IMarker bookmark = file1.createMarker(IMarker.BOOKMARK);
		bookmark.setAttribute(attributeKey, attributeValue);
		markerId = bookmark.getId();

		// opens a file so it (and its parent) cannot be removed on Windows
		try (InputStream input = file1.getContents()) {
			assertThrows(CoreException.class,
					() -> folder.move(destinationFolder.getFullPath(), IResource.FORCE, createTestMonitor()));

			// the source parent is in sync
			assertThat(sourceParent).matches(isSynchronizedDepthInfinite, "is synchronized");
			// the target parent is in-sync
			assertThat(destinationParent).matches(isSynchronizedDepthInfinite, "is synchronized");

			// resources have been copied to destination
			assertThat(destinationFolder).matches(IResource::exists, "exists");
			assertThat(destinationFolder.getFile(file1.getName())).matches(IResource::exists, "exists");
			assertThat(destinationFolder.getFile(file2.getName())).matches(IResource::exists, "exists");

			// ensure marker info has not been lost
			IMarker marker = destinationFolder.getFile(file1.getName()).findMarker(markerId);
			assertNotNull(marker);
			assertEquals(attributeValue, marker.getAttribute(attributeKey));

			// non-removable resources still exist in source
			assertThat(folder).matches(IResource::exists, "exists");
			assertThat(file1).matches(IResource::exists, "exists");
			//this file should be successfully moved
			assertThat(file2).matches(not(IResource::exists), "not exists");

			// refresh the source parent
			sourceParent.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());

			// non-removable resources still in source tree
			assertThat(folder).matches(IResource::exists, "exists");
			assertThat(file1).matches(IResource::exists, "exists");
			assertThat(file2).matches(not(IResource::exists), "not exists");
		}
	}

	@Test
	public void testProjectBugOnWindows() throws Exception {
		assumeTrue(OS.isWindows(), "only relevant on Windows");

		IWorkspace workspace = getWorkspace();
		IProject sourceProject = workspace.getRoot().getProject(createUniqueString() + ".source");
		IProject destinationProject = workspace.getRoot().getProject(createUniqueString() + ".dest");
		// this file will be made un-removable
		IFile file1 = sourceProject.getFile("file1.txt");
		// but not this one
		IFile file2 = sourceProject.getFile("file2.txt");
		createInWorkspace(new IResource[] {file1, file2});
		File originalSourceProjectLocation = sourceProject.getLocation().toFile();

		// add a marker to a file to ensure the move operation is not losing anything
		String attributeKey = createRandomString();
		String attributeValue = createRandomString();
		long markerId = -1;
		IMarker bookmark = file1.createMarker(IMarker.BOOKMARK);
		bookmark.setAttribute(attributeKey, attributeValue);
		markerId = bookmark.getId();

		// opens a file so it (and its parent) cannot be removed on Windows
		try (InputStream input = file1.getContents()) {
			assertThrows(CoreException.class,
					() -> sourceProject.move(destinationProject.getFullPath(), IResource.FORCE, createTestMonitor()));

			// the source does not exist
			assertThat(sourceProject).matches(not(IResource::exists), "not exists");
			assertThat(sourceProject).matches(isSynchronizedDepthInfinite, "is synchronized");
			// the target exists and is in sync
			assertThat(destinationProject).matches(IResource::exists, "exists");
			assertThat(destinationProject).matches(isSynchronizedDepthInfinite, "is synchronized");

			// resources have been copied to destination
			assertThat(destinationProject.getFile(file1.getProjectRelativePath())).matches(IResource::exists, "exists");
			assertThat(destinationProject.getFile(file2.getProjectRelativePath())).matches(IResource::exists, "exists");

			// ensure marker info has not been lost
			IMarker marker = destinationProject.getFile(file1.getProjectRelativePath()).findMarker(markerId);
			assertNotNull(marker);
			assertEquals(attributeValue, marker.getAttribute(attributeKey));
			assertThat(workspace.getRoot()).matches(isSynchronizedDepthInfinite, "is synchronized");
		} finally {
			removeFromFileSystem(originalSourceProjectLocation);
		}
	}

	@Test
	@Disabled("test is currently failing and needs further investigation (bug 203078)")
	public void testFileBugOnLinux() throws CoreException {
		assumeTrue(OS.isLinux() && isReadOnlySupported(), "only relevant on Linux");

		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject(createUniqueString());
		IFolder sourceParent = project.getFolder("source_parent");
		IFolder roFolder = sourceParent.getFolder("sub-folder");
		IFolder destinationParent = project.getFolder("destination_parent");
		// this file will be made un-removable
		IFile sourceFile = roFolder.getFile("file.txt");
		IFile destinationFile = destinationParent.getFile("file.txt");
		createInWorkspace(new IResource[] { sourceFile, destinationParent });

		IFileStore roFolderStore = ((Resource) roFolder).getStore();

		// add a marker to a file to ensure the move operation is not losing anything
		String attributeKey = createRandomString();
		String attributeValue = createRandomString();
		long markerId = -1;
		IMarker bookmark = sourceFile.createMarker(IMarker.BOOKMARK);
		bookmark.setAttribute(attributeKey, attributeValue);
		markerId = bookmark.getId();

		try {
			// mark sub-folder as read-only so its immediate children cannot be removed on Linux
			setReadOnly(roFolder, true);
			assertThrows(CoreException.class,
					() -> sourceFile.move(destinationFile.getFullPath(), IResource.FORCE, createTestMonitor()));

			// the source parent is out-of-sync
			assertThat(sourceParent).matches(not(isSynchronizedDepthInfinite), "is not synchronized");
			// the target parent is in-sync
			assertThat(destinationParent).matches(isSynchronizedDepthInfinite, "is synchronized");

			// file has been copied to destination
			assertThat(destinationFile).matches(IResource::exists, "exists");

			// ensure marker info has not been lost
			IMarker marker = destinationFile.findMarker(markerId);
			assertNotNull(marker);
			assertEquals(attributeValue, marker.getAttribute(attributeKey));

			// non-removable file has been moved (but not in file system - they are out-of-sync)
			assertThat(sourceFile).matches(not(IResource::exists), "not exists");

			// refresh the source parent
			sourceParent.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());

			// non-removable file now reappear in the resource tree
			assertThat(sourceFile).matches(IResource::exists, "exists");
		} finally {
			setReadOnly(roFolderStore, false);
		}
	}

	@Test
	@Disabled("test is currently failing and needs further investigation (bug 203078)")
	public void testFolderBugOnLinux() throws CoreException {
		assumeTrue(OS.isLinux() && isReadOnlySupported(), "only relevant on Linux");

		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject(createUniqueString());
		IFolder sourceParent = project.getFolder("source_parent");
		IFolder roFolder = sourceParent.getFolder("sub-folder");
		IFolder folder = roFolder.getFolder("folder");
		IFile file1 = roFolder.getFile("file1.txt");
		IFile file2 = folder.getFile("file2.txt");
		IFolder destinationParent = project.getFolder("destination_parent");
		IFolder destinationROFolder = destinationParent.getFolder(roFolder.getName());
		createInWorkspace(new IResource[] { file1, file2, destinationParent });

		IFileStore roFolderLocation = ((Resource) roFolder).getStore();
		IFileStore destinationROFolderLocation = ((Resource) destinationROFolder).getStore();

		// add a marker to a file to ensure the move operation is not losing anything
		String attributeKey = createRandomString();
		String attributeValue = createRandomString();
		long markerId = -1;
		IMarker bookmark = file1.createMarker(IMarker.BOOKMARK);
		bookmark.setAttribute(attributeKey, attributeValue);
		markerId = bookmark.getId();

		try {
			// mark sub-folder as read-only so its immediate children cannot be removed on Linux
			setReadOnly(roFolder, true);

			assertThrows(CoreException.class, () -> roFolder
					.move(destinationParent.getFullPath().append(roFolder.getName()), IResource.FORCE, createTestMonitor()));

			// the source parent is out-of-sync
			assertThat(sourceParent).matches(not(isSynchronizedDepthInfinite), "is not synchronized");
			// the target parent is in-sync
			assertThat(destinationParent).matches(isSynchronizedDepthInfinite, "is synchronized");

			// resources have been copied to destination
			IFolder destinationFolder = destinationROFolder.getFolder(folder.getName());
			IFile destinationFile1 = destinationROFolder.getFile(file1.getName());
			IFile destinationFile2 = destinationFolder.getFile(file2.getName());
			assertThat(destinationROFolder).matches(IResource::exists, "exists");
			assertThat(destinationFolder).matches(IResource::exists, "exists");
			assertThat(destinationFile1).matches(IResource::exists, "exists");
			assertThat(destinationFile2).matches(IResource::exists, "exists");

			// ensure marker info has not been lost
			IMarker marker = destinationROFolder.getFile(file1.getName()).findMarker(markerId);
			assertNotNull(marker);
			assertEquals(attributeValue, marker.getAttribute(attributeKey));

			// non-removable resources have been moved (but not in file system - they are out-of-sync)
			assertThat(roFolder).matches(not(IResource::exists), "not exists");
			assertThat(folder).matches(not(IResource::exists), "not exists");
			assertThat(file1).matches(not(IResource::exists), "not exists");
			assertThat(file2).matches(not(IResource::exists), "not exists");

			// refresh the source parent
			sourceParent.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());

			// non-removed resources now reappear in the resource tree
			assertThat(roFolder).matches(IResource::exists, "exists");
			assertThat(folder).matches(IResource::exists, "exists");
			assertThat(file1).matches(IResource::exists, "exists");
			assertThat(file2).matches(not(IResource::exists), "not exists");
		} finally {
			setReadOnly(roFolderLocation, false);
			setReadOnly(destinationROFolderLocation, false);
		}
	}

	@Test
	@Disabled("test is currently failing and needs further investigation (bug 203078)")
	public void testProjectBugOnLinux(@TempDir Path tempDirectory) throws CoreException, IOException {
		assumeTrue(OS.isLinux() && isReadOnlySupported(), "only relevant on Linux");

		IWorkspace workspace = getWorkspace();
		IProject sourceProject = workspace.getRoot().getProject(createUniqueString() + ".source");
		IFileStore projectParentStore = getFileStore(tempDirectory);
		IFileStore projectStore = projectParentStore.getChild(sourceProject.getName());
		IProjectDescription sourceDescription = workspace.newProjectDescription(sourceProject.getName());
		sourceDescription.setLocationURI(projectStore.toURI());

		IProject destinationProject = workspace.getRoot().getProject(createUniqueString() + ".dest");
		IProjectDescription destinationDescription = workspace.newProjectDescription(destinationProject.getName());

		// create and open the source project at a non-default location
		sourceProject.create(sourceDescription, createTestMonitor());
		sourceProject.open(createTestMonitor());

		IFile file1 = sourceProject.getFile("file1.txt");

		createInWorkspace(new IResource[] { file1 });
		File originalSourceProjectLocation = sourceProject.getLocation().toFile();

		// add a marker to a file to ensure the move operation is not losing anything
		String attributeKey = createRandomString();
		String attributeValue = createRandomString();
		long markerId = -1;
		IMarker bookmark = file1.createMarker(IMarker.BOOKMARK);
		bookmark.setAttribute(attributeKey, attributeValue);
		markerId = bookmark.getId();

		try {
			// mark sub-folder as read-only so its immediate children cannot be removed on Linux
			setReadOnly(projectParentStore, true);

			assertThrows(CoreException.class,
					() -> sourceProject.move(destinationDescription, IResource.FORCE, createTestMonitor()));

			// the source does not exist
			assertThat(sourceProject).matches(not(IResource::exists), "not exists");
			// the target exists and is in sync
			assertThat(destinationProject).matches(IResource::exists, "exists");
			assertThat(destinationProject).matches(isSynchronizedDepthInfinite, "is synchronized");

			// resources have been copied to destination
			assertThat(destinationProject.getFile(file1.getProjectRelativePath())).matches(IResource::exists, "exists");

			// ensure marker info has not been lost
			IMarker marker = destinationProject.getFile(file1.getProjectRelativePath()).findMarker(markerId);
			assertNotNull(marker);
			assertEquals(attributeValue, marker.getAttribute(attributeKey));
			// project's content area still exists in file system
			assertThat(projectStore).matches(it -> it.fetchInfo().exists(), "exists");

			assertThat(workspace.getRoot()).matches(isSynchronizedDepthInfinite, "is synchronized");
		} finally {
			setReadOnly(projectParentStore, false);
			removeFromFileSystem(originalSourceProjectLocation);
		}
	}

}
