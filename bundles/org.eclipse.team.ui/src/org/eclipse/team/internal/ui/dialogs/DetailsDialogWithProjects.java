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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.dialogs;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * Display a message with a details that can contain a list of projects
 */
public class DetailsDialogWithProjects extends DetailsDialog {

	private String message;
	private String detailsTitle;
	private IProject[] projects;
	private org.eclipse.swt.widgets.List detailsList;

	private boolean includeCancelButton;

	/**
	 * Constructor for DetailsDialogWithProjects.
	 *
	 * @param parentShell the parent shell
	 * @param dialogTitle the dialog title
	 * @param dialogMessage the dialog message
	 * @param detailsTitle the details title
	 * @param projects the <code>IProject</code>s
	 * @param includeCancelButton <code>true</code> if the 'Cancel' button should be shown
	 * @param imageKey the image key (one of the image constants on Dialog)
	 */
	public DetailsDialogWithProjects(Shell parentShell, String dialogTitle, String dialogMessage, String detailsTitle, IProject[] projects, boolean includeCancelButton, String imageKey) {
		super(parentShell, dialogTitle);
		setImageKey(imageKey);
		this.message = dialogMessage;
		this.detailsTitle = detailsTitle;
		this.projects = projects;
		this.includeCancelButton = includeCancelButton;
	}

	@Override
	protected void createMainDialogArea(Composite composite) {
		Label label = new Label(composite, SWT.WRAP);
		label.setText(message);
		GridData data = new GridData(SWT.FILL, SWT.FILL | SWT.CENTER, true, false);
		data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
		label.setLayoutData(data);
		updateEnablements();
	}

	@Override
	protected Composite createDropDownDialogArea(Composite parent) {
		// create a composite with standard margins and spacing
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		if (detailsTitle != null) {
			Label title = new Label(composite, SWT.WRAP);
			title.setText(detailsTitle);
			title.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		}

		detailsList = new org.eclipse.swt.widgets.List(composite, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		GridData data = new GridData (SWT.FILL, SWT.FILL, true, true);
		data.heightHint = convertHeightInCharsToPixels(5);
		detailsList.setLayoutData(data);


		for (IProject project : projects) {
			detailsList.add(project.getName());
		}
		return composite;
	}

	@Override
	protected void updateEnablements() {
		setPageComplete(true);
	}

	@Override
	protected boolean includeCancelButton() {
		return includeCancelButton;
	}

	@Override
	protected boolean isMainGrabVertical() {
		return false;
	}

}
