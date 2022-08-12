/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
 *     Atsuhiko Yamanaka, JCraft,Inc. - copying this class from o.e.team.cvs.ui plug-in.
 *******************************************************************************/
package org.eclipse.jsch.internal.ui.authenticator;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jsch.internal.ui.IUIConstants;
import org.eclipse.jsch.internal.ui.JSchUIPlugin;
import org.eclipse.jsch.internal.ui.Messages;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

/**
 * A dialog for prompting for a user name and password
 * @since 1.1
 */
public class UserValidationDialog extends TrayDialog{
	// widgets
	protected Text usernameField;
	protected Text passwordField;
	protected Button allowCachingButton;

	protected String comment;
	protected String defaultUsername;
	protected String password=null;
	protected boolean allowCaching=false;
	protected boolean isAllowCaching=false;
	protected Image keyLockImage;

	// whether or not the user name can be changed
	protected boolean isUsernameMutable=true;
	protected String username=null;
	protected String message=null;

	/**
	 * Creates a new UserValidationDialog.
	 *
	 * @param parentShell
	 *          the parent shell
	 * @param comment
	 *          the location
	 * @param defaultName
	 *          the default user name
	 * @param message
	 *          a message to display to the user
	 * @param isAllowCaching
	 *          a flag to show a check box to save password
	 */
	public UserValidationDialog(Shell parentShell, String comment,
			String defaultName, String message, boolean isAllowCaching){
		super(parentShell);
		setShellStyle(getShellStyle()|SWT.RESIZE);
		this.defaultUsername=defaultName;
		this.comment=comment;
		this.message=message;
		this.isAllowCaching=isAllowCaching;
	}

	@Override
	protected void configureShell(Shell newShell){
		super.configureShell(newShell);
		newShell.setText(Messages.UserValidationDialog_required);
		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell,
				IHelpContextIds.USER_VALIDATION_DIALOG);
	}


	@Override
public void create(){
		super.create();
		// add some default values
		usernameField.setText(defaultUsername);

		if(isUsernameMutable){
			// give focus to user name field
			usernameField.selectAll();
			usernameField.setFocus();
		}
		else{
			usernameField.setEditable(false);
			passwordField.setFocus();
		}
	}

	@Override
protected Control createDialogArea(Composite parent){
		Composite top=new Composite(parent, SWT.NONE);
		GridLayout layout=new GridLayout();
		layout.numColumns=2;

		top.setLayout(layout);
		top.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite imageComposite=new Composite(top, SWT.NONE);
		layout=new GridLayout();
		imageComposite.setLayout(layout);
		imageComposite.setLayoutData(new GridData(GridData.FILL_VERTICAL));

		Composite main=new Composite(top, SWT.NONE);
		layout=new GridLayout();
		layout.numColumns=3;
		main.setLayout(layout);
		main.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label imageLabel=new Label(imageComposite, SWT.NONE);
		keyLockImage=JSchUIPlugin.getImageDescriptor(IUIConstants.IMG_KEY_LOCK)
				.createImage();
		imageLabel.setImage(keyLockImage);
		GridData data=new GridData(GridData.FILL_HORIZONTAL
				|GridData.GRAB_HORIZONTAL);
		imageLabel.setLayoutData(data);

		if(message!=null){
			Label messageLabel=new Label(main, SWT.WRAP);
			messageLabel.setText(message);
			data=new GridData(GridData.FILL_HORIZONTAL|GridData.GRAB_HORIZONTAL);
			data.horizontalSpan=3;
			data.widthHint=400;
			messageLabel.setLayoutData(data);
		}
		if(comment!=null){
			Label label=new Label(main, SWT.WRAP);
			if(isUsernameMutable){
				label.setText(NLS.bind(Messages.UserValidationDialog_labelUser,
						new String[] {comment}));
			}
			else{
				label.setText(NLS.bind(Messages.UserValidationDialog_labelPassword,
						(new Object[] {defaultUsername, comment})));
			}
			data=new GridData(GridData.FILL_HORIZONTAL|GridData.GRAB_HORIZONTAL);
			data.horizontalSpan=3;
			data.widthHint=400;
			label.setLayoutData(data);
		}
		createUsernameFields(main);
		createPasswordFields(main);

		if(isAllowCaching){
		allowCachingButton=new Button(main, SWT.CHECK);
		allowCachingButton.setText(Messages.UserValidationDialog_6);
		data=new GridData(GridData.FILL_HORIZONTAL|GridData.GRAB_HORIZONTAL);
		data.horizontalSpan=3;
		allowCachingButton.setLayoutData(data);
		allowCachingButton.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				allowCaching=allowCachingButton.getSelection();
			}
		});
		}

		Dialog.applyDialogFont(parent);

		return main;
	}

	/**
	 * Creates the three widgets that represent the password entry area.
	 *
	 * @param parent
	 *          the parent of the widgets
	 */
	protected void createPasswordFields(Composite parent){
		new Label(parent, SWT.NONE).setText(Messages.UserValidationDialog_password);

		passwordField=new Text(parent, SWT.BORDER|SWT.PASSWORD);
		GridData data=new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan=2;
		data.widthHint=convertHorizontalDLUsToPixels(IDialogConstants.ENTRY_FIELD_WIDTH);
		passwordField.setLayoutData(data);
	}

	/**
	 * Creates the three widgets that represent the user name entry area.
	 *
	 * @param parent
	 *          the parent of the widgets
	 */
	protected void createUsernameFields(Composite parent){
		new Label(parent, SWT.NONE).setText(Messages.UserValidationDialog_user);

		usernameField=new Text(parent, SWT.BORDER);
		GridData data=new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan=2;
		data.widthHint=convertHorizontalDLUsToPixels(IDialogConstants.ENTRY_FIELD_WIDTH);
		usernameField.setLayoutData(data);
	}

	/**
	 * Returns the password entered by the user, or null if the user canceled.
	 *
	 * @return the entered password
	 */
	public String getPassword(){
		return password;
	}

	/**
	 * Returns the username entered by the user, or null if the user canceled.
	 *
	 * @return the entered username
	 */
	public String getUsername(){
		return username;
	}

	/**
	 * Returns <code>true</code> if the save password checkbox was selected.
	 *
	 * @return <code>true</code> if the save password checkbox was selected and
	 *         <code>false</code> otherwise.
	 */
	public boolean getAllowCaching(){
		return allowCaching;
	}

	/**
	 * Notifies that the ok button of this dialog has been pressed.
	 * <p>
	 * The default implementation of this framework method sets this dialog's
	 * return code to <code>Window.OK</code> and closes the dialog. Subclasses
	 * may override.
	 * </p>
	 */
	@Override
protected void okPressed(){
		password=passwordField.getText();
		username=usernameField.getText();

		super.okPressed();
	}

	/**
	 * Sets whether or not the username field should be mutable. This method must
	 * be called before create(), otherwise it will be ignored.
	 *
	 * @param value
	 *          whether the username is mutable
	 */
	public void setUsernameMutable(boolean value){
		isUsernameMutable=value;
	}

	@Override
	public boolean close(){
		if(keyLockImage!=null){
			keyLockImage.dispose();
		}
		return super.close();
	}
}
