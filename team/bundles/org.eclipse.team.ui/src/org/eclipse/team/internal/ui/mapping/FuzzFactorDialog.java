/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.mapping;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.internal.patch.WorkspacePatcher;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.ui.PlatformUI;

public class FuzzFactorDialog extends Dialog {

	private Text valueText;
	private Text errorMessageText;

	private WorkspacePatcher patcher;
	private int fuzzFactor;

	public FuzzFactorDialog(Shell parentShell, WorkspacePatcher patcher) {
		super(parentShell);
		this.patcher = patcher;
	}

	public int getFuzzFactor() {
		return fuzzFactor;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		applyDialogFont(composite);

		Label label = new Label(composite, SWT.WRAP);
		label.setText(TeamUIMessages.FuzzFactorDialog_message);
		GridData data = new GridData();
		data.horizontalSpan = 2;
		label.setLayoutData(data);
		label.setFont(parent.getFont());

		valueText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		data = new GridData(GridData.GRAB_HORIZONTAL
				| GridData.HORIZONTAL_ALIGN_FILL);
		data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
		valueText.setLayoutData(data);
		valueText.addModifyListener(e -> validateInput());
		fuzzFactor = patcher.getFuzz();
		if (fuzzFactor >= 0)
			valueText.setText(Integer.toString(fuzzFactor));

		Button guessButton = new Button(composite, SWT.NONE);
		guessButton.setText(TeamUIMessages.FuzzFactorDialog_guess);
		data = new GridData();
		int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		Point minSize = guessButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		data.widthHint = Math.max(widthHint, minSize.x);
		guessButton.setLayoutData(data);
		guessButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				patcher.setFuzz(-1);
				int fuzz = guessFuzzFactor();
				if (fuzz >= 0) {
					String value = Integer.toString(fuzz);
					valueText.setText(value);
				}
			}
		});

		errorMessageText = new Text(composite, SWT.READ_ONLY | SWT.WRAP);
		errorMessageText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
				| GridData.HORIZONTAL_ALIGN_FILL));
		errorMessageText.setBackground(errorMessageText.getDisplay()
				.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

		validateInput();

		applyDialogFont(composite);
		return composite;
	}

	private void validateInput() {
		String message = null;
		String value = valueText.getText();
		try {
			fuzzFactor = Integer.parseInt(value);
			if (fuzzFactor < 0)
				message = TeamUIMessages.FuzzFactorDialog_numberOutOfRange;
		} catch (NumberFormatException x) {
			message = TeamUIMessages.FuzzFactorDialog_notANumber;
		}
		setErrorMessage(message);
	}

	private void setErrorMessage(String errorMessage) {
		if (errorMessageText != null && !errorMessageText.isDisposed()) {
			errorMessageText.setText(errorMessage == null ? " \n " //$NON-NLS-1$
					: errorMessage);
			boolean hasError = errorMessage != null
					&& (StringConverter.removeWhiteSpaces(errorMessage))
							.length() > 0;
			errorMessageText.setEnabled(hasError);
			errorMessageText.setVisible(hasError);
			errorMessageText.getParent().update();
			Control button = getButton(IDialogConstants.OK_ID);
			if (button != null) {
				button.setEnabled(errorMessage == null);
			}
		}
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(TeamUIMessages.FuzzFactorDialog_title);
	}

	private int guessFuzzFactor() {
		final int[] result = new int[] { -1 };
		try {
			PlatformUI.getWorkbench().getProgressService().run(true, true,
					monitor -> result[0] = patcher.guessFuzzFactor(monitor));
		} catch (InvocationTargetException ex) {
			// NeedWork
		} catch (InterruptedException ex) {
			// NeedWork
		}
		return result[0];
	}

}
