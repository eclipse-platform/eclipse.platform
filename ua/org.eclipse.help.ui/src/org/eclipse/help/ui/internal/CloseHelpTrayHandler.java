/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others.
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
package org.eclipse.help.ui.internal;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.help.ui.internal.views.HelpTray;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * The command handler that gets invoked when the "Close User Assistance
 * Tray" command is invoked. It finds the current active dialog, and if it
 * has the tray open, it closes it.
 */
public class CloseHelpTrayHandler extends AbstractHandler {

	/**
	 * Executes the command.
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Display display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
		}
		if (display != null) {
			Shell shell = Display.getCurrent().getActiveShell();
			if (shell != null && !shell.isDisposed()) {
				Object shellData = shell.getData();
				if (shellData instanceof TrayDialog) {
					TrayDialog trayDialog = (TrayDialog)shellData;
					if (trayDialog.getTray() instanceof HelpTray) {
						trayDialog.closeTray();
					}
				}
			}
		}
		return null;
	}
}
