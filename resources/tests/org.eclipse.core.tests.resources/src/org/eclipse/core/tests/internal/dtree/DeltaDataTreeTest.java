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
package org.eclipse.core.tests.internal.dtree;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.core.internal.dtree.AbstractDataTree;
import org.eclipse.core.internal.dtree.DeltaDataTree;
import org.eclipse.core.internal.dtree.NodeComparison;
import org.eclipse.core.internal.dtree.ObjectNotFoundException;
import org.eclipse.core.internal.dtree.TestHelper;
import org.eclipse.core.internal.watson.DefaultElementComparator;
import org.eclipse.core.runtime.IPath;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for delta trees.
 */
public class DeltaDataTreeTest {
	IPath rootKey, leftKey, rightKey;

	DeltaDataTree tree, emptyTree, changedTree, deltaTree;

	/**
	 * Assert that the given tree is the same as the final delta tree
	 * created in the string of deltas in testLongDeltaChain and testReroot
	 *
	 */
	public void assertDelta(DeltaDataTree originalTree) {

		/* compare to tree */
		assertTrue("1", originalTree.includes(rootKey));
		assertTrue("2", originalTree.includes(leftKey));
		assertTrue("3", originalTree.includes(rightKey));
		assertTrue("4", originalTree.includes(rootKey.append("newTopLevel")));

		assertTrue("5", originalTree.includes(leftKey.append("new")));
		assertTrue("6", originalTree.includes(leftKey.append("two")));
		assertTrue("7", originalTree.includes(leftKey.append("three")));
		assertTrue("8", originalTree.includes(rightKey.append("rightOfRight")));

		/* this was removed from "tree" */
		assertFalse("9", originalTree.includes(leftKey.append("one")));
	}

	/**
	 * Assert that the given tree is the same as the original "tree" created
	 * during setup
	 */
	public void assertTree(DeltaDataTree originalTree) {

		/* compare to tree */
		assertTrue("1", originalTree.includes(rootKey));
		assertTrue("2", originalTree.includes(leftKey));
		assertTrue("3", originalTree.includes(rightKey));

		assertTrue("4", originalTree.includes(leftKey.append("one")));
		assertTrue("5", originalTree.includes(leftKey.append("two")));
		assertTrue("6", originalTree.includes(leftKey.append("three")));
		assertTrue("7", originalTree.includes(rightKey.append("rightOfRight")));
	}

	/**
	 * Init tests
	 */

	@Before
	public void setUp() {

		emptyTree = new DeltaDataTree();
		tree = new DeltaDataTree();
		rootKey = IPath.ROOT;

		/* Add two children to root */
		try {
			tree.createChild(rootKey, "leftOfRoot");
			tree.createChild(rootKey, "rightOfRoot");
		} catch (ObjectNotFoundException e) {
			throw new Error("(1) Error in setUp");
		}

		leftKey = rootKey.append("leftOfRoot");
		rightKey = rootKey.append("rightOfRoot");

		/* Add three children to left of root and one to right of root */
		try {
			tree.createChild(leftKey, "one");
			tree.createChild(leftKey, "two");
			tree.createChild(leftKey, "three");

			tree.createChild(rightKey, "rightOfRight");
		} catch (ObjectNotFoundException e) {
			throw new Error("(2) Error in setUp");
		}

		changedTree = new DeltaDataTree();
		changedTree.createSubtree(rootKey, tree.copyCompleteSubtree(rootKey));
	}

	/**
	 * Test for problem adding and deleting in same delta layer.
	 */
	@Test
	public void testAddAndRemoveOnSameLayer() {
		IPath elementA = IPath.ROOT.append("A");
		DeltaDataTree tree1 = new DeltaDataTree();

		tree1.createChild(IPath.ROOT, "A", "Data for A");

		tree1.immutable();
		DeltaDataTree tree2;
		tree2 = tree1.newEmptyDeltaTree();

		tree2.createChild(elementA, "B", "New B Data");
		tree2.deleteChild(elementA, "B");

		tree2.immutable();

		//do a bunch of operations to ensure the tree isn't corrupt.
		tree1.compareWith(tree2, DefaultElementComparator.getComparator());
		tree2.compareWith(tree1, DefaultElementComparator.getComparator());
		tree1.forwardDeltaWith(tree2, DefaultElementComparator.getComparator());
		tree2.forwardDeltaWith(tree1, DefaultElementComparator.getComparator());
		tree1.copyCompleteSubtree(IPath.ROOT);
		tree2.copyCompleteSubtree(IPath.ROOT);
		tree1.reroot();
		tree2.reroot();
		tree1.makeComplete();
		tree2.makeComplete();
	}

