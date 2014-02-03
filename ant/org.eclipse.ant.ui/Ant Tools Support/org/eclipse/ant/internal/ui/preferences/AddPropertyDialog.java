/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.preferences;

import org.eclipse.ant.internal.ui.IAntUIHelpContextIds;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

public class AddPropertyDialog extends Dialog {

	private String fName;
	private String fValue;

	private String fTitle;
	
	private Label fNameLabel;
	private Text fNameText;
	private Label fValueLabel;
	private Text fValueText;
	
	private String[] fInitialValues;

	public AddPropertyDialog(Shell shell, String title, String[] initialValues) {
		super(shell);
		fTitle = title;
		fInitialValues= initialValues;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite comp= (Composite) super.createDialogArea(parent);
		((GridLayout) comp.getLayout()).numColumns = 2;
		
		fNameLabel = new Label(comp, SWT.NONE);
		fNameLabel.setText(AntPreferencesMessages.AddPropertyDialog__Name__1);
		fNameLabel.setFont(comp.getFont());
		
		fNameText = new Text(comp, SWT.BORDER | SWT.SINGLE);
		fNameText.setText(fInitialValues[0]);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 300;
		fNameText.setLayoutData(gd);
		fNameText.setFont(comp.getFont());
		fNameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateButtons();
			}
		});
		
		fValueLabel = new Label(comp, SWT.NONE);
		fValueLabel.setText(AntPreferencesMessages.AddPropertyDialog__Value__2);
		fValueLabel.setFont(comp.getFont());
		
		fValueText = new Text(comp, SWT.BORDER | SWT.SINGLE);
		fValueText.setText(fInitialValues[1]);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 300;
		fValueText.setLayoutData(gd);
		fValueText.setFont(comp.getFont());
		fValueText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateButtons();
			}
		});		
		
		Button variablesButton = new Button(comp, SWT.PUSH);
		variablesButton.setText(AntPreferencesMessages.AddPropertyDialog_2);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
		gd.horizontalSpan = 2;
		int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		gd.widthHint = Math.max(widthHint, variablesButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		variablesButton.setLayoutData(gd);
		variablesButton.setFont(comp.getFont());
		
		variablesButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent se) {
				getVariable();
			}
		});
		
		return comp;
	}
	
	protected void getVariable() {
		StringVariableSelectionDialog variablesDialog = new StringVariableSelectionDialog(getShell());
		int returnCode = variablesDialog.open();
		if (returnCode == IDialogConstants.OK_ID) {
			String variable = variablesDialog.getVariableExpression();
			if (variable != null) {
				fValueText.insert(variable.trim());
			}
		}
	}

	/**
	 * Return the name/value pair entered in this dialog.  If the cancel button was hit,
	 * both will be <code>null</code>.
	 */
	public String[] getNameValuePair() {
		return new String[] {fName, fValue};
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID) {
			//https://bugs.eclipse.org/bugs/show_bug.cgi?id=248316
			fName= fNameText.getText().trim();
			fValue = fValueText.getText().trim();
		} else {
			fName = null;
			fValue = null;
		}
		super.buttonPressed(buttonId);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
	 */
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		if (fTitle != null) {
			shell.setText(fTitle);
		}
		if (fInitialValues[0].length() == 0) {
			PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IAntUIHelpContextIds.ADD_PROPERTY_DIALOG);
		} else {
			PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IAntUIHelpContextIds.EDIT_PROPERTY_DIALOG);
		}
	}
	
	/**
	 * Enable the OK button if valid input
	 */
	protected void updateButtons() {
		String name = fNameText.getText().trim();
		String value = fValueText.getText().trim();
		getButton(IDialogConstants.OK_ID).setEnabled((name.length() > 0) &&(value.length() > 0));
	}
	
	/**
	 * Enable the buttons on creation.
	 * @see org.eclipse.jface.window.Window#create()
	 */
	public void create() {
		super.create();
		updateButtons();
	}
}
