/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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


import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.syncinfo.MutableResourceSyncInfo;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;

public class NewEntryHandler extends ResponseHandler {

	@Override
	public String getResponseID() {
		return "New-entry"; //$NON-NLS-1$
	}

	@Override
	public void handle(Session session, String localDir, IProgressMonitor monitor)
		throws CVSException {
			
		// read additional data for the response
		String repositoryFile = session.readLine();
		String entryLine = session.readLine();
		
		// Clear the recorded mod-time
		session.setModTime(null);
		
		// Get the local file		
		String fileName = repositoryFile.substring(repositoryFile.lastIndexOf("/") + 1); //$NON-NLS-1$
		ICVSFolder mParent = session.getLocalRoot().getFolder(localDir);
		ICVSFile mFile = mParent.getFile(fileName);

		ResourceSyncInfo fileInfo = mFile.getSyncInfo();
		MutableResourceSyncInfo newInfo = fileInfo.cloneMutable();
		newInfo.setEntryLine(entryLine);
		// Set the tag to the previous tag if the new tag is the base tag (see bug 106876)
		CVSTag newTag = newInfo.getTag();
		if(newTag != null && newTag.isBaseTag()) { 
			newInfo.setTag(fileInfo.getTag());
		}
		mFile.setSyncInfo(newInfo, ICVSFile.UNKNOWN);
	}
}
