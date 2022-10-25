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

import java.util.Objects;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.internal.utils.StringPool;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;

/**
 * Externally, a <code>DeltaDataTree</code> appears to have the same content as
 * a standard data tree.  Internally, the delta tree may be complete, or it may
 * just indicate the changes between itself and its parent.
 *
 * <p>Nodes that exist in the parent but do not exist in the delta, are represented
 * as instances of <code>DeletedNode</code>.  Nodes that are identical in the parent
 * and the delta, but have differences in their subtrees, are represented as
 * instances of <code>NoDataDeltaNode</code> in the delta tree.  Nodes that differ
 * between parent and delta are instances of <code>DataDeltaNode</code>.  However,
 * the <code>DataDeltaNode</code> only contains the children whose subtrees differ
 * between parent and delta.
 *
 * A delta tree algebra is used to manipulate sets of delta trees.  Given two trees,
 * one can obtain the delta between the two using the method
 * <code>forwardDeltaWith(aTree)</code>.  Given a tree and a delta, one can assemble
 * the complete tree that the delta represents using the method <code>
 * assembleWithForwardDelta</code>.  Refer to the public API methods of this class
 * for further details.
 */

public class DeltaDataTree extends AbstractDataTree {
	private volatile AbstractDataTreeNode rootNode;
	private volatile DeltaDataTree parent;

	/**
	 * Creates a new empty tree.
	 */
	public DeltaDataTree() {
		this.rootNode = new DataTreeNode(null, null);
		this.parent = null;
	}

	/**
	 * Creates a new tree.
	 * @param rootNode
	 *	root node of new tree.
	 */
	public DeltaDataTree(AbstractDataTreeNode rootNode) {
		Objects.requireNonNull(rootNode);
		this.rootNode = rootNode;
		this.parent = null;
	}

	protected DeltaDataTree(AbstractDataTreeNode rootNode, DeltaDataTree parent) {
		Objects.requireNonNull(rootNode);
		this.rootNode = rootNode;
		this.parent = parent;
	}

	/**
	 * Adds a child to the tree.
	 *
	 * @param parentKey parent for new child.
	 * @param localName name of child.
	 * @param childNode child node.
	 */
	protected void addChild(IPath parentKey, String localName, AbstractDataTreeNode childNode) {
		if (!includes(parentKey))
			handleNotFound(parentKey);
		childNode.setName(localName);
		this.assembleNode(parentKey, new NoDataDeltaNode(parentKey.lastSegment(), childNode));
	}

	/**
	 * Returns the tree as a backward delta.  If the delta is applied to the tree it
	 * will produce its parent. The receiver must have a forward
	 * delta representation. I.e.:  Call the receiver's parent A,
	 * and the receiver B.  The receiver's representation is A-&gt;B.
	 * Returns the delta A&lt;-B.  The result is equivalent to A, but has B as its parent.
	 */
	DeltaDataTree asBackwardDelta() {
		if (getParent() == null)
			return newEmptyDeltaTree();
		return new DeltaDataTree(getRootNode().asBackwardDelta(this, getParent(), rootKey()), this);
	}

	/**
	 * This method can only be called on a comparison tree created
	 * using DeltaDataTree.compareWith().  This method flips the orientation
	 * of the given comparison tree, so that additions become removals,
	 * and vice-versa.  This method destructively changes the tree
	 * as opposed to making a copy.
	 */
	public DeltaDataTree asReverseComparisonTree(IComparator comparator) {
		/* don't reverse the root node if it's the absolute root (name==null) */
		if (rootNode.getName() == null) {
			AbstractDataTreeNode[] children = rootNode.getChildren();
			int nextChild = 0;
			for (AbstractDataTreeNode c : children) {
				AbstractDataTreeNode newChild = c.asReverseComparisonNode(comparator);
				if (newChild != null) {
					children[nextChild++] = newChild;
				}
			}

			if (nextChild < children.length) {
				AbstractDataTreeNode[] newChildren = new AbstractDataTreeNode[nextChild];
				System.arraycopy(children, 0, newChildren, 0, nextChild);
				rootNode.setChildren(newChildren);
			}
		} else {
			rootNode.asReverseComparisonNode(comparator);
		}
		return this;
	}

