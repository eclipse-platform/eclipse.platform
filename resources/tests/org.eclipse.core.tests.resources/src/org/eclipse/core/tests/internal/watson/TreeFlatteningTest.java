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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import org.eclipse.core.internal.resources.SaveManager;
import org.eclipse.core.internal.watson.ElementTree;
import org.eclipse.core.internal.watson.ElementTreeReader;
import org.eclipse.core.internal.watson.ElementTreeWriter;
import org.eclipse.core.runtime.IPath;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for <code>ElementTreeWriter</code> and
 * <code>ElementTreeReader</code>.
 */
public class TreeFlatteningTest extends ElementTreeSerializationTest {

	/**
	 * Performs the serialization activity for this test
	 */
	@Override
	public Object doRead(ElementTreeReader reader, DataInputStream input) throws IOException {
		return reader.readTree(input);
	}

	/**
	 * Runs a test for this class at a certain depth and path
	 */
	@Override
	public void doTest(IPath path, int depth) {
		/* Get an element tree from somewhere. */
		fTree = TestUtil.createTestElementTree();
		fSubtreePath = path;
		fDepth = depth;

		ElementTree newTree = (ElementTree) doPipeTest();

		TestUtil.assertEqualTrees(this.getClass() + "test0", fTree, newTree, fSubtreePath, fDepth);
	}

	/**
	 * Performs the serialization activity for this test
	 */
	@Override
	public void doWrite(ElementTreeWriter writer, DataOutputStream output) throws IOException {
		writer.writeTree(fTree, fSubtreePath, fDepth, output);
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		fTree = TestUtil.createTestElementTree();
	}

	@Test
	public void test0() {
		/* Get an element tree from somewhere. */
		fTree = TestUtil.createTestElementTree();
		ElementTree newTree = (ElementTree) doFileTest();

		TestUtil.assertEqualTrees(this.getClass() + "test0", fTree, newTree);
	}

	@Test
	public void testSortTreesError() {
		ElementTree tree1 = new ElementTree();
		ElementTree tree11 = tree1.newEmptyDelta();
		ElementTree tree111 = tree11.newEmptyDelta();
		ElementTree tree1111 = tree111.newEmptyDelta(); // <-still mutable
		assertFalse(tree1111.isImmutable());
		assertTrue(tree111.isImmutable());
		assertTrue(tree11.isImmutable());
		assertTrue(tree1.isImmutable());
		ElementTree[] trees = new ElementTree[] { tree1, tree11, tree111, tree1111 };
		ElementTree[] sorted = SaveManager.sortTrees(trees);
		assertNull(sorted); // => not sortable
		// logs java.lang.NullPointerException: Given trees not in unambiguous order
		// (Bug 352867): 16->17->18, 17->18, 18, mutable! 19->18
	}

