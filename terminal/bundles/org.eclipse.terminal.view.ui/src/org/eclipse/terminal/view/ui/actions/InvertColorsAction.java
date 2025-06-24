/*******************************************************************************
 * Copyright (c) 2021 Fabrizio Iannetti.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.terminal.view.ui.actions;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.terminal.internal.control.ITerminalViewControl;
import org.eclipse.terminal.internal.control.actions.AbstractTerminalAction;
import org.eclipse.terminal.internal.provisional.api.TerminalState;
import org.eclipse.terminal.view.ui.nls.Messages;
import org.eclipse.terminal.view.ui.tabs.TabFolderManager;

/**
 * @since 4.8
 */
public class InvertColorsAction extends AbstractTerminalAction {

	/**
	 * Constructor.
	 *
	 * @param tabFolderManager The parent tab folder manager. Must not be <code>null</code>.
	 */
	public InvertColorsAction(TabFolderManager tabFolderManager) {
		super(null, InvertColorsAction.class.getName(), IAction.AS_CHECK_BOX);

		Assert.isNotNull(tabFolderManager);
		setupAction(Messages.InvertColorsAction_menu, Messages.InvertColorsAction_tooltip, (ImageDescriptor) null,
				(ImageDescriptor) null, (ImageDescriptor) null, true);
	}

	@Override
	public void run() {
		ITerminalViewControl target = getTarget();
		if (target == null)
			return;
		target.setInvertedColors(!target.isInvertedColors());
	}

	@Override
	public void updateAction(boolean aboutToShow) {
		setEnabled(aboutToShow && getTarget() != null && getTarget().getState() == TerminalState.CONNECTED);
		setChecked(aboutToShow && getTarget() != null && getTarget().isInvertedColors());
	}

}
