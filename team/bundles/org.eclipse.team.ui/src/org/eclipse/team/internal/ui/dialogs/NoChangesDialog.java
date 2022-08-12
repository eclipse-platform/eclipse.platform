/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.mapping.ISynchronizationScope;

public class NoChangesDialog extends DetailsDialog {

	private String message;
	private Label messageLabel;
	private Label imageLabel;
	private ResourceMappingHierarchyArea selectedMappingsArea;
	private final ISynchronizationScope scope;
	private final String description;

	public NoChangesDialog(Shell parentShell, String dialogTitle, String message, String description, ISynchronizationScope scope) {
		super(parentShell, dialogTitle);
		this.message = message;
		this.description = description;
		this.scope = scope;
	}

	@Override
	protected void initializeStyle() {
		// Use the default dialog style
	}

	@Override
	protected Composite createDropDownDialogArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		selectedMappingsArea = ResourceMappingHierarchyArea.create(scope, null /* no context */);
		selectedMappingsArea.setDescription(description);
		selectedMappingsArea.createArea(composite);
		return composite;
	}

	@Override
	protected void createMainDialogArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 2;
		composite.setLayout(layout);
		createMessageArea(composite);
	}

	@Override
	protected void updateEnablements() {
		// Nothing to do
	}

	/*
	 * Code copied from IconandMessageDialog
	 */
	private Control createMessageArea(Composite composite) {
		// create composite
		// create image
		Image image = getSWTImage(SWT.ICON_INFORMATION);
		if (image != null) {
			imageLabel = new Label(composite, SWT.NULL);
			image.setBackground(imageLabel.getBackground());
			imageLabel.setImage(image);
			imageLabel.setLayoutData(new GridData(
					GridData.HORIZONTAL_ALIGN_CENTER
							| GridData.VERTICAL_ALIGN_BEGINNING));
		}
		// create message
		if (message != null) {
			messageLabel = new Label(composite, SWT.WRAP);
			messageLabel.setText(message);
			GridData data = new GridData(GridData.GRAB_HORIZONTAL
					| GridData.HORIZONTAL_ALIGN_FILL
					| GridData.VERTICAL_ALIGN_BEGINNING);
			data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
			messageLabel.setLayoutData(data);
		}
		return composite;
	}

	/*
	 * Code copied from IconandMessageDialog
	 */
	private Image getSWTImage(final int imageID) {
		Shell shell = getShell();
		final Display display;
		if (shell == null) {
			shell = getParentShell();
		}
		if (shell == null) {
			display = Display.getCurrent();
		} else {
			display = shell.getDisplay();
		}

		final Image[] image = new Image[1];
		display.syncExec(() -> image[0] = display.getSystemImage(imageID));

		return image[0];
	}

	@Override
	public boolean isHelpAvailable() {
		return false;
	}

	@Override
	protected boolean includeCancelButton() {
		return false;
	}

	public static void open(Shell shell, String title, String message, String description, ISynchronizationScope scope) {
		new NoChangesDialog(shell, title, message, description, scope).open();
	}

}
