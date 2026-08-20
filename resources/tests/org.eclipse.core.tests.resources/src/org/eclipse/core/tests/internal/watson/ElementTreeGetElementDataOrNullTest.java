/*******************************************************************************
 * Copyright (c) 2026 Vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vogella GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.internal.watson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.eclipse.core.internal.watson.ElementTree;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ElementTree#getElementDataOrNull(org.eclipse.core.runtime.IPath)}.
 */
public class ElementTreeGetElementDataOrNullTest implements IPathConstants {

	@Test
	public void testPresentElementWithData() {
		ElementTree tree = new ElementTree();
		tree.createElement(solution, "solution");
		tree.createElement(project1, "project1");

		assertEquals("project1", tree.getElementDataOrNull(project1));
		assertEquals(tree.getElementData(project1), tree.getElementDataOrNull(project1));
	}

	@Test
	public void testPresentElementWithNullData() {
		ElementTree tree = new ElementTree();
		tree.createElement(solution, null);

		assertNull(tree.getElementDataOrNull(solution));
	}

	@Test
	public void testAbsentElement() {
		ElementTree tree = new ElementTree();
		tree.createElement(solution, "solution");

		assertNull(tree.getElementDataOrNull(project1));
		assertThrows(IllegalArgumentException.class, () -> tree.getElementData(project1));
	}

	@Test
	public void testRoot() {
		ElementTree tree = new ElementTree();

		assertNull(tree.getElementDataOrNull(root));
	}

	@Test
	public void testDeletedElementInDelta() {
		ElementTree tree = new ElementTree();
		tree.createElement(solution, "solution");
		tree.createElement(project1, "project1");
		tree.immutable();

		ElementTree delta = tree.newEmptyDelta();
		delta.deleteElement(project1);

		assertNull(delta.getElementDataOrNull(project1));
		assertEquals("project1", tree.getElementDataOrNull(project1));
	}

	@Test
	public void testResultIsIndependentOfLookupCache() {
		ElementTree tree = new ElementTree();
		tree.createElement(solution, "solution");
		tree.createElement(project1, "project1");
		tree.createElement(project2, "project2");

		// A lookup for another path replaces the single-slot lookup cache.
		tree.includes(project2);
		assertEquals("project1", tree.getElementDataOrNull(project1));
		tree.includes(project2);
		assertNull(tree.getElementDataOrNull(folder1));
	}
}
