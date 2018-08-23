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
package org.eclipse.team.internal.ccvs.core.client;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;

/**
 * The diff command needs to send a file structure to the server that differs somewhat from the canonical
 * format sent by other commands. Instead of sending new files as questionables this class sends
 * new files as modified and fakes them being added. The contents are sent to the server and are 
 * included in the returned diff report.
 */
class DiffStructureVisitor extends FileStructureVisitor {
	
	public DiffStructureVisitor(Session session, LocalOption[] localOptions) {
		super(session, localOptions, true, true);
	}
	
	/**
	 * Send unmanaged files as modified with a default entry line.
	 */
	protected void sendFile(ICVSFile mFile) throws CVSException {
		byte[] info = mFile.getSyncBytes();
		if (info==null)  {
			return;
		}
		
		// Send the parent folder if it hasn't been sent already
		sendFolder(mFile.getParent());
		Policy.checkCanceled(monitor);
		session.sendEntry(info, null);
		
		if (!mFile.exists()) {
			return;
		}

		if (mFile.isModified(null)) {
			session.sendModified(mFile, ResourceSyncInfo.isBinary(info), monitor);
		} else {
			session.sendUnchanged(mFile);
		}
	}			
}
