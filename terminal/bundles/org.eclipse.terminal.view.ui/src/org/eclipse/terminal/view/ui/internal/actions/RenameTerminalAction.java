/*******************************************************************************
 * Copyright (c) 2021, 2025 Kichwa Coders Canada Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 * Kichwa Coders Canada Inc. - initial API and implementation
 * Alexander Fedorov (ArSysOp) - further evolution
 *******************************************************************************/

package org.eclipse.terminal.view.ui.internal.actions;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.terminal.control.ITerminalViewControl;
import org.eclipse.terminal.control.TerminalTitleRequestor;
import org.eclipse.terminal.view.ui.internal.Messages;
import org.eclipse.terminal.view.ui.internal.tabs.TabFolderManager;

/**
 * @since 4.8
 */
public class RenameTerminalAction extends AbstractTerminalAction {

	/**
	 * Constructor.
	 *
	 * @param tabFolderManager The parent tab folder manager. Must not be <code>null</code>.
	 */
	public RenameTerminalAction(TabFolderManager tabFolderManager) {
		super(RenameTerminalAction.class.getName());

		Assert.isNotNull(tabFolderManager);
		setupAction(Messages.RenameTerminalAction_menu, Messages.RenameTerminalAction_tooltip, (ImageDescriptor) null,
				(ImageDescriptor) null, (ImageDescriptor) null, true);
	}

	@Override
	public void run() {
		ITerminalViewControl target = getTarget();
		if (target == null) {
			return;
		}
		InputDialog inputDialog = new InputDialog(target.getControl().getShell(), //
				Messages.RenameTerminalAction_inputdialog_title, //
				Messages.RenameTerminalAction_inputdialog_prompt, //
				Messages.RenameTerminalAction_inputdialog_defaulttext, //
				null);
		if (inputDialog.open() == Window.OK) {
			String value = inputDialog.getValue();
			if (value != null) {
				target.setTerminalTitle(value, TerminalTitleRequestor.MENU);
			}
		}

	}

	@Override
	public void updateAction(boolean aboutToShow) {
		setEnabled(aboutToShow && getTarget() != null);
	}

}
