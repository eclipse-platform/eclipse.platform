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

import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.core.internal.dtree.AbstractDataTree;
import org.eclipse.core.internal.dtree.DeltaDataTree;
import org.eclipse.core.internal.dtree.NodeComparison;
import org.eclipse.core.internal.dtree.ObjectNotFoundException;
import org.eclipse.core.internal.dtree.TestHelper;
import org.eclipse.core.internal.watson.DefaultElementComparator;
import org.eclipse.core.runtime.IPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for delta trees.
 */
public class DeltaDataTreeTest {
	IPath rootKey, leftKey, rightKey;

	DeltaDataTree tree, emptyTree, changedTree, deltaTree;

	/**
	 * Assert that the given tree is the same as the final delta tree
	 * created in the string of deltas in testLongDeltaChain and testReroot
	 */
	public void assertDelta(DeltaDataTree originalTree) {
		/* compare to tree */
		assertThat(rootKey).matches(originalTree::includes);
		assertThat(leftKey).matches(originalTree::includes);
		assertThat(rightKey).matches(originalTree::includes);
		assertThat(rootKey.append("newTopLevel")).matches(originalTree::includes);

		assertThat(leftKey.append("new")).matches(originalTree::includes);
		assertThat(leftKey.append("two")).matches(originalTree::includes);
		assertThat(leftKey.append("three")).matches(originalTree::includes);
		assertThat(rightKey.append("rightOfRight")).matches(originalTree::includes);

		/* this was removed from "tree" */
		assertThat(leftKey.append("one")).matches(not(originalTree::includes));
	}

	/**
	 * Assert that the given tree is the same as the original "tree" created
	 * during setup
	 */
	public void assertTree(DeltaDataTree originalTree) {
		/* compare to tree */
		assertThat(rootKey).matches(originalTree::includes);
		assertThat(leftKey).matches(originalTree::includes);
		assertThat(rightKey).matches(originalTree::includes);

		assertThat(leftKey.append("one")).matches(originalTree::includes);
		assertThat(leftKey.append("two")).matches(originalTree::includes);
		assertThat(leftKey.append("three")).matches(originalTree::includes);
	}

	/**
	 * Init tests
	 */

	@BeforeEach
	public void setUp() {
		emptyTree = new DeltaDataTree();
		tree = new DeltaDataTree();
		rootKey = IPath.ROOT;

		/* Add two children to root */
		tree.createChild(rootKey, "leftOfRoot");
		tree.createChild(rootKey, "rightOfRoot");

		leftKey = rootKey.append("leftOfRoot");
		rightKey = rootKey.append("rightOfRoot");

		/* Add three children to left of root and one to right of root */
		tree.createChild(leftKey, "one");
		tree.createChild(leftKey, "two");
		tree.createChild(leftKey, "three");

		tree.createChild(rightKey, "rightOfRight");

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
		DeltaDataTree tree2 = tree1.newEmptyDeltaTree();

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
		DeltaDataTree treeA = new DeltaDataTree();
		String oldData = "A Data for x";
		treeA.createChild(IPath.ROOT, X, oldData);
		treeA.immutable();
		DeltaDataTree treeB = treeA.newEmptyDeltaTree();
		String newData = "B Data for x";
		treeB.createChild(IPath.ROOT, X, newData);
		treeB.immutable();
		DeltaDataTree treeC = treeB.newEmptyDeltaTree();
		treeC.immutable();
		DeltaDataTree treeD = treeC.newEmptyDeltaTree();
		treeD.immutable();

		// the method to test:
		DeltaDataTree delta = treeA.compareWith(treeC, DefaultElementComparator.getComparator(), elementX);

		// check:
		assertNull(delta.getParent());
		Object rootData = delta.getRootData();
		assertTrue(delta.isImmutable());
		assertThat(rootData).isInstanceOf(NodeComparison.class);
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
		DeltaDataTree treeD = new DeltaDataTree();
		String oldData = "D Data for x";
		treeD.createChild(IPath.ROOT, X, oldData);
		treeD.immutable();
		DeltaDataTree treeC = treeD.newEmptyDeltaTree();
		treeC.immutable();
		DeltaDataTree treeB = treeC.newEmptyDeltaTree();
		String newData = "B Data for x";
		treeB.createChild(IPath.ROOT, X, newData);
		treeB.immutable();
		DeltaDataTree treeA = treeB.newEmptyDeltaTree();
		treeA.immutable();

		// the method to test:
		DeltaDataTree delta = treeA.compareWith(treeC, DefaultElementComparator.getComparator(), elementX);

		// reverse to swap oldData & newData
		delta = delta.asReverseComparisonTree(DefaultElementComparator.getComparator());

		// check:
		assertNull(delta.getParent());
		Object rootData = delta.getRootData();
		assertTrue(delta.isImmutable());
		assertThat(rootData).isInstanceOf(NodeComparison.class);
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
		DeltaDataTree treeA = new DeltaDataTree();
		String oldData = "Old Data for x";
		treeA.createChild(IPath.ROOT, X, oldData);
		treeA.immutable();
		DeltaDataTree treeB = treeA.newEmptyDeltaTree();
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
		assertThat(rootData).isInstanceOf(NodeComparison.class);
		NodeComparison nodeComparison = (NodeComparison) rootData;
		assertEquals(nodeComparison.getNewData(), nodeComparison.getOldData());
		// assertEquals(0, nodeComparison.getComparison()); XXX fails for tree!=other
		assertThat(delta.getChildren(AbstractDataTree.rootKey())).isEmpty();

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

		assertThat(tree1.getChildCount(IPath.ROOT)).isZero();

	}

