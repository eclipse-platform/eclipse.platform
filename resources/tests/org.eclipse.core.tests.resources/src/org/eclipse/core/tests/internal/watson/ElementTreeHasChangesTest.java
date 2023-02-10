/*******************************************************************************
 * Copyright (c) 2018 Simeon Andreev and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Simeon Andreev - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.internal.watson;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.internal.events.ResourceComparator;
import org.eclipse.core.internal.resources.ResourceInfo;
import org.eclipse.core.internal.watson.ElementTree;
import org.eclipse.core.runtime.IPath;
import org.junit.Test;

/**
 * Tests for
 * {@link ElementTree#hasChanges(ElementTree, ElementTree, org.eclipse.core.internal.watson.IElementComparator, boolean)}
 * while using
 * {@link org.eclipse.core.internal.events.ResourceComparator#getBuildComparator()}.
 */
public class ElementTreeHasChangesTest implements IPathConstants {

	/**
	 * Test hasChanges() without any changes to the tree.
	 */
	@Test
	public void testNoChanges() {
		ElementTree oldTree = createTree(solution, project1, project2, file1);

		ElementTree newTree = newTree(oldTree);

		boolean hasChanges = hasChanges(oldTree, newTree);
		assertFalse("expected no changes to be detected if the tree was not changed", hasChanges);
	}

	/**
	 * Test hasChanges() after adding an element to the tree.
	 */
	@Test
	public void testAddElement() {
		ElementTree oldTree = createTree(solution, project1, project2, file1);

		ElementTree newTree = newTree(oldTree);
		add(newTree, folder1);

		boolean hasChanges = hasChanges(oldTree, newTree);
		assertTrue("expected changes to be detected after adding an element to the tree", hasChanges);
	}

	/**
	 * Test hasChanges() after removing an element from the tree.
	 */
	@Test
	public void testRemoveElement() {
		ElementTree oldTree = createTree(solution, project1, project2, file1);

		ElementTree newTree = newTree(oldTree);
		remove(newTree, file1);

		boolean hasChanges = hasChanges(oldTree, newTree);
		assertTrue("expected changes to be detected after removing an element from the tree", hasChanges);
	}

	/**
	 * Test hasChanges() after adding and then removing an element to and from the
	 * tree.
	 */
	@Test
	public void testAddAndRemoveElement() {
		ElementTree oldTree = createTree(solution, project1, project2, file1);

		ElementTree newTree = newTree(oldTree);
		add(newTree, folder1);
		remove(newTree, folder1);

		boolean hasChanges = hasChanges(oldTree, newTree);
		assertTrue("expected changes to be detected after adding and removing the same element to and from the tree",
				hasChanges);
	}

	/**
	 * Test hasChanges() after changing the data of an element.
	 */
	@Test
	public void testChangeElementData() {
		IPath[] elements1 = { solution, project1, project2, file1 };
		ElementTree oldTree = createTree(elements1);

		ElementTree newTree = newTree(oldTree);
		newTree.setElementData(file1, "different data");

		boolean hasChanges = hasChanges(oldTree, newTree);
		assertTrue("expected changes to be detected after changing the data of a tree element", hasChanges);
	}

	/**
	 * Test hasChanges() after changing the data of a the tree, so that the build
	 * comparator detects a change.
	 */
	@Test
	public void testChangeTreeData() {
		IPath[] elements1 = { solution, project1, project2, file1 };
		ElementTree oldTree = createTree(elements1);
		incrementCharsetGenerationCount(oldTree);

		ElementTree newTree = newTree(oldTree);
		incrementCharsetGenerationCount(newTree);

		boolean hasChanges = hasChanges(oldTree, newTree);
		assertTrue("expected changes to be detected after changing the data of the tree", hasChanges);
	}

	private static boolean hasChanges(ElementTree oldTree, ElementTree newTree) {
		return ElementTree.hasChanges(oldTree, newTree, ResourceComparator.getBuildComparator(), true);
	}

	private static ElementTree createTree(IPath... paths) {
		ElementTree tree = new ElementTree();
		for (IPath path : paths) {
			add(tree, path);
		}
		return tree;
	}

	private static void incrementCharsetGenerationCount(ElementTree tree) {
		Object treeData = tree.getTreeData();
		ResourceInfo resourceInfo;
		if (treeData instanceof ResourceInfo ri) {
			resourceInfo = ri;
		} else {
			resourceInfo = new ResourceInfo();
			tree.setTreeData(resourceInfo);
		}
		resourceInfo.incrementCharsetGenerationCount();
	}

	private static ElementTree newTree(ElementTree oldTree) {
		return oldTree.newEmptyDelta();
	}

	private static void add(ElementTree tree, IPath path) {
		tree.createElement(path, path.lastSegment());
	}

	private static void remove(ElementTree tree, IPath path) {
		tree.deleteElement(path);
	}
}
