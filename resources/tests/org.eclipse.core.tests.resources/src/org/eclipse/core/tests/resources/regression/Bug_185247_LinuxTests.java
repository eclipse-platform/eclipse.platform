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

import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.eclipse.core.internal.localstore.UnifiedTree;
import org.eclipse.core.internal.resources.ProjectDescription;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Platform.OS;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test cases for symbolic links in projects.
 */
@ExtendWith(WorkspaceResetExtension.class)
public class Bug_185247_LinuxTests {
	private String testMethodName;

	private @TempDir Path tempDirectory;

	private IPath testCasesLocation;

	@BeforeEach
	public void setUp(TestInfo testInfo) throws Exception {
		assumeTrue(OS.isLinux(), "only relevant on Linux");

		testMethodName = testInfo.getTestMethod().get().getName();
		IPath randomLocation = IPath.fromPath(tempDirectory);
		testCasesLocation = randomLocation.append("bug185247LinuxTests");
		assertTrue(testCasesLocation.toFile().mkdirs(), "failed to create test location: " + testCasesLocation);
		extractTestCasesArchive(testCasesLocation);
	}

	private void extractTestCasesArchive(IPath outputLocation) throws Exception {
		URL testCasesArchive = Platform.getBundle("org.eclipse.core.tests.resources")
				.getEntry("resources/bug185247/bug185247_LinuxTests.zip");
		URL archiveLocation = FileLocator.resolve(testCasesArchive);
		File archive = URIUtil.toFile(archiveLocation.toURI());
		assertNotNull(archive, "cannot find archive with test cases");
		unzip(archive, outputLocation.toFile());
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	public void test1_trivial(boolean useAdvancedLinkCheck) throws Exception {
		runProjectTestCase(useAdvancedLinkCheck);
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	public void test2_mutual(boolean useAdvancedLinkCheck) throws Exception {
		runProjectTestCase(useAdvancedLinkCheck);
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	public void test3_outside_tree(boolean useAdvancedLinkCheck) throws Exception {
		runProjectTestCase(useAdvancedLinkCheck);
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	public void test5_transitive_mutual(boolean useAdvancedLinkCheck) throws Exception {
		runProjectTestCase(useAdvancedLinkCheck);
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	public void test6_nonrecursive(boolean useAdvancedLinkCheck) throws Exception {
		runProjectTestCase(useAdvancedLinkCheck);
	}

	private void runProjectTestCase(boolean useAdvancedLinkCheck) throws Exception {
		final boolean originalValue = UnifiedTree.isAdvancedRecursiveLinkChecksEnabled();
		try {
			UnifiedTree.enableAdvancedRecursiveLinkChecks(useAdvancedLinkCheck);
			// refresh should hang, if bug 105554 re-occurs
			importProjectAndRefresh(testMethodName);
		} finally {
			UnifiedTree.enableAdvancedRecursiveLinkChecks(originalValue);
		}
	}

	private void importProjectAndRefresh(String projectName) throws Exception {
		IProject project = importTestProject(projectName);
		project.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
	}

	private IProject importTestProject(String projectName) throws Exception {
		IProject testProject = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		IProjectDescription projectDescription = new ProjectDescription();
		projectDescription.setName(projectName);
		String projectRoot = String.join(File.separator, testCasesLocation.toOSString(), "bug185247", projectName);
		projectDescription.setLocationURI(URI.create(projectRoot));
		testProject.create(projectDescription, createTestMonitor());
		testProject.open(createTestMonitor());
		assertTrue(testProject.isAccessible(), "expected project to be open: " + projectName);
		return testProject;
	}

	private static void unzip(File archive, File outputDirectory) throws Exception {
		String[] command = { "unzip", archive.toString(), "-d", outputDirectory.toString() };
		executeCommand(command, outputDirectory);

	}

	private static void executeCommand(String[] command, File outputDirectory) throws Exception {
		assertTrue(outputDirectory.exists(), "output directory does not exist: " + outputDirectory);
		ProcessBuilder processBuilder = new ProcessBuilder(command);
		File commandOutputFile = new File(outputDirectory, "commandOutput.txt");
		if (!commandOutputFile.exists()) {
			assertTrue(commandOutputFile.createNewFile(),
					"failed to create standard output and error file for unzip command");
		}
		processBuilder.redirectOutput(commandOutputFile);
		processBuilder.redirectError(commandOutputFile);
		Process process = processBuilder.start();
		int commandExitCode = process.waitFor();
		String output = formatCommandOutput(command, commandOutputFile);
		assertTrue(commandOutputFile.delete(), "Failed to delete command output file. " + output);
		assertEquals(0, commandExitCode, "Failed to execute commmand. " + output);
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