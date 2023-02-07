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

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.ui.internal.cheatsheets.composite.model.CompositeCheatSheetModel;
import org.eclipse.ui.internal.cheatsheets.composite.model.EditableTask;
import org.eclipse.ui.internal.cheatsheets.composite.model.TaskGroup;
import org.eclipse.ui.internal.provisional.cheatsheets.ICompositeCheatSheetTask;
import org.eclipse.ui.internal.provisional.cheatsheets.ITaskGroup;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for the events that get generated when the state of a task changes.
 * These all use the same model. For reset events we don't count the number of
 * events sent since the algorithm used to determine the impacted tasks will
 * add tasks to the list if there is uncertainty about whether it has become
 * unblocked.
 */

public class TestTaskEvents {

	public static class TaskMap {

		private Map<String, TaskCounter> map = new HashMap<>();
		private int eventCount = 0;

		public void put(ICompositeCheatSheetTask task) {
			final String id = task.getId();
			if (map.containsKey(id)) {
				map.get(id).incrementCount();
			} else {
				map.put(id, new TaskCounter());
			}
			eventCount++;
		}

		public int getEventCount(ICompositeCheatSheetTask task) {
			final String id = task.getId();
			if (map.containsKey(id)) {
				 return map.get(id).getCount();
			} else {
				return 0;
			}
		}

		public int getTotalEventCount() {
			return eventCount ;
		}
	}

	public static class TaskCounter {
		private int count = 1;

		public int getCount() {
			return count;
		}

		public void incrementCount() {
			count++;
		}
	}

	public class ModelObserver implements Observer {
		@Override
		public void update(Observable o, Object arg) {
			taskMap.put((ICompositeCheatSheetTask)arg);
		}
	}

	private CompositeCheatSheetModel model;
	private TaskGroup rootTask;
	private TaskGroup group1;
	private TaskGroup group2;
	private EditableTask task1A;
	private EditableTask task1B;
	private EditableTask task1C;
	private EditableTask task2A;
	private EditableTask task2B;
	private EditableTask task3A;
	private TaskMap taskMap;

	private static final int IN_PROGRESS = ICompositeCheatSheetTask.IN_PROGRESS;
	private static final int COMPLETED = ICompositeCheatSheetTask.COMPLETED;
	private static final int SKIPPED = ICompositeCheatSheetTask.SKIPPED;

	/**
	 * Initialize a composite cheatsheet with one root taskGroup, two sub groups and
	 * three tasks in each group
	 */
	protected void createModel(String rootKind, String group1Kind, String group2Kind)  {
		model = new CompositeCheatSheetModel("name", "description", "explorerId");
		rootTask = new TaskGroup(model, "root", "name", rootKind);
		group1 = new TaskGroup(model, "group1", "group1", group1Kind);
		group2 = new TaskGroup(model, "group2", "group2", group1Kind);
		task1A = new EditableTask(model, "task1A", "name", "kind");
		task1B = new EditableTask(model, "task1B", "name", "kind");
		task1C = new EditableTask(model, "task1C", "name", "kind");
		task2A = new EditableTask(model, "task2A", "name", "kind");
		task2B = new EditableTask(model, "task2B", "name", "kind");
		task3A = new EditableTask(model, "task3A", "name", "kind");
		model.setRootTask(rootTask);
		rootTask.addSubtask(group1);
		rootTask.addSubtask(group2);
		rootTask.addSubtask(task3A);
		group1.addSubtask(task1A);
		group1.addSubtask(task1B);
		group1.addSubtask(task1C);
		group2.addSubtask(task2A);
		group2.addSubtask(task2B);
	}

	@Before
	public void setUp() throws Exception {
		resetTaskMap();
	}

	private void resetTaskMap() {
		taskMap = new TaskMap();
	}

	@Test
	public void testStartTask() {
		createModel(ITaskGroup.SET, ITaskGroup.SEQUENCE, ITaskGroup.CHOICE);
		model.addObserver(new ModelObserver());
		task1A.setState(IN_PROGRESS);
		assertEquals(1, taskMap.getEventCount(task1A));
		assertEquals(1, taskMap.getEventCount(rootTask));
		assertEquals(1, taskMap.getEventCount(group1));
		assertEquals(3, taskMap.getTotalEventCount());
	}

