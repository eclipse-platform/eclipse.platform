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

import java.util.*;
import org.eclipse.core.internal.events.BuildCommand;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobGroup;
import org.eclipse.core.tests.harness.TestBarrier2;
import org.eclipse.core.tests.internal.builders.TimerBuilder.RuleType;
import org.junit.Assert;
import org.junit.Test;

public class ParallelBuildChainTest extends AbstractBuilderTest {

	private static final int LONG_BUILD_DURATION = 1000;

	public ParallelBuildChainTest(String name) {
		super(name);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		IWorkspaceDescription description = getWorkspace().getDescription();
		description.setMaxConcurrentBuilds(3);
		getWorkspace().setDescription(description);
		setAutoBuilding(false);
		TimerBuilder.reset();
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject projectInstantaneousBuild1 = root.getProject("projectInstantaneousBuild1");
		IProject projectLongBuild1 = root.getProject("projectLongBuild1");
		IProject projectInstantaneousBuild2 = root.getProject("projectInstantaneousBuild2");
		IProject projectLongBuild2 = root.getProject("projectLongBuild2");
		IProject projectInstantaneousBuild3 = root.getProject("projectInstantaneousBuild3");
		IProject projectLongBuild3 = root.getProject("projectLongBuild3");
		ensureExistsInWorkspace(new IResource[] {projectInstantaneousBuild1, projectInstantaneousBuild2, projectInstantaneousBuild3, projectLongBuild1, projectLongBuild2, projectLongBuild3}, true);
		configureTimerBuilder(projectInstantaneousBuild1, 0);
		configureTimerBuilder(projectInstantaneousBuild2, 0);
		configureTimerBuilder(projectInstantaneousBuild3, 0);
		configureTimerBuilder(projectLongBuild1, LONG_BUILD_DURATION);
		configureTimerBuilder(projectLongBuild2, LONG_BUILD_DURATION);
		configureTimerBuilder(projectLongBuild3, LONG_BUILD_DURATION);
	}

	private void configureTimerBuilder(IProject project, int duration) throws CoreException {
		BuildCommand buildCommand = new BuildCommand();
		buildCommand.setBuilderName(TimerBuilder.BUILDER_NAME);
		Map<String, String> arguments = new HashMap<>(2, (float) 1.);
		arguments.put(TimerBuilder.DURATION_ARG, Integer.toString(duration));
		arguments.put(TimerBuilder.RULE_TYPE_ARG, TimerBuilder.RuleType.NO_CONFLICT.toString());
		buildCommand.setArguments(arguments);
		IProjectDescription projectDescription = project.getDescription();
		projectDescription.setBuildSpec(new ICommand[] {buildCommand});
		project.setDescription(projectDescription, getMonitor());
	}

	public IProject[] projectWithLongRunningBuilds() {
		return Arrays.stream(getWorkspace().getRoot().getProjects()).filter(project -> {
			ICommand[] commands;
			try {
				commands = project.getDescription().getBuildSpec();
				return commands.length > 0 && commands[0].getBuilderName().equals(TimerBuilder.BUILDER_NAME)
						&& Integer.parseInt(commands[0].getArguments().get(TimerBuilder.DURATION_ARG)) > 0;
			} catch (CoreException e) {
				fail(e.getMessage(), e);
				return false;
			}
		}).toArray(IProject[]::new);
	}

	private void setTimerBuilderSchedulingRuleForAllProjects(TimerBuilder.RuleType type, IProgressMonitor monitor) throws CoreException {
		for (IProject project : getWorkspace().getRoot().getProjects()) {
			IProjectDescription projectDescription = project.getDescription();
			BuildCommand command = (BuildCommand) projectDescription.getBuildSpec()[0];
			Map<String, String> args = command.getArguments();
			if (args == null) {
				args = Collections.singletonMap(TimerBuilder.RULE_TYPE_ARG, type.toString());
			} else {
				args.put(TimerBuilder.RULE_TYPE_ARG, type.toString());
			}
			command.setArguments(args);
			projectDescription.setBuildSpec(new ICommand[] {command});
			project.setDescription(projectDescription, getMonitor());
		}
	}

