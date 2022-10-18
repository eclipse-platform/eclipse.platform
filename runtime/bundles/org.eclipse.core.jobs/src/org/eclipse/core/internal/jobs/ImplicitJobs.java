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
 *******************************************************************************/
package org.eclipse.core.internal.jobs;

import java.util.*;
import org.eclipse.core.internal.runtime.RuntimeLog;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Implicit jobs are jobs that are running by virtue of a JobManager.begin/end
 * pair. They act like normal jobs, except they are tied to an arbitrary thread
 * of the client's choosing, and they can be nested.
 * @ThreadSafe
 */
class ImplicitJobs {

	/**
	 * Cached unused instance that can be reused
	 * @GuardedBy("this")
	 */
	private ThreadJob jobCache = null;
	protected JobManager manager;

	/**
	 * Set of suspended scheduling rules.
	 * @GuardedBy("this")
	 */
	private final Set<ISchedulingRule> suspendedRules = new HashSet<>(20);

	/**
	 * Maps (Thread-&gt;ThreadJob), threads to the currently running job for that
	 * thread.
	 * @GuardedBy("this")
	 */
	private final Map<Thread, ThreadJob> threadJobs = new HashMap<>(20);

	ImplicitJobs(JobManager manager) {
		this.manager = manager;
	}

	/*
	 * @see IJobManager#beginRule(ISchedulingRule, IProgressMonitor)
	 */
	void begin(ISchedulingRule rule, IProgressMonitor monitor, boolean suspend) {
		if (JobManager.DEBUG_BEGIN_END)
			JobManager.debug("Begin rule: " + rule); //$NON-NLS-1$
		final Thread currentThread = Thread.currentThread();
		ThreadJob threadJob;
		synchronized (this) {
			threadJob = threadJobs.get(currentThread);
			if (threadJob != null) {
				//nested rule, just push on stack and return
				threadJob.push(rule);
				return;
			}
			//no need to schedule a thread job for a null rule
			if (rule == null)
				return;
			//create a thread job for this thread, use the rule from the real job if it has one
			Job realJob = manager.currentJob();
			if (realJob != null && realJob.getRule() != null)
				threadJob = newThreadJob(realJob.getRule());
			else {
				threadJob = newThreadJob(rule);
				threadJob.acquireRule = true;
			}
			//don't acquire rule if it is a suspended rule
			if (isSuspended(rule))
				threadJob.acquireRule = false;
			//indicate if it is a system job to ensure isBlocking works correctly
			threadJob.setRealJob(realJob);
			threadJob.setThread(currentThread);
		}
		try {
			threadJob.push(rule);
			//join the thread job outside sync block
			if (threadJob.acquireRule) {
				//no need to re-acquire any locks because the thread did not wait to get this lock
				if (manager.runNow(threadJob, false) == null)
					manager.getLockManager().addLockThread(Thread.currentThread(), rule);
				else
					threadJob = ThreadJob.joinRun(threadJob, monitor);
			}
		} finally {
			//remember this thread job  - only do this
			//after the rule is acquired because it is ok for this thread to acquire
			//and release other rules while waiting.
			synchronized (this) {
				threadJobs.put(currentThread, threadJob);
				if (suspend)
					suspendedRules.add(rule);
			}
		}
	}

	/*
	 * @see IJobManager#endRule
	 */
	synchronized void end(ISchedulingRule rule, boolean resume) {
		if (JobManager.DEBUG_BEGIN_END)
			JobManager.debug("End rule: " + rule); //$NON-NLS-1$
		ThreadJob threadJob = threadJobs.get(Thread.currentThread());
		if (threadJob == null)
			Assert.isLegal(rule == null, "endRule without matching beginRule: " + rule); //$NON-NLS-1$
		else if (threadJob.pop(rule)) {
			endThreadJob(threadJob, resume, false);
		}
	}

	/**
	 * Called when a worker thread has finished running a job. At this
	 * point, the worker thread must not own any scheduling rules
	 * @param lastJob The last job to run in this thread
	 */
	void endJob(InternalJob lastJob) {
		final Thread currentThread = Thread.currentThread();
		IStatus error;
		synchronized (this) {
			ThreadJob threadJob = threadJobs.get(currentThread);
			if (threadJob == null) {
				if (lastJob.getRule() != null)
					notifyWaitingThreadJobs(lastJob);
				return;
			}
			String msg = "Worker thread ended job: " + lastJob + ", but still holds rule: " + threadJob; //$NON-NLS-1$ //$NON-NLS-2$
			error = new Status(IStatus.ERROR, JobManager.PI_JOBS, 1, msg, new IllegalStateException(msg));
			//end the thread job
			endThreadJob(threadJob, false, true);
		}
		try {
			RuntimeLog.log(error);
		} catch (RuntimeException e) {
			//failed to log, so print to console instead
			System.err.println(error.getMessage());
		}
	}

