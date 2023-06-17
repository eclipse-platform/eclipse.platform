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
 *******************************************************************************/
package org.eclipse.core.internal.watson;

import org.eclipse.core.internal.dtree.AbstractDataTreeNode;
import org.eclipse.core.internal.dtree.DataTreeNode;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * A class for performing operations on each element in an element tree.
 * For example, this can be used to print the contents of a tree.
 * <p>
 * When creating an ElementTree iterator, an element tree and root path must be
 * supplied.  When the <code>iterate()</code> method is called, a visitor object
 * must be provided.  The visitor is called once for each node of the tree.  For
 * each node, the visitor is passed the entire tree, the object in the tree at
 * that node, and a callback for requesting the full path of that node.
 * <p>
 * <b>Example:</b>
 * <pre><code>
 * // printing a crude representation of the poster child
 * IElementContentVisitor visitor=
 *     new IElementContentVisitor() {
 *   public boolean visitElement(ElementTree tree, IPathRequestor requestor, Object elementContents) {
 *     System.out.println(requestor.requestPath() + " -&gt; " + elementContents);
 *     return true;
 *   }
 * });
 * ElementTreeIterator iterator = new ElementTreeIterator(tree, Path.ROOT);
 * iterator.iterate(visitor);
 * </code></pre>
 */
public class ElementTreeIterator implements IPathRequestor {
	//for path requestor
	private String[] segments = new String[10];
	private int nextFreeSegment;

	/* the tree being visited */
	private ElementTree tree;

	/* the root of the subtree to visit */
	private IPath path;

	/* the immutable data tree being visited */
	private DataTreeNode treeRoot;

	/**
	 * Creates a new element tree iterator for visiting the given tree starting
	 * at the given path.
	 */
	public ElementTreeIterator(ElementTree tree, IPath path) {
		this.tree = tree;
		this.path = path;
		//treeRoot can be null if deleted concurrently
		//must copy the tree while owning the tree's monitor to prevent concurrent deletion while creating visitor's copy
		synchronized (tree) {
			treeRoot = (DataTreeNode) tree.getDataTree().safeCopyCompleteSubtree(path);
		}
	}

	/**
	 * Iterates through the given element tree and visit each element (node)
	 * passing in the element's ID and element object.
	 */
	private void doIteration(DataTreeNode node, IElementContentVisitor visitor) {
		//push the name of this node to the requestor stack
		if (nextFreeSegment >= segments.length) {
			grow();
		}
		segments[nextFreeSegment++] = node.getName();

		//do the visit
		if (visitor.visitElement(tree, this, node.getData())) {
			//recurse
			AbstractDataTreeNode[] children = node.getChildren();
			int len = children.length;
			for (int i = 0; i < len; i++) {
				doIteration((DataTreeNode) children[i], visitor);
			}
		}

		//pop the segment from the requestor stack
		nextFreeSegment--;
		if (nextFreeSegment < 0)
			nextFreeSegment = 0;
	}

	/**
	 * Method grow.
	 */
	private void grow() {
		//grow the segments array
		int oldLen = segments.length;
		String[] newPaths = new String[oldLen * 2];
		System.arraycopy(segments, 0, newPaths, 0, oldLen);
		segments = newPaths;
	}

	/**
	 * Iterates through this iterator's tree and visits each element in the
	 * subtree rooted at the given path.  The visitor is passed each element's
	 * data and a request callback for obtaining the path.
	 */
	public void iterate(IElementContentVisitor visitor) {
		if (path.isRoot()) {
			//special visit for root element to use special treeData
			if (visitor.visitElement(tree, this, tree.getTreeData())) {
				if (treeRoot == null)
					return;
				AbstractDataTreeNode[] children = treeRoot.getChildren();
				int len = children.length;
				for (int i = 0; i < len; i++) {
					AbstractDataTreeNode node = children[i];
					if (node instanceof DataTreeNode) {
						doIteration((DataTreeNode) node, visitor);
					}
				}
			}
		} else {
			if (treeRoot == null)
				return;
			push(path, path.segmentCount() - 1);
			doIteration(treeRoot, visitor);
		}
	}

	/**
	 * Push the first "toPush" segments of this path.
	 */
	private void push(IPath pathToPush, int toPush) {
		if (toPush <= 0)
			return;
		for (int i = 0; i < toPush; i++) {
			if (nextFreeSegment >= segments.length) {
				grow();
			}
			segments[nextFreeSegment++] = pathToPush.segment(i);
		}
	}

	@Override
	public String requestName() {
		if (nextFreeSegment == 0)
			return ""; //$NON-NLS-1$
		return segments[nextFreeSegment - 1];
	}

	@Override
	public IPath requestPath() {
		if (nextFreeSegment == 0) {
			return IPath.ROOT;
		}
		int length = nextFreeSegment;
		for (int i = 0; i < nextFreeSegment; i++) {
			length += segments[i].length();
		}
		StringBuilder pathBuf = new StringBuilder(length);
		for (int i = 0; i < nextFreeSegment; i++) {
			pathBuf.append('/');
			pathBuf.append(segments[i]);
		}
		return new Path(null, pathBuf.toString());
	}
}
