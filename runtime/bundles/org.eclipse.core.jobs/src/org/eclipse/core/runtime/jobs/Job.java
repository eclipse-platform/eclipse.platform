/*******************************************************************************
 * Copyright (c) 2003, 2018 IBM Corporation and others.
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
 *     Thirumala Reddy Mutchukota (thirumala@google.com) -
 *     		Bug 432049, JobGroup API and implementation
 *     		Bug 105821, Support for Job#join with timeout and progress monitor
 *******************************************************************************/
package org.eclipse.core.runtime.jobs;

import org.eclipse.core.internal.jobs.InternalJob;
import org.eclipse.core.internal.jobs.JobManager;
import org.eclipse.core.runtime.*;

/**
 * Jobs are units of runnable work that can be scheduled to be run with the job
 * manager.  Once a job has completed, it can be scheduled to run again (jobs are
 * reusable).
 * <p>
 * Jobs have a state that indicates what they are currently doing.  When constructed,
 * jobs start with a state value of <code>NONE</code>.  When a job is scheduled
 * to be run, it moves into the <code>WAITING</code> state.  When a job starts
 * running, it moves into the <code>RUNNING</code> state.  When execution finishes
 * (either normally or through cancelation), the state changes back to
 * <code>NONE</code>.
 * </p><p>
 * A job can also be in the <code>SLEEPING</code> state.  This happens if a user
 * calls Job.sleep() on a waiting job, or if a job is scheduled to run after a specified
 * delay.  Only jobs in the <code>WAITING</code> state can be put to sleep.
 * Sleeping jobs can be woken at any time using Job.wakeUp(), which will put the
 * job back into the <code>WAITING</code> state.
 * </p><p>
 * Jobs can be assigned a priority that is used as a hint about how the job should
 * be scheduled.  There is no guarantee that jobs of one priority will be run before
 * all jobs of lower priority.  The javadoc for the various priority constants provide
 * more detail about what each priority means.  By default, jobs start in the
 * <code>LONG</code> priority class.
 *
 * @see IJobManager
 * @since 3.0
 */
public abstract class Job extends InternalJob {

	/**
	 * Job status return value that is used to indicate asynchronous job completion.
	 * @see Job#run(IProgressMonitor)
	 * @see Job#done(IStatus)
	 */
	public static final IStatus ASYNC_FINISH = new Status(IStatus.OK, JobManager.PI_JOBS, 1, "", null);//$NON-NLS-1$

	/* Job priorities */
	/**
	 * Job priority constant (value 10) for interactive jobs.
	 * Interactive jobs generally have priority over all other jobs.
	 * Interactive jobs should be either fast running or very low on CPU
	 * usage to avoid blocking other interactive jobs from running.
	 *
	 * @see #getPriority()
	 * @see #setPriority(int)
	 * @see #run(IProgressMonitor)
	 */
	public static final int INTERACTIVE = 10;
	/**
	 * Job priority constant (value 20) for short background jobs.
	 * Short background jobs are jobs that typically complete within a second,
	 * but may take longer in some cases.  Short jobs are given priority
	 * over all other jobs except interactive jobs.
	 *
	 * @see #getPriority()
	 * @see #setPriority(int)
	 * @see #run(IProgressMonitor)
	 */
	public static final int SHORT = 20;
	/**
	 * Job priority constant (value 30) for long-running background jobs.
	 *
	 * @see #getPriority()
	 * @see #setPriority(int)
	 * @see #run(IProgressMonitor)
	 */
	public static final int LONG = 30;
	/**
	 * Job priority constant (value 40) for build jobs.  Build jobs are
	 * generally run after all other background jobs complete.
	 *
	 * @see #getPriority()
	 * @see #setPriority(int)
	 * @see #run(IProgressMonitor)
	 */
	public static final int BUILD = 40;

