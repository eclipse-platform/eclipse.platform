/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *******************************************************************************/
package org.eclipse.core.internal.watson;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.core.internal.dtree.*;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.internal.utils.StringPool;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;

/**
 * <p>
 * An ElementTree can be viewed as a generic rooted tree that stores a hierarchy
 * of elements. An element in the tree consists of a (name, data, children)
 * 3-tuple. The name ({@link #getNamesOfChildren(IPath)}) can be any String, and
 * the data ({@link #getElementData(IPath)}) can be any Object. The children
 * ({@link #getChildren(IPath)}) are a collection of zero or more elements that
 * logically fall below their parent in the tree. The implementation makes no
 * guarantees about the ordering of children.
 * </p>
 *
 * <p>
 * Elements in the tree are referenced by a key that consists of the names of
 * all elements on the path from the root to that element in the tree. For
 * example, if root node "a" has child "b", which has child "c", element "c" can
 * be referenced in the tree using the key (/a/b/c). Keys are represented using
 * {@link IPath} objects, where the Paths are relative to the root element of
 * the tree.
 * </p>
 *
 * <p>
 * Each ElementTree has a single root element that is created implicitly and is
 * always present in any tree. This root corresponds to the key (/), or the
 * singleton <code>Path.ROOT</code>. The root element cannot be created or
 * deleted, and its data and name cannot be set. The root element's children
 * however can be modified (added, deleted, etc). The root path can be obtained
 * using the <code>getRoot()</code> method.
 * </p>
 *
 * <p>
 * ElementTrees are modified in generations. The method {@link #newEmptyDelta()}
 * returns a new tree generation that can be modified arbitrarily by the user.
 * For the purpose of explanation, we call such a tree "active". When the method
 * {@link #immutable()} is called, that tree generation is frozen, and can never
 * again be modified. A tree must be immutable before a new tree generation can
 * start. Since all ancestor trees are immutable, different active trees can
 * have ancestors in common without fear of thread corruption problems.
 * </p>
 *
 * <p>
 * Internally, any single tree generation is simply stored as the set of changes
 * between itself and its most recent ancestor (its parent). This compact delta
 * representation allows chains of element trees to be created at relatively low
 * cost. Clients of the ElementTree can instantaneously "undo" sets of changes
 * by navigating up to the parent tree using the {@link #getParent()} method.
 * </p>
 *
 * <p>
 * Although the delta representation is compact, extremely long delta chains
 * make for a large structure that is potentially slow to query. For this
 * reason, the client is encouraged to minimize delta chain lengths using the
 * {@link #collapseTo(ElementTree)} and {@link DeltaDataTree#makeComplete()}
 * methods. The entire delta chain can also be re-oriented in terms of the
 * current element tree using the {@link DeltaDataTree#reroot()} operation.
 * </p>
 *
 * <p>
 * Classes are also available for tree serialization and navigation (
 * {@link ElementTreeReader}, {@link ElementTreeWriter},
 * {@link ElementTreeIterator} )
 * </p>
 *
 * Finally, the package name is called "watson" because - (misquoting Sherlock
 * Holmes)
 *
 * <pre>
 * "It's ElementTree my dear Watson, ElementTree."
 * </pre>
 *
 * @see DeltaDataTree
 */
public class ElementTree {
	protected final DeltaDataTree tree;
	protected volatile IElementTreeData userData;

	private static final class ChildIDsCache {
		ChildIDsCache(IPath path, IPath[] childPaths) {
			this.path = path;
			this.childPaths = childPaths;
		}

		final IPath path;
		final IPath[] childPaths;
	}

	/** synchronized access **/
	private volatile ChildIDsCache childIDsCache = null;

	/** synchronized access **/
	private volatile DataTreeLookup lookupCache = null;

	/** synchronized access **/
	private volatile DataTreeLookup lookupCacheIgnoreCase = null;

	private final static AtomicInteger treeCounter = new AtomicInteger();
	private final int treeStamp;

