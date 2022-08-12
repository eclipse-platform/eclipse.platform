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
package org.eclipse.team.core.variants;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.team.core.Team;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.ISubscriberChangeEvent;
import org.eclipse.team.core.subscribers.SubscriberChangeEvent;
import org.eclipse.team.internal.core.TeamPlugin;
import org.eclipse.team.internal.core.subscribers.ThreeWayBaseTree;

/**
 * A resource variant tree subscriber whose trees use an underlying
 * <code>ThreeWaySynchronizer</code> to store and manage the
 * synchronization state for the local workspace. Subclasses need to
 * provide a subclass of <code>ThreeWayRemoteTree</code> and a method
 * to create resource variant handles from the bytes cached in the
 * <code>ThreeWaySynchronizer</code>.
 *
 * @see ThreeWaySynchronizer
 * @see ThreeWayRemoteTree
 * @see CachedResourceVariant
 *
 * @since 3.0
 */
public abstract class ThreeWaySubscriber extends ResourceVariantTreeSubscriber implements ISynchronizerChangeListener {

	private ThreeWayResourceComparator comparator;
	private ThreeWayBaseTree baseTree;
	private ThreeWayRemoteTree remoteTree;
	private ThreeWaySynchronizer synchronizer;

	/**
	 * Create a three-way subscriber that uses the given synchronizer
	 * to manage the synchronization state of local resources
	 * and their variants
	 * @param synchronizer the three-way synchronizer for this subscriber
	 */
	protected ThreeWaySubscriber(ThreeWaySynchronizer synchronizer) {
		this.synchronizer = synchronizer;
		baseTree = new ThreeWayBaseTree(this);
		getSynchronizer().addListener(this);
	}

	@Override
	protected final IResourceVariantTree getBaseTree() {
		return baseTree;
	}

	@Override
	protected final IResourceVariantTree getRemoteTree() {
		if (remoteTree == null) {
			remoteTree = createRemoteTree();
		}
		return remoteTree;
	}

	@Override
	public final IResourceVariantComparator getResourceComparator() {
		if (comparator == null) {
			comparator = new ThreeWayResourceComparator(this.getSynchronizer());
		}
		return comparator;
	}

	@Override
	public void syncStateChanged(IResource[] resources) {
		fireTeamResourceChange(SubscriberChangeEvent.asSyncChangedDeltas(this, resources));
	}

	/**
	 * Returns <code>false</code> for resources that are not children
	 * of a subscriber root, are ignored by the subscriber's synchronizer
	 * or are ignored by the <code>Team.ignoreHist(IResource)</code>. Returns
	 * <code>true</code> otherwise.
	 * @see org.eclipse.team.core.subscribers.Subscriber#isSupervised(IResource)
	 */
	@Override
	public boolean isSupervised(IResource resource) throws TeamException {
		if (!isChildOfRoot(resource)) return false;
		if (getSynchronizer().isIgnored(resource)) return false;
		if (Team.isIgnoredHint(resource)) return false;
		return true;
	}

	/**
	 * Return the three-way synchronizer of this subscriber.
	 * @return the three-way synchronizer of this subscriber.
	 */
	public ThreeWaySynchronizer getSynchronizer() {
		return synchronizer;
	}

	/**
	 * Create the resource variant for the given local resource from the
	 * given bytes. The bytes are those that were previously returned
	 * from a call to <code>IResourceVariant#asBytes()</code>.
	 * @param resource the local resource
	 * @param bytes the bytes that identify a variant of the resource
	 * @return the resource variant handle recreated from the bytes
	 * @throws TeamException if an error occurs
	 */
	public abstract IResourceVariant getResourceVariant(IResource resource, byte[] bytes) throws TeamException;

	/**
	 * Create the three-way remote tree which provides access to the
	 * remote bytes in the three-way synchronizer. This method is invoked
	 * once when the remote tree is first accessed. The returned object is
	 * cached and reused on subsequent accesses.
	 * @return the remote tree
	 */
	protected abstract ThreeWayRemoteTree createRemoteTree();

	/**
	 * Convenience method that can be used by subclasses to notify listeners
	 * when a root is added or removed from the subscriber. The added
	 * parameter should be <code>true</code> if the root was added and <code>false</code>
	 * if it was removed.
	 * @param resource the added or removed root
	 * @param added <code>true</code> if the root was added and <code>false</code>
	 * if it was removed.
	 */
	protected void handleRootChanged(IResource resource, boolean added) {
		if (added) {
			rootAdded(resource);
		} else {
			rootRemoved(resource);
		}
	}

	private void rootAdded(IResource resource) {
		SubscriberChangeEvent delta = new SubscriberChangeEvent(this, ISubscriberChangeEvent.ROOT_ADDED, resource);
		fireTeamResourceChange(new SubscriberChangeEvent[] { delta });
	}

	private void rootRemoved(IResource resource) {
		try {
			getSynchronizer().flush(resource, IResource.DEPTH_INFINITE);
		} catch (TeamException e) {
			TeamPlugin.log(e);
		}
		SubscriberChangeEvent delta = new SubscriberChangeEvent(this, ISubscriberChangeEvent.ROOT_REMOVED, resource);
		fireTeamResourceChange(new SubscriberChangeEvent[] { delta });
	}

	private boolean isChildOfRoot(IResource resource) {
		IResource[] roots = roots();
		IPath fullPath = resource.getFullPath();
		for (IResource root : roots) {
			if (root.getFullPath().isPrefixOf(fullPath)) {
				return true;
			}
		}
		return false;
	}
}