	@Test
	public void testAssembleWithIn() {

		/**
		 * Answer the result of assembling @node1 (a node of the receiver)
		 * with @node2 (a node of @tree2).
		 * The @node2 represents a forward delta based on @node1.
		 */

		/* make a change */
		changedTree.deleteChild(leftKey, "two");

		/* make delta tree */
		deltaTree = tree.forwardDeltaWith(changedTree, DefaultElementComparator.getComparator());

		/* get changedTree from original and forward delta on original */
		DeltaDataTree assembledTree = tree.assembleWithForwardDelta(deltaTree);

		/* make sure the reconstructed tree is as expected */
		assertThat(rootKey).matches(assembledTree::includes);
		assertThat(leftKey).matches(assembledTree::includes);
		assertThat(leftKey.append("one")).matches(assembledTree::includes);
		assertThat(leftKey.append("two")).matches(not(assembledTree::includes));
		assertThat(leftKey.append("three")).matches(assembledTree::includes);
		assertThat(rightKey).matches(assembledTree::includes);
	}

	/**
	 * Create a child of the specified node and give it the specified local name.<p>
	 * If a child with such a name exists, replace it with the new child
	 * @exception ObjectNotFoundException
	 *	parentKey does not exist in the receiver
	 */

	@Test
	public void testCreateChild() {
		int size;

		/* Create child with bogus parent key */
		assertThrows(ObjectNotFoundException.class, () -> tree.createChild(rootKey.append("bogus"), "foobar"));

		/* Create child of empty tree with bogus parent */
		assertThrows(ObjectNotFoundException.class, () -> emptyTree.createChild(rootKey.append("bogus"), "foobar"));

		/* Add child to empty tree */
		emptyTree.createChild(rootKey, "first");
		assertThat(rootKey.append("first")).matches(emptyTree::includes);

		/* Add root level child to non-empty tree */
		tree.createChild(rootKey, "NewTopLevel");
		assertThat(rootKey.append("NewTopLevel")).matches(tree::includes);
		assertThat(leftKey).matches(tree::includes);
		assertThat(rightKey).matches(tree::includes);
		assertThat(leftKey.append("one")).matches(tree::includes);

		/* Add child to leaf in non-empty tree */
		tree.createChild(leftKey.append("one"), "NewBottom");
		assertThat(leftKey).matches(tree::includes);
		assertThat(rightKey).matches(tree::includes);
		assertThat(leftKey.append("one")).matches(tree::includes);
		assertThat(leftKey.append("one").append("NewBottom")).matches(tree::includes);

		/* Add child to node containing only one child */
		tree.createChild(rightKey, "NewRight");
		assertThat(leftKey).matches(tree::includes);
		assertThat(rightKey).matches(tree::includes);
		assertThat(rightKey.append("rightOfRight")).matches(tree::includes);
		assertThat(rightKey.append("NewRight")).matches(tree::includes);

		/* Add same child twice */
		size = (tree.getNamesOfChildren(leftKey)).length;
		tree.createChild(leftKey, "double");
		tree.createChild(leftKey, "double");
		/* Make sure size has only increased by one */
		assertThat(tree.getNamesOfChildren(leftKey)).hasSize(size + 1);
	}