	/**
	 * Creates a new empty element tree.
	 */
	public ElementTree() {
		this(new DeltaDataTree());
	}

	/**
	 * Creates an element tree given its internal node representation.
	 */
	protected ElementTree(DataTreeNode rootNode) {
		/* create the implicit root node */
		this(new DeltaDataTree(new DataTreeNode(null, null, new AbstractDataTreeNode[] { rootNode })));
	}

	/**
	 * Creates a new element tree with the given data tree as its representation.
	 */
	protected ElementTree(DeltaDataTree newTree) {
		// Keep this element tree as the data of the root node.
		// Useful for canonical results for ElementTree.getParent().
		// see getParent().
		treeStamp = treeCounter.incrementAndGet();
		newTree.setData(newTree.rootKey(), this);
		this.tree = newTree;
	}

	/**
	 * Creates a new empty delta element tree having the
	 * given tree as its parent.
	 */
	protected ElementTree(ElementTree parent) {
		this(parent.tree.newEmptyDeltaTree());

		/* copy the user data forward */
		IElementTreeData data = parent.getTreeData();
		if (data != null) {
			userData = (IElementTreeData) data.clone();
		}
	}

	/**
	 * Collapses this tree so that the given ancestor becomes its
	 * immediate parent.  Afterwards, this tree will still have exactly the
	 * same contents, but its internal structure will be compressed.
	 *
	 * <p> This operation should be used to collapse chains of
	 * element trees created by newEmptyDelta()/immutable().
	 *
	 * <p>This element tree must be immutable at the start of this operation,
	 * and will be immutable afterwards.
	 * @return this tree.
	 */
	public synchronized ElementTree collapseTo(ElementTree parent) {
		Assert.isTrue(tree.isImmutable());
		if (this == parent) {
			//already collapsed
			return this;
		}
		//collapse my tree to be a forward delta of the parent's tree.
		tree.collapseTo(parent.tree, DefaultElementComparator.getComparator());
		return this;
	}

	/**
	 * Creates the indicated element and sets its element info.
	 * The parent element must be present, otherwise an IllegalArgumentException
	 * is thrown. If the indicated element is already present in the tree,
	 * its element info is replaced and any existing children are
	 * deleted.
	 *
	 * @param key element key
	 * @param data element data, or <code>null</code>
	 */
	public synchronized void createElement(IPath key, Object data) {
		/* don't allow modification of the implicit root */
		if (key.isRoot())
			return;

		// Clear the child IDs cache in case it's referring to this parent. This is conservative.
		childIDsCache = null;

		IPath parent = key.removeLastSegments(1);
		try {
			tree.createChild(parent, key.lastSegment(), data);
		} catch (ObjectNotFoundException e) {
			elementNotFound(parent);
		}
		// Set the lookup to be this newly created object.
		lookupCache = DataTreeLookup.newLookup(key, true, data, true);
		lookupCacheIgnoreCase = null;
	}

	/**
	 * Creates or replaces the subtree below the given path with
	 * the given tree. The subtree can only have one child below
	 * the root, which will become the node specified by the given
	 * key in this tree.
	 *
	 * @param key The path of the new subtree in this tree.
	 * @see #getSubtree(IPath)
	 */
	public synchronized void createSubtree(IPath key, ElementTree subtree) {
		/* don't allow creating subtrees at the root */
		if (key.isRoot()) {
			throw new IllegalArgumentException(Messages.watson_noModify);
		}

		// Clear the child IDs cache in case it's referring to this parent.
		// This is conservative.
		childIDsCache = null;
		// Clear the lookup cache, in case the element being created is the same
		// as for the last lookup.
		lookupCache = lookupCacheIgnoreCase = null;
		try {
			/* don't copy the implicit root node of the subtree */
			IPath[] children = subtree.getChildren(subtree.getRoot());
			if (children.length != 1) {
				throw new IllegalArgumentException(Messages.watson_illegalSubtree);
			}

			/* get the subtree for the specified key */
			DataTreeNode node = (DataTreeNode) subtree.tree.copyCompleteSubtree(children[0]);

			/* insert the subtree in this tree */
			tree.createSubtree(key, node);

		} catch (ObjectNotFoundException e) {
			elementNotFound(key);
		}
	}

