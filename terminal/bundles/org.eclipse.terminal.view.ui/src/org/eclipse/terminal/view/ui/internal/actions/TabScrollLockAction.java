/*******************************************************************************
 * Copyright (c) 2011, 2025 Wind River Systems, Inc. and others. All rights reserved.
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
import org.eclipse.terminal.connector.TerminalState;
import org.eclipse.terminal.control.ITerminalViewControl;
import org.eclipse.terminal.view.ui.internal.ImageConsts;
import org.eclipse.terminal.view.ui.internal.Messages;
import org.eclipse.terminal.view.ui.internal.UIPlugin;

/**
 * Terminal console tab scroll lock action.
 */
public class TabScrollLockAction extends AbstractTerminalAction {

	/**
	 * Constructor.
	 */
	public TabScrollLockAction() {
		super(null, TabScrollLockAction.class.getName(), IAction.AS_RADIO_BUTTON);

		setupAction(Messages.TabScrollLockAction_text, Messages.TabScrollLockAction_tooltip, null,
				UIPlugin.getImageDescriptor(ImageConsts.ACTION_ScrollLock_Enabled),
				UIPlugin.getImageDescriptor(ImageConsts.ACTION_ScrollLock_Disabled), true);
	}

	@Override
	public void run() {
		ITerminalViewControl target = getTarget();
		if (target != null) {
			target.setScrollLock(!target.isScrollLock());
			setChecked(target.isScrollLock());
		}
	}

	@Override
	public void updateAction(boolean aboutToShow) {
		setEnabled(aboutToShow && getTarget() != null && getTarget().getState() == TerminalState.CONNECTED);
		setChecked(getTarget() != null && getTarget().isScrollLock());
	}

}
