/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
 *     Matt McCutchen <hashproduct+eclipse@gmail.com> - Bug 181546 [Sync Info] Eclipse writes Entries-less metadata in recreated pruned dir
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.core.resources;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.syncinfo.*;
import org.eclipse.team.internal.ccvs.core.syncinfo.ReentrantLock.CVSThreadInfo;
import org.eclipse.team.internal.ccvs.core.util.*;
import org.eclipse.team.internal.core.subscribers.BatchingLock.IFlushOperation;
import org.eclipse.team.internal.core.subscribers.BatchingLock.ThreadInfo;
import org.osgi.framework.Bundle;

/**
 * A synchronizer is responsible for managing synchronization information for local
 * CVS resources.
 * 
 * This class is thread safe but only allows one thread to modify the cache at a time. It
 * doesn't support fine grain locking on a resource basis. Lock ordering between the workspace
 * lock and the synchronizer lock is guaranteed to be deterministic. That is, the workspace
 * lock is *always* acquired before the synchronizer lock. This protects against possible
 * deadlock cases where the synchronizer lock is acquired before a workspace lock.
 * 
 * Special processing has been added for linked folders and their childen so
 * that their CVS meta files are never read or written.
 * 
 * IMPORTANT NOTICE: It is the responsibility of the clients of EclipseSynchronizer
 * to ensure that they have wrapped operations that may modify the workspace in
 * an IWorkspaceRunnable. If this is not done, deltas may fore at inopertune times 
 * and corrupt the sync info. The wrapping could be done within the synchronizer 
 * itself but would require the creation of an inner class for each case that requires
 * it.
 * 
 * @see ResourceSyncInfo
 * @see FolderSyncInfo
 */
public class EclipseSynchronizer implements IFlushOperation {	
	private static final String IS_DIRTY_INDICATOR = SyncInfoCache.IS_DIRTY_INDICATOR;
	private static final String NOT_DIRTY_INDICATOR = SyncInfoCache.NOT_DIRTY_INDICATOR;
	private static final String RECOMPUTE_INDICATOR = SyncInfoCache.RECOMPUTE_INDICATOR; 
		
	// the cvs eclipse synchronizer is a singleton
	private static EclipseSynchronizer instance;
	
	// track resources that have changed in a given operation
	private ILock lock = Job.getJobManager().newLock();
	private ReentrantLock resourceLock = new ReentrantLock();
	
	private SynchronizerSyncInfoCache synchronizerCache = new SynchronizerSyncInfoCache();
	private SessionPropertySyncInfoCache sessionPropertyCache = new SessionPropertySyncInfoCache(synchronizerCache);
	
	/*
	 * Package private constructor to allow specialized subclass for handling folder deletions
	 */
	EclipseSynchronizer() {		
	}
	
	/**
	 * Returns the singleton instance of the synchronizer.
	 */
	public static EclipseSynchronizer getInstance() {		
		if(instance==null) {
			instance = new EclipseSynchronizer();
		}
		return instance;
	}
	
	public SyncInfoCache getSyncInfoCacheFor(IResource resource) {
		if (resource.exists()  && resource.isLocal(IResource.DEPTH_ZERO)) {
			return sessionPropertyCache;
		} else {
			return synchronizerCache;
		}
	}

	private boolean isValid(IResource resource) {
		return resource.exists() || synchronizerCache.isPhantom(resource);
	}
	
	/**
	 * Sets the folder sync info for the specified folder.
	 * The folder must exist and must not be the workspace root.
	 * 
	 * @param folder the folder
	 * @param info the folder sync info, must not be null
	 * @see #getFolderSync
	 * @see #deleteFolderSync
	 */
	public void setFolderSync(IContainer folder, FolderSyncInfo info) throws CVSException {
		Assert.isNotNull(info); // enforce the use of deleteFolderSync
		// ignore folder sync on the root (i.e. CVSROOT/config/TopLevelAdmin=yes but we just ignore it)
		if (folder.getType() == IResource.ROOT) return;
		if (!isValid(folder)) {
			// This means that the folder doesn't exist and is not a phantom
			// Allow the set if the parent is a CVS folder since
			// this can occur when creating phantom folders
			if (getFolderSync(folder.getParent()) == null) {
				IStatus status = new CVSStatus(IStatus.ERROR, TeamException.UNABLE,
						NLS.bind(CVSMessages.EclipseSynchronizer_ErrorSettingFolderSync, new String[] { folder.getFullPath().toString() }),folder);
				throw new CVSException(status); 
			}
		}
		ISchedulingRule rule = null;
		try {
			rule = beginBatching(folder, null);
			try {
				beginOperation();
				// get the old info
				FolderSyncInfo oldInfo = getFolderSync(folder);
				// set folder sync and notify
				getSyncInfoCacheFor(folder).setCachedFolderSync(folder, info, true);
				// if the sync info changed from null, we may need to adjust the ancestors
				if (oldInfo == null) {
					adjustDirtyStateRecursively(folder, RECOMPUTE_INDICATOR);
				}
				folderChanged(folder);
			} finally {
				endOperation();
			}
		} finally {
			if (rule != null) endBatching(rule, null);
		}
	}
	
	/**
	 * Gets the folder sync info for the specified folder.
	 * 
	 * @param folder the folder
	 * @return the folder sync info associated with the folder, or null if none.
	 * @see #setFolderSync
	 * @see #deleteFolderSync
	 */
	public FolderSyncInfo getFolderSync(IContainer folder) throws CVSException {
		if (folder.getType() == IResource.ROOT || !isValid(folder)) return null;
		// Do a check outside the lock for any folder sync info
		FolderSyncInfo info = getSyncInfoCacheFor(folder).getCachedFolderSync(folder, false /* not thread safe */);
		if (info != null)
			return info;
		try {
			beginOperation();
			cacheFolderSync(folder);
			return getSyncInfoCacheFor(folder).getCachedFolderSync(folder, true /* thread safe */);
		} finally {
			endOperation();
		}
	}	

	/**
	 * Deletes the folder sync for the specified folder and the resource sync
	 * for all of its children.  Does not recurse.
	 * 
	 * @param folder the folder
	 * @see #getFolderSync
	 * @see #setFolderSync
	 */
	public void deleteFolderSync(IContainer folder) throws CVSException {
		if (folder.getType() == IResource.ROOT || !isValid(folder)) return;
		ISchedulingRule rule = null;
		try {
			rule = beginBatching(folder, null);
			try {
				beginOperation();
				// iterate over all children with sync info and prepare notifications
				// this is done first since deleting the folder sync may remove a phantom
				cacheResourceSyncForChildren(folder, true /* can modify workspace */);
				IResource[] children = folder.members(true);
				for (IResource resource : children) {
					resourceChanged(resource);
					// delete resource sync for all children
					getSyncInfoCacheFor(resource).setCachedSyncBytes(resource, null, true);
				}
				// delete folder sync
				getSyncInfoCacheFor(folder).setCachedFolderSync(folder, null, true);
				folderChanged(folder);
			} catch (CoreException e) {
				throw CVSException.wrapException(e);
			} finally {
				endOperation();
			}
		} finally {
			if (rule != null) endBatching(rule, null);
		}
	}

	private void folderChanged(IContainer folder) {
		resourceLock.folderChanged(folder);
	}

	private void resourceChanged(IResource resource) {
		resourceLock.resourceChanged(resource);
	}

	/**
	 * Sets the resource sync info for the specified resource.
	 * The parent folder must exist and must not be the workspace root.
	 * 
	 * @param resource the resource
	 * @param info the resource sync info, must not be null
	 * @see #getResourceSync
	 * @see #deleteResourceSync
	 */
	public void setResourceSync(IResource resource, ResourceSyncInfo info) throws CVSException {
		Assert.isNotNull(info); // enforce the use of deleteResourceSync
		IContainer parent = resource.getParent();
		if (parent == null || parent.getType() == IResource.ROOT || !isValid(parent)) {
			IStatus status = new CVSStatus(IStatus.ERROR, TeamException.UNABLE,
				NLS.bind(CVSMessages.EclipseSynchronizer_ErrorSettingResourceSync, new String[] { resource.getFullPath().toString() }), resource);
			throw new CVSException(status); 
		}
		ISchedulingRule rule = null;
		try {
			rule = beginBatching(resource, null);
			try {
				beginOperation();
				// cache resource sync for siblings, set for self, then notify
				cacheResourceSyncForChildren(parent, true /* can modify workspace */);
				setCachedResourceSync(resource, info);
				resourceChanged(resource);		
			} finally {
				endOperation();
			}
		} finally {
			if (rule != null) endBatching(rule, null);
		}
	}
	