	@Test
	public void testCompareWithPath() {
		// setup data:
		String X = "x";
		IPath elementX = IPath.ROOT.append(X);
		DeltaDataTree treeA;
		DeltaDataTree treeB;
		DeltaDataTree treeC;
		DeltaDataTree treeD;
		treeA = new DeltaDataTree();
		String oldData = "A Data for x";
		treeA.createChild(IPath.ROOT, X, oldData);
		treeA.immutable();
		treeB = treeA.newEmptyDeltaTree();
		String newData = "B Data for x";
		treeB.createChild(IPath.ROOT, X, newData);
		treeB.immutable();
		treeC = treeB.newEmptyDeltaTree();
		treeC.immutable();
		treeD = treeC.newEmptyDeltaTree();
		treeD.immutable();

		// the method to test:
		DeltaDataTree delta = treeA.compareWith(treeC, DefaultElementComparator.getComparator(), elementX);

		// check:
		assertNull(delta.getParent());
		Object rootData = delta.getRootData();
		assertTrue(delta.isImmutable());
		assertTrue(rootData instanceof NodeComparison);
		NodeComparison nodeComparison=(NodeComparison) rootData;
		assertEquals(NodeComparison.K_CHANGED, nodeComparison.getComparison());
		assertEquals(oldData, nodeComparison.getOldData());
		assertEquals(newData, nodeComparison.getNewData());
	}

	@Test
	public void testCompareWithPath2() {
		// setup data:
		String X = "x";
		IPath elementX = IPath.ROOT.append(X);
		DeltaDataTree treeD;
		DeltaDataTree treeC;
		DeltaDataTree treeB;
		DeltaDataTree treeA;
		treeD = new DeltaDataTree();
		String oldData = "D Data for x";
		treeD.createChild(IPath.ROOT, X, oldData);
		treeD.immutable();
		treeC = treeD.newEmptyDeltaTree();
		treeC.immutable();
		treeB = treeC.newEmptyDeltaTree();
		String newData = "B Data for x";
		treeB.createChild(IPath.ROOT, X, newData);
		treeB.immutable();
		treeA = treeB.newEmptyDeltaTree();
		treeA.immutable();

		// the method to test:
		DeltaDataTree delta = treeA.compareWith(treeC, DefaultElementComparator.getComparator(), elementX);

		// reverse to swap oldData & newData
		delta = delta.asReverseComparisonTree(DefaultElementComparator.getComparator());

		// check:
		assertNull(delta.getParent());
		Object rootData = delta.getRootData();
		assertTrue(delta.isImmutable());
		assertTrue(rootData instanceof NodeComparison);
		NodeComparison nodeComparison = (NodeComparison) rootData;
		assertEquals(NodeComparison.K_CHANGED, nodeComparison.getComparison());
		assertEquals(oldData, nodeComparison.getOldData());
		assertEquals(newData, nodeComparison.getNewData());
	}

	@Test
	public void testCompareWithPathUnchanged() {
		// setup data:
		String X = "x";
		IPath elementX = IPath.ROOT.append(X);
		DeltaDataTree treeA;
		DeltaDataTree treeB;
		treeA = new DeltaDataTree();
		String oldData = "Old Data for x";
		treeA.createChild(IPath.ROOT, X, oldData);
		treeA.immutable();
		treeB = treeA.newEmptyDeltaTree();
		treeB.immutable();

		// the method to test:
		DeltaDataTree deltaAA = treeA.compareWith(treeA, DefaultElementComparator.getComparator(), elementX);
		assertUnchanged(deltaAA);
		DeltaDataTree deltaAB = treeA.compareWith(treeB, DefaultElementComparator.getComparator(), elementX);
		assertUnchanged(deltaAB);
		DeltaDataTree deltaBA = treeB.compareWith(treeA, DefaultElementComparator.getComparator(), elementX);
		assertUnchanged(deltaBA);
	}

	private void assertUnchanged(DeltaDataTree delta) {
		// check:
		assertNull(delta.getParent());
		Object rootData = delta.getRootData();
		assertTrue(delta.isImmutable());
		assertTrue(rootData instanceof NodeComparison);
		NodeComparison nodeComparison = (NodeComparison) rootData;
		assertEquals(nodeComparison.getNewData(), nodeComparison.getOldData());
		// assertEquals(0, nodeComparison.getComparison()); XXX fails for tree!=other
		assertEquals(0, delta.getChildren(AbstractDataTree.rootKey()).length);

	}

