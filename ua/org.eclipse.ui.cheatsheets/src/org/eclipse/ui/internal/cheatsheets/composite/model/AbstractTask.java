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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.internal.cheatsheets.composite.parser.ITaskParseStrategy;
import org.eclipse.ui.internal.provisional.cheatsheets.ICompositeCheatSheet;
import org.eclipse.ui.internal.provisional.cheatsheets.ICompositeCheatSheetTask;
import org.eclipse.ui.internal.provisional.cheatsheets.ITaskGroup;
import org.osgi.framework.Bundle;

/**
 * A single task within a composite cheatsheet. This class encapsulates the
 * behavior common to editable tasks and taskGroups
 */

public abstract class AbstractTask implements ICompositeCheatSheetTask {
	protected CompositeCheatSheetModel model;
	protected int state = NOT_STARTED;
	private String id;

	private String name;

	protected String kind;

	private Dictionary<String, String> parameters;

	private String description;

	private String completionMessage;

	private ArrayList<ICompositeCheatSheetTask> requiredTasks;

	private ArrayList<ICompositeCheatSheetTask> successorTasks;

	private boolean skippable;

	private TaskGroup parent;

	protected static final ICompositeCheatSheetTask[] EMPTY = new ICompositeCheatSheetTask[0];

	public AbstractTask(CompositeCheatSheetModel model, String id, String name, String kind) {
		this.model = model;
		this.id = id;
		this.name = name;
		this.kind = kind;
		this.parameters = new Hashtable<>();
		this.description = ""; //$NON-NLS-1$
		requiredTasks = new ArrayList<>();
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getKind() {
		return kind;
	}

	@Override
	public Dictionary<String, String> getParameters() {
		return parameters;
	}

	@Override
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setCompletionMessage(String completionMessage) {
		this.completionMessage = completionMessage;
	}

	@Override
	public String getCompletionMessage() {
		return completionMessage;
	}

	@Override
	public ICompositeCheatSheetTask[] getRequiredTasks() {
		if (requiredTasks==null) return EMPTY;
		return requiredTasks.toArray(new ICompositeCheatSheetTask[requiredTasks.size()]);
	}

	public ICompositeCheatSheetTask[] getSuccessorTasks() {
		if (successorTasks==null) return EMPTY;
		return successorTasks.toArray(new ICompositeCheatSheetTask[successorTasks.size()]);
	}

	public void addRequiredTask(AbstractTask task) {
		if (requiredTasks==null)
			requiredTasks = new ArrayList<>();
		requiredTasks.add(task);
		if (task.successorTasks==null)
			task.successorTasks = new ArrayList<>();
		task.successorTasks.add(this);
	}

	@Override
	public int getState() {
		return state;
	}

	public void complete() {
		setState(COMPLETED);
	}

	@Override
	public boolean requiredTasksCompleted() {
		boolean startable = true;
		ICompositeCheatSheetTask[] requiredTasks = getRequiredTasks();
		for (ICompositeCheatSheetTask requiredTask : requiredTasks) {
			if (requiredTask.getState() != COMPLETED && requiredTask.getState() != SKIPPED) {
				startable = false;
			}
		}
		return startable;
	}

	/**
	 * Determine whether the candidate task is a required task for this task.
	 * This function does not test for indirectly required tasks
	 * @param candidateTask a task which may be a required task
	 * @return true if candidateTask is in the list of required tasks.
	 */
	public boolean requiresTask(ICompositeCheatSheetTask candidateTask) {
		return (requiredTasks.contains(candidateTask));
	}

	/**
	 * Interface used when restoring state from a file.
	 * Not intended to be called from task editors.
	 * @param state
	 */
	public void setState(int state) {
		setStateNoNotify(state);
		model.sendTaskChangeEvents();
	}

	/**
	 * Set the state of a task but don't send out any events yet,
	 * let them collect so we don't send out multiple events for
	 * one task
	 * @param state
	 */
	public void setStateNoNotify(int state) {
		this.state = state;
		if (parent != null) {
			parent.checkState();
		}
		model.stateChanged(this);
	}

	/*
	 * Resolves the given path to a URL. The path can either be fully qualified with
	 * the plugin id, e.g. "/plugin_id/path/file.xml" or relative to the composite cheat
	 * sheet file, e.g. "tasks/task1.xml".
	 */
	public URL getInputUrl(String path) throws MalformedURLException {
		int index = path.indexOf('/', 1);
		if (index >= 1 && path.charAt(0) == '/') {
			String bundleName = path.substring(1, index);
			String relativePath = path.substring(index + 1);
			Bundle bundle = Platform.getBundle(bundleName);
			if (bundle != null) {
				return FileLocator.find(bundle, IPath.fromOSString(relativePath), null);
			}
		}
		return new URL(model.getContentUrl(), path);

	}

	@Override
	public ICompositeCheatSheet getCompositeCheatSheet() {
		return model;
	}

	public abstract ITaskParseStrategy getParserStrategy();

	@Override
	public abstract ICompositeCheatSheetTask[] getSubtasks();

	public void setSkippable(boolean skippable) {
		this.skippable = skippable;
	}

	@Override
	public boolean isSkippable() {
		return skippable;
	}

	protected void setParent(TaskGroup parent) {
		this.parent = parent;
	}

	@Override
	public ITaskGroup getParent() {
		return parent;
	}

	@Override
	public int hashCode() {
		return getId().hashCode();
	}

}
