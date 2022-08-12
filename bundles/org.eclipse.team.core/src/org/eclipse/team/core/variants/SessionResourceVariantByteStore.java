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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.team.core.TeamException;

/**
 * A <code>ResourceVariantByteStore</code> that caches the variant bytes in
 * memory and does not persist them over workbench invocations.
 *
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public class SessionResourceVariantByteStore extends ResourceVariantByteStore {

	private static final byte[] NO_REMOTE = new byte[0];
	private Map<IResource, List<IResource>> membersCache = new HashMap<>();

	private Map<IResource, byte[]> syncBytesCache = new HashMap<>();

	@Override
	public boolean deleteBytes(IResource resource) throws TeamException {
		return flushBytes(resource, IResource.DEPTH_ZERO);
	}

	@Override
	public void dispose() {
		syncBytesCache.clear();
		membersCache.clear();
	}

	@Override
	public boolean flushBytes(IResource resource, int depth) throws TeamException {
		if (getSyncBytesCache().containsKey(resource)) {
			if (depth != IResource.DEPTH_ZERO) {
				IResource[] members = members(resource);
				for (IResource child : members) {
					flushBytes(child, (depth == IResource.DEPTH_INFINITE) ? IResource.DEPTH_INFINITE: IResource.DEPTH_ZERO);
				}
			}
			getSyncBytesCache().remove(resource);
			internalRemoveFromParent(resource);
			return true;
		}
		return false;
	}

	@Override
	public byte[] getBytes(IResource resource) throws TeamException {
		byte[] syncBytes = internalGetSyncBytes(resource);
		if (syncBytes != null && equals(syncBytes, NO_REMOTE)) {
			// If it is known that there is no remote, return null
			return null;
		}
		return syncBytes;
	}

	/**
	 * Return <code>true</code> if no bytes are contained in this tree.
	 * @return <code>true</code> if no bytes are contained in this tree.
	 */
	public boolean isEmpty() {
		return syncBytesCache.isEmpty();
	}

	@Override
	public IResource[] members(IResource resource) {
		List<IResource> members = membersCache.get(resource);
		if (members == null) {
			return new IResource[0];
		}
		return members.toArray(new IResource[members.size()]);
	}

	@Override
	public boolean setBytes(IResource resource, byte[] bytes) throws TeamException {
		Assert.isNotNull(bytes);
		byte[] oldBytes = internalGetSyncBytes(resource);
		if (oldBytes != null && equals(oldBytes, bytes)) return false;
		internalSetSyncInfo(resource, bytes);
		return true;
	}

	private Map<IResource, byte[]> getSyncBytesCache() {
		return syncBytesCache;
	}

	private void internalAddToParent(IResource resource) {
		IContainer parent = resource.getParent();
		if (parent == null) return;
		List<IResource> members = membersCache.get(parent);
		if (members == null) {
			members = new ArrayList<>();
			membersCache.put(parent, members);
		}
		members.add(resource);
	}

	private byte[] internalGetSyncBytes(IResource resource) {
		return getSyncBytesCache().get(resource);
	}

	private void internalRemoveFromParent(IResource resource) {
		IContainer parent = resource.getParent();
		List members = membersCache.get(parent);
		if (members != null) {
			members.remove(resource);
			if (members.isEmpty()) {
				membersCache.remove(parent);
			}
		}
	}

	private void internalSetSyncInfo(IResource resource, byte[] bytes) {
		getSyncBytesCache().put(resource, bytes);
		internalAddToParent(resource);
	}
}
