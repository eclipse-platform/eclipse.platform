/*******************************************************************************
 * Copyright (c) 2006, 2016 IBM Corporation and others.
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

package org.eclipse.ua.tests.cheatsheet.other;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetElement;
import org.eclipse.ui.internal.cheatsheets.views.CheatSheetManager;
import org.junit.Test;

public class TestCheatSheetManager {

	private static final String CHEATSHEET_ID = "RandomId0234";
	private static final String CHEATSHEET_NAME = "Name";
	private static final String KEY1 = "key1";
	private static final String PARENT_KEY1 = "parent.key1";
	private static final String PARENT_KEY2 = "parent.key2";
	private static final String VALUE_A = "A";
	private static final String VALUE_B = "B";
	private static final String VALUE_C = "C";

	public CheatSheetManager createManager() {
		CheatSheetElement element = new CheatSheetElement(CHEATSHEET_NAME);
		element.setID(CHEATSHEET_ID);
		return new CheatSheetManager(element);
	}

	/**
	 * Test for correct initialization
	 */
	@Test
	public void testNewManager() {
		CheatSheetManager manager = createManager();
		assertNotNull(manager.getKeySet());
		assertTrue(manager.getKeySet().isEmpty());
		assertEquals(CHEATSHEET_ID, manager.getCheatSheetID());
	}

	/**
	 * Test that if there is no parent all references are local.
	 */
	@Test
	public void testNoParent() {
		CheatSheetManager manager = createManager();
		manager.setDataQualified(KEY1, VALUE_A);
		manager.setDataQualified(PARENT_KEY1, VALUE_B);
		assertEquals(VALUE_A, manager.getData(KEY1));
		assertEquals(VALUE_A, manager.getDataQualified(KEY1));
		assertEquals(VALUE_B, manager.getData(PARENT_KEY1));
		assertEquals(VALUE_B, manager.getDataQualified(PARENT_KEY1));
		Set<?> keys = manager.getKeySet();
		assertEquals(keys.size(), 2);
		assertTrue(keys.contains(KEY1));
		assertTrue(keys.contains(PARENT_KEY1));
	}

	/**
	 * Test that if there is a parent getDataQualified and setDataQualified
	 * can reference the parent but getData is always local
	 */
	@Test
	public void testParentAccess() {
		CheatSheetManager manager = createManager();
		CheatSheetManager parentManager = createManager();
		manager.setParent(parentManager);
		manager.setDataQualified(KEY1, VALUE_A);
		manager.setDataQualified(PARENT_KEY1, VALUE_B);
		assertEquals(VALUE_A, manager.getData(KEY1));
		assertEquals(VALUE_A, manager.getDataQualified(KEY1));
		assertNull(manager.getData(PARENT_KEY1));
		assertEquals(VALUE_B, manager.getDataQualified(PARENT_KEY1));
		Set<?> keys = manager.getKeySet();
		assertEquals(keys.size(), 1);
		assertTrue(keys.contains(KEY1));
		assertFalse(keys.contains(PARENT_KEY1));

		Set<?> parentKeys = parentManager.getKeySet();
		assertEquals(parentKeys.size(), 1);
		assertTrue(parentKeys.contains(KEY1));
		assertFalse(parentKeys.contains(PARENT_KEY1));
	}

	/**
	 * Test that setData always writes locally
	 */
	@Test
	public void testSetDataWithParent() {
		CheatSheetManager manager = createManager();
		CheatSheetManager parentManager = createManager();
		manager.setParent(parentManager);
		manager.setData(KEY1, VALUE_A);
		manager.setData(PARENT_KEY1, VALUE_B);
		assertEquals(VALUE_A, manager.getData(KEY1));
		assertEquals(VALUE_A, manager.getDataQualified(KEY1));
		assertNull(manager.getDataQualified(PARENT_KEY1));
		assertEquals(VALUE_B, manager.getData(PARENT_KEY1));
		Set<?> keys = manager.getKeySet();
		assertEquals(keys.size(), 2);
		assertTrue(keys.contains(KEY1));
		assertTrue(keys.contains(PARENT_KEY1));

		Set<?> parentKeys = parentManager.getKeySet();
		assertEquals(parentKeys.size(), 0);
	}

	@Test
	public void testSubstitution() {
		CheatSheetManager manager = createManager();
		CheatSheetManager parentManager = createManager();
		manager.setParent(parentManager);
		manager.setDataQualified(KEY1, VALUE_A);
		manager.setDataQualified(PARENT_KEY1, VALUE_B);
		manager.setDataQualified(PARENT_KEY2, VALUE_C);
		assertEquals(VALUE_A, manager.getVariableData("${key1}"));
		assertEquals(VALUE_B, manager.getVariableData("${parent.key1}"));
		assertEquals(VALUE_C, manager.getVariableData("${parent.key2}"));
		assertEquals("Values are A B C", manager.performVariableSubstitution
				("Values are ${key1} ${parent.key1} ${parent.key2}"));
	}

}
