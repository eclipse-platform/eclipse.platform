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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.ui.internal.cheatsheets.composite.model.AbstractTask;
import org.eclipse.ui.internal.cheatsheets.composite.model.CompositeCheatSheetModel;
import org.eclipse.ui.internal.cheatsheets.composite.model.EditableTask;
import org.eclipse.ui.internal.cheatsheets.composite.model.TaskGroup;
import org.eclipse.ui.internal.cheatsheets.composite.model.TaskStateUtilities;
import org.eclipse.ui.internal.provisional.cheatsheets.ICompositeCheatSheetTask;
import org.eclipse.ui.internal.provisional.cheatsheets.ITaskGroup;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the functions which determine the state of tasks
 */

public class TestState {

	private CompositeCheatSheetModel model;

	@Before
	public void setUp() throws Exception {
		model = new CompositeCheatSheetModel("name", "description", "explorerId");
	}

	private void skip(ICompositeCheatSheetTask task) {
		((AbstractTask)task).setState(ICompositeCheatSheetTask.SKIPPED);
	}

	@Test
	public void testNoParent() {
		EditableTask task = new EditableTask(model, "id", "name", "ua.junit");
		assertNull(TaskStateUtilities.findSkippedAncestor(task));
		assertNull(TaskStateUtilities.findCompletedAncestor(task));
		assertNull(TaskStateUtilities.findBlockedAncestor(task));
	}

	@Test
	public void testSelfSkip() {
		EditableTask task = new EditableTask(model, "id", "name", "ua.junit");
		skip(task);
		assertNull(TaskStateUtilities.findSkippedAncestor(task));
		assertNull(TaskStateUtilities.findCompletedAncestor(task));
		assertNull(TaskStateUtilities.findBlockedAncestor(task));
	}

	@Test
	public void testSkippedParent() {
		TaskGroup root = new TaskGroup(model, "root", "rname", ITaskGroup.SEQUENCE);
		TaskGroup group = new TaskGroup(model, "group", "gname", ITaskGroup.SEQUENCE);
		EditableTask task = new EditableTask(model, "id", "name", "ua.junit");
		root.addSubtask(group);
		group.addSubtask(task);
		skip(root);
		skip(group);
		assertEquals(group, TaskStateUtilities.findSkippedAncestor(task));
		assertNull(TaskStateUtilities.findBlockedAncestor(task));
	}

	@Test
	public void testSkippedGrandparent() {
		TaskGroup root = new TaskGroup(model, "root", "rname", ITaskGroup.SEQUENCE);
		TaskGroup group = new TaskGroup(model, "group", "gname", ITaskGroup.SEQUENCE);
		EditableTask task = new EditableTask(model, "id", "name", "ua.junit");
		root.addSubtask(group);
		group.addSubtask(task);
		skip(root);
		assertEquals(root, TaskStateUtilities.findSkippedAncestor(task));
		assertNull(TaskStateUtilities.findBlockedAncestor(task));
	}

	@Test
	public void testCompletedGrandparent() {
		TaskGroup root = new TaskGroup(model, "root", "rname", ITaskGroup.CHOICE);
		TaskGroup group = new TaskGroup(model, "group", "gname", ITaskGroup.SEQUENCE);
		EditableTask task = new EditableTask(model, "id", "name", "ua.junit");
		task.setSkippable(true);
		EditableTask completed = new EditableTask(model, "id2", "name2", "ua.junit");
		root.addSubtask(group);
		group.addSubtask(task);
		root.addSubtask(completed);
		assertNull(TaskStateUtilities.findCompletedAncestor(task));
		assertNull(TaskStateUtilities.findBlockedAncestor(task));
		assertNull(TaskStateUtilities.findSkippedAncestor(task));
		assertTrue(TaskStateUtilities.isStartEnabled(task));
		assertTrue(TaskStateUtilities.isSkipEnabled(task));
		completed.complete();
		assertEquals(root, TaskStateUtilities.findCompletedAncestor(task));
		assertFalse(TaskStateUtilities.isStartEnabled(task));
		assertFalse(TaskStateUtilities.isSkipEnabled(task));
	}

	@Test
	public void testBlockedGrandparent() {
		TaskGroup root = new TaskGroup(model, "root", "rname", ITaskGroup.SET);
		TaskGroup grandparent = new TaskGroup(model, "group1", "gname1", ITaskGroup.SEQUENCE);
		TaskGroup parent = new TaskGroup(model, "group2", "gname2", ITaskGroup.SEQUENCE);
		EditableTask task = new EditableTask(model, "id", "name", "ua.junit");
		task.setSkippable(true);
		EditableTask required = new EditableTask(model, "id2", "name2", "ua.junit");
		root.addSubtask(grandparent);
		grandparent.addSubtask(parent);
		parent.addSubtask(task);
		root.addSubtask(required);
		grandparent.addRequiredTask(required);
		assertNull(TaskStateUtilities.findCompletedAncestor(task));
		assertNull(TaskStateUtilities.findSkippedAncestor(task));
		assertEquals(grandparent, TaskStateUtilities.findBlockedAncestor(task));
		assertNull(TaskStateUtilities.findBlockedAncestor(grandparent));
		assertFalse(TaskStateUtilities.isStartEnabled(task));
		assertTrue(TaskStateUtilities.isSkipEnabled(task));
		required.complete();
		assertNull(TaskStateUtilities.findBlockedAncestor(task));
		assertTrue(TaskStateUtilities.isStartEnabled(task));
		assertTrue(TaskStateUtilities.isSkipEnabled(task));
		assertFalse(TaskStateUtilities.isStartEnabled(parent));
	}