	/**
	 * Deletes the indicated element and its descendents.
	 * The element must be present.
	 */
	public synchronized void deleteElement(IPath key) {
		/* don't allow modification of the implicit root */
		if (key.isRoot())
			return;

		// Clear the child IDs cache in case it's referring to this parent.
		// This is conservative.
		childIDsCache = null;
		// Clear the lookup cache, in case the element being deleted is the same
		// as for the last lookup.
		lookupCache = lookupCacheIgnoreCase = null;
		try {
			tree.deleteChild(key.removeLastSegments(1), key.lastSegment());
		} catch (ObjectNotFoundException e) {
			elementNotFound(key);
		}
	}

	/**
	 * Complains that an element was not found
	 */
	protected void elementNotFound(IPath key) {
		throw new IllegalArgumentException(NLS.bind(Messages.watson_elementNotFound, key));
	}

	/**
	 * Given an array of element trees, returns the index of the
	 * oldest tree.  The oldest tree is the tree such that no
	 * other tree in the array is a descendent of that tree.
	 * Note that this counter-intuitive concept of oldest is based on the
	 * ElementTree orientation such that the complete tree is always the
	 * newest tree.
	 */
	public static int findOldest(ElementTree[] trees) {

		/* first put all the trees in a hashtable */
		// using LinkedHashMap to not have a random order in the while-loop below
		HashSet<ElementTree> candidates = new LinkedHashSet<>(List.of(trees));

		/* keep removing parents until only one tree remains */
		ElementTree oldestSoFar = null;
		while (!candidates.isEmpty()) {
			/* get a new candidate */
			ElementTree current = candidates.iterator().next();

			/* remove this candidate from the table */
			candidates.remove(current);

			/* remove all of this element's parents from the list of candidates*/
			ElementTree parent = current.getParent();

			/* walk up chain until we hit the root or a tree we have already tested */
			while (parent != null && parent != oldestSoFar) {
				candidates.remove(parent);
				parent = parent.getParent();
			}

			/* the current candidate is the oldest tree seen so far */
			oldestSoFar = current;

			/* if the table is now empty, we have a winner */
		}
		Assert.isNotNull(oldestSoFar);

		/* return the appropriate index */
		for (int i = 0; i < trees.length; i++) {
			if (trees[i] == oldestSoFar) {
				return i;
			}
		}
		Assert.isTrue(false, "Should not get here"); //$NON-NLS-1$
		return -1;
	}

	/**
	 * Returns the number of children of the element
	 * specified by the given path.
	 * The given element must be present in this tree.
	 */
	public synchronized int getChildCount(IPath key) {
		Assert.isNotNull(key);
		return getChildIDs(key).length;
	}

	/**
	 * Returns the IDs of the children of the specified element.
	 * If the specified element is null, returns the root element path.
	 */
	protected IPath[] getChildIDs(IPath key) {
		ChildIDsCache cache = childIDsCache; // Grab it in case it's replaced concurrently.
		if (cache != null && cache.path == key) {
			return cache.childPaths;
		}
		try {
			if (key == null)
				return new IPath[] {tree.rootKey()};
			IPath[] children = tree.getChildren(key);
			childIDsCache = new ChildIDsCache(key, children); // Cache the result
			return children;
		} catch (ObjectNotFoundException e) {
			elementNotFound(key);
			return null; // can't get here
		}
	}

	/**
	 * Returns the paths of the children of the element
	 * specified by the given path.
	 * The given element must be present in this tree.
	 */
	public synchronized IPath[] getChildren(IPath key) {
		Assert.isNotNull(key);
		return getChildIDs(key);
	}

	/**
	 * Returns the internal data tree.
	 */
	public DeltaDataTree getDataTree() {
		return tree;
	}

