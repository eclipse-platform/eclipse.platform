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
package org.eclipse.team.core.subscribers;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.internal.core.Messages;
import org.eclipse.team.internal.core.Policy;
import org.eclipse.team.internal.core.TeamPlugin;

/**
 * A resource mapping context that provides the client access to the remote state
 * of local resources using a subscriber. It uses a <code>SyncInfoFilter</code>
 * to determine whether the local contents differ from the remote contents.
 * This allows the context to be used for different operations (check-in,
 * update and replace).
 * @since 3.2
 */
public class SubscriberResourceMappingContext extends RemoteResourceMappingContext {

	private final Subscriber subscriber;

	// Lists used to keep track of resources that have been refreshed
	private Set<IResource> shallowRefresh = new HashSet<>();
	private Set<IResource> deepRefresh = new HashSet<>();
	private boolean autoRefresh;

	/**
	 * Return a resource mapping context suitable for comparison operations.
	 * Comparisons require that any out-of-sync resources have contents
	 * that differ.
	 * @param subscriber the subscriber
	 * @return a resource mapping context suitable for compare operations
	 */
	public static RemoteResourceMappingContext createContext(Subscriber subscriber) {
		return new SubscriberResourceMappingContext(subscriber, true);
	}

	/**
	 * Create a resource mapping context for the given subscriber
	 * @param subscriber the subscriber
	 * from the local contents
	 * @param autoRefresh whether the context should auto-refresh when queried
	 */
	public SubscriberResourceMappingContext(Subscriber subscriber, boolean autoRefresh) {
		this.subscriber = subscriber;
		this.autoRefresh = autoRefresh;
	}

