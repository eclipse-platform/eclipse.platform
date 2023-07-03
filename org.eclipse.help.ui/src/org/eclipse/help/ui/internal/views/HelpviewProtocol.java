/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package org.eclipse.help.ui.internal.views;

import org.eclipse.help.internal.base.MissingContentManager;

/**
 * The helpview: protocol is used as a way to create hyperlinks which perform specific functions within the help view.
 */

public class HelpviewProtocol  {
	public static final String HELPVIEW_PROTOCOL = "helpview:"; //$NON-NLS-1$
	public static final String CHECK_REMOTE_STATUS = "checkremote"; //$NON-NLS-1$
	public static final String IGNORE_MISSING_BOOKS = "ignoreMissingBooks"; //$NON-NLS-1$

	public static void handleProtocolCall(String url, ReusableHelpPart part) {
		int index = url.indexOf(HELPVIEW_PROTOCOL);
		if (index == -1) {
			return;
		}
		String command = url.substring(index + HELPVIEW_PROTOCOL.length());
		if ( command.equals(CHECK_REMOTE_STATUS) ) {
			part.checkRemoteStatus();
		}
		if ( command.equals(IGNORE_MISSING_BOOKS)) {
			MissingContentManager.getInstance().ignoreAllMissingPlaceholders();
			part.checkPlaceholderStatus();
		}
	}
}
