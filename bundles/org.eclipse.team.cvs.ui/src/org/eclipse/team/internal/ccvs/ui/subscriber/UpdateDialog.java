/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.subscriber;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;

/**
 * This dialog prompts for the type of update which should take place
 * (i.e. update of auto-mergable files or update of all ignore local
 * changes.
 */
public class UpdateDialog extends SyncInfoSetDetailsDialog {

	public static final int YES = IDialogConstants.YES_ID;
	
	public UpdateDialog(Shell parentShell, SyncInfoSet syncSet) {
		super(parentShell, CVSUIMessages.UpdateDialog_overwriteTitle, syncSet); // 
	}

	@Override
	protected void createMainDialogArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		
		// TODO: set F1 help
		//WorkbenchHelp.setHelp(composite, IHelpContextIds.ADD_TO_VERSION_CONTROL_DIALOG);
		
		createWrappingLabel(composite, CVSUIMessages.UpdateDialog_overwriteMessage); 
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, YES, IDialogConstants.YES_LABEL, false);
		createButton(parent, IDialogConstants.NO_ID, IDialogConstants.NO_LABEL, true);
		super.createButtonsForButtonBar(parent);
	}
	
	@Override
	protected boolean includeOkButton() {
		return false;
	}
	
	@Override
	protected boolean includeCancelButton() {
		return false;
	}

	@Override
	protected void buttonPressed(int id) {
		// hijack yes and no buttons to set the correct return
		// codes.
		if(id == YES || id == IDialogConstants.NO_ID) {
			setReturnCode(id);
			filterSyncSet();
			close();
		} else {
			super.buttonPressed(id);
		}
	}
}
