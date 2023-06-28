/*****************************************************************
 * Copyright (c) 2021 Joerg Kubitz and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Joerg Kubitz - Initial API and implementation
 *****************************************************************/

package org.eclipse.debug.internal.ui.model.elements;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 * A singleton SerialExecutor job instance can be used to execute Runnable
 * objects offered by {@link #schedule(Runnable)} method in order of they
 * submission, one after another.
 */
public final class SerialExecutor extends Job {

	private final ConcurrentLinkedQueue<Runnable> queue;
	private final Object myFamily;

	/**
	 * @param jobName descriptive job name
	 * @param family non null object to control this job execution
	 **/
	public SerialExecutor(String jobName, Object family) {
		super(jobName);
		Assert.isNotNull(family);
		this.myFamily = family;
		this.queue = new ConcurrentLinkedQueue<>();
		setSystem(true);
	}

	@Override
	public boolean belongsTo(Object family) {
		return myFamily == family;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		Runnable action = queue.poll();
		try {
			if (action != null) {
				action.run();
			}
		} finally {
			if (!queue.isEmpty()) {
				// in case actions got faster scheduled then processed reschedule:
				schedule();
			}
		}
		return Status.OK_STATUS;
	}

	/**
	 * Enqueue an action asynchronously.
	 **/
	public void schedule(Runnable action) {
		queue.offer(action);
		schedule(); // will reschedule if already running
	}
}