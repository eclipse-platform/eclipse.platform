/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.core.variants;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;

/**
 * A resource variant tree that caches and obtains its bytes from the remote slot
 * in a three-way synchronizer. Clients must subclass to provide remote resource
 * variant refresh functionality.
 *
 * @see ThreeWaySubscriber
 *
 * @since 3.0
 */
public abstract class ThreeWayRemoteTree extends ResourceVariantTree {

	private ThreeWaySubscriber subscriber;

	/*
	 * A resource variant byte store that accesses the remote bytes
	 * from a three-way synchronizer. Both access and modification
	 * are supported.
	 */
	static class RemoteResourceVariantByteStore extends ResourceVariantByteStore {
		private ThreeWaySynchronizer synchronizer;
		public RemoteResourceVariantByteStore(ThreeWaySynchronizer synchronizer) {
			this.synchronizer = synchronizer;
		}
		public void dispose() {
			// Nothing to do as contents are owned by the TargetSynchronizer
		}
		public byte[] getBytes(IResource resource) throws TeamException {
			return getSynchronizer().getRemoteBytes(resource);
		}
		public boolean setBytes(IResource resource, byte[] bytes) throws TeamException {
			return getSynchronizer().setRemoteBytes(resource, bytes);
		}
		public boolean flushBytes(IResource resource, int depth) throws TeamException {
			// This method is invoked when the remote bytes are stale and should be removed
			// This is handled by the ThreeWaySynchronizer so nothing needs to be done here.
			return false;
		}
		public boolean isVariantKnown(IResource resource) throws TeamException {
			return getSynchronizer().hasSyncBytes(resource);
		}
		public boolean deleteBytes(IResource resource) throws TeamException {
			return getSynchronizer().removeRemoteBytes(resource);
		}
		public IResource[] members(IResource resource) throws TeamException {
			return synchronizer.members(resource);
		}
		private ThreeWaySynchronizer getSynchronizer() {
			return synchronizer;
		}
	}

	/**
	 * Create a remote resource variant tree that stores and obtains
	 * it's bytes from the remote slot of the synchronizer of the
	 * given subscriber
	 * @param subscriber a three-way subscriber
	 */
	public ThreeWayRemoteTree(ThreeWaySubscriber subscriber) {
		super(new RemoteResourceVariantByteStore(subscriber.getSynchronizer()));
		this.subscriber = subscriber;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.core.subscribers.caches.IResourceVariantTree#roots()
	 */
	public IResource[] roots() {
		return getSubscriber().roots();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.core.subscribers.caches.IResourceVariantTree#getResourceVariant(org.eclipse.core.resources.IResource)
	 */
	public IResourceVariant getResourceVariant(IResource resource) throws TeamException {
		return getSubscriber().getResourceVariant(resource, getByteStore().getBytes(resource));
	}

	/**
	 * Return the subscriber associated with this resource variant tree.
	 * @return the subscriber associated with this resource variant tree
	 */
	protected ThreeWaySubscriber getSubscriber() {
		return subscriber;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.variants.AbstractResourceVariantTree#collectChanges(org.eclipse.core.resources.IResource, org.eclipse.team.core.variants.IResourceVariant, int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IResource[] collectChanges(final IResource local,
			final IResourceVariant remote, final int depth, IProgressMonitor monitor)
			throws TeamException {
		final IResource[][] resources = new IResource[][] { null };
		getSubscriber().getSynchronizer().run(local, new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				resources[0] = ThreeWayRemoteTree.super.collectChanges(local, remote, depth, monitor);
			}
		}, monitor);
		return resources[0];
	}
}
