/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize.patch;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.team.internal.ui.TeamUIMessages;

public class PatchParsedPage extends WizardPage {

	public final static String PATCH_PARSED_PAGE_NAME = "PatchParsedPage"; //$NON-NLS-1$
	private Label messageLabel;

	public PatchParsedPage() {
		super(PATCH_PARSED_PAGE_NAME, TeamUIMessages.PatchParsedPage_title,
				null);
		setDescription(TeamUIMessages.PatchParsedPage_description);
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL
				| GridData.HORIZONTAL_ALIGN_FILL));
		setControl(composite);

		initializeDialogUnits(parent);

		messageLabel = new Label(composite, SWT.NONE);
		messageLabel
				.setText(TeamUIMessages.PatchParsedPage_clickFinishToGoToSynchronizeView);
		setPageComplete(true);
	}

}
