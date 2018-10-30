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

import org.eclipse.compare.examples.xml.ui.StatusDialog;
import org.eclipse.compare.examples.xml.ui.StatusInfo;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * This class is used to add or edit a particular ID Mapping
 */
public class XMLCompareEditMappingDialog extends StatusDialog {
	
	private Mapping fMapping;
	private HashMap fIdmapHM;
	private boolean fEdit;
	
	private Text fElementText;
	private Text fSignatureText;
	private Text fIdAttributeText;

	private Button fIdTypeAttributeButton;
	private Button fIdTypeChildBodyButton;
	
	/*
	 * Constructs a new edit mapping dialog.
	 */		
	public XMLCompareEditMappingDialog(Shell parent, Mapping mapping, HashMap idmapHM, boolean edit) {
		super(parent);
	
		int shellStyle= getShellStyle();
		setShellStyle(shellStyle | SWT.MAX | SWT.RESIZE);

	
		fEdit= edit;
		if (fEdit)
			setTitle(XMLCompareMessages.XMLCompareEditMappingDialog_editTitle); 
		else
			setTitle(XMLCompareMessages.XMLCompareEditMappingDialog_newTitle); 

		fMapping= mapping;
		fIdmapHM= idmapHM;
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
		
		Composite inner= new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		inner.setLayout(layout);
		inner.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		//Element
		Label label= new Label(inner, SWT.NULL);
		label.setText(XMLCompareMessages.XMLCompareEditMappingDialog_element); 
		label.setLayoutData(new GridData());

		fElementText= new Text(inner, SWT.BORDER);
		fElementText.setText(fMapping.getElement());
		fElementText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fElementText.addModifyListener(e -> doValidation());

		//Signature
		label= new Label(inner, SWT.NULL);
		label.setText(XMLCompareMessages.XMLCompareEditMappingDialog_signature); 
		label.setLayoutData(new GridData());

		fSignatureText= new Text(inner, SWT.BORDER);
		fSignatureText.setText(fMapping.getSignature());
		GridData data= new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint= convertWidthInCharsToPixels(50);
		fSignatureText.setLayoutData(data);
		fSignatureText.addModifyListener(e -> doValidation());
		
		//Id Attribute
		label= new Label(inner, SWT.NULL);
		label.setText(XMLCompareMessages.XMLCompareEditMappingDialog_idattribute); 
		label.setLayoutData(new GridData());

		fIdAttributeText= new Text(inner, SWT.BORDER);

		fIdAttributeText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fIdAttributeText.addModifyListener(e -> doValidation());

		//Id Source
		createIdSourceGroup(inner);

		fElementText.setFocus();

		return composite;
	}
	
	/**
	 * Validate user input
	 */	
	private void doValidation() {
		StatusInfo status= new StatusInfo();
		String text= fElementText.getText();
		String mappingKey= Mapping.getKey(fSignatureText.getText(), text);
		String errormsg= ""; //$NON-NLS-1$
		boolean isError= false;
		if (text.length() == 0) {
			errormsg= XMLCompareMessages.XMLCompareEditMappingDialog_error_noname; 
			isError= true;
		} else if (XMLComparePreferencePage.containsInvalidCharacters(text)) {
			if (errormsg == "") errormsg= XMLCompareMessages.XMLCompareEditMappingDialog_error_invalidname;  //$NON-NLS-1$
			isError= true;
		} else if (!fEdit && fIdmapHM != null && fIdmapHM.containsKey(mappingKey)) {
			if (errormsg == "") errormsg= XMLCompareMessages.XMLCompareEditMappingDialog_error_mappingExists;  //$NON-NLS-1$
			isError= true;
		}
		text= fSignatureText.getText();
		if (XMLComparePreferencePage.containsInvalidCharacters(text)) {
			if (errormsg == "") errormsg= XMLCompareMessages.XMLCompareEditMappingDialog_error_invalidsignature;  //$NON-NLS-1$
			isError= true;
		}
		text= fIdAttributeText.getText();
		if (text.length() == 0)
			isError= true;
		else if (XMLComparePreferencePage.containsInvalidCharacters(text)) {
			if (errormsg == "") errormsg= XMLCompareMessages.XMLCompareEditMappingDialog_error_invalididattribute;  //$NON-NLS-1$
			isError= true;
		}
		if (isError) status.setError(errormsg);
		updateStatus(status);
	}
	
	/**
	 * Notifies that the ok button of this dialog has been pressed.
	 */	
	@Override
	protected void okPressed() {
		fMapping.setElement(fElementText.getText());
		fMapping.setSignature(fSignatureText.getText());
		String idtext= fIdAttributeText.getText();
		if (fIdTypeChildBodyButton.getSelection()) {
			idtext= Character.valueOf(XMLStructureCreator.ID_TYPE_BODY) + idtext;
		}
		fMapping.setIdAttribute(idtext);
		super.okPressed();
	}
	
	private void createIdSourceGroup(Composite composite) {
		Label titleLabel= new Label(composite, SWT.NONE);
		titleLabel.setText(XMLCompareMessages.XMLCompareEditMappingDialog_idtype); 
		titleLabel.setToolTipText(XMLCompareMessages.XMLCompareEditMappingDialog_idtype_tooltip); 
	
		Composite buttonComposite= new Composite(composite, SWT.LEFT);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		buttonComposite.setLayout(layout);
		composite.setData(new GridData());
	
		//attribute button
		fIdTypeAttributeButton= createRadioButton(buttonComposite, XMLCompareMessages.XMLComparePreference_idtype_attribute); 
		fIdTypeAttributeButton.setToolTipText(XMLCompareMessages.XMLCompareEditMappingDialog_idtype_attribute_tooltip); 
	
		//child body button
		fIdTypeChildBodyButton= createRadioButton(buttonComposite, XMLCompareMessages.XMLComparePreference_idtype_child_body); 
		fIdTypeChildBodyButton.setToolTipText(XMLCompareMessages.XMLCompareEditMappingDialog_idtype_childbody_tooltip); 
	
		String idtext= fMapping.getIdAttribute();
		if (fEdit && idtext.charAt(0) == XMLStructureCreator.ID_TYPE_BODY) {
			idtext= idtext.substring(1,idtext.length());
			fIdTypeChildBodyButton.setSelection(true);
		} else
			fIdTypeAttributeButton.setSelection(true);
		fIdAttributeText.setText(idtext);
	
	}

	private Button createRadioButton(Composite parent, String label) {
		Button button= new Button(parent, SWT.RADIO | SWT.LEFT);
		button.setText(label);
		GridData data= new GridData();
		button.setLayoutData(data);
		return button;
	}	
}
