/*******************************************************************************
 * Copyright (c) 2007 Benjamin Muskalla and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Benjamin Muskalla - initial implementation
 *     IBM Canada - review initial contribution and commit
 *******************************************************************************/
package org.eclipse.debug.internal.ui.stringsubstitution;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Prompts the user to input a string.  The dialog will use a password text field so all
 * typed characters are hidden on the screen.
 */
public class PasswordPrompt extends PromptingResolver {

	private String returnValue;

	@Override
	public void prompt() {
		Dialog dialog = new Dialog((Shell)null){
			private Text text;

			@Override
			protected Control createDialogArea(Composite parent) {
				 // create composite
				Composite composite = (Composite) super.createDialogArea(parent);
				// create message
				if (dialogMessage != null) {
					Label label = new Label(composite, SWT.WRAP);
					label.setText(dialogMessage);
					GridData data = new GridData(GridData.GRAB_HORIZONTAL
							| GridData.GRAB_VERTICAL | GridData.HORIZONTAL_ALIGN_FILL
							| GridData.VERTICAL_ALIGN_CENTER);
					data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
					label.setLayoutData(data);
					label.setFont(parent.getFont());
				}
				text = new Text(composite, SWT.SINGLE | SWT.BORDER | SWT.PASSWORD);
				text.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
						| GridData.HORIZONTAL_ALIGN_FILL));
				String value = defaultValue == null ? lastValue : defaultValue;
				if (value != null){
					text.setText(value);
				}
				applyDialogFont(composite);
				return composite;
			}

			@Override
			protected void buttonPressed(int buttonId) {
				if (buttonId == IDialogConstants.OK_ID) {
					returnValue = text.getText();
				} else {
					returnValue = null;
				}
				super.buttonPressed(buttonId);
			}

			@Override
			protected void configureShell(Shell newShell) {
				super.configureShell(newShell);
				newShell.setText(StringSubstitutionMessages.StringPromptExpander_0);
			}

		};

		int dialogResult = dialog.open();
		if (dialogResult == Window.OK) {
			dialogResultString = returnValue;
		}
	}

}