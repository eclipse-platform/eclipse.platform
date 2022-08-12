/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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

import java.io.DataOutput;
import java.io.IOException;
import org.eclipse.core.runtime.*;

/**
 * Class for writing a single data tree (no parents) to an output stream.
 */
public class DataTreeWriter {
	/**
	 * Callback for serializing tree data
	 */
	protected IDataFlattener flatener;

	/**
	 * The stream to write output to
	 */
	protected DataOutput output;

	/**
	 * Constant representing infinite recursion depth
	 */
	public static final int D_INFINITE = -1;

	/**
	 * Creates a new DeltaTreeWriter.
	 */
	public DataTreeWriter(IDataFlattener f) {
		flatener = f;
	}

	/**
	 * Writes the subtree rooted at the given node.
	 * @param node The subtree to write.
	 * @param path  The path of the current node.
	 * @param depth The depth of the subtree to write.
	 */
	protected void writeNode(AbstractDataTreeNode node, IPath path, int depth) throws IOException {
		int type = node.type();

		/* write the node name */
		String name = node.getName();
		if (name == null) {
			name = ""; //$NON-NLS-1$
		}
		output.writeUTF(name);

		/* write the node type */
		writeNumber(type);

		/* maybe write the data */
		if (node.hasData()) {
			Object data = node.getData();

			/**
			 * Write a flag indicating whether or not the data field is null.
			 * Zero means data is null, non-zero means data is present
			 */
			if (data == null) {
				writeNumber(0);
			} else {
				writeNumber(1);
				flatener.writeData(path, node.getData(), output);
			}

		}

		/* maybe write the children */
		if (depth > 0 || depth == D_INFINITE) {
			AbstractDataTreeNode[] children = node.getChildren();

			/* write the number of children */
			writeNumber(children.length);

			/* write the children */
			int newDepth = (depth == D_INFINITE) ? D_INFINITE : depth - 1;
			for (AbstractDataTreeNode element : children) {
				writeNode(element, path.append(element.getName()), newDepth);
			}
		} else {
			/* write the number of children */
			writeNumber(0);
		}
	}

	/**
	 * Writes an integer in a compact format biased towards
	 * small non-negative numbers. Numbers between
	 * 0 and 254 inclusive occupy 1 byte; other numbers occupy 5 bytes.
	 */
	protected void writeNumber(int number) throws IOException {
		if (number >= 0 && number < 0xff) {
			output.writeByte(number);
		} else {
			output.writeByte(0xff);
			output.writeInt(number);
		}
	}

	/**
	 * Writes a single node to the output.  Does not recurse
	 * on child nodes, and does not write the number of children.
	 */
	protected void writeSingleNode(AbstractDataTreeNode node, IPath path) throws IOException {
		/* write the node name */
		String name = node.getName();
		if (name == null) {
			name = ""; //$NON-NLS-1$
		}
		output.writeUTF(name);

		/* write the node type */
		writeNumber(node.type());

		/* maybe write the data */
		if (node.hasData()) {
			Object data = node.getData();

			/**
			 * Write a flag indicating whether or not the data field is null.
			 * Zero means data is null, non-zero means data is present
			 */
			if (data == null) {
				writeNumber(0);
			} else {
				writeNumber(1);
				flatener.writeData(path, node.getData(), output);
			}
		}
	}

	/**
	 * Writes the given AbstractDataTree to the given stream.  This
	 * writes a single DataTree or DeltaDataTree, ignoring parent
	 * trees.
	 *
	 * @param path Only writes data for the subtree rooted at the given path, and
	 * for all nodes directly between the root and the subtree.
	 * @param depth In the subtree rooted at the given path,
	 *  only write up to this depth.  A depth of infinity is given
	 *  by the constant D_INFINITE.
	 */
	public void writeTree(DeltaDataTree tree, IPath path, int depth, DataOutput output) throws IOException {
		this.output = output;
		/* tunnel down relevant path */
		AbstractDataTreeNode node = tree.getRootNode();
		IPath currentPath = Path.ROOT;
		String[] segments = path.segments();
		for (String nextSegment : segments) {
			/* write this node to the output */
			writeSingleNode(node, currentPath);

			currentPath = currentPath.append(nextSegment);
			node = node.childAtOrNull(nextSegment);

			/* write the number of children for this node */
			if (node != null) {
				writeNumber(1);
			} else {
				/* can't navigate down the path, just give up with what we have so far */
				writeNumber(0);
				return;
			}
		}

		Assert.isTrue(currentPath.equals(path), "dtree.navigationError"); //$NON-NLS-1$

		/* recursively write the subtree we're interested in */
		writeNode(node, path, depth);
	}
}
