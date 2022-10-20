/*******************************************************************************
 * Copyright (c) 2014 Google Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Thirumala Reddy Mutchukota - initial API and implementation
 *******************************************************************************/

package org.eclipse.core.internal.jobs;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobGroup;

/**
 * Internal implementation class for job groups.
 *
 * @noextend This class is not intended to be extended by clients. All job groups
 * must be subclasses of the API <code>org.eclipse.core.runtime.jobs.JobGroup</code> class.
 */
public class InternalJobGroup {
	/**
	 * The maximum amount of time to wait on {@link #jobGroupStateLock}.
	 * Determines how often the progress monitor is checked for cancellation.
	 */
	private static final long MAX_WAIT_INTERVAL = 100;
	/**
	 * This lock will be held while performing state changes on this job group. It is
	 * also used as a notifier to wake up the threads waiting for this job group to complete.
	 *
	 * External code is never called while holding this lock, thus removing the hold and wait
	 * condition necessary for deadlock.
	 *
	 * @GuardedBy("itself")
	 */
	final Object jobGroupStateLock = new Object();

	private static final JobManager manager = JobManager.getInstance();

	private final String name;
	private final int maxThreads;

	/** write is synchronized by jobGroupStateLock, read is not synchronized **/
	private volatile int state = JobGroup.NONE;
	/** write is synchronized by jobGroupStateLock, read is not synchronized **/
	private volatile MultiStatus result;
	/** synchronized by JobManager.lock **/
	private final Set<InternalJob> runningJobs = new HashSet<>();
	/** synchronized by JobManager.lock **/
	private final Set<InternalJob> otherActiveJobs = new HashSet<>();
	/** synchronized by JobManager.lock **/
	private final List<IStatus> results = new ArrayList<>();
	/** synchronized by JobManager.lock **/
	private boolean cancelingDueToError;
	/** synchronized by JobManager.lock **/
	private int failedJobsCount;
	/** synchronized by JobManager.lock **/
	private int canceledJobsCount;
	/** synchronized by JobManager.lock **/
	private int seedJobsCount;
	/** synchronized by JobManager.lock **/
	private int seedJobsRemainingCount;

	protected InternalJobGroup(String name, int maxThreads, int seedJobsCount) {
		Assert.isNotNull(name);
		Assert.isLegal(maxThreads >= 0);
		Assert.isLegal(seedJobsCount >= 0);
		this.name = name;
		this.maxThreads = maxThreads;
		this.seedJobsCount = seedJobsCount;
		this.seedJobsRemainingCount = seedJobsCount;
	}

	protected String getName() {
		return name;
	}

	protected int getMaxThreads() {
		return maxThreads;
	}

	protected MultiStatus getResult() {
		return result;
	}

	protected int getState() {
		return state;
	}

	protected List<Job> getActiveJobs() {
		return manager.find(this);
	}

	protected void cancel() {
		manager.cancel(this);
	}

	protected boolean join(long timeout, IProgressMonitor monitor) throws InterruptedException, OperationCanceledException {
		return manager.join(this, timeout, monitor);
	}

