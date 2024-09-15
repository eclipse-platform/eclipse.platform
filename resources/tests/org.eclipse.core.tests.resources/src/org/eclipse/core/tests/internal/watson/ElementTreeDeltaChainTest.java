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
package org.eclipse.core.tests.internal.watson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.eclipse.core.internal.watson.ElementTree;
import org.eclipse.core.runtime.IPath;
import org.junit.jupiter.api.Test;

/**
 * Tests the ElementTree.mergeDeltaChain() method.
 */
public class ElementTreeDeltaChainTest implements IPathConstants {
	private static final IPath project3 = solution.append("project3");

	/**
	 * Tries some bogus merges and makes sure an exception is thrown.
	 */
	@Test
	public void testIllegalMerges() {
		ElementTree tree = TestUtil.createTestElementTree().newEmptyDelta();

		/* null trees */
		assertThrows(RuntimeException.class, () -> tree.mergeDeltaChain(solution, null));

		/* create a tree with a whole bunch of operations in project3 */
		ElementTree projectTree = new ElementTree();
		projectTree.createElement(solution, "Dummy");
		projectTree.createElement(project3, "project3");
		ElementTree[] trees = TestUtil.doManyRoutineOperations(projectTree, project3);

		/* scramble the order of the project trees */
		TestUtil.scramble(trees);

		/* null handle */
		assertThrows(RuntimeException.class, () -> tree.mergeDeltaChain(null, trees));

		/* non-existent handle */
		assertThrows(RuntimeException.class, () -> tree.mergeDeltaChain(solution.append("bogosity"), trees));

		/* immutable receiver */
		assertThrows(RuntimeException.class, () -> {
			tree.immutable();
			tree.mergeDeltaChain(solution, trees);
		});
	}

	/**
	 * Tests the mergeDeltaChain method
	 */
	@Test
	public void testMergeDeltaChain() {
		ElementTree tree = TestUtil.createTestElementTree();
		/* create a tree with a whole bunch of operations in project3 */
		ElementTree projectTree = new ElementTree();
		projectTree.createElement(solution, "Dummy");
		projectTree.createElement(project3, "project3");
		ElementTree[] trees = TestUtil.doManyRoutineOperations(projectTree, project3);

		/* create a copy for testing purposes */
		ElementTree copyTree = new ElementTree();
		copyTree.createElement(solution, "Dummy");
		copyTree.createElement(project3, "project3");
		ElementTree[] copies = TestUtil.doManyRoutineOperations(copyTree, project3);

		/* scramble the order of the project trees */
		TestUtil.scramble(trees, copies);

		/* do a bunch of operations on fTree to build a delta chain */
		TestUtil.doRoutineOperations(tree, solution);
		tree = tree.newEmptyDelta();

		/* merge the delta chains */
		ElementTree newTree = tree.mergeDeltaChain(project3, trees);
		assertNotEquals(newTree, tree);
		assertFalse(newTree.isImmutable());

		/* make sure old and new trees have same structure */
		for (int i = 0; i < trees.length; i++) {
			TestUtil.assertEqualTrees("testMergeDeltaChain: " + i, copies[i].getSubtree(project3), trees[i].getSubtree(project3));
		}

		TestUtil.assertHasPaths(newTree, TestUtil.getTreePaths());
		TestUtil.assertHasPaths(newTree, new IPath[] {project3});
	}

	/**
	 * Performs merge on trees that have nodes in common.  The chain
	 * being merged should overwrite the receiver.
	 */
	@Test
	public void testMergeOverwrite() {
		ElementTree tree = TestUtil.createTestElementTree();
		/* create a tree with a whole bunch of operations in project3 */
		ElementTree projectTree = new ElementTree();
		projectTree.createElement(solution, "Dummy");
		projectTree.createElement(project3, "project3");

		/* form a delta chain on fTree */
		ElementTree[] trees = TestUtil.doManyRoutineOperations(tree, solution);

		/* scramble the order of the project trees */
		TestUtil.scramble(trees);

		/* merge the delta chains */
		ElementTree newTree = projectTree.mergeDeltaChain(solution, trees);

		assertNotEquals(newTree, projectTree);
		assertEquals("solution", newTree.getElementData(solution));
		TestUtil.assertTreeStructure(newTree);
	}
}
