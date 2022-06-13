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

package org.eclipse.core.runtime.jobs;

import java.util.List;
import org.eclipse.core.internal.jobs.InternalJobGroup;
import org.eclipse.core.runtime.*;

/**
 * JobGroups support throttling, join, cancel, combined progress and error reporting
 * on a group of jobs.
 * <ul>
 * <li>A JobGroup object represents a group of Jobs. Any number of jobs
 * can be added to a group, but a Job can be part of only one group at a time.
 * <li>A JobGroup can be configured with a throttling number, so that only that many
 * jobs from the group are allowed to run in parallel.
 * <li>One can join on all of the jobs in the group and observe the completion progress
 * of those jobs.
 * <li>One can cancel all the jobs in the group.
 * <li>A JobGroup consolidates the return statuses of all the jobs in the group
 * and a single MultiStatus message is available after all the jobs in the group
 * are completed.
 * </ul>
 * <p>
 * JobGroups maintain state for the collective status of the jobs belonging to the group.
 * When constructed, a job group starts with a state value of <code>NONE</code>.
 * When any job belonging to the group is scheduled to run, the job group moves into the
 * <code>ACTIVE</code> state. A job group enters the <code>CANCELING</code> state
 * when cancellation of the whole group is requested. The group will be in this state
 * until all the jobs in the group have finished either through cancellation or
 * normal completion. When a job group is in the <code>CANCELING</code> state,
 * newly scheduled jobs which are part of that group are immediately canceled.
 * When execution of all the jobs belonging to a job group finishes (either normally
 * or through cancellation), the job group state changes back to <code>NONE</code>.
 *
 * @see IJobManager
 * @since 3.7
 */
public class JobGroup extends InternalJobGroup {
	/**
	 * JobGroup state code (value 0) indicating that none of the jobs belonging to
	 * the group are running or scheduled to run.
	 *
	 * @see #getState()
	 */
	public static final int NONE = 0;
	/**
	 * JobGroup state code (value 1) indicating that at least one job belonging to
	 * the group is running or scheduled to run.
	 *
	 * @see #getState()
	 */
	public static final int ACTIVE = 0x01;
	/**
	 * JobGroup state code (value 2) indicating that cancellation of the whole group is requested.
	 * The group will be in this state until all the jobs in the group have finished either through
	 * cancellation or normal completion. When a job group is in this state, newly scheduled jobs
	 * which are part of that group are immediately canceled.
	 */
	public static final int CANCELING = 0x02;

	/**
	 * Creates a new job group with the specified <code>name</code> and <code>maxThreads</code>.
	 * The job group name is a human-readable value that is displayed to users.  The name does
	 * not need to be unique, but it must not be <code>null</code>. The <code>maxThreads</code>
	 * indicates the maximum number of threads allowed to be concurrently scheduled by the
	 * jobs belonging to the group at any given time, or <code>zero</code> to indicate that
	 * no throttling should be applied and all jobs should be allowed to run as soon as possible.
	 *
	 * @param name the name of the job group.
	 * @param maxThreads the maximum number of threads allowed to be concurrently scheduled,
	 * or <code>zero</code> to indicate that no throttling should be applied and all jobs
	 * should be allowed to run as soon as possible.
	 * @param seedJobsCount the initial number of jobs that will be added to the job group.
	 * This is the initial count of jobs with which the creator of the job group will "seed"
	 * the job group. Those initial jobs may discover more work and add yet more jobs, but
	 * those additional jobs should not be included in this initial "seed" count. 
	 * <ul>
	 * <li>
	 * If this value is set too high (higher than the number of actually scheduled jobs), 
	 * the job group will never transition to the done ({@link #NONE})
	 * state, {@link #join(long, IProgressMonitor)} calls will hang, and {@link #getResult()}
	 * calls will return invalid results. 
	 * </li>
	 * <li>
	 * If this value is set too low, the job group may
	 * transition to the ({@link #NONE}) state before all of the jobs have been scheduled,
	 * causing a {@link #join(long, IProgressMonitor)} call to return too early.
	 * </li>
	 * <li>Scheduling more jobs to the group after {@code seedJobsCount} previously 
	 * scheduled jobs were finished may cause a {@link #join(long, IProgressMonitor)} 
	 * call to return too early.
	 * </li>
	 * </ul>
	 */
	public JobGroup(String name, int maxThreads, int seedJobsCount) {
		super(name, maxThreads, seedJobsCount);
	}

