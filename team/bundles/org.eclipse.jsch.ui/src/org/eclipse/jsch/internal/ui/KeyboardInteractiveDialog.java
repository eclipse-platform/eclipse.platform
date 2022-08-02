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
 *     Atsuhiko Yamanaka, JCraft,Inc. - initial API and implementation.
 *     IBM Corporation - ongoing maintenance
 *******************************************************************************/
package org.eclipse.jsch.internal.ui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.resource.ImageDescriptor;
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
 * A dialog for keyboard-interactive authentication for the ssh2 connection.
 */
public class KeyboardInteractiveDialog extends TrayDialog {
	// widgets
	private Text[] texts;
	protected Image keyLockImage;
	protected Button allowCachingButton;
	protected Text usernameField;

	protected String userName;
	protected String domain;
	protected String destination;
	protected String name;
	protected String instruction;
	protected String lang;
	protected String[] prompt;
	protected boolean[] echo;
	private String message;
	private String[] result;
	protected boolean allowCaching=false;

	private boolean isPasswordAuth=false;


	/**
	 * Creates a new KeyboardInteractiveDialog.
	 *
	 * @param parentShell the parent shell
	 * @param location
	 * @param destination
	 * @param name the name
	 * @param userName
	 * @param instruction the instruction
	 * @param prompt the titles for text fields
	 * @param echo '*' should be used or not
	 */
	public KeyboardInteractiveDialog(Shell parentShell,
						String location,
						String destination,
						String name,
						String userName,
						String instruction,
						String[] prompt,
						boolean[] echo){
		super(parentShell);
		this.domain=location;
		this.destination=destination;
		this.name=name;
		this.userName=userName;
		this.instruction=instruction;
		this.prompt=prompt;
		this.echo=echo;
		if (name!=null && name.length()>0) {
			this.message=NLS.bind(Messages.KeyboardInteractiveDialog_0, new String[] { destination, name });
		} else {
			this.message=NLS.bind(Messages.KeyboardInteractiveDialog_1, destination);
		}

		if(KeyboardInteractiveDialog.isPasswordAuth(prompt)){
			isPasswordAuth=true;
		}
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		if (isPasswordAuth) {
			newShell.setText(Messages.KeyboardInteractiveDialog_2);
		} else {
			newShell.setText(message);
		}

		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell,
				IHelpContextIds.KEYBOARD_INTERACTIVE_DIALOG);
	}

	@Override
	public void create() {
		super.create();

		if (isPasswordAuth && usernameField != null && userName != null) {
			usernameField.setText(userName);
			usernameField.setEditable(false);
		}

		if (texts.length > 0) {
			texts[0].setFocus();
		}
	}
	@Override
