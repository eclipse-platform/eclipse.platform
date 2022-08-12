/*******************************************************************************
 * Copyright (c) 2004, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.internal.resources;

import java.util.concurrent.atomic.AtomicIntegerArray;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.tests.harness.CancelingProgressMonitor;
import org.eclipse.core.tests.harness.TestBarrier2;
import org.eclipse.core.tests.resources.ResourceTest;

/**
 * Tests concurrency issues when dealing with operations on the workspace
 */
public class WorkspaceConcurrencyTest extends ResourceTest {

	private void sleep(long duration) {
		try {
			Thread.sleep(duration);
		} catch (InterruptedException e) {
			//ignore
		}
	}

	public void testEndRuleInWorkspaceOperation() {
		try {
			final IProject project = getWorkspace().getRoot().getProject("testEndRuleInWorkspaceOperation");
			getWorkspace().run((IWorkspaceRunnable) monitor -> Job.getJobManager().endRule(project), project, IResource.NONE, getMonitor());
			//should have failed
			fail("1.0");
		} catch (CoreException e) {
			fail("1.99", e);
		} catch (RuntimeException e) {
			//expected
		}
	}

	/**
	 * Tests that it is possible to cancel a workspace operation when it is blocked
	 * by activity in another thread. This is a regression test for bug 56118.
	 */
	public void testCancelOnBlocked() {
		//create a dummy project
		ensureExistsInWorkspace(getWorkspace().getRoot().getProject("P1"), true);
		//add a resource change listener that blocks forever, thus
		//simulating a scenario where workspace lock is held indefinitely
		final AtomicIntegerArray barrier = new AtomicIntegerArray(new int[1]);
		final Throwable[] error = new Throwable[1];
		IResourceChangeListener listener = event -> {
			//block until we are told to do otherwise
			barrier.set(0, TestBarrier2.STATUS_START);
			try {
				TestBarrier2.waitForStatus(barrier, TestBarrier2.STATUS_DONE);
			} catch (Throwable e) {
				error[0] = e;
			}
		};
		getWorkspace().addResourceChangeListener(listener);
		try {
			//create a thread that modifies the workspace. This should
			//hang indefinitely due to the misbehaving listener
			TestWorkspaceJob testJob = new TestWorkspaceJob(10);
			testJob.setTouch(true);
			testJob.setRule(getWorkspace().getRoot());
			testJob.schedule();

			//wait until blocked on the listener
			TestBarrier2.waitForStatus(barrier, TestBarrier2.STATUS_START);

			//create a second thread that attempts to modify the workspace, but immediately
			//cancels itself. This thread should terminate immediately with a cancelation exception
			final boolean[] canceled = new boolean[] {false};
			Thread t2 = new Thread(() -> {
				try {
					getWorkspace().run((IWorkspaceRunnable) monitor -> {
						//no-op
					}, new CancelingProgressMonitor());
				} catch (CoreException e1) {
					fail("1.99", e1);
				} catch (OperationCanceledException e2) {
					canceled[0] = true;
				}
			});
			t2.start();
			try {
				t2.join();
			} catch (InterruptedException e) {
				fail("1.88", e);
			}
			//should have canceled
			assertTrue("2.0", canceled[0]);

			//finally release the listener and ensure the first thread completes
			barrier.set(0, TestBarrier2.STATUS_DONE);
			try {
				testJob.join();
			} catch (InterruptedException e1) {
				//ignore
			}
			if (error[0] != null) {
				fail("3.0", error[0]);
			}
		} finally {
			getWorkspace().removeResourceChangeListener(listener);
		}
	}