	/**
	 * Test for problem when two complete nodes exist, and then
	 * the deleting only masks the first one.
	 */
	@Test
	public void testAddTwiceAndDelete() {
		DeltaDataTree tree1 = new DeltaDataTree();

		tree1.createChild(IPath.ROOT, "A", "Data for A");

		tree1.immutable();
		tree1 = tree1.newEmptyDeltaTree();

		tree1.createChild(IPath.ROOT, "A", "New A Data");

		tree1.immutable();
		tree1 = tree1.newEmptyDeltaTree();

		tree1.deleteChild(IPath.ROOT, "A");
		tree1.immutable();

		assertEquals(0, tree1.getChildCount(IPath.ROOT));

	}

	@Test
	public void testAssembleWithIn() {

		/**
		 * Answer the result of assembling @node1 (a node of the receiver)
		 * with @node2 (a node of @tree2).
		 * The @node2 represents a forward delta based on @node1.
		 */

		DeltaDataTree assembledTree;

		/* make a change */
		changedTree.deleteChild(leftKey, "two");

		/* make delta tree */
		deltaTree = tree.forwardDeltaWith(changedTree, DefaultElementComparator.getComparator());

		/* get changedTree from original and forward delta on original */
		assembledTree = tree.assembleWithForwardDelta(deltaTree);

		/* make sure the reconstructed tree is as expected */
		assertTrue(assembledTree.includes(rootKey));
		assertTrue(assembledTree.includes(leftKey));
		assertTrue(assembledTree.includes(leftKey.append("one")));
		assertFalse(assembledTree.includes(leftKey.append("two")));
		assertTrue(assembledTree.includes(leftKey.append("three")));
		assertTrue(assembledTree.includes(rightKey));

	}

	/**
	 * Create a child of the specified node and give it the specified local name.<p>
	 * If a child with such a name exists, replace it with the new child
	 * @exception ObjectNotFoundException
	 *	parentKey does not exist in the receiver
	 */

	@Test
	public void testCreateChild() {

		boolean caught;
		int size;

		caught = false;
		/* Create child with bogus parent key */
		try {
			tree.createChild(rootKey.append("bogus"), "foobar");
		} catch (ObjectNotFoundException e) {
			caught = true;
		} finally {
			assertTrue("1", caught);
		}

		caught = false;
		/* Create child of empty tree with bogus parent */
		try {
			emptyTree.createChild(rootKey.append("bogus"), "foobar");
		} catch (ObjectNotFoundException e) {
			caught = true;
		} finally {
			assertTrue("2", caught);
		}

		/* Add child to empty tree */
		try {
			emptyTree.createChild(rootKey, "first");
		} catch (ObjectNotFoundException e) {
		}
		assertTrue("3", emptyTree.includes(rootKey.append("first")));

		/* Add root level child to non-empty tree */
		try {
			tree.createChild(rootKey, "NewTopLevel");
		} catch (ObjectNotFoundException e) {
		}
		assertTrue("4", tree.includes(rootKey.append("NewTopLevel")));
		assertTrue("5", tree.includes(leftKey));
		assertTrue("6", tree.includes(rightKey));
		assertTrue("7", tree.includes(leftKey.append("one")));

		/* Add child to leaf in non-empty tree */
		try {
			tree.createChild(leftKey.append("one"), "NewBottom");
		} catch (ObjectNotFoundException e) {
		}
		assertTrue("8", tree.includes(leftKey));
		assertTrue("9", tree.includes(rightKey));
		assertTrue("10", tree.includes(leftKey.append("one")));
		assertTrue("11", tree.includes(leftKey.append("one").append("NewBottom")));

		/* Add child to node containing only one child */
		try {
			tree.createChild(rightKey, "NewRight");
		} catch (ObjectNotFoundException e) {
		}
		assertTrue("12", tree.includes(leftKey));
		assertTrue("13", tree.includes(rightKey));
		assertTrue("14", tree.includes(rightKey.append("rightOfRight")));
		assertTrue("15", tree.includes(rightKey.append("NewRight")));

		/* Add same child twice */
		size = (tree.getNamesOfChildren(leftKey)).length;
		tree.createChild(leftKey, "double");
		tree.createChild(leftKey, "double");
		/* Make sure size has only increased by one */
		assertEquals(size + 1, (tree.getNamesOfChildren(leftKey)).length);
	}

