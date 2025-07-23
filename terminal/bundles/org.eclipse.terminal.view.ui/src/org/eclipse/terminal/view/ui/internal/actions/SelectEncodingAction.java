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

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.terminal.connector.TerminalState;
import org.eclipse.terminal.control.ITerminalViewControl;
import org.eclipse.terminal.view.ui.internal.Messages;
import org.eclipse.terminal.view.ui.internal.dialogs.EncodingSelectionDialog;
import org.eclipse.terminal.view.ui.internal.tabs.TabFolderManager;

/**
 * Terminal control select encoding action implementation.
 */
public class SelectEncodingAction extends AbstractTerminalAction {
	// Reference to the parent tab folder manager
	private final TabFolderManager tabFolderManager;

	/**
	 * Constructor.
	 *
	 * @param tabFolderManager The parent tab folder manager. Must not be <code>null</code>.
	 */
	public SelectEncodingAction(TabFolderManager tabFolderManager) {
		super(null, SelectEncodingAction.class.getName(), IAction.AS_PUSH_BUTTON);

		Assert.isNotNull(tabFolderManager);
		this.tabFolderManager = tabFolderManager;

		setupAction(Messages.SelectEncodingAction_menu, Messages.SelectEncodingAction_tooltip, (ImageDescriptor) null,
				(ImageDescriptor) null, (ImageDescriptor) null, true);
	}

	@Override
	public void run() {
		ITerminalViewControl target = getTarget();
		if (target == null) {
			return;
		}

		EncodingSelectionDialog dialog = new EncodingSelectionDialog(null);
		dialog.setCharset(target.getCharset());
		if (dialog.open() == Window.OK) {
			target.setCharset(dialog.getCharset());
			tabFolderManager.updateStatusLine();
		}
	}

	@Override
	public void updateAction(boolean aboutToShow) {
		setEnabled(aboutToShow && getTarget() != null && getTarget().getState() == TerminalState.CONNECTED);
	}

}
