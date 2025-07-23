/*******************************************************************************
 * Copyright (c) 2012, 2025 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 * Alexander Fedorov (ArSysOp) - further evolution
 *******************************************************************************/
package org.eclipse.terminal.view.ui.internal.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.terminal.connector.TerminalState;
import org.eclipse.terminal.control.ITerminalViewControl;
import org.eclipse.terminal.view.ui.ITerminalsView;
import org.eclipse.terminal.view.ui.internal.ImageConsts;
import org.eclipse.terminal.view.ui.internal.Messages;
import org.eclipse.terminal.view.ui.internal.UIPlugin;
import org.eclipse.terminal.view.ui.internal.tabs.TabCommandFieldHandler;
import org.eclipse.terminal.view.ui.internal.tabs.TabFolderManager;

/**
 * Toggle command input field.
 */
public class ToggleCommandFieldAction extends AbstractTerminalAction {
	private ITerminalsView view = null;

	/**
	 * Constructor.
	 */
	public ToggleCommandFieldAction(ITerminalsView view) {
		super(null, ToggleCommandFieldAction.class.getName(), IAction.AS_CHECK_BOX);

		this.view = view;
		setupAction(Messages.ToggleCommandFieldAction_menu, Messages.ToggleCommandFieldAction_toolTip, null,
				UIPlugin.getImageDescriptor(ImageConsts.ACTION_ToggleCommandField_Enabled),
				UIPlugin.getImageDescriptor(ImageConsts.ACTION_ToggleCommandField_Disabled), true);

		TabCommandFieldHandler handler = getCommandFieldHandler();
		setChecked(handler != null && handler.hasCommandInputField());
	}

	@Override
	public void run() {
		TabCommandFieldHandler handler = getCommandFieldHandler();
		if (handler != null) {
			handler.setCommandInputField(!handler.hasCommandInputField());
		}
		setChecked(handler != null && handler.hasCommandInputField());
	}

	@Override
	public void updateAction(boolean aboutToShow) {
		TabCommandFieldHandler handler = getCommandFieldHandler();
		ITerminalViewControl target = getTarget();
		setEnabled(aboutToShow && handler != null && target != null && target.getState() == TerminalState.CONNECTED);
		setChecked(handler != null && handler.hasCommandInputField());
	}

	/**
	 * Returns the command input field handler for the active tab.
	 *
	 * @return The command input field handler or <code>null</code>.
	 */
	protected TabCommandFieldHandler getCommandFieldHandler() {
		TabCommandFieldHandler handler = null;
		// Get the active tab item from the tab folder manager
		TabFolderManager manager = view.getAdapter(TabFolderManager.class);
		if (manager != null) {
			// If we have the active tab item, we can get the active terminal control
			CTabItem activeTabItem = manager.getActiveTabItem();
			if (activeTabItem != null && !activeTabItem.isDisposed()) {
				handler = manager.getTabCommandFieldHandler(activeTabItem);
			}
		}
		return handler;
	}
}
