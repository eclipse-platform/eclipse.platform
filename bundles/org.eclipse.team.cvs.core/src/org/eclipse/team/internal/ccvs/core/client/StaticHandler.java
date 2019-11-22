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


import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.core.syncinfo.MutableFolderSyncInfo;

/**
 * Handles any "Set-static-directory" and "Clear-static-directory" responses
 * from the CVS server.
 * <p>
 * Suppose as a result of performing a command the CVS server responds
 * as follows:
 * </p>
 * <pre>
 *   [...]
 *   Set-static-directory myproject/ \n
 *   /u/cvsroot/myproject/ \n
 *   [...]
 * </pre>
 * Then we set or clear the static flag of the folder "myproject",
 * automatically creating it if it does not exist locally,
 */
class StaticHandler extends ResponseHandler {
	private final boolean setStaticDirectory;

	public StaticHandler(boolean setStaticDirectory) {
		this.setStaticDirectory = setStaticDirectory;
	}

	public String getResponseID() {
		if (setStaticDirectory) {
			return "Set-static-directory"; //$NON-NLS-1$
		} else {
			return "Clear-static-directory"; //$NON-NLS-1$
		}
	}

	public void handle(Session session, String localDir,
		IProgressMonitor monitor) throws CVSException {
		// read additional data for the response
		String repositoryDir = session.readLine();

		// create the directory then set or clear the static flag
		Assert.isTrue(repositoryDir.endsWith("/")); //$NON-NLS-1$
		repositoryDir = repositoryDir.substring(0, repositoryDir.length() - 1);
		try {
			ICVSFolder folder = createFolder(session, localDir, repositoryDir);
			FolderSyncInfo syncInfo = folder.getFolderSyncInfo();
			// Added to ignore sync info for workspace root
			if (syncInfo == null) return;
			MutableFolderSyncInfo newInfo = syncInfo.cloneMutable();
			newInfo.setStatic(setStaticDirectory);
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

