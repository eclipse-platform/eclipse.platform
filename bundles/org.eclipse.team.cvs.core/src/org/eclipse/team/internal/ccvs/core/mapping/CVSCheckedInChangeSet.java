/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.core.mapping;

import java.text.DateFormat;
import java.util.Date;

import org.eclipse.team.internal.ccvs.core.ILogEntry;
import org.eclipse.team.internal.ccvs.core.client.listeners.LogEntry;
import org.eclipse.team.internal.core.subscribers.DiffChangeSet;

public class CVSCheckedInChangeSet extends DiffChangeSet {

	private final ILogEntry entry;

	public CVSCheckedInChangeSet(ILogEntry entry) {
		this.entry = entry;
		Date date = entry.getDate();
		String comment = LogEntry.flattenText(entry.getComment());
		if (date == null) {
			setName("["+entry.getAuthor()+ "] " + comment); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			String dateString = DateFormat.getDateTimeInstance().format(date);
			setName("["+entry.getAuthor()+ "] (" + dateString +") " + comment); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
		}
	}
	
	public String getAuthor() {
		return entry.getAuthor();
	}

	public Date getDate() {
		return entry.getDate();
	}

	public String getComment() {
		return entry.getComment();
	}
}