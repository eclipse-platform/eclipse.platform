/*******************************************************************************
 * Copyright (c) 2022 Stefan Winkler and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stefan Winkler - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.internal.builders;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.core.tests.harness.TestBarrier2;
import org.eclipse.core.tests.internal.builders.TestBuilder.BuilderRuleCallback;

/**
 * Test for #158
 * (https://github.com/eclipse-platform/eclipse.platform/issues/158).
 *
 * The basic problem case of this bug seems to be a race condition in which a
 * thread modifies the workspace and requests a build while the AutoBuildJob is
 * running.
 *
 * To test this, we use a somewhat illegal {@link ISchedulingRule} that makes it
 * possible to actually modify the workspace while the build is running. We are
 * using the {@link TestBarrier2} mechanism to synchonize the two concurrent
 * threads to get a deterministic flow of events.
 */
public class AutoBuildJobRescheduleTest extends AbstractBuilderTest {
	// state constants to be used for the barrier
	private static final int AUTOBUILD_RUNNING_BLOCKED = 1;
	private static final int AUTOBUILD_CONTINUE = 2;
	private static final int AUTOBUILD_DONE = 3;

	/**
	 * The barrier to synchronize the builder and the concurrent workspace
	 * modification
	 */
	private TestBarrier2 barrier = new TestBarrier2();

	/**
	 * A file in the workspace that we modify to trigger builds
	 */
	private IFile file;

	/**
	 * A thread-safe flag to record the rescheduling of the AutoBuildJob
	 */
	private AtomicBoolean rescheduled = new AtomicBoolean(false);

	/**
	 * A somewhat illegal {@link ISchedulingRule} which satisfies the sanity checks
	 * in {@code JobManager.validateRule()} but still does not prevent that
	 * {@link AutoBuildJobRescheduleTest#file} is modified while the
	 * {@code AutoBuildJob} is running.
	 *
	 * @since 3.2
	 */
	public class SpoiledSchedulingRule implements ISchedulingRule {
		@Override
		public boolean isConflicting(ISchedulingRule rule) {
			return rule == this || rule == file;
		}

		@Override
		public boolean contains(ISchedulingRule rule) {
			return rule == this || rule == file;
		}
	}

	/**
	 * An {@link IJobChangeListener} that reacts to the AutoBuildJob being scheduled
	 * or done.
	 *
	 * Since the AutoBuildJob is not visible, we check against the class name.
	 */
	private IJobChangeListener jobChangeListener = new JobChangeAdapter() {
		@Override
		public void scheduled(IJobChangeEvent event) {
			if ("AutoBuildJob".equals(event.getJob().getClass().getSimpleName())) {
				rescheduled.set(true);
			}
		}

		@Override
		public void done(IJobChangeEvent event) {
			if ("AutoBuildJob".equals(event.getJob().getClass().getSimpleName())) {
				barrier.setStatus(AUTOBUILD_DONE);
			}
		}
	};

	public AutoBuildJobRescheduleTest(String testName) {
		super(testName);
	}

	/**
	 * Setup the test by creating a project and a file. We also add a special
	 * builder that suspends execution using the {@link #barrier}.
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();

		var project = getWorkspace().getRoot().getProject("PROJECT");
		file = project.getFile("file");
		try {
			// Turn auto-building off to prevent too early builds
			setAutoBuilding(false);
			// Create and open the project
			project.create(getMonitor());
			project.open(getMonitor());
			// Create the file
			ensureExistsInWorkspace(file, getRandomContents());

			// Add the EmptyDeltaBuilder. The behavior of that builder is configured below.
			IProjectDescription desc = project.getDescription();
			desc.setBuildSpec(new ICommand[] { createCommand(desc, EmptyDeltaBuilder.BUILDER_NAME, "ProjectBuild") });
			project.setDescription(desc, getMonitor());
			project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());

			// Perform the first build. This instantiates the EmptyDeltaBuilder and makes
			// its instance accessible.
			setAutoBuilding(true);

			// Configure the behavior of the builder
			EmptyDeltaBuilder.getInstance().setRuleCallback(new BuilderRuleCallback() {
				// We only execute the block-continue on the first run in the test case.
				// Therefore, we remember whether we have already run with this flag.
				private AtomicBoolean buildRan = new AtomicBoolean(false);

				@Override
				public IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor)
						throws CoreException {
					// only on the first run, block the builder
					if (!buildRan.getAndSet(true)) {
						barrier.setStatus(AUTOBUILD_RUNNING_BLOCKED);
						// block until the WorkspaceJob has made the file dirty
						barrier.waitForStatus(AUTOBUILD_CONTINUE);
					}
					return new IProject[] { project };
				}
			});

			// Wait until the Content Description Update job has finished, because it also
			// might schedule a build and we don't want that interfere with our test
			// process.
			waitForContentDescriptionUpdate();
		} catch (CoreException e) {
			fail("Unexpected exception in project setup", e);
		}
	}

	@Override
	protected void tearDown() throws Exception {
		Job.getJobManager().removeJobChangeListener(jobChangeListener);

		super.tearDown();
	}

	public void testAutoBuildRescheduling() throws InterruptedException, CoreException {
		// trigger an autobuild. This will execute the builder we have configured above
		// to block.
		dirty(file);

		// Let a concurrent job modify the file
		var job = new WorkspaceJob("ModifyFile") {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				// Make sure the builder is blocking
				barrier.waitForStatus(AUTOBUILD_RUNNING_BLOCKED);
				// Now modify the file - this requests another build
				dirty(file);
				// Register our jobChangeListener so we can record when the AutoBuildJob is
				// rescheduled and done.
				Job.getJobManager().addJobChangeListener(jobChangeListener);
				// Signal to the builder that it can continue now
				barrier.setStatus(AUTOBUILD_CONTINUE);
				return Status.OK_STATUS;
			}
		};
		job.setRule(new SpoiledSchedulingRule());
		job.schedule();
		job.join();

		// Wait for the autobuild job to finish
		barrier.waitForStatus(AUTOBUILD_DONE);

		// Verify that it has been rescheduled
		assertTrue("AutoBuildJob was not rescheduled", rescheduled.get());
	}
}
