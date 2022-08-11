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
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import org.eclipse.core.internal.resources.ProjectDescription;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.tests.resources.ResourceTest;


/**
 * Test cases for symbolic links in projects.
 */
public class Bug_185247_LinuxTests extends ResourceTest {

	private static final boolean IS_LINUX = Platform.getOS().equals(Platform.OS_LINUX);
	private final List<IProject> testProjects = new ArrayList<>();
	private IPath testCasesLocation;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		IPath randomLocation = getRandomLocation();
		deleteOnTearDown(randomLocation);
		testCasesLocation = randomLocation.append("bug185247LinuxTests");
		assertTrue("failed to create test location: " + testCasesLocation, testCasesLocation.toFile().mkdirs());
		extractTestCasesArchive(testCasesLocation);
	}

	private void extractTestCasesArchive(IPath outputLocation) throws Exception {
		if (IS_LINUX) {
			URL testCasesArchive = Platform.getBundle("org.eclipse.core.tests.resources")
					.getEntry("resources/bug185247/bug185247_LinuxTests.zip");
			URL archiveLocation = FileLocator.resolve(testCasesArchive);
			File archive = URIUtil.toFile(archiveLocation.toURI());
			assertNotNull("cannot find archive with test cases", archive);
			unzip(archive, outputLocation.toFile());
		}
	}

	@Override
	protected void tearDown() throws Exception {
		try {
			for (IProject testProject : testProjects) {
				testProject.delete(false, true, getMonitor());
			}
		} finally {
			super.tearDown();
		}
	}

	/**
	 *
	 */
	public void test1_trivial() throws Exception {
		runProjectTestCase();
	}

	/**
	 *
	 */
	public void test2_mutual() throws Exception {
		runProjectTestCase();
	}

	/**
	 *
	 */
	public void test3_outside_tree() throws Exception {
		runProjectTestCase();
	}

	/**
	 *
	 */
	public void test5_transitive_mutual() throws Exception {
		runProjectTestCase();
	}

	/**
	 *
	 */
	public void test6_nonrecursive() throws Exception {
		runProjectTestCase();
	}

	private void runProjectTestCase() throws Exception {
		String projectName = getName();
		// refresh should hang, if bug 105554 re-occurs
		importProjectAndRefresh(projectName);
	}

	private void importProjectAndRefresh(String projectName) throws Exception {
		if (IS_LINUX) {
			IProject project = importTestProject(projectName);
			project.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		}
	}

	private IProject importTestProject(String projectName) throws Exception {
		IProject testProject = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		testProjects.add(testProject);
		IProjectDescription projectDescription = new ProjectDescription();
		projectDescription.setName(projectName);
		String projectRoot = String.join(File.separator, testCasesLocation.toOSString(), "bug185247", projectName);
		projectDescription.setLocationURI(URI.create(projectRoot));
		testProject.create(projectDescription, getMonitor());
		testProject.open(getMonitor());
		assertTrue("expected project to be open: " + projectName, testProject.isAccessible());
		return testProject;
	}

	private static void unzip(File archive, File outputDirectory) throws Exception {
		String[] command = { "unzip", archive.toString(), "-d", outputDirectory.toString() };
		executeCommand(command, outputDirectory);

	}

	private static void executeCommand(String[] command, File outputDirectory) throws Exception {
		assertTrue("output directory does not exist: " + outputDirectory, outputDirectory.exists());
		assertTrue("commands only availabe in Linux environment", IS_LINUX);
		ProcessBuilder processBuilder = new ProcessBuilder(command);
		File commandOutputFile = new File(outputDirectory, "commandOutput.txt");
		if (!commandOutputFile.exists()) {
			assertTrue("failed to create standard output and error file for unzip command",
					commandOutputFile.createNewFile());
		}
		processBuilder.redirectOutput(commandOutputFile);
		processBuilder.redirectError(commandOutputFile);
		Process process = processBuilder.start();
		int commandExitCode = process.waitFor();
		String output = formatCommandOutput(command, commandOutputFile);
		assertTrue("Failed to delete command output file. " + output, commandOutputFile.delete());
		assertEquals("Failed to execute commmand. " + output, 0, commandExitCode);
	}

	private static String formatCommandOutput(String[] command, File commandOutputFile) throws IOException {
		Path commandOutputPath = Paths.get(commandOutputFile.getAbsolutePath());
		List<String> commandOutputLines = Files.readAllLines(commandOutputPath);
		List<String> commandOutputHeader = Arrays.asList("Command:", Arrays.toString(command), "Output:");
		List<String> commandToString = new ArrayList<>();
		commandToString.addAll(commandOutputHeader);
		commandToString.addAll(commandOutputLines);
		String formattedOutput = String.join(System.lineSeparator(), commandToString);
		return formattedOutput;
	}
}