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
 * A <code>DataDeltaNode</code> contains information that represents the differences
 * between itself and a node in another tree.  Refer to the <code>DeltaDataTree</code>
 * API and comments for details.
 *
 * @see DeltaDataTree
 */
public class DataDeltaNode extends DataTreeNode {
	/**
	 * Creates a node with the given name and data, but with no children.
	 */
	DataDeltaNode(String name, Object data) {
		super(name, data);
	}

	/**
	 * Creates a node with the given name, data and children.
	 */
	DataDeltaNode(String name, Object data, AbstractDataTreeNode[] children) {
		super(name, data, children);
	}

	/**
	 * @see AbstractDataTreeNode#asBackwardDelta(DeltaDataTree, DeltaDataTree, IPath)
	 */
	@Override
	AbstractDataTreeNode asBackwardDelta(DeltaDataTree myTree, DeltaDataTree parentTree, IPath key) {
		AbstractDataTreeNode[] newChildren;
		if (children.length == 0) {
			newChildren = NO_CHILDREN;
		} else {
			newChildren = new AbstractDataTreeNode[children.length];
			for (int i = children.length; --i >= 0;) {
				newChildren[i] = children[i].asBackwardDelta(myTree, parentTree, key.append(children[i].getName()));
			}
		}
		return new DataDeltaNode(name, parentTree.getData(key), newChildren);
	}

	@Override
	AbstractDataTreeNode compareWithParent(IPath key, DeltaDataTree parent, IComparator comparator) {
		AbstractDataTreeNode[] comparedChildren = compareWithParent(children, key, parent, comparator);
		Object oldData = parent.getData(key);
		Object newData = data;
		/* don't compare data of root */
		int userComparison = 0;
		if (key != parent.rootKey()) {
			/* allow client to specify user comparison bits */
			userComparison = comparator.compare(oldData, newData);
		}
		return new DataTreeNode(key.lastSegment(), new NodeComparison(oldData, newData, NodeComparison.K_CHANGED, userComparison), comparedChildren);
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
		return new DataDeltaNode(name, data, childrenCopy);
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
	 * Simplifies the given node, and answers its replacement.
	 */
	@Override
	AbstractDataTreeNode simplifyWithParent(IPath key, DeltaDataTree parent, IComparator comparer) {
		AbstractDataTreeNode[] simplifiedChildren = simplifyWithParent(children, key, parent, comparer);
		/* don't compare root nodes */
		if (!key.isRoot() && comparer.compare(parent.getData(key), data) == 0)
			return new NoDataDeltaNode(name, simplifiedChildren);
		return new DataDeltaNode(name, data, simplifiedChildren);
	}

	/**
	 * Returns a constant describing the type of node.
	 */
	@Override
	int type() {
		return T_DELTA_NODE;
	}
}
