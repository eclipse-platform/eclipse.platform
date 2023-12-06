/*******************************************************************************
 *  Copyright (c) 2000, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *******************************************************************************/
package org.eclipse.core.tests.internal.builders;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.waitForBuild;
import static org.eclipse.core.tests.resources.ResourceTestUtil.waitForEncodingRelatedJobs;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.tests.harness.FussyProgressMonitor;
import org.eclipse.core.tests.harness.TestBarrier2;
import org.eclipse.core.tests.harness.TestJob;
import org.junit.function.ThrowingRunnable;

/**
 * This class tests public API related to building and to build specifications.
 * Specifically, the following methods are tested:
 *
 * IWorkspace#build IProject#build IProjectDescription#getBuildSpec
 * IProjectDescription#setBuildSpec
 */
public class BuilderTest extends AbstractBuilderTest {

	/**
	 * BuilderTest constructor comment.
	 *
	 * @param name
	 *                  java.lang.String
	 */
	public BuilderTest(String name) {
		super(name);
	}

	/**
	 * Tears down the fixture, for example, close a network connection. This
	 * method is called after a test is executed.
	 */
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		TestBuilder builder = SortBuilder.getInstance();
		if (builder != null) {
			builder.reset();
		}
		builder = DeltaVerifierBuilder.getInstance();
		if (builder != null) {
			builder.reset();
		}
	}

	/**
	 * Make sure this test runs first, before any other test
	 * has a chance to mess with the build order.
	 */
	public void testAardvarkBuildOrder() {
		IWorkspace workspace = getWorkspace();
		//builder order should initially be null
		assertNull(workspace.getDescription().getBuildOrder());
	}

	/**
	 * Tests the lifecycle of a builder.
	 *
	 * @see SortBuilder
	 */
	public void testAutoBuildPR() throws CoreException {
		//REF: 1FUQUJ4
		// Create some resource handles
		IWorkspace workspace = getWorkspace();
		IProject project1 = workspace.getRoot().getProject("PROJECT" + 1);
		IFolder folder = project1.getFolder("FOLDER");
		IFolder sub = folder.getFolder("sub");
		IFile fileA = folder.getFile("A");
		IFile fileB = sub.getFile("B");
		// Create some resources
		// Turn auto-building on
		setAutoBuilding(true);
		project1.create(createTestMonitor());
		project1.open(createTestMonitor());
		// Set build spec
		IProjectDescription desc = project1.getDescription();
		ICommand command = desc.newCommand();
		command.setBuilderName(SortBuilder.BUILDER_NAME);
		command.getArguments().put(TestBuilder.BUILD_ID, "Project1Build1");
		desc.setBuildSpec(new ICommand[] { command });
		project1.setDescription(desc, createTestMonitor());

		// Create folders and files
		folder.create(true, true, createTestMonitor());
		fileA.create(getRandomContents(), true, createTestMonitor());
		sub.create(true, true, createTestMonitor());
		fileB.create(getRandomContents(), true, createTestMonitor());
	}

	/**
	 * Tests installing and running a builder that always fails during
	 * instantation.
	 */
	public void testBrokenBuilder() throws CoreException {
		// Create some resource handles
		IProject project = getWorkspace().getRoot().getProject("PROJECT");
		setAutoBuilding(false);
		// Create and open a project
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		// Create and set a build spec for the project
		IProjectDescription desc = project.getDescription();
		ICommand command1 = desc.newCommand();
		command1.setBuilderName(BrokenBuilder.BUILDER_NAME);
		ICommand command2 = desc.newCommand();
		command2.setBuilderName(SortBuilder.BUILDER_NAME);
		desc.setBuildSpec(new ICommand[] { command1, command2 });
		project.setDescription(desc, createTestMonitor());
		//do an incremental build -- build should fail, but second builder
		// should run
		assertThrows(CoreException.class,
				() -> getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor()));

		TestBuilder verifier = SortBuilder.getInstance();
		verifier.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
		verifier.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
		verifier.addExpectedLifecycleEvent(TestBuilder.DEFAULT_BUILD_ID);
		verifier.assertLifecycleEvents();

		//build again -- it should succeed this time
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
	}

	public void testBuildClean() throws CoreException {
		// Create some resource handles
		IProject project = getWorkspace().getRoot().getProject("PROJECT");

		// Turn auto-building off
		setAutoBuilding(false);
		// Create and open a project
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		// Create and set a build spec for the project
		IProjectDescription desc = project.getDescription();
		desc.setBuildSpec(new ICommand[] { createCommand(desc, DeltaVerifierBuilder.BUILDER_NAME, "Project2Build2") });
		project.setDescription(desc, createTestMonitor());

		//start with a clean build
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		getWorkspace().build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
		monitor.assertUsedUp();

		DeltaVerifierBuilder verifier = DeltaVerifierBuilder.getInstance();
		assertTrue("3.2", verifier.wasCleanBuild());
		// Now do an incremental build - since delta was null it should appear as a clean build
		monitor = new FussyProgressMonitor();
		getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
		monitor.assertUsedUp();
		assertTrue("3.4", verifier.wasFullBuild());
		// next time it will appear as an incremental build
		project.touch(createTestMonitor());
		monitor = new FussyProgressMonitor();
		getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
		monitor.assertUsedUp();
		assertTrue("3.6", verifier.wasIncrementalBuild());

		//do another clean
		monitor = new FussyProgressMonitor();
		getWorkspace().build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
		monitor.assertUsedUp();
		assertTrue("3.8", verifier.wasCleanBuild());

		//doing a full build should still look like a full build
		monitor = new FussyProgressMonitor();
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, monitor);
		monitor.assertUsedUp();
		assertTrue("3.10", verifier.wasFullBuild());
	}

	/**
	 * Tests the lifecycle of a builder.
	 *
	 * @see SortBuilder
	 */
	public void testBuildCommands() throws CoreException {
		// Create some resource handles
		IWorkspace workspace = getWorkspace();
		IProject project1 = workspace.getRoot().getProject("PROJECT" + 1);
		IProject project2 = workspace.getRoot().getProject("PROJECT" + 2);
		IFile file1 = project1.getFile("FILE1");
		IFile file2 = project2.getFile("FILE2");
		//set the build order
		IWorkspaceDescription workspaceDesc = workspace.getDescription();
		workspaceDesc.setBuildOrder(new String[] { project1.getName(), project2.getName() });
		workspace.setDescription(workspaceDesc);
		TestBuilder verifier = null;

		// Turn auto-building off
		setAutoBuilding(false);
		// Create some resources
		project1.create(createTestMonitor());
		project1.open(createTestMonitor());
		project2.create(createTestMonitor());
		project2.open(createTestMonitor());
		file1.create(getRandomContents(), true, createTestMonitor());
		file2.create(getRandomContents(), true, createTestMonitor());
		// Do an initial build to get the builder instance
		IProjectDescription desc = project1.getDescription();
		desc.setBuildSpec(new ICommand[] { createCommand(desc, "Project1Build1") });
		project1.setDescription(desc, createTestMonitor());
		project1.build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		verifier = SortBuilder.getInstance();
		verifier.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
		verifier.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
		verifier.addExpectedLifecycleEvent("Project1Build1");
		verifier.assertLifecycleEvents();

		// Build spec with no commands
		desc = project1.getDescription();
		desc.setBuildSpec(new ICommand[] {});
		project1.setDescription(desc, createTestMonitor());

		// Build the project -- should do nothing
		verifier.reset();
		dirty(file1);
		project1.build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		verifier.assertLifecycleEvents();

		// Build command with no arguments -- will use default build ID
		desc = project1.getDescription();
		ICommand command = desc.newCommand();
		command.setBuilderName(SortBuilder.BUILDER_NAME);
		desc.setBuildSpec(new ICommand[] { command });
		project1.setDescription(desc, createTestMonitor());

		// Build the project
		// Note that since the arguments have changed, the identity of the build
		// command is different so a new builder will be instantiated
		dirty(file1);
		project1.build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		verifier.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
		verifier.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
		verifier.addExpectedLifecycleEvent(TestBuilder.DEFAULT_BUILD_ID);
		verifier.assertLifecycleEvents();

		// Create and set a build specs for project one
		desc = project1.getDescription();
		desc.setBuildSpec(new ICommand[] { createCommand(desc, "Project1Build1") });
		project1.setDescription(desc, createTestMonitor());

		// Create and set a build spec for project two
		desc = project2.getDescription();
		desc.setBuildSpec(new ICommand[] { createCommand(desc, SortBuilder.BUILDER_NAME, "Project2Build1"),
				createCommand(desc, DeltaVerifierBuilder.BUILDER_NAME, "Project2Build2") });
		project2.setDescription(desc, createTestMonitor());

		// Build
		verifier.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
		verifier.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
		verifier.addExpectedLifecycleEvent("Project1Build1");
		// second builder is touched for the first time
		verifier.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
		verifier.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
		verifier.addExpectedLifecycleEvent("Project2Build1");
		verifier.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
		verifier.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
		verifier.addExpectedLifecycleEvent("Project2Build2");
		dirty(file1);
		dirty(file2);
		workspace.build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		verifier.assertLifecycleEvents();
		verifier.addExpectedLifecycleEvent("Project1Build1");
		dirty(file1);
		project1.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		verifier.assertLifecycleEvents();
		dirty(file2);
		project2.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		verifier.addExpectedLifecycleEvent("Project2Build1");
		verifier.addExpectedLifecycleEvent("Project2Build2");
		verifier.assertLifecycleEvents();

		// Change order of build commands
		desc = project2.getDescription();
		desc.setBuildSpec(new ICommand[] { createCommand(desc, DeltaVerifierBuilder.BUILDER_NAME, "Project2Build2"),
				createCommand(desc, SortBuilder.BUILDER_NAME, "Project2Build1") });
		project2.setDescription(desc, createTestMonitor());

		// Build
		workspace.build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		verifier.addExpectedLifecycleEvent("Project1Build1");
		verifier.addExpectedLifecycleEvent("Project2Build2");
		verifier.addExpectedLifecycleEvent("Project2Build1");
		verifier.assertLifecycleEvents();
		dirty(file1);
		dirty(file2);
		project1.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		verifier.addExpectedLifecycleEvent("Project1Build1");
		verifier.assertLifecycleEvents();
		dirty(file1);
		dirty(file2);
		project2.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		verifier.addExpectedLifecycleEvent("Project2Build2");
		verifier.addExpectedLifecycleEvent("Project2Build1");
		verifier.assertLifecycleEvents();
	}

	/**
	 * Tests that a pre_build listener is not called if there have been no changes
	 * since the last build of any kind occurred.  See https://bugs.eclipse.org/bugs/show_bug.cgi?id=154880.
	 */
	public void testPreBuildEvent() throws CoreException {
		IWorkspace workspace = getWorkspace();
		// Create some resource handles
		final boolean[] notified = new boolean[] {false};
		IProject proj1 = workspace.getRoot().getProject("PROJECT" + 1);
		final IResourceChangeListener listener = event -> notified[0] = true;
		try {
			workspace.addResourceChangeListener(listener, IResourceChangeEvent.PRE_BUILD);
			// Turn auto-building off
			setAutoBuilding(false);
			// Create some resources
			proj1.create(createTestMonitor());
			proj1.open(createTestMonitor());
			// Create and set a build spec for project one
			IProjectDescription desc = proj1.getDescription();
			desc.setBuildSpec(new ICommand[] {createCommand(desc, "Build0")});
			proj1.setDescription(desc, createTestMonitor());
			proj1.build(IncrementalProjectBuilder.FULL_BUILD, SortBuilder.BUILDER_NAME, new HashMap<>(), null);
			notified[0] = false;
			//now turn on autobuild and see if the listener is notified again
			setAutoBuilding(true);
			assertFalse(notified[0]);
		} finally {
			workspace.removeResourceChangeListener(listener);
		}
	}

	/**
	 * Tests the lifecycle of a builder.
	 *
	 * @see SortBuilder
	 */
	public void testBuildOrder() throws CoreException {
		IWorkspace workspace = getWorkspace();
		// Create some resource handles
		IProject proj1 = workspace.getRoot().getProject("PROJECT" + 1);
		IProject proj2 = workspace.getRoot().getProject("PROJECT" + 2);

		// Turn auto-building off
		setAutoBuilding(false);
		// Create some resources
		proj1.create(createTestMonitor());
		proj1.open(createTestMonitor());
		proj2.create(createTestMonitor());
		proj2.open(createTestMonitor());
		// set the build order
		setBuildOrder(proj1, proj2);

		// Create and set a build specs for project one
		IProjectDescription desc = proj1.getDescription();
		desc.setBuildSpec(new ICommand[] { createCommand(desc, "Build0") });
		proj1.setDescription(desc, createTestMonitor());

		// Create and set a build spec for project two
		desc = proj2.getDescription();
		desc.setBuildSpec(new ICommand[] { createCommand(desc, "Build1"), createCommand(desc, "Build2") });
		proj2.setDescription(desc, createTestMonitor());

		// Build the workspace
		workspace.build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		// Set up a plug-in lifecycle verifier for testing purposes
		TestBuilder verifier = SortBuilder.getInstance();
		verifier.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
		verifier.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
		verifier.addExpectedLifecycleEvent("Build0");
		verifier.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
		verifier.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
		verifier.addExpectedLifecycleEvent("Build1");
		verifier.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
		verifier.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
		verifier.addExpectedLifecycleEvent("Build2");
		verifier.assertLifecycleEvents();

		//build in reverse order
		setBuildOrder(proj2, proj1);
		workspace.build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		verifier.addExpectedLifecycleEvent("Build1");
		verifier.addExpectedLifecycleEvent("Build2");
		verifier.addExpectedLifecycleEvent("Build0");
		verifier.assertLifecycleEvents();

		//only specify build order for project1
		setBuildOrder(proj1);
		workspace.build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		verifier.addExpectedLifecycleEvent("Build0");
		verifier.addExpectedLifecycleEvent("Build1");
		verifier.addExpectedLifecycleEvent("Build2");
		verifier.assertLifecycleEvents();

		//only specify build order for project2
		setBuildOrder(proj2, proj1);
		workspace.build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		verifier.addExpectedLifecycleEvent("Build1");
		verifier.addExpectedLifecycleEvent("Build2");
		verifier.addExpectedLifecycleEvent("Build0");
		verifier.assertLifecycleEvents();
	}

	/**
	 * Tests that changing the dynamic build order will induce an autobuild on a project.
	 * This is a regression test for bug 60653.
	 */
	public void testChangeDynamicBuildOrder() throws CoreException {
		IWorkspace workspace = getWorkspace();
		// Create some resource handles
		final IProject proj1 = workspace.getRoot().getProject("PROJECT" + 1);
		final IProject proj2 = workspace.getRoot().getProject("PROJECT" + 2);

		// Turn auto-building on and make sure there is no explicit build order
		setAutoBuilding(true);
		IWorkspaceDescription wsDescription = getWorkspace().getDescription();
		wsDescription.setBuildOrder(null);
		getWorkspace().setDescription(wsDescription);
		// Create and set a build spec for project two
		getWorkspace().run((IWorkspaceRunnable) monitor -> {
			proj2.create(createTestMonitor());
			proj2.open(createTestMonitor());
			IProjectDescription desc = proj2.getDescription();
			desc.setBuildSpec(new ICommand[] { createCommand(desc, "Build1") });
			proj2.setDescription(desc, createTestMonitor());
		}, createTestMonitor());
		waitForBuild();

		// Set up a plug-in lifecycle verifier for testing purposes
		TestBuilder verifier = SortBuilder.getInstance();
		verifier.reset();
		//create project two and establish a build order by adding a dynamic
		//reference from proj2->proj1 in the same operation
		getWorkspace().run((IWorkspaceRunnable) monitor -> extracted(proj1, proj2), createTestMonitor());

		waitForBuild();
		//ensure the build happened in the correct order, and that both projects were built
		verifier.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
		verifier.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
		verifier.addExpectedLifecycleEvent("Build0");
		verifier.addExpectedLifecycleEvent("Build1");
		verifier.assertLifecycleEvents();
	}

	private void extracted(final IProject proj1, final IProject proj2) throws CoreException {
		// Create and set a build specs for project one
		proj1.create(createTestMonitor());
		proj1.open(createTestMonitor());
		IProjectDescription desc = proj1.getDescription();
		desc.setBuildSpec(new ICommand[] { createCommand(desc, "Build0") });
		proj1.setDescription(desc, createTestMonitor());
		// add the dynamic reference to project two
		IProjectDescription description = proj2.getDescription();
		description.setDynamicReferences(new IProject[] { proj1 });
		proj2.setDescription(description, IResource.NONE, null);
	}

	/**
	 * Tests that changing the dynamic build order during a pre-build notification causes projects
	 * to be built in the correct order.
	 * This is a regression test for bug 330194.
	 */
	public void testChangeDynamicBuildOrderDuringPreBuild() throws Throwable {
		IWorkspace workspace = getWorkspace();
		// Create some resource handles
		final IProject proj1 = workspace.getRoot().getProject("bug_330194_referencer");
		final IProject proj2 = workspace.getRoot().getProject("bug_330194_referencee");
		// Disable workspace auto-build
		setAutoBuilding(false);

		proj1.create(createTestMonitor());
		proj1.open(createTestMonitor());
		proj2.create(createTestMonitor());
		proj2.open(createTestMonitor());

		IProjectDescription desc = proj1.getDescription();
		desc.setBuildSpec(new ICommand[] {createCommand(desc, "Build0")});
		proj1.setDescription(desc, createTestMonitor());

		desc = proj2.getDescription();
		desc.setBuildSpec(new ICommand[] {createCommand(desc, "Build1")});
		proj2.setDescription(desc, createTestMonitor());

		// Ensure the builder is instantiated
		workspace.build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());

		final AtomicReference<ThrowingRunnable> exceptionInMainThreadCallback = new AtomicReference<>(
				Function::identity);

		// Add pre-build listener that swap around the dependencies
		IResourceChangeListener buildListener = event -> {
			try {
				IProjectDescription desc1 = proj1.getDescription();
				IProjectDescription desc2 = proj2.getDescription();
				// Swap around the references
				if (desc1.getDynamicReferences().length == 0) {
					desc1.setDynamicReferences(new IProject[] {proj2});
					desc2.setDynamicReferences(new IProject[0]);
				} else {
					desc1.setDynamicReferences(new IProject[0]);
					desc2.setDynamicReferences(new IProject[] {proj1});
				}
				proj1.setDescription(desc1, createTestMonitor());
				proj2.setDescription(desc2, createTestMonitor());
			} catch (CoreException e) {
				exceptionInMainThreadCallback.set(() -> {
					throw e;
				});
			}
		};
		try {
			getWorkspace().addResourceChangeListener(buildListener, IResourceChangeEvent.PRE_BUILD);
			// Set up a plug-in lifecycle verifier for testing purposes
			TestBuilder verifier = SortBuilder.getInstance();
			verifier.reset();

			// FULL_BUILD 1
			workspace.build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
			verifier.addExpectedLifecycleEvent("Build1");
			verifier.addExpectedLifecycleEvent("Build0");
			verifier.assertLifecycleEvents();
			verifier.reset();

			// FULL_BUILD 2
			workspace.build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
			verifier.addExpectedLifecycleEvent("Build0");
			verifier.addExpectedLifecycleEvent("Build1");
			verifier.assertLifecycleEvents();
			verifier.reset();

			// AUTO_BUILD
			setAutoBuilding(true);
			proj1.touch(createTestMonitor());
			waitForBuild();
			verifier.addExpectedLifecycleEvent("Build1");
			verifier.addExpectedLifecycleEvent("Build0");
			verifier.assertLifecycleEvents();
			verifier.reset();

			// AUTO_BUILD 2
			proj1.touch(createTestMonitor());
			waitForBuild();
			verifier.addExpectedLifecycleEvent("Build0");
			verifier.addExpectedLifecycleEvent("Build1");
			verifier.assertLifecycleEvents();
			verifier.reset();

			exceptionInMainThreadCallback.get().run();
		} finally {
			getWorkspace().removeResourceChangeListener(buildListener);
		}
	}

	/**
	 * Ensure that build order is preserved when project is closed/opened.
	 */
	public void testCloseOpenProject() throws CoreException {
		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject("PROJECT" + 1);
		// Create some resources
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		// Create and set a build spec
		IProjectDescription desc = project.getDescription();
		desc.setBuildSpec(new ICommand[] { createCommand(desc, "Build1"), createCommand(desc, "Build2") });
		project.setDescription(desc, createTestMonitor());

		project.close(createTestMonitor());
		project.open(createTestMonitor());

		//ensure the build spec hasn't changed
		desc = project.getDescription();
		ICommand[] commands = desc.getBuildSpec();
		assertEquals(2, commands.length);
		assertEquals(commands[0].getBuilderName(), SortBuilder.BUILDER_NAME);
		assertEquals(commands[1].getBuilderName(), SortBuilder.BUILDER_NAME);
		Map<String, String> args = commands[0].getArguments();
		assertEquals("Build1", args.get(TestBuilder.BUILD_ID));
		args = commands[1].getArguments();
		assertEquals("Build2", args.get(TestBuilder.BUILD_ID));
	}

	/**
	 * Tests that when a project is copied, the copied project has a full build
	 * but the source project does not.
	 */
	public void testCopyProject() throws CoreException {
		IWorkspace workspace = getWorkspace();
		// Create some resource handles
		IProject proj1 = workspace.getRoot().getProject("testCopyProject" + 1);
		IProject proj2 = workspace.getRoot().getProject("testCopyProject" + 2);

		// Turn auto-building on
		setAutoBuilding(true);
		// Create some resources
		proj1.create(createTestMonitor());
		proj1.open(createTestMonitor());
		removeFromWorkspace(proj2);

		// Create and set a build spec for project one
		IProjectDescription desc = proj1.getDescription();
		desc.setBuildSpec(new ICommand[] { createCommand(desc, "Build0") });
		proj1.setDescription(desc, createTestMonitor());

		waitForBuild();
		SortBuilder.getInstance().reset();
		desc = proj1.getDescription();
		desc.setName(proj2.getName());
		proj1.copy(desc, IResource.NONE, createTestMonitor());

		waitForEncodingRelatedJobs(getName());
		waitForBuild();
		SortBuilder builder = SortBuilder.getInstance();
		assertEquals(proj2, builder.getProject());

		//builder 2 should have done a full build
		builder.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
		builder.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
		builder.addExpectedLifecycleEvent("Build0");
		builder.assertLifecycleEvents();
		assertTrue(builder.wasFullBuild());
	}

	/**
	 * Tests an implicit workspace build order created by setting dynamic
	 * project references.
	 */
	public void testDynamicBuildOrder() throws CoreException {
		IWorkspace workspace = getWorkspace();
		// Create some resource handles
		IProject proj1 = workspace.getRoot().getProject("PROJECT" + 1);
		IProject proj2 = workspace.getRoot().getProject("PROJECT" + 2);

		// Turn auto-building off
		setAutoBuilding(false);
		// Create some resources
		proj1.create(createTestMonitor());
		proj1.open(createTestMonitor());
		proj2.create(createTestMonitor());
		proj2.open(createTestMonitor());
		// establish a build order by adding a dynamic reference from
		// proj2->proj1
		IProjectDescription description = proj2.getDescription();
		description.setDynamicReferences(new IProject[] { proj1 });
		proj2.setDescription(description, IResource.NONE, null);
		IWorkspaceDescription wsDescription = getWorkspace().getDescription();
		wsDescription.setBuildOrder(null);
		getWorkspace().setDescription(wsDescription);

		// Create and set a build specs for project one
		IProjectDescription desc = proj1.getDescription();
		desc.setBuildSpec(new ICommand[] { createCommand(desc, "Build0") });
		proj1.setDescription(desc, createTestMonitor());

		// Create and set a build spec for project two
		desc = proj2.getDescription();
		desc.setBuildSpec(new ICommand[] { createCommand(desc, "Build1"), createCommand(desc, "Build2") });
		proj2.setDescription(desc, createTestMonitor());

		// Build the workspace
		workspace.build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		// Set up a plug-in lifecycle verifier for testing purposes
		TestBuilder verifier = SortBuilder.getInstance();
		verifier.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
		verifier.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
		verifier.addExpectedLifecycleEvent("Build0");
		verifier.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
		verifier.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
		verifier.addExpectedLifecycleEvent("Build1");
		verifier.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
		verifier.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
		verifier.addExpectedLifecycleEvent("Build2");
		verifier.assertLifecycleEvents();

		//build in reverse order
		// reverse the order by adding a dynamic reference from proj1->proj2
		description = proj2.getDescription();
		description.setDynamicReferences(new IProject[0]);
		proj2.setDescription(description, IResource.NONE, null);
		description = proj1.getDescription();
		description.setDynamicReferences(new IProject[] { proj2 });
		proj1.setDescription(description, IResource.NONE, null);
		workspace.build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		verifier.addExpectedLifecycleEvent("Build1");
		verifier.addExpectedLifecycleEvent("Build2");
		verifier.addExpectedLifecycleEvent("Build0");
		verifier.assertLifecycleEvents();
	}

	/**
	 * Tests that enabling autobuild causes a build to occur.
	 */
	public void testEnableAutobuild() throws CoreException {
		// Create some resource handles
		IProject project = getWorkspace().getRoot().getProject("PROJECT");

		// Turn auto-building off
		setAutoBuilding(false);
		// Create and open a project
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		// Create and set a build spec for the project
		IProjectDescription desc = project.getDescription();
		ICommand command = desc.newCommand();
		command.setBuilderName(SortBuilder.BUILDER_NAME);
		desc.setBuildSpec(new ICommand[] { command });
		project.setDescription(desc, createTestMonitor());

		// Cause a build by enabling autobuild
		setAutoBuilding(true);
		// Set up a plug-in lifecycle verifier for testing purposes
		TestBuilder verifier = SortBuilder.getInstance();
		verifier.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
		verifier.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
		verifier.addExpectedLifecycleEvent(TestBuilder.DEFAULT_BUILD_ID);
		verifier.assertLifecycleEvents();
	}

	/**
	 * Tests installing and running a builder that always fails in its build method
	 */
	public void testExceptionBuilder() throws CoreException {
		// Create some resource handles
		IProject project = getWorkspace().getRoot().getProject("PROJECT");
		setAutoBuilding(false);
		// Create and open a project
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		// Create and set a build spec for the project
		IProjectDescription desc = project.getDescription();
		ICommand command1 = desc.newCommand();
		command1.setBuilderName(ExceptionBuilder.BUILDER_NAME);
		desc.setBuildSpec(new ICommand[] { command1 });
		project.setDescription(desc, createTestMonitor());

		final AtomicReference<Boolean> listenerCalled = new AtomicReference<>();
		IResourceChangeListener listener = event -> listenerCalled.set(true);
		try {
			getWorkspace().addResourceChangeListener(listener, IResourceChangeEvent.POST_BUILD);
			// do an incremental build -- build should fail, but POST_BUILD should still
			// occur
			CoreException exception = assertThrows(CoreException.class,
				() -> getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor()));
			// see discussion in bug 273147 about build exception severity
			assertEquals(IStatus.ERROR, exception.getStatus().getSeverity());
		} finally {
			getWorkspace().removeResourceChangeListener(listener);
		}
		assertTrue(listenerCalled.get());
	}

	/**
	 * Tests the method IncrementProjectBuilder.forgetLastBuiltState
	 */
	public void testForgetLastBuiltState() throws CoreException {
		// Create some resource handles
		IProject project = getWorkspace().getRoot().getProject("PROJECT");

		// Turn auto-building off
		setAutoBuilding(false);
		// Create and open a project
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		// Create and set a build spec for the project
		IProjectDescription desc = project.getDescription();
		ICommand command = desc.newCommand();
		command.setBuilderName(SortBuilder.BUILDER_NAME);
		desc.setBuildSpec(new ICommand[] { command });
		project.setDescription(desc, createTestMonitor());

		// Set up a plug-in lifecycle verifier for testing purposes
		SortBuilder verifier = null;
		//do an initial build
		project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, SortBuilder.BUILDER_NAME, null, createTestMonitor());
		verifier = SortBuilder.getInstance();

		//forget last built state
		verifier.forgetLastBuiltState();
		// Now do another incremental build. Delta should be null
		project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, SortBuilder.BUILDER_NAME, null, createTestMonitor());
		assertTrue(verifier.wasDeltaNull());

		// Do another incremental build, requesting a null build state. Delta
		// should not be null
		verifier.requestForgetLastBuildState();
		project.touch(createTestMonitor());
		project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, SortBuilder.BUILDER_NAME, null, createTestMonitor());
		assertFalse(verifier.wasDeltaNull());

		//try a snapshot when a builder has a null tree
		getWorkspace().save(false, createTestMonitor());

		// Do another incremental build. Delta should be null
		project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, SortBuilder.BUILDER_NAME, null, createTestMonitor());
		assertTrue(verifier.wasDeltaNull());
	}

	/**
	 * Tests that a client invoking a manual incremental build before autobuild has had
	 * a chance to run will block until the build completes. See bug 275879.
	 */
	public void testIncrementalBuildBeforeAutobuild() throws Exception {
		// Create some resource handles
		final IProject project = getWorkspace().getRoot().getProject("PROJECT");
		final IFile input = project.getFolder(SortBuilder.DEFAULT_UNSORTED_FOLDER).getFile("File.txt");
		final IFile output = project.getFolder(SortBuilder.DEFAULT_SORTED_FOLDER).getFile("File.txt");

		setAutoBuilding(true);
		// Create and open a project
		project.create(createTestMonitor());
		project.open(createTestMonitor());
		IProjectDescription desc = project.getDescription();
		ICommand command = desc.newCommand();
		command.setBuilderName(SortBuilder.BUILDER_NAME);
		desc.setBuildSpec(new ICommand[] { command });
		project.setDescription(desc, createTestMonitor());
		createInWorkspace(input, getRandomString());

		waitForBuild();
		assertTrue("1.0", output.exists());

		//change the file and then immediately perform build
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		AtomicReference<IOException> exception = new AtomicReference<>();
		getWorkspace().run((IWorkspaceRunnable) monitor -> {
			input.setContents(new ByteArrayInputStream(new byte[] { 5, 4, 3, 2, 1 }), IResource.NONE, createTestMonitor());
			project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
			try (InputStream inputStream = output.getContents()) {
				inputStream.transferTo(out);
			} catch (IOException e) {
				exception.set(e);
			}
		}, createTestMonitor());
		if (exception.get() != null) {
			throw exception.get();
		}

		byte[] result = out.toByteArray();
		byte[] expected = new byte[] {1, 2, 3, 4, 5};
		assertArrayEquals(expected, result);
	}

	/**
	 * Tests that autobuild is interrupted by a background scheduled job, but eventually completes.
	 */
	public void testInterruptAutobuild() throws Exception {
		// Create some resource handles
		IProject project = getWorkspace().getRoot().getProject("PROJECT");
		final IFile file = project.getFile("File.txt");

		setAutoBuilding(true);
		// Create and open a project
		project.create(createTestMonitor());
		project.open(createTestMonitor());
		IProjectDescription desc = project.getDescription();
		ICommand command = desc.newCommand();
		command.setBuilderName(SortBuilder.BUILDER_NAME);
		desc.setBuildSpec(new ICommand[] { command });
		project.setDescription(desc, createTestMonitor());
		file.create(getRandomContents(), IResource.NONE, createTestMonitor());
		waitForBuild();

		// Set up a plug-in lifecycle verifier for testing purposes
		TestBuilder verifier = SortBuilder.getInstance();
		verifier.reset();

		final TestJob blockedJob = new TestJob("Interrupt build", 3, 1000);
		blockedJob.setRule(getWorkspace().getRoot());
		//use a barrier to ensure the blocking job starts
		final TestBarrier2 barrier = new TestBarrier2();
		barrier.setStatus(TestBarrier2.STATUS_WAIT_FOR_START);
		//install a listener that will cause autobuild to be interrupted
		IResourceChangeListener listener = event -> {
			blockedJob.schedule();
			//wait for autobuild to become blocking
			while (!Job.getJobManager().currentJob().isBlocking()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					//ignore
				}
			}
			//allow the test main method to continue
			barrier.setStatus(TestBarrier2.STATUS_RUNNING);
		};
		try {
			getWorkspace().addResourceChangeListener(listener, IResourceChangeEvent.PRE_BUILD);
			// Now change a file. The build should not complete until the job triggered by the listener completes
			file.setContents(getRandomContents(), IResource.NONE, createTestMonitor());
			//wait for job to be scheduled
			barrier.waitForStatus(TestBarrier2.STATUS_RUNNING);
			//wait for test job to complete
			blockedJob.join();
			//autobuild should now run after the blocking job is finished
			waitForBuild();
			verifier.addExpectedLifecycleEvent(TestBuilder.DEFAULT_BUILD_ID);
			verifier.assertLifecycleEvents();
		} finally {
			getWorkspace().removeResourceChangeListener(listener);
		}
	}

	/**
	 * Tests the lifecycle of a builder.
	 */
	public void testLifecycleEvents() throws CoreException {
		// Create some resource handles
		IProject project = getWorkspace().getRoot().getProject("PROJECT");

		// Turn auto-building off
		setAutoBuilding(false);
		// Create and open a project
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		// Create and set a build spec for the project
		IProjectDescription desc = project.getDescription();
		ICommand command = desc.newCommand();
		command.setBuilderName(SortBuilder.BUILDER_NAME);
		desc.setBuildSpec(new ICommand[] { command });
		project.setDescription(desc, createTestMonitor());

		//try to do an incremental build when there has never
		//been a batch build
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
		// Set up a plug-in lifecycle verifier for testing purposes
		TestBuilder verifier = SortBuilder.getInstance();
		verifier.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
		verifier.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
		verifier.addExpectedLifecycleEvent(TestBuilder.DEFAULT_BUILD_ID);
		verifier.assertLifecycleEvents();
		monitor.assertUsedUp();

		// Now do another incremental build. Since we just did one, nothing
		// should happen in this one.
		monitor = new FussyProgressMonitor();
		getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
		verifier.assertLifecycleEvents();
		monitor.assertUsedUp();

		// Now do a batch build
		monitor = new FussyProgressMonitor();
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, monitor);
		verifier.addExpectedLifecycleEvent(TestBuilder.DEFAULT_BUILD_ID);
		verifier.assertLifecycleEvents();
		monitor.assertUsedUp();

		// Close the project
		project.close(createTestMonitor());

		// Open the project, build it, and delete it
		project.open(createTestMonitor());
		monitor = new FussyProgressMonitor();
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, monitor);
		monitor.assertUsedUp();
		project.delete(false, createTestMonitor());
		verifier.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
		verifier.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
		verifier.addExpectedLifecycleEvent(TestBuilder.DEFAULT_BUILD_ID);
		verifier.assertLifecycleEvents();
	}

	/**
	 * Tests the lifecycle of a builder.
	 *
	 * @see SortBuilder
	 */
	public void testMoveProject() throws CoreException {
		// Create some resource handles
		IWorkspace workspace = getWorkspace();
		IProject proj1 = workspace.getRoot().getProject("PROJECT" + 1);
		IProject proj2 = workspace.getRoot().getProject("Destination");

		// Turn auto-building off
		setAutoBuilding(false);
		// Create some resources
		proj1.create(createTestMonitor());
		proj1.open(createTestMonitor());

		// Create and set a build specs for project one
		IProjectDescription desc = proj1.getDescription();
		ICommand command = desc.newCommand();
		command.setBuilderName(SortBuilder.BUILDER_NAME);
		command.getArguments().put(TestBuilder.BUILD_ID, "Build0");
		desc.setBuildSpec(new ICommand[] { command });
		proj1.setDescription(desc, createTestMonitor());

		// build project1
		proj1.build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());

		// move proj1 to proj2
		proj1.move(proj2.getFullPath(), false, createTestMonitor());

		// build proj2
		proj2.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
	}

	/**
	 * Tests that turning autobuild on will invoke a build in the next
	 * operation.
	 */
	public void testTurnOnAutobuild() throws CoreException {
		// Create some resource handles
		IProject project = getWorkspace().getRoot().getProject("PROJECT");
		final IFile file = project.getFile("File.txt");

		// Turn auto-building off
		setAutoBuilding(false);
		// Create and open a project
		project.create(createTestMonitor());
		project.open(createTestMonitor());
		file.create(getRandomContents(), IResource.NONE, createTestMonitor());

		// Create and set a build spec for the project
		IProjectDescription desc = project.getDescription();
		ICommand command = desc.newCommand();
		command.setBuilderName(SortBuilder.BUILDER_NAME);
		desc.setBuildSpec(new ICommand[] {command});
		project.setDescription(desc, createTestMonitor());


		//try to do an incremental build when there has never
		//been a batch build
		getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		// Set up a plug-in lifecycle verifier for testing purposes
		TestBuilder verifier = SortBuilder.getInstance();
		verifier.addExpectedLifecycleEvent(TestBuilder.SET_INITIALIZATION_DATA);
		verifier.addExpectedLifecycleEvent(TestBuilder.STARTUP_ON_INITIALIZE);
		verifier.addExpectedLifecycleEvent(TestBuilder.DEFAULT_BUILD_ID);
		verifier.assertLifecycleEvents();

		// Now make a change and then turn autobuild on. Turning it on should
		// cause a build.
		IWorkspaceRunnable r = monitor -> {
			file.setContents(getRandomContents(), IResource.NONE, createTestMonitor());
			IWorkspaceDescription description = getWorkspace().getDescription();
			description.setAutoBuilding(true);
			getWorkspace().setDescription(description);
		};
		waitForBuild();
		getWorkspace().run(r, createTestMonitor());
		waitForBuild();
		verifier.addExpectedLifecycleEvent(TestBuilder.DEFAULT_BUILD_ID);
		verifier.assertLifecycleEvents();
	}
}
