/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.actions.variables;

import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.FrameworkUtil;

/**
 * A simple input dialog for soliciting an input string
 * from the user.
 * <p>
 * This concrete dialog class can be instantiated as is,
 * or further subclassed as required.
 * </p>
 */
public class ChangeVariableValueInputDialog extends TrayDialog {


	/**
	 * The title of the dialog.
	 */
	private String title;

	/**
	 * The message to display, or <code>null</code> if none.
	 */
	private String message;

	/**
	 * The input value; the empty string by default.
	 */
	private String value= IInternalDebugCoreConstants.EMPTY_STRING;

	/**
	 * The input validator, or <code>null</code> if none.
	 */
	private IInputValidator validator;

	/**
	 * Ok button widget.
	 */
	private Button okButton;

	/**
	 * Input text widget.
	 */
	private Text text;

	/**
	 * Error message label widget.
	 */
	private Label errorMessageLabel;
	/**
	 * Creates an input dialog with OK and Cancel buttons.
	 * Note that the dialog will have no visual representation (no widgets)
	 * until it is told to open.
	 * <p>
	 * Note that the <code>open</code> method blocks for input dialogs.
	 * </p>
	 *
	 * @param parentShell the parent shell
	 * @param dialogTitle the dialog title, or <code>null</code> if none
	 * @param dialogMessage the dialog message, or <code>null</code> if none
	 * @param initialValue the initial input value, or <code>null</code> if none
	 *  (equivalent to the empty string)
	 * @param validator an input validator, or <code>null</code> if none
	 */
	public ChangeVariableValueInputDialog(Shell parentShell, String dialogTitle, String dialogMessage, String initialValue, IInputValidator validator) {
		super(parentShell);
		this.title = dialogTitle;
		message = dialogMessage;
		if (initialValue == null) {
			value = IInternalDebugCoreConstants.EMPTY_STRING;
		} else {
			value = initialValue;
		}
		this.validator = validator;

		setShellStyle(getShellStyle() | SWT.RESIZE);
	}
	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID) {
			value= text.getText();
		} else {
			value= null;
		}
		super.buttonPressed(buttonId);
	}
	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		if (title != null) {
			shell.setText(title);
		}
	}
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);

		//do this here because setting the text will set enablement on the ok button
		text.setFocus();
		if (value != null) {
			text.setText(value);
			text.selectAll();
		}
	}
	@Override
	protected Control createDialogArea(Composite parent) {
		Font font = parent.getFont();
		// create composite
		Composite composite = (Composite)super.createDialogArea(parent);

		// create message
		if (message != null) {
			Label label = new Label(composite, SWT.WRAP);
			label.setText(message);
			GridData data = new GridData(
					GridData.GRAB_HORIZONTAL |
					GridData.HORIZONTAL_ALIGN_FILL |
					GridData.VERTICAL_ALIGN_CENTER);
			data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
			label.setLayoutData(data);
			label.setFont(font);
		}

		text= new Text(composite, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL| SWT.H_SCROLL);

		GridData gridData= new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_VERTICAL | GridData.VERTICAL_ALIGN_FILL);
		gridData.heightHint = 50;
		gridData.widthHint = 100;
		text.setLayoutData(gridData);
		text.setFont(font);
		text.addModifyListener(
				e -> {
					if (okButton.isEnabled()) {
						return;
					}
					errorMessageLabel.setText(IInternalDebugCoreConstants.EMPTY_STRING);
					errorMessageLabel.getParent().update();
					okButton.setEnabled(true);
				}
		);

		errorMessageLabel = new Label(composite, SWT.NONE);
		errorMessageLabel.setLayoutData(new GridData(
				GridData.GRAB_HORIZONTAL |
				GridData.HORIZONTAL_ALIGN_FILL));
		errorMessageLabel.setFont(font);
		return composite;
	}
	/**
	 * Returns the error message label.
	 *
	 * @return the error message label
	 */
	protected Label getErrorMessageLabel() {
		return errorMessageLabel;
	}
	/**
	 * Returns the ok button.
	 *
	 * @return the ok button
	 */
	protected Button getOkButton() {
		return okButton;
	}
	/**
	 * Returns the text area.
	 *
	 * @return the text area
	 */
	protected Text getText() {
		return text;
	}
	/**
	 * Returns the validator.
	 *
	 * @return the validator
	 */
	protected IInputValidator getValidator() {
		return validator;
	}
	/**
	 * Returns the string typed into this input dialog.
	 *
	 * @return the input string
	 */
	public String getValue() {
		return value;
	}
	/**
	 * Validates the input.
	 * <p>
	 * The default implementation of this framework method
	 * delegates the request to the supplied input validator object;
	 * if it finds the input invalid, the error message is displayed
	 * in the dialog's message line.
	 * This hook method is called whenever the text changes in the
	 * input field.
	 * </p>
	 */
	protected void validateInput() {

		String errorMessage = null;

		if (validator != null) {
			errorMessage = validator.isValid(text.getText());
		}

		// Bug 16256: important not to treat empty string (blank error) the same as null (no error)
		errorMessageLabel.setText(errorMessage == null ? IInternalDebugCoreConstants.EMPTY_STRING : errorMessage);
		okButton.setEnabled(errorMessage == null);

		errorMessageLabel.getParent().update();
	}
	@Override
	protected void okPressed() {
		validateInput();
		if (okButton.isEnabled()) {
			super.okPressed();
		}
	}

	/**
	 * Returns the name of the section that this dialog stores its settings in
	 *
	 * @return String
	 */
	protected String getDialogSettingsSectionName() {
		return IDebugUIConstants.PLUGIN_ID + ".CHANGE_VARIABLE_VALUE_DIALOG_SECTION"; //$NON-NLS-1$
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		IDialogSettings settings = PlatformUI
				.getDialogSettingsProvider(FrameworkUtil.getBundle(ChangeVariableValueInputDialog.class))
				.getDialogSettings();
		IDialogSettings section = settings.getSection(getDialogSettingsSectionName());
		if (section == null) {
			section = settings.addNewSection(getDialogSettingsSectionName());
		}
		return section;
	}

	@Override
	protected void handleShellCloseEvent() {
		value= null;
		super.handleShellCloseEvent();
	}
}
