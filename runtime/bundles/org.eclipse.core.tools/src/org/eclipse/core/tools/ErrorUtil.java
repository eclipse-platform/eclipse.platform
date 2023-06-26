/*******************************************************************************
 * Copyright (c) 2002, 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tools;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * A helper class that supports error report and logging for
 * Spy plug-in classes.
 */
public class ErrorUtil {

	/**
	 * Logs the provided exception and user message in Spy plug-in's log.
	 *
	 * @param exception the exception to be logged
	 * @param userMessage an optional  higher-level explanation for the exception
	 */
	public static void logException(Exception exception, String userMessage) {
		if (userMessage == null)
			userMessage = exception.getMessage();
		IStatus status = new Status(IStatus.ERROR, ErrorUtil.class, -1, userMessage, exception);
		ILog.of(ErrorUtil.class).log(status);
	}

	/**
	 * Shows the provided message using a MessageDialog.
	 *
	 * @param message
	 * @param title
	 * @see org.eclipse.jface.dialogs.MessageDialog#openError(Shell,String,String)
	 */
	public static void showErrorMessage(String message, String title) {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		if (title == null)
			title = "Error in Spy plug-in"; //$NON-NLS-1$
		MessageDialog.openError(shell, title, message);
	}
}