	@Test
	public void testIndividualProjectBuildsInParallelNoConflict() throws CoreException, OperationCanceledException, InterruptedException {
		long duration = System.currentTimeMillis();
		JobGroup group = new JobGroup("Build Group", 5, getWorkspace().getRoot().getProjects().length);
		for (IProject project : getWorkspace().getRoot().getProjects()) {
			Job job = new Job("Building " + project) {
				@Override
				public IStatus run(IProgressMonitor monitor) {
					try {
						project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
						return Status.OK_STATUS;
					} catch (CoreException e) {
						return new Status(IStatus.ERROR, "org.eclipse.core.tests.resources", e.getMessage(), e);
					}
				}
			};
			job.setJobGroup(group);
			job.schedule();
		}
		Assert.assertTrue("Timeout, most likely a deadlock", group.join(5000, getMonitor()));
		duration = System.currentTimeMillis() - duration;
		assertEquals(getWorkspace().getRoot().getProjects().length, TimerBuilder.getTotalBuilds());
		assertTrue(TimerBuilder.getMaxSimultaneousBuilds() >= 3);
		assertTrue(duration < projectWithLongRunningBuilds().length * LONG_BUILD_DURATION);
	}

	@Test
	public void testIndividualProjectBuildsInParallelProjectScheduling() throws CoreException, OperationCanceledException, InterruptedException {
		setTimerBuilderSchedulingRuleForAllProjects(RuleType.CURRENT_PROJECT_RELAXED, getMonitor());
		long duration = System.currentTimeMillis();
		JobGroup group = new JobGroup("Build Group", 5, getWorkspace().getRoot().getProjects().length);
		for (IProject project : getWorkspace().getRoot().getProjects()) {
			Job job = new Job("Building " + project) {
				@Override
				public IStatus run(IProgressMonitor monitor) {
					try {
						project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
						return Status.OK_STATUS;
					} catch (CoreException e) {
						return new Status(IStatus.ERROR, "org.eclipse.core.tests.resources", e.getMessage(), e);
					}
				}
			};
			job.setJobGroup(group);
			job.schedule();
		}
		Assert.assertTrue("Timeout, most likely a deadlock", group.join(5000, getMonitor()));
		duration = System.currentTimeMillis() - duration;
		assertEquals(getWorkspace().getRoot().getProjects().length, TimerBuilder.getTotalBuilds());
		assertTrue(TimerBuilder.getMaxSimultaneousBuilds() >= 3);
		assertTrue(duration < projectWithLongRunningBuilds().length * LONG_BUILD_DURATION);
	}

