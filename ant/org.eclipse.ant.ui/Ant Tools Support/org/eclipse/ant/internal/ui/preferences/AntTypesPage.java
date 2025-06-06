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
import org.eclipse.ant.core.Type;
import org.eclipse.ant.internal.ui.IAntUIHelpContextIds;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

/**
 * Sub-page that allows the user to enter custom types to be used when running Ant build files.
 */
public class AntTypesPage extends AntPage {

	/**
	 * Creates an instance.
	 */
	public AntTypesPage(AntRuntimePreferencePage preferencePage) {
		super(preferencePage);
	}

	@Override
	protected void addButtonsToButtonGroup(Composite parent) {
		createPushButton(parent, AntPreferencesMessages.AntTypesPage_2, ADD_BUTTON);
		editButton = createPushButton(parent, AntPreferencesMessages.AntTypesPage_3, EDIT_BUTTON);
		removeButton = createPushButton(parent, AntPreferencesMessages.AntTypesPage_1, REMOVE_BUTTON);
	}

	/**
	 * Allows the user to enter a custom type.
	 */
	@Override
	protected void add() {
		String title = AntPreferencesMessages.AntTypesPage_addTypeDialogTitle;
		AddCustomDialog dialog = getCustomDialog(title, IAntUIHelpContextIds.ADD_TYPE_DIALOG);
		if (dialog.open() == Window.CANCEL) {
			return;
		}

		Type type = new Type();
		type.setTypeName(dialog.getName());
		type.setClassName(dialog.getClassName());
		type.setLibraryEntry(dialog.getLibraryEntry());
		addContent(type);
	}

	/**
	 * Creates the tab item that contains this sub-page.
	 */
	protected TabItem createTabItem(TabFolder folder) {
		TabItem item = new TabItem(folder, SWT.NONE);
		item.setText(AntPreferencesMessages.AntTypesPage_typesPageTitle);
		item.setImage(AntObjectLabelProvider.getTypeImage());
		item.setData(this);
		Composite top = new Composite(folder, SWT.NONE);
		top.setFont(folder.getFont());
		item.setControl(createContents(top));

		connectToFolder(item, folder);

		return item;
	}

	@Override
	protected void edit(IStructuredSelection selection) {
		Type type = (Type) selection.getFirstElement();
		String title = AntPreferencesMessages.AntTypesPage_editTypeDialogTitle;
		AddCustomDialog dialog = getCustomDialog(title, IAntUIHelpContextIds.EDIT_TYPE_DIALOG);
		dialog.setClassName(type.getClassName());
		dialog.setName(type.getTypeName());
		dialog.setLibraryEntry(type.getLibraryEntry());
		if (dialog.open() == Window.CANCEL) {
			return;
		}

		type.setTypeName(dialog.getName());
		type.setClassName(dialog.getClassName());
		type.setLibraryEntry(dialog.getLibraryEntry());
		updateContent(type);
	}

	private AddCustomDialog getCustomDialog(String title, String helpContext) {
		Iterator<Object> types = getContents(true).iterator();
		List<String> names = new ArrayList<>();
		while (types.hasNext()) {
			Type aTask = (Type) types.next();
			names.add(aTask.getTypeName());
		}

		AddCustomDialog dialog = new AddCustomDialog(getShell(), getPreferencePage().getLibraryEntries(), names, helpContext);
		dialog.setTitle(title);
		dialog.setAlreadyExistsErrorMsg(AntPreferencesMessages.AntTypesPage_8);
		dialog.setNoNameErrorMsg(AntPreferencesMessages.AntTypesPage_9);
		return dialog;
	}

	@Override
	protected void initialize() {
		AntCorePreferences prefs = AntCorePlugin.getPlugin().getPreferences();
		setInput(prefs.getTypes());
	}

	@Override
	protected String getHelpContextId() {
		return IAntUIHelpContextIds.ANT_TYPES_PAGE;
	}
}
