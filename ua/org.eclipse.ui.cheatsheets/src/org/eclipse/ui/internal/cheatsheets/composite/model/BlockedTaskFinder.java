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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.ui.internal.provisional.cheatsheets.ICompositeCheatSheetTask;

public class BlockedTaskFinder {

	private Set<ICompositeCheatSheetTask> stateChangedTasks;
	private Set<ICompositeCheatSheetTask> impactedTasks;

	/**
	 * Find which tasks have either become blocked or unblocked so that they can
	 * be added to the list of change events.
	 *
	 * @param stateChangedTasks
	 *            The set of tasks which has changed
	 * @return The set of tasks which have become blocked or unblocked by the
	 *         change of state and were not in the original set. The algorithm
	 *         will sometimes add tasks to the result set which were not
	 *         actually impacted but this is not a major problem since it only
	 *         means that extra events get sent to the explorer. For updates
	 *         other than resets the number of extra events is very low.
	 *         <p>
	 *         This takes several steps.
	 *         <ul>
	 *         <li>If a group is completed, skipped or reset add any non-started
	 *         children.
	 *         <li>Determine all successors of tasks whose state has changed
	 *         that are not in the change set
	 *         <li>Add the successor and its children to the list if not started
	 *         </ul>
	 */
	public Set<ICompositeCheatSheetTask> findBlockedTaskChanges(Set<ICompositeCheatSheetTask> stateChangedTasks) {
		this.stateChangedTasks = stateChangedTasks;
		impactedTasks = new HashSet<>();
		visitChangedTasks();
		findSuccesors();
		return impactedTasks;
	}

	private void visitChangedTasks() {
		for (ICompositeCheatSheetTask nextTask : stateChangedTasks) {
			if (nextTask.getState() != ICompositeCheatSheetTask.IN_PROGRESS) {
				findUnstartedChildren(nextTask);
			}
		}
	}

	/*
	 * Look for children which we have not seen elsewhere and if they are not started
	 * add them to the list of impacted tasks.
	 */
	private void findUnstartedChildren(ICompositeCheatSheetTask task) {
		ICompositeCheatSheetTask[] children = task.getSubtasks();
		for (ICompositeCheatSheetTask nextChild : children) {
			// Ignore if this task has been seen before
			if ((!stateChangedTasks.contains(nextChild)) && !impactedTasks.contains(nextChild)) {
				if (nextChild.getState() == ICompositeCheatSheetTask.NOT_STARTED) {
					impactedTasks.add(nextChild);
				}
				findUnstartedChildren(nextChild);
			}
		}
	}

	private void findSuccesors() {
		for (ICompositeCheatSheetTask iCompositeCheatSheetTask : stateChangedTasks) {
			final AbstractTask nextTask = (AbstractTask) iCompositeCheatSheetTask;
			ICompositeCheatSheetTask[] successors = nextTask.getSuccessorTasks();
			for (ICompositeCheatSheetTask nextSuccessor : successors) {
				if (nextSuccessor.getState() == ICompositeCheatSheetTask.NOT_STARTED) {
					impactedTasks.add(nextSuccessor);
				}
				findUnstartedChildren(nextSuccessor);
			}
		}
	}

}
