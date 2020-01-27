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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.core.Policy;
import org.eclipse.team.internal.core.subscribers.BatchingLock;
import org.eclipse.team.internal.core.subscribers.BatchingLock.IFlushOperation;
import org.eclipse.team.internal.core.subscribers.SyncByteConverter;

/**
 * This class manages the synchronization between local resources and their
 * corresponding resource variants. It provides the following:
 * <ul>
 * <li>Three way synchronization (set base, set remote, ignored)
 * <li>Resource traversal (members)
 * <li>Change events and event batching (run)
 * <li>Thread-safety
 * </ul>
 *
 * @since 3.0
 */
public class ThreeWaySynchronizer {
	/**
	 * Callback which is invoked when the batching resource lock is released or when
	 * a flush is requested (see beginBatching(IResource)).
	 *
	 * @see BatchingLock#flush(IProgressMonitor)
	 */
	private IFlushOperation flushOperation = (info, monitor) -> {
		if (info != null && !info.isEmpty()) {
			broadcastSyncChanges(info.getChangedResources());
		}
	};

	private static final byte[] IGNORED_BYTES = "i".getBytes(); //$NON-NLS-1$

	private ILock lock = Job.getJobManager().newLock();
	private BatchingLock batchingLock = new BatchingLock();
	private ResourceVariantByteStore cache;
	private Set<ISynchronizerChangeListener> listeners = new HashSet<>();

	/**
	 * Create a three-way synchronizer that uses a persistent
	 * byte store with the given qualified name as its unique
	 * identifier.
	 * @param name the unique identifier for the persistent store
	 */
	public ThreeWaySynchronizer(QualifiedName name) {
		this(new PersistantResourceVariantByteStore(name));
	}

	/**
	 * Create a three-way synchronizer that uses the given byte store
	 * as its underlying byte cache.
	 * @param store the byte store this synchronizer uses to cache its bytes
	 */
	public ThreeWaySynchronizer(ResourceVariantByteStore store) {
		cache = store;
	}

	/**
	 * Adds a listener to this synchronizer. Listeners will be notified
	 * when the synchronization state of a resource changes. Listeners
	 * are not notified when files are modified locally. Clients can
	 * make use of the <code>IResource</code> delta mechanism if they
	 * need to know about local modifications.
	 * Has no effect if an identical listener is already registered.
	 * <p>
	 * Team resource change listeners are informed about state changes
	 * that affect the resources supervised by this subscriber.</p>
	 *
	 * @param listener a synchronizer change listener
	 */
	public void addListener(ISynchronizerChangeListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}

