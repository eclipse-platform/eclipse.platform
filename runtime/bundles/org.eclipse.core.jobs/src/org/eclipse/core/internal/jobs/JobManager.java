/*******************************************************************************
 * Copyright (c) 2003, 2022 IBM Corporation and others.
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
 *     Stephan Wahlbrink  - Fix for bug 200997.
 *     Danail Nachev - Fix for bug 109898
 *     Mike Moreaty - Fix for bug 289790
 *     Oracle Corporation - Fix for bug 316839
 *     Thirumala Reddy Mutchukota (thirumala@google.com) -
 *              Bug 432049, JobGroup API and implementation
 *              Bug 105821, Support for Job#join with timeout and progress monitor
 *     Jan Koehnlein - Fix for bug 60964 (454698)
 *     Terry Parker - Bug 457504, Publish a job group's final status to IJobChangeListeners
 *     Xored Software Inc - Fix for bug 550738
 *     Christoph Laeubrich 	- Issue #40
 *******************************************************************************/
package org.eclipse.core.internal.jobs;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.core.internal.runtime.RuntimeLog;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.osgi.service.debug.*;
import org.eclipse.osgi.util.NLS;

/**
 * Implementation of API type IJobManager
 *
 * Implementation note: all the data structures of this class are protected by a
 * single lock object held as a private field in this class. The JobManager
 * instance itself is not used because this class is publicly reachable, and
 * third party clients may try to synchronize on it.
 *
 * There are various locks used and held throughout the JobManager
 * implementation. When multiple locks interact, circular hold and waits must
 * never happen, or a deadlock will occur. To prevent deadlocks, this is the
 * order that locks must be acquired.
 *
 * WorkerPool -&gt; JobManager.implicitJobs -&gt; JobManager.lock -&gt;
 * InternalJob.jobStateLock or InternalJobGroup.jobGroupStateLock
 *
 * @ThreadSafe
 */
public class JobManager implements IJobManager, DebugOptionsListener {

	private static final int NANOS_IN_MS = 1_000_000;

	/**
	 * The unique identifier constant of this plug-in.
	 */
	public static final String PI_JOBS = "org.eclipse.core.jobs"; //$NON-NLS-1$

	/**
	 * Status code constant indicating an error occurred while running a plug-in.
	 * For backward compatibility with Platform.PLUGIN_ERROR left at (value = 2).
	 */
	public static final int PLUGIN_ERROR = 2;

	/**
	 * Determines how often the progress monitor is checked for cancellation during the join call.
	 */
	public static final long MAX_WAIT_INTERVAL = 100;

	private static final String OPTION_DEADLOCK_ERROR = PI_JOBS + "/jobs/errorondeadlock"; //$NON-NLS-1$
	private static final String OPTION_DEBUG_BEGIN_END = PI_JOBS + "/jobs/beginend"; //$NON-NLS-1$
	private static final String OPTION_DEBUG_YIELDING = PI_JOBS + "/jobs/yielding"; //$NON-NLS-1$
	private static final String OPTION_DEBUG_YIELDING_DETAILED = PI_JOBS + "/jobs/yielding/detailed"; //$NON-NLS-1$
	private static final String OPTION_DEBUG_JOBS = PI_JOBS + "/jobs"; //$NON-NLS-1$
	private static final String OPTION_LOCKS = PI_JOBS + "/jobs/locks"; //$NON-NLS-1$
	private static final String OPTION_SHUTDOWN = PI_JOBS + "/jobs/shutdown"; //$NON-NLS-1$

	static DebugTrace DEBUG_TRACE;
	static boolean DEBUG = false;
	static boolean DEBUG_BEGIN_END = false;
	static boolean DEBUG_YIELDING = false;
	static boolean DEBUG_YIELDING_DETAILED = false;
	static boolean DEBUG_DEADLOCK = false;
	static boolean DEBUG_LOCKS = false;
	static boolean DEBUG_SHUTDOWN = false;

	/**
	 * The singleton job manager instance. It must be a singleton because
	 * all job instances maintain a reference (as an optimization) and have no way
	 * of updating it.
	 */
	private static JobManager instance;

	/** Baseline value for current time calculations */
	private final long originTime = System.nanoTime();

	/**
	 * Last time (relative to originTime) returned by {@link #now()}
	 */
	private final AtomicLong currentTimeInMs;

	/**
	 * Scheduling rule used for validation of client-defined rules.
	 */
	private static final ISchedulingRule nullRule = new ISchedulingRule() {
		@Override
		public boolean contains(ISchedulingRule rule) {
			return rule == this;
		}

		@Override
		public boolean isConflicting(ISchedulingRule rule) {
			return rule == this;
		}
	};

	/**
	 * True if this manager is active, and false otherwise.  A job manager
	 * starts out active, and becomes inactive if it has been shutdown.
	 */
	private volatile boolean active = true;

	final ImplicitJobs implicitJobs = new ImplicitJobs(this);

	/**
	 * Listeners for the job lifecycle. It is important that the
	 * JobManager#JobGroupUpdater is the first one that is dispatched to, since
	 * it updates the JobChangeEvent#jobGroupStatus field, which other listeners
	 * may use.
	 */
	private final JobListeners jobListeners = new JobListeners();

	/**
	 * The lock for synchronizing all activity in the job manager.  To avoid deadlock,
	 * this lock must never be held for extended periods, and must never be
	 * held while third party code is being called.
	 * @GuardedBy("itself")
	 */
	private final Object lock = new Object();

	/**
	 * A job listener to check for the cancellation and completion of the job groups.
	 */
	private final IJobChangeListener jobGroupUpdater = new JobGroupUpdater(lock);

	private final LockManager lockManager = new LockManager();

	/**
	 * The pool of worker threads.
	 */
	private WorkerPool pool;

	/**
	 * @GuardedBy("lock")
	 */
	private ProgressProvider progressProvider = null;
	/**
	 * Jobs that are currently running. Should only be modified from changeState
	 * @GuardedBy("lock")
	 */
	private final HashSet<InternalJob> running;

	/**
	 * Jobs that are currently yielding. Should only be modified from changeState
	 * @GuardedBy("lock")
	 */
	private final HashSet<InternalJob> yielding;

	/**
	 * Jobs that are sleeping.  Some sleeping jobs are scheduled to wake
	 * up at a given start time, while others will sleep indefinitely until woken.
	 * Should only be modified from changeState
	 * @GuardedBy("lock")
	 */
	private final JobQueue sleeping;
	/**
	 * True if this manager has been suspended, and false otherwise.  A job manager
	 * starts out not suspended, and becomes suspended when <code>suspend</code>
	 * is invoked. Once suspended, no jobs will start running until <code>resume</code>
	 * is called.
	 * @GuardedBy("lock")
	 */
	private boolean suspended = false;

	/**
	 * jobs that are waiting to be run. Should only be modified from changeState
	 * @GuardedBy("lock")
	 */
	private final JobQueue waiting;

	/**
	 * ThreadJobs that are waiting to be run. Should only be modified from changeState
	 * @GuardedBy("lock")
	 */
	final JobQueue waitingThreadJobs;

	/**
	 * Counter to record wait queue insertion order.
	 * @GuardedBy("lock")
	 */
	private final AtomicLong waitQueueCounter = new AtomicLong();

	/**
	 * A set of progress monitors we must track cancellation requests for.
	 * @GuardedBy("itself")
	 */
	final List<Object[]> monitorStack = new ArrayList<>();

	private final InternalWorker internalWorker;

	public static void debug(String msg) {
		DEBUG_TRACE.trace(null, msg);
	}

	long getNextWaitQueueStamp() {
		return waitQueueCounter.getAndIncrement();
	}

	/**
	 * Returns the job manager singleton. For internal use only.
	 */
	static synchronized JobManager getInstance() {
		if (instance == null)
			new JobManager();
		return instance;
	}

	/**
	 * For debugging purposes only
	 */
	private static String printJobName(Job job) {
		if (job instanceof ThreadJob) {
			Job realJob = ((ThreadJob) job).realJob;
			if (realJob != null)
				return realJob.getClass().getName();
			return "ThreadJob on rule: " + job.getRule(); //$NON-NLS-1$
		}
		return job.getClass().getName();
	}

