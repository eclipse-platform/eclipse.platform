/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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

package org.eclipse.ui.internal.provisional.cheatsheets;

import java.util.Dictionary;

/**
 * A task within a composite cheat sheet.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 */

public interface ICompositeCheatSheetTask {
	/**
	 * The constant that indicates that the task has not been
	 * processed yet.
	 */
	public static final int NOT_STARTED = 0;
	/**
	 * The constant that indicates that the task is in progress.
	 */
	public static final int IN_PROGRESS = 1;
	/**
	 * The constant that indicates that the task has been skipped.
	 */
	public static final int SKIPPED = 2;
	/**
	 * The constant that indicates that the task has been completed.
	 */
	public static final int COMPLETED = 3;
	/**
	 * @return the unique identifier of this task.
	 */
	public String getId();
	/**
	 * @return the translatable name of the task.
	 */
	public String getName();
	/**
	 * Returns the kind of the task editor or task group.
	 * @return task editor kind or <code>null</code> if no editor
	 * is assoticated with this task.
	 */
	public String getKind();
	/**
	 * The task parameters are used to configure the
	 * task editor with data meaningful to an editor of this kind.
	 * @return the parameter names and values as specified in the
	 * composite cheatsheet content file.
	 */
	public Dictionary<String, String> getParameters();
	/**
	 * Returns the description of the task.
	 * @return a plain String, or XML markup that can
	 * be understood by FormText widget.
	 * @see org.eclipse.ui.forms.widgets.FormText
	 */
	public String getDescription();

	/**
	 * Gets the text to be displayed when this task is completed
	 * @return a plain String, or XML markup that can
	 * be understood by FormText widget.
	 * @see org.eclipse.ui.forms.widgets.FormText
	 */
	public String getCompletionMessage();

	/**
	 * Get the subtasks of this task. Each subtask may be
	 * a task group or editable task. If the task is an editable task
	 * there will be no children and an empty array will be returned.
	 * @return an array of subtasks for this task
	 */
	public ICompositeCheatSheetTask [] getSubtasks();

	/**
	 * get the tasks which are required to be completed
	 * before this task is started.
	 * @return an array of tasks that must be completed
	 * before this task can be started.  The array will be
	 * empty if this tasks is independent of other tasks.
	 */
	public ICompositeCheatSheetTask [] getRequiredTasks();

	/**
	 * Determine whether the required tasks for this task have
	 * all been completed.
	 * @return true if there are noi required tasks or all required
	 * tasks have been completed.
	 */
	public boolean requiredTasksCompleted();

	/**
	 * Get the state of this task
	 * @return NOT_STARTED, IN_PROGRESS, SKIPPED or COMPLETED.
	 */
	public int getState();

	/**
	 * Get the enclosing composite cheat sheet
	 * @return the composite cheat sheet which contains this task
	 */
	public ICompositeCheatSheet getCompositeCheatSheet();

	/**
	 * Get the parent task group
	 * @return The task group which contains this task or <code>null</code>
	 * if this is the root of the composite cheat sheet.
	 */
	public ITaskGroup getParent();

	/**
	 * Test whether this task can be skipped. Skippable tasks are optional
	 * tasks which are identified in the content file by having the attribute
	 * <code>skip = "true"</code>. Only skippable tasks can be skipped.
	 * @return true if this task has the skip attribute set to true in the
	 * content file.
	 */
	public boolean isSkippable();

}
