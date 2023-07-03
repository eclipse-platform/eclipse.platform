/*******************************************************************************
 * Copyright (c) 2004, 2007 IBM Corporation and others.
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

package org.eclipse.debug.internal.ui.views.memory;

import org.eclipse.debug.core.model.IMemoryBlockRetrieval;
import org.eclipse.debug.core.model.IMemoryBlockRetrievalExtension;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.DebugUIMessages;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

/**
 * @since 3.0
 */
public class MonitorMemoryBlockDialog extends TrayDialog implements ModifyListener {

	private Combo expressionInput;
	private Text lengthInput;
	private String expression;
	private String length;
	private boolean needLength = true;
	private String fPrefillExp = null;
	private String fPrefillLength = null;

	/**
	 * the predefined width of the wrapping label for the expression to enter
	 * combo
	 *
	 * @since 3.3
	 */
	private static final int LABEL_WIDTH = 210;

	/**
	 * @param parentShell
	 */
	public MonitorMemoryBlockDialog(Shell parentShell, IMemoryBlockRetrieval memRetrieval, String prefillExp, String prefillLength) {
		super(parentShell);

		if (memRetrieval instanceof IMemoryBlockRetrievalExtension) {
			needLength = false;
		}

		fPrefillExp = prefillExp;
		fPrefillLength = prefillLength;
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite comp = (Composite) super.createDialogArea(parent);
		SWTFactory.createWrapLabel(comp, DebugUIMessages.MonitorMemoryBlockDialog_EnterExpressionToMonitor, 1, LABEL_WIDTH);
		expressionInput = SWTFactory.createCombo(comp, SWT.BORDER, 1, MemoryViewUtil.getHistory());
		if (fPrefillExp != null) {
			expressionInput.setText(fPrefillExp);
		}
		expressionInput.addModifyListener(this);

		if (needLength) {
			SWTFactory.createLabel(comp, DebugUIMessages.MonitorMemoryBlockDialog_NumberOfBytes, 1);
			lengthInput = SWTFactory.createSingleText(comp, 1);
			if (fPrefillLength != null) {
				lengthInput.setText(fPrefillLength);
			}
			lengthInput.addModifyListener(this);
		}
		PlatformUI.getWorkbench().getHelpSystem().setHelp(comp, IDebugUIConstants.PLUGIN_ID + ".MonitorMemoryBlockDialog_context"); //$NON-NLS-1$
		return comp;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);

		newShell.setText(DebugUIMessages.MonitorMemoryBlockDialog_MonitorMemory);
	}

	/**
	 * @return the entered expression
	 */
	public String getExpression() {
		return expression;
	}

	/**
	 * @return the entered length
	 */
	public String getLength() {
		return length;
	}

	@Override
	protected void okPressed() {

		expression = expressionInput.getText();

		// add to HISTORY list
		MemoryViewUtil.addHistory(expression);

		if (needLength) {
			length = lengthInput.getText();
		}

		super.okPressed();
	}

	@Override
	public void modifyText(ModifyEvent e) {
		updateOKButtonState();
	}

	private void updateOKButtonState() {
		if (needLength) {
			String lengthText = lengthInput.getText();
			String input = expressionInput.getText();

			if (input == null || input.equals(IInternalDebugCoreConstants.EMPTY_STRING) || lengthText == null || lengthText.equals(IInternalDebugCoreConstants.EMPTY_STRING)) {
				getButton(IDialogConstants.OK_ID).setEnabled(false);
			} else {
				getButton(IDialogConstants.OK_ID).setEnabled(true);
			}
		}
	}

	@Override
	protected Control createButtonBar(Composite parent) {

		Control ret = super.createButtonBar(parent);

		if (needLength) {
			updateOKButtonState();
		} else {
			// always enable the OK button if we only need the expression
			getButton(IDialogConstants.OK_ID).setEnabled(true);
		}

		return ret;
	}
}
