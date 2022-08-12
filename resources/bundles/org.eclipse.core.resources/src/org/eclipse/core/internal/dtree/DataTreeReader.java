/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
 * Francis Lynch (Wind River) - [305718] Allow reading snapshot into renamed project
 *******************************************************************************/
package org.eclipse.core.internal.dtree;

import java.io.DataInput;
import java.io.IOException;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.runtime.*;

/**
 * Class used for reading a single data tree (no parents) from an input stream
 */
public class DataTreeReader {
	/**
	 * Callback for reading tree data
	 */
	protected IDataFlattener flatener;

	/**
	 * The stream to read the tree from
	 */
	protected DataInput input;

	/**
	 * Creates a new DeltaTreeReader.
	 */
	public DataTreeReader(IDataFlattener f) {
		flatener = f;
	}

	/**
	 * Returns true if the given node type has data.
	 */
	protected boolean hasData(int nodeType) {
		switch (nodeType) {
			case AbstractDataTreeNode.T_COMPLETE_NODE :
			case AbstractDataTreeNode.T_DELTA_NODE :
				return true;
			case AbstractDataTreeNode.T_DELETED_NODE :
			case AbstractDataTreeNode.T_NO_DATA_DELTA_NODE :
			default :
				return false;
		}
	}

	/**
	 * Reads a node from the given input stream.
	 * If newProjectName is non-empty, use it for the name of
	 * the project (first node under root) in the created node
	 * instead of the name read from the stream.
	 */
	protected AbstractDataTreeNode readNode(IPath parentPath, String newProjectName) throws IOException {
		/* read the node name */
		String name = input.readUTF();

		/* read the node type */
		int nodeType = readNumber();

		/* maybe read the data */
		IPath path;

		/* if not the root node */
		if (parentPath != null) {
			if (parentPath.equals(Path.ROOT) && newProjectName.length() > 0 && name.length() > 0) {
				/* use the supplied name for the project node */
				name = newProjectName;
			}
			path = parentPath.append(name);
		} else {
			path = Path.ROOT;
		}

		Object data = null;
		if (hasData(nodeType)) {

			/* read flag indicating if the data is null */
			int dataFlag = readNumber();
			if (dataFlag != 0) {
				data = flatener.readData(path, input);
			}
		}

		/* read the number of children */
		int childCount = readNumber();

		/* read the children */
		AbstractDataTreeNode[] children;
		if (childCount == 0) {
			children = AbstractDataTreeNode.NO_CHILDREN;
		} else {
			children = new AbstractDataTreeNode[childCount];
			for (int i = 0; i < childCount; i++) {
				children[i] = readNode(path, newProjectName);
			}
		}

		/* create the appropriate node */
		switch (nodeType) {
			case AbstractDataTreeNode.T_COMPLETE_NODE :
				return new DataTreeNode(name, data, children);
			case AbstractDataTreeNode.T_DELTA_NODE :
				return new DataDeltaNode(name, data, children);
			case AbstractDataTreeNode.T_DELETED_NODE :
				return new DeletedNode(name);
			case AbstractDataTreeNode.T_NO_DATA_DELTA_NODE :
				return new NoDataDeltaNode(name, children);
			default :
				Assert.isTrue(false, Messages.dtree_switchError);
				return null;
		}
	}

	/**
	 * Reads an integer stored in compact format.  Numbers between
	 * 0 and 254 inclusive occupy 1 byte; other numbers occupy 5 bytes,
	 * the first byte being 0xff and the next 4 bytes being the standard
	 * representation of an int.
	 */
	protected int readNumber() throws IOException {
		byte b = input.readByte();
		int number = (b & 0xff); // not a no-op! converts unsigned byte to int

		if (number == 0xff) { // magic escape value
			number = input.readInt();
		}
		return number;
	}

	/**
	 * Reads a DeltaDataTree from the given input stream.
	 * If newProjectName is non-empty, use it for the name of
	 * the project (first node under root) in the returned tree
	 * instead of the name read from the stream.
	 */
	public DeltaDataTree readTree(DeltaDataTree parent, DataInput input, String newProjectName) throws IOException {
		this.input = input;
		AbstractDataTreeNode root = readNode(Path.ROOT, newProjectName);
		return new DeltaDataTree(root, parent);
	}
}
