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
package org.eclipse.team.internal.core.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IPath;

/**
 * A tree of objects keyed by path
 */
public class PathTree {

	class Node {
		Object payload;
		Set<IPath> descendantsWithPayload;
		int flags;
		public boolean isEmpty() {
			return payload == null && (descendantsWithPayload == null || descendantsWithPayload.isEmpty());
		}
		public Object getPayload() {
			return payload;
		}
		public void setPayload(Object payload) {
			this.payload = payload;
		}
		public boolean hasDescendants() {
			return descendantsWithPayload != null && !descendantsWithPayload.isEmpty();
		}
		public boolean hasFlag(int propertyBit) {
			return (flags & propertyBit) != 0;
		}
		public void setProperty(int propertyBit, boolean value) {
			if (value)
				flags |= propertyBit;
			else
				flags ^= propertyBit;
		}
		public boolean descendantHasFlag(int property) {
			if (hasDescendants()) {
				for (IPath path : descendantsWithPayload) {
					Node child = getNode(path);
					if (child.hasFlag(property)) {
						return true;
					}
				}
			}
			return false;
		}
	}

	private Map<IPath, Node> objects = new HashMap<>();

	/**
	 * Return the object at the given path or <code>null</code>
	 * if there is no object at that path
	 * @param path the path
	 * @return the object at the given path or <code>null</code>
	 */
	public synchronized Object get(IPath path) {
		Node node = getNode(path);
		if (node == null)
			return null;
		return node.getPayload();
	}

	/**
	 * Put the object at the given path. Return the
	 * previous object at that path or <code>null</code>
	 * if the path did not previously have an object.
	 * @param path the path of the object
	 * @param object the object
	 * @return the previous object at that path or <code>null</code>
	 */
	public synchronized Object put(IPath path, Object object) {
		Node node = getNode(path);
		if (node == null) {
			node = addNode(path);
		}
		Object previous = node.getPayload();
		node.setPayload(object);
		if(previous == null) {
			addToParents(path, path);
		}
		return previous;
	}

	/**
	 * Remove the object at the given path and return
	 * the removed object or <code>null</code> if no
	 * object was removed.
	 * @param path the path  to remove
	 * @return the removed object at the given path and return
	 * the removed object or <code>null</code>
	 */
	public synchronized Object remove(IPath path) {
		Node node = getNode(path);
		if (node == null)
			return null;
		Object previous = node.getPayload();
		node.setPayload(null);
		if(previous != null) {
			removeFromParents(path, path);
			if (node.isEmpty()) {
				removeNode(path);
			}
		}
		return previous;

	}

	/**
	 * Return whether the given path has children in the tree
	 * @param path
	 * @return whether there are children for the given path
	 */
	public synchronized boolean hasChildren(IPath path) {
		if (path.isEmpty()) return !objects.isEmpty();
		Node node = getNode(path);
		if (node == null)
			return false;
		return node.hasDescendants();
	}

	/**
	 * Return the paths for any children of the given path in this set.
	 * @param path the path
	 * @return the paths for any children of the given path in this set
	 */
	public synchronized IPath[] getChildren(IPath path) {
		// OPTIMIZE: could be optimized so that we don't traverse all the deep
		// children to find the immediate ones.
		Set<IPath> children = new HashSet<>();
		Node node = getNode(path);
		if (node != null) {
			Set possibleChildren = node.descendantsWithPayload;
			if(possibleChildren != null) {
				for (Object next : possibleChildren) {
					IPath descendantPath = (IPath)next;
					IPath childPath = null;
					if(descendantPath.segmentCount() == (path.segmentCount() +  1)) {
						childPath = descendantPath;
					} else if (descendantPath.segmentCount() > path.segmentCount()) {
						childPath = descendantPath.removeLastSegments(descendantPath.segmentCount() - path.segmentCount() - 1);
					}
					if (childPath != null) {
						children.add(childPath);
					}
				}
			}
		}
		return children.toArray(new IPath[children.size()]);
	}

