/*******************************************************************************
 * Copyright (c) 2004, 2019 IBM Corporation and others.
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
 *     Alexander Blaas (arctis Softwaretechnologie GmbH) - bug 412809
 *******************************************************************************/
package org.eclipse.ant.tests.ui;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.ant.internal.ui.AntUtil;
import org.eclipse.ant.internal.ui.model.AntProjectNode;
import org.eclipse.ant.internal.ui.model.AntTargetNode;
import org.eclipse.ant.internal.ui.model.IAntElement;
import org.eclipse.ant.internal.ui.model.IAntModel;
import org.eclipse.ant.launching.IAntLaunchConstants;
import org.eclipse.ant.tests.ui.testplugin.AbstractAntUITest;
import org.eclipse.core.externaltools.internal.IExternalToolConstants;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.junit.Assert;

@SuppressWarnings("restriction")
public class AntUtilTests extends AbstractAntUITest {

	private static final long EXECUTION_THRESHOLD_INCLUDE_TASK = 10000;
	private static final long WINDOWS_EXECUTION_THRESHOLD_INCLUDE_TASK = 15000;

	public AntUtilTests(String name) {
		super(name);
	}

	public void testGetTargetsLaunchConfiguration() throws CoreException {
		String buildFileName = "echoing"; //$NON-NLS-1$
		File buildFile = getBuildFile(buildFileName + ".xml"); //$NON-NLS-1$
		String arguments = null;
		Map<String, String> properties = null;
		String propertyFiles = null;
		AntTargetNode[] targets = AntUtil.getTargets(buildFile.getAbsolutePath(), getLaunchConfiguration(buildFileName, arguments, properties, propertyFiles));
		assertTrue(targets != null);
		assertTrue("Incorrect number of targets retrieved; should be 4 was: " + targets.length, targets.length == 4); //$NON-NLS-1$
		assertContains("echo3", targets); //$NON-NLS-1$
	}

	public void testGetTargetsLaunchConfigurationMinusD() throws CoreException {
		String buildFileName = "importRequiringUserProp"; //$NON-NLS-1$
		File buildFile = getBuildFile(buildFileName + ".xml"); //$NON-NLS-1$
		String arguments = "-DimportFileName=toBeImported.xml"; //$NON-NLS-1$
		Map<String, String> properties = null;
		String propertyFiles = null;
		AntTargetNode[] targets = AntUtil.getTargets(buildFile.getAbsolutePath(), getLaunchConfiguration(buildFileName, arguments, properties, propertyFiles));
		assertTrue(targets != null);
		assertTrue("Incorrect number of targets retrieved; should be 3 was: " + targets.length, targets.length == 3); //$NON-NLS-1$
		assertContains("import-default", targets); //$NON-NLS-1$
	}

	public void testGetTargetsLaunchConfigurationMinusDAndProperty() throws CoreException {
		String buildFileName = "importRequiringUserProp"; //$NON-NLS-1$
		File buildFile = getBuildFile(buildFileName + ".xml"); //$NON-NLS-1$
		String arguments = "-DimportFileName=toBeImported.xml"; //$NON-NLS-1$
		// arguments should win
		Map<String, String> properties = new HashMap<>();
		properties.put("importFileName", "notToBeImported.xml"); //$NON-NLS-1$ //$NON-NLS-2$
		String propertyFiles = null;
		AntTargetNode[] targets = AntUtil.getTargets(buildFile.getAbsolutePath(), getLaunchConfiguration(buildFileName, arguments, properties, propertyFiles));
		assertTrue(targets != null);
		assertTrue("Incorrect number of targets retrieved; should be 3 was: " + targets.length, targets.length == 3); //$NON-NLS-1$
		assertContains("import-default", targets); //$NON-NLS-1$
	}

	public void testGetTargetsLaunchConfigurationProperty() throws CoreException {
		String buildFileName = "importRequiringUserProp"; //$NON-NLS-1$
		File buildFile = getBuildFile(buildFileName + ".xml"); //$NON-NLS-1$
		String arguments = null;
		Map<String, String> properties = new HashMap<>();
		properties.put("importFileName", "toBeImported.xml"); //$NON-NLS-1$ //$NON-NLS-2$
		String propertyFiles = null;
		AntTargetNode[] targets = AntUtil.getTargets(buildFile.getAbsolutePath(), getLaunchConfiguration(buildFileName, arguments, properties, propertyFiles));
		assertTrue(targets != null);
		assertTrue("Incorrect number of targets retrieved; should be 3 was: " + targets.length, targets.length == 3); //$NON-NLS-1$
		assertContains("import-default", targets); //$NON-NLS-1$
	}