	/**
	 * Returns the element data for the given element identifier.
	 * The given element must be present in this tree.
	 */
	public synchronized Object getElementData(IPath key) {
		/* don't allow modification of the implicit root */
		if (key.isRoot())
			return null;
		DataTreeLookup lookup = lookupCache; // Grab it in case it's replaced concurrently.
		if (lookup == null || lookup.key != key)
			lookupCache = lookup = tree.lookup(key);
		if (lookup.isPresent)
			return lookup.data;
		elementNotFound(key);
		return null; // can't get here
	}

	/**
	 * Returns the element data for the given element identifier.
	 * The given element must be present in this tree.
	 */
	public synchronized Object getElementDataIgnoreCase(IPath key) {
		/* don't allow modification of the implicit root */
		if (key.isRoot())
			return null;
		DataTreeLookup lookup = lookupCacheIgnoreCase; // Grab it in case it's replaced concurrently.
		if (lookup == null || lookup.key != key)
			lookupCacheIgnoreCase = lookup = tree.lookupIgnoreCase(key);
		if (lookup.isPresent)
			return lookup.data;
		elementNotFound(key);
		return null; // can't get here
	}

	/**
	 * Returns the names of the children of the specified element.
	 * The specified element must exist in the tree.
	 * If the specified element is null, returns the root element path.
	 */
	public synchronized String[] getNamesOfChildren(IPath key) {
		try {
			if (key == null)
				return new String[] {""}; //$NON-NLS-1$
			return tree.getNamesOfChildren(key);
		} catch (ObjectNotFoundException e) {
			elementNotFound(key);
			return null; // can't get here
		}
	}

	/**
	 * Returns the parent tree, or <code>null</code> if there is no parent.
	 */
	public ElementTree getParent() {
		DeltaDataTree parentTree = tree.getParent();
		if (parentTree == null) {
			return null;
		}
		// The parent ElementTree is stored as the node data of the parent DeltaDataTree,
		// to simplify canonicalization in the presence of rerooting.
		return (ElementTree) parentTree.getData(tree.rootKey());
	}

	/**
	 * Returns the root node of this tree.
	 */
	public IPath getRoot() {
		return getChildIDs(null)[0];
	}

	/**
	 * Returns the subtree rooted at the given key. In the resulting tree,
	 * the implicit root node (designated by Path.ROOT), has a single child,
	 * which is the node specified by the given key in this tree.
	 *
	 * The subtree must be present in this tree.
	 *
	 * @see #createSubtree(IPath, ElementTree)
	 */
	public ElementTree getSubtree(IPath key) {
		/* the subtree of the root of this tree is just this tree */
		if (key.isRoot()) {
			return this;
		}
		try {
			DataTreeNode elementNode = (DataTreeNode) tree.copyCompleteSubtree(key);
			return new ElementTree(elementNode);
		} catch (ObjectNotFoundException e) {
			elementNotFound(key);
			return null;
		}
	}

	/**
	 * Returns the user data associated with this tree.
	 */
	public IElementTreeData getTreeData() {
		return userData;
	}

