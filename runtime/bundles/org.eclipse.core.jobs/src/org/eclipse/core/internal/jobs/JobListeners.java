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

	/** Should not be used during a lock */
	void sendEvents(InternalJob job) {
		JobChangeEvent event;
		// Synchronize eventQueue to get a stable order of events across Threads.
		// There is however no guarantee in which Thread the event is delivered.
		synchronized (job.eventQueue) {
			while ((event = job.eventQueue.poll()) != null) {
				sendEvent(event);
			}
		}
	}

	/**
	 * Process the given event for all global listeners and all local listeners on
	 * the given job.
	 */
	private void sendEvent(final JobChangeEvent event) {
		IListenerDoit doit = event.doit;
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

	/** Can be used while synchronize(JobManager.lock) */
	private void queueEvent(final JobChangeEvent event) {
		((InternalJob) event.job).eventQueue.offer(event);
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

	public void queueAboutToRun(Job job) {
		queueEvent(new JobChangeEvent(aboutToRun, job));
	}

	public void queueAwake(Job job) {
		queueEvent(new JobChangeEvent(awake, job));
	}

	public void queueDone(Job job, IStatus result, boolean reschedule) {
		queueEvent(new JobChangeEvent(done, job, result, reschedule));
	}

	public void queueRunning(Job job) {
		queueEvent(new JobChangeEvent(running, job));
	}

	public void queueScheduled(Job job, long delay, boolean reschedule) {
		queueEvent(new JobChangeEvent(scheduled, job, delay, reschedule));
	}

	public void queueSleeping(Job job) {
		queueEvent(new JobChangeEvent(sleeping, job));
	}
}