	/**
	 * Job priority constant (value 50) for decoration jobs.
	 * Decoration jobs have lowest priority.  Decoration jobs generally
	 * compute extra information that the user may be interested in seeing
	 * but is generally not waiting for.
	 *
	 * @see #getPriority()
	 * @see #setPriority(int)
	 * @see #run(IProgressMonitor)
	 */
	public static final int DECORATE = 50;
	/**
	 * Job state code (value 0) indicating that a job is not
	 * currently sleeping, waiting, or running (i.e., the job manager doesn't know
	 * anything about the job).
	 *
	 * @see #getState()
	 */
	public static final int NONE = 0;
	/**
	 * Job state code (value 1) indicating that a job is sleeping.
	 *
	 * @see #run(IProgressMonitor)
	 * @see #getState()
	 */
	public static final int SLEEPING = 0x01;
	/**
	 * Job state code (value 2) indicating that a job is waiting to run.
	 *
	 * @see #getState()
	 * @see #yieldRule(IProgressMonitor)
	 */
	public static final int WAITING = 0x02;
	/**
	 * Job state code (value 4) indicating that a job is currently running
	 *
	 * @see #getState()
	 */
	public static final int RUNNING = 0x04;

	/**
	 * Returns the job manager.
	 *
	 * @return the job manager
	 * @since org.eclipse.core.jobs 3.2
	 */
	public static final IJobManager getJobManager() {
		return manager;
	}