	/**
	 * Delete the child with the specified local name from the specified
	 * node.  Note: this method requires both parentKey and localName,
	 * making it impossible to delete the root node.<p>
	 *
	 * @exception ObjectNotFoundException
	 *	a child of parentKey with name localName does not exist in the receiver
	 */

	@Test
	public void testDeleteChild() {

		boolean caught;

		/* Delete from empty */
		caught = false;
		try {
			emptyTree.deleteChild(rootKey, "non-existant");
		} catch (ObjectNotFoundException e) {
			caught = true;
		} finally {
			assertTrue("1", caught);
		}

		/* delete a child that is not the child of parentKey */
		caught = false;
		try {
			tree.deleteChild(rootKey, "rightOfRight");
		} catch (ObjectNotFoundException e) {
			caught = true;
		} finally {
			assertTrue("2", caught);
		}
		assertTrue("3", tree.includes(rightKey.append("rightOfRight")));

		/* delete with bogus parent */
		caught = false;
		try {
			tree.deleteChild(rootKey.append("bogus"), "rightOfRight");
		} catch (ObjectNotFoundException e) {
			caught = true;
		} finally {
			assertTrue("4", caught);
		}
		assertTrue("5", tree.includes(rightKey.append("rightOfRight")));

		/* delete with bogus local name */
		caught = false;
		try {
			tree.deleteChild(leftKey, "four");
		} catch (ObjectNotFoundException e) {
			caught = true;
		} finally {
			assertTrue("6", caught);
		}
		assertTrue("7", tree.includes(leftKey));

		/* Delete a node with children */
		try {
			tree.deleteChild(rootKey, "leftOfRoot");
		} catch (ObjectNotFoundException e) {
		}

		assertFalse("8", tree.includes(leftKey));
		assertFalse("9", tree.includes(leftKey.append("one")));
		assertTrue("10", tree.includes(rootKey));

		/* delete a leaf */
		try {
			tree.deleteChild(rightKey, "rightOfRight");
		} catch (ObjectNotFoundException e) {
		}
		assertFalse("11", tree.includes(rightKey.append("rightOfRight")));
		assertTrue("12", tree.includes(rightKey));
	}

	/**
	 * Creates a delta on two unrelated delta trees
	 */
	@Test
	public void testDeltaOnCompletelyDifferentTrees() {

		DeltaDataTree newTree = new DeltaDataTree();

		/* Create a new tree */

		/* Add two children to root */
		try {
			newTree.createChild(rootKey, "newLeft");
			newTree.createChild(rootKey, "newRight");
		} catch (ObjectNotFoundException e) {
			fail("(1) Error in setUp");
		}

		/* Add three children to left of root and one to right of root */
		try {
			newTree.createChild(rootKey.append("newLeft"), "newOne");
			newTree.createChild(rootKey.append("newLeft"), "newTwo");
			newTree.createChild(rootKey.append("newLeft"), "newThree");

			newTree.createChild(rootKey.append("newRight"), "newRightOfRight");
			newTree.createChild(rootKey.append("newRight").append("newRightOfRight"), "bottom");
		} catch (ObjectNotFoundException e) {
			fail("(2) Error in setUp");
		}

		/* get delta on different trees */
		deltaTree = newTree.forwardDeltaWith(tree, DefaultElementComparator.getComparator());

		/* assert delta has same content as tree */
		assertTree(deltaTree);
		assertFalse(deltaTree.includes(rootKey.append("newLeft")));
		assertFalse(deltaTree.includes(rootKey.append("newRight")));

	}

	/**
	 * Initialize the receiver so that it is a complete, empty tree.  It does
	 * not represent a delta on another tree.  An empty tree is defined to
	 * have a root node with nil data and no children.
	 */

	@Test
	public void testEmpty() {

		assertTrue("1", emptyTree.includes(rootKey));
		assertNotNull("2", TestHelper.getRootNode(emptyTree));
		assertEquals("3", 0, TestHelper.getRootNode(emptyTree).getChildren().length);
	}

	/**
	 * Tests the forwardDeltaWith function where the delta is calculated
	 * between a data delta node in the old tree and a complete data node
	 * in the new tree.
	 * This is a regression test for a problem with DataDeltaNode.forwardDeltaWith(...).
	 */
	@Test
	public void testForwardDeltaOnDataDeltaNode() {

		tree.immutable();
		DeltaDataTree tree1 = tree.newEmptyDeltaTree();

		tree1.setData(leftKey, "replaced");
		DeltaDataTree delta = tree1.forwardDeltaWith(changedTree, DefaultElementComparator.getComparator());
		assertNull(delta.getData(leftKey)); // the value in changedTree
	}

