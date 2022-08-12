/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.team.internal.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.ui.IWorkbenchSite;

/**
 * This runnable context executes it's operation in the context of a background job.
 */
public class JobRunnableContext implements ITeamRunnableContext {

	private IJobChangeListener listener;
	private IWorkbenchSite site;
	private String jobName;
	private ISchedulingRule schedulingRule;
	private boolean postponeBuild;

	/*
	 * Interface that provides access to the runnable of the job so
	 * that subclasses can do belongsTo family checking.
	 */
	public interface IContextJob {
		IRunnableWithProgress getRunnable();
	}

	/*
	 * Class that provides a basic job (i.e. no resource specific interactions)
	 */
	private class BasicJob extends Job implements IContextJob {
		private final IRunnableWithProgress runnable;
		public BasicJob(String name, IRunnableWithProgress runnable) {
			super(name);
			this.runnable = runnable;
		}
		@Override
		public IStatus run(IProgressMonitor monitor) {
			return JobRunnableContext.this.run(runnable, monitor);
		}
		@Override
		public boolean belongsTo(Object family) {
			return JobRunnableContext.this.belongsTo(this, family);
		}
		@Override
		public IRunnableWithProgress getRunnable() {
			return runnable;
		}
	}

	/*
	 * Class that provides a resource job (i.e. resource specific interactions)
	 */
	private class ResourceJob extends WorkspaceJob implements IContextJob {
		private final IRunnableWithProgress runnable;
		public ResourceJob(String name, IRunnableWithProgress runnable) {
			super(name);
			this.runnable = runnable;
		}
		@Override
		public IStatus runInWorkspace(IProgressMonitor monitor) {
			return JobRunnableContext.this.run(runnable, monitor);
		}
		@Override
		public boolean belongsTo(Object family) {
			return JobRunnableContext.this.belongsTo(this, family);
		}
		@Override
		public IRunnableWithProgress getRunnable() {
			return runnable;
		}
	}
	public JobRunnableContext(String jobName, IJobChangeListener listener, IWorkbenchSite site) {
		this.jobName = jobName;
		this.listener = listener;
		this.site = site;
	}

	@Override
	public void run(IRunnableWithProgress runnable) {
		Job job;
		if (schedulingRule == null && !postponeBuild) {
			job = new BasicJob(jobName, runnable);
		} else {
			job = new ResourceJob(jobName, runnable);
		}
		if (listener != null) {
			job.addJobChangeListener(listener);
		}
		configureJob(job);
		Utils.schedule(job, site);
	}

	/**
	 * Configure the job. By default, the job is configured to be a user
	 * job meaning that it will make use of the progress service.
	 * Subclasses can tailor how the job appears in the progress service.
	 * @param job the job that will provide the execution context
	 */
	protected void configureJob(Job job) {
		if (schedulingRule != null) {
			job.setRule(schedulingRule);
		}
		job.setUser(isUser());
	}

	/**
	 * Set whether the auto-build will be postponed while this
	 * context is executing a runnable.
	 * @param postponeBuild whether to postpone the auto-build.
	 */
	public void setPostponeBuild(boolean postponeBuild) {
		this.postponeBuild = postponeBuild;
	}

	/**
	 * Return whether this job context is user initiated. Subclasses may override.
	 */
	protected boolean isUser() {
		return true;
	}

	/**
	 * Set the scheduling rule that will be obtained before the context
	 * executes a runnable or <code>null</code> if no scheduling rule is to be onbtained.
	 * @param schedulingRule The schedulingRule to be obtained or <code>null</code>.
	 */
	public void setSchedulingRule(ISchedulingRule schedulingRule) {
		this.schedulingRule = schedulingRule;
	}

	/* private */ IStatus run(IRunnableWithProgress runnable, IProgressMonitor monitor) {
		try {
			runnable.run(monitor);
		} catch (InvocationTargetException e) {
			return TeamException.asTeamException(e).getStatus();
		} catch (InterruptedException e) {
			return Status.CANCEL_STATUS;
		}
		return getCompletionStatus();
	}

	/**
	 * Return the completions status for the job.
	 * By default, <code>Status.OK_STATUS</code>
	 * is returned.
	 * @return the completions status for the job
	 */
	protected IStatus getCompletionStatus() {
		return Status.OK_STATUS;
	}

	/**
	 * Return whether the job for this context is in the given family.
	 * By default, <code>false</code> is returned. Subclasses may override.
	 * @param family the job family being queried
	 */
	protected boolean belongsTo(IContextJob job, Object family) {
		return false;
	}

}