	/**
	 * For debugging purposes only
	 */
	public static String printState(Job job) {
		return printState(((InternalJob) job).internalGetState());
	}

	/**
	* For debugging purposes only
	*/
	public static String printState(int state) {
		switch (state) {
			case Job.NONE :
				return "NONE"; //$NON-NLS-1$
			case Job.WAITING :
				return "WAITING"; //$NON-NLS-1$
			case Job.SLEEPING :
				return "SLEEPING"; //$NON-NLS-1$
			case Job.RUNNING :
				return "RUNNING"; //$NON-NLS-1$
			case InternalJob.BLOCKED :
				return "BLOCKED"; //$NON-NLS-1$
			case InternalJob.YIELDING :
				return "YIELDING"; //$NON-NLS-1$
			case InternalJob.ABOUT_TO_RUN :
				return "ABOUT_TO_RUN"; //$NON-NLS-1$
			case InternalJob.ABOUT_TO_SCHEDULE :
				return "ABOUT_TO_SCHEDULE";//$NON-NLS-1$
		}
		return "UNKNOWN"; //$NON-NLS-1$
	}

	/**
	 * Note that although this method is not API, clients have historically used
	 * it to force jobs shutdown in cases where OSGi shutdown does not occur.
	 * For this reason, this method should be considered near-API and should not
	 * be changed if at all possible.
	 */
	public static void shutdown() {
		if (instance != null) {
			instance.doShutdown();
			instance = null;
		}
	}

	private JobManager() {
		currentTimeInMs = new AtomicLong(lifeTimeInMs());
		instance = this;
		synchronized (lock) {
			waiting = new JobQueue(false);
			waitingThreadJobs = new JobQueue(false, false);
			sleeping = new JobQueue(true);
			running = new HashSet<>(10);
			yielding = new HashSet<>(10);
			pool = new WorkerPool(this);
		}
		pool.setDaemon(JobOSGiUtils.getDefault().useDaemonThreads());
		internalWorker = new InternalWorker(this);
		internalWorker.setDaemon(JobOSGiUtils.getDefault().useDaemonThreads());
		internalWorker.start();
		jobListeners.add(jobGroupUpdater);
	}

	@Override
	public void addJobChangeListener(IJobChangeListener listener) {
		jobListeners.add(listener);
	}

	@Override
	public void beginRule(ISchedulingRule rule, IProgressMonitor monitor) {
		validateRule(rule);
		implicitJobs.begin(rule, monitorFor(monitor), false);
	}

	/**
	 * Cancels a job
	 */
	protected boolean cancel(InternalJob job) {
		IProgressMonitor monitor = null;
		boolean runCanceling = false;
		synchronized (lock) {
			// signal that the job should be canceled before it gets a chance to run
			job.setAboutToRunCanceled(true);
			switch (job.getState()) {
				case Job.NONE :
					return true;
				case Job.RUNNING :
					//cannot cancel a job that has already started (as opposed to ABOUT_TO_RUN)
					if (job.internalGetState() == Job.RUNNING) {
						monitor = job.getProgressMonitor();
						runCanceling = !job.isRunCanceled();
						if (runCanceling)
							job.setRunCanceled(true);
						break;
					}
					return false;
				default :
					changeState(job, Job.NONE);
			}
			if (monitor == null) {
				// only notify listeners if the job was waiting or sleeping
				jobListeners.queueDone((Job) job, Status.CANCEL_STATUS, false);
			}
		}
		//call monitor and canceling outside sync block
		if (monitor != null) {
			if (runCanceling) {
				if (!monitor.isCanceled())
					monitor.setCanceled(true);
				job.canceling();
			}
			return false;
		}
		jobListeners.sendEvents(job);
		return true;
	}

	@Override
	public void cancel(Object family) {
		//don't synchronize because cancel calls listeners
		for (InternalJob internalJob : select(family))
			cancel(internalJob);
	}

	void cancel(InternalJobGroup jobGroup) {
		cancel(jobGroup, false);
	}

	void cancel(InternalJobGroup jobGroup, boolean cancelDueToError) {
		Assert.isLegal(jobGroup != null, "jobGroup should not be null"); //$NON-NLS-1$
		synchronized (lock) {
			switch (jobGroup.getState()) {
				case JobGroup.NONE :
					return;
				case JobGroup.CANCELING :
					if (!cancelDueToError) {
						// User cancellation takes precedence over the cancel due to error.
						jobGroup.updateCancelingReason(cancelDueToError);
					}
					return;
				default :
					jobGroup.cancelAndNotify(cancelDueToError);
			}
		}
	}

