/*******************************************************************************
 *  Copyright (c) 2018 Simeon Andreev and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *     Simeon Andreev - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources.regression;

import static org.eclipse.core.tests.harness.FileSystemHelper.canCreateSymLinks;
import static org.eclipse.core.tests.harness.FileSystemHelper.createSymLink;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Path;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.internal.localstore.UnifiedTree;
import org.eclipse.core.internal.resources.ProjectDescription;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for recursive symbolic links in projects.
 */
@ExtendWith(WorkspaceResetExtension.class)
public class Bug_185247_recursiveLinks {

	private @TempDir Path tempDirectory;

	private String testMethodName;

	@BeforeEach
	public void requireCanCreateSymlinks(TestInfo testInfo) throws IOException {
		assumeTrue(canCreateSymLinks(), "only relevant for platforms supporting symbolic links");
		testMethodName = testInfo.getTestMethod().get().getName();
	}

	/**
	 * Test project structure:
	 *
	 * <pre>
	 * project root
	 *   |
	 *   |-- directory
	 *         |
	 *         |-- link_current -&gt; ./ (links "directory")
	 * </pre>
	 */
	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	public void test1_linkCurrentDirectory(boolean useAdvancedLinkCheck) throws Exception {
		CreateTestProjectStructure createSymlinks = directory -> {
			createSymlink(directory, "link_current", "./");
		};

		runTest(createSymlinks, useAdvancedLinkCheck);
	}

	/**
	 * Test project structure:
	 *
	 * <pre>
	 * project root
	 *   |
	 *   |-- directory
	 *         |
	 *         |-- link_parent -&gt; ../ (links "project root")
	 * </pre>
	 */
	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	public void test2_linkParentDirectory(boolean useAdvancedLinkCheck) throws Exception {
		CreateTestProjectStructure createSymlinks = directory -> {
			createSymlink(directory, "link_parent", "../");
		};

		runTest(createSymlinks, useAdvancedLinkCheck);
	}

	/**
	 * Test project structure:
	 *
	 * <pre>
	 * project root
	 *   |
	 *   |-- directory
	 *         |
	 *         |-- subdirectory
	 *              |
	 *              |-- link_grandparent -&gt; ../../ (links "project root")
	 * </pre>
	 */
	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	public void test3_linkGrandparentDirectory(boolean useAdvancedLinkCheck) throws Exception {
		CreateTestProjectStructure createSymlinks = directory -> {
			File subdirectory = new File(directory, "subdirectory");
			createDirectory(subdirectory);
			createSymlink(subdirectory, "link_grandparent", "../../");
		};

		runTest(createSymlinks, useAdvancedLinkCheck);
	}

	/**
	 * Test project structure:
	 *
	 * <pre>
	 * project root
	 *   |
	 *   |-- directory
	 *         |
	 *         |-- subdirectory1
	 *         |    |
	 *         |    |-- link_parent -&gt; ../ (links directory)
	 *         |
	 *         |-- subdirectory2
	 *              |
	 *              |-- link_parent -&gt; ../ (links directory)
	 * </pre>
	 */
	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	public void test4_linkParentDirectoryTwice(boolean useAdvancedLinkCheck) throws Exception {
		CreateTestProjectStructure createSymlinks = directory -> {
			String[] subdirectoryNames = { "subdirectory1", "subdirectory2" };
			for (String subdirectoryName : subdirectoryNames) {
				File subdirectory = new File(directory, subdirectoryName);
				createDirectory(subdirectory);
				createSymlink(subdirectory, "link_parent", "../../");
			}
		};

		runTest(createSymlinks, useAdvancedLinkCheck);
	}

	/**
	 * Test project structure:
	 *
	 * <pre>
	 * project root
	 *   |
	 *   |-- directory
	 *         |
	 *         |-- subdirectory1
	 *         |    |
	 *         |    |-- link_parent -&gt; /tmp/&lt;random string&gt;/bug185247recursive/test5_linkParentDirectoyTwiceWithAbsolutePath/directory
	 *         |
	 *         |-- subdirectory2
	 *              |
	 *              |-- link_parent -&gt; /tmp/&lt;random string&gt;/bug185247recursive/test5_linkParentDirectoyTwiceWithAbsolutePath/directory
	 * </pre>
	 */
	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	public void test5_linkParentDirectoyTwiceWithAbsolutePath(boolean useAdvancedLinkCheck) throws Exception {
		CreateTestProjectStructure createSymlinks = directory -> {
			String[] subdirectoryNames = { "subdirectory1", "subdirectory2" };
			for (String subdirectoryName : subdirectoryNames) {
				File subdirectory = new File(directory, subdirectoryName);
				createDirectory(subdirectory);
				createSymlink(subdirectory, "link_parent", directory.getAbsolutePath());
			}
		};

		runTest(createSymlinks, useAdvancedLinkCheck);
	}

	private void runTest(CreateTestProjectStructure createSymlinks, boolean useAdvancedLinkCheck)
			throws MalformedURLException, Exception {
		final boolean originalValue = UnifiedTree.isAdvancedRecursiveLinkChecksEnabled();
		try {
			UnifiedTree.enableAdvancedRecursiveLinkChecks(useAdvancedLinkCheck);
			String projectName = testMethodName;
			IPath testRoot = IPath.fromPath(tempDirectory);
			IPath projectRoot = testRoot.append("bug185247recursive").append(projectName);
			File directory = projectRoot.append("directory").toFile();
			createDirectory(directory);

			createSymlinks.accept(directory);

			URI projectRootLocation = URIUtil.toURI((projectRoot));
			// refreshing the project with recursive symlinks should not hang
			importProjectAndRefresh(projectName, projectRootLocation);
		} finally {
			UnifiedTree.enableAdvancedRecursiveLinkChecks(originalValue);
		}
	}

	private static void createDirectory(File directory) {
		assertTrue(directory.mkdirs(), "failed to create test directory: " + directory);
	}

	void createSymlink(File directory, String linkName, String linkTarget) throws IOException {
		assertTrue(canCreateSymLinks(), "symlinks not supported by platform");
		boolean isDir = true;
		createSymLink(directory, linkName, linkTarget, isDir);
	}

	private void importProjectAndRefresh(String projectName, URI projectRootLocation) throws Exception {
		IProject project = importTestProject(projectName, projectRootLocation);
		project.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
	}

	private IProject importTestProject(String projectName, URI projectRootLocation) throws Exception {
		IProject testProject = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		IProjectDescription projectDescription = new ProjectDescription();
		projectDescription.setName(projectName);
		projectDescription.setLocationURI(projectRootLocation);
		testProject.create(projectDescription, createTestMonitor());
		testProject.open(createTestMonitor());
		assertTrue(testProject.isAccessible(), "expected project to be open: " + projectName);
		return testProject;
	}

	interface CreateTestProjectStructure {
		void accept(File file) throws Exception;
	}

}
