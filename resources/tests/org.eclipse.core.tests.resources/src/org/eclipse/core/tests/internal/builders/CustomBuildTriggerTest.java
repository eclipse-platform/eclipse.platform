/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.core.tests.internal.builders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomContentsStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.setAutoBuilding;
import static org.eclipse.core.tests.resources.ResourceTestUtil.updateProjectDescription;
import static org.eclipse.core.tests.resources.ResourceTestUtil.waitForBuild;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * These tests exercise the function added in Eclipse 3.1 to allow a builder
 * to specify what build triggers it responds to.  Related API includes:
 * ICommand#isConfigurable()
 * ICommand.isBuilding(int)
 * ICommand.setBuilding(int, boolean)
 * The "isConfigurable" attribute in the builder extension schema
 */
@ExtendWith(WorkspaceResetExtension.class)
public class CustomBuildTriggerTest {

	@BeforeEach
	public void setUp() throws Exception {
		SortBuilder.resetSingleton();
		CustomTriggerBuilder.resetSingleton();
	}

	/**
	 * Tests that a builder that responds only to the "full" trigger will be called
	 * on the first build after a clean.
	 * See bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=182781.
	 */
	@Test
	public void testBuildAfterClean_builderRespondingToFull() throws CoreException {
		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject("PROJECT" + 1);
		// Turn auto-building off
		setAutoBuilding(false);
		// Create some resources
		project.create(createTestMonitor());
		project.open(createTestMonitor());
		// Create and set a build specs for project
		updateProjectDescription(project).addingCommand(CustomTriggerBuilder.BUILDER_NAME).withTestBuilderId("Build0")
				.withBuildingSetting(IncrementalProjectBuilder.AUTO_BUILD, false) //
				.withBuildingSetting(IncrementalProjectBuilder.FULL_BUILD, true) //
				.withBuildingSetting(IncrementalProjectBuilder.INCREMENTAL_BUILD, false) //
				.withBuildingSetting(IncrementalProjectBuilder.CLEAN_BUILD, false) //
				.apply();
		setAutoBuilding(true);

		//do an initial workspace build to get the builder instance
		workspace.build(IncrementalProjectBuilder.CLEAN_BUILD, createTestMonitor());
		CustomTriggerBuilder builder = CustomTriggerBuilder.getInstance();
		assertNotNull(builder);
		assertTrue(builder.triggerForLastBuild == 0);

		//do a clean - builder should not be called
		waitForBuild();
		builder.reset();
		workspace.build(IncrementalProjectBuilder.CLEAN_BUILD, createTestMonitor());
		assertEquals(0, builder.triggerForLastBuild);

		// Ensure that Auto-build doesn't cause a FULL_BUILD
		waitForBuild();
		assertEquals(0, builder.triggerForLastBuild);

		// But first requested build should cause a FULL_BUILD
		builder.reset();
		workspace.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		assertTrue(builder.wasFullBuild());

		// But subsequent builds shouldn't
		builder.reset();
		builder.clearBuildTrigger();
		workspace.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		assertTrue(builder.triggerForLastBuild == 0);
	}