	/**
	 * Removes a listener previously registered with this synchronizer.
	 * Has no effect if an identical listener is not registered.
	 *
	 * @param listener a synchronizer change listener
	 */
	public void removeListener(ISynchronizerChangeListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	/**
	 * Return the base bytes that are cached for the given resource
	 * or <code>null</code> if no base is cached. The returned bytes
	 * should uniquely identify the resource variant that is the base
	 * for the given local resource.
	 *
	 * @param resource the resource
	 * @return the base bytes cached with the resource or <code>null</code>
	 * @throws TeamException if an error occurs
	 */
	public byte[] getBaseBytes(IResource resource) throws TeamException {
		try {
			beginOperation();
			byte[] syncBytes = internalGetSyncBytes(resource);
			if (syncBytes == null) return null;
			byte[] baseBytes = getSlot(syncBytes, 1);
			if (baseBytes == null || baseBytes.length == 0) return null;
			return baseBytes;
		} finally {
			endOperation();
		}
	}

	/**
	 * Set the base bytes for the given resource. The provided bytes
	 * should encode enough information to uniquely identify
	 * (and possibly recreate) the resource variant that is the base
	 * for the given local resource. In essence, setting the base
	 * bytes is equivalent to marking the file as in-sync. As such,
	 * setting the base bytes will also set the remote bytes and mark
	 * the file as clean (i.e. having no outgoing changes).
	 *
	 * @param resource the resource
	 * @param baseBytes the base bytes that identify the base resource variant
	 * @throws TeamException if an error occurs
	 */
	public void setBaseBytes(IResource resource, byte[] baseBytes) throws TeamException {
		Assert.isNotNull(baseBytes);
		ISchedulingRule rule = null;
		try {
			rule = beginBatching(resource, null);
			try {
				beginOperation();
				String base = new String(baseBytes);
				String[] slots = new String[] {
						Long.valueOf(resource.getModificationStamp()).toString(),
						base,
						base
				};
				byte[] syncBytes = toBytes(slots);
				internalSetSyncBytes(resource, syncBytes);
				batchingLock.resourceChanged(resource);
			} finally {
				endOperation();
			}
		} finally {
			if (rule != null) endBatching(rule, null);
		}
	}

	/**
	 * Return whether the local resource has been modified since the last time
	 * the base bytes were set. This method will return <code>false</code>
	 * for ignored resources and <code>true</code> for non-existant resources
	 * that have base bytes cached.
	 * @param resource the resource
	 * @return <code>true</code> if the resource has been modified since the
	 * last time the base bytes were set.
	 * @throws TeamException if an error occurs
	 */
	public boolean isLocallyModified(IResource resource) throws TeamException {
		return ((internalGetSyncBytes(resource) == null && ! isIgnored(resource)) ||
				(getLocalTimestamp(resource) != resource.getModificationStamp()) ||
				(getBaseBytes(resource) != null && !resource.exists()));
	}

	/**
	 * Return the remote bytes that are cached for the given resource
	 * or <code>null</code> if no remote is cached. The returned bytes
	 * should uniquely identify the resource variant that is the remote
	 * for the given local resource.
	 *
	 * @param resource the resource
	 * @return the remote bytes cached with the resource or <code>null</code>
	 * @throws TeamException if an error occurs
	 */
	public byte[] getRemoteBytes(IResource resource) throws TeamException {
		try {
			beginOperation();
			byte[] syncBytes = internalGetSyncBytes(resource);
			if (syncBytes == null) return null;
			byte[] remoteBytes = getSlot(syncBytes, 2);
			if (remoteBytes == null || remoteBytes.length == 0) return null;
			return remoteBytes;
		} finally {
			endOperation();
		}
	}

	/**
	 * Set the remote bytes for the given resource. The provided bytes
	 * should encode enough information to uniquely identify
	 * (and possibly recreate) the resource variant that is the remote
	 * for the given local resource. If the remote for a resource
	 * no longer exists, <code>removeRemoteBytes(IResource)</code>
	 * should be called.
	 *
	 * @param resource the resource
	 * @param remoteBytes the base bytes that identify the remote resource variant
	 * @return <code>true</code> if the remote bytes changed as a result of the set
	 * @throws TeamException if an error occurs
	 */
	public boolean setRemoteBytes(IResource resource, byte[] remoteBytes) throws TeamException {
		Assert.isNotNull(remoteBytes);
		ISchedulingRule rule = null;
		try {
			rule = beginBatching(resource, null);
			try {
				beginOperation();
				byte[] syncBytes = internalGetSyncBytes(resource);
				if (syncBytes == null) {
					String[] slots = new String[] {
							"", //$NON-NLS-1$
							"", //$NON-NLS-1$
							new String(remoteBytes)
					};
					syncBytes = toBytes(slots);
				} else {
					byte[] currentRemote = getSlot(syncBytes, 2);
					if (equals(remoteBytes, currentRemote)) return false;
					syncBytes = setSlot(syncBytes, 2, remoteBytes);
				}
				internalSetSyncBytes(resource, syncBytes);
				batchingLock.resourceChanged(resource);
				return true;
			} finally {
				endOperation();
			}
		} finally {
			if (rule != null) endBatching(rule, null);
		}
	}

	/**
	 * Remove the remote bytes associated with the resource. This is typically
	 * done when the corresponding remote resource variant no longer exists.
	 * @param resource the resource
	 * @return <code>true</code> if the remote bytes changed as a result of the removal
	 * @throws TeamException if an error occurs
	 */
	public boolean removeRemoteBytes(IResource resource) throws TeamException {
		ISchedulingRule rule = null;
		try {
			rule = beginBatching(resource, null);
			try {
				beginOperation();
				byte[] syncBytes = internalGetSyncBytes(resource);
				if (syncBytes != null) {
					String currentRemote = new String(getSlot(syncBytes, 2));
					if (currentRemote.length() == 0) return false;
					syncBytes = setSlot(syncBytes, 2, new byte[0]);
					internalSetSyncBytes(resource, syncBytes);
					batchingLock.resourceChanged(resource);
					return true;
				}
				return false;
			} finally {
				endOperation();
			}
		} finally {
			if (rule != null) endBatching(rule, null);
		}
	}

	/**
	 * Return whether the given resource has sync bytes in the synchronizer.
	 * @param resource the local resource
	 * @return whether there are sync bytes cached for the local resources.
	 * @throws TeamException if an error occurs
	 */
	public boolean hasSyncBytes(IResource resource) throws TeamException {
		return internalGetSyncBytes(resource) != null;
	}

	/**
	 * Returns whether the resource has been marked as ignored
	 * using <code>setIgnored(IResource)</code>.
	 * @param resource the resource
	 * @return <code>true</code> if the resource is ignored.
	 * @throws TeamException if an error occurs
	 */
	public boolean isIgnored(IResource resource) throws TeamException {
		byte[] bytes = cache.getBytes(resource);
		return (bytes != null && equals(bytes, IGNORED_BYTES));
	}

	/**
	 * Mark the resource as being ignored. Ignored resources
	 * are not returned by the <code>members</code> method,
	 * are never dirty (see <code>isLocallyModified</code>) and
	 * do not have base or remote bytes cached for them.
	 * @param resource the resource to be ignored
	 * @throws TeamException if an error occurs
	 */
	public void setIgnored(IResource resource) throws TeamException {
		internalSetSyncBytes(resource, IGNORED_BYTES);
	}

	/**
	 * Return the members of the local resource that either have sync bytes
	 * or exist locally and are not ignored.
	 * @param resource the local resource
	 * @return the children of the local resource that have cached sync bytes
	 * or are not ignored
	 * @throws TeamException if an error occurs
	 */
	public IResource[] members(IResource resource) throws TeamException {
		if (resource.getType() == IResource.FILE) {
			return new IResource[0];
		}
		try {
			Set<IResource> potentialChildren = new HashSet<>();
			IContainer container = (IContainer)resource;
			if (container.exists()) {
				potentialChildren.addAll(Arrays.asList(container.members()));
			}
			potentialChildren.addAll(Arrays.asList(cache.members(resource)));
			List<IResource> result = new ArrayList<>();
			for (IResource child : potentialChildren) {
				if (child.exists() || hasSyncBytes(child)) {
					result.add(child);
				}
			}
			return result.toArray(new IResource[result.size()]);
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		}
	}

	/**
	 * Flush any cached bytes for the given resource to the depth specified.
	 * @param resource the resource
	 * @param depth the depth of the flush (one of <code>IResource.DEPTH_ZERO</code>,
	 * <code>IResource.DEPTH_ONE</code>, or <code>IResource.DEPTH_INFINITE</code>)
	 * @throws TeamException if an error occurs
	 */
	public void flush(IResource resource, int depth) throws TeamException {
		ISchedulingRule rule = null;
		try {
			rule = beginBatching(resource, null);
			try {
				beginOperation();
				if (cache.flushBytes(resource, depth)) {
					batchingLock.resourceChanged(resource);
				}
			} finally {
				endOperation();
			}
		} finally {
			if (rule != null) endBatching(rule, null);
		}
	}

	/**
	 * Perform multiple sync state modifications and fire only a single change notification
	 * at the end.
	 * @param resourceRule the scheduling rule that encompasses all modifications
	 * @param runnable the runnable that performs the sync state modifications
	 * @param monitor a progress monitor
	 * @throws TeamException if an error occurs
	 */
	public void run(IResource resourceRule, IWorkspaceRunnable runnable, IProgressMonitor monitor) throws TeamException {
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(null, 100);
		ISchedulingRule rule = beginBatching(resourceRule, Policy.subMonitorFor(monitor, 10));
		try {
			cache.run(resourceRule, runnable, Policy.subMonitorFor(monitor, 80));
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		} finally {
			if (rule != null) endBatching(rule, Policy.subMonitorFor(monitor, 10));
			monitor.done();
		}
	}

	private void broadcastSyncChanges(final IResource[] resources) {
		ISynchronizerChangeListener[] allListeners;
		// Copy the listener list so we're not calling client code while synchronized
		synchronized(listeners) {
			allListeners = listeners.toArray(new ISynchronizerChangeListener[listeners.size()]);
		}
		// Notify the listeners safely so all will receive notification
		for (ISynchronizerChangeListener listener : allListeners) {
			SafeRunner.run(new ISafeRunnable() {
				@Override
				public void handleException(Throwable exception) {
					// don't log the exception....it is already being logged in Platform#run
				}
				@Override
				public void run() throws Exception {
					listener.syncStateChanged(resources);

				}
			});
		}
	}

	/*
	 * Return the cached sync bytes for the given resource.
	 * The value <code>null</code> is returned if there is no
	 * cached bytes or if the resource is ignored.
	 */
	private byte[] internalGetSyncBytes(IResource resource) throws TeamException {
		byte[] bytes = cache.getBytes(resource);
		if (bytes != null && equals(bytes, IGNORED_BYTES)) return null;
		return bytes;
	}

	/*
	 * Set the cached sync bytes
	 */
	private boolean internalSetSyncBytes(IResource resource, byte[] syncBytes) throws TeamException {
		return cache.setBytes(resource, syncBytes);
	}

	private byte[] getSlot(byte[] syncBytes, int i) {
		return SyncByteConverter.getSlot(syncBytes, i, false);
	}

	private byte[] setSlot(byte[] syncBytes, int i, byte[] insertBytes) throws TeamException {
		return SyncByteConverter.setSlot(syncBytes, i, insertBytes);
	}

	private byte[] toBytes(String[] slots) {
		return SyncByteConverter.toBytes(slots);
	}

	private long getLocalTimestamp(IResource resource) throws TeamException {
		try {
			beginOperation();
			byte[] syncBytes = internalGetSyncBytes(resource);
			if (syncBytes == null) return -1;
			byte[] bytes = getSlot(syncBytes, 0);
			if (bytes == null || bytes.length == 0) return -1;
			return Long.parseLong(new String(bytes));
		} finally {
			endOperation();
		}
	}

	private boolean equals(byte[] syncBytes, byte[] oldBytes) {
		if (syncBytes.length != oldBytes.length) return false;
		for (int i = 0; i < oldBytes.length; i++) {
			if (oldBytes[i] != syncBytes[i]) return false;
		}
		return true;
	}

	/*
	 * Begin an access to the internal data structures of the synchronizer
	 */
	private void beginOperation() {
		// Do not try to acquire the lock if the resources tree is locked
		// The reason for this is that during the resource delta phase (i.e. when the tree is locked)
		// the workspace lock is held. If we obtain our lock, there is
		// a chance of deadlock. It is OK if we don't as we are still protected
		// by scheduling rules and the workspace lock.
		if (ResourcesPlugin.getWorkspace().isTreeLocked()) return;
		lock.acquire();
	}

	/*
	 * End an access to the internal data structures of the synchronizer
	 */
	private void endOperation() {
		// See beginOperation() for a description of why the lock is not obtained when the tree is locked
		if (ResourcesPlugin.getWorkspace().isTreeLocked()) return;
		lock.release();
	}

	/*
	 * Begins a batch of operations in order to batch event changes.
	 * The provided scheduling rule indicates the resources
	 * that the resources affected by the operation while the returned scheduling rule
	 * is the rule obtained by the lock. It may differ from the provided rule.
	 */
	private ISchedulingRule beginBatching(ISchedulingRule resourceRule, IProgressMonitor monitor) {
		return batchingLock.acquire(resourceRule, flushOperation /* IFlushOperation */, monitor);
	}

	/*
	 * Ends a batch of operations. The provided rule must be the one that was returned
	 * by the corresponding call to beginBatching.
	 */
	private void endBatching(ISchedulingRule rule, IProgressMonitor monitor) throws TeamException {
		batchingLock.release(rule, monitor);
	}
}