protected Control createDialogArea(Composite parent) {
		Composite top = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;

		top.setLayout(layout);
		top.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite imageComposite = new Composite(top, SWT.NONE);
		layout = new GridLayout();
		imageComposite.setLayout(layout);
		imageComposite.setLayoutData(new GridData(GridData.FILL_VERTICAL));

		Composite main = new Composite(top, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 3;
		main.setLayout(layout);
		main.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label imageLabel = new Label(imageComposite, SWT.NONE);
		keyLockImage = ImageDescriptor.createFromURL(JSchUIPlugin.getPlugin().getImageUrl(IUIConstants.IMG_KEY_LOCK)).createImage();
		imageLabel.setImage(keyLockImage);
		GridData data = new GridData(GridData.FILL_HORIZONTAL
				| GridData.GRAB_HORIZONTAL);
		imageLabel.setLayoutData(data);

		if (message != null) {
			Label messageLabel = new Label(main, SWT.WRAP);
			messageLabel.setText(message);
			data = new GridData(GridData.FILL_HORIZONTAL
					| GridData.GRAB_HORIZONTAL);
			data.horizontalSpan = 3;
			data.widthHint = 300;
			messageLabel.setLayoutData(data);
		}

		if (domain != null) {
			Label d = new Label(main, SWT.WRAP);
			d.setText(Messages.KeyboardInteractiveDialog_3);
			data = new GridData();
			d.setLayoutData(data);
			Label label = new Label(main, SWT.WRAP);

			label.setText(domain);

			data = new GridData(GridData.FILL_HORIZONTAL
					| GridData.GRAB_HORIZONTAL);
			data.horizontalSpan = 2;
			data.widthHint = 300;
			label.setLayoutData(data);
		}

		if (instruction != null && instruction.length() > 0) {
			Label label = new Label(main, SWT.WRAP);
			label.setText(instruction);
			data = new GridData(GridData.FILL_HORIZONTAL
					| GridData.GRAB_HORIZONTAL);
			data.horizontalSpan = 3;
			data.widthHint = 300;
			label.setLayoutData(data);
		}

		if (isPasswordAuth) {
			createUsernameFields(main);
		}

		createPasswordFields(main);

		if (isPasswordAuth && domain != null) {
			allowCachingButton = new Button(main, SWT.CHECK);
			allowCachingButton.setText(Messages.KeyboardInteractiveDialog_4);
			data = new GridData(GridData.FILL_HORIZONTAL
					| GridData.GRAB_HORIZONTAL);
			data.horizontalSpan = 3;
			allowCachingButton.setLayoutData(data);
			allowCachingButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					allowCaching = allowCachingButton.getSelection();
				}
			});
		}

		Dialog.applyDialogFont(parent);
		return main;
	}

	/**
	 * Creates the three widgets that represent the user name entry area.
	 *
	 * @param parent  the parent of the widgets
	 */
	protected void createUsernameFields(Composite parent){
		new Label(parent, SWT.NONE).setText(Messages.KeyboardInteractiveDialog_6);

		usernameField=new Text(parent, SWT.BORDER);
		GridData data=new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan=2;
		data.widthHint=convertHorizontalDLUsToPixels(IDialogConstants.ENTRY_FIELD_WIDTH);
		usernameField.setLayoutData(data);
	}

	/**
	 * Creates the widgets that represent the entry area.
	 *
	 * @param parent
	 *            the parent of the widgets
	 */
	protected void createPasswordFields(Composite parent) {
		texts=new Text[prompt.length];

		for(int i=0; i<prompt.length; i++){
			new Label(parent, SWT.NONE).setText(prompt[i]);
			int flag=SWT.BORDER;
			if(!echo[i]){
				flag|=SWT.PASSWORD;
			}
			texts[i]=new Text(parent, flag);
			GridData data=new GridData(GridData.FILL_HORIZONTAL);
			data.horizontalSpan=2;
			data.widthHint=convertHorizontalDLUsToPixels(IDialogConstants.ENTRY_FIELD_WIDTH);
			texts[i].setLayoutData(data);
			if(!echo[i]){
				texts[i].setEchoChar('*');
			}
		}
	}
	/**
	 * Returns the entered values, or null
	 * if the user canceled.
	 *
	 * @return the entered values
	 */
	public String[] getResult() {
		return result;
	}

	/**
	 * Returns <code>true</code> if the save password checkbox was selected.
	 * @return <code>true</code> if the save password checkbox was selected and <code>false</code>
	 * otherwise.
	 */
	public boolean getAllowCaching(){
		return allowCaching;
	}

	/**
	 * Notifies that the ok button of this dialog has been pressed.
	 * <p>
	 * The default implementation of this framework method sets
	 * this dialog's return code to <code>Window.OK</code>
	 * and closes the dialog. Subclasses may override.
	 * </p>
	 */
	@Override
protected void okPressed() {
		result=new String[prompt.length];
		for(int i=0; i<texts.length; i++){
			result[i]=texts[i].getText();
		}
		super.okPressed();
	}
	/**
	 * Notifies that the cancel button of this dialog has been pressed.
	 * <p>
	 * The default implementation of this framework method sets
	 * this dialog's return code to <code>Window.CANCEL</code>
	 * and closes the dialog. Subclasses may override.
	 * </p>
	 */
	@Override
protected void cancelPressed() {
		result=null;
		super.cancelPressed();
	}

	@Override
	public boolean close(){
		if(keyLockImage!=null){
			keyLockImage.dispose();
		}
		return super.close();
	}

	/**
	 * Guesses if this dialog is used for password authentication.
	 * @param prompt prompts for keyboard-interactive authentication method.
	 * @return <code>true</code> if this dialog is used for password authentication.
	 */
	static boolean isPasswordAuth(String[] prompt) {
		return prompt != null && prompt.length == 1
				&& prompt[0].trim().equalsIgnoreCase("password:");  //$NON-NLS-1$
	}
}
