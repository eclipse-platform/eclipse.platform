/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
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

import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchDelegate;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;

/**
 * This class provides a mechanism to prompt users in the UI thread from debug.core in the case where
 * duplicate launch delegates have been detected and a preferred delegate needs to be selected.
 *
 * As this handler is used once a launch has been started, and only prompts in the event that the launch <i>can</i>
 * continue with further input, it must be a blocking operation.
 *
 * @since 3.3
 */
public class DuplicateLaunchDelegatesStatusHandler implements IStatusHandler {

	/**
	 * @see org.eclipse.debug.core.IStatusHandler#handleStatus(org.eclipse.core.runtime.IStatus, java.lang.Object)
	 */
	@Override
	public Object handleStatus(IStatus status, Object source) throws CoreException {
		if(source instanceof Object[]) {
			Object[] infos = (Object[]) source;
			if(infos.length == 2) {
				ILaunchConfiguration config = (ILaunchConfiguration) infos[0];
				String mode = (String) infos[1];
				Shell shell = DebugUIPlugin.getShell();
				HashSet<String> modes = new HashSet<>();
				modes.add(mode);
				modes.addAll(config.getModes());
				SelectLaunchersDialog sldd = new SelectLaunchersDialog(shell,
						config.getType().getDelegates(modes),
						config.getWorkingCopy(),
						mode);
				if(sldd.open() != IDialogConstants.OK_ID) {
					return Status.CANCEL_STATUS;
				}
				//check that the delegate has been set
				ILaunchDelegate delegate = config.getPreferredDelegate(modes);
				if(delegate == null) {
					delegate = config.getType().getPreferredDelegate(modes);
				}
				return (delegate == null ? Status.CANCEL_STATUS : Status.OK_STATUS);
			}
		}
		return Status.CANCEL_STATUS;
	}
}
