/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.core.subscribers;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.ITeamStatus;
import org.eclipse.team.core.synchronize.*;
import org.eclipse.team.internal.core.Policy;

/**
 * This class uses the contents of one sync set as the input of another.
 */
public class SyncSetInputFromSyncSet extends SyncSetInput implements ISyncInfoSetChangeListener {

	SubscriberSyncInfoSet inputSyncSet;

	public SyncSetInputFromSyncSet(SubscriberSyncInfoSet set, SubscriberEventHandler handler) {
		super(handler);
		this.inputSyncSet = set;
		inputSyncSet.addSyncSetChangedListener(this);
	}

	public void disconnect() {
		if (inputSyncSet == null) return;
		inputSyncSet.removeSyncSetChangedListener(this);
		inputSyncSet = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ccvs.syncviews.views.AbstractSyncSet#initialize(java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void fetchInput(IProgressMonitor monitor) {
		if (inputSyncSet == null) return;
		SyncInfo[] infos = inputSyncSet.getSyncInfos();
		for (int i = 0; i < infos.length; i++) {
			collect(infos[i], monitor);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ccvs.syncviews.views.ISyncSetChangedListener#syncSetChanged(org.eclipse.team.ccvs.syncviews.views.SyncSetChangedEvent)
	 */
	public void syncInfoChanged(ISyncInfoSetChangeEvent event, IProgressMonitor monitor) {
		SyncInfoSet syncSet = getSyncSet();
		try {
			syncSet.beginInput();
			monitor.beginTask(null, 105);
			syncSetChanged(event.getChangedResources(), Policy.subMonitorFor(monitor, 50));
			syncSetChanged(event.getAddedResources(), Policy.subMonitorFor(monitor, 50));
			remove(event.getRemovedResources());
		} finally {
			syncSet.endInput(Policy.subMonitorFor(monitor, 5));
		}
	}

	private void syncSetChanged(SyncInfo[] infos, IProgressMonitor monitor) {
		for (int i = 0; i < infos.length; i++) {
			collect(infos[i], monitor);
		}
	}

	private void remove(IResource[] resources) {
		for (int i = 0; i < resources.length; i++) {
			remove(resources[i]);
		}
	}

	public void reset() {
		inputSyncSet.connect(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.ISyncInfoSetChangeListener#syncInfoSetReset(org.eclipse.team.core.subscribers.SyncInfoSet, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void syncInfoSetReset(SyncInfoSet set, IProgressMonitor monitor) {
		if(inputSyncSet == null) {
			set.removeSyncSetChangedListener(this);
		} else {
			SyncInfoSet syncSet = getSyncSet();
			try {
				syncSet.beginInput();
				monitor.beginTask(null, 100);
				syncSet.clear();
				fetchInput(Policy.subMonitorFor(monitor, 95));
			} finally {
				syncSet.endInput(Policy.subMonitorFor(monitor, 5));
				monitor.done();
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.ISyncInfoSetChangeListener#syncInfoSetError(org.eclipse.team.core.subscribers.SyncInfoSet, org.eclipse.team.core.ITeamStatus[], org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void syncInfoSetErrors(SyncInfoSet set, ITeamStatus[] errors, IProgressMonitor monitor) {
		SubscriberSyncInfoSet syncSet = getSyncSet();
		try {
			syncSet.beginInput();
			for (int i = 0; i < errors.length; i++) {
				ITeamStatus status = errors[i];
				syncSet.addError(status);
			}
		} finally {
			syncSet.endInput(monitor);
		}
	}
}
