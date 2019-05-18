/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.console;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.*;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.ui.*;

public class ConsolePreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public ConsolePreferencesPage() {
		super(GRID);
		setPreferenceStore(CVSUIPlugin.getPlugin().getPreferenceStore());
	}
	private ColorFieldEditor commandColorEditor;
	private ColorFieldEditor messageColorEditor;
	private ColorFieldEditor errorColorEditor;
	private BooleanFieldEditor showOnMessage;
	private BooleanFieldEditor restrictOutput;
	private BooleanFieldEditor wrap;
	private IntegerFieldEditor highWaterMark;
	private IntegerFieldEditor width;

	@Override
	protected void createFieldEditors() {
		final Composite composite = getFieldEditorParent();
		createLabel(composite, CVSUIMessages.ConsolePreferencesPage_9); 
		IPreferenceStore store = getPreferenceStore();
		
		// ** WRAP
		wrap = new BooleanFieldEditor(ICVSUIConstants.PREF_CONSOLE_WRAP, CVSUIMessages.ConsolePreferencesPage_6, composite); 
		addField(wrap);
		
		width = new IntegerFieldEditor(ICVSUIConstants.PREF_CONSOLE_WIDTH, CVSUIMessages.ConsolePreferencesPage_7, composite); //)
		width.setValidRange(80, Integer.MAX_VALUE - 1);
		addField(width);
		width.setEnabled(store.getBoolean(ICVSUIConstants.PREF_CONSOLE_WRAP), composite);
		
		// ** RESTRICT OUTPUT
		restrictOutput = new BooleanFieldEditor(ICVSUIConstants.PREF_CONSOLE_LIMIT_OUTPUT, CVSUIMessages.ConsolePreferencesPage_5, composite); 
		addField(restrictOutput);
		
		highWaterMark = new IntegerFieldEditor(ICVSUIConstants.PREF_CONSOLE_HIGH_WATER_MARK, CVSUIMessages.ConsolePreferencesPage_8, composite); //)
		highWaterMark.setValidRange(1000, Integer.MAX_VALUE - 1);
		addField(highWaterMark);
		highWaterMark.setEnabled(store.getBoolean(ICVSUIConstants.PREF_CONSOLE_LIMIT_OUTPUT), composite);
		
		// ** SHOW AUTOMATICALLY
		showOnMessage = new BooleanFieldEditor(ICVSUIConstants.PREF_CONSOLE_SHOW_ON_MESSAGE, CVSUIMessages.ConsolePreferencesPage_4, composite); 
		addField(showOnMessage);
		
		createLabel(composite, CVSUIMessages.ConsolePreferencePage_consoleColorSettings); 
		
		//	** COLORS AND FONTS
		commandColorEditor = createColorFieldEditor(ICVSUIConstants.PREF_CONSOLE_COMMAND_COLOR,
			CVSUIMessages.ConsolePreferencePage_commandColor, composite); 
		addField(commandColorEditor);
		
		messageColorEditor = createColorFieldEditor(ICVSUIConstants.PREF_CONSOLE_MESSAGE_COLOR,
			CVSUIMessages.ConsolePreferencePage_messageColor, composite); 
		addField(messageColorEditor);
		
		errorColorEditor = createColorFieldEditor(ICVSUIConstants.PREF_CONSOLE_ERROR_COLOR,
			CVSUIMessages.ConsolePreferencePage_errorColor, composite); 
		addField(errorColorEditor);
		
		Dialog.applyDialogFont(composite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.CONSOLE_PREFERENCE_PAGE);
	}
	
	
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		super.propertyChange(event);
		highWaterMark.setEnabled(restrictOutput.getBooleanValue(), getFieldEditorParent());
		width.setEnabled(wrap.getBooleanValue(), getFieldEditorParent());
	}

	/**
	 * Utility method that creates a label instance
	 * and sets the default layout data.
	 *
	 * @param parent  the parent for the new label
	 * @param text  the text for the new label
	 * @return the new label
	 */
	private Label createLabel(Composite parent, String text) {
		Label label = new Label(parent, SWT.LEFT);
		label.setText(text);
		GridData data = new GridData();
		data.horizontalSpan = 2;
		data.horizontalAlignment = GridData.FILL;
		label.setLayoutData(data);
		return label;
	}
	/**
	 * Creates a new color field editor.
	 */
	private ColorFieldEditor createColorFieldEditor(String preferenceName, String label, Composite parent) {
		ColorFieldEditor editor = new ColorFieldEditor(preferenceName, label, parent);
		editor.setPreferencePage(this);
		editor.setPreferenceStore(getPreferenceStore());
		return editor;
	}

	@Override
	public void init(IWorkbench workbench) {
	}
	
	@Override
	public boolean performOk() {
		CVSUIPlugin.getPlugin().savePluginPreferences();
		return super.performOk();
	}
}
