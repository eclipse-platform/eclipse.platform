/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package org.eclipse.debug.ui;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.internal.ui.DebugUIMessages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * Provides warning prompt dialog
 *
 * @since 3.19
 */
public class SkipBreakpointsWarning extends Dialog {

	protected SkipBreakpointsWarning(Shell parentShell) {
		super(parentShell);
	}

	Button disableWarning;

	Button disableSkipAllBreakpoints;

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(DebugUIMessages.skipBreakpointWarningTitle);
	}
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout layout = new GridLayout(1, false);
		layout.marginTop = 10;
		layout.marginLeft = 10;
		layout.marginRight = 10;
		layout.verticalSpacing = 10;
		container.setLayout(layout);

		Label messageLabel = new Label(container, SWT.WRAP);
		messageLabel.setText(DebugUIMessages.skipBreakpointWarningLabel);
		GridData messageData = new GridData(SWT.FILL, SWT.CENTER, false, false);
		messageData.widthHint = 410;
		messageLabel.setLayoutData(messageData);

		disableWarning = new Button(container, SWT.CHECK);
		disableWarning.setText(DebugUIMessages.skipBreakpointWarningToggle1);

		GridData data = new GridData(SWT.NONE);
		data.verticalIndent = 20;
		data.horizontalAlignment = GridData.BEGINNING;
		disableWarning.setLayoutData(data);

		disableSkipAllBreakpoints = new Button(container, SWT.CHECK);
		disableSkipAllBreakpoints.setText(DebugUIMessages.skipBreakpointWarningToggle2);
		disableSkipAllBreakpoints.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		return container;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.OK_ID == buttonId) {
			if (disableWarning.getSelection()) {
				DebugUIPlugin.getDefault().getPreferenceStore()
						.setValue(IInternalDebugUIConstants.PREF_SKIP_ALL_BREAKPOINTS_PROMPT, false);
			}
			if (disableSkipAllBreakpoints.getSelection()) {
				DebugPlugin.getDefault().getBreakpointManager().setEnabled(true);
			}
			okPressed();
		} else if (IDialogConstants.CANCEL_ID == buttonId) {
			cancelPressed();
		}
	}

	@Override
	protected Point getInitialSize() {
		return new Point(450, 250);
	}

	@Override
	public boolean close() {
		disableWarning.dispose();
		disableSkipAllBreakpoints.dispose();
		return super.close();
	}

}
