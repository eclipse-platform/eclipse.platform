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
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *******************************************************************************/
package org.eclipse.core.internal.events;

import java.util.Map;
import org.eclipse.core.internal.dtree.DeltaDataTree;
import org.eclipse.core.internal.dtree.NodeComparison;
import org.eclipse.core.internal.resources.*;
import org.eclipse.core.internal.watson.ElementTree;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * This class is used for calculating and building resource delta trees for notification
 * and build purposes.
 */
public class ResourceDeltaFactory {
	/**
	 * Singleton indicating no delta children
	 */
	protected static final ResourceDelta[] NO_CHILDREN = new ResourceDelta[0];

	/**
	 * Returns the resource delta representing the changes made between the given old and new trees,
	 * starting from the given root element.
	 * @param markerGeneration the start generation for which deltas should be computed, or -1
	 * if marker deltas should not be provided.
	 */
	public static ResourceDelta computeDelta(Workspace workspace, ElementTree oldTree, ElementTree newTree, IPath root, long markerGeneration) {
		//compute the underlying delta tree.
		ResourceComparator comparator = markerGeneration >= 0 ? ResourceComparator.getNotificationComparator() : ResourceComparator.getBuildComparator();
		newTree.immutable();
		DeltaDataTree delta = null;
		if (Path.ROOT.equals(root))
			delta = newTree.getDataTree().compareWith(oldTree.getDataTree(), comparator);
		else
			delta = newTree.getDataTree().compareWith(oldTree.getDataTree(), comparator, root);

		delta = delta.asReverseComparisonTree(comparator);
		IPath pathInTree = root.isRoot() ? Path.ROOT : root;
		IPath pathInDelta = Path.ROOT;

		// get the marker deltas for the delta info object....if needed
		Map<IPath, MarkerSet> allMarkerDeltas = null;
		if (markerGeneration >= 0)
			allMarkerDeltas = workspace.getMarkerManager().getMarkerDeltas(markerGeneration);

		//recursively walk the delta and create a tree of ResourceDelta objects.
		ResourceDeltaInfo deltaInfo = new ResourceDeltaInfo(workspace, allMarkerDeltas, comparator);
		ResourceDelta result = createDelta(workspace, delta, deltaInfo, pathInTree, pathInDelta);

		//compute node ID map and fix up moves
		deltaInfo.setNodeIDMap(computeNodeIDMap(result, new NodeIDMap()));
		result.fixMovesAndMarkers(oldTree);

		// check all the projects and if they were added and opened then tweek the flags
		// so the delta reports both.
		int segmentCount = result.getFullPath().segmentCount();
		if (segmentCount <= 1)
			checkForOpen(result, segmentCount);
		return result;
	}

	/**
	 * Checks to see if added projects were also opens and tweaks the flags
	 * accordingly. Should only be called for root and projects. Pass the segment count
	 * in since we've already calculated it before.
	 */
	protected static void checkForOpen(ResourceDelta delta, int segmentCount) {
		if (delta.getKind() == IResourceDelta.ADDED)
			if (delta.newInfo.isSet(ICoreConstants.M_OPEN))
				delta.status |= IResourceDelta.OPEN;
		// return for PROJECT
		if (segmentCount == 1)
			return;
		// recurse for ROOT
		IResourceDelta[] children = delta.children;
		for (IResourceDelta element : children)
			checkForOpen((ResourceDelta) element, 1);
	}

	/**
	 * Creates the map from node id to element id for the old and new states.
	 * Used for recognizing moves.  Returns the map.
	 */
	protected static NodeIDMap computeNodeIDMap(ResourceDelta delta, NodeIDMap nodeIDMap) {
		IResourceDelta[] children = delta.children;
		for (IResourceDelta element : children) {
			ResourceDelta child = (ResourceDelta) element;
			IPath path = child.getFullPath();
			switch (child.getKind()) {
				case IResourceDelta.ADDED :
					nodeIDMap.putNewPath(child.newInfo.getNodeId(), path);
					break;
				case IResourceDelta.REMOVED :
					nodeIDMap.putOldPath(child.oldInfo.getNodeId(), path);
					break;
				case IResourceDelta.CHANGED :
					long oldID = child.oldInfo.getNodeId();
					long newID = child.newInfo.getNodeId();
					//don't add entries to the map if nothing has changed.
					if (oldID != newID) {
						nodeIDMap.putOldPath(oldID, path);
						nodeIDMap.putNewPath(newID, path);
					}
					break;
			}
			//recurse
			computeNodeIDMap(child, nodeIDMap);
		}
		return nodeIDMap;
	}

	/**
	 * Recursively creates the tree of ResourceDelta objects rooted at
	 * the given path.
	 */
	protected static ResourceDelta createDelta(Workspace workspace, DeltaDataTree delta, ResourceDeltaInfo deltaInfo, IPath pathInTree, IPath pathInDelta) {
		// create the delta and fill it with information
		ResourceDelta result = new ResourceDelta(pathInTree, deltaInfo);

		// fill the result with information
		NodeComparison compare = (NodeComparison) delta.getData(pathInDelta);
		int comparison = compare.getUserComparison();
		result.setStatus(comparison);
		if (comparison == IResourceDelta.NO_CHANGE || Path.ROOT.equals(pathInTree)) {
			ResourceInfo info = workspace.getResourceInfo(pathInTree, true, false);
			result.setOldInfo(info);
			result.setNewInfo(info);
		} else {
			result.setOldInfo((ResourceInfo) compare.getOldData());
			result.setNewInfo((ResourceInfo) compare.getNewData());
		}
		// recurse over the children
		IPath[] childKeys = delta.getChildren(pathInDelta);
		int numChildren = childKeys.length;
		if (numChildren == 0) {
			result.setChildren(NO_CHILDREN);
		} else {
			ResourceDelta[] children = new ResourceDelta[numChildren];
			for (int i = 0; i < numChildren; i++) {
				//reuse the delta path if tree-relative and delta-relative are the same
				IPath newTreePath = pathInTree == pathInDelta ? childKeys[i] : pathInTree.append(childKeys[i].lastSegment());
				children[i] = createDelta(workspace, delta, deltaInfo, newTreePath, childKeys[i]);
			}
			result.setChildren(children);
		}

		// if this delta has children but no other changes, mark it as changed
		int status = result.status;
		if ((status & IResourceDelta.ALL_WITH_PHANTOMS) == 0 && numChildren != 0)
			result.setStatus(status |= IResourceDelta.CHANGED);

		// return the delta
		return result;
	}

	/**
	 * Returns an empty build delta describing the fact that no
	 * changes occurred in the given project.  The returned delta
	 * is not appropriate for use as a notification delta because
	 * it is rooted at a project, and does not contain marker deltas.
	 */
	public static IResourceDelta newEmptyDelta(IProject project) {
		ResourceDelta result = new ResourceDelta(project.getFullPath(), new ResourceDeltaInfo(((Workspace) project.getWorkspace()), null, ResourceComparator.getBuildComparator()));
		result.setStatus(0);
		result.setChildren(NO_CHILDREN);
		ResourceInfo info = ((Project) project).getResourceInfo(true, false);
		result.setOldInfo(info);
		result.setNewInfo(info);
		return result;
	}
}