	public void testGetTargetsLaunchConfigurationPropertyFile() throws CoreException {
		String buildFileName = "importRequiringUserProp"; //$NON-NLS-1$
		File buildFile = getBuildFile(buildFileName + ".xml"); //$NON-NLS-1$
		String arguments = null;
		Map<String, String> properties = null;
		String propertyFiles = "buildtest1.properties"; //$NON-NLS-1$
		AntTargetNode[] targets = AntUtil.getTargets(buildFile.getAbsolutePath(), getLaunchConfiguration(buildFileName, arguments, properties, propertyFiles));
		assertTrue(targets != null);
		assertTrue("Incorrect number of targets retrieved; should be 3 was: " + targets.length, targets.length == 3); //$NON-NLS-1$
		assertContains("import-default", targets); //$NON-NLS-1$
	}

	// for bugfix of bug 412809: Testing a simple "include-hierarchy" (only two levels setting the "as" property)
	public void testGetIncludeTargetsSimpleHierarchyAlias() {
		// The file itself contains one target. The included file contains the other one.
		String buildFileName = "bug412809/simple/buildFileAlias"; //$NON-NLS-1$
		AntTargetNode[] targets = getAntTargetNodesOfBuildFile(buildFileName);
		String[] expectedTargets = { "deploy", "commonPrefixed.deploy" }; //$NON-NLS-1$ //$NON-NLS-2$
		assertTargets(targets, expectedTargets);
	}

	// for bugfix of bug 412809: Testing a simple "include-hierarchy" (only two levels without the "as" property)
	public void testGetIncludeTargetsSimpleHierarchyNoAliases() {
		// The file itself contains one target. The included file contains the other one.
		String buildFileName = "bug412809/simple/buildFileNoAlias"; //$NON-NLS-1$
		AntTargetNode[] targets = getAntTargetNodesOfBuildFile(buildFileName);
		String[] expectedTargets = { "deploy", "common.deploy" }; //$NON-NLS-1$ //$NON-NLS-2$
		assertTargets(targets, expectedTargets);
	}

	// for bugfix of bug 412809: Testing a complex "include-hierarchy" (three levels, only non-aliases used)
	public void testGetIncludeTargetsComplexHierarchyNoAlias() {
		// The file itself contains one target. The included file contains the other one.
		String buildFileName = "bug412809/complex/noAlias/buildFileHierarchical"; //$NON-NLS-1$
		AntTargetNode[] targets = getAntTargetNodesOfBuildFile(buildFileName);
		String[] expectedTargets = { "deploy", "commonLv1.deploy", "commonLv1.commonLv2.deploySuper", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				"commonLv1.commonLv2.commonLv3.deployLv3", "commonLv1.commonLv2.commonLv3.commonLv4.deployLv4" }; //$NON-NLS-1$ //$NON-NLS-2$
		assertTargets(targets, expectedTargets);
	}

	// for bugfix of bug 412809: Testing a complex "include-hierarchy" (three levels, only aliases used)
	public void testGetIncludeTargetsComplexHierarchyAlias() {
		// The file itself contains one target. The included file contains the other one.
		String buildFileName = "bug412809/complex/alias/buildFileHierarchical"; //$NON-NLS-1$
		AntTargetNode[] targets = getAntTargetNodesOfBuildFile(buildFileName);
		String[] expectedTargets = { "deploy", "commonLv1Prefix.deploy", "commonLv1Prefix.commonLv2Prefix.deploySuper", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				"commonLv1Prefix.commonLv2Prefix.commonLv3Prefix.deployLv3", //$NON-NLS-1$
				"commonLv1Prefix.commonLv2Prefix.commonLv3Prefix.commonLv4Prefix.deployLv4" }; //$NON-NLS-1$
		assertTargets(targets, expectedTargets);
	}