	/**
	 * @GuardedBy("this")
	 */
	private void endThreadJob(ThreadJob threadJob, boolean resume, boolean worker) {
		Thread currentThread = Thread.currentThread();
		//clean up when last rule scope exits
		threadJobs.remove(currentThread);
		ISchedulingRule rule = threadJob.getRule();
		if (resume && rule != null)
			suspendedRules.remove(rule);
		//if this job had a rule, then we are essentially releasing a lock
		//note it is safe to do this even if the acquire was aborted
		if (threadJob.acquireRule) {
			manager.getLockManager().removeLockThread(currentThread, rule);
			notifyWaitingThreadJobs(threadJob);
		}
		//if the job was started, we need to notify job manager to end it
		if (threadJob.isRunning())
			manager.endJob(threadJob, Status.OK_STATUS, false, worker);
		recycle(threadJob);
	}

	/**
	 * Returns true if this rule has been suspended, and false otherwise.
	 * @GuardedBy("this")
	 */
	private boolean isSuspended(ISchedulingRule rule) {
		if (suspendedRules.isEmpty())
			return false;
		for (ISchedulingRule iSchedulingRule : suspendedRules)
			if (iSchedulingRule.contains(rule))
				return true;
		return false;
	}

	/**
	 * Returns a new or reused ThreadJob instance.
	 * @GuardedBy("this")
	 */
	private ThreadJob newThreadJob(ISchedulingRule rule) {
		if (jobCache != null) {
			ThreadJob job = jobCache;
			// calling setRule will try to acquire JobManager.lock, breaking
			// lock acquisition protocol. Since we managing this special job
			// ourselves we can call internalSetRule
			((InternalJob) job).internalSetRule(rule);
			job.acquireRule = job.isRunning = false;
			job.realJob = null;
			jobCache = null;
			return job;
		}
		return new ThreadJob(rule);
	}

	/**
	 * A job has just finished that was holding a scheduling rule, and the
	 * scheduling rule is now free.  Wake any blocked thread jobs so they can
	 * compete for the newly freed lock
	 */
	void notifyWaitingThreadJobs(InternalJob job) {
		synchronized (job.jobStateLock) {
			job.jobStateLock.notifyAll();
		}
	}

	/**
	 * Indicates that a thread job is no longer in use and can be reused.
	 * @GuardedBy("this")
	 */
	private void recycle(ThreadJob job) {
		if (jobCache == null && job.recycle())
			jobCache = job;
	}

	/**
	 * Implements IJobManager#resume(ISchedulingRule)
	 * @param rule
	 */
	void resume(ISchedulingRule rule) {
		//resume happens as a consequence of freeing the last rule in the stack
		end(rule, true);
		if (JobManager.DEBUG_BEGIN_END)
			JobManager.debug("Resume rule: " + rule); //$NON-NLS-1$
	}

	/**
	 * Implements IJobManager#suspend(ISchedulingRule, IProgressMonitor)
	 * @param rule
	 * @param monitor
	 */
	void suspend(ISchedulingRule rule, IProgressMonitor monitor) {
		if (JobManager.DEBUG_BEGIN_END)
			JobManager.debug("Suspend rule: " + rule); //$NON-NLS-1$
		//the suspend job will be remembered once the rule is acquired
		begin(rule, monitor, true);
	}

	/**
	 * Implements IJobManager#transferRule(ISchedulingRule, Thread)
	 */
	synchronized void transfer(ISchedulingRule rule, Thread destinationThread) {
		//nothing to do for null
		if (rule == null)
			return;
		final Thread currentThread = Thread.currentThread();
		//nothing to do if transferring to the same thread
		if (currentThread == destinationThread)
			return;
		//ensure destination thread doesn't already have a rule
		ThreadJob target = threadJobs.get(destinationThread);
		Assert.isLegal(target == null, "Transfer rule to job that already owns a rule"); //$NON-NLS-1$
		//ensure calling thread owns the job being transferred
		ThreadJob source = threadJobs.get(currentThread);
		Assert.isNotNull(source, "transferRule without beginRule"); //$NON-NLS-1$
		Assert.isLegal(source.getRule() == rule, "transferred rule " + rule + " does not match beginRule: " + source.getRule()); //$NON-NLS-1$ //$NON-NLS-2$		// transfer the thread job without ending it
		source.setThread(destinationThread);
		threadJobs.remove(currentThread);
		threadJobs.put(destinationThread, source);
		// transfer lock
		if (source.acquireRule) {
			manager.getLockManager().removeLockThread(currentThread, rule);
			manager.getLockManager().addLockThread(destinationThread, rule);
		}
		// Wake up any blocked jobs (waiting within yield or joinRun) waiting on
		// this rule
		notifyWaitingThreadJobs(source);
	}

	synchronized void removeWaiting(ThreadJob threadJob) {
		synchronized (((InternalJob) threadJob).jobStateLock) {
			threadJob.isWaiting = false;
			notifyWaitingThreadJobs(threadJob);
			((InternalJob) threadJob).setWaitQueueStamp(InternalJob.T_NONE);
		}
		manager.dequeue(manager.waitingThreadJobs, threadJob);
	}

	synchronized void addWaiting(ThreadJob threadJob) {
		synchronized (((InternalJob) threadJob).jobStateLock) {
			threadJob.isWaiting = true;
			notifyWaitingThreadJobs(threadJob);
			((InternalJob) threadJob).setWaitQueueStamp(manager.getNextWaitQueueStamp());
		}
		manager.enqueue(manager.waitingThreadJobs, threadJob);
	}

	synchronized ThreadJob getThreadJob(Thread thread) {
		return threadJobs.get(thread);
	}

}
