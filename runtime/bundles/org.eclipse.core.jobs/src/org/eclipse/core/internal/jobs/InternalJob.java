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
 *     Stephan Wahlbrink  - Fix for bug 200997.
 *     Thirumala Reddy Mutchukota (thirumala@google.com) -
 *     		Bug 432049, JobGroup API and implementation
 *     		Bug 105821, Support for Job#join with timeout and progress monitor
 *******************************************************************************/
package org.eclipse.core.internal.jobs;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;

/**
 * Internal implementation class for jobs. Clients must not implement this class
 * directly.  All jobs must be subclasses of the API <code>org.eclipse.core.runtime.jobs.Job</code> class.
 */
public abstract class InternalJob extends PlatformObject implements Comparable<InternalJob> {
	/**
	 * Job state code (value 16) indicating that a job has been removed from
	 * the wait queue and is about to start running. From an API point of view,
	 * this is the same as RUNNING.
	 */
	static final int ABOUT_TO_RUN = 0x10;

	/**
	 * Job state code (value 32) indicating that a job has passed scheduling
	 * precondition checks and is about to be added to the wait queue. From an API point of view,
	 * this is the same as WAITING.
	 */
	static final int ABOUT_TO_SCHEDULE = 0x20;
	/**
	 * Job state code (value 8) indicating that a job is blocked by another currently
	 * running job.  From an API point of view, this is the same as WAITING.
	 */
	static final int BLOCKED = 0x08;
	/**
	 * Job state code (value 64) indicating that a job is yielding.
	 * From an API point of view, this is the same as WAITING.
	 */
	static final int YIELDING = 0x40;

	//flag mask bits
	private static final int M_STATE = 0xFF;
	private static final int M_SYSTEM = 0x0100;
	private static final int M_USER = 0x0200;

	/*
	 * flag on a job indicating that it was about to run, but has been canceled
	 */
	private static final int M_ABOUT_TO_RUN_CANCELED = 0x0400;

	/*
	 * Flag on a job indicating that it was canceled when running. This flag
	 * is used to ensure that #canceling is only ever called once on a job in
	 * case of recursive cancelation attempts.
	 */
	private static final int M_RUN_CANCELED = 0x0800;

	private static int nextJobNumber = 0;
	protected static final JobManager manager = JobManager.getInstance();

	/**
	 * Start time constant indicating a job should be started at
	 * a time in the infinite future, causing it to sleep forever.
	 */
	static final long T_INFINITE = Long.MAX_VALUE;
	/**
	 * Start time constant indicating that the job has no start time.
	 */
	static final long T_NONE = -1;

	private volatile int flags = Job.NONE;
	private final int jobNumber = getNextJobNumber();
	/**
	 * The list of job listeners. Never null.
	 * @GuardedBy("itself")
	 */
	private final ListenerList<IJobChangeListener> listeners = new ListenerList<>(ListenerList.IDENTITY);

	private volatile IProgressMonitor monitor;
	private String name;
	private JobGroup jobGroup;
	/**
	 * The job ahead of me in a queue or list.
	 * @GuardedBy("manager.lock")
	 */
	private InternalJob next;
	/**
	 * The job behind me in a queue or list.
	 * @GuardedBy("manager.lock")
	 */
	private InternalJob previous;
	private int priority = Job.LONG;
	/**
	 * Arbitrary properties (key,value) pairs, attached
	 * to a job instance by a third party.
	 */
	private ObjectMap<QualifiedName, Object> properties;

	/**
	 * Volatile because it is usually set via a Worker thread and is read via a
	 * client thread.
	 */
	private volatile IStatus result;
	/**
	 * @GuardedBy("manager.lock")
	 */
	private ISchedulingRule schedulingRule;
	/**
	 * If the job is waiting, this represents the time the job should start by.
	 * If this job is sleeping, this represents the time the job should wake up.
	 * If this job is running, this represents the delay automatic rescheduling,
	 * or -1 if the job should not be rescheduled.
	 * @GuardedBy("manager.lock")
	 */
	private volatile long startTime;

