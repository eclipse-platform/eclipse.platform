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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.core.tests.harness.TestBarrier2;

/**
 * This runnable will try to begin the given rule in the Job Manager.  It will
 * end the rule before returning.
 */
class SimpleRuleRunner implements Runnable {
	private ISchedulingRule rule;
	private IProgressMonitor monitor;
	private AtomicIntegerArray status;
	RuntimeException exception;
	private static final IJobManager manager = Job.getJobManager();

	public SimpleRuleRunner(ISchedulingRule rule, AtomicIntegerArray status, IProgressMonitor monitor) {
		this.rule = rule;
		this.monitor = monitor;
		this.status = status;
		this.exception = null;
	}

	@Override
	public void run() {
		//tell the caller that we have entered the run method
		status.set(0, TestBarrier2.STATUS_RUNNING);
		try {
			try {
				manager.beginRule(rule, monitor);
			} finally {
				manager.endRule(rule);
			}
		} catch (OperationCanceledException e) {
			//ignore
		} catch (RuntimeException e) {
			exception = e;
		} finally {
			status.set(0, TestBarrier2.STATUS_DONE);
		}
	}
}
