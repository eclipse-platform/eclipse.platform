/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.help.ui.internal.views;

import java.util.ArrayList;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.help.ui.internal.HelpUIPlugin;
import org.eclipse.help.ui.internal.Messages;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.SelectionStatusDialog;

public class RenameDialog extends SelectionStatusDialog {
	private ArrayList<String> oldNames;
	private String oldName;
	private String newName;
	private Text text;
	private IStatus status;
	private boolean isCaseSensitive;

	/**
	 * Create a new rename dialog instance for the given window.
	 * @param shell The parent of the dialog
	 * @param oldName Current name of the item being renamed
	 */
	public RenameDialog(Shell shell, String oldName) {
		super(shell);
		this.isCaseSensitive = false;
		initialize();
		setOldName(oldName);
	}

	/**
	 * Create a new rename dialog instance for the given window.
	 * @param shell The parent of the dialog
	 * @param isCaseSensitive Flags whether dialog will perform case sensitive checks against old names
	 * @param names Set of names which the user should not be allowed to rename to
	 * @param oldName Current name of the item being renamed
	 */
	public RenameDialog(Shell shell, boolean isCaseSensitive, String[] names, String oldName){
		super(shell);
		this.isCaseSensitive = isCaseSensitive;
		initialize();
		if (names!=null){
			for (int i = 0; i<names.length; i++)
				addOldName(names[i]);
		}
		setOldName(oldName);
	}

	public void initialize(){
		oldNames = new ArrayList<>();
		setStatusLineAboveButtons(true);
		this.setHelpAvailable(false);
	}
	public void addOldName(String oldName){
		if (!oldNames.contains(oldName))
			oldNames.add(oldName);

	}
	public void setOldName(String oldName) {
		this.oldName = oldName;
		if (text!=null)
			text.setText(oldName);
		this.newName = oldName;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = layout.marginWidth = 9;
		container.setLayout(layout);

		GridData gd = new GridData(GridData.FILL_BOTH);
		container.setLayoutData(gd);

		Label label = new Label(container, SWT.NULL);
		label.setText(Messages.RenameDialog_label);

		text = new Text(container, SWT.SINGLE|SWT.BORDER);
		text.addModifyListener(e -> textChanged(text.getText(), true));
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 200;
		text.setLayoutData(gd);
		applyDialogFont(container);
		return container;
	}

	@Override
	public int open() {
		text.setText(oldName);
		text.selectAll();
		setOkStatus();
		textChanged(oldName, false);
		return super.open();
	}

	private void textChanged(String text, boolean setStatus) {
		Button okButton = getButton(IDialogConstants.OK_ID);
		for (int i=0; i<oldNames.size(); i++){
			if((isCaseSensitive && text.equals(oldNames.get(i))) ||
					(!isCaseSensitive && text.equalsIgnoreCase(oldNames.get(i).toString()))){
				if (setStatus) {
					setErrorStatus(Messages.RenameDialog_validationError);
				}
				okButton.setEnabled(false);
				return;
			}
		}
		if (text.length() == 0 ) {
			if (setStatus) {
				setErrorStatus(Messages.RenameDialog_emptyName);
			}
			okButton.setEnabled(false);
			return;
		}
		okButton.setEnabled(true);
		if (setStatus) {
			setOkStatus();
		}
	}

	private void setErrorStatus(String errorMessage) {
		status =  new Status(
			IStatus.ERROR,
			HelpUIPlugin.PLUGIN_ID,
			IStatus.ERROR,
			errorMessage,
			null);
		updateStatus(status);
	}

	private void setOkStatus() {
		status = new Status(
			IStatus.OK,
			HelpUIPlugin.PLUGIN_ID,
			IStatus.OK,
			"", //$NON-NLS-1$
			null);
		updateStatus(status);
	}

	public String getNewName() {
		return newName;
	}

	@Override
	protected void okPressed() {
		newName = text.getText();
		super.okPressed();
	}

	@Override
	protected void computeResult() {
	}

	@Override
	public void setTitle(String title) {
		getShell().setText(title);
	}
}
