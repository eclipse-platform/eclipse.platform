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
package org.eclipse.debug.internal.ui.views.console;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.actions.RemoveAllTerminatedAction;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IUpdate;

/**
 * ConsoleRemoveAllTerminatedAction
 */
public class ConsoleRemoveAllTerminatedAction extends Action implements IUpdate, ILaunchesListener2 {

	public void dispose() {
		DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(this);
	}

	@Override
	public synchronized void update() {
		ILaunch[] launches = DebugPlugin.getDefault().getLaunchManager().getLaunches();
		for (ILaunch launch : launches) {
			if (launch.isTerminated()) {
				setEnabled(true);
				return;
			}
		}
		setEnabled(false);

	}

	@Override
	public void run() {
		ILaunch[] launches = DebugPlugin.getDefault().getLaunchManager().getLaunches();
		RemoveAllTerminatedAction.removeTerminatedLaunches(launches);
	}

	public ConsoleRemoveAllTerminatedAction() {
		super(ConsoleMessages.ConsoleRemoveAllTerminatedAction_0);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IDebugHelpContextIds.CONSOLE_REMOVE_ALL_TERMINATED);
		setToolTipText(ConsoleMessages.ConsoleRemoveAllTerminatedAction_1);
		setImageDescriptor(DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_LCL_REMOVE_ALL));
		setDisabledImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_REMOVE_ALL));
		setHoverImageDescriptor(DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_LCL_REMOVE_ALL));
		DebugPlugin.getDefault().getLaunchManager().addLaunchListener(this);
		update();
	}

	@Override
	public void launchesRemoved(ILaunch[] launches) {
		if (isEnabled()) {
			update();
		}
	}

	@Override
	public void launchesAdded(ILaunch[] launches) {
	}

	@Override
	public void launchesChanged(ILaunch[] launches) {
	}

	@Override
	public void launchesTerminated(ILaunch[] launches) {
		update();
	}
}