	/**
	 * Delete the child with the specified local name from the specified
	 * node.  Note: this method requires both parentKey and localName,
	 * making it impossible to delete the root node.
	 *
	 * @exception ObjectNotFoundException
	 *	a child of parentKey with name localName does not exist in the receiver
	 */

	@Test
	public void testDeleteChild() {
		/* Delete from empty */
		assertThrows(ObjectNotFoundException.class, () -> emptyTree.deleteChild(rootKey, "non-existant"));

		/* delete a child that is not the child of parentKey */
		assertThrows(ObjectNotFoundException.class, () -> tree.deleteChild(rootKey, "rightOfRight"));
		assertThat(rightKey.append("rightOfRight")).matches(tree::includes);

		/* delete with bogus parent */
		assertThrows(ObjectNotFoundException.class, () -> tree.deleteChild(rootKey.append("bogus"), "rightOfRight"));
		assertThat(rightKey.append("rightOfRight")).matches(tree::includes);

		/* delete with bogus local name */
		assertThrows(ObjectNotFoundException.class, () -> tree.deleteChild(leftKey, "four"));
		assertThat(leftKey).matches(tree::includes);

		/* Delete a node with children */
		tree.deleteChild(rootKey, "leftOfRoot");
		assertThat(leftKey).matches(not(tree::includes));
		assertThat(leftKey.append("one")).matches(not(tree::includes));
		assertThat(rootKey).matches(tree::includes);

		/* delete a leaf */
		tree.deleteChild(rightKey, "rightOfRight");
		assertThat(rightKey.append("rightOfRight")).matches(not(tree::includes));
		assertThat(rightKey).matches(tree::includes);
	}

	/**
	 * Creates a delta on two unrelated delta trees
	 */
	@Test
	public void testDeltaOnCompletelyDifferentTrees() {
		DeltaDataTree newTree = new DeltaDataTree();

		/* Create a new tree */

		/* Add two children to root */
		newTree.createChild(rootKey, "newLeft");
		newTree.createChild(rootKey, "newRight");

		/* Add three children to left of root and one to right of root */
		newTree.createChild(rootKey.append("newLeft"), "newOne");
		newTree.createChild(rootKey.append("newLeft"), "newTwo");
		newTree.createChild(rootKey.append("newLeft"), "newThree");

		newTree.createChild(rootKey.append("newRight"), "newRightOfRight");
		newTree.createChild(rootKey.append("newRight").append("newRightOfRight"), "bottom");

		/* get delta on different trees */
		deltaTree = newTree.forwardDeltaWith(tree, DefaultElementComparator.getComparator());

		/* assert delta has same content as tree */
		assertTree(deltaTree);
		assertThat(rootKey.append("newLeft")).matches(not(tree::includes));
		assertThat(rootKey.append("newRight")).matches(not(tree::includes));
	}

	/**
	 * Initialize the receiver so that it is a complete, empty tree.  It does
	 * not represent a delta on another tree.  An empty tree is defined to
	 * have a root node with nil data and no children.
	 */

