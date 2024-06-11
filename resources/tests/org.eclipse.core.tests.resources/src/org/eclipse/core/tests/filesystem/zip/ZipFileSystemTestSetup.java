/*******************************************************************************
 * Copyright (c) 2024 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: Vector Informatik GmbH - initial API and implementation
 *******************************************************************************/

package org.eclipse.core.tests.filesystem.zip;

import static org.eclipse.core.tests.filesystem.zip.ZipFileSystemTestUtil.ensureExists;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;

class ZipFileSystemTestSetup {

	static final String FIRST_PROJECT_NAME = "TestProject";
	static final String SECOND_PROJECT_NAME = "SecondProject";
	static final String ZIP_FILE_VIRTUAL_FOLDER_NAME = "BasicText.zip"; // Assuming the ZIP is represented as this
																		// folder
	static final String EMPTY_ZIP_FILE_NAME = "Empty.zip";
	static final String NESTED_ZIP_FILE_PARENT_NAME = "NestedZipFileParent.zip";
	static final String NESTED_ZIP_FILE_CHILD_NAME = "NestedZipFileChild.zip";
	static final String TEXT_FILE_NAME = "Text.txt";
	static final String DEEP_NESTED_ZIP_FILE_NAME = "DeepNested.zip";
	static final String FAKE_ZIP_FILE_NAME = "Fake.zip";
	static final String PASSWORD_PROTECTED_ZIP_FILE_NAME = "PasswordProtected.zip";
	static IProject firstProject;
	static IProject secondProject;
	static IProgressMonitor progressMonitor = new NullProgressMonitor();

	static void defaultSetup() throws Exception {
		String[] defaultZipFileNames = { ZIP_FILE_VIRTUAL_FOLDER_NAME };
		setup(defaultZipFileNames);
	}

	static void setup(String[] zipFileNames) throws Exception {
		firstProject = createProject(FIRST_PROJECT_NAME);
		refreshProject(firstProject);
		for (String zipFileName : zipFileNames) {
			copyZipFileIntoProject(firstProject, zipFileName);
			refreshProject(firstProject);
			ZipFileSystemTestUtil.openZipFile(firstProject.getFile(zipFileName));
		}
	}

	static void setupWithTwoProjects() throws Exception {
		defaultSetup();
		secondProject = createProject(SECOND_PROJECT_NAME);
		refreshProject(secondProject);
		refreshEntireWorkspace();
	}

	static void teardown() throws Exception {
		deleteProject(firstProject);
		deleteProject(secondProject);
	}

	private static void deleteProject(IProject project) throws CoreException {
		if (project != null && project.exists()) {
			project.delete(true, true, progressMonitor);
			project = null;
		}
	}

	static IProject createProject(String projectName) throws CoreException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject project = workspace.getRoot().getProject(projectName);

		if (!project.exists()) {
			project.create(progressMonitor);
		}
		project.open(progressMonitor);
		return project;
	}

	private static void refreshProject(IProject project) {
		try {
			if (project.exists() && project.isOpen()) {
				// Refreshing the specific project
				project.refreshLocal(IResource.DEPTH_INFINITE, null);
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	public static void refreshEntireWorkspace() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		try {
			// IResource.DEPTH_INFINITE will cause all resources in the workspace to be
			// refreshed.
			workspace.getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	static void copyZipFileIntoProject(IProject project, String zipFileName) throws IOException, CoreException {
		// Resolve the source file URL from the plugin bundle
		URL zipFileUrl = Platform.getBundle("org.eclipse.core.tests.resources")
				.getEntry("resources/ZipFileSystem/" + zipFileName);
		// Ensure proper conversion from URL to URI to Path
		URL resolvedURL = FileLocator.resolve(zipFileUrl); // Resolves any redirection or bundling
		java.nio.file.Path sourcePath;
		try {
			// Convert URL to URI to Path correctly handling spaces and special characters
			URI resolvedURI = resolvedURL.toURI();
			sourcePath = Paths.get(resolvedURI);
		} catch (URISyntaxException e) {
			throw new IOException("Failed to resolve URI for the ZIP file", e);
		}

		// Determine the target location within the project
		java.nio.file.Path targetPath = Paths.get(project.getLocation().toOSString(), zipFileName);

		// Copy the file using java.nio.file.Files
		Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

		// Refresh the project to make Eclipse aware of the new file
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
	}

	static void copyAndOpenNestedZipFileIntoProject() throws IOException, CoreException, URISyntaxException {
		copyZipFileIntoProject(firstProject, NESTED_ZIP_FILE_PARENT_NAME);
		IFile nestedZipFileParent = firstProject.getFile(NESTED_ZIP_FILE_PARENT_NAME);
		ensureExists(nestedZipFileParent);
		ZipFileSystemTestUtil.openZipFile(nestedZipFileParent);
		IFolder openedNestedZipFileParent = firstProject.getFolder(NESTED_ZIP_FILE_PARENT_NAME);
		ensureExists(openedNestedZipFileParent);
		IFile nestedZipFileChild = openedNestedZipFileParent.getFile(NESTED_ZIP_FILE_CHILD_NAME);
		ensureExists(nestedZipFileChild);
		ZipFileSystemTestUtil.openZipFile(nestedZipFileChild);
		IFolder openedNestedZipFileChild = openedNestedZipFileParent
				.getFolder(NESTED_ZIP_FILE_CHILD_NAME);
		ensureExists(openedNestedZipFileChild);
	}
}