	/**
	 * Tests the forwardDeltaWith() function
	 */
	@Test
	public void testForwardDeltaWith() {

		DeltaDataTree assembledTree;

		/* make several changes */
		changedTree.deleteChild(leftKey, "two");
		changedTree.createChild(leftKey, "four");
		changedTree.createChild(leftKey, "five");
		changedTree.createChild(leftKey, "six");
		changedTree.createChild(rootKey, "NewTopLevel");

		/* make delta tree */
		deltaTree = tree.forwardDeltaWith(changedTree, DefaultElementComparator.getComparator());

		/* get changedTree from original and forward delta on original */
		assembledTree = tree.assembleWithForwardDelta(deltaTree);

		/* make sure the reconstructed tree is as expected */
		assertTrue(assembledTree.includes(rootKey));
		assertTrue(assembledTree.includes(leftKey));
		assertTrue(assembledTree.includes(rightKey));
		assertTrue(assembledTree.includes(rootKey.append("NewTopLevel")));

		assertTrue(assembledTree.includes(leftKey.append("one")));
		assertFalse(assembledTree.includes(leftKey.append("two")));
		assertTrue(assembledTree.includes(leftKey.append("three")));
		assertTrue(assembledTree.includes(leftKey.append("four")));
		assertTrue(assembledTree.includes(leftKey.append("five")));
		assertTrue(assembledTree.includes(leftKey.append("six")));
		assertTrue(assembledTree.includes(rightKey.append("rightOfRight")));

	}

	/**
	 * Tests the forwardDeltaWith() function using the equality comparer.
	 */
	@Test
	public void testForwardDeltaWithEquality() {

		DeltaDataTree assembledTree;

		/* make several changes */
		changedTree.deleteChild(leftKey, "two");
		changedTree.createChild(leftKey, "four");
		IPath oneKey = leftKey.append("one");
		changedTree.setData(oneKey, "New");

		/* make delta tree */
		deltaTree = tree.forwardDeltaWith(changedTree, DefaultElementComparator.getComparator());

		/* get changedTree from original and forward delta on original */
		assembledTree = tree.assembleWithForwardDelta(deltaTree);

		/* make sure the reconstructed tree is as expected */
		assertTrue(assembledTree.includes(rootKey));
		assertTrue(assembledTree.includes(leftKey));
		assertTrue(assembledTree.includes(rightKey));

		assertTrue(assembledTree.includes(leftKey.append("one")));
		assertFalse(assembledTree.includes(leftKey.append("two")));
		assertTrue(assembledTree.includes(leftKey.append("three")));
		assertTrue(assembledTree.includes(leftKey.append("four")));
		Object data = assembledTree.getData(oneKey);
		assertNotNull(data);
		assertEquals("New", data);

	}

	/**
	 * Answer the key of the child with the given index of the
	 * specified node.<p>
	 *
	 * @param NodeKey, int
	 * @return NodeKey
	 *
	 * @exception ObjectNotFoundException
	 * 	parentKey does not exist in the receiver
	 * @exception ArrayIndexOutOfBoundsException
	 *	if no child with the given index (runtime exception)
	 */

	@Test
	public void testGetChild() {

		boolean caught;

		/* Get valid children */
		caught = false;
		try {
			assertTrue(tree.getChild(rootKey, 0).equals(leftKey));
			assertTrue(tree.getChild(leftKey, 2).equals(leftKey.append("two")));
		} catch (ObjectNotFoundException e) {
			caught = true;
		}
		assertFalse(caught);

		/* Get non-existant child of root */
		caught = false;
		try {
			tree.getChild(rootKey, 99);
		} catch (ArrayIndexOutOfBoundsException e) {
			caught = true;
		} catch (ObjectNotFoundException e) {
			fail();
		}
		assertTrue(caught);

		/* Get non-existant child of interior node */
		caught = false;
		try {
			tree.getChild(leftKey, 99);
		} catch (ArrayIndexOutOfBoundsException e) {
			caught = true;
		} catch (ObjectNotFoundException e) {
			fail();
		}
		assertTrue(caught);

		/* Get non-existant child of leaf node */
		caught = false;
		try {
			tree.getChild(leftKey.append("one"), 99);
		} catch (ArrayIndexOutOfBoundsException e) {
			caught = true;
		} catch (ObjectNotFoundException e) {
			fail();
		}
		assertTrue(caught);

		/* Try to getChild using non-existent key */
		caught = false;
		try {
			tree.getChild(rootKey.append("bogus"), 0);
		} catch (ObjectNotFoundException e) {
			caught = true;
		}
		assertTrue(caught);
	}

