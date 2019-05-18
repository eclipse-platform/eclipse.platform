/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;

/**
 * The low level cache provides the sync info as bytes
 */
/*package*/ abstract class SyncInfoCache {

	// the resources plugin synchronizer is used to cache and possibly persist. These
	// are keys for storing the sync info.
	/*package*/ static final QualifiedName FOLDER_SYNC_KEY = new QualifiedName(CVSProviderPlugin.ID, "folder-sync"); //$NON-NLS-1$
	/*package*/ static final QualifiedName RESOURCE_SYNC_KEY = new QualifiedName(CVSProviderPlugin.ID, "resource-sync"); //$NON-NLS-1$
	/*package*/ static final QualifiedName IGNORE_SYNC_KEY = new QualifiedName(CVSProviderPlugin.ID, "folder-ignore"); //$NON-NLS-1$

	/*package*/ static final byte[][] EMPTY_RESOURCE_SYNC_INFOS = new byte[0][0];
	
	/*package*/ static final QualifiedName IS_DIRTY = new QualifiedName(CVSProviderPlugin.ID, "is-dirty"); //$NON-NLS-1$
	/*package*/ static final String IS_DIRTY_INDICATOR = "d"; //$NON-NLS-1$
	/*package*/ static final String NOT_DIRTY_INDICATOR = "c"; //$NON-NLS-1$
	/*package*/ static final String RECOMPUTE_INDICATOR = "r"; //$NON-NLS-1$
	
	/*package*/ static final IStatus STATUS_OK = new Status(IStatus.OK, CVSProviderPlugin.ID, 0, CVSMessages.ok, null); 
	
	/**
	 * Returns the folder sync info for the container; null if none.
	 * Folder must exist and must not be the workspace root.
	 * The folder sync info for the container MUST ALREADY BE CACHED.
	 * <p>
	 * The <code>canModifyWorkspace</code>
	 * flag is used to indicate whether it is OK to modify ISycnrhonizer entries for
	 * the given resource. A value of <code>true</code> indicates that the client
	 * holds a scheduling rule that encompasses the resource and the workspace is
	 * open for modification.
	 * @param container the container
	 * @param threadSafeAccess if false, the return value can only be used if not null
	 * @param canModifyWorkspace indicates if it is OK to modify the ISycnrhonizer
	 *
	 * @return the folder sync info for the folder, or null if none.
	 * @see #cacheFolderSync
	 */
	/*package*/ abstract FolderSyncInfo getCachedFolderSync(IContainer container, boolean threadSafeAccess) throws CVSException;

	/**
	 * Sets the folder sync info for the container; if null, deletes it.
	 * Folder must exist and must not be the workspace root.
	 * The folder sync info for the container need not have previously been
	 * cached. The <code>canModifyWorkspace</code>
	 * flag is used to indicate whether it is OK to modify ISycnrhonizer entries for
	 * the given resource. A value of <code>true</code> indicates that the client
	 * holds a scheduling rule that encompasses the resource and the workspace is
	 * open for modification.
	 *
	 * @param container the container
	 * @param info the new folder sync info
	 * @param canModifyWorkspace indicates if it is OK to modify the ISycnrhonizer
	 */
	/*package*/ abstract void setCachedFolderSync(IContainer container, FolderSyncInfo info, boolean canModifyWorkspace) throws CVSException;

	/**
	 * Returns the resource sync info for the given resource. The resource sync
	 * info for the resource MUST ALREADY BE CACHED.
	 * @param resource the resource
	 * @param threadSafeAccess if false, the return value can only be used if not null
	 *
	 * @return the bytes containing the resource's sync info
	 * @see #cacheResourceSyncForChildren
	 */
	/*package*/ abstract byte[] getCachedSyncBytes(IResource resource, boolean threadSafeAccess) throws CVSException;

	/**
	 * Sets the resource sync info for the resource; if null, deletes it. Parent
	 * must exist and must not be the workspace root. The resource sync info for
	 * the resource MUST ALREADY BE CACHED. The <code>canModifyWorkspace</code>
	 * flag is used to indicate whether it is OK to modify ISycnrhonizer entries for
	 * the given resource. A value of <code>true</code> indicates that the client
	 * holds a scheduling rule that encompasses the resource and the workspace is
	 * open for modification.
	 *
	 * @param resource the resource
	 * @param syncBytes the bytes containing the new resource sync info
	 * @param canModifyWorkspace indicates if it is OK to modify the ISycnrhonizer
	 * @see #cacheResourceSyncForChildren
	 */
	/*package*/ abstract void setCachedSyncBytes(IResource resource, byte[] syncBytes, boolean canModifyWorkspace) throws CVSException;
	
	/*package*/ abstract String getDirtyIndicator(IResource resource, boolean threadSafeAccess) throws CVSException;
	
	/*package*/ abstract void setDirtyIndicator(IResource resource, String indicator) throws CVSException;
	
	/*package*/ abstract void flushDirtyCache(IResource resource) throws CVSException;
	
	/*package*/ abstract boolean isSyncInfoLoaded(IContainer parent) throws CVSException;
	
	/**
	 * Query the low level cache to see if the sync info for the provided
	 * container is loaded.
	 * 
	 * @param container
	 * @return boolean
	 * @throws CVSException
	 */
	/*package*/ abstract boolean isFolderSyncInfoCached(IContainer container) throws CVSException;
	
	/**
	 * Query the low level cache to see if the sync info for the direct children
	 * of the provided container is loaded.
	 * 
	 * @param container
	 * @return boolean
	 */
	/*package*/ abstract boolean isResourceSyncInfoCached(IContainer container) throws CVSException;
	
	/**
	 * Indicate to the low level cache that the sync info for all it's direct
	 * children have been set so they match what is on disk.
	 * 
	 * @param container
	 */
	/*package*/ abstract void setResourceSyncInfoCached(IContainer container) throws CVSException;

	/**
	 * Return whether the cache also caches dirty state or recomputes it
	 * each time it is requested.
	 */
	public abstract boolean cachesDirtyState();
}
