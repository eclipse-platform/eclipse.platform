/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.ui.internal.console;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.texteditor.IUpdate;

/**
 * Pins the currently visible console in a console view.
 */
public class PinConsoleAction extends Action implements IUpdate {

	private IConsoleView fView = null;

	/**
	 * Constructs a 'pin console' action.
	 *
	 * @param view the view to pin with this action
	 */
	public PinConsoleAction(IConsoleView view) {
		super(ConsoleMessages.PinConsoleAction_0, IAction.AS_CHECK_BOX);
		setToolTipText(ConsoleMessages.PinConsoleAction_1);
		setImageDescriptor(ConsolePluginImages.getImageDescriptor(IInternalConsoleConstants.IMG_ELCL_PIN));
		setDisabledImageDescriptor(ConsolePluginImages.getImageDescriptor(IInternalConsoleConstants.IMG_DLCL_PIN));
		setHoverImageDescriptor(ConsolePluginImages.getImageDescriptor(IInternalConsoleConstants.IMG_LCL_PIN));
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IConsoleHelpContextIds.CONSOLE_PIN_CONSOLE_ACITON);
		fView = view;
		update();
	}

	@Override
	public void run() {
		fView.setPinned(isChecked());
	}

	@Override
	public void update() {
		setEnabled(fView.getConsole() != null);
		setChecked(fView.isPinned());
	}
}
