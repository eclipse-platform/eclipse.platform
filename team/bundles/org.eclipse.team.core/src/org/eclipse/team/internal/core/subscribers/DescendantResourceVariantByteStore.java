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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.ResourceVariantByteStore;

/**
 * A <code>ResourceVariantByteStore</code> that optimizes the memory footprint
 * of a remote resource variant tree by only storing those bytes that differ
 * from a base resource variant tree. This class should only be used for cases
 * where the base and remote are on the same line-of-descent. For example, when
 * the remote tree represents the current state of a branch and the base
 * represents the state of the same branch when the local workspace as last
 * refreshed.
 * <p>
 * This class also contains the logic that allows subclasses to determine if
 * bytes stored in the remote tree are on a different line-of-descent than the
 * base. This is necessary because it is possible for the base tree to change in
 * ways that invalidate the stored remote variants. For example, if the local
 * resources are moved from the main trunk to a branch, any cached remote
 * resource variants would be stale.
 *
 * @since 3.0
 */
public abstract class DescendantResourceVariantByteStore extends ResourceVariantByteStore {

	ResourceVariantByteStore baseStore, remoteStore;

	public DescendantResourceVariantByteStore(ResourceVariantByteStore baseCache, ResourceVariantByteStore remoteCache) {
		this.baseStore = baseCache;
		this.remoteStore = remoteCache;
	}

	/**
	 * This method will dispose the remote cache but not the base cache.
	 * @see org.eclipse.team.core.variants.ResourceVariantByteStore#dispose()
	 */
	@Override
	public void dispose() {
		remoteStore.dispose();
	}

	@Override
	public byte[] getBytes(IResource resource) throws TeamException {
		byte[] remoteBytes = remoteStore.getBytes(resource);
		byte[] baseBytes = baseStore.getBytes(resource);
		if (baseBytes == null) {
			// There is no base so use the remote bytes
			return remoteBytes;
		}
		if (remoteBytes == null) {
			if (isVariantKnown(resource)) {
				// The remote is known to not exist
				// TODO: The check for NO_REMOTE does not take into consideration the line-of-descent
				return remoteBytes;
			} else {
				// The remote was either never queried or was the same as the base.
				// In either of these cases, the base bytes are used.
				return baseBytes;
			}
		}
		if (isDescendant(resource, baseBytes, remoteBytes)) {
			// Only use the remote bytes if they are later on the same line-of-descent as the base
			return remoteBytes;
		}
		// Use the base sbytes since the remote bytes must be stale (i.e. are
		// not on the same line-of-descent
		return baseBytes;
	}

	@Override
	public boolean setBytes(IResource resource, byte[] bytes) throws TeamException {
		byte[] baseBytes = baseStore.getBytes(resource);
		if (baseBytes != null && equals(baseBytes, bytes)) {
			// Remove the existing bytes so the base will be used (thus saving space)
			return remoteStore.flushBytes(resource, IResource.DEPTH_ZERO);
		} else {
			return remoteStore.setBytes(resource, bytes);
		}
	}

	@Override
	public boolean flushBytes(IResource resource, int depth) throws TeamException {
		return remoteStore.flushBytes(resource, depth);
	}

	/**
	 * Return <code>true</code> if the variant associated with the given local
	 * resource has been cached. This method is useful for those cases when
	 * there are no bytes for a resource variant and the client wants to
	 * know if this means that the remote does exist (i.e. this method returns
	 * <code>true</code>) or the remote has not been fetched (i.e. this method returns
	 * <code>false</code>).
	 * @param resource the local resource
	 * @return <code>true</code> if the variant associated with the given local
	 * resource has been cached.
	 * @throws TeamException
	 */
	public abstract boolean isVariantKnown(IResource resource) throws TeamException;

	/**
	 * This method indicates whether the remote bytes are a later revision or version
	 * on the same line-of-descent as the base. A line of descent may be a branch or a fork
	 * (depending on the terminology used by the versioing server). If this method returns
	 * <code>false</code> then the remote bytes will be ignored by this tree.
	 * @param resource the local resource
	 * @param baseBytes the base bytes for the local resoource
	 * @param remoteBytes the remote bytes for the local resoource
	 * @return whether the remote bytes are later on the same line-of-descent as the base bytes
	 */
	protected abstract boolean isDescendant(IResource resource, byte[] baseBytes, byte[] remoteBytes) throws TeamException;

	@Override
	public boolean deleteBytes(IResource resource) throws TeamException {
		return remoteStore.deleteBytes(resource);
	}

	/**
	 * Return the base tree from which the remote is descendant.
	 * @return Returns the base tree.
	 */
	protected ResourceVariantByteStore getBaseStore() {
		return baseStore;
	}

	/**
	 * Return the remote tree which contains bytes only for the resource variants
	 * that differ from those in the base tree.
	 * @return Returns the remote tree.
	 */
	protected ResourceVariantByteStore getRemoteStore() {
		return remoteStore;
	}

	@Override
	public IResource[] members(IResource resource) throws TeamException {
		IResource[] remoteMembers = getRemoteStore().members(resource);
		IResource[] baseMembers = getBaseStore().members(resource);
		Set<IResource> members = new HashSet<>();
		Collections.addAll(members, remoteMembers);
		for (IResource member : baseMembers) {
			// Add the base only if the remote does not know about it
			// (i.e. hasn't marked it as deleted
			if (!isVariantKnown(member)) {
				members.add(member);
			}
		}
		return members.toArray(new IResource[members.size()]);
	}

	@Override
	public void run(IResource root, IWorkspaceRunnable runnable, IProgressMonitor monitor) throws TeamException {
		remoteStore.run(root, runnable, monitor);
	}
}
