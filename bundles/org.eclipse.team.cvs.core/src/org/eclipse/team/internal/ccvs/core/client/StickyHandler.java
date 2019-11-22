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


import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.CVSEntryLineTag;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.core.syncinfo.MutableFolderSyncInfo;

/**
 * Handles any "Set-sticky" and "Clear-stick" responses from the CVS server.
 * <p>
 * Suppose as a result of performing a command the CVS server responds
 * as follows:
 * </p>
 * <pre>
 *   [...]
 *   Set-sticky myproject/ \n
 *   /u/cvsroot/myproject/ \n
 *   Tsometag \n
 *   [...]
 * </pre>
 * Then we set or clear the sticky tag property of the folder "myproject",
 * automatically creating it if it does not exist locally,
 */
class StickyHandler extends ResponseHandler {
	private final boolean setSticky;
		
	public StickyHandler(boolean setSticky) {
		this.setSticky = setSticky;
	}

	public String getResponseID() {
		if (setSticky) {
			return "Set-sticky"; //$NON-NLS-1$
		} else {
			return "Clear-sticky"; //$NON-NLS-1$
		}
	}

	public void handle(Session session, String localDir,
		IProgressMonitor monitor) throws CVSException {
		// read additional data for the response
		String repositoryDir = session.readLine();
		String tag = null;
		if (setSticky) {
			tag = session.readLine();
			if (tag != null && tag.length() == 0) tag = null;
		}

		// create the directory then set or clear the sticky tag
		Assert.isTrue(repositoryDir.endsWith("/")); //$NON-NLS-1$
		repositoryDir = repositoryDir.substring(0, repositoryDir.length() - 1);		
		try {
			ICVSFolder folder = createFolder(session, localDir, repositoryDir);
			FolderSyncInfo syncInfo = folder.getFolderSyncInfo();
			// Added to ignore sync info for workspace root
			if (syncInfo == null) return;
			MutableFolderSyncInfo newInfo = syncInfo.cloneMutable();
			newInfo.setTag(tag != null ? new CVSEntryLineTag(tag) : null);
			/* if we are reverting to BASE we do not change anything here 
			 * see bug 106876 */
			if(tag != null && tag.equals("TBASE"))  //$NON-NLS-1$
				newInfo.setTag(syncInfo.getTag());
			// only set the sync info if it has changed
			if (!syncInfo.equals(newInfo))
				folder.setFolderSyncInfo(newInfo);
		} catch (CVSException e) {
			if (!handleInvalidResourceName(session, session.getLocalRoot().getFolder(localDir), e)) {
				throw e;
			}
		}
	}
}