	/**
	 * Replaces a node in the tree with the result of assembling the node
	 * with the given delta node (which represents a forward delta on
	 * the existing node).
	 *
	 * @param key key of the node to replace.
	 * @param deltaNode delta node to use to assemble the new node.
	 */
	protected void assembleNode(IPath key, AbstractDataTreeNode deltaNode) {
		setRootNode(rootNode.assembleWith(deltaNode, key, 0));
	}

	/**
	 * Assembles the receiver with the given delta tree and answer
	 * the resulting, mutable source tree.  The given delta tree must be a
	 * forward delta based on the receiver (i.e. missing information is taken from
	 * the receiver).  This operation is used to coalesce delta trees.
	 *
	 * <p>In detail, suppose that c is a forward delta over source tree a.
	 * Let d := a assembleWithForwardDelta: c.
	 * d has the same content as c, and is represented as a delta tree
	 * whose parent is the same as a's parent.
	 *
	 * <p>In general, if c is represented as a chain of deltas of length n,
	 * then d is represented as a chain of length n-1.
	 *
	 * <p>So if a is a complete tree (i.e., has no parent, length=0), then d
	 * will be a complete tree too.
	 *
	 * <p>Corollary: (a assembleWithForwardDelta: (a forwardDeltaWith: b)) = b
	 */
	public DeltaDataTree assembleWithForwardDelta(DeltaDataTree deltaTree) {
		return new DeltaDataTree(getRootNode().assembleWith(deltaTree.getRootNode()), this);
	}

	/**
	 * Compares this tree with another tree, starting from the given path.  The
	 * given path will be the root node of the returned tree.  Both this
	 * tree and other tree must contain the given path.
	 */
	protected DeltaDataTree basicCompare(DeltaDataTree other, IComparator comparator, IPath path) {
		DeltaDataTree newTree;
		if (this == other) {
			newTree = new DeltaDataTree();
			newTree.setRootData(new NodeComparison(null, null, 0, 0));
		} else if (other.hasAncestor(this)) {
			AbstractDataTreeNode assembled = other.searchNodeAt(path);
			DeltaDataTree tree = other;

			/* Iterate through the receiver's ancestors until the receiver is reached */
			while ((tree = tree.getParent()) != this) {
				//ancestor may not contain the given path
				AbstractDataTreeNode treeNode = tree.searchLocalNodeAt(path);
				if (treeNode != null) {
					assembled = treeNode.assembleWith(assembled);
				}
			}
			AbstractDataTreeNode comparedRoot = assembled.compareWithParent(path, this, comparator);
			newTree = new DeltaDataTree(comparedRoot);
		} else if (this.hasAncestor(other)) {
			AbstractDataTreeNode assembled = this.asBackwardDelta().searchNodeAt(path);
			DeltaDataTree tree = this;

			/* Iterate through the receiver's ancestors until the other tree is reached */
			while ((tree = tree.getParent()) != other) {
				assembled = assembled.assembleWith(tree.asBackwardDelta().searchNodeAt(path));
			}
			AbstractDataTreeNode comparedRoot = assembled.compareWithParent(path, this, comparator);
			newTree = new DeltaDataTree(comparedRoot);
		} else {
			//revert to naive comparison
			DataTreeNode thisCompleteRoot = (DataTreeNode) this.copyCompleteSubtree(path);
			DataTreeNode otherCompleteRoot = (DataTreeNode) other.copyCompleteSubtree(path);
			AbstractDataTreeNode comparedRoot = thisCompleteRoot.compareWith(otherCompleteRoot, comparator);
			newTree = new DeltaDataTree(comparedRoot);
		}
		newTree.immutable();
		return newTree;
	}

	/**
	 * Collapses this tree so that the given ancestor becomes its
	 * immediate parent.  Afterwards, this tree will still have exactly the
	 * same contents, but its internal structure will be compressed.
	 *
	 * <p> This operation should be used to collapse chains of
	 * delta trees that don't contain interesting intermediate states.
	 *
	 * <p>This is a destructive operation, since it modifies the structure of this
	 * tree instance.  This tree must be immutable at the start of this operation,
	 * and will be immutable afterwards.
	 * @return this tree.
	 */
	public DeltaDataTree collapseTo(DeltaDataTree collapseTo, IComparator comparator) {
		if (this == collapseTo || getParent() == collapseTo) {
			//already collapsed
			return this;
		}
		//collapse my tree to be a forward delta of the parent's tree.
		//c will have the same content as this tree, but its parent will be "parent".
		DeltaDataTree c = collapseTo.forwardDeltaWith(this, comparator);

		//update my internal root node and parent pointers.
		setParent(collapseTo);
		setRootNode(c.rootNode);
		return this;
	}

