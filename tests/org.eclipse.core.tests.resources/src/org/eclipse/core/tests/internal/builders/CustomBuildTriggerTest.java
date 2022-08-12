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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;

/**
 * These tests exercise the function added in Eclipse 3.1 to allow a builder
 * to specify what build triggers it responds to.  Related API includes:
 * ICommand#isConfigurable()
 * ICommand.isBuilding(int)
 * ICommand.setBuilding(int, boolean)
 * The "isConfigurable" attribute in the builder extension schema
 */
public class CustomBuildTriggerTest extends AbstractBuilderTest {

	public CustomBuildTriggerTest(String name) {
		super(name);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		SortBuilder.resetSingleton();
		CustomTriggerBuilder.resetSingleton();
	}

	/**
	 * Tests that a builder that responds only to the "full" trigger will be called
	 * on the first build after a clean.
	 * See bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=182781.
	 */
	public void testBuildAfterClean_builderRespondingToFull() throws CoreException {
		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject("PROJECT" + 1);
		ICommand command = null;
		// Turn auto-building off
		setAutoBuilding(false);
		// Create some resources
		project.create(getMonitor());
		project.open(getMonitor());
		// Create and set a build specs for project
		IProjectDescription desc = project.getDescription();
		command = createCommand(desc, CustomTriggerBuilder.BUILDER_NAME, "Build0");
		command.setBuilding(IncrementalProjectBuilder.AUTO_BUILD, false);
		command.setBuilding(IncrementalProjectBuilder.FULL_BUILD, true);
		command.setBuilding(IncrementalProjectBuilder.INCREMENTAL_BUILD, false);
		command.setBuilding(IncrementalProjectBuilder.CLEAN_BUILD, false);
		desc.setBuildSpec(new ICommand[] {command});
		project.setDescription(desc, getMonitor());
		command = project.getDescription().getBuildSpec()[0];
		setAutoBuilding(true);

		//do an initial workspace build to get the builder instance
		workspace.build(IncrementalProjectBuilder.CLEAN_BUILD, getMonitor());
		CustomTriggerBuilder builder = CustomTriggerBuilder.getInstance();
		assertNotNull("1.0", builder);
		assertTrue("1.1", builder.triggerForLastBuild == 0);

		//do a clean - builder should not be called
		waitForBuild();
		builder.reset();
		workspace.build(IncrementalProjectBuilder.CLEAN_BUILD, getMonitor());
		assertEquals("2.0", 0, builder.triggerForLastBuild);

		// Ensure that Auto-build doesn't cause a FULL_BUILD
		waitForBuild();
		assertEquals("2.1", 0, builder.triggerForLastBuild);

		// But first requested build should cause a FULL_BUILD
		builder.reset();
		workspace.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
		assertTrue("3.0", builder.wasFullBuild());

		// But subsequent builds shouldn't
		builder.reset();
		builder.clearBuildTrigger();
		workspace.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
		assertTrue("3.1", builder.triggerForLastBuild == 0);
	}

