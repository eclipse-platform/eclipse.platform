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
package org.eclipse.team.core.synchronize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.internal.core.Messages;
import org.eclipse.team.internal.core.TeamPlugin;
import org.eclipse.team.internal.core.subscribers.SyncInfoTreeChangeEvent;

/**
 * Provides addition API for accessing the <code>SyncInfo</code> in the set through
 * their resource's hierarchical relationships.
 * <p>
 * Events fired from a <code>SyncInfoTree</code> will be instances of <code>ISyncInfoTreeChangeEvent</code>.
 * </p>
 * @see SyncInfoSet
 * @since 3.0
 */
public class SyncInfoTree extends SyncInfoSet {

	protected Map<IPath, Set<IResource>> parents = Collections.synchronizedMap(new HashMap<>());

	/**
	 * Create an empty sync info tree.
	 */
	public SyncInfoTree() {
		super();
	}

	/**
	 * Create a sync info tree containing the given sync info elements.
	 *
	 * @param infos the sync info elements
	 */
	public SyncInfoTree(SyncInfo[] infos) {
		super(infos);
		for (SyncInfo info : infos) {
			IResource local = info.getLocal();
			addToParents(local, local);
		}
	}

	/**
	 * Return whether the given resource has any children in the sync set. The children
	 * could be either out-of-sync resources that are contained by the set or containers
	 * that are ancestors of out-of-sync resources contained by the set.
	 *
	 * @param resource the resource to check for children.
	 * @return <code>true</code> if the resource has children in the set.
	 */
	public synchronized boolean hasMembers(IResource resource) {
		if (resource.getType() == IResource.FILE) return false;
		IContainer parent = (IContainer)resource;
		if (parent.getType() == IResource.ROOT) return !isEmpty();
		IPath path = parent.getFullPath();
		Set allDescendants = parents.get(path);
		return (allDescendants != null && !allDescendants.isEmpty());
	}

	/**
	 * Return the <code>SyncInfo</code> for each out-of-sync resource in the subtree
	 * rooted at the given resource to the depth specified. The depth is one of:
	 * <ul>
	 * <li><code>IResource.DEPTH_ZERO</code>: the resource only,
	 * <li><code>IResource.DEPTH_ONE</code>: the resource or its direct children,
	 * <li><code>IResource.DEPTH_INFINITE</code>: the resource and all of it's
	 * descendants.
	 * </ul>
	 * If the given resource is out of sync, it will be included in the result.
	 * <p>
	 * The default implementation makes use of <code>getSyncInfo(IResource)</code>,
	 * <code>members(IResource)</code> and <code>getSyncInfos()</code> to provide
	 * the varying depths. Subclasses may override to optimize.
	 * </p>
	 *
	 * @param resource the root of the resource subtree
	 * @param depth    the depth of the subtree
	 * @return the <code>SyncInfo</code> for any out-of-sync resources
	 */
	public synchronized SyncInfo[] getSyncInfos(IResource resource, int depth) {
		if (depth == IResource.DEPTH_ZERO || resource.getType() == IResource.FILE) {
			SyncInfo info = getSyncInfo(resource);
			if (info == null) {
				return new SyncInfo[0];
			} else {
				return new SyncInfo[] { info };
			}
		}
		if (depth == IResource.DEPTH_ONE) {
			List<SyncInfo> result = new ArrayList<>();
			SyncInfo info = getSyncInfo(resource);
			if (info != null) {
				result.add(info);
			}
			IResource[] members = members(resource);
			for (IResource member : members) {
				info = getSyncInfo(member);
				if (info != null) {
					result.add(info);
				}
			}
			return result.toArray(new SyncInfo[result.size()]);
		}
		// if it's the root then return all out of sync resources.
		if(resource.getType() == IResource.ROOT) {
			return getSyncInfos();
		}
		// for folders return all children deep.
		return internalGetDeepSyncInfo((IContainer)resource);
	}

	/*
	 * Return the <code>SyncInfo</code> for all out-of-sync resources in the
	 * set that are at or below the given resource in the resource hierarchy.
	 * @param resource the root resource
	 * @return the <code>SyncInfo</code> for all out-of-sync resources at or below the given resource
	 */
	private synchronized SyncInfo[] internalGetDeepSyncInfo(IContainer resource) {
		List<SyncInfo> infos = new ArrayList<>();
		IResource[] children = internalGetOutOfSyncDescendants(resource);
		for (IResource child : children) {
			SyncInfo info = getSyncInfo(child);
			if(info != null) {
				infos.add(info);
			} else {
				TeamPlugin.log(IStatus.INFO, Messages.SyncInfoTree_0 + child.getFullPath(), null);
			}
		}
		return infos.toArray(new SyncInfo[infos.size()]);
	}

	@Override
	protected SyncInfoSetChangeEvent createEmptyChangeEvent() {
		return new SyncInfoTreeChangeEvent(this);
	}

	@Override
	public void add(SyncInfo info) {
		try {
			beginInput();
			boolean alreadyExists = getSyncInfo(info.getLocal()) != null;
			super.add(info);
			if(! alreadyExists) {
				IResource local = info.getLocal();
				addToParents(local, local);
			}
		} finally {
			endInput(null);
		}
	}

	@Override
	public void remove(IResource resource) {
		try {
			beginInput();
			super.remove(resource);
			removeFromParents(resource, resource);
		} finally {
			endInput(null);
		}

	}

	@Override
	public void clear() {
		try {
			beginInput();
			super.clear();
			synchronized(this) {
				parents.clear();
			}
		} finally {
			endInput(null);
		}
	}

