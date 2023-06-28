/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.launchConfigurations;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.swt.widgets.Shell;

/**
 * This class provides a mechanism to prompt users in the UI thread from debug.core in the case where
 * a launch delegate has gone missing and a new choice needs to be made in the launch dialog.
 *
 * @since 3.3
 */
public class LaunchDelegateNotAvailableHandler implements IStatusHandler {

	/**
	 * @see org.eclipse.debug.core.IStatusHandler#handleStatus(org.eclipse.core.runtime.IStatus, java.lang.Object)
	 */
	@Override
	public Object handleStatus(IStatus status, Object source) throws CoreException {
		if(source instanceof Object[]) {
			Object[] infos = (Object[]) source;
			if(infos.length == 2) {
				final ILaunchConfiguration config = (ILaunchConfiguration) infos[0];
				final String mode = (String) infos[1];
				final Shell shell = DebugUIPlugin.getShell();
				Runnable runnable = () -> {
					ILaunchGroup group = DebugUITools.getLaunchGroup(config, mode);
					if (group != null) {
						DebugUITools.openLaunchConfigurationDialog(shell, config, group.getIdentifier(), null);
					}
				};
				DebugUIPlugin.getStandardDisplay().asyncExec(runnable);
			}
		}
		return Status.OK_STATUS;
	}
}
