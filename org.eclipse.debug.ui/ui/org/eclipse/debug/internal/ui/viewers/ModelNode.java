/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.viewers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.TreePath;

/**
 * A node in an asynchronous model.
 *
 * @since 3.2
 */
public class ModelNode {

	private Object fElement; // model element
	private boolean fIsContainer; // whether this element may have children
	private ModelNode fParent; // parent node or null for root
	private ModelNode[] fChildren; // child nodes, possibly null
	private boolean fDisposed; // whether this node has been disposed

	public ModelNode(ModelNode parent, Object element) {
		fParent = parent;
		fElement = element;
	}

	public synchronized Object getElement() {
		return fElement;
	}

	public synchronized void remap(Object element) {
		fElement = element;
	}

	public ModelNode getParentNode() {
		return fParent;
	}

	public synchronized boolean isContainer() {
		return fIsContainer;
	}

	public synchronized ModelNode[] getChildrenNodes() {
		return fChildren;
	}

	public synchronized boolean isDisposed() {
		return fDisposed;
	}

	public synchronized void dispose() {
		fDisposed = true;
		ModelNode[] childrenNodes = getChildrenNodes();
		if (childrenNodes != null) {
			for (ModelNode node : childrenNodes) {
				node.dispose();
			}
		}
	}

	/**
	 * Returns whether this node corresponds to the given path
	 *
	 * @param path tree path
	 */
	public synchronized boolean correspondsTo(TreePath path) {
		int index = path.getSegmentCount() - 1;
		ModelNode node = this;
		while (index >= 0 && node != null) {
			Object pathElement = path.getSegment(index);
			if (pathElement.equals(node.getElement())) {
				index--;
				node = node.getParentNode();
			} else {
				return false;
			}
		}
		return index == -1;
	}

	/**
	 * Returns a tree path corresponding to this node.
	 *
	 * @return
	 */
	public synchronized TreePath getTreePath() {
		List<Object> path = new ArrayList<>();
		ModelNode node = this;
		while (node != null) {
			path.add(0, node.getElement());
			node = node.getParentNode();
		}
		return new TreePath(path.toArray());
	}

	/**
	 * Adds the given child to this node.
	 *
	 * @param child
	 */
	public synchronized void addChild(ModelNode child) {
		if (fChildren == null) {
			fChildren = new ModelNode[] {child};
		} else {
			ModelNode[] kids = new ModelNode[fChildren.length + 1];
			System.arraycopy(fChildren, 0, kids, 0, fChildren.length);
			kids[fChildren.length] = child;
			fChildren = kids;
		}
	}

	/**
	 * Removes the given child from this node.
	 *
	 * @param child
	 */
	public synchronized void removeChild(ModelNode child) {
		if (fChildren != null) {
			for (int i = 0; i < fChildren.length; i++) {
				ModelNode kid = fChildren[i];
				if (child == kid) {
					ModelNode[] newNodes= new ModelNode[fChildren.length - 1];
					System.arraycopy(fChildren, 0, newNodes, 0, i);
					if (i < newNodes.length) {
						System.arraycopy(fChildren, i + 1, newNodes, i, newNodes.length - i);
					}
					fChildren = newNodes;
					return;
				}
			}
		}
	}

	/**
	 * Sets the children for this node
	 *
	 * @param children
	 */
	public synchronized void setChildren(ModelNode[] children) {
		if (children != null && children.length == 0) {
			fChildren = null;
			setIsContainer(false);
		} else {
			fChildren = children;
		}
	}

	/**
	 * Returns the number of children for this node.
	 *
	 * @return
	 */
	public synchronized int getChildCount() {
		if (fChildren == null) {
			if (isContainer()) {
				return 1;
			}
			return 0;
		}
		return fChildren.length;
	}

	/**
	 * Returns the index of the given child in this parent, or -1
	 *
	 * @param child
	 */
	public synchronized int getChildIndex(ModelNode child) {
		if (fChildren != null) {
			for (int i = 0; i < fChildren.length; i++) {
				if (child == fChildren[i]) {
					return i;
				}
			}
		}
		return -1;
	}

	/**
	 * Sets whether this node has children.
	 *
	 * @param container
	 */
	public synchronized void setIsContainer(boolean container) {
		fIsContainer = container;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		if (isDisposed()) {
			buf.append("[DISPOSED] "); //$NON-NLS-1$
		}
		if (isContainer()) {
			buf.append("[+] "); //$NON-NLS-1$
		}
		buf.append(getElement());
		return buf.toString();
	}

}
