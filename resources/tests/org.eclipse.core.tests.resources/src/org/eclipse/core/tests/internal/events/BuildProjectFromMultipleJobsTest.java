/*******************************************************************************
 * Copyright (c) 2017 Simeon Andreev and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Simeon Andreev <simeon.danailov.andreev@gmail.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.internal.events;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.setAutoBuilding;
import static org.eclipse.core.tests.resources.ResourceTestUtil.updateProjectDescription;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.eclipse.core.internal.events.BuildCommand;
import org.eclipse.core.internal.resources.Project;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.tests.internal.builders.ConfigurationBuilder;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.eclipse.core.tests.resources.regression.SimpleBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests that triggering a project build from multiple jobs does not cause assertion failures,
 * e.g. due to adding builders to the {@link BuildCommand} in parallel.
 *
 * @see <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=517411">Eclipse bug 517411</a>
 */
public class BuildProjectFromMultipleJobsTest {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	private static final String TEST_PROJECT_NAME = "ProjectForBuildCommandTest";

	private final ErrorLogListener logListener = new ErrorLogListener();

	@Before
	public void setUp() throws Exception {
		// auto-build makes reproducing the problem harder,
		// since it may build before we trigger parallel builds from the test
		setAutoBuilding(false);
		Platform.addLogListener(logListener);
	}

	@After
	public void tearDown() throws Exception {
		Job.getJobManager().cancel(BuildTestProject.class);

		Platform.removeLogListener(logListener);
		logListener.clear();
	}

	/**
	 * Creates a project with no contents and a builder, and triggers a project
	 * build from multiple jobs. Checks that no
	 * {@link org.eclipse.core.runtime.AssertionFailedException} were logged during
	 * the builds.
	 *
	 * Repeats this several times, to ensure that no exceptions are thrown due to
	 * the build from parallel threads.
	 *
	 * @see <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=517411">Eclipse
	 *      bug 517411</a>
	 */
	@Test
	public void test10IterationsWithBuildsFrom8Jobs() throws Exception {
		IProgressMonitor monitor = new NullProgressMonitor();

		int iterations = 10;
		int jobs = 8;

		for (int i = 0; i < iterations; ++i) {
			IProject project = createTestProject(SimpleBuilder.BUILDER_ID, monitor);

			List<BuildTestProject> buildJobs = new ArrayList<>();
			for (int j = 0; j < jobs; ++j) {
				BuildTestProject buildTestProject = new BuildTestProject(project, j);
				buildJobs.add(buildTestProject);
			}

			for (BuildTestProject buildJob : buildJobs) {
				buildJob.schedule();
			}

			for (BuildTestProject buildJob : buildJobs) {
				buildJob.join();
			}

			project.delete(true, monitor);

			String errorMessage = "Building in parallel encountered an exception in iteration " + i;
			logListener.assertNoExceptionsWereLogged(errorMessage);
		}
	}


	/**
	 * Tests that modifying {@link BuildCommand#getBuilders()} map does not allow to modify internal state of the command.
	 */
	@Test
	public void testBuildersAreNotModifiable() throws Exception {
		Project project = (Project) createTestProject(ConfigurationBuilder.BUILDER_NAME, null);
		project.build(IncrementalProjectBuilder.FULL_BUILD, null);

		// Get a non-cloned version of the project desc build spec
		BuildCommand buildCommand = (BuildCommand) project.internalGetDescription().getBuildSpec(false)[0];
		assertThat(buildCommand.getBuilders(), instanceOf(Map.class));
		if (buildCommand.getBuilders() instanceof Map<?, ?> buildersMap) {
			assertThat(buildersMap.entrySet(), hasSize(1));
			// Try to change the internal data
			buildersMap.clear();
			assertThat(buildersMap.entrySet(), hasSize(0));
		}

		assertThat(buildCommand.getBuilders(), instanceOf(Map.class));
		if (buildCommand.getBuilders() instanceof Map<?, ?> buildersMap) {
			// Should still be OK
			assertThat("BuildCommand state was changed!", buildersMap.entrySet(), hasSize(1));
		}
	}

	private IProject createTestProject(String builderId, IProgressMonitor monitor) throws CoreException {
		IProject project = getTestProject();
		assertFalse("Expected test project to not exist at beginning of test", project.exists());

		createInWorkspace(project);
		assertTrue("Expected test project to be open after creation", project.isOpen());

		// add some builder to the project, so that we can run into the concurrency problem
		updateProjectDescription(project).addingCommand(builderId).apply();

		return project;
	}

	private static IProject getTestProject() {
		IWorkspaceRoot workspaceRoot = getWorkspace().getRoot();
		IProject project = workspaceRoot.getProject(TEST_PROJECT_NAME);
		return project;
	}

	private static class ErrorLogListener implements ILogListener {

		private final List<Throwable> loggedExceptions;

		public ErrorLogListener() {
			loggedExceptions = new ArrayList<>();
		}

		@Override
		public void logging(IStatus status, String plugin) {
			Throwable statusException = status.getException();
			loggedExceptions.add(statusException);
		}

		void assertNoExceptionsWereLogged(String errorMessage) {
			for (Throwable loggedException : loggedExceptions) {
				throw new AssertionError(errorMessage, loggedException);
			}
		}

		void clear() {
			loggedExceptions.clear();
		}
	}

	private static class BuildTestProject extends Job {

		private final IProject project;

		public BuildTestProject(IProject project, int number) {
			super("build test project " + number);
			this.project = project;
		}

		@Override
		protected IStatus run(IProgressMonitor jobMonitor) {
			try {
				if (!jobMonitor.isCanceled()) {
					project.build(IncrementalProjectBuilder.FULL_BUILD, jobMonitor);
				}
			} catch (CoreException e) {
				return e.getStatus();
			}
			return new Status(IStatus.OK, PI_RESOURCES_TESTS, getName() + " finished");
		}

		@Override
		public boolean belongsTo(Object family) {
			return BuildTestProject.class == family;
		}
	}
}
