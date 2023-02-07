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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.cheatsheets.ICheatSheetManager;
import org.eclipse.ui.internal.cheatsheets.CheatSheetPlugin;
import org.eclipse.ui.internal.cheatsheets.composite.parser.ICompositeCheatsheetTags;
import org.eclipse.ui.internal.cheatsheets.data.IParserTags;
import org.eclipse.ui.internal.cheatsheets.state.ICheatSheetStateManager;
import org.eclipse.ui.internal.cheatsheets.state.NoSaveStateManager;
import org.eclipse.ui.internal.cheatsheets.views.CheatSheetManager;
import org.eclipse.ui.internal.provisional.cheatsheets.ICompositeCheatSheetTask;
import org.eclipse.ui.internal.provisional.cheatsheets.IEditableTask;
import org.eclipse.ui.internal.provisional.cheatsheets.TaskEditor;

/**
 * Class to save and restore composite cheatsheet state using a memento
 * There is a child memento for each task which contains keys for the
 * state complete. There is also a grandchild memento for
 * each task that has been started.
 */

public class CompositeCheatSheetSaveHelper {
	private static final String DOT_XML = ".xml"; //$NON-NLS-1$
	private Map<String, IMemento> taskMementoMap;
	private ICheatSheetStateManager stateManager;

	/**
	 * Constructor
	 */
	public CompositeCheatSheetSaveHelper(ICheatSheetStateManager stateManager) {
		super();
		this.stateManager = stateManager;
	}

	public IStatus loadCompositeState(CompositeCheatSheetModel model, Map<String, String> layoutData) {
		if (stateManager instanceof NoSaveStateManager) return Status.OK_STATUS;
		XMLMemento readMemento = CheatSheetPlugin.getPlugin().readMemento(model.getId() + DOT_XML);
		if (readMemento == null) {
			return Status.OK_STATUS;
		}
		taskMementoMap = createTaskMap(readMemento);
		loadTaskState(taskMementoMap, (AbstractTask)model.getRootTask());
		loadCheatsheetManagerData(readMemento, model.getCheatSheetManager());
		loadLayoutData(readMemento, layoutData);
		model.sendTaskChangeEvents();
		return Status.OK_STATUS;
	}

	private Map<String, IMemento> createTaskMap(XMLMemento readMemento) {
		Map<String, IMemento> map = new HashMap<>();
		IMemento[] tasks = readMemento.getChildren(ICompositeCheatsheetTags.TASK);
		for (IMemento task : tasks) {
			String taskId = task.getString(ICompositeCheatsheetTags.TASK_ID);
			if (taskId != null) {
				map.put(taskId, task);
			}
		}
		return map;
	}

	private void loadTaskState(Map<String, IMemento> taskMap, AbstractTask task) {
		ICompositeCheatSheetTask[] children = task.getSubtasks();
		IMemento memento = taskMap.get(task.getId());
		if (memento != null) {
			String state = memento.getString(ICompositeCheatsheetTags.STATE);
			if (state != null) {
				task.setStateNoNotify(Integer.parseInt(state));
			}
		}
		if (task instanceof TaskGroup) {
			for (ICompositeCheatSheetTask element : children) {
				loadTaskState(taskMap, (AbstractTask) element);
			}
			((TaskGroup)task).checkState();
		}
	}

	private void loadCheatsheetManagerData(XMLMemento readMemento, ICheatSheetManager manager) {
		if (manager == null) {
			return;
		}
		IMemento[] children = readMemento.getChildren(ICompositeCheatsheetTags.CHEAT_SHEET_MANAGER);
		for (IMemento childMemento : children) {
			String key = childMemento.getString(ICompositeCheatsheetTags.KEY);
			String value = childMemento.getString(ICompositeCheatsheetTags.VALUE);
			manager.setData(key, value);
		}
	}

	private void loadLayoutData(XMLMemento readMemento, Map<String, String> layoutData) {
		if (layoutData == null) {
			return;
		}
		IMemento[] children = readMemento.getChildren(ICompositeCheatsheetTags.LAYOUT_DATA);
		for (IMemento childMemento : children) {
			String key = childMemento.getString(ICompositeCheatsheetTags.KEY);
			String value = childMemento.getString(ICompositeCheatsheetTags.VALUE);
			layoutData.put(key, value);
		}
	}

