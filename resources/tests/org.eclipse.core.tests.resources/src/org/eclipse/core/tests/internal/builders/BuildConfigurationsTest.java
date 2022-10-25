/*******************************************************************************
 * Copyright (c) 2010, 2015 Broadcom Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Broadcom Corporation - initial API and implementation
 * Baltasar Belyavsky (Texas Instruments) - [361675] Order mismatch when saving/restoring workspace trees
 ******************************************************************************/
package org.eclipse.core.tests.internal.builders;

import java.util.Map;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.tests.internal.builders.TestBuilder.BuilderRuleCallback;
import org.eclipse.core.tests.resources.ResourceDeltaVerifier;

/**
 * These tests exercise the project buildConfigs functionality which allows a different
 * builder to be run for different project buildConfigs.
 */
public class BuildConfigurationsTest extends AbstractBuilderTest {

	private IProject project0;
	private IProject project1;
	private IFile file0;
	private IFile file1;
	private final String variant0 = "Variant0";
	private final String variant1 = "Variant1";
	private final String variant2 = "Variant2";

	public BuildConfigurationsTest(String name) {
		super(name);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		// Create resources
		IWorkspaceRoot root = getWorkspace().getRoot();
		project0 = root.getProject("BuildVariantTest_p0");
		project1 = root.getProject("BuildVariantTest_p1");
		file0 = project0.getFile("File0");
		file1 = project1.getFile("File1");
		IResource[] resources = {project0, project1, file0, file1};
		ensureExistsInWorkspace(resources, true);
		setAutoBuilding(false);
		setupProject(project0);
		setupProject(project1);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();

		// Delete resources
		project0.delete(true, null);
		project1.delete(true, null);
	}

