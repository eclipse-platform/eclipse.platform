/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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


import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

public class RelaunchActionDelegate extends AbstractDebugActionDelegate {

	@Override
	protected void doAction(Object object) {
		ILaunch launch = DebugUIPlugin.getLaunch(object);
		if (launch != null) {
			relaunch(launch.getLaunchConfiguration(), launch.getLaunchMode(), isShift());
		}
	}

	/**
	 * Re-launches the given configuration in the specified mode.
	 *
	 */
	public static void relaunch(ILaunchConfiguration config, String mode) {
		DebugUITools.launch(config, mode);
	}

	/**
	 * Re-launches the given configuration in the specified mode after
	 * terminating the previous if Preferred.
	 *
	 * @param isShift is Shift pressed (use <code>false</code> if no support for
	 *            Shift)
	 */
	public static void relaunch(ILaunchConfiguration config, String mode, boolean isShift) {
		DebugUITools.launch(config, mode, isShift);
	}

	@Override
	protected boolean isEnabledFor(Object element) {
		ILaunch launch= DebugUIPlugin.getLaunch(element);
		return launch != null && launch.getLaunchConfiguration() != null && LaunchConfigurationManager.isVisible(launch.getLaunchConfiguration());
	}

	@Override
	protected IStructuredSelection getTargetSelection(IStructuredSelection s) {
		if (s.isEmpty()) {
			return s;
		}
		Set<ILaunch> dups = new LinkedHashSet<>();
		Iterator<?> iterator = s.iterator();
		while (iterator.hasNext()) {
			Object object = iterator.next();
			ILaunch launch = DebugUIPlugin.getLaunch(object);
			if (launch == null) {
				return s;
			}
			dups.add(launch);
		}
		return new StructuredSelection(dups.toArray());
	}

	@Override
	protected String getErrorDialogMessage() {
		return ActionMessages.RelaunchActionDelegate_Launch_Failed_1;
	}

	@Override
	protected String getStatusMessage() {
		return ActionMessages.RelaunchActionDelegate_An_exception_occurred_while_launching_2;
	}
}
