/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.internal.builders;

import static org.eclipse.core.tests.resources.TestUtil.waitForCondition;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.hamcrest.number.OrderingComparison.lessThanOrEqualTo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.core.internal.events.BuildCommand;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobGroup;
import org.eclipse.core.tests.harness.TestBarrier2;
import org.eclipse.core.tests.internal.builders.TimerBuilder.RuleType;
import org.junit.Assert;
import org.junit.Test;

public class ParallelBuildChainTest extends AbstractBuilderTest {
	private static final int NUMBER_OF_PROJECTS_TO_CREATE_AT_ONCE = 3;

	private static final int MAXIMUM_NUMBER_OF_CONCURRENT_BUILDS = 3;

	private static final int TIMEOUT_IN_MILLIS = 60_000;

	private static enum BuildDurationType {
		/*
		 * Immediately finishing build
		 */
		IMMEDIATE,

		/**
		 * Short running build
		 */
		SHORT_RUNNING,

		/**
		 * Long running build (will usually not end during test run)
		 */
		LONG_RUNNING;

		public int getDurationInMillis() {
			switch (this) {
			case LONG_RUNNING:
				return 30_000;
			case SHORT_RUNNING:
				return 300;
			case IMMEDIATE:
			default:
				return 0;
			}
		}

		@Override
		public String toString() {
			switch (this) {
			case IMMEDIATE:
				return "immediateBuild";
			case LONG_RUNNING:
				return "longRunningBuild";
			case SHORT_RUNNING:
				return "shortRunningBuild";
			}
			throw new UnsupportedOperationException();
		}

	}

	public ParallelBuildChainTest(String name) {
		super(name);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		setWorkspaceMaxNumberOfConcurrentBuilds();
		setAutoBuilding(false);
	}

	@Override
	protected void tearDown() throws Exception {
		cleanup();
		super.tearDown();
		TimerBuilder.abortCurrentBuilds();
	}

	private void setWorkspaceMaxNumberOfConcurrentBuilds() throws CoreException {
		IWorkspaceDescription description = getWorkspace().getDescription();
		description.setMaxConcurrentBuilds(MAXIMUM_NUMBER_OF_CONCURRENT_BUILDS);
		getWorkspace().setDescription(description);
	}

	@Test
	public void testIndividualProjectBuilds_NoConflictRule() throws Exception {
		createMultipleTestProjects(BuildDurationType.IMMEDIATE, RuleType.NO_CONFLICT);
		var longRunningProjects = createMultipleTestProjects(BuildDurationType.LONG_RUNNING, RuleType.NO_CONFLICT);
		executeIndividualFullProjectBuilds(() -> {
			assertBuildsToStart(getAllProjects());
			assertMinimumNumberOfSimultaneousBuilds(longRunningProjects.size());
		});
	}

	@Test
	public void testIndividualProjectBuilds_ProjectRelaxedRule() throws Exception {
		createMultipleTestProjects(BuildDurationType.IMMEDIATE, RuleType.CURRENT_PROJECT_RELAXED);
		var longRunningProjects = createMultipleTestProjects(BuildDurationType.LONG_RUNNING,
				RuleType.CURRENT_PROJECT_RELAXED);
		executeIndividualFullProjectBuilds(() -> {
			assertBuildsToStart(getAllProjects());
			assertMinimumNumberOfSimultaneousBuilds(longRunningProjects.size());
		});
	}

	@Test
	public void testWorkspaceBuild_NoConflictRule() throws Exception {
		createMultipleTestProjects(BuildDurationType.IMMEDIATE, RuleType.NO_CONFLICT);
		var longRunningBuildProjects = createMultipleTestProjects(BuildDurationType.LONG_RUNNING, RuleType.NO_CONFLICT);
		executeIncrementalWorkspaceBuild(() -> {
			assertBuildsToStart(longRunningBuildProjects);
			assertMinimumNumberOfSimultaneousBuilds(longRunningBuildProjects.size());
			assertMaximumNumberOfWorkspaceBuilds();
		});
	}

