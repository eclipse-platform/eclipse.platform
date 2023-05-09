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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.internal.resources.ProjectDescription;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.resources.ResourceTest;


/**
 * Tests for recursive symbolic links in projects.
 */
public class Bug_185247_recursiveLinks extends ResourceTest {

	private final List<IProject> testProjects = new ArrayList<>();

	@Override
	protected void tearDown() throws Exception {
		try {
			cleanUpTestProjects();
		} finally {
			super.tearDown();
		}
	}

	private void cleanUpTestProjects() throws CoreException {
		for (IProject testProject : testProjects) {
			testProject.delete(false, true, getMonitor());
		}
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
	public void test1_linkCurrentDirectory() throws Exception {
		CreateTestProjectStructure createSymlinks = directory -> {
			createSymlink(directory, "link_current", "./");
		};

		runTest(createSymlinks);
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
	public void test2_linkParentDirectory() throws Exception {
		CreateTestProjectStructure createSymlinks = directory -> {
			createSymlink(directory, "link_parent", "../");
		};

		runTest(createSymlinks);
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
	public void test3_linkGrandparentDirectory() throws Exception {
		CreateTestProjectStructure createSymlinks = directory -> {
			File subdirectory = new File(directory, "subdirectory");
			createDirectory(subdirectory);
			createSymlink(subdirectory, "link_grandparent", "../../");
		};

		runTest(createSymlinks);
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
	public void test4_linkParentDirectoryTwice() throws Exception {
		CreateTestProjectStructure createSymlinks = directory -> {
			String[] subdirectoryNames = { "subdirectory1", "subdirectory2" };
			for (String subdirectoryName : subdirectoryNames) {
				File subdirectory = new File(directory, subdirectoryName);
				createDirectory(subdirectory);
				createSymlink(subdirectory, "link_parent", "../../");
			}
		};

		runTest(createSymlinks);
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
	public void test5_linkParentDirectoyTwiceWithAbsolutePath() throws Exception {
		CreateTestProjectStructure createSymlinks = directory -> {
			String[] subdirectoryNames = { "subdirectory1", "subdirectory2" };
			for (String subdirectoryName : subdirectoryNames) {
				File subdirectory = new File(directory, subdirectoryName);
				createDirectory(subdirectory);
				createSymlink(subdirectory, "link_parent", directory.getAbsolutePath());
			}
		};

		runTest(createSymlinks);
	}

	private void runTest(CreateTestProjectStructure createSymlinks) throws MalformedURLException, Exception {
		if (!canCreateSymLinks()) {
			/*
			 * we don't run this test case on platforms that have no symlinks, since we want
			 * to test recursive symlinks
			 */
			return;
		}

		String projectName = getName();
		IPath testRoot = getRandomLocation();
		deleteOnTearDown(testRoot);
		IPath projectRoot = testRoot.append("bug185247recursive").append(projectName);
		File directory = projectRoot.append("directory").toFile();
		createDirectory(directory);

		createSymlinks.accept(directory);


		URI projectRootLocation = URIUtil.toURI((projectRoot));
		// refreshing the project with recursive symlinks should not hang
		importProjectAndRefresh(projectName, projectRootLocation);
	}

	private static void createDirectory(File directory) {
		assertTrue("failed to create test directory: " + directory, directory.mkdirs());
	}

	void createSymlink(File directory, String linkName, String linkTarget) {
		assertTrue("symlinks not supported by platform", canCreateSymLinks());
		boolean isDir = true;
		createSymLink(directory, linkName, linkTarget, isDir);
	}

	private void importProjectAndRefresh(String projectName, URI projectRootLocation) throws Exception {
		IProject project = importTestProject(projectName, projectRootLocation);
		project.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
	}

	private IProject importTestProject(String projectName, URI projectRootLocation) throws Exception {
		IProject testProject = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		testProjects.add(testProject);
		IProjectDescription projectDescription = new ProjectDescription();
		projectDescription.setName(projectName);
		projectDescription.setLocationURI(projectRootLocation);
		testProject.create(projectDescription, getMonitor());
		testProject.open(getMonitor());
		assertTrue("expected project to be open: " + projectName, testProject.isAccessible());
		return testProject;
	}

	interface CreateTestProjectStructure extends Consumer<File> {
		/*
		 * we give a class name for the runnable that we use to create different project
		 * structures in different tests.
		 */
	}
}