	/**
	 * Tests that a builder that responds only to the "incremental" trigger will be called
	 * on the first build after a clean.
	 */
	@Test
	public void testBuildAfterClean_builderRespondingToIncremental() throws CoreException {
		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject("PROJECT" + 1);
		// Turn auto-building off
		setAutoBuilding(false);
		// Create some resources
		project.create(createTestMonitor());
		project.open(createTestMonitor());
		// Create and set a build specs for project
		updateProjectDescription(project).addingCommand(CustomTriggerBuilder.BUILDER_NAME).withTestBuilderId("Build0")
				.withBuildingSetting(IncrementalProjectBuilder.AUTO_BUILD, false) //
				.withBuildingSetting(IncrementalProjectBuilder.FULL_BUILD, false) //
				.withBuildingSetting(IncrementalProjectBuilder.INCREMENTAL_BUILD, true) //
				.withBuildingSetting(IncrementalProjectBuilder.CLEAN_BUILD, false) //
				.apply();
		setAutoBuilding(true);

		//do an initial workspace build to get the builder instance
		workspace.build(IncrementalProjectBuilder.CLEAN_BUILD, createTestMonitor());
		CustomTriggerBuilder builder = CustomTriggerBuilder.getInstance();
		assertNotNull(builder);
		assertEquals(0, builder.triggerForLastBuild);

		//do a clean - builder should not be called
		waitForBuild();
		builder.reset();
		workspace.build(IncrementalProjectBuilder.CLEAN_BUILD, createTestMonitor());
		assertEquals(0, builder.triggerForLastBuild);

		// Ensure that Auto-build doesn't cause a FULL_BUILD
		waitForBuild();
		assertEquals(0, builder.triggerForLastBuild);

		// But first requested build should cause a FULL_BUILD
		builder.reset();
		workspace.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		assertTrue(builder.wasFullBuild());

		IFile file = project.getFile("a.txt");
		file.create(createRandomContentsStream(), IResource.NONE, createTestMonitor());

		// But subsequent INCREMENTAL_BUILD builds should cause INCREMENTAL_BUILD
		builder.reset();
		builder.clearBuildTrigger();
		workspace.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		assertEquals(IncrementalProjectBuilder.INCREMENTAL_BUILD, builder.triggerForLastBuild);
	}

	/**
	 * Tests that a builder that responds only to the "auto" trigger will be called
	 * on the first build after a clean.
	 */
	@Test
	public void testBuildAfterClean_builderRespondingToAuto() throws CoreException {
		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject("PROJECT" + 1);
		// Turn auto-building off
		setAutoBuilding(false);
		// Create some resources
		project.create(createTestMonitor());
		project.open(createTestMonitor());
		// Create and set a build specs for project
		updateProjectDescription(project).addingCommand(CustomTriggerBuilder.BUILDER_NAME).withTestBuilderId("Build0")
				.withBuildingSetting(IncrementalProjectBuilder.AUTO_BUILD, true) //
				.withBuildingSetting(IncrementalProjectBuilder.FULL_BUILD, false) //
				.withBuildingSetting(IncrementalProjectBuilder.INCREMENTAL_BUILD, false) //
				.withBuildingSetting(IncrementalProjectBuilder.CLEAN_BUILD, false) //
				.apply();

		// Turn on autobuild without waiting for build to be finished
		IWorkspaceDescription description = workspace.getDescription();
		description.setAutoBuilding(true);
		workspace.setDescription(description);

		// do an initial workspace build to get the builder instance
		workspace.build(IncrementalProjectBuilder.CLEAN_BUILD, createTestMonitor());
		CustomTriggerBuilder builder = CustomTriggerBuilder.getInstance();
		assertNotNull(builder);
		assertEquals(0, builder.triggerForLastBuild);

		//do a clean - Ensure that Auto-build causes a FULL_BUILD
		waitForBuild();
		builder.reset();
		workspace.build(IncrementalProjectBuilder.CLEAN_BUILD, createTestMonitor());

		waitForBuild();
		assertTrue(builder.wasFullBuild());

		// add a file in the project to trigger an auto-build - no FULL_BUILD should be triggered
		builder.clearBuildTrigger();
		builder.reset();

		IFile file = project.getFile("b.txt");
		file.create(createRandomContentsStream(), IResource.NONE, createTestMonitor());

		waitForBuild();
		assertFalse(builder.wasCleanBuild());
		assertTrue(builder.wasAutobuild());
	}

