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

import org.eclipse.ui.internal.cheatsheets.composite.model.CompositeCheatSheetModel;
import org.eclipse.ui.internal.cheatsheets.composite.model.EditableTask;
import org.eclipse.ui.internal.cheatsheets.composite.model.SuccesorTaskFinder;
import org.eclipse.ui.internal.cheatsheets.composite.model.TaskGroup;
import org.eclipse.ui.internal.provisional.cheatsheets.ICompositeCheatSheetTask;
import org.junit.Test;

/**
 * Tests the computation of task successors, which will be shown
 * as "Go To" links after a task is complete or for a task group.
 * All tasks use the same base model
 */

public class TestSuccessors {

	private CompositeCheatSheetModel model;
	private TaskGroup rootTask;
	private TaskGroup subGroup;
	private EditableTask task1;
	private EditableTask task2;
	private EditableTask task3;
	private EditableTask task4;

	/**
	 * Initialize a composite cheatsheet whose root task is the parent of
	 * subGroup and task4. subGroup is the parent of task1, task2 and task3
	 */
	private void setupModel(boolean subGroupIsChoice) {
		model = new CompositeCheatSheetModel("name", "description", "explorerId");
		model.setId("org.eclipse.ua.tests.testPersistence");
		rootTask = new TaskGroup(model, "root", "name", "set");
		String subgroupKind = "set";
		if (subGroupIsChoice) {
			subgroupKind = "choice";
		}
		subGroup = new TaskGroup(model, "subGroup", "name", subgroupKind);
		task1 = new EditableTask(model, "task1", "name", "ua.junit");
		task2 = new EditableTask(model, "task2", "name", "ua.junit");
		task3 = new EditableTask(model, "task3", "name", "ua.junit");
		task4 = new EditableTask(model, "task4", "name", "ua.junit");
		model.setRootTask(rootTask);
		rootTask.addSubtask(subGroup);
		rootTask.addSubtask(task4);
		subGroup.addSubtask(task1);
		subGroup.addSubtask(task2);
		subGroup.addSubtask(task3);
	}

	/*
	 * Assert that a task has one successor and it matches the expected value
	 */
	private void assertSingleSuccessor(ICompositeCheatSheetTask task, ICompositeCheatSheetTask expectedSuccessor) {
		SuccesorTaskFinder finder = new SuccesorTaskFinder(task);
		ICompositeCheatSheetTask[] successors = finder.getRecommendedSuccessors();
		assertEquals(1, successors.length);
		assertEquals(expectedSuccessor, successors[0]);
	}

	private void assertNoSuccessors(ICompositeCheatSheetTask task) {
		SuccesorTaskFinder finder = new SuccesorTaskFinder(task);
		ICompositeCheatSheetTask[] successors = finder.getRecommendedSuccessors();
		assertEquals(0, successors.length);
	}

	@Test
	public void testSuccessorsCleanModel() {
		setupModel(false);
		assertSingleSuccessor(rootTask, subGroup);
		assertSingleSuccessor(subGroup, task1);
	}

	@Test
	public void testSuccessorsFirstTaskInProgress() {
		setupModel(false);
		task1.setState(ICompositeCheatSheetTask.IN_PROGRESS);
		assertSingleSuccessor(rootTask, subGroup);
		assertSingleSuccessor(subGroup, task1);
	}

	@Test
	public void testSuccessorsFirstTaskSkipped() {
		setupModel(false);
		task1.setState(ICompositeCheatSheetTask.SKIPPED);
		assertSingleSuccessor(rootTask, subGroup);
		assertSingleSuccessor(subGroup, task2);
		assertSingleSuccessor(task1, task2);
	}

	@Test
	public void testSuccessorsFirstTaskCompleted() {
		setupModel(false);
		task1.complete();
		assertSingleSuccessor(rootTask, subGroup);
		assertSingleSuccessor(subGroup, task2);
		assertSingleSuccessor(task1, task2);
	}

	@Test
	public void testSuccessorsWithBackwardDependency() {
		setupModel(false);
		task1.addRequiredTask(task3);
		assertSingleSuccessor(subGroup, task2);
		task2.complete();
		assertSingleSuccessor(subGroup, task3);
		assertSingleSuccessor(task2, task3);
	}

	@Test
	public void testCompleteGroup() {
		setupModel(false);
		task1.complete();
		task2.complete();
		task3.complete();
		assertEquals(ICompositeCheatSheetTask.COMPLETED, subGroup.getState());
		assertSingleSuccessor(rootTask, task4);
		assertSingleSuccessor(subGroup, task4);
		assertSingleSuccessor(task1, task4);
		assertSingleSuccessor(task2, task4);
		assertSingleSuccessor(task3, task4);
	}

	@Test
	public void testAllTasksComplete() {
		setupModel(false);
		task1.complete();
		task2.complete();
		task3.complete();
		task4.complete();
		assertNoSuccessors(rootTask);
		assertNoSuccessors(subGroup);
		assertNoSuccessors(task1);
		assertNoSuccessors(task4);
	}

	@Test
	public void testUnstartedChoice() {
		setupModel(true);
		SuccesorTaskFinder finder = new SuccesorTaskFinder(subGroup);
		ICompositeCheatSheetTask[] successors = finder.getRecommendedSuccessors();
		assertEquals(3, successors.length);
		// The successors should be in task order
		assertEquals(task1, successors[0]);
		assertEquals(task2, successors[1]);
		assertEquals(task3, successors[2]);
	}

	@Test
	public void testCompletedChoice() {
		setupModel(true);
		task1.complete();
		assertSingleSuccessor(rootTask, task4);
		assertSingleSuccessor(subGroup, task4);
		assertSingleSuccessor(task1, task4);
		assertSingleSuccessor(task2, task4);
		assertSingleSuccessor(task3, task4);
	}

	@Test
	public void testSkippedGroup() {
		setupModel(false);
		task1.complete();
		subGroup.setSkippable(true);
		subGroup.setState(ICompositeCheatSheetTask.SKIPPED);
		assertSingleSuccessor(rootTask, task4);
		assertSingleSuccessor(subGroup, task4);
		assertSingleSuccessor(task1, task4);
		assertSingleSuccessor(task2, task4);
		assertSingleSuccessor(task3, task4);
	}


}
