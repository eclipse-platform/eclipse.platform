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
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.team.core.ITeamStatus;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.TeamStatus;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.core.synchronize.SyncInfoTree;
import org.eclipse.team.internal.core.Messages;
import org.eclipse.team.internal.core.TeamPlugin;

/**
 * An event handler that collects {@link SyncInfo} in a {@link SyncInfoTree}.
 */
public class SubscriberSyncInfoEventHandler extends SubscriberEventHandler {

	// The set that receives notification when the resource synchronization state
	// has been calculated by the job.
	private final SyncSetInputFromSubscriber syncSetInput;

	private class SubscriberSyncInfoEvent extends SubscriberEvent {
		private final SyncInfo result;

		public SubscriberSyncInfoEvent(IResource resource, int type, int depth, SyncInfo result) {
			super(resource, type, depth);
			this.result = result;
		}
		public SyncInfo getResult() {
			return result;
		}
	}

	public static ISynchronizationScope createScope(IResource[] roots, Subscriber subscriber) {
		if (roots == null)
			roots = subscriber.roots();
		return new RootResourceSynchronizationScope(roots);
	}

	/**
	 * Create the event handler for the given subscriber and roots
	 * @param subscriber the subscriber
	 * @param roots the roots or <code>null</code> if the roots from the subscriber
	 * should be used.
	 */
	public SubscriberSyncInfoEventHandler(final Subscriber subscriber, IResource[] roots) {
		super(subscriber, createScope(roots, subscriber));
		this.syncSetInput = new SyncSetInputFromSubscriber(subscriber, this);
	}

	@Override
	protected void handleException(CoreException e, IResource resource, int code, String message) {
		super.handleException(e, resource, code, message);
		syncSetInput.handleError(new TeamStatus(IStatus.ERROR, TeamPlugin.ID, code, message, e, resource));
	}

	@Override
	protected void handleCancel(OperationCanceledException e) {
		super.handleCancel(e);
		syncSetInput.handleError(new TeamStatus(IStatus.ERROR, TeamPlugin.ID, ITeamStatus.SYNC_INFO_SET_CANCELLATION, Messages.SubscriberEventHandler_12, e, ResourcesPlugin.getWorkspace().getRoot()));
	}

	/**
	 * Return the sync set input that was created by this event handler
	 * @return the sync set input
	 */
	public SyncSetInputFromSubscriber getSyncSetInput() {
		return syncSetInput;
	}

	@Override
	protected void handleChange(IResource resource) throws TeamException {
		SyncInfo info = syncSetInput.getSubscriber().getSyncInfo(resource);
		// resource is no longer under the subscriber control
		if (info == null) {
			queueDispatchEvent(
				new SubscriberEvent(resource, SubscriberEvent.REMOVAL, IResource.DEPTH_ZERO));
		} else {
			queueDispatchEvent(
				new SubscriberSyncInfoEvent(resource, SubscriberEvent.CHANGE, IResource.DEPTH_ZERO, info));
		}
	}

	@Override
	protected void collectAll(
			IResource resource,
			int depth,
			IProgressMonitor monitor) {

		monitor.beginTask(null, IProgressMonitor.UNKNOWN);
		try {

			// Create a monitor that will handle preemption and dispatch if required
			IProgressMonitor collectionMonitor = new SubProgressMonitor(monitor, IProgressMonitor.UNKNOWN) {
				boolean dispatching = false;
				@Override
				public void subTask(String name) {
					dispatch();
					super.subTask(name);
				}
				private void dispatch() {
					if (dispatching) return;
					try {
						dispatching = true;
						handlePreemptiveEvents(this);
						handlePendingDispatch(this);
					} finally {
						dispatching = false;
					}
				}
				@Override
				public void worked(int work) {
					dispatch();
					super.worked(work);
				}
			};

			// Create a sync set that queues up resources and errors for dispatch
			SyncInfoSet collectionSet = new SyncInfoSet() {
				@Override
				public void add(SyncInfo info) {
					super.add(info);
					queueDispatchEvent(
							new SubscriberSyncInfoEvent(info.getLocal(), SubscriberEvent.CHANGE, IResource.DEPTH_ZERO, info));
				}
				@Override
				public void addError(ITeamStatus status) {
					if (status instanceof TeamStatus) {
						TeamStatus ts = (TeamStatus) status;
						IResource resource = ts.getResource();
						if (resource != null && !resource.getProject().isAccessible()) {
							// The project was closed while we were collecting sync info.
							// The close delta will cause us to clean up properly
							return;
						}
					}
					super.addError(status);
					TeamPlugin.getPlugin().getLog().log(status);
					syncSetInput.handleError(status);
				}
				@Override
				public void remove(IResource resource) {
					super.remove(resource);
					queueDispatchEvent(
							new SubscriberEvent(resource, SubscriberEvent.REMOVAL, IResource.DEPTH_ZERO));
				}
			};

			syncSetInput.getSubscriber().collectOutOfSync(new IResource[] { resource }, depth, collectionSet, collectionMonitor);

		} finally {
			monitor.done();
		}
	}

	@Override
	protected void dispatchEvents(SubscriberEvent[] events, IProgressMonitor monitor) {
		// this will batch the following set changes until endInput is called.
		SubscriberSyncInfoSet syncSet = syncSetInput.getSyncSet();
		try {
			syncSet.beginInput();
			for (SubscriberEvent event : events) {
				switch (event.getType()) {
					case SubscriberEvent.CHANGE :
						if (event instanceof SubscriberSyncInfoEvent) {
							SubscriberSyncInfoEvent se = (SubscriberSyncInfoEvent) event;
							syncSetInput.collect(se.getResult(), monitor);
						}
						break;
					case SubscriberEvent.REMOVAL :
						syncSet.remove(event.getResource(), event.getDepth());
						break;
				}
			}
		} finally {
			syncSet.endInput(monitor);
		}
	}

	/**
	 * Initialize all resources for the subscriber associated with the set. This
	 * will basically recalculate all synchronization information for the
	 * subscriber.
	 * <p>
	 * This method is synchronized with the queueEvent method to ensure that the
	 * two events queued by this method are back-to-back.
	 *
	 * @param roots
	 *            the new roots or <code>null</code> if the roots from the
	 *            subscriber should be used.
	 */
	public void reset(IResource[] roots) {
		RootResourceSynchronizationScope scope = (RootResourceSynchronizationScope)getScope();
		if (roots == null)
			roots = getSubscriber().roots();
		scope.setRoots(roots);
	}

	@Override
	protected synchronized void reset(ResourceTraversal[] oldTraversals, ResourceTraversal[] newTraversals) {
		// First, reset the sync set input to clear the sync set
		run(monitor -> syncSetInput.reset(monitor), false /* keep ordering the same */);
		// Then, prime the set from the subscriber
		super.reset(oldTraversals, newTraversals);
	}
}
