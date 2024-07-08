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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.setAutoBuilding;
import static org.eclipse.core.tests.resources.ResourceTestUtil.updateProjectDescription;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * This class tests builder behavior related to re-building
 */
@ExtendWith(WorkspaceResetExtension.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
public class RebuildTest {
	final String builderName = RebuildingBuilder.BUILDER_NAME;
	private int maxBuildIterations;
	private String testName;

	@BeforeEach
	public void setUp(TestInfo testInfo) throws CoreException {
		maxBuildIterations = getWorkspace().getDescription().getMaxBuildIterations();
		testName = testInfo.getTestMethod().get().getName();
		// Turn auto-building off
		setAutoBuilding(false);
		boolean earlyExitAllowed = ((Workspace) getWorkspace()).getBuildManager()
				.isEarlyExitFromBuildLoopAllowed();
		assertFalse(earlyExitAllowed, "early exit shouldn't be set");
	}

	@AfterEach
	public void tearDown() throws CoreException {
		IWorkspaceDescription description = getWorkspace().getDescription();
		description.setMaxBuildIterations(maxBuildIterations);
		getWorkspace().setDescription(description);
		RebuildingBuilder.getInstances().clear();
		allowEarlyBuildLoopExit(false);
	}

	/**
	 * Tests IncrementProjectBuilder.requestProjectRebuild with a single project
	 */
	@Test
	public void testSingleProjectPropagationAndNoOtherBuilders() throws Exception {
		// Create some resource handles
		IProject project = getWorkspace().getRoot().getProject(testName);

		// Create and open a project
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		// Create and set a build spec for the project
		updateProjectDescription(project).addingCommand(builderName).withTestBuilderId("builder1") //
				.andCommand(builderName).withTestBuilderId("builder2") //
				.andCommand(builderName).withTestBuilderId("builder3").apply();

		// do an initial build to create builders
		project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		List<RebuildingBuilder> builders = RebuildingBuilder.getInstances();
		assertThat(project.getDescription().getBuildSpec()).hasSameSizeAs(builders);
		RebuildingBuilder b1 = builders.get(0);
		RebuildingBuilder b2 = builders.get(1);
		RebuildingBuilder b3 = builders.get(2);
		builders.forEach(RebuildingBuilder::reset);

		// Confirm basic functionality first - one build per builder
		project.build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(1, b1.buildsCount());
		assertEquals(1, b2.buildsCount());
		assertEquals(1, b3.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// First builder requests rebuild - it will cause main build to loop two times
		// and project loop will run as usually (no repetitions)
		b1.setRequestProjectRebuild(project, 1);
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(2, b1.buildsCount());
		assertEquals(2, b2.buildsCount());
		assertEquals(2, b3.buildsCount());

		List<Integer> expectedKinds = List.of(IncrementalProjectBuilder.FULL_BUILD,
				IncrementalProjectBuilder.INCREMENTAL_BUILD);
		assertEquals(expectedKinds, b1.buildKinds());
		assertEquals(expectedKinds, b2.buildKinds());
		assertEquals(expectedKinds, b3.buildKinds());

		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// Second builder requests rebuild - it will cause main build to loop two times
		// and project loop will restart from the first one, so first and second
		// builder will run 3 times
		b2.setRequestProjectRebuild(project, 1);
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(3, b1.buildsCount());
		assertEquals(3, b2.buildsCount());
		assertEquals(2, b3.buildsCount());

		expectedKinds = List.of(IncrementalProjectBuilder.FULL_BUILD, IncrementalProjectBuilder.INCREMENTAL_BUILD,
				IncrementalProjectBuilder.INCREMENTAL_BUILD);
		assertEquals(expectedKinds, b1.buildKinds());
		assertEquals(expectedKinds, b2.buildKinds());

		expectedKinds = List.of(IncrementalProjectBuilder.FULL_BUILD, IncrementalProjectBuilder.INCREMENTAL_BUILD);
		assertEquals(expectedKinds, b3.buildKinds());

		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// Third builder requests rebuild - it will cause main build to loop two times
		// and project loop will restart from the first one, so all builders will run 3
		// times
		b3.setRequestProjectRebuild(project, 1);
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(3, b1.buildsCount());
		assertEquals(3, b2.buildsCount());
		assertEquals(3, b3.buildsCount());
		expectedKinds = List.of(IncrementalProjectBuilder.FULL_BUILD, IncrementalProjectBuilder.INCREMENTAL_BUILD,
				IncrementalProjectBuilder.INCREMENTAL_BUILD);
		assertEquals(expectedKinds, b1.buildKinds());
		assertEquals(expectedKinds, b2.buildKinds());
		assertEquals(expectedKinds, b3.buildKinds());

		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// Second and third builder request rebuild - it will cause main build to loop
		// two times
		// and project loop will restart from the first one
		// first and second builder 4 times, third 3 times
		b2.setRequestProjectRebuild(project, 1);
		b3.setRequestProjectRebuild(project, 1);
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(4, b1.buildsCount());
		assertEquals(4, b2.buildsCount());
		assertEquals(3, b3.buildsCount());
		expectedKinds = List.of(IncrementalProjectBuilder.FULL_BUILD, IncrementalProjectBuilder.INCREMENTAL_BUILD,
				IncrementalProjectBuilder.INCREMENTAL_BUILD, IncrementalProjectBuilder.INCREMENTAL_BUILD);
		assertEquals(expectedKinds, b1.buildKinds());
		assertEquals(expectedKinds, b2.buildKinds());
		expectedKinds = List.of(IncrementalProjectBuilder.FULL_BUILD, IncrementalProjectBuilder.INCREMENTAL_BUILD,
				IncrementalProjectBuilder.INCREMENTAL_BUILD);
		assertEquals(expectedKinds, b3.buildKinds());

		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		final int max = 5;
		IWorkspaceDescription description = getWorkspace().getDescription();
		description.setMaxBuildIterations(max);
		getWorkspace().setDescription(description);

		// Third builder requests N rebuilds - it will cause main build to loop
		// not more than (max * max) times (both inner / outer loop use limit)
		int rebuilds = 42;
		int expectedRebuildsFirstBuilders = max * max;
		int expectedRebuildslastBuilder = max;
		b2.setRequestProjectRebuild(project, rebuilds);
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(expectedRebuildsFirstBuilders, b1.buildsCount());
		assertEquals(expectedRebuildsFirstBuilders, b2.buildsCount());
		assertEquals(expectedRebuildslastBuilder, b3.buildsCount());
	}

	/**
	 * Tests IncrementProjectBuilder.requestProjectRebuild with a single project
	 */
	@Test
	public void testSingleProjectPropagationAndOtherBuilders() throws Exception {
		// Create some resource handles
		IProject project = getWorkspace().getRoot().getProject(testName);

		// Create and open a project
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		// Create and set a build spec for the project
		updateProjectDescription(project).addingCommand(builderName).withTestBuilderId("builder1")
				.andCommand(builderName).withTestBuilderId("builder2")
				.andCommand(builderName).withTestBuilderId("builder3").apply();

		// do an initial build to create builders
		project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		List<RebuildingBuilder> builders = RebuildingBuilder.getInstances();
		assertThat(project.getDescription().getBuildSpec()).hasSameSizeAs(builders);
		RebuildingBuilder b1 = builders.get(0);
		RebuildingBuilder b2 = builders.get(1);
		RebuildingBuilder b3 = builders.get(2);
		builders.forEach(RebuildingBuilder::reset);

		// Confirm basic functionality first - one build per builder
		project.build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(1, b1.buildsCount());
		assertEquals(1, b2.buildsCount());
		assertEquals(1, b3.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(true));

		// First builder requests rebuild - it will cause main build to loop two times
		// and project loop will run once more
		b1.setRequestProjectRebuild(project, 1);
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(3, b1.buildsCount());
		assertEquals(3, b2.buildsCount());
		assertEquals(3, b3.buildsCount());
		List<Integer> expectedKinds = List.of(IncrementalProjectBuilder.FULL_BUILD,
				IncrementalProjectBuilder.INCREMENTAL_BUILD, IncrementalProjectBuilder.INCREMENTAL_BUILD);
		assertEquals(expectedKinds, b1.buildKinds());
		assertEquals(expectedKinds, b2.buildKinds());
		assertEquals(expectedKinds, b3.buildKinds());

		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(true));

		// Second builder requests rebuild - it will cause main build to loop two times
		// and project loop will run once more
		b2.setRequestProjectRebuild(project, 1);
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(3, b1.buildsCount());
		assertEquals(3, b2.buildsCount());
		assertEquals(3, b3.buildsCount());
		expectedKinds = List.of(IncrementalProjectBuilder.FULL_BUILD, IncrementalProjectBuilder.INCREMENTAL_BUILD,
				IncrementalProjectBuilder.INCREMENTAL_BUILD);
		assertEquals(expectedKinds, b1.buildKinds());
		assertEquals(expectedKinds, b2.buildKinds());
		assertEquals(expectedKinds, b3.buildKinds());

		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(true));

		// Third builder requests rebuild - it will cause main build to loop two times
		// and project loop will restart from the first one, so all builders will run 3
		// times
		b3.setRequestProjectRebuild(project, 1);
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(3, b1.buildsCount());
		assertEquals(3, b2.buildsCount());
		assertEquals(3, b3.buildsCount());
		expectedKinds = List.of(IncrementalProjectBuilder.FULL_BUILD, IncrementalProjectBuilder.INCREMENTAL_BUILD,
				IncrementalProjectBuilder.INCREMENTAL_BUILD);
		assertEquals(expectedKinds, b1.buildKinds());
		assertEquals(expectedKinds, b2.buildKinds());
		assertEquals(expectedKinds, b3.buildKinds());

		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(true));

		// Second and third builder request rebuild - it will cause main build to loop
		// two times and project loop will restart once more
		b2.setRequestProjectRebuild(project, 1);
		b3.setRequestProjectRebuild(project, 1);
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(3, b1.buildsCount());
		assertEquals(3, b2.buildsCount());
		assertEquals(3, b3.buildsCount());
		expectedKinds = List.of(IncrementalProjectBuilder.FULL_BUILD, IncrementalProjectBuilder.INCREMENTAL_BUILD,
				IncrementalProjectBuilder.INCREMENTAL_BUILD);
		assertEquals(expectedKinds, b1.buildKinds());
		assertEquals(expectedKinds, b2.buildKinds());
		assertEquals(expectedKinds, b3.buildKinds());

		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(true));

		final int max = 5;
		IWorkspaceDescription description = getWorkspace().getDescription();
		description.setMaxBuildIterations(max);
		getWorkspace().setDescription(description);

		// Third builder requests N rebuilds - it will cause main build to loop
		// not more than (max * max) times (both inner / outer loop use limit)
		int rebuilds = 42;
		int expectedRebuildsFirstBuilders = max * max;
		int expectedRebuildslastBuilder = max * max;
		b2.setRequestProjectRebuild(project, rebuilds);
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(expectedRebuildsFirstBuilders, b1.buildsCount());
		assertEquals(expectedRebuildsFirstBuilders, b2.buildsCount());
		assertEquals(expectedRebuildslastBuilder, b3.buildsCount());
	}

	/**
	 * Tests IncrementProjectBuilder.requestProjectRebuild with a single project
	 */
	@Test
	public void testSingleProjectNoPropagationAndProcessOtherBuilder() throws Exception {
		// Create some resource handles
		IProject project = getWorkspace().getRoot().getProject(testName);

		// Create and open a project
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		// Create and set a build spec for the project
		updateProjectDescription(project).addingCommand(builderName).withTestBuilderId("builder1") //
				.andCommand(builderName).withTestBuilderId("builder2") //
				.andCommand(builderName).withTestBuilderId("builder3").apply();

		// do an initial build to create builders
		project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		List<RebuildingBuilder> builders = RebuildingBuilder.getInstances();
		assertThat(project.getDescription().getBuildSpec()).hasSameSizeAs(builders);
		RebuildingBuilder b1 = builders.get(0);
		RebuildingBuilder b2 = builders.get(1);
		RebuildingBuilder b3 = builders.get(2);
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(false));
		builders.forEach(b -> b.setProcessOtherBuilders(true));

		// Confirm basic functionality first - one build per builder
		project.build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(1, b1.buildsCount());
		assertEquals(1, b2.buildsCount());
		assertEquals(1, b3.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(false));
		builders.forEach(b -> b.setProcessOtherBuilders(true));

		// First builder requests rebuild - all builders will run twice
		b1.setRequestProjectRebuild(project, 1);
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(2, b1.buildsCount());
		assertEquals(2, b2.buildsCount());
		assertEquals(2, b3.buildsCount());
		List<Integer> expectedKinds = List.of(IncrementalProjectBuilder.FULL_BUILD, IncrementalProjectBuilder.INCREMENTAL_BUILD);
		assertEquals(expectedKinds, b1.buildKinds());
		assertEquals(expectedKinds, b2.buildKinds());
		assertEquals(expectedKinds, b3.buildKinds());

		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(false));
		builders.forEach(b -> b.setProcessOtherBuilders(true));

		// Second builder requests rebuild - all builders will run twice
		b2.setRequestProjectRebuild(project, 1);
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(2, b1.buildsCount());
		assertEquals(2, b2.buildsCount());
		assertEquals(2, b3.buildsCount());
		expectedKinds = List.of(IncrementalProjectBuilder.FULL_BUILD, IncrementalProjectBuilder.INCREMENTAL_BUILD);
		assertEquals(expectedKinds, b1.buildKinds());
		assertEquals(expectedKinds, b2.buildKinds());
		assertEquals(expectedKinds, b3.buildKinds());

		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(false));
		builders.forEach(b -> b.setProcessOtherBuilders(true));

		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(false));
		builders.forEach(b -> b.setProcessOtherBuilders(true));

		// Third builder requests rebuild - all builders will run twice
		b3.setRequestProjectRebuild(project, 1);
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(2, b1.buildsCount());
		assertEquals(2, b2.buildsCount());
		assertEquals(2, b3.buildsCount());
		expectedKinds = List.of(IncrementalProjectBuilder.FULL_BUILD, IncrementalProjectBuilder.INCREMENTAL_BUILD);
		assertEquals(expectedKinds, b1.buildKinds());
		assertEquals(expectedKinds, b2.buildKinds());
		assertEquals(expectedKinds, b3.buildKinds());

		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(false));
		builders.forEach(b -> b.setProcessOtherBuilders(true));

		// Second and third builder request rebuild - all builders will run twice
		b2.setRequestProjectRebuild(project, 1);
		b3.setRequestProjectRebuild(project, 1);
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(2, b1.buildsCount());
		assertEquals(2, b2.buildsCount());
		assertEquals(2, b3.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(false));
		builders.forEach(b -> b.setProcessOtherBuilders(true));

		final int max = 5;
		IWorkspaceDescription description = getWorkspace().getDescription();
		description.setMaxBuildIterations(max);
		getWorkspace().setDescription(description);

		// Second builder requests N rebuilds - it will cause main build to loop
		// not more than (max * max) times (both inner / outer loop use limit)
		int rebuilds = 42;
		int expectedRebuildsFirstBuilders = max;
		int expectedRebuildslastBuilder = max;
		b2.setRequestProjectRebuild(project, rebuilds);
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(expectedRebuildsFirstBuilders, b1.buildsCount());
		assertEquals(expectedRebuildsFirstBuilders, b2.buildsCount());
		assertEquals(expectedRebuildslastBuilder, b3.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(false));
		builders.forEach(b -> b.setProcessOtherBuilders(true));

		// Third builder requests N rebuilds - it will cause main build to loop
		// not more than (max * max) times (both inner / outer loop use limit)
		expectedRebuildslastBuilder = max;
		b3.setRequestProjectRebuild(project, rebuilds);
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(expectedRebuildsFirstBuilders, b1.buildsCount());
		assertEquals(expectedRebuildsFirstBuilders, b2.buildsCount());
		assertEquals(expectedRebuildslastBuilder, b3.buildsCount());
	}

	/**
	 * Tests IncrementProjectBuilder.requestProjectRebuild with a single project
	 */
	@Test
	public void testSingleProjectNoPropagationNoOtherBuilders() throws Exception {
		// Create some resource handles
		IProject project = getWorkspace().getRoot().getProject(testName);

		// Create and open a project
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		// Create and set a build spec for the project
		updateProjectDescription(project).addingCommand(builderName).withTestBuilderId("builder1") //
				.andCommand(builderName).withTestBuilderId("builder2") //
				.andCommand(builderName).withTestBuilderId("builder3").apply();

		// do an initial build to create builders
		project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		List<RebuildingBuilder> builders = RebuildingBuilder.getInstances();
		assertThat(project.getDescription().getBuildSpec()).hasSameSizeAs(builders);
		RebuildingBuilder b1 = builders.get(0);
		RebuildingBuilder b2 = builders.get(1);
		RebuildingBuilder b3 = builders.get(2);
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(false));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// Confirm basic functionality first - one build per builder
		project.build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(1, b1.buildsCount());
		assertEquals(1, b2.buildsCount());
		assertEquals(1, b3.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(false));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// First builder requests rebuild - no repetitions
		b1.setRequestProjectRebuild(project, 1);
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(1, b1.buildsCount());
		assertEquals(1, b2.buildsCount());
		assertEquals(1, b3.buildsCount());
		List<Integer> expectedKinds = List.of(IncrementalProjectBuilder.FULL_BUILD);
		assertEquals(expectedKinds, b1.buildKinds());
		assertEquals(expectedKinds, b2.buildKinds());
		assertEquals(expectedKinds, b3.buildKinds());

		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(false));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// Second builder requests rebuild - first & second builder will run twice
		b2.setRequestProjectRebuild(project, 1);
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(2, b1.buildsCount());
		assertEquals(2, b2.buildsCount());
		assertEquals(1, b3.buildsCount());
		expectedKinds = List.of(IncrementalProjectBuilder.FULL_BUILD, IncrementalProjectBuilder.INCREMENTAL_BUILD);
		assertEquals(expectedKinds, b1.buildKinds());
		assertEquals(expectedKinds, b2.buildKinds());
		expectedKinds = List.of(IncrementalProjectBuilder.FULL_BUILD);
		assertEquals(expectedKinds, b3.buildKinds());

		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(false));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// Third builder requests rebuild - all builders will run three twice
		b3.setRequestProjectRebuild(project, 1);
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(2, b1.buildsCount());
		assertEquals(2, b2.buildsCount());
		assertEquals(2, b3.buildsCount());
		expectedKinds = List.of(IncrementalProjectBuilder.FULL_BUILD, IncrementalProjectBuilder.INCREMENTAL_BUILD);
		assertEquals(expectedKinds, b1.buildKinds());
		assertEquals(expectedKinds, b2.buildKinds());
		assertEquals(expectedKinds, b3.buildKinds());

		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(false));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// Second and third builder request rebuild - 1 & 2 builders will run three
		// times, last one one time less
		b2.setRequestProjectRebuild(project, 1);
		b3.setRequestProjectRebuild(project, 1);
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(3, b1.buildsCount());
		assertEquals(3, b2.buildsCount());
		assertEquals(2, b3.buildsCount());

		expectedKinds = List.of(IncrementalProjectBuilder.FULL_BUILD, IncrementalProjectBuilder.INCREMENTAL_BUILD,
				IncrementalProjectBuilder.INCREMENTAL_BUILD);
		assertEquals(expectedKinds, b1.buildKinds());
		assertEquals(expectedKinds, b2.buildKinds());

		expectedKinds = List.of(IncrementalProjectBuilder.FULL_BUILD, IncrementalProjectBuilder.INCREMENTAL_BUILD);
		assertEquals(expectedKinds, b3.buildKinds());

		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(false));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		final int max = 5;
		IWorkspaceDescription description = getWorkspace().getDescription();
		description.setMaxBuildIterations(max);
		getWorkspace().setDescription(description);

		// Second builder requests N rebuilds - it will cause main build to loop
		// not more than (max * max) times (both inner / outer loop use limit)
		int rebuilds = 42;
		int expectedRebuildsFirstBuilders = max;
		int expectedRebuildslastBuilder = 1;
		b2.setRequestProjectRebuild(project, rebuilds);
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(expectedRebuildsFirstBuilders, b1.buildsCount());
		assertEquals(expectedRebuildsFirstBuilders, b2.buildsCount());
		assertEquals(expectedRebuildslastBuilder, b3.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(false));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// Third builder requests N rebuilds - it will cause main build to loop
		// not more than (max * max) times (both inner / outer loop use limit)
		expectedRebuildslastBuilder = max;
		b3.setRequestProjectRebuild(project, rebuilds);
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(expectedRebuildsFirstBuilders, b1.buildsCount());
		assertEquals(expectedRebuildsFirstBuilders, b2.buildsCount());
		assertEquals(expectedRebuildslastBuilder, b3.buildsCount());
	}

	/**
	 * Tests IncrementProjectBuilder.requestProjectRebuild with multiple projects
	 */
	@Test
	public void testMultipleProjectsPropagationAndNoOtherBuilders() throws Exception {
		// Create some resource handles
		IProject project1 = getWorkspace().getRoot().getProject(testName + 1);
		IProject project2 = getWorkspace().getRoot().getProject(testName + 2);

		// Create and open a project
		project1.create(createTestMonitor());
		project1.open(createTestMonitor());
		project2.create(createTestMonitor());
		project2.open(createTestMonitor());

		// Create and set a build spec for the project
		updateProjectDescription(project1).addingCommand(builderName).withTestBuilderId("builder1") //
				.andCommand(builderName).withTestBuilderId("builder2") //
				.andCommand(builderName).withTestBuilderId("builder3").apply();

		// do an initial build to create builders
		project1.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		List<RebuildingBuilder> builders = RebuildingBuilder.getInstances();
		assertThat(project1.getDescription().getBuildSpec()).hasSameSizeAs(builders);
		RebuildingBuilder b1 = builders.get(0);
		RebuildingBuilder b2 = builders.get(1);
		RebuildingBuilder b3 = builders.get(2);
		builders.forEach(RebuildingBuilder::reset);

		// Create and set a build spec for the project
		updateProjectDescription(project2).addingCommand(builderName).withTestBuilderId("builder1") //
				.andCommand(builderName).withTestBuilderId("builder2") //
				.andCommand(builderName).withTestBuilderId("builder3").apply();

		project2.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		assertEquals(6, builders.size());
		RebuildingBuilder b4 = builders.get(3);
		RebuildingBuilder b5 = builders.get(4);
		RebuildingBuilder b6 = builders.get(5);
		builders.forEach(RebuildingBuilder::reset);

		// Confirm basic functionality first - one build per builder
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(1, b1.buildsCount());
		assertEquals(1, b2.buildsCount());
		assertEquals(1, b3.buildsCount());

		assertEquals(1, b4.buildsCount());
		assertEquals(1, b5.buildsCount());
		assertEquals(1, b6.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// First builder requests rebuild - it will cause main build to
		// loop two times for both projects
		b1.setRequestProjectRebuild(project1, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(2, b1.buildsCount());
		assertEquals(2, b2.buildsCount());
		assertEquals(2, b3.buildsCount());

		assertEquals(2, b4.buildsCount());
		assertEquals(2, b5.buildsCount());
		assertEquals(2, b6.buildsCount());

		List<Integer> expectedKinds = List.of(IncrementalProjectBuilder.FULL_BUILD,
				IncrementalProjectBuilder.INCREMENTAL_BUILD);
		assertEquals(expectedKinds, b1.buildKinds());
		assertEquals(expectedKinds, b2.buildKinds());
		assertEquals(expectedKinds, b3.buildKinds());

		assertEquals(expectedKinds, b4.buildKinds());
		assertEquals(expectedKinds, b5.buildKinds());
		assertEquals(expectedKinds, b6.buildKinds());

		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// All builders in one project request rebuild - it will cause main build
		// to loop only one extra time
		b1.setRequestProjectRebuild(project1, 1);
		b2.setRequestProjectRebuild(project1, 1);
		b3.setRequestProjectRebuild(project1, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(4, b1.buildsCount());
		assertEquals(4, b2.buildsCount());
		assertEquals(3, b3.buildsCount());

		assertEquals(2, b4.buildsCount());
		assertEquals(2, b5.buildsCount());
		assertEquals(2, b6.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// One builder in each project requests rebuild - it will cause main build
		// to loop only one extra time
		b1.setRequestProjectRebuild(project1, 1);
		b4.setRequestProjectRebuild(project2, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(2, b1.buildsCount());
		assertEquals(2, b2.buildsCount());
		assertEquals(2, b3.buildsCount());

		assertEquals(2, b4.buildsCount());
		assertEquals(2, b5.buildsCount());
		assertEquals(2, b6.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// All builders in all projects request rebuild - it will cause main build
		// to loop only one extra time
		b1.setRequestProjectRebuild(project1, 1);
		b2.setRequestProjectRebuild(project1, 1);
		b3.setRequestProjectRebuild(project1, 1);
		b4.setRequestProjectRebuild(project2, 1);
		b5.setRequestProjectRebuild(project2, 1);
		b6.setRequestProjectRebuild(project2, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(4, b1.buildsCount());
		assertEquals(4, b2.buildsCount());
		assertEquals(3, b3.buildsCount());

		assertEquals(4, b4.buildsCount());
		assertEquals(4, b5.buildsCount());
		assertEquals(3, b6.buildsCount());
	}

	/**
	 * Tests IncrementProjectBuilder.requestProjectRebuild with multiple projects
	 */
	@Test
	public void testMultipleProjectsPropagationAndProcessOtherBuilders() throws Exception {
		// Create some resource handles
		IProject project1 = getWorkspace().getRoot().getProject(testName + 1);
		IProject project2 = getWorkspace().getRoot().getProject(testName + 2);

		// Create and open a project
		project1.create(createTestMonitor());
		project1.open(createTestMonitor());
		project2.create(createTestMonitor());
		project2.open(createTestMonitor());

		// Create and set a build spec for the project
		updateProjectDescription(project1).addingCommand(builderName).withTestBuilderId("builder1") //
				.andCommand(builderName).withTestBuilderId("builder2") //
				.andCommand(builderName).withTestBuilderId("builder3").apply();

		// do an initial build to create builders
		project1.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		List<RebuildingBuilder> builders = RebuildingBuilder.getInstances();
		assertThat(project1.getDescription().getBuildSpec()).hasSameSizeAs(builders);
		RebuildingBuilder b1 = builders.get(0);
		RebuildingBuilder b2 = builders.get(1);
		RebuildingBuilder b3 = builders.get(2);
		builders.forEach(RebuildingBuilder::reset);

		// Create and set a build spec for the project
		updateProjectDescription(project2).addingCommand(builderName).withTestBuilderId("builder1") //
				.andCommand(builderName).withTestBuilderId("builder2") //
				.andCommand(builderName).withTestBuilderId("builder3").apply();

		project2.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		assertEquals(6, builders.size());
		RebuildingBuilder b4 = builders.get(3);
		RebuildingBuilder b5 = builders.get(4);
		RebuildingBuilder b6 = builders.get(5);
		builders.forEach(RebuildingBuilder::reset);

		// Confirm basic functionality first - one build per builder
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(1, b1.buildsCount());
		assertEquals(1, b2.buildsCount());
		assertEquals(1, b3.buildsCount());

		assertEquals(1, b4.buildsCount());
		assertEquals(1, b5.buildsCount());
		assertEquals(1, b6.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(true));

		// First builder requests rebuild - it will cause main build to
		// loop two times for both projects and one extra time for current one
		b1.setRequestProjectRebuild(project1, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(3, b1.buildsCount());
		assertEquals(3, b2.buildsCount());
		assertEquals(3, b3.buildsCount());

		assertEquals(2, b4.buildsCount());
		assertEquals(2, b5.buildsCount());
		assertEquals(2, b6.buildsCount());

		List<Integer> expectedKinds = List.of(IncrementalProjectBuilder.FULL_BUILD,
				IncrementalProjectBuilder.INCREMENTAL_BUILD, IncrementalProjectBuilder.INCREMENTAL_BUILD);
		assertEquals(expectedKinds, b1.buildKinds());
		assertEquals(expectedKinds, b2.buildKinds());
		assertEquals(expectedKinds, b3.buildKinds());

		expectedKinds = List.of(IncrementalProjectBuilder.FULL_BUILD, IncrementalProjectBuilder.INCREMENTAL_BUILD);
		assertEquals(expectedKinds, b4.buildKinds());
		assertEquals(expectedKinds, b5.buildKinds());
		assertEquals(expectedKinds, b6.buildKinds());

		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(true));

		// All builders in one project request rebuild - it will cause main build to
		// loop two times for both projects and one extra time for current one
		// to loop only one extra time
		b1.setRequestProjectRebuild(project1, 1);
		b2.setRequestProjectRebuild(project1, 1);
		b3.setRequestProjectRebuild(project1, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(3, b1.buildsCount());
		assertEquals(3, b2.buildsCount());
		assertEquals(3, b3.buildsCount());

		assertEquals(2, b4.buildsCount());
		assertEquals(2, b5.buildsCount());
		assertEquals(2, b6.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(true));

		// One builder in each project requests rebuild - it will cause main build
		// to loop only one extra time and every project twice
		b1.setRequestProjectRebuild(project1, 1);
		b4.setRequestProjectRebuild(project2, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(3, b1.buildsCount());
		assertEquals(3, b2.buildsCount());
		assertEquals(3, b3.buildsCount());

		assertEquals(3, b4.buildsCount());
		assertEquals(3, b5.buildsCount());
		assertEquals(3, b6.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(true));

		// All builders in all projects request rebuild - it will cause main build
		// to loop only one extra time and every project twice
		b1.setRequestProjectRebuild(project1, 1);
		b2.setRequestProjectRebuild(project1, 1);
		b3.setRequestProjectRebuild(project1, 1);
		b4.setRequestProjectRebuild(project2, 1);
		b5.setRequestProjectRebuild(project2, 1);
		b6.setRequestProjectRebuild(project2, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(3, b1.buildsCount());
		assertEquals(3, b2.buildsCount());
		assertEquals(3, b3.buildsCount());

		assertEquals(3, b4.buildsCount());
		assertEquals(3, b5.buildsCount());
		assertEquals(3, b6.buildsCount());
	}

	/**
	 * Tests IncrementProjectBuilder.requestProjectRebuild with multiple projects
	 * and early exit enabled
	 */
	@Test
	public void testMultipleProjectsPropagationAndNoOtherBuildersWithEarlyExit() throws Exception {
		// turn rebuild request propagation to other projects in same loop off
		allowEarlyBuildLoopExit(true);

		// Create some resource handles
		IProject project1 = getWorkspace().getRoot().getProject(testName + 1);
		IProject project2 = getWorkspace().getRoot().getProject(testName + 2);

		// Create and open a project
		project1.create(createTestMonitor());
		project1.open(createTestMonitor());
		project2.create(createTestMonitor());
		project2.open(createTestMonitor());

		// Create and set a build spec for the project
		updateProjectDescription(project1).addingCommand(builderName).withTestBuilderId("builder1") //
				.andCommand(builderName).withTestBuilderId("builder2") //
				.andCommand(builderName).withTestBuilderId("builder3").apply();

		// do an initial build to create builders
		project1.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		List<RebuildingBuilder> builders = RebuildingBuilder.getInstances();
		assertThat(project1.getDescription().getBuildSpec()).hasSameSizeAs(builders);
		RebuildingBuilder b1 = builders.get(0);
		RebuildingBuilder b2 = builders.get(1);
		RebuildingBuilder b3 = builders.get(2);
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// Create and set a build spec for the project
		updateProjectDescription(project2).addingCommand(builderName).withTestBuilderId("builder1") //
				.andCommand(builderName).withTestBuilderId("builder2") //
				.andCommand(builderName).withTestBuilderId("builder3").apply();

		project2.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		assertEquals(6, builders.size());
		RebuildingBuilder b4 = builders.get(3);
		RebuildingBuilder b5 = builders.get(4);
		RebuildingBuilder b6 = builders.get(5);
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// Confirm basic functionality first - one build per builder
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(1, b1.buildsCount());
		assertEquals(1, b2.buildsCount());
		assertEquals(1, b3.buildsCount());

		assertEquals(1, b4.buildsCount());
		assertEquals(1, b5.buildsCount());
		assertEquals(1, b6.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// Second and third builder request rebuild - it will cause main build to loop
		// one extra time for the first project but the second project will be built
		// only once (on second loop cycle)
		b2.setRequestProjectRebuild(project1, 1);
		b3.setRequestProjectRebuild(project1, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(4, b1.buildsCount());
		assertEquals(4, b2.buildsCount());
		assertEquals(3, b3.buildsCount());

		assertEquals(1, b4.buildsCount());
		assertEquals(1, b5.buildsCount());
		assertEquals(1, b6.buildsCount());

		List<Integer> expectedKinds = List.of(IncrementalProjectBuilder.FULL_BUILD,
				IncrementalProjectBuilder.INCREMENTAL_BUILD, IncrementalProjectBuilder.INCREMENTAL_BUILD,
				IncrementalProjectBuilder.INCREMENTAL_BUILD);
		assertEquals(expectedKinds, b1.buildKinds());
		assertEquals(expectedKinds, b2.buildKinds());

		expectedKinds = List.of(IncrementalProjectBuilder.FULL_BUILD, IncrementalProjectBuilder.INCREMENTAL_BUILD,
				IncrementalProjectBuilder.INCREMENTAL_BUILD);
		assertEquals(expectedKinds, b3.buildKinds());

		// TODO: this is unexpected. If the allowEarlyBuildLoopExit is set, we
		// start rebuild and automatically change trigger to INCREMENTAL build
		// even if the not yet built projects haven't done full build yet
		expectedKinds = List.of(IncrementalProjectBuilder.INCREMENTAL_BUILD);
		assertEquals(expectedKinds, b4.buildKinds());
		assertEquals(expectedKinds, b5.buildKinds());
		assertEquals(expectedKinds, b6.buildKinds());

		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// One builder in each project requests rebuild - it will cause main build
		// to loop two times more
		b1.setRequestProjectRebuild(project1, 1);
		b4.setRequestProjectRebuild(project2, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(3, b1.buildsCount());
		assertEquals(3, b2.buildsCount());
		assertEquals(3, b3.buildsCount());

		assertEquals(2, b4.buildsCount());
		assertEquals(2, b5.buildsCount());
		assertEquals(2, b6.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// All builders in all projects request rebuild - it will cause main build
		// to loop three times (only two extra builds for the second project)
		b1.setRequestProjectRebuild(project1, 1);
		b2.setRequestProjectRebuild(project1, 1);
		b3.setRequestProjectRebuild(project1, 1);
		b4.setRequestProjectRebuild(project2, 1);
		b5.setRequestProjectRebuild(project2, 1);
		b6.setRequestProjectRebuild(project2, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(5, b1.buildsCount());
		assertEquals(5, b2.buildsCount());
		assertEquals(4, b3.buildsCount());

		assertEquals(4, b4.buildsCount());
		assertEquals(4, b5.buildsCount());
		assertEquals(3, b6.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		final int max = 5;
		IWorkspaceDescription description = getWorkspace().getDescription();
		description.setMaxBuildIterations(max);
		getWorkspace().setDescription(description);

		// Third builder requests N rebuilds - it will cause main build to loop
		// not more than (max * max) times (both inner / outer loop use limit)
		int rebuilds = 42;
		int expectedRebuildsFirstProject = max * max;
		int expectedBuildCycles = max;
		b2.setRequestProjectRebuild(project1, rebuilds);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(expectedRebuildsFirstProject, b1.buildsCount());
		assertEquals(expectedRebuildsFirstProject, b2.buildsCount());
		assertEquals(expectedBuildCycles, b3.buildsCount());

		assertEquals(1, b4.buildsCount());
		assertEquals(1, b5.buildsCount());
		assertEquals(1, b6.buildsCount());
	}


	/**
	 * Tests IncrementProjectBuilder.requestProjectRebuild with multiple projects
	 */
	@Test
	public void testMultipleProjectsNoPropagationNoOtherBuilders() throws Exception {
		// Create some resource handles
		IProject project1 = getWorkspace().getRoot().getProject(testName + 1);
		IProject project2 = getWorkspace().getRoot().getProject(testName + 2);

		// Create and open a project
		project1.create(createTestMonitor());
		project1.open(createTestMonitor());
		project2.create(createTestMonitor());
		project2.open(createTestMonitor());

		// Create and set a build spec for the project
		updateProjectDescription(project1).addingCommand(builderName).withTestBuilderId("builder1") //
				.andCommand(builderName).withTestBuilderId("builder2") //
				.andCommand(builderName).withTestBuilderId("builder3").apply();

		// do an initial build to create builders
		project1.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		List<RebuildingBuilder> builders = RebuildingBuilder.getInstances();
		assertThat(project1.getDescription().getBuildSpec()).hasSameSizeAs(builders);
		RebuildingBuilder b1 = builders.get(0);
		RebuildingBuilder b2 = builders.get(1);
		RebuildingBuilder b3 = builders.get(2);
		builders.forEach(RebuildingBuilder::reset);

		// Create and set a build spec for the project
		updateProjectDescription(project2).addingCommand(builderName).withTestBuilderId("builder1") //
				.andCommand(builderName).withTestBuilderId("builder2") //
				.andCommand(builderName).withTestBuilderId("builder3").apply();

		builders.forEach(b -> b.setPropagateRebuild(false));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		project2.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		assertEquals(6, builders.size());
		RebuildingBuilder b4 = builders.get(3);
		RebuildingBuilder b5 = builders.get(4);
		RebuildingBuilder b6 = builders.get(5);
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(false));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// Confirm basic functionality first - one build per builder
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(1, b1.buildsCount());
		assertEquals(1, b2.buildsCount());
		assertEquals(1, b3.buildsCount());

		assertEquals(1, b4.buildsCount());
		assertEquals(1, b5.buildsCount());
		assertEquals(1, b6.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(false));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// First builder requests rebuild - it will not cause main build to
		// loop extra time
		b1.setRequestProjectRebuild(project1, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(1, b1.buildsCount());
		assertEquals(1, b2.buildsCount());
		assertEquals(1, b3.buildsCount());

		assertEquals(1, b4.buildsCount());
		assertEquals(1, b5.buildsCount());
		assertEquals(1, b6.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(false));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// Second builder requests rebuild - it will not cause main build to
		// loop extra time
		b2.setRequestProjectRebuild(project1, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(2, b1.buildsCount());
		assertEquals(2, b2.buildsCount());
		assertEquals(1, b3.buildsCount());

		assertEquals(1, b4.buildsCount());
		assertEquals(1, b5.buildsCount());
		assertEquals(1, b6.buildsCount());

		List<Integer> expectedKinds = List.of(IncrementalProjectBuilder.FULL_BUILD,
				IncrementalProjectBuilder.INCREMENTAL_BUILD);
		assertEquals(expectedKinds, b1.buildKinds());
		assertEquals(expectedKinds, b2.buildKinds());

		expectedKinds = List.of(IncrementalProjectBuilder.FULL_BUILD);
		assertEquals(expectedKinds, b3.buildKinds());
		assertEquals(expectedKinds, b4.buildKinds());
		assertEquals(expectedKinds, b5.buildKinds());
		assertEquals(expectedKinds, b6.buildKinds());

		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(false));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// All builders in one project request rebuild - still no extra round for
		// second project
		b1.setRequestProjectRebuild(project1, 1);
		b2.setRequestProjectRebuild(project1, 1);
		b3.setRequestProjectRebuild(project1, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(3, b1.buildsCount());
		assertEquals(3, b2.buildsCount());
		assertEquals(2, b3.buildsCount());

		assertEquals(1, b4.buildsCount());
		assertEquals(1, b5.buildsCount());
		assertEquals(1, b6.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(false));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// One builder in each project requests rebuild - still no extra round for
		// the main loop
		b2.setRequestProjectRebuild(project1, 1);
		b5.setRequestProjectRebuild(project2, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(2, b1.buildsCount());
		assertEquals(2, b2.buildsCount());
		assertEquals(1, b3.buildsCount());

		assertEquals(2, b4.buildsCount());
		assertEquals(2, b5.buildsCount());
		assertEquals(1, b6.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(false));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// All builders in all projects request rebuild - still no extra round for
		// the main loop
		b1.setRequestProjectRebuild(project1, 1);
		b2.setRequestProjectRebuild(project1, 1);
		b3.setRequestProjectRebuild(project1, 1);
		b4.setRequestProjectRebuild(project2, 1);
		b5.setRequestProjectRebuild(project2, 1);
		b6.setRequestProjectRebuild(project2, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(3, b1.buildsCount());
		assertEquals(3, b2.buildsCount());
		assertEquals(2, b3.buildsCount());

		assertEquals(3, b4.buildsCount());
		assertEquals(3, b5.buildsCount());
		assertEquals(2, b6.buildsCount());
	}

	/**
	 * Tests IncrementProjectBuilder.requestProjectRebuild with multiple projects
	 */
	@Test
	public void testMultipleProjectsNoPropagationAndOtherBuilders() throws Exception {
		// Create some resource handles
		IProject project1 = getWorkspace().getRoot().getProject(testName + 1);
		IProject project2 = getWorkspace().getRoot().getProject(testName + 2);

		// Create and open a project
		project1.create(createTestMonitor());
		project1.open(createTestMonitor());
		project2.create(createTestMonitor());
		project2.open(createTestMonitor());

		// Create and set a build spec for the project
		updateProjectDescription(project1).addingCommand(builderName).withTestBuilderId("builder1") //
				.andCommand(builderName).withTestBuilderId("builder2") //
				.andCommand(builderName).withTestBuilderId("builder3").apply();

		// do an initial build to create builders
		project1.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		List<RebuildingBuilder> builders = RebuildingBuilder.getInstances();
		assertThat(project1.getDescription().getBuildSpec()).hasSameSizeAs(builders);
		RebuildingBuilder b1 = builders.get(0);
		RebuildingBuilder b2 = builders.get(1);
		RebuildingBuilder b3 = builders.get(2);
		builders.forEach(RebuildingBuilder::reset);

		// Create and set a build spec for the project
		updateProjectDescription(project2).addingCommand(builderName).withTestBuilderId("builder1") //
				.andCommand(builderName).withTestBuilderId("builder2") //
				.andCommand(builderName).withTestBuilderId("builder3").apply();

		builders.forEach(b -> b.setPropagateRebuild(false));
		builders.forEach(b -> b.setProcessOtherBuilders(true));

		project2.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		assertEquals(6, builders.size());
		RebuildingBuilder b4 = builders.get(3);
		RebuildingBuilder b5 = builders.get(4);
		RebuildingBuilder b6 = builders.get(5);
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(false));
		builders.forEach(b -> b.setProcessOtherBuilders(true));

		// Confirm basic functionality first - one build per builder
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(1, b1.buildsCount());
		assertEquals(1, b2.buildsCount());
		assertEquals(1, b3.buildsCount());

		assertEquals(1, b4.buildsCount());
		assertEquals(1, b5.buildsCount());
		assertEquals(1, b6.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(false));
		builders.forEach(b -> b.setProcessOtherBuilders(true));

		// First builder requests rebuild - it will not cause main build to
		// loop extra time
		b1.setRequestProjectRebuild(project1, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(2, b1.buildsCount());
		assertEquals(2, b2.buildsCount());
		assertEquals(2, b3.buildsCount());

		assertEquals(1, b4.buildsCount());
		assertEquals(1, b5.buildsCount());
		assertEquals(1, b6.buildsCount());

		List<Integer> expectedKinds = List.of(IncrementalProjectBuilder.FULL_BUILD, IncrementalProjectBuilder.INCREMENTAL_BUILD);
		assertEquals(expectedKinds, b1.buildKinds());
		assertEquals(expectedKinds, b2.buildKinds());
		assertEquals(expectedKinds, b3.buildKinds());

		expectedKinds = List.of(IncrementalProjectBuilder.FULL_BUILD);
		assertEquals(expectedKinds, b4.buildKinds());
		assertEquals(expectedKinds, b5.buildKinds());
		assertEquals(expectedKinds, b6.buildKinds());

		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(false));
		builders.forEach(b -> b.setProcessOtherBuilders(true));

		// Second builder requests rebuild - it will not cause main build to
		// loop extra time
		b2.setRequestProjectRebuild(project1, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(2, b1.buildsCount());
		assertEquals(2, b2.buildsCount());
		assertEquals(2, b3.buildsCount());

		assertEquals(1, b4.buildsCount());
		assertEquals(1, b5.buildsCount());
		assertEquals(1, b6.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(false));
		builders.forEach(b -> b.setProcessOtherBuilders(true));

		// All builders in one project request rebuild - still no extra round for
		// second project
		b1.setRequestProjectRebuild(project1, 1);
		b2.setRequestProjectRebuild(project1, 1);
		b3.setRequestProjectRebuild(project1, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(2, b1.buildsCount());
		assertEquals(2, b2.buildsCount());
		assertEquals(2, b3.buildsCount());

		assertEquals(1, b4.buildsCount());
		assertEquals(1, b5.buildsCount());
		assertEquals(1, b6.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(false));
		builders.forEach(b -> b.setProcessOtherBuilders(true));

		// One builder in each project requests rebuild - still no extra round for
		// the main loop
		b2.setRequestProjectRebuild(project1, 1);
		b5.setRequestProjectRebuild(project2, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(2, b1.buildsCount());
		assertEquals(2, b2.buildsCount());
		assertEquals(2, b3.buildsCount());

		assertEquals(2, b4.buildsCount());
		assertEquals(2, b5.buildsCount());
		assertEquals(2, b6.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(false));
		builders.forEach(b -> b.setProcessOtherBuilders(true));

		// All builders in all projects request rebuild - still no extra round for
		// the main loop
		b1.setRequestProjectRebuild(project1, 1);
		b2.setRequestProjectRebuild(project1, 1);
		b3.setRequestProjectRebuild(project1, 1);
		b4.setRequestProjectRebuild(project2, 1);
		b5.setRequestProjectRebuild(project2, 1);
		b6.setRequestProjectRebuild(project2, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(2, b1.buildsCount());
		assertEquals(2, b2.buildsCount());
		assertEquals(2, b3.buildsCount());

		assertEquals(2, b4.buildsCount());
		assertEquals(2, b5.buildsCount());
		assertEquals(2, b6.buildsCount());
	}

	/**
	 * Tests IncrementProjectBuilder.requestProjectRebuild with multiple projects
	 */
	@Test
	public void testMultipleProjectsNoPropagationNoOtherBuildersEarlyExit() throws Exception {
		// turn rebuild request propagation to other projects in same loop off
		allowEarlyBuildLoopExit(true);

		// Create some resource handles
		IProject project1 = getWorkspace().getRoot().getProject(testName + 1);
		IProject project2 = getWorkspace().getRoot().getProject(testName + 2);

		// Create and open a project
		project1.create(createTestMonitor());
		project1.open(createTestMonitor());
		project2.create(createTestMonitor());
		project2.open(createTestMonitor());

		// Create and set a build spec for the project
		updateProjectDescription(project1).addingCommand(builderName).withTestBuilderId("builder1") //
				.andCommand(builderName).withTestBuilderId("builder2") //
				.andCommand(builderName).withTestBuilderId("builder3").apply();

		// do an initial build to create builders
		project1.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		List<RebuildingBuilder> builders = RebuildingBuilder.getInstances();
		assertThat(project1.getDescription().getBuildSpec()).hasSameSizeAs(builders);
		RebuildingBuilder b1 = builders.get(0);
		RebuildingBuilder b2 = builders.get(1);
		RebuildingBuilder b3 = builders.get(2);

		// Create and set a build spec for the project
		updateProjectDescription(project2).addingCommand(builderName).withTestBuilderId("builder1") //
				.andCommand(builderName).withTestBuilderId("builder2") //
				.andCommand(builderName).withTestBuilderId("builder3").apply();

		builders.forEach(b -> b.setPropagateRebuild(false));

		project2.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		assertEquals(6, builders.size());
		RebuildingBuilder b4 = builders.get(3);
		RebuildingBuilder b5 = builders.get(4);
		RebuildingBuilder b6 = builders.get(5);
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(false));

		// Confirm basic functionality first - one build per builder
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(1, b1.buildsCount());
		assertEquals(1, b2.buildsCount());
		assertEquals(1, b3.buildsCount());

		assertEquals(1, b4.buildsCount());
		assertEquals(1, b5.buildsCount());
		assertEquals(1, b6.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(false));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// First builder requests rebuild - it will not cause main build to
		// loop extra time
		b1.setRequestProjectRebuild(project1, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(1, b1.buildsCount());
		assertEquals(1, b2.buildsCount());
		assertEquals(1, b3.buildsCount());

		assertEquals(1, b4.buildsCount());
		assertEquals(1, b5.buildsCount());
		assertEquals(1, b6.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(false));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// Second builder requests rebuild - it will not cause main build to
		// loop extra time
		b2.setRequestProjectRebuild(project1, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(2, b1.buildsCount());
		assertEquals(2, b2.buildsCount());
		assertEquals(1, b3.buildsCount());

		assertEquals(1, b4.buildsCount());
		assertEquals(1, b5.buildsCount());
		assertEquals(1, b6.buildsCount());

		List<Integer> expectedKinds = List.of(IncrementalProjectBuilder.FULL_BUILD,
				IncrementalProjectBuilder.INCREMENTAL_BUILD);
		assertEquals(expectedKinds, b1.buildKinds());
		assertEquals(expectedKinds, b2.buildKinds());

		expectedKinds = List.of(IncrementalProjectBuilder.FULL_BUILD);
		assertEquals(expectedKinds, b3.buildKinds());
		assertEquals(expectedKinds, b4.buildKinds());
		assertEquals(expectedKinds, b5.buildKinds());
		assertEquals(expectedKinds, b6.buildKinds());

		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(false));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// All builders in one project request rebuild - still no extra round for
		// second project
		b1.setRequestProjectRebuild(project1, 1);
		b2.setRequestProjectRebuild(project1, 1);
		b3.setRequestProjectRebuild(project1, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(3, b1.buildsCount());
		assertEquals(3, b2.buildsCount());
		assertEquals(2, b3.buildsCount());

		assertEquals(1, b4.buildsCount());
		assertEquals(1, b5.buildsCount());
		assertEquals(1, b6.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(false));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// One builder in each project requests rebuild - still no extra round for
		// the main loop
		b2.setRequestProjectRebuild(project1, 1);
		b5.setRequestProjectRebuild(project2, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(2, b1.buildsCount());
		assertEquals(2, b2.buildsCount());
		assertEquals(1, b3.buildsCount());

		assertEquals(2, b4.buildsCount());
		assertEquals(2, b5.buildsCount());
		assertEquals(1, b6.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(false));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// All builders in all projects request rebuild - still no extra round for
		// the main loop
		b1.setRequestProjectRebuild(project1, 1);
		b2.setRequestProjectRebuild(project1, 1);
		b3.setRequestProjectRebuild(project1, 1);
		b4.setRequestProjectRebuild(project2, 1);
		b5.setRequestProjectRebuild(project2, 1);
		b6.setRequestProjectRebuild(project2, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(3, b1.buildsCount());
		assertEquals(3, b2.buildsCount());
		assertEquals(2, b3.buildsCount());

		assertEquals(3, b4.buildsCount());
		assertEquals(3, b5.buildsCount());
		assertEquals(2, b6.buildsCount());
	}

	/**
	 * Tests IncrementProjectBuilder.requestProjectRebuild with multiple projects
	 */
	@Test
	public void testMultipleProjectsPropagationAndNoOtherBuildersExplicitRebuild() throws Exception {
		// Create some resource handles
		IProject project1 = getWorkspace().getRoot().getProject(testName + 1);
		IProject project2 = getWorkspace().getRoot().getProject(testName + 2);
		IProject project3 = getWorkspace().getRoot().getProject(testName + 3);

		// Create and open a project
		project1.create(createTestMonitor());
		project1.open(createTestMonitor());
		project2.create(createTestMonitor());
		project2.open(createTestMonitor());
		project3.create(createTestMonitor());
		project3.open(createTestMonitor());

		// Create and set a build spec for the project
		updateProjectDescription(project1).addingCommand(builderName).withTestBuilderId("builder1").apply();

		// do an initial build to create builders
		project1.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		List<RebuildingBuilder> builders = RebuildingBuilder.getInstances();
		assertThat(project1.getDescription().getBuildSpec()).hasSameSizeAs(builders);
		RebuildingBuilder b1 = builders.get(0);

		// Create and set a build spec for the project
		updateProjectDescription(project2).addingCommand(builderName).withTestBuilderId("builder1").apply();

		project2.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		assertEquals(2, builders.size());
		RebuildingBuilder b2 = builders.get(1);

		// Create and set a build spec for the project
		updateProjectDescription(project3).addingCommand(builderName).withTestBuilderId("builder1").apply();

		project3.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		assertEquals(3, builders.size());
		RebuildingBuilder b3 = builders.get(2);
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// Confirm basic functionality first - one build per builder
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(1, b1.buildsCount());
		assertEquals(1, b2.buildsCount());
		assertEquals(1, b3.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// First builder requests rebuild for second project - it not rebuild anything
		// because second is not built yet
		b1.setPropagateRebuild(false);
		b1.setRequestProjectRebuild(project2, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(1, b1.buildsCount());
		assertEquals(1, b2.buildsCount());
		assertEquals(1, b3.buildsCount());

		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// First builder requests rebuild for second project, second for third
		// same here: no rebuilds
		b1.setPropagateRebuild(false);
		b1.setRequestProjectRebuild(project2, 1);
		b2.setPropagateRebuild(false);
		b2.setRequestProjectRebuild(project3, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(1, b1.buildsCount());
		assertEquals(1, b2.buildsCount());
		assertEquals(1, b3.buildsCount());

		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// First builder requests rebuild for every project.
		// Only first one will be rebuilt, others aren't because they weren't built
		// before
		b1.setPropagateRebuild(false);
		b1.setRequestProjectRebuild(project1, 1);
		b1.setPropagateRebuild(false);
		b1.setRequestProjectRebuild(project2, 1);
		b1.setPropagateRebuild(false);
		b1.setRequestProjectRebuild(project3, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(2, b1.buildsCount());
		assertEquals(1, b2.buildsCount());
		assertEquals(1, b3.buildsCount());

		List<Integer> expectedKinds = List.of(IncrementalProjectBuilder.FULL_BUILD,
				IncrementalProjectBuilder.INCREMENTAL_BUILD);
		assertEquals(expectedKinds, b1.buildKinds());

		expectedKinds = List.of(IncrementalProjectBuilder.FULL_BUILD);
		assertEquals(expectedKinds, b2.buildKinds());
		assertEquals(expectedKinds, b3.buildKinds());

		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// Second builder requests rebuild for p1
		b2.setPropagateRebuild(false);
		b2.setRequestProjectRebuild(project1, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(2, b1.buildsCount());
		assertEquals(1, b2.buildsCount());
		assertEquals(1, b3.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// All projects request rebuilds for next projects: only p1
		// will be rebuilt
		b1.setPropagateRebuild(false);
		b1.setRequestProjectRebuild(project2, 1);
		b2.setPropagateRebuild(false);
		b2.setRequestProjectRebuild(project3, 1);
		b3.setPropagateRebuild(false);
		b3.setRequestProjectRebuild(project1, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(2, b1.buildsCount());
		assertEquals(1, b2.buildsCount());
		assertEquals(1, b3.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// Third builder requests rebuild for all: all will be rebuilt
		b3.setPropagateRebuild(false);
		b3.setRequestProjectRebuild(project1, 1);
		b3.setPropagateRebuild(false);
		b3.setRequestProjectRebuild(project2, 1);
		b3.setPropagateRebuild(false);
		b3.setRequestProjectRebuild(project3, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(2, b1.buildsCount());
		assertEquals(2, b2.buildsCount());
		assertEquals(2, b3.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// All builders requests rebuild for all: all will be rebuilt
		b1.setPropagateRebuild(false);
		b1.setRequestProjectRebuild(project1, project2, project3);
		b2.setPropagateRebuild(false);
		b2.setRequestProjectRebuild(project1, project2, project3);
		b3.setPropagateRebuild(false);
		b3.setRequestProjectRebuild(project1, project2, project3);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(2, b1.buildsCount());
		assertEquals(2, b2.buildsCount());
		assertEquals(2, b3.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		// One builder requests rebuild for same project and sets the global flag
		// all will be rebuilt once
		b1.setPropagateRebuild(true);
		b1.setRequestProjectRebuild(project1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(2, b1.buildsCount());
		assertEquals(2, b2.buildsCount());
		assertEquals(2, b3.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(false));

		final int max = 5;
		IWorkspaceDescription description = getWorkspace().getDescription();
		description.setMaxBuildIterations(max);
		getWorkspace().setDescription(description);

		// First and third builder requests N rebuilds - it will cause main build to
		// loop not more than max times (both inner / outer loop use limit)
		int rebuilds = 42;
		b1.setPropagateRebuild(false);
		b1.setRequestProjectRebuild(project3, rebuilds);
		b3.setPropagateRebuild(false);
		b3.setRequestProjectRebuild(project1, rebuilds);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(max - 1, b1.buildsCount());
		assertEquals(1, b2.buildsCount());
		assertEquals(max - 2, b3.buildsCount());
	}

	/**
	 * Tests IncrementProjectBuilder.requestProjectRebuild with multiple projects
	 */
	@Test
	public void testMultipleProjectsPropagationAndProcessOtherBuildersExplicitRebuild() throws Exception {
		// Create some resource handles
		IProject project1 = getWorkspace().getRoot().getProject(testName + 1);
		IProject project2 = getWorkspace().getRoot().getProject(testName + 2);
		IProject project3 = getWorkspace().getRoot().getProject(testName + 3);

		// Create and open a project
		project1.create(createTestMonitor());
		project1.open(createTestMonitor());
		project2.create(createTestMonitor());
		project2.open(createTestMonitor());
		project3.create(createTestMonitor());
		project3.open(createTestMonitor());

		// Create and set a build spec for the project
		updateProjectDescription(project1).addingCommand(builderName).withTestBuilderId("builder1").apply();

		// do an initial build to create builders
		project1.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		List<RebuildingBuilder> builders = RebuildingBuilder.getInstances();
		assertThat(project1.getDescription().getBuildSpec()).hasSameSizeAs(builders);
		RebuildingBuilder b1 = builders.get(0);

		// Create and set a build spec for the project
		updateProjectDescription(project2).addingCommand(builderName).withTestBuilderId("builder1").apply();

		project2.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		assertEquals(2, builders.size());
		RebuildingBuilder b2 = builders.get(1);

		// Create and set a build spec for the project
		updateProjectDescription(project3).addingCommand(builderName).withTestBuilderId("builder1").apply();

		project3.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		assertEquals(3, builders.size());
		RebuildingBuilder b3 = builders.get(2);
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(true));

		// Confirm basic functionality first - one build per builder
		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(1, b1.buildsCount());
		assertEquals(1, b2.buildsCount());
		assertEquals(1, b3.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(true));

		// First builder requests rebuild for second project - it not rebuild anything
		// because second is not built yet
		b1.setPropagateRebuild(false);
		b1.setRequestProjectRebuild(project2, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(1, b1.buildsCount());
		assertEquals(1, b2.buildsCount());
		assertEquals(1, b3.buildsCount());

		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(true));

		// First builder requests rebuild for second project, second for third
		// same here: no rebuilds
		b1.setPropagateRebuild(false);
		b1.setRequestProjectRebuild(project2, 1);
		b2.setPropagateRebuild(false);
		b2.setRequestProjectRebuild(project3, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(1, b1.buildsCount());
		assertEquals(1, b2.buildsCount());
		assertEquals(1, b3.buildsCount());

		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(true));

		// First builder requests rebuild for every project.
		// Only first one will be rebuilt, others aren't because they weren't built
		// before
		b1.setPropagateRebuild(false);
		b1.setRequestProjectRebuild(project1, 1);
		b1.setPropagateRebuild(false);
		b1.setRequestProjectRebuild(project2, 1);
		b1.setPropagateRebuild(false);
		b1.setRequestProjectRebuild(project3, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(2, b1.buildsCount());
		assertEquals(1, b2.buildsCount());
		assertEquals(1, b3.buildsCount());

		List<Integer> expectedKinds = List.of(IncrementalProjectBuilder.FULL_BUILD,
				IncrementalProjectBuilder.INCREMENTAL_BUILD);
		assertEquals(expectedKinds, b1.buildKinds());

		expectedKinds = List.of(IncrementalProjectBuilder.FULL_BUILD);
		assertEquals(expectedKinds, b2.buildKinds());
		assertEquals(expectedKinds, b3.buildKinds());

		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(true));

		// Second builder requests rebuild for p1
		b2.setPropagateRebuild(false);
		b2.setRequestProjectRebuild(project1, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(2, b1.buildsCount());
		assertEquals(1, b2.buildsCount());
		assertEquals(1, b3.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(true));

		// All projects request rebuilds for next projects: only p1
		// will be rebuilt
		b1.setPropagateRebuild(false);
		b1.setRequestProjectRebuild(project2, 1);
		b2.setPropagateRebuild(false);
		b2.setRequestProjectRebuild(project3, 1);
		b3.setPropagateRebuild(false);
		b3.setRequestProjectRebuild(project1, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(2, b1.buildsCount());
		assertEquals(1, b2.buildsCount());
		assertEquals(1, b3.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(true));

		// Third builder requests rebuild for all: all will be rebuilt
		b3.setPropagateRebuild(false);
		b3.setRequestProjectRebuild(project1, 1);
		b3.setPropagateRebuild(false);
		b3.setRequestProjectRebuild(project2, 1);
		b3.setPropagateRebuild(false);
		b3.setRequestProjectRebuild(project3, 1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(2, b1.buildsCount());
		assertEquals(2, b2.buildsCount());
		assertEquals(2, b3.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(true));

		// All builders requests rebuild for all: all will be rebuilt
		b1.setPropagateRebuild(false);
		b1.setRequestProjectRebuild(project1, project2, project3);
		b2.setPropagateRebuild(false);
		b2.setRequestProjectRebuild(project1, project2, project3);
		b3.setPropagateRebuild(false);
		b3.setRequestProjectRebuild(project1, project2, project3);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(2, b1.buildsCount());
		assertEquals(2, b2.buildsCount());
		assertEquals(2, b3.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(true));

		// One builder requests rebuild for same project and sets the global flag
		// one will be built twice, others once
		b1.setPropagateRebuild(true);
		b1.setRequestProjectRebuild(project1);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(3, b1.buildsCount());
		assertEquals(2, b2.buildsCount());
		assertEquals(2, b3.buildsCount());
		builders.forEach(RebuildingBuilder::reset);
		builders.forEach(b -> b.setPropagateRebuild(true));
		builders.forEach(b -> b.setProcessOtherBuilders(true));

		final int max = 5;
		IWorkspaceDescription description = getWorkspace().getDescription();
		description.setMaxBuildIterations(max);
		getWorkspace().setDescription(description);

		// First and third builder requests N rebuilds - it will cause main build to
		// loop not more than max times (both inner / outer loop use limit)
		int rebuilds = 42;
		b1.setPropagateRebuild(false);
		b1.setRequestProjectRebuild(project3, rebuilds);
		b3.setPropagateRebuild(false);
		b3.setRequestProjectRebuild(project1, rebuilds);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		assertEquals(max - 1, b1.buildsCount());
		assertEquals(1, b2.buildsCount());
		assertEquals(max - 2, b3.buildsCount());
	}

	private void allowEarlyBuildLoopExit(boolean earlyExitFromInnerBuildLoopAllowed) {
		((Workspace) getWorkspace()).getBuildManager()
				.setEarlyExitFromBuildLoopAllowed(earlyExitFromInnerBuildLoopAllowed);
	}

}