	/**
	 * Answer  the number of children of the specified node.
	 *
	 * @exception ObjectNotFoundException
	 *	parentKey does not exist in the receiver
	 */

	@Test
	public void testGetChildCount() {

		boolean caught;

		caught = false;
		try {
			/* empty tree */
			assertEquals("1", 0, emptyTree.getChildCount(rootKey));

			/* root node */
			assertEquals("2", 2, tree.getChildCount(rootKey));

			/* interior nodes */
			assertEquals("3", 3, tree.getChildCount(leftKey));
			assertEquals("4", 1, tree.getChildCount(rightKey));

			/* leaf nodes */
			assertEquals("5", 0, tree.getChildCount(leftKey.append("one")));
			assertEquals("6", 0, tree.getChildCount(leftKey.append("three")));
			assertEquals("7", 0, tree.getChildCount(rightKey.append("rightOfRight")));
		} catch (ObjectNotFoundException e) {
			caught = true;
		} finally {
			assertFalse(caught);
		}

		caught = false;
		/* invalid parent key */
		try {
			tree.getChildCount(rootKey.append("bogus"));
		} catch (ObjectNotFoundException e) {
			caught = true;
		}
		assertTrue(caught);

		/* invalid parent of empty tree */
		caught = false;
		try {
			emptyTree.getChildCount(rootKey.append("bogus"));
		} catch (ObjectNotFoundException e) {
			caught = true;
		}
		assertTrue(caught);
	}

	/**
	 * Answer the keys for the children of the specified node.
	 *
	 * @exception ObjectNotFoundException
	 *	parentKey does not exist in the receiver"
	 */

	@Test
	public void testGetChildren() {

		boolean caught;
		IPath testChildren[], rootChildren[] = {leftKey, rightKey}, leftChildren[] = {leftKey.append("one"), leftKey.append("two"), leftKey.append("three")}, rightChildren[] = {rightKey.append("rightOfRight")};

		caught = false;
		try {
			/* empty tree */
			testChildren = emptyTree.getChildren(rootKey);
			assertEquals("1", 0, testChildren.length);

			/* root node */
			testChildren = tree.getChildren(rootKey);
			assertEquals("2", 2, testChildren.length);
			assertTrue("3", testChildren[0].equals(rootChildren[0]));
			assertTrue("4", testChildren[1].equals(rootChildren[1]));

			/* interior nodes */
			testChildren = tree.getChildren(leftKey);
			assertEquals("5", 3, testChildren.length);
			assertTrue("6", testChildren[0].equals(leftChildren[0]));
			assertTrue("7", testChildren[2].equals(leftChildren[1]));
			assertTrue("8", testChildren[1].equals(leftChildren[2]));

			/* leaf nodes */
			testChildren = tree.getChildren(leftChildren[0]);
			assertEquals("9", 0, testChildren.length);

			testChildren = tree.getChildren(rightChildren[0]);
			assertEquals("10", 0, testChildren.length);
		} catch (ObjectNotFoundException e) {
			caught = true;
		} finally {
			assertFalse("11", caught);
		}

		caught = false;
		/* invalid parent key */
		try {
			tree.getChildren(rootKey.append("bogus"));
		} catch (ObjectNotFoundException e) {
			caught = true;
		}
		assertTrue("12", caught);

		/* invalid parent of empty tree */
		caught = false;
		try {
			emptyTree.getChildren(rootKey.append("bogus"));
		} catch (ObjectNotFoundException e) {
			caught = true;
		}
		assertTrue("13", caught);
	}

	/**
	 * Returns the local names for the children of the specified node.
	 *
	 * @exception ObjectNotFoundException
	 *	parentKey does not exist in the receiver
	 */

