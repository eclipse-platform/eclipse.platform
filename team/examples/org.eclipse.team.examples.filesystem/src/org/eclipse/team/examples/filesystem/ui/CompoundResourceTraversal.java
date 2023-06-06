/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.examples.filesystem.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.IPath;

/**
 * Helper class that accumulates several traversals in order
 * to generate a final set of traversals and to perform certain
 * queries on a set of traversals.
 *
 * TODO: This class was copied from the Team Core plugin since it was an internal
 * class. It should probably be made API at some point.
 */
public class CompoundResourceTraversal {

	private Set<IResource> deepFolders = new HashSet<>();
	private Set<IResource> shallowFolders = new HashSet<>();
	private Set<IResource> zeroFolders = new HashSet<>();
	private Set<IResource> files = new HashSet<>();

	public void addTraversals(ResourceTraversal[] traversals) {
		for (ResourceTraversal traversal : traversals) {
			addTraversal(traversal);
		}
	}

	public void addTraversal(ResourceTraversal traversal) {
		IResource[] resources = traversal.getResources();
		for (IResource resource : resources) {
			addResource(resource, traversal.getDepth());
		}
	}

	public void addResource(IResource resource, int depth) {
		if (resource.getType() == IResource.FILE) {
			if (!isCovered(resource, IResource.DEPTH_ZERO))
				files.add(resource);
		}
		switch (depth) {
		case IResource.DEPTH_INFINITE:
			addDeepFolder(resource);
			break;
		case IResource.DEPTH_ONE:
			addShallowFolder(resource);
			break;
		case IResource.DEPTH_ZERO:
			addZeroFolder(resource);
			break;
		}
	}

	private void addShallowFolder(IResource resource) {
		if (!isCovered(resource, IResource.DEPTH_ONE)) {
			shallowFolders.add(resource);
			removeDescendants(resource, IResource.DEPTH_ONE);
		}
	}

	public boolean isCovered(IResource resource, int depth) {
		IPath fullPath = resource.getFullPath();
		// Regardless of the depth, look for a deep folder that covers the resource
		for (IResource deepFolder : deepFolders) {
			if (deepFolder.getFullPath().isPrefixOf(fullPath)) {
				return true;
			}
		}
		// For files, look in the shallow folders and files
		if (resource.getType() == IResource.FILE) {
			return (shallowFolders.contains(resource.getParent()) || files.contains(resource));
		}
		// For folders, look in appropriate sets
		switch (depth) {
		case IResource.DEPTH_ONE:
			return (shallowFolders.contains(resource));
		case IResource.DEPTH_ZERO:
			return (shallowFolders.contains(resource.getParent()) || zeroFolders.contains(resource));
		}
		return false;
	}

	private void addZeroFolder(IResource resource) {
		if (!isCovered(resource, IResource.DEPTH_ZERO))
			zeroFolders.add(resource);
	}

	private void addDeepFolder(IResource resource) {
		if (!isCovered(resource, IResource.DEPTH_INFINITE)) {
			deepFolders.add(resource);
			removeDescendants(resource, IResource.DEPTH_INFINITE);
		}
	}

	private void removeDescendants(IResource resource, int depth) {
		IPath fullPath = resource.getFullPath();
		// First, remove any files that are now covered
		for (Iterator iter = files.iterator(); iter.hasNext();) {
			IResource child = (IResource) iter.next();
			switch (depth) {
			case IResource.DEPTH_INFINITE:
				if (fullPath.isPrefixOf(child.getFullPath())) {
					iter.remove();
				}
				break;
			case IResource.DEPTH_ONE:
				if (fullPath.equals(child.getFullPath().removeLastSegments(1))) {
					iter.remove();
				}
				break;
			}
		}
		// Now, remove any shallow folders
		if (depth == IResource.DEPTH_INFINITE) {
			for (Iterator iter = shallowFolders.iterator(); iter.hasNext();) {
				IResource child = (IResource) iter.next();
				if (fullPath.isPrefixOf(child.getFullPath())) {
					iter.remove();
				}
			}
		}
		// Finally, remove any zero folders
		for (Iterator iter = zeroFolders.iterator(); iter.hasNext();) {
			IResource child = (IResource) iter.next();
			switch (depth) {
			case IResource.DEPTH_INFINITE:
				if (fullPath.isPrefixOf(child.getFullPath())) {
					iter.remove();
				}
				break;
			case IResource.DEPTH_ONE:
				// TODO: Is a zero folder covered by a shallow folder?
				if (fullPath.equals(child.getFullPath().removeLastSegments(1))) {
					iter.remove();
				}
				break;
			}
		}
	}

