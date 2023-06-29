/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.actions.variables.details;

import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.views.variables.VariablesViewMessages;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.FrameworkUtil;

/**
 * Provides a dialog for changing the maximum length allowed in the detail pane
 *
 * @since 3.0
 */
public class DetailPaneMaxLengthDialog extends TrayDialog {

	private static final String SETTINGS_ID = DebugUIPlugin.getUniqueIdentifier() + ".MAX_DETAILS_LENGTH_DIALOG"; //$NON-NLS-1$

	private Text fTextWidget;
	private Text fErrorTextWidget;
	private String fErrorMessage;
	private String fValue;
	private IInputValidator fValidator;

	/**
	 * Constructs a new dialog on the given shell.
	 *
	 * @param parent shell
	 */
	public DetailPaneMaxLengthDialog(Shell parent) {
		super(parent);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		fValue = Integer.toString(DebugUIPlugin.getDefault().getPreferenceStore().getInt(IDebugUIConstants.PREF_MAX_DETAIL_LENGTH));
		fValidator = newText -> {
			try {
				int num = Integer.parseInt(newText);
				if (num < 0) {
					return VariablesViewMessages.DetailPaneMaxLengthDialog_2;
				}
			} catch (NumberFormatException e) {
				return VariablesViewMessages.DetailPaneMaxLengthDialog_3;
			}
			return null;
		};
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		IDialogSettings settings = PlatformUI
				.getDialogSettingsProvider(FrameworkUtil.getBundle(DetailPaneMaxLengthDialog.class))
				.getDialogSettings();
		IDialogSettings section = settings.getSection(SETTINGS_ID);
		if (section == null) {
			section = settings.addNewSection(SETTINGS_ID);
		}
		return section;
	}

	@Override
	protected Control createContents(Composite parent) {
		getShell().setText(VariablesViewMessages.DetailPaneMaxLengthDialog_0);
		Control contents = super.createContents(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getDialogArea(), IDebugHelpContextIds.DETAIL_PANE_MAX_LENGTH_ACTION);
		return contents;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		Label label = new Label(composite, SWT.WRAP);
		label.setText(VariablesViewMessages.DetailPaneMaxLengthDialog_1);
		GridData data = new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_CENTER);
		data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
		label.setLayoutData(data);
		label.setFont(parent.getFont());
		fTextWidget = new Text(composite, SWT.SINGLE | SWT.BORDER);
		fTextWidget.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		fTextWidget.setText(fValue);
		fTextWidget.addModifyListener(e -> {
			validateInput();
			fValue = fTextWidget.getText();
		});
		fErrorTextWidget = new Text(composite, SWT.READ_ONLY);
		fErrorTextWidget.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
				| GridData.HORIZONTAL_ALIGN_FILL));
		fErrorTextWidget.setBackground(fErrorTextWidget.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		setErrorMessage(fErrorMessage);
		applyDialogFont(composite);
		return composite;
	}

	@Override
	protected void okPressed() {
		String text = getValue();
		try {
			DebugUIPlugin.getDefault().getPreferenceStore().setValue(IDebugUIConstants.PREF_MAX_DETAIL_LENGTH, Integer.parseInt(text));
		}
		catch (NumberFormatException e) {
			DebugUIPlugin.log(e);
		}
		super.okPressed();
	}

	/**
	 * Returns the string typed into this input dialog.
	 *
	 * @return the input string
	 * @since 3.3
	 */
	public String getValue() {
		return fValue;
	}

	/**
	 * Validates the current input
	 * @since 3.3
	 */
	private void validateInput() {
		String errorMessage = null;
		if (fValidator != null) {
			errorMessage = fValidator.isValid(fTextWidget.getText());
		}
		setErrorMessage(errorMessage);
	}

	/**
	 * Sets the current error message or none if null
	 * @param errorMessage the message to display
	 * @since 3.3
	 */
	public void setErrorMessage(String errorMessage) {
		fErrorMessage = errorMessage;
		if (fErrorTextWidget != null && !fErrorTextWidget.isDisposed()) {
			fErrorTextWidget.setText(errorMessage == null ? IInternalDebugCoreConstants.EMPTY_STRING : errorMessage);
			fErrorTextWidget.getParent().update();
			// Access the ok button by id, in case clients have overridden button creation.
			// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=113643
			Control button = getButton(IDialogConstants.OK_ID);
			if (button != null) {
				button.setEnabled(errorMessage == null);
			}
		}
	}
}