	@Test
	public void testEmpty() {
		assertThat(rootKey).matches(emptyTree::includes);
		assertNotNull(TestHelper.getRootNode(emptyTree));
		assertThat(TestHelper.getRootNode(emptyTree).getChildren()).isEmpty();
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
		/* make several changes */
		changedTree.deleteChild(leftKey, "two");
		changedTree.createChild(leftKey, "four");
		changedTree.createChild(leftKey, "five");
		changedTree.createChild(leftKey, "six");
		changedTree.createChild(rootKey, "NewTopLevel");

		/* make delta tree */
		deltaTree = tree.forwardDeltaWith(changedTree, DefaultElementComparator.getComparator());

		/* get changedTree from original and forward delta on original */
		DeltaDataTree assembledTree = tree.assembleWithForwardDelta(deltaTree);

		/* make sure the reconstructed tree is as expected */
		assertThat(rootKey).matches(assembledTree::includes);
		assertThat(leftKey).matches(assembledTree::includes);
		assertThat(rightKey).matches(assembledTree::includes);
		assertThat(rootKey.append("NewTopLevel")).matches(assembledTree::includes);

		assertThat(leftKey.append("one")).matches(assembledTree::includes);
		assertThat(leftKey.append("two")).matches(not(assembledTree::includes));
		assertThat(leftKey.append("three")).matches(assembledTree::includes);
		assertThat(leftKey.append("four")).matches(assembledTree::includes);
		assertThat(leftKey.append("five")).matches(assembledTree::includes);
		assertThat(leftKey.append("six")).matches(assembledTree::includes);
		assertThat(rightKey.append("rightOfRight")).matches(assembledTree::includes);
	}

	/**
	 * Tests the forwardDeltaWith() function using the equality comparer.
	 */
	@Test
	public void testForwardDeltaWithEquality() {
		/* make several changes */
		changedTree.deleteChild(leftKey, "two");
		changedTree.createChild(leftKey, "four");
		IPath oneKey = leftKey.append("one");
		changedTree.setData(oneKey, "New");

		/* make delta tree */
		deltaTree = tree.forwardDeltaWith(changedTree, DefaultElementComparator.getComparator());

		/* get changedTree from original and forward delta on original */
		DeltaDataTree assembledTree = tree.assembleWithForwardDelta(deltaTree);

		/* make sure the reconstructed tree is as expected */
		assertThat(rootKey).matches(assembledTree::includes);
		assertThat(leftKey).matches(assembledTree::includes);
		assertThat(rightKey).matches(assembledTree::includes);

		assertThat(leftKey.append("one")).matches(assembledTree::includes);
		assertThat(leftKey.append("two")).matches(not(assembledTree::includes));
		assertThat(leftKey.append("three")).matches(assembledTree::includes);
		assertThat(leftKey.append("four")).matches(assembledTree::includes);
		Object data = assembledTree.getData(oneKey);
		assertNotNull(data);
		assertEquals("New", data);
	}

	/**
	 * Answer the key of the child with the given index of the
	 * specified node.
	 *
	 * @exception ObjectNotFoundException
	 * 	parentKey does not exist in the receiver
	 * @exception ArrayIndexOutOfBoundsException
	 *	if no child with the given index (runtime exception)
	 */

	@Test
	public void testGetChild() {
		/* Get valid children */
		assertEquals(leftKey, tree.getChild(rootKey, 0));
		assertEquals(leftKey.append("two"), tree.getChild(leftKey, 2));

		/* Get non-existant child of root */
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> tree.getChild(rootKey, 99));

