/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.core.variants;

import java.util.*;

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
	private Map membersCache = new HashMap();

	private Map syncBytesCache = new HashMap();

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.variants.ResourceVariantByteStore#deleteBytes(org.eclipse.core.resources.IResource)
	 */
	public boolean deleteBytes(IResource resource) throws TeamException {
		return flushBytes(resource, IResource.DEPTH_ZERO);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.variants.ResourceVariantByteStore#dispose()
	 */
	public void dispose() {
		syncBytesCache.clear();
		membersCache.clear();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.variants.ResourceVariantByteStore#flushBytes(org.eclipse.core.resources.IResource, int)
	 */
	public boolean flushBytes(IResource resource, int depth) throws TeamException {
		if (getSyncBytesCache().containsKey(resource)) {
			if (depth != IResource.DEPTH_ZERO) {
				IResource[] members = members(resource);
				for (int i = 0; i < members.length; i++) {
					IResource child = members[i];
					flushBytes(child, (depth == IResource.DEPTH_INFINITE) ? IResource.DEPTH_INFINITE: IResource.DEPTH_ZERO);
				}
			}
			getSyncBytesCache().remove(resource);
			internalRemoveFromParent(resource);
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.variants.ResourceVariantByteStore#getBytes(org.eclipse.core.resources.IResource)
	 */
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

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.variants.ResourceVariantByteStore#members(org.eclipse.core.resources.IResource)
	 */
	public IResource[] members(IResource resource) {
		List members = (List)membersCache.get(resource);
		if (members == null) {
			return new IResource[0];
		}
		return (IResource[]) members.toArray(new IResource[members.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.variants.ResourceVariantByteStore#setBytes(org.eclipse.core.resources.IResource, byte[])
	 */
	public boolean setBytes(IResource resource, byte[] bytes) throws TeamException {
		Assert.isNotNull(bytes);
		byte[] oldBytes = internalGetSyncBytes(resource);
		if (oldBytes != null && equals(oldBytes, bytes)) return false;
		internalSetSyncInfo(resource, bytes);
		return true;
	}

	private Map getSyncBytesCache() {
		return syncBytesCache;
	}

	private void internalAddToParent(IResource resource) {
		IContainer parent = resource.getParent();
		if (parent == null) return;
		List members = (List)membersCache.get(parent);
		if (members == null) {
			members = new ArrayList();
			membersCache.put(parent, members);
		}
		members.add(resource);
	}

	private byte[] internalGetSyncBytes(IResource resource) {
		return (byte[])getSyncBytesCache().get(resource);
	}

	private void internalRemoveFromParent(IResource resource) {
		IContainer parent = resource.getParent();
		List members = (List)membersCache.get(parent);
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
