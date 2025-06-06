/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.ant.internal.ui.preferences;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.core.AntCorePreferences;
import org.eclipse.ant.core.Task;
import org.eclipse.ant.internal.ui.IAntUIHelpContextIds;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

/**
 * Sub-page that allows the user to enter custom tasks to be used when running Ant build files.
 */
public class AntTasksPage extends AntPage {

	/**
	 * Creates an instance.
	 */
	public AntTasksPage(AntRuntimePreferencePage preferencePage) {
		super(preferencePage);
	}

	@Override
	protected void addButtonsToButtonGroup(Composite parent) {
		createPushButton(parent, AntPreferencesMessages.AntTasksPage_1, ADD_BUTTON);
		editButton = createPushButton(parent, AntPreferencesMessages.AntTasksPage_2, EDIT_BUTTON);
		removeButton = createPushButton(parent, AntPreferencesMessages.AntTasksPage_3, REMOVE_BUTTON);
	}

	/**
	 * Allows the user to enter a custom task.
	 */
	@Override
	protected void add() {
		String title = AntPreferencesMessages.AntTasksPage_addTaskDialogTitle;
		AddCustomDialog dialog = getCustomDialog(title, IAntUIHelpContextIds.ADD_TASK_DIALOG);
		if (dialog.open() == Window.CANCEL) {
			return;
		}

		Task task = new Task();
		task.setTaskName(dialog.getName());
		task.setClassName(dialog.getClassName());
		task.setLibraryEntry(dialog.getLibraryEntry());
		addContent(task);
	}

	private AddCustomDialog getCustomDialog(String title, String helpContext) {
		Iterator<Object> tasks = getContents(true).iterator();
		List<String> names = new ArrayList<>();
		while (tasks.hasNext()) {
			Task task = (Task) tasks.next();
			names.add(task.getTaskName());
		}
		AddCustomDialog dialog = new AddCustomDialog(getShell(), getPreferencePage().getLibraryEntries(), names, helpContext);
		dialog.setTitle(title);
		dialog.setAlreadyExistsErrorMsg(AntPreferencesMessages.AntTasksPage_8);
		dialog.setNoNameErrorMsg(AntPreferencesMessages.AntTasksPage_9);
		return dialog;
	}

	/**
	 * Creates the tab item that contains this sub-page.
	 */
	protected TabItem createTabItem(TabFolder folder) {
		TabItem item = new TabItem(folder, SWT.NONE);
		item.setText(AntPreferencesMessages.AntTasksPage_title);
		item.setImage(AntObjectLabelProvider.getTaskImage());
		item.setData(this);
		Composite top = new Composite(folder, SWT.NONE);
		top.setFont(folder.getFont());
		item.setControl(createContents(top));

		connectToFolder(item, folder);

		return item;
	}

	@Override
	protected void edit(IStructuredSelection selection) {
		Task task = (Task) selection.getFirstElement();
		String title = AntPreferencesMessages.AntTasksPage_editTaskDialogTitle;
		AddCustomDialog dialog = getCustomDialog(title, IAntUIHelpContextIds.EDIT_TASK_DIALOG);
		dialog.setClassName(task.getClassName());
		dialog.setName(task.getTaskName());
		dialog.setLibraryEntry(task.getLibraryEntry());
		if (dialog.open() == Window.CANCEL) {
			return;
		}

		task.setTaskName(dialog.getName());
		task.setClassName(dialog.getClassName());
		task.setLibraryEntry(dialog.getLibraryEntry());
		updateContent(task);
	}

	@Override
	protected void initialize() {
		AntCorePreferences prefs = AntCorePlugin.getPlugin().getPreferences();
		setInput(prefs.getTasks());
	}

	@Override
	protected String getHelpContextId() {
		return IAntUIHelpContextIds.ANT_TASKS_PAGE;
	}
}
