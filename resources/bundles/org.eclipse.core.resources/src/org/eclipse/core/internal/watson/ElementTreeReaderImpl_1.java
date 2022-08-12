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
 * Francis Lynch (Wind River) - [305718] Allow reading snapshot into renamed project
 *******************************************************************************/
package org.eclipse.core.internal.watson;

import java.io.DataInput;
import java.io.IOException;
import org.eclipse.core.internal.dtree.DeltaDataTree;

/** <code>ElementTreeReader_1</code> is an implementation
 * of the <code>ElementTreeReader</code> for format version 1.
 *
 * <p>Instances of this reader read only format 1
 * of a saved element tree (they do not deal with
 * compatibility issues).
 *
 * @see ElementTreeReader
 */
/* package */class ElementTreeReaderImpl_1 extends ElementTreeReader {

	/**
	 * Constructs a new element tree reader that works for
	 * the given element info factory.
	 */
	ElementTreeReaderImpl_1(IElementInfoFlattener factory) {
		super(factory);
	}

	/**
	 * Reads an element tree delta from the input stream, and
	 * reconstructs it as a delta on the given tree.
	 */
	@Override
	public ElementTree readDelta(ElementTree parentTree, DataInput input) throws IOException {
		DeltaDataTree complete = parentTree.getDataTree();
		DeltaDataTree delta = dataTreeReader.readTree(complete, input, ""); //$NON-NLS-1$

		//if the delta is empty, just return the parent
		if (delta.isEmptyDelta())
			return parentTree;

		ElementTree tree = new ElementTree(delta);

		//copy the user data forward
		IElementTreeData data = parentTree.getTreeData();
		if (data != null) {
			tree.setTreeData((IElementTreeData) data.clone());
		}

		//make the underlying data tree immutable
		//can't call immutable() on the ElementTree because
		//this would attempt to reroot.
		delta.immutable();
		return tree;
	}

	@Override
	public ElementTree[] readDeltaChain(DataInput input, String newProjectName) throws IOException {
		/* read the number of trees */
		int treeCount = readNumber(input);
		ElementTree[] results = new ElementTree[treeCount];

		if (treeCount <= 0) {
			return results;
		}

		/* read the sort order */
		int[] order = new int[treeCount];
		for (int i = 0; i < treeCount; i++) {
			order[i] = readNumber(input);
		}

		/* read the complete tree */
		results[order[0]] = super.readTree(input, newProjectName);

		/* reconstitute each of the remaining trees from their written deltas */
		for (int i = 1; i < treeCount; i++) {
			results[order[i]] = super.readDelta(results[order[i - 1]], input);
		}

		return results;
	}

	@Override
	public ElementTree readTree(DataInput input, String newProjectName) throws IOException {

		/* The format version number has already been consumed
		 * by ElementTreeReader#readFrom.
		 */
		ElementTree result = new ElementTree(dataTreeReader.readTree(null, input, newProjectName));
		return result;
	}
}