	/**
	 * Returns a DeltaDataTree that describes the differences between
	 * this tree and "other" tree.  Each node of the returned tree
	 * will contain a NodeComparison object that describes the differences
	 * between the two trees.
	 */
	public DeltaDataTree compareWith(DeltaDataTree other, IComparator comparator) {

		DeltaDataTree newTree;
		if (this == other) {
			newTree = new DeltaDataTree();
			newTree.setRootData(new NodeComparison(null, null, 0, 0));
		} else if (other.hasAncestor(this)) {

			AbstractDataTreeNode assembled = other.getRootNode();
			DeltaDataTree tree = other;

			/* Iterate through the receiver's ancestors until the receiver is reached */
			while ((tree = tree.getParent()) != this) {
				assembled = tree.getRootNode().assembleWith(assembled);
			}
			AbstractDataTreeNode comparedRoot = assembled.compareWithParent(rootKey(), this, comparator);
			newTree = new DeltaDataTree(comparedRoot);
		} else if (this.hasAncestor(other)) {
			AbstractDataTreeNode assembled = this.asBackwardDelta().getRootNode();
			DeltaDataTree tree = this;

			/* Iterate through the receiver's ancestors until the other tree is reached */
			while ((tree = tree.getParent()) != other) {
				assembled = assembled.assembleWith(tree.asBackwardDelta().getRootNode());
			}
			AbstractDataTreeNode comparedRoot = assembled.compareWithParent(rootKey(), this, comparator);
			newTree = new DeltaDataTree(comparedRoot);
		} else {
			//revert to naive comparison if trees have no common ancestry
			DataTreeNode thisCompleteRoot = (DataTreeNode) this.copyCompleteSubtree(rootKey());
			DataTreeNode otherCompleteRoot = (DataTreeNode) other.copyCompleteSubtree(rootKey());
			AbstractDataTreeNode comparedRoot = thisCompleteRoot.compareWith(otherCompleteRoot, comparator);
			newTree = new DeltaDataTree(comparedRoot);
		}
		newTree.immutable();
		return newTree;
	}

	/**
	 * Compares this tree with another tree, starting from the given path.  The
	 * given path will be the root node of the returned tree.
	 */
	public DeltaDataTree compareWith(DeltaDataTree other, IComparator comparator, IPath path) {
		/* need to figure out if trees really contain the given path */
		if (this.includes(path)) {
			if (other.includes(path))
				return basicCompare(other, comparator, path);
			/* only exists in this tree */
			return new DeltaDataTree(AbstractDataTreeNode.convertToRemovedComparisonNode(this.copyCompleteSubtree(path), comparator.compare(this.getData(path), null)));
		}
		if (other.includes(path))
			/* only exists in other tree */
			return new DeltaDataTree(AbstractDataTreeNode.convertToAddedComparisonNode(other.copyCompleteSubtree(path), comparator.compare(null, other.getData(path))));
		/* doesn't exist in either tree */
		return DeltaDataTree.createEmptyDelta();
	}

	/**
	 * Returns a copy of the tree which shares its instance variables.
	 */
	protected DeltaDataTree copy() {
		return new DeltaDataTree(rootNode, parent);
	}

	/**
	 * Returns a complete node containing the contents of a subtree of the tree.
	 *
	 * @param key
	 *	key of subtree to copy
	 */
	@Override
	public AbstractDataTreeNode copyCompleteSubtree(IPath key) {
		AbstractDataTreeNode node = searchNodeAt(key);
		if (node == null) {
			handleNotFound(key);
			return null;
		}
		if (node.isDelta())
			return naiveCopyCompleteSubtree(key);
		//copy the node in case the user wants to hammer the subtree name
		return node.copy();
	}

	/**
	 * @see AbstractDataTree#createChild(IPath, String)
	 */
	@Override
	public void createChild(IPath parentKey, String localName) {
		createChild(parentKey, localName, null);
	}

	/**
	 * @see AbstractDataTree#createChild(IPath, String, Object)
	 */
	@Override
	public void createChild(IPath parentKey, String localName, Object data) {
		if (isImmutable())
			handleImmutableTree();
		addChild(parentKey, localName, new DataTreeNode(localName, data));
	}

