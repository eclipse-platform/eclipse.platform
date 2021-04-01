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
 *     Eugene Kuleshov (eu@md.pp.ru) - Bug 138152 Improve sync job status reporting
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.ProgressMonitorWrapper;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.synchronize.AbstractSynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ISynchronizeManager;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.SubscriberParticipant;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.progress.IProgressConstants;
import org.eclipse.ui.progress.UIJob;

/**
 * Job to refresh a {@link Subscriber} in the background. The job can be configured
 * to be re-scheduled and run at a specified interval.
 * <p>
 * The job supports a basic work flow for modal/non-modal usage. If the job is
 * run in the foreground (e.g. in a modal progress dialog) the refresh listeners
 * action is invoked immediately after the refresh is completed. Otherwise the refresh
 * listeners action is associated to the job as a <i>goto</i> action. This will
 * allow the user to select the action in the progress view and run it when they
 * choose.
 * </p>
 * @since 3.0
 */
public abstract class RefreshParticipantJob extends Job {

	/**
	 * Uniquely identifies this type of job. This is used for cancellation.
	 */
	private final static Object FAMILY_ID = new Object();

	/**
	 * If true this job will be restarted when it completes
	 */
	private boolean reschedule = false;

	/**
	 * If true a rescheduled refresh job should be restarted when canceled
	 */
	private boolean restartOnCancel = true;

	/**
	 * The schedule delay used when rescheduling a completed job
	 */
	private static long scheduleDelay;

	/**
	 * The participant that is being refreshed.
	 */
	private ISynchronizeParticipant participant;

	/**
	 * The task name for this refresh. This is usually more descriptive than the
	 * job name.
	 */
	private String taskName;

	/**
	 * Refresh started/completed listener for every refresh
	 */
	private static List<IRefreshSubscriberListener> listeners = new ArrayList<>(1);
	private static final int STARTED = 1;
	private static final int DONE = 2;

	/*
	 * Lock used to sequence refresh jobs
	 */
	private static final ILock lock = Job.getJobManager().newLock();

	/*
	 * Constant used for postponement
	 */
	private static final IStatus POSTPONED = new Status(IStatus.CANCEL, TeamUIPlugin.ID, 0, "Scheduled refresh postponed due to conflicting operation", null); //$NON-NLS-1$

	/*
	 * Action wrapper which allows the goto action
	 * to be set later. It also handles errors
	 * that have occurred during the refresh
	 */
	private final class GotoActionWrapper extends WorkbenchAction {
		private ActionFactory.IWorkbenchAction gotoAction;
		private IStatus status;
		@Override
		public void run() {
			if (status != null && !status.isOK()) {
				ErrorDialog.openError(Utils.getShell(null), null, TeamUIMessages.RefreshSubscriberJob_3, status);
			} else if(gotoAction != null) {
				gotoAction.run();
			}
		}
		@Override
		public boolean isEnabled() {
			if(gotoAction != null) {
				return gotoAction.isEnabled();
			}
			return true;
		}
		@Override
		public String getText() {
			if(gotoAction != null) {
				return gotoAction.getText();
			}
			return null;
		}
		@Override
		public String getToolTipText() {
			if (status != null && !status.isOK()) {
				return status.getMessage();
			}
			if(gotoAction != null) {
				return gotoAction.getToolTipText();
			}
			return Utils.shortenText(SynchronizeView.MAX_NAME_LENGTH, RefreshParticipantJob.this.getName());
		}
		@Override
		public void dispose() {
			super.dispose();
			if(gotoAction != null) {
				gotoAction.dispose();
			}
		}
		public void setGotoAction(ActionFactory.IWorkbenchAction gotoAction) {
			this.gotoAction = gotoAction;
			setEnabled(isEnabled());
			setToolTipText(getToolTipText());
			gotoAction.addPropertyChangeListener(event -> {
				if(event.getProperty().equals(IAction.ENABLED)) {
					Boolean bool = (Boolean) event.getNewValue();
					GotoActionWrapper.this.setEnabled(bool.booleanValue());
				}
			});
		}
		public void setStatus(IStatus status) {
			this.status = status;
		}
	}

	/**
	 * Notification for safely notifying listeners of refresh lifecycle.
	 */
	private abstract static class Notification implements ISafeRunnable {
		private IRefreshSubscriberListener listener;
		@Override
		public void handleException(Throwable exception) {
			// don't log the exception....it is already being logged in Platform#run
		}
		public void run(IRefreshSubscriberListener listener) {
			this.listener = listener;
			SafeRunner.run(this);
		}
		@Override
		public void run() throws Exception {
			notify(listener);
		}
		/**
		 * Subclasses override this method to send an event safely to a listener
		 * @param listener
		 */
		protected abstract void notify(IRefreshSubscriberListener listener);
	}

