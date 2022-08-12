/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
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
package org.eclipse.core.internal.dtree;

import org.eclipse.core.runtime.IPath;

/**
 * A <code>NoDataDeltaNode</code>is a node in a delta tree whose subtree contains
 * differences since the delta's parent.  Refer to the <code>DeltaDataTree</code>
 * API and class comment for details.
 *
 * @see DeltaDataTree
 */
public class NoDataDeltaNode extends AbstractDataTreeNode {
	/**
	 * Creates a new empty delta.
	 */
	public NoDataDeltaNode(String name) {
		this(name, NO_CHILDREN);
	}

	/**
	 * Creates a new data tree node
	 *
	 * @param name name of new node
	 * @param children children of the new node
	 */
	public NoDataDeltaNode(String name, AbstractDataTreeNode[] children) {
		super(name, children);
	}

	/**
	 * Creates a new data tree node
	 *
	 * @param localName name of new node
	 * @param childNode single child for new node
	 */
	NoDataDeltaNode(String localName, AbstractDataTreeNode childNode) {
		super(localName, new AbstractDataTreeNode[] {childNode});
	}

	/**
	 * @see AbstractDataTreeNode#asBackwardDelta(DeltaDataTree, DeltaDataTree, IPath)
	 */
	@Override
	AbstractDataTreeNode asBackwardDelta(DeltaDataTree myTree, DeltaDataTree parentTree, IPath key) {
		int numChildren = children.length;
		if (numChildren == 0)
			return new NoDataDeltaNode(name, NO_CHILDREN);
		AbstractDataTreeNode[] newChildren = new AbstractDataTreeNode[numChildren];
		for (int i = numChildren; --i >= 0;) {
			newChildren[i] = children[i].asBackwardDelta(myTree, parentTree, key.append(children[i].getName()));
		}
		return new NoDataDeltaNode(name, newChildren);
	}

	/**
	 * @see AbstractDataTreeNode#compareWithParent(IPath, DeltaDataTree, IComparator)
	 */
	@Override
	AbstractDataTreeNode compareWithParent(IPath key, DeltaDataTree parent, IComparator comparator) {
		AbstractDataTreeNode[] comparedChildren = compareWithParent(children, key, parent, comparator);
		Object oldData = parent.getData(key);
		return new DataTreeNode(key.lastSegment(), new NodeComparison(oldData, oldData, NodeComparison.K_CHANGED, 0), comparedChildren);
	}

	/**
	 * Creates and returns a new copy of the receiver.  Makes a deep copy of
	 * children, but a shallow copy of name and data.
	 */
	@Override
	AbstractDataTreeNode copy() {
		AbstractDataTreeNode[] childrenCopy;
		if (children.length == 0) {
			childrenCopy = NO_CHILDREN;
		} else {
			childrenCopy = new AbstractDataTreeNode[children.length];
			System.arraycopy(children, 0, childrenCopy, 0, children.length);
		}
		return new NoDataDeltaNode(name, childrenCopy);
	}

	/**
	 * Returns true if the receiver represents delta information,
	 * false if it represents the complete information.
	 */
	@Override
	boolean isDelta() {
		return true;
	}

	/**
	 * Returns true if the receiver is an empty delta node, false otherwise.
	 */
	@Override
	boolean isEmptyDelta() {
		return this.size() == 0;
	}

	/**
	 * Simplifies the given node, and returns its replacement.
	 */
	@Override
	AbstractDataTreeNode simplifyWithParent(IPath key, DeltaDataTree parent, IComparator comparer) {
		AbstractDataTreeNode[] simplifiedChildren = simplifyWithParent(children, key, parent, comparer);
		return new NoDataDeltaNode(name, simplifiedChildren);
	}

	/**
	 * Return a constant describing the type of node.
	 */
	@Override
	int type() {
		return T_NO_DATA_DELTA_NODE;
	}
}