	/**
	 * Stamp added when a job is added to the wait queue. Used to ensure
	 * jobs in the wait queue maintain their insertion order even if they are
	 * removed from the wait queue temporarily while blocked
	 * @GuardedBy("manager.lock")
	 */
	private long waitQueueStamp = T_NONE;

	/*
	 * The thread that is currently running this job
	 */
	private volatile Thread thread = null;

	/**
	 * This lock will be held while performing state changes on this job. It is
	 * also used as a notifier used to wake up yielding jobs or waiting ThreadJobs
	 * when 1) a conflicting job completes and releases a scheduling rule, or 2)
	 * when a this job changes state.
	 *
	 * See also the lock ordering protocol explanation in JobManager's
	 * documentation.
	 *
	 * @GuardedBy("itself")
	 */
	final Object jobStateLock = new Object();

	/**
	 * Used to synchronize Job listener notification
	 */
	final Queue<JobChangeEvent> eventQueue = new ConcurrentLinkedQueue<>();
	final ReentrantLock eventQueueLock = new ReentrantLock();
	final AtomicReference<Thread> eventQueueThread = new AtomicReference<>();

	private static synchronized int getNextJobNumber() {
		return nextJobNumber++;
	}

	protected InternalJob(String name) {
		Assert.isNotNull(name);
		this.name = name;
	}

	protected void addJobChangeListener(IJobChangeListener listener) {
		listeners.add(listener);
	}

	/**
	 * Adds an entry at the end of the list of which this item is the head.
	 * @GuardedBy("manager.lock")
	 */
	final void addLast(InternalJob entry) {
		InternalJob last = this;
		//find the end of the queue
		while (last.previous != null)
			last = last.previous;
		//add the new entry to the end of the queue
		last.previous = entry;
		entry.next = last;
		entry.previous = null;
	}

	protected boolean belongsTo(Object family) {
		return false;
	}

	protected boolean cancel() {
		return manager.cancel(this);
	}

	protected void canceling() {
		//default implementation does nothing
	}

	@Override
	public final int compareTo(InternalJob otherJob) {
		return otherJob.startTime >= startTime ? 1 : -1;
	}

	protected void done(IStatus endResult) {
		manager.endJob(this, endResult, true, false);
	}

	/**
	 * Returns the job listeners that are only listening to this job. Never returns
	 * null.
	 */
	final ListenerList<IJobChangeListener> getListeners() {
		return listeners;
	}

	protected String getName() {
		return name;
	}

	protected int getPriority() {
		return priority;
	}

	/**
	 * Returns the job's progress monitor, or null if it is not running.
	 */
	final IProgressMonitor getProgressMonitor() {
		return monitor;
	}

	protected Object getProperty(QualifiedName key) {
		// thread safety: (Concurrency001 - copy on write)
		Map<QualifiedName, Object> temp = properties;
		if (temp == null)
			return null;
		return temp.get(key);
	}

	protected IStatus getResult() {
		return result;
	}

	protected ISchedulingRule getRule() {
		return schedulingRule;
	}

	/**
	 * Returns the time that this job should be started, awakened, or
	 * rescheduled, depending on the current state.
	 * @return time in milliseconds
	 */
	final long getStartTime() {
		return startTime;
	}

	protected int getState() {
		int state = flags & M_STATE;
		switch (state) {
		//blocked and yielding state is equivalent to waiting state for clients
			case YIELDING :
			case BLOCKED :
				return Job.WAITING;
			case ABOUT_TO_RUN :
				return Job.RUNNING;
			case ABOUT_TO_SCHEDULE :
				return Job.WAITING;
			default :
				return state;
		}
	}

	protected Thread getThread() {
		return thread;
	}