	/**
	 * Returns the human readable name of this job group.  The name is never <code>null</code>.
	 *
	 * @return the name of this job group
	 */
	@Override
	public final String getName() {
		return super.getName();
	}

	/**
	 * Returns the maximum number of threads allowed to be scheduled by the jobs belonging
	 * to the group at any given time, or <code>zero</code> to indicate that no throttling
	 * should be applied and all jobs should be allowed to run as soon as possible.
	 *
	 * @return the maximum number of threads allowed to be used.
	 */
	@Override
	public final int getMaxThreads() {
		return super.getMaxThreads();
	}

	/**
	 * Returns the result of this job group's last run. If a job group completes and then
	 * its jobs are rescheduled, this method returns the results of the previous run.
	 *
	 * @return the result of this job group's last run, or <code>null</code> if this
	 * job group has never finished running.
	 */
	@Override
	public final MultiStatus getResult() {
		return super.getResult();
	}

	/**
	 * Returns the state of the job group. Result will be one of:
	 * <ul>
	 * <li><code>JobGroup.NONE</code> - when the jobs belonging to the group are not
	 * yet scheduled to run.</li>
	 * <li><code>JobGroup.ACTIVE</code> - when the jobs belonging to the group are
	 * running or scheduled to run.</li>
	 * <li><code>JobGroup.CANCELING</code> - when the jobs belonging to the group
	 * are being canceled.</li>
	 * </ul>
	 * <p>
	 * Note that job group state is inherently volatile, and in most cases clients
	 * cannot rely on the result of this method being valid by the time the result
	 * is obtained. For example, if <code>getState</code> returns
	 * <code>ACTIVE</code>, the job group may have actually completed by the time
	 * the <code>getState</code> method returns. All that clients can infer from
	 * invoking this method is that the job group was recently in the returned
	 * state.
	 *
	 * @return the job group state
	 */
	@Override
	public final int getState() {
		return super.getState();
	}

	/**
	 * Returns all waiting, executing and sleeping jobs belonging
	 * to this job group. If no jobs are found, an empty array is returned.
	 *
	 * @return the list of active jobs
	 * @see Job#setJobGroup(JobGroup)
	 */
	@Override
	public final List<Job> getActiveJobs() {
		return super.getActiveJobs();
	}

	/**
	 * Cancels all jobs belonging to this job group. Jobs belonging to this group
	 * that are currently in the <code>WAITING</code> state will be removed from the queue.
	 * Sleeping jobs will be discarded without having a chance to wake up.
	 * Currently executing jobs will be asked to cancel but there is no guarantee
	 * that they will do so.
	 * <p>
	 * The job group will be placed into <code>CANCELING</code> state and will be
	 * in that state until all the jobs in the group have finished either through
	 * cancellation or normal completion. When a job group is in the <code>CANCELING</code>
	 * state, newly scheduled jobs which are part of the group are immediately canceled.
	 *
	 * @see Job#setJobGroup(JobGroup)
	 * @see JobGroup#getState()
	 */
	@Override
	public final void cancel() {
		super.cancel();
	}