	@Override
	public final boolean hasRemoteChange(IResource resource, IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask(null, 100);
			ensureRefreshed(resource, IResource.DEPTH_ONE, NONE, monitor);
			SyncInfo syncInfo = subscriber.getSyncInfo(resource);
			validateRemote(resource, syncInfo);
			if (syncInfo == null) return false;
			int direction = SyncInfo.getDirection(syncInfo.getKind());
			return direction == SyncInfo.INCOMING || direction == SyncInfo.CONFLICTING;
		} finally {
			monitor.done();
		}
	}

	@Override
	public boolean hasLocalChange(IResource resource, IProgressMonitor monitor) throws CoreException {
		SyncInfo syncInfo = subscriber.getSyncInfo(resource);
		if (syncInfo == null) return false;
		int direction = SyncInfo.getDirection(syncInfo.getKind());
		return direction == SyncInfo.OUTGOING || direction == SyncInfo.CONFLICTING;
	}

	@Override
	public final IStorage fetchRemoteContents(IFile file, IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask(null, 100);
			ensureRefreshed(file, IResource.DEPTH_ZERO, FILE_CONTENTS_REQUIRED, Policy.subMonitorFor(monitor, 10));
			SyncInfo syncInfo = subscriber.getSyncInfo(file);
			IResourceVariant remote = validateRemote(file, syncInfo);
			if (remote == null) {
				return null;
			}
			return remote.getStorage(Policy.subMonitorFor(monitor, 90));
		} finally {
			monitor.done();
		}
	}

	@Override
	public final IStorage fetchBaseContents(IFile file, IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask(null, 100);
			ensureRefreshed(file, IResource.DEPTH_ZERO, FILE_CONTENTS_REQUIRED, Policy.subMonitorFor(monitor, 10));
			SyncInfo syncInfo = subscriber.getSyncInfo(file);
			IResourceVariant base = validateBase(file, syncInfo);
			if (base == null) {
				return null;
			}
			return base.getStorage(Policy.subMonitorFor(monitor, 90));
		} finally {
			monitor.done();
		}
	}

	@Override
	public final IResource[] fetchMembers(IContainer container, IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask(null, 100);
			ensureRefreshed(container, IResource.DEPTH_ONE, NONE, Policy.subMonitorFor(monitor, 100));
			SyncInfo syncInfo = subscriber.getSyncInfo(container);
			if (validateRemote(container, syncInfo) == null) {
				// There is no remote so return an empty array
				return new IResource[0];
			}
			return subscriber.members(container);
		} finally {
			monitor.done();
		}
	}

	@Override
	public final void refresh(ResourceTraversal[] traversals, int flags, IProgressMonitor monitor) throws CoreException {
		subscriber.refresh(traversals, monitor);
		for (ResourceTraversal traversal : traversals) {
			refreshed(traversal.getResources(), traversal.getDepth());
		}
	}

	/**
	 * Refresh the subscriber and cache the fact that the resources were refreshed by
	 * calling the <code>refreshed</code> method. The default implementation only refreshes
	 * the state and does not fetch contents in the <code>FILE_CONTENTS_REQUIRED</code>
	 * flag is passed. It is up to subclass to handle this.
	 * @param resources the resources to be refreshed
	 * @param depth the depth of the refresh
	 * @param flags the flags that indicate extra state that should be fetched
	 * @param monitor a progress monitor
	 * @throws TeamException if this method fails. Reasons include:
	 * <ul>
	 * <li>The server could not be contacted.</li>
	 * </ul>
	 */
	protected void refresh(IResource[] resources, int depth, int flags, IProgressMonitor monitor) throws TeamException {
		subscriber.refresh(resources, depth, monitor);
		refreshed(resources, depth);
	}

	/**
	 * Record the fact that the resources have been refreshed to the given depth.
	 * This is done so that accesses to refreshed resources will not need to perform
	 * another refresh.
	 * @param resources the resources that were refreshed
	 * @param depth the depth to which the resources were refreshed
	 */
	protected final void refreshed(IResource[] resources, int depth) {
		for (IResource resource : resources) {
			// Include files and depth-one folders as shallow
			if (depth == IResource.DEPTH_ONE || resource.getType() == IResource.FILE) {
				shallowRefresh.add(resource);
			} else if (depth == IResource.DEPTH_INFINITE) {
				deepRefresh.add(resource);
			}
		}
	}

	/*
	 * Ensure that the given resource has been refreshed to the specified depth
	 * since the context has been created.
	 */
	private void ensureRefreshed(IResource resource, int depth, int flags, IProgressMonitor monitor) throws TeamException {
		if (autoRefresh) {
			if (depth == IResource.DEPTH_INFINITE) {
				// If the resource or a parent was refreshed deeply, no need to do it again
				if (wasRefreshedDeeply(resource))
					return;
				// if the resource is a file, a shallow refresh is enough
				if (resource.getType() == IResource.FILE && wasRefreshedShallow(resource))
					return;
			} else {
				if (wasRefreshedShallow(resource))
					return;
			}
			refresh(new IResource[] { resource }, depth, flags, monitor);
		}
	}

	/*
	 * Look for a shallow refresh of the resource. If not there,
	 * look fir a deep refresh of a parent or a shallow refresh of the
	 * direct parent if the resource is a file.
	 */
	private boolean wasRefreshedShallow(IResource resource) {
		if  (shallowRefresh.contains(resource))
			return true;
		if (resource.getType() == IResource.FILE && shallowRefresh.contains(resource.getParent()))
			return true;
		if (wasRefreshedDeeply(resource))
			return true;
		return false;
	}

	/*
	 * Look for a deep refresh of the resource or any of it's parents
	 */
	private boolean wasRefreshedDeeply(IResource resource) {
		if (resource.getType() == IResource.ROOT)
			return false;
		if (deepRefresh.contains(resource))
			return true;
		return wasRefreshedDeeply(resource.getParent());
	}

	/*
	 * Validate that the remote resource is of the proper type and return the
	 * remote resource if it is OK. A return of null indicates that there is no remote.
	 */
	private IResourceVariant validateRemote(IResource resource, SyncInfo syncInfo) throws CoreException {
		if (syncInfo == null) return null;
		IResourceVariant remote = syncInfo.getRemote();
		if (remote == null) return null;
		return validateRemote(resource, remote);
	}

	private IResourceVariant validateRemote(IResource resource, IResourceVariant remote) throws CoreException {
		boolean containerExpected = resource.getType() != IResource.FILE;
		if (remote.isContainer() && !containerExpected) {
			throw new CoreException(new Status(IStatus.ERROR, TeamPlugin.ID, IResourceStatus.RESOURCE_WRONG_TYPE, Messages.SubscriberResourceMappingContext_0 + resource.getFullPath().toString(), null));
		} else if (!remote.isContainer() && containerExpected) {
			throw new CoreException(new Status(IStatus.ERROR, TeamPlugin.ID, IResourceStatus.RESOURCE_WRONG_TYPE, Messages.SubscriberResourceMappingContext_1 + resource.getFullPath().toString(), null));
		}
		return remote;
	}

	/*
	 * Validate that the base resource is of the proper type and return the
	 * base resource if it is OK. A return of null indicates that there is no base.
	 */
	private IResourceVariant validateBase(IResource resource, SyncInfo syncInfo) throws CoreException {
		if (syncInfo == null) return null;
		IResourceVariant base = syncInfo.getBase();
		if (base == null) return null;
		return validateRemote(resource, base);
	}

	/**
	 * Set whether the context should refresh the state of resources
	 * when their state is requested. The context keeps track of what
	 * resources were refreshed and only auto-refreshes a resource
	 * once.
	 * @param autoRefresh whether the context should refresh the state of resources
	 * when their state is requested
	 */
	public void setAutoRefresh(boolean autoRefresh) {
		this.autoRefresh = autoRefresh;
	}

	@Override
	public boolean isThreeWay() {
		return subscriber.getResourceComparator().isThreeWay();
	}

	public boolean contentDiffers(IFile file, IProgressMonitor monitor) throws CoreException {
		return hasRemoteChange(file, monitor) || hasLocalChange(file, monitor);
	}

	@Override
	public IProject[] getProjects() {
		Set<IProject> projects = new HashSet<>();
		IResource[] roots = subscriber.roots();
		for (IResource resource : roots) {
			projects.add(resource.getProject());
		}
		return projects.toArray(new IProject[projects.size()]);
	}
}
