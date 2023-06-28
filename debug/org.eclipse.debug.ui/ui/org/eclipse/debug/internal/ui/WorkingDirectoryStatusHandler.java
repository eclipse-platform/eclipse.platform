/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui;


import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.jface.dialogs.MessageDialog;

/**
 * Prompts the user to re-launch when a working directory
 * is not supported by the Eclipse runtime.
 */
public class WorkingDirectoryStatusHandler implements IStatusHandler {

	/**
	 * @see IStatusHandler#handleStatus(IStatus, Object)
	 */
	@Override
	public Object handleStatus(IStatus status, Object source) {
		final boolean[] result = new boolean[1];
		DebugUIPlugin.getStandardDisplay().syncExec(() -> {
			String title = DebugUIMessages.WorkingDirectoryStatusHandler_Eclipse_Runtime_1;
			String message = DebugUIMessages.WorkingDirectoryStatusHandler_0;
			result[0] = (MessageDialog.openQuestion(DebugUIPlugin.getShell(), title, message));
		});
		return Boolean.valueOf(result[0]);
	}

}
