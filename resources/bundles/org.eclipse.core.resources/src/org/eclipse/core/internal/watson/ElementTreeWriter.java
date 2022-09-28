/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
 *     Mickael Istria (Red Hat Inc.) - Bug 488937
 *******************************************************************************/
package org.eclipse.core.internal.watson;

import java.io.*;
import java.util.*;
import org.eclipse.core.internal.dtree.*;
import org.eclipse.core.internal.resources.SaveManager;
import org.eclipse.core.runtime.*;

/** <code>ElementTreeWriter</code> flattens an ElementTree
 * onto a data output stream.
 *
 * <p>This writer generates the most up-to-date format
 * of a saved element tree (cf. readers, which must usually also
 * deal with backward compatibility issues).  The flattened
 * representation always includes a format version number.
 *
 * <p>The writer has an <code>IElementInfoFactory</code>,
 * which it consults for writing element infos.
 *
 * <p>Element tree writers are thread-safe; several
 * threads may share a single writer.
 *
 */
public class ElementTreeWriter {
	/**
	 * The current format version number.
	 */
	public static final int CURRENT_FORMAT = 1;

	/**
	 * Constant representing infinite depth
	 */
	public static final int D_INFINITE = DataTreeWriter.D_INFINITE;

	/**
	 * For writing DeltaDataTrees
	 */
	protected DataTreeWriter dataTreeWriter;

	/**
	 * Constructs a new element tree writer that works for
	 * the given element info flattener.
	 */
	public ElementTreeWriter(final IElementInfoFlattener flattener) {

		/* wrap the IElementInfoFlattener in an IDataFlattener */
		IDataFlattener f = new IDataFlattener() {
			@Override
			public void writeData(IPath path, Object data, DataOutput output) throws IOException {
				// never write the root node of an ElementTree
				//because it contains the parent backpointer.
				if (!Path.ROOT.equals(path)) {
					flattener.writeElement(path, data, output);
				}
			}

			@Override
			public Object readData(IPath path, DataInput input) {
				return null;
			}
		};
		dataTreeWriter = new DataTreeWriter(f);
	}

	/**
	 * Writes indexes of sorted trees to output stream.
	 *
	 * @return sorted array of trees.
	 * @throws IOException if sorting can not be done.
	 * @see SaveManager#sortTrees
	 */
	protected ElementTree[] writeSortedTrees(ElementTree[] trees, DataOutput output) throws IOException {
		ElementTree[] sorted = SaveManager.sortTrees(trees);
		if (sorted == null) {
			throw new IOException("Unable to save workspace - Trees in ambiguous order (Bug 352867)"); //$NON-NLS-1$
		}

		// compute indexes from sorted elements:
		int numTrees = trees.length;
		Map<ElementTree, Deque<Integer>> indicesByTree = new HashMap<>();
		for (int i = 0; i < numTrees; i++) {
			indicesByTree.computeIfAbsent(trees[i], k -> new ArrayDeque<>()).push(i);
		}

		/* write the order array */
		for (int i = 0; i < numTrees; i++) {
			Integer order = indicesByTree.get(sorted[i]).pop();
			writeNumber(order, output);
		}
		return sorted;
	}

	/**
	 * Writes the delta describing the changes that have to be made
	 * to newerTree to obtain olderTree.
	 *
	 * @param path The path of the subtree to write.  All nodes on the path above
	 *  the subtree are represented as empty nodes.
	 * @param depth The depth of the subtree to write.  A depth of zero writes a
	 *  single node, and a depth of D_INFINITE writes the whole subtree.
	 * @param output The stream to write the subtree to.
	 */
	public void writeDelta(ElementTree olderTree, ElementTree newerTree, IPath path, int depth, final DataOutput output, IElementComparator comparator) throws IOException {

		/* write the version number */
		writeNumber(CURRENT_FORMAT, output);

		/**
		 * Note that in current ElementTree usage, the newest
		 * tree is the complete tree, and older trees are just
		 * deltas on the new tree.
		 */
		DeltaDataTree completeTree = newerTree.getDataTree();
		DeltaDataTree derivedTree = olderTree.getDataTree();
		DeltaDataTree deltaToWrite = completeTree.forwardDeltaWith(derivedTree, comparator);

		Assert.isTrue(deltaToWrite.isImmutable());
		dataTreeWriter.writeTree(deltaToWrite, path, depth, output);
	}

	/**
	 * Writes an array of ElementTrees to the given output stream.
	 * @param trees A chain of ElementTrees, where on tree in the list is
	 * complete, and all other trees are deltas on the previous tree in the list.
	 * @param path The path of the subtree to write.  All nodes on the path above
	 *  the subtree are represented as empty nodes.
	 * @param depth The depth of the subtree to write.  A depth of zero writes a
	 *  single node, and a depth of D_INFINITE writes the whole subtree.
	 * @param output The stream to write the subtree to.

	 */
	public void writeDeltaChain(ElementTree[] trees, IPath path, int depth, DataOutput output, IElementComparator comparator) throws IOException {
		/* Write the format version number */
		writeNumber(CURRENT_FORMAT, output);

		/* Write the number of trees */
		int treeCount = trees.length;
		writeNumber(treeCount, output);

		if (treeCount <= 0) {
			return;
		}

		/**
		 * Sort the trees in ancestral order,
		 * which writes the tree order to the output
		 */
		ElementTree[] sortedTrees = writeSortedTrees(trees, output);

		/* Write the complete tree */
		writeTree(sortedTrees[0], path, depth, output);

		/* Write the deltas for each of the remaining trees */
		for (int i = 1; i < treeCount; i++) {
			writeDelta(sortedTrees[i], sortedTrees[i - 1], path, depth, output, comparator);
		}
	}

	/**
	 * Writes an integer in a compact format biased towards
	 * small non-negative numbers. Numbers between
	 * 0 and 254 inclusive occupy 1 byte; other numbers occupy 5 bytes.
	 */
	protected void writeNumber(int number, DataOutput output) throws IOException {
		if (number >= 0 && number < 0xff) {
			output.writeByte(number);
		} else {
			output.writeByte(0xff);
			output.writeInt(number);
		}
	}

	/**
	 * Writes all or some of an element tree to an output stream.
	 * This always writes the most current version of the element tree
	 * file format, whereas the reader supports multiple versions.
	 *
	 * @param tree The tree to write
	 * @param path The path of the subtree to write.  All nodes on the path above
	 *  the subtree are represented as empty nodes.
	 * @param depth The depth of the subtree to write.  A depth of zero writes a
	 *  single node, and a depth of D_INFINITE writes the whole subtree.
	 * @param output The stream to write the subtree to.
	 */
	public void writeTree(ElementTree tree, IPath path, int depth, final DataOutput output) throws IOException {

		/* Write the format version number. */
		writeNumber(CURRENT_FORMAT, output);

		/* This actually just copies the root node, which is what we want */
		DeltaDataTree subtree = new DeltaDataTree(tree.getDataTree().copyCompleteSubtree(Path.ROOT));

		dataTreeWriter.writeTree(subtree, path, depth, output);
	}
}
