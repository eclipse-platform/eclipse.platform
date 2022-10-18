/*******************************************************************************
 *  Copyright (c) 2003, 2012 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM - Initial API and implementation
 *     salesforce.com - limit number of sleeping worker threads
 *******************************************************************************/
package org.eclipse.core.internal.jobs;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Maintains a pool of worker threads. Threads are constructed lazily as
 * required, and are eventually discarded if not in use for awhile. This class
 * maintains the thread creation/destruction policies for the job manager.
 *
 * Implementation note: all the data structures of this class are protected
 * by the instance's object monitor.  To avoid deadlock with third party code,
 * this lock is never held when calling methods outside this class that may in
 * turn use locks.
 */
class WorkerPool {
	/**
	 * Threads not used by their best before timestamp are destroyed.
	 */
	private static final int BEST_BEFORE = 60000;
	/**
	 * There will always be at least MIN_THREADS workers in the pool.
	 */
	private static final int MIN_THREADS = 1;

	/**
	 * Soft limit on the maximum number of workers in the pool. An idle worker
	 * is not put back in the pool if the total number of workers is more than
	 * MAX_THREADS.
	 */
	private static final int MAX_THREADS = 50;

	/**
	 * Use the busy thread count to avoid starting new threads when a living
	 * thread is just doing house cleaning (notifying listeners, etc).
	 */
	private int busyThreads = 0;

	/**
	 * The default context class loader to use when creating worker threads.
	 */
	protected final ClassLoader defaultContextLoader;

	/**
	 * Records whether new worker threads should be daemon threads.
	 */
	private boolean isDaemon = false;

	private final JobManager manager;
	/**
	 * The number of workers in the threads array
	 */
	private int numThreads = 0;
	/**
	 * The number of threads that are currently sleeping
	 */
	private int sleepingThreads = 0;
	/**
	 * The living set of workers in this pool.
	 */
	private Worker[] threads = new Worker[10];

	protected WorkerPool(JobManager manager) {
		this.manager = manager;
		this.defaultContextLoader = Thread.currentThread().getContextClassLoader();
	}

	/**
	 * Adds a worker to the list of workers.
	 */
	private synchronized void add(Worker worker) {
		int size = threads.length;
		if (numThreads + 1 > size) {
			Worker[] newThreads = new Worker[2 * size];
			System.arraycopy(threads, 0, newThreads, 0, size);
			threads = newThreads;
		}
		threads[numThreads++] = worker;
	}

	private synchronized void decrementBusyThreads() {
		//impossible to have less than zero busy threads
		if (--busyThreads < 0) {
			if (JobManager.DEBUG)
				Assert.isTrue(false, Integer.toString(busyThreads));
			busyThreads = 0;
		}
	}

	/**
	 * Signals the end of a job.  Note that this method can be called under
	 * OutOfMemoryError conditions and thus must be paranoid about allocating objects.
	 */
	protected void endJob(InternalJob job, IStatus result) {
		try {
			//need to end rule in graph before ending job so that 2 threads
			//do not become the owners of the same rule in the graph
			if ((job.getRule() != null) && !(job instanceof ThreadJob)) {
				//remove any locks this thread may be owning on that rule
				manager.getLockManager().removeLockCompletely(Thread.currentThread(), job.getRule());
			}
			manager.endJob(job, result, true, false);
			//ensure this thread no longer owns any scheduling rules
			manager.implicitJobs.endJob(job);
		} finally {
			decrementBusyThreads();
		}
	}

	/**
	 * Signals the death of a worker thread.  Note that this method can be called under
	 * OutOfMemoryError conditions and thus must be paranoid about allocating objects.
	 */
	protected synchronized void endWorker(Worker worker) {
		if (remove(worker) && JobManager.DEBUG)
			JobManager.debug("worker removed from pool: " + worker); //$NON-NLS-1$
	}

	private synchronized void incrementBusyThreads() {
		//impossible to have more busy threads than there are threads
		if (++busyThreads > numThreads) {
			if (JobManager.DEBUG)
				Assert.isTrue(false, Integer.toString(busyThreads) + ',' + numThreads);
			busyThreads = numThreads;
		}
	}