	protected JobGroup getJobGroup() {
		return jobGroup;
	}

	/**
	 * Returns the raw job state, including internal states no exposed as API.
	 */
	final int internalGetState() {
		return flags & M_STATE;
	}

	/**
	 * Must be called from JobManager#setPriority
	 */
	final void internalSetPriority(int newPriority) {
		this.priority = newPriority;
	}

	/**
	 * Must be called from JobManager#setRule
	 */
	final void internalSetRule(ISchedulingRule rule) {
		this.schedulingRule = rule;
	}

	/**
	 * Must be called from JobManager#changeState
	 */
	final void internalSetState(int i) {
		flags = (flags & ~M_STATE) | i;
	}

	/**
	 * Returns whether this job was canceled when it was about to run
	 */
	final boolean isAboutToRunCanceled() {
		return (flags & M_ABOUT_TO_RUN_CANCELED) != 0;
	}

	/**
	 * Returns whether this job was canceled when it was running.
	 */
	final boolean isRunCanceled() {
		return (flags & M_RUN_CANCELED) != 0;
	}

	protected boolean isBlocking() {
		return manager.isBlocking(this);
	}

	/**
	 * Returns true if this job conflicts with the given job, and false otherwise.
	 */
	final boolean isConflicting(InternalJob otherJob) {
		ISchedulingRule otherRule = otherJob.getRule();
		if (schedulingRule == null || otherRule == null)
			return false;
		//if one of the rules is a compound rule, it must be asked the question.
		if (schedulingRule.getClass() == MultiRule.class)
			return schedulingRule.isConflicting(otherRule);
		return otherRule.isConflicting(schedulingRule);
	}

	protected boolean isSystem() {
		return (flags & M_SYSTEM) != 0;
	}

	protected boolean isUser() {
		return (flags & M_USER) != 0;
	}

	protected void join() throws InterruptedException {
		manager.join(this, 0, null);
	}

	protected boolean join(long timeout, IProgressMonitor joinMonitor) throws InterruptedException, OperationCanceledException {
		return manager.join(this, timeout, joinMonitor);
	}

	/**
	 * Returns the next entry (ahead of this one) in the list, or null if there is no next entry
	 */
	final InternalJob next() {
		return next;
	}

	/**
	 * Returns the previous entry (behind this one) in the list, or null if there is no previous entry
	 */
	final InternalJob previous() {
		return previous;
	}

	/**
	 * Removes this entry from any list it belongs to.  Returns the receiver.
	 */
	final InternalJob remove() {
		if (next != null)
			next.setPrevious(previous);
		if (previous != null)
			previous.setNext(next);
		next = previous = null;
		return this;
	}

	protected void removeJobChangeListener(IJobChangeListener listener) {
		listeners.remove(listener);
	}

	protected abstract IStatus run(IProgressMonitor progressMonitor);

	protected void schedule(long delay) {
		if (shouldSchedule())
			manager.schedule(this, delay);
	}

	/**
	 * Sets whether this job was canceled when it was about to run
	 */
	final void setAboutToRunCanceled(boolean value) {
		synchronized (jobStateLock) {
			flags = value ? flags | M_ABOUT_TO_RUN_CANCELED : flags & ~M_ABOUT_TO_RUN_CANCELED;
		}

	}

	/**
	 * Sets whether this job was canceled when it was running
	 */
	final void setRunCanceled(boolean value) {
		synchronized (jobStateLock) {
			flags = value ? flags | M_RUN_CANCELED : flags & ~M_RUN_CANCELED;
		}
	}

	protected void setName(String name) {
		Assert.isNotNull(name);
		this.name = name;
	}

	/**
	 * Sets the next entry in this linked list of jobs.
	 * @param entry
	 */
	final void setNext(InternalJob entry) {
		this.next = entry;
	}

	/**
	 * Sets the previous entry in this linked list of jobs.
	 * @param entry
	 */
	final void setPrevious(InternalJob entry) {
		this.previous = entry;
	}

