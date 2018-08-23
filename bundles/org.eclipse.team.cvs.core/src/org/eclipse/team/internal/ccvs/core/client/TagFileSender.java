/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
 * Special visitor which handles added and removed files in a special way.
 * Added resources are skipped. Deleted resources are sent as if they were not deleted.
 */
class TagFileSender extends FileStructureVisitor {

	public TagFileSender(Session session, LocalOption[] localOptions) {
		super(session, localOptions, false, false);
	}
	
	/** 
	 * Override sendFile to provide custom handling of added and deleted resources.
	 * Added resources are skipped. Deleted resources are sent as if they were not deleted.
	 */
	protected void sendFile(ICVSFile mFile) throws CVSException {
		Policy.checkCanceled(monitor);
		byte[] syncBytes = mFile.getSyncBytes();
		if (syncBytes != null) {
			// Send the parent folder if it hasn't been sent already
			sendFolder(mFile.getParent());
			// Send the file if appropriate
			if (ResourceSyncInfo.isDeletion(syncBytes)) {
				// makes this resource sync undeleted
				syncBytes = ResourceSyncInfo.convertFromDeletion(syncBytes);
			}
			if (!ResourceSyncInfo.isAddition(syncBytes)) {
				session.sendEntry(syncBytes, ResourceSyncInfo.getTimestampToServer(syncBytes, mFile.getTimeStamp()));
				session.sendIsModified(mFile, ResourceSyncInfo.isBinary(syncBytes), monitor);
			}
		}
	}
}
