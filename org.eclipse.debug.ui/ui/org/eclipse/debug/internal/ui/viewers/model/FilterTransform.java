/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
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

package org.eclipse.debug.internal.ui.viewers.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.TreePath;


/**
 * Helper class to support filtering in virtual tree viewer.
 * Translates indexes from viewer to model coordinate space (and visa versa).
 * <p>
 * This filter transform maintains a tree representing filtered elements in the
 * viewer. The filtering is performed dynamically as elements are 'replaced' in the tree
 * by a lazy tree content provider.
 * </p>
 * <p>
 * This class not intended to be subclassed or instantiated. For internal use only.
 * </p>
 * @since 3.3
 */
public class FilterTransform {

	private Node root = new Node();

	class Node {
		private int[] filteredIndexes = null;
		private Object[] filteredElements = null;
		private Map<Object, Node> children = null; // only set for parent nodes,
													// indexed by child

		Node() {
		}

		boolean addFilter(TreePath path, int childIndex, int pathIndex, Object filtered) {
			if (pathIndex == path.getSegmentCount()) {
				if (filteredIndexes == null) {
					filteredIndexes = new int[]{childIndex};
					filteredElements = new Object[]{filtered};
					return true;
				}
				int location = Arrays.binarySearch(filteredIndexes, childIndex);
				if (location >= 0) {
					return false;
				}
				location = 0 - (location + 1);
				int[] next = new int[filteredIndexes.length + 1];
				Object[] filt = new Object[next.length];
				if (location == 0) {
					next[0] = childIndex;
					filt[0] = filtered;
					System.arraycopy(filteredIndexes, 0, next, 1, filteredIndexes.length);
					System.arraycopy(filteredElements, 0, filt, 1, filteredElements.length);
				} else if (location == filteredIndexes.length) {
					next[filteredIndexes.length] = childIndex;
					filt[filteredElements.length] = filtered;
					System.arraycopy(filteredIndexes, 0, next, 0, filteredIndexes.length);
					System.arraycopy(filteredElements, 0, filt, 0, filteredElements.length);
				} else {
					System.arraycopy(filteredIndexes, 0, next, 0, location);
					System.arraycopy(filteredElements, 0, filt, 0, location);
					next[location] = childIndex;
					filt[location] = filtered;
					System.arraycopy(filteredIndexes, location, next, location + 1, filteredIndexes.length - location);
					System.arraycopy(filteredElements, location, filt, location + 1, filteredElements.length - location);
				}
				filteredIndexes = next;
				filteredElements = filt;
				return true;
			}

			if (children == null) {
				children = new HashMap<>();
			}
			Object element = path.getSegment(pathIndex);
			Node node = children.get(element);
			if (node == null) {
				node = new Node();
				children.put(element, node);
			}
			return node.addFilter(path, childIndex, pathIndex + 1, filtered);
		}

		boolean clear(TreePath path, int pathIndex) {
			if (pathIndex == path.getSegmentCount()) {
				return true;
			}
			if (children == null) {
				return false;
			}
			Object child = path.getSegment(pathIndex);
			Node node = children.get(child);
			if (node != null) {
				if (node.clear(path, pathIndex + 1)) {
					children.remove(child);
				}
			}
			return children.isEmpty() && (filteredIndexes == null || filteredIndexes.length == 0);
		}

		boolean clear(TreePath path, int childIndex, int pathIndex) {
			if (pathIndex == path.getSegmentCount()) {
				if (filteredIndexes != null) {
					int location = Arrays.binarySearch(filteredIndexes, childIndex);
					if (location >= 0) {
						// remove it
						if (location == 0 && filteredIndexes.length == 1) {
							filteredIndexes = null;
							filteredElements = null;
							return true;
						}
						int[] next = new int[filteredIndexes.length - 1];
						Object[] filt = new Object[next.length];
						if (location == 0) {
							System.arraycopy(filteredIndexes, 1, next, 0, next.length);
							System.arraycopy(filteredElements, 1, filt, 0, filt.length);
						} else if (location == (filteredIndexes.length - 1)) {
							System.arraycopy(filteredIndexes, 0, next, 0, location);
							System.arraycopy(filteredElements, 0, filt, 0, location);
						} else {
							System.arraycopy(filteredIndexes, 0, next, 0, location);
							System.arraycopy(filteredElements, 0, filt, 0, location);
							System.arraycopy(filteredIndexes, location + 1, next, location, next.length - location);
							System.arraycopy(filteredElements, location + 1, filt, location, filt.length - location);
						}
						filteredIndexes = next;
						filteredElements = filt;
						return false;
					}
				} else {
					return false;
				}
			}
			if (children == null) {
				return false;
			}
			Object element = path.getSegment(pathIndex);
			Node node = children.get(element);
			if (node == null) {
				return false;
			}
			boolean remove = node.clear(path, childIndex, pathIndex + 1);
			if (remove) {
				children.remove(element);
				return filteredIndexes == null && children.isEmpty();
			} else {
				return false;
			}
		}

