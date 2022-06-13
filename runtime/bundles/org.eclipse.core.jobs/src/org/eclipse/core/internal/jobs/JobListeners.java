/*******************************************************************************
 * Copyright (c) 2003, 2014 IBM Corporation and others.
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
package org.eclipse.core.internal.jobs;

import org.eclipse.core.internal.runtime.RuntimeLog;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.osgi.util.NLS;

/**
 * Responsible for notifying all job listeners about job lifecycle events.  Uses a
 * specialized iterator to ensure the complex iteration logic is contained in one place.
 */
class JobListeners {
	interface IListenerDoit {
		void notify(IJobChangeListener listener, IJobChangeEvent event);
	}

	private final IListenerDoit aboutToRun = IJobChangeListener::aboutToRun;
	private final IListenerDoit awake = IJobChangeListener::awake;
	private final IListenerDoit done = IJobChangeListener::done;
	private final IListenerDoit running = IJobChangeListener::running;
	private final IListenerDoit scheduled = IJobChangeListener::scheduled;
	private final IListenerDoit sleeping = IJobChangeListener::sleeping;

	/**
	 * The global job listeners.
	 */
	protected final ListenerList<IJobChangeListener> global = new ListenerList<>(ListenerList.IDENTITY);

	/**
	 * TODO Could use an instance pool to re-use old event objects
	 */
	static JobChangeEvent newEvent(Job job) {
		JobChangeEvent instance = new JobChangeEvent();
		instance.job = job;
		return instance;
	}

	static JobChangeEvent newEvent(Job job, IStatus result) {
		JobChangeEvent instance = new JobChangeEvent();
		instance.job = job;
		instance.result = result;
		return instance;
	}

	static JobChangeEvent newEvent(Job job, long delay) {
		JobChangeEvent instance = new JobChangeEvent();
		instance.job = job;
		instance.delay = delay;
		return instance;
	}

	/**
	 * Process the given doit for all global listeners and all local listeners
	 * on the given job.
	 */
	private void doNotify(final IListenerDoit doit, final IJobChangeEvent event) {
		//notify all global listeners
		for (IJobChangeListener listener : global) {
			try {
				doit.notify(listener, event);
			} catch (Throwable e) {
				handleException(listener, e);
			}
		}
		for (IJobChangeListener listener : ((InternalJob) event.getJob()).getListeners()) {
			try {
				doit.notify(listener, event);
			} catch (Throwable e) {
				handleException(listener, e);
			}
		}
	}

	private void handleException(IJobChangeListener listener, Throwable e) {
		//this code is roughly copied from InternalPlatform.run(ISafeRunnable),
		//but in-lined here for performance reasons
		if (e instanceof OperationCanceledException)
			return;
		String pluginId = JobOSGiUtils.getDefault().getBundleId(listener);
		if (pluginId == null)
			pluginId = JobManager.PI_JOBS;
		String message = NLS.bind(JobMessages.meta_pluginProblems, pluginId);
		RuntimeLog.log(new Status(IStatus.ERROR, pluginId, JobManager.PLUGIN_ERROR, message, e));
	}

	public void add(IJobChangeListener listener) {
		global.add(listener);
	}

	public void remove(IJobChangeListener listener) {
		global.remove(listener);
	}

	public void aboutToRun(Job job) {
		doNotify(aboutToRun, newEvent(job));
	}

	public void awake(Job job) {
		doNotify(awake, newEvent(job));
	}

	public void done(Job job, IStatus result, boolean reschedule) {
		JobChangeEvent event = newEvent(job, result);
		event.reschedule = reschedule;
		doNotify(done, event);
	}

	public void running(Job job) {
		doNotify(running, newEvent(job));
	}

	public void scheduled(Job job, long delay, boolean reschedule) {
		JobChangeEvent event = newEvent(job, delay);
		event.reschedule = reschedule;
		doNotify(scheduled, event);
	}

	public void sleeping(Job job) {
		doNotify(sleeping, newEvent(job));
	}
}
