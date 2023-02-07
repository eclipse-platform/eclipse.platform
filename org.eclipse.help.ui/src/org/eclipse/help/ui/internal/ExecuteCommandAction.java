/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
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
 ******************************************************************************/
package org.eclipse.help.ui.internal;

import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.CommandException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.ILiveHelpAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;

/**
 * Executes a serialized parameterized command using the workbench command
 * service. This class is intended to be called by the
 * <code>executeCommand</code> function in <code>livehelp.js</code> (defined
 * in <code>org.eclipse.help</code> plugin).
 *
 * @since 3.2
 */
public class ExecuteCommandAction implements ILiveHelpAction {

	/**
	 * Stores the serialized command for execution when the <code>run</code>
	 * method is called.
	 */
	private String serializedCommand;

	@Override
	public void setInitializationString(String data) {
		serializedCommand = data;
	}

	@Override
	public void run() {

		if (serializedCommand == null) {
			// No command to execute!
			return;
		}

		// workaround problem described in https://bugs.eclipse.org/bugs/show_bug.cgi?id=133694
		// by making sure we can get the workbench before running the command.  In standalone
		// help mode the attempt to get the workbench will fail and we will show an error dialog.
		IWorkbench workbench = null;
		try {
			workbench = PlatformUI.getWorkbench();
		}
		catch (IllegalStateException ex) {
			// this will happen when there is no workbench
		}
		if (workbench == null) {
			Display.getDefault().syncExec(() -> MessageDialog.openError(null, Messages.Help_Error,
					Messages.NoWorkbenchForExecuteCommand_msg));
			return;
		}

		Display.getDefault().syncExec(() -> {
			forceDialogsOnTop();
			executeSerializedCommand();
		});

	}

	/**
	 * This was introduced to work around the behavior described in
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=130206
	 */
	private void forceDialogsOnTop() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		Display display = workbench.getDisplay();
		/*
		 * If the active shell is not in this display (e.g. help window),
		 * bring the active workbench window up.
		 */
		if (display.getActiveShell() == null) {
			IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
			if (window != null) {
				Shell windowShell = window.getShell();
				windowShell.forceActive();
				if (Platform.getWS().equals(Platform.WS_WIN32)) {
					// feature in Windows. Without this code,
					// the window will only flash in the launch bar.
					windowShell.setVisible(false);
					windowShell.setMinimized(true);
					windowShell.setVisible(true);
					windowShell.setMinimized(false);
				}
			}
		}
	}

	private void executeSerializedCommand() {
		try {
			ICommandService commandService = getCommandService();
			IHandlerService handlerService = getHandlerService();
			ParameterizedCommand command = commandService.deserialize(serializedCommand);
			handlerService.executeCommand(command, null);
		} catch (CommandException ex) {
			Platform.getLog(getClass()).error("There was an error executing the command: " + serializedCommand, //$NON-NLS-1$
					ex);
		}
	}

	private static ICommandService getCommandService() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		Object serviceObject = workbench.getAdapter(ICommandService.class);
		if (serviceObject != null) {
			ICommandService service = (ICommandService) serviceObject;
			return service;
		}
		return null;
	}

	private IHandlerService getHandlerService() {
		IWorkbench wb =	PlatformUI.getWorkbench();
		if (wb != null) {
			Object serviceObject = wb.getAdapter(IHandlerService.class);
			if (serviceObject != null) {
				IHandlerService service = (IHandlerService)serviceObject;
				return service;
			}
		}
		return null;
	}
}