	/**
	 * Returns true if there have been changes in the tree between the two
	 * given layers.  The two must be related and new must be newer than old.
	 * That is, new must be an ancestor of old.
	 */
	public static boolean hasChanges(ElementTree newLayer, ElementTree oldLayer, IElementComparator comparator, boolean inclusive) {
		// if any of the layers are null, assume that things have changed
		if (newLayer == null || oldLayer == null)
			return true;
		if (newLayer == oldLayer)
			return false;
		//if the tree data has changed, then the tree has changed
		if (comparator.compare(newLayer.getTreeData(), oldLayer.getTreeData()) != IElementComparator.K_NO_CHANGE)
			return true;

		// The tree structure has the top layer(s) (i.e., tree) parentage pointing down to a complete
		// layer whose parent is null.  The bottom layers (i.e., operationTree) point up to the
		// common complete layer whose parent is null.  The complete layer moves up as
		// changes happen.  To see if any changes have happened, we should consider only
		// layers whose parent is not null.  That is, skip the complete layer as it will clearly not be
		// empty.

		// look down from the current layer (always inclusive) if the top layer is mutable
		ElementTree stopLayer = null;
		if (newLayer.isImmutable())
			// if the newLayer is immutable, the tree structure all points up so ensure that
			// when searching up, we stop at newLayer (inclusive)
			stopLayer = newLayer.getParent();
		else {
			ElementTree layer = newLayer;
			while (layer != null && layer.getParent() != null) {
				if (!layer.getDataTree().isEmptyDelta())
					return true;
				layer = layer.getParent();
			}
		}

		// look up from the layer at which we started to null or newLayer's parent (variably inclusive)
		// depending on whether newLayer is mutable.
		ElementTree layer = inclusive ? oldLayer : oldLayer.getParent();
		while (layer != null && layer.getParent() != stopLayer) {
			if (!layer.getDataTree().isEmptyDelta())
				return true;
			layer = layer.getParent();
		}
		// didn't find anything that changed
		return false;
	}

	/**
	 * Makes this tree immutable (read-only); ignored if it is already
	 * immutable.
	 */
	public synchronized void immutable() {
		if (!tree.isImmutable()) {
			tree.immutable();
			/* need to clear the lookup cache since it reports whether results were found
			 in the topmost delta, and the order of deltas is changing */
			lookupCache = lookupCacheIgnoreCase = null;
			/* reroot the delta chain at this tree */
			tree.reroot();
		}
	}

	/**
	 * Returns true if this element tree includes an element with the given
	 * key, false otherwise.
	 */
	public synchronized boolean includes(IPath key) {
		DataTreeLookup lookup = lookupCache; // Grab it in case it's replaced concurrently.
		if (lookup == null || lookup.key != key) {
			lookupCache = lookup = tree.lookup(key);
		}
		return lookup.isPresent;
	}

	/**
	 * Returns true if this element tree includes an element with the given
	 * key, ignoring the case of the key, and false otherwise.
	 */
	public synchronized boolean includesIgnoreCase(IPath key) {
		DataTreeLookup lookup = lookupCacheIgnoreCase; // Grab it in case it's replaced concurrently.
		if (lookup == null || lookup.key != key) {
			lookupCacheIgnoreCase = lookup = tree.lookupIgnoreCase(key);
		}
		return lookup.isPresent;
	}

	/**
	 * Returns whether this tree is immutable.
	 */
	public boolean isImmutable() {
		return tree.isImmutable();
	}

	/**
	 * Merges a chain of deltas for a certain subtree to this tree.
	 * If this tree has any data in the specified subtree, it will
	 * be overwritten.  The receiver tree must be open, and it will
	 * be made immutable during the merge operation.  The trees in the
	 * provided array will be replaced by new trees that have been
	 * merged into the receiver's delta chain.
	 *
	 * @param path The path of the subtree chain to merge
	 * @param trees The chain of trees to merge.  The trees can be
	 *  in any order, but they must all form a simple ancestral chain.
	 * @return A new open tree with the delta chain merged in.
	 */
	public ElementTree mergeDeltaChain(IPath path, ElementTree[] trees) {
		if (path == null || trees == null) {
			throw new IllegalArgumentException(NLS.bind(Messages.watson_nullArg, "ElementTree.mergeDeltaChain")); //$NON-NLS-1$
		}

		/* The tree has to be open */
		if (isImmutable()) {
			throw new IllegalArgumentException(Messages.watson_immutable);
		}
		ElementTree current = this;
		if (trees.length > 0) {
			/* find the oldest tree to be merged */
			ElementTree toMerge = trees[findOldest(trees)];

			/* merge the trees from oldest to newest */
			while (toMerge != null) {
				if (path.isRoot()) {
					//copy all the children
					IPath[] children = toMerge.getChildren(Path.ROOT);
					for (IPath element : children) {
						current.createSubtree(element, toMerge.getSubtree(element));
					}
				} else {
					//just copy the specified node
					current.createSubtree(path, toMerge.getSubtree(path));
				}
				current.immutable();

				/* replace the tree in the array */
				/* we have to go through all trees because there may be duplicates */
				for (int i = 0; i < trees.length; i++) {
					if (trees[i] == toMerge) {
						trees[i] = current;
					}
				}
				current = current.newEmptyDelta();
				toMerge = toMerge.getParent();
			}
		}
		return current;
	}

