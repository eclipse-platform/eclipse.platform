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
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *******************************************************************************/
package org.eclipse.core.tests.internal.watson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Random;
import org.eclipse.core.internal.watson.ElementTree;
import org.eclipse.core.internal.watson.ElementTreeWriter;
import org.eclipse.core.internal.watson.IElementComparator;
import org.eclipse.core.runtime.IPath;

/**
 * Framework for running tests on element tree components.
 * Supplies convenience methods and some representative element trees.
 */
class TestUtil implements IPathConstants {
	static final IElementComparator fComparator = TestElementComparator.getComparator();

	/**
	 * Asserts that two trees are equal. If they are not
	 * an AssertionFailedError is thrown.
	 * @param message the detail message for this assertion
	 * @param expected the expected value of a tree
	 * @param actual the actual value of a tree
	 */
	static protected void assertEqualTrees(String message, ElementTree expected, ElementTree actual) {
		assertEqualTrees(message, expected, actual, IPath.ROOT, ElementTreeWriter.D_INFINITE);
	}

	/**
	 * Asserts that two trees are equal from the given path downwards. If they are not
	 * an AssertionFailedError is thrown.
	 * @param message the detail message for this assertion
	 * @param expected the expected value of a tree
	 * @param actual the actual value of a tree
	 */
	static protected void assertEqualTrees(String message, ElementTree expected, ElementTree actual, IPath path) {
		assertEqualTrees(message, expected, actual, path, ElementTreeWriter.D_INFINITE);
	}

	/**
	 * Asserts that two trees are equal. If they are not
	 * an AssertionFailedError is thrown.
	 * @param msg	 the detail message for this assertion
	 * @param expected the expected value of a tree
	 * @param actual the actual value of a tree
	 * @param depth The depth to compare to.
	 */
	static protected void assertEqualTrees(String msg, ElementTree expected, ElementTree actual, IPath path, int depth) {
		/* check node at current path */
		assertTrue(msg, expected.includes(path));
		assertTrue(msg, actual.includes(path));
		assertEquals(msg, expected.getElementData(path), actual.getElementData(path));

		if (depth != 0) {
			/* get the children */
			IPath[] expectedChildren = expected.getChildren(path);
			IPath[] actualChildren = actual.getChildren(path);
			assertEquals("Number of children", expectedChildren.length, actualChildren.length);

			int newDepth = depth;
			if (depth != ElementTreeWriter.D_INFINITE) {
				--newDepth;
			}
			for (int i = 0; i < expectedChildren.length; i++) {
				assertEquals("children IDs", expectedChildren[i], actualChildren[i]);
				assertEqualTrees("Recursive call", expected, actual, expectedChildren[i], newDepth);
			}
		}
	}

	/**
	 * Asserts that the given tree contains all of the given paths
	 */
	static protected void assertHasPaths(ElementTree tree, IPath[] paths) {
		for (int i = 0; i < paths.length; i++) {
			assertTrue("assertHasPaths" + i, tree.includes(paths[i]));
		}
	}

	/**
	 * Asserts that the given tree doesn not contain all of the given paths
	 */
	static protected void assertNoPaths(ElementTree tree, IPath[] paths) {
		for (IPath path : paths) {
			assertFalse("assertNoPaths: " + path, tree.includes(path));
		}
	}

	/**
	 * Asserts that the Test ElementTree has its original structure
	 */
	static protected void assertTreeStructure(ElementTree tree) {
		assertHasPaths(tree, getTreePaths());

		IPath[] children = tree.getChildren(IPath.ROOT);
		assertEquals(1, children.length);

		/* solution children */
		children = tree.getChildren(children[0]);
		assertEquals(2, children.length);
		assertEquals(0, tree.getChildren(project1).length);

		/* project2 children */
		children = tree.getChildren(children[1]);
		assertEquals(3, children.length);
		assertEquals(0, tree.getChildren(file1).length);
		assertEquals(0, tree.getChildren(folder2).length);

		/* folder1 children */
		children = tree.getChildren(children[1]);
		assertEquals(3, children.length);
		assertEquals(0, tree.getChildren(file2).length);
		assertEquals(0, tree.getChildren(folder4).length);

		/* folder3 children */
		children = tree.getChildren(children[1]);
		assertEquals(1, children.length);
		assertEquals(0, tree.getChildren(file3).length);
	}

	static ElementTree createTestElementTree() {
		/**
		 * This is a sample element tree on which tests are performed.
		 * The Paths in this tree are found in IPathConstants.  The tree's
		 * basic structure is as follows:
		 *	solution
		 *		project1
		 *		project2
		 *			file1
		 *			folder1
		 *				file2
		 *				folder3
		 *					file3
		 *				folder4
		 *			folder2
		 */

		ElementTree tree = new ElementTree();
		tree.createElement(solution, "solution");
		tree.createElement(project1, "project1");
		tree.createElement(project2, "project2");
		tree.createElement(file1, "file1");
		tree.createElement(folder1, "folder1");
		tree.createElement(folder2, "folder2");
		tree.createElement(file2, "file2");
		tree.createElement(folder3, "folder3");
		tree.createElement(folder4, "folder4");
		tree.createElement(file3, "file3");

		return tree;
	}

