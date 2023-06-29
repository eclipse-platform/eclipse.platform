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
package org.eclipse.debug.internal.ui.actions;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.jface.action.IAction;

/**
 * Removes all terminated/detached launches from the
 * active debug view.
 */
public class RemoveAllTerminatedAction extends AbstractRemoveAllActionDelegate implements ILaunchesListener2 {

	@Override
	protected boolean isEnabled() {
		ILaunch[] launches = DebugPlugin.getDefault().getLaunchManager().getLaunches();
		if (launches != null) {
			for (ILaunch launch : launches) {
				if (launch.isTerminated()) {
					return true;
				}
			}
		}
		return false;
	}

	public static void removeTerminatedLaunches(ILaunch[] elements) {
		List<ILaunch> removed = new ArrayList<>();
		for (ILaunch launch : elements) {
			if (launch.isTerminated()) {
				removed.add(launch);
			}
		}
		if (!removed.isEmpty()) {
			ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
			manager.removeLaunches(removed.toArray(new ILaunch[removed.size()]));
		}
	}

	@Override
	protected void initialize() {
		DebugPlugin.getDefault().getLaunchManager().addLaunchListener(this);
	}

	@Override
	public void dispose() {
		super.dispose();
		DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(this);
	}

	@Override
	public void launchesAdded(ILaunch[] launches) {
	}

	@Override
	public void launchesChanged(ILaunch[] launches) {
	}

	@Override
	public void launchesRemoved(ILaunch[] launches) {
		IAction action = getAction();
		if (action != null) {
			if (action.isEnabled()) {
				update();
			}
		}
	}

	@Override
	public void launchesTerminated(ILaunch[] launches) {
		update();
	}

	@Override
	public void run(IAction action) {
		ILaunch[] launches = DebugPlugin.getDefault().getLaunchManager().getLaunches();
		removeTerminatedLaunches(launches);
	}
}