	// for bugfix of bug 412809: Testing a complex "include-hierarchy" (three levels, aliases and non-aliases used)
	public void testGetIncludeTargetsComplexHierarchyMisc() {
		// The file itself contains one target. The included file contains the other one.
		String buildFileName = "bug412809/complex/misc/buildFileHierarchical"; //$NON-NLS-1$
		AntTargetNode[] targets = getAntTargetNodesOfBuildFile(buildFileName);
		String[] expectedTargets = { "deploy", "commonLv1.deploy", "commonLv1.commonLv2.deploySuper", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				"commonLv1.commonLv2.commonLv3Prefix.deployLv3", "commonLv1.commonLv2.commonLv3Prefix.commonLv4Prefix.deployLv4" }; //$NON-NLS-1$ //$NON-NLS-2$
		assertTargets(targets, expectedTargets);
	}

	// for bugfix of bug 412809: Assure explicitly that the provided patch works on external as well as non-external build-files
	public void testGetIncludeTargetsExternalFiles() {
		// First assure that external and non-external files are included
		String buildFileName = "bug412809/complex/misc/buildFileHierarchical"; //$NON-NLS-1$
		File buildFile = getBuildFile(buildFileName + ".xml"); //$NON-NLS-1$
		// tasks and position info but no lexical info
		IAntModel model = AntUtil.getAntModel(buildFile.getAbsolutePath(), false, true, true);
		AntProjectNode project = model.getProjectNode();

		// 9 childnodes are contained
		long childNodesExpected = 9;
		boolean atLeastOneExternal = false;
		List<IAntElement> childNodes = project.getChildNodes();

		Assert.assertNotNull(childNodes);

		int actualSize = childNodes.size();
		Assert.assertEquals("Expecting " + childNodesExpected + " childnodes, but have: " + actualSize, childNodesExpected, actualSize); //$NON-NLS-1$ //$NON-NLS-2$

		for (IAntElement element : childNodes) {
			// "External" seems to be true if the element was defined within an "importNode" => check the import nodes
			IAntElement importNode = element.getImportNode();

			if (importNode != null) {
				Assert.assertTrue(element.isExternal());
				atLeastOneExternal = true;
			} else {
				Assert.assertFalse(element.isExternal());
			}
		}
		// At least on external include-file was found
		Assert.assertTrue(atLeastOneExternal);
		// Then just execute the rest of the previous test
		AntTargetNode[] targets = getAntTargetNodesOfBuildFile(buildFileName);
		String[] expectedTargets = { "deploy", "commonLv1.deploy", "commonLv1.commonLv2.deploySuper", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				"commonLv1.commonLv2.commonLv3Prefix.deployLv3", "commonLv1.commonLv2.commonLv3Prefix.commonLv4Prefix.deployLv4" }; //$NON-NLS-1$ //$NON-NLS-2$
		assertTargets(targets, expectedTargets);
	}

	// for bugfix of bug 412809: Testing the performance by including the huge build file from "/testbuildfiles/performance/build.xml"
	public void testGetIncludeTargetsPerformance() {
		/*
		 * More or less the same files (noAlias-files because the parsing to search the project-name only occurs at includes where the alias-property
		 * is not set), but every include-file now includes the "big build.xml"
		 */
		String buildFileName = "bug412809/performance/buildFileHierarchical"; //$NON-NLS-1$
		long startTimeInNanoseconds = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
		File buildFile = getBuildFile(buildFileName + ".xml"); //$NON-NLS-1$
		IAntModel model = AntUtil.getAntModel(buildFile.getAbsolutePath(), false, true, true);
		AntProjectNode project = model.getProjectNode();
		long endTimeInNanoseconds = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();

		Assert.assertNotNull(project);
		/*
		 * Parsing the file-hierarchy should not take longer than:
		 *
		 * - 15s on windows (seems to be a general performance issue on windows)
		 *
		 * - 10s elsewhere
		 */

		long durationInMilliseconds = (endTimeInNanoseconds - startTimeInNanoseconds) / 1_000_000;
		// Change this value if it does not fit the performance needs
		long maxDuration = this.getExecutionTresholdIncludeTask();

		Assert.assertTrue("Expecting a duration < " + maxDuration + ", but we have " + durationInMilliseconds + "ms", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				durationInMilliseconds < maxDuration);
		// Test the rest
		AntTargetNode[] targets = getAntTargetNodesOfBuildFile(buildFileName);

		String[] expectedTargets = { "deploy", "commonLv1.deploy", "commonLv1.commonLv2.deploySuper", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				"commonLv1.commonLv2.commonLv3.deployLv3", "commonLv1.commonLv2.commonLv3.commonLv4.deployLv4" }; //$NON-NLS-1$ //$NON-NLS-2$
		// Expected targets
		Assert.assertEquals(3380, targets.length);
		// Just test if the mentioned targets are contained
		for (String expectedTarget : expectedTargets) {
			assertContains(expectedTarget, targets);
		}
	}

