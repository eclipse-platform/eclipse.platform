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
package org.eclipse.ui.internal.console;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IConsoleView;

/**
 * Toggles console auto-scroll
 *
 * @since 3.1
 */
public class ScrollLockAction extends Action {

	private IConsoleView fConsoleView;

	public ScrollLockAction(IConsoleView consoleView) {
		super(ConsoleMessages.ScrollLockAction_0);
		fConsoleView = consoleView;

		setToolTipText(ConsoleMessages.ScrollLockAction_1);
		setHoverImageDescriptor(ConsolePluginImages.getImageDescriptor(IInternalConsoleConstants.IMG_LCL_LOCK));
		setDisabledImageDescriptor(ConsolePluginImages.getImageDescriptor(IInternalConsoleConstants.IMG_DLCL_LOCK));
		setImageDescriptor(ConsolePluginImages.getImageDescriptor(IInternalConsoleConstants.IMG_ELCL_LOCK));
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IConsoleHelpContextIds.CONSOLE_SCROLL_LOCK_ACTION);
		boolean checked = fConsoleView.getScrollLock();
		setChecked(checked);
	}

	/**
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	@Override
	public void run() {
		fConsoleView.setScrollLock(isChecked());
	}

	public void dispose() {
		fConsoleView = null;
	}

}

