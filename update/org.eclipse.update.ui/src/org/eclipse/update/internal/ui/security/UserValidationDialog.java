/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.ui.security;

import java.net.*;

import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.*;
import org.eclipse.update.internal.ui.*;


/**
 * User authentication dialog
 */
public class UserValidationDialog extends Dialog {
	protected Text usernameField;
	protected Text passwordField;

	protected String domain;
	protected String realm;
	protected String defaultUsername;
	protected String password = null;
	protected String userid = null;
	protected boolean isUsernameMutable = true;
	/**
	 * Creates a new UserValidationDialog.
	 */
	public UserValidationDialog(Shell parentShell, URL location, String realm, String defaultName) {
		super(parentShell);
		this.defaultUsername = defaultName;
		this.domain = location.getHost();
		this.realm = realm;
	}
	/**
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(UpdateUI.getString("UserVerificationDialog.PasswordRequired")); //$NON-NLS-1$
	}
	/**
	 */
	public void create() {
		super.create();
		//add some default values
		usernameField.setText(defaultUsername);

		if (isUsernameMutable) {
			//give focus to username field
			usernameField.selectAll();
			usernameField.setFocus();
		}
		else {
			usernameField.setEditable(false);
			passwordField.setFocus();
		}
	}
	/**
	 */
	protected Control createDialogArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		main.setLayout(layout);
		main.setLayoutData(new GridData(GridData.FILL_BOTH));

		Label label = new Label(main, SWT.WRAP);
		String text = UpdateUI.getString("UserVerificationDialog.EnterNameAndPassword")+ realm; //$NON-NLS-1$
		text += "\n" + UpdateUI.getString("UserVerificationDialog.Domain")+domain; //$NON-NLS-1$ //$NON-NLS-2$
		label.setText(text);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 3;
		label.setLayoutData(data);

		createUsernameFields(main);
		createPasswordFields(main);
		WorkbenchHelp.setHelp(main, "org.eclipse.update.ui.UserValidationDialog"); //$NON-NLS-1$
		return main;
	}
	/**
	 * Creates the three widgets that represent the user name entry
	 * area.
	 */
	protected void createPasswordFields(Composite parent) {
		new Label(parent, SWT.NONE).setText(UpdateUI.getString("UserVerificationDialog.Password")); //$NON-NLS-1$

		passwordField = new Text(parent, SWT.BORDER|SWT.PASSWORD);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.ENTRY_FIELD_WIDTH);
		passwordField.setLayoutData(data);

		new Label(parent, SWT.NONE); //spacer
	}
	/**
	 * Creates the three widgets that represent the user name entry
	 * area.
	 */
	protected void createUsernameFields(Composite parent) {
		new Label(parent, SWT.NONE).setText(UpdateUI.getString("UserVerificationDialog.UserName")); //$NON-NLS-1$

		usernameField = new Text(parent, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.ENTRY_FIELD_WIDTH);
		usernameField.setLayoutData(data);

		new Label(parent, SWT.NONE); //spacer
	}
	/**
	 * Returns the password entered by the user, or null
	 * if the user canceled.
	 */
	public String getPassword() {
		return password;
	}
	/**
	 * Returns the username entered by the user, or null
	 * if the user canceled.
	 */
	public String getUserid() {
		return userid;
	}
	/**
	 * Notifies that the ok button of this dialog has been pressed.
	 */
	protected void okPressed() {
		password = passwordField.getText();
		userid = usernameField.getText();
		super.okPressed();
	}
	/**
	 * Sets whether or not the username field should be mutable.
	 * This method must be called before create(), otherwise it
	 * will be ignored.
	 */
	public void setUsernameMutable(boolean value) {
		isUsernameMutable = value;
	}
}