	@Test
	public void testWorkspaceBuild_NoConflictRule_WithBuildConfigurations() throws Exception {
		createMultipleTestProjects(BuildDurationType.IMMEDIATE, RuleType.NO_CONFLICT);
		var longRunningBuildProjects = createMultipleTestProjects(BuildDurationType.LONG_RUNNING, RuleType.NO_CONFLICT);
		IBuildConfiguration[] buildConfigurations = getBuildConfigurations(getAllProjects());
		executeIncrementalWorkspaceBuild(buildConfigurations, () -> {
			assertBuildsToStart(longRunningBuildProjects);
			assertMinimumNumberOfSimultaneousBuilds(longRunningBuildProjects.size());
			assertMaximumNumberOfWorkspaceBuilds();
		});
	}

	@Test
	public void testWorkspaceBuild_ProjectRule() throws Exception {
		createMultipleTestProjects(BuildDurationType.IMMEDIATE, RuleType.CURRENT_PROJECT);
		var longRunningProjects = createMultipleTestProjects(BuildDurationType.LONG_RUNNING, RuleType.CURRENT_PROJECT);
		executeIncrementalWorkspaceBuild(() -> {
			assertBuildsToStart(longRunningProjects);
			assertMinimumNumberOfSimultaneousBuilds(longRunningProjects.size());
			assertMaximumNumberOfWorkspaceBuilds();
		});
	}

	@Test
	public void testWorkspaceBuild_ProjectRule_WithBuildConfigurations() throws Exception {
		createMultipleTestProjects(BuildDurationType.IMMEDIATE, RuleType.CURRENT_PROJECT);
		var longRunningBuildProjects = createMultipleTestProjects(BuildDurationType.LONG_RUNNING,
				RuleType.CURRENT_PROJECT);
		IBuildConfiguration[] buildConfigurations = getBuildConfigurations(getAllProjects());
		executeIncrementalWorkspaceBuild(buildConfigurations, () -> {
			assertBuildsToStart(longRunningBuildProjects);
			assertMinimumNumberOfSimultaneousBuilds(longRunningBuildProjects.size());
			assertMaximumNumberOfWorkspaceBuilds();
		});
	}

	@Test
	public void testWorkspaceBuild_ConflictingRule() throws Exception {
		int millisToWaitForUnexpectedParallelBuild = 3_000;
		var longRunningProjects = createMultipleTestProjects(BuildDurationType.LONG_RUNNING, RuleType.WORKSPACE_ROOT);
		executeIncrementalWorkspaceBuild(() -> {
			waitForCondition(() -> TimerBuilder.getStartedProjectBuilds().size() > 1,
					millisToWaitForUnexpectedParallelBuild);
			assertThat(
					"all build jobs have started in time although infinitely running builds with conflicting rules exist",
					TimerBuilder.getStartedProjectBuilds(), not(containsInAnyOrder(longRunningProjects)));
			assertMaximumNumberOfSimultaneousBuilds(1);
		});
	}

	public void testWorkspaceBuild_DependentProjects() throws Exception {
		createMultipleTestProjects(BuildDurationType.IMMEDIATE, RuleType.NO_CONFLICT);
		var shortRunningProjects = createMultipleTestProjects(BuildDurationType.SHORT_RUNNING, RuleType.NO_CONFLICT);
		var projectsToBuild = getAllProjects();
		makeProjectsDependOnEachOther(projectsToBuild);
		int minimumExecutionTimeInMillis = shortRunningProjects.size()
				* BuildDurationType.SHORT_RUNNING.getDurationInMillis();
		ExpectedExecutionTime expectedExecutionTime = ExpectedExecutionTime
				.captureFromCurrentTime(minimumExecutionTimeInMillis);
		executeIncrementalWorkspaceBuild(() -> {
			assertBuildsToFinish(projectsToBuild);
			expectedExecutionTime.assertMinimumExecutionTimeReached();
			assertMaximumNumberOfSimultaneousBuilds(1);
			assertSequentialBuildEventsForProjects(projectsToBuild);
		});
	}

