/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
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
import org.eclipse.update.internal.ui.UpdateUI;
import org.eclipse.update.internal.ui.wizards.*;
import org.eclipse.update.operations.*;
import org.eclipse.update.search.*;

public class AutomaticUpdatesJob
	extends Job
	implements IUpdateSearchResultCollector {
	
	// job family	
	public static final Object family = new Object();
	
	private static final IStatus OK_STATUS =
		new Status(
			IStatus.OK,
			UpdateScheduler.getPluginId(),
			IStatus.OK,
			"",
			null);
	private UpdateSearchRequest searchRequest;
	private ArrayList updates;

	public AutomaticUpdatesJob() {
		super("Automatic Update Search");
		updates = new ArrayList();
		setPriority(Job.DECORATE);
	}

	public void accept(IFeature feature) {
		IInstallFeatureOperation operation =
			OperationsManager
				.getOperationFactory()
				.createInstallOperation(null, null, feature, null, null, null);
		updates.add(operation);
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
			UpdateCore.debug("Automatic update search started.");
		}
		searchRequest = UpdateUtils.createNewUpdatesRequest(null);
		try {
			searchRequest.performSearch(this, monitor);
			if (UpdateCore.DEBUG) {
				UpdateCore.debug("Automatic update search finished - "
				+ updates.size()
				+ " results.");
			}
			if (updates.size() > 0) {
				boolean download = UpdateCore.getPlugin().getPluginPreferences().getBoolean(UpdateScheduler.P_DOWNLOAD);
				// silently download if download enabled 
				if (download)
				{
					if (UpdateCore.DEBUG) {
						UpdateCore.debug("Automatic download of updates started.");
					}
					for (int i=0; i<updates.size(); i++) {
						IInstallFeatureOperation op = (IInstallFeatureOperation)updates.get(i);
						IFeature feature = op.getFeature();
						UpdateUtils.downloadFeatureContent(feature, monitor);
					}
					if (UpdateCore.DEBUG) {
						UpdateCore.debug("Automatic download of updates finished.");
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
				"Eclipse Updates",
				"New updates are available. Do you want to review and install them now?")) {
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
				"Eclipse Updates",
				"New updates are available and downloaded. Do you want to review and install them now?")) {
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
			new ResizableWizardDialog(
				UpdateScheduler.getActiveWorkbenchShell(),
				wizard);
		dialog.create();
		dialog.getShell().setText("Updates");
		dialog.getShell().setSize(600, 500);
		dialog.open();
		if (wizard.isSuccessfulInstall())
			UpdateUI.requestRestart();
	}
}