	/**
	 * Tests that a builder that does not declare itself as configurable
	 * is not configurable.
	 */
	@Test
	public void testConfigurable() throws CoreException {
		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject("PROJECT" + 1);

		// Turn auto-building off
		setAutoBuilding(false);
		// Create some resources
		project.create(createTestMonitor());
		project.open(createTestMonitor());
		// Create and set a build specs for project
		updateProjectDescription(project).addingCommand(CustomTriggerBuilder.BUILDER_NAME).withTestBuilderId("Build0")
				.apply();

		assertThat(project.getDescription().getBuildSpec()).hasSize(1);
		ICommand command = project.getDescription().getBuildSpec()[0];
		assertTrue(command.isConfigurable());
		//ensure that setBuilding has effect
		assertTrue(command.isBuilding(IncrementalProjectBuilder.AUTO_BUILD));
		command.setBuilding(IncrementalProjectBuilder.AUTO_BUILD, false);
		assertFalse(command.isBuilding(IncrementalProjectBuilder.AUTO_BUILD));

		assertTrue(command.isBuilding(IncrementalProjectBuilder.CLEAN_BUILD));
		command.setBuilding(IncrementalProjectBuilder.CLEAN_BUILD, false);
		assertFalse(command.isBuilding(IncrementalProjectBuilder.CLEAN_BUILD));

		assertTrue(command.isBuilding(IncrementalProjectBuilder.FULL_BUILD));
		command.setBuilding(IncrementalProjectBuilder.FULL_BUILD, false);
		assertFalse(command.isBuilding(IncrementalProjectBuilder.FULL_BUILD));

		assertTrue(command.isBuilding(IncrementalProjectBuilder.INCREMENTAL_BUILD));
		command.setBuilding(IncrementalProjectBuilder.INCREMENTAL_BUILD, false);
		assertFalse(command.isBuilding(IncrementalProjectBuilder.INCREMENTAL_BUILD));

		// set the command back into the project for change to take effect
		updateProjectDescription(project).removingExistingCommands().addingCommand(command).apply();

		//ensure the builder is not called
		project.build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		CustomTriggerBuilder builder = CustomTriggerBuilder.getInstance();
		assertTrue(builder == null || builder.triggerForLastBuild == 0);

		project.build(IncrementalProjectBuilder.CLEAN_BUILD, createTestMonitor());
		builder = CustomTriggerBuilder.getInstance();
		assertTrue(builder == null || builder.triggerForLastBuild == 0);

		project.touch(createTestMonitor());
		project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		builder = CustomTriggerBuilder.getInstance();
		assertTrue(builder == null || builder.triggerForLastBuild == 0);
		setAutoBuilding(true);
		project.touch(createTestMonitor());
		builder = CustomTriggerBuilder.getInstance();
		assertTrue(builder == null || builder.triggerForLastBuild == 0);

		//turn the builder back on and make sure it runs
		setAutoBuilding(false);
		updateProjectDescription(project).removingExistingCommands().addingCommand(command)
				.withBuildingSetting(IncrementalProjectBuilder.AUTO_BUILD, true)
				.withBuildingSetting(IncrementalProjectBuilder.CLEAN_BUILD, true)
				.withBuildingSetting(IncrementalProjectBuilder.FULL_BUILD, true)
				.withBuildingSetting(IncrementalProjectBuilder.INCREMENTAL_BUILD, true).apply();

		//ensure the builder is called
		project.build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		builder = CustomTriggerBuilder.getInstance();
		assertTrue(builder.wasFullBuild());

		project.build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		builder = CustomTriggerBuilder.getInstance();
		assertTrue(builder.wasFullBuild());

		project.build(IncrementalProjectBuilder.CLEAN_BUILD, createTestMonitor());
		assertTrue(builder.wasCleanBuild());
	}