	/**
	 * Called by the JobManager when the state of a job belonging to this group has changed.
	 * Must be called from JobManager#changeState
	 *
	 * @param job a job belonging to this group
	 * @param oldState the old state of the job
	 * @param newState the new state of the job
	 * @GuardedBy("JobManager.lock")
	 */
	final void jobStateChanged(InternalJob job, int oldState, int newState) {
		assert Thread.holdsLock(manager.lock);
		switch (oldState) {
			case Job.NONE :
				break;
			case Job.SLEEPING :
			case Job.WAITING :
				otherActiveJobs.remove(job);
				break;
			case Job.RUNNING :
				runningJobs.remove(job);
				break;
			default :
				Assert.isLegal(false, "Invalid job state: " + job + ", state: " + oldState); //$NON-NLS-1$//$NON-NLS-2$
				break;
		}

		switch (newState) {
			case Job.NONE :
				break;
			case Job.SLEEPING :
			case Job.WAITING :
				otherActiveJobs.add(job);
				break;
			case Job.RUNNING :
				runningJobs.add(job);
				break;
			default :
				Assert.isLegal(false, "Invalid job state: " + job + ", state: " + newState); //$NON-NLS-1$//$NON-NLS-2$
				break;
		}

		/*
		 * We can determine if the job that is being scheduled is one of its job group's "seed"
		 * jobs by retrieving the currently running job for this thread. If there is a running
		 * job on this thread or that job's job group is the same as this job's job group, then
		 * this job is being scheduled in response to discovering more work, so it is not one
		 * of its group's seed jobs, otherwise it is.
		 */
		if (job.internalGetState() == InternalJob.ABOUT_TO_SCHEDULE && getGroupOfCurrentlyRunningJob() != job.getJobGroup()) {
			seedJobsRemainingCount--;
		}

		if (oldState == Job.RUNNING && newState == Job.NONE) {
			IStatus jobResult = job.getResult();
			Assert.isLegal(jobResult != null);
			if (cancelingDueToError && jobResult.getSeverity() == IStatus.CANCEL)
				return;

			results.add(jobResult);
			int jobResultSeverity = jobResult.getSeverity();
			if (jobResultSeverity == IStatus.ERROR) {
				failedJobsCount++;
			} else if (jobResultSeverity == IStatus.CANCEL) {
				canceledJobsCount++;
			}
		}
		//make sure this job group is running
		if (getState() == JobGroup.NONE && getActiveJobsCount() > 0) {
			synchronized (jobGroupStateLock) {
				state = JobGroup.ACTIVE;
				jobGroupStateLock.notifyAll();
			}
		}

	}

	/**
	 * Returns the job group of the job currently running in this thread. Will return null if
	 * either this thread is not running a job or the job is not part of a job group.
	 */
	private JobGroup getGroupOfCurrentlyRunningJob() {
		Job job = manager.currentJob();
		return job == null ? null : job.getJobGroup();
	}

	/**
	 * Called by the JobManager to signify that the group canceling reason is changed.
	 * Must be called from JobManager#cancel(InternalJobGroup).
	 *
	 * @param cancelDueToError <code>true</code> if the group is getting canceled because
	 * the <code>shouldCancel(IStatus, int, int)</code> method returned <code>true</code>,
	 * <code>false</code> otherwise.
	 * @GuardedBy("JobManager.lock")
	 */
	final void updateCancelingReason(boolean cancelDueToError) {
		assert Thread.holdsLock(manager.lock);
		cancelingDueToError = cancelDueToError;
		if (!cancelDueToError) {
			// add a dummy cancel status to the results to make sure the combined status
			// will be of severity CANCEL.
			results.add(Status.CANCEL_STATUS);
		}
	}

	/**
	 * Called by the JobManager to signify that the group is getting canceled.
	 * Must be called from JobManager#cancel(InternalJobGroup).
	 *
	 * @param cancelDueToError <code>true</code> if the group is getting canceled because
	 * the <code>shouldCancel(IStatus, int, int)</code> method returned <code>true</code>,
	 * <code>false</code> otherwise.
	 * @GuardedBy("JobManager.lock")
	 * @GuardedBy("jobGroupStateLock")
	 */
	final List<Job> cancelAndNotify(boolean cancelDueToError) {
		synchronized (jobGroupStateLock) {
			switch (getState()) {
			case JobGroup.NONE:
				return Collections.emptyList();
			case JobGroup.CANCELING:
				if (!cancelDueToError) {
					// User cancellation takes precedence over the cancel due to error.
					updateCancelingReason(cancelDueToError);
				}
				return Collections.emptyList();
			}
			state = JobGroup.CANCELING;
			updateCancelingReason(cancelDueToError);
			jobGroupStateLock.notifyAll();
			return internalGetActiveJobs();
		}
	}

