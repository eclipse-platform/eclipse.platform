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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.memory.renderings;

import java.math.BigInteger;

import org.eclipse.debug.internal.ui.DebugUIMessages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

public class GoToAddressComposite {

	private Text fExpression;
	private Button fOKButton;
	private Button fCancelButton;
	private Composite fComposite;
	private Combo fGoToCombo;
	private Button fHexButton;

	/**
	 * @param parent
	 * @return
	 */
	public Control createControl(Composite parent)
	{
		fComposite = new Composite(parent, SWT.NONE);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(fComposite, DebugUIPlugin.getUniqueIdentifier() + ".GoToAddressComposite_context"); //$NON-NLS-1$
		GridLayout layout = new GridLayout();
		layout.numColumns = 6;
		layout.makeColumnsEqualWidth = false;
		layout.marginHeight = 0;
		layout.marginLeft = 0;
		fComposite.setLayout(layout);

		fGoToCombo = new Combo(fComposite, SWT.READ_ONLY);
		fGoToCombo.add(DebugUIMessages.GoToAddressComposite_0);
		fGoToCombo.add(DebugUIMessages.GoToAddressComposite_4);
		fGoToCombo.add(DebugUIMessages.GoToAddressComposite_5);
		fGoToCombo.select(0);

		fExpression = new Text(fComposite, SWT.SINGLE | SWT.BORDER);
		fExpression.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fHexButton = new Button(fComposite, SWT.CHECK);
		fHexButton.setText(DebugUIMessages.GoToAddressComposite_6);
		fHexButton.setSelection(true);

		fOKButton = new Button(fComposite, SWT.NONE);
		fOKButton.setText(DebugUIMessages.GoToAddressComposite_1);

		fCancelButton = new Button(fComposite, SWT.NONE);
		fCancelButton.setText(DebugUIMessages.GoToAddressComposite_2);

		return fComposite;
	}

	public int getHeight()
	{
		int height = fComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		return height;
	}

	public Button getButton(int id)
	{
		if (id == IDialogConstants.OK_ID)
			return fOKButton;
		else if (id == IDialogConstants.CANCEL_ID)
			return fCancelButton;
		return null;
	}

	public String getExpressionText()
	{
		return fExpression.getText();
	}

	public Text getExpressionWidget()
	{
		return fExpression;
	}

	public boolean isGoToAddress()
	{
		return fGoToCombo.getSelectionIndex() == 0;
	}

	public boolean isOffset()
	{
		return fGoToCombo.getSelectionIndex() == 1;
	}

	public boolean isJump()
	{
		return fGoToCombo.getSelectionIndex() == 2;
	}

	public boolean isHex()
	{
		return fHexButton.getSelection();
	}

	public BigInteger getGoToAddress(BigInteger baseAddress, BigInteger selectedAddress) throws NumberFormatException
	{
		boolean add = true;
		String expression = getExpressionText();
		boolean hex = isHex();
		int radix = hex?16:10;

		expression = expression.trim();

		if (isGoToAddress())
		{
			expression = expression.toUpperCase();
			if (expression.startsWith("0X")) //$NON-NLS-1$
			{
				expression = expression.substring(2);
				radix = 16;
			}

			return new BigInteger(expression, radix);
		}

		if (expression.startsWith("+")) //$NON-NLS-1$
		{
			expression = expression.substring(1);
		}
		else if (expression.startsWith("-")) //$NON-NLS-1$
		{
			expression = expression.substring(1);
			add = false;
		}

		expression = expression.toUpperCase();
		if (expression.startsWith("0X")) //$NON-NLS-1$
		{
			expression = expression.substring(2);
			radix = 16;
		}

		BigInteger gotoAddress = new BigInteger(expression, radix);

		BigInteger address = baseAddress;
		if (isJump())
			address = selectedAddress;

		if (address == null)
			throw new NumberFormatException(DebugUIMessages.GoToAddressComposite_7);

		if (add)
			gotoAddress = address.add(gotoAddress);
		else
			gotoAddress = address.subtract(gotoAddress);

		return gotoAddress;
	}

}
