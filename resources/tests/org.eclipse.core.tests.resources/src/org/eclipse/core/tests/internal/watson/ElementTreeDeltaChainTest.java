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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.internal.watson.ElementTree;
import org.eclipse.core.runtime.IPath;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the ElementTree.mergeDeltaChain() method.
 */
public class ElementTreeDeltaChainTest implements IPathConstants {
	protected ElementTree fTree;
	protected IPath project3;

	@Before
	public void setUp() throws Exception {
		fTree = TestUtil.createTestElementTree();
		project3 = solution.append("project3");
	}

	/**
	 * Tries some bogus merges and makes sure an exception is thrown.
	 */
	@Test
	public void testIllegalMerges() {
		fTree = fTree.newEmptyDelta();

		/* null trees */
		boolean caught = false;
		Exception ex = null;
		try {
			fTree.mergeDeltaChain(solution, null);
		} catch (RuntimeException e) {
			caught = true;
			ex = e;
		} finally {
			assertTrue("", caught);
		}

		/* create a tree with a whole bunch of operations in project3 */
		ElementTree projectTree = new ElementTree();
		projectTree.createElement(solution, "Dummy");
		projectTree.createElement(project3, "project3");
		ElementTree[] trees = TestUtil.doManyRoutineOperations(projectTree, project3);

		/* scramble the order of the project trees */
		TestUtil.scramble(trees);

		/* null handle */
		caught = false;
		try {
			fTree.mergeDeltaChain(null, trees);
		} catch (RuntimeException e) {
			caught = true;
			ex = e;
		} finally {
			assertTrue(ex.getMessage(), caught);
		}

		/* non-existent handle */
		caught = false;
		try {
			fTree.mergeDeltaChain(solution.append("bogosity"), trees);
		} catch (RuntimeException e) {
			caught = true;
			ex = e;
		} finally {
			assertTrue(ex.getMessage(), caught);
		}

		/* immutable receiver */
		caught = false;
		try {
			fTree.immutable();
			fTree.mergeDeltaChain(solution, trees);
		} catch (RuntimeException e) {
			caught = true;
			ex = e;
		} finally {
			assertTrue(ex.getMessage(), caught);
		}
	}

	/**
	 * Tests the mergeDeltaChain method
	 */
	@Test
	public void testMergeDeltaChain() {
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
		TestUtil.doRoutineOperations(fTree, solution);
		fTree = fTree.newEmptyDelta();

		/* merge the delta chains */
		ElementTree newTree = fTree.mergeDeltaChain(project3, trees);
		assertNotEquals("returned tree should be different", newTree, fTree);
		assertFalse("returned tree should be open", newTree.isImmutable());

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
		/* create a tree with a whole bunch of operations in project3 */
		ElementTree projectTree = new ElementTree();
		projectTree.createElement(solution, "Dummy");
		projectTree.createElement(project3, "project3");

		/* form a delta chain on fTree */
		ElementTree[] trees = TestUtil.doManyRoutineOperations(fTree, solution);

		/* scramble the order of the project trees */
		TestUtil.scramble(trees);

		/* merge the delta chains */
		ElementTree newTree = projectTree.mergeDeltaChain(solution, trees);

		assertNotEquals(newTree, projectTree);
		assertTrue(newTree.getElementData(solution).equals("solution"));
		TestUtil.assertTreeStructure(newTree);
	}
}