	/**
	 * Tests that a builder that does not declare itself as configurable
	 * is not configurable.
	 */
	@Test
	public void testNonConfigurable() throws CoreException {
		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject("PROJECT" + 1);

		// Turn auto-building off
		setAutoBuilding(false);
		// Create some resources
		project.create(createTestMonitor());
		project.open(createTestMonitor());
		// Create and set a build specs for project
		updateProjectDescription(project).addingCommand(SortBuilder.BUILDER_NAME).withTestBuilderId("Build0").apply();

		assertThat(project.getDescription().getBuildSpec()).hasSize(1);
		ICommand command = project.getDescription().getBuildSpec()[0];
		assertFalse(command.isConfigurable());
		//ensure that setBuilding has no effect
		command.setBuilding(IncrementalProjectBuilder.AUTO_BUILD, false);
		assertTrue(command.isBuilding(IncrementalProjectBuilder.AUTO_BUILD));
		command.setBuilding(IncrementalProjectBuilder.CLEAN_BUILD, false);
		assertTrue(command.isBuilding(IncrementalProjectBuilder.CLEAN_BUILD));
		command.setBuilding(IncrementalProjectBuilder.FULL_BUILD, false);
		assertTrue(command.isBuilding(IncrementalProjectBuilder.FULL_BUILD));
		command.setBuilding(IncrementalProjectBuilder.INCREMENTAL_BUILD, false);
		assertTrue(command.isBuilding(IncrementalProjectBuilder.INCREMENTAL_BUILD));

		//set the command back into the project for change to take effect
		updateProjectDescription(project).removingExistingCommands().addingCommand(command);

		//ensure that builder is still called
		project.build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		SortBuilder builder = SortBuilder.getInstance();
		assertTrue(builder.wasBuilt());
		assertTrue(builder.wasFullBuild());
		assertEquals(command, builder.getCommand());

		project.touch(createTestMonitor());
		project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		assertTrue(builder.wasBuilt());
		assertTrue(builder.wasIncrementalBuild());
	}

	/**
	 * Tests that a builder that skips autobuild still receives the correct resource delta
	 * See bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=173931
	 */
	@Test
	public void testSkipAutobuildDelta() throws CoreException {
		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject("PROJECT" + 1);
		CustomTriggerBuilder.resetSingleton();

		// Turn auto-building off
		setAutoBuilding(false);
		// Create some resources
		project.create(createTestMonitor());
		project.open(createTestMonitor());
		// Create and set a build specs for project
		updateProjectDescription(project).addingCommand(CustomTriggerBuilder.BUILDER_NAME).withTestBuilderId("Build0")
				.withBuildingSetting(IncrementalProjectBuilder.AUTO_BUILD, false)
				.apply();
		assertThat(project.getDescription().getBuildSpec()).hasSize(1);
		ICommand command = project.getDescription().getBuildSpec()[0];

		// turn autobuild back on
		setAutoBuilding(true);

		assertTrue(command.isConfigurable());
		//ensure that setBuilding has effect
		assertFalse(command.isBuilding(IncrementalProjectBuilder.AUTO_BUILD));

		//do an initial build to get the builder instance
		project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		waitForBuild();
		CustomTriggerBuilder builder = CustomTriggerBuilder.getInstance();
		assertNotNull(builder);
		builder.clearBuildTrigger();

		//add a file in the project, to trigger an autobuild
		IFile file = project.getFile("a.txt");
		file.create(createRandomContentsStream(), IResource.NONE, createTestMonitor());

		//autobuild should not call our builder
		waitForBuild();
		assertFalse(builder.wasIncrementalBuild());
		assertFalse(builder.wasAutobuild());

		//but, a subsequent incremental build should call it
		project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		assertFalse(builder.wasAutobuild());
		assertTrue(builder.wasIncrementalBuild());

	}