		/* Get non-existant child of interior node */
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> tree.getChild(leftKey, 99));

		/* Get non-existant child of leaf node */
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> tree.getChild(leftKey.append("one"), 99));

		/* Try to getChild using non-existent key */
		assertThrows(ObjectNotFoundException.class, () -> tree.getChild(rootKey.append("bogus"), 0));
	}

	/**
	 * Answer  the number of children of the specified node.
	 *
	 * @exception ObjectNotFoundException
	 *	parentKey does not exist in the receiver
	 */

	@Test
	public void testGetChildCount() {
		/* empty tree */
		assertEquals(0, emptyTree.getChildCount(rootKey));

		/* root node */
		assertEquals(2, tree.getChildCount(rootKey));

		/* interior nodes */
		assertEquals(3, tree.getChildCount(leftKey));
		assertEquals(1, tree.getChildCount(rightKey));

		/* leaf nodes */
		assertEquals(0, tree.getChildCount(leftKey.append("one")));
		assertEquals(0, tree.getChildCount(leftKey.append("three")));
		assertEquals(0, tree.getChildCount(rightKey.append("rightOfRight")));

		/* invalid parent key */
		assertThrows(ObjectNotFoundException.class, () -> tree.getChildCount(rootKey.append("bogus")));

		/* invalid parent of empty tree */
		assertThrows(ObjectNotFoundException.class, () -> emptyTree.getChildCount(rootKey.append("bogus")));
	}

	/**
	 * Answer the keys for the children of the specified node.
	 *
	 * @exception ObjectNotFoundException
	 *	parentKey does not exist in the receiver"
	 */

	@Test
	public void testGetChildren() {
		IPath testChildren[], rootChildren[] = {leftKey, rightKey}, leftChildren[] = {leftKey.append("one"), leftKey.append("two"), leftKey.append("three")}, rightChildren[] = {rightKey.append("rightOfRight")};

		/* empty tree */
		testChildren = emptyTree.getChildren(rootKey);
		assertThat(testChildren).isEmpty();

		/* root node */
		testChildren = tree.getChildren(rootKey);
		assertThat(testChildren).containsExactly(rootChildren[0], rootChildren[1]);

		/* interior nodes */
		testChildren = tree.getChildren(leftKey);
		assertThat(testChildren).containsExactly(leftChildren[0], leftChildren[2], leftChildren[1]);

		/* leaf nodes */
		testChildren = tree.getChildren(leftChildren[0]);
		assertThat(testChildren).isEmpty();

		testChildren = tree.getChildren(rightChildren[0]);
		assertThat(testChildren).isEmpty();

		/* invalid parent key */
		assertThrows(ObjectNotFoundException.class, () -> tree.getChildren(rootKey.append("bogus")));

		/* invalid parent of empty tree */
		assertThrows(ObjectNotFoundException.class, () -> emptyTree.getChildren(rootKey.append("bogus")));
	}

	/**
	 * Returns the local names for the children of the specified node.
	 *
	 * @exception ObjectNotFoundException
	 *	parentKey does not exist in the receiver
	 */

	@Test
	public void testGetNamesOfChildren() {
		String testChildren[], rootChildren[] = {"leftOfRoot", "rightOfRoot"}, leftChildren[] = {"one", "two", "three"}, rightChildren[] = {"rightOfRight"};

		/* empty tree */
		testChildren = emptyTree.getNamesOfChildren(rootKey);
		assertThat(testChildren).isEmpty();

		/* root node */
		testChildren = tree.getNamesOfChildren(rootKey);
		assertThat(testChildren).containsExactly(rootChildren[0], rootChildren[1]);

		/* interior nodes */
		testChildren = tree.getNamesOfChildren(leftKey);
		assertThat(testChildren).containsExactly(leftChildren[0], leftChildren[2], leftChildren[1]);

		testChildren = tree.getNamesOfChildren(rightKey);
		assertThat(testChildren).containsExactly(rightChildren[0]);

		/* leaf nodes */
		testChildren = tree.getNamesOfChildren(leftKey.append("one"));
		assertThat(testChildren).isEmpty();

		testChildren = tree.getNamesOfChildren(rightKey.append("rightOfRight"));
		assertThat(testChildren).isEmpty();

		/* invalid parent key */
		assertThrows(ObjectNotFoundException.class, () -> tree.getNamesOfChildren(rootKey.append("bogus")));

		/* invalid parent of empty tree */
		assertThrows(ObjectNotFoundException.class, () -> emptyTree.getNamesOfChildren(rootKey.append("bogus")));
	}

	/**
	 * Returns true if the receiver includes a node with the given key, false
	 * otherwise.
	 */

	@Test
	public void testIncludes() {
		/* tested in testCreateChild() and testDeleteChild() */
		assertThat(rootKey).matches(emptyTree::includes);
		assertThat(rootKey).matches(tree::includes);
		assertThat(leftKey).matches(tree::includes);
		assertThat(rightKey).matches(tree::includes);
		assertThat(leftKey.append("one")).matches(tree::includes);
		assertThat(rightKey.append("rightOfRight")).matches(tree::includes);

		assertThat(rootKey.append("bogus")).matches(not(emptyTree::includes));
		assertThat(rootKey.append("bogus")).matches(not(tree::includes));
		assertThat(leftKey.append("bogus")).matches(not(tree::includes));
		assertThat(leftKey.append("one").append("bogus")).matches(not(tree::includes));
		assertThat(rightKey.append("bogus")).matches(not(tree::includes));
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
		assertThat(leftKey.append("new")).matches(deltas[0]::includes);

		/* create a second delta and make a change to that */
		deltas[0].immutable();
		deltas[1] = deltas[0].newEmptyDeltaTree();
		deltas[1].deleteChild(leftKey, "one");
		assertEquals(deltas[1].getParent(), deltas[0]);
		assertThat(leftKey.append("one")).matches(not(deltas[1]::includes));

		/* create a third delta and make a change to that */
		deltas[1].immutable();
		deltas[2] = deltas[1].newEmptyDeltaTree();
		deltas[2].createChild(rootKey, "newTopLevel");
		assertEquals(deltas[2].getParent(), deltas[1]);
		assertEquals(deltas[2].getParent().getParent(), deltas[0]);
		assertThat(leftKey.append("one")).matches(not(deltas[2]::includes));
		assertThat(rootKey.append("newTopLevel")).matches(deltas[2]::includes);
	}

	/**
	 * Tests the newEmptyDeltaTree method
	 */
	@Test
	public void testNewEmptyDeltaTree() {
		tree.immutable();
		DeltaDataTree delta = tree.newEmptyDeltaTree();
		assertEquals(tree, delta.getParent());
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

		tree1.copyCompleteSubtree(IPath.ROOT);
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

		assertThat(elementC).as("child should not exist after deletion").matches(not(tree1::includes));

		tree1.copyCompleteSubtree(IPath.ROOT);
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
		assertThat(leftKey.append("new")).matches(deltas[0]::includes);

		/* create a second delta and make a change to that */
		deltas[0].immutable();
		deltas[1] = deltas[0].newEmptyDeltaTree();
		deltas[1].deleteChild(leftKey, "one");
		assertEquals(deltas[1].getParent(), deltas[0]);
		assertThat(leftKey.append("one")).matches(not(deltas[1]::includes));

		/* create a third delta and make a change to that */
		deltas[1].immutable();
		deltas[2] = deltas[1].newEmptyDeltaTree();
		deltas[2].createChild(rootKey, "newTopLevel");
		assertEquals(deltas[2].getParent(), deltas[1]);
		assertEquals(deltas[2].getParent().getParent(), deltas[0]);
		assertThat(leftKey.append("one")).matches(not(deltas[2]::includes));
		assertThat(rootKey.append("newTopLevel")).matches(deltas[2]::includes);

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
		assertThat(leftKey.append("new")).matches(not(tree::includes));
		assertThat(leftKey.append("one")).matches(tree::includes);
		assertTree(deltas[0]);
		assertThat(leftKey.append("new")).matches(deltas[0]::includes);
		assertThat(leftKey.append("one")).matches(deltas[0]::includes);
		assertThat(leftKey.append("new")).matches(deltas[1]::includes);
		assertThat(leftKey.append("one")).matches(not(deltas[1]::includes));
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
