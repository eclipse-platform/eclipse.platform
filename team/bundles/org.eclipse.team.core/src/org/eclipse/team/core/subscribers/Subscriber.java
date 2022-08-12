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
package org.eclipse.team.core.subscribers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.ITeamStatus;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.TeamStatus;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IDiffVisitor;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.core.variants.IResourceVariantComparator;
import org.eclipse.team.internal.core.Messages;
import org.eclipse.team.internal.core.Policy;
import org.eclipse.team.internal.core.TeamPlugin;
import org.eclipse.team.internal.core.mapping.SyncInfoToDiffConverter;

/**
 * A Subscriber provides synchronization between local resources and a
 * remote location that is used to share those resources.
 * <p>
 * When queried for the <code>SyncInfo</code> corresponding to a local resource using
 * <code>getSyncInfo(IResource)</code>, the subscriber should not contact the server.
 * Server round trips should only occur within the <code>refresh</code>
 * method of the subscriber. Consequently,
 * the implementation of a subscriber must cache enough state information for a remote resource to calculate the
 * synchronization state without contacting the server.  During a refresh, the latest remote resource state
 * information should be fetched and cached. For
 * a subscriber that supports three-way compare, the refresh should also fetch the latest base state unless this is
 * available by some other means (e.g. for some repository tools, the base state is persisted on disk with the
 * local resources).
 * </p>
 * <p>
 * After a refresh, the subscriber must notify any listeners of local resources whose corresponding remote resource
 * or base resource changed. The subscriber does not need to notify listeners when the state changes due to a local
 * modification since local changes are available through the <code>IResource</code> delta mechanism. However,
 * the subscriber must
 * cache enough information (e.g. the local timestamp of when the file was in-sync with its corresponding remote
 * resource)
 * to determine if the file represents an outgoing change so that <code>SyncInfo</code> obtained
 * after a delta will indicate that the file has an outgoing change. The subscriber must also notify listeners
 * when roots are added
 * or removed. For example, a subscriber for a repository provider would fire a root added event when a project
 * was shared
 * with a repository. No event is required when a root is deleted as this is available through the
 * <code>IResource</code> delta mechanism. It is up to clients to re-query the subscriber
 * when the state of a resource changes locally by listening to <code>IResource</code> deltas.
 * </p><p>
 * The remote and base states can also include the state for resources that do not exist locally (i.e outgoing deletions
 * or incoming additions). When queried for the members of a local resource, the subscriber should include any children
 * for which a remote exists even if the local does not.
 * </p>
 * @since 3.0
 */
abstract public class Subscriber {

	private List<ISubscriberChangeListener> listeners = new ArrayList<>(1);

	/**
	 * Return the name of this subscription, in a format that is
	 * suitable for display to an end user.
	 *
	 * @return String representing the name of this subscription.
	 */
	abstract public String getName();

	/**
	 * Returns <code>true</code> if this resource is supervised by this
	 * subscriber. A supervised resource is one for which this subscriber
	 * maintains the synchronization state. Supervised resources are the only
	 * resources returned when <code>members(IResource)</code> was invoked with the parent
	 * of the resource. Returns <code>false</code> in all
	 * other cases.
	 *
	 * @param resource the resource being tested
	 * @return <code>true</code> if this resource is supervised, and <code>false</code>
	 *               otherwise
	 * @throws TeamException if an error occurs
	 */
	abstract public boolean isSupervised(IResource resource) throws TeamException;

	/**
	 * Returns all non-transient member resources of the given resource. The
	 * result will include entries for resources that exist either in the
	 * workspace or are implicated in an incoming change. Returns an empty list
	 * if the given resource exists neither in the workspace nor in the
	 * corresponding subscriber location, or if the given resource is transient.
	 * <p>
	 * This is a fast operation; the repository is not contacted.
	 * </p>
	 * @param resource the resource
	 * @return a list of member resources
	 * @throws TeamException if an error occurs
	 */
	abstract public IResource[] members(IResource resource) throws TeamException;

