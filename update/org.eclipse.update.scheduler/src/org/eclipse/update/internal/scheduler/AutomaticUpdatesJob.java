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

import java.util.ArrayList;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.internal.operations.UpdateUtils;
import org.eclipse.update.internal.ui.wizards.*;
import org.eclipse.update.operations.*;
import org.eclipse.update.search.*;

public class AutomaticUpdatesJob extends Job {
	
	private class AutomaticSearchResultCollector implements IUpdateSearchResultCollector {
		public void accept(IFeature feature) {
			IInstallFeatureOperation operation =
				OperationsManager
					.getOperationFactory()
					.createInstallOperation(null, feature, null, null, null);
			updates.add(operation);
		}
	}
	
	// job family	
	public static final Object family = new Object();
	private IUpdateSearchResultCollector resultCollector;
	
	private static final IStatus OK_STATUS =
		new Status(
			IStatus.OK,
			UpdateScheduler.getPluginId(),
			IStatus.OK,
			"", //$NON-NLS-1$
			null);
	private UpdateSearchRequest searchRequest;
	private ArrayList updates;

	public AutomaticUpdatesJob() {
		super(UpdateScheduler.getString("AutomaticUpdatesJob.AutomaticUpdateSearch")); //$NON-NLS-1$
		updates = new ArrayList();
		setPriority(Job.DECORATE);
	}


	/**
	 * Returns the standard display to be used. The method first checks, if
	 * the thread calling this method has an associated disaply. If so, this
	 * display is returned. Otherwise the method returns the default display.
	 */
	public static Display getStandardDisplay() {
		Display display;
		display = Display.getCurrent();
		if (display == null)
			display = Display.getDefault();
		return display;
	}

	public boolean belongsTo(Object family) {
		return AutomaticUpdatesJob.family == family;
	}
	
	public IStatus run(IProgressMonitor monitor) {
		if (UpdateCore.DEBUG) {
			UpdateCore.debug("Automatic update search started."); //$NON-NLS-1$
		}
		searchRequest = UpdateUtils.createNewUpdatesRequest(null);
		try {
			if (resultCollector == null)
				resultCollector = new AutomaticSearchResultCollector();
			searchRequest.performSearch(resultCollector, monitor);
			if (UpdateCore.DEBUG) {
				UpdateCore.debug("Automatic update search finished - " //$NON-NLS-1$
				+ updates.size()
				+ " results."); //$NON-NLS-1$
			}
			if (updates.size() > 0) {
				boolean download = UpdateScheduler.getDefault().getPluginPreferences().getBoolean(UpdateScheduler.P_DOWNLOAD);
				// silently download if download enabled 
				if (download)
				{
					if (UpdateCore.DEBUG) {
						UpdateCore.debug("Automatic download of updates started."); //$NON-NLS-1$
					}
					for (int i=0; i<updates.size(); i++) {
						IInstallFeatureOperation op = (IInstallFeatureOperation)updates.get(i);
						IFeature feature = op.getFeature();
						UpdateUtils.downloadFeatureContent(feature, monitor);
					}
					if (UpdateCore.DEBUG) {
						UpdateCore.debug("Automatic download of updates finished."); //$NON-NLS-1$
					}
				}
				// prompt the user
				if (!InstallWizard.isRunning()) {
					if (download) {
						getStandardDisplay().asyncExec(new Runnable() {
							public void run() {
								asyncNotifyDownloadUser();
							}
						});
					} else {
						getStandardDisplay().asyncExec(new Runnable() {
							public void run() {
								asyncNotifyUser();
							}
						});	
					}
				}
				return Job.ASYNC_FINISH;
			}
		} catch (CoreException e) {
			return e.getStatus();
		}
		return OK_STATUS;
	}

	private void asyncNotifyUser() {
		// ask the user to install updates
		getStandardDisplay().beep();
		if (MessageDialog
			.openQuestion(
				UpdateScheduler.getActiveWorkbenchShell(),
				UpdateScheduler.getString("AutomaticUpdatesJob.EclipseUpdates1"), //$NON-NLS-1$
				UpdateScheduler.getString("AutomaticUpdatesJob.UpdatesAvailable"))) { //$NON-NLS-1$
			BusyIndicator.showWhile(getStandardDisplay(), new Runnable() {
				public void run() {
					openInstallWizard();
				}
			});
		}
		// notify the manager that the job is done
		done(OK_STATUS);
	}
	
	private void asyncNotifyDownloadUser() {
		// ask the user to install updates
		getStandardDisplay().beep();
		if (MessageDialog
			.openQuestion(
				UpdateScheduler.getActiveWorkbenchShell(),
				UpdateScheduler.getString("AutomaticUpdatesJob.EclipseUpdates2"), //$NON-NLS-1$
				UpdateScheduler.getString("AutomaticUpdatesJob.UpdatesDownloaded"))) { //$NON-NLS-1$
			BusyIndicator.showWhile(getStandardDisplay(), new Runnable() {
				public void run() {
					openInstallWizard();
				}
			});
		} else {
			// Don't discard downloaded data, as next time we compare timestamps.
			
			// discard all the downloaded data from cache (may include old data as well)
			//Utilities.flushLocalFile();
		}
		// notify the manager that the job is done
		done(OK_STATUS);
	}

	private void openInstallWizard() {
		if (InstallWizard.isRunning())
			// job ends and a new one is rescheduled
			return;
			
		InstallWizard wizard = new InstallWizard(searchRequest, updates);
		WizardDialog dialog =
			new ResizableInstallWizardDialog(
				UpdateScheduler.getActiveWorkbenchShell(),
				wizard,
				UpdateScheduler.getString("AutomaticUpdatesJob.Updates")); //$NON-NLS-1$
		dialog.create();
		dialog.open();
	}
}
