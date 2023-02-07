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
import static org.junit.Assert.assertTrue;

/**
 * Tests for the saving and restoring of the state of a simple cheat sheet in a memento
 */

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import org.eclipse.ui.internal.cheatsheets.data.CheatSheetSaveHelper;
import org.eclipse.ui.internal.cheatsheets.data.IParserTags;
import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetElement;
import org.eclipse.ui.internal.cheatsheets.views.CheatSheetManager;
import org.junit.Before;
import org.junit.Test;

public class TestStatePersistence {

	private static final String VALUE2 = "value2";
	private static final String KEY2 = "key2";
	private static final String VALUE1 = "value1";
	private static final String KEY1 = "key1";
	private static final String TEST_ID = "TestId";
	private static final String PATH = "ContentPath";

	private static class PropertySet {
		public String id;
		public int currentItem;
		public int button;
		public String contentPath;
		public List<String> completed;
		public List<String> expanded;
		public List<String> expandRestore;
		public Hashtable<String, String> subItemCompleted;
		public Hashtable<String, String> subItemSkipped;
		public CheatSheetManager manager;
	}

	private PropertySet propsToSave;
	private CheatSheetSaveHelper helper = new CheatSheetSaveHelper();
	private PropertySet restored;

	/*
	 * Initialize the properties that will be saved. Individual tests will modify
	 * the properties which apply to a particular test.
	 */
	@Before
	public void setUp() throws Exception {
		propsToSave = new PropertySet();
		propsToSave.button = 1;
		propsToSave.currentItem = 2;
		propsToSave.id = TEST_ID;
		propsToSave.contentPath = PATH;
		propsToSave.completed = new ArrayList<>();
		propsToSave.expanded = new ArrayList<>();
		propsToSave.expandRestore = new ArrayList<>();
		propsToSave.subItemCompleted = new Hashtable<>();
		propsToSave.subItemSkipped = new Hashtable<>();
		CheatSheetElement csElement = new CheatSheetElement(TEST_ID);
		propsToSave.manager = new CheatSheetManager(csElement);
	}

	private void save() {
		Properties propertiesToSave = new Properties();
		propertiesToSave.put(IParserTags.ID, propsToSave.id);
		propertiesToSave.put(IParserTags.CURRENT, Integer.toString(propsToSave.currentItem));
		propertiesToSave.put(IParserTags.BUTTON, Integer.toString(propsToSave.button));
		propertiesToSave.put(IParserTags.COMPLETED, propsToSave.completed);
		propertiesToSave.put(IParserTags.EXPANDED, propsToSave.expanded);
		propertiesToSave.put(IParserTags.EXPANDRESTORE, propsToSave.expandRestore);
		if (propsToSave.contentPath != null) {
			propertiesToSave.put(IParserTags.CONTENT_URL, propsToSave.contentPath);
		}
		if (propsToSave.subItemCompleted.size() > 0) {
			propertiesToSave.put(IParserTags.SUBITEMCOMPLETED, propsToSave.subItemCompleted);
		}
		if (propsToSave.subItemSkipped.size() > 0) {
			propertiesToSave.put(IParserTags.SUBITEMSKIPPED, propsToSave.subItemSkipped);
		}
		helper.saveState(propertiesToSave, propsToSave.manager);
	}

	@SuppressWarnings("unchecked")
	private PropertySet restore(String id) {
		PropertySet result = new PropertySet();
		Properties restored = helper.loadState(id);
		result.id = restored.getProperty(IParserTags.ID);
		result.button = Integer.parseInt(restored.getProperty(IParserTags.BUTTON));
		result.currentItem = Integer.parseInt(restored.getProperty(IParserTags.CURRENT));
		result.completed = (ArrayList<String>) restored.get(IParserTags.COMPLETED);
		result.contentPath = (String) restored.get(IParserTags.CONTENT_URL);
		result.expanded = (ArrayList<String>) restored.get(IParserTags.EXPANDED);
		result.expandRestore = (ArrayList<String>) restored.get(IParserTags.EXPANDRESTORE);
		result.subItemCompleted = (Hashtable<String, String>) restored.get(IParserTags.SUBITEMCOMPLETED);
		result.subItemSkipped = (Hashtable<String, String>) restored.get(IParserTags.SUBITEMSKIPPED);
		Hashtable<String, String> managerData = (Hashtable<String, String>) restored.get(IParserTags.MANAGERDATA);
		CheatSheetElement csElement = new CheatSheetElement(id);
		result.manager = new CheatSheetManager(csElement);
		result.manager.setData(managerData);
		return result;
	}

	private void restore() {
		restored = restore(TEST_ID);
	}

	/**
	 * Test save and restore of id, name
	 *
	 */
	@Test
	public void testBasicProperties() {
		save();
		restore();
		// Check the restored properties
		assertEquals(TEST_ID, restored.id);
		assertEquals(2, restored.currentItem);
		assertEquals(1, restored.button);
		assertEquals(PATH, restored.contentPath);
	}

	/**
	 * Test save and restore of CheatSheetManager
	 */
	@Test
	public void testCheatSheetManagerPersistence() {
		propsToSave.manager.setData(KEY1, VALUE1);
		propsToSave.manager.setData(KEY2, VALUE2);
		save();
		restore();
		assertEquals(VALUE1, restored.manager.getData(KEY1));
		assertEquals(VALUE2, restored.manager.getData(KEY2));
	}

	/**
	 * Test save and restore of completed, expanded, expandRestore
	 */
	@Test
	public void testItemPropertyPersistence() {
		propsToSave.completed.add("2");
		propsToSave.completed.add("5");
		propsToSave.expanded.add("3");
		propsToSave.expanded.add("6");
		propsToSave.expandRestore.add("9");
		propsToSave.expandRestore.add("99");
		propsToSave.expandRestore.add("999");
		save();
		restore();
		assertEquals(2, restored.completed.size());
		assertTrue(restored.completed.contains("2"));
		assertTrue(restored.completed.contains("5"));
		assertFalse(restored.completed.contains("3"));
		assertEquals(2, restored.expanded.size());
		assertTrue(restored.expanded.contains("3"));
		assertTrue(restored.expanded.contains("6"));
		assertEquals(3, restored.expandRestore.size());
		assertTrue(restored.expandRestore.contains("9"));
		assertTrue(restored.expandRestore.contains("99"));
		assertTrue(restored.expandRestore.contains("999"));
	}

	/**
	 * Test save and restore of subitem completed and skipped
	 */
	@Test
	public void testSubItemPropertyPersistence() {
		propsToSave.subItemCompleted.put("1", "3,5");
		propsToSave.subItemCompleted.put("2", "4,6");
		propsToSave.subItemSkipped.put("3","1,4,9");
		save();
		restore();
		assertEquals(2, restored.subItemCompleted.size());
		assertEquals("3,5", restored.subItemCompleted.get("1"));
		assertEquals("4,6", restored.subItemCompleted.get("2"));
		assertEquals(1, restored.subItemSkipped.size());
		assertEquals("1,4,9", restored.subItemSkipped.get("3"));
	}

}
