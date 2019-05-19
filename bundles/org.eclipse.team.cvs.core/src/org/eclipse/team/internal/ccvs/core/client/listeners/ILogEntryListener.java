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

import org.eclipse.team.internal.ccvs.core.ILogEntry;

/**
 * Interface for receiving log entries from the <code>LogListener</code>
 */
public interface ILogEntryListener {

	/**
	 * A log entry was received for the current file
	 * @param entry the received log entry.
	 */
	void handleLogEntryReceived(ILogEntry entry);
	
}