	/**
	 * Creates a new element tree which is represented as a delta on this one.
	 * Initially they have the same content.  Subsequent changes to the new
	 * tree will not affect this one.
	 */
	public synchronized ElementTree newEmptyDelta() {
		// Don't want old trees hanging onto cached infos.
		lookupCache = lookupCacheIgnoreCase = null;
		if (!this.isImmutable()) {
			this.immutable();
		}
		return new ElementTree(this);
	}

	/**
	 * Returns a mutable copy of the element data for the given path.
	 * This copy will be held onto in the most recent delta.
	 * ElementTree data MUST implement the IElementTreeData interface
	 * for this method to work.  If the data does not define that interface
	 * this method will fail.
	 */
	public synchronized Object openElementData(IPath key) {
		Assert.isTrue(!isImmutable());

		/* don't allow modification of the implicit root */
		if (key.isRoot())
			return null;
		DataTreeLookup lookup = lookupCache; // Grab it in case it's replaced concurrently.
		if (lookup == null || lookup.key != key) {
			lookupCache = lookup = tree.lookup(key);
		}
		if (lookup.isPresent) {
			if (lookup.foundInFirstDelta)
				return lookup.data;
			/**
			 * The node has no data in the most recent delta.
			 * Pull it up to the present delta by setting its data with a clone.
			 */
			IElementTreeData oldData = (IElementTreeData) lookup.data;
			if (oldData != null) {
				try {
					Object newData = oldData.clone();
					tree.setData(key, newData);
					lookupCache = lookupCacheIgnoreCase = null;
					return newData;
				} catch (ObjectNotFoundException e) {
					elementNotFound(key);
				}
			}
		} else {
			elementNotFound(key);
		}
		return null;
	}

	/**
	 * Sets the element for the given element identifier.
	 * The given element must be present in this tree.
	 * @param key element identifier
	 * @param data element info, or <code>null</code>
	 */
	public synchronized void setElementData(IPath key, Object data) {
		/* don't allow modification of the implicit root */
		if (key.isRoot())
			return;

		Assert.isNotNull(key);
		// Clear the lookup cache, in case the element being modified is the same
		// as for the last lookup.
		lookupCache = lookupCacheIgnoreCase = null;
		try {
			tree.setData(key, data);
		} catch (ObjectNotFoundException e) {
			elementNotFound(key);
		}
	}

	/**
	 * Sets the user data associated with this tree.
	 */
	public void setTreeData(IElementTreeData data) {
		userData = data;
	}

	/* (non-Javadoc)
	 * Method declared on IStringPoolParticipant
	 */
	public void shareStrings(StringPool set) {
		tree.storeStrings(set);
	}

	/**
	 * Returns a string representation of this element tree's
	 * structure suitable for debug purposes.
	 */
	public String toDebugString() {
		final StringBuilder buffer = new StringBuilder("\n"); //$NON-NLS-1$
		IElementContentVisitor visitor = (aTree, elementID, elementContents) -> {
			buffer.append(elementID.requestPath() + " " + elementContents + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
			return true;
		};
		new ElementTreeIterator(this, Path.ROOT).iterate(visitor);
		return buffer.toString();
	}

	@Override
	public String toString() {
		ElementTree root = getParent();
		while (root != null && root.getParent() != null) {
			root = root.getParent();
		}
		return "ElementTree(" + treeStamp + "->" + (getParent() == null ? null : getParent().treeStamp) + "..." //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ (root == null ? null : root.treeStamp) + ")"; //$NON-NLS-1$
	}

	public int getTreeStamp() {
		return treeStamp;
	}
}
