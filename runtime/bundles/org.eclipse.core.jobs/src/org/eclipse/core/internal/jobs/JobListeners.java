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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.core.internal.runtime.RuntimeLog;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.osgi.util.NLS;

/**
 * Responsible for notifying all job listeners about job lifecycle events.  Uses a
 * specialized iterator to ensure the complex iteration logic is contained in one place.
 */
public class JobListeners {

	interface IListenerDoit {
		void notify(IJobChangeListener listener, IJobChangeEvent event);
	}

	private final IListenerDoit aboutToRun = IJobChangeListener::aboutToRun;
	private final IListenerDoit awake = IJobChangeListener::awake;
	private final IListenerDoit done = IJobChangeListener::done;
	private final IListenerDoit running = IJobChangeListener::running;
	private final IListenerDoit scheduled = IJobChangeListener::scheduled;
	private final IListenerDoit sleeping = IJobChangeListener::sleeping;

	private static final int DEFAULT_JOB_LISTENER_TIMEOUT = 3000;
	/**
	 * When any IJobChangeListener does not return within that time (in ms) an error
	 * will be logged and calls to IJobChangeListener will not be guaranteed to be
	 * in order. This is to detect deadlocking Listeners which relied on buggy
	 * implementation that calls to IJobChangeListener can overtake each other in
	 * different threads.
	 **/
	public static volatile int jobListenerTimeout = DEFAULT_JOB_LISTENER_TIMEOUT;
	/**
	 * The global job listeners.
	 */
	protected final ListenerList<IJobChangeListener> global = new ListenerList<>(ListenerList.IDENTITY);

	/** Send=true should not be used during a lock */
	void waitAndSendEvents(InternalJob job, boolean shouldSend) {
		// Instead of just waiting this threads may also need to help to send
		// to make sure there is progress.
		// For example during a Job.cancel() within a IJobChangeListener.scheduled()
		// this thread already has a lock for sending so it can not wait for another
		// Thread. See JobTest.testCancelAboutToSchedule() for an example.
		boolean send = shouldSend || job.eventQueueLock.isHeldByCurrentThread();
		// Synchronize eventQueue to get a stable order of events across Threads.
		// There is however no guarantee in which Thread the event is delivered.
		while (!job.eventQueue.isEmpty()) {
			if (getJobListenerTimeout() == 0) {
				// backward compatibility mode for listeners that may deadlock
				if (send) {
					sendEventsAsync(job);
				}
				return;
			}
			int timeout = getJobListenerTimeout();
			try {
				if (job.eventQueueLock.tryLock(timeout, TimeUnit.MILLISECONDS)) {
					try {
						job.eventQueueThread.set(Thread.currentThread());
						if (send) {
							sendEventsAsync(job);
						}
						job.eventQueueThread.set(null);
						return;
					} finally {
						job.eventQueueLock.unlock();
					}
				}
			} catch (InterruptedException ie) {
				continue;
			}
			Thread eventQueueThread = job.eventQueueThread.get();
			if (eventQueueThread != null) {
				setJobListenerTimeout(0);
				StackTraceElement[] stackTrace = eventQueueThread.getStackTrace();
				String msg = "IJobChangeListener timeout detected. Further calls to IJobChangeListener may occur in random order and join(family) can return too soon. IJobChangeListener should return within " //$NON-NLS-1$
						+ timeout
						+ " ms. IJobChangeListener methods should not block. Possible deadlock."; //$NON-NLS-1$
				MultiStatus status = new MultiStatus(JobManager.PI_JOBS, JobManager.PLUGIN_ERROR, msg,
						new TimeoutException(msg));
				StringBuilder buf = new StringBuilder(
						"Thread that is running the IJobChangeListener: " + eventQueueThread.getName()); //$NON-NLS-1$
				buf.append(System.lineSeparator());
				for (StackTraceElement stackTraceElement : stackTrace) {
					buf.append('\t');
					buf.append("at "); //$NON-NLS-1$
					buf.append(stackTraceElement);
					buf.append(System.lineSeparator());
				}
				Status child = new Status(IStatus.ERROR, JobManager.PI_JOBS, JobManager.PLUGIN_ERROR, buf.toString(),
						null);
				status.add(child);
				RuntimeLog.log(status);
			}
		}
	}

	private void sendEventsAsync(InternalJob job) {
		JobChangeEvent event;
		while ((event = job.eventQueue.poll()) != null) {
			sendEvent(event);
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

	public static void resetJobListenerTimeout() {
		setJobListenerTimeout(DEFAULT_JOB_LISTENER_TIMEOUT);
	}

	public static int getJobListenerTimeout() {
		return jobListenerTimeout;
	}

	public static void setJobListenerTimeout(int jobListenerTimeout) {
		JobListeners.jobListenerTimeout = jobListenerTimeout;
	}
}