	protected void setPriority(int newPriority) {
		switch (newPriority) {
			case Job.INTERACTIVE :
			case Job.SHORT :
			case Job.LONG :
			case Job.BUILD :
			case Job.DECORATE :
				manager.setPriority(this, newPriority);
				break;
			default :
				throw new IllegalArgumentException(String.valueOf(newPriority));
		}
	}

	protected void setProgressGroup(IProgressMonitor group, int ticks) {
		Assert.isNotNull(group);
		IProgressMonitor pm = manager.createMonitor(this, group, ticks);
		if (pm != null)
			setProgressMonitor(pm);
	}

	/**
	 * Sets the progress monitor to use for the next execution of this job,
	 * or for clearing the monitor when a job completes.
	 * @param monitor a progress monitor
	 */
	final void setProgressMonitor(IProgressMonitor monitor) {
		this.monitor = monitor;
	}

	protected void setProperty(QualifiedName key, Object value) {
		// thread safety: (Concurrency001 - copy on write)
		if (value == null) {
			if (properties == null)
				return;
			ObjectMap<QualifiedName, Object> temp = new ObjectMap<>(properties);
			temp.remove(key);
			if (temp.isEmpty())
				properties = null;
			else
				properties = temp;
		} else {
			ObjectMap<QualifiedName, Object> temp = properties;
			if (temp == null)
				temp = new ObjectMap<>(5);
			else
				temp = new ObjectMap<>(properties);
			temp.put(key, value);
			properties = temp;
		}
	}

	/**
	 * Sets or clears the result of an execution of this job.
	 * @param result a result status, or <code>null</code>
	 * @GuardedBy("manager.lock")
	 */
	final void setResult(IStatus result) {
		this.result = result;
	}

	protected void setRule(ISchedulingRule rule) {
		manager.setRule(this, rule);
	}

	/**
	 * Sets a time to start, wake up, or schedule this job,
	 * depending on the current state
	 * @param time a time in milliseconds
	 * @GuardedBy("manager.lock")
	 */
	final void setStartTime(long time) {
		startTime = time;
	}

	protected void setSystem(boolean value) {
		synchronized (jobStateLock) {
			if (getState() != Job.NONE)
				throw new IllegalStateException();
			flags = value ? flags | M_SYSTEM : flags & ~M_SYSTEM;
		}
	}

	protected void setThread(Thread thread) {
		this.thread = thread;
	}

	protected void setUser(boolean value) {
		synchronized (jobStateLock) {
			if (getState() != Job.NONE)
				throw new IllegalStateException();
			flags = value ? flags | M_USER : flags & ~M_USER;
		}
	}

	protected void setJobGroup(JobGroup jobGroup) {
		if (getState() != Job.NONE)
			throw new IllegalStateException("Setting job group of an already scheduled job is not allowed"); //$NON-NLS-1$
		this.jobGroup = jobGroup;
	}

	protected boolean shouldSchedule() {
		return true;
	}

	protected boolean sleep() {
		return manager.sleep(this);
	}

	protected Job yieldRule(IProgressMonitor progressMonitor) {
		return manager.yieldRule(this, progressMonitor);
	}

	@Override
	public String toString() {
		return getName() + "(" + jobNumber + ")"; //$NON-NLS-1$//$NON-NLS-2$
	}

	protected void wakeUp(long delay) {
		manager.wakeUp(this, delay);
	}

	/**
	 * @param waitQueueStamp The waitQueueStamp to set.
	 * @GuardedBy("manager.lock")
	 */
	void setWaitQueueStamp(long waitQueueStamp) {
		this.waitQueueStamp = waitQueueStamp;
	}

	/**
	 * @return Returns the waitQueueStamp.
	 * @GuardedBy("manager.lock")
	 */
	long getWaitQueueStamp() {
		return waitQueueStamp;
	}

}
