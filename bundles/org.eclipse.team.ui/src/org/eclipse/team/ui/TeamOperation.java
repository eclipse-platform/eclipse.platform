/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.ui;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.actions.ITeamRunnableContext;
import org.eclipse.team.internal.ui.actions.JobRunnableContext;
import org.eclipse.team.internal.ui.actions.ProgressDialogRunnableContext;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.progress.IProgressConstants;

/**
 * An operation that can be configured to run in the foreground using
 * the {@link org.eclipse.ui.progress.IProgressService} or the background
 * as a {@link org.eclipse.core.runtime.jobs.Job}. The execution context is determined
 * by what is returned by the {@link #canRunAsJob()} hint which may be overridden by subclasses.
 * Subclass must override the <code>run(IProgressMonitor)</code> method to perform
 * the behavior of the operation in the desired execution context.
 * <p>
 * If this operation is run as a job, it is registered with the job as a
 * {@link org.eclipse.core.runtime.jobs.IJobChangeListener} and is scheduled with
 * the part of this operation if it is not <code>null</code>.
 * Subclasses can override the methods of this interface to receive job change notifications.
 * </p>
 * @see org.eclipse.ui.progress.IProgressService
 * @see org.eclipse.core.runtime.jobs.Job
 * @see org.eclipse.core.runtime.jobs.ISchedulingRule
 * @see org.eclipse.core.runtime.jobs.IJobChangeListener
 * @since 3.0
 */
public abstract class TeamOperation extends JobChangeAdapter implements IRunnableWithProgress {

	private IWorkbenchPart part;
	private IRunnableContext context;

	/*
	 * Job context that configures how the team operation will
	 * interact with the progress service
	 */
	private static class TeamOperationJobContext extends JobRunnableContext {

		private final TeamOperation operation;
		private IAction gotoAction;

		public TeamOperationJobContext(TeamOperation operation) {
			super(operation.getJobName(), operation, operation.getSite());
			this.operation = operation;
		}

		@Override
		protected void configureJob(Job job) {
			super.configureJob(job);
			if (operation.isKeepOneProgressServiceEntry())
				job.setProperty(IProgressConstants.KEEPONE_PROPERTY, Boolean.TRUE);
			else if(operation.getKeepOperation())
				job.setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
			gotoAction = operation.getGotoAction();
			if(gotoAction != null)
				job.setProperty(IProgressConstants.ACTION_PROPERTY, gotoAction);
			URL icon = operation.getOperationIcon();
			if(icon != null)
				job.setProperty(IProgressConstants.ICON_PROPERTY, icon);
		}

		@Override
		protected boolean belongsTo(IContextJob job, Object family) {
			if (family instanceof IContextJob) {
				IContextJob otherJob = (IContextJob)family;
				IRunnableWithProgress runnable = otherJob.getRunnable();
				if (runnable instanceof TeamOperation) {
					return operation.isSameFamilyAs((TeamOperation)runnable);
				}
			}
			return operation.belongsTo(family);
		}

		@Override
		protected IStatus getCompletionStatus() {
			if (gotoAction != null) {
				return new Status(IStatus.OK, TeamUIPlugin.ID, IStatus.OK, gotoAction.getText(), null);
			}
			return super.getCompletionStatus();
		}

		@Override
		protected boolean isUser() {
			return operation.isUserInitiated();
		}
	}

	/**
	 * Create an team operation associated with the given part.
	 * @param part the part the operation is associated with or <code>null</code> if the
	 * operation is to be run without a part.
	 */
	protected TeamOperation(IWorkbenchPart part) {
		this(part, null);
	}

	/**
	 * Create an team operation that will run in the given context.
	 * @param context a runnable context
	 */
	protected TeamOperation(IRunnableContext context) {
		this(null, context);
	}

	/**
	 * Create an team operation associated with the given part
	 * that will run in the given context.
	 * @param part the part the operation is associated with or <code>null</code>
	 * @param context a runnable context
	 */
	protected TeamOperation(IWorkbenchPart part, IRunnableContext context) {
		this.part = part;
		this.context = context;
	}

	/**
	 * Return the part that is associated with this operation.
	 *
	 * @return Returns the part or <code>null</code>
	 */
	public IWorkbenchPart getPart() {
		return part;
	}

	/**
	 * Run the operation in a context that is determined by the {@link #canRunAsJob()}
	 * hint. If this operation can run as a job then it will be run in a background thread.
	 * Otherwise it will run in the foreground and block the caller.
	 */
	public final void run() throws InvocationTargetException, InterruptedException {
		if (shouldRun()) {
			getRunnableContext().run(this);
		}
	}

	/**
	 * This method is invoked from the <code>run()</code> method before
	 * the operation is run in the operation's context. Subclasses may
	 * override in order to perform pre-checks to determine if the operation
	 * should run. This may include prompting the user for information, etc.
	 *
	 * @return whether the operation should be run.
	 */
	protected boolean shouldRun() {
		return true;
	}

	/**
	 * Returns the scheduling rule that is to be obtained before this
	 * operation is executed by its context or <code>null</code> if
	 * no scheduling rule is to be obtained. If the operation is run
	 * as a job, the scheduling rule is used as the scheduling rule of the
	 * job. Otherwise, it is obtained before execution of the operation
	 * occurs.
	 * <p>
	 * By default, no scheduling
	 * rule is obtained. Subclasses can override in order to obtain a
	 * scheduling rule or can obtain scheduling rules within their operation
	 * if finer grained scheduling is desired.
	 *
	 * @return the scheduling rule to be obtained by this operation
	 * or <code>null</code>.
	 */
	protected ISchedulingRule getSchedulingRule() {
		return null;
	}