	/**
	 * Does several routine operations on the subtree rooted at
	 * the given path.  Opens a new delta after every operation.
	 * Returns an array of all the intermediary ElementTrees.
	 */
	static protected ElementTree[] doManyRoutineOperations(ElementTree tree, IPath path) {
		ArrayList<ElementTree> trees = new ArrayList<>();
		trees.add(tree);

		int repeat = 1;

		/* create file elements */
		tree = tree.newEmptyDelta();
		IPath[] files = getFilePaths(path);
		for (IPath file : files) {
			Object data = file.toString();
			tree.createElement(file, data);

			tree.immutable();
			trees.add(tree);
			tree = tree.newEmptyDelta();
		}

		/* modify the data of all file elements a few times */
		for (int i = 0; i < repeat; i++) {
			Object data = "data" + i;
			for (IPath file : files) {
				tree.setElementData(file, data);
				tree.immutable();
				trees.add(tree);
				tree = tree.newEmptyDelta();
			}
		}

		/* delete all file elements */
		for (IPath file : files) {
			tree.deleteElement(file);
			tree.immutable();
			trees.add(tree);
			tree = tree.newEmptyDelta();
		}

		ElementTree[] results = new ElementTree[trees.size()];
		trees.toArray(results);
		return results;
	}

	/**
	 * Does several routine operations on the subtree rooted at
	 * the given path.  Returns an array of all the intermediary elementtrees.
	 */
	static protected ElementTree[] doRoutineOperations(ElementTree tree, IPath path) {
		ArrayList<org.eclipse.core.internal.watson.ElementTree> trees = new ArrayList<>();
		trees.add(tree);

		int repeat = 1;

		/* create file elements */
		tree = tree.newEmptyDelta();
		IPath[] files = getFilePaths(path);
		for (IPath file : files) {
			Object data = file.toString();

			tree.createElement(file, data);
		}

		tree.immutable();
		trees.add(tree);
		tree = tree.newEmptyDelta();

		/* modify the data of all file elements a few times */
		for (int i = 0; i < repeat; i++) {
			Object data = "data" + i;
			for (IPath file : files) {
				tree.setElementData(file, data);
			}
		}

		tree.immutable();
		trees.add(tree);
		tree = tree.newEmptyDelta();

		/* delete all file elements */
		for (IPath file : files) {
			tree.deleteElement(file);
		}

		tree.immutable();
		trees.add(tree);

		ElementTree[] results = new ElementTree[trees.size()];
		trees.toArray(results);
		return results;
	}

	/**
	 * Returns an array of paths below the given path.
	 */
	static protected IPath[] getFilePaths(IPath project3) {
		String[] names = getJavaLangUnits();
		IPath[] paths = new IPath[names.length];
		for (int i = 0; i < paths.length; i++) {
			paths[i] = project3.append(names[i]);
		}
		return paths;
	}

	/**
	 * Returns strings representing javalang classes
	 */
	static protected String[] getJavaLangUnits() {
		return new String[] {"AbstractMethodError.java", "ArithmeticException.java", "ArrayIndexOutOfBoundsException.java", "ArrayStoreException.java", "Boolean.java", "Byte.java", "Character.java", "Class.java", "ClassCastException.java", "ClassCircularityError.java", "ClassFormatError.java", "ClassLoader.java", "ClassNotFoundException.java", "Cloneable.java", "CloneNotSupportedException.java", "Compiler.java", "Double.java", "Error.java",};
	}

	/**
	 * Returns the paths for all the elements in the tree in top-down order
	 */
	static protected IPath[] getTreePaths() {
		return new IPath[] {solution, project1, project2, folder1, folder2, folder3, folder4, file1, file2, file3};
	}

	/**
	 * Randomly scrambles an array.
	 */
	static protected void scramble(Object[] first) {
		Random random = new Random(System.currentTimeMillis());

		final int len = first.length;
		for (int i = 0; i < len * 100; i++) {
			/* get any array offset */
			int off1 = (int) (random.nextFloat() * len);
			if (off1 == len) {
				continue;
			}

			/* get another array offset */
			int off2 = (int) (random.nextFloat() * len);
			if (off2 == len) {
				continue;
			}

			/* switch */
			Object temp = first[off1];
			first[off1] = first[off2];
			first[off2] = temp;
		}
	}

	/**
	 * Randomly scrambles two arrays, ensuring that both
	 * arrays undergo the same permutation.
	 */
	static protected void scramble(Object[] first, Object[] second) {
		assertTrue(first.length == second.length);
		Random random = new Random(System.currentTimeMillis());

		final int len = first.length;
		for (int i = 0; i < len * 100; i++) {
			/* get any array offset */
			int off1 = (int) (random.nextFloat() * len);
			if (off1 == len) {
				continue;
			}

			/* get another array offset */
			int off2 = (int) (random.nextFloat() * len);
			if (off2 == len) {
				continue;
			}

			/* switch */
			Object temp = first[off1];
			first[off1] = first[off2];
			first[off2] = temp;

			temp = second[off1];
			second[off1] = second[off2];
			second[off2] = temp;
		}
	}

}