	/**
	 * Returns a delta data tree that represents an empty delta.
	 * (i.e. it represents a delta on another (unspecified) tree,
	 * but introduces no changes).
	 */
	static DeltaDataTree createEmptyDelta() {
		return new DeltaDataTree(new NoDataDeltaNode(null), null);
	}

	/**
	 * @see AbstractDataTree#createSubtree(IPath, AbstractDataTreeNode)
	 */
	@Override
	public void createSubtree(IPath key, AbstractDataTreeNode node) {
		if (isImmutable())
			handleImmutableTree();
		if (key.isRoot()) {
			setParent(null);
			setRootNode(node);
		} else {
			addChild(key.removeLastSegments(1), key.lastSegment(), node);
		}
	}

	/**
	 * @see AbstractDataTree#deleteChild(IPath, String)
	 */
	@Override
	public void deleteChild(IPath parentKey, String localName) {
		if (isImmutable())
			handleImmutableTree();
		/* If the child does not exist */
		IPath childKey = parentKey.append(localName);
		if (!includes(childKey))
			handleNotFound(childKey);
		assembleNode(parentKey, new NoDataDeltaNode(parentKey.lastSegment(), new DeletedNode(localName)));
	}

	/**
	 * Returns a node of the tree if it is present, otherwise returns null
	 *
	 * @param key
	 *	key of node to find
	 */
	public AbstractDataTreeNode findNodeAt(IPath key) {
		AbstractDataTreeNode node = rootNode;
		int segmentCount = key.segmentCount();
		for (int i = 0; i < segmentCount; i++) {
			node = node.childAtOrNull(key.segment(i));
			if (node == null)
				return null;
		}
		return node;
	}

	/**
	 * Returns a forward delta between the receiver and the given source tree,
	 * using the given comparer to compare data objects.
	 * The result describes the changes which, if assembled with the receiver,
	 * will produce the given source tree.
	 * In more detail, let c = a.forwardDeltaWith(b).
	 * c has the same content as b, but is represented as a delta tree with a as the parent.
	 * Also, c is immutable.
	 *
	 * There is no requirement that a and b be related, although it is usually more
	 * efficient if they are. The node keys are used as the basis of correlation
	 * between trees.
	 *
	 * Note that if b is already represented as a delta over a,
	 * then c will have the same internal structure as b.
	 * Thus the very common case of previous forwardDeltaWith: current
	 * is actually very fast when current is a modification of previous.
	 *
	 * @param sourceTree second delta tree to create a delta between
	 * @param comparer the comparer used to compare data objects
	 * @return the new delta
	 */
	public DeltaDataTree forwardDeltaWith(DeltaDataTree sourceTree, IComparator comparer) {
		DeltaDataTree newTree;
		if (this == sourceTree) {
			newTree = this.newEmptyDeltaTree();
		} else if (sourceTree.hasAncestor(this)) {
			AbstractDataTreeNode assembled = sourceTree.getRootNode();
			DeltaDataTree treeParent = sourceTree;

			/* Iterate through the sourceTree's ancestors until the receiver is reached */
			while ((treeParent = treeParent.getParent()) != this) {
				assembled = treeParent.getRootNode().assembleWith(assembled);
			}
			newTree = new DeltaDataTree(assembled, this);
			newTree.simplify(comparer);
		} else if (this.hasAncestor(sourceTree)) {
			//create the delta backwards and then reverse it
			newTree = sourceTree.forwardDeltaWith(this, comparer);
			newTree = newTree.asBackwardDelta();
		} else {
			DataTreeNode thisCompleteRoot = (DataTreeNode) this.copyCompleteSubtree(rootKey());
			DataTreeNode sourceTreeCompleteRoot = (DataTreeNode) sourceTree.copyCompleteSubtree(rootKey());
			AbstractDataTreeNode deltaRoot = thisCompleteRoot.forwardDeltaWith(sourceTreeCompleteRoot, comparer);
			newTree = new DeltaDataTree(deltaRoot, this);
		}
		newTree.immutable();
		return newTree;
	}

	/**
	 * @see AbstractDataTree#getChildCount(IPath)
	 */
	@Override
	public int getChildCount(IPath parentKey) {
		return getChildNodes(parentKey).length;
	}