	/**
	 * Returns the list of root resources this subscriber considers for
	 * synchronization. A client should call this method first then can safely
	 * call <code>members</code> to navigate the resources managed by this
	 * subscriber.
	 *
	 * @return a list of resources
	 */
	abstract public IResource[] roots();

	/**
	 * Returns synchronization info for the given resource, or <code>null</code>
	 * if there is no synchronization info because the subscriber does not apply
	 * to this resource.
	 * <p>
	 * Note that sync info may be returned for non-existing or for resources
	 * which have no corresponding remote resource.
	 * </p>
	 * <p>
	 * This method will be quick. If synchronization calculation requires
	 * content from the server it must be cached when the subscriber is
	 * refreshed. A client should call refresh before calling this method to
	 * ensure that the latest information is available for computing the sync
	 * state.
	 * </p>
	 * <p>
	 * The sync-info node returned by this method does not fully describe
	 * all types of changes. A more descriptive change can be obtained from
	 * the {@link #getDiff(IResource) } method.
	 *
	 * @param resource the resource of interest
	 * @return sync info
	 * @throws TeamException if an error occurs
	 * @see #getDiff(IResource)
	 */
	abstract public SyncInfo getSyncInfo(IResource resource) throws TeamException;

	/**
	 * Returns the comparison criteria that will be used by the sync info
	 * created by this subscriber.
	 *
	 * @return the comparator to use when computing sync states for this
	 * subscriber.
	 */
	abstract public IResourceVariantComparator getResourceComparator();

	/**
	 * Refreshes the resource hierarchy from the given resources and their
	 * children (to the specified depth) from the corresponding resources in the
	 * remote location. Resources are ignored in the following cases:
	 * <ul>
	 * <li>if they do not exist either in the workspace or in the corresponding
	 * remote location</li>
	 * <li>if the given resource is not supervised by this subscriber</li>
	 * <li>if the given resource is a closed project (they are ineligible for
	 * synchronization)</li>
	 * </ul>
	 * <p>
	 * Typical synchronization operations use the statuses computed by this
	 * method as the basis for determining what to do. It is possible for the
	 * actual sync status of the resource to have changed since the current
	 * local sync status was refreshed. Operations typically skip resources with
	 * stale sync information. The chances of stale information being used can
	 * be reduced by running this method (where feasible) before doing other
	 * operations. Note that this will of course affect performance.
	 * </p>
	 * <p>
	 * The depth parameter controls whether refreshing is performed on just the
	 * given resource (depth= <code>DEPTH_ZERO</code>), the resource and its
	 * children (depth= <code>DEPTH_ONE</code>), or recursively to the
	 * resource and all its descendents (depth= <code>DEPTH_INFINITE</code>).
	 * Use depth <code>DEPTH_ONE</code>, rather than depth
	 * <code>DEPTH_ZERO</code>, to ensure that new members of a project or
	 * folder are detected.
	 * </p>
	 * <p>
	 * This method might change resources; any changes will be reported in a
	 * subsequent subscriber resource change event indicating changes to server
	 * sync status.
	 * </p>
	 * <p>
	 * This method contacts the server and is therefore long-running; progress
	 * and cancellation are provided by the given progress monitor.
	 * </p>
	 * @param resources the resources
	 * @param depth valid values are <code>DEPTH_ZERO</code>,
	 * <code>DEPTH_ONE</code>, or <code>DEPTH_INFINITE</code>
	 * @param monitor progress monitor, or <code>null</code> if progress
	 * reporting and cancellation are not desired
	 * @exception TeamException if this method fails. Reasons include:
	 * <ul>
	 * <li>The server could not be contacted.</li>
	 * </ul>
	 */
	abstract public void refresh(IResource[] resources, int depth, IProgressMonitor monitor) throws TeamException;

	/**
	 * Adds a listener to this team subscriber. Has no effect if an identical
	 * listener is already registered.
	 * <p>
	 * Team resource change listeners are informed about state changes that
	 * affect the resources supervised by this subscriber.
	 * </p>
	 * @param listener a team resource change listener
	 */
	public void addListener(ISubscriberChangeListener listener) {
		synchronized (listeners) {
			if (!listeners.contains(listener)) {
				listeners.add(listener);
			}
		}
	}

