/*******************************************************************************
 * Copyright (c) 2022 Andrey Loskutov (loskutov@gmx.de) and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov (loskutov@gmx.de) - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.internal.builders;

import static org.junit.Assert.assertThrows;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.core.internal.events.BuildManager;
import org.eclipse.core.internal.events.BuildManager.JobManagerSuspendedException;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.core.tests.internal.builders.TestBuilder.BuilderRuleCallback;
import org.junit.Test;

/**
 * Test for various AutoBuildJob scheduling use cases
 */
public class AutoBuildJobTest extends AbstractBuilderTest {

	private IProject project;
	private AtomicLong running;
	private AtomicLong scheduled;

	public AutoBuildJobTest(String name) {
		super(name);
	}

	IJobChangeListener jobChangeListener = new JobChangeAdapter() {

		@Override
		public void scheduled(IJobChangeEvent event) {
			if (event.getJob().belongsTo(ResourcesPlugin.FAMILY_AUTO_BUILD)) {
				scheduled.incrementAndGet();
			}
		}

		@Override
		public void running(IJobChangeEvent event) {
			if (event.getJob().belongsTo(ResourcesPlugin.FAMILY_AUTO_BUILD)) {
				running.incrementAndGet();
			}
		}
	};

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		scheduled = new AtomicLong(0);
		running = new AtomicLong(0);
		setupProjectWithOurBuilder();
		setAutoBuilding(true);
		Job.getJobManager().addJobChangeListener(jobChangeListener);
	}

	@Override
	protected void tearDown() throws Exception {
		Job.getJobManager().removeJobChangeListener(jobChangeListener);
		super.tearDown();
	}

	private void setupProjectWithOurBuilder() throws CoreException {
		project = getWorkspace().getRoot().getProject(getName());
		project.create(getMonitor());
		project.open(getMonitor());
		IProjectDescription desc = project.getDescription();
		desc.setBuildSpec(new ICommand[] { createCommand(desc, EmptyDeltaBuilder.BUILDER_NAME, getName()) });
		project.setDescription(desc, getMonitor());
	}

	private void requestAutoBuildJobExecution() {
		// Simulates autobuild job triggering from build thread
		// basically same as autoBuildJob.build(true);
		getBuildManager().endTopLevel(true);
	}

	private BuildManager getBuildManager() {
		return ((Workspace) project.getWorkspace()).getBuildManager();
	}

	public void testNoBuildIfBuildRequestedFromSameThread() throws Exception {
		triggerAutobuildAndCheckNoExtraBuild(false);
	}

	public void testNoBuildIfBuildRequestedFromSameThreadAfterCancel() throws Exception {
		triggerAutobuildAndCheckNoExtraBuild(true);
	}

	private void triggerAutobuildAndCheckNoExtraBuild(boolean cancel) throws Exception {
		AtomicBoolean cancelled = new AtomicBoolean();
		EmptyDeltaBuilder.getInstance().setRuleCallback(new BuilderRuleCallback() {
			@Override
			public IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
				if (cancel) {
					monitor.setCanceled(true);
					cancelled.set(true);
				}
				// should be ignored
				requestAutoBuildJobExecution();
				return new IProject[0];
			}
		});

		triggerAutoBuildAndWait();
		Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
		assertEquals("Should see one scheduled() call", 1, scheduled.get());
		assertEquals("Should see one running() call", 1, running.get());

		if (cancel) {
			assertEquals(true, cancelled.get());
		}
	}

	public void testExtraBuildIfBuildRequestedFromOtherThreadDuringRun() throws Exception {
		EmptyDeltaBuilder.getInstance().setRuleCallback(new BuilderRuleCallback() {
			@Override
			public IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
				// Simulate someone requests autobuild in parallel to already running autobuild.
				// That shouldn't be ignored, otherwise no build may happen
				Job job = Job.createSystem("", (ICoreRunnable) m -> requestAutoBuildJobExecution());
				job.schedule();
				try {
					job.join();
				} catch (InterruptedException e) {
					//
				}
				return new IProject[0];
			}
		});

		triggerAutoBuildAndWait();
		Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
		assertEquals("Should see two scheduled() calls", 2, scheduled.get());
		assertEquals("Should see two running() calls", 2, running.get());
	}

	@Test
	public void testWaitForAutoBuild_JobManagerIsSuspended_ExceptionIsThrown()
			throws InterruptedException, CoreException, ExecutionException {
		Job.getJobManager().suspend();

		assertEquals("Scheduled calls", 0, scheduled.get());
		assertEquals("Running calls", 0, running.get());

		triggerAutoBuildAndWait();
		Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);

		assertEquals("Scheduled calls", 1, scheduled.get());
		assertEquals("Running calls", 0, running.get());

		assertThrows(JobManagerSuspendedException.class, () -> waitForAutoBuild(2_000));

		Job.getJobManager().resume();
	}

	@Test
	public void testWaitForAutoBuild_JobManagerIsRunning_NoExceptionIsThrown()
			throws Throwable {
		assertEquals("Scheduled calls", 0, scheduled.get());
		assertEquals("Running calls", 0, running.get());

		triggerAutoBuildAndWait();
		Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);

		assertEquals("Scheduled calls", 1, scheduled.get());
		assertEquals("Running calls", 1, running.get());

		waitForAutoBuild(2_000);
	}

	/**
	 * Trigger an auto-build and wait for it to start.
	 *
	 * @throws CoreException
	 * @throws InterruptedException
	 */
	private void triggerAutoBuildAndWait() throws CoreException, InterruptedException {
		project.touch(getMonitor());
		Thread.sleep(Policy.MAX_BUILD_DELAY);
	}

	/**
	 * Wait for an auto-build operation to finish.
	 *
	 * @param timeoutMillis
	 *            after this timeout, a <code>TimeoutException</code> will be
	 *            thrown.
	 * @return the exception thrown by {@linkplain BuildManager#waitForAutoBuild()},
	 *         it can be <code>null</code>.
	 * @throws InterruptedException
	 *             if the thread waiting for the auto-build is interrupted.
	 */
	private void waitForAutoBuild(long timeoutMillis) throws Throwable {
		try {
			ForkJoinPool.commonPool()//
					.submit(() -> getBuildManager().waitForAutoBuild())//
					.get(timeoutMillis, TimeUnit.MILLISECONDS);
		} catch (ExecutionException e) {
			// Since the wait is happening in a Future, the original exception is packed
			// inside the ExecutionException
			throw e.getCause();
		} catch (TimeoutException e) {
			fail("This test timed out which means there is no safeguard to avoid waiting indefinitely "
					+ "for an auto-build job while the JobManager is suspended", e);
		}
	}

}