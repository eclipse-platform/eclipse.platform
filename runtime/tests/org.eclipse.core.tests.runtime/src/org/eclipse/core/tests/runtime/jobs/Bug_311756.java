/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.runtime.jobs;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.ProgressMonitorWrapper;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.tests.harness.TestBarrier2;
import org.junit.Test;

/**
 * Make sure that IProgressMonitor's blocked/unblocked is invoked.
 */
public class Bug_311756 extends AbstractJobTest {

	int UNSET = -1;
	int CLEARED = 0;
	int BLOCKED = 1;

	/**
	 * Tests that the progress monitor blocked state is cleared in the normal case
	 * that the rule becomes available after being blocked.
	 */
	@Test
	public void testBlockingAndUnblockingMonitor() throws Exception {
		final int[] blocked = new int[] {UNSET};
		ProgressMonitorWrapper wrapper = new ProgressMonitorWrapper(new NullProgressMonitor()) {
			@Override
			public void setBlocked(IStatus reason) {
				super.setBlocked(reason);
				blocked[0] = BLOCKED;
			}

			@Override
			public void clearBlocked() {
				super.clearBlocked();
				if (blocked[0] == BLOCKED) {
					blocked[0] = CLEARED;
				}
			}
		};
		final TestBarrier2 barrier = new TestBarrier2(TestBarrier2.STATUS_START);
		IdentityRule rule = new IdentityRule();
		AtomicReference<InterruptedException> exceptionInJob = new AtomicReference<>();
		Job conflicting = new Job("Conflicting") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				barrier.setStatus(TestBarrier2.STATUS_RUNNING);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					exceptionInJob.set(e);
				}
				return Status.OK_STATUS;
			}
		};
		conflicting.setRule(rule);
		conflicting.schedule();

		barrier.waitForStatus(TestBarrier2.STATUS_RUNNING);
		try {
			Job.getJobManager().beginRule(rule, wrapper);
		} finally {
			Job.getJobManager().endRule(rule);
		}
		if (exceptionInJob.get() != null) {
			throw exceptionInJob.get();
		}
		assertEquals(blocked[0] == UNSET ? "setBlocked never called" : "clearBlocked never called", CLEARED,
				blocked[0]);
	}

	/**
	 * Tests that the progress monitor blocked state is cleared in the case that a rule
	 * is transferring to the waiting job while blocked.
	 */
	@Test
	public void testBlockingAndUnblockingMonitorUsingTransfer() throws Exception {
		final int[] blocked = new int[] {UNSET};
		final ProgressMonitorWrapper wrapper = new ProgressMonitorWrapper(new NullProgressMonitor()) {
			@Override
			public void setBlocked(IStatus reason) {
				super.setBlocked(reason);
				blocked[0] = BLOCKED;
			}

			@Override
			public void clearBlocked() {
				super.clearBlocked();
				if (blocked[0] == BLOCKED) {
					blocked[0] = CLEARED;
				}
			}
		};
		final TestBarrier2 barrier = new TestBarrier2(TestBarrier2.STATUS_START);
		final IdentityRule rule = new IdentityRule();
		final Thread[] destinationThread = new Thread[1];
		AtomicReference<InterruptedException> exceptionInJob = new AtomicReference<>();
		Job conflicting = new Job("Conflicting") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				barrier.setStatus(TestBarrier2.STATUS_RUNNING);
				barrier.waitForStatus(TestBarrier2.STATUS_BLOCKED);
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					exceptionInJob.set(e);
					return Status.CANCEL_STATUS;
				}
				getJobManager().transferRule(rule, destinationThread[0]);
				return Status.OK_STATUS;
			}
		};
		Job transferTo = new Job("TransferTo") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				destinationThread[0] = getThread();
				barrier.setStatus(TestBarrier2.STATUS_BLOCKED);
				getJobManager().beginRule(rule, wrapper);
				getJobManager().endRule(rule);
				return Status.OK_STATUS;
			}
		};
		conflicting.setRule(rule);
		conflicting.schedule();
		barrier.waitForStatus(TestBarrier2.STATUS_RUNNING);
		transferTo.schedule();
		waitForCompletion(conflicting);
		waitForCompletion(transferTo);
		if (exceptionInJob.get() != null) {
			throw exceptionInJob.get();
		}
		assertEquals(blocked[0] == UNSET ? "setBlocked never called" : "clearBlocked never called", CLEARED,
				blocked[0]);
	}

	/**
	 * Tests that the progress monitor blocked state is cleared in the case that a rule
	 * becomes available to a yielded job while it is blocked.
	 */
	@Test
	public void testBlockingAndUnblockingMonitorUsingYield() throws Exception {
		final int[] blocked = new int[] {-1};
		final ProgressMonitorWrapper wrapper = new ProgressMonitorWrapper(new NullProgressMonitor()) {
			@Override
			public void setBlocked(IStatus reason) {
				super.setBlocked(reason);
				blocked[0] = BLOCKED;
			}

			@Override
			public void clearBlocked() {
				super.clearBlocked();
				if (blocked[0] == BLOCKED) {
					blocked[0] = CLEARED;
				}
			}
		};
		final TestBarrier2 barrier = new TestBarrier2(TestBarrier2.STATUS_START);
		IdentityRule rule = new IdentityRule();
		AtomicReference<InterruptedException> exceptionInJob = new AtomicReference<>();
		Job conflicting = new Job("Conflicting") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				barrier.setStatus(TestBarrier2.STATUS_RUNNING);
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					exceptionInJob.set(e);
					return Status.CANCEL_STATUS;
				}
				Job j = null;
				while (j == null) {
					j = yieldRule(wrapper);
				}
				return Status.OK_STATUS;
			}
		};
		conflicting.setRule(rule);
		conflicting.schedule();

		barrier.waitForStatus(TestBarrier2.STATUS_RUNNING);
		try {
			Job.getJobManager().beginRule(rule, null);
			Thread.sleep(1000);
		} finally {
			Job.getJobManager().endRule(rule);
		}
		conflicting.join();
		if (exceptionInJob.get() != null) {
			throw exceptionInJob.get();
		}
		assertEquals(blocked[0] == UNSET ? "setBlocked never called" : "clearBlocked never called", CLEARED, blocked[0]);
	}

	/**
	 * Tests that the progress monitor blocked state is cleared in the case that a rule
	 * is transferred back to a yielded job while it is blocked.
	 */
	@Test
	public void testBlockingAndUnblockingMonitorUsingYieldAndTransfer() throws Exception {
		final int[] blocked = new int[] {-1};
		final ProgressMonitorWrapper wrapper = new ProgressMonitorWrapper(new NullProgressMonitor()) {
			@Override
			public void setBlocked(IStatus reason) {
				super.setBlocked(reason);
				blocked[0] = BLOCKED;
			}

			@Override
			public void clearBlocked() {
				super.clearBlocked();
				if (blocked[0] == BLOCKED) {
					blocked[0] = CLEARED;
				}
			}
		};
		final TestBarrier2 barrier = new TestBarrier2(TestBarrier2.STATUS_START);
		IdentityRule rule = new IdentityRule();
		AtomicReference<InterruptedException> exceptionInJob = new AtomicReference<>();
		Job conflicting = new Job("Conflicting") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				barrier.setStatus(TestBarrier2.STATUS_RUNNING);
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					exceptionInJob.set(e);
					return Status.CANCEL_STATUS;
				}
				Job j = null;
				while (j == null) {
					j = yieldRule(wrapper);
				}
				return Status.OK_STATUS;
			}
		};
		conflicting.setRule(rule);
		conflicting.schedule();

		barrier.waitForStatus(TestBarrier2.STATUS_RUNNING);
		Job.getJobManager().beginRule(rule, null);
		Job.getJobManager().transferRule(rule, conflicting.getThread());
		Thread.sleep(1000);
		conflicting.join();
		if (exceptionInJob.get() != null) {
			throw exceptionInJob.get();
		}
		assertEquals(blocked[0] == UNSET ? "setBlocked never called" : "clearBlocked never called", CLEARED, blocked[0]);
	}
	
}
