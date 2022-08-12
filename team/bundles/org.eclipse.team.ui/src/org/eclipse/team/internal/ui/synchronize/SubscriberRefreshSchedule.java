/*******************************************************************************
 * Copyright (c) 2003, 2017 IBM Corporation and others.
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
 *     Trevor S. Kaufman <endante@gmail.com> - bug 156152
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.actions.ActionFactory;

/**
 * Schedule to refresh a subscriber at a specified interval. The schedule can be disabled or enabled
 * and will create the refresh job.
 *
 * @since 3.0
 */
public class SubscriberRefreshSchedule {
	private long refreshInterval = 3600; // 1 hour default
	private Date refreshStart;
	private boolean runOnce = false;

	private boolean enabled = false;

	private RefreshParticipantJob job;

	private IRefreshable refreshable;

	private IRefreshEvent lastRefreshEvent;

	/**
	 * Key for settings in memento
	 */
	private static final String CTX_REFRESHSCHEDULE_INTERVAL = TeamUIPlugin.ID + ".CTX_REFRESHSCHEDULE_INTERVAL"; //$NON-NLS-1$

	/**
	 * Key for schedule in memento
	 */
	private static final String CTX_REFRESHSCHEDULE_ENABLED = TeamUIPlugin.ID + ".CTX_REFRESHSCHEDULE_ENABLED"; //$NON-NLS-1$

	/**
	 * Key for start date in memento
	 */
	private static final String CTX_REFRESHSCHEDULE_START = TeamUIPlugin.ID + ".CTX_REFRESHSCHEDULE_START"; //$NON-NLS-1$

	/**
	 * Key for run once in memento
	 */
	private static final String CTX_REFRESHSCHEDULE_RUNONCE = TeamUIPlugin.ID + ".CTX_REFRESHSCHEDULE_RUNONCE"; //$NON-NLS-1$

	private IRefreshSubscriberListener refreshSubscriberListener = new IRefreshSubscriberListener() {
		@Override
		public void refreshStarted(IRefreshEvent event) {
		}
		@Override
		public ActionFactory.IWorkbenchAction refreshDone(final IRefreshEvent event) {
			if (getRefreshable(event.getParticipant()) == refreshable) {
				lastRefreshEvent = event;
				if(enabled && event.getRefreshType() == IRefreshEvent.SCHEDULED_REFRESH) {
					RefreshUserNotificationPolicy policy = new RefreshUserNotificationPolicy(refreshable.getParticipant());
					policy.refreshDone(event);
				}
			}
			return null;
		}
		private IRefreshable getRefreshable(ISynchronizeParticipant participant) {
			return Adapters.adapt(participant, IRefreshable.class);
		}
	};


	public SubscriberRefreshSchedule(IRefreshable refreshable) {
		this.refreshable = refreshable;
		RefreshParticipantJob.addRefreshListener(refreshSubscriberListener);

//		Calendar cal = Calendar.getInstance();
//		cal.clear();
//		refreshStart = cal.getTime();
	}

	/**
	 * @return Returns the enabled.
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * @param enabled The enabled to set.
	 * @param allowedToStart Is the job allowed to start.
	 */
	public void setEnabled(boolean enabled, boolean allowedToStart) {
		boolean wasEnabled = isEnabled();
		this.enabled = enabled;
		if(enabled && ! wasEnabled) {
			if(allowedToStart) {
				startJob();
			}
		} else {
			stopJob();
		}
	}

	/**
	 * @return Returns the refreshInterval in seconds.
	 */
	public long getRefreshInterval() {
		return refreshInterval;
	}

	public ISynchronizeParticipant getParticipant() {
		return refreshable.getParticipant();
	}

	/**
	 * @param refreshInterval The refreshInterval to set.
	 */
	public void setRefreshInterval(long refreshInterval) {
		if(refreshInterval != getRefreshInterval()) {
			stopJob();
			this.refreshInterval = refreshInterval;
			runOnce = false;
			if(isEnabled()) {
				startJob();
			}
		}
	}

	public void startJob() {
		if(job == null) {
			job = refreshable.createJob(getRefreshIntervalAsString());
			job.setUser(false);
		} else if(job.getState() != Job.NONE){
			stopJob();
		}
		job.setRefreshInterval(getRefreshInterval());
		job.setRestartOnCancel(true);
		job.setReschedule(!runOnce);
		if (refreshStart != null) {
			job.schedule(getJobDelay());
		} else {
			job.schedule();
		}
	}

	/**
	 * @return schedule delay in milliseconds
	 */
	private long getJobDelay() {
		Calendar now = Calendar.getInstance();
		Calendar start = Calendar.getInstance();
		start.setTime(refreshStart);
		while (now.after(start)) {
			start.add(Calendar.DATE, 1); // tomorrow
		}
		return start.getTimeInMillis() - now.getTimeInMillis();
	}

