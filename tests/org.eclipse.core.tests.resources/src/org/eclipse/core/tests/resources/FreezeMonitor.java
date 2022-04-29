/*******************************************************************************
 * Copyright (c) 2016 Google, Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Stefan Xenos (Google) - Initial implementation
 *******************************************************************************/

package org.eclipse.core.tests.resources;

import java.lang.management.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;

public class FreezeMonitor {

	public static final long FROZEN_TEST_TIMEOUT_MS = 60_000;

	private static /* @Nullable */ Job monitorJob;

	/**
	 * Will dump JVM threads if test runs over one minute
	 */
	public static void expectCompletionInAMinute() {
		expectCompletionIn(FROZEN_TEST_TIMEOUT_MS);
	}

	/**
	 * Will dump JVM threads if test runs over given time
	 */
	public static void expectCompletionIn(final long timeout) {
		done();
		monitorJob = new Job("FreezeMonitor") {
			@Override
			public IStatus run(IProgressMonitor monitor) {
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				StringBuilder result = new StringBuilder();
				result.append("Possible frozen test case\n");
				ThreadMXBean threadStuff = ManagementFactory.getThreadMXBean();
				ThreadInfo[] allThreads = threadStuff.getThreadInfo(threadStuff.getAllThreadIds(), 200);
				for (ThreadInfo threadInfo : allThreads) {
					if (threadInfo == null) {
						continue;
					}
					result.append("\"");
					result.append(threadInfo.getThreadName());
					result.append("\": ");
					result.append(threadInfo.getThreadState());
					result.append("\n");
					final StackTraceElement[] elements = threadInfo.getStackTrace();
					for (StackTraceElement element : elements) {
						result.append("    ");
						result.append(element);
						result.append("\n");
					}
					result.append("\n");
				}
				System.out.println(result.toString());
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				return FreezeMonitor.class == family;
			}
		};
		monitorJob.schedule(timeout);
	}

	public static void done() {
		if (monitorJob != null) {
			monitorJob.cancel();
			monitorJob = null;
		}
	}
}