	/**
	 * Removes a listener previously registered with this team subscriber. Has
	 * no effect if an identical listener is not registered.
	 *
	 * @param listener a team resource change listener
	 */
	public void removeListener(ISubscriberChangeListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	/**
	 * Adds all out-of-sync resources (<code>getKind() != SyncInfo.IN_SYNC</code>) that occur
	 * under the given resources to the specified depth. The purpose of this
	 * method is to provide subscribers a means of optimizing the determination
	 * of all out-of-sync descendants of a set of resources.
	 * <p>
	 * If any of the directly provided resources are not supervised by the subscriber, then
	 * they should be removed from the set.
	 * If errors occur while determining the sync info for the resources, they should
	 * be added to the set using <code>SyncInfoSet.addError</code>.
	 * </p>
	 * @param resources the root of the resource subtrees from which out-of-sync sync info should be collected
	 * @param depth the depth to which sync info should be collected
	 * (one of <code>IResource.DEPTH_ZERO</code>,
	 * <code>IResource.DEPTH_ONE</code>, or <code>IResource.DEPTH_INFINITE</code>)
	 * @param set the sync info set to which out-of-sync resources should be added (or removed). Any errors
	 * should be added to the set as well.
	 * @param monitor a progress monitor
	 */
	public void collectOutOfSync(IResource[] resources, int depth, SyncInfoSet set, IProgressMonitor monitor) {
		try {
			monitor.beginTask(null, 100 * resources.length);
			for (IResource resource : resources) {
				IProgressMonitor subMonitor = Policy.subMonitorFor(monitor, 100);
				subMonitor.beginTask(null, IProgressMonitor.UNKNOWN);
				collect(resource, depth, set, subMonitor);
				subMonitor.done();
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Fires a team resource change event to all registered listeners. Only
	 * listeners registered at the time this method is called are notified.
	 * Listener notification makes use of an <code>ISafeRunnable</code> to ensure that
	 * client exceptions do not affect the notification to other clients.
	 */
	protected void fireTeamResourceChange(final ISubscriberChangeEvent[] deltas) {
		ISubscriberChangeListener[] allListeners;
		// Copy the listener list so we're not calling client code while synchronized
		synchronized (listeners) {
			allListeners = listeners.toArray(new ISubscriberChangeListener[listeners.size()]);
		}
		// Notify the listeners safely so all will receive notification
		for (ISubscriberChangeListener listener : allListeners) {
			SafeRunner.run(new ISafeRunnable() {
				@Override
				public void handleException(Throwable exception) {
					// don't log the exception....it is already being logged in
					// Platform#run
				}
				@Override
				public void run() throws Exception {
					listener.subscriberResourceChanged(deltas);
				}
			});
		}
	}

	/*
	 * Collect the calculated synchronization information for the given resource at the given depth. The
	 * results are added to the provided list.
	 */
	private void collect(
		IResource resource,
		int depth,
		SyncInfoSet set,
		IProgressMonitor monitor) {

		Policy.checkCanceled(monitor);

		if (resource.getType() != IResource.FILE
			&& depth != IResource.DEPTH_ZERO) {
			try {
				IResource[] members = members(resource);
				for (IResource member : members) {
					collect(member, depth == IResource.DEPTH_INFINITE
							? IResource.DEPTH_INFINITE
							: IResource.DEPTH_ZERO, set, monitor);
				}
			} catch (TeamException e) {
				set.addError(new TeamStatus(IStatus.ERROR, TeamPlugin.ID, ITeamStatus.SYNC_INFO_SET_ERROR, NLS.bind(Messages.SubscriberEventHandler_8, new String[] { resource.getFullPath().toString(), e.getMessage() }), e, resource));
			}
		}

		monitor.subTask(NLS.bind(Messages.SubscriberEventHandler_2, new String[] { resource.getFullPath().toString() }));
		try {
			SyncInfo info = getSyncInfo(resource);
			if (info == null || info.getKind() == SyncInfo.IN_SYNC) {
				// Resource is no longer under the subscriber control.
				// This can occur for the resources past as arguments to collectOutOfSync
				set.remove(resource);
			} else {
				set.add(info);
			}
		} catch (TeamException e) {
			set.addError(new TeamStatus(
					IStatus.ERROR, TeamPlugin.ID, ITeamStatus.RESOURCE_SYNC_INFO_ERROR,
					NLS.bind(Messages.SubscriberEventHandler_9, new String[] { resource.getFullPath().toString(), e.getMessage() }),
					e, resource));
		}
		// Tick the monitor to give the owner a chance to do something
		monitor.worked(1);
	}

	/**
	 * Returns synchronization info, in the form of an {@link IDiff} for the
	 * given resource, or <code>null</code> if there is no synchronization
	 * info because the subscriber does not apply to this resource or the resource
	 * is in-sync.
	 * <p>
	 * Note that a diff may be returned for non-existing or for resources
	 * which have no corresponding remote resource.
	 * </p>
	 * <p>
	 * This method will be quick. If synchronization calculation requires
	 * content from the server it must be cached when the subscriber is
	 * refreshed. A client should call refresh before calling this method to
	 * ensure that the latest information is available for computing the diff.
	 * </p>
	 * <p>
	 * The diff node returned by this method describes the changes associated
	 * with the given resource in more detail than the sync-info returned
	 * by calling {@link #getSyncInfo(IResource) }.
	 *
	 * @param resource the resource of interest
	 * @return the diff for the resource or <code>null</code>
	 * @throws CoreException if an error occurs
	 * @throws TeamException if errors occur
	 * @since 3.2
	 */
	public IDiff getDiff(IResource resource) throws CoreException {
		SyncInfo info = getSyncInfo(resource);
		if (info == null || info.getKind() == SyncInfo.IN_SYNC)
			return null;
		return SyncInfoToDiffConverter.getDefault().getDeltaFor(info);
	}

	/**
	 * Visit any out-of-sync resources covered by the given traversals. Any resources
	 * covered by the traversals are ignored in the following cases:
	 * <ul>
	 * <li>if they do not exist either in the workspace or in the corresponding
	 * remote location</li>
	 * <li>if the given resource is not supervised by this subscriber</li>
	 * <li>if the given resource is a closed project (they are ineligible for
	 * synchronization)</li>
	 * </ul>
	 * @param traversals the traversals to be visited
	 * @param visitor the visitor
	 * @throws CoreException if an error occurs
	 * @throws TeamException if errors occur
	 * @since 3.2
	 */
	public void accept(ResourceTraversal[] traversals, IDiffVisitor visitor) throws CoreException {
		for (ResourceTraversal traversal : traversals) {
			accept(traversal.getResources(), traversal.getDepth(), visitor);
		}
	}

	/**
	 * Visit any out-of-sync resources in the given resources visited to the
	 * given depth. Resources are ignored in the following cases:
	 * <ul>
	 * <li>if they do not exist either in the workspace or in the corresponding
	 * remote location</li>
	 * <li>if the given resource is not supervised by this subscriber</li>
	 * <li>if the given resource is a closed project (they are ineligible for
	 * synchronization)</li>
	 * </ul>
	 *
	 * @param resources the root of the resource subtrees from which out-of-sync
	 *            sync info should be visited
	 * @param depth the depth to which sync info should be collected (one of
	 *            <code>IResource.DEPTH_ZERO</code>,
	 *            <code>IResource.DEPTH_ONE</code>, or
	 *            <code>IResource.DEPTH_INFINITE</code>)
	 * @param visitor the visitor
	 * @throws CoreException if errors occur
	 * @since 3.2
	 */
	public void accept(IResource[] resources, int depth, IDiffVisitor visitor) throws CoreException {
		for (IResource resource : resources) {
			accept(resource, depth, visitor);
		}
	}

	private void accept(IResource resource, int depth, IDiffVisitor visitor) throws CoreException {
		IDiff node = getDiff(resource);
		if (node != null && node.getKind() != IDiff.NO_CHANGE) {
			if (!visitor.visit(node))
				return;
		}
		if (depth != IResource.DEPTH_ZERO) {
			IResource[] members = members(resource);
			int newDepth = depth == IResource.DEPTH_INFINITE ? IResource.DEPTH_INFINITE : IResource.DEPTH_ZERO;
			for (IResource member : members) {
				accept(member, newDepth, visitor);
			}
		}
	}

	/**
	 * Refresh the subscriber for the given traversals. By default this method calls
	 * {@link #refresh(IResource[], int, IProgressMonitor) } for each traversal. Any resources
	 * covered by the traversals are ignored in the following cases:
	 * <ul>
	 * <li>if they do not exist either in the workspace or in the corresponding
	 * remote location</li>
	 * <li>if the given resource is not supervised by this subscriber</li>
	 * <li>if the given resource is a closed project (they are ineligible for
	 * synchronization)</li>
	 * </ul>
	 * <p>
	 * Subclasses may override.
	 * @param traversals the traversals to be refreshed
	 * @param monitor a progress monitor
	 * @throws TeamException if errors occur
	 * @since 3.2
	 */
	public void refresh(ResourceTraversal[] traversals, IProgressMonitor monitor) throws TeamException {
		monitor.beginTask(null, 100 * traversals.length);
		for (ResourceTraversal traversal : traversals) {
			refresh(traversal.getResources(), traversal.getDepth(), Policy.subMonitorFor(monitor, 100));
		}
		monitor.done();
	}

	/**
	 * Return the synchronization state of the given resource mapping.
	 * Only return the portion of the synchronization state that matches
	 * the provided <code>stateMask</code>. The synchronization state flags that are
	 * guaranteed to be interpreted by this method are:
	 * <ul>
	 * <li>The kind flags {@link IDiff#ADD}, {@link IDiff#REMOVE} and {@link IDiff#CHANGE}.
	 * If none of these flags are included then all are assumed.
	 * <li>The direction flags {@link IThreeWayDiff#INCOMING} and {@link IThreeWayDiff#OUTGOING} if the
	 * subscriber is a three-way subscriber. If neither are provided, both are assumed.
	 * </ul>
	 * Other flags can be included and may or may not be interpreted by the subscriber.
	 * <p>
	 * An element will only include {@link IDiff#ADD} in the returned state if all resources covered
	 * by the traversals mappings are added. Similarly, {@link IDiff#REMOVE} will only be included
	 * if all the resources covered by the traversals are deleted. Otherwise {@link IDiff#CHANGE}
	 * will be returned.
	 *
	 * @param mapping the resource mapping whose synchronization state is to be determined
	 * @param stateMask the mask that identifies the state flags of interested
	 * @param monitor a progress monitor
	 * @return the synchronization state of the given resource mapping
	 * @throws CoreException if an error occurs
	 * @since 3.2
	 * @see IDiff
	 * @see IThreeWayDiff
	 */
	public int getState(ResourceMapping mapping, int stateMask, IProgressMonitor monitor) throws CoreException {
		ResourceTraversal[] traversals = mapping.getTraversals(new SubscriberResourceMappingContext(this, true), monitor);
		final int[] direction = new int[] { 0 };
		final int[] kind = new int[] { 0 };
		accept(traversals, diff -> {
			if (diff instanceof IThreeWayDiff) {
				IThreeWayDiff twd = (IThreeWayDiff) diff;
				direction[0] |= twd.getDirection();
			}
			// If the traversals contain a combination of kinds, return a CHANGE
			int diffKind = diff.getKind();
			if (kind[0] == 0)
				kind[0] = diffKind;
			if (kind[0] != diffKind) {
				kind[0] = IDiff.CHANGE;
			}
			// Only need to visit the children of a change
			return diffKind == IDiff.CHANGE;
		});
		return (direction[0] | kind[0]) & stateMask;
	}
}
