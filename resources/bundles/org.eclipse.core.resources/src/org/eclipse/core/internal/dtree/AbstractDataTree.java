/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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

import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.NLS;

/**
 * Data trees can be viewed as generic multi-leaf trees.  The tree points to a single
 * rootNode, and each node can contain an arbitrary number of children.
 *
 * @see DeltaDataTree
 */

public abstract class AbstractDataTree {

	/**
	 * Whether modifications to the given source tree are allowed
	 */
	private boolean immutable = false;

	/**
	 * Singleton indicating no children
	 */
	protected static final IPath[] NO_CHILDREN = new IPath[0];

	/**
	 * Returns a copy of the node subtree rooted at the given key.
	 *
	 */
	public abstract AbstractDataTreeNode copyCompleteSubtree(IPath key);

	/**
	 * Creates a new child in the tree.  If a child with such a name exists,
	 * it is replaced with the new child
	 *
	 * @param parentKey key of parent for new child.
	 * @param localName name for new child.
	 * @exception ObjectNotFoundException
	 *	parentKey does not exist in the receiver
	 * @exception RuntimeException
	 *	receiver is immutable
	 */
	public abstract void createChild(IPath parentKey, String localName);

	/**
	 * Creates a new child in the tree.  If a child with such a name exists,
	 * it is replaced with the new child
	 *
	 * @param parentKey key of parent for new child.
	 * @param localName name for new child.
	 * @param object the data for the new child
	 * @exception ObjectNotFoundException
	 *	parentKey does not exist in the receiver
	 * @exception RuntimeException
	 *	receiver is immutable
	 */
	public abstract void createChild(IPath parentKey, String localName, Object object);

	/**
	 * Creates or replaces a subtree in the tree.  The parent node must exist.
	 *
	 * @param key key of parent of subtree to create/replace
	 * @param subtree new subtree to add to tree
	 * @exception RuntimeException receiver is immutable
	 */
	public abstract void createSubtree(IPath key, AbstractDataTreeNode subtree);

	/**
	 * Deletes a child from the tree.
	 *
	 * <p>Note: this method requires both parentKey and localName,
	 * making it impossible to delete the root node.
	 *
	 * @param parentKey parent of node to delete.
	 * @param localName name of node to delete.
	 * @exception ObjectNotFoundException
	 *	a child of parentKey with name localName does not exist in the receiver
	 * @exception RuntimeException
	 *	receiver is immutable
	 */
	public abstract void deleteChild(IPath parentKey, String localName);

	/**
	 * Returns the key of a node in the tree.
	 *
	 * @param parentKey
	 *	parent of child to retrieve.
	 * @param index
	 *	index of the child to retrieve in its parent.
	 * @exception ObjectNotFoundException
	 * 	parentKey does not exist in the receiver
	 * @exception ArrayIndexOutOfBoundsException
	 *	if no child with the given index (runtime exception)
	 */
	public IPath getChild(IPath parentKey, int index) {
		/* Get name of given child of the parent */
		String child = getNameOfChild(parentKey, index);
		return parentKey.append(child);
	}

	/**
	 * Returns the number of children of a node
	 *
	 * @param parentKey key of the node for which we want to retreive the number of
	 *                  children
	 * @exception ObjectNotFoundException parentKey does not exist in the receiver
	 */
	abstract int getChildCount(IPath parentKey);

	/**
	 * Returns the keys of all children of a node.
	 *
	 * @param parentKey key of parent whose children we want to retrieve.
	 * @exception ObjectNotFoundException parentKey does not exist in the receiver
	 */
	abstract IPath[] getChildren(IPath parentKey);

	/**
	 * Returns the data of a node.
	 *
	 * @param key key of node for which we want to retrieve data.
	 * @exception ObjectNotFoundException key does not exist in the receiver
	 */
	public abstract Object getData(IPath key);

	/**
	 * Returns the local name of a node in the tree
	 *
	 * @param parentKey
	 *	parent of node whose name we want to retrieve
	 * @param index
	 *	index of node in its parent
	 * @exception ObjectNotFoundException
	 *	parentKey does not exist in the receiver
	 * @exception ArrayIndexOutOfBoundsException
	 *	if no child with the given index
	 */
	abstract String getNameOfChild(IPath parentKey, int index);

	/**
	 * Returns the local names for the children of a node
	 *
	 * @param parentKey
	 *	key of node whose children we want to retrieve
	 * @exception ObjectNotFoundException
	 *	parentKey does not exist in the receiver
	 */
	public abstract String[] getNamesOfChildren(IPath parentKey);

	/**
	 * Handles the case where an attempt was made to modify the tree when it was in
	 * an immutable state. Throws an unchecked exception.
	 */
	static void handleImmutableTree() {
		throw new RuntimeException(Messages.dtree_immutable);
	}

	/**
	 * Handles the case where an attempt was made to manipulate
	 * an element in the tree that does not exist.  Throws an
	 * unchecked exception.
	 */
	static void handleNotFound(IPath key) {
		throw new ObjectNotFoundException(NLS.bind(Messages.dtree_notFound, key));
	}

	/**
	 * Makes the tree immutable
	 */
	public void immutable() {
		immutable = true;
	}

	/**
	 * Returns true if the receiver includes a node with the given key, false
	 * otherwise.
	 *
	 * @param key
	 *	key of node to find
	 */
	public abstract boolean includes(IPath key);

	/**
	 * Returns true if the tree is immutable, and false otherwise.
	 */
	public boolean isImmutable() {
		return immutable;
	}

	/**
	 * Returns an object containing:
	 * 	- a flag indicating whether the specified node was found
	 *  - the data for the node, if it was found
	 * @param key
	 *	key of node for which we want to retrieve data.
	 */
	public abstract DataTreeLookup lookup(IPath key);

	/**
	 * Returns the key of the root node.
	 */
	public IPath rootKey() {
		return Path.ROOT;
	}

	/**
	 * Sets the data of a node.
	 *
	 * @param key
	 *	key of node for which to set data
	 * @param data
	 *	new data value for node
	 * @exception ObjectNotFoundException
	 *	the nodeKey does not exist in the receiver
	 * @exception IllegalArgumentException
	 *	receiver is immutable
	 */
	public abstract void setData(IPath key, Object data);

	/**
	 * Sets the immutable field.
	 */
	void setImmutable(boolean bool) {
		immutable = bool;
	}

	/**
	 * Sets the root node of the tree.
	 *
	 * <p>Both subclasses must be able to set their root node.  However subclasses
	 * can have different types of root nodes, so this is not enforced as an abstract
	 * method
	 */
	void setRootNode(AbstractDataTreeNode node) {
		throw new Error(Messages.dtree_subclassImplement);
	}
}
