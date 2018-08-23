/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.repo;

import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.IHelpContextIds;
import org.eclipse.ui.PlatformUI;

public class RepositoriesFilterDialog extends TrayDialog {

	private RepositoriesFilter filter;

	private Button fShowModules;

	public RepositoriesFilterDialog(Shell shell) {
		super(shell);
	}

	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(CVSUIMessages.RepositoryFilterDialog_title);
	}

	protected Control createDialogArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		composite.setLayout(layout);

		fShowModules = new Button(composite, SWT.CHECK);
		fShowModules.setText(CVSUIMessages.RepositoryFilterDialog_showModules);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		fShowModules.setLayoutData(data);
		fShowModules.setSelection(true);

		initializeValues();

		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IHelpContextIds.REPOSITORY_FILTER_DIALOG);
		Dialog.applyDialogFont(parent);
		return composite;
	}

	void initializeValues() {
		if (filter == null)
			return;
		fShowModules.setSelection(filter.isShowModules());
	}

	/**
	 * A button has been pressed.  Process the dialog contents.
	 */
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.CANCEL_ID == buttonId) {
			super.buttonPressed(buttonId);
			return;
		}

		//create the filter
		filter = new RepositoriesFilter(fShowModules.getSelection());

		super.buttonPressed(buttonId);
	}

	/**
	 * Returns the filter that was created from the provided
	 * user input.
	 */
	public RepositoriesFilter getFilter() {
		return filter;
	}

	/**
	 * Set the initial value of the dialog to the given filter.
	 */
	public void setFilter(RepositoriesFilter filter) {
		this.filter = filter;
	}

}