	/**
	 * Tests calling IWorkspace.run with a non-workspace rule.  This should be
	 * allowed. This is a regression test for bug 60114.
	 */
	public void testRunnableWithOtherRule() {
		ISchedulingRule rule = new ISchedulingRule() {
			@Override
			public boolean contains(ISchedulingRule rule) {
				return rule == this;
			}

			@Override
			public boolean isConflicting(ISchedulingRule rule) {
				return rule == this;
			}
		};
		try {
			getWorkspace().run((IWorkspaceRunnable) monitor -> {
				//noop
			}, rule, IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("1.99", e);
		}
	}

	/**
	 * Tests three overlapping jobs
	 *  - Job 1 (root rule) does a build.
	 *  - Job 2 (null rule) overlaps Job 1 and has null scheduling rule
	 *  - Job 3 (project rule) overlaps Job 2
	 *
	 * The POST_BUILD event should occur at the end of Job 1. If it
	 * is delayed until the end of Job 3, an appropriate scheduling rule
	 * will not be available and it will fail.
	 * This is a regression test for bug 	62927.
	 */
	public void testRunWhileBuilding() {
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		//create a POST_BUILD listener that will touch a project
		final IProject touch = workspace.getRoot().getProject("ToTouch");
		final IProject rule = workspace.getRoot().getProject("jobThree");
		final IFile ruleFile = rule.getFile("somefile.txt");
		ensureExistsInWorkspace(rule, true);
		ensureExistsInWorkspace(touch, true);
		ensureExistsInWorkspace(ruleFile, true);
		final Throwable[] failure = new Throwable[1];
		IResourceChangeListener listener = event -> {
			try {
				touch.touch(null);
			} catch (CoreException | RuntimeException e2) {
				failure[0] = e2;
			}
		};
		workspace.addResourceChangeListener(listener, IResourceChangeEvent.POST_BUILD);
		try {
			//create one job that does a build, and then waits
			final AtomicIntegerArray status = new AtomicIntegerArray(new int[3]);
			Job jobOne = new Job("jobOne") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						workspace.run((IWorkspaceRunnable) monitor1 -> {
							//do a build
							workspace.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor1);
							//signal that the job has done the build
							status.set(0, TestBarrier2.STATUS_RUNNING);
							//wait for job two to start
							TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_WAIT_FOR_DONE);
						}, null);
					} catch (CoreException e) {
						return e.getStatus();
					}
					return Status.OK_STATUS;
				}
			};
			//schedule and wait for job one to start
			jobOne.schedule();
			TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_RUNNING);

			//create job two that does an empty workspace operation
			Job jobTwo = new Job("jobTwo") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						workspace.run((IWorkspaceRunnable) monitor1 -> {
							//signal that this job has started
							status.set(1, TestBarrier2.STATUS_RUNNING);
							//let job one finish
							status.set(0, TestBarrier2.STATUS_WAIT_FOR_DONE);
							//wait for job three to start
							TestBarrier2.waitForStatus(status, 1, TestBarrier2.STATUS_WAIT_FOR_DONE);
						}, null, IResource.NONE, null);
					} catch (CoreException e) {
						return e.getStatus();
					}
					return Status.OK_STATUS;
				}
			};
			jobTwo.schedule();
			//create job three that has a non-null rule
			Job jobThree = new Job("jobThree") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						workspace.run((IWorkspaceRunnable) monitor1 -> {
							//signal that this job has started
							status.set(2, TestBarrier2.STATUS_RUNNING);
							//let job two finish
							status.set(1, TestBarrier2.STATUS_WAIT_FOR_DONE);
							//ensure this job does something so the build listener runs
							ruleFile.touch(null);
							//wait for the ok to complete
							TestBarrier2.waitForStatus(status, 2, TestBarrier2.STATUS_WAIT_FOR_DONE);
						}, workspace.getRuleFactory().modifyRule(ruleFile), IResource.NONE, null);
					} catch (CoreException e) {
						return e.getStatus();
					}
					return Status.OK_STATUS;
				}
			};
			jobThree.schedule();
			//wait for job two to complete
			waitForCompletion(jobTwo);

			//let job three complete
			status.set(2, TestBarrier2.STATUS_WAIT_FOR_DONE);

			//wait for job three to complete
			waitForCompletion(jobThree);

			//ensure no jobs failed
			IStatus result = jobOne.getResult();
			if (!result.isOK()) {
				fail("1.0", new CoreException(result));
			}
			result = jobTwo.getResult();
			if (!result.isOK()) {
				fail("1.1", new CoreException(result));
			}
			result = jobThree.getResult();
			if (!result.isOK()) {
				fail("1.2", new CoreException(result));
			}

			if (failure[0] != null) {
				fail("1.3", failure[0]);
			}
		} finally {
			//ensure listener is removed
			workspace.removeResourceChangeListener(listener);
		}
	}

	private void waitForCompletion(Job job) {
		int i = 0;
		while (job.getState() != Job.NONE) {
			sleep(100);
			//sanity test to avoid hanging tests
			assertTrue("Timeout waiting for job to complete", i++ < 1000);
		}
	}
}