	@Test
	public void testSortTrees() {
		ElementTree tree1 = new ElementTree();
		ElementTree tree11 = tree1.newEmptyDelta();
		ElementTree tree111 = tree11.newEmptyDelta();
		ElementTree tree1111 = tree111.newEmptyDelta();

		assertFalse(tree1111.isImmutable());
		tree1111.newEmptyDelta(); // without this final newEmptyDelta() two trees have same parent (strange)
		assertTrue(tree1111.isImmutable());

		assertSame(tree1.getParent(), tree11);
		assertSame(tree11.getParent(), tree111);
		assertSame(tree111.getParent(), tree1111);
		assertSame(tree1111.getParent(), null);

		{ // list of all trees
			ElementTree[] trees12 = new ElementTree[] { tree1, tree11, tree111, tree1111 };
			ElementTree[] trees21 = reversed(trees12);
			int oldest12 = ElementTree.findOldest(trees12);
			int oldest21 = ElementTree.findOldest(trees21);

			assertSame(trees12[oldest12], trees21[oldest21]);
			assertSame(trees12[oldest12], tree1); // "oldest" is the first created

			System.out.println("oldest=" + trees12[oldest12].toDebugString());
			ElementTree[] sorted12 = SaveManager.sortTrees(trees12);
			ElementTree[] sorted21 = SaveManager.sortTrees(trees21);
			assertTrue(Arrays.equals(sorted12, sorted21));
			assertSame(tree1111, sorted12[0]); // sorted by creation time desc
		}
		{ // trees with duplicates
			ElementTree[] trees12 = new ElementTree[] { tree1, tree1, tree11, tree11, tree111, tree111, tree1111,
					tree1111 };
			ElementTree[] trees21 = reversed(trees12);
			int oldest12 = ElementTree.findOldest(trees12);
			int oldest21 = ElementTree.findOldest(trees21);

			assertSame(trees12[oldest12], trees21[oldest21]);
			assertSame(trees12[oldest12], tree1); // "oldest" is the first created

			System.out.println("oldest=" + trees12[oldest12].toDebugString());
			ElementTree[] sorted12 = SaveManager.sortTrees(trees12);
			ElementTree[] sorted21 = SaveManager.sortTrees(trees21);
			assertTrue(Arrays.equals(sorted12, sorted21));
			assertSame(tree1111, sorted12[0]); // sorted by creation time desc
		}
		{ // sparse (without all intermediate trees)
			ElementTree[] trees12 = new ElementTree[] { tree1, tree1, tree1111, tree1111 };
			ElementTree[] trees21 = reversed(trees12);
			int oldest12 = ElementTree.findOldest(trees12);
			int oldest21 = ElementTree.findOldest(trees21);

			assertSame(trees12[oldest12], trees21[oldest21]);
			assertSame(trees12[oldest12], tree1); // "oldest" is the first created

			System.out.println("oldest=" + trees12[oldest12].toDebugString());
			ElementTree[] sorted12 = SaveManager.sortTrees(trees12);
			ElementTree[] sorted21 = SaveManager.sortTrees(trees21);
			assertTrue(Arrays.equals(sorted12, sorted21));
			assertSame(tree1111, sorted12[0]); // sorted by creation time desc
		}
		{ // without newest
			ElementTree[] trees12 = new ElementTree[] { tree1, tree111 };
			ElementTree[] trees21 = reversed(trees12);
			int oldest12 = ElementTree.findOldest(trees12);
			int oldest21 = ElementTree.findOldest(trees21);

			assertSame(trees12[oldest12], trees21[oldest21]);
			assertSame(trees12[oldest12], tree1); // "oldest" is the first created

			System.out.println("oldest=" + trees12[oldest12].toDebugString());
			ElementTree[] sorted12 = SaveManager.sortTrees(trees12);
			ElementTree[] sorted21 = SaveManager.sortTrees(trees21);
			assertTrue(Arrays.equals(sorted12, sorted21));
			assertSame(tree111, sorted12[0]); // sorted by creation time desc
		}
		{ // without oldest
			ElementTree[] trees12 = new ElementTree[] { tree11, tree1111 };
			ElementTree[] trees21 = reversed(trees12);
			int oldest12 = ElementTree.findOldest(trees12);
			int oldest21 = ElementTree.findOldest(trees21);

			assertSame(trees12[oldest12], trees21[oldest21]);
			assertSame(trees12[oldest12], tree11); // "oldest" is the first created

			System.out.println("oldest=" + trees12[oldest12].toDebugString());
			ElementTree[] sorted12 = SaveManager.sortTrees(trees12);
			ElementTree[] sorted21 = SaveManager.sortTrees(trees21);
			assertTrue(Arrays.equals(sorted12, sorted21));
			assertSame(tree1111, sorted12[0]); // sorted by creation time desc
		}
		{ // trees with odd duplicates
			ElementTree[] trees12 = new ElementTree[] { tree1, tree11, tree11, tree11, tree111, tree1111, tree1111,
					tree1111 };
			ElementTree[] trees21 = reversed(trees12);
			int oldest12 = ElementTree.findOldest(trees12);
			int oldest21 = ElementTree.findOldest(trees21);

			assertSame(trees12[oldest12], trees21[oldest21]);
			assertSame(trees12[oldest12], tree1); // "oldest" is the first created

			System.out.println("oldest=" + trees12[oldest12].toDebugString());
			ElementTree[] sorted12 = SaveManager.sortTrees(trees12);
			ElementTree[] sorted21 = SaveManager.sortTrees(trees21);
			assertTrue(Arrays.equals(sorted12, sorted21));
			assertSame(tree1111, sorted12[0]); // sorted by creation time desc
		}
	}

	private ElementTree[] reversed(ElementTree[] trees) {
		ElementTree[] result = new ElementTree[trees.length];
		for (int i = 0; i < trees.length; i++) {
			result[i] = trees[trees.length - i - 1];
		}
		return result;
	}

	/**
	 * Tests the reading and writing of element deltas
	 */
	@Test
	public void testExhaustive() {
		doExhaustiveTests();
	}

	@Test
	public void testNullData() {
		/* Get an element tree from somewhere. */
		fTree = TestUtil.createTestElementTree();
		fTree = fTree.newEmptyDelta();

		/* set some elements to have null data */
		fTree.setElementData(solution, null);
		fTree.setElementData(folder2, null);
		fTree.immutable();

		ElementTree newTree = (ElementTree) doPipeTest();

		TestUtil.assertEqualTrees(this.getClass() + "test0", fTree, newTree);
	}

	@Test
	public void testWriteRoot() {
		/* Get an element tree from somewhere. */
		fTree = TestUtil.createTestElementTree();
		fSubtreePath = IPath.ROOT;

		ElementTree newTree = (ElementTree) doPipeTest();

		TestUtil.assertEqualTrees(this.getClass() + "test0", fTree, newTree, fSubtreePath);
	}
}
