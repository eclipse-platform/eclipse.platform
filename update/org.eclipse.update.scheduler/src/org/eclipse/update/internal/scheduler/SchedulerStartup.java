/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.scheduler;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.swt.custom.*;
import org.eclipse.ui.*;
import org.eclipse.update.internal.ui.*;
import org.eclipse.update.internal.ui.wizards.*;
import org.eclipse.update.search.*;


/**
 * This plug-in is loaded on startup to fork a job that
 * searches for new plug-ins.
 */
public class SchedulerStartup implements IStartup {
	// Preferences
	public static final String P_ENABLED = "enabled"; //$NON-NLS-1$

	public static final String P_SCHEDULE = "schedule"; //$NON-NLS-1$
	public static final String VALUE_ON_STARTUP = "on-startup"; //$NON-NLS-1$
	public static final String VALUE_ON_SCHEDULE = "on-schedule"; //$NON-NLS-1$
	
	public static final String P_DOWNLOAD = "download"; // value is true or false, default is false //$NON-NLS-1$

	// values are to be picked up from the arryas DAYS and HOURS 
	public static final String P_DAY = "day"; //$NON-NLS-1$
	public static final String P_HOUR = "hour"; //$NON-NLS-1$

	// Keeps track of running job
	private UpdateJob job;
    private static final Object automaticJobFamily = new Object();
	// Listener for job changes
	private UpdateJobChangeAdapter jobListener;

	public static final String[] DAYS =
		{
			UpdateScheduler.getString("SchedulerStartup.day"), //$NON-NLS-1$
			UpdateScheduler.getString("SchedulerStartup.Monday"), //$NON-NLS-1$
			UpdateScheduler.getString("SchedulerStartup.Tuesday"), //$NON-NLS-1$
			UpdateScheduler.getString("SchedulerStartup.Wednesday"), //$NON-NLS-1$
			UpdateScheduler.getString("SchedulerStartup.Thursday"), //$NON-NLS-1$
			UpdateScheduler.getString("SchedulerStartup.Friday"), //$NON-NLS-1$
			UpdateScheduler.getString("SchedulerStartup.Saturday"), //$NON-NLS-1$
			UpdateScheduler.getString("SchedulerStartup.Sunday") }; //$NON-NLS-1$

	public static final String[] HOURS =
		{
			UpdateScheduler.getString("SchedulerStartup.1AM"), //$NON-NLS-1$
			UpdateScheduler.getString("SchedulerStartup.2AM"), //$NON-NLS-1$
			UpdateScheduler.getString("SchedulerStartup.3AM"), //$NON-NLS-1$
			UpdateScheduler.getString("SchedulerStartup.4AM"), //$NON-NLS-1$
			UpdateScheduler.getString("SchedulerStartup.5AM"), //$NON-NLS-1$
			UpdateScheduler.getString("SchedulerStartup.6AM"), //$NON-NLS-1$
			UpdateScheduler.getString("SchedulerStartup.7AM"), //$NON-NLS-1$
			UpdateScheduler.getString("SchedulerStartup.8AM"), //$NON-NLS-1$
			UpdateScheduler.getString("SchedulerStartup.9AM"), //$NON-NLS-1$
			UpdateScheduler.getString("SchedulerStartup.10AM"), //$NON-NLS-1$
			UpdateScheduler.getString("SchedulerStartup.11AM"), //$NON-NLS-1$
			UpdateScheduler.getString("SchedulerStartup.12PM"), //$NON-NLS-1$
			UpdateScheduler.getString("SchedulerStartup.1PM"), //$NON-NLS-1$
			UpdateScheduler.getString("SchedulerStartup.2PM"), //$NON-NLS-1$
			UpdateScheduler.getString("SchedulerStartup.3PM"), //$NON-NLS-1$
			UpdateScheduler.getString("SchedulerStartup.4PM"), //$NON-NLS-1$
			UpdateScheduler.getString("SchedulerStartup.5PM"), //$NON-NLS-1$
			UpdateScheduler.getString("SchedulerStartup.6PM"), //$NON-NLS-1$
			UpdateScheduler.getString("SchedulerStartup.7PM"), //$NON-NLS-1$
			UpdateScheduler.getString("SchedulerStartup.8PM"), //$NON-NLS-1$
			UpdateScheduler.getString("SchedulerStartup.9PM"), //$NON-NLS-1$
			UpdateScheduler.getString("SchedulerStartup.10PM"), //$NON-NLS-1$
			UpdateScheduler.getString("SchedulerStartup.11PM"), //$NON-NLS-1$
			UpdateScheduler.getString("SchedulerStartup.12AM"), //$NON-NLS-1$
			};

