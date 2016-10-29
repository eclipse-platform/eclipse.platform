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
 * Provides caching for a {@link AbstractResourceVariantTree} using a
 * {@link ResourceVariantByteStore}.
 *
 * @see IResourceVariantTree
 * @see AbstractResourceVariantTree
 * @see ResourceVariantByteStore
 * @since 3.0
 */
public abstract class ResourceVariantTree extends AbstractResourceVariantTree {

	private ResourceVariantByteStore store;

	/**
	 * Create a resource variant tree that uses the provided byte store to
	 * cache the resource variant bytes.
	 * @param store the resource variant byte store used to cache resource variants
	 */
	protected ResourceVariantTree(ResourceVariantByteStore store) {
		this.store = store;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.variants.IResourceVariantTree#members(org.eclipse.core.resources.IResource)
	 */
	public IResource[] members(IResource resource) throws TeamException {
		return getByteStore().members(resource);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.variants.IResourceVariantTree#hasResourceVariant(org.eclipse.core.resources.IResource)
	 */
	public boolean hasResourceVariant(IResource resource) throws TeamException {
		return getByteStore().getBytes(resource) != null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.variants.IResourceVariantTree#flushVariants(org.eclipse.core.resources.IResource, int)
	 */
	public void flushVariants(IResource resource, int depth) throws TeamException {
		getByteStore().flushBytes(resource, depth);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.variants.AbstractResourceVariantTree#setVariant(org.eclipse.core.resources.IResource, org.eclipse.team.core.variants.IResourceVariant)
	 */
	protected boolean setVariant(IResource local, IResourceVariant remote) throws TeamException {
		ResourceVariantByteStore cache = getByteStore();
		byte[] newRemoteBytes = getBytes(local, remote);
		boolean changed;
		if (newRemoteBytes == null) {
			changed = cache.deleteBytes(local);
		} else {
			changed = cache.setBytes(local, newRemoteBytes);
		}
		return changed;
	}

	/**
	 * Get the byte store that is used to cache the serialization bytes
	 * for the resource variants of this tree. A byte store is used
	 * to reduce the memory footprint of the tree.
	 * <p>
	 * This method is not intended to be overridden by subclasses.
	 *
	 * @return the resource variant tree that is being refreshed.
	 */
	protected ResourceVariantByteStore getByteStore() {
		return store;
	}

	/**
	 * Get the bytes to be stored in the <code>ResourceVariantByteStore</code>
	 * from the given resource variant. By default, the <code>IResourceVariant#asBytes()</code>
	 * method is used to get the bytes.
	 * @param local the local resource
	 * @param remote the corresponding resource variant handle
	 * @return the bytes for the resource variant.
	 */
	protected byte[] getBytes(IResource local, IResourceVariant remote) throws TeamException {
		if (remote == null) return null;
		return remote.asBytes();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.variants.AbstractResourceVariantTree#collectChanges(org.eclipse.core.resources.IResource, org.eclipse.team.core.variants.IResourceVariant, int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IResource[] collectChanges(final IResource local,
			final IResourceVariant remote, final int depth, IProgressMonitor monitor)
			throws TeamException {
		final IResource[][] resources = new IResource[][] { null };
		getByteStore().run(local, new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				resources[0] = ResourceVariantTree.super.collectChanges(local, remote, depth, monitor);
			}
		}, monitor);
		return resources[0];
	}
}
