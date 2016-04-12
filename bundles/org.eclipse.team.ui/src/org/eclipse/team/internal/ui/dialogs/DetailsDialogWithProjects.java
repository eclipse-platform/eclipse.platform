/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.eclipse.swt.widgets.*;

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

	/**
	 * @see DetailsDialog#createMainDialogArea(Composite)
	 */
	@Override
	protected void createMainDialogArea(Composite composite) {
		Label label = new Label(composite, SWT.WRAP);
		label.setText(message);
		GridData data = new GridData(SWT.FILL, SWT.FILL | SWT.CENTER, true, false);
		data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
		label.setLayoutData(data);
		updateEnablements();
	}

	/**
	 * @see DetailsDialog#createDropDownDialogArea(Composite)
	 */
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


		for (int i = 0; i < projects.length; i++) {
			detailsList.add(projects[i].getName());
		}
		return composite;
	}

	/**
	 * @see DetailsDialog#updateEnablements()
	 */
	@Override
	protected void updateEnablements() {
		setPageComplete(true);
	}

	/**
	 * @see DetailsDialog#includeCancelButton()
	 */
	@Override
	protected boolean includeCancelButton() {
		return includeCancelButton;
	}

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.dialogs.DetailsDialog#isMainGrabVertical()
     * @since 3.4
     */
    @Override
	protected boolean isMainGrabVertical() {
        return false;
    }

}