    private class AutomaticUpdateJob extends UpdateJob {

        public AutomaticUpdateJob(String name, boolean isUpdate, boolean isAutomatic, boolean download) {
            super(name, isUpdate, isAutomatic, download);
        }

        public AutomaticUpdateJob(String name, UpdateSearchRequest searchRequest) {
            super(name, searchRequest);
        }

        public boolean belongsTo(Object family) {
            return SchedulerStartup.automaticJobFamily == family;
        }    
    }
    
	private class UpdateJobChangeAdapter extends JobChangeAdapter {
		public void done(IJobChangeEvent event) {
			if (event.getJob() == SchedulerStartup.this.job) {
                
                // prompt the user
                if (SchedulerStartup.this.job.getUpdates().size() > 0 && !InstallWizard.isRunning()) {
                    if (UpdateScheduler.getDefault().getPluginPreferences().getBoolean(UpdateScheduler.P_DOWNLOAD)) {
                        UpdateUI.getStandardDisplay().asyncExec(new Runnable() {
                            public void run() {
                                asyncNotifyDownloadUser();
                            }
                        });
                    } else {
                        UpdateUI.getStandardDisplay().asyncExec(new Runnable() {
                            public void run() {
                                asyncNotifyUser();
                            }
                        });
                    }
                }
				scheduleUpdateJob();
			}
		}
        
        private void asyncNotifyUser() {
            // ask the user to install updates
            UpdateUI.getStandardDisplay().beep();
            if (MessageDialog
                .openQuestion(
                    UpdateUI.getActiveWorkbenchShell(),
                    UpdateUI.getString("AutomaticUpdatesJob.EclipseUpdates1"), //$NON-NLS-1$
                    UpdateUI.getString("AutomaticUpdatesJob.UpdatesAvailable"))) { //$NON-NLS-1$
                BusyIndicator.showWhile(UpdateUI.getStandardDisplay(), new Runnable() {
                    public void run() {
                        openInstallWizard2();
                    }
                });
            }
            // notify the manager that the job is done
//            job.done(Status.OK_STATUS);
        }
        
        private void asyncNotifyDownloadUser() {
            // ask the user to install updates
            UpdateUI.getStandardDisplay().beep();
            if (MessageDialog
                .openQuestion(
                    UpdateUI.getActiveWorkbenchShell(),
                    UpdateUI.getString("AutomaticUpdatesJob.EclipseUpdates2"), //$NON-NLS-1$
                    UpdateUI.getString("AutomaticUpdatesJob.UpdatesDownloaded"))) { //$NON-NLS-1$
                BusyIndicator.showWhile(UpdateUI.getStandardDisplay(), new Runnable() {
                    public void run() {
                        openInstallWizard2();
                    }
                });
            } else {
                // Don't discard downloaded data, as next time we compare timestamps.
                
                // discard all the downloaded data from cache (may include old data as well)
                //Utilities.flushLocalFile();
            }
            // notify the manager that the job is done
//            job.done(Status.OK_STATUS);
        }

        private void openInstallWizard2() {
            if (InstallWizard.isRunning())
                // job ends and a new one is rescheduled
                return;
                
            InstallWizard2 wizard = new InstallWizard2(job.getSearchRequest(), job.getUpdates(), true);
            WizardDialog dialog =
                new ResizableInstallWizardDialog(
                    UpdateUI.getActiveWorkbenchShell(),
                    wizard,
                    UpdateUI.getString("AutomaticUpdatesJob.Updates")); //$NON-NLS-1$
            dialog.create();
            dialog.open();
        }
        
	}
	
