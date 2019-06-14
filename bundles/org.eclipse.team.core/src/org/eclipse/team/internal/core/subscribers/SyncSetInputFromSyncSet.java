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
package org.eclipse.team.internal.core.subscribers;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.ITeamStatus;
import org.eclipse.team.core.synchronize.ISyncInfoSetChangeEvent;
import org.eclipse.team.core.synchronize.ISyncInfoSetChangeListener;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
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

	@Override
	public void disconnect() {
		if (inputSyncSet == null) return;
		inputSyncSet.removeSyncSetChangedListener(this);
		inputSyncSet = null;
	}

	@Override
	protected void fetchInput(IProgressMonitor monitor) {
		if (inputSyncSet == null) return;
		SyncInfo[] infos = inputSyncSet.getSyncInfos();
		for (SyncInfo info : infos) {
			collect(info, monitor);
		}
	}

	@Override
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
		for (SyncInfo info : infos) {
			collect(info, monitor);
		}
	}

	private void remove(IResource[] resources) {
		for (IResource resource : resources) {
			remove(resource);
		}
	}

	public void reset() {
		inputSyncSet.connect(this);
	}

	@Override
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

	@Override
	public void syncInfoSetErrors(SyncInfoSet set, ITeamStatus[] errors, IProgressMonitor monitor) {
		SubscriberSyncInfoSet syncSet = getSyncSet();
		try {
			syncSet.beginInput();
			for (ITeamStatus status : errors) {
				syncSet.addError(status);
			}
		} finally {
			syncSet.endInput(monitor);
		}
	}
}