	@Test
	public void testStartTwoTasks() {
		createModel(ITaskGroup.SET, ITaskGroup.SEQUENCE, ITaskGroup.CHOICE);
		model.addObserver(new ModelObserver());
		task1A.setState(IN_PROGRESS);
		task1B.setState(IN_PROGRESS);
		assertEquals(1, taskMap.getEventCount(task1A));
		assertEquals(1, taskMap.getEventCount(task1B));
		assertEquals(1, taskMap.getEventCount(rootTask));
		assertEquals(1, taskMap.getEventCount(group1));
		assertEquals(4, taskMap.getTotalEventCount());
	}

	@Test
	public void testCompleteTaskGroup() {
		createModel(ITaskGroup.SET, ITaskGroup.SEQUENCE, ITaskGroup.CHOICE);
		model.addObserver(new ModelObserver());
		task1A.setState(COMPLETED);
		task1B.setState(COMPLETED);
		task1C.setState(COMPLETED);
		assertEquals(1, taskMap.getEventCount(task1A));
		assertEquals(1, taskMap.getEventCount(task1B));
		assertEquals(1, taskMap.getEventCount(task1C));
		assertEquals(1, taskMap.getEventCount(rootTask));
		assertEquals(2, taskMap.getEventCount(group1));
		assertEquals(6, taskMap.getTotalEventCount());
	}

	@Test
	public void testCompleteTaskWithDependent() {
		createModel(ITaskGroup.SET, ITaskGroup.SEQUENCE, ITaskGroup.CHOICE);
		task1B.addRequiredTask(task1A);
		model.addObserver(new ModelObserver());
		task1A.setState(COMPLETED);
		assertEquals(1, taskMap.getEventCount(task1A));
		assertEquals(1, taskMap.getEventCount(task1B));
		assertEquals(1, taskMap.getEventCount(rootTask));
		assertEquals(1, taskMap.getEventCount(group1));
		assertEquals(4, taskMap.getTotalEventCount());
	}

	@Test
	public void testResetSingleTask() {
		createModel(ITaskGroup.SET, ITaskGroup.SEQUENCE, ITaskGroup.CHOICE);
		model.addObserver(new ModelObserver());
		task1A.setState(COMPLETED);
		task1B.setState(COMPLETED);
		resetTaskMap();
		model.resetTasks(new ICompositeCheatSheetTask[]{task1A});
		assertEquals(1, taskMap.getEventCount(task1A));
		assertEquals(1, taskMap.getTotalEventCount());
	}

	@Test
	public void testResetTaskWithDependent() {
		createModel(ITaskGroup.SET, ITaskGroup.SEQUENCE, ITaskGroup.CHOICE);
		model.addObserver(new ModelObserver());
		task1B.addRequiredTask(task1A);
		task1A.setState(COMPLETED);
		resetTaskMap();
		model.resetTasks(new ICompositeCheatSheetTask[]{task1A});
		assertEquals(1, taskMap.getEventCount(task1A));
		assertEquals(1, taskMap.getEventCount(task1B));
	}

	@Test
	public void testResetSingleTaskInCompleteSet() {
		createModel(ITaskGroup.SET, ITaskGroup.SEQUENCE, ITaskGroup.CHOICE);
		model.addObserver(new ModelObserver());
		task1A.setState(COMPLETED);
		task1B.setState(COMPLETED);
		task1C.setState(COMPLETED);
		resetTaskMap();
		model.resetTasks(new ICompositeCheatSheetTask[]{task1A});
		assertEquals(1, taskMap.getEventCount(task1A));
		assertEquals(1, taskMap.getEventCount(group1));
		assertEquals(2, taskMap.getTotalEventCount());
	}

	@Test
	public void testResetTwoTasksInCompleteSet() {
		createModel(ITaskGroup.SET, ITaskGroup.SEQUENCE, ITaskGroup.CHOICE);
		model.addObserver(new ModelObserver());
		task1A.setState(COMPLETED);
		task1B.setState(COMPLETED);
		task1C.setState(COMPLETED);
		resetTaskMap();
		model.resetTasks(new ICompositeCheatSheetTask[]{task1A, task1B});
		assertEquals(1, taskMap.getEventCount(task1A));
		assertEquals(1, taskMap.getEventCount(task1B));
		assertEquals(1, taskMap.getEventCount(group1));
		assertEquals(3, taskMap.getTotalEventCount());
	}

