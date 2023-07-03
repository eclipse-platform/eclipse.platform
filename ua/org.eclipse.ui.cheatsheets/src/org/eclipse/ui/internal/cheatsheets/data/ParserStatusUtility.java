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

package org.eclipse.ui.internal.cheatsheets.data;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.internal.cheatsheets.ICheatSheetResource;
import org.eclipse.ui.internal.cheatsheets.Messages;

public class ParserStatusUtility {

	public final static int PARSER_ERROR = 1001; // TODO is there another number that would be more meaningful

	/**
	 * Modify an existing IStatus to add information about a new error/warning.
	 * If the old status is OK return a status reflecting the new error condition, otherwise
	 * add to the existing status making it a MultiStatus if necessary.
	 */
	public static IStatus addStatus(IStatus status, int severity, String message, Throwable exception) {
		Status newStatus = new Status(severity, ICheatSheetResource.CHEAT_SHEET_PLUGIN_ID, PARSER_ERROR, message, exception);
		if (status.isOK()) {
			return newStatus;
		} else if (status instanceof MultiStatus) {
			((MultiStatus)status).add(newStatus);
			return status;
		} else {
			MultiStatus multiStatus = new MultiStatus(ICheatSheetResource.CHEAT_SHEET_PLUGIN_ID, IStatus.OK,
					Messages.ERROR_MULTIPLE_ERRORS,  exception);
			multiStatus.add(status);
			multiStatus.add(newStatus);
			return multiStatus;
		}
	}

}