	@Test
	public void testRestartStates() {
		TaskGroup root = new TaskGroup(model, "root", "rname", ITaskGroup.SET);
		TaskGroup group = new TaskGroup(model, "group1", "gname", ITaskGroup.SET);
		EditableTask task1 = new EditableTask(model, "id1", "name1", "ua.junit");
		EditableTask task2 = new EditableTask(model, "id2", "name2", "ua.junit");
		EditableTask task3 = new EditableTask(model, "id3", "name3", "ua.junit");
		EditableTask task4 = new EditableTask(model, "id4", "name4", "ua.junit");
		task4.setSkippable(true);
		root.addSubtask(task1);
		root.addSubtask(task2);
		root.addSubtask(group);
		group.addSubtask(task3);
		group.addSubtask(task4);
		task1.setState(ICompositeCheatSheetTask.IN_PROGRESS);
		task2.setState(ICompositeCheatSheetTask.COMPLETED);
		task4.setState(ICompositeCheatSheetTask.SKIPPED);
		assertEquals(1, TaskStateUtilities.getRestartTasks(task1).length);
		assertEquals(task1, TaskStateUtilities.getRestartTasks(task1)[0]);
		assertEquals(1, TaskStateUtilities.getRestartTasks(task2).length);
		assertEquals(0, TaskStateUtilities.getRestartTasks(task3).length);
		assertEquals(1, TaskStateUtilities.getRestartTasks(task4).length);
		assertEquals(1, TaskStateUtilities.getRestartTasks(group).length);
		assertEquals(task4, TaskStateUtilities.getRestartTasks(group)[0]);
		assertEquals(3, TaskStateUtilities.getRestartTasks(root).length);
	}

	/**
	 * Test that resetting a task resets dependents and their children also
	 */
	@Test
	public void testResetDependents() {
		TaskGroup root = new TaskGroup(model, "root", "rname", ITaskGroup.SET);
		TaskGroup group = new TaskGroup(model, "group1", "gname", ITaskGroup.SET);
		TaskGroup subGroup = new TaskGroup(model, "group2", "gname2", ITaskGroup.SET);
		EditableTask task1 = new EditableTask(model, "id1", "name1", "ua.junit");
		EditableTask task2 = new EditableTask(model, "id2", "name2", "ua.junit");
		EditableTask task3 = new EditableTask(model, "id3", "name3", "ua.junit");
		EditableTask task4 = new EditableTask(model, "id4", "name4", "ua.junit");
		EditableTask task5 = new EditableTask(model, "id5", "name5", "ua.junit");
		task2.addRequiredTask(task1);
		task3.addRequiredTask(task2);
		subGroup.addRequiredTask(task3);
		task4.addRequiredTask(subGroup);
		root.addSubtask(group);
		group.addSubtask(task1);
		group.addSubtask(task2);
		group.addSubtask(task3);
		group.addSubtask(subGroup);
		group.addSubtask(task4);
		subGroup.addSubtask(task5);
		task1.complete();
		task2.complete();
		task3.complete();
		task5.setStarted();
		assertEquals(4, TaskStateUtilities.getRestartTasks(root).length);
		assertEquals(4, TaskStateUtilities.getRestartTasks(task1).length);
		assertEquals(3, TaskStateUtilities.getRestartTasks(task2).length);
		assertEquals(2, TaskStateUtilities.getRestartTasks(task3).length);
		assertEquals(1, TaskStateUtilities.getRestartTasks(subGroup).length);
		// Reset task5 and  start task 1 and 3 and skip task 2.
		// Resetting task1 will not require task2 or task3 to be reset
		task5.setState(ICompositeCheatSheetTask.NOT_STARTED);
		task3.setState(ICompositeCheatSheetTask.NOT_STARTED);
		task2.setState(ICompositeCheatSheetTask.NOT_STARTED);
		task2.setState(ICompositeCheatSheetTask.SKIPPED);
		task3.setStarted();
		assertEquals(3, TaskStateUtilities.getRestartTasks(root).length);
		assertEquals(1, TaskStateUtilities.getRestartTasks(task1).length);
		assertEquals(2, TaskStateUtilities.getRestartTasks(task2).length);
	}

}
