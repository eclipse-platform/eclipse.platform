/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Shell;


public class DebugModePromptStatusHandler implements IStatusHandler {

	@Override
	public Object handleStatus(IStatus status, Object source) throws CoreException {
		if (source instanceof ILaunchConfiguration) {
			ILaunchConfiguration config = (ILaunchConfiguration)source;
			if (DebugUITools.isPrivate(config)) {
				return Boolean.FALSE;
			}
		}

		IPreferenceStore store = DebugUIPlugin.getDefault().getPreferenceStore();
		ILaunchConfiguration configuration = (ILaunchConfiguration)source;
		String pref = store.getString(IInternalDebugUIConstants.PREF_RELAUNCH_IN_DEBUG_MODE);
		if (pref != null) {
			if (pref.equals(MessageDialogWithToggle.NEVER)) {
				return Boolean.FALSE;
			} else if (pref.equals(MessageDialogWithToggle.ALWAYS)) {
				relaunchInDebugMode(configuration);
				return Boolean.TRUE;
			}
		}

		Shell activeShell = DebugUIPlugin.getShell();
		String title = LaunchConfigurationsMessages.DebugModePromptStatusHandler_0;
		String message = LaunchConfigurationsMessages.DebugModePromptStatusHandler_1;

		MessageDialogWithToggle dialog = MessageDialogWithToggle.openYesNoCancelQuestion(activeShell, title, message, null, false, store, IInternalDebugUIConstants.PREF_RELAUNCH_IN_DEBUG_MODE);
		int buttonId = dialog.getReturnCode();
		if (buttonId == IDialogConstants.YES_ID) {
			relaunchInDebugMode(configuration);
			return Boolean.TRUE; // stops launch
		} else if (buttonId == IDialogConstants.NO_ID) {
			return Boolean.FALSE; // continue launch
		} else { //CANCEL
			return Boolean.TRUE; // stops the launch
		}
	}
	/**
	 * @param configuration
	 */
	private void relaunchInDebugMode(ILaunchConfiguration configuration) {
		DebugUITools.launch(configuration, ILaunchManager.DEBUG_MODE);
	}
}
