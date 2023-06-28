/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.preferences;


import java.text.MessageFormat;

import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsoleConstants;

/**
 * A page to set the preferences for the console
 */
public class ConsolePreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	/**
	 * This class exists to provide visibility to the
	 * <code>refreshValidState</code> method and to perform more intelligent
	 * clearing of the error message.
	 */
	protected class ConsoleIntegerFieldEditor extends IntegerFieldEditor {

		public ConsoleIntegerFieldEditor(String name, String labelText, Composite parent) {
			super(name, labelText, parent);
		}

		/**
		 * @see org.eclipse.jface.preference.FieldEditor#refreshValidState()
		 */
		@Override
		protected void refreshValidState() {
			super.refreshValidState();
		}

		/**
		 * Clears the error message from the message line if the error
		 * message is the error message from this field editor.
		 */
		@Override
		protected void clearErrorMessage() {
			if (canClearErrorMessage()) {
				super.clearErrorMessage();
			}
		}
	}

	private BooleanFieldEditor2 fWrapEditor;
	private ConsoleIntegerFieldEditor fWidthEditor;

	private BooleanFieldEditor2 fUseBufferSize;
	private ConsoleIntegerFieldEditor fBufferSizeEditor;

	private ConsoleIntegerFieldEditor fTabSizeEditor;
	private BooleanFieldEditor autoScrollLockEditor;

	private BooleanFieldEditor2 fWordWrapEditor;

	private BooleanFieldEditor2 fInterpretControlCharactersEditor;
	private BooleanFieldEditor2 fInterpretCrAsControlCharacterEditor;

	/**
	 * Create the console page.
	 */
	public ConsolePreferencePage() {
		super(GRID);
		setDescription(DebugPreferencesMessages.ConsolePreferencePage_Console_settings);
		setPreferenceStore(DebugUIPlugin.getDefault().getPreferenceStore());
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(
			getControl(),
			IDebugHelpContextIds.CONSOLE_PREFERENCE_PAGE );
	}

	/**
	 * Create all field editors for this page
	 */
	@Override
	public void createFieldEditors() {

		fWrapEditor = new BooleanFieldEditor2(IDebugPreferenceConstants.CONSOLE_WRAP, DebugPreferencesMessages.ConsolePreferencePage_Wrap_text_1, SWT.NONE, getFieldEditorParent());
		addField(fWrapEditor);

		fWidthEditor = new ConsoleIntegerFieldEditor(IDebugPreferenceConstants.CONSOLE_WIDTH, DebugPreferencesMessages.ConsolePreferencePage_Console_width, getFieldEditorParent());
		addField(fWidthEditor);
		fWidthEditor.setValidRange(80, 1000);
		fWidthEditor.setErrorMessage(DebugPreferencesMessages.ConsolePreferencePage_console_width);

		fWrapEditor.getChangeControl(getFieldEditorParent()).addSelectionListener(
			new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					updateWidthEditor();
				}
			}
		);

		fUseBufferSize = new BooleanFieldEditor2(IDebugPreferenceConstants.CONSOLE_LIMIT_CONSOLE_OUTPUT, DebugPreferencesMessages.ConsolePreferencePage_Limit_console_output_1, SWT.NONE, getFieldEditorParent());
		addField(fUseBufferSize);

		fBufferSizeEditor = new ConsoleIntegerFieldEditor(IDebugPreferenceConstants.CONSOLE_LOW_WATER_MARK, DebugPreferencesMessages.ConsolePreferencePage_Console_buffer_size__characters___2, getFieldEditorParent());
		addField(fBufferSizeEditor);
		fBufferSizeEditor.setValidRange(1000, Integer.MAX_VALUE - 100000);
		fBufferSizeEditor.setErrorMessage(MessageFormat.format(DebugPreferencesMessages.ConsolePreferencePage_The_console_buffer_size_must_be_at_least_1000_characters__1, Integer.valueOf(Integer.MAX_VALUE - 100000)));

		fUseBufferSize.getChangeControl(getFieldEditorParent()).addSelectionListener(
			new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					updateBufferSizeEditor();
				}
			}
		);

		fTabSizeEditor = new ConsoleIntegerFieldEditor(IDebugPreferenceConstants.CONSOLE_TAB_WIDTH, DebugPreferencesMessages.ConsolePreferencePage_12, getFieldEditorParent());
		addField(fTabSizeEditor);
		fTabSizeEditor.setValidRange(1,100);
		fTabSizeEditor.setErrorMessage(DebugPreferencesMessages.ConsolePreferencePage_13);

		autoScrollLockEditor = new BooleanFieldEditor(IConsoleConstants.P_CONSOLE_AUTO_SCROLL_LOCK, DebugPreferencesMessages.ConsolePreferencePage_Show__Console_View_enable_auto_scroll_lock, SWT.NONE, getFieldEditorParent());
		addField(autoScrollLockEditor);

		fWordWrapEditor = new BooleanFieldEditor2(IConsoleConstants.P_CONSOLE_WORD_WRAP,
				DebugPreferencesMessages.ConsolePreferencePage_Enable_Word_Wrap_text, SWT.NONE, getFieldEditorParent());
		addField(fWordWrapEditor);

		addField(new BooleanFieldEditor(IDebugPreferenceConstants.CONSOLE_OPEN_ON_OUT, DebugPreferencesMessages.ConsolePreferencePage_Show__Console_View_when_there_is_program_output_3, SWT.NONE, getFieldEditorParent()));
		addField(new BooleanFieldEditor(IDebugPreferenceConstants.CONSOLE_OPEN_ON_ERR, DebugPreferencesMessages.ConsolePreferencePage_Show__Console_View_when_there_is_program_error_3, SWT.NONE, getFieldEditorParent()));

		ColorFieldEditor sysout= new ColorFieldEditor(IDebugPreferenceConstants.CONSOLE_SYS_OUT_COLOR, DebugPreferencesMessages.ConsolePreferencePage_Standard_Out__2, getFieldEditorParent());
		ColorFieldEditor syserr= new ColorFieldEditor(IDebugPreferenceConstants.CONSOLE_SYS_ERR_COLOR, DebugPreferencesMessages.ConsolePreferencePage_Standard_Error__3, getFieldEditorParent());
		ColorFieldEditor sysin= new ColorFieldEditor(IDebugPreferenceConstants.CONSOLE_SYS_IN_COLOR, DebugPreferencesMessages.ConsolePreferencePage_Standard_In__4, getFieldEditorParent());
		ColorFieldEditor background= new ColorFieldEditor(IDebugPreferenceConstants.CONSOLE_BAKGROUND_COLOR, DebugPreferencesMessages.ConsolePreferencePage_11, getFieldEditorParent());

		addField(sysout);
		addField(syserr);
		addField(sysin);
		addField(background);

		fInterpretControlCharactersEditor = new BooleanFieldEditor2(IDebugPreferenceConstants.CONSOLE_INTERPRET_CONTROL_CHARACTERS, DebugPreferencesMessages.ConsolePreferencePage_Interpret_control_characters, SWT.NONE, getFieldEditorParent());
		fInterpretCrAsControlCharacterEditor = new BooleanFieldEditor2(IDebugPreferenceConstants.CONSOLE_INTERPRET_CR_AS_CONTROL_CHARACTER, DebugPreferencesMessages.ConsolePreferencePage_Interpret_cr_as_control_character, SWT.NONE, getFieldEditorParent());
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent = 20;
		fInterpretCrAsControlCharacterEditor.getChangeControl(getFieldEditorParent()).setLayoutData(gd);

		fInterpretControlCharactersEditor.getChangeControl(getFieldEditorParent()).addListener(SWT.Selection,
				event -> updateInterpretCrAsControlCharacterEditor());

		addField(fInterpretControlCharactersEditor);
		addField(fInterpretCrAsControlCharacterEditor);
	}

	/**
	 * @see IWorkbenchPreferencePage#init(IWorkbench)
	 */
	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	public boolean performOk() {
		boolean ok= super.performOk();
		// update high water mark to be (about) 100 lines (100 * 80 chars) greater than low water mark
		IPreferenceStore store = DebugUIPlugin.getDefault().getPreferenceStore();
		int low = store.getInt(IDebugPreferenceConstants.CONSOLE_LOW_WATER_MARK);
		int high = low + 8000;
		store.setValue(IDebugPreferenceConstants.CONSOLE_HIGH_WATER_MARK, high);
		return ok;
	}

	/**
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#initialize()
	 */
	@Override
	protected void initialize() {
		super.initialize();
		updateWidthEditor();
		updateAutoScrollLockEditor();
		updateBufferSizeEditor();
		updateInterpretCrAsControlCharacterEditor();
		updateWordWrapEditorFromConsolePreferences();
	}

	/**
	 * Because the autoscroll value is in another plugin we must update the preference store manually
	 */
	protected void updateAutoScrollLockEditor() {
		autoScrollLockEditor.setPreferenceStore(ConsolePlugin.getDefault().getPreferenceStore());
		autoScrollLockEditor.load();
	}

	/**
	 * Update enablement of width editor based on enablement of 'fixed width' editor.
	 */
	protected void updateWidthEditor() {
		Button b = fWrapEditor.getChangeControl(getFieldEditorParent());
		fWidthEditor.getTextControl(getFieldEditorParent()).setEnabled(b.getSelection());
		fWidthEditor.getLabelControl(getFieldEditorParent()).setEnabled(b.getSelection());
	}

	/**
	 * Update enablement of buffer size editor based on enablement of 'limit
	 * console output' editor.
	 */
	protected void updateBufferSizeEditor() {
		Button b = fUseBufferSize.getChangeControl(getFieldEditorParent());
		fBufferSizeEditor.getTextControl(getFieldEditorParent()).setEnabled(b.getSelection());
		fBufferSizeEditor.getLabelControl(getFieldEditorParent()).setEnabled(b.getSelection());
	}

	/**
	 * Update enablement of carriage return interpretation based on general control
	 * character interpretation.
	 */
	protected void updateInterpretCrAsControlCharacterEditor() {
		Button b = fInterpretControlCharactersEditor.getChangeControl(getFieldEditorParent());
		fInterpretCrAsControlCharacterEditor.getChangeControl(getFieldEditorParent()).setEnabled(b.getSelection());
	}

	/**
	 * Update enablement of word wrapping from Console plugin preference store.
	 */
	protected void updateWordWrapEditorFromConsolePreferences() {
		fWordWrapEditor.setPreferenceStore(ConsolePlugin.getDefault().getPreferenceStore());
		fWordWrapEditor.load();
	}

	/**
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	@Override
	protected void performDefaults() {
		super.performDefaults();
		updateWidthEditor();
		updateBufferSizeEditor();
		updateInterpretCrAsControlCharacterEditor();
	}

	protected boolean canClearErrorMessage() {
		return fWidthEditor.isValid() && fBufferSizeEditor.isValid() && fTabSizeEditor.isValid();
	}

	/**
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	@Override
	public void propertyChange(PropertyChangeEvent event) {

		if (event.getProperty().equals(FieldEditor.IS_VALID)) {
			boolean newValue = ((Boolean) event.getNewValue()).booleanValue();
			// If the new value is true then we must check all field editors.
			// If it is false, then the page is invalid in any case.
			if (newValue) {
				if (fWidthEditor != null && event.getSource() != fWidthEditor) {
					fWidthEditor.refreshValidState();
				}
				if (fBufferSizeEditor != null && event.getSource() != fBufferSizeEditor) {
					fBufferSizeEditor.refreshValidState();
				}
				if (fTabSizeEditor != null && event.getSource() != fTabSizeEditor) {
					fTabSizeEditor.refreshValidState();
				}
				checkState();
			} else {
				super.propertyChange(event);
			}

		} else {
			super.propertyChange(event);
		}
	}
}
