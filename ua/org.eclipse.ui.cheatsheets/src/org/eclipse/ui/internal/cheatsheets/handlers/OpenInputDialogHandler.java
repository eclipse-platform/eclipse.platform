/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
package org.eclipse.ui.internal.cheatsheets.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * A command handler to open an <code>InputDialog</code> and return the
 * result.
 *
 * @since 3.2
 */
public class OpenInputDialogHandler extends AbstractHandler {

	private static final String PARAM_ID_TITLE = "title"; //$NON-NLS-1$

	private static final String PARAM_ID_MESSAGE = "message"; //$NON-NLS-1$

	private static final String PARAM_ID_INITIAL_VALUE = "initialValue"; //$NON-NLS-1$

	private static final String PARAM_ID_CANCEL_RETURNS = "cancelReturns"; //$NON-NLS-1$

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		String title = event.getParameter(PARAM_ID_TITLE);
		String message = event.getParameter(PARAM_ID_MESSAGE);
		String initialValue = event.getParameter(PARAM_ID_INITIAL_VALUE);

		IWorkbenchWindow activeWindow = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		Shell shell = (activeWindow != null) ? activeWindow.getShell() : null;

		InputDialog dialog = new InputDialog(shell, title, message,
				initialValue, null);
		int returnCode = dialog.open();

		if (returnCode == Window.CANCEL) {
			String cancelReturns = event.getParameter(PARAM_ID_CANCEL_RETURNS);
			if (cancelReturns != null)
				return cancelReturns;
			else
				throw new ExecutionException("dialog canceled"); //$NON-NLS-1$
		}

		return dialog.getValue();
	}

}
