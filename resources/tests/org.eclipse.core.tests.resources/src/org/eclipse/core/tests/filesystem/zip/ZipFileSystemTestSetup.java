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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.ZipFileTransformer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;

class ZipFileSystemTestSetup {
	static final String ZIP_FILE_NAME = "BasicText.zip";
	static final String JAR_FILE_NAME = "BasicText.jar";
	static final String WAR_FILE_NAME = "BasicText.war";

	static final String EMPTY_ZIP_FILE_NAME = "Empty.zip";
	static final String FAKE_ZIP_FILE_NAME = "Fake.zip";
	static final String PASSWORD_PROTECTED_ZIP_FILE_NAME = "PasswordProtected.zip";
	static final String NESTED_ZIP_FILE_PARENT_NAME = "NestedZipFileParent.zip";
	static final String NESTED_ZIP_FILE_CHILD_NAME = "NestedZipFileChild.zip";
	static final String DEEP_NESTED_ZIP_FILE_NAME = "DeepNested.zip";

	static final String TEXT_FILE_NAME = "Text.txt";

	static List<IProject> projects = new ArrayList<>();
	static List<String> zipFileNames = List.of(ZIP_FILE_NAME, JAR_FILE_NAME, WAR_FILE_NAME);

	public static Stream<String> zipFileNames() {
		return zipFileNames.stream();
	}

	static void setup() throws Exception {
		for (int i = 0; i <= 1; i++) {
			projects.add(createProject("Project" + i));
			for (String zipFileName : zipFileNames) {
				copyZipFileIntoProject(projects.get(i), zipFileName);
				ZipFileTransformer.openZipFile(projects.get(i).getFile(zipFileName), true);
			}
		}
	}

	static void teardown() throws Exception {
		deleteProjects();
	}

	private static void deleteProjects() throws CoreException {
		for (IProject project : projects) {
			if (project != null && project.exists()) {
				project.delete(true, true, new NullProgressMonitor());
				project = null;
			}
		}
	}

	static IProject createProject(String projectName) throws CoreException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject project = workspace.getRoot().getProject(projectName);

		if (!project.exists()) {
			project.create(new NullProgressMonitor());
		}
		project.open(new NullProgressMonitor());
		return project;
	}

	static void copyZipFileIntoProject(IProject project, String zipFileName) throws IOException, CoreException {
		try {
			URL zipFileUrl = Platform.getBundle("org.eclipse.core.tests.resources")
					.getEntry("resources/ZipFileSystem/" + zipFileName);
			Path sourcePath = Paths.get(FileLocator.resolve(zipFileUrl).toURI());
			Path targetPath = project.getLocation().append(zipFileName).toFile().toPath();
			Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
			project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		} catch (URISyntaxException e) {
			throw new IOException("Failed to resolve URI for the ZIP file", e);
		}
	}

	static void ensureExistence(IResource resource, boolean shouldExist) throws CoreException, IOException {
		IFileStore fileStore = EFS.getStore(resource.getLocationURI());
		boolean fileStoreExists = fileStore.fetchInfo().exists();
		assertTrue("File store existence check failed for: " + fileStore, fileStoreExists == shouldExist);

		if (resource instanceof IFile file) {
			assertTrue("File existence check failed for: " + file, file.exists() == shouldExist);
		} else if (resource instanceof IFolder folder) {
			assertTrue("Folder existence check failed for: " + folder, folder.exists() == shouldExist);
		}
	}

	static void ensureExists(IResource resource) throws CoreException, IOException {
		ensureExistence(resource, true);
	}

	static void ensureDoesNotExist(IResource resource) throws CoreException, IOException {
		ensureExistence(resource, false);
	}
}