	/**
	 * Tests that a builder that responds only to the "full" trigger will be called
	 * on the first and only first build after a clean.
	 * See bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=206540.
	 */
	@Test
	public void testCleanBuild_AfterCleanBuilder() throws CoreException {
		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject("PROJECT" + 1);

		// Create some resources
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		// Create and set a build specs for project
		updateProjectDescription(project).addingCommand(CustomTriggerBuilder.BUILDER_NAME).withTestBuilderId("Build0")
				.withBuildingSetting(IncrementalProjectBuilder.AUTO_BUILD, false) //
				.withBuildingSetting(IncrementalProjectBuilder.FULL_BUILD, true) //
				.withBuildingSetting(IncrementalProjectBuilder.INCREMENTAL_BUILD, false) //
				.withBuildingSetting(IncrementalProjectBuilder.CLEAN_BUILD, false) //
				.apply();

		// turn auto-building off
		setAutoBuilding(false);

		// do an initial build to get the builder instance
		project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		waitForBuild();
		CustomTriggerBuilder builder = CustomTriggerBuilder.getInstance();
		assertNotNull(builder);
		assertTrue(builder.wasFullBuild());

		// do a clean - builder should not be called
		builder.clearBuildTrigger();
		builder.reset();
		workspace.build(IncrementalProjectBuilder.CLEAN_BUILD, createTestMonitor());
		assertFalse(builder.wasCleanBuild());
		assertFalse(builder.wasFullBuild());

		// do an incremental build - FULL_BUILD should be triggered
		builder.clearBuildTrigger();
		builder.reset();
		workspace.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		waitForBuild();
		assertFalse(builder.wasCleanBuild());
		assertTrue(builder.wasFullBuild());

		// add a file in the project before an incremental build is triggered again
		IFile file = project.getFile("a.txt");
		file.create(createRandomContentsStream(), IResource.NONE, createTestMonitor());

		// do an incremental build - build should NOT be triggered
		builder.clearBuildTrigger();
		builder.reset();
		workspace.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		waitForBuild();
		assertFalse(builder.wasCleanBuild());
		assertFalse(builder.wasFullBuild());
	}

	/**
	 * Tests that a builder that responds only to the "full" trigger will be called
	 * on the first and only first (non-auto) build after a clean.
	 * See bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=206540.
	 */
	@Test
	public void testCleanAutoBuild_AfterCleanBuilder() throws CoreException {
		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject("PROJECT" + 1);

		// Create some resources
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		// Create and set a build specs for project
		updateProjectDescription(project).addingCommand(CustomTriggerBuilder.BUILDER_NAME).withTestBuilderId("Build0")
				.withBuildingSetting(IncrementalProjectBuilder.AUTO_BUILD, false) //
				.withBuildingSetting(IncrementalProjectBuilder.FULL_BUILD, true) //
				.withBuildingSetting(IncrementalProjectBuilder.INCREMENTAL_BUILD, false) //
				.withBuildingSetting(IncrementalProjectBuilder.CLEAN_BUILD, false) //
				.apply();

		// turn auto-building on
		setAutoBuilding(true);
		CustomTriggerBuilder builder = CustomTriggerBuilder.getInstance();
		assertNotNull(builder);
		assertEquals(0, builder.triggerForLastBuild);

		// do a clean - builder should not be called
		builder.clearBuildTrigger();
		builder.reset();
		workspace.build(IncrementalProjectBuilder.CLEAN_BUILD, createTestMonitor());
		assertFalse(builder.wasCleanBuild());
		assertFalse(builder.wasFullBuild());

		// add a file in the project to trigger an auto-build - no FULL_BUILD should be triggered
		builder.clearBuildTrigger();
		builder.reset();

		IFile file = project.getFile("a.txt");
		file.create(createRandomContentsStream(), IResource.NONE, createTestMonitor());

		waitForBuild();
		assertEquals(0, builder.triggerForLastBuild);

		// Build the project explicitly -- full build should be triggered
		project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		waitForBuild();
		assertTrue(builder.wasFullBuild());

		// add another file in the project to trigger an auto-build - build should NOT be triggered
		builder.clearBuildTrigger();
		builder.reset();

		file = project.getFile("b.txt");
		file.create(createRandomContentsStream(), IResource.NONE, createTestMonitor());

		waitForBuild();
		assertFalse(builder.wasCleanBuild());
		assertFalse(builder.wasFullBuild());
	}

}
