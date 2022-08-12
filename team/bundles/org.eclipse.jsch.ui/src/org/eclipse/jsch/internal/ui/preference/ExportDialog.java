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
 *     Sebastian Davids (sdavids@gmx.de) - Bug 54599 [SSH2] Export SSH Key ... Dialog does not standard margins
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

class ExportDialog extends Dialog{
	protected Text field;
	protected String target=null;
	protected String title=null;
	protected String message=null;

	public ExportDialog(Shell parentShell, String title, String message){
		super(parentShell);
		this.title=title;
		this.message=message;
	}

	@Override
	protected void configureShell(Shell newShell){
		super.configureShell(newShell);
		newShell.setText(title);
	}

	@Override
	public void create(){
		super.create();
		field.setFocus();
	}

	@Override
	protected Control createDialogArea(Composite parent){
		initializeDialogUnits(parent);
		Composite main=new Composite(parent, SWT.NONE);
		GridLayout layout=new GridLayout();
		layout.numColumns=2;
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
			data.horizontalSpan=2;
			messageLabel.setLayoutData(data);
		}

		createTargetFields(main);
		Dialog.applyDialogFont(main);
		return main;
	}

	protected void createTargetFields(Composite parent){
		new Label(parent, SWT.NONE).setText(Messages.CVSSSH2PreferencePage_125);

		field=new Text(parent, SWT.BORDER);
		GridData data=new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint=convertHorizontalDLUsToPixels(IDialogConstants.ENTRY_FIELD_WIDTH);
		field.setLayoutData(data);
	}

	public String getTarget(){
		return target;
	}

	@Override
	protected void okPressed(){
		String _target=field.getText();
		if(_target==null||_target.length()==0){
			return;
		}
		target=_target;
		super.okPressed();
	}

	@Override
	protected void cancelPressed(){
		target=null;
		super.cancelPressed();
	}
}