	/**
	 * Waits until either all jobs belonging to this job group have finished or
	 * the given timeout has expired. This method will block the calling thread
	 * until all such jobs have finished executing, or the given timeout is
	 * expired, or the given progress monitor is canceled by the user or the
	 * calling thread is interrupted. If there are no jobs belonging to the
	 * group that are currently waiting, running, or sleeping, this method
	 * returns immediately. Feedback on how the join is progressing is provided
	 * to the given progress monitor.
	 * <p>
	 * If this method is called while the job manager is suspended, only jobs
	 * that are currently running will be joined. Once there are no jobs belongs
	 * to the group in the {@link Job#RUNNING} state, the method returns.
	 * </p>
	 * <p>
	 * Jobs may be added to this job group after the initial set of jobs are
	 * scheduled, and this method will wait for all newly added jobs to complete
	 * (or the given timeout has expired), even if they are added to the group
	 * after this method is invoked.
	 * </p>
	 * <p>
	 * Throws an <code>OperationCanceledException</code> when the given progress
	 * monitor is canceled. Canceling the monitor does not cancel the jobs
	 * belonging to the group and, if required, the group may be canceled
	 * explicitly using the {@link #cancel()} method.
	 * </p>
	 * <p>
	 * <b>NOTE:</b> If the job manager is suspended, the result of the job group
	 * run may not be set when this method returns.
	 * </p>
	 *
	 * @param timeoutMillis
	 *            the maximum amount of time to wait for the join to complete,
	 *            or <code>zero</code> for no timeout.
	 * @param monitor
	 *            the progress monitor for reporting progress on how the wait is
	 *            progressing and to be able to cancel the join operation, or
	 *            <code>null</code> if no progress monitoring is required.
	 * @return <code>true</code> when all the jobs in the group complete, or
	 *         <code>false</code> when the operation is not completed within the
	 *         given time.
	 * @exception InterruptedException
	 *                if the calling thread is interrupted while waiting
	 * @exception OperationCanceledException
	 *                if the progress monitor is canceled while waiting
	 * @see Job#setJobGroup(JobGroup)
	 * @see #cancel()
	 */
	@Override
	public final boolean join(long timeoutMillis, IProgressMonitor monitor) throws InterruptedException, OperationCanceledException {
		return super.join(timeoutMillis, monitor);
	}

	/**
	 * This method is called by the JobManager after the completion of every job belonging
	 * to this group, and is used to control the job group's cancellation policy. Returning
	 * <code>true</code> from this function causes all remaining running and scheduled jobs
	 * to be canceled.
	 * <p>
	 * The default implementation returns <code>true</code> when <code>numberOfFailedJobs &gt; 0</code>.
	 * Subclasses may override this method to implement a different cancellation strategy.
	 *
	 * @param lastCompletedJobResult result of the last completed job belonging to this group.
	 * @param numberOfFailedJobs the total number of jobs belonging to this group that are
	 *     finished with status <code>IStatus.ERROR</code>.
	 * @param numberOfCanceledJobs the total number of jobs belonging to this group that are
	 *     finished with status <code>IStatus.CANCEL</code>.
	 * @return <code>true</code> when the remaining jobs belonging to this group should be canceled.
	 */
	@Override
	protected boolean shouldCancel(IStatus lastCompletedJobResult, int numberOfFailedJobs, int numberOfCanceledJobs) {
		return super.shouldCancel(lastCompletedJobResult, numberOfFailedJobs, numberOfCanceledJobs);
	}

	/**
	 * This method is called by the JobManager when all the jobs belonging to the group
	 * are completed. The combined status returned by this method is used as the result
	 * of the job group.
	 * <p>
	 * The default implementation will return a <code>MultiStatus</code> object containing
	 * the returned statuses of the completed jobs. The results with <code>IStatus.OK</code>
	 * are omitted from the result since those statuses usually do not contain valuable information.
	 * Subclasses may override this method to implement custom status reporting,
	 * but should never return <code>null</code>.
	 *
	 * @param jobResults results of all the completed jobs belonging to this group.
	 * @return the combined status of the group, not <code>null</code>.
	 * @see #getResult()
	 */
	@Override
	protected MultiStatus computeGroupResult(List<IStatus> jobResults) {
		return super.computeGroupResult(jobResults);
	}
}