	/**
	 * Creates a new Job that will execute the provided function when it runs.
	 *
	 * Prefer using {@link Job#create(String, ICoreRunnable)} as this does not
	 * require to call done on the monitor and relies on OperationCanceledException
	 *
	 * @param name     The name of the job
	 * @param function The function to execute
	 * @return A job that encapsulates the provided function
	 * @see IJobFunction
	 * @since 3.6
	 */
	public static Job create(String name, final IJobFunction function) {
		return new Job(name) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				return function.run(monitor);
			}
		};
	}

	/**
	 * Creates a new Job that will execute the provided runnable when it runs.
	 *
	 * @param name
	 *            the name of the job
	 * @param runnable
	 *            the runnable to execute
	 * @return a job that encapsulates the provided runnable
	 * @see ICoreRunnable
	 * @since 3.8
	 */
	public static Job create(String name, final ICoreRunnable runnable) {
		return new Job(name) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					runnable.run(monitor);
				} catch (CoreException e) {
					IStatus st = e.getStatus();
					return new Status(st.getSeverity(), st.getPlugin(), st.getCode(), st.getMessage(), e);
				}
				return Status.OK_STATUS;
			}
		};
	}

	/**
	 * Creates a new system {@link Job} with the given name that will execute the
	 * provided function when it runs.
	 *
	 * Prefer using {@link Job#createSystem(String, ICoreRunnable)} as this does not
	 * require to call done on the monitor and relies on OperationCanceledException
	 *
	 * @param name     the name of the job
	 * @param function The function to execute
	 * @return a job that encapsulates the provided function
	 * @see IJobFunction
	 * @see Job#setSystem(boolean)
	 * @since 3.10
	 */
	public static Job createSystem(String name, final IJobFunction function) {
		Job job = create(name, function);
		job.setSystem(true);
		return job;
	}

	/**
	 * Creates a new system {@link Job} with the given name that will execute
	 * the provided runnable when it runs.
	 *
	 * @param name
	 *            the name of the job
	 * @param runnable
	 *            the runnable to execute
	 * @return a job that encapsulates the provided runnable
	 * @see ICoreRunnable
	 * @see Job#setSystem(boolean)
	 * @since 3.8
	 */
	public static Job createSystem(String name, final ICoreRunnable runnable) {
		Job job = create(name, runnable);
		job.setSystem(true);
		return job;
	}

	/**
	 * Creates a new job with the specified name. The job name is a
	 * human-readable value that is displayed to users. The name does not need
	 * to be unique, but it must not be <code>null</code>.
	 *
	 * @param name the name of the job.
	 */
	public Job(String name) {
		super(name);
	}

	/**
	 * Registers a job listener with this job
	 * Has no effect if an identical listener is already registered.
	 *
	 * @param listener the listener to be added.
	 */
	@Override
	public final void addJobChangeListener(IJobChangeListener listener) {
		super.addJobChangeListener(listener);
	}

	/**
	 * Returns whether this job belongs to the given family.  Job families are
	 * represented as objects that are not interpreted or specified in any way
	 * by the job manager.  Thus, a job can choose to belong to any number of
	 * families.
	 * <p>
	 * Clients may override this method.  This default implementation always returns
	 * <code>false</code>.  Overriding implementations must return <code>false</code>
	 * for families they do not recognize.
	 * </p>
	 *
	 * @param family the job family identifier
	 * @return <code>true</code> if this job belongs to the given family, and
	 * <code>false</code> otherwise.
	 */
	@Override
	public boolean belongsTo(Object family) {
		return false;
	}

	/**
	 * Stops the job.  If the job is currently waiting,
	 * it will be removed from the queue.  If the job is sleeping,
	 * it will be discarded without having a chance to resume and its sleeping state
	 * will be cleared.  If the job is currently executing, it will be asked to
	 * stop but there is no guarantee that it will do so.
	 *
	 * @return <code>false</code> if the job is currently running (and thus may not
	 * respond to cancelation), and <code>true</code> in all other cases.
	 */
	@Override
	public final boolean cancel() {
		return super.cancel();
	}

	/**
	 * A hook method indicating that this job is running and {@link #cancel()}
	 * is being called for the first time.
	 * <p>
	 * Subclasses may override this method to perform additional work when
	 * a cancelation request is made.  This default implementation does nothing.
	 * @since 3.3
	 */
	@Override
	protected void canceling() {
		//default implementation does nothing
	}

	/**
	 * Jobs that complete their execution asynchronously must indicate when they
	 * are finished by calling this method.  This method must not be called by
	 * a job that has not indicated that it is executing asynchronously.
	 * <p>
	 * This method must not be called from within the scope of a job's <code>run</code>
	 * method.  Jobs should normally indicate completion by returning an appropriate
	 * status from the <code>run</code> method.  Jobs that return a status of
	 * <code>ASYNC_FINISH</code> from their run method must later call
	 * <code>done</code> to indicate completion.
	 *
	 * @param result a status object indicating the result of the job's execution.
	 * @see #ASYNC_FINISH
	 * @see #run(IProgressMonitor)
	 */
	@Override
	public final void done(IStatus result) {
		super.done(result);
	}

	/**
	 * Returns the human readable name of this job.  The name is never
	 * <code>null</code>.
	 *
	 * @return the name of this job
	 */
	@Override
	public final String getName() {
		return super.getName();
	}

	/**
	 * Returns the priority of this job.  The priority is used as a hint when the job
	 * is scheduled to be run.
	 *
	 * @return the priority of the job.  One of INTERACTIVE, SHORT, LONG, BUILD,
	 * 	or DECORATE.
	 */
	@Override
	public final int getPriority() {
		return super.getPriority();
	}

	/**
	 * Returns the value of the property of this job identified by the given key,
	 * or <code>null</code> if this job has no such property.
	 *
	 * @param key the name of the property
	 * @return the value of the property,
	 *     or <code>null</code> if this job has no such property
	 * @see #setProperty(QualifiedName, Object)
	 */
	@Override
	public final Object getProperty(QualifiedName key) {
		return super.getProperty(key);
	}

	/**
	 * Returns the result of this job's last run.
	 *
	 * @return the result of this job's last run, or <code>null</code> if this
	 * job has never finished running.
	 */
	@Override
	public final IStatus getResult() {
		return super.getResult();
	}

	/**
	 * Returns the scheduling rule for this job.  Returns <code>null</code> if this job has no
	 * scheduling rule.
	 *
	 * @return the scheduling rule for this job, or <code>null</code>.
	 * @see ISchedulingRule
	 * @see #setRule(ISchedulingRule)
	 */
	@Override
	public final ISchedulingRule getRule() {
		return super.getRule();
	}

	/**
	 * Returns the state of the job. Result will be one of:
	 * <ul>
	 * <li><code>Job.RUNNING</code> - if the job is currently running.</li>
	 * <li><code>Job.WAITING</code> - if the job is waiting to be run.</li>
	 * <li><code>Job.SLEEPING</code> - if the job is sleeping.</li>
	 * <li><code>Job.NONE</code> - in all other cases.</li>
	 * </ul>
	 * <p>
	 * Note that job state is inherently volatile, and in most cases clients cannot
	 * rely on the result of this method being valid by the time the result is
	 * obtained. For example, if <code>getState</code> returns <code>RUNNING</code>,
	 * the job may have actually completed by the time the <code>getState</code>
	 * method returns. All clients can infer from invoking this method is that the
	 * job was recently in the returned state.
	 *
	 * @return the job state
	 */
	@Override
	public final int getState() {
		return super.getState();
	}

	/**
	 * Returns the thread that this job is currently running in.
	 *
	 * @return the thread this job is running in, or <code>null</code>
	 * if this job is not running or the thread is unknown.
	 */
	@Override
	public final Thread getThread() {
		return super.getThread();
	}

	/**
	 * Returns the job group this job belongs to, or <code>null</code> if this job
	 * does not belongs to any group.
	 *
	 * @return the job group this job belongs to, or <code>null</code>.
	 * @see JobGroup
	 * @see #setJobGroup(JobGroup)
	 * @since 3.7
	 */
	@Override
	public final JobGroup getJobGroup() {
		return super.getJobGroup();
	}

	/**
	 * Returns whether this job is blocking a higher priority non-system job from
	 * starting due to a conflicting scheduling rule.  Returns <code>false</code>
	 * if this job is not running, or is not blocking a higher priority non-system job.
	 *
	 * @return <code>true</code> if this job is blocking a higher priority non-system
	 * job, and <code>false</code> otherwise.
	 * @see #getRule()
	 * @see #isSystem()
	 */
	@Override
	public final boolean isBlocking() {
		return super.isBlocking();
	}

	/**
	 * Returns whether this job is a system job.  System jobs are typically not
	 * revealed to users in any UI presentation of jobs.  Other than their UI presentation,
	 * system jobs act exactly like other jobs.  If this value is not explicitly set, jobs
	 * are treated as non-system jobs.  The default value is <code>false</code>.
	 *
	 * @return <code>true</code> if this job is a system job, and
	 * <code>false</code> otherwise.
	 * @see #setSystem(boolean)
	 */
	@Override
	public final boolean isSystem() {
		return super.isSystem();
	}

	/**
	 * Returns whether this job has been directly initiated by a UI end user.
	 * These jobs may be presented differently in the UI.  The default value
	 * is <code>false</code>.
	 *
	 * @return <code>true</code> if this job is a user-initiated job, and
	 * <code>false</code> otherwise.
	 * @see #setUser(boolean)
	 */
	@Override
	public final boolean isUser() {
		return super.isUser();
	}

	/**
	 * Waits until this job is finished. This method will block the calling thread
	 * until the job has finished executing, or until this thread has been
	 * interrupted. If the job has not been scheduled, this method returns
	 * immediately. A job must not be joined from within the scope of its run
	 * method.
	 * <p>
	 * If this method is called on a job that reschedules itself from within the
	 * <code>run</code> method, the join will return at the end of the first
	 * execution. In other words, join will return the first time this job exits the
	 * {@link #RUNNING} state, or as soon as this job enters the {@link #NONE}
	 * state.
	 * </p>
	 * <p>
	 * If this method is called while the job manager is suspended, this job will
	 * only be joined if it is already running; if this job is waiting or sleeping,
	 * this method returns immediately.
	 * </p>
	 * <p>
	 * Note that there is a deadlock risk when using join. If the calling thread
	 * owns a lock or object monitor that the joined thread is waiting for, deadlock
	 * will occur.
	 * </p>
	 * <p>
	 * Joining on another job belonging to the same group is not allowed if the
	 * group enforces throttling due to the potential for deadlock. For example,
	 * when the maximum threads allowed is set to 1 and a currently running Job A
	 * issues a join on another Job B belonging to its own job group, A waits
	 * indefinitely for its join to finish, but B never gets to run. To avoid that
	 * an IllegalStateException is thrown when a job tries to join another job
	 * belonging to the same job group. Joining another job belonging to the same
	 * group is allowed when the job group does not enforce throttling
	 * (JobGroup#getMaxThreads is zero).
	 * </p>
	 * <p>
	 * Calling this method is equivalent to calling <code>join(0, null)</code> and
	 * it is recommended to use the other join method with timeout and progress
	 * monitor as that will provide more control over the join operation.
	 * </p>
	 *
	 * @exception InterruptedException  if this thread is interrupted while waiting
	 * @exception IllegalStateException when a job tries to join on itself or join
	 *                                  on another job belonging to the same job
	 *                                  group and the group is configured with non
	 *                                  zero maximum threads allowed.
	 * @see #setJobGroup(JobGroup)
	 * @see #join(long, IProgressMonitor)
	 * @see ILock
	 * @see IJobManager#suspend()
	 */
	@Override
	public final void join() throws InterruptedException {
		super.join();
	}

	/**
	 * Waits until either the job is finished or the given timeout has expired. This
	 * method will block the calling thread until the job has finished executing, or
	 * the given timeout is expired, or the given progress monitor is canceled by
	 * the user or the calling thread is interrupted. If the job has not been
	 * scheduled, this method returns immediately. A job must not be joined from
	 * within the scope of its run method.
	 * <p>
	 * If this method is called on a job that reschedules itself from within the
	 * <code>run</code> method, the join will return at the end of the first
	 * execution. In other words, join will return the first time this job exits the
	 * {@link #RUNNING} state, or as soon as this job enters the {@link #NONE}
	 * state.
	 * </p>
	 * <p>
	 * If this method is called while the job manager is suspended, this job will
	 * only be joined if it is already running; if this job is waiting or sleeping,
	 * this method returns immediately.
	 * </p>
	 * <p>
	 * Note that there is a deadlock risk when using join. If the calling thread
	 * owns a lock or object monitor that the joined thread is waiting for and the
	 * timeout is set zero (i.e no timeout), deadlock will occur.
	 * </p>
	 * <p>
	 * Joining on another job belonging to the same group is not allowed if the
	 * timeout is set to zero and the group enforces throttling due to the potential
	 * for deadlock. For example, when the maximum threads allowed is set to 1 and a
	 * currently running Job A issues a join with no timeout on another Job B
	 * belonging to its own job group, A waits indefinitely for its join to finish,
	 * but B never gets to run. To avoid that an IllegalStateException is thrown
	 * when a job tries to join (with no timeout) another job belonging to the same
	 * job group. Joining another job belonging to the same group is allowed when
	 * either the job group does not enforce throttling (JobGroup#getMaxThreads is
	 * zero) or a non zero timeout value is provided.
	 * </p>
	 * <p>
	 * Throws an <code>OperationCanceledException</code> when the given progress
	 * monitor is canceled. Canceling the monitor does not cancel the job and, if
	 * required, the job may be canceled explicitly using the {@link #cancel()}
	 * method.
	 * </p>
	 *
	 * @param timeoutMillis the maximum amount of time to wait for the join to
	 *                      complete, or <code>zero</code> for no timeout.
	 * @param monitor       the progress monitor that can be used to cancel the join
	 *                      operation, or <code>null</code> if cancellation is not
	 *                      required. No progress is reported on this monitor.
	 * @return <code>true</code> when the job completes, or <code>false</code> when
	 *         the operation is not completed within the given time.
	 * @exception InterruptedException       if this thread is interrupted while
	 *                                       waiting
	 * @exception IllegalStateException      when a job tries to join on itself or
	 *                                       join with no timeout on another job
	 *                                       belonging to the same job group and the
	 *                                       group is configured with non-zero
	 *                                       maximum threads allowed.
	 * @exception OperationCanceledException if the progress monitor is canceled
	 *                                       while waiting
	 * @see #setJobGroup(JobGroup)
	 * @see #cancel()
	 * @see ILock
	 * @see IJobManager#suspend()
	 * @since 3.7
	 */
	@Override
	public final boolean join(long timeoutMillis, IProgressMonitor monitor) throws InterruptedException, OperationCanceledException {
		return super.join(timeoutMillis, monitor);
	}

	/**
	 * Removes a job listener from this job.
	 * Has no effect if an identical listener is not already registered.
	 *
	 * @param listener the listener to be removed
	 */
	@Override
	public final void removeJobChangeListener(IJobChangeListener listener) {
		super.removeJobChangeListener(listener);
	}

	/**
	 * Executes this job.  Returns the result of the execution.
	 * <p>
	 * The provided monitor can be used to report progress and respond to
	 * cancellation.  If the progress monitor has been canceled, the job
	 * should finish its execution at the earliest convenience and return a result
	 * status of severity {@link IStatus#CANCEL}.  The singleton
	 * cancel status {@link Status#CANCEL_STATUS} can be used for
	 * this purpose.  The monitor is only valid for the duration of the invocation
	 * of this method.
	 * <p>
	 * This method must not be called directly by clients.  Clients should call
	 * <code>schedule</code>, which will in turn cause this method to be called.
	 * <p>
	 * Jobs can optionally finish their execution asynchronously (in another thread) by
	 * returning a result status of {@link #ASYNC_FINISH}.  Jobs that finish
	 * asynchronously <b>must</b> specify the execution thread by calling
	 * <code>setThread</code>, and must indicate when they are finished by calling
	 * the method <code>done</code>.
	 *
	 * @param monitor the monitor to be used for reporting progress and
	 * responding to cancelation. The monitor is never <code>null</code>
	 * @return resulting status of the run. The result must not be <code>null</code>
	 * @see #ASYNC_FINISH
	 * @see #done(IStatus)
	 */
	@Override
	protected abstract IStatus run(IProgressMonitor monitor);

	/**
	 * Schedules this job to be run.  The job is added to a queue of waiting
	 * jobs, and will be run when it arrives at the beginning of the queue.
	 * <p>
	 * This is a convenience method, fully equivalent to
	 * <code>schedule(0L)</code>.
	 * </p>
	 * @see #schedule(long)
	 */
	public final void schedule() {
		super.schedule(0L);
	}

	/**
	 * Schedules this job to be run after a specified delay. The job is put in the
	 * {@link #SLEEPING} state until the specified delay has elapsed, after which
	 * the job is added to a queue of {@link #WAITING} jobs. Once the job arrives at
	 * the beginning of the queue, it will be run at the first available
	 * opportunity.
	 * <p>
	 * Jobs of equal priority and <code>delay</code> with conflicting scheduling
	 * rules are guaranteed to run in the order they are scheduled. No guarantees
	 * are made about the relative execution order of jobs with unrelated or
	 * <code>null</code> scheduling rules, or different priorities.
	 * </p>
	 * <p>
	 * If this job is currently running, it will be rescheduled with the specified
	 * delay as soon as it finishes. If this method is called multiple times while
	 * the job is running, the job will still only be rescheduled once, with the
	 * most recent delay value that was provided.
	 * </p>
	 * <p>
	 * Scheduling a job that is waiting or sleeping has no effect.
	 * </p>
	 *
	 * @param delay a time delay in milliseconds before the job should run
	 * @see ISchedulingRule
	 */
	@Override
	public final void schedule(long delay) {
		super.schedule(delay);
	}

	/**
	 * Changes the name of this job.  If the job is currently running, waiting,
	 * or sleeping, the new job name may not take effect until the next time the
	 * job is scheduled.
	 * <p>
	 * The job name is a human-readable value that is displayed to users.  The name
	 * does not need to be unique, but it must not be <code>null</code>.
	 *
	 * @param name the name of the job.
	 */
	@Override
	public final void setName(String name) {
		super.setName(name);
	}

	/**
	 * Sets the priority of the job.  This will not affect the execution of
	 * a running job, but it will affect how the job is scheduled while
	 * it is waiting to be run.
	 *
	 * @param priority the new job priority.  One of
	 * INTERACTIVE, SHORT, LONG, BUILD, or DECORATE.
	 */
	@Override
	public final void setPriority(int priority) {
		super.setPriority(priority);
	}

	/**
	 * Associates this job with a progress group. Progress feedback on this job's
	 * next execution will be displayed together with other jobs in that group. The
	 * provided monitor must be a monitor created by the method
	 * <code>IJobManager.createProgressGroup</code> and must have at least
	 * <code>ticks</code> units of available work.
	 * <p>
	 * The progress group must be set before the job is scheduled. The group will be
	 * used only for a single invocation of the job's <code>run</code> method, after
	 * which any association of this job to the group will be lost.
	 * </p>
	 *
	 * @see IJobManager#createProgressGroup()
	 * @param group The progress group to use for this job
	 * @param ticks the number of work ticks allocated from the parent monitor, or
	 *              {@link IProgressMonitor#UNKNOWN}
	 */
	@Override
	public final void setProgressGroup(IProgressMonitor group, int ticks) {
		super.setProgressGroup(group, ticks);
	}

	/**
	 * Sets the value of the property of this job identified
	 * by the given key. If the supplied value is <code>null</code>,
	 * the property is removed from this resource.
	 * <p>
	 * Properties are intended to be used as a caching mechanism
	 * by ISV plug-ins. They allow key-object associations to be stored with
	 * a job instance.  These key-value associations are maintained in
	 * memory (at all times), and the information is never discarded automatically.
	 * </p><p>
	 * The qualifier part of the property name must be the unique identifier
	 * of the declaring plug-in (e.g. <code>"com.example.plugin"</code>).
	 * </p>
	 *
	 * @param key the qualified name of the property
	 * @param value the value of the property,
	 *     or <code>null</code> if the property is to be removed
	 * @see #getProperty(QualifiedName)
	 */
	@Override
	public void setProperty(QualifiedName key, Object value) {
		super.setProperty(key, value);
	}

	/**
	 * Sets the scheduling rule to be used when scheduling this job.  This method
	 * must be called before the job is scheduled.
	 *
	 * @param rule the new scheduling rule, or <code>null</code> if the job
	 * should have no scheduling rule
	 * @see #getRule()
	 */
	@Override
	public final void setRule(ISchedulingRule rule) {
		super.setRule(rule);
	}

	/**
	 * Sets whether or not this job is a system job.  System jobs are typically not
	 * revealed to users in any UI presentation of jobs.  Other than their UI presentation,
	 * system jobs act exactly like other jobs.  If this value is not explicitly set, jobs
	 * are treated as non-system jobs. This method must be called before the job
	 * is scheduled.
	 *
	 * @param value <code>true</code> if this job should be a system job, and
	 * <code>false</code> otherwise.
	 * @see #isSystem()
	 */
	@Override
	public final void setSystem(boolean value) {
		super.setSystem(value);
	}

	/**
	 * Sets whether or not this job has been directly initiated by a UI end user.
	 * These jobs may be presented differently in the UI. This method must be
	 * called before the job is scheduled.
	 *
	 * @param value <code>true</code> if this job is a user-initiated job, and
	 * <code>false</code> otherwise.
	 * @see #isUser()
	 */
	@Override
	public final void setUser(boolean value) {
		super.setUser(value);
	}

	/**
	 * Sets the thread that this job is currently running in, or <code>null</code>
	 * if this job is not running or the thread is unknown.
	 * <p>
	 * Jobs that use the {@link #ASYNC_FINISH} return code should tell
	 * the job what thread it is running in.  This is used to prevent deadlocks.
	 *
	 * @param thread the thread that this job is running in.
	 *
	 * @see #ASYNC_FINISH
	 * @see #run(IProgressMonitor)
	 */
	@Override
	public final void setThread(Thread thread) {
		super.setThread(thread);
	}

	/**
	 * Sets the job group to which this job belongs. This method must be called before
	 * the job is scheduled, otherwise an <code>IllegalStateException</code> is thrown.
	 *
	 * @param jobGroup the group to which this job belongs to, or <code>null</code> if
	 * this job does not belongs to any group.
	 * @see JobGroup
	 * @since 3.7
	 */
	@Override
	public final void setJobGroup(JobGroup jobGroup) {
		super.setJobGroup(jobGroup);
	}

	/**
	 * Returns whether this job should be run.
	 * If <code>false</code> is returned, this job will be discarded by the job manager
	 * without running.
	 * <p>
	 * This method is called immediately prior to calling the job's
	 * run method, so it can be used for last minute precondition checking before
	 * a job is run. This method must not attempt to schedule or change the
	 * state of any other job.
	 * </p><p>
	 * Clients may override this method.  This default implementation always returns
	 * <code>true</code>.
	 * </p>
	 *
	 * @return <code>true</code> if this job should be run
	 *   and <code>false</code> otherwise
	 */
	public boolean shouldRun() {
		return true;
	}

	/**
	 * Returns whether this job should be scheduled.
	 * If <code>false</code> is returned, this job will be discarded by the job manager
	 * without being added to the queue.
	 * <p>
	 * This method is called immediately prior to adding the job to the waiting job
	 * queue.,so it can be used for last minute precondition checking before
	 * a job is scheduled.
	 * </p><p>
	 * Clients may override this method.  This default implementation always returns
	 * <code>true</code>.
	 * </p>
	 *
	 * @return <code>true</code> if the job manager should schedule this job
	 *   and <code>false</code> otherwise
	 */
	@Override
	public boolean shouldSchedule() {
		return true;
	}

	/**
	 * Requests that this job be suspended.  If the job is currently waiting to be run, it
	 * will be removed from the queue move into the {@link #SLEEPING} state.
	 * The job will remain asleep until either resumed or canceled.  If this job is not
	 * currently waiting to be run, this method has no effect.
	 * <p>
	 * Sleeping jobs can be resumed using <code>wakeUp</code>.
	 *
	 * @return <code>false</code> if the job is currently running (and thus cannot
	 * be put to sleep), and <code>true</code> in all other cases
	 * @see #wakeUp()
	 */
	@Override
	public final boolean sleep() {
		return super.sleep();
	}

	/**
	 * Returns a string representation of this job to be used for debugging purposes only.
	 * @since org.eclipse.core.jobs 3.5
	 */
	@Override
	public String toString() {
		return super.toString();
	}

	/**
	 * Puts this job immediately into the {@link #WAITING} state so that it is
	 * eligible for immediate execution. If this job is not currently sleeping,
	 * the request is ignored.
	 * <p>
	 * This is a convenience method, fully equivalent to
	 * <code>wakeUp(0L)</code>.
	 * </p>
	 * @see #sleep()
	 */
	public final void wakeUp() {
		super.wakeUp(0L);
	}

	/**
	 * Puts this job back into the {@link #WAITING} state after
	 * the specified delay. This is equivalent to canceling the sleeping job and
	 * rescheduling with the given delay.  If this job is not currently sleeping,
	 * the request  is ignored.
	 *
	 * @param delay the number of milliseconds to delay
	 * @see #sleep()
	 */
	@Override
	public final void wakeUp(long delay) {
		super.wakeUp(delay);
	}

	/**
	 * Temporarily puts this <code>Job</code> back into {@link #WAITING} state and
	 * relinquishes the job's scheduling rule so that any {@link #WAITING} jobs that
	 * conflict with this job's scheduling rule have an opportunity to start. This
	 * method will wait until the rule this job held prior to invoking this method
	 * is re-acquired. This method has no effect and returns <code>null</code> if
	 * there are no {@link #WAITING} jobs that conflict with this job's scheduling
	 * rule.
	 * <p>
	 * <b>Note:</b> If this job has acquired any other locks, implicit or explicit,
	 * they will <i>not</i> be released. This may increase the risk of deadlock, so
	 * this method should only be used when it is known that the environment is
	 * safe.
	 * </p>
	 * <p>
	 * This method must be invoked by this job's <code>Thread</code>, and only when
	 * it is {@link #RUNNING} state.
	 * </p>
	 *
	 * @param monitor a progress monitor, or <code>null</code> if progress reporting
	 *                is not desired. Cancellation attempts will be ignored.
	 * @return The Job that was {@link #WAITING}, and blocked by this
	 *         <code>Job</code> (at the time this method was invoked) that was
	 *         unblocked and allowed a chance to run, or <code>null</code> if no
	 *         jobs were unblocked. Note: it is not guaranteed that this
	 *         <code>Job</code> resume immediately if other conflicting jobs are
	 *         also waiting after the unblocked job ends.
	 *
	 * @since org.eclipse.core.jobs 3.5
	 * @see Job#getRule()
	 * @see Job#isBlocking()
	 */
	@Override
	public Job yieldRule(IProgressMonitor monitor) {
		return super.yieldRule(monitor);
	}
}
