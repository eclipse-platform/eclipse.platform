/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
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
package org.eclipse.core.tests.runtime.jobs;

import java.util.concurrent.atomic.AtomicIntegerArray;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.tests.harness.TestBarrier2;

/**
 * A test job that begins and ends a scheduling rule, waiting on signals
 * at each step to allow synchronization with the thread that is running the test.
 */
class JobRuleRunner extends Job {
	private ISchedulingRule rule;
	private TestBarrier2 barrier;
	private int numRepeats;
	private boolean reportBlocking;

	/**
	 * This job will start applying the given rule in the manager
	 */
	public JobRuleRunner(String name, ISchedulingRule rule, AtomicIntegerArray status, int index, int numRepeats,
			boolean reportBlocking) {
		this(name, rule, new TestBarrier2(status, index), numRepeats, reportBlocking);
	}
	/**
	 * This job will start applying the given rule in the manager
	 */
	public JobRuleRunner(String name, ISchedulingRule rule, TestBarrier2 barrier, int numRepeats, boolean reportBlocking) {
		super(name);
		this.rule = rule;
		this.barrier = barrier;
		this.numRepeats = numRepeats;
		this.reportBlocking = reportBlocking;
	}
	/**
	 * This job will start applying the given rule in the manager
	 */
	public JobRuleRunner(String name, ISchedulingRule rule, TestBarrier2 barrier) {
		this(name, rule, barrier, 1, false);
	}

	@Override
	@SuppressWarnings("restriction")
	protected IStatus run(IProgressMonitor monitor) {
		//begin executing the job
		monitor.beginTask(getName(), numRepeats);
		try {
			//set the status flag to START
			barrier.setStatus(TestBarrier2.STATUS_START);
			for (int i = 0; i < numRepeats; i++) {
				monitor.worked(1);
				//wait until the tester allows this job to run again
				barrier.waitForStatusNoFail(TestBarrier2.STATUS_WAIT_FOR_RUN);
				//create a hook that would notify this thread when this job was blocked on a rule (if needed)
				TestBlockingMonitor bMonitor = null;
				if (reportBlocking) {
					bMonitor = new TestBlockingMonitor(barrier);
				}

				//start the given rule in the manager
				manager.beginRule(rule, bMonitor);
				//set status to RUNNING
				barrier.setStatus(TestBarrier2.STATUS_RUNNING);

				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}

				//wait until tester allows this job to finish
				barrier.waitForStatusNoFail(TestBarrier2.STATUS_WAIT_FOR_DONE);
				//end the given rule
				manager.endRule(rule);
				//set status to DONE
				barrier.setStatus(TestBarrier2.STATUS_DONE);

				Thread.yield();
			}

		} finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}

}
