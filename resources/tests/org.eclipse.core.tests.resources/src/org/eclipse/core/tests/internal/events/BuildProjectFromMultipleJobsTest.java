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

import java.util.*;
import org.eclipse.core.internal.events.BuildCommand;
import org.eclipse.core.internal.resources.Project;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.tests.internal.builders.ConfigurationBuilder;
import org.eclipse.core.tests.resources.AutomatedResourceTests;
import org.eclipse.core.tests.resources.ResourceTest;
import org.eclipse.core.tests.resources.regression.SimpleBuilder;

/**
 * Tests that triggering a project build from multiple jobs does not cause assertion failures,
 * e.g. due to adding builders to the {@link BuildCommand} in parallel.
 *
 * @see <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=517411">Eclipse bug 517411</a>
 */
public class BuildProjectFromMultipleJobsTest extends ResourceTest {

	private static final String TEST_PROJECT_NAME = "ProjectForBuildCommandTest";

	private final ErrorLogListener logListener = new ErrorLogListener();
	private boolean wasAutoBuildOn;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		// auto-build makes reproducing the problem harder,
		// since it may build before we trigger parallel builds from the test
		wasAutoBuildOn = setWorkspaceAutoBuild(false);

		Platform.addLogListener(logListener);
	}

	@Override
	protected void tearDown() throws Exception {
		Job.getJobManager().cancel(BuildTestProject.class);

		Platform.removeLogListener(logListener);
		logListener.clear();

		try {
			IProject testProject = getTestProject();
			if (testProject.exists()) {
				testProject.delete(true, null);
			}
		} finally {
			setWorkspaceAutoBuild(wasAutoBuildOn);
		}

		super.tearDown();
	}

	/**
	 * Creates a project with no contents and a builder, and triggers a project build from multiple jobs.
	 * Checks that no {@link AssertionFailedException} were logged during the builds.
	 *
	 * Repeats this several times, to ensure that no exceptions are thrown due to the build from parallel threads.
	 *
	 * @see <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=517411">Eclipse bug 517411</a>
	 */
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
	@SuppressWarnings("rawtypes")
	public void testBuildersAreNotModifiable() throws Exception {
		Project project = (Project) createTestProject(ConfigurationBuilder.BUILDER_NAME, null);
		project.build(IncrementalProjectBuilder.FULL_BUILD, null);

		// Get a non-cloned version of the project desc build spec
		BuildCommand buildCommand = (BuildCommand) project.internalGetDescription().getBuildSpec(false)[0];
		Map buildersMap = (Map) buildCommand.getBuilders();
		assertEquals(1, buildersMap.size());

		// Try to change the internal data
		buildersMap.clear();
		assertEquals(0, buildersMap.size());

		// Should still be OK
		buildersMap = (Map) buildCommand.getBuilders();
		assertEquals("BuildCommand state was changed!", 1, buildersMap.size());
	}

	private IProject createTestProject(String builderId, IProgressMonitor monitor) throws CoreException {
		IProject project = getTestProject();
		assertFalse("Expected test project to not exist at beginning of test", project.exists());

		ensureExistsInWorkspace(project, true);
		assertTrue("Expected test project to be open after creation", project.isOpen());

		// add some builder to the project, so that we can run into the concurrency problem
		IProjectDescription projectDescription = project.getDescription();
		ICommand[] buildSpec = projectDescription.getBuildSpec();
		ICommand command = projectDescription.newCommand();
		command.setBuilderName(builderId);
		Collection<ICommand> builders = new ArrayList<>(Arrays.asList(buildSpec));
		builders.add(command);
		projectDescription.setBuildSpec(builders.toArray(new ICommand[] {}));
		project.setDescription(projectDescription, monitor);

		return project;
	}

	private static IProject getTestProject() {
		IWorkspaceRoot workspaceRoot = getWorkspace().getRoot();
		IProject project = workspaceRoot.getProject(TEST_PROJECT_NAME);
		return project;
	}

	private static boolean setWorkspaceAutoBuild(boolean autobuildOn) throws CoreException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final IWorkspaceDescription description = workspace.getDescription();
		boolean oldAutoBuildingState = description.isAutoBuilding();
		if (oldAutoBuildingState != autobuildOn) {
			description.setAutoBuilding(autobuildOn);
			workspace.setDescription(description);
		}
		return oldAutoBuildingState;
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
			return new Status(IStatus.OK, AutomatedResourceTests.PI_RESOURCES_TESTS, getName() + " finished");
		}

		@Override
		public boolean belongsTo(Object family) {
			return BuildTestProject.class == family;
		}
	}
}