	public void testWorkspaceBuild_DependentProjects_ProjectSubset() throws Exception {
		var immediateBuiltProjects = createMultipleTestProjects(BuildDurationType.IMMEDIATE, RuleType.NO_CONFLICT);
		var shortRunningProjects = createMultipleTestProjects(BuildDurationType.SHORT_RUNNING, RuleType.NO_CONFLICT);
		var projectsToBuild = List.of(immediateBuiltProjects.get(0),
				immediateBuiltProjects.get(immediateBuiltProjects.size() - 1), shortRunningProjects.get(0),
				shortRunningProjects.get(shortRunningProjects.size() - 1));
		makeProjectsDependOnEachOther(projectsToBuild);
		IBuildConfiguration[] selectedBuildConfigurations = getBuildConfigurations(projectsToBuild);
		int minimumExecutionTimeInMillis = 2 * BuildDurationType.SHORT_RUNNING.getDurationInMillis();
		ExpectedExecutionTime expectedExecutionTime = ExpectedExecutionTime
				.captureFromCurrentTime(minimumExecutionTimeInMillis);
		executeIncrementalWorkspaceBuild(selectedBuildConfigurations, () -> {
			assertBuildsToFinish(projectsToBuild);
			expectedExecutionTime.assertMinimumExecutionTimeReached();
			assertMaximumNumberOfSimultaneousBuilds(1);
			assertSequentialBuildEventsForProjects(projectsToBuild);
		});
	}

	public void testWorkspaceBuild_DependentProjectBuildConfigurations() throws Exception {
		createMultipleTestProjects(BuildDurationType.IMMEDIATE, RuleType.NO_CONFLICT);
		var shortRunningProjects = createMultipleTestProjects(BuildDurationType.SHORT_RUNNING, RuleType.NO_CONFLICT);
		var projectsToBuild = getAllProjects();
		makeProjectBuildConfigurationsDependOnEachOther(projectsToBuild);
		int minimumExecutionTimeInMillis = shortRunningProjects.size()
				* BuildDurationType.SHORT_RUNNING.getDurationInMillis();
		ExpectedExecutionTime expectedExecutionTime = ExpectedExecutionTime
				.captureFromCurrentTime(minimumExecutionTimeInMillis);
		executeIncrementalWorkspaceBuild(() -> {
			assertBuildsToFinish(projectsToBuild);
			expectedExecutionTime.assertMinimumExecutionTimeReached();
			assertMaximumNumberOfSimultaneousBuilds(1);
			assertSequentialBuildEventsForProjects(projectsToBuild);
		});
	}

	public void testWorkspaceBuild_DependentProjectBuildConfigurations_ProjectSubset() throws Exception {
		var immediateBuiltProjects = createMultipleTestProjects(BuildDurationType.IMMEDIATE, RuleType.NO_CONFLICT);
		var shortRunningProjects = createMultipleTestProjects(BuildDurationType.SHORT_RUNNING, RuleType.NO_CONFLICT);
		var projectsToBuild = List.of(immediateBuiltProjects.get(0),
				immediateBuiltProjects.get(immediateBuiltProjects.size() - 1), shortRunningProjects.get(0),
				shortRunningProjects.get(shortRunningProjects.size() - 1));
		makeProjectBuildConfigurationsDependOnEachOther(getAllProjects());
		IBuildConfiguration[] selectedBuildConfigurations = getBuildConfigurations(projectsToBuild);
		int minimumExecutionTimeInMillis = 2 * BuildDurationType.SHORT_RUNNING.getDurationInMillis();
		ExpectedExecutionTime expectedExecutionTime = ExpectedExecutionTime
				.captureFromCurrentTime(minimumExecutionTimeInMillis);
		executeIncrementalWorkspaceBuild(selectedBuildConfigurations, () -> {
			assertBuildsToFinish(projectsToBuild);
			expectedExecutionTime.assertMinimumExecutionTimeReached();
			assertMaximumNumberOfSimultaneousBuilds(1);
			assertSequentialBuildEventsForProjects(projectsToBuild);
		});
	}

	private List<IProject> getAllProjects() {
		return Arrays.asList(getWorkspace().getRoot().getProjects());
	}

	private static IBuildConfiguration[] getBuildConfigurations(List<IProject> projects) throws CoreException {
		IBuildConfiguration[] buildConfigurations = new IBuildConfiguration[projects.size()];
		for (int projectNumber = 0; projectNumber < projects.size(); projectNumber++) {
			buildConfigurations[projectNumber] = projects.get(projectNumber).getActiveBuildConfig();
		}
		return buildConfigurations;
	}

	private List<IProject> createMultipleTestProjects(BuildDurationType buildDurationType, RuleType ruleType)
			throws CoreException {
		List<IProject> result = new ArrayList<>();
		for (int projectNumber = 0; projectNumber < NUMBER_OF_PROJECTS_TO_CREATE_AT_ONCE; projectNumber++) {
			result.add(createTestProject(buildDurationType, ruleType));
		}
		return result;
	}

