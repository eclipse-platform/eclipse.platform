/*******************************************************************************
 *  Copyright (c) 2010, 2015 Broadcom Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     James Blackburn (Broadcom Corp.) - initial API and implementation
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *******************************************************************************/
package org.eclipse.core.tests.internal.builders;

import java.io.ByteArrayInputStream;
import java.lang.Thread.State;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.internal.events.ResourceDelta;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.core.tests.harness.TestBarrier2;
import org.eclipse.core.tests.internal.builders.TestBuilder.BuilderRuleCallback;

/**
 * This class tests extended functionality (since 3.6) which allows
 * builders to be run with reduced (non-Workspace) scheduling rules.
 *
 * When one of these builders runs, other threads may modify the workspace
 * depending on the builder's scheduling rule
 */
public class RelaxedSchedRuleBuilderTest extends AbstractBuilderTest {

	public RelaxedSchedRuleBuilderTest(String name) {
		super(name);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		TestBuilder builder = DeltaVerifierBuilder.getInstance();
		if (builder != null) {
			builder.reset();
		}
		builder = EmptyDeltaBuilder.getInstance();
		if (builder != null) {
			builder.reset();
		}
		builder = EmptyDeltaBuilder2.getInstance();
		if (builder != null) {
			builder.reset();
		}
	}