	/**
	 * Atomically updates the state of a job, adding or removing from the
	 * necessary queues or sets.
	 */
	private void changeState(InternalJob job, int newState) {
		boolean blockedJobs = false;
		synchronized (lock) {
			int oldJobState;
			synchronized (job.jobStateLock) {
				job.jobStateLock.notifyAll();
				oldJobState = job.getState();
				int oldState = job.internalGetState();
				switch (oldState) {
					case InternalJob.YIELDING :
						yielding.remove(job);
					case Job.NONE :
					case InternalJob.ABOUT_TO_SCHEDULE :
						break;
					case InternalJob.BLOCKED :
						//remove this job from the linked list of blocked jobs
						job.remove();
						break;
					case Job.WAITING :
						try {
							waiting.remove(job);
						} catch (RuntimeException e) {
							Assert.isLegal(false, "Tried to remove a job that wasn't in the queue"); //$NON-NLS-1$
						}
						break;
					case Job.SLEEPING :
						try {
							sleeping.remove(job);
						} catch (RuntimeException e) {
							Assert.isLegal(false, "Tried to remove a job that wasn't in the queue"); //$NON-NLS-1$
						}
						break;
					case Job.RUNNING :
					case InternalJob.ABOUT_TO_RUN :
						running.remove(job);
						//add any blocked jobs back to the wait queue
						InternalJob blocked = job.previous();
						job.remove();
						blockedJobs = blocked != null;
						while (blocked != null) {
							InternalJob previous = blocked.previous();
							changeState(blocked, Job.WAITING);
							blocked = previous;
						}
						break;
					default :
						Assert.isLegal(false, "Invalid job state: " + job + ", state: " + oldState); //$NON-NLS-1$ //$NON-NLS-2$
				}
				job.internalSetState(newState);
				switch (newState) {
					case Job.NONE :
						job.setStartTime(InternalJob.T_NONE);
						job.setWaitQueueStamp(InternalJob.T_NONE);
						job.setRunCanceled(false);
					case InternalJob.BLOCKED :
						break;
					case Job.WAITING :
						waiting.enqueue(job);
						break;
					case Job.SLEEPING :
						try {
							sleeping.enqueue(job);
						} catch (RuntimeException e) {
							throw new RuntimeException("Error changing from state: " + oldState); //$NON-NLS-1$
						}
						break;
					case Job.RUNNING :
					case InternalJob.ABOUT_TO_RUN :
						// These flags must be reset in all cases, including resuming from yield
						job.setStartTime(InternalJob.T_NONE);
						job.setWaitQueueStamp(InternalJob.T_NONE);
						running.add(job);
						break;
					case InternalJob.YIELDING :
						yielding.add(job);
					case InternalJob.ABOUT_TO_SCHEDULE :
						break;
					default :
						Assert.isLegal(false, "Invalid job state: " + job + ", state: " + newState); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}

			InternalJobGroup jobGroup = job.getJobGroup();
			if (jobGroup != null) {
				jobGroup.jobStateChanged(job, oldJobState, job.getState());
			}
		}

		//notify queue outside sync block
		if (blockedJobs)
			pool.jobQueued();
	}

	/**
	 * Returns a new progress monitor for this job, belonging to the given
	 * progress group.  Returns null if it is not a valid time to set the job's group.
	 */
	protected IProgressMonitor createMonitor(InternalJob job, IProgressMonitor group, int ticks) {
		synchronized (lock) {
			//group must be set before the job is scheduled
			//this includes the ABOUT_TO_SCHEDULE state, during which it is still
			//valid to set the progress monitor
			if (job.getState() != Job.NONE)
				return null;
			IProgressMonitor monitor = null;
			if (progressProvider != null)
				monitor = progressProvider.createMonitor((Job) job, group, ticks);
			if (monitor == null)
				monitor = new NullProgressMonitor();
			return monitor;
		}
	}

	/**
	 * Returns a new progress monitor for this job.  Never returns null.
	 * @GuardedBy("lock")
	 */
	private IProgressMonitor createMonitor(Job job) {
		IProgressMonitor monitor = null;
		if (progressProvider != null)
			monitor = progressProvider.createMonitor(job);
		if (monitor == null)
			monitor = new NullProgressMonitor();
		return monitor;
	}

	@Override
	public IProgressMonitor createProgressGroup() {
		if (progressProvider != null)
			return progressProvider.createProgressGroup();
		return new NullProgressMonitor();
	}

	@Override
	public Job currentJob() {
		Thread current = Thread.currentThread();
		if (current instanceof Worker)
			return ((Worker) current).currentJob();
		synchronized (lock) {
			for (InternalJob internalJob : running) {
				Job job = (Job) internalJob;
				if (job.getThread() == current)
					return job;
			}
		}
		return null;
	}

	@Override
	public ISchedulingRule currentRule() {
		//check thread job first, because actual current job may have null rule
		Job currentJob = implicitJobs.getThreadJob(Thread.currentThread());
		if (currentJob != null)
			return currentJob.getRule();
		currentJob = currentJob();
		if (currentJob != null)
			return currentJob.getRule();
		return null;
	}

	/**
	 * Returns the delay in milliseconds that a job with a given priority can
	 * tolerate waiting.
	 */
	private long delayFor(int priority) {
		//these values may need to be tweaked based on machine speed
		switch (priority) {
			case Job.INTERACTIVE :
				return 0L;
			case Job.SHORT :
				return 50L;
			case Job.LONG :
				return 100L;
			case Job.BUILD :
				return 500L;
			case Job.DECORATE :
				return 1000L;
			default :
				Assert.isTrue(false, "Job has invalid priority: " + priority); //$NON-NLS-1$
				return 0;
		}
	}

	/**
	 * Performs the scheduling of a job.
	 *
	 * @return true on success, false if cancelled, or scheduled by another thread
	 */
	private boolean doSchedule(InternalJob job, long delay) {
		boolean cancelling = false;
		synchronized (lock) {
			//job may have been canceled already
			int state = job.internalGetState();
			if (state != InternalJob.ABOUT_TO_SCHEDULE && state != Job.SLEEPING)
				return false;

			if (job.isAboutToRunCanceled()) {
				cancelling = true;
				job.setResult(Status.CANCEL_STATUS);
				job.setProgressMonitor(null);
				job.setThread(null);
				changeState(job, Job.NONE);
			} else {
				// if it's a decoration job with no rule, don't run it right now if the system
				// is busy
				if (job.getPriority() == Job.DECORATE && job.getRule() == null) {
					long minDelay = running.size() * 100;
					delay = Math.max(delay, minDelay);
				}
				if (delay > 0) {
					job.setStartTime(now() + delay);
					changeState(job, Job.SLEEPING);
				} else {
					job.setStartTime(now() + delayFor(job.getPriority()));
					job.setWaitQueueStamp(getNextWaitQueueStamp());
					changeState(job, Job.WAITING);
				}
			}
			if (cancelling) {
				jobListeners.queueDone((Job) job, Status.CANCEL_STATUS, false);
			}
		}
		return !cancelling;
	}

	/**
	 * Shuts down the job manager.  Currently running jobs will be told
	 * to stop, but worker threads may still continue processing.
	 * (note: This implemented IJobManager.shutdown which was removed
	 * due to problems caused by premature shutdown)
	 */
	private void doShutdown() {
		Job[] toCancel = null;
		synchronized (lock) {
			if (!active)
				return;
			active = false;
			//cancel all running jobs
			toCancel = running.toArray(new Job[running.size()]);
			//discard any jobs that have not yet started running
			sleeping.clear();
			waiting.clear();
		}

		// Give running jobs a chance to finish. Wait 0.1 seconds for up to 3 times.
		if (toCancel != null && toCancel.length > 0) {
			for (Job element : toCancel) {
				cancel(element); // cancel jobs outside sync block to avoid deadlock
			}

			for (int waitAttempts = 0; waitAttempts < 3; waitAttempts++) {
				Thread.yield();
				synchronized (lock) {
					if (running.isEmpty())
						break;
				}
				if (DEBUG_SHUTDOWN) {
					JobManager.debug("Shutdown - job wait cycle #" + (waitAttempts + 1)); //$NON-NLS-1$
					Job[] stillRunning = null;
					synchronized (lock) {
						stillRunning = running.toArray(new Job[running.size()]);
					}
					if (stillRunning != null) {
						for (Job element : stillRunning) {
							JobManager.debug("\tJob: " + printJobName(element)); //$NON-NLS-1$
						}
					}
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					//ignore
				}
				Thread.yield();
			}

			synchronized (lock) { // retrieve list of the jobs that are still running
				toCancel = running.toArray(new Job[running.size()]);
			}
		}
		internalWorker.cancel();
		if (toCancel != null) {
			for (Job element : toCancel) {
				String jobName = printJobName(element);
				//this doesn't need to be translated because it's just being logged
				String msg = "Job found still running after platform shutdown.  Jobs should be canceled by the plugin that scheduled them during shutdown: " + jobName; //$NON-NLS-1$
				RuntimeLog.log(new Status(IStatus.WARNING, JobManager.PI_JOBS, JobManager.PLUGIN_ERROR, msg, null));

				// TODO the RuntimeLog.log in its current implementation won't produce a log
				// during this stage of shutdown. For now add a standard error output.
				// One the logging story is improved, the System.err output below can be removed:
				System.err.println(msg);
			}
		}
		synchronized (lock) {
			//discard reference to any jobs still running at this point
			running.clear();
		}

		pool.shutdown();
		jobListeners.remove(jobGroupUpdater);
	}

	/**
	 * Indicates that a job was running, and has now finished.  Note that this method
	 * can be called under OutOfMemoryError conditions and thus must be paranoid
	 * about allocating objects.
	 */
	protected void endJob(InternalJob job, IStatus result, boolean notify) {
		long rescheduleDelay = InternalJob.T_NONE;
		final boolean reschedule;
		final boolean scheduled;
		synchronized (lock) {
			//if the job is finishing asynchronously, there is nothing more to do for now
			if (result == Job.ASYNC_FINISH)
				return;
			//if job is not known then it cannot be done
			if (job.getState() == Job.NONE)
				return;
			if (JobManager.DEBUG && notify)
				JobManager.debug("Ending job: " + job); //$NON-NLS-1$
			job.setResult(result);
			job.setProgressMonitor(null);
			job.setThread(null);
			rescheduleDelay = job.getStartTime(); // XXX time used as delay
			changeState(job, Job.NONE); // removes from #running without adding to #sleeping or #waiting
			reschedule = active && rescheduleDelay > InternalJob.T_NONE && job.shouldSchedule();
			if (reschedule) {
				// adds to #sleeping or #waiting
				// since reschedule==true doSchedule will not notify CANCEL_STATUS
				scheduled = scheduleInternal(job, rescheduleDelay, reschedule);
			} else {
				scheduled = false;
			}
			if (notify)
				jobListeners.queueDone((Job) job, result, reschedule);
		}
		//reschedule the job if requested and we are still active
		if (scheduled) {
			pool.jobQueued();
		}
		//notify listeners outside sync block
		jobListeners.sendEvents(job);

		//log result if it is warning or error. When the job belongs to a job group defer the logging
		//until the whole group is completed (see JobManager#updateJobGroup).
		if (job.getJobGroup() == null && result.matches(IStatus.ERROR | IStatus.WARNING))
			RuntimeLog.log(result);
	}

	@Override
	public void endRule(ISchedulingRule rule) {
		implicitJobs.end(rule, false);
	}

	@Override
	public Job[] find(Object family) {
		List<InternalJob> members = select(family);
		return members.toArray(new Job[members.size()]);
	}

	List<Job> find(InternalJobGroup jobGroup) {
		Assert.isLegal(jobGroup != null, "jobGroup should not be null"); //$NON-NLS-1$
		synchronized (lock) {
			return jobGroup.internalGetActiveJobs();
		}
	}

	/**
	 * Returns a running or blocked job whose scheduling rule conflicts with the
	 * scheduling rule of the given waiting job.  Returns null if there are no
	 * conflicting jobs.  A job can only run if there are no running jobs and no blocked
	 * jobs whose scheduling rule conflicts with its rule.
	 */
	protected InternalJob findBlockingJob(InternalJob waitingJob) {
		if (waitingJob.getRule() == null)
			return null;
		synchronized (lock) {
			if (running.isEmpty())
				return null;
			//check the running jobs
			boolean hasBlockedJobs = false;
			for (InternalJob job : running) {
				if (waitingJob.isConflicting(job))
					return job;
				if (!hasBlockedJobs)
					hasBlockedJobs = job.previous() != null;
			}
			//there are no blocked jobs, so we are done
			if (!hasBlockedJobs)
				return null;
			//check all jobs blocked by running jobs
			for (InternalJob job : running) {
				while (true) {
					job = job.previous();
					if (job == null)
						break;
					if (waitingJob.isConflicting(job))
						return job;
				}
			}
		}
		return null;
	}

	/**
	 * Returns a job from the given collection whose scheduling rule conflicts
	 * with the scheduling rule of the given job.  Returns null if there are no
	 * conflicting jobs.
	 */
	private InternalJob findBlockedJob(InternalJob job) {
		synchronized (lock) {
			for (InternalJob waitingJob : waitingThreadJobs) {
				if (waitingJob.isConflicting(job)) {
					return waitingJob;
				}
			}
			return null;
		}
	}

	void dequeue(JobQueue queue, InternalJob job) {
		synchronized (lock) {
			queue.remove(job);
		}
	}

	void enqueue(JobQueue queue, InternalJob job) {
		synchronized (lock) {
			queue.enqueue(job);
		}
	}

	public LockManager getLockManager() {
		return lockManager;
	}

	/**
	 * Returns a translated message indicating we are waiting for the given
	 * number of jobs to complete.
	 */
	private String getWaitMessage(int jobCount) {
		String message = jobCount == 1 ? JobMessages.jobs_waitFamSubOne : JobMessages.jobs_waitFamSub;
		return NLS.bind(message, Integer.toString(jobCount));
	}

	/**
	 * Returns whether the job manager is active (has not been shutdown).
	 */
	protected boolean isActive() {
		return active;
	}

	/**
	 * Returns true if the given job is blocking the execution of a non-system
	 * job.
	 */
	protected boolean isBlocking(InternalJob runningJob) {
		synchronized (lock) {
			// if this job isn't running, it can't be blocking anyone
			if (runningJob.getState() != Job.RUNNING)
				return false;
			// if any job is queued behind this one, it is blocked by it
			InternalJob previous = runningJob.previous();
			while (previous != null) {
				// ignore jobs of lower priority (higher priority value means lower priority)
				if (previous.getPriority() < runningJob.getPriority()) {
					if (!previous.isSystem())
						return true;
					// implicit jobs should interrupt unless they act on behalf of system jobs
					if (previous instanceof ThreadJob && ((ThreadJob) previous).shouldInterrupt())
						return true;
				}
				previous = previous.previous();
			}
			// consider threads waiting on IJobManager#beginRule
			for (InternalJob waitingThreadJob : waitingThreadJobs) {
				ThreadJob waitingJob = (ThreadJob) waitingThreadJob;
				if (runningJob.isConflicting(waitingJob) && waitingJob.shouldInterrupt())
					return true;
			}
			// none found
			return false;
		}
	}

	@Override
	public boolean isIdle() {
		synchronized (lock) {
			return running.isEmpty() && waiting.isEmpty();
		}
	}

	@Override
	public boolean isSuspended() {
		synchronized (lock) {
			return suspended;
		}
	}

	protected boolean join(InternalJob job, long timeout, IProgressMonitor monitor) throws InterruptedException {
		Assert.isLegal(timeout >= 0, "timeout should not be negative"); //$NON-NLS-1$
		long deadline = timeout == 0 ? 0 : now() + timeout;

		Job currentJob = currentJob();
		if (currentJob != null) {
			JobGroup jobGroup = currentJob.getJobGroup();
			if (timeout == 0 && jobGroup != null && jobGroup.getMaxThreads() != 0 && jobGroup == job.getJobGroup())
				throw new IllegalStateException("Joining on a job belonging to the same group is not allowed"); //$NON-NLS-1$
		}

		final IJobChangeListener listener;
		final Semaphore barrier;
		synchronized (lock) {
			int state = job.getState();
			if (state == Job.NONE)
				return true;
			//don't join a waiting or sleeping job when suspended (deadlock risk)
			if (suspended && state != Job.RUNNING)
				return true;
			//it's an error for a job to join itself
			if (state == Job.RUNNING && job.getThread() == Thread.currentThread())
				throw new IllegalStateException("Job attempted to join itself"); //$NON-NLS-1$
			//the semaphore will be released when the job is done
			barrier = new Semaphore(null);
			listener = new JobChangeAdapter() {
				@Override
				public void done(IJobChangeEvent event) {
					barrier.release();
				}
			};
			job.addJobChangeListener(listener);
		}

		//wait until listener notifies this thread.
		try {
			boolean canBlock = lockManager.canBlock();
			while (true) {
				if (monitor != null && monitor.isCanceled())
					throw new OperationCanceledException();
				long remainingTime = deadline;
				if (deadline != 0) {
					remainingTime -= now();
					if (remainingTime <= 0) {
						return false;
					}
				}
				//notify hook to service pending syncExecs before falling asleep
				lockManager.aboutToWait(job.getThread());
				try {
					// If remaining time is greater than MAX_WAIT_INTERVAL, sleep only for
					// MAX_WAIT_INTERVAL instead to be more responsive to monitor cancellation.
					long sleepTime = remainingTime != 0 && remainingTime <= MAX_WAIT_INTERVAL ? remainingTime : MAX_WAIT_INTERVAL;
					if (barrier.acquire(sleepTime))
						break;
				} catch (InterruptedException e) {
					// if non-UI thread, re-throw the exception
					if (canBlock)
						throw e;
					// if UI thread, loop and keep trying
				}
			}
		} finally {
			lockManager.aboutToRelease();
			job.removeJobChangeListener(listener);
		}
		return true;
	}

	@Override
	public void join(final Object family, IProgressMonitor monitor) throws InterruptedException, OperationCanceledException {
		monitor = monitorFor(monitor);
		IJobChangeListener listener = null;
		final Set<InternalJob> jobs;
		int jobCount;
		synchronized (lock) {
			//don't join a waiting or sleeping job when suspended (deadlock risk)
			int states = suspended ? Job.RUNNING : Job.RUNNING | Job.WAITING | Job.SLEEPING;
			jobs = Collections.synchronizedSet(new HashSet<>(select(family, states)));
			jobCount = jobs.size();
			if (jobCount > 0) {
				listener = new JobChangeAdapter() {
					/* Normally called in the thread that did executed the job. */
					@Override
					public void done(IJobChangeEvent event) {
						//don't remove from list if job is being rescheduled
						if (!((JobChangeEvent) event).reschedule) {
							jobs.remove(event.getJob()); // sometimes nothing to remove anymore
							if (jobs.isEmpty()) { // minimal notification
								synchronized (jobs) {
									jobs.notifyAll();
								}
							}
						}
					}

					/*
					 * Normally called in the thread that will execute the job (may be another thread
					 * then the last execute). Will (now) always be reported after done() finished
					 */
					@Override
					public void running(IJobChangeEvent event) {
						Job job = event.getJob();
						if (family == null || job.belongsTo(family)) {
							jobs.add(job); // never really adds, as it already was added by scheduled()
							// no notification upon increased size
						}
					}

					/*
					 * Normally called in the thread that scheduled the job - which may be the job's
					 * thread itself or an unrelated thread. This may happen after running() is
					 * reported.
					 */
					@Override
					public void scheduled(IJobChangeEvent event) {
						//don't add to list if job is being rescheduled
						if (((JobChangeEvent) event).reschedule)
							return;
						//if job manager is suspended we only wait for running jobs
						if (isSuspended())
							return;
						Job job = event.getJob();
						if (family == null || job.belongsTo(family)) {
							jobs.add(job);
							// no notification upon increased size
						}
					}
				};
				addJobChangeListener(listener);
			}
		}
		if (jobCount == 0) {
			//use up the monitor outside synchronized block because monitors call untrusted code
			monitor.beginTask(JobMessages.jobs_blocked0, 1);
			monitor.done();
			return;
		}
		int blockReports = 0;
		//spin until all jobs are completed
		try {
			monitor.beginTask(JobMessages.jobs_blocked0, jobCount);
			monitor.subTask(getWaitMessage(jobCount));
			int reportedWorkDone = 0;
			Set<InternalJob> reportedBlockingJobs = Set.of();
			boolean first = true;
			while (true) {
				Set<InternalJob> blockingJobs;
				synchronized (jobs) {
					if (jobs.isEmpty()) {
						break;
					}
					if (first) {
						first = false;
						// don't wait in the first iteration.
					} else {
						jobs.wait(100);// notified by JobChangeAdapter when empty
						// check again in case of notification:
						if (jobs.isEmpty()) {
							break;
						}
					}
					blockingJobs = new HashSet<>(jobs);
				}
				int jobsLeft = blockingJobs.size();
				if (!Objects.equals(reportedBlockingJobs, blockingJobs)) {
					blockReports++; // see WorkbenchDialogBlockedHandler.nestingDepth
					reportBlocked(monitor, blockingJobs);
					reportedBlockingJobs = blockingJobs;
				}
				//don't let there be negative work done if new jobs have
				//been added since the join began
				int actualWorkDone = Math.max(0, jobCount - jobsLeft);
				if (reportedWorkDone < actualWorkDone) {
					monitor.worked(actualWorkDone - reportedWorkDone);
					reportedWorkDone = actualWorkDone;
					monitor.subTask(getWaitMessage(jobsLeft));
				}
				if (Thread.interrupted())
					throw new InterruptedException();
				if (monitor.isCanceled())
					throw new OperationCanceledException();
				//notify hook to service pending syncExecs before falling asleep
				lockManager.aboutToWait(null);
			}
		} finally {
			lockManager.aboutToRelease();
			removeJobChangeListener(listener);
			for (int i = 0; i < blockReports; i++) {
				reportUnblocked(monitor);
			}
			monitor.done();
		}
	}

	boolean join(InternalJobGroup jobGroup, long timeout, IProgressMonitor monitor) throws InterruptedException, OperationCanceledException {
		Assert.isLegal(jobGroup != null, "jobGroup should not be null"); //$NON-NLS-1$
		Assert.isLegal(timeout >= 0, "timeout should not be negative"); //$NON-NLS-1$
		long deadline = timeout == 0 ? 0 : now() + timeout;
		int jobCount;
		synchronized (lock) {
			jobCount = jobGroup.getActiveJobsCount();
		}

		SubMonitor subMonitor = SubMonitor.convert(monitor, JobMessages.jobs_blocked0, jobCount);
		try {
			while (true) {
				if (subMonitor.isCanceled())
					throw new OperationCanceledException();
				long remainingTime = deadline;
				if (deadline != 0) {
					remainingTime -= now();
					if (remainingTime <= 0) {
						return false;
					}
				}
				synchronized (lock) {
					if ((suspended && jobGroup.getRunningJobsCount() == 0))
						break;
				}
				if (jobGroup.doJoin(remainingTime))
					break;
				int jobsLeft;
				synchronized (lock) {
					jobsLeft = jobGroup.getActiveJobsCount();
				}
				if (jobsLeft < jobCount)
					subMonitor.worked(jobCount - jobsLeft);

				jobCount = jobsLeft;
				subMonitor.setWorkRemaining(jobCount);
				subMonitor.subTask(getWaitMessage(jobCount));
			}
		} finally {
			if (monitor != null) {
				monitor.done();
			}
		}
		return true;
	}

	/**
	 * Returns a non-null progress monitor instance. If a progress provider is set,
	 * it is asked to provide a wrapped instance of the given <code>monitor</code>
	 * parameter. If no progress provider is set and <code>monitor</code> is null, a
	 * NullProgressMonitor is returned in all other cases the given
	 * <code>monitor</code> instance.
	 *
	 * @param monitor the monitor instance (could be null) for further usage
	 * @return a non-null progress monitor instance.
	 */
	private IProgressMonitor monitorFor(IProgressMonitor monitor) {
		if (progressProvider != null) {
			try {
				IProgressMonitor monitorFor = progressProvider.monitorFor(monitor);
				if (monitorFor == null) {
					throw new IllegalStateException("Internal error: " + //$NON-NLS-1$
							progressProvider.getClass().getName() + "#monitorFor(" + monitor + ") returned null!"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				return monitorFor;
			} catch (Exception e) {
				String msg = NLS.bind(JobMessages.meta_pluginProblems, JobManager.PI_JOBS);
				RuntimeLog.log(new Status(IStatus.ERROR, JobManager.PI_JOBS, JobManager.PLUGIN_ERROR, msg, e));
			}
		}

		if (monitor == null)
			return new NullProgressMonitor();
		return monitor;
	}

	@Override
	public ILock newLock() {
		return lockManager.newLock();
	}

	/**
	 * Removes and returns the first waiting job in the queue which is ready to run.
	 * Returns null if there are no items waiting in the queue.  If an item is
	 * removed from the queue, it is moved to the running jobs list.
	 */
	private Job nextJob() {
		synchronized (lock) {
			// do nothing if the job manager is suspended
			if (suspended)
				return null;
			// tickle the sleep queue to see if anyone wakes up
			long now = now();
			InternalJob job = sleeping.peek();
			while (job != null && job.getStartTime() < now) {
				job.setStartTime(now + delayFor(job.getPriority()));
				job.setWaitQueueStamp(getNextWaitQueueStamp());
				changeState(job, Job.WAITING);
				job = sleeping.peek();
			}
			InternalJobGroup jobGroup = null;
			// process the wait queue until we find a job whose rules are satisfied.
			job = waiting.peek();
			while (job != null) {
				InternalJob blocker = findBlockingJob(job);
				jobGroup = job.getJobGroup();
				// previous() method returns the next job in the queue.
				InternalJob nextWaitingJob = job.previous();
				if (blocker != null) {
					// queue this job after the job that's blocking it
					changeState(job, InternalJob.BLOCKED);
					// assert job does not already belong to some other data structure
					Assert.isTrue(job.next() == null);
					Assert.isTrue(job.previous() == null);
					blocker.addLast(job);
				} else if (jobGroup == null || jobGroup.getMaxThreads() == 0 || (jobGroup.getState() != JobGroup.CANCELING && jobGroup.getRunningJobsCount() < jobGroup.getMaxThreads())) {
					break;
				}
				// skip this job as either this job is blocked on another job or
				// the maximum number of jobs from the same group are already running.
				job = nextWaitingJob == waiting.dummy ? null : nextWaitingJob;
			}
			// the job to run must be in the running list before we exit
			// the sync block, otherwise two jobs with conflicting rules could start at once
			if (job != null) {
				changeState(job, InternalJob.ABOUT_TO_RUN);
				if (JobManager.DEBUG)
					JobManager.debug("Starting job: " + job); //$NON-NLS-1$
			}
			return (Job) job;
		}
	}

	/**
	 * Calculates some relative time value in milliseconds. This value does not
	 * represent wall clock time and can only be used to compare it with another
	 * value returned from this function, and to measure time elapsed in
	 * millisceconds between two calls of this method. The values returned here are
	 * supposed to be always non negative and greater or equal to previously
	 * returned values, but they do not guarantee to grow. The values returned here
	 * are consistent across multiple threads.
	 *
	 * @return current time as a result of the {@code Math.max()} function applied
	 *         to last calculated time and current value of {@link #lifeTimeInMs()}.
	 * @see System#nanoTime()
	 */
	public long now() {
		long now = currentTimeInMs.updateAndGet(lastValue -> Math.max(lastValue, lifeTimeInMs()));
		return now;
	}

	/**
	 * @return time in milliseconds since creation of this
	 *         {@codeJobManager} instance, greater or equal zero
	 */
	private long lifeTimeInMs() {
		long now = Math.max(System.nanoTime() - originTime, 0);
		return now / NANOS_IN_MS;
	}

	@Override
	public void optionsChanged(DebugOptions options) {
		DEBUG_TRACE = options.newDebugTrace(PI_JOBS);
		DEBUG = options.getBooleanOption(OPTION_DEBUG_JOBS, false);
		DEBUG_BEGIN_END = options.getBooleanOption(OPTION_DEBUG_BEGIN_END, false);
		DEBUG_YIELDING = options.getBooleanOption(OPTION_DEBUG_YIELDING, false);
		DEBUG_YIELDING_DETAILED = options.getBooleanOption(OPTION_DEBUG_YIELDING_DETAILED, false);
		DEBUG_DEADLOCK = options.getBooleanOption(OPTION_DEADLOCK_ERROR, false);
		DEBUG_LOCKS = options.getBooleanOption(OPTION_LOCKS, false);
		DEBUG_SHUTDOWN = options.getBooleanOption(OPTION_SHUTDOWN, false);
	}

	@Override
	public void removeJobChangeListener(IJobChangeListener listener) {
		jobListeners.remove(listener);
	}

	/**
	 * Report to the progress monitor that this thread is blocked, supplying an
	 * information message, and if possible jobs that are causing the blockage.
	 * Important: An invocation of this method MUST be followed eventually be an
	 * invocation of reportUnblocked.
	 *
	 * @param monitor      The monitor to report blocking to
	 * @param blockingJobs The jobs that are blocking this thread
	 * @see #reportUnblocked
	 */
	final void reportBlocked(IProgressMonitor monitor, Collection<InternalJob> blockingJobs) {
		IStatus reason;
		InternalJob blockingJob = blockingJobs.stream().sorted(Comparator.comparing(InternalJob::isSystem)).findFirst()
				.orElse(null);
		if (blockingJob == null || blockingJob instanceof ThreadJob || blockingJob.isSystem()) {
			reason = new Status(IStatus.INFO, JobManager.PI_JOBS, 1, JobMessages.jobs_blocked0, null);
		} else {
			String msg = NLS.bind(JobMessages.jobs_blocked1, blockingJob.getName());
			reason = new JobStatus(IStatus.INFO, (Job) blockingJob, msg);
		}
		monitor.setBlocked(reason);
	}

	/**
	 * Reports that this thread was blocked, but is no longer blocked and is able
	 * to proceed.
	 * @param monitor The monitor to report unblocking to.
	 * @see #reportBlocked
	 */
	final void reportUnblocked(IProgressMonitor monitor) {
		monitor.clearBlocked();
	}

	@Override
	public final void resume() {
		synchronized (lock) {
			suspended = false;
			//poke the job pool
			pool.jobQueued();
		}
	}

	@Deprecated
	@Override
	public final void resume(ISchedulingRule rule) {
		implicitJobs.resume(rule);
	}

	/**
	 * Attempts to immediately start a given job.  Returns null if the job was
	 * successfully started, and the blocking job if it could not be started immediately
	 * due to a currently running job with a conflicting rule.  Listeners will never
	 * be notified of jobs that are run in this way.
	 */
	protected InternalJob runNow(ThreadJob job, boolean releaseWaiting) {
		if (releaseWaiting) {
			synchronized (implicitJobs) {
				synchronized (lock) {
					return doRunNow(job, releaseWaiting);
				}
			}
		}
		synchronized (lock) {
			return doRunNow(job, releaseWaiting);
		}
	}

	private InternalJob doRunNow(ThreadJob job, boolean releaseWaiting) {
		InternalJob blocking = findBlockingJob(job);
		//cannot start if there is a conflicting job
		if (blocking == null) {
			changeState(job, Job.RUNNING);
			((InternalJob) job).setProgressMonitor(new NullProgressMonitor());
			job.run(null);
			if (releaseWaiting) {
				// atomically release waiting
				implicitJobs.removeWaiting(job);
			}
		}
		return blocking;
	}

	protected void schedule(InternalJob job, long delay, boolean reschedule) {
		if (scheduleInternal(job, delay, reschedule)) {
			pool.jobQueued();
			jobListeners.sendEvents(job);
		}
	}

	protected boolean scheduleInternal(InternalJob job, long delay, boolean reschedule) {
		if (!active)
			throw new IllegalStateException("Job manager has been shut down."); //$NON-NLS-1$
		Assert.isNotNull(job, "Job is null"); //$NON-NLS-1$
		Assert.isLegal(delay >= 0, "Scheduling delay is negative"); //$NON-NLS-1$
		synchronized (lock) {
			if (!reschedule)
				job.setAboutToRunCanceled(false);
			//if the job is already running, set it to be rescheduled when done
			if (job.getState() == Job.RUNNING) {
				job.setStartTime(delay); //XXX delay used as time
				return false;
			}
			//can't schedule a job that is waiting or sleeping
			if (job.internalGetState() != Job.NONE)
				return false;
			if (JobManager.DEBUG)
				JobManager.debug("Scheduling job: " + job); //$NON-NLS-1$
			//remember that we are about to schedule the job
			//to prevent multiple schedule attempts from succeeding (bug 68452)
			changeState(job, InternalJob.ABOUT_TO_SCHEDULE);
			jobListeners.queueScheduled((Job) job, delay, reschedule);
		}
		//schedule the job
		doSchedule(job, delay);
		return true;
	}

	/**
	 * Adds all family members in the list of jobs to the collection
	 */
	private void select(List<InternalJob> members, Object family, InternalJob firstJob, int stateMask) {
		if (firstJob == null)
			return;
		InternalJob job = firstJob;
		do {
			//note that job state cannot be NONE at this point
			if ((family == null || job.belongsTo(family)) && ((job.getState() & stateMask) != 0))
				members.add(job);
			job = job.previous();
		} while (job != null && job != firstJob);
	}

	/**
	 * Returns a list of all jobs known to the job manager that belong to the given family.
	 */
	private List<InternalJob> select(Object family) {
		return select(family, Job.WAITING | Job.SLEEPING | Job.RUNNING);
	}

	/**
	 * Returns a list of all jobs known to the job manager that belong to the given
	 * family and are in one of the provided states.
	 */
	private List<InternalJob> select(Object family, int stateMask) {
		List<InternalJob> members = new ArrayList<>();
		synchronized (lock) {
			if ((stateMask & Job.RUNNING) != 0) {
				for (InternalJob internalJob : running) {
					select(members, family, internalJob, stateMask);
				}
			}
			if ((stateMask & Job.WAITING) != 0) {
				select(members, family, waiting.peek(), stateMask);
				for (InternalJob internalJob : yielding) {
					select(members, family, internalJob, stateMask);
				}
			}
			if ((stateMask & Job.SLEEPING) != 0)
				select(members, family, sleeping.peek(), stateMask);
		}
		return members;
	}

	@Override
	public void setLockListener(LockListener listener) {
		lockManager.setLockListener(listener);
	}

	/**
	 * Changes a job priority.
	 */
	protected void setPriority(InternalJob job, int newPriority) {
		synchronized (lock) {
			int oldPriority = job.getPriority();
			if (oldPriority == newPriority)
				return;
			job.internalSetPriority(newPriority);
			//if the job is waiting to run, re-shuffle the queue
			if (job.getState() == Job.WAITING) {
				long oldStart = job.getStartTime();
				job.setStartTime(oldStart + (delayFor(newPriority) - delayFor(oldPriority)));
				waiting.resort(job);
			}
		}
	}

	@Override
	public void setProgressProvider(ProgressProvider provider) {
		progressProvider = provider;
	}

	public void setRule(InternalJob job, ISchedulingRule rule) {
		synchronized (lock) {
			//cannot change the rule of a job that is already running
			Assert.isLegal(job.getState() == Job.NONE);
			validateRule(rule);
			job.internalSetRule(rule);
		}
	}

	/**
	 * Puts a job to sleep. Returns true if the job was successfully put to sleep.
	 */
	protected boolean sleep(InternalJob job) {
		synchronized (lock) {
			switch (job.getState()) {
				case Job.RUNNING :
					//cannot be paused if it is already running (as opposed to ABOUT_TO_RUN)
					if (job.internalGetState() == Job.RUNNING)
						return false;
					//job hasn't started running yet (aboutToRun listener)
					break;
				case Job.SLEEPING :
					//update the job wake time
					job.setStartTime(InternalJob.T_INFINITE);
					//change state again to re-shuffle the sleep queue
					changeState(job, Job.SLEEPING);
					return true;
				case Job.NONE :
					return true;
				case Job.WAITING :
					//put the job to sleep
					break;
			}
			job.setStartTime(InternalJob.T_INFINITE);
			changeState(job, Job.SLEEPING);
			jobListeners.queueSleeping((Job) job);
		}
		jobListeners.sendEvents(job);
		return true;
	}

	@Override
	public void sleep(Object family) {
		//don't synchronize because sleep calls listeners
		for (InternalJob internalJob : select(family)) {
			sleep(internalJob);
		}
	}

	/**
	 * Returns the estimated time in milliseconds before the next job is scheduled
	 * to wake up. The result may be negative.  Returns InternalJob.T_INFINITE if
	 * there are no sleeping or waiting jobs.
	 */
	protected long sleepHint() {
		synchronized (lock) {
			//wait forever if job manager is suspended
			if (suspended)
				return InternalJob.T_INFINITE;
			if (!waiting.isEmpty())
				return 0L;
			//return the anticipated time that the next sleeping job will wake
			InternalJob next = sleeping.peek();
			if (next == null)
				return InternalJob.T_INFINITE;
			return next.getStartTime() - now();
		}
	}

	/**
	 * Implementation of {@link Job#yieldRule(IProgressMonitor)}
	 */
	protected Job yieldRule(InternalJob job, IProgressMonitor monitor) {
		Thread currentThread = Thread.currentThread();
		Assert.isLegal(job.getState() == Job.RUNNING, "Cannot yieldRule job that is " + printState(job.internalGetState())); //$NON-NLS-1$
		Assert.isLegal(currentThread == job.getThread(), "Cannot yieldRule from outside job's thread"); //$NON-NLS-1$

		InternalJob unblocked;
		// If job is not a ThreadJob, and it has implicitly started rules, likeThreadJob
		// is the corresponding ThreadJob. Similarly, if likeThreadJob is not null, then
		// job is not a ThreadJob
		ThreadJob likeThreadJob;
		synchronized (implicitJobs) {
			synchronized (lock) {
				// The nested implicit job, if any
				likeThreadJob = implicitJobs.getThreadJob(currentThread);

				unblocked = job.previous();

				// if unblocked is not null, it was a blocked job. It is guaranteed
				// that it will be the next job run by the worker threads once this
				// lock is released.
				if (unblocked == null) {

					if (likeThreadJob != null) {

						// look for any explicit jobs we may be blocking
						unblocked = ((InternalJob) likeThreadJob).previous();

						if (unblocked == null) {

							// look for any implicit (or yielding) jobs we may be blocking.
							unblocked = findBlockedJob(likeThreadJob);
						}

					} else {

						// look for any implicit (or yielding) jobs we may be blocking.
						unblocked = findBlockedJob(job);
					}
				}

				// optimization: do nothing if we don't unblock any job
				if (unblocked == null)
					return null;

				// "release" our rule by exiting RUNNING state
				changeState(job, InternalJob.YIELDING);
				if (DEBUG_YIELDING)
					JobManager.debug(job + " will yieldRule to " + unblocked); //$NON-NLS-1$

				if (likeThreadJob != null && likeThreadJob != job) {
					// if there is a corresponding thread job, it needs yield as well
					changeState(likeThreadJob, InternalJob.YIELDING);
					if (DEBUG_YIELDING)
						JobManager.debug(job + " will yieldRule to " + unblocked); //$NON-NLS-1$
				}

				if (likeThreadJob != null) {
					// only null-out threads out for non-ThreadJobs
					job.setThread(null);
					if (likeThreadJob.getRule() != null) {
						getLockManager().removeLockThread(currentThread, likeThreadJob.getRule());
					}
				}

				if ((job.getRule() != null) && !(job instanceof ThreadJob))
					getLockManager().removeLockThread(currentThread, job.getRule());
			}
		}
		// To prevent this job from immediately re-grabbing the scheduling rule wait until
		// the unblocked job changes state. This unblocked job is guaranteed to be the
		// next job of the set of similar conflicting rules to attempt to run.
		if (DEBUG_YIELDING_DETAILED)
			JobManager.debug(job + " is waiting for " + unblocked + " to transition from WAITING state"); //$NON-NLS-1$ //$NON-NLS-2$

		waitForUnblocked(unblocked);

		// restart this job, unless we've been restarted already
		// This is the same as ThreadJob begin, except that cancelation CAN NOT be supported
		// throwing the OperationCanceledException will return execution to the caller.
		IProgressMonitor mon = monitorFor(monitor);
		ProgressMonitorWrapper nonCanceling = new ProgressMonitorWrapper(mon) {
			@Override
			public boolean isCanceled() {
				// pass-through request
				getWrappedProgressMonitor().isCanceled();
				// ignore result
				return false;
			}
		};

		if (DEBUG_YIELDING)
			JobManager.debug(job + " waiting to resume"); //$NON-NLS-1$

		// this yielding job becomes an implicit job, unless it is one already
		if (likeThreadJob == null) {
			// Create a Threadjob proxy. This is strictly an internal job, but its not
			// preventing from "leaking" out to clients in the form of listener
			// notifications, and via IJobManager API usage like find().
			// Set a flag to differentiate it from regular ThreadJobs.
			ThreadJob threadJob = new ThreadJob(job.getRule()) {
				@Override
				boolean isResumingAfterYield() {
					return true;
				}
			};
			threadJob.setRealJob((Job) job);
			ThreadJob.joinRun(threadJob, nonCanceling);
			// the following state changes are atomic
			synchronized (lock) {
				// Must end the temporary threadJob to remove from running list
				changeState(threadJob, Job.NONE);
				changeState(job, Job.RUNNING);
				job.setThread(currentThread);
			}
		} else {
			ThreadJob.joinRun(likeThreadJob, nonCanceling);
			synchronized (lock) {
				changeState(job, Job.RUNNING);
				job.setThread(currentThread);
			}
		}
		if (DEBUG_YIELDING) {
			// extra assert: make sure no other conflicting jobs are running now
			synchronized (lock) {
				for (InternalJob other : running) {
					if (other == job)
						continue;
					Assert.isTrue(!other.isConflicting(job), other + " conflicts and ran simultaneously with " + job); //$NON-NLS-1$
				}
			}
			JobManager.debug(job + " resumed"); //$NON-NLS-1$
		}
		if (unblocked instanceof ThreadJob && ((ThreadJob) unblocked).isResumingAfterYield()) {
			// if the unblocked job is a proxy for a yielding job to start, return
			// the original job. No need to expose the proxy ThreadJob.
			return ((ThreadJob) unblocked).realJob;
		}
		return (Job) unblocked;
	}

	private void waitForUnblocked(InternalJob theJob) {
		// wait until theJob leaves WAITING state
		boolean interrupted = false;
		synchronized (theJob.jobStateLock) {
			if (theJob instanceof ThreadJob) {
				// We can't acquire the implicitJob lock while holding jobStateLock,
				// so use isWaiting instead.
				while (((ThreadJob) theJob).isWaiting) {
					try {
						theJob.jobStateLock.wait();
					} catch (InterruptedException e) {
						interrupted = true;
					}
				}
			} else {
				while (theJob.internalGetState() == Job.WAITING) {
					try {
						theJob.jobStateLock.wait();
					} catch (InterruptedException e) {
						interrupted = true;
					}
				}
			}
		}
		if (interrupted)
			Thread.currentThread().interrupt();
	}

	/**
	 * Invokes {@link Job#shouldRun()} while guarding against unexpected failures.
	 */
	private boolean shouldRun(Job job) {
		Throwable t;
		try {
			return job.shouldRun();
		} catch (Exception | LinkageError | AssertionError e) {
			t = e;
		}
		RuntimeLog.log(new Status(IStatus.ERROR, JobManager.PI_JOBS, JobManager.PLUGIN_ERROR, "Error invoking shouldRun() method on: " + job, t)); //$NON-NLS-1$
		//if the should is unexpectedly failing it is safer not to run it
		return false;
	}

	/**
	 * Returns the next job to be run, or null if no jobs are waiting to run.
	 * The worker must call endJob when the job is finished running.
	 */
	protected Job startJob(Worker worker) {
		Job job = null;
		while (true) {
			job = nextJob();
			if (job == null)
				return null;
			//must perform this outside sync block because it is third party code
			boolean shouldRun = shouldRun(job);
			//check for listener veto
			if (shouldRun) {
				jobListeners.queueAboutToRun(job);
				jobListeners.sendEvents(job);
			}
			//listeners may have canceled or put the job to sleep
			boolean endJob = false;
			synchronized (lock) {
				JobGroup jobGroup = job.getJobGroup();
				if (jobGroup != null && jobGroup.getState() == JobGroup.CANCELING)
					shouldRun = false;
				InternalJob internal = job;
				synchronized (internal.jobStateLock) {
					if (internal.internalGetState() == InternalJob.ABOUT_TO_RUN) {
						if (shouldRun && !internal.isAboutToRunCanceled()) {
							internal.setProgressMonitor(createMonitor(job));
							//change from ABOUT_TO_RUN to RUNNING
							internal.setThread(worker);
							internal.internalSetState(Job.RUNNING);
							internal.jobStateLock.notifyAll();
							jobListeners.queueRunning(job);
							break;
						}
						endJob = true;
						//fall through and end the job below
					}
				}
			}
			if (endJob) {
				//job has been vetoed or canceled, so mark it as done
				endJob(job, Status.CANCEL_STATUS, true);
				continue;
			}
		}
		jobListeners.sendEvents(job);
		return job;

	}

	@Override
	public final void suspend() {
		synchronized (lock) {
			suspended = true;
		}
	}

	@Deprecated
	@Override
	public final void suspend(ISchedulingRule rule, IProgressMonitor monitor) {
		Assert.isNotNull(rule);
		implicitJobs.suspend(rule, monitorFor(monitor));
	}

	@Override
	public void transferRule(ISchedulingRule rule, Thread destinationThread) {
		implicitJobs.transfer(rule, destinationThread);
	}

	/**
	 * Validates that the given scheduling rule obeys the constraints of
	 * scheduling rules as described in the <code>ISchedulingRule</code>
	 * javadoc specification.
	 */
	private void validateRule(ISchedulingRule rule) {
		//null rule always valid
		if (rule == null)
			return;
		if (rule instanceof MultiRule) {
			ISchedulingRule[] children = ((MultiRule) rule).getChildren();
			for (ISchedulingRule element : children) {
				Assert.isLegal(element != rule);
				validateRule(element);
			}
		}
		//contains method must be reflexive
		Assert.isLegal(rule.contains(rule));
		//contains method must return false when given an unknown rule
		Assert.isLegal(!rule.contains(nullRule));
		//isConflicting method must be reflexive
		Assert.isLegal(rule.isConflicting(rule));
		//isConflicting method must return false when given an unknown rule
		Assert.isLegal(!rule.isConflicting(nullRule));
	}

	protected void wakeUp(InternalJob job, long delay) {
		Assert.isLegal(delay >= 0, "Scheduling delay is negative"); //$NON-NLS-1$
		boolean scheduled;
		synchronized (lock) {
			//cannot wake up if it is not sleeping
			if (job.getState() != Job.SLEEPING)
				return;
			scheduled = doSchedule(job, delay);
			// only notify of wake up if immediate
			if (scheduled && delay == 0)
				jobListeners.queueAwake((Job) job);
		}
		//call the pool outside sync block to avoid deadlock
		pool.jobQueued();
		jobListeners.sendEvents(job);
	}

	@Override
	public void wakeUp(Object family) {
		//don't synchronize because wakeUp calls listeners
		for (InternalJob internalJob : select(family)) {
			wakeUp(internalJob, 0L);
		}
	}

	void endMonitoring(ThreadJob threadJob) {
		synchronized (monitorStack) {
			for (int i = monitorStack.size() - 1; i >= 0; i--) {
				if (monitorStack.get(i)[0] == threadJob) {
					monitorStack.remove(i);
					monitorStack.notifyAll();
					break;
				}
			}
		}
	}

	void beginMonitoring(ThreadJob threadJob, IProgressMonitor monitor) {
		synchronized (monitorStack) {
			monitorStack.add(new Object[] {threadJob, monitor});
			monitorStack.notifyAll();
		}
	}

	/**
	 * Listens for the job completion events and checks for the job group cancellation,
	 * computes and logs the group result.
	 */
	private class JobGroupUpdater extends JobChangeAdapter {
		Object jobManagerLock;

		public JobGroupUpdater(Object jobManagerLock) {
			this.jobManagerLock = jobManagerLock;
		}

		@Override
		public void done(IJobChangeEvent event) {
			InternalJob job = event.getJob();
			InternalJobGroup jobGroup = job.getJobGroup();
			if (jobGroup == null)
				return;
			IStatus jobResult = event.getResult();
			boolean reschedule = ((JobChangeEvent) event).reschedule;

			int jobGroupState;
			int activeJobsCount;
			int failedJobsCount;
			int canceledJobsCount;
			int seedJobsRemainingCount;
			List<IStatus> jobResults = Collections.emptyList();
			synchronized (jobManagerLock) {
				// Collect the required details to check for the group cancellation and completion
				// outside the synchronized block.
				jobGroupState = jobGroup.getState();
				activeJobsCount = jobGroup.getActiveJobsCount();
				failedJobsCount = jobGroup.getFailedJobsCount();
				canceledJobsCount = jobGroup.getCanceledJobsCount();
				seedJobsRemainingCount = jobGroup.getSeedJobsRemainingCount();
				if (activeJobsCount == 0)
					jobResults = jobGroup.getCompletedJobResults();
			}

			// Check for the group completion.
			if (!reschedule && jobGroupState != JobGroup.NONE && activeJobsCount == 0 && (seedJobsRemainingCount <= 0 || jobGroupState == JobGroup.CANCELING)) {
				// Must perform this outside the sync block to avoid a potential deadlock
				MultiStatus jobGroupResult = jobGroup.computeGroupResult(jobResults);
				Assert.isLegal(jobGroupResult != null, "The group result should not be null"); //$NON-NLS-1$
				boolean isJobGroupCompleted = false;
				synchronized (jobManagerLock) {
					// If more jobs were added to the group while were computing the result, the job group
					// remains in the ACTIVE state and the computed result is discarded to be recomputed later,
					// after the new jobs finish.
					if (jobGroup.getState() != JobGroup.NONE && jobGroup.getActiveJobsCount() == 0) {
						jobGroup.endJobGroup(jobGroupResult);
						isJobGroupCompleted = true;
					}
				}

				// If the job group is completing, add the job group's status to the event
				// and log errors and warnings.
				if (isJobGroupCompleted) {
					((JobChangeEvent) event).jobGroupResult = jobGroupResult;
					if (jobGroupResult.matches(IStatus.ERROR | IStatus.WARNING))
						RuntimeLog.log(jobGroupResult);
				}

				return;
			}

			if (jobGroupState != JobGroup.CANCELING && jobGroup.shouldCancel(jobResult, failedJobsCount, canceledJobsCount))
				cancel(jobGroup, true);
		}
	}
}
