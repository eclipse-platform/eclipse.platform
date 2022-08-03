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
 *     Atsuhiko Yamanaka, JCraft,Inc. - initial API and implementation.
 *     IBM Corporation - ongoing maintenance
 *******************************************************************************/
package org.eclipse.jsch.internal.ui.preference;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jsch.internal.ui.Messages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;


class PassphraseDialog extends Dialog{
	protected Text passphraseField;
	protected String passphrase=null;
	protected String message=null;

	public PassphraseDialog(Shell parentShell, String message){
		super(parentShell);
		this.message=message;
	}

	@Override
	protected void configureShell(Shell newShell){
		super.configureShell(newShell);
		newShell.setText(message);
	}

	@Override
	public void create(){
		super.create();
		passphraseField.setFocus();
	}

	@Override
	protected Control createDialogArea(Composite parent){
		initializeDialogUnits(parent);
		Composite main=new Composite(parent, SWT.NONE);

		GridLayout layout=new GridLayout();
		layout.numColumns=3;
		layout.marginHeight=convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth=convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing=convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing=convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		main.setLayout(layout);
		main.setLayoutData(new GridData(GridData.FILL_BOTH));

		if(message!=null){
			Label messageLabel=new Label(main, SWT.WRAP);
			messageLabel.setText(message);
			GridData data=new GridData(GridData.FILL_HORIZONTAL);
			data.horizontalSpan=3;
			messageLabel.setLayoutData(data);
		}

		createPassphraseFields(main);
		Dialog.applyDialogFont(main);
		return main;
	}

	protected void createPassphraseFields(Composite parent){
		new Label(parent, SWT.NONE).setText(Messages.CVSSSH2PreferencePage_127);
		passphraseField=new Text(parent, SWT.BORDER);
		GridData data=new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint=convertHorizontalDLUsToPixels(IDialogConstants.ENTRY_FIELD_WIDTH);
		passphraseField.setLayoutData(data);
		passphraseField.setEchoChar('*');

		new Label(parent, SWT.NONE);
	}

	public String getPassphrase(){
		return passphrase;
	}

	@Override
	protected void okPressed(){
		String _passphrase=passphraseField.getText();
		if(_passphrase==null||_passphrase.length()==0){
			return;
		}
		passphrase=_passphrase;
		super.okPressed();
	}

	@Override
	protected void cancelPressed(){
		passphrase=null;
		super.cancelPressed();
	}
}