	private boolean addToParents(IPath path, IPath parent) {
		// this flag is used to indicate if the parent was previously in the set
		boolean addedParent = false;
		if (path == parent) {
			// this is the leaf that was just added
			addedParent = true;
		} else {
			Node node = getNode(parent);
			if (node == null)
				node = addNode(parent);
			Set<IPath> children = node.descendantsWithPayload;
			if (children == null) {
				children = new HashSet<>();
				node.descendantsWithPayload = children;
				// this is a new folder in the sync set
				addedParent = true;
			}
			children.add(path);
		}
		// if the parent already existed and the resource is new, record it
		if ((parent.segmentCount() == 0 || !addToParents(path, parent.removeLastSegments(1))) && addedParent) {
			// TODO: we may not need to record the removed subtree
			// internalAddedSubtreeRoot(parent);
		}
		return addedParent;
	}

	private boolean removeFromParents(IPath path, IPath parent) {
		// this flag is used to indicate if the parent was removed from the set
		boolean removedParent = false;
		Node node = getNode(parent);
		if (node == null) {
			// this is the leaf
			removedParent = true;
		} else {
			Set children = node.descendantsWithPayload;
			if (children == null) {
				// this is the leaf
				removedParent = true;
			} else {
				children.remove(path);
				if (children.isEmpty()) {
					node.descendantsWithPayload = null;
					if (node.isEmpty())
						removeNode(parent);
					removedParent = true;
				}
			}
		}
		//	if the parent wasn't removed and the resource was, record it
		if ((parent.segmentCount() == 0 || !removeFromParents(path, parent.removeLastSegments(1))) && removedParent) {
			// TODO: may not need to record this
			//internalRemovedSubtreeRoot(parent);
		}
		return removedParent;
	}

	/**
	 * Clear all entries from the path tree.
	 */
	public synchronized void clear() {
		objects.clear();
	}

	/**
	 * Return whether the path tree is empty.
	 * @return whether the path tree is empty
	 */
	public synchronized boolean isEmpty() {
		return objects.isEmpty();
	}

	/**
	 * Return the paths in this tree that contain diffs.
	 * @return the paths in this tree that contain diffs.
	 */
	public synchronized IPath[] getPaths() {
		List<IPath> result = new ArrayList<>();
		for (IPath path : objects.keySet()) {
			Node node = getNode(path);
			if (node.getPayload() != null)
				result.add(path);
		}
		return result.toArray(new IPath[result.size()]);
	}

	/**
	 * Return all the values contained in this path tree.
	 * @return all the values in the tree
	 */
	public synchronized Collection<Object> values() {
		List<Object> result = new ArrayList<>();
		for (IPath path : objects.keySet()) {
			Node node = getNode(path);
			if (node.getPayload() != null)
				result.add(node.getPayload());
		}
		return result;
	}

	/**
	 * Return the number of nodes contained in this path tree.
	 * @return the number of nodes contained in this path tree
	 */
	public int size() {
		return values().size();
	}

	private Node getNode(IPath path) {
		return objects.get(path);
	}

	private Node addNode(IPath path) {
		Node node;
		node = new Node();
		objects.put(path, node);
		return node;
	}

	private Object removeNode(IPath path) {
		return objects.remove(path);
	}

	/**
	 * Set the property for the given path and propogate the
	 * bit to the root. The property is only set if the given path
	 * already exists in the tree.
	 * @param path the path
	 * @param property the property bit to set
	 * @param value whether the bit should be on or off
	 * @return the paths whose bit changed
	 */
	public synchronized IPath[] setPropogatedProperty(IPath path, int property, boolean value) {
		Set<IPath> changed = new HashSet<>();
		internalSetPropertyBit(path, property, value, changed);
		return changed.toArray(new IPath[changed.size()]);
	}

	private void internalSetPropertyBit(IPath path, int property, boolean value, Set<IPath> changed) {
		if (path.segmentCount() == 0)
			return;
		Node node = getNode(path);
		if (node == null)
			return;
		// No need to set it if the value hans't changed
		if (value == node.hasFlag(property))
			return;
		// Only unset the property if no descendants have the flag set
		if (!value && node.descendantHasFlag(property))
			return;
		node.setProperty(property, value);
		changed.add(path);
		internalSetPropertyBit(path.removeLastSegments(1), property, value, changed);
	}

	public synchronized boolean getProperty(IPath path, int property) {
		if (path.segmentCount() == 0)
			return false;
		Node node = getNode(path);
		if (node == null)
			return false;
		return (node.hasFlag(property));
	}

}