	/**
	 * Test a simple builder with a relaxed scheduling rule
	 * @throws Exception
	 */
	public void testBasicRelaxedSchedulingRules() throws Exception {
		String name = "TestRelaxed";
		setAutoBuilding(false);
		final IProject project = getWorkspace().getRoot().getProject(name);
		create(project, false);
		addBuilder(project, EmptyDeltaBuilder.BUILDER_NAME);

		// Ensure the builder is instantiated
		project.build(IncrementalProjectBuilder.CLEAN_BUILD, getMonitor());

		final TestBarrier2 tb = new TestBarrier2(TestBarrier2.STATUS_WAIT_FOR_START);

		// Create a builder set a null scheduling rule
		EmptyDeltaBuilder builder = EmptyDeltaBuilder.getInstance();
		builder.setRuleCallback(new BuilderRuleCallback() {
			@Override
			public ISchedulingRule getRule(String name, IncrementalProjectBuilder builder, int trigger, Map<String, String> args) {
				return null;
			}

			@Override
			public IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
				assertTrue(Job.getJobManager().currentRule() == null);
				tb.setStatus(TestBarrier2.STATUS_START);
				while (!monitor.isCanceled()) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						// Don't care
					}
				}
				tb.setStatus(TestBarrier2.STATUS_DONE);
				return super.build(kind, args, monitor);
			}
		});

		Job j = new Job("build job") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
				} catch (CoreException e) {
					fail();
				}
				return Status.OK_STATUS;
			}
		};
		j.schedule();

		// wait for build to be called
		tb.waitForStatus(TestBarrier2.STATUS_START);

		// Should be able to write a file in the project
		create(project.getFile("foo.c"), false);
		assertTrue(project.getFile("foo.c").exists());

		// Cancel the builder
		j.cancel();
		tb.waitForStatus(TestBarrier2.STATUS_DONE);
	}

	/**
	 * Test two builders, each with relaxed scheduling rules, run in the same build operation
	 * Each should be run with their own scheduling rule.
	 * Tests:
	 *     Bug 306824 - null scheduling rule and non-null scheduling rule don't work together
	 *     Builders should have separate scheduling rules
	 * @throws Exception
	 */
	public void testTwoBuildersRunInOneBuild() throws Exception {
		String name = "testTwoBuildersRunInOneBuild";
		setAutoBuilding(false);
		final IProject project = getWorkspace().getRoot().getProject(name);
		create(project, false);

		IProjectDescription desc = project.getDescription();
		desc.setBuildSpec(new ICommand[] {createCommand(desc, EmptyDeltaBuilder.BUILDER_NAME, "Project1Build1"), createCommand(desc, EmptyDeltaBuilder2.BUILDER_NAME, "Project1Build2")});
		project.setDescription(desc, getMonitor());

		// Ensure the builder is instantiated
		project.build(IncrementalProjectBuilder.CLEAN_BUILD, getMonitor());

		final TestBarrier2 tb1 = new TestBarrier2(TestBarrier2.STATUS_WAIT_FOR_START);
		final TestBarrier2 tb2 = new TestBarrier2(TestBarrier2.STATUS_WAIT_FOR_START);

		// Create a builder set a null scheduling rule
		EmptyDeltaBuilder builder = EmptyDeltaBuilder.getInstance();
		EmptyDeltaBuilder2 builder2 = EmptyDeltaBuilder2.getInstance();

		// Set the rule call-back
		builder.setRuleCallback(new BuilderRuleCallback() {
			@Override
			public ISchedulingRule getRule(String name, IncrementalProjectBuilder builder, int trigger, Map<String, String> args) {
				tb1.setStatus(TestBarrier2.STATUS_START);
				return project;
			}

			@Override
			public IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
				// shared scheduling rule
				assertTrue(Job.getJobManager().currentRule().contains(project));
				tb1.setStatus(TestBarrier2.STATUS_RUNNING);
				tb1.waitForStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);
				tb1.setStatus(TestBarrier2.STATUS_DONE);
				return super.build(kind, args, monitor);
			}
		});
		// Set the rule call-back
		builder2.setRuleCallback(new BuilderRuleCallback() {
			@Override
			public ISchedulingRule getRule(String name, IncrementalProjectBuilder builder, int trigger, Map<String, String> args) {
				// get rule is called before starting
				tb2.setStatus(TestBarrier2.STATUS_START);
				return null;
			}

			@Override
			public IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
				// shared scheduling rule
				assertTrue(Job.getJobManager().currentRule() == null || Job.getJobManager().currentRule().contains(getWorkspace().getRoot()));
				tb2.setStatus(TestBarrier2.STATUS_RUNNING);
				tb2.waitForStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);
				tb2.setStatus(TestBarrier2.STATUS_DONE);
				return super.build(kind, args, monitor);
			}
		});

		// Run the build
		Job j = new Job("build job1") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
				} catch (CoreException e) {
					fail();
				}
				return Status.OK_STATUS;
			}
		};
		j.schedule();

		// Wait for the build to transition

		tb1.waitForStatus(TestBarrier2.STATUS_RUNNING);
		tb1.setStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);
		tb1.waitForStatus(TestBarrier2.STATUS_DONE);

		tb2.waitForStatus(TestBarrier2.STATUS_RUNNING);
		tb2.setStatus(TestBarrier2.STATUS_WAIT_FOR_DONE);
		tb2.waitForStatus(TestBarrier2.STATUS_DONE);
	}

	HashSet<ISchedulingRule> getRulesAsSet(ISchedulingRule rule) {
		HashSet<ISchedulingRule> rules = new HashSet<>();
		if (rule == null) {
			return rules;
		}
		if (rule instanceof MultiRule mRule) {
			rules.addAll(Arrays.asList(mRule.getChildren()));
		} else {
			rules.add(rule);
		}
		return rules;
	}

	/**
	 * As the builder is run with a relaxed scheduling rule, we ensure that any changes made before
	 * the build is actually run are present in the delta.
	 * Acquiring the scheduling rule must be done outside of the WS lock, so this tests that
	 * a change which sneaks in during the window or the build thread acquiring its scheduling
	 * rule, is correctly present in the builder's delta.
	 * @throws Exception
	 */
	public void testBuilderDeltaUsingRelaxedRuleBug343256() throws Exception {
		final int timeout = 10000;
		String name = "testBuildDeltaUsingRelaxedRuleBug343256";
		setAutoBuilding(false);
		final IProject project = getWorkspace().getRoot().getProject(name);
		final IFile foo = project.getFile("foo");
		create(project, false);

		waitForEncodingRelatedJobs();
		waitForContentDescriptionUpdate();
		// wait for noBuildJob so POST_BUILD will fire
		((Workspace) getWorkspace()).getBuildManager().waitForAutoBuildOff();

		IProjectDescription desc = project.getDescription();
		desc.setBuildSpec(new ICommand[] { createCommand(desc, EmptyDeltaBuilder.BUILDER_NAME, "Project1Build1") });
		project.setDescription(desc, getMonitor());

		// Ensure the builder is instantiated
		project.build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());

		final TestBarrier2 tb = new TestBarrier2(TestBarrier2.STATUS_WAIT_FOR_START);
		AtomicReference<Throwable> error = new AtomicReference<>();

		Job workspaceChangingJob = new Job("Workspace Changing Job") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				tb.setStatus(TestBarrier2.STATUS_WAIT_FOR_RUN);
				ensureExistsInWorkspace(foo, new ByteArrayInputStream(new byte[0]));
				return Status.OK_STATUS;
			}
		};

		Job buildTriggeringJob = new Job("Build Triggering Job") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					getWorkspace().build(new IBuildConfiguration[] { project.getActiveBuildConfig() },
							IncrementalProjectBuilder.INCREMENTAL_BUILD, true, monitor);
				} catch (CoreException e) {
					IStatus status = e.getStatus();
					IStatus[] children = status.getChildren();
					if (children.length > 0) {
						error.set(children[0].getException());
					} else {
						error.set(e);
					}
				}
				return Status.OK_STATUS;
			}
		};

		// Create a builder set a null scheduling rule
		EmptyDeltaBuilder builder = EmptyDeltaBuilder.getInstance();

		// Set the rule call-back
		builder.setRuleCallback(new BuilderRuleCallback() {

			boolean called = false;

			@Override
			public ISchedulingRule getRule(String name, IncrementalProjectBuilder builder, int trigger, Map<String, String> args) {
				// Remove once Bug 331187 is fixed.
				// Currently #getRule is called twice when building a specific build configuration (so as to minimized change in
				// 3.7 end-game.  As this test is trying to provoke a bug in the window between fetching a rule and applying it
				// to the build, we don't want to run the first time #getRule is called (in Workspace#build)
				if (!called) {
					called = true;
					return project;
				}
				tb.setStatus(TestBarrier2.STATUS_START);
				tb.waitForStatus(TestBarrier2.STATUS_WAIT_FOR_RUN);
				// Wait for workspace changing job acquiring lock to ensure that it performs
				// workspace change after getRule released workspace lock and before build
				// re-acquired it
				boolean workspaceChangingJobAcquiringLock = waitForThreadStateWaiting(workspaceChangingJob.getThread());
				assertTrue("timed out waiting for workspace changing job to wait for workspace lock",
						workspaceChangingJobAcquiringLock);
				return project;
			}

			private boolean waitForThreadStateWaiting(Thread threadToWaitFor) {
				long startWaitingTime = System.currentTimeMillis();
				while (!(threadToWaitFor.getState() == State.TIMED_WAITING
						|| threadToWaitFor.getState() == State.WAITING)) {
					long elapsedWaitingTime = System.currentTimeMillis() - startWaitingTime;
					if (elapsedWaitingTime > timeout) {
						return false;
					}
				}
				return true;
			}

			@Override
			public IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
				// shared scheduling rule
				assertTrue(Job.getJobManager().currentRule().equals(project));
				// assert that the delta contains the file foo
				IResourceDelta delta = getDelta(project);
				assertNotNull("no workspace change occurred between getRule and build", delta);
				assertEquals("unexpected number of changes occurred: " + ((ResourceDelta) delta).toDeepDebugString(), 1,
						delta.getAffectedChildren().length);
				assertEquals("unexpected resource was changed", foo, delta.getAffectedChildren()[0].getResource());
				tb.setStatus(TestBarrier2.STATUS_DONE);
				return super.build(kind, args, monitor);
			}
		});

		// buildTriggeringJob runs, builds, calls BuilderRuleCallback, that waits
		// for workspaceChangingJob to wait for the workspace lock, such that it
		// performs a workspace change between getRule and build
		buildTriggeringJob.schedule();
		// Wait for the build to transition to getRule
		tb.waitForStatus(TestBarrier2.STATUS_START);
		// Modify a file in the project
		workspaceChangingJob.schedule();

		workspaceChangingJob.join(timeout, null);
		buildTriggeringJob.join(timeout, null);
		if (error.get() != null) {
			fail("Error observed", error.get());
		}
		tb.waitForStatus(TestBarrier2.STATUS_DONE);
		assertNoErrorsLogged();
	}

	/**
	 * Tests for regression in running the build with reduced scheduling rules.
	 * @throws Exception
	 */
	public void testBug343256() throws Exception {
		String name = "testBug343256";
		setAutoBuilding(false);
		final IProject project = getWorkspace().getRoot().getProject(name);
		create(project, false);

		IProjectDescription desc = project.getDescription();
		desc.setBuildSpec(new ICommand[] {createCommand(desc, EmptyDeltaBuilder.BUILDER_NAME, "Project1Build1"), createCommand(desc, EmptyDeltaBuilder2.BUILDER_NAME, "Project1Build2")});
		project.setDescription(desc, getMonitor());

		// Ensure the builder is instantiated
		project.build(IncrementalProjectBuilder.CLEAN_BUILD, getMonitor());

		final TestBarrier2 tb1 = new TestBarrier2(TestBarrier2.STATUS_WAIT_FOR_START);
		final TestBarrier2 tb2 = new TestBarrier2(TestBarrier2.STATUS_WAIT_FOR_START);

		// Scheduling rules to returng from #getRule
		final ISchedulingRule[] getRules = new ISchedulingRule[2];
		final ISchedulingRule[] buildRules = new ISchedulingRule[2];

		// Create a builder set a null scheduling rule
		EmptyDeltaBuilder builder = EmptyDeltaBuilder.getInstance();
		EmptyDeltaBuilder2 builder2 = EmptyDeltaBuilder2.getInstance();

		// Set the rule call-back
		builder.setRuleCallback(new BuilderRuleCallback() {
			@Override
			public ISchedulingRule getRule(String name, IncrementalProjectBuilder builder, int trigger, Map<String, String> args) {
				tb1.waitForStatus(TestBarrier2.STATUS_START);
				return getRules[0];
			}

			@Override
			public IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
				HashSet<ISchedulingRule> h1 = getRulesAsSet(Job.getJobManager().currentRule());
				HashSet<ISchedulingRule> h2 = getRulesAsSet(buildRules[0]);
				// shared scheduling rule
				assertTrue(h1.equals(h2));
				tb1.setStatus(TestBarrier2.STATUS_DONE);
				return super.build(kind, args, monitor);
			}
		});
		// Set the rule call-back
		builder2.setRuleCallback(new BuilderRuleCallback() {
			@Override
			public ISchedulingRule getRule(String name, IncrementalProjectBuilder builder, int trigger, Map<String, String> args) {
				tb2.waitForStatus(TestBarrier2.STATUS_START);
				return getRules[1];
			}

			@Override
			public IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
				HashSet<ISchedulingRule> h1 = getRulesAsSet(Job.getJobManager().currentRule());
				HashSet<ISchedulingRule> h2 = getRulesAsSet(buildRules[1]);
				assertTrue(h1.equals(h2));
				tb2.setStatus(TestBarrier2.STATUS_DONE);
				return super.build(kind, args, monitor);
			}
		});

		Job j;

		// Enable for Bug 331187
		//		// IProject.build()
		//		j = new Job("IProject.build()") {
		//			protected IStatus run(IProgressMonitor monitor) {
		//				try {
		//					project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
		//				} catch (CoreException e) {
		//					fail(e.toString());
		//				}
		//				return Status.OK_STATUS;
		//			}
		//		};
		//		invokeTestBug343256(project, getRules, buildRules, tb1, tb2, j);

		//		// IWorkspace.build()
		//		j = new Job("IWorkspace.build()") {
		//			protected IStatus run(IProgressMonitor monitor) {
		//				try {
		//					getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, monitor);
		//				} catch (CoreException e) {
		//					fail(e.toString());
		//				}
		//				return Status.OK_STATUS;
		//			}
		//		};
		//		invokeTestBug343256(project, getRules, buildRules, tb1, tb2, j);

		// IWorkspace.build(IBuildConfiguration[],...)
		j = new Job("IWorkspace.build(IBuildConfiguration[],...)") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					getWorkspace().build(new IBuildConfiguration[] {project.getActiveBuildConfig()}, IncrementalProjectBuilder.FULL_BUILD, true, monitor);
				} catch (CoreException e) {
					fail(e.toString());
				}
				return Status.OK_STATUS;
			}
		};
		invokeTestBug343256(project, getRules, buildRules, tb1, tb2, j);

		// Test Auto-build
		//		j = new Job("Auto-build") {
		//			protected IStatus run(IProgressMonitor monitor) {
		//				try {
		//					getWorkspace().build(IncrementalProjectBuilder.CLEAN_BUILD, getMonitor());
		//				} catch (CoreException e) {
		//					fail(e.toString());
		//				}
		//				return Status.OK_STATUS;
		//			}
		//		};
		//		// Auto-build
		//		setAutoBuilding(true);
		//		// Wait for the build to transition
		//		invokeTestBug343256(project, getRules, buildRules, tb1, tb2, j);
	}

	/**
	 * Helper method do invoke a set of tests on Bug343256 using the different sets of builder API
	 */
	private void invokeTestBug343256(IProject project, ISchedulingRule[] getRules, ISchedulingRule[] buildRules, TestBarrier2 tb1, TestBarrier2 tb2, Job j) {
		// Test 1 - build project sched rule
		getRules[0] = getRules[1] = project;
		buildRules[0] = buildRules[1] = new MultiRule(new ISchedulingRule[] {getRules[0]});
		tb1.setStatus(TestBarrier2.STATUS_START);
		tb2.setStatus(TestBarrier2.STATUS_START);
		j.schedule();
		tb1.waitForStatus(TestBarrier2.STATUS_DONE);
		tb2.waitForStatus(TestBarrier2.STATUS_DONE);

		// Test 2 - build null rule
		getRules[0] = getRules[1] = null;
		buildRules[0] = buildRules[1] = null;
		tb1.setStatus(TestBarrier2.STATUS_START);
		tb2.setStatus(TestBarrier2.STATUS_START);
		j.schedule();
		tb1.waitForStatus(TestBarrier2.STATUS_DONE);
		tb2.waitForStatus(TestBarrier2.STATUS_DONE);

		// Test 3 - build mixed projects
		getRules[0] = buildRules[0] = project;
		getRules[1] = buildRules[1] = getWorkspace().getRoot().getProject("other");
		tb1.setStatus(TestBarrier2.STATUS_START);
		tb2.setStatus(TestBarrier2.STATUS_START);
		j.schedule();
		tb1.waitForStatus(TestBarrier2.STATUS_DONE);
		tb2.waitForStatus(TestBarrier2.STATUS_DONE);

		// Test 4 - build project + null
		getRules[0] = buildRules[0] = project;
		getRules[1] = buildRules[1] = null;
		// TODO: Fixed in Bug 331187 ; BuildManager#getRule is pessimistic when there's a mixed resource and null rule
		buildRules[0] = buildRules[1] = getWorkspace().getRoot();
		tb1.setStatus(TestBarrier2.STATUS_START);
		tb2.setStatus(TestBarrier2.STATUS_START);
		j.schedule();
		tb1.waitForStatus(TestBarrier2.STATUS_DONE);
		tb2.waitForStatus(TestBarrier2.STATUS_DONE);
	}
}