	/**
	 * The constructor.
	 */
	public SchedulerStartup() {
		UpdateScheduler.setScheduler(this);
		jobListener = new UpdateJobChangeAdapter();
		Platform.getJobManager().addJobChangeListener(jobListener);
	}

	public void earlyStartup() {
		scheduleUpdateJob();
	}

	public void scheduleUpdateJob() {
		Preferences pref = UpdateScheduler.getDefault().getPluginPreferences();
		// See if automatic search is enabled at all
		if (pref.getBoolean(P_ENABLED) == false)
			return;

		String schedule = pref.getString(P_SCHEDULE);
		long delay = -1L;
		if (schedule.equals(VALUE_ON_STARTUP))
			// have we already started a job ?
			if (job == null)
				delay = 0L;
			else
				delay = -1L;
		else
			delay = computeDelay(pref);
		if (delay == -1L)
			return;
		startSearch(delay);
	}

	private int getDay(Preferences pref) {
		String day = pref.getString(P_DAY);
		for (int d = 0; d < DAYS.length; d++)
			if (DAYS[d].equals(day))
				switch (d) {
					case 0 :
						return -1;
					case 1 :
						return Calendar.MONDAY;
					case 2 :
						return Calendar.TUESDAY;
					case 3 :
						return Calendar.WEDNESDAY;
					case 4 :
						return Calendar.THURSDAY;
					case 5 :
						return Calendar.FRIDAY;
					case 6 :
						return Calendar.SATURDAY;
					case 7 :
						return Calendar.SUNDAY;
				}
		return -1;
	}

	private int getHour(Preferences pref) {
		String hour = pref.getString(P_HOUR);
		for (int h = 0; h < HOURS.length; h++)
			if (HOURS[h].equals(hour))
				return h + 1;
		return 1;
	}
	/*
	 * Computes the number of milliseconds from this moment
	 * to the next scheduled search. If that moment has
	 * already passed, returns 0L (start immediately).
	 */
	private long computeDelay(Preferences pref) {

		int target_d = getDay(pref);
		int target_h = getHour(pref);

		Calendar calendar = Calendar.getInstance();
		// may need to use the BootLoader locale
		int current_d = calendar.get(Calendar.DAY_OF_WEEK);
		// starts with SUNDAY
		int current_h = calendar.get(Calendar.HOUR_OF_DAY);
		int current_m = calendar.get(Calendar.MINUTE);
		int current_s = calendar.get(Calendar.SECOND);
		int current_ms = calendar.get(Calendar.MILLISECOND);

		long delay = 0L; // milliseconds

		if (target_d == -1) {
			// Compute the delay for "every day at x o'clock"
			// Is it now ?
			if (target_h == current_h && current_m == 0 && current_s == 0)
				return delay;

			int delta_h = target_h - current_h;
			if (target_h <= current_h)
				delta_h += 24;
			delay =
				((delta_h * 60 - current_m) * 60 - current_s) * 1000 - current_ms;
			return delay;
		} else {
			// Compute the delay for "every Xday at x o'clock"
			// Is it now ?
			if (target_d == current_d
				&& target_h == current_h
				&& current_m == 0
				&& current_s == 0)
				return delay;

			int delta_d = target_d - current_d;
			if (target_d < current_d
				|| target_d == current_d
				&& (target_h < current_h
					|| target_h == current_h
					&& current_m > 0))
				delta_d += 7;

			delay =
				(((delta_d * 24 + target_h - current_h) * 60 - current_m) * 60 - current_s)* 1000 - current_ms;

			return delay;
		}
		//return -1L;
	}

	private void startSearch(long delay) {
		if (job != null) {
			// cancel old job.
			// We need to deregister the listener first,so we won't automatically start another job
			Platform.getJobManager().removeJobChangeListener(jobListener);
			Platform.getJobManager().cancel(job);
			Platform.getJobManager().addJobChangeListener(jobListener);
		}
        String jobName = UpdateScheduler.getString("AutomaticUpdatesJob.AutomaticUpdateSearch"); //$NON-NLS-1$);
        boolean download = UpdateScheduler.getDefault().getPluginPreferences().getBoolean(UpdateScheduler.P_DOWNLOAD);
		job = new AutomaticUpdateJob(jobName, true, true, download );
		job.schedule(delay);
	}
}
