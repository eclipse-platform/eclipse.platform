/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.core.resources;

import java.util.*;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.ISynchronizer;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.core.util.Util;

/**
 * This cache uses session properties to hold the bytes representing the sync
 * info
 */
/*package*/ class SynchronizerSyncInfoCache extends SyncInfoCache {
	
	// Map of sync bytes that were set without a scheduling rule
	Map<IResource, Object> pendingCacheWrites = new HashMap<>();
	private static final Object BYTES_REMOVED = new byte[0];

	public SynchronizerSyncInfoCache() {
		getWorkspaceSynchronizer().add(FOLDER_SYNC_KEY);
		getWorkspaceSynchronizer().add(RESOURCE_SYNC_KEY);
	}
	/**
	 * Return the Eclipse Workspace Synchronizer (from org.eclipse.core.resources)
	 */
	private ISynchronizer getWorkspaceSynchronizer() {
		return ResourcesPlugin.getWorkspace().getSynchronizer();
	}
	
	/*package*/ void flush(IProject project) throws CVSException {
		purgeCache(project, true);
	}
	
	/**
	 * Method flush.
	 * @param folder
	 */
	/*package*/ void flush(IFolder folder) throws CVSException {
		purgeCache(folder, false);
	}
	
	/**
	 * Returns the folder sync info for the container; null if none.
	 * Folder must exist and must not be the workspace root.
	 * The folder sync info for the container MUST ALREADY BE CACHED.
	 * @param container the container
	 *
	 * @return the folder sync info for the folder, or null if none.
	 * @see #cacheFolderSync
	 */
	FolderSyncInfo getCachedFolderSync(IContainer container, boolean threadSafeAccess) throws CVSException {
		byte[] bytes = internalGetCachedSyncBytes(container);
		if (bytes == null) return null;
		return FolderSyncInfo.getFolderSyncInfo(bytes);
	}
	
	boolean hasCachedFolderSync(IContainer container) throws CVSException {
		return internalGetCachedSyncBytes(container) != null;
	}
	
	/*
	 * Retieve the cached sync bytes from the synchronizer. A null
	 * is returned if there are no cached sync bytes.
	 */
	private byte[] internalGetCachedSyncBytes(IContainer container) throws CVSException {
		try {
			return getWorkspaceSynchronizer().getSyncInfo(FOLDER_SYNC_KEY, container);
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}
	/**
	 * Sets the folder sync info for the container; if null, deletes it.
	 * Folder must exist and must not be the workspace root.
	 * The folder sync info for the container need not have previously been
	 * cached.
	 *
	 * @param container the container
	 * @param info the new folder sync info
	 */
	void setCachedFolderSync(IContainer container, FolderSyncInfo info, boolean canModifyWorkspace) throws CVSException {
		try {
			if (info == null) {
				if (container.exists() || container.isPhantom()) {
					getWorkspaceSynchronizer().flushSyncInfo(FOLDER_SYNC_KEY, container, IResource.DEPTH_ZERO);
				}
			} else {
				getWorkspaceSynchronizer().setSyncInfo(FOLDER_SYNC_KEY, container, info.getBytes());
			}
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}

	byte[] getCachedSyncBytes(IResource resource, boolean threadSafeAccess) throws CVSException {
		try {
			byte[] bytes = null;
			if (!hasPendingCacheRemoval(resource)) {
				bytes = getPendingCacheWrite(resource);
				if (bytes == null) {
					bytes = getWorkspaceSynchronizer().getSyncInfo(RESOURCE_SYNC_KEY, resource);
				}
			}
			if (bytes != null && resource.getType() == IResource.FILE) {
				if (ResourceSyncInfo.isAddition(bytes)) {
					// The local file has been deleted but was an addition
					// Therefore, ignore the sync bytes
					bytes = null;
				} else if (!ResourceSyncInfo.isDeletion(bytes)) {
					// Ensure the bytes indicate an outgoing deletion
					bytes = ResourceSyncInfo.convertToDeletion(bytes);
				}
			}
			return bytes;
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}
	
	void setCachedSyncBytes(IResource resource, byte[] syncBytes, boolean canModifyWorkspace) throws CVSException {
		byte[] oldBytes = getCachedSyncBytes(resource, true);
		try {
			if (syncBytes == null) {
				if (oldBytes != null) {
					if (canModifyWorkspace) {
						if (resource.exists() || resource.isPhantom()) {
							getWorkspaceSynchronizer().flushSyncInfo(RESOURCE_SYNC_KEY, resource, IResource.DEPTH_ZERO);
						}
						removePendingCacheWrite(resource);
					} else {
						if (resource.exists() || resource.isPhantom()) {
							setPendingCacheWriteToDelete(resource);
						}
					}
				}
			} else {
				// ensure that the sync info is not already set to the same thing.
				// We do this to avoid causing a resource delta when the sync info is 
				// initially loaded (i.e. the synchronizer has it and so does the Entries file
				// Ignore the 
				if (oldBytes == null || !equals(syncBytes, oldBytes)) {
					if (canModifyWorkspace) {
						getWorkspaceSynchronizer().setSyncInfo(RESOURCE_SYNC_KEY, resource, syncBytes);
						removePendingCacheWrite(resource);
					} else {
						setPendingCacheWrite(resource, syncBytes);
					}
				}
			}
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}

	/*
	 * Convert file sync bytes to deletions to ensure proper comparison
	 */
	private boolean equals(byte[] syncBytes, byte[] oldBytes) throws CVSException {
		if (!ResourceSyncInfo.isFolder(syncBytes)) {
			syncBytes = ResourceSyncInfo.convertToDeletion(syncBytes);
		}
		if (!ResourceSyncInfo.isFolder(oldBytes)) {
			try {
				oldBytes = ResourceSyncInfo.convertToDeletion(oldBytes);
			} catch (CVSException e) {
				CVSProviderPlugin.log(e);
				return false;
			}
		}
		return Util.equals(syncBytes, oldBytes);
	}
	
	@Override
	String getDirtyIndicator(IResource resource, boolean threadSafeAccess) throws CVSException {		
		if (resource.getType() == IResource.FILE) {
			// a phantom file is dirty if it was managed before it was deleted			 
			return getCachedSyncBytes(resource, threadSafeAccess) != null ? 
							IS_DIRTY_INDICATOR : 
							NOT_DIRTY_INDICATOR;
		} else {
			return calculateDirtyCountForPhantomFolder((IContainer)resource);
		}
	}

	@Override
	void setDirtyIndicator(IResource resource, String indicator) throws CVSException {
		// We don't cache the dirty count for folders because it would cause
		// resource delta's in the decorator thread and possible deadlock.
	}
	
	@Override
	public boolean cachesDirtyState() {
		// We don't cache the dirty count for folders because it would cause
		// resource delta's in the decorator thread and possible deadlock.
		return false;
	}
		
	/*package*/ void flushDirtyCache(IResource container) throws CVSException {
		// Dirty state is not cached
	}
	
	/*package*/ boolean isSyncInfoLoaded(IContainer parent) throws CVSException {
		return true;
	}
	
	boolean isResourceSyncInfoCached(IContainer container) throws CVSException {
		// the sync info is always cahced when using the synchronizer
		return true;
	}
	
	void setResourceSyncInfoCached(IContainer container) throws CVSException {
		// do nothing
	}
	
	boolean isFolderSyncInfoCached(IContainer container) throws CVSException {
		return true;
	}
	
	/*
	 * Calculate the dirty count for the given phantom folder, performing any
	 * necessary calculations on the childen as well
	 */
	private String calculateDirtyCountForPhantomFolder(IContainer parent) throws CVSException {
		ICVSFolder cvsFolder = CVSWorkspaceRoot.getCVSFolderFor(parent);
		if(getCachedFolderSync(parent, true) == null) {
			return NOT_DIRTY_INDICATOR;
		}
		
		String indicator = NOT_DIRTY_INDICATOR;
		ICVSResource[] children = cvsFolder.members(ICVSFolder.MANAGED_MEMBERS | ICVSFolder.PHANTOM_MEMBERS);
		for (ICVSResource resource : children) {
			// keep looking into phantom folders until a managed phantom file 
			// is found.
			if (resource.isFolder()) {
				indicator = calculateDirtyCountForPhantomFolder((IContainer)resource.getIResource());
			} else {
				// Any non-existant managed files are dirty (outgoing deletion)
				indicator = IS_DIRTY_INDICATOR;
				break;
			}
		}
		return indicator;
	}
	
	/**
	 * @param root
	 * @param deep
	 */
	public void purgeCache(IContainer root, boolean deep) throws CVSException {
		int depth = deep ? IResource.DEPTH_INFINITE : IResource.DEPTH_ZERO;
		try {
			if (root.exists() || root.isPhantom()) {
				getWorkspaceSynchronizer().flushSyncInfo(RESOURCE_SYNC_KEY, root, depth);
			}
			if (root.exists() || root.isPhantom()) {
				getWorkspaceSynchronizer().flushSyncInfo(FOLDER_SYNC_KEY, root, depth);
			}
			if (deep) {
				removePendingCacheWritesUnder(root);
			} else {
				removePendingCacheWrite(root);
			}
		} catch (CoreException e) {
			if (e.getStatus().getCode() == IResourceStatus.RESOURCE_NOT_FOUND) {
				// Must have been deleted since we checked
				return;
			}
			throw CVSException.wrapException(e);
		}
		
	}
	
	public boolean isPhantom(IResource resource) {
		return resource.isPhantom() || hasPendingCacheWrite(resource);
	}
	
	public IResource[] members(IContainer folder) throws CoreException {
		IResource[] pendingWrites = getPendingCacheWrites();
		if (pendingWrites != null){
			HashSet<IResource> cachedResources = new HashSet<>();
			for (IResource resource : pendingWrites) {
				if (resource.getParent().equals(folder))
					cachedResources.add(resource);
			}
			
			if (!cachedResources.isEmpty()){
				IResource[] resources = folder.members(true);
				IResource[] cachedResourcesArray = cachedResources.toArray(new IResource[cachedResources.size()]);
				IResource[]finalResources = new IResource[resources.length + cachedResourcesArray.length];
				System.arraycopy(resources, 0, finalResources, 0, resources.length);
				System.arraycopy(cachedResourcesArray, 0, finalResources, resources.length, cachedResourcesArray.length);
				return finalResources;
			}
		}
		try {
			return folder.members(true);
		} catch (CoreException e) {
			if (e.getStatus().getCode() == IResourceStatus.RESOURCE_NOT_FOUND)
				return new IResource[0];
			throw e;
		}
	}

	/**
	 * Return whether the given resource has a pending cache write
	 * @param resource the resource
	 * @return whether the given resource has a pending cache write
	 */
	private boolean hasPendingCacheWrite(IResource resource) {
		synchronized (pendingCacheWrites) {
			return pendingCacheWrites.containsKey(resource);
		}
	}
	
	private byte[] getPendingCacheWrite(IResource resource) {
		synchronized (pendingCacheWrites) {
			Object object = pendingCacheWrites.get(resource);
			if (object instanceof byte[]) {
				return (byte[])object;
			}
			return null;
		}
	}
	
	private boolean hasPendingCacheRemoval(IResource resource) {
		synchronized (pendingCacheWrites) {
			Object object = pendingCacheWrites.get(resource);
			return object == BYTES_REMOVED;
		}
	}
	
	private void setPendingCacheWrite(IResource resource, byte[] syncBytes) {
		synchronized (pendingCacheWrites) {
			pendingCacheWrites.put(resource, syncBytes);
		}
	}
	
	private void setPendingCacheWriteToDelete(IResource resource) {
		synchronized (pendingCacheWrites) {
			pendingCacheWrites.put(resource, BYTES_REMOVED);
		}
	}
	
	private void removePendingCacheWrite(IResource resource) {
		synchronized (pendingCacheWrites) {
			pendingCacheWrites.remove(resource);
		}
	}
	
	private void removePendingCacheWritesUnder(IContainer root) {
		synchronized (pendingCacheWrites) {
			IPath fullPath = root.getFullPath();
			for (Iterator iter = pendingCacheWrites.keySet().iterator(); iter.hasNext();) {
				IResource resource = (IResource) iter.next();
				if (fullPath.isPrefixOf(resource.getFullPath())) {
					iter.remove();
				}
			}
		}
	}
	
	/**
	 * Return the resources with pending cache writes or
	 * <code>null</code> if there aren't any.
	 * @return the resources with pending cache writes or
	 * <code>null</code>
	 */
	private IResource[] getPendingCacheWrites() {
		synchronized (pendingCacheWrites) {
			if (pendingCacheWrites.isEmpty())
				return null;
			return pendingCacheWrites.keySet().toArray(new IResource[pendingCacheWrites.size()]);
		}
	}
}
