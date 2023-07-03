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

import java.util.ArrayList;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * A command handler to open a <code>MessageDialog</code> and return the
 * result.
 *
 * @since 3.2
 */
public class OpenMessageDialogHandler extends AbstractHandler {

	private static final String PARAM_ID_TITLE = "title"; //$NON-NLS-1$

	private static final String PARAM_ID_MESSAGE = "message"; //$NON-NLS-1$

	private static final String PARAM_ID_IMAGE_TYPE = "imageType"; //$NON-NLS-1$

	private static final String PARAM_ID_DEFAULT_INDEX = "defaultIndex"; //$NON-NLS-1$

	private static final String PARAM_ID_BUTTON_LABEL_PREFIX = "buttonLabel"; //$NON-NLS-1$

	private static final int BUTTON_LABEL_COUNT = 4;

	private static final String PARAM_ID_CANCEL_RETURNS = "cancelReturns"; //$NON-NLS-1$

	private static final int CANCEL_RETURN_CODE = -1;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		String title = event.getParameter(PARAM_ID_TITLE);
		String message = event.getParameter(PARAM_ID_MESSAGE);

		int imageType = MessageDialog.NONE;
		if (event.getParameter(PARAM_ID_IMAGE_TYPE) != null) {
			Integer imageTypeInteger = (Integer) event
					.getObjectParameterForExecution(PARAM_ID_IMAGE_TYPE);
			imageType = imageTypeInteger.intValue();
		}

		int defaultValue = 0;
		if (event.getParameter(PARAM_ID_DEFAULT_INDEX) != null) {
			Integer defaultValueInteger = (Integer) event
					.getObjectParameterForExecution(PARAM_ID_DEFAULT_INDEX);
			defaultValue = defaultValueInteger.intValue();
		}

		String[] buttonLabels = collectButtonLabels(event);

		IWorkbenchWindow activeWindow = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		Shell shell = (activeWindow != null) ? activeWindow.getShell() : null;

		MessageDialog dialog = new MessageDialog(shell, title, null, message,
				imageType, buttonLabels, defaultValue);
		int returnCode = dialog.open();

		if (returnCode == CANCEL_RETURN_CODE) {
			String cancelReturns = event.getParameter(PARAM_ID_CANCEL_RETURNS);
			if (cancelReturns != null)
				return cancelReturns;
			else
				throw new ExecutionException("dialog canceled"); //$NON-NLS-1$
		}

		return buttonLabels[returnCode];
	}

	private String[] collectButtonLabels(ExecutionEvent event) {

		ArrayList<String> buttonLabelList = new ArrayList<>();

		for (int i = 0; i < BUTTON_LABEL_COUNT; i++) {
			String buttonLabelParamId = PARAM_ID_BUTTON_LABEL_PREFIX
					+ Integer.toString(i);
			String buttonLabel = event.getParameter(buttonLabelParamId);

			if (buttonLabel == null) {
				break;
			}

			buttonLabelList.add(buttonLabel);
		}

		return buttonLabelList.toArray(new String[buttonLabelList
				.size()]);
	}

}