	public void add(CompoundResourceTraversal compoundTraversal) {
		addResources(
				compoundTraversal.deepFolders.toArray(new IResource[compoundTraversal.deepFolders.size()]),
				IResource.DEPTH_INFINITE);
		addResources(
				compoundTraversal.shallowFolders.toArray(new IResource[compoundTraversal.shallowFolders.size()]),
				IResource.DEPTH_ONE);
		addResources(
				compoundTraversal.zeroFolders.toArray(new IResource[compoundTraversal.zeroFolders.size()]),
				IResource.DEPTH_ZERO);
		addResources(
				compoundTraversal.files.toArray(new IResource[compoundTraversal.files.size()]),
				IResource.DEPTH_ZERO);
	}

	public void addResources(IResource[] resources, int depth) {
		for (IResource resource : resources) {
			addResource(resource, depth);
		}

	}

	/**
	 * Return the resources contained in the given traversals that are not covered by this traversal
	 * @param traversals the traversals being testes
	 * @return the resources contained in the given traversals that are not covered by this traversal
	 */
	public IResource[] getUncoveredResources(ResourceTraversal[] traversals) {
		CompoundResourceTraversal newTraversals = new CompoundResourceTraversal();
		newTraversals.addTraversals(traversals);
		return getUncoveredResources(newTraversals);
	}

	/*
	 * Return any resources in the other traversal that are not covered by this traversal
	 */
	private IResource[] getUncoveredResources(CompoundResourceTraversal otherTraversal) {
		Set<IResource> result = new HashSet<>();
		for (IResource resource : otherTraversal.files) {
			if (!isCovered(resource, IResource.DEPTH_ZERO)) {
				result.add(resource);
			}
		}
		for (IResource resource : otherTraversal.zeroFolders) {
			if (!isCovered(resource, IResource.DEPTH_ZERO)) {
				result.add(resource);
			}
		}
		for (IResource resource : otherTraversal.shallowFolders) {
			if (!isCovered(resource, IResource.DEPTH_ONE)) {
				result.add(resource);
			}
		}
		for (IResource resource : otherTraversal.deepFolders) {
			if (!isCovered(resource, IResource.DEPTH_INFINITE)) {
				result.add(resource);
			}
		}
		return result.toArray(new IResource[result.size()]);
	}

	public ResourceTraversal[] asTraversals() {
		List<ResourceTraversal> result = new ArrayList<>();
		if (!files.isEmpty() || ! zeroFolders.isEmpty()) {
			Set<IResource> combined = new HashSet<>();
			combined.addAll(files);
			combined.addAll(zeroFolders);
			result.add(new ResourceTraversal(combined.toArray(new IResource[combined.size()]), IResource.DEPTH_ZERO, IResource.NONE));
		}
		if (!shallowFolders.isEmpty()) {
			result.add(new ResourceTraversal(shallowFolders.toArray(new IResource[shallowFolders.size()]), IResource.DEPTH_ONE, IResource.NONE));
		}
		if (!deepFolders.isEmpty()) {
			result.add(new ResourceTraversal(deepFolders.toArray(new IResource[deepFolders.size()]), IResource.DEPTH_INFINITE, IResource.NONE));
		}
		return result.toArray(new ResourceTraversal[result.size()]);
	}

	public IResource[] getRoots() {
		List<IResource> result = new ArrayList<>();
		result.addAll(files);
		result.addAll(zeroFolders);
		result.addAll(shallowFolders);
		result.addAll(deepFolders);
		return result.toArray(new IResource[result.size()]);
	}

	public ResourceTraversal[] getUncoveredTraversals(ResourceTraversal[] traversals) {
		CompoundResourceTraversal other = new CompoundResourceTraversal();
		other.addTraversals(traversals);
		return getUncoveredTraversals(other);
	}

	public ResourceTraversal[] getUncoveredTraversals(CompoundResourceTraversal otherTraversal) {
		CompoundResourceTraversal uncovered = new CompoundResourceTraversal();
		for (IResource resource : otherTraversal.files) {
			if (!isCovered(resource, IResource.DEPTH_ZERO)) {
				uncovered.addResource(resource, IResource.DEPTH_ZERO);
			}
		}
		for (IResource resource : otherTraversal.zeroFolders) {
			if (!isCovered(resource, IResource.DEPTH_ZERO)) {
				uncovered.addResource(resource, IResource.DEPTH_ZERO);
			}
		}
		for (IResource resource : otherTraversal.shallowFolders) {
			if (!isCovered(resource, IResource.DEPTH_ONE)) {
				uncovered.addResource(resource, IResource.DEPTH_ONE);
			}
		}
		for (IResource resource : otherTraversal.deepFolders) {
			if (!isCovered(resource, IResource.DEPTH_INFINITE)) {
				uncovered.addResource(resource, IResource.DEPTH_INFINITE);
			}
		}
		return uncovered.asTraversals();
	}

	public void clear() {
		deepFolders.clear();
		shallowFolders.clear();
		zeroFolders.clear();
		files.clear();
	}

}
