/*******************************************************************************
 * Copyright (c) 2006, 2019 IBM Corporation and others.
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

package org.eclipse.ui.internal.cheatsheets.composite.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.ui.internal.provisional.cheatsheets.ICompositeCheatSheetTask;
import org.eclipse.ui.internal.provisional.cheatsheets.IEditableTask;
import org.eclipse.ui.internal.provisional.cheatsheets.ITaskGroup;

/**
 * This class contains utility functions to determine the state of a task based on
 * dependencies, parent state etc.
 */

public class TaskStateUtilities {

	/**
	 * Find the most recent ancestor of this task that is blocked
	 * @param task
	 * @return A blocked task or null if no ancestors are blocked
	 */
	public static ICompositeCheatSheetTask findBlockedAncestor(ICompositeCheatSheetTask task) {
		ITaskGroup parent = ((AbstractTask)task).getParent();
		if (parent == null) {
			return null;
		}
		if (!parent.requiredTasksCompleted()) {
			return parent;
		}
		return findBlockedAncestor(parent);
	}

	/**
	 * Find the most recent ancestor of this task that is skipped
	 * @param task
	 * @return A skipped task or null if no ancestors are skipped
	 */
	public static ICompositeCheatSheetTask findSkippedAncestor(ICompositeCheatSheetTask task) {
		ITaskGroup parent = ((AbstractTask)task).getParent();
		if (parent == null) {
			return null;
		}
		if (parent.getState() == ICompositeCheatSheetTask.SKIPPED) {
			return parent;
		}
		return findSkippedAncestor(parent);
	}

	/**
	 * Find the most recent ancestor of this task that is completed
	 * @param task
	 * @return A completed task or null if no ancestors are completed
	 */
	public static ICompositeCheatSheetTask findCompletedAncestor(ICompositeCheatSheetTask task) {
		ITaskGroup parent = ((AbstractTask)task).getParent();
		if (parent == null) {
			return null;
		}
		if (parent.getState() == ICompositeCheatSheetTask.COMPLETED) {
			return parent;
		}
		return findCompletedAncestor(parent);
	}

	/**
	 * Determine whether a task can be skipped.
	 * A task can be skipped if it is skippable, its state is not SKIPPED or completed
	 * and it has no skipped ot completed ancestors.
	 */
	public static boolean isSkipEnabled(ICompositeCheatSheetTask task) {
		if (!task.isSkippable()) return false;
		if (task.getState() == ICompositeCheatSheetTask.COMPLETED) return false;
		if (task.getState() == ICompositeCheatSheetTask.SKIPPED) return false;
		if (findCompletedAncestor(task) != null) return false;
		if (findSkippedAncestor(task) != null) return false;
		return true;
	}

	/**
	 * Determine whether a task can be started
	 * Only editable tasks which are not blocked and whose ancestors
	 * are not completed can be started
	 */
	public static boolean isStartEnabled(ICompositeCheatSheetTask task) {
		if (!(task instanceof IEditableTask)) return false;
		return isStartable(task);
	}

	/**
	 * Determines whether a task is in a state where it has net been started and
	 * cannot be started. This is used to determine when to gray out the icon for a task.
	 */
	public static boolean isBlocked(ICompositeCheatSheetTask task) {
		return (task.getState() == ICompositeCheatSheetTask.NOT_STARTED && !isStartable(task));
	}

	/**
	 * Determines whether an editable task, or a task group has anything
	 * that would prevent it or its children from being started.
	 */
	private static boolean isStartable(ICompositeCheatSheetTask task) {
		if (task.getState() != ICompositeCheatSheetTask.NOT_STARTED) return false;
		if (findSkippedAncestor(task) != null) return false;
		if (findCompletedAncestor(task) != null) return false;
		if (!task.requiredTasksCompleted()) return false;
		if (findBlockedAncestor(task) != null) return false;
		return true;
	}

	/**
	 * Determine which tasks need to be restarted if this tasks is restarted
	 */
	public static AbstractTask[] getRestartTasks(ICompositeCheatSheetTask task) {
		List<ICompositeCheatSheetTask> restartables = new ArrayList<>();
		Set<ICompositeCheatSheetTask> visited = new HashSet<>();
		addRestartableTasks(restartables, task, visited);
		return restartables.toArray(new AbstractTask[restartables.size()]);
	}


	private static void addRestartableTasks(List<ICompositeCheatSheetTask> restartables, ICompositeCheatSheetTask task,
			Set<ICompositeCheatSheetTask> visited) {
		if (visited.contains(task)) {
			return;
		}
		visited.add(task);
		if (task instanceof IEditableTask && task.getState() != ICompositeCheatSheetTask.NOT_STARTED) {
			restartables.add(task);
		} else if (task.getState() == ICompositeCheatSheetTask.SKIPPED){
			restartables.add(task);
		}

		// Add all children
		ICompositeCheatSheetTask[] children = task.getSubtasks();
		for (int i = 0; i < children.length; i++) {
			addRestartableTasks(restartables, children[i], visited);
		}

		// Add all dependents that are started or in progress but not skipped
		ICompositeCheatSheetTask[] successors = ((AbstractTask)task).getSuccessorTasks();
		for (ICompositeCheatSheetTask successor : successors) {
			int state = successor.getState();
			if (state == ICompositeCheatSheetTask.COMPLETED || state == ICompositeCheatSheetTask.IN_PROGRESS) {
				addRestartableTasks(restartables, successor, visited);
			}
		}
	}



}
