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

package org.eclipse.ua.tests.cheatsheet.composite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Hashtable;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.ua.tests.cheatsheet.util.MockTaskEditor;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.internal.cheatsheets.CheatSheetPlugin;
import org.eclipse.ui.internal.cheatsheets.composite.model.CompositeCheatSheetModel;
import org.eclipse.ui.internal.cheatsheets.composite.model.CompositeCheatSheetSaveHelper;
import org.eclipse.ui.internal.cheatsheets.composite.model.EditableTask;
import org.eclipse.ui.internal.cheatsheets.composite.model.TaskGroup;
import org.eclipse.ui.internal.cheatsheets.state.DefaultStateManager;
import org.eclipse.ui.internal.provisional.cheatsheets.ICompositeCheatSheetTask;
import org.junit.Test;

public class TestPersistence {

	/**
	 * Test that data can be saved and restored in Mementos
	 */

	private static final String MEMENTO_TAG = "Mtag";
	private final static String PATH1 = "Path1";
	private final static String PATH2 = "Path2";
	private final static String KEY = "key";
	private final static String DATA1 = "1";
	private final static String DATA2 = "2";

	private CompositeCheatSheetModel model;
	private TaskGroup rootTask;
	private EditableTask task1;
	private EditableTask task2;
	private MockTaskEditor editor1;
	private MockTaskEditor editor2;
	private CompositeCheatSheetSaveHelper helper;

	/**
	 * Initialize a composite cheatsheet with one root and two leaf tasks.
	 */
	private void createCompositeCheatSheet() {
		model = new CompositeCheatSheetModel("name", "description", "explorerId");
		model.setId("org.eclipse.ua.tests.testPersistence");
		rootTask = new TaskGroup(model, "root", "name", "kind");
		task1 = new EditableTask(model, "task1", "name", "kind");
		task2 = new EditableTask(model, "task2", "name", "kind");
		helper = new CompositeCheatSheetSaveHelper(new DefaultStateManager());
		model.setSaveHelper(helper);
		editor1 = new MockTaskEditor();
		editor2 = new MockTaskEditor();
		task1.setEditor(editor1);
		task2.setEditor(editor2);
		model.setRootTask(rootTask);
		rootTask.addSubtask(task1);
		rootTask.addSubtask(task2);
	}

	/**
	 * Test that the routines saveMemento() and readMemento() can write
	 * mementos to different files and keep the contents distinct.
	 */
	@Test
	public void testMementoSaveMultipleFiles() {
		XMLMemento memento = XMLMemento.createWriteRoot(MEMENTO_TAG);
		memento.putString(KEY, DATA1);
		CheatSheetPlugin cheatSheetPlugin = CheatSheetPlugin.getPlugin();
		IStatus status = cheatSheetPlugin.saveMemento(memento, PATH1);
		assertTrue(status.isOK());
		memento = XMLMemento.createWriteRoot(MEMENTO_TAG);
		memento.putString(KEY, DATA2);
		status = cheatSheetPlugin.saveMemento(memento, PATH2);
		assertTrue(status.isOK());
		memento = cheatSheetPlugin.readMemento(PATH1);
		assertEquals(DATA1, memento.getString(KEY));
		memento = cheatSheetPlugin.readMemento(PATH2);
		assertEquals(DATA2, memento.getString(KEY));
	}

	@Test
	public void testSaveTaskState() {
		createCompositeCheatSheet();
		task1.setState(ICompositeCheatSheetTask.IN_PROGRESS);
		task2.setState(ICompositeCheatSheetTask.COMPLETED);
		helper.saveCompositeState(model, null);

		createCompositeCheatSheet();
		model.loadState(new Hashtable<>());
		assertEquals(ICompositeCheatSheetTask.IN_PROGRESS, task1.getState());
		assertEquals(ICompositeCheatSheetTask.COMPLETED, task2.getState());
	}

	/**
	 * Test that each task can save its state in a memento and that state
	 * can be restored.
	 */
	@Test
	public void testSaveTaskMemento() {
		final String value1 = "13579";
		final String value2 = "AB24";
		createCompositeCheatSheet();
		// Start tasks with no memento
		task1.setState(ICompositeCheatSheetTask.COMPLETED);
		task2.setState(ICompositeCheatSheetTask.IN_PROGRESS);
		editor1.setInput(task1, null);
		editor2.setInput(task2, null);
		assertEquals(MockTaskEditor.NO_MEMENTO, editor1.getValue());
		assertEquals(MockTaskEditor.NO_MEMENTO, editor2.getValue());

		// Set the values to save in the memento
		editor1.setValue(value1);
		editor2.setValue(value2);
		task1.setState(ICompositeCheatSheetTask.COMPLETED);
		task2.setState(ICompositeCheatSheetTask.IN_PROGRESS);
		helper.saveCompositeState(model, null);

		createCompositeCheatSheet();
		model.loadState(new Hashtable<>());
		editor1.setInput(task1, model.getTaskMemento(task1.getId()));
		editor2.setInput(task2, model.getTaskMemento(task2.getId()));
		assertEquals(value1, editor1.getValue());
		assertEquals(value2, editor2.getValue());
	}

	/**
	 * Test that layout data is restored
	 */
	@Test
	public void testSaveLayoutData() {
		createCompositeCheatSheet();
		Map<String, String> values = new Hashtable<>();
		values.put("One", "1");
		values.put("Two", "2");
		helper.saveCompositeState(model, values);
		Map<String, String> restoredValues = new Hashtable<>();
		createCompositeCheatSheet();
		model.loadState(restoredValues);
		assertEquals(2, restoredValues.size());
		assertEquals("1", values.get("One"));
		assertEquals("2", values.get("Two"));
	}

}
