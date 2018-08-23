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
package org.eclipse.team.internal.ccvs.ui.subscriber;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
/**
 * A class that adds tool-tips to the buttons of a standard message dialog.
 */
public class ToolTipMessageDialog extends MessageDialog {
	private String[] buttonToolTips;
	/**
	 * Same as the MessageDialog constructor, with the addition of a button tooltip
	 * argument.  The number of button tool tips must match the number of button labels.
	 */
	public ToolTipMessageDialog(Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage, int dialogImageType, String[] dialogButtonLabels, String[] buttonToolTips, int defaultIndex) {
		super(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, dialogButtonLabels, defaultIndex);
		this.buttonToolTips = buttonToolTips;
	}
	/**
	 * Method declared on MessageDialog.
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		if (buttonToolTips != null) {
			for (int i = 0; i < buttonToolTips.length; i++) {
				getButton(i).setToolTipText(buttonToolTips[i]);
			}
		}
	}
}