	private synchronized boolean addToParents(IResource resource, IResource parent) {
		if (parent.getType() == IResource.ROOT) {
			return false;
		}
		// this flag is used to indicate if the parent was previously in the set
		boolean addedParent = false;
		if (parent.getType() == IResource.FILE) {
			// the file is new
			addedParent = true;
		} else {
			Set<IResource> children = parents.get(parent.getFullPath());
			if (children == null) {
				children = new HashSet<>();
				parents.put(parent.getFullPath(), children);
				// this is a new folder in the sync set
				addedParent = true;
			}
			children.add(resource);
		}
		// if the parent already existed and the resource is new, record it
		if (!addToParents(resource, parent.getParent()) && addedParent) {
			internalAddedSubtreeRoot(parent);
		}
		return addedParent;
	}

	private synchronized boolean removeFromParents(IResource resource, IResource parent) {
		if (parent.getType() == IResource.ROOT) {
			return false;
		}
		// this flag is used to indicate if the parent was removed from the set
		boolean removedParent = false;
		if (parent.getType() == IResource.FILE) {
			// the file will be removed
			removedParent = true;
		} else {
			Set<IResource> children = parents.get(parent.getFullPath());
			if (children != null) {
				children.remove(resource);
				if (children.isEmpty()) {
					parents.remove(parent.getFullPath());
					removedParent = true;
				}
			}
		}
		//	if the parent wasn't removed and the resource was, record it
		if (!removeFromParents(resource, parent.getParent()) && removedParent) {
			internalRemovedSubtreeRoot(parent);
		}
		return removedParent;
	}

	private void internalAddedSubtreeRoot(IResource parent) {
		((SyncInfoTreeChangeEvent)getChangeEvent()).addedSubtreeRoot(parent);
	}

	private void internalRemovedSubtreeRoot(IResource parent) {
		((SyncInfoTreeChangeEvent)getChangeEvent()).removedSubtreeRoot(parent);
	}

	/**
	 * Remove from this set the <code>SyncInfo</code> for the given resource and any
	 * of its descendants within the specified depth. The depth is one of:
	 * <ul>
	 * <li><code>IResource.DEPTH_ZERO</code>: the resource only,
	 * <li><code>IResource.DEPTH_ONE</code>: the resource or its direct children,
	 * <li><code>IResource.DEPTH_INFINITE</code>: the resource and all of it's
	 * descendants.
	 * </ul>
	 *
	 * @param resource the root of the resource subtree
	 * @param depth    the depth of the subtree
	 */
	public void remove(IResource resource, int depth) {
		try {
			beginInput();
			if (getSyncInfo(resource) != null) {
				remove(resource);
			}
			if (depth == IResource.DEPTH_ZERO || resource.getType() == IResource.FILE) return;
			if (depth == IResource.DEPTH_ONE) {
				IResource[] members = members(resource);
				for (IResource member : members) {
					if (getSyncInfo(member) != null) {
						remove(member);
					}
				}
			} else if (depth == IResource.DEPTH_INFINITE) {
				IResource [] toRemove = internalGetOutOfSyncDescendants((IContainer)resource);
				for (IResource t : toRemove) {
					remove(t);
				}
			}
		} finally {
			endInput(null);
		}
	}

	/**
	 * This is an internal method and is not intended to be invoked or
	 * overridden by clients.
	 */
	protected synchronized IResource[] internalGetOutOfSyncDescendants(IContainer resource) {
		// The parent map contains a set of all out-of-sync children
		Set<IResource> allChildren = parents.get(resource.getFullPath());
		if (allChildren == null) return new IResource[0];
		return allChildren.toArray(new IResource[allChildren.size()]);
	}

	private synchronized IResource[] internalMembers(IWorkspaceRoot root) {
		Set possibleChildren = parents.keySet();
		Set<IResource> children = new HashSet<>();
		for (Object next : possibleChildren) {
			IResource element = root.findMember((IPath)next);
			if (element != null) {
				children.add(element.getProject());
			}
		}
		return children.toArray(new IResource[children.size()]);
	}

	/**
	 * Return the immediate children of the given resource who are either out-of-sync
	 * or contain out-of-sync resources.
	 *
	 * @param resource the parent resource
	 * @return the children of the resource that are either out-of-sync or are ancestors of
	 * out-of-sync resources contained in the set
	 */
	public synchronized IResource[] members(IResource resource) {
		if (resource.getType() == IResource.FILE) return new IResource[0];
		IContainer parent = (IContainer)resource;
		if (parent.getType() == IResource.ROOT) return internalMembers((IWorkspaceRoot)parent);
		// OPTIMIZE: could be optimized so that we don't traverse all the deep
		// children to find the immediate ones.
		Set<IResource> children = new HashSet<>();
		IPath path = parent.getFullPath();
		Set possibleChildren = parents.get(path);
		if(possibleChildren != null) {
			for (Object next : possibleChildren) {
				IResource element = (IResource)next;
				IPath childPath = element.getFullPath();
				IResource modelObject = null;
				if(childPath.segmentCount() == (path.segmentCount() +  1)) {
					modelObject = element;

				} else if (childPath.segmentCount() > path.segmentCount()) {
					IContainer childFolder = parent.getFolder(new Path(null, childPath.segment(path.segmentCount())));
					modelObject = childFolder;
				}
				if (modelObject != null) {
					children.add(modelObject);
				}
			}
		}
		return children.toArray(new IResource[children.size()]);
	}

}