	/**
	 * Helper method to configure a project with a build command and several buildConfigs.
	 */
	private void setupProject(IProject project) throws CoreException {
		IProjectDescription desc = project.getDescription();

		// Add build command
		ICommand command = createCommand(desc, ConfigurationBuilder.BUILDER_NAME, "Build0");
		command.setBuilding(IncrementalProjectBuilder.AUTO_BUILD, true);
		command.setBuilding(IncrementalProjectBuilder.FULL_BUILD, true);
		command.setBuilding(IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
		command.setBuilding(IncrementalProjectBuilder.CLEAN_BUILD, true);
		desc.setBuildSpec(new ICommand[] {command});

		// Create buildConfigs
		desc.setBuildConfigs(new String[] {variant0, variant1, variant2});

		project.setDescription(desc, getMonitor());
	}

	/**
	 * Tests that an incremental builder is run/not run correctly, depending on deltas,
	 * and is given the correct deltas depending on which project variant is being built
	 */
	public void testDeltas() throws CoreException {
		ConfigurationBuilder.clearStats();
		// Run some incremental builds while varying the active variant and whether the project was modified
		// and check that the builder is run/not run with the correct trigger
		file0.setContents(getRandomContents(), true, true, getMonitor());
		incrementalBuild(1, project0, variant1, true, 1, IncrementalProjectBuilder.FULL_BUILD);
		incrementalBuild(2, project0, variant1, false, 1, 0);
		incrementalBuild(3, project0, variant2, true, 1, IncrementalProjectBuilder.FULL_BUILD);
		incrementalBuild(4, project0, variant1, false, 1, 0);
		file0.setContents(getRandomContents(), true, true, getMonitor());
		incrementalBuild(5, project0, variant1, true, 2, IncrementalProjectBuilder.INCREMENTAL_BUILD);
		incrementalBuild(6, project0, variant2, true, 2, IncrementalProjectBuilder.INCREMENTAL_BUILD);
		incrementalBuild(7, project0, variant0, true, 1, IncrementalProjectBuilder.FULL_BUILD);
	}

	/**
	 * Tests that deltas are preserved per variant when a project is closed then opened.
	 */
	public void testCloseAndOpenProject() throws CoreException {
		ConfigurationBuilder.clearStats();
		file0.setContents(getRandomContents(), true, true, getMonitor());
		incrementalBuild(1, project0, variant0, true, 1, IncrementalProjectBuilder.FULL_BUILD);
		incrementalBuild(2, project0, variant1, true, 1, IncrementalProjectBuilder.FULL_BUILD);
		incrementalBuild(3, project0, variant2, true, 1, IncrementalProjectBuilder.FULL_BUILD);

		project0.close(getMonitor());
		ConfigurationBuilder.clearStats();
		project0.open(getMonitor());

		incrementalBuild(4, project0, variant0, false, 0, 0);
		incrementalBuild(5, project0, variant1, false, 0, 0);
		incrementalBuild(6, project0, variant2, false, 0, 0);
	}

	/**
	 * Tests that deltas are restored in the correct order per variant when a project is closed then opened.
	 */
	public void testCloseAndOpenProject_Bug361675() throws CoreException {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject tempProject = root.getProject("BuildVariantTest_pTemp");
		IFile tempFile0 = tempProject.getFile("File0");
		IFile tempFile1 = tempProject.getFile("File1");
		IResource[] resources = {tempProject, tempFile0, tempFile1};
		ensureExistsInWorkspace(resources, true);
		setupProject(tempProject);

		try {
			ConfigurationBuilder.clearStats();

			tempFile0.setContents(getRandomContents(), true, true, getMonitor());
			tempFile1.setContents(getRandomContents(), true, true, getMonitor());
			incrementalBuild(1, tempProject, variant0, true, 1, IncrementalProjectBuilder.FULL_BUILD);
			incrementalBuild(2, tempProject, variant1, true, 1, IncrementalProjectBuilder.FULL_BUILD);
			incrementalBuild(3, tempProject, variant2, true, 1, IncrementalProjectBuilder.FULL_BUILD);

			tempFile0.setContents(getRandomContents(), true, true, getMonitor());
			incrementalBuild(4, tempProject, variant1, true, 2, IncrementalProjectBuilder.INCREMENTAL_BUILD);

			tempFile1.setContents(getRandomContents(), true, true, getMonitor());
			incrementalBuild(5, tempProject, variant2, true, 2, IncrementalProjectBuilder.INCREMENTAL_BUILD);

			tempProject.close(getMonitor());
			ConfigurationBuilder.clearStats();
			tempProject.open(getMonitor());

			// verify variant0 - both File0 and File1 are expected to have changed since it was last built
			incrementalBuild(6, tempProject, variant0, true, 1, IncrementalProjectBuilder.INCREMENTAL_BUILD);
			ConfigurationBuilder builder0 = ConfigurationBuilder.getBuilder(tempProject.getBuildConfig(variant0));
			assertNotNull("6.10", builder0);
			ResourceDeltaVerifier verifier0 = new ResourceDeltaVerifier();
			verifier0.addExpectedChange(tempFile0, tempProject, IResourceDelta.CHANGED, IResourceDelta.CONTENT);
			verifier0.addExpectedChange(tempFile1, tempProject, IResourceDelta.CHANGED, IResourceDelta.CONTENT);
			verifier0.verifyDelta(builder0.deltaForLastBuild);
			assertTrue("6.11: " + verifier0.getMessage(), verifier0.isDeltaValid());

			// verify variant1 - only File1 is expected to have changed since it was last built
			incrementalBuild(7, tempProject, variant1, true, 1, IncrementalProjectBuilder.INCREMENTAL_BUILD);
			ConfigurationBuilder builder1 = ConfigurationBuilder.getBuilder(tempProject.getBuildConfig(variant1));
			assertNotNull("7.10", builder1);
			ResourceDeltaVerifier verifier1 = new ResourceDeltaVerifier();
			verifier1.addExpectedChange(tempFile1, tempProject, IResourceDelta.CHANGED, IResourceDelta.CONTENT);
			verifier1.verifyDelta(builder1.deltaForLastBuild);
			assertTrue("7.11: " + verifier1.getMessage(), verifier1.isDeltaValid());

			// verify variant2 - no changes are expected since it was last built
			incrementalBuild(8, tempProject, variant2, false, 0, 0);

		} finally {
			tempProject.delete(true, getMonitor());
		}
	}

	/**
	 * Run a workspace build with project references
	 *
	 * References are:
	 *     p0,v0 depends on p0,v1
	 *     p0,v0 depends on p1,v0
	 *     p0,v0 depends on p1,v2
	 * Active buildConfigs are:
	 *     p0,v0 and p1,v0
	 * Build order should be:
	 *     p0,v1  p1,v0  p1,v2  p0,v0
	 */
	public void testBuildReferences() throws CoreException {
		ConfigurationBuilder.clearStats();
		ConfigurationBuilder.clearBuildOrder();
		IProjectDescription desc = project0.getDescription();
		desc.setActiveBuildConfig(variant0);
		project0.setDescription(desc, getMonitor());
		desc = project1.getDescription();
		desc.setActiveBuildConfig(variant0);
		project1.setDescription(desc, getMonitor());

		// Note: references are not alphabetically ordered to check that references are sorted into a stable order
		setReferences(project0, variant0, new IBuildConfiguration[] {project0.getBuildConfig(variant1), project1.getBuildConfig(variant2), project1.getBuildConfig(variant0)});
		getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());

		assertEquals("1.0", 4, ConfigurationBuilder.buildOrder.size());
		assertEquals("1.1", project0.getBuildConfig(variant1), ConfigurationBuilder.buildOrder.get(0));
		assertEquals("1.2", project1.getBuildConfig(variant0), ConfigurationBuilder.buildOrder.get(1));
		assertEquals("1.3", project1.getBuildConfig(variant2), ConfigurationBuilder.buildOrder.get(2));
		assertEquals("1.4", project0.getBuildConfig(variant0), ConfigurationBuilder.buildOrder.get(3));
		checkBuild(2, project0, variant0, true, 1, IncrementalProjectBuilder.FULL_BUILD);
		checkBuild(3, project0, variant1, true, 1, IncrementalProjectBuilder.FULL_BUILD);
		checkBuild(4, project0, variant2, false, 0, 0);
		checkBuild(5, project1, variant0, true, 1, IncrementalProjectBuilder.FULL_BUILD);
		checkBuild(6, project1, variant1, false, 0, 0);
		checkBuild(7, project1, variant2, true, 1, IncrementalProjectBuilder.FULL_BUILD);

		// Modify project1, all project1 builders should do an incremental build
		file1.setContents(getRandomContents(), true, true, getMonitor());

		ConfigurationBuilder.clearBuildOrder();
		getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());

