/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
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
package org.eclipse.debug.tests.viewer.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.debug.internal.ui.viewers.model.FilterTransform;
import org.eclipse.debug.tests.AbstractDebugTest;
import org.eclipse.jface.viewers.TreePath;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the virtual viewer's filter transform
 * @since 3.3
 */
public class FilterTransformTests extends AbstractDebugTest {

	public Object root;
	public Object element0;
	public Object element1;
	public Object element2;
	public Object element3;
	public Object element4;
	public Object element5;
	public Object element6;
	public Object element7;

	public FilterTransform transform;

	/**
	 * Builds a filter transform. Model has 8 elements,
	 * and elements 0, 2, 3, 6, 7 are filtered. Elements
	 * 1, 4, 5 are visible.
	 */
	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		transform = new FilterTransform();
		root = new Object();
		element0 = new Object();
		element1 = new Object();
		element2 = new Object();
		element3 = new Object();
		element4 = new Object();
		element5 = new Object();
		element6 = new Object();
		element7 = new Object();
		assertTrue(transform.addFilteredIndex(TreePath.EMPTY, 0, element0));
		assertTrue(transform.addFilteredIndex(TreePath.EMPTY, 2, element2));
		assertTrue(transform.addFilteredIndex(TreePath.EMPTY, 3, element3));
		assertTrue(transform.addFilteredIndex(TreePath.EMPTY, 6, element6));
		assertTrue(transform.addFilteredIndex(TreePath.EMPTY, 7, element7));
	}

	@Test
	public void testRemoveMiddleElementFromFilters() {
		assertTrue("Element should be filtered", transform.isFiltered(TreePath.EMPTY, 3)); //$NON-NLS-1$
		transform.removeElementFromFilters(TreePath.EMPTY, element3);
		assertFalse("Element should be unfiltered", transform.isFiltered(TreePath.EMPTY, 3)); //$NON-NLS-1$
		assertArrayEquals("Wrong filter state", transform.getFilteredChildren(TreePath.EMPTY), new int[] { //$NON-NLS-1$
				0, 2, 5, 6 });
	}

	@Test
	public void testRemoveFirstElementFromFilters() {
		assertTrue("Element should be filtered", transform.isFiltered(TreePath.EMPTY, 0)); //$NON-NLS-1$
		transform.removeElementFromFilters(TreePath.EMPTY, element0);
		assertFalse("Element should be unfiltered", transform.isFiltered(TreePath.EMPTY, 0)); //$NON-NLS-1$
		assertArrayEquals("Wrong filter state", transform.getFilteredChildren(TreePath.EMPTY), new int[] { //$NON-NLS-1$
				1, 2, 5, 6 });
	}

	@Test
	public void testRemoveLastFromFilters() {
		assertTrue("Element should be filtered", transform.isFiltered(TreePath.EMPTY, 7)); //$NON-NLS-1$
		transform.removeElementFromFilters(TreePath.EMPTY, element7);
		assertFalse("Element should be unfiltered", transform.isFiltered(TreePath.EMPTY, 7)); //$NON-NLS-1$
		assertArrayEquals("Wrong filter state", transform.getFilteredChildren(TreePath.EMPTY), new int[] { //$NON-NLS-1$
				0, 2, 3, 6 });
	}

	@Test
	public void testClearMiddleElementFromFilters() {
		assertTrue("Element should be filtered", transform.isFiltered(TreePath.EMPTY, 3)); //$NON-NLS-1$
		transform.clear(TreePath.EMPTY, 3);
		assertFalse("Element should be unfiltered", transform.isFiltered(TreePath.EMPTY, 3)); //$NON-NLS-1$
		assertArrayEquals("Wrong filter state", transform.getFilteredChildren(TreePath.EMPTY), new int[] { //$NON-NLS-1$
				0, 2, 6, 7 });
	}

	@Test
	public void testClearFirstElementFromFilters() {
		assertTrue("Element should be filtered", transform.isFiltered(TreePath.EMPTY, 0)); //$NON-NLS-1$
		transform.clear(TreePath.EMPTY, 0);
		assertFalse("Element should be unfiltered", transform.isFiltered(TreePath.EMPTY, 0)); //$NON-NLS-1$
		assertArrayEquals("Wrong filter state", transform.getFilteredChildren(TreePath.EMPTY), new int[] { //$NON-NLS-1$
				2, 3, 6, 7 });
	}

	@Test
	public void testClearLastFromFilters() {
		assertTrue("Element should be filtered", transform.isFiltered(TreePath.EMPTY, 7)); //$NON-NLS-1$
		transform.clear(TreePath.EMPTY, 7);
		assertFalse("Element should be unfiltered", transform.isFiltered(TreePath.EMPTY, 7)); //$NON-NLS-1$
		assertArrayEquals("Wrong filter state", transform.getFilteredChildren(TreePath.EMPTY), new int[] { //$NON-NLS-1$
				0, 2, 3, 6 });
	}

	@Test
	public void testViewToModelCount() {
		assertEquals("Wrong model count", 8, transform.viewToModelCount(TreePath.EMPTY, 3)); //$NON-NLS-1$
	}

	@Test
	public void testViewToModelIndex() {
		assertEquals("Wrong model index", 1, transform.viewToModelIndex(TreePath.EMPTY, 0)); //$NON-NLS-1$
		assertEquals("Wrong model index", 4, transform.viewToModelIndex(TreePath.EMPTY, 1)); //$NON-NLS-1$
		assertEquals("Wrong model index", 5, transform.viewToModelIndex(TreePath.EMPTY, 2)); //$NON-NLS-1$
	}

	@Test
	public void testAddAlreadyFiltered() {
		assertTrue("Element should be filtered", transform.isFiltered(TreePath.EMPTY, 0)); //$NON-NLS-1$
		boolean added = transform.addFilteredIndex(TreePath.EMPTY, 0, element0);
		assertFalse("Filter should not be added - should already have been there", added); //$NON-NLS-1$
	}


	/**
	 * Test to make sure that setModelChildCount() updates internal arrays
	 * appropriately. See bug 200325.
	 */
	@Test
	public void testRegression200325() {
		transform.setModelChildCount(TreePath.EMPTY, 2);
		try {
			transform.addFilteredIndex(TreePath.EMPTY, 3, new Object());
		} catch (ArrayIndexOutOfBoundsException e) {
			fail("AIOOBE Exception should not be thrown here, " + //$NON-NLS-1$
			"setModelChildCount should leave " + //$NON-NLS-1$
			"FilterTransform$Node in a consistent state"); //$NON-NLS-1$
		}
	}
}
