/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.launchConfigurations;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Shell;


public class CompileErrorPromptStatusHandler implements IStatusHandler {

	@Override
	public Object handleStatus(IStatus status, Object source) throws CoreException {
		if (source instanceof ILaunchConfiguration) {
			ILaunchConfiguration config = (ILaunchConfiguration)source;
			if (DebugUITools.isPrivate(config)) {
				return Boolean.TRUE;
			}
		}

		Shell shell = DebugUIPlugin.getShell();
		String title = LaunchConfigurationsMessages.CompileErrorPromptStatusHandler_0;
		String message = LaunchConfigurationsMessages.CompileErrorPromptStatusHandler_1;
		IPreferenceStore store = DebugUIPlugin.getDefault().getPreferenceStore();

		String pref = store.getString(IInternalDebugUIConstants.PREF_CONTINUE_WITH_COMPILE_ERROR);
		if (pref != null) {
			if (pref.equals(MessageDialogWithToggle.ALWAYS)) {
				return Boolean.TRUE;
			}
		}
		MessageDialogWithToggle dialog = new MessageDialogWithToggle(
				shell,
				title,
				null,
				message,
				MessageDialog.WARNING,
				new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL},
				1,
				LaunchConfigurationsMessages.CompileErrorProjectPromptStatusHandler_1,
				false);
		dialog.setPrefKey(IInternalDebugUIConstants.PREF_CONTINUE_WITH_COMPILE_ERROR);
		dialog.setPrefStore(store);
		dialog.open();

		int returnValue = dialog.getReturnCode();
		if (returnValue == IDialogConstants.YES_ID) {
			return Boolean.TRUE;
		}
		return Boolean.FALSE;
	}
}
