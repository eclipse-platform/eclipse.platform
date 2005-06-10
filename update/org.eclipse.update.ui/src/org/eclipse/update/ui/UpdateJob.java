/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.ui;

import java.util.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.ISite;
import org.eclipse.update.core.ISiteWithMirrors;
import org.eclipse.update.core.IURLEntry;
import org.eclipse.update.core.model.InstallAbortedException;
import org.eclipse.update.internal.core.Messages;
import org.eclipse.update.internal.core.UpdateCore;
import org.eclipse.update.internal.operations.UpdateUtils;
import org.eclipse.update.internal.ui.*;
import org.eclipse.update.internal.ui.wizards.*;
import org.eclipse.update.operations.IInstallFeatureOperation;
import org.eclipse.update.operations.OperationsManager;
import org.eclipse.update.search.IUpdateSearchResultCollector;
import org.eclipse.update.search.IUpdateSearchResultCollectorFromMirror;
import org.eclipse.update.search.UpdateSearchRequest;

/**
 * An UpdateJob performs the lookup for new features or updates to the existing features,
 * depending on how you construct it.
 * @since 3.1
 */
public class UpdateJob extends Job {
	
	private class SearchResultCollector implements IUpdateSearchResultCollector {
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
	private UpdateSearchRequest searchRequest;
	private ArrayList updates;
    private boolean isUpdate;
    private boolean download;
    private boolean isAutomatic;
    private IStatus jobStatus = Status.OK_STATUS;

    /**
     * Use this constructor to search for updates to installed features
     * @param isAutomatic true if automatically searching for updates   
     * @param name the job name
     * @param download download updates automatically
     */
	public UpdateJob( String name, boolean isAutomatic, boolean download ) {
		super(name);
        this.isUpdate = true;
        this.isAutomatic = isAutomatic;
        this.download = download;
		updates = new ArrayList();
		setPriority(Job.DECORATE);
	}

    /**
     * Use this constructor to search for features as indicated by the search request
     * @param name the job name
     * @param searchRequest the search request to execute
     */
    public UpdateJob( String name, UpdateSearchRequest searchRequest ) {
        super(name);
        this.searchRequest = searchRequest;
        updates = new ArrayList();
        setPriority(Job.DECORATE);
    }

    /**
     * Returns true if the job is performing update lookups, or false when searching for new features
     * @return true when searching for updates of existing features
     */
    public boolean isUpdate() {
        return isUpdate;
    }
    
	/* (non-Javadoc)
	 * @see org.eclipse.core.internal.jobs.InternalJob#belongsTo(java.lang.Object)
	 */
	public boolean belongsTo(Object family) {
		return UpdateJob.family == family;
	}
	
	/**
     * Runs the job and returns the OK status. Call getStatus() to get the actual execution status.
     * @param monitor progress monitor
     * @return IStatus the return code is always OK
	 */
    public IStatus run(IProgressMonitor monitor) {
        if (isUpdate)
            jobStatus = runUpdates(monitor);
        else
            jobStatus = runSearchForNew(monitor);
        return Status.OK_STATUS;
    }
    
    private IStatus runSearchForNew(IProgressMonitor monitor) {
        if (UpdateCore.DEBUG) {
            UpdateCore.debug("Search for features started."); //$NON-NLS-1$
        }

        try {
            if (resultCollector == null)
                resultCollector = new ResultCollectorWithMirrors();
            searchRequest.performSearch(resultCollector, monitor);
            if (UpdateCore.DEBUG) {
                UpdateCore.debug("Automatic update search finished - " //$NON-NLS-1$
                        + updates.size() + " results."); //$NON-NLS-1$
            }
            return Status.OK_STATUS;
        } catch (CoreException e) {
            return e.getStatus();
        }
    }
    

