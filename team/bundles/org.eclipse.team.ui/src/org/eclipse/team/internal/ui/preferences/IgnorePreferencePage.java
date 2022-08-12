/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.preferences;


import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.team.core.IIgnoreInfo;
import org.eclipse.team.core.Team;
import org.eclipse.team.internal.ui.IHelpContextIds;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
public class IgnorePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	private Table ignoreTable;
	private Button addButton;
	private Button removeButton;
	@Override
	public void init(IWorkbench workbench) {
		setDescription(TeamUIMessages.IgnorePreferencePage_description);
	}

	/**
	 * Creates preference page controls on demand.
	 *
	 * @param ancestor  the parent for the preference page
	 */
	@Override
	protected Control createContents(Composite ancestor) {

		Composite parent = new Composite(ancestor, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.numColumns = 2;
		parent.setLayout(layout);
		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		parent.setLayoutData(data);

		Label l1 = new Label(parent, SWT.NULL);
		l1.setText(TeamUIMessages.IgnorePreferencePage_ignorePatterns);
		data = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		data.horizontalSpan = 2;
		l1.setLayoutData(data);

		ignoreTable = new Table(parent, SWT.CHECK | SWT.BORDER | SWT.MULTI);
		GridData gd = new GridData(GridData.FILL_BOTH);
		//gd.widthHint = convertWidthInCharsToPixels(30);
		gd.heightHint = 300;
		ignoreTable.setLayoutData(gd);
		ignoreTable.addListener(SWT.Selection, e -> handleSelection());

		Composite buttons = new Composite(parent, SWT.NULL);
		buttons.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		buttons.setLayout(layout);

		addButton = new Button(buttons, SWT.PUSH);
		addButton.setText(TeamUIMessages.IgnorePreferencePage_add);
		addButton.addListener(SWT.Selection, e -> addIgnore());

		removeButton = new Button(buttons, SWT.PUSH);
		removeButton.setText(TeamUIMessages.IgnorePreferencePage_remove);
		removeButton.setEnabled(false);
		removeButton.addListener(SWT.Selection, e -> removeIgnore());
		fillTable(Team.getAllIgnores());
		Dialog.applyDialogFont(ancestor);
		setButtonLayoutData(addButton);
		setButtonLayoutData(removeButton);

		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.IGNORE_PREFERENCE_PAGE);

		return parent;
	}
	/**
	 * Do anything necessary because the OK button has been pressed.
	 *
	 * @return whether it is okay to close the preference page
	 */
	@Override
	public boolean performOk() {
		int count = ignoreTable.getItemCount();
		String[] patterns = new String[count];
		boolean[] enabled = new boolean[count];
		TableItem[] items = ignoreTable.getItems();
		for (int i = 0; i < count; i++) {
			patterns[i] = items[i].getText();
			enabled[i] = items[i].getChecked();
		}
		Team.setAllIgnores(patterns, enabled);
		TeamUIPlugin.broadcastPropertyChange(new PropertyChangeEvent(this, TeamUI.GLOBAL_IGNORES_CHANGED, null, null));
		return true;
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
		ignoreTable.removeAll();
		IIgnoreInfo[] ignore = Team.getDefaultIgnores();
		fillTable(ignore);
	}

	/**
	 * @param ignore
	 */
	private void fillTable(IIgnoreInfo[] ignore) {
		for (IIgnoreInfo info : ignore) {
			TableItem item = new TableItem(ignoreTable, SWT.NONE);
			item.setText(TextProcessor.process(info.getPattern(), ".*")); //$NON-NLS-1$
			item.setChecked(info.getEnabled());
		}
	}

	private void addIgnore() {

		InputDialog dialog = new InputDialog(getShell(), TeamUIMessages.IgnorePreferencePage_enterPatternShort, TeamUIMessages.IgnorePreferencePage_enterPatternLong, null, null) {
			@Override
			protected Control createDialogArea(Composite parent) {
				Control control = super.createDialogArea(parent);
				PlatformUI.getWorkbench().getHelpSystem().setHelp(control, IHelpContextIds.IGNORE_PREFERENCE_PAGE);
				return control;
			}
		};

		dialog.open();
		if (dialog.getReturnCode() != Window.OK) return;
		String pattern = dialog.getValue();
		if (pattern.isEmpty()) return;
		// Check if the item already exists
		TableItem[] items = ignoreTable.getItems();
		for (TableItem item : items) {
			if (item.getText().equals(pattern)) {
				MessageDialog.openWarning(getShell(), TeamUIMessages.IgnorePreferencePage_patternExistsShort, TeamUIMessages.IgnorePreferencePage_patternExistsLong);
				return;
			}
		}
		TableItem item = new TableItem(ignoreTable, SWT.NONE);
		item.setText(TextProcessor.process(pattern, ".*")); //$NON-NLS-1$
		item.setChecked(true);
	}

	private void removeIgnore() {
		int[] selection = ignoreTable.getSelectionIndices();
		ignoreTable.remove(selection);
	}
	private void handleSelection() {
		if (ignoreTable.getSelectionCount() > 0) {
			removeButton.setEnabled(true);
		} else {
			removeButton.setEnabled(false);
		}
	}
}