	private IProject createTestProject(BuildDurationType buildDurationType, RuleType ruleType) throws CoreException {
		String projectName = createUniqueProjectName(buildDurationType.toString());
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject(projectName);
		ensureExistsInWorkspace(project, true);
		configureTimerBuilder(project, buildDurationType.getDurationInMillis(), ruleType);
		return project;
	}

	private String createUniqueProjectName(String projectPrefix) {
		int suffix = 0;
		IWorkspaceRoot root = getWorkspace().getRoot();
		while (root.getProject(projectPrefix + "Project" + suffix).exists()) {
			suffix++;
		}
		return projectPrefix + "Project" + suffix;
	}

	private void configureTimerBuilder(IProject project, int duration, RuleType ruleType) throws CoreException {
		BuildCommand buildCommand = new BuildCommand();
		buildCommand.setBuilderName(TimerBuilder.BUILDER_NAME);
		Map<String, String> arguments = new HashMap<>();
		arguments.put(TimerBuilder.DURATION_ARG, Integer.toString(duration));
		arguments.put(TimerBuilder.RULE_TYPE_ARG, ruleType.toString());
		buildCommand.setArguments(arguments);
		IProjectDescription projectDescription = project.getDescription();
		projectDescription.setBuildSpec(new ICommand[] { buildCommand });
		project.setDescription(projectDescription, getMonitor());
	}

	private void makeProjectsDependOnEachOther(List<IProject> projects) throws CoreException {
		for (int projectNumber = 1; projectNumber < projects.size(); projectNumber++) {
			IProject project = projects.get(projectNumber);
			IProjectDescription desc = project.getDescription();
			desc.setReferencedProjects(new IProject[] { projects.get(projectNumber - 1) });
			project.setDescription(desc, getMonitor());
		}
	}

	private void makeProjectBuildConfigurationsDependOnEachOther(List<IProject> projects) throws CoreException {
		for (int projectNumber = 1; projectNumber < projects.size(); projectNumber++) {
			IProject project = projects.get(projectNumber);
			IProjectDescription description = project.getDescription();
			description.setBuildConfigReferences(project.getActiveBuildConfig().getName(),
					new IBuildConfiguration[] { projects.get(projectNumber - 1).getActiveBuildConfig() });
			project.setDescription(description, getMonitor());
		}
	}

	private void executeIncrementalWorkspaceBuild(Runnable executeWhileRunningBuild) throws Exception {
		executeIncrementalWorkspaceBuild(null, executeWhileRunningBuild);
	}

