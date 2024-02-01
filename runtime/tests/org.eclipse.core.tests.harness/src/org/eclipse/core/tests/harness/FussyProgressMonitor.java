/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.core.tests.harness;

import static org.assertj.core.api.Assertions.assertThat;

import junit.framework.AssertionFailedError;
import org.eclipse.core.runtime.jobs.Job;

/**
 * This class can be used for testing progress monitoring.
 * If you want to reuse one instance of this class for several
 * invocations, make sure that you call prepare() before every
 * invocation.
 * Call sanityCheck() after the operation whose progress monitoring
 * you are testing.
 */
public class FussyProgressMonitor extends TestProgressMonitor {
	public static class FussyProgressAssertionFailed extends AssertionFailedError {
		/**
		 * All serializable objects should have a stable serialVersionUID
		 */
		private static final long serialVersionUID = 1L;

		FussyProgressAssertionFailed(String name) {
			super(name);
		}
	}

	private static final double EPS_FACTOR = 0.01;
	private static final long NOTICEABLE_DELAY = 1000; // milliseconds
	private boolean beginTaskCalled;
	private long beginTime;
	private boolean canceled;
	private int doneCalls;
	private boolean hasFailed;
	private boolean sanityCheckCalled = true;
	private String taskName;
	private int totalWork;
	private double workedSoFar;
	private Job job;
	public FussyProgressMonitor() {
		prepare();
	}
	/**
	 * Creates a fussy progress monitor that is associated with a particular job.
	 * On assertion failure the job name will be included in the failure message.
	 */
	public FussyProgressMonitor(Job job) {
		this();
		this.job = job;
	}

	private void wrapAssertion(Runnable assertion) {
		// silently ignore follow-up failures
		if (hasFailed) {
			return;
		}
		try {
			assertion.run();
		} catch (AssertionError error) {
			processFailedAssertion(error);
		}
	}

	public void processFailedAssertion(AssertionError assertionError) throws FussyProgressAssertionFailed {
		hasFailed = true;
		String jobSuffix = "";
		if (job != null) {
			jobSuffix += " in job: " + job.getName();
		}
		FussyProgressAssertionFailed failure = new FussyProgressAssertionFailed(
				"Progress monitor assertion failed" + jobSuffix);
		failure.initCause(assertionError);
		throw failure;
	}

	/**
	 * Asserts that this progress monitor is all used up
	 */
	public void assertUsedUp() {
		wrapAssertion(
				() -> assertThat(beginTaskCalled).withFailMessage("beginTask has not been called on ProgressMonitor")
						.isTrue());
		wrapAssertion(() -> assertThat(Math.round(workedSoFar)).as("work done").isGreaterThanOrEqualTo(totalWork));
	}

	@Override
	public void beginTask(String name, int newTotalWork) {
		wrapAssertion(() -> assertThat(beginTaskCalled)
				.withFailMessage("beginTask may only be called once (old name=%s)", taskName).isFalse());
		beginTaskCalled = true;
		taskName = name;
		wrapAssertion(() -> assertThat(newTotalWork).as("total work").satisfiesAnyOf(
				work -> assertThat(work).isEqualTo(UNKNOWN), work -> assertThat(work).isGreaterThan(0)));
		this.totalWork = newTotalWork;
		beginTime = System.currentTimeMillis();
	}

	@Override
	public void done() {
		workedSoFar = totalWork;
		doneCalls++;
	}

	@Override
	public void internalWorked(double work) {
		wrapAssertion(() -> assertThat(beginTaskCalled)
				.withFailMessage("can accept calls to worked/internalWorked only after beginTask").isTrue());
		wrapAssertion(() -> assertThat(doneCalls)
				.withFailMessage("can accept calls to worked/internalWorked only before done is called").isZero());
		wrapAssertion(() -> assertThat(work).as("amount worked").isGreaterThan(0.0));
		workedSoFar += work;
		if (totalWork != UNKNOWN) {
			wrapAssertion(() -> assertThat(workedSoFar).as("total work done")
					.isLessThanOrEqualTo(totalWork + (totalWork * EPS_FACTOR)));
		}
	}

	@Override
	public boolean isCanceled() {
		return canceled;
	}

	/**
	 * should be called before every use of a FussyProgressMonitor
	 */
	public void prepare() {
		sanityCheckCalled = false;
		taskName = null;
		totalWork = 0;
		workedSoFar = 0;
		beginTaskCalled = false;
		doneCalls = 0;
		hasFailed = false;
	}

	/**
	 *  should be called after every use of a FussyProgressMonitor
	 */
	public void sanityCheck() {
		wrapAssertion(
				() -> assertThat(sanityCheckCalled).withFailMessage("sanityCheck has already been called").isFalse());
		sanityCheckCalled = true;
		long duration = System.currentTimeMillis() - beginTime;
		if (duration > NOTICEABLE_DELAY && beginTaskCalled) {
			wrapAssertion(() -> assertThat(workedSoFar)
					.withFailMessage("this operation took: %s ms, it should report progress", duration)
					.isGreaterThan(0.0));
		}
	}

	@Override
	public void setCanceled(boolean b) {
		boolean wasCanceled = isCanceled();
		canceled = b;
		if (canceled && !wasCanceled && job != null) {
			job.cancel();
		}
	}

	@Override
	public void setTaskName(String name) {
		taskName = name;
	}

	@Override
	public void subTask(String name) {
		// do nothing
	}

	@Override
	public void worked(int work) {
		internalWorked(work);
	}
}
