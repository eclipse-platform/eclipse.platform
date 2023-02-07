/*******************************************************************************
 * Copyright (c) 2005, 2019 IBM Corporation and others.
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
import java.util.List;

import org.eclipse.ui.internal.provisional.cheatsheets.ICompositeCheatSheetTask;
import org.eclipse.ui.internal.provisional.cheatsheets.ITaskGroup;

public class SuccesorTaskFinder {

	private AbstractTask currentTask;
	ICompositeCheatSheetTask bestLaterTask;
	ICompositeCheatSheetTask bestEarlierTask;
	private boolean seenThisTask;

	public SuccesorTaskFinder(ICompositeCheatSheetTask task) {
		currentTask = (AbstractTask)task;
	}

	/**
	 * Find the next recommended task or tasks to be completed.
	 * Algorithm - visualize the tree as having its root at the top,
	 * children below and to the left of their parents and then
	 * search the tree from left to right. Look for
	 * the best predecessor which is the first task to the
	 * left of this task that is runnable and the best successor
	 * which is the first task to the
	 * right of this task which is runnable.
	 * @param task The task which was just completed
	 * @return An array of tasks which can be started
	 */
	public ICompositeCheatSheetTask[] getRecommendedSuccessors()
	{
		// TODO this code could be moved to TaskGroup
		if (ITaskGroup.CHOICE.equals(currentTask.getKind())) {
			// For a choice if more than one child is runnable return it
			List<ICompositeCheatSheetTask> runnableChoices = findRunnableChoices();
			if (runnableChoices.size() != 0) {
				return runnableChoices.toArray(new ICompositeCheatSheetTask[runnableChoices.size()]);
			}
		}
		return getBestSuccessor();
	}

	private List<ICompositeCheatSheetTask> findRunnableChoices() {
		List<ICompositeCheatSheetTask> result = new ArrayList<>();
		if (isStartable(currentTask)) {
			ICompositeCheatSheetTask[] subtasks = currentTask.getSubtasks();
			for (ICompositeCheatSheetTask subtask : subtasks) {
				if (isStartable(subtask)) {
					result.add(subtask);
				}
			}
		}
		return result;
	}

	private boolean isStartable(ICompositeCheatSheetTask task) {
		int state = task.getState();
		return (state != ICompositeCheatSheetTask.COMPLETED &&
				state != ICompositeCheatSheetTask.SKIPPED &&
				task.requiredTasksCompleted());
	}

	private ICompositeCheatSheetTask[] getBestSuccessor() {
		bestLaterTask = null;
		bestEarlierTask = null;
		seenThisTask = false;
		searchRunnableChildren(currentTask.getCompositeCheatSheet().getRootTask());
		// If there is a task which is found later in the tree return
		// that, otherwise an earlier task.
		if (bestLaterTask != null) {
			return new ICompositeCheatSheetTask[] {bestLaterTask};
		}
		if (bestEarlierTask != null) {
			return new ICompositeCheatSheetTask[] {bestEarlierTask};
		}
		return new ICompositeCheatSheetTask[0];
	}

	private void searchRunnableChildren(ICompositeCheatSheetTask task) {
		// Don't search completed tasks or their children
		// and stop searching if we've already found the best successor
		if (bestLaterTask != null) {
			return;
		}
		if (task == currentTask) {
			seenThisTask = true;
		}
		if (task.getState() == ICompositeCheatSheetTask.COMPLETED ||
			task.getState() == ICompositeCheatSheetTask.SKIPPED ) {
			if (isTaskAncestor(task, currentTask)) {
				seenThisTask = true;
			}
			return;
		}

		if ( isStartable(task) && task != currentTask) {
			if (seenThisTask) {
				if (bestLaterTask == null) {
					bestLaterTask = task;
				}
			} else {
				if (bestEarlierTask == null) {
					bestEarlierTask = task;
				}
			}
		}

		ICompositeCheatSheetTask[] subtasks = task.getSubtasks();
		for (ICompositeCheatSheetTask subtask : subtasks) {
			searchRunnableChildren(subtask);
		}

	}

	private boolean isTaskAncestor(ICompositeCheatSheetTask ancestorCandididate, ICompositeCheatSheetTask task) {
		ICompositeCheatSheetTask nextTask = task;
		while (nextTask != null) {
			if (nextTask == ancestorCandididate) {
				return true;
			}
			nextTask = nextTask.getParent();
		}
		return false;
	}
}