	private IStatus runUpdates(IProgressMonitor monitor) {
        ArrayList statusList = new ArrayList();
        if (UpdateCore.DEBUG) {
            if (isAutomatic)
                UpdateCore.debug("Automatic update search started."); //$NON-NLS-1$
            else
                UpdateCore.debug("Update search started."); //$NON-NLS-1$
        }
        searchRequest = UpdateUtils.createNewUpdatesRequest(null);

        if (resultCollector == null)
            resultCollector = new ResultCollectorWithMirrors();
        try {
            searchRequest.performSearch(resultCollector, monitor);
        } catch (CoreException e) {
            statusList.add(e.getStatus());
        }
        if (UpdateCore.DEBUG) {
            UpdateCore.debug("Automatic update search finished - " //$NON-NLS-1$
                    + updates.size() + " results."); //$NON-NLS-1$
        }
        if (updates.size() > 0) {
            // silently download if download enabled
            if (download) {
                if (UpdateCore.DEBUG) {
                    UpdateCore.debug("Automatic download of updates started."); //$NON-NLS-1$
                }
                for (int i = 0; i < updates.size(); i++) {
                    IInstallFeatureOperation op = (IInstallFeatureOperation) updates
                            .get(i);
                    IFeature feature = op.getFeature();
                    try {
                        UpdateUtils.downloadFeatureContent(op.getTargetSite(),
                                feature, null, monitor);
                    } catch (InstallAbortedException e) {
                        return Status.CANCEL_STATUS;
                    } catch (CoreException e) {
                        statusList.add(e.getStatus());
                        updates.remove(i);
                        i -= 1;
                    }
                }
                if (UpdateCore.DEBUG) {
                    UpdateCore.debug("Automatic download of updates finished."); //$NON-NLS-1$
                }
            }
        }
        
        if (statusList.size() == 0)
            return Status.OK_STATUS;
        else if (statusList.size() == 1)
            return (IStatus) statusList.get(0);
        else {
            IStatus[] children = (IStatus[]) statusList
                    .toArray(new IStatus[statusList.size()]);
            return new MultiStatus("org.eclipse.update.ui", //$NON-NLS-1$
                    ISite.SITE_ACCESS_EXCEPTION, children, Messages.Search_networkProblems, //$NON-NLS-1$
                    null);
        }
    }

    /**
     * Returns an array of features to install
     * @return IInstallFeatureOperation[]
     */
    public IInstallFeatureOperation[] getUpdates() {
        return (IInstallFeatureOperation[]) updates.toArray(new IInstallFeatureOperation[updates.size()]);
    }
    
    /**
     * Returns the job status upon termination.
     * @return IStatus
     */
    public IStatus getStatus() {
        return jobStatus;
    }
    
    /**
     * Returns the update search request for this job.
     * @return UpdateSearchRequest
     */
    public UpdateSearchRequest getSearchRequest() {
        return searchRequest;
    }
    
    private class ResultCollectorWithMirrors extends SearchResultCollector
            implements IUpdateSearchResultCollectorFromMirror {
        
        private HashMap mirrors = new HashMap(0);
        
        /* (non-Javadoc)
         * @see org.eclipse.update.search.IUpdateSearchResultCollectorFromMirror#getMirror(org.eclipse.update.core.ISite, java.lang.String)
         */
        public IURLEntry getMirror(final ISiteWithMirrors site, final String siteName) {
            //return null;
            if (mirrors.containsKey(site))
                return (IURLEntry)mirrors.get(site);
            try {
                IURLEntry[] mirrorURLs = site.getMirrorSiteEntries();
                if (mirrorURLs.length == 0)
                    return null;
                else {
                    // here we need to prompt the user
                    final IURLEntry[] returnValue = new IURLEntry[1];
                    UpdateUI.getStandardDisplay().syncExec(new Runnable() {
                        public void run() {
                            MirrorsDialog dialog = new MirrorsDialog(UpdateUI.getActiveWorkbenchShell(), site, siteName);
                            dialog.create();
                            dialog.open();
                            IURLEntry mirror = dialog.getMirror();
                            mirrors.put(site, mirror);
                            returnValue[0] = mirror;
                        }
                    });
                    return returnValue[0];
                }
            } catch (CoreException e) {
                return null;
            }
        }
    }
}
