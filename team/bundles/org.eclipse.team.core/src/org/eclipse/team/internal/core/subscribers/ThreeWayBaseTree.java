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
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.ResourceVariantByteStore;
import org.eclipse.team.core.variants.ResourceVariantTree;
import org.eclipse.team.core.variants.ThreeWaySubscriber;

/**
 * Allow access to the base resource variants but do not support refresh
 * or modification.
 */
public final class ThreeWayBaseTree extends ResourceVariantTree {

	private ThreeWaySubscriber subscriber;

	/*
	 * A resource variant byte store that accesses the base bytes from a three-way
	 * synchronizer. The modification methods are disabled as the base should
	 * only be modified in the synchronizer directly.
	 */
	static class BaseResourceVariantByteStore extends ResourceVariantByteStore {
		private ThreeWaySubscriber subscriber;
		public BaseResourceVariantByteStore(ThreeWaySubscriber subscriber) {
			this.subscriber = subscriber;
		}
		@Override
		public void dispose() {
			// Nothing to do
		}
		@Override
		public byte[] getBytes(IResource resource) throws TeamException {
			return subscriber.getSynchronizer().getBaseBytes(resource);
		}
		@Override
		public boolean setBytes(IResource resource, byte[] bytes) throws TeamException {
			// Base bytes are set directly in the synchronizer
			return false;
		}
		@Override
		public boolean flushBytes(IResource resource, int depth) throws TeamException {
			// Base bytes are flushed directly in the synchronizer
			return false;
		}
		@Override
		public boolean deleteBytes(IResource resource) throws TeamException {
			// Base bytes are deleted directly in the synchronizer
			return false;
		}
		@Override
		public IResource[] members(IResource resource) throws TeamException {
			return subscriber.getSynchronizer().members(resource);
		}
	}

	/**
	 * Create a base resource variant tree that accesses the base bytes
	 * from a three-way synchronizer.
	 * @param subscriber the three-way subscriber
	 */
	public ThreeWayBaseTree(ThreeWaySubscriber subscriber) {
		super(new BaseResourceVariantByteStore(subscriber));
		this.subscriber = subscriber;
	}

	@Override
	public IResource[] refresh(IResource[] resources, int depth, IProgressMonitor monitor) throws TeamException {
		return new IResource[0];
	}

	@Override
	protected IResourceVariant[] fetchMembers(IResourceVariant variant, IProgressMonitor progress) throws TeamException {
		// Refresh not supported
		return new IResourceVariant[0];
	}

	@Override
	protected IResourceVariant fetchVariant(IResource resource, int depth, IProgressMonitor monitor) throws TeamException {
		// Refresh not supported
		return null;
	}

	@Override
	public IResource[] roots() {
		return getSubscriber().roots();
	}

	@Override
	public IResourceVariant getResourceVariant(IResource resource) throws TeamException {
		return getSubscriber().getResourceVariant(resource, getByteStore().getBytes(resource));
	}

	private ThreeWaySubscriber getSubscriber() {
		return subscriber;
	}

}