	@Test
	public void testGetNamesOfChildren() {

		boolean caught;
		String testChildren[], rootChildren[] = {"leftOfRoot", "rightOfRoot"}, leftChildren[] = {"one", "two", "three"}, rightChildren[] = {"rightOfRight"};

		caught = false;
		try {
			/* empty tree */
			testChildren = emptyTree.getNamesOfChildren(rootKey);
			assertEquals("1", 0, testChildren.length);

			/* root node */
			testChildren = tree.getNamesOfChildren(rootKey);
			assertEquals("2", 2, testChildren.length);
			assertTrue("3", testChildren[0].equals(rootChildren[0]));
			assertTrue("4", testChildren[1].equals(rootChildren[1]));

			/* interior nodes */
			testChildren = tree.getNamesOfChildren(leftKey);
			assertEquals("5", 3, testChildren.length);
			assertTrue("6", testChildren[0].equals(leftChildren[0]));
			assertTrue("7", testChildren[2].equals(leftChildren[1]));
			assertTrue("8", testChildren[1].equals(leftChildren[2]));

			testChildren = tree.getNamesOfChildren(rightKey);
			assertEquals("8.1", 1, testChildren.length);
			assertTrue("8.2", testChildren[0].equals(rightChildren[0]));

			/* leaf nodes */
			testChildren = tree.getNamesOfChildren(leftKey.append("one"));
			assertEquals("9", 0, testChildren.length);

			testChildren = tree.getNamesOfChildren(rightKey.append("rightOfRight"));
			assertEquals("10", 0, testChildren.length);
		} catch (ObjectNotFoundException e) {
			caught = true;
		} finally {
			assertFalse("11", caught);
		}

		caught = false;
		/* invalid parent key */
		try {
			tree.getNamesOfChildren(rootKey.append("bogus"));
		} catch (ObjectNotFoundException e) {
			caught = true;
		}
		assertTrue("12", caught);

		/* invalid parent of empty tree */
		caught = false;
		try {
			emptyTree.getNamesOfChildren(rootKey.append("bogus"));
		} catch (ObjectNotFoundException e) {
			caught = true;
		}
		assertTrue("13", caught);
	}

	/**
	 * Returns true if the receiver includes a node with the given key, false
	 * otherwise.
	 */

	@Test
	public void testIncludes() {

		/* tested in testCreateChild() and testDeleteChild() */

		assertTrue(emptyTree.includes(rootKey));
		assertTrue(tree.includes(rootKey));
		assertTrue(tree.includes(leftKey));
		assertTrue(tree.includes(rightKey));
		assertTrue(tree.includes(leftKey.append("one")));
		assertTrue(tree.includes(rightKey.append("rightOfRight")));

		assertFalse(emptyTree.includes(rootKey.append("bogus")));
		assertFalse(tree.includes(rootKey.append("bogus")));
		assertFalse(tree.includes(leftKey.append("bogus")));
		assertFalse(tree.includes(leftKey.append("one").append("bogus")));
		assertFalse(tree.includes(rightKey.append("bogus")));
	}

	/**
	 * Tests operations on a chain of deltas
	 */
	@Test
	public void testLongDeltaChain() {

		final int NUM_DELTAS = 10;

		DeltaDataTree deltas[] = new DeltaDataTree[NUM_DELTAS];

		/* create a delta on the original tree, and make a change */
		tree.immutable();
		deltas[0] = tree.newEmptyDeltaTree();
		deltas[0].createChild(leftKey, "new");
		assertTree(deltas[0]);
		assertTrue(deltas[0].includes(leftKey.append("new")));

		/* create a second delta and make a change to that */
		deltas[0].immutable();
		deltas[1] = deltas[0].newEmptyDeltaTree();
		deltas[1].deleteChild(leftKey, "one");
		assertEquals("parent 0 -> 1", deltas[1].getParent(), deltas[0]);
		assertFalse(deltas[1].includes(leftKey.append("one")));

		/* create a third delta and make a change to that */
		deltas[1].immutable();
		deltas[2] = deltas[1].newEmptyDeltaTree();
		deltas[2].createChild(rootKey, "newTopLevel");
		assertEquals("parent 1 -> 2", deltas[2].getParent(), deltas[1]);
		assertEquals("parent 0 -> 2", deltas[2].getParent().getParent(), deltas[0]);
		assertFalse(deltas[2].includes(leftKey.append("one")));
		assertTrue(deltas[2].includes(rootKey.append("newTopLevel")));

	}

	/**
	 * Tests the newEmptyDeltaTree method
	 */
	@Test
	public void testNewEmptyDeltaTree() {
		tree.immutable();
		DeltaDataTree delta = tree.newEmptyDeltaTree();
		assertEquals("parent", tree, delta.getParent());
		assertTree(delta);
	}

