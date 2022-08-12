/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * 	   IBM Corporation - initial API and implementation
 * 	   Eugene Kuleshov (eu@md.pp.ru) - Bug 138152 Improve sync job status reporting
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoTree;
import org.eclipse.team.internal.core.subscribers.SubscriberSyncInfoCollector;
import org.eclipse.team.ui.synchronize.SubscriberParticipant;

public class RefreshSubscriberParticipantJob extends RefreshParticipantJob {

	private final IResource[] resources;

	public RefreshSubscriberParticipantJob(SubscriberParticipant participant, String jobName, String taskName, IResource[] resources, IRefreshSubscriberListener listener) {
		super(participant, jobName, taskName, listener);
		this.resources = resources;
	}

	protected Subscriber getSubscriber() {
		return ((SubscriberParticipant)getParticipant()).getSubscriber();
	}

	private SubscriberSyncInfoCollector getCollector() {
		return ((SubscriberParticipant)getParticipant()).getSubscriberSyncInfoCollector();
	}

	@Override
	protected int getChangeCount() {
		int numChanges = 0;
		SubscriberSyncInfoCollector collector = getCollector();
		if (collector != null) {
			SyncInfoTree set = collector.getSyncInfoSet();
			for (IResource resource : resources) {
				SyncInfo[] infos = set.getSyncInfos(resource, IResource.DEPTH_INFINITE);
				if(infos != null && infos.length > 0) {
					numChanges += infos.length;
				}
			}
		}
		return numChanges;
	}

	@Override
	protected int getIncomingChangeCount() {
		return getChangesInMode(SyncInfo.INCOMING);
	}

	@Override
	protected int getOutgoingChangeCount() {
		return getChangesInMode(SyncInfo.OUTGOING);
	}

	private int getChangesInMode(int kind) {
		int numChanges = 0;
		SubscriberSyncInfoCollector collector = getCollector();
		if (collector != null) {
			SyncInfoTree set = collector.getSyncInfoSet();
			for (IResource resource : resources) {
				SyncInfo[] infos = set.getSyncInfos(resource, IResource.DEPTH_INFINITE);
				if (infos != null && infos.length > 0) {
					for (SyncInfo info : infos) {
						if ((info.getKind() & kind) > 0) {
							numChanges++;
						}
					}
				}
			}
		}
		return numChanges;
	}

	@Override
	protected RefreshParticipantJob.IChangeDescription createChangeDescription() {
		return new RefreshChangeListener(resources, getCollector());
	}

	@Override
	protected void handleProgressGroupSet(IProgressMonitor group, int ticks) {
		getCollector().setProgressGroup(group, ticks);
	}

	/**
	 * If a collector is available then run the refresh and the background event processing
	 * within the same progress group.
	 */
	@Override
	public boolean shouldRun() {
		// Ensure that any progress shown as a result of this refresh occurs hidden in a progress group.
		return getSubscriber() != null && getCollector().getSyncInfoSet() != null;
	}

	@Override
	public boolean belongsTo(Object family) {
		if(family instanceof RefreshSubscriberParticipantJob) {
			return ((RefreshSubscriberParticipantJob)family).getSubscriber() == getSubscriber();
		}
		return super.belongsTo(family);
	}

	@Override
	protected void doRefresh(IChangeDescription changeListener, IProgressMonitor monitor) throws TeamException {
		Subscriber subscriber = getSubscriber();
		if (subscriber != null) {
			try {
				subscriber.addListener((RefreshChangeListener)changeListener);
				subscriber.refresh(resources, IResource.DEPTH_INFINITE, monitor);
				getCollector().waitForCollector(monitor);
			} finally {
				subscriber.removeListener((RefreshChangeListener)changeListener);
			}
		}
	}
}
