/*******************************************************************************
 * Copyright (c) 2004, 2013 IBM Corporation and others.
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

package org.eclipse.debug.internal.ui.views.memory.renderings;

import java.util.Vector;

import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.DebugUIMessages;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * @since 3.0
 */

public class GoToAddressDialog extends TrayDialog implements ModifyListener {

	private static Vector<String> history = new Vector<>();
	private Combo expressionInput;
	private String expression;

	/**
	 * @param parentShell
	 */
	public GoToAddressDialog(Shell parentShell) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite comp = (Composite) super.createDialogArea(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IDebugUIConstants.PLUGIN_ID + ".GoToAddressDialog_context"); //$NON-NLS-1$
		comp.setLayout(new GridLayout());
		GridData spec2 = new GridData();
		spec2.grabExcessVerticalSpace = true;
		spec2.grabExcessHorizontalSpace = true;
		spec2.horizontalAlignment = GridData.FILL;
		spec2.verticalAlignment = GridData.CENTER;
		comp.setLayoutData(spec2);

		Label textLabel = new Label(comp, SWT.NONE);
		textLabel.setText(DebugUIMessages.GoToAddressDialog_Address);
		GridData textLayout = new GridData();
		textLayout.widthHint = 280;
		textLabel.setLayoutData(textLayout);

		expressionInput = new Combo(comp, SWT.BORDER);
		GridData spec = new GridData();
		spec.grabExcessVerticalSpace = false;
		spec.grabExcessHorizontalSpace = true;
		spec.horizontalAlignment = GridData.FILL;
		spec.verticalAlignment = GridData.BEGINNING;
		spec.heightHint = 50;
		expressionInput.setLayoutData(spec);

		// add history
		String[] historyExpression = history.toArray(new String[history.size()]);
		for (String h : historyExpression) {
			expressionInput.add(h);
		}

		expressionInput.addModifyListener(this);

		return comp;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);

		newShell.setText(DebugUIMessages.GoToAddressDialog_GoToAddress);
	}

	public String getExpression() {
		return expression;
	}

	@Override
	protected void okPressed() {

		expression = expressionInput.getText();

		// add to history list
		if (!history.contains(expression)) {
			history.insertElementAt(expression, 0);
		}

		super.okPressed();
	}

	@Override
	public void modifyText(ModifyEvent e) {

		String input = expressionInput.getText();

		if (input == null || input.equals(IInternalDebugCoreConstants.EMPTY_STRING)) {
			getButton(IDialogConstants.OK_ID).setEnabled(false);
		} else {
			getButton(IDialogConstants.OK_ID).setEnabled(true);
		}

	}

	@Override
	protected Control createButtonBar(Composite parent) {

		Control ret = super.createButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setEnabled(false);

		return ret;
	}

}