	/**
	 * Notification that a job has been added to the queue. Wake a worker,
	 * creating a new worker if necessary. The provided job may be null.
	 */
	protected synchronized void jobQueued() {
		//if there is a sleeping thread, wake it up
		if (sleepingThreads > 0) {
			notify();
			return;
		}
		//create a thread if all threads are busy
		if (busyThreads >= numThreads) {
			Worker worker = new Worker(this);
			worker.setDaemon(isDaemon);
			add(worker);
			if (JobManager.DEBUG)
				JobManager.debug("worker added to pool: " + worker); //$NON-NLS-1$
			worker.start();
			return;
		}
	}

	/**
	 * Remove a worker thread from our list.
	 * @return true if a worker was removed, and false otherwise.
	 */
	private synchronized boolean remove(Worker worker) {
		for (int i = 0; i < threads.length; i++) {
			if (threads[i] == worker) {
				System.arraycopy(threads, i + 1, threads, i, numThreads - i - 1);
				threads[--numThreads] = null;
				return true;
			}
		}
		return false;
	}

	/**
	 * Sets whether threads created in the worker pool should be daemon threads.
	 */
	void setDaemon(boolean value) {
		this.isDaemon = value;
	}

	protected synchronized void shutdown() {
		notifyAll();
	}

	/**
	 * Sleep for the given duration or until woken.
	 */
	private synchronized void sleep(long duration) {
		sleepingThreads++;
		busyThreads--;
		if (JobManager.DEBUG)
			JobManager.debug("worker sleeping for: " + duration + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
		try {
			wait(duration);
		} catch (InterruptedException e) {
			if (JobManager.DEBUG)
				JobManager.debug("worker interrupted while waiting... :-|"); //$NON-NLS-1$
		} finally {
			sleepingThreads--;
			busyThreads++;
		}
	}

	/**
	 * Returns a new job to run. Returns null if the thread should die.
	 */
	protected InternalJob startJob(Worker worker) {
		// must endWorker and decrementBusyThreads from the same synchronized block
		boolean busy;
		synchronized (this) {
			if (!manager.isActive()) {
				//must remove the worker immediately to prevent all threads from expiring
				endWorker(worker);
				return null;
			}
			// set the thread to be busy now in case of reentrant scheduling
			incrementBusyThreads();
			busy = true;
		}
		Job job = null;
		try {
			job = manager.startJob(worker);
			//spin until a job is found or until we have been idle for too long
			long idleStart = manager.now();
			while (manager.isActive() && job == null) {
				long hint = manager.sleepHint();
				if (hint > 0) {
					synchronized (this) {
						if (numThreads > MAX_THREADS) {
							endWorker(worker);
							decrementBusyThreads();
							busy = false;
							return null;
						}
					}
					sleep(Math.min(hint, BEST_BEFORE));
				}
				job = manager.startJob(worker);
				//if we were already idle, and there are still no new jobs, then
				// the thread can expire
				synchronized (this) {
					if (job == null && (manager.now() - idleStart > BEST_BEFORE) && (numThreads - busyThreads) > MIN_THREADS) {
						//must remove the worker immediately to prevent all threads from expiring
						endWorker(worker);
						decrementBusyThreads();
						busy = false;
						return null;
					}
				}
				//if we didn't sleep but there was no job available, make sure we sleep to avoid a tight loop (bug 260724)
				if (hint <= 0 && job == null)
					sleep(50);
			}
			if (job != null) {
				//if this job has a rule, then we are essentially acquiring a lock
				if ((job.getRule() != null) && !(job instanceof ThreadJob)) {
					//don't need to re-acquire locks because it was not recorded in the graph
					//that this thread waited to get this rule
					manager.getLockManager().addLockThread(Thread.currentThread(), job.getRule());
				}
				//see if we need to wake another worker
				if (manager.sleepHint() < InternalJob.T_INFINITE)
					jobQueued();
			}
		} finally {
			//decrement busy thread count if we're not running a job
			if (job == null && busy)
				decrementBusyThreads();
		}
		return job;
	}
}
