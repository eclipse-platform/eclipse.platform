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
package org.eclipse.debug.internal.ui.launchConfigurations;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
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

public class CompileErrorProjectPromptStatusHandler implements IStatusHandler {

	@Override
	public Object handleStatus(IStatus status, Object source) throws CoreException {
		ILaunchConfiguration config = null;
		List<Object> projects = new ArrayList<>();
		if (source instanceof List) {
			List<?> args = (List<?>) source;
			Iterator<?> iterator = args.iterator();
			while (iterator.hasNext()) {
				Object arg = iterator.next();
				if (arg instanceof ILaunchConfiguration) {
					config = (ILaunchConfiguration) arg;
					if (DebugUITools.isPrivate(config)) {
						return Boolean.TRUE;
					}
				} else if (arg instanceof IProject) {
					projects.add(arg);
				}
			}
		}
		Shell shell = DebugUIPlugin.getShell();
		StringBuilder projectList = new StringBuilder();
		//we need to limit this
		int size = Math.min(20, projects.size());
		for (int i = 0; i < size; i++) {
			if (i > 0) {
				projectList.append(", "); //$NON-NLS-1$
			}
			projectList.append(((IProject)projects.get(i)).getName());
		}
		String projectMessage = null;
		if(projects.size() > 20) {
			projectMessage = MessageFormat.format(LaunchConfigurationsMessages.CompileErrorProjectPromptStatusHandler_0, new Object[]{projectList.toString()});
		} else{
			projectMessage = projectList.toString();
		}
		String title =  LaunchConfigurationsMessages.CompileErrorPromptStatusHandler_0;
		String message = MessageFormat.format(LaunchConfigurationsMessages.CompileErrorPromptStatusHandler_2, new Object[] { projectMessage });
		IPreferenceStore store = DebugUIPlugin.getDefault().getPreferenceStore();

		String pref = store.getString(IInternalDebugUIConstants.PREF_CONTINUE_WITH_COMPILE_ERROR);
		if (pref != null) {
			if (pref.equals(MessageDialogWithToggle.ALWAYS)) {
				return Boolean.TRUE;
			}
		}
		MessageDialogWithToggle dialog = new MessageDialogWithToggle(shell,
				title,
				null,
				message,
				MessageDialog.QUESTION,
				new String[] {IDialogConstants.PROCEED_LABEL, IDialogConstants.CANCEL_LABEL},
				0,
				LaunchConfigurationsMessages.CompileErrorProjectPromptStatusHandler_1,
				false);
		int open = dialog.open();
		if (open == IDialogConstants.PROCEED_ID) {
			if(dialog.getToggleState()) {
				store.setValue(IInternalDebugUIConstants.PREF_CONTINUE_WITH_COMPILE_ERROR, MessageDialogWithToggle.ALWAYS);
			}
			return Boolean.TRUE;
		}
		else {
			return Boolean.FALSE;
		}
	}
}
