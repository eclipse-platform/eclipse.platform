/*******************************************************************************
 * Copyright (c) 2021, 2025 Fabrizio Iannetti and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 * Fabrizio Iannetti - initial API and implementation
 * Alexander Fedorov (ArSysOp) - further evolution
 *******************************************************************************/

package org.eclipse.terminal.view.ui.internal.actions;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.terminal.connector.TerminalState;
import org.eclipse.terminal.control.ITerminalViewControl;
import org.eclipse.terminal.view.ui.internal.Messages;
import org.eclipse.terminal.view.ui.internal.tabs.TabFolderManager;

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
		if (target == null) {
			return;
		}
		target.setInvertedColors(!target.isInvertedColors());
	}

	@Override
	public void updateAction(boolean aboutToShow) {
		setEnabled(aboutToShow && getTarget() != null && getTarget().getState() == TerminalState.CONNECTED);
		setChecked(aboutToShow && getTarget() != null && getTarget().isInvertedColors());
	}

}