	/**
	 * Called by the JobManager to notify the group when the last job belonging
	 * to the group has finished execution. Must be called from JobManager#updateJobGroup.
	 *
	 * @param groupResult the combined status of the job group
	 * @GuardedBy("JobManager.lock")
	 * @GuardedBy("jobGroupStateLock")
	 */
	final void endJobGroup(MultiStatus groupResult) {
		assert Thread.holdsLock(manager.lock);
		synchronized (jobGroupStateLock) {
			if (seedJobsRemainingCount > 0 && !groupResult.matches(IStatus.CANCEL))
				throw new IllegalStateException("Invalid initial jobs remaining count"); //$NON-NLS-1$
			state = JobGroup.NONE;
			if (result != null) {
				IStatus[] children1 = result.getChildren();
				IStatus[] children2 = groupResult.getChildren();
				if (children1.length > 0 || children2.length > 0) {
					List<IStatus> combined = new ArrayList<>(children1.length + children2.length);
					for (IStatus s : children1) {
						combined.add(s);
					}
					for (IStatus s : children2) {
						combined.add(s);
					}
					groupResult = computeGroupResult(combined);
				}
			}
			result = groupResult;
			results.clear();
			cancelingDueToError = false;
			failedJobsCount = 0;
			canceledJobsCount = 0;

			// Set to 0 or 1 to allow additionally added jobs "end" this job group again
			// by entering JobManager.JobGroupUpdater.done(IJobChangeEvent).
			// Setting to value > 1 would cause an endless loop if the value is
			// bigger then the number of additionally scheduled jobs.
			// The drawback is that every extra scheduled job will trigger
			// endJobGroup(), but that's something we can't avoid
			seedJobsRemainingCount = seedJobsCount == 0 ? 0 : 1;
			jobGroupStateLock.notifyAll();
		}
	}

	final List<Job> internalGetActiveJobs() {
		assert Thread.holdsLock(manager.lock);
		List<Job> activeJobs = new ArrayList<>(runningJobs.size() + otherActiveJobs.size());
		for (InternalJob job : runningJobs)
			activeJobs.add((Job) job);
		for (InternalJob job : otherActiveJobs)
			activeJobs.add((Job) job);
		return activeJobs;
	}

	/**
	 * Called by the JobManager when updating the job group status. Prevents the job group
	 * from entering the NONE state prematurely, which can happen if all scheduled jobs
	 * run to completion while the master thread still has more "seed" jobs to schedule.
	 *
	 * @return the count of initial jobs remaining to be scheduled
	 */
	final int getSeedJobsRemainingCount() {
		assert Thread.holdsLock(manager.lock);
		return seedJobsRemainingCount;
	}

	final int getActiveJobsCount() {
		assert Thread.holdsLock(manager.lock);
		return runningJobs.size() + otherActiveJobs.size();
	}

	final int getRunningJobsCount() {
		assert Thread.holdsLock(manager.lock);
		return runningJobs.size();
	}

	final int getFailedJobsCount() {
		assert Thread.holdsLock(manager.lock);
		return failedJobsCount;
	}

	final int getCanceledJobsCount() {
		assert Thread.holdsLock(manager.lock);
		return canceledJobsCount;
	}

	final List<IStatus> getCompletedJobResults() {
		assert Thread.holdsLock(manager.lock);
		return new ArrayList<>(results);
	}

	protected boolean shouldCancel(IStatus lastCompletedJobResult, int numberOfFailedJobs, int numberOfCanceledJobs) {
		return numberOfFailedJobs > 0;
	}

	protected MultiStatus computeGroupResult(List<IStatus> jobResults) {
		List<IStatus> importantResults = new ArrayList<>();
		for (IStatus jobResult : jobResults) {
			if (jobResult.getSeverity() != IStatus.OK)
				importantResults.add(jobResult);
		}
		if (importantResults.isEmpty())
			return new MultiStatus("org.eclipse.core.jobs", 0, name, null); //$NON-NLS-1$

		String pluginId = importantResults.get(0).getPlugin();
		IStatus[] groupResults = importantResults.toArray(new IStatus[importantResults.size()]);
		return new MultiStatus(pluginId, 0, groupResults, name, null);
	}

	/**
	 * Implementation of joining a job group.
	 * @param remainingTime
	 * @return <code>true</code> if the join completed, and false otherwise (still waiting).
	 */
	boolean doJoin(long remainingTime) throws InterruptedException {
		synchronized (jobGroupStateLock) {
			if (getState() == JobGroup.NONE)
				return true;
			// If remaining time is greater than MAX_WAIT_INTERVAL, sleep only for
			// MAX_WAIT_INTERVAL instead to be more responsive to monitor cancellation.
			long sleepTime = remainingTime != 0 && remainingTime <= MAX_WAIT_INTERVAL ? remainingTime : MAX_WAIT_INTERVAL;
			jobGroupStateLock.wait(sleepTime);
			return getState() == JobGroup.NONE;
		}
	}
}
