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
package org.eclipse.team.core.mapping.provider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.team.core.diff.FastDiffFilter;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IDiffVisitor;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.diff.provider.DiffTree;
import org.eclipse.team.core.mapping.IResourceDiff;
import org.eclipse.team.core.mapping.IResourceDiffTree;

/**
 * Implementation of {@link IResourceDiffTree}.
 *
 * @since 3.2
 * @noextend This class is not intended to be subclassed by clients.
 */
public class ResourceDiffTree extends DiffTree implements IResourceDiffTree {

	/**
	 * Get the resource for the diff node that was obtained from an
	 * {@link IResourceDiffTree}.
	 * @param node the diff node.
	 * @return the resource for the diff node
	 */
	public static IResource getResourceFor(IDiff node) {
		if (node instanceof IResourceDiff) {
			IResourceDiff rd = (IResourceDiff) node;
			return rd.getResource();
		}
		if (node instanceof IThreeWayDiff) {
			IThreeWayDiff twd = (IThreeWayDiff) node;
			IDiff child = twd.getLocalChange();
			if (child != null)
				return getResourceFor(child);
			child = twd.getRemoteChange();
			if (child != null)
				return getResourceFor(child);
		}
		Assert.isLegal(false);
		return null;
	}

	@Override
	public IDiff getDiff(IResource resource) {
		return getDiff(resource.getFullPath());
	}

	@Override
	public IResource getResource(IDiff diff) {
		if (diff instanceof IThreeWayDiff) {
			IThreeWayDiff twd = (IThreeWayDiff) diff;
			IResourceDiff localChange = ((IResourceDiff)twd.getLocalChange());
			if (localChange != null)
				return localChange.getResource();
			return ((IResourceDiff)twd.getRemoteChange()).getResource();
		} else {
			return ((IResourceDiff)diff).getResource();
		}
	}

	@Override
	public void accept(ResourceTraversal[] traversals, IDiffVisitor visitor) {
		for (ResourceTraversal traversal : traversals) {
			IResource[] resources = traversal.getResources();
			for (IResource resource : resources) {
				accept(resource.getFullPath(), visitor, traversal.getDepth());
			}
		}
	}

	@Override
	public IDiff[] getDiffs(final ResourceTraversal[] traversals) {
		final Set<IDiff> result = new HashSet<>();
		for (ResourceTraversal traversal : traversals) {
			IResource[] resources = traversal.getResources();
			for (IResource resource : resources) {
				internalGetDiffs(resource, traversal.getDepth(), result);
			}
		}
		return result.toArray(new IDiff[result.size()]);
	}

	@Override
	public IDiff[] getDiffs(IResource resource, int depth) {
		final Set<IDiff> result = new HashSet<>();
		internalGetDiffs(resource, depth, result);
		return result.toArray(new IDiff[result.size()]);
	}

	private void internalGetDiffs(IResource resource, int depth, final Set<IDiff> result) {
		accept(resource.getFullPath(), diff -> result.add(diff), depth);
	}

	private IResource internalGetResource(IPath fullPath, boolean container) {
		if (container) {
			if (fullPath.segmentCount() == 1)
				return ResourcesPlugin.getWorkspace().getRoot().getProject(fullPath.segment(0));
			return ResourcesPlugin.getWorkspace().getRoot().getFolder(fullPath);
		}
		return ResourcesPlugin.getWorkspace().getRoot().getFile(fullPath);
	}

	@Override
	public IResource[] members(IResource resource) {
		List<IResource> result = new ArrayList<>();
		IPath[] paths = getChildren(resource.getFullPath());
		for (IPath path : paths) {
			IDiff node = getDiff(path);
			if (node == null) {
				result.add(internalGetResource(path, true));
			} else {
				result.add(getResource(node));
			}
		}
		return result.toArray(new IResource[result.size()]);
	}

	@Override
	public IResource[] getAffectedResources() {
		List<IResource> result = new ArrayList<>();
		IDiff[] nodes = getDiffs();
		for (IDiff node : nodes) {
			result.add(getResource(node));
		}
		return result.toArray(new IResource[result.size()]);
	}

	@Override
	public void add(IDiff delta) {
		Assert.isTrue(delta instanceof IResourceDiff || delta instanceof IThreeWayDiff);
		super.add(delta);
	}

	/**
	 * Remove the diff associated with the given resource from
	 * the tree.
	 * @param resource the resource
	 */
	public void remove(IResource resource) {
		remove(resource.getFullPath());
	}

	@Override
	public boolean hasMatchingDiffs(ResourceTraversal[] traversals, final FastDiffFilter filter) {
		final RuntimeException found = new RuntimeException();
		try {
			accept(traversals, delta -> {
				if (filter.select(delta)) {
					throw found;
				}
				return false;
			});
		} catch (RuntimeException e) {
			if (e == found)
				return true;
			throw e;
		}
		return false;
	}
}
