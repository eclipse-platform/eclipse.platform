/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.synchronize;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.subscribers.ISubscriberChangeEvent;
import org.eclipse.team.core.subscribers.ISubscriberChangeListener;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.internal.core.subscribers.SubscriberSyncInfoCollector;
import org.eclipse.team.internal.ui.synchronize.RefreshParticipantJob.IChangeDescription;

public class RefreshChangeListener implements ISubscriberChangeListener, IChangeDescription {
	private List<ISubscriberChangeEvent> changes = new ArrayList<>();
	private SubscriberSyncInfoCollector collector;
	private IResource[] resources;

	public RefreshChangeListener(IResource[] resources, SubscriberSyncInfoCollector collector) {
		this.resources = resources;
		this.collector = collector;
	}
	@Override
	public void subscriberResourceChanged(ISubscriberChangeEvent[] deltas) {
		for (ISubscriberChangeEvent delta : deltas) {
			if (delta.getFlags() == ISubscriberChangeEvent.SYNC_CHANGED) {
				changes.add(delta);
			}
		}
	}
	public SyncInfo[] getChanges() {
		List<SyncInfo> changedSyncInfos = new ArrayList<>();
		SyncInfoSet set = collector.getSyncInfoSet();
		for (ISubscriberChangeEvent delta : changes) {
			SyncInfo info = set.getSyncInfo(delta.getResource());
			if (info != null && interestingChange(info)) {
				changedSyncInfos.add(info);
			}
		}
		return changedSyncInfos.toArray(new SyncInfo[changedSyncInfos.size()]);
	}

	private boolean interestingChange(SyncInfo info) {
		int kind = info.getKind();
		if(isThreeWay()) {
			int direction = SyncInfo.getDirection(kind);
			return (direction == SyncInfo.INCOMING || direction == SyncInfo.CONFLICTING);
		} else {
			return SyncInfo.getChange(kind) != SyncInfo.IN_SYNC;
		}
	}

	private boolean isThreeWay() {
		return collector.getSubscriber().getResourceComparator().isThreeWay();
	}
	@Override
	public int getChangeCount() {
		return getChanges().length;
	}
	public IResource[] getResources() {
		return resources;
	}
}