		Node find(TreePath path, int pathIndex) {
			if (pathIndex == path.getSegmentCount()) {
				return this;
			}
			if (children == null) {
				return null;
			}
			Object child = path.getSegment(pathIndex);
			Node node = children.get(child);
			if (node != null) {
				return node.find(path, pathIndex + 1);
			}
			return null;
		}

		int viewToModel(int childIndex) {
			if (filteredIndexes == null) {
				return childIndex;
			}
			// If there are filtered children, then we want to find the
			// (n+1)th missing number in the list of filtered indexes (missing
			// entries are visible in the view). For example, if the request
			// has asked for the model index corresponding to the 4th viewer
			// index, then we want to find the 5th missing number in the
			// filtered index sequence.

			int count = -1; // count from 0, 1, 2...
			int missingNumbers = 0; // how many numbers missing from the filtered index
			int offset = 0; // offset into the filtered index

			while (missingNumbers < (childIndex + 1)) {
				count++;
				if (offset < filteredIndexes.length) {
					if (filteredIndexes[offset] == count) {
						// not missing
						offset++;
					} else {
						// missing
						missingNumbers++;
					}
				} else {
					missingNumbers++;
				}
			}
			return count;
		}

		int modelToView(int childIndex) {
			if (filteredIndexes == null) {
				return childIndex;
			}
			int offset = 0;
			for (int filteredIndex : filteredIndexes) {
				if (childIndex == filteredIndex ) {
					return -1;
				} else if (childIndex > filteredIndex) {
					offset++;
				} else {
					break;
				}
			}
			return childIndex - offset;
		}

		int modelToViewCount(int childCount) {
			if (filteredIndexes == null) {
				return childCount;
			}
			return childCount - filteredIndexes.length;
		}

		boolean isFiltered(int index) {
			if (filteredIndexes != null) {
				int location = Arrays.binarySearch(filteredIndexes, index);
				return location >= 0;
			}
			return false;
		}

		int indexOfFilteredElement(Object element) {
			if (filteredElements != null) {
				for (int i = 0; i < filteredElements.length; i++) {
					if (element.equals(filteredElements[i])) {
						return filteredIndexes[i];
					}
				}
			}
			return -1;
		}

		/**
		 * Sets the child count for this element, trimming any filtered elements
		 * that were above this count.
		 *
		 * @param childCount new child count
		 */
		void setModelChildCount(int childCount) {
			if (filteredIndexes != null) {
				for (int i = 0; i < filteredIndexes.length; i++) {
					if (filteredIndexes[i] >= childCount) {
						// trim
						if (i == 0) {
							filteredIndexes = null;
							// bug 200325 - filteredElements should have the same length
							// as filteredIndexes
							filteredElements = null;
							return;
						} else {
							int[] temp = new int[i + 1];
							System.arraycopy(filteredIndexes, 0, temp, 0, temp.length);
							filteredIndexes = temp;
							// bug 200325 - filteredElements should have the same length
							// as filteredIndexes
							Object[] temp2 = new Object[i + 1];
							System.arraycopy(filteredElements, 0, temp2, 0, temp2.length);
							filteredElements = temp2;
						}
					}
				}
			}
		}

		/**
		 * Updates filter index for a removed element at the given index
		 *
		 * @param index index at which an element was removed
		 */
		void removeElementFromFilters(int index) {
			if (filteredIndexes != null) {
				int location = Arrays.binarySearch(filteredIndexes, index);
				if (location >= 0) {
					// remove a filtered item
					if (filteredIndexes.length == 1) {
						// only filtered item
						filteredIndexes = null;
						filteredElements = null;
					} else {
						int[] next = new int[filteredIndexes.length - 1];
						Object[] filt = new Object[next.length];
						if (location == 0) {
							// first
							System.arraycopy(filteredIndexes, 1, next, 0, next.length);
							System.arraycopy(filteredElements, 1, filt, 0, filt.length);
						} else if (location == (filteredIndexes.length - 1)) {
							// last
							System.arraycopy(filteredIndexes, 0, next, 0, next.length);
							System.arraycopy(filteredElements, 0, filt, 0, filt.length);
						} else {
							// middle
							System.arraycopy(filteredIndexes, 0, next, 0, location);
							System.arraycopy(filteredElements, 0, filt, 0, location);
							System.arraycopy(filteredIndexes, location + 1, next, location, next.length - location);
							System.arraycopy(filteredElements, location + 1, filt, location, filt.length - location);
						}
						filteredIndexes = next;
						filteredElements = filt;
					}
				} else {
					location = 0 - (location + 1);
				}
				if (filteredIndexes != null) {
					// decrement remaining indexes
					for (int i = location; i < filteredIndexes.length; i ++) {
						filteredIndexes[i]--;
					}
				}
			}
		}
	}

	/**
	 * Filters the specified child of the given parent and returns
	 * whether the filter was added.
	 *
	 * @param parentPath path to parent element
	 * @param childIndex index of filtered child relative to parent (in model coordinates)
	 * @param element the filtered element
	 * @return whether the filter was added - returns <code>true</code> if the filter is
	 *  added, and <code>false</code> if the index was already filtered
	 */
	public boolean addFilteredIndex(TreePath parentPath, int childIndex, Object element) {
		return root.addFilter(parentPath, childIndex, 0, element);
	}