	/**
	 * Returns the child nodes of a node in the tree.
	 */
	protected AbstractDataTreeNode[] getChildNodes(IPath parentKey) {

		/* Algorithm:
		 *   for each delta in chain (going backwards),
		 *     get list of child nodes, if any in delta
		 *     assemble with previously seen list, if any
		 *     break when complete tree found,
		 *   report error if parent is missing or has been deleted
		 */

		AbstractDataTreeNode[] childNodes = null;
		int keyLength = parentKey.segmentCount();
		for (DeltaDataTree tree = this; tree != null; tree = tree.parent) {
			AbstractDataTreeNode node = tree.rootNode;
			boolean complete = !node.isDelta();
			for (int i = 0; i < keyLength; i++) {
				node = node.childAtOrNull(parentKey.segment(i));
				if (node == null) {
					break;
				}
				if (!node.isDelta()) {
					complete = true;
				}
			}
			if (node != null) {
				if (node.isDeleted()) {
					break;
				}
				if (childNodes == null) {
					childNodes = node.children;
				} else {
					// Be sure to assemble(old, new) rather than (new, old).
					// Keep deleted nodes if we haven't encountered the complete node yet.
					childNodes = AbstractDataTreeNode.assembleWith(node.children, childNodes, !complete);
				}
			}
			if (complete) {
				if (childNodes != null) {
					return childNodes;
				}
				// Not found, but complete node encountered, so should not check parent tree.
				break;
			}
		}
		if (childNodes != null) {
			// Some deltas carry info about children, but there is
			// no complete node against which they describe deltas.
			Assert.isTrue(false, Messages.dtree_malformedTree);
		}

		// Node is missing or has been deleted.
		handleNotFound(parentKey);
		return null;//should not get here
	}

	/**
	 * @see AbstractDataTree#getChildren(IPath)
	 */
	@Override
	public IPath[] getChildren(IPath parentKey) {
		AbstractDataTreeNode[] childNodes = getChildNodes(parentKey);
		int len = childNodes.length;
		if (len == 0)
			return NO_CHILDREN;
		IPath[] answer = new IPath[len];
		for (int i = 0; i < len; ++i)
			answer[i] = parentKey.append(childNodes[i].name);
		return answer;
	}

	/**
	 * Returns the data at a node of the tree.
	 *
	 * @param key
	 *	key of node for which to return data.
	 */
	@Override
	public Object getData(IPath key) {

		/* Algorithm:
		 *   for each delta in chain (going backwards),
		 *     get node, if any in delta
		 *	   if it carries data, return it
		 *     break when complete tree found
		 *   report error if node is missing or has been deleted
		 */

		int keyLength = key.segmentCount();
		for (DeltaDataTree tree = this; tree != null; tree = tree.parent) {
			AbstractDataTreeNode node = tree.rootNode;
			boolean complete = !node.isDelta();
			for (int i = 0; i < keyLength; i++) {
				node = node.childAtOrNull(key.segment(i));
				if (node == null) {
					break;
				}
				if (!node.isDelta()) {
					complete = true;
				}
			}
			if (node != null) {
				if (node.hasData()) {
					return node.getData();
				} else if (node.isDeleted()) {
					break;
				}
			}
			if (complete) {
				// Not found, but complete node encountered, so should not check parent tree.
				break;
			}
		}
		handleNotFound(key);
		return null; //can't get here
	}

	/**
	 * @see AbstractDataTree#getNameOfChild(IPath, int)
	 */
	@Override
	public String getNameOfChild(IPath parentKey, int index) {
		AbstractDataTreeNode[] childNodes = getChildNodes(parentKey);
		return childNodes[index].name;
	}

	/**
	 * Returns the local names for the children of a node of the tree.
	 *
	 * @see AbstractDataTree#getNamesOfChildren(IPath)
	 */
	@Override
	public String[] getNamesOfChildren(IPath parentKey) {
		AbstractDataTreeNode[] childNodes = getChildNodes(parentKey);
		int len = childNodes.length;
		String[] namesOfChildren = new String[len];
		for (int i = 0; i < len; ++i)
			namesOfChildren[i] = childNodes[i].name;
		return namesOfChildren;
	}

	/**
	 * Returns the parent of the tree.
	 */
	public DeltaDataTree getParent() {
		return parent;
	}

