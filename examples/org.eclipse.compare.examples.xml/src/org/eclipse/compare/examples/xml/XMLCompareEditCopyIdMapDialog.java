/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.compare.examples.xml;

import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.eclipse.compare.examples.xml.ui.StatusDialog;
import org.eclipse.compare.examples.xml.ui.StatusInfo;

/**
 * This class is used to create an editable ID Mapping Scheme from an internal ID Mappping Scheme
 */
public class XMLCompareEditCopyIdMapDialog extends StatusDialog {
	
	private HashMap fIdMaps;
	private HashMap fIdMapsInternal;
	
	private Text fIdMapText;
	private String fResult;

	/*
	 * Constructs a new edit copy mapping dialog.
	 */	
	public XMLCompareEditCopyIdMapDialog(Shell parent, IdMap idmap, HashMap idmaps, HashMap idmapsInternal) {
		super(parent);
	
		setTitle(XMLCompareMessages.XMLCompareEditCopyIdMapDialog_title); 

		fIdMaps= idmaps;
		fIdMapsInternal= idmapsInternal;
	}
	
	public String getResult() {
		return fResult;
	}
	
	/**
	 * Creates and returns the contents of the upper part 
	 * of the dialog (above the button bar).
	 *
	 * Subclasses should override.
	 *
	 * @param ancestor the parent composite to contain the dialog area
	 * @return the dialog area control
	 */		
	@Override
	protected Control createDialogArea(Composite ancestor) {
		Composite composite= (Composite) super.createDialogArea(ancestor);
		
		Label comment= new Label(composite, SWT.NONE);
		comment.setText(XMLCompareMessages.XMLCompareEditCopyIdMapDialog_comment); 
		GridData data= new GridData();
		data.horizontalAlignment= GridData.FILL;
		data.verticalAlignment= GridData.BEGINNING;
		comment.setLayoutData(data);
		
		Composite inner= new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		inner.setLayout(layout);
		inner.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Label label= new Label(inner, SWT.NULL);
		label.setText(XMLCompareMessages.XMLCompareEditCopyIdMapDialog_label); 
		label.setLayoutData(new GridData());

		fIdMapText= new Text(inner, SWT.BORDER);
		fIdMapText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fIdMapText.addModifyListener(e -> doValidation());

		fIdMapText.setFocus();
		
		return composite;
	}
	
	/**
	 * Validate user input
	 */		
	private void doValidation() {
		StatusInfo status= new StatusInfo();
		String newText= fIdMapText.getText();
		if (newText.length() == 0)
			status.setError(XMLCompareMessages.XMLCompareEditCopyIdMapDialog_error_noname); 
		else if (XMLComparePreferencePage.containsInvalidCharacters(newText))
			status.setError(XMLCompareMessages.XMLCompareEditCopyIdMapDialog_error_invalidname); 
		else if (fIdMaps.containsKey(newText) || fIdMapsInternal.containsKey(newText))
			status.setError(XMLCompareMessages.XMLCompareEditCopyIdMapDialog_error_nameExists); 
		updateStatus(status);
	}
	
	/**
	 * Notifies that the ok button of this dialog has been pressed.
	 */		
	@Override
	protected void okPressed() {
		fResult= fIdMapText.getText();
		super.okPressed();
	}
}