	/**
	 * Tests that a builder that responds only to the "incremental" trigger will be called
	 * on the first build after a clean.
	 */
	public void testBuildAfterClean_builderRespondingToIncremental() throws CoreException {
		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject("PROJECT" + 1);
		ICommand command = null;
		// Turn auto-building off
		setAutoBuilding(false);
		// Create some resources
		project.create(getMonitor());
		project.open(getMonitor());
		// Create and set a build specs for project
		IProjectDescription desc = project.getDescription();
		command = createCommand(desc, CustomTriggerBuilder.BUILDER_NAME, "Build0");
		command.setBuilding(IncrementalProjectBuilder.AUTO_BUILD, false);
		command.setBuilding(IncrementalProjectBuilder.FULL_BUILD, false);
		command.setBuilding(IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
		command.setBuilding(IncrementalProjectBuilder.CLEAN_BUILD, false);
		desc.setBuildSpec(new ICommand[] {command});
		project.setDescription(desc, getMonitor());
		command = project.getDescription().getBuildSpec()[0];
		setAutoBuilding(true);

		//do an initial workspace build to get the builder instance
		workspace.build(IncrementalProjectBuilder.CLEAN_BUILD, getMonitor());
		CustomTriggerBuilder builder = CustomTriggerBuilder.getInstance();
		assertNotNull("1.0", builder);
		assertEquals("1.1", 0, builder.triggerForLastBuild);

		//do a clean - builder should not be called
		waitForBuild();
		builder.reset();
		workspace.build(IncrementalProjectBuilder.CLEAN_BUILD, getMonitor());
		assertEquals("2.0", 0, builder.triggerForLastBuild);

		// Ensure that Auto-build doesn't cause a FULL_BUILD
		waitForBuild();
		assertEquals("2.1", 0, builder.triggerForLastBuild);

		// But first requested build should cause a FULL_BUILD
		builder.reset();
		workspace.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
		assertTrue("3.0", builder.wasFullBuild());

		IFile file = project.getFile("a.txt");
		try {
			file.create(getRandomContents(), IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("4.0", e);
		}

		// But subsequent INCREMENTAL_BUILD builds should cause INCREMENTAL_BUILD
		builder.reset();
		builder.clearBuildTrigger();
		workspace.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
		assertEquals("5.0", IncrementalProjectBuilder.INCREMENTAL_BUILD, builder.triggerForLastBuild);
	}

	/**
	 * Tests that a builder that responds only to the "auto" trigger will be called
	 * on the first build after a clean.
	 */
	public void testBuildAfterClean_builderRespondingToAuto() throws CoreException {
		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject("PROJECT" + 1);
		ICommand command = null;
		// Turn auto-building off
		setAutoBuilding(false);
		// Create some resources
		project.create(getMonitor());
		project.open(getMonitor());
		// Create and set a build specs for project
		IProjectDescription desc = project.getDescription();
		command = createCommand(desc, CustomTriggerBuilder.BUILDER_NAME, "Build0");
		command.setBuilding(IncrementalProjectBuilder.AUTO_BUILD, true);
		command.setBuilding(IncrementalProjectBuilder.FULL_BUILD, false);
		command.setBuilding(IncrementalProjectBuilder.INCREMENTAL_BUILD, false);
		command.setBuilding(IncrementalProjectBuilder.CLEAN_BUILD, false);
		desc.setBuildSpec(new ICommand[] {command});
		project.setDescription(desc, getMonitor());
		command = project.getDescription().getBuildSpec()[0];
		setAutoBuilding(true);

		// do an initial workspace build to get the builder instance
		workspace.build(IncrementalProjectBuilder.CLEAN_BUILD, getMonitor());
		CustomTriggerBuilder builder = CustomTriggerBuilder.getInstance();
		assertNotNull("1.0", builder);
		assertTrue("1.1", builder.triggerForLastBuild == 0);

		//do a clean - Ensure that Auto-build causes a FULL_BUILD
		waitForBuild();
		builder.reset();
		workspace.build(IncrementalProjectBuilder.CLEAN_BUILD, getMonitor());

		waitForBuild();
		assertTrue("2.1", builder.wasFullBuild());

		// add a file in the project to trigger an auto-build - no FULL_BUILD should be triggered
		builder.clearBuildTrigger();
		builder.reset();

		IFile file = project.getFile("b.txt");
		try {
			file.create(getRandomContents(), IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("3.00", e);
		}

		waitForBuild();
		assertTrue("6.0", !builder.wasCleanBuild());
		assertTrue("6.1", builder.wasAutobuild());
	}

	/**
	 * Tests that a builder that does not declare itself as configurable
	 * is not configurable.
	 */
	public void testConfigurable() {
		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject("PROJECT" + 1);
		ICommand command = null;
		try {
			// Turn auto-building off
			setAutoBuilding(false);
			// Create some resources
			project.create(getMonitor());
			project.open(getMonitor());
			// Create and set a build specs for project
			IProjectDescription desc = project.getDescription();
			desc.setBuildSpec(new ICommand[] {createCommand(desc, CustomTriggerBuilder.BUILDER_NAME, "Build0")});
			project.setDescription(desc, getMonitor());
			command = project.getDescription().getBuildSpec()[0];
		} catch (CoreException e) {
			fail("0.99", e);
		}
		assertTrue("1.0", command.isConfigurable());
		//ensure that setBuilding has effect
		assertTrue("1.1", command.isBuilding(IncrementalProjectBuilder.AUTO_BUILD));
		command.setBuilding(IncrementalProjectBuilder.AUTO_BUILD, false);
		assertTrue("1.2", !command.isBuilding(IncrementalProjectBuilder.AUTO_BUILD));

		assertTrue("1.3", command.isBuilding(IncrementalProjectBuilder.CLEAN_BUILD));
		command.setBuilding(IncrementalProjectBuilder.CLEAN_BUILD, false);
		assertTrue("1.4", !command.isBuilding(IncrementalProjectBuilder.CLEAN_BUILD));

		assertTrue("1.5", command.isBuilding(IncrementalProjectBuilder.FULL_BUILD));
		command.setBuilding(IncrementalProjectBuilder.FULL_BUILD, false);
		assertTrue("1.6", !command.isBuilding(IncrementalProjectBuilder.FULL_BUILD));

		assertTrue("1.7", command.isBuilding(IncrementalProjectBuilder.INCREMENTAL_BUILD));
		command.setBuilding(IncrementalProjectBuilder.INCREMENTAL_BUILD, false);
		assertTrue("1.8", !command.isBuilding(IncrementalProjectBuilder.INCREMENTAL_BUILD));

		try {
			//set the command back into the project for change to take effect
			IProjectDescription desc = project.getDescription();
			desc.setBuildSpec(new ICommand[] {command});
			project.setDescription(desc, getMonitor());
		} catch (CoreException e1) {
			fail("1.99", e1);
		}

		//ensure the builder is not called
		try {
			project.build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
		} catch (CoreException e) {
			fail("2.99", e);
		}
		CustomTriggerBuilder builder = CustomTriggerBuilder.getInstance();
		assertTrue("2.0", builder == null || builder.triggerForLastBuild == 0);

		try {
			project.build(IncrementalProjectBuilder.CLEAN_BUILD, getMonitor());
		} catch (CoreException e) {
			fail("2.91", e);
		}
		builder = CustomTriggerBuilder.getInstance();
		assertTrue("2.1", builder == null || builder.triggerForLastBuild == 0);

		try {
			project.touch(getMonitor());
			project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
		} catch (CoreException e) {
			fail("3.99", e);
		}
		builder = CustomTriggerBuilder.getInstance();
		assertTrue("3.0", builder == null || builder.triggerForLastBuild == 0);
		try {
			setAutoBuilding(true);
			project.touch(getMonitor());
		} catch (CoreException e) {
			fail("4.99", e);
		}
		builder = CustomTriggerBuilder.getInstance();
		assertTrue("4.0", builder == null || builder.triggerForLastBuild == 0);

		//turn the builder back on and make sure it runs
		command.setBuilding(IncrementalProjectBuilder.AUTO_BUILD, true);
		command.setBuilding(IncrementalProjectBuilder.CLEAN_BUILD, true);
		command.setBuilding(IncrementalProjectBuilder.FULL_BUILD, true);
		command.setBuilding(IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
		try {
			//set the command back into the project for change to take effect
			setAutoBuilding(false);
			IProjectDescription desc = project.getDescription();
			desc.setBuildSpec(new ICommand[] {command});
			project.setDescription(desc, getMonitor());
		} catch (CoreException e1) {
			fail("5.99", e1);
		}

		//ensure the builder is called
		try {
			project.build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
		} catch (CoreException e) {
			fail("6.99", e);
		}
		builder = CustomTriggerBuilder.getInstance();
		assertTrue("6.1", builder.wasFullBuild());

		try {
			project.build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
		} catch (CoreException e) {
			fail("7.99", e);
		}
		builder = CustomTriggerBuilder.getInstance();
		assertTrue("7.1", builder.wasFullBuild());

		try {
			project.build(IncrementalProjectBuilder.CLEAN_BUILD, getMonitor());
		} catch (CoreException e) {
			fail("8.99", e);
		}
		assertTrue("8.1", builder.wasCleanBuild());
	}

	/**
	 * Tests that a builder that does not declare itself as configurable
	 * is not configurable.
	 */
	public void testNonConfigurable() {
		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject("PROJECT" + 1);
		ICommand command = null;
		try {
			// Turn auto-building off
			setAutoBuilding(false);
			// Create some resources
			project.create(getMonitor());
			project.open(getMonitor());
			// Create and set a build specs for project
			IProjectDescription desc = project.getDescription();
			desc.setBuildSpec(new ICommand[] {createCommand(desc, "Build0")});
			project.setDescription(desc, getMonitor());
			command = project.getDescription().getBuildSpec()[0];
		} catch (CoreException e) {
			fail("0.99", e);
		}
		assertTrue("1.0", !command.isConfigurable());
		//ensure that setBuilding has no effect
		command.setBuilding(IncrementalProjectBuilder.AUTO_BUILD, false);
		assertTrue("1.1", command.isBuilding(IncrementalProjectBuilder.AUTO_BUILD));
		command.setBuilding(IncrementalProjectBuilder.CLEAN_BUILD, false);
		assertTrue("1.2", command.isBuilding(IncrementalProjectBuilder.CLEAN_BUILD));
		command.setBuilding(IncrementalProjectBuilder.FULL_BUILD, false);
		assertTrue("1.3", command.isBuilding(IncrementalProjectBuilder.FULL_BUILD));
		command.setBuilding(IncrementalProjectBuilder.INCREMENTAL_BUILD, false);
		assertTrue("1.4", command.isBuilding(IncrementalProjectBuilder.INCREMENTAL_BUILD));

		//set the command back into the project for change to take effect
		try {
			IProjectDescription desc = project.getDescription();
			desc.setBuildSpec(new ICommand[] {command});
			project.setDescription(desc, getMonitor());
		} catch (CoreException e1) {
			fail("1.99", e1);
		}

		//ensure that builder is still called
		try {
			project.build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
		} catch (CoreException e) {
			fail("2.99", e);
		}
		SortBuilder builder = SortBuilder.getInstance();
		assertTrue("2.0", builder.wasBuilt());
		assertTrue("2.1", builder.wasFullBuild());
		assertEquals("2.2", command, builder.getCommand());

		try {
			project.touch(getMonitor());
			project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
		} catch (CoreException e) {
			fail("3.99", e);
		}
		assertTrue("3.0", builder.wasBuilt());
		assertTrue("3.1", builder.wasIncrementalBuild());
	}

	/**
	 * Tests that a builder that skips autobuild still receives the correct resource delta
	 * See bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=173931
	 */
	public void testSkipAutobuildDelta() {
		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject("PROJECT" + 1);
		ICommand command = null;
		CustomTriggerBuilder.resetSingleton();
		try {
			// Turn auto-building off
			setAutoBuilding(false);
			// Create some resources
			project.create(getMonitor());
			project.open(getMonitor());
			// Create and set a build specs for project
			IProjectDescription desc = project.getDescription();
			command = createCommand(desc, CustomTriggerBuilder.BUILDER_NAME, "Build0");
			command.setBuilding(IncrementalProjectBuilder.AUTO_BUILD, false);
			desc.setBuildSpec(new ICommand[] {command});
			project.setDescription(desc, getMonitor());
			command = project.getDescription().getBuildSpec()[0];
			//turn autobuild back on
			setAutoBuilding(true);
		} catch (CoreException e) {
			fail("0.99", e);
		}
		assertTrue("1.0", command.isConfigurable());
		//ensure that setBuilding has effect
		assertTrue("1.1", !command.isBuilding(IncrementalProjectBuilder.AUTO_BUILD));

		//do an initial build to get the builder instance
		try {
			project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
		} catch (CoreException e) {
			fail("1.2", e);
		}
		waitForBuild();
		CustomTriggerBuilder builder = CustomTriggerBuilder.getInstance();
		assertNotNull("1.3", builder);
		builder.clearBuildTrigger();

		//add a file in the project, to trigger an autobuild
		IFile file = project.getFile("a.txt");
		try {
			file.create(getRandomContents(), IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("1.99", e);
		}
		//autobuild should not call our builder
		waitForBuild();
		assertTrue("2.0", !builder.wasIncrementalBuild());
		assertTrue("2.1", !builder.wasAutobuild());

		//but, a subsequent incremental build should call it
		try {
			project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
		} catch (CoreException e) {
			fail("2.99", e);
		}
		assertTrue("2.1", !builder.wasAutobuild());
		assertTrue("3.0", builder.wasIncrementalBuild());

	}

	/**
	 * Tests that a builder that responds only to the "full" trigger will be called
	 * on the first and only first build after a clean.
	 * See bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=206540.
	 */
	public void testCleanBuild_AfterCleanBuilder() throws CoreException {
		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject("PROJECT" + 1);
		ICommand command = null;

		// Create some resources
		project.create(getMonitor());
		project.open(getMonitor());

		// Create and set a build specs for project
		IProjectDescription desc = project.getDescription();
		command = createCommand(desc, CustomTriggerBuilder.BUILDER_NAME, "Build0");
		command.setBuilding(IncrementalProjectBuilder.AUTO_BUILD, false);
		command.setBuilding(IncrementalProjectBuilder.FULL_BUILD, true);
		command.setBuilding(IncrementalProjectBuilder.INCREMENTAL_BUILD, false);
		command.setBuilding(IncrementalProjectBuilder.CLEAN_BUILD, false);
		desc.setBuildSpec(new ICommand[] {command});
		project.setDescription(desc, getMonitor());
		command = project.getDescription().getBuildSpec()[0];

		// turn auto-building off
		setAutoBuilding(false);

		// do an initial build to get the builder instance
		try {
			project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}
		waitForBuild();
		CustomTriggerBuilder builder = CustomTriggerBuilder.getInstance();
		assertNotNull("2.0", builder);
		assertTrue("2.1", builder.wasFullBuild());

		// do a clean - builder should not be called
		builder.clearBuildTrigger();
		builder.reset();
		workspace.build(IncrementalProjectBuilder.CLEAN_BUILD, getMonitor());
		assertTrue("3.0", !builder.wasCleanBuild());
		assertTrue("3.1", !builder.wasFullBuild());

		// do an incremental build - FULL_BUILD should be triggered
		builder.clearBuildTrigger();
		builder.reset();
		workspace.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
		waitForBuild();
		assertTrue("4.0", !builder.wasCleanBuild());
		assertTrue("4.1", builder.wasFullBuild());

		// add a file in the project before an incremental build is triggered again
		IFile file = project.getFile("a.txt");
		try {
			file.create(getRandomContents(), IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("5.00", e);
		}

		// do an incremental build - build should NOT be triggered
		builder.clearBuildTrigger();
		builder.reset();
		workspace.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
		waitForBuild();
		assertTrue("6.0", !builder.wasCleanBuild());
		assertTrue("6.1", !builder.wasFullBuild());
	}

	/**
	 * Tests that a builder that responds only to the "full" trigger will be called
	 * on the first and only first (non-auto) build after a clean.
	 * See bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=206540.
	 */
	public void testCleanAutoBuild_AfterCleanBuilder() throws CoreException {
		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject("PROJECT" + 1);
		ICommand command = null;

		// Create some resources
		project.create(getMonitor());
		project.open(getMonitor());

		// Create and set a build specs for project
		IProjectDescription desc = project.getDescription();
		command = createCommand(desc, CustomTriggerBuilder.BUILDER_NAME, "Build0");
		command.setBuilding(IncrementalProjectBuilder.AUTO_BUILD, false);
		command.setBuilding(IncrementalProjectBuilder.FULL_BUILD, true);
		command.setBuilding(IncrementalProjectBuilder.INCREMENTAL_BUILD, false);
		command.setBuilding(IncrementalProjectBuilder.CLEAN_BUILD, false);
		desc.setBuildSpec(new ICommand[] {command});
		project.setDescription(desc, getMonitor());
		command = project.getDescription().getBuildSpec()[0];

		// turn auto-building on
		setAutoBuilding(true);
		waitForBuild();
		CustomTriggerBuilder builder = CustomTriggerBuilder.getInstance();
		assertNotNull("1.0", builder);
		assertEquals("1.1", 0, builder.triggerForLastBuild);

		// do a clean - builder should not be called
		builder.clearBuildTrigger();
		builder.reset();
		workspace.build(IncrementalProjectBuilder.CLEAN_BUILD, getMonitor());
		assertTrue("2.0", !builder.wasCleanBuild());
		assertTrue("2.1", !builder.wasFullBuild());

		// add a file in the project to trigger an auto-build - no FULL_BUILD should be triggered
		builder.clearBuildTrigger();
		builder.reset();

		IFile file = project.getFile("a.txt");
		try {
			file.create(getRandomContents(), IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("3.00", e);
		}

		waitForBuild();
		assertEquals("4.0", 0, builder.triggerForLastBuild);

		// Build the project explicitly -- full build should be triggered
		project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
		waitForBuild();
		assertTrue("4.1", builder.wasFullBuild());

		// add another file in the project to trigger an auto-build - build should NOT be triggered
		builder.clearBuildTrigger();
		builder.reset();

		file = project.getFile("b.txt");
		try {
			file.create(getRandomContents(), IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("5.00", e);
		}

		waitForBuild();
		assertTrue("6.0", !builder.wasCleanBuild());
		assertTrue("6.1", !builder.wasFullBuild());
	}
}