	/**
	 * Clears all filtered elements.
	 */
	public void clear() {
		root = new Node();
	}

	/**
	 * Clears all filters in the subtree of the given element.
	 *
	 * @param path element path
	 */
	public void clear(TreePath path) {
		root.clear(path, 0);
	}

	/**
	 * Clears the given filtered index of the specified parent. I.e.
	 * the child still exists, but is no longer filtered.
	 *
	 * @param parentPath parent path
	 * @param index index to clear
	 */
	public void clear(TreePath parentPath, int index) {
		root.clear(parentPath, index, 0);
	}

	public int indexOfFilteredElement(TreePath parentPath, Object element) {
		Node parentNode = root.find(parentPath, 0);
		if (parentNode == null) {
			return -1;
		}
		return parentNode.indexOfFilteredElement(element);
	}

	/**
	 * Translates and returns the given model index (raw index) into
	 * a view index (filtered index), or -1 if filtered.
	 *
	 * @param parentPath path to parent element
	 * @param childIndex index of child element in model space
	 * @return the given index in view coordinates, or -1 if filtered.
	 */
	public int modelToViewIndex(TreePath parentPath, int childIndex) {
		Node parentNode = root.find(parentPath, 0);
		if (parentNode == null) {
			return childIndex;
		}
		return parentNode.modelToView(childIndex);
	}

	/**
	 * Translates and returns the given view index (filtered) into
	 * a model index (raw index).
	 *
	 * @param parentPath path to parent element
	 * @param childIndex index of child element in view space
	 * @return the given index in model coordinates
	 */
	public int viewToModelIndex(TreePath parentPath, int childIndex) {
		Node parentNode = root.find(parentPath, 0);
		if (parentNode == null) {
			return childIndex;
		}
		return parentNode.viewToModel(childIndex);
	}

	/**
	 * Returns the number of children for the given parent, in the model.
	 *
	 * @param parentPath path to parent element
	 * @param viewCount number of children in the view
	 * @return number of children in the model
	 */
	public int viewToModelCount(TreePath parentPath, int viewCount) {
		Node parentNode = root.find(parentPath, 0);
		if (parentNode != null) {
			if (parentNode.filteredIndexes != null) {
				return viewCount + parentNode.filteredIndexes.length;
			}
		}
		return viewCount;
	}

	/**
	 * Translates and returns the given model child count (raw) into
	 * a view count (filtered).
	 *
	 * @param parentPath path to parent element
	 * @param count child count in model space
	 * @return the given count in view coordinates
	 */
	public int modelToViewCount(TreePath parentPath, int count) {
		Node parentNode = root.find(parentPath, 0);
		if (parentNode == null) {
			return count;
		}
		return parentNode.modelToViewCount(count);
	}

	/**
	 * Returns whether the given index of the specified parent is currently filtered.
	 *
	 * @param parentPath path to parent element
	 * @param index index of child element
	 * @return whether the child is currently filtered
	 */
	public boolean isFiltered(TreePath parentPath, int index) {
		Node parentNode = root.find(parentPath, 0);
		if (parentNode == null) {
			return false;
		}
		return parentNode.isFiltered(index);
	}

	/**
	 * Returns filtered children of the given parent, or <code>null</code> if none.
	 *
	 * @param parentPath Path of parent element
	 * @return filtered children or <code>null</code>
	 */
	public int[] getFilteredChildren(TreePath parentPath) {
		Node parentNode = root.find(parentPath, 0);
		if (parentNode == null) {
			return null;
		}
		return parentNode.filteredIndexes;
	}

	/**
	 * Clears any filters for the given parent above the given count.
	 *
	 * @param parentPath path to parent element
	 * @param childCount child count
	 */
	public void setModelChildCount(TreePath parentPath, int childCount) {
		Node parentNode = root.find(parentPath, 0);
		if (parentNode != null) {
			parentNode.setModelChildCount(childCount);
		}
	}

	/**
	 * The element at the given index has been removed from the parent. Update
	 * indexes.
	 *
	 * @param parentPath path to parent element
	 * @param index index of child element in model coordinates
	 */
	public void removeElementFromFilters(TreePath parentPath, int index) {
		Node parentNode = root.find(parentPath, 0);
		if (parentNode != null) {
			parentNode.removeElementFromFilters(index);
		}
	}

	/**
	 * The element has been removed from the parent. Update
	 * filtered indexes, in case it was a filtered object.
	 *
	 * @param parentPath path to parent element
	 * @param element removed element
	 * @return true if element was removed
	 */
	public boolean removeElementFromFilters(TreePath parentPath, Object element) {
		Node parentNode = root.find(parentPath, 0);
		if (parentNode != null) {
			int index = parentNode.indexOfFilteredElement(element);
			if (index >= 0) {
				parentNode.removeElementFromFilters(index);
				return true;
			}
		}
		return false;
	}
}
