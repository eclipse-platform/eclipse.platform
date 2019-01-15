/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.team.core.mapping;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.team.core.diff.FastDiffFilter;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IDiffTree;
import org.eclipse.team.core.diff.IDiffVisitor;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;

/**
 * A resource diff tree provides access to a tree of {@link IDiff} instances
 * that either contain {@link IResourceDiff} nodes or {@link IThreeWayDiff}
 * nodes that contain {@link IResourceDiff} nodes as the local and remote
 * changes. For efficiency reasons, the tree only provides diffs for resources
 * that have changes. Resources that do not contain a change but are returned
 * from the tree will contain children in the set.
 *
 * @since 3.2
 * @noimplement This interface is not intended to be implemented by clients.
 *              Clients can use {@link ResourceDiffTree} instead.
 */
public interface IResourceDiffTree extends IDiffTree {

	/**
	 * Return the diff for the given resource. This method is a convenience
	 * method that uses the path of the resource to access the diff.
	 *
	 * @param resource a resource
	 * @return the diff associated with the resource or <code>null</code> if
	 *         the resource does not have any changes.
	 */
	IDiff getDiff(IResource resource);

	/**
	 * Return the resource associated with the given diff. This method will only
	 * return meaningful results for diffs which were obtained from this tree.
	 *
	 * @param diff a diff
	 * @return the resource associated with the given diff
	 */
	IResource getResource(IDiff diff);

	/**
	 * Visit all diffs in this tree that are covered by the given traversals.
	 * @param traversals the set of traversals whose diffs are to be visited
	 * @param visitor a diff visitor
	 *
	 */
	void accept(ResourceTraversal[] traversals, IDiffVisitor visitor);

	/**
	 * Return all the diffs in the tree that are contained in the given
	 * traversals.
	 *
	 * @param traversals the traversals
	 * @return all the diffs in the tree that are contained in the given
	 *         traversals
	 */
	IDiff[] getDiffs(ResourceTraversal[] traversals);

	/**
	 * Return all the diffs in the tree that are found for
	 * the given resource when traversed to the given depth.
	 *
	 * @param resource the resource
	 * @param depth the depth
	 * @return all the diffs in the tree that are found for
	 * the given resource when traversed to the given depth
	 */
	IDiff[] getDiffs(IResource resource, int depth);

	/**
	 * Return the members of the given resource that either have diffs in this
	 * tree of contain descendants that have diffs in this tree.
	 *
	 * @param resource a resource
	 * @return the members of the given resource that either have diffs in this
	 *         tree of contain descendants that have diffs in this tree
	 */
	IResource[] members(IResource resource);

	/**
	 * Return all resources that contain diffs in this
	 * diff tree.
	 * @return all resources that contain diffs in this
	 * diff tree
	 */
	IResource[] getAffectedResources();

	/**
	 * Return whether the this diff tree contains any diffs that match the given filter
	 * within the given traversals.
	 * @param traversals the traversals
	 * @param filter the diff node filter
	 * @return whether the given diff tree contains any deltas that match the given filter
	 */
	public boolean hasMatchingDiffs(ResourceTraversal[] traversals, final FastDiffFilter filter);

}