	/**
	 * Gets the resource sync info for the specified folder.
	 * 
	 * @param resource the resource
	 * @return the resource sync info associated with the resource, or null if none.
	 * @see #setResourceSync
	 * @see #deleteResourceSync
	 */
	public ResourceSyncInfo getResourceSync(IResource resource) throws CVSException {
		byte[] info = getSyncBytes(resource);
		if (info == null) return null;
		return new ResourceSyncInfo(info);
	}

	/**
	 * Gets the resource sync info for the specified folder.
	 * 
	 * @param resource the resource
	 * @return the resource sync info associated with the resource, or null if none.
	 * @see #setResourceSync
	 * @see #deleteResourceSync
	 */
	public byte[] getSyncBytes(IResource resource) throws CVSException {
		IContainer parent = resource.getParent();
		if (parent == null || parent.getType() == IResource.ROOT || !isValid(parent)) return null;
		// Do a quick check outside the lock to see if there are sync butes for the resource.
		byte[] info = getSyncInfoCacheFor(resource).getCachedSyncBytes(resource, false /* not thread safe */);
		if (info != null)
			return info;
		try {
			beginOperation();
			// cache resource sync for siblings, then return for self
			try {
				cacheResourceSyncForChildren(parent, false /* cannot modify workspace */);
			} catch (CVSException e) {
				if (isCannotModifySynchronizer(e) || isResourceNotFound(e)) {
					// We will resort to loading the sync info for the requested resource from disk
					byte[] bytes =  getSyncBytesFromDisk(resource);
					if (!resource.exists() && bytes != null && !ResourceSyncInfo.isDeletion(bytes)) {
						bytes = ResourceSyncInfo.convertToDeletion(bytes);
					}
					return bytes;
				} else {
					throw e;
				}
			}
			return getCachedSyncBytes(resource);
		} finally {
			endOperation();
		}
	}

	/**
	 * Sets the resource sync info for the specified resource.
	 * The parent folder must exist and must not be the workspace root.
	 * 
	 * @param resource the resource
	 * @param info the resource sync info, must not be null
	 * @see #getResourceSync
	 * @see #deleteResourceSync
	 */
	public void setSyncBytes(IResource resource, byte[] syncBytes) throws CVSException {
		Assert.isNotNull(syncBytes); // enforce the use of deleteResourceSync
		IContainer parent = resource.getParent();
		if (parent == null || parent.getType() == IResource.ROOT || !isValid(parent)) {
			IStatus status = new CVSStatus(IStatus.ERROR, TeamException.UNABLE,
				NLS.bind(CVSMessages.EclipseSynchronizer_ErrorSettingResourceSync, new String[] { resource.getFullPath().toString() }),resource);
			throw new CVSException(status); 
		}
		ISchedulingRule rule = null;
		try {
			rule = beginBatching(resource, null);
			try {
				beginOperation();
				// cache resource sync for siblings, set for self, then notify
				cacheResourceSyncForChildren(parent, true /* can modify workspace */);
				setCachedSyncBytes(resource, syncBytes);
				resourceChanged(resource);		
			} finally {
				endOperation();
			}
		} finally {
			if (rule != null) endBatching(rule, null);
		}
	}
		
	/**
	 * Deletes the resource sync info for the specified resource, if it exists.
	 * 
	 * @param resource the resource
	 * @see #getResourceSync
	 * @see #setResourceSync
	 */
	public void deleteResourceSync(IResource resource) throws CVSException {
		IContainer parent = resource.getParent();
		if (parent == null || parent.getType() == IResource.ROOT || !isValid(parent)) return;
		ISchedulingRule rule = null;
		try {
			rule = beginBatching(resource, null);
			try {
				beginOperation();
				// cache resource sync for siblings, delete for self, then notify
				cacheResourceSyncForChildren(parent, true /* can modify workspace */);
				if (getCachedSyncBytes(resource) != null) { // avoid redundant notifications
					setCachedSyncBytes(resource, null);
					clearDirtyIndicator(resource);
					resourceChanged(resource);
				}
			} finally {
				endOperation();
			}
		} finally {
			if (rule != null) endBatching(rule, null);
		}
	}

	/**
	 * @param resource
	 */
	private void clearDirtyIndicator(IResource resource) throws CVSException {
		getSyncInfoCacheFor(resource).flushDirtyCache(resource);
		adjustDirtyStateRecursively(resource.getParent(), RECOMPUTE_INDICATOR);
	}

	/**
	 * Gets the array of ignore patterns for the specified folder.
	 * 
	 * @param folder the folder
	 * @return the patterns, or an empty array if none
	 * @see #addIgnored
	 */
	public boolean isIgnored(IResource resource) throws CVSException {
		if (resource.getType() == IResource.ROOT || 
			resource.getType() == IResource.PROJECT || 
			! resource.exists()) {
			return false;
		}
		IContainer parent = resource.getParent();
		FileNameMatcher matcher = sessionPropertyCache.getFolderIgnores(parent, false /* not thread safe */);
		if (matcher == null) {
			try {
				beginOperation();
				matcher = cacheFolderIgnores(parent);
			} finally {
				endOperation();
			}
		}
		return matcher.match(resource.getName());
	}
	
	/**
	 * Adds a pattern to the set of ignores for the specified folder.
	 * 
	 * @param folder the folder
	 * @param pattern the pattern
	 */
	public void addIgnored(IContainer folder, String pattern) throws CVSException {
		if (folder.getType() == IResource.ROOT || ! folder.exists()) {
			IStatus status = new CVSStatus(IStatus.ERROR, TeamException.UNABLE,
				NLS.bind(CVSMessages.EclipseSynchronizer_ErrorSettingIgnorePattern, new String[] { folder.getFullPath().toString() }),folder);
			throw new CVSException(status); 
		}
		ISchedulingRule rule = null;
		try {
			rule = beginBatching(folder.getFile(new Path(SyncFileWriter.IGNORE_FILE)), null);
			try {
				beginOperation();
				String[] ignores = SyncFileWriter.readCVSIgnoreEntries(folder);
				if (ignores != null) {
					// verify that the pattern has not already been added
					for (String ignore : ignores) {
						if (ignore.equals(pattern)) {
							return;
						}
					}
					// add the pattern
					String[] oldIgnores = ignores;
					ignores = new String[oldIgnores.length + 1];
					System.arraycopy(oldIgnores, 0, ignores, 0, oldIgnores.length);
					ignores[oldIgnores.length] = pattern;
				} else {
					ignores = new String[] { pattern };
				}
				setCachedFolderIgnores(folder, ignores);
				SyncFileWriter.writeCVSIgnoreEntries(folder, ignores);
				// broadcast changes to unmanaged children - they are the only candidates for being ignored
				List<IResource> possibleIgnores = new ArrayList<>();
				accumulateNonManagedChildren(folder, possibleIgnores);
				ResourceStateChangeListeners.getListener().resourceSyncInfoChanged(possibleIgnores.toArray(new IResource[possibleIgnores.size()]));
			} finally {
				endOperation();
			}
		} finally {
			if (rule != null) endBatching(rule, null);
		}
	}
	