	/**
	 * Test for problem deleting and re-adding in same delta layer.
	 */
	@Test
	public void testRegression1FVVP6L() {
		IPath elementA = IPath.ROOT.append("A");

		DeltaDataTree tree1 = new DeltaDataTree();

		tree1.createChild(IPath.ROOT, "A", "Data for A");
		tree1.createChild(elementA, "B", "Data for B");

		tree1.immutable();
		tree1 = tree1.newEmptyDeltaTree();

		tree1.deleteChild(elementA, "B");
		tree1.createChild(elementA, "B", "New B Data");

		tree1.immutable();
		tree1 = tree1.newEmptyDeltaTree();

		tree1.deleteChild(elementA, "B");

		try {
			tree1.copyCompleteSubtree(IPath.ROOT);
		} catch (RuntimeException e) {
			fail("Unexpected error copying tree");
		}

	}

	/**
	 * Test for problem deleting and re-adding in same delta layer.
	 */
	@Test
	public void testRegression1FVVP6LWithChildren() {
		IPath elementA = IPath.ROOT.append("A");
		IPath elementB = elementA.append("B");
		IPath elementC = elementB.append("C");

		DeltaDataTree tree1 = new DeltaDataTree();

		tree1.createChild(IPath.ROOT, "A", "Data for A");
		tree1.createChild(elementA, "B", "Data for B");
		tree1.createChild(elementB, "C", "Data for C");

		tree1.immutable();
		tree1 = tree1.newEmptyDeltaTree();

		tree1.deleteChild(elementA, "B");
		tree1.createChild(elementA, "B", "New B Data");

		tree1.immutable();
		tree1 = tree1.newEmptyDeltaTree();

		assertFalse("Child exists after deletion", tree1.includes(elementC));

		try {
			tree1.copyCompleteSubtree(IPath.ROOT);
		} catch (RuntimeException e) {
			fail("Unexpected error copying tree");
		}

	}

	/**
	 * Tests the reroot function
	 */
	@Test
	public void testReroot() {

		final int NUM_DELTAS = 10;

		DeltaDataTree deltas[] = new DeltaDataTree[NUM_DELTAS];

		/* create a delta on the original tree, and make a change */
		tree.immutable();
		deltas[0] = tree.newEmptyDeltaTree();
		deltas[0].createChild(leftKey, "new");
		assertTree(deltas[0]);
		assertTrue(deltas[0].includes(leftKey.append("new")));

		/* create a second delta and make a change to that */
		deltas[0].immutable();
		deltas[1] = deltas[0].newEmptyDeltaTree();
		deltas[1].deleteChild(leftKey, "one");
		assertEquals("parent 0 -> 1", deltas[1].getParent(), deltas[0]);
		assertFalse(deltas[1].includes(leftKey.append("one")));

		/* create a third delta and make a change to that */
		deltas[1].immutable();
		deltas[2] = deltas[1].newEmptyDeltaTree();
		deltas[2].createChild(rootKey, "newTopLevel");
		assertEquals("parent 1 -> 2", deltas[2].getParent(), deltas[1]);
		assertEquals("parent 0 -> 2", deltas[2].getParent().getParent(), deltas[0]);
		assertFalse(deltas[2].includes(leftKey.append("one")));
		assertTrue(deltas[2].includes(rootKey.append("newTopLevel")));

		/* create a fourth delta and reroot at it */
		deltas[2].immutable();
		deltas[3] = deltas[2].newEmptyDeltaTree();
		deltas[3].immutable();
		deltas[3].reroot();
		assertNull(deltas[3].getParent());
		assertEquals(deltas[2].getParent(), deltas[3]);
		assertEquals(deltas[1].getParent(), deltas[2]);
		assertEquals(deltas[0].getParent(), deltas[1]);

		/* test that all trees have the same representation as before rerooting */
		assertTree(tree);
		assertFalse(tree.includes(leftKey.append("new")));
		assertTrue(tree.includes(leftKey.append("one")));
		assertTree(deltas[0]);
		assertTrue(deltas[0].includes(leftKey.append("new")));
		assertTrue(deltas[0].includes(leftKey.append("one")));
		assertTrue(deltas[1].includes(leftKey.append("new")));
		assertFalse(deltas[1].includes(leftKey.append("one")));
		assertDelta(deltas[2]);
		assertDelta(deltas[3]);

	}

	/**
	 * Tests that the setUp() method is doing what it should
	 */
	@Test
	public void testSetup() {
		assertTree(tree);
		assertTree(changedTree);
	}
}
