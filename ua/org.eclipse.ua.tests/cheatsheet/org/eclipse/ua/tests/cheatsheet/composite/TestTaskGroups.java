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
import org.eclipse.ui.internal.cheatsheets.composite.model.TaskGroup;
import org.eclipse.ui.internal.provisional.cheatsheets.ICompositeCheatSheetTask;
import org.eclipse.ui.internal.provisional.cheatsheets.ITaskGroup;
import org.junit.Test;

/**
 * Test that the state of a task group is determined by the state of its children.
 * Each test creates a task group and sets the state of its children then tests the
 * state of the parent task group.
 */

public class TestTaskGroups {

	private static final int NOT_STARTED = ICompositeCheatSheetTask.NOT_STARTED;
	private static final int IN_PROGRESS = ICompositeCheatSheetTask.IN_PROGRESS;
	private static final int SKIPPED = ICompositeCheatSheetTask.SKIPPED;
	private static final int COMPLETED = ICompositeCheatSheetTask.COMPLETED;

	private int getGroupState(String kind, int[] childStates) {
		CompositeCheatSheetModel model = new CompositeCheatSheetModel("name", "desc", null);
		TaskGroup group = new TaskGroup(model, "id", "name", kind);
		for (int i = 0; i < childStates.length; i++) {
			EditableTask editable = new EditableTask(model, "id" + i, "name" + i, "editableKind");
			group.addSubtask(editable);
			editable.setState(childStates[i]);
		}
		return group.computeState();
	}

	@Test
	public void testEmptySet() {
		assertEquals(COMPLETED, getGroupState(ITaskGroup.SET, new int[0]));
	}

	@Test
	public void testSetNotStarted() {
		assertEquals(NOT_STARTED, getGroupState(ITaskGroup.SET, new int[] {NOT_STARTED, NOT_STARTED}));
	}

	@Test
	public void testSetInProgress() {
		assertEquals(IN_PROGRESS, getGroupState(ITaskGroup.SET, new int[] {IN_PROGRESS}));
	}

	@Test
	public void testSetPartiallyComplete() {
		assertEquals(IN_PROGRESS, getGroupState(ITaskGroup.SET, new int[] {COMPLETED, NOT_STARTED}));
	}

	@Test
	public void testSetCompleted() {
		assertEquals(COMPLETED, getGroupState(ITaskGroup.SET, new int[] {COMPLETED, SKIPPED}));
	}

	@Test
	public void testEmptySequence() {
		assertEquals(COMPLETED, getGroupState(ITaskGroup.SEQUENCE, new int[0]));
	}

	@Test
	public void testSequenceNotStarted() {
		assertEquals(NOT_STARTED, getGroupState(ITaskGroup.SEQUENCE, new int[] {NOT_STARTED, NOT_STARTED}));
	}

	@Test
	public void testSequenceInProgress() {
		assertEquals(IN_PROGRESS, getGroupState(ITaskGroup.SEQUENCE, new int[] {IN_PROGRESS}));
	}

	@Test
	public void testSequencePartiallyComplete() {
		assertEquals(IN_PROGRESS, getGroupState(ITaskGroup.SEQUENCE, new int[] {COMPLETED, NOT_STARTED}));
	}

	@Test
	public void testSequenceCompleted() {
		assertEquals(COMPLETED, getGroupState(ITaskGroup.SEQUENCE, new int[] {COMPLETED, SKIPPED}));
	}

	@Test
	public void testSequenceSkipped() {
		assertEquals(COMPLETED, getGroupState(ITaskGroup.SEQUENCE, new int[] {SKIPPED, SKIPPED}));
	}

	@Test
	public void testEmptyChoice() {
		assertEquals(COMPLETED, getGroupState(ITaskGroup.CHOICE, new int[0]));
	}

	@Test
	public void testChoiceNotStarted() {
		assertEquals(NOT_STARTED, getGroupState(ITaskGroup.CHOICE, new int[] {NOT_STARTED, NOT_STARTED}));
	}

	@Test
	public void testSingleChoiceNotStarted() {
		assertEquals(NOT_STARTED, getGroupState(ITaskGroup.CHOICE, new int[] {NOT_STARTED}));
	}

	@Test
	public void testChoiceInProgress() {
		assertEquals(IN_PROGRESS, getGroupState(ITaskGroup.CHOICE, new int[] {IN_PROGRESS}));
	}

	@Test
	public void testChoicePartiallyComplete() {
		assertEquals(COMPLETED, getGroupState(ITaskGroup.CHOICE, new int[] {COMPLETED, NOT_STARTED}));
	}

	@Test
	public void testChoiceCompleted() {
		assertEquals(COMPLETED, getGroupState(ITaskGroup.CHOICE, new int[] {COMPLETED, SKIPPED}));
	}

	@Test
	public void testSingleChoiceCompleted() {
		assertEquals(COMPLETED, getGroupState(ITaskGroup.CHOICE, new int[] {COMPLETED}));
	}

	@Test
	public void testChoiceSkipped() {
		assertEquals(COMPLETED, getGroupState(ITaskGroup.CHOICE, new int[] {NOT_STARTED, SKIPPED}));
	}

}