	protected void stopJob() {
		if(job != null) {
			job.setRestartOnCancel(false /* don't restart the job */);
			job.setReschedule(false);
			job.cancel();
			job = null;
		}
	}

	public void dispose() {
		stopJob();
		RefreshParticipantJob.removeRefreshListener(refreshSubscriberListener);
	}

	public void saveState(IMemento memento) {
		memento.putString(CTX_REFRESHSCHEDULE_ENABLED, Boolean.toString(enabled));
		memento.putInteger(CTX_REFRESHSCHEDULE_INTERVAL, (int)refreshInterval);
		if (refreshStart != null)
			memento.putString(CTX_REFRESHSCHEDULE_START, Long.toString(refreshStart.getTime()));
		memento.putString(CTX_REFRESHSCHEDULE_RUNONCE, Boolean.toString(runOnce));
	}

	public static SubscriberRefreshSchedule init(IMemento memento, IRefreshable refreshable) {
		SubscriberRefreshSchedule schedule = new SubscriberRefreshSchedule(refreshable);
		if(memento != null) {
			String enabled = memento.getString(CTX_REFRESHSCHEDULE_ENABLED);
			int interval = memento.getInteger(CTX_REFRESHSCHEDULE_INTERVAL).intValue();
			String startString = memento.getString(CTX_REFRESHSCHEDULE_START);
			String runOnce = memento.getString(CTX_REFRESHSCHEDULE_RUNONCE);
			if (startString != null) {
				long start = Long.parseLong(startString);
				schedule.setRefreshStartTime(new Date(start));
			}
			schedule.setRefreshInterval(interval);
			schedule.setRunOnce("true".equals(runOnce) ? true : false); //$NON-NLS-1$
			schedule.setEnabled("true".equals(enabled) ? true : false, false /* don't start job */); //$NON-NLS-1$
		}
		// Use the defaults if a schedule hasn't been saved or can't be found.
		return schedule;
	}

	public static String refreshEventAsString(IRefreshEvent event) {
		if(event == null) {
			return TeamUIMessages.SyncViewPreferencePage_lastRefreshRunNever;
		}
		long stopMills = event.getStopTime();
		StringBuilder text = new StringBuilder();
		if(stopMills <= 0) {
			text.append(TeamUIMessages.SyncViewPreferencePage_lastRefreshRunNever);
		} else {
			Date lastTimeRun = new Date(stopMills);
			text.append(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(lastTimeRun));
		}
		int changeCount = event.getChangeDescription().getChangeCount();
		if (changeCount == 0) {
			text.append(TeamUIMessages.RefreshSchedule_7);
		} else if (changeCount == 1) {
			text.append(NLS.bind(TeamUIMessages.RefreshSchedule_changesSingular, new String[] { Integer.toString(changeCount) }));
		} else {
			text.append(NLS.bind(TeamUIMessages.RefreshSchedule_changesPlural, new String[] { Integer.toString(changeCount) }));
		}
		return text.toString();
	}

	public IRefreshEvent getLastRefreshEvent() {
		return lastRefreshEvent;
	}

	private String getRefreshIntervalAsString() {
		if (runOnce)
			return TeamUIMessages.RefreshSchedule_16;
		boolean hours = false;
		long seconds = getRefreshInterval();
		if(seconds <= 60) {
			seconds = 60;
		}
		long minutes = seconds / 60;
		if(minutes >= 60) {
			minutes = minutes / 60;
			hours = true;
		}
		String unit;
		if(minutes >= 1) {
			unit = (hours ? TeamUIMessages.RefreshSchedule_9 : TeamUIMessages.RefreshSchedule_10);
		} else {
			unit = (hours ? TeamUIMessages.RefreshSchedule_11 : TeamUIMessages.RefreshSchedule_12);
		}
		return NLS.bind(TeamUIMessages.RefreshSchedule_13, new String[] { Long.toString(minutes), unit });
	}

	public IRefreshable getRefreshable() {
		return refreshable;
	}

	/**
	 * @return The time when the job should start or <code>null</code> when it
	 *         should be run immediately.
	 */
	public Date getRefreshStartTime() {
		return refreshStart;
	}

	public void setRefreshStartTime(Date refreshStart) {
		if(refreshStart==null || refreshStart != getRefreshStartTime()) {
			stopJob();
			this.refreshStart = refreshStart;
			if(isEnabled()) {
				startJob();
			}
		}
	}

	/**
	 * @return Return <code>false</code> if the job should be run again when
	 *         finished, or <code>true</code> otherwise.
	 */
	public boolean getRunOnce() {
		return runOnce;
	}

	public void setRunOnce(boolean runOnce) {
		if (runOnce != getRunOnce()) {
			stopJob();
			this.runOnce = runOnce;
			if (isEnabled()) {
				startJob();
			}
		}
	}
}