	private void executeIncrementalWorkspaceBuild(IBuildConfiguration[] buildConfigurations,
			Runnable executeWhileRunningBuild) throws Exception {
		int expectedNumberOfBuilds = buildConfigurations != null ? buildConfigurations.length : getAllProjects().size();
		TimerBuilder.setExpectedNumberOfBuilds(expectedNumberOfBuilds);
		TestBarrier2 waitForRunningJobBarrier = new TestBarrier2();
		Job job = new Job("Workspace Build") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					waitForRunningJobBarrier.setStatus(TestBarrier2.STATUS_RUNNING);
					if (buildConfigurations != null) {
						getWorkspace().build(buildConfigurations, IncrementalProjectBuilder.INCREMENTAL_BUILD, false,
								getMonitor());
					} else {
						getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
					}
					return Status.OK_STATUS;
				} catch (CoreException e) {
					return new Status(IStatus.ERROR, PI_RESOURCES_TESTS, e.getMessage(), e);
				}

			}
		};
		job.schedule();
		waitForRunningJobBarrier.waitForStatus(TestBarrier2.STATUS_RUNNING);
		try {
			executeWhileRunningBuild.run();
		} finally {
			TimerBuilder.abortCurrentBuilds();
			job.cancel();
			boolean joinSuccessful = job.join(TIMEOUT_IN_MILLIS, getMonitor());
			Assert.assertTrue("timeout occurred when waiting for job that runs the build to finish", joinSuccessful);
		}
	}

	private void executeIndividualFullProjectBuilds(Runnable executeWhileRunningBuild) throws Exception {
		int maximumThreadsForJobGroup = 5;
		List<IProject> projects = getAllProjects();
		TimerBuilder.setExpectedNumberOfBuilds(projects.size());
		JobGroup jobGroup = new JobGroup("Build Group", maximumThreadsForJobGroup, projects.size());
		Map<IProject, TestBarrier2> waitForRunningJobBarriers = new HashMap<>();
		for (IProject project : projects) {
			waitForRunningJobBarriers.put(project, new TestBarrier2());
			Job job = new Job("Building " + project.getName()) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						waitForRunningJobBarriers.get(project).setStatus(TestBarrier2.STATUS_RUNNING);
						project.build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
						return Status.OK_STATUS;
					} catch (CoreException e) {
						return new Status(IStatus.ERROR, PI_RESOURCES_TESTS, e.getMessage(), e);
					}

				}
			};
			job.setJobGroup(jobGroup);
			job.schedule();
		}
		for (TestBarrier2 barrier : waitForRunningJobBarriers.values()) {
			barrier.waitForStatus(TestBarrier2.STATUS_RUNNING);
		}
		try {
			executeWhileRunningBuild.run();
		} finally {
			TimerBuilder.abortCurrentBuilds();
			jobGroup.cancel();
			boolean joinSuccessful = jobGroup.join(TIMEOUT_IN_MILLIS, getMonitor());
			Assert.assertTrue("timeout occurred when waiting for job group that runs the builds to finish",
					joinSuccessful);
		}
	}

	private void assertMinimumNumberOfSimultaneousBuilds(int minimumNumberOfSimulaneousBuilds) {
		assertThat("too few builds have run in parallel", TimerBuilder.getMaximumNumberOfSimultaneousBuilds(),
				greaterThanOrEqualTo(minimumNumberOfSimulaneousBuilds));
	}

	private void assertMaximumNumberOfSimultaneousBuilds(int maximumNumberOfSimulaneousBuilds) {
		assertThat("too many builds have run in parallel", TimerBuilder.getMaximumNumberOfSimultaneousBuilds(),
				lessThanOrEqualTo(maximumNumberOfSimulaneousBuilds));
	}

	private void assertMaximumNumberOfWorkspaceBuilds() {
		assertThat("too many workspace builds have run in parallel",
				TimerBuilder.getMaximumNumberOfSimultaneousBuilds(),
				lessThanOrEqualTo(getWorkspace().getDescription().getMaxConcurrentBuilds()));
	}

	private void assertBuildsToStart(List<IProject> projects) {
		waitForCondition(() -> TimerBuilder.getStartedProjectBuilds().containsAll(projects), TIMEOUT_IN_MILLIS);
		assertThat("not all build jobs have started in time", TimerBuilder.getStartedProjectBuilds(),
				hasItems(projects.toArray(IProject[]::new)));
	}

	private static class ExpectedExecutionTime {
		final long startTimeInNs = System.nanoTime();
		final long minimumExecutionTimeInMillis;

		private ExpectedExecutionTime(int minimumExecutionTimeInMillis) {
			this.minimumExecutionTimeInMillis = minimumExecutionTimeInMillis;
		}

		private long getExecutionTimeInMillis() {
			return (int) ((System.nanoTime() - startTimeInNs) / 1_000_000);
		}

		void assertMinimumExecutionTimeReached() {
			assertThat("build was faster than the expected execution time (in milliseconds)",
					getExecutionTimeInMillis(), greaterThanOrEqualTo(minimumExecutionTimeInMillis));
		}

		static ExpectedExecutionTime captureFromCurrentTime(int minimumExecutionTimeInMillis) {
			return new ExpectedExecutionTime(minimumExecutionTimeInMillis);
		}
	}

	private void assertBuildsToFinish(List<IProject> projects) {
		waitForCondition(() -> TimerBuilder.getFinishedProjectBuilds().containsAll(projects), TIMEOUT_IN_MILLIS);
		assertThat("not all build jobs have finished in time", TimerBuilder.getFinishedProjectBuilds(),
				hasItems(projects.toArray(IProject[]::new)));
	}

	private void assertSequentialBuildEventsForProjects(Iterable<IProject> projects) {
		assertThat("unexpected order of build events occurred", TimerBuilder.getBuildEvents(),
				equalTo(getExpectedSequentialBuildEvents(projects)));
	}

	private Iterable<Object> getExpectedSequentialBuildEvents(Iterable<IProject> projects) {
		List<Object> res = new ArrayList<>();
		for (IProject project : projects) {
			res.add(TimerBuilder.createStartEvent(project));
			res.add(TimerBuilder.createCompleteEvent(project));
		}
		return res;
	}

}