	@Test
	public void testWorkspaceBuildConfigParrallelProjectRule() throws CoreException, OperationCanceledException, InterruptedException {
		setTimerBuilderSchedulingRuleForAllProjects(RuleType.CURRENT_PROJECT, getMonitor());
		long duration = System.currentTimeMillis();
		Job job = new Job("Workspace Build") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					getWorkspace().build(
							Arrays.stream(getWorkspace().getRoot().getProjects()).map(p -> {
								try {
									return p.getActiveBuildConfig();
								} catch (CoreException e) {
									fail(e.getMessage(), e);
									return null;
								}
							}).toArray(IBuildConfiguration[]::new),
							IncrementalProjectBuilder.INCREMENTAL_BUILD,
							true,
							getMonitor());
					return Status.OK_STATUS;
				} catch (CoreException e) {
					return new Status(IStatus.ERROR, "org.eclipse.core.tests.resources", e.getMessage(), e);
				}
			}
		};
		job.schedule();
		Assert.assertTrue("Timeout, most likely a deadlock", job.join(5000, getMonitor()));
		duration = System.currentTimeMillis() - duration;
		assertEquals(getWorkspace().getRoot().getProjects().length, TimerBuilder.getTotalBuilds());
		assertTrue(TimerBuilder.getMaxSimultaneousBuilds() > 1);
		assertTrue(TimerBuilder.getMaxSimultaneousBuilds() <= getWorkspace().getDescription().getMaxConcurrentBuilds());
		assertTrue(duration < projectWithLongRunningBuilds().length * LONG_BUILD_DURATION);
	}

	@Test
	public void testWorkspaceParrallelBuildNoConflict() throws CoreException, OperationCanceledException, InterruptedException {
		setTimerBuilderSchedulingRuleForAllProjects(RuleType.NO_CONFLICT, getMonitor());
		long duration = System.currentTimeMillis();
		Job job = new Job("Workspace Build") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
					return Status.OK_STATUS;
				} catch (CoreException e) {
					return new Status(IStatus.ERROR, "org.eclipse.core.tests.resources", e.getMessage(), e);
				}
			}
		};
		job.schedule();
		Assert.assertTrue("Timeout, most likely a deadlock", job.join(5000, getMonitor()));
		duration = System.currentTimeMillis() - duration;
		assertEquals(getWorkspace().getRoot().getProjects().length, TimerBuilder.getTotalBuilds());
		assertTrue(TimerBuilder.getMaxSimultaneousBuilds() > 1);
		assertTrue(TimerBuilder.getMaxSimultaneousBuilds() <= getWorkspace().getDescription().getMaxConcurrentBuilds());
		assertTrue(duration < projectWithLongRunningBuilds().length * LONG_BUILD_DURATION);
	}

	@Test
	public void testWorkspaceParrallelBuildConflictingRules() throws CoreException, OperationCanceledException, InterruptedException {
		setTimerBuilderSchedulingRuleForAllProjects(RuleType.WORKSPACE_ROOT, getMonitor());
		long duration = System.currentTimeMillis();
		Job job = new Job("Workspace Build") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
					return Status.OK_STATUS;
				} catch (CoreException e) {
					return new Status(IStatus.ERROR, "org.eclipse.core.tests.resources", e.getMessage(), e);
				}
			}
		};
		job.schedule();
		Assert.assertTrue("Timeout, most likely a deadlock", job.join(5000, getMonitor()));
		duration = System.currentTimeMillis() - duration;
		assertEquals(getWorkspace().getRoot().getProjects().length, TimerBuilder.getTotalBuilds());
		assertEquals(1, TimerBuilder.getMaxSimultaneousBuilds());
		assertTrue(
				"Running " + projectWithLongRunningBuilds().length + " conflicting jobs of duration "
						+ LONG_BUILD_DURATION + " should have taken more than " + duration,
				duration > projectWithLongRunningBuilds().length * LONG_BUILD_DURATION);
	}

	@Test
	public void testWorkspaceParrallelBuildCurrentProject() throws CoreException, OperationCanceledException, InterruptedException {
		TestBarrier2 waitForRunningJobBarrier = new TestBarrier2();
		setTimerBuilderSchedulingRuleForAllProjects(RuleType.CURRENT_PROJECT, getMonitor());
		long duration = System.currentTimeMillis();
		Job job = new Job("Workspace Build") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					waitForRunningJobBarrier.setStatus(TestBarrier2.STATUS_RUNNING);
					getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
					return Status.OK_STATUS;
				} catch (CoreException e) {
					return new Status(IStatus.ERROR, "org.eclipse.core.tests.resources", e.getMessage(), e);
				}
			}
		};

		job.schedule();
		waitForRunningJobBarrier.waitForStatus(TestBarrier2.STATUS_RUNNING);
		Assert.assertTrue("Timeout, most likely a deadlock", job.join(20000, getMonitor()));
		duration = System.currentTimeMillis() - duration;
		assertEquals(getWorkspace().getRoot().getProjects().length, TimerBuilder.getTotalBuilds());
		assertTrue(TimerBuilder.getMaxSimultaneousBuilds() > 1);
		assertTrue(TimerBuilder.getMaxSimultaneousBuilds() <= getWorkspace().getDescription().getMaxConcurrentBuilds());
		assertTrue(duration < projectWithLongRunningBuilds().length * LONG_BUILD_DURATION);
	}

	public void testDependentProjectsBuildSequentially() throws Exception {
		IProject[] allProjects = getWorkspace().getRoot().getProjects();
		for (int i = 1; i < allProjects.length; i++) {
			IProject project = allProjects[i];
			IProjectDescription desc = project.getDescription();
			desc.setReferencedProjects(new IProject[] {allProjects[i - 1]});
			project.setDescription(desc, getMonitor());
		}
		setTimerBuilderSchedulingRuleForAllProjects(RuleType.NO_CONFLICT, getMonitor());
		long duration = System.currentTimeMillis();
		Job job = new Job("Workspace Build") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
					return Status.OK_STATUS;
				} catch (CoreException e) {
					return new Status(IStatus.ERROR, "org.eclipse.core.tests.resources", e.getMessage(), e);
				}
			}
		};
		job.schedule();
		Assert.assertTrue("Timeout, most likely a deadlock", job.join(5000, getMonitor()));
		duration = System.currentTimeMillis() - duration;
		assertEquals(allProjects.length, TimerBuilder.getTotalBuilds());
		assertEquals(1, TimerBuilder.getMaxSimultaneousBuilds());
		assertTrue(duration > projectWithLongRunningBuilds().length * LONG_BUILD_DURATION);
		assertEquals(sequentialBuildEvents(allProjects), TimerBuilder.events);
	}

	private List<Object> sequentialBuildEvents(IProject[] allProjects) {
		List<Object> res = new ArrayList<>(allProjects.length * 2);
		for (IProject project : allProjects) {
			res.add(TimerBuilder.buildStartEvent(project));
			res.add(TimerBuilder.buildCompleteEvent(project));
		}
		return res;
	}

	public void testDependentBuildConfigBuildSequentially() throws Exception {
		IProject[] allProjects = getWorkspace().getRoot().getProjects();
		for (int i = 1; i < allProjects.length; i++) {
			IProject project = allProjects[i];
			IProjectDescription desc = project.getDescription();
			desc.setBuildConfigReferences(project.getActiveBuildConfig().getName(), new IBuildConfiguration[] {allProjects[i - 1].getActiveBuildConfig()});
			project.setDescription(desc, getMonitor());
		}
		setTimerBuilderSchedulingRuleForAllProjects(RuleType.NO_CONFLICT, getMonitor());
		long duration = System.currentTimeMillis();
		Job job = new Job("Workspace Build") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
					return Status.OK_STATUS;
				} catch (CoreException e) {
					return new Status(IStatus.ERROR, "org.eclipse.core.tests.resources", e.getMessage(), e);
				}
			}
		};
		job.schedule();
		Assert.assertTrue("Timeout, most likely a deadlock", job.join(0, getMonitor()));
		duration = System.currentTimeMillis() - duration;
		assertEquals(allProjects.length, TimerBuilder.getTotalBuilds());
		assertEquals(1, TimerBuilder.getMaxSimultaneousBuilds());
		assertTrue(duration > projectWithLongRunningBuilds().length * LONG_BUILD_DURATION);
		assertEquals(sequentialBuildEvents(allProjects), TimerBuilder.events);
	}

	public void testDependentBuildConfigsSubset() throws Exception {
		setTimerBuilderSchedulingRuleForAllProjects(RuleType.NO_CONFLICT, getMonitor());
		IProject[] allProjects = getWorkspace().getRoot().getProjects();
		for (int i = 1; i < allProjects.length; i++) {
			IProject project = allProjects[i];
			IProjectDescription desc = project.getDescription();
			desc.setBuildConfigReferences(project.getActiveBuildConfig().getName(),
					new IBuildConfiguration[] { allProjects[i - 1].getActiveBuildConfig() });
			project.setDescription(desc, getMonitor());
		}
		long duration = System.currentTimeMillis();
		Job job = new Job("Workspace Build") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					getWorkspace().build(new IBuildConfiguration[] {allProjects[0].getActiveBuildConfig(), allProjects[2].getActiveBuildConfig(), allProjects[3].getActiveBuildConfig(), allProjects[5].getActiveBuildConfig(),}, IncrementalProjectBuilder.INCREMENTAL_BUILD, false, getMonitor());
					return Status.OK_STATUS;
				} catch (CoreException e) {
					return new Status(IStatus.ERROR, "org.eclipse.core.tests.resources", e.getMessage(), e);
				}
			}
		};
		job.schedule();
		Assert.assertTrue("Timeout, most likely a deadlock", job.join(0, getMonitor()));
		duration = System.currentTimeMillis() - duration;
		assertEquals(allProjects.length - 2, TimerBuilder.getTotalBuilds());
		assertEquals(Arrays.asList(TimerBuilder.buildStartEvent(allProjects[0]),
				TimerBuilder.buildCompleteEvent(allProjects[0]),
				TimerBuilder.buildStartEvent(allProjects[2]),
				TimerBuilder.buildCompleteEvent(allProjects[2]),
				TimerBuilder.buildStartEvent(allProjects[3]),
				TimerBuilder.buildCompleteEvent(allProjects[3]),
				TimerBuilder.buildStartEvent(allProjects[5]),
				TimerBuilder.buildCompleteEvent(allProjects[5])
			), TimerBuilder.events);
	}
}