	/**
	 * Returns the root node of the tree.
	 *
	 * <p>
	 * Both subclasses must be able to return their root node. However subclasses
	 * can have different types of root nodes, so this is not enforced as an
	 * abstract method
	 */
	protected AbstractDataTreeNode getRootNode() {
		return rootNode;
	}

	/**
	 * Returns true if the receiver's parent has the specified ancestor
	 *
	 * @param ancestor the ancestor in question
	 */
	protected boolean hasAncestor(DeltaDataTree ancestor) {
		DeltaDataTree myParent = this;
		while ((myParent = myParent.getParent()) != null) {
			if (myParent == ancestor) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns true if the receiver includes a node with
	 * the given key, false otherwise.
	 */
	@Override
	public boolean includes(IPath key) {
		return searchNodeAt(key) != null;
	}

	public boolean isEmptyDelta() {
		return rootNode.getChildren().length == 0;
	}

	/**
	 * Returns an object containing:
	 *  - the node key
	 * 	- a flag indicating whether the specified node was found
	 *  - the data for the node, if it was found
	 *
	 * @param key  key of node for which we want to retrieve data.
	 */
	@Override
	public DataTreeLookup lookup(IPath key) {
		int keyLength = key.segmentCount();
		for (DeltaDataTree tree = this; tree != null; tree = tree.parent) {
			AbstractDataTreeNode node = tree.rootNode;
			boolean complete = !node.isDelta();
			for (int i = 0; i < keyLength; i++) {
				node = node.childAtOrNull(key.segment(i));
				if (node == null) {
					break;
				}
				complete |= !node.isDelta();
			}
			if (node != null) {
				if (node.hasData()) {
					return DataTreeLookup.newLookup(key, true, node.getData(), tree == this);
				} else if (node.isDeleted()) {
					break;
				}
			}
			if (complete) {
				// Not found, but complete node encountered, so should not check parent tree.
				break;
			}
		}
		return DataTreeLookup.newLookup(key, false, null);
	}

	/**
	 * Returns an object containing:
	 *  - the node key
	 * 	- a flag indicating whether the specified node was found
	 *  - the data for the node, if it was found
	 *
	 * This is a case-insensitive variant of the <code>lookup</code>
	 * method.
	 *
	 * @param key  key of node for which we want to retrieve data.
	 */
	public DataTreeLookup lookupIgnoreCase(IPath key) {
		int keyLength = key.segmentCount();
		for (DeltaDataTree tree = this; tree != null; tree = tree.parent) {
			AbstractDataTreeNode node = tree.rootNode;
			boolean complete = !node.isDelta();
			for (int i = 0; i < keyLength; i++) {
				node = node.childAtIgnoreCase(key.segment(i));
				if (node == null) {
					break;
				}
				complete |= !node.isDelta();
			}
			if (node != null) {
				if (node.hasData()) {
					return DataTreeLookup.newLookup(key, true, node.getData(), tree == this);
				} else if (node.isDeleted()) {
					break;
				}
			}
			if (complete) {
				// Not found, but complete node encountered, so should not check parent tree.
				break;
			}
		}
		return DataTreeLookup.newLookup(key, false, null);
	}

	/**
	 * Converts this tree's representation to be a complete tree, not a delta.
	 * This disconnects this tree from its parents.
	 * The parent trees are unaffected.
	 */
	public void makeComplete() {
		AbstractDataTreeNode assembled = getRootNode();
		DeltaDataTree myParent = getParent();
		while (myParent != null) {
			assembled = myParent.getRootNode().assembleWith(assembled);
			myParent = myParent.getParent();
		}
		setRootNode(assembled);
		setParent(null);
	}

	/**
	 * Returns a complete node containing the contents of the subtree
	 * rooted at <code>key</code> in the receiver.  Uses the public API.
	 *
	 * @param key
	 *	key of subtree whose contents we want to copy.
	 */
	protected AbstractDataTreeNode naiveCopyCompleteSubtree(IPath key) {
		String[] childNames = getNamesOfChildren(key);
		int numChildren = childNames.length;
		AbstractDataTreeNode[] childNodes;
		if (numChildren == 0) {
			childNodes = AbstractDataTreeNode.NO_CHILDREN;
		} else {
			childNodes = new AbstractDataTreeNode[numChildren];
			/* do for each child */
			for (int i = numChildren; --i >= 0;) {
				childNodes[i] = copyCompleteSubtree(key.append(childNames[i]));
			}
		}
		return new DataTreeNode(key.lastSegment(), getData(key), childNodes);
	}

	/**
	 * Returns a new tree which represents an empty, mutable delta on the
	 * receiver.  It is not possible to obtain a new delta tree if the receiver is
	 * not immutable, as subsequent changes to the receiver would affect the
	 * resulting delta.
	 */
	public DeltaDataTree newEmptyDeltaTree() {
		if (!isImmutable())
			throw new IllegalArgumentException(Messages.dtree_notImmutable);
		return new DeltaDataTree(new NoDataDeltaNode(null), this);
	}

	/**
	 * Makes the receiver the root tree in the list of trees on which it is based.
	 * The receiver's representation becomes a complete tree, while its parents'
	 * representations become backward deltas based on the receiver.
	 * It is not possible to re-root a source tree that is not immutable, as this
	 * would require that its parents be expressed as deltas on a source tree
	 * which could still change.
	 *
	 * @exception RuntimeException
	 *	receiver is not immutable
	 */
	public DeltaDataTree reroot() {
		/* self mutex critical region */
		reroot(this);
		return this;
	}

	/**
	 * Makes the given source tree the root tree in the list of trees on which it is based.
	 * The source tree's representation becomes a complete tree, while its parents'
	 * representations become backward deltas based on the source tree.
	 * It is not possible to re-root a source tree that is not immutable, as this
	 * would require that its parents be expressed as deltas on a source tree
	 * which could still change.
	 *
	 * @param sourceTree
	 *	source tree to set as the new root
	 * @exception RuntimeException
	 *	sourceTree is not immutable
	 */
	protected void reroot(DeltaDataTree sourceTree) {
		if (!sourceTree.isImmutable())
			handleImmutableTree();
		DeltaDataTree sourceParent = sourceTree.getParent();
		if (sourceParent == null)
			return;
		this.reroot(sourceParent);
		DeltaDataTree backwardDelta = sourceTree.asBackwardDelta();
		DeltaDataTree complete = sourceParent.assembleWithForwardDelta(sourceTree);
		sourceTree.setRootNode(complete.getRootNode());
		sourceTree.setParent(null);
		sourceParent.setRootNode(backwardDelta.getRootNode());
		sourceParent.setParent(sourceTree);
	}

	/**
	 * Returns a complete node containing the contents of a subtree of the tree.
	 * Returns null  if the node at this key does not exist.  This is a thread-safe
	 * version of copyCompleteSubtree
	 *
	 * @param key key of subtree to copy
	 */
	public AbstractDataTreeNode safeCopyCompleteSubtree(IPath key) {
		AbstractDataTreeNode node = searchNodeAt(key);
		if (node == null)
			return null;
		if (node.isDelta())
			return safeNaiveCopyCompleteSubtree(key);
		//copy the node in case the user wants to hammer the subtree name
		return node.copy();
	}

	/**
	 * Returns a complete node containing the contents of the subtree
	 * rooted at @key in the receiver.  Returns null if this node does not exist in
	 * the tree.  This is a thread-safe version of naiveCopyCompleteSubtree
	 *
	 * @param key
	 *	key of subtree whose contents we want to copy.
	 */
	protected AbstractDataTreeNode safeNaiveCopyCompleteSubtree(IPath key) {
		try {
			String[] childNames = getNamesOfChildren(key);
			int numChildren = childNames.length;
			AbstractDataTreeNode[] childNodes;
			if (numChildren == 0) {
				childNodes = AbstractDataTreeNode.NO_CHILDREN;
			} else {
				childNodes = new AbstractDataTreeNode[numChildren];
				/* do for each child */
				int actualChildCount = 0;
				for (int i = numChildren; --i >= 0;) {
					childNodes[i] = safeCopyCompleteSubtree(key.append(childNames[i]));
					if (childNodes[i] != null)
						actualChildCount++;
				}
				//if there are less actual children due to concurrent deletion, shrink the child array
				if (actualChildCount < numChildren) {
					AbstractDataTreeNode[] actualChildNodes = new AbstractDataTreeNode[actualChildCount];
					for (int iOld = 0, iNew = 0; iOld < numChildren; iOld++)
						if (childNodes[iOld] != null)
							actualChildNodes[iNew++] = childNodes[iOld];
					childNodes = actualChildNodes;
				}
			}
			return new DataTreeNode(key.lastSegment(), getData(key), childNodes);
		} catch (ObjectNotFoundException e) {
			return null;
		}
	}

	/**
	 * Returns the specified node.  Search in the parent if necessary.  Return null
	 * if the node is not found or if it has been deleted
	 */
	protected AbstractDataTreeNode searchNodeAt(IPath key) {
		int keyLength = key.segmentCount();
		for (DeltaDataTree tree = this; tree != null; tree = tree.parent) {
			AbstractDataTreeNode node = tree.rootNode;
			boolean complete = !node.isDelta();
			for (int i = 0; i < keyLength; i++) {
				node = node.childAtOrNull(key.segment(i));
				if (node == null) {
					break;
				}
				if (!node.isDelta()) {
					complete = true;
				}
			}
			if (node != null) {
				if (node.isDeleted())
					break;
				return node;
			}
			if (complete) {
				// Not found, but complete node encountered, so should not check parent tree.
				break;
			}
		}
		return null;
	}

	/**
	 * Returns the specified node. Return null if the node is not found or if it has
	 * been deleted. Like {@link DeltaDataTree#searchNodeAt(IPath)} but does NOT
	 * search in the parent.
	 */
	protected AbstractDataTreeNode searchLocalNodeAt(IPath key) {
		int keyLength = key.segmentCount();
		DeltaDataTree tree = this;
		AbstractDataTreeNode node = tree.rootNode;
		boolean complete = !node.isDelta();
		for (int i = 0; i < keyLength; i++) {
			node = node.childAtOrNull(key.segment(i));
			if (node == null) {
				break;
			}
			if (!node.isDelta()) {
				complete = true;
			}
		}
		if (node != null) {
			if (node.isDeleted())
				return null;
			return node;
		}
		if (complete) {
			// Not found, but complete node encountered, so should not check parent tree.
			return null;
		}
		return null;
	}
	/**
	 * @see AbstractDataTree#setData(IPath, Object)
	 */
	@Override
	public void setData(IPath key, Object data) {
		if (isImmutable())
			handleImmutableTree();
		if (!includes(key))
			handleNotFound(key);
		assembleNode(key, new DataDeltaNode(key.lastSegment(), data));
	}

	public void setRootData(Object data) {
		setData(rootKey(), data);
	}

	public Object getRootData() {
		return getData(rootKey());
	}

	/**
	 * Sets the parent of the tree.
	 */
	protected void setParent(DeltaDataTree aTree) {
		parent = aTree;
	}

	/**
	 * Sets the root node of the tree
	 */
	@Override
	void setRootNode(AbstractDataTreeNode aNode) {
		rootNode = aNode;
	}

	/**
	 * Simplifies the receiver:
	 *	- replaces any DataDelta nodes with the same data as the parent
	 *	  with a NoDataDelta node
	 *	- removes any empty (leaf NoDataDelta) nodes
	 */
	protected void simplify(IComparator comparer) {
		if (parent == null)
			return;
		setRootNode(rootNode.simplifyWithParent(rootKey(), parent, comparer));
	}

	/**
	 * @see org.eclipse.core.internal.utils.IStringPoolParticipant#shareStrings(StringPool)
	 */
	public void storeStrings(StringPool set) {
		AbstractDataTreeNode root = null;
		for (DeltaDataTree dad = this; dad != null; dad = dad.getParent()) {
			root = dad.getRootNode();
			if (root != null)
				root.storeStrings(set);
		}
	}

	/** for debugging purposes only */
	@Override
	public String toString() {
		DeltaDataTree root = getParent();
		if (root == null && isEmptyDelta()) {
			return this.getClass().getSimpleName() + " -> rootNode=" + rootNode.toShortString(); //$NON-NLS-1$
		}
		int depth = root == null ? 0 : 1;
		while (root != null && root.getParent() != null) {
			root = root.getParent();
			depth++;
		}

		return this.getClass().getSimpleName() + ' ' + getRootData() //
				+ "->" + (parent == null ? null : parent.getRootData()) // $NON-NLS-1$ //$NON-NLS-1$
				+ "->...depth " + depth + "..." //$NON-NLS-1$ //$NON-NLS-2$
				+ "->" + (parent == null ? null : root.getRootData()) // $NON-NLS-1$ //$NON-NLS-1$
				+ " rootNode=" + rootNode.toShortString(); //$NON-NLS-1$
	}
}