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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.core.util.FileNameMatcher;
import org.eclipse.team.internal.ccvs.core.util.SyncFileWriter;

/**
 * This cache uses session properties to hold the bytes representing the sync
 * info. In addition when the workbench closes or a project is closed, the dirty 
 * state for all cvs managed folders are persisted using the resource's plugin
 * synchronizer.
 */
/*package*/ class SessionPropertySyncInfoCache extends SyncInfoCache implements ISaveParticipant {
	
	// key used on a folder to indicate that the resource sync has been cahced for it's children
	private static final QualifiedName RESOURCE_SYNC_CACHED_KEY = new QualifiedName(CVSProviderPlugin.ID, "resource-sync-cached"); //$NON-NLS-1$
	private static final Object RESOURCE_SYNC_CACHED = new Object();
	
	/*package*/ static final FileNameMatcher NULL_IGNORES = new FileNameMatcher();
	private static final FolderSyncInfo NULL_FOLDER_SYNC_INFO = new FolderSyncInfo("dummy-repo", "dummy-root", null, false); //$NON-NLS-1$ //$NON-NLS-2$
	
	private QualifiedName FOLDER_DIRTY_STATE_KEY = new QualifiedName(CVSProviderPlugin.ID, "folder-dirty-state-cached"); //$NON-NLS-1$
	
	// defer to the sychronizer if there is no sync info
	// (i.e. for those cases where a deleted resource is recreated)
	private SynchronizerSyncInfoCache synchronizerCache;
	
	/*package*/ SessionPropertySyncInfoCache(SynchronizerSyncInfoCache synchronizerCache) {
		this.synchronizerCache = synchronizerCache;
		try {
			// this save participant is removed when the plugin is shutdown.			
			ResourcesPlugin.getWorkspace().addSaveParticipant(CVSProviderPlugin.getPlugin(), this);
			ISynchronizer synchronizer = ResourcesPlugin.getWorkspace().getSynchronizer();
			synchronizer.add(FOLDER_DIRTY_STATE_KEY);
		} catch (CoreException e) {
			CVSProviderPlugin.log(e);
		}
	}

	/**
	 * If not already cached, loads and caches the folder ignores sync for the container.
	 * Folder must exist and must not be the workspace root.
	 *
	 * @param container the container
	 * @return the folder ignore patterns, or an empty array if none
	 */
	/*package*/ FileNameMatcher getFolderIgnores(IContainer container, boolean threadSafeAccess) throws CVSException {
		// don't try to load if the information is already cached
		FileNameMatcher matcher = (FileNameMatcher)safeGetSessionProperty(container, IGNORE_SYNC_KEY);
		if (threadSafeAccess && matcher == null) {
			// read folder ignores and remember it
			String[] ignores = SyncFileWriter.readCVSIgnoreEntries(container);
			if (ignores == null) {
				matcher = NULL_IGNORES;
			} else {
				matcher = new FileNameMatcher(ignores);
			}
			safeSetSessionProperty(container, IGNORE_SYNC_KEY, matcher);
		}
		return matcher;
	}
	
	/* package */ boolean isIgnoresCached(IContainer container) throws CVSException {
		return safeGetSessionProperty(container, IGNORE_SYNC_KEY) != null;
	}

	/*package*/ boolean isFolderSyncInfoCached(IContainer container) throws CVSException {
		Object info = safeGetSessionProperty(container, FOLDER_SYNC_KEY);
		if (info == null){
			// Defer to the synchronizer in case the folder was recreated
			info = synchronizerCache.getCachedFolderSync(container, true);
		}
		return info != null;
	}

	/*package*/ boolean isResourceSyncInfoCached(IContainer container) throws CVSException {
		return safeGetSessionProperty(container, RESOURCE_SYNC_CACHED_KEY) != null;
	}
	
	/*package*/ void setResourceSyncInfoCached(IContainer container) throws CVSException {
		safeSetSessionProperty(container, RESOURCE_SYNC_CACHED_KEY, RESOURCE_SYNC_CACHED);
	}

	/**
	 * Returns the folder sync info for the container; null if none.
	 * Folder must exist and must not be the workspace root.
	 * The folder sync info for the container MUST ALREADY BE CACHED.
	 * @param container the container
	 * @param threadSafeAccess if false, the return value can only be used if not null
	 * @return the folder sync info for the folder, or null if none.
	 * @see #cacheFolderSync
	 */
	FolderSyncInfo getCachedFolderSync(IContainer container, boolean threadSafeAccess) throws CVSException {
		FolderSyncInfo info = (FolderSyncInfo)safeGetSessionProperty(container, FOLDER_SYNC_KEY);
		// If we are not thread safe, just return whatever was found in the session property
		if (!threadSafeAccess)
			return info == NULL_FOLDER_SYNC_INFO ? null : info;
		if (info == null) {
			// Defer to the synchronizer in case the folder was recreated
			info = synchronizerCache.getCachedFolderSync(container, true);
			if (info != null) {
				safeSetSessionProperty(container, FOLDER_SYNC_KEY, info);
			}
		}
		if (info == null) {
			// There should be sync info but it was missing. Report the error.
			// Only report the error is the folder is not derived (see bug 97023)
			if (container.exists() && !container.isDerived()){
				IStatus status = new CVSStatus(IStatus.ERROR, CVSStatus.ERROR, NLS.bind(CVSMessages.EclipseSynchronizer_folderSyncInfoMissing, new String[] { container.getFullPath().toString() }), container);
				throw new CVSException(status); 
			}
		}
		if (info == NULL_FOLDER_SYNC_INFO) return null;
		return info;
	}

	/**
	 * Purges the cache recursively for all resources beneath the container.
	 * There must not be any pending uncommitted changes.
	 * @return the resources whose sync info was flushed
	 */
	/*package*/ IResource[] purgeCache(IContainer container, boolean deep) throws CVSException {
		if (! container.exists()) return new IResource[0];
		try {
			Set<IResource> flushed = new HashSet<>();
			if (container.getType() != IResource.ROOT) {
				safeSetSessionProperty(container, IGNORE_SYNC_KEY, null);
				safeSetSessionProperty(container, FOLDER_SYNC_KEY, null);
				safeSetSessionProperty(container, RESOURCE_SYNC_CACHED_KEY, null);
				flushed.add(container);
				EclipseSynchronizer.getInstance().adjustDirtyStateRecursively(container, RECOMPUTE_INDICATOR);
			}
			IResource[] members = container.members();
			for (IResource resource : members) {
				purgeResourceSyncCache(resource);
				flushed.add(resource);
				if (deep && resource.getType() != IResource.FILE) {
					IResource[] flushedChildren = purgeCache((IContainer) resource, deep);
					flushed.addAll(Arrays.asList(flushedChildren));
				}
			}
			return flushed.toArray(new IResource[flushed.size()]);
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}
	
	/* package*/ void purgeResourceSyncCache(IResource resource) throws CVSException {
		safeSetSessionProperty(resource, RESOURCE_SYNC_KEY, null);
		EclipseSynchronizer.getInstance().adjustDirtyStateRecursively(resource, RECOMPUTE_INDICATOR);
	}
	
	/**
	 * Sets the array of folder ignore patterns for the container, must not be null.
	 * Folder must exist and must not be the workspace root.
	 *
	 * @param container the container
	 * @param ignores the array of ignore patterns
	 */
	/*package*/ void setCachedFolderIgnores(IContainer container, String[] ignores) throws CVSException {
		safeSetSessionProperty(container, IGNORE_SYNC_KEY, new FileNameMatcher(ignores));
	}


	/**
	 * Sets the folder sync info for the container; if null, deletes it.
	 * Folder must exist and must not be the workspace root.
	 * The folder sync info for the container need not have previously been cached.
	 *
	 * @param container the container
	 * @param info the new folder sync info
	 */
	void setCachedFolderSync(IContainer container, FolderSyncInfo info, boolean canModifyWorkspace) throws CVSException {
		if (!container.exists()) return;
		if (info == null) {
			info = NULL_FOLDER_SYNC_INFO;
		} 
		safeSetSessionProperty(container, FOLDER_SYNC_KEY, info);
		// Ensure the synchronizer is clear for exiting resources
		if (canModifyWorkspace && synchronizerCache.getCachedFolderSync(container, true) != null) {
			synchronizerCache.setCachedFolderSync(container, null, true);
		}
	}

	/*package*/ void setDirtyIndicator(IResource resource, String indicator) throws CVSException {
		if (resource.getType() == IResource.FILE) {
			internalSetDirtyIndicator((IFile)resource, indicator);
		} else {
			internalSetDirtyIndicator((IContainer)resource, indicator);
		}
	}
	/*package*/ String getDirtyIndicator(IResource resource, boolean threadSafeAccess) throws CVSException {
		if (resource.getType() == IResource.FILE) {
			return internalGetDirtyIndicator((IFile)resource, threadSafeAccess);
		} else {
			return internalGetDirtyIndicator((IContainer)resource, threadSafeAccess);
		}
	}
	
	private void internalSetDirtyIndicator(IFile file, String indicator) throws CVSException {
		safeSetSessionProperty(file, IS_DIRTY, indicator);
	}
	
	private String internalGetDirtyIndicator(IFile file, boolean threadSafeAccess) throws CVSException {
		String di = (String)safeGetSessionProperty(file, IS_DIRTY);
		if(di == null) {
			di = RECOMPUTE_INDICATOR;
		}
		return di;
	}

	private void internalSetDirtyIndicator(IContainer container, String indicator) throws CVSException {
		safeSetSessionProperty(container, IS_DIRTY, indicator);
	}
	
	private String internalGetDirtyIndicator(IContainer container, boolean threadSafeAccess) throws CVSException {
		try {
			String di = (String)safeGetSessionProperty(container, IS_DIRTY);
			
			// if the session property is not available then restore from persisted sync info. At this
			// time the sync info is not flushed because we don't want the workspace to generate
			// a delta.			
			if(di == null) {
				byte [] diBytes = ResourcesPlugin.getWorkspace().getSynchronizer().getSyncInfo(FOLDER_DIRTY_STATE_KEY, container);
				if(diBytes != null && !CVSProviderPlugin.getPlugin().crashOnLastRun()) {
					di = new String(diBytes);
					switch (di) {
					case NOT_DIRTY_INDICATOR:
						di = NOT_DIRTY_INDICATOR;
						break;
					case IS_DIRTY_INDICATOR:
						di = IS_DIRTY_INDICATOR;
						break;
					default:
						di = RECOMPUTE_INDICATOR;
						break;
					}
				} else {
					di = RECOMPUTE_INDICATOR;
				}
				// Only set the session property if we are thread safe
				if (threadSafeAccess) {
					setDirtyIndicator(container, di);
				}
			}
			return di;
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}
		
	/*
	 * Flush dirty cache for the resource
	 */
	/*package*/ void flushDirtyCache(IResource resource) throws CVSException {
		if (resource.exists()) {
			if (resource.getType() == IResource.FILE) {
				safeSetSessionProperty(resource, IS_DIRTY, null);
			} else {
				safeSetSessionProperty(resource, IS_DIRTY, null);
				flushDirtyStateFromDisk((IContainer)resource);
			}
		}
	}
	
	/**
	 * Method isSyncInfoLoaded returns true if all the sync info for the
	 * provided resources is loaded into the internal cache.
	 *
	 * @param resources
	 * @param i
	 * @return boolean
	 */
	/*package*/ boolean isSyncInfoLoaded(IContainer parent) throws CVSException {
		if (parent.getFolder(new Path(SyncFileWriter.CVS_DIRNAME)).exists()) {
			if (safeGetSessionProperty(parent, RESOURCE_SYNC_CACHED_KEY) == null)
				return false;
			if (safeGetSessionProperty(parent, FOLDER_SYNC_KEY) == null)
				return false;
//				if (parent.getSessionProperty(IGNORE_SYNC_KEY) == null)
//					return false;
		}
		return true;
	}
	
	byte[] getCachedSyncBytes(IResource resource, boolean threadSafeAccess) throws CVSException {
		byte[] bytes = (byte[])safeGetSessionProperty(resource, RESOURCE_SYNC_KEY);
		// If we are not thread safe, just return whatever was found in the session property
		if (!threadSafeAccess)
			return bytes;
		if (bytes == null) {
			// Defer to the synchronizer in case the file was recreated
			bytes = synchronizerCache.getCachedSyncBytes(resource, true);
			if (bytes != null) {
				boolean genderChange = false;
				if (resource.getType() == IResource.FILE) {
					if (ResourceSyncInfo.isFolder(bytes)) {
						genderChange = true;
					}
				} else if (!ResourceSyncInfo.isFolder(bytes)) {
					genderChange = true;
				}
				if (genderChange) {
					// Return null if it is a gender change
					bytes = null;
				} else {
					safeSetSessionProperty(resource, RESOURCE_SYNC_KEY, ResourceSyncInfo.convertFromDeletion(bytes));
				}
			}
		}
		return bytes;
	}

	Object safeGetSessionProperty(IResource resource, QualifiedName key) throws CVSException {
		try {
			return resource.getSessionProperty(key);
		} catch (CoreException e) {
			IStatus status = e.getStatus();
			if(status != null) {
				int code = e.getStatus().getCode();
				if(code != IResourceStatus.RESOURCE_NOT_LOCAL ||
					code != IResourceStatus.RESOURCE_NOT_FOUND) {
						// ignore error since a phantom would of been created
						// and we can safely ignore these cases
						return null;
				}
			}
			// some other error we did not expect
			throw CVSException.wrapException(e);
		}
	}
	
	void safeSetSessionProperty(IResource resource, QualifiedName key, Object value) throws CVSException {
			try {
				resource.setSessionProperty(key, value);
			} catch (CoreException e) {
				throw CVSException.wrapException(e);
			}
		}
	
	void setCachedSyncBytes(IResource resource, byte[] syncBytes, boolean canModifyWorkspace) throws CVSException {
		// Ensure that the sync bytes do not indicate a deletion
		if (syncBytes != null && ResourceSyncInfo.isDeletion(syncBytes)) {
			syncBytes = ResourceSyncInfo.convertFromDeletion(syncBytes);
		}
		// Put the sync bytes into the cache
		safeSetSessionProperty(resource, RESOURCE_SYNC_KEY, syncBytes);
		// Ensure the synchronizer is clear
		if (canModifyWorkspace && synchronizerCache.getCachedSyncBytes(resource, true) != null) {
			synchronizerCache.setCachedSyncBytes(resource, null, canModifyWorkspace);
		}
	}
	
	@Override
	public void doneSaving(ISaveContext context) {
	}

	@Override
	public void prepareToSave(ISaveContext context) throws CoreException {
	}

	@Override
	public void rollback(ISaveContext context) {			
	}

	/* Called when the workbench is shutdown or projects are closed. The dirty state
	 * of folders is persisted, using sync info, so that at startup or project open
	 * the folder state can be quickly calculated. This is mainly for improving decorator
	 * performance.
	 * @see org.eclipse.core.resources.ISaveParticipant#saving(org.eclipse.core.resources.ISaveContext)
	 */
	public void saving(ISaveContext context) throws CoreException {
		boolean fullSave = (context.getKind() == ISaveContext.FULL_SAVE);
		boolean projectSave = (context.getKind() == ISaveContext.PROJECT_SAVE);
		
		if((projectSave || fullSave)) {
			// persist all session properties for folders into sync info.
			final ISynchronizer synchronizer = ResourcesPlugin.getWorkspace().getSynchronizer();
		
			// traverse the workspace looking for CVS managed projects or just the 
			// specific projects being closed
			IProject[] projects;
			if(projectSave) {
				projects = new IProject[1];
				projects[0] = context.getProject();
			} else {
				projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
			}
			for (IProject project : projects) {
				RepositoryProvider provider = RepositoryProvider.getProvider(
						project,
						CVSProviderPlugin.getTypeId());

				// found a project managed by CVS, convert each session property on a
				// folder to a sync object.
				if (provider != null) {
					project.accept(new IResourceVisitor() {
						public boolean visit(IResource resource) throws CoreException {
							if(resource.getType() != IResource.FILE) {
								String di = null;
								try {
									di = getDirtyIndicator(resource, true);
								} catch (CVSException e) {
									// continue traversal
									CVSProviderPlugin.log(e);
								}
								if(di != null) {
									synchronizer.setSyncInfo(FOLDER_DIRTY_STATE_KEY, resource, di.getBytes());
								}								
							}
							return true;
						}
					});
				}
			}
		}
	}
		
	/* 
	 * Called to clear the folder dirty state from the resource sync tree and stop persisting
	 * these values to disk.
	 */
	private void flushDirtyStateFromDisk(IContainer container) {
		final ISynchronizer synchronizer = ResourcesPlugin.getWorkspace().getSynchronizer();									
		try {
			synchronizer.flushSyncInfo(FOLDER_DIRTY_STATE_KEY, container, IResource.DEPTH_INFINITE);
		} catch (CoreException e) {
			CVSProviderPlugin.log(e);
		}
	}

	/*
	 * Flush all the cahced dirty state for the resource and its members.
	 */
	/* package*/ void purgeDirtyCache(IResource resource) throws CVSException {
		if (! resource.exists()) return;
		try {
			if (resource.getType() != IResource.ROOT) {
				safeSetSessionProperty(resource, IS_DIRTY, null);
			}
			if (resource.getType() != IResource.FILE) {
				ResourcesPlugin.getWorkspace().getSynchronizer().flushSyncInfo(FOLDER_DIRTY_STATE_KEY, resource, IResource.DEPTH_INFINITE);
				IResource[] members = ((IContainer)resource).members();
				for (IResource member : members) {
					purgeDirtyCache(member);
				}
			}
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}

	@Override
	public boolean cachesDirtyState() {
		return true;
	}
}
