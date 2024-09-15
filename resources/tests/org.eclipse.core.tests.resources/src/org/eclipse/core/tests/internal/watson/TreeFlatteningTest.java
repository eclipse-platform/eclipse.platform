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

import static org.eclipse.core.tests.internal.watson.ElementTreeSerializationTestHelper.doFileTest;
import static org.eclipse.core.tests.internal.watson.ElementTreeSerializationTestHelper.doPipeTest;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import org.eclipse.core.internal.resources.SaveManager;
import org.eclipse.core.internal.watson.ElementTree;
import org.eclipse.core.internal.watson.ElementTreeReader;
import org.eclipse.core.internal.watson.ElementTreeWriter;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.internal.watson.ElementTreeSerializationTestHelper.StreamReader;
import org.eclipse.core.tests.internal.watson.ElementTreeSerializationTestHelper.StreamWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Unit tests for <code>ElementTreeWriter</code> and
 * <code>ElementTreeReader</code>.
 */
public class TreeFlatteningTest implements IPathConstants {

	private StreamReader getReader() throws IOException {
		return (ElementTreeReader reader, DataInputStream input) -> reader.readTree(input);
	}

	private StreamWriter getWriter(ElementTree tree, IPath path, int depth) throws IOException {
		return (ElementTreeWriter writer, DataOutputStream output) -> writer.writeTree(tree, path, depth, output);
	}

	@Test
	public void test0(@TempDir Path tempDir) throws IOException {
		ElementTree tree = TestUtil.createTestElementTree();
		IPath testTreeRootPath = IPathConstants.solution;
		ElementTree newTree = (ElementTree) doFileTest(tempDir, getReader(),
				getWriter(tree, testTreeRootPath, ElementTreeWriter.D_INFINITE));

		TestUtil.assertEqualTrees(this.getClass() + "test0", tree, newTree);
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
			assertArrayEquals(sorted12, sorted21);
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
			assertArrayEquals(sorted12, sorted21);
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
			assertArrayEquals(sorted12, sorted21);
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
			assertArrayEquals(sorted12, sorted21);
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
			assertArrayEquals(sorted12, sorted21);
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
			assertArrayEquals(sorted12, sorted21);
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
	@ParameterizedTest
	@ArgumentsSource(ElementTreeSerializationTestHelper.class)
	public void testExhaustive(IPath path, int depth) throws IOException {
		ElementTree tree = TestUtil.createTestElementTree();
		ElementTree newTree = (ElementTree) doPipeTest(getWriter(tree, path, depth),
				getReader());

		TestUtil.assertEqualTrees(this.getClass() + "test0", tree, newTree, path, depth);
	}

	@Test
	public void testNullData() throws IOException {
		ElementTree tree = TestUtil.createTestElementTree();
		tree = tree.newEmptyDelta();
		IPath testTreeRootPath = IPathConstants.solution;

		/* set some elements to have null data */
		tree.setElementData(IPathConstants.solution, null);
		tree.setElementData(IPathConstants.folder2, null);
		tree.immutable();

		ElementTree newTree = (ElementTree) doPipeTest(getWriter(tree, testTreeRootPath, ElementTreeWriter.D_INFINITE),
				getReader());

		TestUtil.assertEqualTrees(this.getClass() + "test0", tree, newTree);
	}

	@Test
	public void testWriteRoot() throws IOException {
		ElementTree tree = TestUtil.createTestElementTree();
		IPath path = IPath.ROOT;
		ElementTree newTree = (ElementTree) doPipeTest(getWriter(tree, path, ElementTreeWriter.D_INFINITE),
				getReader());

		TestUtil.assertEqualTrees(this.getClass() + "test0", tree, newTree, path);
	}
}