	/**
	 * Returns the members of this folder including deleted resources with sync info,
	 * but excluding special resources such as CVS subdirectories.
	 *
	 * @param folder the container to list
	 * @return the array of members
	 */
	public IResource[] members(IContainer folder) throws CVSException {
		if (! isValid(folder)) return new IResource[0];
		try {				
			beginOperation();
			if (folder.getType() != IResource.ROOT) {
				// ensure that the sync info is cached so any required phantoms are created
				cacheResourceSyncForChildren(folder, false);
			}
		} catch (CVSException e) {
			if (!isCannotModifySynchronizer(e) && !isResourceNotFound(e)) {
				throw e;
			}
		} finally {
			endOperation();
		}
		try {
			
			return synchronizerCache.members(folder);
			
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}
	
	private boolean isCannotModifySynchronizer(CVSException e) {
		// IResourceStatus.WORKSPACE_LOCKED can occur if the resource sync is loaded 
		// during the POST_CHANGE delta phase.
		// CVSStatus.FAILED_TO_CACHE_SYNC_INFO can occur if the resource sync is loaded
		// when no scheduling rule is held.
		return (e.getStatus().getCode() == IResourceStatus.WORKSPACE_LOCKED 
				|| e.getStatus().getCode() == CVSStatus.FAILED_TO_CACHE_SYNC_INFO);
	}
	
	private boolean isResourceNotFound(CVSException e) {
		return e.getStatus().getCode() == IResourceStatus.RESOURCE_NOT_FOUND;
	}
	
	/**
	 * Begins a batch of operations in order to optimize sync file writing. 
	 * The provided scheduling rule indicates the resources
	 * that the resources affected by the operation while the returned scheduling rule
	 * is the rule obtained by the lock. It may differ from the provided rule as it must
	 * encompass any sync files that may change as a result of the operation.
	 */
	public ISchedulingRule beginBatching(ISchedulingRule resourceRule, IProgressMonitor monitor) {
		return resourceLock.acquire(resourceRule, this /* IFlushOperation */, monitor);
	}
	
	/**
	 * Ends a batch of operations. The provided rule must be the one that was returned
	 * by the corresponding call to beginBatching.
	 * <p>
	 * Progress cancellation is ignored while writting the cache to disk. This
	 * is to ensure cache to disk consistency.
	 * </p>
	 * 
	 * @param monitor the progress monitor, may be null
	 * @exception CVSException with a status with code <code>COMMITTING_SYNC_INFO_FAILED</code>
	 * if all the CVS sync information could not be written to disk.
	 */
	public void endBatching(ISchedulingRule rule, IProgressMonitor monitor) throws CVSException {
		try {
			resourceLock.release(rule, monitor);
		} catch (TeamException e) {
			throw CVSException.wrapException(e);
		}
	}
	
	/* 
	 * Callback which is invoked when the batching resource lock is released 
	 * or when a flush is requested (see beginBatching(IResource)).
	 * 
	 * @see org.eclipse.team.internal.ccvs.core.syncinfo.ReentrantLock.IRunnableOnExit#run(org.eclipse.team.internal.ccvs.core.syncinfo.ReentrantLock.ThreadInfo, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void flush(final ThreadInfo info, IProgressMonitor monitor) throws CVSException {
		if (info != null && !info.isEmpty()) {
			try {
				ResourcesPlugin.getWorkspace().run((IWorkspaceRunnable) pm -> {
					IStatus status = commitCache(info, pm);
					if (!status.isOK()) {
						throw new CVSException(status);
					}
				}, null, 0 /* no flags */, monitor);
			} catch (CoreException e) {
				throw CVSException.wrapException(e);
			}
		}
	}
	
	/*
	 * Begin an access to the internal data structures of the synchronizer
	 */
	private void beginOperation() {
		try {
			// Do not try to acquire the lock if the resources tree is locked
			// The reason for this is that during the resource delta phase (i.e. when the tree is locked)
			// the workspace lock is held. If we obtain our lock, there is 
			// a chance of dealock. It is OK if we don't as we are still protected
			// by scheduling rules and the workspace lock.
			if (ResourcesPlugin.getWorkspace().isTreeLocked()) return;
		} catch (RuntimeException e) {
			// If we are not active, throw a cancel. Otherwise, propogate it.
			// (see bug 78303)
			if (Platform.getBundle(CVSProviderPlugin.ID).getState() == Bundle.ACTIVE) {
				throw e;
			} else {
				throw new OperationCanceledException();
			}
		}
		lock.acquire();
	}
	
	/*
	 * End an access to the internal data structures of the synchronizer
	 */
	private void endOperation() {
		try {
			// See beginOperation() for a description of why the lock is not obtained when the tree is locked
			if (ResourcesPlugin.getWorkspace().isTreeLocked()) return;
		} catch (RuntimeException e) {
			// If we are not active, throw a cancel. Otherwise, propogate it.
			// (see bug 78303)
			if (Platform.getBundle(CVSProviderPlugin.ID).getState() == Bundle.ACTIVE) {
				throw e;
			} else {
				throw new OperationCanceledException();
			}
		}
		lock.release();
	}
	
	/**
	 * Flush the sync information from the in-memory cache to disk and purge
	 * the entries from the cache.
	 * <p>
	 * Recursively flushes the sync information for all resources 
	 * below the root to disk and purges the entries from memory
	 * so that the next time it is accessed it will be retrieved from disk.
	 * May flush more sync information than strictly needed, but never less.
	 * </p>
	 * 
	 * @param root the root of the subtree to purge
	 * @param deep purge sync from child folders
	 * @param monitor the progress monitor, may be null
	 */
	public void flush(IContainer root, boolean deep, IProgressMonitor monitor) throws CVSException {
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(null, 10);
		ISchedulingRule rule = null;
		try {
			rule = beginBatching(root, Policy.subMonitorFor(monitor, 1));
			try {
				beginOperation();
				try {
					// Flush changes to disk
					resourceLock.flush(Policy.subMonitorFor(monitor, 8));
				} catch (TeamException e) {
					throw CVSException.wrapException(e);
				} finally {
					// Purge the in-memory cache
					sessionPropertyCache.purgeCache(root, deep);
				}
			} finally {
				endOperation();
			}
		} finally {
			if (rule != null) endBatching(rule, Policy.subMonitorFor(monitor, 1));
			monitor.done();
		}
	}

	public void deconfigure(final IProject project, IProgressMonitor monitor) throws CVSException {
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(null, 100);
		ISchedulingRule rule = null;
		try {
			rule = beginBatching(project, Policy.subMonitorFor(monitor, 10));
			// Flush the sync info
			flush(project, true /* deep */, Policy.subMonitorFor(monitor, 80));
			
			purgeDirtyCache(project, Policy.subMonitorFor(monitor, 5));
				
			// forget about pruned folders however the top level pruned folder will have resource sync (e.g. 
			// a line in the Entry file). As a result the folder is managed but is not a CVS folder.
			synchronizerCache.purgeCache(project, true);
		} finally {
			if (rule != null) endBatching(rule, Policy.subMonitorFor(monitor, 5));
			monitor.done();
		}
	}

	/**
	 * Called to notify the synchronizer that meta files have changed on disk, outside 
	 * of the workbench. The cache will be flushed for this folder and it's immediate
	 * children and appropriate state change events are broadcasts to state change
	 * listeners.
	 */
	public void ignoreFilesChanged(IContainer[] roots) throws CVSException {
		for (IContainer container : roots) {
			if (container.exists()) {
				ISchedulingRule rule = null;
				try {
					Set<IResource> changed = new HashSet<>();
					rule = beginBatching(container, null);
					try {
						beginOperation();
						
						// Record the previous ignore pattterns
						FileNameMatcher oldIgnores = null;
						if (sessionPropertyCache.isFolderSyncInfoCached(container)) {
							oldIgnores = cacheFolderIgnores(container);
						}
						
						// Purge the cached state for direct children of the container
						changed.addAll(Arrays.asList(
							sessionPropertyCache.purgeCache(container, oldIgnores == null /*flush deeply if the old patterns are not known*/)));
						
						// Purge the state for any children of previously ignored containers
						if (oldIgnores != null) {
							FileNameMatcher newIgnores = cacheFolderIgnores(container);
							try {
								IResource[] members = container.members();
								for (IResource resource : members) {
									if (resource.getType() == IResource.FOLDER) {
										String name = resource.getName();
										if (oldIgnores.match(name) && !newIgnores.match(name)) {
											changed.addAll(Arrays.asList(
													sessionPropertyCache.purgeCache((IContainer)resource, true /*flush deeply*/)));
										}
									}
								}
							} catch (CoreException e) {
								// Just log and continue
								CVSProviderPlugin.log(e);
							}
						}
					} finally {
						endOperation();
					}
					if (!changed.isEmpty()) {
						ResourceStateChangeListeners.getListener().resourceSyncInfoChanged(
							changed.toArray(new IResource[changed.size()]));
					}
				} finally {
					if (rule != null) endBatching(rule, null);
				}
			}
		}
	}
	
	public void syncFilesChangedExternally(IContainer[] changedMetaFiles, IFile[] externalDeletions) throws CVSException {
		List<IResource> changed = new ArrayList<>();
		for (IContainer container : changedMetaFiles) {
			if (!isWithinActiveOperationScope(container)) {
				changed.addAll(Arrays.asList(
						sessionPropertyCache.purgeCache(container, false /*don't flush children*/)));
			}
		}
		for (IFile file : externalDeletions) {
			if (!isWithinActiveOperationScope(file)) {
				sessionPropertyCache.purgeCache(file.getParent(), false /*don't flush children*/);
				changed.add(file);
			}
		}
		if (!changed.isEmpty()) {
			ResourceStateChangeListeners.getListener().externalSyncInfoChange(
					changed.toArray(new IResource[changed.size()]));
		}
	}
	
	/*
	 * The resource is about to be deleted by the move delete hook.
	 * In all cases (except when the resource doesn't exist), this method 
	 * will indicate that the dirty state of the parent needs to be recomputed.
	 * For managed resources, it will move the cached sync info from the session
	 * property cache into the synchronizer cache, purging the session cache.
	 * @param resource the resource about to be deleted.
	 * <p>
	 * Note that this method is not recursive. Hence, for managed resources
	 * 
	 * @returns whether children need to be prepared
	 * @throws CVSException
	 */
	/* private */ boolean prepareForDeletion(IResource resource) throws CVSException {
		if (!resource.exists()) return false;
		ISchedulingRule rule = null;
		try {
			rule = beginBatching(resource, null);
			try {
				beginOperation();
				// Flush the dirty info for the resource and it's ancestors.
				// Although we could be smarter, we need to do this because the
				// deletion may fail.
				adjustDirtyStateRecursively(resource, RECOMPUTE_INDICATOR);
				if (resource.getType() == IResource.FILE) {
					byte[] syncBytes = getSyncBytes(resource);
					if (syncBytes != null) {
						if (ResourceSyncInfo.isAddition(syncBytes)) {
							deleteResourceSync(resource);
						} else {
							syncBytes = convertToDeletion(syncBytes);
							synchronizerCache.setCachedSyncBytes(resource, syncBytes, true);
						}
						sessionPropertyCache.purgeResourceSyncCache(resource);
						resourceChanged(resource);
					}
					return false;
				} else {
					IContainer container = (IContainer)resource;
					if (container.getType() == IResource.PROJECT) {
						synchronizerCache.flush((IProject)container);
						return false;
					} else {
						// Move the folder sync info into phantom space
						FolderSyncInfo info = getFolderSync(container);
						if (info == null) return false;
						synchronizerCache.setCachedFolderSync(container, info, true);
						folderChanged(container);
						// move the resource sync as well
						byte[] syncBytes = getSyncBytes(resource);
						synchronizerCache.setCachedSyncBytes(resource, syncBytes, true);
						sessionPropertyCache.purgeResourceSyncCache(container);
						sessionPropertyCache.purgeCache(container, false);
						return true;
					}
				}
			} finally {
				endOperation();
			}
		} finally {
			if (rule != null) endBatching(rule, null);
		}
	}
	
	/**
	 * The resource has been deleted. Make sure any cached state is cleared.
	 * This is needed because the move/delete hook is not invoked in all situations
	 * (e.g. external deletion).
	 * 
	 * @param resource
	 * @throws CVSException
	 */
	protected void handleDeleted(IResource resource) throws CVSException {
		if (resource.exists()) return;
		try {
			beginOperation();
			adjustDirtyStateRecursively(resource, RECOMPUTE_INDICATOR);
		} finally {
			endOperation();
		}
	}
	
	/**
	 * The resource has been added. Make sure any cached state is cleared.
	 * This is needed because the add hook is not invoked in all situations
	 * (e.g. external addition).
	 * 
	 * @param resource
	 * @throws CVSException
	 */
	protected void handleAdded(IResource resource) throws CVSException {
		if (!resource.exists()) return;
		try {
			beginOperation();
			adjustDirtyStateRecursively(resource, RECOMPUTE_INDICATOR);
		} finally {
			endOperation();
		}
	}
	
	/**
	 * Prepare for the deletion of the target resource from within 
	 * the move/delete hook. The method is invoked by both the 
	 * deleteFile/Folder methods and for the source resource
	 * of moveFile/Folder. This method will move the cached sync info
	 * into the phantom (ISynchronizer) cache so that outgoing deletions
	 * and known remote folders are preserved.
	 * 
	 * @param resource
	 * @param monitor
	 * @throws CVSException
	 */
	public void prepareForDeletion(IResource resource, IProgressMonitor monitor) throws CVSException {
		// Move sync info to phantom space for the resource and all it's children
		monitor = Policy.monitorFor(monitor);
		try {
			beginOperation();
			monitor.beginTask(null, 100);
			try {
				resource.accept(new IResourceVisitor() {
					public boolean visit(IResource innerResource) throws CoreException {
						try {
							return prepareForDeletion(innerResource);
						} catch (CVSException e) {
							CVSProviderPlugin.log(e);
							throw new CoreException(e.getStatus());
						}
					}
				});
			} catch (CoreException e) {
				throw CVSException.wrapException(e);
			}
		} finally {
			endOperation();
			monitor.done();
		}
	}
	
	/**
	 * If not already cached, loads and caches the resource sync for the children of the container.
	 * Folder must exist and must not be the workspace root.
	 *
	 * @param container the container
	 */
	private void cacheResourceSyncForChildren(IContainer container, boolean canModifyWorkspace) throws CVSException {
		// don't try to load if the information is already cached
		if (! getSyncInfoCacheFor(container).isResourceSyncInfoCached(container)) {
			// load the sync info from disk
			byte[][] infos;
			// do not load the sync info for resources that are linked
			if (isLinkedResource(container)) {
				infos = null;
			} else {
				infos = SyncFileWriter.readAllResourceSync(container);
			}
			try {
				if (infos != null) {
					for (byte[] syncBytes : infos) {
						IPath name = new Path(null, getName(syncBytes));
						IResource resource;
						if (isFolder(syncBytes)) {
							resource = container.getFolder(name);
						} else {
							resource = container.getFile(name);
						}
						getSyncInfoCacheFor(resource).setCachedSyncBytes(resource, syncBytes, canModifyWorkspace);
					}
				}
				getSyncInfoCacheFor(container).setResourceSyncInfoCached(container);
			} catch (CVSException e) {
				if (Policy.DEBUG_METAFILE_CHANGES) {
					System.err.println("Failed to cache Entries for folder " + container.getFullPath()); //$NON-NLS-1$
				}
				throw e;
			}
		}
	}
	
	/**
	 * If not already cached, loads and caches the folder sync for the
	 * container. Folder must exist and must not be the workspace root.
	 *
	 * @param container the container
	 */
	private void cacheFolderSync(IContainer container) throws CVSException {
		// don't try to load if the information is already cached
		if (! getSyncInfoCacheFor(container).isFolderSyncInfoCached(container)) {
			// load the sync info from disk
			FolderSyncInfo info;
			// do not load the sync info for resources that are linked
			if (isLinkedResource(container)) {
				info = null;
			} else {
				info = SyncFileWriter.readFolderSync(container);
			}
			getSyncInfoCacheFor(container).setCachedFolderSync(container, info, false);
		}
	}
	
	private boolean isLinkedResource(IResource resource) {
		return CVSWorkspaceRoot.isLinkedResource(resource);
	}

	/**
	 * Load the sync info for the given resource from disk
	 * @param resource
	 * @return byte[]
	 */
	private byte[] getSyncBytesFromDisk(IResource resource) throws CVSException {
		byte[][] infos = SyncFileWriter.readAllResourceSync(resource.getParent());
		if (infos == null) return null;
		for (byte[] syncBytes : infos) {
			if (resource.getName().equals(getName(syncBytes))) {
				return syncBytes;
			}
		}
		return null;
	}
	
	/**
	 * Commits the cache after a series of operations.
	 * 
	 * Will return STATUS_OK unless there were problems writting sync 
	 * information to disk. If an error occurs a multistatus is returned
	 * with the list of reasons for the failures. Failures are recovered,
	 * and all changed resources are given a chance to be written to disk.
	 * 
	 * @param monitor the progress monitor, may be null
	 */
	/* internal use only */ IStatus commitCache(ThreadInfo threadInfo, IProgressMonitor monitor) {
		if (threadInfo.isEmpty()) {
			return SyncInfoCache.STATUS_OK;
		}
		List<IStatus> errors = new ArrayList<>();
		try {
			/*** prepare operation ***/
			// find parents of changed resources
			IResource[] changedResources = threadInfo.getChangedResources();
			IContainer[] changedFolders;
			if (threadInfo instanceof CVSThreadInfo) {
				changedFolders = ((CVSThreadInfo)threadInfo).getChangedFolders();
			} else {
				changedFolders = new IContainer[0];
			}
			Set<IContainer> dirtyParents = new HashSet<>();
			for (IResource resource : changedResources) {
				IContainer folder = resource.getParent();
				dirtyParents.add(folder);
			}
			
			monitor = Policy.monitorFor(monitor);
			int numDirty = dirtyParents.size();
			int numResources = changedFolders.length + numDirty;
			monitor.beginTask(null, numResources);
			if(monitor.isCanceled()) {
				monitor.subTask(CVSMessages.EclipseSynchronizer_UpdatingSyncEndOperationCancelled); 
			} else {
				monitor.subTask(CVSMessages.EclipseSynchronizer_UpdatingSyncEndOperation); 
			}
			
			/*** write sync info to disk ***/
			// folder sync info changes
			for (IContainer folder : changedFolders) {
				if (folder.exists() && folder.getType() != IResource.ROOT) {
					try {
						beginOperation();
						FolderSyncInfo info = sessionPropertyCache.getCachedFolderSync(folder, true);
						// Do not write the folder sync for linked resources
						if (info == null) {
							// deleted folder sync info since we loaded it
							// (but don't overwrite the sync info for linked folders
							if (!isLinkedResource(folder))
								SyncFileWriter.deleteFolderSync(folder);
							dirtyParents.remove(folder);
						} else {
							// modified or created new folder sync info since we loaded it
							SyncFileWriter.writeFolderSync(folder, info);
						}
					} catch(CVSException e) {					
						try {
							sessionPropertyCache.purgeCache(folder, true /* deep */);
						} catch(CVSException pe) {
							errors.add(pe.getStatus());
						}
						errors.add(e.getStatus());
					} finally {
						endOperation();
					}
				}
				monitor.worked(1);
			}

			// update progress for parents we will skip because they were deleted
			monitor.worked(numDirty - dirtyParents.size());

			// resource sync info changes
			for (Iterator it = dirtyParents.iterator(); it.hasNext();) {
				IContainer folder = (IContainer) it.next();
				if (folder.exists() && folder.getType() != IResource.ROOT) {
					// write sync info for all children in one go
					try {
						beginOperation();
						List<byte[]> infos = new ArrayList<>();
						IResource[] children = folder.members(true);
						for (IResource resource : children) {
							byte[] syncBytes = getSyncBytes(resource);
							if (syncBytes != null) {
								infos.add(syncBytes);
							}
						}
						// do not overwrite the sync info for linked resources
						if (infos.size() > 0 || !isLinkedResource(folder))
							SyncFileWriter.writeAllResourceSync(folder,
								infos.toArray(new byte[infos.size()][]));
					} catch(CVSException e) {
						try {
							sessionPropertyCache.purgeCache(folder, false /* depth 1 */);
						} catch(CVSException pe) {
							errors.add(pe.getStatus());
						}							
						errors.add(e.getStatus());
					} catch (CoreException e) {
						try {
							sessionPropertyCache.purgeCache(folder, false /* depth 1 */);
						} catch(CVSException pe) {
							errors.add(pe.getStatus());
						}							
						errors.add(e.getStatus());
					} finally {
						endOperation();
					}
				}
				monitor.worked(1);
			}
			
			/*** broadcast events ***/
			monitor.subTask(CVSMessages.EclipseSynchronizer_NotifyingListeners); 
			Set<IResource> allChanges = new HashSet<>();
			allChanges.addAll(Arrays.asList(changedResources));
			allChanges.addAll(Arrays.asList(changedFolders));
			allChanges.addAll(dirtyParents);	
			IResource[] resources = allChanges.toArray(
				new IResource[allChanges.size()]);
			broadcastResourceStateChanges(resources);
			if ( ! errors.isEmpty()) {
				MultiStatus status = new MultiStatus(CVSProviderPlugin.ID, 
											CVSStatus.COMMITTING_SYNC_INFO_FAILED, 
											CVSMessages.EclipseSynchronizer_ErrorCommitting, 
											null);
				for (int i = 0; i < errors.size(); i++) {
					status.merge(errors.get(i));
				}
				return status;
			}
			return SyncInfoCache.STATUS_OK;
		} finally {
			monitor.done();
		}
	}

	/**
	 * Broadcasts the resource state changes for the given resources to CVS Provider Plugin
	 */
	void broadcastResourceStateChanges(IResource[] resources) {
		if (resources.length > 0) {
			ResourceStateChangeListeners.getListener().resourceSyncInfoChanged(resources);
		}
	}

	/**
	 * Returns the resource sync info for the resource; null if none.
	 * Parent must exist and must not be the workspace root.
	 * The resource sync info for the children of the parent container MUST ALREADY BE CACHED.
	 * 
	 * @param resource the resource
	 * @return the resource sync info for the resource, or null
	 * @see #cacheResourceSyncForChildren
	 */
	private byte[] getCachedSyncBytes(IResource resource) throws CVSException {
		return getSyncInfoCacheFor(resource).getCachedSyncBytes(resource, true);
	}

	/**
	 * Returns the resource sync info for the resource; null if none.
	 * Parent must exist and must not be the workspace root.
	 * The resource sync info for the children of the parent container MUST ALREADY BE CACHED.
	 * 
	 * @param resource the resource
	 * @return the resource sync info for the resource, or null
	 * @see #cacheResourceSyncForChildren
	 */
	private void setCachedSyncBytes(IResource resource, byte[] syncBytes) throws CVSException {
		getSyncInfoCacheFor(resource).setCachedSyncBytes(resource, syncBytes, true);
		resourceChanged(resource);
	}
		
	/**
	 * Sets the resource sync info for the resource; if null, deletes it. Parent
	 * must exist and must not be the workspace root. The resource sync info for
	 * the children of the parent container MUST ALREADY BE CACHED.
	 * 
	 * @param resource the resource
	 * @param info the new resource sync info
	 * @see #cacheResourceSyncForChildren
	 */
	private void setCachedResourceSync(IResource resource, ResourceSyncInfo info) throws CVSException {
		//todo
		byte[] syncBytes = null;
		if (info != null) syncBytes = info.getBytes();
		getSyncInfoCacheFor(resource).setCachedSyncBytes(resource, syncBytes, true);
	}
	
	/**
	 * If not already cached, loads and caches the folder ignores sync for the container.
	 * Folder must exist and must not be the workspace root.
	 * 
	 * @param container the container
	 * @return the folder ignore patterns, or an empty array if none
	 */
	private FileNameMatcher cacheFolderIgnores(IContainer container) throws CVSException {
		return sessionPropertyCache.getFolderIgnores(container, true);
	}
	
	/**
	 * Sets the array of folder ignore patterns for the container, must not be null.
	 * Folder must exist and must not be the workspace root.
	 * 
	 * @param container the container
	 * @param ignores the array of ignore patterns
	 */
	private void setCachedFolderIgnores(IContainer container, String[] ignores) throws CVSException {
		sessionPropertyCache.setCachedFolderIgnores(container, ignores);
	}
	
	/*
	 * Recursively adds to the possibleIgnores list all children of the given 
	 * folder that can be ignored. This method may only be invoked when a 
	 * schedling rule for the given foldr is held and when the CVs sync lock is
	 * held.
	 * 
	 * @param folder the folder to be searched
	 * @param possibleIgnores the list of IResources that can be ignored
	 */
	private void accumulateNonManagedChildren(IContainer folder, List<IResource> possibleIgnores) throws CVSException {
		try {
			cacheResourceSyncForChildren(folder, true /* can modify workspace */);
			IResource[] children = folder.members();
			List<IResource> folders = new ArrayList<>();
			// deal with all files first and then folders to be otimized for caching scheme
			for (IResource child : children) {
				if(getCachedSyncBytes(child)==null) {
					possibleIgnores.add(child);
				}
				if(child.getType()!=IResource.FILE) {
					folders.add(child);
				}
			}
			for (Iterator iter = folders.iterator(); iter.hasNext();) {
				IContainer child = (IContainer) iter.next();
				accumulateNonManagedChildren(child, possibleIgnores);
			}
		} catch(CoreException e) {
			throw CVSException.wrapException(e);
		}
	}
	
	/**
	 * Add the entry to the CVS/Notify file. We are not initially concerned with efficiency
	 * since edit/unedit are typically issued on a small set of files.
	 * 
	 * XXX If there was a previous notify entry for the resource, it is replaced. This is
	 * probably not the proper behavior (see EclipseFile).
	 * 
	 * A value of null for info indicates that any entry for the given
	 * resource is to be removed from the Notify file.
	 * 
	 * @param resource
	 * @param info
	 */
	public void setNotifyInfo(IResource resource, NotifyInfo info) throws CVSException {
		NotifyInfo[] infos = SyncFileWriter.readAllNotifyInfo(resource.getParent());
		if (infos == null) {
			// if the file is empty and we are removing an entry, just return;
			if (info == null) return;
			infos = new NotifyInfo[] { info };
		} else {
			Map<String, NotifyInfo> infoMap = new HashMap<>();
			for (NotifyInfo notifyInfo : infos) {
				infoMap.put(notifyInfo.getName(), notifyInfo);
			}
			if (info == null) {
				// if the info is null, remove the entry
				infoMap.remove(resource.getName());
			} else {
				// add the new entry to the list
				infoMap.put(info.getName(), info);
			}
			
			NotifyInfo[] newInfos = new NotifyInfo[infoMap.size()];
			int i = 0;
			for (Iterator iter = infoMap.values().iterator(); iter.hasNext();) {
				newInfos[i++] = (NotifyInfo) iter.next();
			}
			infos = newInfos;
		}
		SyncFileWriter.writeAllNotifyInfo(resource.getParent(), infos);
	}

	/**
	 * Method getNotifyInfo.
	 * @param resource
	 * @return NotifyInfo
	 */
	public NotifyInfo getNotifyInfo(IResource resource) throws CVSException {
		NotifyInfo[] infos = SyncFileWriter.readAllNotifyInfo(resource.getParent());
		if (infos == null) return null;
		for (NotifyInfo notifyInfo : infos) {
			if (notifyInfo.getName().equals(resource.getName())) {
				return notifyInfo;
			}
		}
		return null;
	}

	/**
	 * Method deleteNotifyInfo.
	 * @param resource
	 */
	public void deleteNotifyInfo(IResource resource) throws CVSException {
		NotifyInfo[] infos = SyncFileWriter.readAllNotifyInfo(resource.getParent());
		if (infos == null) return;
		Map<String, NotifyInfo> infoMap = new HashMap<>();
		for (NotifyInfo notifyInfo : infos) {
			infoMap.put(notifyInfo.getName(), notifyInfo);
		}
		infoMap.remove(resource.getName());
		NotifyInfo[] newInfos = new NotifyInfo[infoMap.size()];
		int i = 0;
		for (Iterator iter = infoMap.values().iterator(); iter.hasNext();) {
			newInfos[i++] = (NotifyInfo) iter.next();
		}
		SyncFileWriter.writeAllNotifyInfo(resource.getParent(), newInfos);
	}
	
	/**
	 * Add the entry to the CVS/Baserev file. We are not initially concerned
	 * with efficiency since edit/unedit are typically issued on a small set of
	 * files.
	 *
	 * XXX If there was a previous notify entry for the resource, it is replaced. This is
	 * probably not the proper behavior (see EclipseFile).
	 *
	 * @param resource
	 * @param info
	 */
	public void setBaserevInfo(IResource resource, BaserevInfo info) throws CVSException {
		BaserevInfo[] infos = SyncFileWriter.readAllBaserevInfo(resource.getParent());
		if (infos == null) {
			infos = new BaserevInfo[] { info };
		} else {
			Map<String, BaserevInfo> infoMap = new HashMap<>();
			for (BaserevInfo i : infos) {
				infoMap.put(i.getName(), i);
			}
			infoMap.put(info.getName(), info);
			BaserevInfo[] newInfos = new BaserevInfo[infoMap.size()];
			int i = 0;
			for (Iterator iter = infoMap.values().iterator(); iter.hasNext();) {
				newInfos[i++] = (BaserevInfo) iter.next();
			}
			infos = newInfos;
		}
		SyncFileWriter.writeAllBaserevInfo(resource.getParent(), infos);
	}

	/**
	 * Method getBaserevInfo.
	 * @param resource
	 * @return BaserevInfo
	 */
	public BaserevInfo getBaserevInfo(IResource resource) throws CVSException {
		BaserevInfo[] infos = SyncFileWriter.readAllBaserevInfo(resource.getParent());
		if (infos == null) return null;
		for (BaserevInfo info : infos) {
			if (info.getName().equals(resource.getName())) {
				return info;
			}
		}
		return null;
	}
			
	/**
	 * Method deleteNotifyInfo.
	 * @param resource
	 */
	public void deleteBaserevInfo(IResource resource) throws CVSException {
		BaserevInfo[] infos = SyncFileWriter.readAllBaserevInfo(resource.getParent());
		if (infos == null) return;
		Map<String, BaserevInfo> infoMap = new HashMap<>();
		for (BaserevInfo info : infos) {
			infoMap.put(info.getName(), info);
		}
		infoMap.remove(resource.getName());
		BaserevInfo[] newInfos = new BaserevInfo[infoMap.size()];
		int i = 0;
		for (Iterator iter = infoMap.values().iterator(); iter.hasNext();) {
			newInfos[i++] = (BaserevInfo) iter.next();
		}
		SyncFileWriter.writeAllBaserevInfo(resource.getParent(), newInfos);
	}

	public void copyFileToBaseDirectory(final IFile file, IProgressMonitor monitor) throws CVSException {
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(null, 100);
		ISchedulingRule rule = null;
		try {
			rule = beginBatching(file, Policy.subMonitorFor(monitor, 10));
			ResourceSyncInfo info = getResourceSync(file);
			// The file must exist remotely and locally
			if (info == null || info.isAdded() || info.isDeleted())
				return;
			SyncFileWriter.writeFileToBaseDirectory(file, Policy.subMonitorFor(monitor, 80));
			resourceChanged(file);
		} finally {
			if (rule != null) endBatching(rule, Policy.subMonitorFor(monitor, 10));
			monitor.done();
		}
	}
	
	public void restoreFileFromBaseDirectory(final IFile file, IProgressMonitor monitor) throws CVSException {
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(null, 100);
		ISchedulingRule rule = null;
		try {
			rule = beginBatching(file, Policy.subMonitorFor(monitor, 10));
			ResourceSyncInfo info = getResourceSync(file);
			// The file must exist remotely
			if (info == null || info.isAdded())
				return;
			SyncFileWriter.restoreFileFromBaseDirectory(file, Policy.subMonitorFor(monitor, 80));
			resourceChanged(file);
		} finally {
			if (rule != null) endBatching(rule, Policy.subMonitorFor(monitor, 10));
			monitor.done();
		}
	}
	
	public void deleteFileFromBaseDirectory(final IFile file, IProgressMonitor monitor) throws CVSException {
		ResourceSyncInfo info = getResourceSync(file);
		// The file must exist remotely
		if (info == null || info.isAdded())
			return;
		SyncFileWriter.deleteFileFromBaseDirectory(file, monitor);
	}
	
	/**
	 * Method isSyncInfoLoaded returns true if all the sync info for the
	 * provided resources is loaded into the internal cache.
	 * 
	 * @param resources
	 * @param i
	 * @return boolean
	 */
	public boolean isSyncInfoLoaded(IResource[] resources, int depth) throws CVSException {
		// get the folders involved
		IContainer[] folders = getParentFolders(resources, depth);
		// for all folders that have a CVS folder, ensure the sync info is cached
		for (IContainer parent : folders) {
			if (!getSyncInfoCacheFor(parent).isSyncInfoLoaded(parent)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Method ensureSyncInfoLoaded loads all the relevent sync info into the cache.
	 * This method can only be invoked when the workspace is open for modification.
	 * in other words it cannot be invoked from inside a POST_CHANGE delta listener.
	 * @param resources
	 * @param i
	 * @return Object
	 */
	public void ensureSyncInfoLoaded(IResource[] resources, int depth) throws CVSException {
		// get the folders involved
		IContainer[] folders = getParentFolders(resources, depth);
		// Cache the sync info for all the folders
		for (IContainer parent : folders) {
			ISchedulingRule rule = null;
			try {
				rule = beginBatching(parent, null);
				try {
					beginOperation();
					cacheResourceSyncForChildren(parent, true /* can modify workspace */);
					cacheFolderSync(parent);
					cacheFolderIgnores(parent);
				} finally {
					endOperation();
				}
			} finally {
				if (rule != null) endBatching(rule, null);
			}
		}
	}

	/*
	 * Collect the projects and parent folders of the resources since 
	 * thats were the sync info is kept.
	 */
	private IContainer[] getParentFolders(IResource[] resources, int depth) throws CVSException {
		final Set<IResource> folders = new HashSet<>();
		for (IResource resource : resources) {
			folders.add(resource.getProject());
			if (resource.getType() != IResource.PROJECT) {
				folders.add(resource.getParent());
			}
			// use the depth to gather child folders when appropriate
			if (depth != IResource.DEPTH_ZERO) {
				try {
					resource.accept(new IResourceVisitor() {
						public boolean visit(IResource innerResource) throws CoreException {
							if (innerResource.getType() == IResource.FOLDER)
								folders.add(innerResource);
							// let the depth determine who we visit
							return true;
						}
					}, depth, false);
				} catch (CoreException e) {
					throw CVSException.wrapException(e);
				}
			}
		}
		return folders.toArray(new IContainer[folders.size()]);
	}
	
	/**
	 * Perform sync info batching within the context of the given resource
	 * scheduling rule while running the given ICVSRunnable.
	 * @param runnable
	 * @param monitor
	 * @throws CVSException
	 */
	public void run(ISchedulingRule resourceRule, ICVSRunnable runnable, IProgressMonitor monitor) throws CVSException {
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(null, 100);
		ISchedulingRule rule = beginBatching(resourceRule, Policy.subMonitorFor(monitor, 1));
		try {
			runnable.run(Policy.subMonitorFor(monitor, 98));
		} finally {
			if (rule != null) endBatching(rule, Policy.subMonitorFor(monitor, 1));
			monitor.done();
		}
	}
	
	/**
	 * Method isEdited returns true if a "cvs edit" was performed on the given
	 * file and no commit or unedit has yet been performed.
	 * @param iResource
	 * @return boolean
	 */
	public boolean isEdited(IFile resource) {
		return SyncFileWriter.isEdited(resource);
	}
	
	/* package */ void adjustDirtyStateRecursively(IResource resource, String indicator) throws CVSException {
		if (resource.getType() == IResource.ROOT) return;
		try {
			beginOperation();
			
			if (getSyncInfoCacheFor(resource).cachesDirtyState()) {
				if (indicator == getDirtyIndicator(resource)) {
					return;
				}
				getSyncInfoCacheFor(resource).setDirtyIndicator(resource, indicator);	
			} 					
			
			if (Policy.DEBUG_DIRTY_CACHING) {
				debug(resource, indicator, "adjusting dirty state"); //$NON-NLS-1$
			}									

			IContainer parent = resource.getParent();
			if(indicator == NOT_DIRTY_INDICATOR) {
				adjustDirtyStateRecursively(parent, RECOMPUTE_INDICATOR);
			}
			
			if(indicator == RECOMPUTE_INDICATOR) {
				adjustDirtyStateRecursively(parent, RECOMPUTE_INDICATOR);
			} 
			
			if(indicator == IS_DIRTY_INDICATOR) {
				adjustDirtyStateRecursively(parent, indicator);
			} 
		} finally {
			endOperation();
		}
	}

	protected String getDirtyIndicator(IResource resource) throws CVSException {
		// Do a check outside the lock for the dirty indicator
		String indicator = getSyncInfoCacheFor(resource).getDirtyIndicator(resource, false /* not thread safe */);
		if (indicator != null)
			return indicator;
		try {
			beginOperation();
			return getSyncInfoCacheFor(resource).getDirtyIndicator(resource, true);
		} finally {
			endOperation();
		}
	}
	
	/*
	 * Mark the given resource as either modified or clean using a persistant
	 * property. Do nothing if the modified state is already what we want.
	 * Return true if the modification state was changed.
	 */
	protected void setDirtyIndicator(IResource resource, boolean modified) throws CVSException {
		String indicator = modified ? IS_DIRTY_INDICATOR : NOT_DIRTY_INDICATOR;
		// set the dirty indicator and adjust the parent accordingly			
		adjustDirtyStateRecursively(resource, indicator);
	}

	/**
	 * Method getName.
	 * @param syncBytes
	 */
	private String getName(byte[] syncBytes) throws CVSException {
		return ResourceSyncInfo.getName(syncBytes);
	}
	
	/**
	 * Method isFolder.
	 * @param syncBytes
	 * @return boolean
	 */
	private boolean isFolder(byte[] syncBytes) {
		return ResourceSyncInfo.isFolder(syncBytes);
	}
		
	/**
	 * Method convertToDeletion.
	 * @param syncBytes
	 * @return byte[]
	 */
	private byte[] convertToDeletion(byte[] syncBytes) throws CVSException {
		return ResourceSyncInfo.convertToDeletion(syncBytes);
	}

	static public void debug(IResource resource, String indicator, String string) {
		String di = EclipseSynchronizer.IS_DIRTY_INDICATOR;
		if(indicator == EclipseSynchronizer.IS_DIRTY_INDICATOR) {
			di = "dirty";	//$NON-NLS-1$
		} else if(indicator == EclipseSynchronizer.NOT_DIRTY_INDICATOR) {
			di = "clean";	//$NON-NLS-1$
		} else {
			di = "needs recomputing";	//$NON-NLS-1$
		} 
		System.out.println("["+string + ":" + di + "]  "  + resource.getFullPath()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	/**
	 * @param file
	 * @return int
	 */
	public int getModificationState(IResource resource) throws CVSException {
		String indicator =  getDirtyIndicator(resource);
		if (Policy.DEBUG_DIRTY_CACHING) {
			debug(resource, indicator, "getModificationState"); //$NON-NLS-1$
		}
		if (indicator == null || indicator == RECOMPUTE_INDICATOR) {
			return ICVSFile.UNKNOWN;
		} else if (indicator == IS_DIRTY_INDICATOR) {
			return ICVSFile.DIRTY;
		} else if (indicator == NOT_DIRTY_INDICATOR) {
			return ICVSFile.CLEAN;
		} else {
			return ICVSFile.UNKNOWN;
		}
	}

	/**
	 * Return whether the resource is within the scope of a currently active
	 * CVS operation.
	 * @param resource
	 * @return
	 */
	public boolean isWithinActiveOperationScope(IResource resource) {
		return resourceLock.isWithinActiveOperationScope(resource);
	}
	
	/**
	 * Set the timestamp of the given file and set it to be CLEAN. It is
	 * assumed that this method is only invoked to reset the file timestamp
	 * to the timestamp that is in the CVS/Entries file.
	 * @param file
	 * @param time
	 * @throws CVSException
	 */
	public void setTimeStamp(EclipseFile cvsFile, long time) throws CVSException {
		ISchedulingRule rule = null;
		IFile file = (IFile)cvsFile.getIResource();
		try {
			rule = beginBatching(file, null);
			try {
				beginOperation();
				try {
					file.setLocalTimeStamp(time);
					setModified(cvsFile, ICVSFile.CLEAN);
				} catch (CoreException e) {
					throw CVSException.wrapException(e);
				}
				resourceChanged(file);		
			} finally {
				endOperation();
			}
		} finally {
			if (rule != null) endBatching(rule, null);
		}
	}

	/**
	 * React to a resource that was just moved by the move/delete hook.
	 * @param resource the resource that was moved (at its new location)
	 */
	public void postMove(IResource resource) throws CVSException {
		try {
			beginOperation();
			if (resource.getType() == IResource.FILE) {
				// Purge any copied sync info so true sync info will 
				// be obtained from the synchronizer cache
				sessionPropertyCache.purgeResourceSyncCache(resource);
			} else {
				IContainer container = (IContainer)resource;
				// Purge any copied sync info
				sessionPropertyCache.purgeCache(container, true /* deep */);
				// Dirty all resources so old sync info will be rewritten to disk
				try {
					container.accept(new IResourceVisitor() {
						public boolean visit(IResource resource) throws CoreException {
							if (getSyncBytes(resource) != null) {
								resourceChanged(resource);
							}
							if (resource.getType() != IResource.FILE) {
								if (getFolderSync((IContainer)resource) != null) {
									folderChanged((IContainer)resource);
									return true;
								}
							}
							return false;
						}
					});
				} catch (CoreException e) {
					throw CVSException.wrapException(e);
				}
				// Flush the sync info to disk
				flush(container, true /* deep */, null);
			}
		} finally {
			endOperation();
		}
	}
	
	/**
	 * This method is to be invoked only from the move/delete hook. Its purpose is to obtain the
	 * sync look in order to prevent other threads from accessing sync info while the move/delete is
	 * taking place.
	 * 
	 * @param runnable
	 * @param monitor
	 * @throws CVSException
	 */
	public void performMoveDelete(ICVSRunnable runnable, IProgressMonitor monitor) throws CVSException {
		ISchedulingRule rule = null;
		try {
			monitor.beginTask(null, 100);
			rule = beginBatching(null, null);
			try {
				beginOperation();
				runnable.run(Policy.subMonitorFor(monitor, 95));
			} finally {
				endOperation();
			}
		} finally {
			if (rule != null) endBatching(rule, Policy.subMonitorFor(monitor, 5));
			monitor.done();
		}
	}
	
	/**
	 * Compute the modification state for the given file. If the modificationState is
	 * ICVSFile.UNKNOWN, it is computed. However, if it is CLEAN or DIRTY, 
	 * it is set accordingly. CLEAN or DIRTY can only be used if the caller is protected
	 * from resource modifications (either by a scheduling rule or inside a delta handler).
	 * @param file
	 * @param modificationState
	 * @return true if the file is dirty
	 */
	public boolean setModified(EclipseFile cvsFile, int modificationState) throws CVSException {
		try {
			beginOperation();
			boolean dirty;
			if (modificationState == ICVSFile.UNKNOWN) {
				dirty = cvsFile.isDirty();
			} else {
				dirty = modificationState == ICVSFile.DIRTY;
			}
			setDirtyIndicator(cvsFile.getIResource(), dirty);
			return dirty;
		} finally {
			endOperation();
		}

	}

	/**
	 * Set the modified state of the folder. This method can be called when no resource locks are
	 * held. It will check the cached modification state of all the folder's children before setting.
	 * If the states of the children do not match, the state for the folder is not cached.
	 * @param folder
	 * @param modified
	 */
	public void setModified(ICVSFolder cvsFolder, boolean modified) throws CVSException {
		try {
			beginOperation();
			IContainer folder = (IContainer)cvsFolder.getIResource();
			// The drop out condition for clean or dirty are the opposite.
			// (i.e. if modified and a dirty is found we can set the indicator
			// and if not modified and a dirty or unknown is found we cannot set the indicator)
			boolean okToSet = !modified;
			// Obtain the children while we're locked to ensure some were not added or changed
			ICVSResource[] children = cvsFolder.members(ICVSFolder.ALL_UNIGNORED_MEMBERS);
			for (ICVSResource child : children) {
				IResource resource = child.getIResource();
				if (modified) {
					if (getDirtyIndicator(resource) == IS_DIRTY_INDICATOR) {
						okToSet = true;
						break;
					}
				} else {
					if (getDirtyIndicator(resource) != NOT_DIRTY_INDICATOR) {
						okToSet = false;
						break;
					}
				}
			}
			if (okToSet) {
				setDirtyIndicator(folder, modified);
			}
		} finally {
			endOperation();
		}
	}
	
	public boolean wasPhantom(IResource resource) {
		if (resource.exists()) {
			try {
				return (synchronizerCache.getCachedSyncBytes(resource, true) != null 
					|| (resource.getType() == IResource.FOLDER
							&& synchronizerCache.hasCachedFolderSync((IContainer)resource)));
			} catch (CVSException e) {
				// Log and assume resource is not a phantom
				CVSProviderPlugin.log(e);
			}
		}
		return false;
	}
	
	/**
	 * Method called from background handler when resources that are mapped to CVS are recreated
	 * @param resources
	 * @param monitor
	 * @throws CVSException
	 */
	public void resourcesRecreated(IResource[] resources, IProgressMonitor monitor) throws CVSException {
		if (resources.length == 0) return;
		ISchedulingRule rule = null;
		ISchedulingRule projectsRule = getProjectRule(resources);
		try {
			monitor = Policy.monitorFor(monitor);
			monitor.beginTask(null, 100);
			rule = beginBatching(projectsRule, monitor);
			for (IResource resource : resources) {
				try {
					created(resource);
				} catch (CVSException e) {
					CVSProviderPlugin.log(e);
				}
			}
		} finally {
			if (rule != null) endBatching(rule, Policy.subMonitorFor(monitor, 5));
			monitor.done();
		}
	}
	
	private ISchedulingRule getProjectRule(IResource[] resources) {
		HashSet<IProject> set = new HashSet<>();
		for (IResource resource : resources) {
			set.add(resource.getProject());
		}
		IProject[] projects = set.toArray(new IProject[set.size()]);
		if (projects.length == 1) {
			return projects[0];
		}
		return new MultiRule(projects);
	}

	protected void created(IResource resource) throws CVSException {
		try {
			beginOperation();
			if (resource.exists()) {
				restoreResourceSync(resource);
				if (resource.getType() == IResource.FOLDER) {
					restoreFolderSync((IFolder)resource);
				}
			}
		} finally {
			endOperation();
		}
	}
	
	/*
	 * Restore the folder sync info for the given folder
	 */
	private void restoreFolderSync(IFolder folder) throws CVSException {
		try {
			// set the dirty count using what was cached in the phantom it
			beginOperation();
			FolderSyncInfo folderInfo = synchronizerCache.getCachedFolderSync(folder, true);
			if (folderInfo != null) {
				// There is folder sync info to restore
				if (folder.getFolder(SyncFileWriter.CVS_DIRNAME).exists()) {
					// There is already a CVS subdirectory which indicates that
					// either the folder was recreated by an external tool or that
					// a folder with CVS information was copied from another location.
					// To know the difference, we need to compare the folder sync info.
					// If they are mapped to the same root and repository then just
					// purge the phantom info. Otherwise, keep the original sync info.

					// Get the new folder sync info
					FolderSyncInfo newFolderInfo = getFolderSync(folder);
					if (newFolderInfo.getRoot().equals(folderInfo.getRoot())
							&& newFolderInfo.getRepository().equals(folderInfo.getRepository())) {
						// The folder is the same so use what is on disk.
						// Fall through to ensure that the Root and Repository files exist
					} else {
						// The folder is mapped to a different location.
						// Purge new resource sync before restoring from phantom
						ICVSFolder cvsFolder = CVSWorkspaceRoot.getCVSFolderFor(folder);
						ICVSResource[] children = cvsFolder.members(ICVSFolder.MANAGED_MEMBERS);
						for (ICVSResource resource : children) {
							deleteResourceSync(resource.getIResource());
						}
					}
				}

				// set the sync info using what was cached in the phantom
				setFolderSync(folder, folderInfo);
				// purge the dirty cache so any old persisted dirty state is purged
				sessionPropertyCache.purgeDirtyCache(folder);
				// Indicate that a member has changed so the entries file gets written (see bug 181546)
				IResource[] members = members(folder);
				IResource changedResource = null;
				for (IResource resource : members) {
					if (getSyncBytes(resource) != null) {
						changedResource = resource;
						break;
					}
				}
				if (changedResource == null) {
					changedResource = folder.getFile("dummy"); //$NON-NLS-1$
				}
				resourceChanged(changedResource);
			}
		} finally {
			try {
				endOperation();
			} finally {
				synchronizerCache.flush(folder);
			}
		}
	}

	/*
	 * Restore the resource sync info for the given resource.
	 */
	private void restoreResourceSync(IResource resource) throws CVSException {
		try {
			beginOperation();
			byte[] syncBytes = synchronizerCache.getCachedSyncBytes(resource, true);
			if (syncBytes != null) {
				if (!ResourceSyncInfo.isFolder(syncBytes)) {
					syncBytes = ResourceSyncInfo.convertFromDeletion(syncBytes);
				}
				byte[] newBytes = getSyncBytes(resource);
				if (newBytes != null && !ResourceSyncInfo.isFolder(newBytes)) {
					newBytes = ResourceSyncInfo.convertFromDeletion(newBytes);
				}
				if (newBytes == null || Util.equals(syncBytes, newBytes)) {
					// only move the sync info if there is no new sync info
					setSyncBytes(resource, syncBytes);
				}
			}
		} finally {
			try {
				endOperation();
			} finally {
				synchronizerCache.setCachedSyncBytes(resource, null, true);
			}
		}
	}
	
	private void purgeDirtyCache(IProject project, IProgressMonitor monitor) throws CVSException {
		sessionPropertyCache.purgeDirtyCache(project);
	}
}