	/**
	 * Monitor wrapper that will indicate that the job is canceled
	 * if the job is blocking another.
	 */
	private class NonblockingProgressMonitor extends ProgressMonitorWrapper {
		private final RefreshParticipantJob job;
		private long blockTime;
		private static final int THRESHOLD = 250;
		private boolean wasBlocking = false;
		protected NonblockingProgressMonitor(IProgressMonitor monitor, RefreshParticipantJob job) {
			super(monitor);
			this.job = job;
		}
		@Override
		public boolean isCanceled() {
			if (super.isCanceled()) {
				return true;
			}
			if (job.shouldReschedule() && job.isBlocking()) {
				if (blockTime == 0) {
					blockTime = System.currentTimeMillis();
				} else if (System.currentTimeMillis() - blockTime > THRESHOLD) {
					// We've been blocking for too long
					wasBlocking = true;
					return true;
				}
			} else {
				blockTime = 0;
			}
			wasBlocking = false;
			return false;
		}
		public boolean wasBlocking() {
			return wasBlocking;
		}
	}

	public static interface IChangeDescription {
		int getChangeCount();
	}

	/**
	 * Create a job to refresh the specified resources with the subscriber.
	 *
	 * @param participant the subscriber participant
	 * @param jobName
	 * @param taskName
	 * @param listener
	 */
	public RefreshParticipantJob(ISynchronizeParticipant participant, String jobName, String taskName, IRefreshSubscriberListener listener) {
		super(jobName);
		Assert.isNotNull(participant);
		this.participant = participant;
		this.taskName = taskName;
		setPriority(Job.DECORATE);
		setRefreshInterval(3600 /* 1 hour */);

		// Handle restarting of job if it is configured as a scheduled refresh job.
		addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				if(shouldReschedule()) {
					IStatus result = event.getResult();
					if(result.getSeverity() == IStatus.CANCEL && ! restartOnCancel) {
						return;
					}
					long delay = scheduleDelay;
					if (result == POSTPONED) {
						// Restart in 5 seconds
						delay = 5000;
					}
					RefreshParticipantJob.this.schedule(delay);
					restartOnCancel = true;
				}
			}
		});
		if(listener != null)
			initialize(listener);
	}

	@Override
	public boolean belongsTo(Object family) {
		if (family instanceof SubscriberParticipant) {
			return family == participant;
		} else {
			return (family == getFamily() || family == ISynchronizeManager.FAMILY_SYNCHRONIZE_OPERATION);
		}
	}

	public static Object getFamily() {
		return FAMILY_ID;
	}

	/**
	 * This is run by the job scheduler. A list of subscribers will be refreshed, errors will not stop the job
	 * and it will continue to refresh the other subscribers.
	 */
	@Override
	public IStatus run(IProgressMonitor monitor) {
		// Perform a pre-check for auto-build or manual build jobs
		// when auto-refreshing
		if (shouldReschedule() &&
				(isJobInFamilyRunning(ResourcesPlugin.FAMILY_AUTO_BUILD)
				|| isJobInFamilyRunning(ResourcesPlugin.FAMILY_MANUAL_BUILD))) {
			return POSTPONED;
		}
		// Only allow one refresh job at a time
		// NOTE: It would be cleaner if this was done by a scheduling
		// rule but at the time of writing, it is not possible due to
		// the scheduling rule containment rules.
		// Acquiring lock to ensure only one refresh job is running at a particular time
		boolean acquired = false;
		try {
			while (!acquired) {
				try {
					acquired = lock.acquire(1000);
				} catch (InterruptedException e1) {
					acquired = false;
				}
				Policy.checkCanceled(monitor);
			}

			IChangeDescription changeDescription = createChangeDescription();
			RefreshEvent event = new RefreshEvent(reschedule ? IRefreshEvent.SCHEDULED_REFRESH : IRefreshEvent.USER_REFRESH, participant, changeDescription);
			IStatus status = null;
			NonblockingProgressMonitor wrappedMonitor = null;
			try {
				event.setStartTime(System.currentTimeMillis());
				if(monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				// Pre-Notify
				notifyListeners(STARTED, event);
				// Perform the refresh
				monitor.setTaskName(getName());
				wrappedMonitor = new NonblockingProgressMonitor(monitor, this);
				doRefresh(changeDescription, wrappedMonitor);
				// Prepare the results
				setProperty(IProgressConstants.KEEPONE_PROPERTY, Boolean.valueOf(! isJobModal()));
			} catch(OperationCanceledException e2) {
				if (monitor.isCanceled()) {
					// The refresh was canceled by the user
					status = Status.CANCEL_STATUS;
				} else {
					// The refresh was canceled due to a blockage or a canceled authentication
					if (wrappedMonitor != null && wrappedMonitor.wasBlocking()) {
						status = POSTPONED;
					} else {
						status = Status.CANCEL_STATUS;
					}
				}
			} catch(CoreException e) {
				// Determine the status to be returned and the GOTO action
				status = e.getStatus();
				if (!isUser()) {
					// Use the GOTO action to show the error and return OK
					Object prop = getProperty(IProgressConstants.ACTION_PROPERTY);
					if (prop instanceof GotoActionWrapper) {
						GotoActionWrapper wrapper = (GotoActionWrapper)prop;
						wrapper.setStatus(e.getStatus());
						status = new Status(IStatus.OK, TeamUIPlugin.ID, IStatus.OK, e.getStatus().getMessage(), e);
					}
				}
				if (!isUser() && status.getSeverity() == IStatus.ERROR) {
					// Never prompt for errors on non-user jobs
					setProperty(IProgressConstants.NO_IMMEDIATE_ERROR_PROMPT_PROPERTY, Boolean.TRUE);
				}
			} finally {
				event.setStopTime(System.currentTimeMillis());
			}

			// Post-Notify
			if (status == null) {
				status = calculateStatus(event);
			}
			event.setStatus(status);
			notifyListeners(DONE, event);
			if (event.getChangeDescription().getChangeCount() > 0) {
				if (participant instanceof AbstractSynchronizeParticipant) {
					AbstractSynchronizeParticipant asp = (AbstractSynchronizeParticipant) participant;
					asp.firePropertyChange(participant, ISynchronizeParticipant.P_CONTENT, null, event.getChangeDescription());
				}
			}
			return event.getStatus();
		} finally {
			if (acquired) lock.release();
			monitor.done();
		}
	}

	protected abstract void doRefresh(IChangeDescription changeListener, IProgressMonitor monitor) throws CoreException;

	/**
	 * Return the total number of changes covered by the resources
	 * of this job.
	 * @return the total number of changes covered by the resources
	 * of this job
	 */
	protected abstract int getChangeCount();

	protected abstract int getIncomingChangeCount();
	protected abstract int getOutgoingChangeCount();

	private boolean isJobInFamilyRunning(Object family) {
		Job[] jobs = Job.getJobManager().find(family);
		if (jobs != null && jobs.length > 0) {
			for (Job job : jobs) {
				if (job.getState() != Job.NONE) {
					return true;
				}
			}
		}
		return false;
	}

	private IStatus calculateStatus(IRefreshEvent event) {
		StringBuilder text = new StringBuilder();
		int code = IStatus.OK;
		int changeCount = event.getChangeDescription().getChangeCount();
		int numChanges = getChangeCount();
		if (numChanges > 0) {
			code = IRefreshEvent.STATUS_CHANGES;

			int incomingChanges = getIncomingChangeCount();
			String numIncomingChanges = incomingChanges==0 ? ""  //$NON-NLS-1$
				: NLS.bind(TeamUIMessages.RefreshCompleteDialog_incomingChanges, Integer.toString(incomingChanges));

			int outgoingChanges = getOutgoingChangeCount();
			String numOutgoingChanges = outgoingChanges==0 ? ""  //$NON-NLS-1$
				: NLS.bind(TeamUIMessages.RefreshCompleteDialog_outgoingChanges, Integer.toString(outgoingChanges));

			String sep = incomingChanges>0 && outgoingChanges>0 ? "; " : "";  //$NON-NLS-1$ //$NON-NLS-2$

			if (changeCount > 0) {
			// New changes found
				code = IRefreshEvent.STATUS_NEW_CHANGES;
				String numNewChanges = Integer.toString(changeCount);
				if (changeCount == 1) {
					text.append(NLS.bind(TeamUIMessages.RefreshCompleteDialog_newChangesSingular, (new Object[]{getName(), numNewChanges, numIncomingChanges, sep, numOutgoingChanges})));
				} else {
					text.append(NLS.bind(TeamUIMessages.RefreshCompleteDialog_newChangesPlural, (new Object[]{getName(), numNewChanges, numIncomingChanges, sep, numOutgoingChanges})));
				}
			} else {
				// Refreshed resources contain changes
				if (numChanges == 1) {
					text.append(NLS.bind(TeamUIMessages.RefreshCompleteDialog_changesSingular, (new Object[]{getName(), Integer.valueOf(numChanges), numIncomingChanges, sep, numOutgoingChanges})));
				} else {
					text.append(NLS.bind(TeamUIMessages.RefreshCompleteDialog_changesPlural, (new Object[]{getName(), Integer.valueOf(numChanges), numIncomingChanges, sep, numOutgoingChanges})));
				}
			}
		} else {
			// No changes found
			code = IRefreshEvent.STATUS_NO_CHANGES;
			text.append(NLS.bind(TeamUIMessages.RefreshCompleteDialog_6, new String[] { getName() }));
		}
		return new Status(IStatus.OK, TeamUIPlugin.ID, code, text.toString(), null);
	}

	private void initialize(final IRefreshSubscriberListener listener) {
		final GotoActionWrapper actionWrapper = new GotoActionWrapper();

		IProgressMonitor group = Job.getJobManager().createProgressGroup();
		group.beginTask(taskName, 100);
		setProgressGroup(group, 80);
		handleProgressGroupSet(group, 20);
		setProperty(IProgressConstants.ICON_PROPERTY, participant.getImageDescriptor());
		setProperty(IProgressConstants.ACTION_PROPERTY, actionWrapper);
		setProperty(IProgressConstants.KEEPONE_PROPERTY, Boolean.valueOf(! isJobModal()));
		// Listener delegate
		IRefreshSubscriberListener autoListener = new IRefreshSubscriberListener() {
			@Override
			public void refreshStarted(IRefreshEvent event) {
				if(listener != null) {
					listener.refreshStarted(event);
				}
			}
			@Override
			public ActionFactory.IWorkbenchAction refreshDone(IRefreshEvent event) {
				if(listener != null) {
					boolean isModal = isJobModal();
					event.setIsLink(!isModal);
					final ActionFactory.IWorkbenchAction runnable = listener.refreshDone(event);
					if(runnable != null) {
						// If the job is being run modally then simply prompt the user immediately
						if(isModal) {
							if(runnable != null) {
								Job update = new UIJob("") { //$NON-NLS-1$
									@Override
									public IStatus runInUIThread(IProgressMonitor monitor) {
										runnable.run();
										return Status.OK_STATUS;
									}
								};
								update.setSystem(true);
								update.schedule();
							}
						} else {
							// If the job is being run in the background, don't interrupt the user and simply update the goto action
							// to perform the results.
							actionWrapper.setGotoAction(runnable);
						}
					}
					RefreshParticipantJob.removeRefreshListener(this);
				}
				return null;
			}
		};

		if (listener != null) {
			RefreshParticipantJob.addRefreshListener(autoListener);
		}
	}

	/**
	 * The progress group of this job has been set. Any subclasses should
	 * assign this group to any additional jobs they use to collect
	 * changes from the refresh.
	 * @param group a progress group
	 * @param ticks the ticks for the change collection job
	 */
	protected abstract void handleProgressGroupSet(IProgressMonitor group, int ticks);

	protected abstract IChangeDescription createChangeDescription();

	public long getScheduleDelay() {
		return scheduleDelay;
	}

	protected void start() {
		if(getState() == Job.NONE) {
			if(shouldReschedule()) {
				schedule(getScheduleDelay());
			}
		}
	}

	/**
	 * Specify the interval in seconds at which this job is scheduled.
	 * @param seconds delay specified in seconds
	 */
	public void setRefreshInterval(long seconds) {
		boolean restart = false;
		if(getState() == Job.SLEEPING) {
			restart = true;
			cancel();
		}
		scheduleDelay = seconds * 1000;
		if(restart) {
			start();
		}
	}

	public void setRestartOnCancel(boolean restartOnCancel) {
		this.restartOnCancel = restartOnCancel;
	}

	public void setReschedule(boolean reschedule) {
		this.reschedule = reschedule;
	}

	public boolean shouldReschedule() {
		return reschedule;
	}

	public static void addRefreshListener(IRefreshSubscriberListener listener) {
		synchronized(listeners) {
			if(! listeners.contains(listener)) {
				listeners.add(listener);
			}
		}
	}

	public static void removeRefreshListener(IRefreshSubscriberListener listener) {
		synchronized(listeners) {
			listeners.remove(listener);
		}
	}

	protected void notifyListeners(final int state, final IRefreshEvent event) {
		// Get a snapshot of the listeners so the list doesn't change while we're firing
		IRefreshSubscriberListener[] listenerArray;
		synchronized (listeners) {
			listenerArray = listeners.toArray(new IRefreshSubscriberListener[listeners.size()]);
		}
		// Notify each listener in a safe manner (i.e. so their exceptions don't kill us)
		for (IRefreshSubscriberListener listener : listenerArray) {
			Notification notification = new Notification() {
				@Override
				protected void notify(IRefreshSubscriberListener listener) {
					switch (state) {
						case STARTED:
							listener.refreshStarted(event);
							break;
						case DONE:
							listener.refreshDone(event);
							break;
						default:
							break;
					}
				}
			};
			notification.run(listener);
		}
	}

	private boolean isJobModal() {
		Boolean isModal = (Boolean)getProperty(IProgressConstants.PROPERTY_IN_DIALOG);
		if(isModal == null) return false;
		return isModal.booleanValue();
	}

	public ISynchronizeParticipant getParticipant() {
		return participant;
	}
}