	/**
	 * Save the state of a composite cheat sheet model
	 * @param model
	 * @param selectedTask
	 * @param layoutData Will contain pairs of name/value Strings used to save and restore layout
	 * @return
	 */
	public IStatus saveCompositeState(CompositeCheatSheetModel model, Map<String, String> layoutData) {
		if (stateManager instanceof NoSaveStateManager) return Status.OK_STATUS;
		XMLMemento writeMemento = XMLMemento.createWriteRoot(ICompositeCheatsheetTags.COMPOSITE_CHEATSHEET_STATE);
		writeMemento.putString(IParserTags.ID, model.getId());
		saveTaskState(writeMemento, (AbstractTask)model.getRootTask());
		saveCheatSheetManagerData(writeMemento, model.getCheatSheetManager());
		taskMementoMap = createTaskMap(writeMemento);
		if (layoutData != null) {
			saveMap(writeMemento, layoutData, ICompositeCheatsheetTags.LAYOUT_DATA);
		}
		return CheatSheetPlugin.getPlugin().saveMemento(writeMemento, model.getId() + DOT_XML);
	}

	private void saveCheatSheetManagerData(XMLMemento writeMemento, ICheatSheetManager manager) {
		if (!(manager instanceof CheatSheetManager)) {
			return;
		}
		Map<String, String> data = ((CheatSheetManager) manager).getData();
		saveMap(writeMemento, data, ICompositeCheatsheetTags.CHEAT_SHEET_MANAGER);
	}

	private void saveMap(XMLMemento writeMemento, Map<String, String> data, String tag) {
		for (Map.Entry<String, String> entry : data.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			IMemento childMemento = writeMemento.createChild(tag);
			childMemento.putString(ICompositeCheatsheetTags.KEY, key);
			childMemento.putString(ICompositeCheatsheetTags.VALUE, value);
		}
	}

	private void saveTaskState(IMemento writeMemento, AbstractTask task) {
		IMemento childMemento = writeMemento.createChild(ICompositeCheatsheetTags.TASK);
		childMemento.putString(ICompositeCheatsheetTags.TASK_ID, task.getId());
		childMemento.putString(ICompositeCheatsheetTags.STATE, Integer.toString(task.getState()));

		// If this is an editable task that has been started, completed or skipped save the editor state
		if (task instanceof IEditableTask && task.getState() != ICompositeCheatSheetTask.NOT_STARTED) {
			TaskEditor editor = getEditor(task);
			if (editor != null) {
				IMemento taskDataMemento = childMemento.createChild(ICompositeCheatsheetTags.TASK_DATA);
				editor.saveState(taskDataMemento);
			} else {
				// The editor has not been started so save its previously loaded state
				IMemento taskData = getTaskMemento(task.getId());
				if (taskData != null) {
					IMemento previousDataMemento = childMemento.createChild(ICompositeCheatsheetTags.TASK_DATA);
					previousDataMemento.putMemento(taskData);
				}
			}
		}
		ICompositeCheatSheetTask[] subtasks = task.getSubtasks();
		for (int i = 0; i < subtasks.length; i++) {
			saveTaskState(writeMemento, (AbstractTask)subtasks[i]);
		}
	}

	private TaskEditor getEditor(AbstractTask task) {
		if (task instanceof EditableTask) {
			return ((EditableTask)task).getEditor();
		}
		return null;
	}

	public IMemento getTaskMemento(String id) {
		if (taskMementoMap == null) {
			return null;
		}
		IMemento childMemento = taskMementoMap.get(id);
		if (childMemento == null) {
			return  null;
		}
		return childMemento.getChild(ICompositeCheatsheetTags.TASK_DATA);
	}

	public void clearTaskMementos() {
		taskMementoMap = null;
	}

	public void clearTaskMemento(String id) {
		if (taskMementoMap != null) {
			taskMementoMap.remove(id);
		}
	}

}