	private long getExecutionTresholdIncludeTask() {
		if (this.runsOnWindows()) {
			return WINDOWS_EXECUTION_THRESHOLD_INCLUDE_TASK;
		}
		return EXECUTION_THRESHOLD_INCLUDE_TASK;
	}

	private boolean runsOnWindows() {
		return System.getProperty("os.name").toLowerCase().contains("win"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private AntTargetNode[] getAntTargetNodesOfBuildFile(String buildFileName) {
		File buildFile = getBuildFile(buildFileName + ".xml"); //$NON-NLS-1$
		AntTargetNode[] targets = AntUtil.getTargets(buildFile.getAbsolutePath());
		assertTrue(targets != null);
		return targets;
	}

	private void assertTargets(AntTargetNode[] targets, String[] expectedTargetNames) {
		// Before the bugfix, the dependend target (defined in the included file) was not found and the dependencies-check failed
		int expectedSize = expectedTargetNames.length;
		assertTrue("Incorrect number of targets retrieved; should be " + expectedSize + " was: " //$NON-NLS-1$ //$NON-NLS-2$
				+ targets.length, targets.length == expectedSize);

		for (String expectedTarget : expectedTargetNames) {
			assertContains(expectedTarget, targets);
		}
	}

	protected ILaunchConfiguration getLaunchConfiguration(String buildFileName, String arguments, Map<String, String> properties, String propertyFiles) throws CoreException {
		ILaunchConfiguration config = getLaunchConfiguration(buildFileName);
		assertNotNull("Could not locate launch configuration for " + buildFileName, config); //$NON-NLS-1$
		ILaunchConfigurationWorkingCopy copy = config.getWorkingCopy();
		if (arguments != null) {
			copy.setAttribute(IExternalToolConstants.ATTR_TOOL_ARGUMENTS, arguments);
		}
		if (properties != null) {
			copy.setAttribute(IAntLaunchConstants.ATTR_ANT_PROPERTIES, properties);
		}
		if (propertyFiles != null) {
			copy.setAttribute(IAntLaunchConstants.ATTR_ANT_PROPERTY_FILES, propertyFiles);
		}
		return copy;
	}

	/**
	 * Asserts that <code>displayString</code> is in one of the completion proposals.
	 */
	private void assertContains(String targetName, AntTargetNode[] targets) {
		boolean found = false;
		for (AntTargetNode target : targets) {
			String foundName = target.getTargetName();
			if (targetName.equals(foundName)) {
				found = true;
				break;
			}
		}
		assertEquals("Did not find target: " + targetName, true, found); //$NON-NLS-1$
	}

	public void testIsKnownAntFileName() throws Exception {
		assertTrue("The file name 'foo.xml' is a valid name", AntUtil.isKnownAntFileName("a/b/c/d/foo.xml")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("The file name 'foo.ant' is a valid name", AntUtil.isKnownAntFileName("a/b/c/d/foo.ant")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("The file name 'foo.ent' is a valid name", AntUtil.isKnownAntFileName("a/b/c/d/foo.ent")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("The file name 'foo.macrodef' is a valid name", AntUtil.isKnownAntFileName("a/b/c/d/foo.macrodef")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse("The file name 'foo.xsi' is not a valid name", AntUtil.isKnownAntFileName("a/b/c/d/foo.xsi")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse("The file name 'foo.txt' is a valid name", AntUtil.isKnownAntFileName("a/b/c/d/foo.txt")); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
