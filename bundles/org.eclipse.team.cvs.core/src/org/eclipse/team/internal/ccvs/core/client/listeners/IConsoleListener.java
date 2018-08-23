/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.core.client.listeners;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.internal.ccvs.core.client.Session;

public interface IConsoleListener {
	/**
	 * Called when a command is invoked.
	 * @param session the session that the command is being executed over
	 * @param line the command invocation string
	 */
	public void commandInvoked(Session session, String line);
	
	/**
	 * Called when a line of message text has been received.
	 * @param session the session that the command is being executed over
	 * @param line the line of text
	 * @param status the status returned from the command message parser
	 */
	public void messageLineReceived(Session session, String line, IStatus status);
	
	/**
	 * Called when a line of error text has been received.
	 * @param session the session that the command is being executed over
	 * @param line the line of text
	 * @param status the status returned from the command message parser
	 */
	public void errorLineReceived(Session session, String line, IStatus status);
	
	/**
	 * Called when a command has been completed.
	 * @param session the session that the command is being executed over
	 * @param status the status code, or null if not applicable
	 * @param exception an exception, or null if not applicable
	 */
	public void commandCompleted(Session session, IStatus status, Exception exception);
}
