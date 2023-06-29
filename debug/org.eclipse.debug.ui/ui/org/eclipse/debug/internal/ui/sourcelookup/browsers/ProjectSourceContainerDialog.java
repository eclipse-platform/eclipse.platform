/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.sourcelookup.browsers;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.sourcelookup.SourceLookupUIMessages;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListSelectionDialog;

/**
 * The dialog for selecting the project for which a source container will be created.
 *
 * @since 3.0
 */
public class ProjectSourceContainerDialog extends ListSelectionDialog {

	private boolean fAddRequiredProjects = false;

	public ProjectSourceContainerDialog(
			Shell parentShell,
			Object input,
			IStructuredContentProvider contentProvider,
			ILabelProvider labelProvider,
			String message) {
		super(parentShell, input, contentProvider, labelProvider, message);
	}


	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Font font = parent.getFont();

		Composite composite = (Composite)super.createDialogArea(parent);

		final Button addRequired = new Button(composite, SWT.CHECK);
		addRequired.setText(SourceLookupUIMessages.projectSelection_requiredLabel);
		addRequired.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fAddRequiredProjects = addRequired.getSelection();
			}
		});
		addRequired.setSelection(fAddRequiredProjects);
		addRequired.setFont(font);

		applyDialogFont(composite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getShell(),  IDebugHelpContextIds.ADD_PROJECT_CONTAINER_DIALOG);
		return composite;
	}


	/**
	 * Returns whether the user has selected to add required projects.
	 *
	 * @return whether the user has selected to add required projects
	 */
	public boolean isAddRequiredProjects() {
		return fAddRequiredProjects;
	}
}