	/**
	 * Return whether the auto-build should be postponed until after
	 * the operation is complete. The default is to postpone the auto-build.
	 * subclasses can override.
	 *
	 * @return whether to postpone the auto-build while the operation is executing.
	 */
	protected boolean isPostponeAutobuild() {
		return true;
	}

	/**
	 * If this operation can safely be run in the background, then subclasses can
	 * override this method and return <code>true</code>. This will make their
	 * action run in a {@link  org.eclipse.core.runtime.jobs.Job}.
	 * Subclass that override this method should
	 * also override the <code>getJobName()</code> method.
	 *
	 * @return <code>true</code> if this action can be run in the background and
	 * <code>false</code> otherwise.
	 */
	protected boolean canRunAsJob() {
		return false;
	}

	/**
	 * Return the job name to be used if the action can run as a job. (i.e.
	 * if <code>canRunAsJob()</code> returns <code>true</code>).
	 *
	 * @return the string to be used as the job name
	 */
	protected String getJobName() {
		return ""; //$NON-NLS-1$
	}

	/**
	 * This method is called to allow subclasses to configure an action that could be run to show
	 * the results of the action to the user. Default is to return null.
	 *
	 * @return an action that could be run to see the results of this operation
	 */
	protected IAction getGotoAction() {
		return null;
	}

	/**
	 * This method is called to allow subclasses to configure an icon to show when running this
	 * operation.
	 *
	 * @return an URL to an icon
	 */
	protected URL getOperationIcon() {
		return null;
	}

	/**
	 * This method is called to allow subclasses to have the results of the
	 * operation remain available to the user in the progress service even after
	 * the job is done. This method is only relevant if the operation is run as
	 * a job (i.e., <code>canRunAsJob</code> returns <code>true</code>).
	 *
	 * @return <code>true</code> to keep the operation and <code>false</code>
	 *         otherwise.
	 */
	protected boolean getKeepOperation() {
		return false;
	}

	/**
	 * This method is similar to <code>getKeepOperation</code> but will
	 * only keep one entry of a particular type available.
	 * This method is only relevant if the operation is run as
	 * a job (i.e., <code>canRunAsJob</code> returns <code>true</code>).
	 * Subclasses that override this method should also override
	 * <code>isSameFamilyAs</code> in order to match operations of the same type.
	 *
	 * @return <code>true</code> to keep the operation and <code>false</code>
	 *         otherwise.
	 * @since 3.1
	 */
	public boolean isKeepOneProgressServiceEntry() {
		return false;
	}

	/**
	 * Return whether this Team operation belongs to the same family
	 * as the given operation for the purpose of showing only one
	 * operation of the same type in the progress service when
	 * <code>isKeepOneProgressServiceEntry</code> is overridden to
	 * return <code>true</code>. By default,
	 * <code>false</code> is returned. Subclasses may override.
	 * @param operation a team operation
	 * @since 3.1
	 */
	protected boolean isSameFamilyAs(TeamOperation operation) {
		return false;
	}

	/**
	 * Return whether the job that is running this operation should be considered
	 * a member member of the given family. Subclasses can override this method in
	 * order to support the family based functionality provided by the {@link IJobManager}.
	 * By default, <code>false</code> is always returned. Subclasses that override the
	 * <code>isKeepOneProgressServiceEntry</code> method do not need to override
	 * this method, but instead should override <code>isSameFamilyAs</code>.
	 *
	 * @param family the family being tested.
	 * @return whether the job that is running this operation should be considered
	 * a member member of the given family.
	 * @since 3.1
	 */
	public boolean belongsTo(Object family) {
		return false;
	}

	/**
	 * Indicates whether the operation was user initiated. The
	 * progress for user initiated jobs may be presented differently
	 * than non-user initiated operations if they are run as jobs.
	 * @return whether the operation is user initiated
	 * @since 3.1
	 */
	public boolean isUserInitiated() {
		return true;
	}

	/**
	 * Return a shell that can be used by the operation to display dialogs, etc.
	 *
	 * @return a shell
	 */
	protected Shell getShell() {
		final Shell[] shell = new Shell[] { null };
		if (Display.getCurrent() == null) {
			Display.getDefault().syncExec(() -> shell[0] = Utils.getShell(getSite()));
		} else {
			shell[0] = Utils.getShell(getSite());
		}
		return shell[0];
	}

	/*
	 * Uses the {@link #canRunAsJob()} hint to return a {@link ITeamRunnableContext}
	 * that is used to execute the <code>run(SyncInfoSet, IProgressMonitor)</code>
	 * method of this action.
	 *
	 * @param syncSet the sync info set containing the selected elements for which this
	 * action is enabled.
	 * @return the runnable context in which to run this action.
	 */
	private ITeamRunnableContext getRunnableContext() {
		if (context == null && canRunAsJob()) {
			JobRunnableContext context = new TeamOperationJobContext(this);
			context.setPostponeBuild(isPostponeAutobuild());
			context.setSchedulingRule(getSchedulingRule());
			return context;
		} else {
			ProgressDialogRunnableContext context = new ProgressDialogRunnableContext();
			context.setPostponeBuild(isPostponeAutobuild());
			context.setSchedulingRule(getSchedulingRule());
			if (this.context != null) {
				context.setRunnableContext(this.context);
			}
			return context;
		}
	}

	private IWorkbenchSite getSite() {
		IWorkbenchSite site = null;
		if(part != null) {
			site = part.getSite();
		}
		return site;
	}
}
