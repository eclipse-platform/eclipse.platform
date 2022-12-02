/*******************************************************************************
 * Copyright (c) 2022 Patrick Ziegler and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Patrick Ziegler - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.internal.builders;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;

/**
 * This class delegates "performance-heavy" computation to a worker thread.
 */
public class InterruptingBuilder extends IncrementalProjectBuilder {

	public static final String BUILDER_NAME = "org.eclipse.core.tests.resources.interruptingBuilder";
	private static CountDownLatch latch = new CountDownLatch(1);
	private static Job autoBuildJob;

	public static void reset() {
		latch = new CountDownLatch(1);
		autoBuildJob = null;
	}

	public static boolean waitForStart() {
		try {
			return latch.await(15, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			return false;
		}
	}

	public static boolean waitForEnd() {
		Objects.requireNonNull(autoBuildJob);

		try {
			return autoBuildJob.join(15000, new NullProgressMonitor());
		} catch (InterruptedException e) {
			return false;
		}
	}

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		autoBuildJob = Job.getJobManager().currentJob();
		latch.countDown();

		Job worker = new WorkerJob();
		worker.setPriority(Job.LONG);
		worker.setRule(new WorkerSchedulingRule());
		worker.schedule();

		try {
			worker.join();
		} catch (InterruptedException e) {
			// ignored
		}

		// The interrupt flag is set whenever the build finishes successfully. Hence we
		// have to simulate clicking the "Cancel Job" button.
		throw new OperationCanceledException();
	}

	private class WorkerJob extends Job {

		public WorkerJob() {
			super("Worker");
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				// Raises the interrupt flag of the auto-builder
				getProject().getFile(".project").touch(monitor);
			} catch (CoreException e) {
				return Status.error(e.getMessage(), e);
			}

			return Status.OK_STATUS;
		}

		@Override
		public boolean belongsTo(Object family) {
			return ResourcesPlugin.FAMILY_AUTO_BUILD.equals(family);
		}
	}

	private static class WorkerSchedulingRule implements ISchedulingRule {

		private final ISchedulingRule buildRule;

		public WorkerSchedulingRule() {
			buildRule = ResourcesPlugin.getWorkspace().getRuleFactory().buildRule();
		}

		@Override
		public boolean contains(ISchedulingRule rule) {
			return rule == this || buildRule.contains(rule);
		}

		@Override
		public boolean isConflicting(ISchedulingRule rule) {
			return rule == this || buildRule.isConflicting(rule);
		}
	}

}
