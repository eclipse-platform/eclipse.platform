/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     CSC - Intial implementation
 *     IBM Corporation - ongoing maintenance
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ccvs.core.EditorsInfo;
import org.eclipse.ui.PlatformUI;

/**
 * 
 * A dialog for showing the result of a cvs editors command.
 * Currently not in use, but can be used before executing the edit command
 * 
 * @author <a href="mailto:gregor.kohlwes@csc.com,kohlwes@gmx.net">Gregor Kohlwes</a>
 */
public class EditorsDialog extends TrayDialog {
	/**
	 * Constructor EditorsDialog.
	 * @param shell
	 * @param iEditorsInfos
	 */
	
	EditorsView editorsView;
	EditorsInfo[] editorsInfo;
	
	public EditorsDialog(Shell shell, EditorsInfo[] infos) {
		super(shell);
		editorsInfo = infos;
	}

	protected Control createDialogArea(Composite container) {

		Composite parent = (Composite) super.createDialogArea(container);
						
		getShell().setText(CVSUIMessages.EditorsDialog_title); 
		createMessageArea(parent);
		editorsView = new EditorsView();
		editorsView.createPartControl(container);
		editorsView.setInput(editorsInfo);
		
		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IHelpContextIds.EDITORS_DIALOG);
		
		Dialog.applyDialogFont(parent);

		return parent;
	}
	/**
	 * Method createMessageArea.
	 * @param parent
	 */
	private void createMessageArea(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(CVSUIMessages.EditorsDialog_question); //		
	}
	
}
