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
package org.eclipse.update.internal.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.*;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.update.internal.ui.UpdateUI;
import org.eclipse.update.internal.ui.model.NamedModelObject;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class RenameDialog extends Dialog {
	private static final String KEY_LABEL = "RenameDialog.label";
	private static final String KEY_EXISTS = "RenameDialog.exists";
	private Text text;
	private CLabel status;
	private NamedModelObject object;
	private Object [] siblings;
	private Button okButton;

	/**
	 * Constructor for RenameDialog.
	 * @param parentShell
	 */
	public RenameDialog(Shell parentShell, NamedModelObject object, Object [] siblings) {
		super(parentShell);
		//setShellStyle(getShellStyle()|SWT.RESIZE);
		this.object = object;
		this.siblings = siblings;
	}

	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		okButton =
			createButton(
				parent,
				IDialogConstants.OK_ID,
				IDialogConstants.OK_LABEL,
				true);
		createButton(
			parent,
			IDialogConstants.CANCEL_ID,
			IDialogConstants.CANCEL_LABEL,
			false);
		verifyName();
	}

	public Control createDialogArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 10;
		layout.marginWidth = 10;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		Label label = new Label(container, SWT.NULL);
		label.setText(UpdateUI.getString(KEY_LABEL));

		text = new Text(container, SWT.SINGLE | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		text.setLayoutData(gd);

		status = new CLabel(container, SWT.NULL);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		status.setLayoutData(gd);

		text.setText(object.getName());
		text.selectAll();
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				verifyName();
			}
		});
		WorkbenchHelp.setHelp(container, "org.eclipse.update.ui.RenameDialog");
		return container;
	}

	private void verifyName() {
		String name = text.getText().trim();
		boolean duplicate = false;
		for (int i = 0; i < siblings.length; i++) {
			if (((NamedModelObject)siblings[i]).getName().equals(name)) {
				duplicate = true;
				break;
			}
		}
		status.setText(
			duplicate
				? UpdateUI.getFormattedMessage(KEY_EXISTS, name)
				: "");
		okButton.setEnabled(!duplicate);
	}

	public void buttonPressed(int id) {
		if (id == IDialogConstants.OK_ID) {
			object.setName(text.getText());
		}
		super.buttonPressed(id);
	}

}