	@Test
	public void testResetAllTasksInCompleteSet() {
		createModel(ITaskGroup.SET, ITaskGroup.SEQUENCE, ITaskGroup.CHOICE);
		model.addObserver(new ModelObserver());
		task1A.setState(COMPLETED);
		task1B.setState(COMPLETED);
		task1C.setState(COMPLETED);
		resetTaskMap();
		model.resetTasks(new ICompositeCheatSheetTask[]{task1A, task1B, task1C});
		assertEquals(1, taskMap.getEventCount(task1A));
		assertEquals(1, taskMap.getEventCount(task1B));
		assertEquals(1, taskMap.getEventCount(task1C));
		assertEquals(1, taskMap.getEventCount(group1));
		assertEquals(1, taskMap.getEventCount(rootTask));
	}

	@Test
	public void testResetSingleTaskInCompleteChoice() {
		createModel(ITaskGroup.SET, ITaskGroup.CHOICE, ITaskGroup.CHOICE);
		model.addObserver(new ModelObserver());
		task1A.setState(COMPLETED);
		resetTaskMap();
		model.resetTasks(new ICompositeCheatSheetTask[]{task1A});
		assertEquals(1, taskMap.getEventCount(task1A));
		assertEquals(1, taskMap.getEventCount(task1B));
		assertEquals(1, taskMap.getEventCount(task1C));
		assertEquals(1, taskMap.getEventCount(group1));
		assertEquals(1, taskMap.getEventCount(rootTask));
	}

	@Test
	public void testResetGroupWithTwoStartedTasks() {
		createModel(ITaskGroup.SET, ITaskGroup.SEQUENCE, ITaskGroup.CHOICE);
		model.addObserver(new ModelObserver());
		task1A.setState(COMPLETED);
		task1B.setState(COMPLETED);
		task3A.setState(IN_PROGRESS);
		resetTaskMap();
		model.resetTasks(new ICompositeCheatSheetTask[]{group1});
		assertEquals(1, taskMap.getEventCount(task1A));
		assertEquals(1, taskMap.getEventCount(task1B));
		assertEquals(1, taskMap.getEventCount(group1));
		assertEquals(4, taskMap.getTotalEventCount());
	}

	@Test
	public void testCompleteChoice() {
		createModel(ITaskGroup.SET, ITaskGroup.CHOICE, ITaskGroup.CHOICE);
		model.addObserver(new ModelObserver());
		task1A.setState(COMPLETED);
		assertEquals(1, taskMap.getEventCount(task1A));
		assertEquals(1, taskMap.getEventCount(task1B));
		assertEquals(1, taskMap.getEventCount(task1C));
		assertEquals(1, taskMap.getEventCount(group1));
		assertEquals(1, taskMap.getEventCount(rootTask));
		assertEquals(5, taskMap.getTotalEventCount());
	}

	@Test
	public void testCompleteGroupWithDependentGroup() {
		createModel(ITaskGroup.SET, ITaskGroup.CHOICE, ITaskGroup.CHOICE);
		group2.addRequiredTask(group1);
		model.addObserver(new ModelObserver());
		task1A.setState(COMPLETED);
		assertEquals(1, taskMap.getEventCount(task1A));
		assertEquals(1, taskMap.getEventCount(task1B));
		assertEquals(1, taskMap.getEventCount(task1C));
		assertEquals(1, taskMap.getEventCount(group1));
		assertEquals(1, taskMap.getEventCount(task2A));
		assertEquals(1, taskMap.getEventCount(task2B));
		assertEquals(1, taskMap.getEventCount(group2));
		assertEquals(1, taskMap.getEventCount(rootTask));
		assertEquals(8, taskMap.getTotalEventCount());
	}

	@Test
	public void testSkipTaskGroup() {
		createModel(ITaskGroup.SET, ITaskGroup.CHOICE, ITaskGroup.CHOICE);
		model.addObserver(new ModelObserver());
		group1.setState(SKIPPED);
		assertEquals(1, taskMap.getEventCount(task1A));
		assertEquals(1, taskMap.getEventCount(task1B));
		assertEquals(1, taskMap.getEventCount(task1C));
		assertEquals(1, taskMap.getEventCount(group1));
		assertEquals(1, taskMap.getEventCount(rootTask));
		assertEquals(5, taskMap.getTotalEventCount());
	}

}