		assertEquals("8.0", 2, ConfigurationBuilder.buildOrder.size());
		assertEquals("8.1", project1.getBuildConfig(variant0), ConfigurationBuilder.buildOrder.get(0));
		assertEquals("8.2", project1.getBuildConfig(variant2), ConfigurationBuilder.buildOrder.get(1));
		checkBuild(9, project0, variant0, false, 1, 0);
		checkBuild(10, project0, variant1, false, 1, 0);
		checkBuild(11, project0, variant2, false, 0, 0);
		checkBuild(12, project1, variant0, true, 2, IncrementalProjectBuilder.INCREMENTAL_BUILD);
		checkBuild(13, project1, variant1, false, 0, 0);
		checkBuild(14, project1, variant2, true, 2, IncrementalProjectBuilder.INCREMENTAL_BUILD);
	}

	/**
	 * Tests that building a configuration that references a closed / inaccessible project works correctly.
	 * References are:
	 *     p0,v0 depends on p1,v0
	 * p1 is closed.
	 * p0v0 should still be built.
	 * @throws CoreException
	 */
	public void testBuildReferencesOfClosedProject() throws CoreException {
		ConfigurationBuilder.clearStats();
		ConfigurationBuilder.clearBuildOrder();
		IProjectDescription desc = project0.getDescription();
		desc.setActiveBuildConfig(variant0);
		desc.setBuildConfigReferences(variant0, new IBuildConfiguration[] {project1.getBuildConfig(variant0)});
		project0.setDescription(desc, getMonitor());

		// close project 1
		project1.close(getMonitor());
		// should still be able to build project 0.
		getWorkspace().build(new IBuildConfiguration[] {project0.getBuildConfig(variant0)}, IncrementalProjectBuilder.FULL_BUILD, true, getMonitor());
		assertEquals("1.0", 1, ConfigurationBuilder.buildOrder.size());
		assertEquals("1.1", project0.getBuildConfig(variant0), ConfigurationBuilder.buildOrder.get(0));
		checkBuild(2, project0, variant0, true, 1, IncrementalProjectBuilder.FULL_BUILD);

		// Workspace full build should also build project 0
		ConfigurationBuilder.clearStats();
		ConfigurationBuilder.clearBuildOrder();
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
		assertEquals("1.0", 1, ConfigurationBuilder.buildOrder.size());
		assertEquals("1.1", project0.getBuildConfig(variant0), ConfigurationBuilder.buildOrder.get(0));
		checkBuild(2, project0, variant0, true, 1, IncrementalProjectBuilder.FULL_BUILD);

		// re-open project 1
		project1.open(getMonitor());

		ConfigurationBuilder.clearStats();
		ConfigurationBuilder.clearBuildOrder();
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());

		assertEquals("8.0", 2, ConfigurationBuilder.buildOrder.size());
		assertEquals("8.1", project1.getBuildConfig(variant0), ConfigurationBuilder.buildOrder.get(0));
		assertEquals("8.2", project0.getBuildConfig(variant0), ConfigurationBuilder.buildOrder.get(1));
	}

	/**
	 * Tests that cleaning a project variant does not affect other buildConfigs in the same project
	 */
	public void testClean() throws CoreException {
		ConfigurationBuilder.clearStats();
		incrementalBuild(1, project0, variant0, true, 1, IncrementalProjectBuilder.FULL_BUILD);
		incrementalBuild(2, project0, variant1, true, 1, IncrementalProjectBuilder.FULL_BUILD);
		clean(3, project0, variant0, 2);
		incrementalBuild(4, project0, variant1, false, 1, 0);
	}

	/**
	 * Helper method to set the references for a project.
	 */
	private void setReferences(IProject project, String configId, IBuildConfiguration[] configs) throws CoreException {
		IProjectDescription desc = project.getDescription();
		desc.setBuildConfigReferences(configId, configs);
		project.setDescription(desc, getMonitor());
	}

	/**
	 * Run an incremental build for the given project variant, and check the behaviour of the build.
	 */
	private void incrementalBuild(int testId, IProject project, String variant, boolean shouldBuild, int expectedCount, int expectedTrigger) throws CoreException {
		project.build(project.getBuildConfig(variant), IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
		checkBuild(testId, project, variant, shouldBuild, expectedCount, expectedTrigger);
	}

	/**
	 * Clean the specified project variant.
	 */
	private void clean(int testId, IProject project, String variant, int expectedCount) throws CoreException {
		project.build(project.getBuildConfig(variant), IncrementalProjectBuilder.CLEAN_BUILD, getMonitor());
		ConfigurationBuilder builder = ConfigurationBuilder.getBuilder(project.getBuildConfig(variant));
		assertNotNull(testId + ".0", builder);
		assertEquals(testId + ".1", expectedCount, builder.buildCount);
		assertEquals(testId + ".2", IncrementalProjectBuilder.CLEAN_BUILD, builder.triggerForLastBuild);
	}

	/**
	 * Check the behaviour of a build
	 */
	private void checkBuild(int testId, IProject project, String variant, boolean shouldBuild, int expectedCount, int expectedTrigger) throws CoreException {
		try {
			project.getBuildConfig(variant);
		} catch (CoreException e) {
			fail(testId + ".0");
		}
		ConfigurationBuilder builder = ConfigurationBuilder.getBuilder(project.getBuildConfig(variant));
		if (builder == null) {
			assertFalse(testId + ".1", shouldBuild);
			assertEquals(testId + ".2", 0, expectedCount);
		} else {
			assertEquals(testId + ".3", expectedCount, builder.buildCount);
			if (shouldBuild) {
				assertEquals(testId + ".4", expectedTrigger, builder.triggerForLastBuild);
			}
		}
	}

	/**
	 * Regression test for
	 * https://github.com/eclipse-platform/eclipse.platform/issues/243.
	 *
	 * Build should not be broken by a builder that references not existing project.
	 *
	 * Application use case: importing an Xtext based project that references
	 * another (not yet imported) project. Xtext builder reports it is interested in
	 * the project that isn't there (but is referenced in the .project file).
	 */
	public void testBuildProjectWithNotExistingReference() throws Exception {
		// need a build to create builder
		IBuildConfiguration buildConfig = project0.getBuildConfig(variant0);
		project0.build(buildConfig, IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());

		// Configure builder to report "interesting" projects
		ConfigurationBuilder builder = ConfigurationBuilder.getBuilder(buildConfig);
		IProject notExistingProject = getWorkspace().getRoot().getProject("not_existing_one");

		builder.setRuleCallback(new BuilderRuleCallback() {
			@Override
			public IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
				// return not existing project, it will be remembered via
				// InternalBuilder.setInterestingProjects(IProject[])
				// after full build
				return new IProject[] { notExistingProject };
			}
		});

		// need a full build to remember "interesting" projects
		project0.build(buildConfig, IncrementalProjectBuilder.FULL_BUILD, getMonitor());

		// need a delta NOT in the builder's own project
		project1.touch(getMonitor());

		// this will try to find delta for non existing resource
		project0.build(buildConfig, IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
	}

}
